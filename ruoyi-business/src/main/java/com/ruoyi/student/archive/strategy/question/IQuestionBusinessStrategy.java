package com.ruoyi.student.archive.strategy.question;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.domain.bo.paper.*;
import com.ruoyi.student.archive.domain.dto.paper.PaperQuestionDTO;
import com.ruoyi.student.archive.domain.dto.paper.PaperQuestionResultDTO;
import com.ruoyi.student.archive.domain.dto.paper.UserPaperCheckSubmitDTO;
import com.ruoyi.student.archive.domain.dto.paper.UserPaperDTO;
import com.ruoyi.student.archive.domain.dto.question.QuestionCorrectAnswerDTO;

import java.util.List;

/**
 * 新的统一业务流程策略接口
 */
public interface IQuestionBusinessStrategy {

    /**
     * 获取本策略支持的业务类型
     */
    Integer getBusinessType();

    /**
     * 获取试卷题目列表
     */
    List<PaperQuestionDTO> getPaperQuestions(QueryPaperQuestionsBO queryBO) throws ServiceException;

    /**
     * 提交试卷
     */
    UserPaperDTO submitPaper(SubmitPaperBO submitBO) throws ServiceException;

    /**
     * 获取试卷结果
     */
    PaperQuestionResultDTO getPaperResult(QueryPaperResultBO queryBO) throws ServiceException;

    /**
     * 重考
     */
    void retakePaper(RetakePaperBO retakePaperBO) throws ServiceException;

    /**
     * 检查是否已提交
     */
    UserPaperCheckSubmitDTO checkSubmit(CheckSubmitBO serviceBO) throws ServiceException;

    /**
     * 获取试卷信息
     */
    UserPaperDTO getPaperInfo(Integer paperId) throws ServiceException;

    /**
     * 获取题目正确答案
     */
    List<QuestionCorrectAnswerDTO> getQuestionsCorrectAnswers(Integer businessId, Integer businessType, List<Integer> questionIds) throws ServiceException;

}