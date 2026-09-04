package com.campusmarket.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusmarket.common.PageResult;
import com.campusmarket.entity.Favorite;
import com.campusmarket.entity.Item;
import com.campusmarket.exception.BusinessException;
import com.campusmarket.mapper.FavoriteMapper;
import com.campusmarket.mapper.ItemMapper;
import com.campusmarket.security.LoginUser;
import com.campusmarket.vo.ItemVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ItemMapper itemMapper;
    private final ItemService itemService;

    public FavoriteService(FavoriteMapper favoriteMapper,
                           ItemMapper itemMapper,
                           ItemService itemService) {
        this.favoriteMapper = favoriteMapper;
        this.itemMapper = itemMapper;
        this.itemService = itemService;
    }

    public void add(Long itemId, LoginUser loginUser) {
        if (itemId == null) {
            throw new BusinessException("商品id不能为空");
        }

        Item item = itemMapper.selectById(itemId);
        if (item == null || !"ON_SALE".equals(item.getStatus())) {
            throw new BusinessException("商品不存在或已下架");
        }

        Long count = favoriteMapper.selectCount(
                Wrappers.<Favorite>lambdaQuery()
                        .eq(Favorite::getUserId, loginUser.id())
                        .eq(Favorite::getItemId, itemId));
        if (count != null && count > 0) {
            return;
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(loginUser.id());
        favorite.setItemId(itemId);
        favoriteMapper.insert(favorite);
    }

    public void remove(Long itemId, LoginUser loginUser) {
        if (itemId == null) {
            throw new BusinessException("商品id不能为空");
        }

        favoriteMapper.delete(
                Wrappers.<Favorite>lambdaQuery()
                        .eq(Favorite::getUserId, loginUser.id())
                        .eq(Favorite::getItemId, itemId));
    }

    public PageResult<ItemVO> myFavorites(LoginUser loginUser, long page, long size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException("分页参数不正确");
        }

        Page<Favorite> favoritePage = favoriteMapper.selectPage(
                new Page<>(page, size),
                Wrappers.<Favorite>lambdaQuery()
                        .eq(Favorite::getUserId, loginUser.id())
                        .orderByDesc(Favorite::getCreateTime));

        List<Favorite> favorites = favoritePage.getRecords();
        if (favorites.isEmpty()) {
            return new PageResult<>(List.of(), favoritePage.getTotal(), page, size);
        }

        List<Long> itemIds = favorites.stream()
                .map(Favorite::getItemId)
                .distinct()
                .collect(Collectors.toList());

        List<Item> items = itemMapper.selectList(
                Wrappers.<Item>lambdaQuery().in(Item::getId, itemIds));
        Map<Long, Item> itemMap = items.stream()
                .collect(Collectors.toMap(Item::getId, Function.identity(), (left, right) -> left));

        List<Item> orderedItems = favorites.stream()
                .map(favorite -> itemMap.get(favorite.getItemId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PageResult<>(itemService.toItemVOs(orderedItems),
                favoritePage.getTotal(),
                favoritePage.getCurrent(),
                favoritePage.getSize());
    }
}
