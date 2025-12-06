package com.ruoyi.student.archive.biz.question.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.AppErrorCode;
// BasicConfigBizTypeEnum 已废弃，题目业务不再使用 BasicConfig
import com.ruoyi.common.enums.YesOrNoEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.biz.question.IAppUserWrongQuestionBlankBiz;
import com.ruoyi.student.archive.domain.bo.question.AddBlankWrongQuestionBO;
import com.ruoyi.student.archive.domain.question.wrongquestion.AppUserWrongQuestionBlank;
import com.ruoyi.student.archive.mapper.question.AppUserWrongQuestionBlankMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 用户完形填空错题本业务实现类
 */
@Slf4j
@Service
public class AppUserWrongQuestionBlankBizImpl extends ServiceImpl<AppUserWrongQuestionBlankMapper, AppUserWrongQuestionBlank> implements IAppUserWrongQuestionBlankBiz {

    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddWrongQuestions(List<AddBlankWrongQuestionBO> boList) throws ServiceException {
        if (CollectionUtils.isEmpty(boList)) {
            log.error("批量添加完形填空错题失败，参数为空");
            throw new ServiceException(AppErrorCode.PARAMS_NOT_NULL_MSG);
        }
        
        List<AppUserWrongQuestionBlank> entityList = new ArrayList<>(boList.size());
        
        for (AddBlankWrongQuestionBO bo : boList) {
            AppUserWrongQuestionBlank entity = new AppUserWrongQuestionBlank();
            BeanUtil.copyProperties(bo, entity);
            
            // 确保字段名映射正确
            entity.setAnswerIds(bo.getAnswerIds());
            
            entityList.add(entity);
        }

        saveBatch(entityList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeWrongQuestion(Integer appUserId, List<Integer> ids) throws ServiceException {
        if (appUserId == null || CollectionUtils.isEmpty(ids)) {
            log.error("删除错题失败，参数为空，appUserId={}, ids={}", appUserId, ids);
            throw new ServiceException(AppErrorCode.PARAMS_NOT_NULL_MSG);
        }

        LambdaUpdateWrapper<AppUserWrongQuestionBlank> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AppUserWrongQuestionBlank::getAppUserId, appUserId)
                .in(AppUserWrongQuestionBlank::getId, ids)
                .set(AppUserWrongQuestionBlank::getDelFlag, YesOrNoEnum.YES.getCode());
        update(updateWrapper);
    }

    @Override
    public List<AppUserWrongQuestionBlank> listUserWrongQuestions(Integer appUserId, Integer subjectId) throws ServiceException {
        if (appUserId == null) {
            log.error("获取用户完形填空错题列表失败，用户ID为空");
            throw new ServiceException("用户ID不能为空");
        }
        
        LambdaQueryWrapper<AppUserWrongQuestionBlank> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppUserWrongQuestionBlank::getAppUserId, appUserId)
                .eq(AppUserWrongQuestionBlank::getDelFlag, 0);
        
        if (subjectId != null) {
            queryWrapper.eq(AppUserWrongQuestionBlank::getSubjectId, subjectId);
        }
        
        queryWrapper.orderByDesc(AppUserWrongQuestionBlank::getCreateTime);
        
        return list(queryWrapper);
    }

    @Override
    public List<AppUserWrongQuestionBlank> listQuestionBlanks(Integer appUserId, Integer questionId) throws ServiceException {
        if (appUserId == null || questionId == null) {
            log.error("获取题目空位错题列表失败，参数为空，appUserId={}, questionId={}", appUserId, questionId);
            throw new ServiceException(AppErrorCode.PARAMS_NOT_NULL_MSG);
        }
        
        LambdaQueryWrapper<AppUserWrongQuestionBlank> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppUserWrongQuestionBlank::getAppUserId, appUserId)
                .eq(AppUserWrongQuestionBlank::getQuestionId, questionId)
                .eq(AppUserWrongQuestionBlank::getDelFlag, 0)
                .orderByAsc(AppUserWrongQuestionBlank::getBlankIndex);
        
        return list(queryWrapper);
    }


    @Override
    public List<AppUserWrongQuestionBlank> queryBlankWrongQuestionsWithJoin(Integer appUserId, Integer subjectId, Set<Integer> questionIds, Integer type) {
        // BasicConfig 已废弃，题目业务不再使用，bizType 参数传入 "subject" 字符串
        return baseMapper.queryBlankWrongQuestionsWithJoin(appUserId, subjectId, questionIds, "subject", type);
    }
} 