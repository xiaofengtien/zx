package com.ruoyi.student.archive.strategy.question.manager;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.AppErrorCode;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.enums.YesOrNoEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.biz.paper.IAppUserPaperInfoBiz;
import com.ruoyi.student.archive.biz.paper.IAppUserPaperQuestionBlankResultBiz;
import com.ruoyi.student.archive.biz.paper.IAppUserPaperQuestionResultBiz;
import com.ruoyi.student.archive.biz.question.QuestionBiz;
import com.ruoyi.student.archive.domain.bo.paper.*;
import com.ruoyi.student.archive.domain.dto.paper.UserPaperCheckSubmitDTO;
import com.ruoyi.student.archive.domain.dto.paper.UserPaperDTO;
import com.ruoyi.student.archive.domain.paper.AppUserPaperInfo;
import com.ruoyi.student.archive.domain.paper.AppUserPaperQuestionBlankResult;
import com.ruoyi.student.archive.domain.paper.AppUserPaperQuestionResult;
import com.ruoyi.student.archive.domain.question.Question;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;


@Component
@Slf4j
public class PaperLifecycleManager {
    // 注入你需要的Biz, Mapper等
    @Resource
    private IAppUserPaperInfoBiz paperInfoBiz;
    @Resource
    private IAppUserPaperQuestionResultBiz questionResultBiz;
    @Resource
    private IAppUserPaperQuestionBlankResultBiz blankResultBiz;
    @Resource
    private QuestionBiz questionBiz;
    @Resource
    private RedisCache redisCache;
    @Resource
    protected IAppUserPaperQuestionResultBiz paperQuestionResultBiz;
    @Resource
    protected IAppUserPaperQuestionBlankResultBiz paperQuestionBlankResultBiz;


    public UserPaperDTO submit(SubmitPaperBO submitBO, String paperName) throws ServiceException {
        try {
            if (submitBO == null) {
                throw new ServiceException(AppErrorCode.APP_SUBMIT_PARAM_NOT_NULL_MSG);
            }
            if (submitBO.getAppUserId() == null) {
                throw new ServiceException(AppErrorCode.APP_USER_ID_NOT_NULL_MSG);
            }
            if (submitBO.getBusinessId() == null) {
                throw new ServiceException(AppErrorCode.APP_BUSINESS_ID_NOT_NULL_MSG);
            }
            if (submitBO.getBusinessType() == null) {
                throw new ServiceException(AppErrorCode.APP_BUSINESS_TYPE_NOT_NULL_MSG);
            }

            if (org.springframework.util.CollectionUtils.isEmpty(submitBO.getQuestionResults()) && org.springframework.util.CollectionUtils.isEmpty(submitBO.getBlankResults())) {
                throw new ServiceException(AppErrorCode.APP_PAPER_NO_ANSWER_MSG);
            }

            // 创建试卷
            CreateUserPaperBO createBO = new CreateUserPaperBO();
            createBO.setAppUserId(submitBO.getAppUserId());
            createBO.setBusinessId(submitBO.getBusinessId());
            createBO.setBusinessType(submitBO.getBusinessType());
            String redisKey = "paper:question:order:" + submitBO.getAppUserId() + ":" + submitBO.getBusinessId() + ":" + submitBO.getBusinessType();
            // TODO: 需要实现Redis缓存逻辑
            // String orders = redisCache.getCacheObject(redisKey);
            // log.info("试卷缓存中获得的题目列表顺序：{}", JSON.toJSONString(orders));
            // createBO.setQuestionOrder(orders);
            createBO.setPaperName(paperName);
            createBO.setIsSubmit(YesOrNoEnum.YES.getCode());
            UserPaperDTO paperInfo = createPaper(createBO);
            return BeanUtil.copyProperties(paperInfo,UserPaperDTO.class);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("保存试卷结果失败", e);
            throw new ServiceException(AppErrorCode.APP_SAVE_PAPER_RESULT_FAIL_MSG);
        }
    }

