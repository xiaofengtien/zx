//package com.ruoyi.student.archive.strategy.question.impl;
//
//
//import com.ruoyi.common.constant.AppErrorCode;
//import com.ruoyi.common.enums.BusinessTypeEnum;
//import com.ruoyi.student.archive.domain.bo.paper.QueryPaperQuestionsBO;
//import com.ruoyi.student.archive.domain.bo.paper.RetakePaperBO;
//import com.ruoyi.student.archive.domain.bo.paper.SubmitPaperBO;
//import com.ruoyi.student.archive.domain.dto.paper.PaperQuestionDTO;
//import com.ruoyi.student.archive.domain.dto.question.QuestionCorrectAnswerDTO;
//import com.ruoyi.common.enums.question.WrongQuestionSourceTypeEnum;
//import com.ruoyi.student.archive.biz.question.QuestionBiz;
//import com.ruoyi.student.archive.domain.question.Question;
//import com.ruoyi.student.archive.strategy.question.AbstractBusinessProcessStrategy;
//import com.ruoyi.student.archive.strategy.question.manager.PaperResultConverter;
//import com.ruoyi.student.archive.strategy.question.manager.QuestionLoaderService;
//import com.ruoyi.common.exception.ServiceException;
//import org.apache.commons.collections4.CollectionUtils;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.Resource;
//import java.util.Collections;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//
//import static com.ruoyi.common.constant.AppErrorCode.*;
//
//@Component
//public class PointReadingBookProcessStrategy extends AbstractBusinessProcessStrategy {
//
//    @Resource
//    private QuestionLoaderService questionLoaderService;
//    @Resource
//    private PaperResultConverter paperResultConverter;
//    @Resource
//    private QuestionBiz questionBiz;
//    @Override
//    public Integer getBusinessType() {
//        return BusinessTypeEnum.POINT_READING_BUSINESS.getCode();
//    }
//
//    @Override
//    public List<PaperQuestionDTO> getPaperQuestions(QueryPaperQuestionsBO queryBO) throws ServiceException {
//        validateQueryParams(queryBO);
//        List<Question> questions = questionLoaderService.loadForQuestionSettings(queryBO);
//        Map<Integer, Integer> questionOrderMap = new LinkedHashMap<>();
//        int order = 1;
//        for (Question question : questions) {
//            questionOrderMap.put(question.getId(), order++);
//        }
//        return paperResultConverter.toPaperQuestionDTOs(questions, questionOrderMap);
//    }
//
//    @Override
//    protected String getPaperDefaultName(SubmitPaperBO submitBO) {
//        return "学习中心-默认试卷名称";
//    }
//
//    @Override
//    protected Integer getWrongQuestionSourceType() {
//        return WrongQuestionSourceTypeEnum.STUDY_CENTER.getCode();
//    }
//
//    @Override
//    public List<QuestionCorrectAnswerDTO> getQuestionsCorrectAnswers(Integer businessId, Integer businessType, List<Integer> questionIds) throws ServiceException {
//        List<Question> questions;
//        if (CollectionUtils.isNotEmpty(questionIds)) {
//            questions = questionBiz.listByIds(questionIds);
//        } else {
//            if (businessId == null) {
//                throw new ServiceException(AppErrorCode.APP_BUSINESS_ID_NOT_NULL_MSG);
//            }
//            if (businessType == null) {
//                throw new ServiceException(AppErrorCode.APP_BUSINESS_TYPE_NOT_NULL_MSG);
//            }
//            QueryPaperQuestionsBO questionsBO = new QueryPaperQuestionsBO();
//            questionsBO.setBusinessType(businessType);
//            questionsBO.setBusinessId(businessId);
//            questions = questionLoaderService.loadForQuestionSettings(questionsBO);
//        }
//        if (CollectionUtils.isEmpty(questions)) {
//            return Collections.emptyList();
//        }
//        return paperResultConverter.convertToCorrectAnswerDTOs(questions);
//    }
//
//    @Override
//    public void retakePaper(RetakePaperBO retakePaperBO) throws ServiceException {
//        paperLifecycleManager.retake(retakePaperBO);
//    }
//}