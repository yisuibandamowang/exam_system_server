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

import java.util.ArrayList;
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

    @Override
    public List<Category> getCategoryTree() {
        // 1. 获取所有分类，并按sort排序
        List<Category> allCategories = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .orderByAsc(Category::getSort)
        );

        // 2. 为每个分类填充其自身的题目数量
        fillQuestionCount(allCategories);
        // 3. 构建树形结构并返回
        List<Category> buildTree = buildTree(allCategories);
        log.info("查询类别树状结构集合：{}",buildTree);
        return buildTree;
    }

    // 构建树形结构的私有辅助方法
    private List<Category> buildTree(List<Category> categories) {
        // 1. 使用Stream API按parentId进行分组，得到 Map<parentId, List<children>>
        Map<Long, List<Category>> childrenMap = categories.stream()
                .collect(Collectors.groupingBy(Category::getParentId));

        /*
            stream()：把 List<Category> 转成 Stream 流，开启流式操作。
            Collectors.groupingBy：按指定规则分组，这里用方法引用 Category::getParentId ，提取分类的 parentId 作为分组 key，value 是对应 parentId 的分类列表，快速构建 父 ID - 子分类列表 映射。
         */

        // 2. 遍历所有分类，为它们设置children属性，并递归地累加题目数量
        categories.forEach(category -> {
            // 从Map中找到当前分类的所有子分类
            List<Category> children = childrenMap.getOrDefault(category.getId(), new ArrayList<>());
            category.setChildren(children);

            // 汇总子分类的题目数量到父分类
            long childrenQuestionCount = children.stream()
                    .mapToLong(c -> c.getCount() != null ? c.getCount() : 0L)
                    .sum();
            /*
                forEach：遍历每个分类，对单个分类做处理，类似增强 for 循环，但结合 Stream 更灵活。
                getOrDefault：从分组好的 childrenMap 取当前分类的子分类，无对应值时给默认空列表，避免空指针。
                嵌套 stream().mapToLong().sum()：先转成 LongStream ，通过 mapToLong 处理 count （空值转 0 ），再用 sum 汇总子分类题目数，结合自身题目数，设置到当前分类，完成递归汇总逻辑。
             */

            long selfQuestionCount = category.getCount() != null ? category.getCount() : 0L;
            // 父分类的总数 = 自身的题目数 + 所有子分类的题目数总和
            category.setCount(selfQuestionCount + childrenQuestionCount);
        });

        // 3. 最后，筛选出所有顶级分类（parentId为0），它们是树的根节点
        /*
            filter：按条件（parentId == 0 ）过滤分类，只保留顶级分类。
            collect(Collectors.toList())：把过滤后的 Stream 流转为 List ，作为分类树的根节点集合返回。
         */
        return categories.stream()
                .filter(c -> c.getParentId() == 0)
                .collect(Collectors.toList());
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