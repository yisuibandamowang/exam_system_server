package com.atguigu.exam.service;

import com.atguigu.exam.entity.Category;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface CategoryService extends IService<Category> {

    /*
    查询分类列表和所有分类的题目数量
     */
    List<Category> findCategoryList();

    /**
     * 查询所有类别信息的树状集合，并按照sort字段排序
     * @return
     */
    List<Category> getCategoryTree();

    /**
     * 保存分类信息
     * @param category
     */
    void addCategory(Category category);
    /**
     * 修改分类信息
     * @param category
     */
    void updateCategory(Category category);

    /**
     * 删除分类信息
     * @param id
     */
    void deleteCategory(Long id);
}