package com.ruoyi.student.archive.mapper.question;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.student.archive.domain.question.QuestionCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import com.ruoyi.student.archive.domain.dto.question.CategoryParentRelationDTO;

/**
 * 题目分类 Mapper
 */
@Mapper
public interface QuestionCategoryMapper extends BaseMapper<QuestionCategory> {
    
    /**
     * 统计分类下的题目数量（包含子分类）
     */
    Integer countQuestionsByCategoryId(@Param("categoryId") Integer categoryId);

    /**
     * 获取同级分类中的最大排序号
     *
     * @param fatherId 父级ID
     * @return 最大排序号
     */
    Integer selectMaxSortNum(@Param("fatherId") Integer fatherId);
    
    /**
     * 递归查询所有子分类ID（使用MySQL递归CTE）
     *
     * @param categoryId 分类ID
     * @return 所有子分类ID列表（包含自身）
     */
    List<Integer> getChildCategoryIds(@Param("categoryId") Integer categoryId);
    
    /**
     * 批量查询所有分类的父子关系（用于在内存中构建树和计算子节点数）
     *
     * @return 分类父子关系列表
     */
    List<CategoryParentRelationDTO> getAllCategoryParentRelations();
} 