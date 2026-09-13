package com.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusmarket.common.PageResult;
import com.campusmarket.dto.CreateOrderRequest;
import com.campusmarket.dto.UpdateOrderStatusRequest;
import com.campusmarket.entity.Item;
import com.campusmarket.entity.Order;
import com.campusmarket.entity.User;
import com.campusmarket.exception.BusinessException;
import com.campusmarket.mapper.ItemMapper;
import com.campusmarket.mapper.OrderMapper;
import com.campusmarket.mapper.UserMapper;
import com.campusmarket.security.LoginUser;
import com.campusmarket.vo.OrderVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;
    private final ItemService itemService;

    public OrderService(OrderMapper orderMapper,
                        ItemMapper itemMapper,
                        UserMapper userMapper,
                        ObjectMapper objectMapper,
                        ItemService itemService) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
        this.itemService = itemService;
    }

    @Transactional
    public Long create(CreateOrderRequest request, LoginUser loginUser) {
        Long itemId = request.getItemId();
        if (itemId == null) {
            throw new BusinessException("商品id不能为空");
        }

        Item item = itemMapper.selectByIdForUpdate(itemId);
        if (item == null) {
            throw new BusinessException("商品不存在");
        }
        if (item.getSellerId().equals(loginUser.id())) {
            throw new BusinessException("不能购买自己发布的商品");
        }

        int updated = itemMapper.update(null, Wrappers.<Item>lambdaUpdate()
                .eq(Item::getId, itemId)
                .eq(Item::getStatus, "ON_SALE")
                .set(Item::getStatus, "SOLD"));
        if (updated != 1) {
            throw new BusinessException("商品已售出或已下架");
        }

        Order order = new Order();
        order.setBuyerId(loginUser.id());
        order.setSellerId(item.getSellerId());
        order.setItemId(itemId);
        order.setPrice(item.getPrice());
        order.setStatus("CREATED");
        orderMapper.insert(order);
        itemService.evictItemCachesAfterCommit();

        return order.getId();
    }

    @Transactional
    public void updateStatus(Long orderId, UpdateOrderStatusRequest request, LoginUser loginUser) {
        if (orderId == null) {
            throw new BusinessException("订单id不能为空");
        }

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        boolean isBuyer = order.getBuyerId().equals(loginUser.id());
        boolean isSeller = order.getSellerId().equals(loginUser.id());
        if (!isBuyer && !isSeller) {
            throw new BusinessException("无权操作该订单");
        }

        String targetStatus = request == null ? null : request.getStatus();
        OrderStatusPolicy.validateTransition(
                order.getStatus(), targetStatus, isBuyer, isSeller);

        int updated = orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .eq(Order::getId, order.getId())
                .eq(Order::getStatus, order.getStatus())
                .set(Order::getStatus, targetStatus));
        if (updated != 1) {
            throw new BusinessException("订单状态已发生变化，请刷新后重试");
        }

        if ("CANCELLED".equals(targetStatus)) {
            itemMapper.update(null, Wrappers.<Item>lambdaUpdate()
                    .eq(Item::getId, order.getItemId())
                    .eq(Item::getStatus, "SOLD")
                    .set(Item::getStatus, "ON_SALE"));
            itemService.evictItemCachesAfterCommit();
        }
    }

    public PageResult<OrderVO> myOrders(LoginUser loginUser, String role, long page, long size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new BusinessException("分页参数不正确");
        }

        String normalizedRole = role == null || role.isBlank() ? "buyer" : role;
        if (!"buyer".equals(normalizedRole) && !"seller".equals(normalizedRole)) {
            throw new BusinessException("role参数只能是buyer或seller");
        }

        LambdaQueryWrapper<Order> queryWrapper = Wrappers.lambdaQuery();
        if ("buyer".equals(normalizedRole)) {
            queryWrapper.eq(Order::getBuyerId, loginUser.id());
        } else {
            queryWrapper.eq(Order::getSellerId, loginUser.id());
        }
        queryWrapper.orderByDesc(Order::getCreateTime);

        Page<Order> orderPage = orderMapper.selectPage(new Page<>(page, size), queryWrapper);
        List<Order> records = orderPage.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), orderPage.getTotal(), page, size);
        }

        List<Long> itemIds = records.stream().map(Order::getItemId).distinct().collect(Collectors.toList());
        List<Long> userIds = new ArrayList<>();
        userIds.addAll(records.stream().map(Order::getBuyerId).distinct().collect(Collectors.toList()));
        userIds.addAll(records.stream().map(Order::getSellerId).distinct().collect(Collectors.toList()));

        Map<Long, Item> itemMap = itemMapper.selectList(
                        Wrappers.<Item>lambdaQuery().in(Item::getId, itemIds))
                .stream()
                .collect(Collectors.toMap(Item::getId, Function.identity(), (left, right) -> left));

        Map<Long, User> userMap = userMapper.selectList(
                        Wrappers.<User>lambdaQuery().in(User::getId, userIds))
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (left, right) -> left));

        List<OrderVO> orderVos = new ArrayList<>();
        for (Order order : records) {
            Item item = itemMap.get(order.getItemId());
            User buyer = userMap.get(order.getBuyerId());
            User seller = userMap.get(order.getSellerId());

            OrderVO vo = new OrderVO();
            vo.setId(order.getId());
            vo.setItemId(order.getItemId());
            vo.setItemTitle(item == null ? null : item.getTitle());
            vo.setImages(item == null ? Collections.emptyList() : parseImages(item.getImages()));
            vo.setPrice(order.getPrice());
            vo.setStatus(order.getStatus());
            vo.setBuyerId(order.getBuyerId());
            vo.setBuyerUsername(buyer == null ? null : buyer.getUsername());
            vo.setSellerId(order.getSellerId());
            vo.setSellerUsername(seller == null ? null : seller.getUsername());
            vo.setCreateTime(order.getCreateTime());
            vo.setUpdateTime(order.getUpdateTime());
            orderVos.add(vo);
        }

        return new PageResult<>(orderVos, orderPage.getTotal(), orderPage.getCurrent(), orderPage.getSize());
    }

    private List<String> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(imagesJson, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException ex) {
            return Collections.emptyList();
        }
    }
}
