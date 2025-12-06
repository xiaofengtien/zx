package com.ruoyi.student.archive.biz.question.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.AppErrorCode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.biz.question.QuestionBlankAreaBiz;
import com.ruoyi.student.archive.domain.question.QuestionBlankArea;
import com.ruoyi.student.archive.mapper.question.QuestionBlankAreaMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * 完形填空业务实现类
 */
@Slf4j
@Service
public class QuestionBlankAreaBizImpl extends ServiceImpl<QuestionBlankAreaMapper, QuestionBlankArea> 
    implements QuestionBlankAreaBiz {

    @Override
    public List<QuestionBlankArea> getBlankAreas(Integer questionId) throws ServiceException {
        if (questionId == null) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_ID_NOT_NULL_MSG);
        }

        List<QuestionBlankArea> blankAreas = this.baseMapper.selectList(
                new LambdaQueryWrapper<QuestionBlankArea>()
                        .eq(QuestionBlankArea::getQuestionId, questionId)
                        .orderByAsc(QuestionBlankArea::getBlankIndex)
        );

        return CollectionUtils.isEmpty(blankAreas) ? Collections.emptyList() : blankAreas;
    }

    @Override
    public List<QuestionBlankArea> getBlankAreasByQuestionId(Integer questionId) {
        return this.lambdaQuery()
                .eq(QuestionBlankArea::getQuestionId, questionId)
                .orderByAsc(QuestionBlankArea::getBlankIndex)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBlankAreas(List<QuestionBlankArea> blankAreas) {
        this.saveBatch(blankAreas);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBlankAreas(Integer questionId, List<QuestionBlankArea> blankAreas) {
        this.deleteByQuestionId(List.of(questionId));
        blankAreas.forEach(area ->
                area.setQuestionId(questionId));
        this.saveBatch(blankAreas);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByQuestionId(List<Integer> questionIds) {
        this.lambdaUpdate()
                .in(QuestionBlankArea::getQuestionId, questionIds)
                .remove();
    }
} 