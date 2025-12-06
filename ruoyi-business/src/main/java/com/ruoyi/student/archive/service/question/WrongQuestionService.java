package com.ruoyi.student.archive.service.question;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.domain.bo.question.AddWrongQuestionBO;
import com.ruoyi.student.archive.domain.bo.question.QueryWrongQuestionsBO;
import com.ruoyi.student.archive.domain.bo.question.WrongQuestionPkIdsBO;
import com.ruoyi.student.archive.domain.dto.question.QuestionCorrectAnswerDTO;
import com.ruoyi.student.archive.domain.dto.question.WrongQuestionResultDTO;
import com.ruoyi.student.archive.domain.dto.question.WrongQuestionSubjectDTO;

import java.util.List;
import java.util.Set;

/**
 * 用户错题本服务接口
 */
public interface WrongQuestionService {

    /**
     * 添加错题（统一接口，根据题目类型区分普通题和完形填空）
     *
     * @param addWrongQuestionBO 添加错题参数
     * @return 是否成功
     * @throws BusinessException 业务异常
     */
    boolean addWrongQuestion(AddWrongQuestionBO addWrongQuestionBO) throws ServiceException;

    /**
     * 批量添加错题（统一接口，根据题目类型区分普通题和完形填空）
     *
     * @param addWrongQuestionBOList 添加错题参数列表
     * @return 是否成功
     * @throws ServiceException 业务异常
     */
    boolean batchAddWrongQuestions(List<AddWrongQuestionBO> addWrongQuestionBOList) throws ServiceException;
    /**
     * 删除错题
     *
     * @param removeBO 删除错题参数
     * @return 是否成功
     * @throws ServiceException 业务异常
     */
    boolean removeWrongQuestion(WrongQuestionPkIdsBO removeBO) throws ServiceException;

    /**
     * 查询用户错题列表（统一接口，返回普通题和完形填空题）
     *
     * @param queryBO 查询参数
     * @return 错题列表（包含普通题和完形填空题）
     * @throws ServiceException 业务异常
     */
    List<WrongQuestionResultDTO> listWrongQuestions(QueryWrongQuestionsBO queryBO) throws ServiceException;

    /**
     * 查询错题本中题目的正确答案
     *
     * @param appUserId 用户ID
     * @param questionIds 题目ID集合，为空则查询全部
     * @return 正确答案列表
     * @throws ServiceException 业务异常
     */
    List<QuestionCorrectAnswerDTO> getWrongQuestionsCorrectAnswers(Integer appUserId, Set<Integer> questionIds)
            throws ServiceException;

    /**
     * 查询错题本中题目的学科
     * @param appUserId
     * @return
     */
    List<WrongQuestionSubjectDTO> listSubject(Integer appUserId);
}