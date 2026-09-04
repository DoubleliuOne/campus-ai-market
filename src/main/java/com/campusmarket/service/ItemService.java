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
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ItemService {

    private final ItemMapper itemMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public ItemService(ItemMapper itemMapper,
                       CategoryMapper categoryMapper,
                       UserMapper userMapper,
                       ObjectMapper objectMapper) {
        this.itemMapper = itemMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    public Long publish(CreateItemRequest request, LoginUser loginUser) {
        Item item = new Item();
        item.setSellerId(loginUser.id());
        applyItemContent(item, request.getTitle(), request.getDescription(), request.getPrice(),
                request.getCategoryId(), request.getImages());
        item.setStatus("ON_SALE");

        itemMapper.insert(item);
        return item.getId();
    }

    public void update(Long id, UpdateItemRequest request, LoginUser loginUser) {
        Item item = getOwnedItem(id, loginUser);
        if (!"ON_SALE".equals(item.getStatus())) {
            throw new BusinessException("商品当前状态不能修改");
        }

        applyItemContent(item, request.getTitle(), request.getDescription(), request.getPrice(),
                request.getCategoryId(), request.getImages());
        itemMapper.updateById(item);
    }

    public void takeOffShelf(Long id, LoginUser loginUser) {
        Item item = getOwnedItem(id, loginUser);
        if ("SOLD".equals(item.getStatus())) {
            throw new BusinessException("已售出商品不能下架");
        }
        if (!"ON_SALE".equals(item.getStatus())) {
            return;
        }

        item.setStatus("OFF_SHELF");
        itemMapper.updateById(item);
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
                                     long page,
                                     long size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException("分页参数不正确");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BusinessException("最低价格不能大于最高价格");
        }

        LambdaQueryWrapper<Item> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper
                .eq(Item::getStatus, "ON_SALE")
                .eq(categoryId != null, Item::getCategoryId, categoryId)
                .ge(minPrice != null, Item::getPrice, minPrice)
                .le(maxPrice != null, Item::getPrice, maxPrice)
                .and(StringUtils.hasText(keyword), wrapper -> wrapper
                        .like(Item::getTitle, keyword)
                        .or()
                        .like(Item::getDescription, keyword))
                .orderByDesc(Item::getCreateTime);

        Page<Item> itemPage = itemMapper.selectPage(new Page<>(page, size), queryWrapper);
        List<Item> records = itemPage.getRecords();
        return new PageResult<>(toItemVOs(records), itemPage.getTotal(), itemPage.getCurrent(), itemPage.getSize());
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
        Item item = itemMapper.selectById(id);
        if (item == null || !"ON_SALE".equals(item.getStatus())) {
            throw new BusinessException("商品不存在或已下架");
        }

        Map<Long, String> usernameMap = new HashMap<>();
        Map<Long, String> categoryNameMap = new HashMap<>();

        User seller = userMapper.selectById(item.getSellerId());
        Category category = categoryMapper.selectById(item.getCategoryId());
        if (seller != null) {
            usernameMap.put(seller.getId(), seller.getUsername());
        }
        if (category != null) {
            categoryNameMap.put(category.getId(), category.getName());
        }

        return toItemVO(item, usernameMap, categoryNameMap);
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
        try {
            return objectMapper.writeValueAsString(images);
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
