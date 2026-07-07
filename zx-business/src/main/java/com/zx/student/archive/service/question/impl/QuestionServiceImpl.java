package com.zx.student.archive.service.question.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zx.common.constant.AppErrorCode;
import com.zx.common.enums.YesOrNoEnum;
import com.zx.common.enums.question.QuestionTypeEnum;
import com.zx.common.exception.ServiceException;
import com.zx.student.archive.biz.question.*;
import com.zx.system.service.ISysDictDataService;
import com.zx.student.archive.domain.bo.question.*;
import com.zx.student.archive.domain.dto.question.*;
import com.zx.student.archive.domain.question.Question;
import com.zx.student.archive.domain.question.QuestionAnswer;
import com.zx.student.archive.domain.question.QuestionBlankArea;
import com.zx.student.archive.domain.question.QuestionCategory;
import com.zx.student.archive.domain.question.QuestionMedia;
import com.zx.student.archive.service.question.QuestionService;
import com.zx.student.archive.util.QuestionValidator;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.zx.common.constant.AppErrorCode.*;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionBiz questionBiz;
    private final QuestionCategoryBiz questionCategoryBiz;
    private final QuestionAnswerBiz questionAnswerBiz;
    private final QuestionBlankAreaBiz questionBlankAreaBiz;
    private final ISysDictDataService dictDataService;
    private final IQuestionCategoryBusinessRefBiz questionCategoryBusinessRefBiz;
    private final IQuestionMediaBiz questionMediaBiz;
    /**
     * 根节点的父ID
     */
    public static final Integer DEFAULT_FATHER_ID = 1;

    @Override
    public QuestionInfoDTO getQuestion(QuestionIdBO idBO) throws ServiceException {
        Question question = questionBiz.getQuestionById(idBO.getId());
        if (question == null) {
            return null;
        }
        List<QuestionAnswer> answers = questionAnswerBiz.getAnswersByQuestionId(idBO.getId());
        question.setAnswers(answers);
        return convertToDTO(question);
    }

    @SneakyThrows
    @Override
    public List<QuestionInfoDTO> getQuestionList(QuestionCategoryIdQueryBO idBO) {
        List<Question> questions = questionBiz.getQuestionListByCategoryId(idBO.getCategoryId());
        return questions.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 保存单个答案并返回保存后的实体
     *
     * @param answerBO    答案BO对象
     * @param questionId  问题ID
     * @param blankAreaId 空位区域ID，可为null
     * @return 保存后的答案实体
     */
    public QuestionAnswer saveAnswer(QuestionAnswerBO answerBO, Integer questionId, Integer blankAreaId) {
        QuestionAnswer answer = new QuestionAnswer();
        BeanUtil.copyProperties(answerBO, answer);
        answer.setId(null);
        answer.setQuestionId(questionId);

        if (blankAreaId != null) {
            answer.setBlankAreaId(blankAreaId);
        }

        // 保存答案
        questionAnswerBiz.save(answer);

        return answer;
    }

    /**
     * 处理答案的媒体文件引用
     *
     * @param answerBO   答案BO对象
     * @param answerId   保存后的答案ID
     * @param questionId 题目ID（用于选项媒体，确保question_id不为null）
     */
    public void processAnswerMediaReferences(QuestionAnswerBO answerBO, Integer answerId, Integer questionId) {
        if (!CollectionUtils.isEmpty(answerBO.getMediaUrl())) {
            // 如果questionId为null，通过answerId查找对应的questionId
            if (questionId == null && answerId != null) {
                QuestionAnswer answer = questionAnswerBiz.getById(answerId);
                if (answer != null) {
                    questionId = answer.getQuestionId();
                }
            }

            Integer finalQuestionId = questionId;
            List<QuestionMedia> mediaList = answerBO.getMediaUrl().stream()
                    .map(mediaBO -> {
                        QuestionMedia media = new QuestionMedia();
                        // 选项媒体也需要设置questionId（用于数据库约束，即使业务上不直接关联）
                        media.setQuestionId(finalQuestionId);
                        media.setOptionId(answerId);
                        media.setMediaType(2); // 2-选项媒体
                        media.setMediaName(mediaBO.getMediaName() != null ? mediaBO.getMediaName() : "");
                        media.setMediaPath(mediaBO.getMediaPath());
                        media.setMediaUrl(mediaBO.getMediaUrl());
                        media.setMediaSize(mediaBO.getMediaSize());
                        media.setMediaFormat(mediaBO.getMediaFormat());
                        media.setMediaDuration(mediaBO.getMediaDuration());
                        media.setIsCompressed(mediaBO.getIsCompressed());
                        media.setStorageType(mediaBO.getStorageType());
                        return media;
                    })
                    .collect(Collectors.toList());
            questionMediaBiz.batchSave(mediaList);
        }
    }

    /**
     * 处理答案的媒体文件引用（兼容旧方法，自动查找questionId）
     *
     * @param answerBO 答案BO对象
     * @param answerId 保存后的答案ID
     */
    public void processAnswerMediaReferences(QuestionAnswerBO answerBO, Integer answerId) {
        processAnswerMediaReferences(answerBO, answerId, null);
    }

    public void updateAnswerMediaReferences(QuestionAnswerBO answerBO, Integer answerId) {
        // 删除原有关联的选项媒体文件
        List<QuestionMedia> existingMedia = questionMediaBiz.list(
                new LambdaQueryWrapper<QuestionMedia>()
                        .eq(QuestionMedia::getOptionId, answerId)
                        .eq(QuestionMedia::getMediaType, 2));
        if (!CollectionUtils.isEmpty(existingMedia)) {
            questionMediaBiz.removeByIds(existingMedia.stream().map(QuestionMedia::getId).collect(Collectors.toList()));
        }

        // 保存新的媒体文件（通过answerId查找questionId）
        if (!CollectionUtils.isEmpty(answerBO.getMediaUrl())) {
            processAnswerMediaReferences(answerBO, answerId, null); // 传入null，方法内部会自动查找questionId
        }
    }

    /**
     * 处理普通题型的所有答案
     *
     * @param questionBO 问题BO对象
     * @param questionId 问题ID
     * @param answerIds  保存正确答案ID的集合
     */
    public void processNormalAnswers(QuestionBO questionBO, Integer questionId, Set<String> answerIds) {
        if (questionBO.getAnswers() != null) {
            // 循环处理每个答案
            for (QuestionAnswerBO answerBO : questionBO.getAnswers()) {
                // 保存答案
                QuestionAnswer answer = saveAnswer(answerBO, questionId, null);

                // 收集正确答案ID
                if (Objects.equals(answerBO.getIsAnswer(), YesOrNoEnum.YES.getCode())) {
                    answerIds.add(answer.getId().toString());
                }

                // 处理媒体文件引用（传入questionId确保不为null）
                processAnswerMediaReferences(answerBO, answer.getId(), questionId);
            }
        }
    }

    /**
     * 处理完形填空题中单个空位区域的所有答案
     *
     * @param areaBO     空位区域BO对象
     * @param area       保存后的空位区域实体
     * @param questionId 问题ID
     */
    public void processBlankAreaAnswers(QuestionBlankAreaBO areaBO, QuestionBlankArea area, Integer questionId) {
        if (!CollectionUtils.isEmpty(areaBO.getAnswers())) {
            // 收集正确答案ID
            Set<String> blankAnswerIds = new LinkedHashSet<>();

            // 循环处理每个空位区域的答案
            for (QuestionAnswerBO answerBO : areaBO.getAnswers()) {
                // 保存答案
                QuestionAnswer answer = saveAnswer(answerBO, questionId, area.getId());

                // 收集正确答案ID
                if (Objects.equals(answerBO.getIsAnswer(), YesOrNoEnum.YES.getCode())) {
                    blankAnswerIds.add(answer.getId().toString());
                }

                // 处理媒体文件引用（传入questionId确保不为null）
                processAnswerMediaReferences(answerBO, answer.getId(), questionId);
            }

            // 更新空位的正确答案ID
            if (!blankAnswerIds.isEmpty()) {
                area.setAnswerIds(String.join(",", blankAnswerIds));
                questionBlankAreaBiz.updateById(area);
            }
        }
    }

    /**
     * 处理完形填空题的所有空位区域
     *
     * @param questionBO 问题BO对象
     * @param questionId 问题ID
     */
    public void processBlankAreas(QuestionBO questionBO, Integer questionId) {
        if (Objects.equals(questionBO.getType(), QuestionTypeEnum.CLOZE.getValue())
                && questionBO.getBlankAreas() != null) {
            // 保存空位区域
            for (QuestionBlankAreaBO areaBO : questionBO.getBlankAreas()) {
                QuestionBlankArea area = new QuestionBlankArea();
                BeanUtil.copyProperties(areaBO, area);
                area.setId(null);
                area.setQuestionId(questionId);

                // 保存空位区域
                questionBlankAreaBiz.save(area);

                // 处理空位区域答案
                processBlankAreaAnswers(areaBO, area, questionId);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createQuestion(QuestionBO questionBO) throws ServiceException {
        QuestionValidator.validate(questionBO, questionBO.getAnswers());
        Question question = convertToDomain(questionBO);
        if (question.getAnalyzes() != null && question.getAnalyzes().trim().isEmpty()) {
            question.setAnalyzes(null);
        }
        if (question.getQuestionCategoryId() != null) {
            QuestionCategory questionCategory = questionCategoryBiz.getById(question.getQuestionCategoryId());
            if (questionCategory == null) {
                throw new ServiceException(APP_QUESTION_CATEGORY_NOT_EXIST_MSG);
            }
            if (questionCategory.getIsDefault()) {
                throw new ServiceException(APP_QUESTION_CATEGORY_NOT_ROOT_MSG);
            }
        }
        Integer questionId = questionBiz.createQuestion(question);
        Set<String> answerIds = new LinkedHashSet<>();

        // 处理问题的媒体文件引用（使用新的QuestionMedia表）
        if (!CollectionUtils.isEmpty(questionBO.getMediaUrl())) {
            List<QuestionMedia> mediaList = questionBO.getMediaUrl().stream()
                    .map(mediaBO -> {
                        QuestionMedia media = new QuestionMedia();
                        media.setQuestionId(questionId);
                        media.setMediaType(1); // 1-题目媒体
                        media.setMediaName(mediaBO.getMediaName() != null ? mediaBO.getMediaName() : "");
                        media.setMediaPath(mediaBO.getMediaPath());
                        media.setMediaUrl(mediaBO.getMediaUrl());
                        media.setMediaSize(mediaBO.getMediaSize());
                        media.setMediaFormat(mediaBO.getMediaFormat());
                        media.setMediaDuration(mediaBO.getMediaDuration());
                        media.setIsCompressed(mediaBO.getIsCompressed());
                        media.setStorageType(mediaBO.getStorageType());
                        return media;
                    })
                    .collect(Collectors.toList());
            questionMediaBiz.batchSave(mediaList);
        }

        // 处理问题的辅助识图引用（使用新的QuestionMedia表，mediaType=3）
        if (!CollectionUtils.isEmpty(questionBO.getAidedRecognitionUrl())) {
            List<QuestionMedia> recognitionMediaList = questionBO.getAidedRecognitionUrl().stream()
                    .map(recognitionBO -> {
                        QuestionMedia media = new QuestionMedia();
                        media.setQuestionId(questionId);
                        media.setMediaType(3); // 3-辅助识图
                        media.setMediaName(
                                recognitionBO.getMaterialName() != null ? recognitionBO.getMaterialName() : "");
                        media.setMediaPath(recognitionBO.getMaterialPath());
                        // 辅助识图通常只有路径，没有URL，如果需要可以后续扩展
                        return media;
                    })
                    .collect(Collectors.toList());
            questionMediaBiz.batchSave(recognitionMediaList);
        }

        // 处理普通题型的答案
        processNormalAnswers(questionBO, questionId, answerIds);

        // 设置问题的正确答案
        question.setAnswer(String.join(",", answerIds));

        // 处理完形填空题
        processBlankAreas(questionBO, questionId);

        // 更新问题
        questionBiz.updateQuestion(question);
        return questionId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuestion(QuestionBO questionBO) throws ServiceException {
        QuestionValidator.validate(questionBO, questionBO.getAnswers());
        Question question = convertToDomain(questionBO);
        Integer questionId = questionBO.getId();
        Set<String> answerIds = new LinkedHashSet<>();

        // 采用"删除后新增"策略：先删除所有相关的选项和媒体
        // 1. 删除所有选项（包括选项的媒体）
        List<QuestionAnswer> existingAnswers = questionAnswerBiz.getAnswersByQuestionId(questionId);
        if (!CollectionUtils.isEmpty(existingAnswers)) {
            List<Integer> optionIds = existingAnswers.stream()
                    .map(QuestionAnswer::getId)
                    .collect(Collectors.toList());
            // 删除选项的媒体文件
            if (!CollectionUtils.isEmpty(optionIds)) {
                questionMediaBiz.remove(new LambdaQueryWrapper<QuestionMedia>()
                        .in(QuestionMedia::getOptionId, optionIds));
            }
            // 删除选项
            questionAnswerBiz.deleteByQuestionId(List.of(questionId));
        }

        // 2. 删除所有题目相关的媒体文件（题目媒体、辅助识图等）
        questionMediaBiz.deleteByQuestionId(questionId);

        // 3. 删除完形填空区域（包括区域的答案和媒体）
        if (Objects.equals(questionBO.getType(), QuestionTypeEnum.CLOZE.getValue())) {
            questionBlankAreaBiz.deleteByQuestionId(List.of(questionId));
        }

        // 4. 重新保存题目媒体文件
        if (!CollectionUtils.isEmpty(questionBO.getMediaUrl())) {
            List<QuestionMedia> mediaList = questionBO.getMediaUrl().stream()
                    .map(mediaBO -> {
                        QuestionMedia media = new QuestionMedia();
                        media.setQuestionId(questionId);
                        media.setMediaType(1); // 1-题目媒体
                        media.setMediaName(mediaBO.getMediaName() != null ? mediaBO.getMediaName() : "");
                        media.setMediaPath(mediaBO.getMediaPath());
                        media.setMediaUrl(mediaBO.getMediaUrl());
                        media.setMediaSize(mediaBO.getMediaSize());
                        media.setMediaFormat(mediaBO.getMediaFormat());
                        media.setMediaDuration(mediaBO.getMediaDuration());
                        media.setIsCompressed(mediaBO.getIsCompressed());
                        media.setStorageType(mediaBO.getStorageType());
                        return media;
                    })
                    .collect(Collectors.toList());
            questionMediaBiz.batchSave(mediaList);
        }

        // 5. 重新保存辅助识图
        if (!CollectionUtils.isEmpty(questionBO.getAidedRecognitionUrl())) {
            List<QuestionMedia> recognitionMediaList = questionBO.getAidedRecognitionUrl().stream()
                    .map(recognitionBO -> {
                        QuestionMedia media = new QuestionMedia();
                        media.setQuestionId(questionId);
                        media.setMediaType(3); // 3-辅助识图
                        media.setMediaName(
                                recognitionBO.getMaterialName() != null ? recognitionBO.getMaterialName() : "");
                        media.setMediaPath(recognitionBO.getMaterialPath());
                        return media;
                    })
                    .collect(Collectors.toList());
            questionMediaBiz.batchSave(recognitionMediaList);
        }

        // 6. 重新保存所有答案（普通题型）
        if (questionBO.getAnswers() != null) {
            for (QuestionAnswerBO answerBO : questionBO.getAnswers()) {
                QuestionAnswer answer = saveAnswer(answerBO, questionId, null);
                if (Objects.equals(answerBO.getIsAnswer(), YesOrNoEnum.YES.getCode())) {
                    answerIds.add(answer.getId().toString());
                }
                // 保存答案的媒体文件（传入questionId确保不为null）
                processAnswerMediaReferences(answerBO, answer.getId(), questionId);
            }
        }

        // 7. 重新保存完形填空区域
        if (Objects.equals(questionBO.getType(), QuestionTypeEnum.CLOZE.getValue())
                && questionBO.getBlankAreas() != null) {
            processBlankAreas(questionBO, questionId);
        }

        // 8. 设置问题的正确答案
        question.setAnswer(String.join(",", answerIds));

        // 9. 更新问题基本信息
        questionBiz.updateQuestion(question);
    }

    /**
     * 差异化更新普通题目答案
     * 
     * @param questionBO 题目BO
     * @param questionId 题目ID
     * @param answerIds  正确答案ID集合
     */
    private void processNormalAnswersDiff(QuestionBO questionBO, Integer questionId, Set<String> answerIds) {
        if (CollectionUtils.isEmpty(questionBO.getAnswers())) {
            return;
        }
        List<QuestionAnswer> existingAnswers = questionAnswerBiz.getAnswersByQuestionId(questionId);
        Map<Integer, QuestionAnswer> existingAnswerMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(existingAnswers)) {
            for (QuestionAnswer answer : existingAnswers) {
                existingAnswerMap.put(answer.getId(), answer);
            }
        }
        Set<Integer> frontendAnswerIds = new LinkedHashSet<>();
        for (QuestionAnswerBO answerBO : questionBO.getAnswers()) {
            QuestionAnswer answer = new QuestionAnswer();
            BeanUtil.copyProperties(answerBO, answer);
            answer.setQuestionId(questionId);

            if (answerBO.getId() != null) {
                frontendAnswerIds.add(answerBO.getId());
                if (existingAnswerMap.containsKey(answerBO.getId())) {
                    // 更新
                    questionAnswerBiz.updateById(answer);
                    updateAnswerMediaReferences(answerBO, answerBO.getId());
                } else {
                    answer.setId(null);
                    questionAnswerBiz.save(answer);
                    updateAnswerMediaReferences(answerBO, answer.getId());
                }
            } else {
                // 新增
                questionAnswerBiz.save(answer);
                updateAnswerMediaReferences(answerBO, answer.getId());
            }
            if (YesOrNoEnum.YES.getCode().equals(answerBO.getIsAnswer())) {
                answerIds.add(String.valueOf(answer.getId() != null ? answer.getId() : answerBO.getId()));
            }
        }
        for (Integer id : existingAnswerMap.keySet()) {
            if (!frontendAnswerIds.contains(id)) {
                questionAnswerBiz.removeById(id);
                updateAnswerMediaReferences(new QuestionAnswerBO(), id);
            }
        }
    }

    /**
     * 差异化更新完形填空区域
     * 
     * @param questionBO 题目BO
     * @param questionId 题目ID
     */
    private void processBlankAreasDiff(QuestionBO questionBO, Integer questionId) throws ServiceException {
        if (CollectionUtils.isEmpty(questionBO.getBlankAreas())) {
            return;
        }

        // 获取数据库中的现有空位区域
        List<QuestionBlankArea> existingBlankAreas = questionBlankAreaBiz.getBlankAreasByQuestionId(questionId);
        Map<Integer, QuestionBlankArea> existingBlankAreaMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(existingBlankAreas)) {
            for (QuestionBlankArea area : existingBlankAreas) {
                existingBlankAreaMap.put(area.getId(), area);
            }
        }

        // 记录前端传来的空位区域ID
        Set<Integer> frontendBlankAreaIds = new LinkedHashSet<>();

        // 处理前端传来的空位区域
        for (QuestionBlankAreaBO areaBO : questionBO.getBlankAreas()) {
            QuestionBlankArea blankArea = new QuestionBlankArea();
            BeanUtil.copyProperties(areaBO, blankArea);
            blankArea.setQuestionId(questionId);

            if (areaBO.getId() != null) {
                // 前端传来ID不为空，说明是更新
                frontendBlankAreaIds.add(areaBO.getId());
                if (existingBlankAreaMap.containsKey(areaBO.getId())) {
                    // 数据库中存在此ID，进行更新
                    questionBlankAreaBiz.updateById(blankArea);

                    // 处理该空位区域的答案
                    processBlankAreaAnswersDiff(areaBO, areaBO.getId());
                } else {
                    // 数据库中不存在此ID，说明前端ID可能有问题，执行新增
                    blankArea.setId(null);
                    questionBlankAreaBiz.save(blankArea);

                    // 处理新增的空位区域答案
                    if (!CollectionUtils.isEmpty(areaBO.getAnswers())) {
                        List<QuestionAnswer> answers = new ArrayList<>();
                        for (QuestionAnswerBO answerBO : areaBO.getAnswers()) {
                            QuestionAnswer answer = new QuestionAnswer();
                            BeanUtil.copyProperties(answerBO, answer);
                            answer.setQuestionId(questionId);
                            answer.setBlankAreaId(blankArea.getId());
                            answers.add(answer);
                        }
                        questionAnswerBiz.saveBatch(answers);

                        // 设置空位区域的答案IDs
                        List<String> answerIds = answers.stream()
                                .map(answer -> String.valueOf(answer.getId()))
                                .collect(Collectors.toList());
                        blankArea.setAnswerIds(String.join(",", answerIds));
                        questionBlankAreaBiz.updateById(blankArea);
                    }
                }
            } else {
                // 前端传来ID为空，说明是新增
                questionBlankAreaBiz.save(blankArea);

                // 处理新增的空位区域答案
                if (!CollectionUtils.isEmpty(areaBO.getAnswers())) {
                    List<QuestionAnswer> answers = new ArrayList<>();
                    for (QuestionAnswerBO answerBO : areaBO.getAnswers()) {
                        QuestionAnswer answer = new QuestionAnswer();
                        BeanUtil.copyProperties(answerBO, answer);
                        answer.setQuestionId(questionId);
                        answer.setBlankAreaId(blankArea.getId());
                        answers.add(answer);
                    }
                    questionAnswerBiz.saveBatch(answers);

                    // 设置空位区域的答案IDs
                    List<String> answerIds = answers.stream()
                            .map(answer -> String.valueOf(answer.getId()))
                            .collect(Collectors.toList());
                    blankArea.setAnswerIds(String.join(",", answerIds));
                    questionBlankAreaBiz.updateById(blankArea);
                }
            }
        }

        // 删除数据库中有但前端未传的空位区域
        for (Integer id : existingBlankAreaMap.keySet()) {
            if (!frontendBlankAreaIds.contains(id)) {
                // 删除该空位区域的所有答案
                questionAnswerBiz.deleteByBlankAreaId(Collections.singletonList(id));
                // 删除空位区域
                questionBlankAreaBiz.removeById(id);
            }
        }
    }

    /**
     * 差异化更新空位区域的答案
     * 
     * @param areaBO      空位区域BO
     * @param blankAreaId 空位区域ID
     */
    private void processBlankAreaAnswersDiff(QuestionBlankAreaBO areaBO, Integer blankAreaId) throws ServiceException {
        if (CollectionUtils.isEmpty(areaBO.getAnswers())) {
            questionAnswerBiz.deleteByBlankAreaId(Collections.singletonList(blankAreaId));
            return;
        }
        List<QuestionAnswer> existingAnswers = questionAnswerBiz.getAnswersByBlankAreaId(areaBO.getQuestionId(),
                blankAreaId);
        Map<Integer, QuestionAnswer> existingAnswerMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(existingAnswers)) {
            for (QuestionAnswer answer : existingAnswers) {
                existingAnswerMap.put(answer.getId(), answer);
            }
        }
        Set<Integer> frontendAnswerIds = new LinkedHashSet<>();
        List<String> answerIds = new ArrayList<>();
        for (QuestionAnswerBO answerBO : areaBO.getAnswers()) {
            QuestionAnswer answer = new QuestionAnswer();
            BeanUtil.copyProperties(answerBO, answer);
            answer.setBlankAreaId(blankAreaId);

            if (answerBO.getId() != null) {
                frontendAnswerIds.add(answerBO.getId());
                if (existingAnswerMap.containsKey(answerBO.getId())) {
                    questionAnswerBiz.updateById(answer);
                    updateAnswerMediaReferences(answerBO, answerBO.getId());
                    answerIds.add(String.valueOf(answerBO.getId()));
                } else {
                    answer.setId(null);
                    questionAnswerBiz.save(answer);
                    updateAnswerMediaReferences(answerBO, answer.getId());
                    answerIds.add(String.valueOf(answer.getId()));
                }
            } else {
                questionAnswerBiz.save(answer);
                updateAnswerMediaReferences(answerBO, answer.getId());
                answerIds.add(String.valueOf(answer.getId()));
            }
        }
        for (Integer id : existingAnswerMap.keySet()) {
            if (!frontendAnswerIds.contains(id)) {
                questionAnswerBiz.removeById(id);
                updateAnswerMediaReferences(new QuestionAnswerBO(), id);
            }
        }
        QuestionBlankArea blankArea = new QuestionBlankArea();
        blankArea.setId(blankAreaId);
        blankArea.setAnswerIds(String.join(",", answerIds));
        questionBlankAreaBiz.updateById(blankArea);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteQuestion(QuestionIdsBO idsBO) throws ServiceException {
        if (Objects.isNull(idsBO)) {
            return;
        }
        List<Question> questions = questionBiz.getBaseMapper().selectBatchIds(idsBO.getIds());
        if (CollectionUtils.isEmpty(questions)) {
            return;
        }
        boolean hasBusinessRefs = questionCategoryBusinessRefBiz
                .hasBusinessRefs(questions.stream().map(Question::getQuestionCategoryId).collect(Collectors.toList()));
        if (hasBusinessRefs) {
            throw new ServiceException(APP_QUESTION_HAS_REF_MSG);
        }
        // 删除题目的媒体文件（使用新的QuestionMedia表）
        for (Integer questionId : idsBO.getIds()) {
            questionMediaBiz.deleteByQuestionId(questionId);

            // 删除选项的媒体文件
            List<QuestionAnswer> answers = questionAnswerBiz.getAnswersByQuestionId(questionId);
            if (!CollectionUtils.isEmpty(answers)) {
                for (QuestionAnswer answer : answers) {
                    List<QuestionMedia> answerMedia = questionMediaBiz.list(
                            new LambdaQueryWrapper<QuestionMedia>()
                                    .eq(QuestionMedia::getOptionId, answer.getId())
                                    .eq(QuestionMedia::getMediaType, 2));
                    if (!CollectionUtils.isEmpty(answerMedia)) {
                        questionMediaBiz.removeByIds(
                                answerMedia.stream().map(QuestionMedia::getId).collect(Collectors.toList()));
                    }
                }
            }
        }
        questionAnswerBiz.deleteByQuestionId(idsBO.getIds());
        questionBlankAreaBiz.deleteByQuestionId(idsBO.getIds());
        questionBiz.batchDeleteQuestion(idsBO.getIds());
    }

    @Override
    public List<String> getQuestionAnswer(QuestionIdBO idBO) {
        Question question = questionBiz.getQuestionById(idBO.getId());
        if (question == null) {
            return null;
        }
        if (Objects.equals(question.getType(), QuestionTypeEnum.CLOZE.getValue())) {
            List<QuestionBlankArea> areas = questionBlankAreaBiz.getBlankAreasByQuestionId(idBO.getId());
            return areas.stream()
                    .map(QuestionBlankArea::getAnswerIds)
                    .collect(Collectors.toList());
        }

        return Arrays.asList(question.getAnswer().split(","));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchCopyQuestion(QuestionCopyBO copyBO) throws ServiceException {
        List<Question> sourceQuestions = questionBiz.listByIds(copyBO.getQuestionIds());

        for (Question sourceQuestion : sourceQuestions) {
            Question newQuestion = new Question();
            BeanUtil.copyProperties(sourceQuestion, newQuestion);
            newQuestion.setId(null);
            newQuestion.setQuestionCategoryId(copyBO.getTargetCategoryId());
            questionBiz.createQuestion(newQuestion);

            List<QuestionAnswer> sourceAnswers = questionAnswerBiz.getAnswersByQuestionId(sourceQuestion.getId());
            if (!sourceAnswers.isEmpty()) {
                Set<String> answerIds = new LinkedHashSet<>();
                List<QuestionAnswer> newAnswers = sourceAnswers.stream()
                        .map(sourceAnswer -> {
                            QuestionAnswer newAnswer = new QuestionAnswer();
                            BeanUtil.copyProperties(sourceAnswer, newAnswer);
                            newAnswer.setId(null);
                            newAnswer.setQuestionId(newQuestion.getId());
                            questionAnswerBiz.save(newAnswer);
                            // 复制选项的媒体文件
                            List<QuestionMedia> sourceAnswerMedia = questionMediaBiz.list(
                                    new LambdaQueryWrapper<QuestionMedia>()
                                            .eq(QuestionMedia::getOptionId, sourceAnswer.getId())
                                            .eq(QuestionMedia::getMediaType, 2));
                            if (!CollectionUtils.isEmpty(sourceAnswerMedia)) {
                                List<QuestionMedia> newAnswerMedia = sourceAnswerMedia.stream()
                                        .map(sourceMedia -> {
                                            QuestionMedia newMedia = new QuestionMedia();
                                            BeanUtil.copyProperties(sourceMedia, newMedia);
                                            newMedia.setId(null);
                                            newMedia.setOptionId(newAnswer.getId());
                                            return newMedia;
                                        })
                                        .collect(Collectors.toList());
                                questionMediaBiz.batchSave(newAnswerMedia);
                            }
                            return newAnswer;
                        })
                        .toList();
                for (QuestionAnswer newAnswer : newAnswers) {
                    if (Objects.equals(newAnswer.getIsAnswer(), YesOrNoEnum.YES.getCode())) {
                        answerIds.add(newAnswer.getId().toString());
                    }
                }
                newQuestion.setAnswer(String.join(",", answerIds));
                questionBiz.updateById(newQuestion);
            }

            if (Objects.equals(sourceQuestion.getType(), QuestionTypeEnum.CLOZE.getValue())) {
                List<QuestionBlankArea> sourceBlankAreas = questionBlankAreaBiz
                        .getBlankAreasByQuestionId(sourceQuestion.getId());
                if (!sourceBlankAreas.isEmpty()) {
                    for (QuestionBlankArea sourceArea : sourceBlankAreas) {
                        QuestionBlankArea newArea = new QuestionBlankArea();
                        BeanUtil.copyProperties(sourceArea, newArea);
                        newArea.setId(null);
                        newArea.setQuestionId(newQuestion.getId());
                        questionBlankAreaBiz.saveBlankAreas(List.of(newArea));

                        // 复制该空位区域的答案列表
                        List<QuestionAnswer> sourceBlankAnswers = questionAnswerBiz
                                .getAnswersByBlankAreaId(sourceQuestion.getId(), sourceArea.getId());
                        if (!CollectionUtils.isEmpty(sourceBlankAnswers)) {
                            Set<String> answerIds = new LinkedHashSet<>();
                            List<QuestionAnswer> newBlankAnswers = sourceBlankAnswers.stream()
                                    .map(sourceAnswer -> {
                                        QuestionAnswer newAnswer = new QuestionAnswer();
                                        BeanUtil.copyProperties(sourceAnswer, newAnswer);
                                        newAnswer.setId(null);
                                        newAnswer.setQuestionId(newQuestion.getId());
                                        newAnswer.setBlankAreaId(newArea.getId());
                                        return newAnswer;
                                    })
                                    .collect(Collectors.toList());
                            questionAnswerBiz.saveAnswers(newBlankAnswers);
                            for (QuestionAnswer newBlankAnswer : newBlankAnswers) {
                                if (Objects.equals(newBlankAnswer.getIsAnswer(), YesOrNoEnum.YES.getCode())) {
                                    answerIds.add(newBlankAnswer.getId().toString());
                                }
                            }
                            newArea.setAnswerIds(String.join(",", answerIds));
                        }
                        questionBlankAreaBiz.updateById(newArea);
                    }
                }
            }

            // 复制题目的媒体文件
            List<QuestionMedia> sourceMediaList = questionMediaBiz.listByQuestionIdAndType(sourceQuestion.getId(), 1);
            if (!CollectionUtils.isEmpty(sourceMediaList)) {
                List<QuestionMedia> newMediaList = sourceMediaList.stream()
                        .map(sourceMedia -> {
                            QuestionMedia newMedia = new QuestionMedia();
                            BeanUtil.copyProperties(sourceMedia, newMedia);
                            newMedia.setId(null);
                            newMedia.setQuestionId(newQuestion.getId());
                            return newMedia;
                        })
                        .collect(Collectors.toList());
                questionMediaBiz.batchSave(newMediaList);
            }

            // 复制题目的辅助识图（使用新的QuestionMedia表，mediaType=3）
            List<QuestionMedia> sourceRecognitionMedia = questionMediaBiz
                    .listByQuestionIdAndType(sourceQuestion.getId(), 3);
            if (!CollectionUtils.isEmpty(sourceRecognitionMedia)) {
                List<QuestionMedia> newRecognitionMediaList = sourceRecognitionMedia.stream()
                        .map(media -> {
                            QuestionMedia newMedia = new QuestionMedia();
                            BeanUtil.copyProperties(media, newMedia);
                            newMedia.setId(null);
                            newMedia.setQuestionId(newQuestion.getId());
                            return newMedia;
                        })
                        .collect(Collectors.toList());
                questionMediaBiz.batchSave(newRecognitionMediaList);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchMoveQuestion(QuestionMoveBO moveBO) {
        questionBiz.batchUpdateQuestionCategory(moveBO.getQuestionIds(), moveBO.getTargetCategoryId());
    }

    @Override
    public QuestionBlankContentDTO getQuestionBlankContent(QuestionIdBO idBO) throws ServiceException {
        Question question = questionBiz.getQuestionById(idBO.getId());
        if (question == null || !Objects.equals(question.getType(), QuestionTypeEnum.CLOZE.getValue())) {
            return null;
        }

        QuestionBlankContentDTO dto = new QuestionBlankContentDTO();
        dto.setQuestionId(idBO.getId());

        List<QuestionBlankArea> blankAreas = questionBlankAreaBiz.getBlankAreasByQuestionId(idBO.getId());
        if (CollectionUtils.isEmpty(blankAreas)) {
            return dto;
        }
        Map<String, String> answerMap = new HashMap<>();
        Map<String, List<String>> optionsMap = new HashMap<>();
        List<QuestionBlankAreaDTO> blankAreaList = new ArrayList<>();
        for (QuestionBlankArea area : blankAreas) {
            QuestionBlankAreaDTO areaDTO = new QuestionBlankAreaDTO();
            BeanUtil.copyProperties(area, areaDTO);
            blankAreaList.add(areaDTO);
            answerMap.put(String.valueOf(area.getBlankIndex()), area.getAnswerIds());
            List<QuestionAnswer> answers = questionAnswerBiz.getAnswersByBlankAreaId(area.getQuestionId(),
                    area.getId());
            List<String> options = answers.stream()
                    .map(QuestionAnswer::getOptionContent)
                    .collect(Collectors.toList());
            optionsMap.put(String.valueOf(area.getBlankIndex()), options);
        }

        dto.setBlankAnswerMap(answerMap);
        dto.setBlankOptionsMap(optionsMap);
        dto.setBlankAreaList(blankAreaList);

        return dto;
    }

    @SneakyThrows
    @Override
    public Page<QuestionInfoDTO> pageList(QuestionPageBO pageBO) {
        Question condition = new Question();
        BeanUtil.copyProperties(pageBO, condition);
        condition.setQuestionCategoryId(pageBO.getCategoryId() == null ? DEFAULT_FATHER_ID : pageBO.getCategoryId());

        // 从请求参数中获取分页信息，默认值：pageNum=1, pageSize=10
        Integer pageNum = pageBO.getPageNum() != null && pageBO.getPageNum() > 0 ? pageBO.getPageNum() : 1;
        Integer pageSize = pageBO.getPageSize() != null && pageBO.getPageSize() > 0 ? pageBO.getPageSize() : 10;

        Page<Question> page = questionBiz.pageList(
                new Page<>(pageNum, pageSize),
                condition);

        return (Page<QuestionInfoDTO>) page.convert(this::convertToPageDTO);
    }

    private QuestionInfoDTO convertToPageDTO(Question question) {
        if (question == null) {
            return null;
        }
        QuestionInfoDTO dto = new QuestionInfoDTO();
        BeanUtil.copyProperties(question, dto);

        dto.setType(QuestionTypeEnum.getByValue(question.getType()).getValue());
        dto.setSubjectId(question.getSubjectId());
        // 使用字典表获取学科名称（字典类型：subject）
        if (question.getSubjectId() != null) {
            String subjectName = dictDataService.selectDictLabel("subject", String.valueOf(question.getSubjectId()));
            dto.setSubjectName(subjectName != null ? subjectName : "");
        } else {
            dto.setSubjectName("");
        }

        // 获取题目的媒体文件（使用新的QuestionMedia表）
        List<QuestionMedia> questionMediaList = questionMediaBiz.listByQuestionIdAndType(question.getId(), 1);
        if (!CollectionUtils.isEmpty(questionMediaList)) {
            List<QuestionMediaDTO> mediaList = questionMediaList.stream()
                    .map(media -> {
                        QuestionMediaDTO mediaDTO = new QuestionMediaDTO();
                        BeanUtil.copyProperties(media, mediaDTO);
                        // 兼容字段
                        mediaDTO.setMaterialId(media.getId());
                        mediaDTO.setSortNum(0); // 新表结构中没有排序号，使用默认值
                        return mediaDTO;
                    })
                    .collect(Collectors.toList());
            dto.setMediaUrl(mediaList);
        }

        // 获取题目的辅助识图（使用新的QuestionMedia表，mediaType=3）
        List<QuestionMedia> recognitionMediaList = questionMediaBiz.listByQuestionIdAndType(question.getId(), 3);
        if (!CollectionUtils.isEmpty(recognitionMediaList)) {
            List<QuestionRecognitionDTO> recognitionList = recognitionMediaList.stream()
                    .map(media -> {
                        QuestionRecognitionDTO recognition = new QuestionRecognitionDTO();
                        recognition.setMaterialId(media.getId());
                        recognition.setMaterialPath(media.getMediaPath());
                        recognition.setMaterialName(media.getMediaName());
                        recognition.setSortNum(0); // 新表结构中没有排序号，使用默认值
                        return recognition;
                    })
                    .collect(Collectors.toList());
            dto.setAidedRecognitionUrl(recognitionList);
        }

        return dto;
    }

    private QuestionInfoDTO convertToDTO(Question question) {
        if (question == null) {
            return null;
        }
        QuestionInfoDTO dto = new QuestionInfoDTO();
        BeanUtil.copyProperties(question, dto);

        dto.setType(QuestionTypeEnum.getByValue(question.getType()).getValue());
        dto.setSubjectId(question.getSubjectId());
        // 使用字典表获取学科名称（字典类型：subject）
        if (question.getSubjectId() != null) {
            String subjectName = dictDataService.selectDictLabel("subject", String.valueOf(question.getSubjectId()));
            dto.setSubjectName(subjectName != null ? subjectName : "");
        } else {
            dto.setSubjectName("");
        }

        // 获取题目的媒体文件（使用新的QuestionMedia表，包含 mediaType=1 题目媒体 和 mediaType=4 题目音频）
        List<QuestionMedia> questionMediaList = questionMediaBiz.list(
                new LambdaQueryWrapper<QuestionMedia>()
                        .eq(QuestionMedia::getQuestionId, question.getId())
                        .in(QuestionMedia::getMediaType, Arrays.asList(1, 4)));
        if (!CollectionUtils.isEmpty(questionMediaList)) {
            List<QuestionMediaDTO> mediaList = questionMediaList.stream()
                    .map(media -> {
                        QuestionMediaDTO mediaDTO = new QuestionMediaDTO();
                        BeanUtil.copyProperties(media, mediaDTO);
                        // 兼容字段
                        mediaDTO.setMaterialId(media.getId());
                        mediaDTO.setSortNum(0); // 新表结构中没有排序号，使用默认值
                        return mediaDTO;
                    })
                    .collect(Collectors.toList());
            dto.setMediaUrl(mediaList);
        }

        // 获取题目的辅助识图（使用新的QuestionMedia表，mediaType=3）
        List<QuestionMedia> recognitionMediaList = questionMediaBiz.listByQuestionIdAndType(question.getId(), 3);
        if (!CollectionUtils.isEmpty(recognitionMediaList)) {
            List<QuestionRecognitionDTO> recognitionList = recognitionMediaList.stream()
                    .map(media -> {
                        QuestionRecognitionDTO recognition = new QuestionRecognitionDTO();
                        recognition.setMaterialId(media.getId());
                        recognition.setMaterialPath(media.getMediaPath());
                        recognition.setMaterialName(media.getMediaName());
                        recognition.setSortNum(0); // 新表结构中没有排序号，使用默认值
                        return recognition;
                    })
                    .collect(Collectors.toList());
            dto.setAidedRecognitionUrl(recognitionList);
        }

        List<QuestionAnswer> answers = questionAnswerBiz.getAnswersByQuestionId(question.getId());
        if (!CollectionUtils.isEmpty(answers)) {
            List<QuestionAnswerDTO> answerDTOs = answers.stream()
                    .map(answer -> {
                        QuestionAnswerDTO answerDTO = new QuestionAnswerDTO();
                        BeanUtil.copyProperties(answer, answerDTO);

                        // 获取选项的媒体文件（使用新的QuestionMedia表）
                        List<QuestionMedia> answerMediaList = questionMediaBiz.list(
                                new LambdaQueryWrapper<QuestionMedia>()
                                        .eq(QuestionMedia::getOptionId, answer.getId())
                                        .eq(QuestionMedia::getMediaType, 2));
                        if (!CollectionUtils.isEmpty(answerMediaList)) {
                            List<QuestionMediaDTO> mediaDTOList = answerMediaList.stream()
                                    .map(media -> {
                                        QuestionMediaDTO mediaDTO = new QuestionMediaDTO();
                                        BeanUtil.copyProperties(media, mediaDTO);
                                        // 兼容字段
                                        mediaDTO.setMaterialId(media.getId());
                                        mediaDTO.setSortNum(0);
                                        return mediaDTO;
                                    })
                                    .collect(Collectors.toList());
                            answerDTO.setMediaUrl(mediaDTOList);
                        }
                        return answerDTO;
                    })
                    .collect(Collectors.toList());
            dto.setAnswers(answerDTOs);
        }

        // 如果是完形填空题，获取并转换空位区域列表
        if (Objects.equals(question.getType(), QuestionTypeEnum.CLOZE.getValue())) {
            List<QuestionBlankArea> blankAreas = questionBlankAreaBiz.getBlankAreasByQuestionId(question.getId());
            if (!CollectionUtils.isEmpty(blankAreas)) {
                List<QuestionBlankAreaDTO> blankAreaDTOs = new ArrayList<>();
                for (QuestionBlankArea area : blankAreas) {
                    QuestionBlankAreaDTO areaDTO = new QuestionBlankAreaDTO();
                    BeanUtil.copyProperties(area, areaDTO);

                    // 获取并转换该空位区域的答案列表
                    List<QuestionAnswer> blankAnswers = questionAnswerBiz.getAnswersByBlankAreaId(area.getQuestionId(),
                            area.getId());
                    if (!CollectionUtils.isEmpty(blankAnswers)) {
                        List<QuestionAnswerDTO> blankAnswerDTOs = blankAnswers.stream()
                                .map(answer -> {
                                    QuestionAnswerDTO answerDTO = new QuestionAnswerDTO();
                                    BeanUtil.copyProperties(answer, answerDTO);
                                    return answerDTO;
                                })
                                .collect(Collectors.toList());
                        areaDTO.setAnswers(blankAnswerDTOs);
                    }
                    blankAreaDTOs.add(areaDTO);
                }
                dto.setBlankAreas(blankAreaDTOs);
            }
        }

        // 填空题：从 answer 字段解析 blankAnswers
        if (Objects.equals(question.getType(), QuestionTypeEnum.FILL_BLANK.getValue())) {
            if (question.getAnswer() != null && !question.getAnswer().isEmpty()) {
                dto.setBlankAnswers(Arrays.asList(question.getAnswer().split("[,，;；]")));
            } else {
                dto.setBlankAnswers(new ArrayList<>());
            }
        }

        // 作文题：设置额外字段
        if (Objects.equals(question.getType(), QuestionTypeEnum.ESSAY.getValue())) {
            dto.setWordLimit(question.getWordLimit());
            dto.setSampleAnswer(question.getSampleAnswer());
            // requirements 字段是 JSON 数组格式存储
            if (question.getRequirements() != null && !question.getRequirements().isEmpty()) {
                try {
                    // 尝试解析 JSON 数组
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    List<String> reqList = mapper.readValue(question.getRequirements(),
                            mapper.getTypeFactory().constructCollectionType(List.class, String.class));
                    dto.setRequirements(reqList);
                } catch (Exception e) {
                    // 解析失败则作为单个字符串处理
                    dto.setRequirements(Arrays.asList(question.getRequirements()));
                }
            } else {
                dto.setRequirements(new ArrayList<>());
            }
        }

        return dto;
    }

    private Question convertToDomain(QuestionBO questionBO) {
        if (questionBO == null) {
            return null;
        }

        Question question = new Question();
        BeanUtil.copyProperties(questionBO, question);

        question.setType(questionBO.getType());

        if (questionBO.getAnswers() != null) {
            List<QuestionAnswer> answers = questionBO.getAnswers().stream()
                    .map(answerDTO -> {
                        QuestionAnswer answer = new QuestionAnswer();
                        BeanUtil.copyProperties(answerDTO, answer);
                        return answer;
                    })
                    .collect(Collectors.toList());
            question.setAnswers(answers);
        }

        if (questionBO.getBlankAreas() != null) {
            List<QuestionBlankArea> blankAreas = questionBO.getBlankAreas().stream()
                    .map(areaDTO -> {
                        QuestionBlankArea area = new QuestionBlankArea();
                        BeanUtil.copyProperties(areaDTO, area);
                        if (!CollectionUtils.isEmpty(areaDTO.getAnswers())) {
                            List<QuestionAnswer> blankAnswers = areaDTO.getAnswers().stream()
                                    .map(answerDTO -> {
                                        QuestionAnswer answer = new QuestionAnswer();
                                        BeanUtil.copyProperties(answerDTO, answer);
                                        return answer;
                                    })
                                    .collect(Collectors.toList());
                            area.setAnswers(blankAnswers);
                        }
                        return area;
                    })
                    .collect(Collectors.toList());
            question.setBlankAreas(blankAreas);
        }

        // 填空题：将 blankAnswers 转换为逗号分隔的 answer
        if (Objects.equals(questionBO.getType(), QuestionTypeEnum.FILL_BLANK.getValue())) {
            if (questionBO.getBlankAnswers() != null && !questionBO.getBlankAnswers().isEmpty()) {
                question.setAnswer(String.join(",", questionBO.getBlankAnswers()));
            }
        }

        // 作文题：处理额外字段
        if (Objects.equals(questionBO.getType(), QuestionTypeEnum.ESSAY.getValue())) {
            question.setWordLimit(questionBO.getWordLimit());
            question.setSampleAnswer(questionBO.getSampleAnswer());
            // requirements 转换为 JSON 数组存储
            if (questionBO.getRequirements() != null && !questionBO.getRequirements().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    question.setRequirements(mapper.writeValueAsString(questionBO.getRequirements()));
                } catch (Exception e) {
                    // 序列化失败则使用逗号分隔
                    question.setRequirements(String.join(",", questionBO.getRequirements()));
                }
            }
            // 作文题的答案就是参考范文
            if (questionBO.getSampleAnswer() != null) {
                question.setAnswer(questionBO.getSampleAnswer());
            }
        }

        return question;
    }
}