package com.zx.student.archive.biz.paper.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zx.common.constant.AppErrorCode;
import com.zx.common.enums.YesOrNoEnum;
import com.zx.common.exception.ServiceException;
import com.zx.student.archive.biz.paper.IAppUserPaperQuestionBlankResultBiz;
import com.zx.student.archive.domain.bo.paper.CheckBlankAnsweredBO;
import com.zx.student.archive.domain.bo.paper.QueryBlankResultBO;
import com.zx.student.archive.domain.bo.paper.QueryErrorBlankResultsBO;
import com.zx.student.archive.domain.bo.paper.SaveBlankResultBO;
import com.zx.student.archive.domain.dto.paper.BlankResultDTO;
import com.zx.student.archive.domain.paper.AppUserPaperQuestionBlankResult;
import com.zx.student.archive.mapper.paper.AppUserPaperQuestionBlankResultMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户试卷题目空位结果业务实现类
 */
@Slf4j
@Service
public class AppUserPaperQuestionBlankResultBizImpl extends ServiceImpl<AppUserPaperQuestionBlankResultMapper, AppUserPaperQuestionBlankResult> implements IAppUserPaperQuestionBlankResultBiz {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBlankResult(SaveBlankResultBO saveBO) throws ServiceException {
        if (saveBO == null) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_CLOZE_PARAM_NOT_NULL_MSG);
        }
        AppUserPaperQuestionBlankResult result = new AppUserPaperQuestionBlankResult();
        BeanUtil.copyProperties(saveBO, result);
        
        return save(result);
    }

    @Override
    public List<BlankResultDTO> getBlankResults(QueryBlankResultBO queryBO) throws ServiceException {
        if (queryBO == null) {
            return List.of();
        }

        List<AppUserPaperQuestionBlankResult> results = list(new LambdaQueryWrapper<AppUserPaperQuestionBlankResult>()
                .eq(Objects.nonNull(queryBO.getPaperId()),AppUserPaperQuestionBlankResult::getPaperId, queryBO.getPaperId())
                .eq(Objects.nonNull(queryBO.getQuestionId()),AppUserPaperQuestionBlankResult::getQuestionId, queryBO.getQuestionId())
                .eq(AppUserPaperQuestionBlankResult::getAppUserId, queryBO.getAppUserId())
                .orderByAsc(AppUserPaperQuestionBlankResult::getBlankIndex));

        return results.stream()
                .map(result -> {
                    BlankResultDTO dto = new BlankResultDTO();
                    BeanUtil.copyProperties(result, dto);
                    dto.setUserAnswerIds(result.getAnswerIds());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean checkAllBlanksAnswered(CheckBlankAnsweredBO checkBO) throws ServiceException {
        if (checkBO == null) {
            return false;
        }

        long answeredCount = count(new LambdaQueryWrapper<AppUserPaperQuestionBlankResult>()
                .eq(AppUserPaperQuestionBlankResult::getQuestionId, checkBO.getQuestionId())
                .eq(AppUserPaperQuestionBlankResult::getAppUserId, checkBO.getAppUserId()));

        return answeredCount >= checkBO.getTotalBlanks();
    }
    
    @Override
    public List<AppUserPaperQuestionBlankResult> batchGetErrorResults(QueryErrorBlankResultsBO queryBO) {
        if (queryBO == null || CollectionUtils.isEmpty(queryBO.getQuestionIds())) {
            return Collections.emptyList();
        }
        
        LambdaQueryWrapper<AppUserPaperQuestionBlankResult> queryWrapper = new LambdaQueryWrapper<AppUserPaperQuestionBlankResult>()
                .eq(AppUserPaperQuestionBlankResult::getAppUserId, queryBO.getAppUserId())
                .in(AppUserPaperQuestionBlankResult::getQuestionId, queryBO.getQuestionIds())
                .eq(AppUserPaperQuestionBlankResult::getResult, YesOrNoEnum.NO.getCode());
        
        if (queryBO.getSourceType() != null) {
            queryWrapper.eq(AppUserPaperQuestionBlankResult::getBusinessType, queryBO.getSourceType());
        }
        
        if (queryBO.getSourceId() != null) {
            queryWrapper.eq(AppUserPaperQuestionBlankResult::getBusinessId, queryBO.getSourceId());
        }
        
        return this.list(queryWrapper);
    }
} 