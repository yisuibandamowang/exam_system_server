package com.atguigu.exam.service.impl;


import com.atguigu.exam.entity.Category;
import com.atguigu.exam.mapper.CategoryMapper;
import com.atguigu.exam.mapper.QuestionMapper;
import com.atguigu.exam.service.CategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final CategoryMapper categoryMapper;

    private final QuestionMapper questionMapper;

    @Override
    public List<Category> findCategoryList() {
        // 1. 获取所有分类的基础信息，并按sort字段排序
        List<Category> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSort)
        );

        // 2. 为分类列表填充题目数量【进行子分类和count数量填充】
        fillQuestionCount(categories);

        return categories;
    }

    // 这是一个私有的辅助方法，用于填充题目数
    private void fillQuestionCount(List<Category> categories) {
        // 1. 一次性查询出所有分类的题目数量
        List<Map<Long, Object>> questionCountList = questionMapper.getCategoryQuestionCount();

        // 2. 将查询结果从 List<Map> 转换为 Map<categoryId, count>，便于快速查找
        Map<Long, Long> questionCountMap = questionCountList.stream().collect(Collectors.toMap(
                map -> Long.valueOf(map.get("category_id").toString()),
                map -> Long.valueOf(map.get("count").toString())
        ));

        // 3. 遍历分类列表，为每个分类设置其对应的题目数量
        categories.forEach(category -> {
            category.setCount(questionCountMap.getOrDefault(category.getId(),0L));
        });
    }
}