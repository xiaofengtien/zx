package com.ruoyi.student.archive.biz.paper.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.enums.YesOrNoEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.biz.paper.IAppUserPaperQuestionResultBiz;
import com.ruoyi.student.archive.domain.bo.paper.QueryErrorResultsBO;
import com.ruoyi.student.archive.domain.bo.paper.QueryQuestionResultBO;
import com.ruoyi.student.archive.domain.bo.paper.SaveQuestionResultBO;
import com.ruoyi.student.archive.domain.dto.paper.QuestionResultDTO;
import com.ruoyi.student.archive.domain.paper.AppUserPaperQuestionResult;
import com.ruoyi.student.archive.mapper.paper.AppUserPaperQuestionResultMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户试卷题目结果业务实现类
 */
@Slf4j
@Service
public class AppUserPaperQuestionResultBizImpl extends ServiceImpl<AppUserPaperQuestionResultMapper, AppUserPaperQuestionResult> implements IAppUserPaperQuestionResultBiz {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveQuestionResult(SaveQuestionResultBO saveBO) throws ServiceException {
        AppUserPaperQuestionResult result = new AppUserPaperQuestionResult();
        BeanUtil.copyProperties(saveBO, result);
        return save(result);
    }

    @Override
    public List<QuestionResultDTO> getQuestionResults(QueryQuestionResultBO queryBO) throws ServiceException {
        List<AppUserPaperQuestionResult> results = list(new LambdaQueryWrapper<AppUserPaperQuestionResult>()
                .eq(AppUserPaperQuestionResult::getPaperId, queryBO.getPaperId())
                .eq(AppUserPaperQuestionResult::getAppUserId, queryBO.getAppUserId()));

        return results.stream()
                .map(result -> {
                    QuestionResultDTO dto = new QuestionResultDTO();
                    BeanUtil.copyProperties(result, dto);
                    dto.setUserAnswerIds(result.getAnswerIds());
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public List<AppUserPaperQuestionResult> batchGetErrorResults(QueryErrorResultsBO queryBO) {
        if (queryBO == null || CollectionUtils.isEmpty(queryBO.getQuestionIds())) {
            return Collections.emptyList();
        }
        
        LambdaQueryWrapper<AppUserPaperQuestionResult> queryWrapper = new LambdaQueryWrapper<AppUserPaperQuestionResult>()
                .eq(AppUserPaperQuestionResult::getAppUserId, queryBO.getAppUserId())
                .in(AppUserPaperQuestionResult::getQuestionId, queryBO.getQuestionIds())
                .eq(AppUserPaperQuestionResult::getResult, YesOrNoEnum.NO.getCode());
        
        if (queryBO.getSourceType() != null) {
            queryWrapper.eq(AppUserPaperQuestionResult::getBusinessType, queryBO.getSourceType());
        }
        
        if (queryBO.getSourceId() != null) {
            queryWrapper.eq(AppUserPaperQuestionResult::getBusinessId, queryBO.getSourceId());
        }
        
        return this.list(queryWrapper);
    }
} 