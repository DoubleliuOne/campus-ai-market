package com.campusmarket.controller;

import com.campusmarket.common.ApiResponse;
import com.campusmarket.entity.Category;
import com.campusmarket.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "分类", description = "商品分类查询")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "查询全部分类")
    public ApiResponse<List<Category>> list() {
        return ApiResponse.ok(categoryService.listAll());
    }
}
