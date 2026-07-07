package com.zx.student.archive.service.question;

import com.zx.common.exception.ServiceException;
import com.zx.student.archive.domain.bo.question.*;
import com.zx.student.archive.domain.dto.question.QuestionCategoryDTO;
import com.zx.student.archive.domain.dto.question.QuestionCategoryRefDTO;

import java.util.List;

/**
 * 题目分类服务接口
 */
public interface QuestionCategoryService {

    /**
     * 获取分类树
     *
     * @param queryBO 查询参数
     * @return 分类树列表
     */
    List<QuestionCategoryDTO> getCategoryTree(QuestionCategoryQueryBO queryBO);

    /**
     * 获取分类详情（包含子分类）
     *
     * @param idBO 分类ID
     * @return 分类详情
     */
    QuestionCategoryDTO getCategory(QuestionCategoryIdBO idBO);

    /**
     * 创建分类
     *
     * @param categoryBO 分类信息
     * @return 分类ID
     * @throws BusinessException 业务异常
     */
    Integer createCategory(QuestionCategoryBO categoryBO) throws ServiceException;

    /**
     * 更新分类
     *
     * @param categoryBO 分类信息
     * @throws ServiceException 业务异常
     */
    void updateCategory(QuestionCategoryBO categoryBO) throws ServiceException;

    /**
     * 批量删除分类
     *
     * @param idsBO 分类ID列表
     * @throws ServiceException 业务异常
     */
    void batchDeleteCategory(QuestionCategoryIdsBO idsBO) throws ServiceException;

    /**
     * 更新分类排序
     *
     * @param sortBO 排序信息
     * @throws ServiceException 业务异常
     */
    void updateSort(QuestionCategorySortBO sortBO) throws ServiceException;

    /**
     * 检查分类名称是否重复
     *
     * @param name      分类名称
     * @param parentId  父级ID
     * @param excludeId 排除的ID
     * @return true: 重复, false: 不重复
     */
    boolean checkNameExists(String name, Integer parentId, Integer excludeId);

    List<QuestionCategoryRefDTO> getBusinessRefs(QuestionCategoryIdQueryBO queryBO);
}