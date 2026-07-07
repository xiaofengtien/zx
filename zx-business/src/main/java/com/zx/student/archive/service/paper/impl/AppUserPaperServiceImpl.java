package com.zx.student.archive.service.paper.impl;

import cn.hutool.core.bean.BeanUtil;
import com.zx.common.constant.AppErrorCode;
import com.zx.common.core.redis.RedisCache;
import com.zx.common.exception.ServiceException;
import com.zx.student.archive.domain.bo.paper.*;
import com.zx.student.archive.domain.bo.question.QueryQuestionsCorrectAnswerBO;
import com.zx.student.archive.domain.dto.paper.PaperQuestionDTO;
import com.zx.student.archive.domain.dto.paper.PaperQuestionResultDTO;
import com.zx.student.archive.domain.dto.paper.UserPaperCheckSubmitDTO;
import com.zx.student.archive.domain.dto.paper.UserPaperDTO;
import com.zx.student.archive.domain.dto.question.QuestionCorrectAnswerDTO;
import com.zx.student.archive.factory.QuestionBusinessProcessStrategyFactory;
import com.zx.student.archive.service.paper.IAppUserPaperService;
import com.zx.student.archive.strategy.question.IQuestionBusinessStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户试卷服务实现类
 */
@Slf4j
@Service
public class AppUserPaperServiceImpl implements IAppUserPaperService {

    @Resource
    private RedisCache redisCache;
    @Resource
    private QuestionBusinessProcessStrategyFactory processStrategyFactory;
    @Override
    public List<PaperQuestionDTO> getPaperQuestions(QueryPaperQuestionsBO queryBO) throws ServiceException {
        validateParam(queryBO);
        IQuestionBusinessStrategy processStrategy = processStrategyFactory.getStrategy(queryBO.getBusinessType());
        List<PaperQuestionDTO> questions = processStrategy.getPaperQuestions(queryBO);
        if (CollectionUtils.isNotEmpty(questions)) {
            String redisKey = "paper:question:order:" + queryBO.getAppUserId() + ":" + queryBO.getBusinessId() + ":" + queryBO.getBusinessType();
            List<Integer> questionIds = questions.stream()
                    .map(PaperQuestionDTO::getId)
                    .toList();
            // 将 List<Integer> 转换为逗号分隔的字符串
            String orders = questionIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            // TODO: 需要实现Redis缓存逻辑
            // redisCache.setCacheObject(redisKey, orders, 30L * 24 * 60 * 60);
        }
        return questions;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserPaperDTO submitPaper(SubmitPaperBO submitBO) throws ServiceException {
        validateParam(submitBO);
        try {
            IQuestionBusinessStrategy processStrategy = processStrategyFactory.getStrategy(submitBO.getBusinessType());
            UserPaperDTO paperInfo = processStrategy.submitPaper(submitBO);
            return BeanUtil.copyProperties(paperInfo, UserPaperDTO.class);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("提交试卷失败", e);
            throw new ServiceException(AppErrorCode.APP_SUBMIT_PARER_FAIL_MSG);
        }
    }
    
    @Override
    public PaperQuestionResultDTO getPaperQuestionResult(QueryPaperResultBO queryBO) throws ServiceException {
        IQuestionBusinessStrategy processStrategy = processStrategyFactory.getStrategy(queryBO.getBusinessType());
        return processStrategy.getPaperResult(queryBO);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retakePaper(RetakePaperBO retakePaperBO) throws ServiceException {
        try {
            validateParam(retakePaperBO);
            IQuestionBusinessStrategy processStrategy = processStrategyFactory.getStrategy(retakePaperBO.getBusinessType());
            processStrategy.retakePaper(retakePaperBO);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("重新考试失败", e);
            throw new ServiceException(AppErrorCode.APP_RE_EXAM_FAIL_MSG);
        }
    }

    @Override
    public List<QuestionCorrectAnswerDTO> getAllQuestionsCorrectAnswers(QueryQuestionsCorrectAnswerBO bo) throws ServiceException {
        try {
            IQuestionBusinessStrategy processStrategy = processStrategyFactory.getStrategy(bo.getBusinessType());
            return processStrategy.getQuestionsCorrectAnswers(bo.getBusinessId(), bo.getBusinessType(), bo.getQuestionIds());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取题目正确答案失败", e);
            throw new ServiceException(AppErrorCode.APP_GET_QUESTION_FAIL_MSG);
        }
    }

    @Override
    public UserPaperCheckSubmitDTO checkSubmit(CheckSubmitBO serviceBO) throws ServiceException {
        validateParam(serviceBO);
        IQuestionBusinessStrategy processStrategy = processStrategyFactory.getStrategy(serviceBO.getBusinessType());
        return processStrategy.checkSubmit(serviceBO);
    }

    /**
     * 验证参数
     */
    private void validateParam(Object param) throws ServiceException {
        if (param == null) {
            throw new ServiceException(AppErrorCode.APP_PARAM_NOT_NULL_MSG);
        }
    }
} 