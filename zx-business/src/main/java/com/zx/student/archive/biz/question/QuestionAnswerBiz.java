package com.zx.student.archive.biz.question;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zx.common.exception.ServiceException;
import com.zx.student.archive.domain.question.QuestionAnswer;

import java.util.Collection;
import java.util.List;

public interface QuestionAnswerBiz extends IService<QuestionAnswer> {
    
    /**
     * 根据题目ID获取答案列表
     *
     * @param questionId 题目ID
     * @return 答案列表
     */
    List<QuestionAnswer> getAnswersByQuestionId(Integer questionId);

    /**
     * 根据答案ID列表获取答案列表（包括已删除的记录，用于数据一致性处理）
     *
     * @param answerIds 答案ID集合
     * @return 答案列表
     */
    List<QuestionAnswer> getAnswersByIds(Collection<Integer> answerIds);

    /**
     * 根据空位区域ID获取答案列表
     *
     * @param questionId
     * @param blankAreaId 空位区域ID
     * @return 答案列表
     * @throws BusinessException 业务异常
     */
    List<QuestionAnswer> getAnswersByBlankAreaId(Integer questionId,Integer blankAreaId) ;

    /**
     * 保存答案列表
     *
     * @param answers 答案列表
     * @return 是否保存成功
     */
    boolean saveAnswers(List<QuestionAnswer> answers) throws ServiceException;

    /**
     * 更新答案列表
     *
     * @param questionId 题目ID
     * @param answers 答案列表
     * @return 是否更新成功
     */
    boolean updateAnswers(Integer questionId, List<QuestionAnswer> answers) throws ServiceException;


    /**
     * 更新答案列表
     *
     * @param blankAreaId 题目ID
     * @param answers 答案列表
     * @return 是否更新成功
     */
    boolean updateBlankAnswers(Integer blankAreaId, List<QuestionAnswer> answers) throws ServiceException;


    /**
     * 根据题目ID删除答案
     *
     * @param questionIds 题目ID列表
     * @return 是否删除成功
     */
    boolean deleteByQuestionId(List<Integer> questionIds) throws ServiceException;

    /**
     * 根据areaID删除答案
     * @param areaIds
     * @return
     * @throws ServiceException
     */
    boolean deleteByBlankAreaId(List<Integer> areaIds) throws ServiceException;

} 