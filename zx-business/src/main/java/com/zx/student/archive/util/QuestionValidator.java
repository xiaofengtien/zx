package com.zx.student.archive.util;

import com.zx.common.constant.AppErrorCode;
import com.zx.student.archive.domain.bo.question.QuestionAnswerBO;
import com.zx.student.archive.domain.bo.question.QuestionBO;
import com.zx.student.archive.domain.bo.question.QuestionBlankAreaBO;
import com.zx.student.archive.domain.bo.question.QuestionMediaBO;
import com.zx.common.enums.question.OptionTypeEnum;
import com.zx.common.enums.question.QuestionTypeEnum;
import com.zx.common.exception.ServiceException;
import com.zx.common.enums.YesOrNoEnum;
import com.zx.common.utils.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

import static com.zx.common.constant.AppErrorCode.*;

/**
 * 题目校验器
 */
public class QuestionValidator {

    /**
     * 选项最大数量
     */
    private static final int MAX_OPTION_COUNT = 8;

    /**
     * 排序题最小选项数量
     */
    private static final int MIN_SORT_OPTION_COUNT = 2;

    /**
     * 完形填空每个空位的最小选项数
     */
    private static final int MIN_CLOZE_OPTIONS_PER_BLANK = 2;

    /**
     * 完形填空每个空位的最大选项数
     */
    private static final int MAX_CLOZE_OPTIONS_PER_BLANK = 8;

