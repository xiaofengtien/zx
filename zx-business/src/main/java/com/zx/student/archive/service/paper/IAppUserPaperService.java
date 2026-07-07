package com.zx.student.archive.service.paper;

import com.zx.common.exception.ServiceException;
import com.zx.student.archive.domain.bo.paper.*;
import com.zx.student.archive.domain.bo.question.QueryQuestionsCorrectAnswerBO;
import com.zx.student.archive.domain.dto.paper.*;
import com.zx.student.archive.domain.dto.question.QuestionCorrectAnswerDTO;

import java.util.List;

/**
 * 用户试卷服务接口
 */
public interface IAppUserPaperService {

    /**
     * 获取试卷题目列表
     *
     * @param queryBO 查询参数
     * @return 题目列表
     */
    List<PaperQuestionDTO> getPaperQuestions(QueryPaperQuestionsBO queryBO) throws ServiceException;
    
    /**
     * 提交试卷
     *
     * @param submitBO 提交试卷参数
     * @return 试卷信息
     * @throws ServiceException 业务异常
     */
    UserPaperDTO submitPaper(SubmitPaperBO submitBO) throws ServiceException;
    
    /**
     * 获取题目作答结果列表
     *
     * @param queryBO 查询题目结果参数
     * @return 题目结果列表
     * @throws ServiceException 业务异常
     */
    PaperQuestionResultDTO getPaperQuestionResult(QueryPaperResultBO queryBO) throws ServiceException;
    
    /**
     * 重新考试（删除原有答题记录，重新获取试卷）
     *
     * @param retakePaperBO 重新考试参数
     * @throws ServiceException 业务异常
     */
    void retakePaper(RetakePaperBO retakePaperBO) throws ServiceException;

    /**
     * 获取所有题目的正确答案信息
     *
     * @param bo 查询参数
     * @return 正确答案DTO列表
     * @throws ServiceException 业务异常
     */
    List<QuestionCorrectAnswerDTO> getAllQuestionsCorrectAnswers(QueryQuestionsCorrectAnswerBO bo) throws ServiceException;

    /**
     * 检查提交状态
     * @param serviceBO 检查参数
     * @return 提交状态信息
     * @throws ServiceException 业务异常
     */
    UserPaperCheckSubmitDTO checkSubmit(CheckSubmitBO serviceBO) throws ServiceException;
}