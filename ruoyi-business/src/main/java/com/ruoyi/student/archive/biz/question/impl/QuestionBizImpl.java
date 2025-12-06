package com.ruoyi.student.archive.biz.question.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.AppErrorCode;
import com.ruoyi.common.enums.YesOrNoEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.biz.question.QuestionBiz;
import com.ruoyi.student.archive.domain.dto.question.QuestionCountDTO;
import com.ruoyi.student.archive.domain.question.Question;
import com.ruoyi.student.archive.domain.question.QuestionAnswer;
import com.ruoyi.student.archive.domain.question.QuestionBlankArea;
import com.ruoyi.student.archive.mapper.question.QuestionAnswerMapper;
import com.ruoyi.student.archive.mapper.question.QuestionBlankAreaMapper;
import com.ruoyi.student.archive.mapper.question.QuestionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 题目业务实现类
 */
@Slf4j
@Service
public class QuestionBizImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionBiz {

    private final QuestionMapper questionMapper;
    @Resource
    private QuestionBlankAreaMapper questionBlankAreaMapper;
    @Resource
    private QuestionAnswerMapper answerMapper;
    public QuestionBizImpl(QuestionMapper questionMapper) {
        this.questionMapper = questionMapper;
    }

    @Override
    public Question getQuestionById(Integer id) {
        return this.getById(id);
    }

    @Override
    public List<QuestionCountDTO> countByCategoryIds(List<Integer> categoryIds) {
        return questionMapper.countByCategoryIds(categoryIds);
    }

    @Override
    public List<Question> getQuestionListByCategoryId(Integer categoryId) {
        return this.lambdaQuery()
                .eq(Question::getQuestionCategoryId, categoryId)
                .list();
    }

    @Override
    public Integer createQuestion(Question question) {
        this.save(question);
        return question.getId();
    }

    @Override
    public void updateQuestion(Question question) {
        this.updateById(question);
    }

    @Override
    public void batchDeleteQuestion(List<Integer> ids) {

        this.lambdaUpdate()
                .in(Question::getId, ids)
                .remove();
    }

    @Override
    public void batchUpdateQuestionCategory(List<Integer> questionIds, Integer targetCategoryId) {
        this.lambdaUpdate()
            .in(Question::getId, questionIds)
            .set(Question::getQuestionCategoryId, targetCategoryId)
            .update();
    }

    @Override
    public Page<Question> pageList(Page<Question> page, Question condition) {
        // BasicConfig 已废弃，题目业务不再使用，使用字典表获取学科名称
        return questionMapper.pageList(page, condition, "subject");
    }

    @Override
    public boolean hasCategoryQuestions(List<Integer> categoryIds) {
        if (CollectionUtils.isEmpty(categoryIds)) {
            return false;
        }
        LambdaQueryWrapper<Question> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Question::getQuestionCategoryId, categoryIds);
        return count(wrapper) > 0;
    }

    @Override
    public List<Question> getAllQuestionsByCategory(Integer categoryId) {
        return baseMapper.selectList(new LambdaQueryWrapper<Question>()
                .eq(Question::getQuestionCategoryId, categoryId)
                .eq(Question::getDelFlag, YesOrNoEnum.NO.getCode())
                .orderByDesc(Question::getWeight,Question::getId));
    }

    @Override
    public List<Question> getQuestionsByCategory(Integer categoryId, Integer limit) throws ServiceException {
        List<Question> questions = null;
        if (categoryId == null) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_BANK_ID_NOT_NULL_MSG);
        }
        if (limit == null || limit <= 0) {
            questions = this.baseMapper.selectList(
                    new LambdaQueryWrapper<Question>()
                            .eq(Question::getQuestionCategoryId, categoryId)
                            .orderByAsc(Question::getId)
            );
        }else{
            questions = this.baseMapper.selectList(
                    new LambdaQueryWrapper<Question>()
                            .eq(Question::getQuestionCategoryId, categoryId)
                            .orderByAsc(Question::getId)
                            .last("LIMIT " + limit)
            );
        }
        return CollectionUtils.isEmpty(questions) ? Collections.emptyList() : questions;
    }

    @Override
    public Map<Integer, Map<Integer, List<QuestionAnswer>>> getBlankAreaAnswerMap(Set<Integer> questionIds) {
        if (CollectionUtils.isEmpty(questionIds)) {
            return Collections.emptyMap();
        }

        // 获取所有空白区域
        List<QuestionBlankArea> blankAreas = questionBlankAreaMapper.selectList(
                new LambdaQueryWrapper<QuestionBlankArea>()
                        .in(QuestionBlankArea::getQuestionId, questionIds)
        );
        if (CollectionUtils.isEmpty(blankAreas)) {
            return Collections.emptyMap();
        }

        List<Integer> blankAreaIds = blankAreas.stream().map(QuestionBlankArea::getId).collect(Collectors.toList());

        // 获取所有答案
        List<QuestionAnswer> allAnswers = answerMapper.selectList(
                new LambdaQueryWrapper<QuestionAnswer>()
                        .in(QuestionAnswer::getBlankAreaId, blankAreaIds)
        );
        if (CollectionUtils.isEmpty(allAnswers)) {
            return Collections.emptyMap();
        }

        // 创建复合键，确保答案正确对应到题目和空白区域
        Map<String, List<QuestionAnswer>> answersMap = new HashMap<>();
        for (QuestionAnswer answer : allAnswers) {
            String key = answer.getQuestionId() + ":" + answer.getBlankAreaId();
            answersMap.computeIfAbsent(key, k -> new ArrayList<>()).add(answer);
        }

        // 按题目ID和空白区域ID组织结果
        Map<Integer, Map<Integer, List<QuestionAnswer>>> result = new HashMap<>();
        for (QuestionBlankArea area : blankAreas) {
            Integer questionId = area.getQuestionId();
            Integer blankAreaId = area.getId();
            String key = questionId + ":" + blankAreaId;

            List<QuestionAnswer> answers = answersMap.getOrDefault(key, Collections.emptyList());

            // 按serialNo排序，确保答案顺序一致
            answers.sort(Comparator.comparing(QuestionAnswer::getSerialNo, Comparator.nullsLast(Comparator.naturalOrder())));

            result.computeIfAbsent(questionId, k -> new HashMap<>())
                    .put(blankAreaId, answers);
        }

        return result;
    }
} 