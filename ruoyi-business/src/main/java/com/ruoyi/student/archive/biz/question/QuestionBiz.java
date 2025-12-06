package com.ruoyi.student.archive.biz.question;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.domain.dto.question.QuestionCountDTO;
import com.ruoyi.student.archive.domain.question.Question;
import com.ruoyi.student.archive.domain.question.QuestionAnswer;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 题目基础操作接口
 */
public interface QuestionBiz extends IService<Question> {
    
    /**
     * 根据ID获取题目
     *
     * @param id 题目ID
     * @return 题目实体
     */
    Question getQuestionById(Integer id);



    List<QuestionCountDTO> countByCategoryIds(List<Integer> categoryIds);

    /**
     * 根据分类ID获取题目列表
     *
     * @param categoryId 分类ID
     * @return 题目列表
     */
    List<Question> getQuestionListByCategoryId(Integer categoryId);

    /**
     * 创建题目
     *
     * @param question 题目实体
     * @return 新创建的题目ID
     */
    Integer createQuestion(Question question);

    /**
     * 更新题目
     *
     * @param question 题目实体
     */
    void updateQuestion(Question question);

    /**
     * 批量逻辑删除题目
     *
     * @param ids 题目ID列表
     */
    void batchDeleteQuestion(List<Integer> ids);
    /**
     * 批量更新题目分类
     *
     * @param questionIds 题目ID列表
     * @param targetCategoryId 目标分类ID
     */
    void batchUpdateQuestionCategory(List<Integer> questionIds, Integer targetCategoryId);

    Page<Question> pageList(Page<Question> page, Question condition) ;

    /**
     * 检查分类是否有关联的试题
     *
     * @param categoryIds 分类ID列表
     * @return true 如果存在关联的试题
     */
    boolean hasCategoryQuestions(List<Integer> categoryIds);

    /**
     * 根据分类ID获取题目列表
     * @param categoryId
     * @return
     */
    List<Question> getAllQuestionsByCategory(Integer categoryId);

    /**
     * 根据分类ID获取题目列表
     * @param categoryId
     * @param questionNum
     * @return
     * @throws BusinessException
     */
    List<Question> getQuestionsByCategory(Integer categoryId, Integer questionNum) throws ServiceException;

    /**
     * 获取空位区域选项Map
     */
    /**
     * 获取空位区域选项Map，按questionId和blankAreaId两级分组
     */
    Map<Integer, Map<Integer, List<QuestionAnswer>>> getBlankAreaAnswerMap(Set<Integer> questionIds);

}