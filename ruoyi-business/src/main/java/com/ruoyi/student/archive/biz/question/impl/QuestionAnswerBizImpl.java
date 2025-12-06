package com.ruoyi.student.archive.biz.question.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.AppErrorCode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.biz.question.QuestionAnswerBiz;
import com.ruoyi.student.archive.domain.question.QuestionAnswer;
import com.ruoyi.student.archive.mapper.question.QuestionAnswerMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class QuestionAnswerBizImpl extends ServiceImpl<QuestionAnswerMapper, QuestionAnswer> implements QuestionAnswerBiz {

    @Override
    public List<QuestionAnswer> getAnswersByQuestionId(Integer questionId) {
        if (questionId == null) {
            return Collections.emptyList();
        }
        return this.lambdaQuery()
                .eq(QuestionAnswer::getQuestionId, questionId)
                .orderByAsc(QuestionAnswer::getSerialNo)
                .list();
    }

    @Override
    public List<QuestionAnswer> getAnswersByIds(Collection<Integer> answerIds) {
        if (CollectionUtils.isEmpty(answerIds)) {
            return Collections.emptyList();
        }
        // 查询时不过滤删除状态，以获取用户答题时的完整选项信息
        return this.lambdaQuery()
                .in(QuestionAnswer::getId, answerIds)
                .orderByAsc(QuestionAnswer::getSerialNo)
                .list();
    }

    @Override
    public List<QuestionAnswer> getAnswersByBlankAreaId(Integer questionId, Integer blankAreaId){
        if (blankAreaId == null) {
            return Collections.emptyList();
        }
        return this.lambdaQuery()
                .eq(QuestionAnswer::getBlankAreaId, blankAreaId)
                .eq(QuestionAnswer::getQuestionId, questionId)
                .orderByAsc(QuestionAnswer::getSerialNo)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveAnswers(List<QuestionAnswer> answers) throws ServiceException {
        if (CollectionUtils.isEmpty(answers)) {
            return true;
        }
        try {
            return this.saveBatch(answers);
        } catch (Exception e) {
            log.error("保存答案列表失败", e);
            throw new ServiceException(AppErrorCode.APP_SAVE_ANSWER_LIST_FAIL_MSG);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAnswers(Integer questionId, List<QuestionAnswer> answers) throws ServiceException {
        if (questionId == null) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_ID_NOT_NULL_MSG);
        }
        try {
            this.lambdaUpdate()
                    .eq(QuestionAnswer::getQuestionId, questionId)
                    .remove();

            if (!CollectionUtils.isEmpty(answers)) {
                return this.saveBatch(answers);
            }
            return true;
        } catch (Exception e) {
            log.error("更新答案列表失败", e);
            throw new ServiceException(AppErrorCode.APP_UPDATE_ANSWER_LIST_FAIL_MSG);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBlankAnswers(Integer blankAreaId, List<QuestionAnswer> answers) throws ServiceException {
        if (blankAreaId == null) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_ID_NOT_NULL_MSG);
        }
        try {
            this.lambdaUpdate()
                    .eq(QuestionAnswer::getBlankAreaId, blankAreaId)
                    .remove();

            if (!CollectionUtils.isEmpty(answers)) {
                return this.saveBatch(answers);
            }
            return true;
        } catch (Exception e) {
            log.error("更新答案列表失败", e);
            throw new ServiceException(AppErrorCode.APP_UPDATE_ANSWER_LIST_FAIL_MSG);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByQuestionId(List<Integer> questionIds) throws ServiceException {
        if (CollectionUtils.isEmpty(questionIds)) {
            return true;
        }
        try {
            return this.lambdaUpdate()
                    .in(QuestionAnswer::getQuestionId, questionIds)
                    .remove();
        } catch (Exception e) {
            log.error("删除答案列表失败", e);
            throw new ServiceException(AppErrorCode.APP_DELETE_ANSWER_LIST_FAIL_MSG);
        }
    }

    @Override
    public boolean deleteByBlankAreaId(List<Integer> areaIds) throws ServiceException {
        if (CollectionUtils.isEmpty(areaIds)) {
            return true;
        }
        try {
            return this.lambdaUpdate()
                    .in(QuestionAnswer::getBlankAreaId, areaIds)
                    .remove();
        } catch (Exception e) {
            log.error("删除答案列表失败", e);
            throw new ServiceException(AppErrorCode.APP_DELETE_ANSWER_LIST_FAIL_MSG);
        }
    }
} 