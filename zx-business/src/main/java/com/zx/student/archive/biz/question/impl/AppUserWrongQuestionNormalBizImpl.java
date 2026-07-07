package com.zx.student.archive.biz.question.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zx.common.constant.AppErrorCode;
// BasicConfigBizTypeEnum 已废弃，题目业务不再使用 BasicConfig
import com.zx.common.enums.YesOrNoEnum;
import com.zx.student.archive.biz.question.IAppUserWrongQuestionNormalBiz;
import com.zx.student.archive.domain.bo.question.AddNormalWrongQuestionBO;
import com.zx.student.archive.domain.question.wrongquestion.AppUserWrongQuestionNormal;
import com.zx.student.archive.mapper.question.AppUserWrongQuestionNormalMapper;
import com.zx.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 用户普通题错题本业务实现类
 */
@Slf4j
@Service
public class AppUserWrongQuestionNormalBizImpl extends ServiceImpl<AppUserWrongQuestionNormalMapper, AppUserWrongQuestionNormal> implements IAppUserWrongQuestionNormalBiz {

    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddWrongQuestions(List<AddNormalWrongQuestionBO> boList) throws ServiceException {
        if (CollectionUtils.isEmpty(boList)) {
            log.error("批量添加错题失败，参数为空");
            throw new ServiceException(AppErrorCode.PARAMS_NOT_NULL_MSG);
        }
        
        List<AppUserWrongQuestionNormal> entityList = new ArrayList<>(boList.size());
        
        for (AddNormalWrongQuestionBO bo : boList) {
            AppUserWrongQuestionNormal entity = new AppUserWrongQuestionNormal();
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
        if (appUserId == null || ids == null) {
            log.error("删除错题失败，参数为空，appUserId={}, ids={}", appUserId, ids);
            throw new ServiceException(AppErrorCode.PARAMS_NOT_NULL_MSG);
        }
        
        LambdaUpdateWrapper<AppUserWrongQuestionNormal> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(AppUserWrongQuestionNormal::getAppUserId, appUserId)
                .in(AppUserWrongQuestionNormal::getId, ids)
                .set(AppUserWrongQuestionNormal::getDelFlag, YesOrNoEnum.YES.getCode());

        update(updateWrapper);
    }

    @Override
    public List<AppUserWrongQuestionNormal> listUserWrongQuestions(Integer appUserId, Integer subjectId) throws ServiceException {
        if (appUserId == null) {
            log.error("获取用户错题列表失败，用户ID为空");
            throw new ServiceException(AppErrorCode.PARAMS_NOT_NULL_MSG);
        }
        
        LambdaQueryWrapper<AppUserWrongQuestionNormal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AppUserWrongQuestionNormal::getAppUserId, appUserId)
                .eq(AppUserWrongQuestionNormal::getDelFlag, 0);
        
        if (subjectId != null) {
            queryWrapper.eq(AppUserWrongQuestionNormal::getSubjectId, subjectId);
        }
        
        queryWrapper.orderByDesc(AppUserWrongQuestionNormal::getCreateTime);
        
        return list(queryWrapper);
    }

    @Override
    public List<AppUserWrongQuestionNormal> queryNormalWrongQuestionsWithJoin(Integer appUserId, Integer subjectId, Set<Integer> questionIds, Integer type) {
         // BasicConfig 已废弃，题目业务不再使用，bizType 参数传入 "subject" 字符串
         return baseMapper.queryNormalWrongQuestionsWithJoin(appUserId, subjectId, questionIds, "subject", type);
    }

} 