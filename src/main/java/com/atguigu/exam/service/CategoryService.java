package com.atguigu.exam.service;

import com.atguigu.exam.entity.Category;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CategoryService extends IService<Category> {

    /*
    查询分类列表和所有分类的题目数量
     */
    List<Category> findCategoryList();
}