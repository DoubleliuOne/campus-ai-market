package com.campusmarket.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campusmarket.entity.Category;
import com.campusmarket.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<Category> listAll() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .orderByAsc(Category::getId));
    }
}