    private UserPaperDTO createPaper(CreateUserPaperBO createBO) throws ServiceException {
        validateCreateParams(createBO);
        try {
            return paperInfoBiz.createPaper(createBO);
        } catch (ServiceException e) {
            log.error("创建试卷失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("创建试卷发生异常", e);
            throw new ServiceException(AppErrorCode.APP_CREATE_PAPER_FAIL_MSG);
        }
    }

    /**
     * 校验创建参数
     */
    private void validateCreateParams(CreateUserPaperBO createBO) throws ServiceException {
        if (createBO == null) {
            throw new ServiceException(AppErrorCode.APP_SUBMIT_PARAM_NOT_NULL_MSG);
        }
        if (createBO.getAppUserId() == null) {
            throw new ServiceException(AppErrorCode.APP_USER_ID_NOT_NULL_MSG);
        }
        if (createBO.getBusinessId() == null) {
            throw new ServiceException(AppErrorCode.APP_BUSINESS_ID_NOT_NULL_MSG);
        }
        if (createBO.getBusinessType() == null) {
            throw new ServiceException(AppErrorCode.APP_BUSINESS_TYPE_NOT_NULL_MSG);
        }
    }


    public void retake(RetakePaperBO retakePaperBO) throws ServiceException {
        try {
            AppUserPaperInfo paperInfo = paperInfoBiz.getBaseMapper().selectById(retakePaperBO.getPaperId());
            if (paperInfo == null) {
                throw new ServiceException(AppErrorCode.APP_PAPER_NOT_EXIST_MSG);
            }
            if (!retakePaperBO.getAppUserId().equals(paperInfo.getAppUserId())) {
                throw new ServiceException(AppErrorCode.APP_PAPER_NO_PERMISSION_MSG);
            }
            paperInfoBiz.remove(new LambdaQueryWrapper<AppUserPaperInfo>().eq(AppUserPaperInfo::getId,retakePaperBO.getPaperId()));

            paperQuestionResultBiz.getBaseMapper().delete(
                    new LambdaQueryWrapper<AppUserPaperQuestionResult>()
                            .eq(AppUserPaperQuestionResult::getPaperId, retakePaperBO.getPaperId())
                            .eq(AppUserPaperQuestionResult::getAppUserId, retakePaperBO.getAppUserId())
            );

            paperQuestionBlankResultBiz.getBaseMapper().delete(
                    new LambdaQueryWrapper<AppUserPaperQuestionBlankResult>()
                            .eq(AppUserPaperQuestionBlankResult::getPaperId, retakePaperBO.getPaperId())
                            .eq(AppUserPaperQuestionBlankResult::getAppUserId, retakePaperBO.getAppUserId())
            );
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("重新考试失败", e);
            throw new ServiceException(AppErrorCode.APP_RE_EXAM_FAIL_MSG);
        }
    }


    public void saveQuestionDetails(SubmitPaperBO submitBO, Integer paperId) throws ServiceException {
        saveQuestionResult(submitBO,paperId);
        saveBlankResult(submitBO,paperId);
    }


    
    public Map<Integer, Question> loadQuestionsForSubmission(SubmitPaperBO submitBO) {
        Set<Integer> questionIds = new LinkedHashSet<>();

        if (!CollectionUtils.isEmpty(submitBO.getQuestionResults())) {
            submitBO.getQuestionResults().forEach(result -> questionIds.add(result.getQuestionId()));
        }
        if (!CollectionUtils.isEmpty(submitBO.getBlankResults())) {
            submitBO.getBlankResults().forEach(result -> questionIds.add(result.getQuestionId()));
        }

        Map<Integer, Question> questionMap = new HashMap<>();
        if (!questionIds.isEmpty()) {
            questionBiz.listByIds(questionIds).forEach(q -> questionMap.put(q.getId(), q));
        }
        return questionMap;
    }
    
    public UserPaperCheckSubmitDTO checkSubmit(CheckSubmitBO serviceBO) throws ServiceException {
        List<AppUserPaperInfo> existingPapers = paperInfoBiz.getBaseMapper().selectList(
                new LambdaQueryWrapper<AppUserPaperInfo>()
                        .eq(AppUserPaperInfo::getAppUserId, serviceBO.getAppUserId())
                        .eq(AppUserPaperInfo::getBusinessId, serviceBO.getBusinessId())
                        .eq(AppUserPaperInfo::getBusinessType, serviceBO.getBusinessType())
                        .orderByDesc(AppUserPaperInfo::getCreateTime)
        );

        if (org.springframework.util.CollectionUtils.isEmpty(existingPapers)) {
            return UserPaperCheckSubmitDTO.builder().isSubmit(Boolean.FALSE).paperId(null).build();
        } else {
            return UserPaperCheckSubmitDTO.builder().isSubmit(Boolean.TRUE).paperId(existingPapers.get(0).getId()).build();
        }
    }

    public UserPaperDTO getPaperInfo(Integer paperId) throws ServiceException {
        if (paperId == null) {
            throw new ServiceException(AppErrorCode.APP_PAPER_ID_NOT_NULL_MSG);
        }
        try {
            return paperInfoBiz.getPaperInfo(paperId);
        } catch (ServiceException e) {
            log.error("获取试卷信息失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取试卷信息发生异常", e);
            throw new ServiceException(AppErrorCode.APP_GET_PAPER_INFO_ERROR_MSG);
        }
    }


    public void saveQuestionResult(SubmitPaperBO submitBO, Integer paperId) throws ServiceException {
        if (CollectionUtils.isEmpty(submitBO.getQuestionResults()) && CollectionUtils.isEmpty(submitBO.getBlankResults())) {
            throw new ServiceException(AppErrorCode.APP_PAPER_NO_ANSWER_MSG);
        }
        if (!CollectionUtils.isEmpty(submitBO.getQuestionResults())) {
            for (SubmitQuestionResultBO resultBO : submitBO.getQuestionResults()) {
                SaveQuestionResultBO result = new SaveQuestionResultBO();
                BeanUtil.copyProperties(resultBO, result);
                result.setBusinessId(submitBO.getBusinessId());
                result.setBusinessType(submitBO.getBusinessType());
                result.setAppUserId(submitBO.getAppUserId());
                result.setPaperId(paperId);
                boolean success = questionResultBiz.saveQuestionResult(result);
                if (!success) {
                    throw new ServiceException(AppErrorCode.APP_SAVE_QUESTION_RESULT_FAIL_MSG);
                }
            }
        }
    }



    private void saveBlankResult(SubmitPaperBO submitBO, Integer paperId) throws ServiceException {
        if (!CollectionUtils.isEmpty(submitBO.getBlankResults())) {
            for (SubmitBlankResultBO blankBO : submitBO.getBlankResults()) {
                if(StringUtils.isEmpty((blankBO.getAnswerIds()))){
                    continue;
                }
                SaveBlankResultBO result = new SaveBlankResultBO();
                BeanUtil.copyProperties(blankBO, result);
                result.setPaperId(paperId);
                result.setBusinessId(submitBO.getBusinessId());
                result.setBusinessType(submitBO.getBusinessType());
                result.setAppUserId(submitBO.getAppUserId());
                boolean success = blankResultBiz.saveBlankResult(result);
                if (!success) {
                    throw new ServiceException(AppErrorCode.APP_SAVE_CLOZE_RESULT_FAIL_MSG);
                }
            }
        }
    }
}

