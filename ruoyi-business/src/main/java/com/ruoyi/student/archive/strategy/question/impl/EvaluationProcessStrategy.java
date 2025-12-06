//package com.ruoyi.student.archive.strategy.question.impl;
//
//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import com.ruoyi.student.archive.domain.bo.user.UserIdApiBO;
//import com.ruoyi.student.archive.domain.dto.user.CurrentUserApiDTO;
//import com.ruoyi.common.enums.BusinessTypeEnum;
//import com.ruoyi.student.archive.domain.bo.evaluation.app.AppUserPaperEvaluationSaveBO;
//import com.ruoyi.student.archive.domain.bo.paper.*;
//import com.ruoyi.common.constant.AppErrorCode;
//import com.ruoyi.student.archive.domain.dto.evaluation.app.AppUserPaperEvaluationSaveDTO;
//import com.ruoyi.student.archive.domain.dto.paper.PaperQuestionDTO;
//import com.ruoyi.student.archive.domain.dto.question.QuestionCorrectAnswerDTO;
//import com.ruoyi.common.enums.question.WrongQuestionSourceTypeEnum;
//import com.ruoyi.student.archive.biz.evaluation.IAppEvaluationBiz;
//import com.ruoyi.student.archive.biz.evaluation.IAppEvaluationNodeBiz;
//import com.ruoyi.student.archive.biz.evaluation.IAppUserPaperEvaluationAnalysisBiz;
//import com.ruoyi.student.archive.biz.evaluation.IAppUserPaperEvaluationBiz;
//import com.ruoyi.student.archive.biz.question.QuestionBiz;
//import com.ruoyi.student.archive.biz.question.QuestionBusinessRefBiz;
//import com.ruoyi.student.archive.domain.evaluation.AppEvaluation;
//import com.ruoyi.student.archive.domain.evaluation.AppEvaluationNode;
//import com.ruoyi.student.archive.domain.evaluation.AppUserPaperEvaluation;
//import com.ruoyi.student.archive.domain.evaluation.AppUserPaperEvaluationAnalysis;
//import com.ruoyi.student.archive.domain.question.Question;
//import com.ruoyi.student.archive.domain.question.QuestionBusinessRef;
//import com.ruoyi.student.archive.service.remote.SysAppUserRemoteService;
//import com.ruoyi.student.archive.strategy.question.AbstractBusinessProcessStrategy;
//import com.ruoyi.student.archive.strategy.question.manager.PaperResultConverter;
//import com.ruoyi.student.archive.strategy.question.manager.QuestionLoaderService;
//import com.ruoyi.common.exception.ServiceException;
//import jakarta.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.collections4.CollectionUtils;
//import org.springframework.stereotype.Component;
//import org.springframework.util.ObjectUtils;
//
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
//import static com.ruoyi.common.constant.AppErrorCode.*;
//import static com.ruoyi.common.enums.evaluation.PaperEnvaluationStatusEnum.EVALUATION_GENERATED_YES;
//import static com.ruoyi.common.enums.question.QuestionAnswerResultEnum.ANSWER_CORRECT;
//import static com.ruoyi.common.enums.question.QuestionAnswerResultEnum.ANSWER_WRONG;
//
//@Component
//@Slf4j
//public class EvaluationProcessStrategy extends AbstractBusinessProcessStrategy {
//    // TODO: 待实现 - SysAppUserRemoteService需要后续实现
//    // @Resource
//    // private SysAppUserRemoteService sysAppUserRemoteService;
//    @Resource
//    private QuestionLoaderService questionLoaderService;
//    @Resource
//    private PaperResultConverter paperResultConverter;
//    @Resource
//    private IAppEvaluationNodeBiz appEvaluationNodeBiz;
//    @Resource
//    private IAppUserPaperEvaluationBiz appUserPaperEvaluationBiz;
//    @Resource
//    private IAppUserPaperEvaluationAnalysisBiz appUserPaperEvaluationAnalysisBiz;
//    @Resource
//    private QuestionBusinessRefBiz questionBusinessRefBiz;
//    @Resource
//    protected QuestionBiz questionBiz;
//    @Resource
//    private IAppEvaluationBiz appEvaluationBiz;
//    @Override
//    public Integer getBusinessType() {
//        return BusinessTypeEnum.EVALUATION_BUSINESS.getCode();
//    }
//
//    @Override
//    public List<PaperQuestionDTO> getPaperQuestions(QueryPaperQuestionsBO queryBO) throws ServiceException {
//        validateQueryParams(queryBO);
//        List<QuestionBusinessRef> questionBusinessRefList = questionBusinessRefBiz.lambdaQuery()
//                .orderByDesc(QuestionBusinessRef::getOrderNum)
//                .orderByAsc(QuestionBusinessRef::getId)
//                .eq(QuestionBusinessRef::getBusinessId, queryBO.getBusinessId())
//                .eq(QuestionBusinessRef::getBusinessType, queryBO.getBusinessType()).list();
//        if (CollectionUtils.isEmpty(questionBusinessRefList)){
//            throw new ServiceException(APP_QUESTION_SETTING_NOT_EXIST_MSG);
//        }
//
//        List<Question> questions = questionLoaderService.loadForEvaluation(queryBO,questionBusinessRefList);
//
//        Set<Integer> questionIdSet = questionBusinessRefList.stream().map(QuestionBusinessRef::getQuestionId).collect(Collectors.toSet());
//        Map<Integer, Integer> questionOrderMap = new HashMap<>(questionIdSet.size());
//        for (int i = 0; i < questionBusinessRefList.size(); i++) {
//            questionOrderMap.put(questionBusinessRefList.get(i).getQuestionId(), i);
//        }
//        Map<Integer, Question> questionMap = questions.stream().collect(Collectors.toMap(Question::getId, Function.identity()));
//
//        // 按原始关联顺序构建结果集
//        List<Question> sortQuestion = questionBusinessRefList.stream()
//                .map(ref -> questionMap.get(ref.getQuestionId()))
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//
//        return paperResultConverter.toPaperQuestionDTOs(sortQuestion, questionOrderMap);
//    }
//
//    @Override
//    protected String getPaperDefaultName(SubmitPaperBO submitBO) throws ServiceException {
//        AppEvaluationNode evaluationNode = appEvaluationNodeBiz.getById(submitBO.getBusinessId());
//        if (ObjectUtils.isEmpty(evaluationNode)){
//            throw new ServiceException(APP_EVALUATION_NODE_SORT_FAIL_MSG);
//        }
//        if (submitBO.getAppEvaluationId() == null) {
//            throw new ServiceException(APP_EVALUATION_NOT_EXIST_MSG);
//        }
//        AppEvaluation evaluation = appEvaluationBiz.getById(submitBO.getAppEvaluationId());
//        if (ObjectUtils.isEmpty(evaluation)){
//            throw new ServiceException(APP_EVALUATION_NOT_EXIST_MSG);
//        }
//        return evaluationNode.getAppEvaluationNodeName();
//    }
//
//    @Override
//    protected Integer getWrongQuestionSourceType() {
//        return WrongQuestionSourceTypeEnum.USER_EVALUATION.getCode();
//    }
//
//    @Override
//    protected void afterSubmit(SubmitPaperBO submitBO, Integer paperId) throws ServiceException {
//        saveEvaluation(submitBO,paperId);
//    }
//
//    @Override
//    public List<QuestionCorrectAnswerDTO> getQuestionsCorrectAnswers(Integer businessId, Integer businessType, List<Integer> questionIds) throws ServiceException {
//        if (businessId== null) {
//            throw new ServiceException(APP_BUSINESS_ID_NOT_NULL_MSG);
//        }
//        if (businessType == null) {
//            throw new ServiceException(APP_BUSINESS_TYPE_NOT_NULL_MSG);
//        }
//        List<Question> questions;
//        if (CollectionUtils.isNotEmpty(questionIds)) {
//            questions = questionBiz.listByIds(questionIds);
//        } else {
//            QueryPaperQuestionsBO questionsBO = new QueryPaperQuestionsBO();
//            questionsBO.setBusinessType(businessType);
//            questionsBO.setBusinessId(businessId);
//            List<QuestionBusinessRef> questionBusinessRefList = questionBusinessRefBiz.lambdaQuery()
//                    .orderByDesc(QuestionBusinessRef::getOrderNum)
//                    .orderByAsc(QuestionBusinessRef::getId)
//                    .eq(QuestionBusinessRef::getBusinessId,questionsBO.getBusinessId())
//                    .eq(QuestionBusinessRef::getBusinessType,questionsBO.getBusinessType()).list();
//            if (CollectionUtils.isEmpty(questionBusinessRefList)){
//                throw new ServiceException(APP_QUESTION_SETTING_NOT_EXIST_MSG);
//            }
//            questions = questionLoaderService.loadForEvaluation(questionsBO,questionBusinessRefList);
//        }
//        if (CollectionUtils.isEmpty(questions)) {
//            return Collections.emptyList();
//        }
//        return paperResultConverter.convertToCorrectAnswerDTOs(questions);
//    }
//    @Override
//    public void retakePaper(RetakePaperBO retakePaperBO) throws ServiceException {
//        try {
//            List<AppUserPaperEvaluationAnalysis> list = appUserPaperEvaluationAnalysisBiz.lambdaQuery().eq(AppUserPaperEvaluationAnalysis::getAppUserPaperInfoId, retakePaperBO.getPaperId()).orderByDesc(AppUserPaperEvaluationAnalysis::getCreateTime).list();
//            if (!org.springframework.util.CollectionUtils.isEmpty(list)) {
//                AppUserPaperEvaluation appUserPaperEvaluation = appUserPaperEvaluationBiz.getById(list.get(0).getAppUserPaperEvaluationId());
//                if (!ObjectUtils.isEmpty(appUserPaperEvaluation) && appUserPaperEvaluation.getIsGenerated().equals(EVALUATION_GENERATED_YES.getCode())) {
//                    throw new ServiceException(APP_EVALUATION_GENERATED_NOT_RETAKE_MSG);
//                }
//            }
//            // 先调用父类的重考方法
//            paperLifecycleManager.retake(retakePaperBO);
//
//            // 然后删除评估分析记录
//            appUserPaperEvaluationAnalysisBiz.getBaseMapper().delete(
//                    new LambdaQueryWrapper<AppUserPaperEvaluationAnalysis>()
//                            .eq(AppUserPaperEvaluationAnalysis::getAppUserPaperInfoId, retakePaperBO.getPaperId())
//            );
//        } catch (ServiceException e) {
//            throw e;
//        } catch (Exception e) {
//            log.error("重新考试失败", e);
//            throw new ServiceException(APP_RE_EXAM_FAIL_MSG);
//        }
//    }
//
//    private void saveEvaluation(SubmitPaperBO submitBO, Integer paperId) throws ServiceException {
//        //如果是评测模块的考试，还需要新增评测报告和每个评测项的评测记录
//        if (submitBO.getBusinessId() == null){
//            throw new ServiceException(APP_EVALUATION_NODE_NO_ID_MSG);
//        }
//        AppEvaluationNode node = appEvaluationNodeBiz.getById(submitBO.getBusinessId());
//        if (ObjectUtils.isEmpty(node)){
//            throw new ServiceException(APP_EVALUATION_NODE_NO_ID_MSG);
//        }
//        //查询答题用户信息
//        // TODO: 待实现 - SysAppUserRemoteService需要后续实现
//        // UserIdApiBO userIdApiBO = new UserIdApiBO();
//        // userIdApiBO.setUserId(submitBO.getAppUserId());
//        // CurrentUserApiDTO currentUser = sysAppUserRemoteService.getCurrentUser(userIdApiBO);
//
//        //1. 新增评测报告并返回新增结果,如果评测表存在该用户的未生成的报告，则返回最近的一条报告记录
//        AppUserPaperEvaluationSaveBO evaluationSaveBO = new AppUserPaperEvaluationSaveBO();
//        evaluationSaveBO.setAppUserId(submitBO.getAppUserId());
//        evaluationSaveBO.setAppEvaluationId(submitBO.getAppEvaluationId());
//        // TODO: 待实现 - 需要从sysAppUserRemoteService获取用户信息
//        // evaluationSaveBO.setGradeName(currentUser.getGradeName());
//        // evaluationSaveBO.setGradeId(currentUser.getGradeId());
//        // evaluationSaveBO.setRegion(currentUser.getIpCity());
//        evaluationSaveBO.setGradeName(""); // 临时占位，待实现
//        evaluationSaveBO.setGradeId(null); // 临时占位，待实现
//        evaluationSaveBO.setRegion(""); // 临时占位，待实现
//        AppUserPaperEvaluationSaveDTO saveEvaluationReport = appUserPaperEvaluationBiz.saveEvaluationReport(evaluationSaveBO);
//
//
//        AppUserPaperEvaluationAnalysis saveOrUpdateAnalysis = new AppUserPaperEvaluationAnalysis();
//        //查询是否存在该评测表下评测项的评测报告分析，如果存在则修改正确错误题目数量,否则新增
//        List<AppUserPaperEvaluationAnalysis> analysisList = appUserPaperEvaluationAnalysisBiz.lambdaQuery()
//                .orderByDesc(AppUserPaperEvaluationAnalysis::getCreateTime)
//                .eq(AppUserPaperEvaluationAnalysis::getAppUserPaperInfoId,paperId)
//                .eq(AppUserPaperEvaluationAnalysis::getAppEvaluationNodeId, submitBO.getBusinessId())
//                .eq(AppUserPaperEvaluationAnalysis::getAppUserPaperEvaluationId, saveEvaluationReport.getId())
//                .list();
//        if (!CollectionUtils.isEmpty(analysisList)){
//            saveOrUpdateAnalysis = analysisList.get(0);
//            saveOrUpdateAnalysis.setUpdateTime(LocalDateTime.now());
//        }else{
//            saveOrUpdateAnalysis.setAppUserPaperEvaluationId(saveEvaluationReport.getId());
//            saveOrUpdateAnalysis.setAppEvaluationNodeId(submitBO.getBusinessId());
//            saveOrUpdateAnalysis.setAppEvaluationNodeName(node.getAppEvaluationNodeName());
//            saveOrUpdateAnalysis.setAppUserPaperInfoId(paperId);
//        }
//
//        //解析答题结果，正确几题，错误几题
//        List<SubmitQuestionResultBO> questionResults = submitBO.getQuestionResults();
//        List<SubmitBlankResultBO> blankResults = submitBO.getBlankResults();
//
//        long questionCorrect = questionResults.stream().filter(d -> d.getResult().equals(ANSWER_CORRECT.getValue())).count();
//        Map<Integer, List<SubmitBlankResultBO>> blankResultMap = blankResults.stream().collect(Collectors.groupingBy(SubmitBlankResultBO::getQuestionId));
//        int blankCorrect = 0;
//        int blankWrong = 0;
//        for (Integer questionId : blankResultMap.keySet()) {
//            List<SubmitBlankResultBO> submitBlankResultBOS = blankResultMap.get(questionId);
//            long correctCount = submitBlankResultBOS.stream().filter(d -> d.getResult().equals(ANSWER_CORRECT.getValue())).count();
//            if (correctCount == submitBlankResultBOS.size()) {
//                blankCorrect++;
//            }else{
//                blankWrong++;
//            }
//        }
//        long questionWrong = questionResults.stream().filter(d -> d.getResult().equals(ANSWER_WRONG.getValue())).count();
//        int rightCount = (int)questionCorrect+blankCorrect;
//        int wrongCount = (int)questionWrong+blankWrong;
//        saveOrUpdateAnalysis.setRightCount(rightCount);
//        saveOrUpdateAnalysis.setErrorCount(wrongCount);
//        saveOrUpdateAnalysis.setTotalCount(rightCount+wrongCount);
//
//        boolean saveOrUpdateFlag = appUserPaperEvaluationAnalysisBiz.saveOrUpdate(saveOrUpdateAnalysis);
//        if (!saveOrUpdateFlag){
//            throw new ServiceException(APP_EVALUATION_ANALYSIS_SAVE_FAIL_MSG);
//        }
//    }
//
//}