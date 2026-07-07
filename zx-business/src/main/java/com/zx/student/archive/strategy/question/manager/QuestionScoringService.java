package com.zx.student.archive.strategy.question.manager;

import com.zx.common.enums.YesOrNoEnum;
import com.zx.common.enums.question.QuestionTypeEnum;
import com.zx.student.archive.biz.question.QuestionAnswerBiz;
import com.zx.student.archive.biz.question.QuestionBiz;
import com.zx.student.archive.domain.bo.paper.SubmitBlankResultBO;
import com.zx.student.archive.domain.bo.paper.SubmitPaperBO;
import com.zx.student.archive.domain.bo.paper.SubmitQuestionResultBO;
import com.zx.student.archive.domain.question.Question;
import com.zx.student.archive.domain.question.QuestionAnswer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class QuestionScoringService {
    @Resource
    protected QuestionAnswerBiz questionAnswerBiz;
    @Resource
    protected QuestionBiz questionBiz;
    
    public void score(SubmitPaperBO submitBO, Map<Integer, Question> questionMap) {
        scoreNormalQuestions(submitBO.getQuestionResults(), questionMap);
        scoreBlankQuestions(submitBO.getBlankResults(), questionMap);
    }
    
    private void scoreNormalQuestions(List<SubmitQuestionResultBO> results, Map<Integer, Question> questionMap) {
        scoreQuestionResults(results,questionMap);
    }
    
    private void scoreBlankQuestions(List<SubmitBlankResultBO> results, Map<Integer, Question> questionMap) {
        validateBlankAnswer(results, questionMap.keySet());

    }

    private void scoreQuestionResults(List<SubmitQuestionResultBO> results, Map<Integer, Question> questionMap) {
        if (CollectionUtils.isEmpty(results)) {
            return;
        }

        for (SubmitQuestionResultBO resultBO : results) {
            Question question = questionMap.get(resultBO.getQuestionId());
            if (question == null) {
                continue;
            }

            boolean isCorrect = validateQuestionAnswer(question, resultBO.getAnswerIds(), resultBO.getUserAnswer());
            resultBO.setResult(isCorrect ? YesOrNoEnum.YES.getCode() : YesOrNoEnum.NO.getCode());
        }
    }

    private boolean validateQuestionAnswer(Question question, String userAnswerIds, String userAnswer) {
        if (question == null) {
            return false;
        }
        List<String> correctAnswerIds =null;
        List<String> userAnswerList = null;
        if(StringUtils.isNoneEmpty(question.getAnswer())){
            correctAnswerIds = Arrays.asList(question.getAnswer().split(","));
        }
        if(StringUtils.isNoneEmpty(userAnswerIds)){
            userAnswerList = Arrays.asList(userAnswerIds.split(","));
        }

        if(CollectionUtils.isNotEmpty(correctAnswerIds)
                && CollectionUtils.isNotEmpty(userAnswerList)){
            if (QuestionTypeEnum.SINGLE_CHOICE.getValue().equals(question.getType())
                    || QuestionTypeEnum.TRUE_FALSE.getValue().equals(question.getType())
                    || QuestionTypeEnum.SORT.getValue().equals(question.getType())) {
                return correctAnswerIds.equals(userAnswerList);
            } else if (QuestionTypeEnum.MULTIPLE_CHOICE.getValue().equals(question.getType())) {
                return new LinkedHashSet<>(correctAnswerIds).equals(new LinkedHashSet<>(userAnswerList));
            }
        }
        if (QuestionTypeEnum.FILL_BLANK.getValue().equals(question.getType()) && CollectionUtils.isNotEmpty(correctAnswerIds)) {
            if (userAnswer == null) {
                return false;
            }

            List<QuestionAnswer> questionAnswers = questionAnswerBiz.getAnswersByQuestionId(question.getId());
            if (questionAnswers == null || questionAnswers.isEmpty()) {
                return false;
            }

            Map<String, String> idToContentMap = new HashMap<>();
            for (QuestionAnswer qa : questionAnswers) {
                idToContentMap.put(qa.getId().toString(), qa.getOptionContent());
            }

            String[] userAnswers = userAnswer.split(",");
            String[] answerIdGroups = correctAnswerIds.toArray(new String[0]);

            if (userAnswers.length != answerIdGroups.length) {
                return false;
            }

            for (int i = 0; i < answerIdGroups.length; i++) {
                String userAns = userAnswers[i].trim();

                String answerId = answerIdGroups[i];
                String acceptableAnswersStr = idToContentMap.get(answerId);

                if (acceptableAnswersStr == null) {
                    return false;
                }

                String[] acceptableAnswers = acceptableAnswersStr.split("\\|");

                boolean matchFound = false;
                for (String acceptable : acceptableAnswers) {
                    if (userAns.equals(acceptable.trim())) {
                        matchFound = true;
                        break;
                    }
                }

                if (!matchFound) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * 评分填空题
     * 此实现将同一题目下所有空位的答案收集成一个大的集合，然后与正确答案的集合进行一次比较
     */
    private void validateBlankAnswer(List<SubmitBlankResultBO> results, Set<Integer> questionIds) {
        if (CollectionUtils.isEmpty(results)) {
            return;
        }

        Map<Integer, Map<Integer, List<QuestionAnswer>>> blankAreaAnswerMap = questionBiz.getBlankAreaAnswerMap(questionIds);


        // 按题目ID分组
        Map<Integer, List<SubmitBlankResultBO>> resultsByQuestion = results.stream()
                .collect(Collectors.groupingBy(SubmitBlankResultBO::getQuestionId));

        for (Map.Entry<Integer, List<SubmitBlankResultBO>> entry : resultsByQuestion.entrySet()) {
            Integer questionId = entry.getKey();
            List<SubmitBlankResultBO> questionResults = entry.getValue();



            // 按blankIndex排序
            questionResults.sort(Comparator.comparing(SubmitBlankResultBO::getBlankIndex));

            // 收集用户所有空位的答案ID
            Set<String> allUserAnswers = new LinkedHashSet<>();
            for (SubmitBlankResultBO blankBO : questionResults) {
                String userAnswerIds = blankBO.getAnswerIds();
                if (StringUtils.isNotEmpty(userAnswerIds)) {
                    Arrays.stream(userAnswerIds.split(","))
                            .map(String::trim)
                            .filter(id -> !id.isEmpty())
                            .forEach(allUserAnswers::add);
                }
            }
            // 获取该题目的所有空位区域
            Map<Integer, List<QuestionAnswer>> questionBlankAreaMap = blankAreaAnswerMap.getOrDefault(questionId, Collections.emptyMap());
            if (questionBlankAreaMap == null || questionBlankAreaMap.isEmpty()) {
                continue;
            }
            // 收集所有空位的正确答案ID
            Set<String> allCorrectAnswers = new LinkedHashSet<>();
            for (SubmitBlankResultBO blankBO : questionResults) {
                List<QuestionAnswer> answers = questionBlankAreaMap.getOrDefault(blankBO.getBlankAreaId(), Collections.emptyList());
                if (!CollectionUtils.isEmpty(answers)) {
                    allCorrectAnswers.addAll(answers.stream()
                            .filter(answer -> YesOrNoEnum.YES.getCode().equals(answer.getIsAnswer()))
                            .map(e -> String.valueOf(e.getId()))
                            .collect(Collectors.toSet()));

                }
            }

            // 判断用户答案集合与正确答案集合是否完全匹配
            boolean allCorrect = !allUserAnswers.isEmpty() && !allCorrectAnswers.isEmpty()
                    && allUserAnswers.equals(allCorrectAnswers);

            // 设置该题目下所有空位的结果
            Integer result = allCorrect ? YesOrNoEnum.YES.getCode() : YesOrNoEnum.NO.getCode();
            for (SubmitBlankResultBO blankBO : questionResults) {
                blankBO.setResult(result);
            }
        }
    }
}