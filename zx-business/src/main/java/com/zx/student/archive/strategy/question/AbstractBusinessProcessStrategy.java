package com.zx.student.archive.strategy.question;

import com.zx.common.constant.AppErrorCode;
import com.zx.common.exception.ServiceException;
import com.zx.student.archive.domain.bo.paper.CheckSubmitBO;
import com.zx.student.archive.domain.bo.paper.QueryPaperQuestionsBO;
import com.zx.student.archive.domain.bo.paper.QueryPaperResultBO;
import com.zx.student.archive.domain.bo.paper.SubmitPaperBO;
import com.zx.student.archive.domain.dto.paper.PaperQuestionResultDTO;
import com.zx.student.archive.domain.dto.paper.UserPaperCheckSubmitDTO;
import com.zx.student.archive.domain.dto.paper.UserPaperDTO;
import com.zx.student.archive.domain.question.Question;
import com.zx.student.archive.strategy.question.manager.PaperLifecycleManager;
import com.zx.student.archive.strategy.question.manager.PaperResultConverter;
import com.zx.student.archive.strategy.question.manager.QuestionScoringService;
import com.zx.student.archive.strategy.question.manager.WrongQuestionRecorder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 新的、轻量级的抽象基类
 * 通过组合注入通用能力组件
 */
@Slf4j
public abstract class AbstractBusinessProcessStrategy implements IQuestionBusinessStrategy {

    @Resource
    protected PaperLifecycleManager paperLifecycleManager;
    @Resource
    protected QuestionScoringService questionScoringService;
    @Resource
    protected PaperResultConverter paperResultConverter;
    @Resource
    protected WrongQuestionRecorder wrongQuestionRecorder;

    /**
     * 提交试卷的模板方法，定义了通用的提交流程
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserPaperDTO submitPaper(SubmitPaperBO submitBO) throws ServiceException {

        Map<Integer, Question> questionMap = paperLifecycleManager.loadQuestionsForSubmission(submitBO);
        
        questionScoringService.score(submitBO, questionMap);
        
        UserPaperDTO paperInfo = paperLifecycleManager.submit(submitBO, getPaperDefaultName(submitBO));

        paperLifecycleManager.saveQuestionDetails(submitBO, paperInfo.getId());

        wrongQuestionRecorder.record(submitBO, submitBO.getBusinessId(), getWrongQuestionSourceType());

        afterSubmit(submitBO, paperInfo.getId());
        
        return paperInfo;
    }
    
    @Override
    public PaperQuestionResultDTO getPaperResult(QueryPaperResultBO queryBO) throws ServiceException {
        return paperResultConverter.getPaperResult(queryBO);
    }

    @Override
    public UserPaperCheckSubmitDTO checkSubmit(CheckSubmitBO serviceBO) throws ServiceException {
        return paperLifecycleManager.checkSubmit(serviceBO);
    }

    @Override
    public UserPaperDTO getPaperInfo(Integer paperId) throws ServiceException {
        return paperLifecycleManager.getPaperInfo(paperId);
    }



    /**
     * 获取默认试卷名称，由子类提供
     */
    protected abstract String getPaperDefaultName(SubmitPaperBO submitBO) throws ServiceException;

    /**
     *  获取错题来源类型，由子类提供
     */
    protected abstract Integer getWrongQuestionSourceType();

    /**
     * 提交后的特定业务处理，默认为空实现
     */
    protected void afterSubmit(SubmitPaperBO submitBO, Integer paperId) throws ServiceException {

    }

    protected final void validateQueryParams(QueryPaperQuestionsBO queryBO) throws ServiceException {
        if (queryBO == null) {
            throw new ServiceException(AppErrorCode.APP_QUERY_PARAM_NOT_NULL_MSG);
        }
        if (queryBO.getBusinessId() == null) {
            throw new ServiceException(AppErrorCode.APP_BUSINESS_ID_NOT_NULL_MSG);
        }
        if (queryBO.getBusinessType() == null) {
            throw new ServiceException(AppErrorCode.APP_BUSINESS_TYPE_NOT_NULL_MSG);
        }
    }

}