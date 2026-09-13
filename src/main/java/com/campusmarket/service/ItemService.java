package com.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusmarket.common.PageResult;
import com.campusmarket.dto.CreateItemRequest;
import com.campusmarket.dto.UpdateItemRequest;
import com.campusmarket.entity.Category;
import com.campusmarket.entity.Item;
import com.campusmarket.entity.User;
import com.campusmarket.exception.BusinessException;
import com.campusmarket.mapper.CategoryMapper;
import com.campusmarket.mapper.ItemMapper;
import com.campusmarket.mapper.UserMapper;
import com.campusmarket.security.LoginUser;
import com.campusmarket.vo.ItemVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private static final String HOT_ITEMS_KEY = "cache:item:hot";
    private static final long HOT_ITEMS_TTL_SECONDS = 300L;
    private static final String SEARCH_CACHE_VERSION_KEY = "cache:item:search:v1:version";
    private static final String SEARCH_CACHE_PREFIX = "cache:item:search:v1:";
    private static final long SEARCH_CACHE_TTL_SECONDS = 90L;
    private static final String INITIAL_SEARCH_CACHE_VERSION = "0";

    private final ItemMapper itemMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public ItemService(ItemMapper itemMapper,
                       CategoryMapper categoryMapper,
                       UserMapper userMapper,
                       ObjectMapper objectMapper,
                       StringRedisTemplate redisTemplate) {
        this.itemMapper = itemMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    public Long publish(CreateItemRequest request, LoginUser loginUser) {
        Item item = new Item();
        item.setSellerId(loginUser.id());
        applyItemContent(item, request.getTitle(), request.getDescription(), request.getPrice(),
                request.getCategoryId(), request.getImages());
        item.setStatus("ON_SALE");

        itemMapper.insert(item);
        evictItemCaches();
        return item.getId();
    }

    public void update(Long id, UpdateItemRequest request, LoginUser loginUser) {
        Item item = getOwnedItem(id, loginUser);
        if (!"ON_SALE".equals(item.getStatus())) {
            throw new BusinessException("商品当前状态不能修改");
        }

        Item content = new Item();
        applyItemContent(content, request.getTitle(), request.getDescription(), request.getPrice(),
                request.getCategoryId(), request.getImages());
        int updated = itemMapper.update(null, Wrappers.<Item>lambdaUpdate()
                .eq(Item::getId, id)
                .eq(Item::getSellerId, loginUser.id())
                .eq(Item::getStatus, "ON_SALE")
                .set(Item::getTitle, content.getTitle())
                .set(Item::getDescription, content.getDescription())
                .set(Item::getPrice, content.getPrice())
                .set(Item::getCategoryId, content.getCategoryId())
                .set(Item::getImages, content.getImages()));
        if (updated != 1) {
            throw new BusinessException("商品状态已发生变化，请刷新后重试");
        }
        evictItemCaches();
    }

    public void takeOffShelf(Long id, LoginUser loginUser) {
        Item item = getOwnedItem(id, loginUser);
        if ("SOLD".equals(item.getStatus())) {
            throw new BusinessException("已售出商品不能下架");
        }
        if (!"ON_SALE".equals(item.getStatus())) {
            return;
        }

        int updated = itemMapper.update(null, Wrappers.<Item>lambdaUpdate()
                .eq(Item::getId, id)
                .eq(Item::getSellerId, loginUser.id())
                .eq(Item::getStatus, "ON_SALE")
                .set(Item::getStatus, "OFF_SHELF"));
        if (updated != 1) {
            throw new BusinessException("商品状态已发生变化，请刷新后重试");
        }
        evictItemCaches();
    }

    public void relist(Long id, LoginUser loginUser) {
        Item item = getOwnedItem(id, loginUser);
        if (!"OFF_SHELF".equals(item.getStatus())) {
            throw new BusinessException("只有已下架商品可以重新上架");
        }

        int updated = itemMapper.update(null, Wrappers.<Item>lambdaUpdate()
                .eq(Item::getId, id)
                .eq(Item::getSellerId, loginUser.id())
                .eq(Item::getStatus, "OFF_SHELF")
                .set(Item::getStatus, "ON_SALE"));
        if (updated != 1) {
            throw new BusinessException("商品状态已发生变化，请刷新后重试");
        }
        evictItemCaches();
    }

    public List<ItemVO> getHotItems() {
        try {
            String cached = redisTemplate.opsForValue().get(HOT_ITEMS_KEY);
            if (StringUtils.hasText(cached)) {
                return objectMapper.readValue(cached, new TypeReference<List<ItemVO>>() {
                });
            }
        } catch (RuntimeException | JsonProcessingException ignored) {
            // Redis unavailable or cache data invalid: fall through to MySQL.
        }

        List<Item> items = itemMapper.selectList(Wrappers.<Item>lambdaQuery()
                .eq(Item::getStatus, "ON_SALE")
                .orderByDesc(Item::getCreateTime)
                .last("LIMIT 10"));
        List<ItemVO> itemVos = toItemVOs(items);

        try {
            redisTemplate.opsForValue().set(HOT_ITEMS_KEY,
                    objectMapper.writeValueAsString(itemVos),
                    HOT_ITEMS_TTL_SECONDS,
                    TimeUnit.SECONDS);
        } catch (RuntimeException | JsonProcessingException ignored) {
            // Cache write failure should not break the business request.
        }

        return itemVos;
    }

    public void evictHotCache() {
        try {
            redisTemplate.delete(HOT_ITEMS_KEY);
        } catch (RuntimeException ignored) {
            // Redis unavailable: cache will expire by TTL.
        }
    }

    public void evictItemCaches() {
        evictHotCache();
        try {
            redisTemplate.opsForValue().increment(SEARCH_CACHE_VERSION_KEY);
        } catch (RuntimeException ignored) {
            // Redis unavailable: versioned search keys will expire by TTL.
        }
    }

    public void evictItemCachesAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictItemCaches();
                }
            });
            return;
        }
        evictItemCaches();
    }

    private Item getOwnedItem(Long id, LoginUser loginUser) {
        Item item = itemMapper.selectById(id);
        if (item == null || !item.getSellerId().equals(loginUser.id())) {
            throw new BusinessException("商品不存在或无权操作");
        }
        return item;
    }

    private void applyItemContent(Item item,
                                  String title,
                                  String description,
                                  BigDecimal price,
                                  Long categoryId,
                                  List<String> images) {
        String trimmedTitle = trimToNull(title);
        if (trimmedTitle == null) {
            throw new BusinessException("商品标题不能为空");
        }
        if (trimmedTitle.length() > 100) {
            throw new BusinessException("商品标题不能超过100个字符");
        }
        if (categoryId == null) {
            throw new BusinessException("请选择商品分类");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("商品价格必须大于0");
        }
        if (images != null && images.size() > 5) {
            throw new BusinessException("最多只能保存5张商品图片");
        }

        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException("商品分类不存在");
        }

        item.setCategoryId(category.getId());
        item.setTitle(trimmedTitle);
        item.setDescription(trimToNull(description));
        item.setPrice(price);
        item.setImages(toImagesJson(images));
    }

    public PageResult<ItemVO> search(String keyword,
                                     Long categoryId,
                                     BigDecimal minPrice,
                                     BigDecimal maxPrice,
                                     String sort,
                                     long page,
                                     long size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException("分页参数不正确");
        }
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("最低价格不能小于0");
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("最高价格不能小于0");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException("最低价格不能大于最高价格");
        }

        String normalizedKeyword = trimToNull(keyword);
        String normalizedSort = normalizeSort(sort);
        String cacheKey = buildSearchCacheKey(
                normalizedKeyword, categoryId, minPrice, maxPrice, normalizedSort, page, size);

        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cached)) {
                return objectMapper.readValue(cached, new TypeReference<PageResult<ItemVO>>() {
                });
            }
        } catch (RuntimeException | JsonProcessingException ignored) {
            // Redis unavailable or cache data invalid: fall through to MySQL.
        }

        LambdaQueryWrapper<Item> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper
                .eq(Item::getStatus, "ON_SALE")
                .eq(categoryId != null, Item::getCategoryId, categoryId)
                .ge(minPrice != null, Item::getPrice, minPrice)
                .le(maxPrice != null, Item::getPrice, maxPrice)
                .and(normalizedKeyword != null, wrapper -> wrapper
                        .like(Item::getTitle, normalizedKeyword)
                        .or()
                        .like(Item::getDescription, normalizedKeyword));

        if ("priceAsc".equals(normalizedSort)) {
            queryWrapper.orderByAsc(Item::getPrice).orderByDesc(Item::getCreateTime);
        } else if ("priceDesc".equals(normalizedSort)) {
            queryWrapper.orderByDesc(Item::getPrice).orderByDesc(Item::getCreateTime);
        } else {
            queryWrapper.orderByDesc(Item::getCreateTime);
        }

        Page<Item> itemPage = itemMapper.selectPage(new Page<>(page, size), queryWrapper);
        List<Item> records = itemPage.getRecords();
        PageResult<ItemVO> result = new PageResult<>(toItemVOs(records),
                itemPage.getTotal(), itemPage.getCurrent(), itemPage.getSize());

        try {
            redisTemplate.opsForValue().set(cacheKey,
                    objectMapper.writeValueAsString(result),
                    SEARCH_CACHE_TTL_SECONDS,
                    TimeUnit.SECONDS);
        } catch (RuntimeException | JsonProcessingException ignored) {
            // Cache write failure should not break the business request.
        }
        return result;
    }

    private String normalizeSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return "latest";
        }
        return switch (sort.trim()) {
            case "priceAsc", "priceDesc", "latest" -> sort.trim();
            default -> throw new BusinessException("排序方式不正确");
        };
    }

    private String buildSearchCacheKey(String keyword,
                                       Long categoryId,
                                       BigDecimal minPrice,
                                       BigDecimal maxPrice,
                                       String sort,
                                       long page,
                                       long size) {
        String version = currentSearchCacheVersion();

        String canonical = String.join("|",
                keyword == null ? "" : keyword.toLowerCase(Locale.ROOT),
                categoryId == null ? "" : categoryId.toString(),
                minPrice == null ? "" : minPrice.stripTrailingZeros().toPlainString(),
                maxPrice == null ? "" : maxPrice.stripTrailingZeros().toPlainString(),
                sort,
                Long.toString(page),
                Long.toString(size));
        return SEARCH_CACHE_PREFIX + version + ":" + sha256(canonical);
    }

    private String currentSearchCacheVersion() {
        try {
            String storedVersion = redisTemplate.opsForValue().get(SEARCH_CACHE_VERSION_KEY);
            if (StringUtils.hasText(storedVersion)) {
                return storedVersion;
            }

            Boolean initialized = redisTemplate.opsForValue()
                    .setIfAbsent(SEARCH_CACHE_VERSION_KEY, INITIAL_SEARCH_CACHE_VERSION);
            if (Boolean.TRUE.equals(initialized)) {
                return INITIAL_SEARCH_CACHE_VERSION;
            }

            String initializedVersion = redisTemplate.opsForValue().get(SEARCH_CACHE_VERSION_KEY);
            return StringUtils.hasText(initializedVersion)
                    ? initializedVersion
                    : INITIAL_SEARCH_CACHE_VERSION;
        } catch (RuntimeException ignored) {
            // A local fallback version still produces a valid short-lived key.
            return INITIAL_SEARCH_CACHE_VERSION;
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    public PageResult<ItemVO> myItems(LoginUser loginUser,
                                      String status,
                                      long page,
                                      long size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException("分页参数不正确");
        }

        LambdaQueryWrapper<Item> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper
                .eq(Item::getSellerId, loginUser.id())
                .eq(StringUtils.hasText(status), Item::getStatus, status)
                .orderByDesc(Item::getCreateTime);

        Page<Item> itemPage = itemMapper.selectPage(new Page<>(page, size), queryWrapper);
        return new PageResult<>(toItemVOs(itemPage.getRecords()),
                itemPage.getTotal(),
                itemPage.getCurrent(),
                itemPage.getSize());
    }

    public ItemVO getDetail(Long id) {
        return getDetailForUser(id, null);
    }

    public ItemVO getDetailForUser(Long id, LoginUser loginUser) {
        Item item = itemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException("商品不存在或已下架");
        }
        boolean owner = loginUser != null && item.getSellerId().equals(loginUser.id());
        if (!owner && !"ON_SALE".equals(item.getStatus())) {
            throw new BusinessException("商品不存在或已下架");
        }
        return toItemVOs(List.of(item)).get(0);
    }

    public ItemVO getOwnedDetail(Long id, LoginUser loginUser) {
        Item item = getOwnedItem(id, loginUser);
        return toItemVOs(List.of(item)).get(0);
    }

    public List<ItemVO> toItemVOs(List<Item> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> sellerIds = items.stream().map(Item::getSellerId).distinct().collect(Collectors.toList());
        List<Long> categoryIds = items.stream().map(Item::getCategoryId).distinct().collect(Collectors.toList());

        Map<Long, String> usernameMap = userMapper.selectList(
                        Wrappers.<User>lambdaQuery().in(User::getId, sellerIds))
                .stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (left, right) -> left));

        Map<Long, String> categoryNameMap = categoryMapper.selectList(
                        Wrappers.<Category>lambdaQuery().in(Category::getId, categoryIds))
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (left, right) -> left));

        List<ItemVO> itemVos = new ArrayList<>();
        for (Item item : items) {
            itemVos.add(toItemVO(item, usernameMap, categoryNameMap));
        }
        return itemVos;
    }

    private ItemVO toItemVO(Item item,
                            Map<Long, String> usernameMap,
                            Map<Long, String> categoryNameMap) {
        ItemVO vo = new ItemVO();
        vo.setId(item.getId());
        vo.setSellerId(item.getSellerId());
        vo.setSellerUsername(usernameMap.get(item.getSellerId()));
        vo.setCategoryId(item.getCategoryId());
        vo.setCategoryName(categoryNameMap.get(item.getCategoryId()));
        vo.setTitle(item.getTitle());
        vo.setDescription(item.getDescription());
        vo.setPrice(item.getPrice());
        vo.setImages(parseImages(item.getImages()));
        vo.setStatus(item.getStatus());
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }

    private String toImagesJson(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        List<String> normalizedImages = images.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(image -> image.length() <= 500)
                .distinct()
                .limit(5)
                .toList();
        if (normalizedImages.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(normalizedImages);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("图片信息格式错误");
        }
    }

    private List<String> parseImages(String imagesJson) {
        if (!StringUtils.hasText(imagesJson)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            return Collections.emptyList();
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
