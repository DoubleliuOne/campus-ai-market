package com.campusmarket.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campusmarket.entity.Category;
import com.campusmarket.entity.Item;
import com.campusmarket.entity.User;
import com.campusmarket.exception.BusinessException;
import com.campusmarket.mapper.CategoryMapper;
import com.campusmarket.mapper.ItemMapper;
import com.campusmarket.mapper.UserMapper;
import com.campusmarket.security.LoginUser;
import com.campusmarket.vo.ItemVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    private static final String SEARCH_VERSION_KEY = "cache:item:search:v1:version";

    @Mock
    private ItemMapper itemMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemService(
                itemMapper, categoryMapper, userMapper, objectMapper, redisTemplate);
    }

    @Test
    void ownerCanReadSoldItemButOtherUsersCannot() {
        Item item = soldItem(5L, 1L);
        User seller = new User();
        seller.setId(1L);
        seller.setUsername("bob");
        Category category = new Category();
        category.setId(1L);
        category.setName("数码产品");

        when(itemMapper.selectById(5L)).thenReturn(item);
        when(userMapper.selectList(any())).thenReturn(List.of(seller));
        when(categoryMapper.selectList(any())).thenReturn(List.of(category));

        ItemVO result = itemService.getDetailForUser(5L, new LoginUser(1L, "bob"));

        assertEquals("SOLD", result.getStatus());
        assertThrows(BusinessException.class,
                () -> itemService.getDetailForUser(5L, new LoginUser(2L, "buyer1")));
    }

    @Test
    void firstWriteChangesSearchCacheVersionFromZeroToOne() throws Exception {
        AtomicReference<String> version = new AtomicReference<>();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(SEARCH_VERSION_KEY))
                .thenAnswer(invocation -> version.get());
        when(valueOperations.setIfAbsent(eq(SEARCH_VERSION_KEY), eq("0")))
                .thenAnswer(invocation -> {
                    if (version.get() == null) {
                        version.set("0");
                        return true;
                    }
                    return false;
                });
        when(valueOperations.increment(SEARCH_VERSION_KEY)).thenAnswer(invocation -> {
            long next = version.get() == null ? 1L : Long.parseLong(version.get()) + 1L;
            version.set(Long.toString(next));
            return next;
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(itemMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<Item> page = invocation.getArgument(0);
            page.setRecords(Collections.emptyList());
            page.setTotal(0);
            return page;
        });

        itemService.search(null, null, null, null, "latest", 1, 5);
        itemService.evictItemCaches();
        itemService.search(null, null, null, null, "latest", 1, 5);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations, times(2))
                .set(keyCaptor.capture(), anyString(), anyLong(), eq(TimeUnit.SECONDS));
        List<String> keys = keyCaptor.getAllValues();
        assertNotEquals(keys.get(0), keys.get(1));
        org.junit.jupiter.api.Assertions.assertTrue(keys.get(1)
                .startsWith("cache:item:search:v1:1:"));
    }

    private Item soldItem(Long itemId, Long sellerId) {
        Item item = new Item();
        item.setId(itemId);
        item.setSellerId(sellerId);
        item.setCategoryId(1L);
        item.setTitle("小米14");
        item.setDescription("九成新");
        item.setPrice(new BigDecimal("2000.00"));
        item.setStatus("SOLD");
        return item;
    }
}