    /**
     * 校验题目
     */
    public static void validate(QuestionBO question, List<QuestionAnswerBO> answers) throws ServiceException {
        if (Objects.isNull(question)) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_NOT_NULL_MSG);
        }

        // 校验题目类型
        if (Objects.isNull(question.getType())) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_TYPE_NOT_NULL_MSG);
        }
        if (Objects.isNull(question.getMediaType())) {
            throw new ServiceException(AppErrorCode.APP_ANSWER_MEDIA_TYPE_FAIL_MSG);
        }
        // 根据题目类型进行校验
        QuestionTypeEnum type = QuestionTypeEnum.getByValue(question.getType());
        switch (type) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE -> validateChoiceQuestion(question, answers);
            case FILL_BLANK -> validateFillBlankQuestion(answers);
            case CLOZE -> validateClozeQuestion(question, answers);
            case TRUE_FALSE -> validateTrueFalseQuestion(answers);
            case SORT -> validateSortQuestion(question,answers);
            default -> throw new ServiceException(APP_QUESTION_TYPE_NOT_SUPPORT_MSG);
        }
    }

    /**
     * 校验选择题
     */
    private static void validateChoiceQuestion(QuestionBO question, List<QuestionAnswerBO> answers) throws ServiceException {
        if(question.getOptionType().equals(OptionTypeEnum.TEXT.getCode()) && answers.stream().anyMatch(e -> e.getOptionContent() == null)){
            throw new ServiceException(APP_ANSWER_OPTION_CONTENT_FAIL_MSG);
        }
        // 校验选项数量
        if (CollectionUtils.isEmpty(answers)) {
            throw new ServiceException(APP_QUESTION_CHOICE_NOT_NULL_MSG);
        }

        if (answers.size() > MAX_OPTION_COUNT) {
            throw new ServiceException(APP_QUESTION_CHOICE_COUNT_ERROR_MSG);
        }

        // 校验每个选项
        for (QuestionAnswerBO answer : answers) {
            // 校验选项内容
            if (StringUtils.isEmpty(answer.getOptionName())) {
                throw new ServiceException(APP_QUESTION_CONTENT_ERROR_MSG);
            }

            // 校验选项类型
            validateOptionType(answer, question.getMediaUrl());
        }

        // 校验答案
        validateChoiceAnswer(question, answers);
    }

    /**
     * 校验填空题
     */
    private static void validateFillBlankQuestion(List<QuestionAnswerBO> answers) throws ServiceException {
        // 校验答案
        if (CollectionUtils.isEmpty(answers)) {
            throw new ServiceException(APP_QUESTION_FILL_BLANK_NOT_NULL_MSG);
        }

        // 校验每个答案
        for (QuestionAnswerBO answer : answers) {
            if (StringUtils.isEmpty(answer.getOptionContent())) {
                throw new ServiceException(APP_QUESTION_FILL_BLANK_NOT_NULL_MSG);
            }
        }
    }

    /**
     * 校验完形填空
     */
    private static void validateClozeQuestion(QuestionBO question, List<QuestionAnswerBO> answers) throws ServiceException {
        if (CollectionUtils.isEmpty(question.getBlankAreas())) {
            throw new ServiceException(APP_QUESTION_CLOZE_BLANK_NOT_NULL_MSG);
        }

        // 校验空位序号是否连续
        for (int i = 0; i < question.getBlankAreas().size(); i++) {
            if (!Objects.equals(question.getBlankAreas().get(i).getBlankIndex(), i + 1)) {
                throw new ServiceException(APP_QUESTION_CLOZE_BLANK_ORDER_ERROR_MSG);
            }
        }

        // 校验每个空位的答案选项
        for (QuestionBlankAreaBO blankArea : question.getBlankAreas()) {
            List<QuestionAnswerBO> blankAnswers = blankArea.getAnswers();

            // 校验选项数量
            if (CollectionUtils.isEmpty(blankArea.getAnswers())) {
                throw new ServiceException(String.format("第%d个空位没有答案选项", blankArea.getBlankIndex()));
            }
            if (blankAnswers.size() < MIN_CLOZE_OPTIONS_PER_BLANK || blankAnswers.size() > MAX_CLOZE_OPTIONS_PER_BLANK) {
                throw new ServiceException(String.format("第%d个空位的选项数量必须在%d到%d之间",
                    blankArea.getBlankIndex(), MIN_CLOZE_OPTIONS_PER_BLANK, MAX_CLOZE_OPTIONS_PER_BLANK));
            }

            // 校验每个选项
            for (QuestionAnswerBO answer : blankAnswers) {
                // 校验选项内容
                if (StringUtils.isEmpty(answer.getOptionContent())) {
                    throw new ServiceException(APP_QUESTION_CLOZE_NOT_NULL_MSG);
                }
                if (Objects.isNull(answer.getIsAnswer())) {
                    throw new ServiceException(String.format("第%d个空位未设置正确答案", blankArea.getBlankIndex()));
                }
                // 校验选项类型
                validateOptionType(answer, question.getMediaUrl());
            }
        }
    }

    /**
     * 校验判断题
     */
    private static void validateTrueFalseQuestion(List<QuestionAnswerBO> answers) throws ServiceException {
        // 校验答案
        if (CollectionUtils.isEmpty(answers) || answers.stream().allMatch(e -> e.getIsAnswer() == null)) {
            throw new ServiceException(APP_QUESTION_TRUE_FALSE_ONLY_ONE_MSG);
        }

        QuestionAnswerBO answer = answers.get(0);
        if (StringUtils.isEmpty(answer.getOptionContent())) {
            throw new ServiceException(APP_QUESTION_TRUE_FALSE_NOT_NULL_MSG);
        }

        // 校验答案内容是否为对/错
        String content = answer.getOptionContent().trim();
        if (!"对".equals(content) && !"错".equals(content)) {
            throw new ServiceException(APP_QUESTION_TRUE_FALSE_ERROR_MSG);
        }
    }

    /**
     * 校验排序题
     */
    private static void validateSortQuestion(QuestionBO question,List<QuestionAnswerBO> answers) throws ServiceException {
        if(question.getOptionType().equals(OptionTypeEnum.TEXT.getCode()) && answers.stream().anyMatch(e -> e.getOptionContent() == null)){
            throw new ServiceException(APP_ANSWER_OPTION_CONTENT_FAIL_MSG);
        }
        // 校验选项数量
        if (CollectionUtils.isEmpty(answers)) {
            throw new ServiceException(APP_QUESTION_SORT_NOT_NULL_MSG);
        }
        if (answers.size() < MIN_SORT_OPTION_COUNT || answers.size() > MAX_OPTION_COUNT) {
            throw new ServiceException(APP_QUESTION_SORT_COUNT_ERROR_MSG);
        }

        // 校验每个选项
        for (QuestionAnswerBO answer : answers) {
            // 校验选项内容
            boolean textValid = question.getOptionType().equals(OptionTypeEnum.TEXT.getCode()) &&(StringUtils.isEmpty(answer.getOptionName()) && StringUtils.isEmpty(answer.getOptionContent()));
            boolean mediaValid = !question.getOptionType().equals(OptionTypeEnum.TEXT.getCode()) &&(CollectionUtils.isEmpty(answer.getMediaUrl()));
            if (textValid || mediaValid){
                throw new ServiceException(APP_QUESTION_CONTENT_ERROR_MSG);
            }

            // 校验选项类型
            validateOptionType(answer, null);
        }

        // 校验序号是否连续
        validateSortNumbers(answers);
    }

    /**
     * 校验选择题答案
     */
    private static void validateChoiceAnswer(QuestionBO question, List<QuestionAnswerBO> answers) throws ServiceException {
        // 获取正确答案数量
        long correctCount = answers.stream()
                .filter(answer -> YesOrNoEnum.YES.getCode().equals(answer.getIsAnswer()))
                .count();

        // 单选题只能有一个正确答案
        if (Objects.equals(QuestionTypeEnum.SINGLE_CHOICE.getValue(), question.getType())) {
            if (correctCount != 1) {
                throw new ServiceException(APP_QUESTION_SINGLE_CHOICE_ERROR_MSG);
            }
        }
        // 多选题至少要有一个正确答案
        else if (Objects.equals(QuestionTypeEnum.MULTIPLE_CHOICE.getValue(), question.getType())) {
            if (correctCount == 0) {
                throw new ServiceException(APP_QUESTION_MULTIPLE_CHOICE_ERROR_MSG);
            }
        }
    }

    /**
     * 校验选项类型
     */
    private static void validateOptionType(QuestionAnswerBO answer, List<QuestionMediaBO> mediaUrls) throws ServiceException {
        // 如果有媒体URL，检查是否只有一个
        if (mediaUrls != null && !mediaUrls.isEmpty()) {
            if (mediaUrls.size() > 1) {
                throw new ServiceException(APP_QUESTION_OPTION_IMAGE_ERROR_MSG);
            }
        }
    }

    /**
     * 校验排序题序号
     */
    private static void validateSortNumbers(List<QuestionAnswerBO> answers) throws ServiceException {
        // 检查序号是否从1开始连续
        for (int i = 0; i < answers.size(); i++) {
            if (!Objects.equals(answers.get(i).getSerialNo(), i + 1)) {
                throw new ServiceException(APP_QUESTION_SORT_ORDER_ERROR_MSG);
            }
        }
    }
} 