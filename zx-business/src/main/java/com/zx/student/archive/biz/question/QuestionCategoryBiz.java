package com.zx.student.archive.biz.question;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zx.common.exception.ServiceException;
import com.zx.student.archive.domain.bo.question.QuestionCategoryBO;
import com.zx.student.archive.domain.bo.question.QuestionCategoryQueryBO;
import com.zx.student.archive.domain.bo.question.QuestionCategorySortBO;
import com.zx.student.archive.domain.dto.question.QuestionCategoryDTO;
import com.zx.student.archive.domain.question.QuestionCategory;

import java.util.List;

/**
 * 题目分类业务接口
 */
public interface QuestionCategoryBiz extends IService<QuestionCategory> {

    /**
     * 获取分类树
     */
    List<QuestionCategoryDTO> getCategoryTree(QuestionCategoryQueryBO queryBO);

    /**
     * 获取分类及其所有子分类
     */
    List<QuestionCategoryDTO> getCategoryWithChildren(Integer categoryId);


    /**
     * 创建分类
     */
    Integer createCategory(QuestionCategoryBO categoryBO) throws ServiceException;

    /**
     * 更新分类
     */
    void updateCategory(QuestionCategoryBO categoryBO) throws ServiceException;

    /**
     * 更新分类排序
     *
     * @param sortBO 排序参数
     * @throws ServiceException 业务异常
     */
    void updateSort(QuestionCategorySortBO sortBO) throws ServiceException;

    /**
     * 统计分类下的题目数量（包含子分类）
     */
    Integer countQuestionsByCategoryId(Integer categoryId);

    Integer countCurrentQuestionsByCategoryId(Integer categoryId);


    /**
     * 检查分类名称是否重复
     */
    boolean checkNameExists(String name, Integer fatherId, Integer excludeId);

    /**
     * 检查是否存在子分类
     *
     * @param ids 分类ID列表
     * @return true 如果存在子分类
     */
    boolean hasChildren(List<Integer> ids);

}