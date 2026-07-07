package com.zx.student.archive.strategy.question.impl;


import com.zx.common.enums.BusinessTypeEnum;
import com.zx.common.enums.question.WrongQuestionSourceTypeEnum;
import com.zx.common.exception.ServiceException;
import com.zx.student.archive.biz.question.QuestionBiz;
import com.zx.student.archive.domain.bo.paper.QueryPaperQuestionsBO;
import com.zx.student.archive.domain.bo.paper.RetakePaperBO;
import com.zx.student.archive.domain.bo.paper.SubmitPaperBO;
import com.zx.student.archive.domain.dto.paper.PaperQuestionDTO;
import com.zx.student.archive.domain.dto.question.QuestionCorrectAnswerDTO;
import com.zx.student.archive.domain.question.Question;
import com.zx.student.archive.strategy.question.AbstractBusinessProcessStrategy;
import com.zx.student.archive.strategy.question.manager.PaperResultConverter;
import com.zx.student.archive.strategy.question.manager.QuestionLoaderService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.zx.common.constant.AppErrorCode.*;


@Component
public class DefaultProcessStrategy extends AbstractBusinessProcessStrategy {

    @Resource
    private QuestionLoaderService questionLoaderService;
    @Resource
    private PaperResultConverter paperResultConverter;
    @Resource
    private QuestionBiz questionBiz;
    @Override
    public Integer getBusinessType() {
        return BusinessTypeEnum.DEFAULT_BUSINESS.getCode();
    }

    @Override
    public List<PaperQuestionDTO> getPaperQuestions(QueryPaperQuestionsBO queryBO) throws ServiceException {
        validateQueryParams(queryBO);
        List<Question> questions = questionLoaderService.loadForQuestionSettings(queryBO);
        Map<Integer, Integer> questionOrderMap = new LinkedHashMap<>();
        int order = 1;
        for (Question question : questions) {
            questionOrderMap.put(question.getId(), order++);
        }
        return paperResultConverter.toPaperQuestionDTOs(questions, questionOrderMap);
    }
    
    @Override
    protected String getPaperDefaultName(SubmitPaperBO submitBO) {
        return "图书答题-默认试卷名称";
    }

    @Override
    protected Integer getWrongQuestionSourceType() {
        return WrongQuestionSourceTypeEnum.USER_PRACTICE.getCode();
    }

    @Override
    public void retakePaper(RetakePaperBO retakePaperBO) throws ServiceException {
        paperLifecycleManager.retake(retakePaperBO);
    }

    @Override
    public List<QuestionCorrectAnswerDTO> getQuestionsCorrectAnswers(Integer businessId, Integer businessType, List<Integer> questionIds) throws ServiceException {
        List<Question> questions;
        if (CollectionUtils.isNotEmpty(questionIds)) {
            questions = questionBiz.listByIds(questionIds);
        } else {
            if (businessId == null) {
                throw new ServiceException(APP_BUSINESS_ID_NOT_NULL_MSG);
            }
            if (businessType == null) {
                throw new ServiceException(APP_BUSINESS_TYPE_NOT_NULL_MSG);
            }
            QueryPaperQuestionsBO questionsBO = new QueryPaperQuestionsBO();
            questionsBO.setBusinessType(businessType);
            questionsBO.setBusinessId(businessId);
            questions = questionLoaderService.loadForQuestionSettings(questionsBO);
        }
        if (CollectionUtils.isEmpty(questions)) {
            return Collections.emptyList();
        }
        return paperResultConverter.convertToCorrectAnswerDTOs(questions);
    }
}