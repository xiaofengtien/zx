package com.ruoyi.student.archive.strategy.question.manager;

// ... (导入你需要的 BO, Entity, Biz, Mapper)

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.student.archive.domain.bo.paper.QueryPaperQuestionsBO;
import com.ruoyi.common.constant.AppErrorCode;
import com.ruoyi.common.enums.question.QuestionMethodEnum;
import com.ruoyi.common.enums.question.QuestionTypeEnum;
import com.ruoyi.student.archive.biz.question.IQuestionCategoryBusinessRefBiz;
import com.ruoyi.student.archive.biz.question.QuestionBiz;
import com.ruoyi.student.archive.biz.question.QuestionCategoryBiz;
import com.ruoyi.student.archive.domain.question.*;
import com.ruoyi.student.archive.mapper.question.QuestionCategoryBusinessSettingsMapper;
import com.ruoyi.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.ruoyi.common.constant.AppErrorCode.APP_GET_QUESTION_FAIL_CODE;
import static com.ruoyi.common.constant.AppErrorCode.APP_GET_QUESTION_FAIL_MSG;

@Component
@Slf4j
public class QuestionLoaderService {
    @Resource
    private QuestionBiz questionBiz;
    @Resource
    private QuestionCategoryBusinessSettingsMapper questionSettingMapper;
    @Resource
    private IQuestionCategoryBusinessRefBiz categoryBusinessRefBiz;
    @Resource
    private QuestionCategoryBiz questionCategoryBiz;

    public List<Question> loadForEvaluation(QueryPaperQuestionsBO queryBO,List<QuestionBusinessRef> questionBusinessRefList) throws ServiceException {
        try {
            Set<Integer> questionIdSet = questionBusinessRefList.stream().map(QuestionBusinessRef::getQuestionId).collect(Collectors.toSet());
            //查询所有题目信息
            List<Question> allQuestions = questionBiz.listByIds(questionIdSet);
            if (CollectionUtils.isEmpty(allQuestions)) {
                return Collections.emptyList();
            }
            allQuestions.sort((q1, q2) -> {
                int type1Order = QuestionTypeEnum.getOrderByValue(q1.getType());
                int type2Order = QuestionTypeEnum.getOrderByValue(q2.getType());
                return Integer.compare(type1Order, type2Order);
            });
            return allQuestions;
        }catch (Exception e) {
            log.error("根据业务信息获取题目失败", e);
            throw new ServiceException(AppErrorCode.APP_GET_QUESTION_FAIL_MSG);
        }

    }

    public List<Question> loadForQuestionSettings(QueryPaperQuestionsBO queryBO) throws ServiceException {
        QuestionCategoryBusinessSettings questionSetting = questionSettingMapper.selectOne(new LambdaQueryWrapper<QuestionCategoryBusinessSettings>()
                .eq(QuestionCategoryBusinessSettings::getBusinessId,queryBO.getBusinessId())
                .eq(QuestionCategoryBusinessSettings::getBusinessType,queryBO.getBusinessType()));
        if (questionSetting == null) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_SETTING_NOT_EXIST_MSG);
        }
        return queryQuestionCategoryBusinessRef(questionSetting.getId(),questionSetting.getBusinessType(), questionSetting.getQuestionNum(), questionSetting.getQuestionMethod());
    }


    public List<Question> queryQuestionCategoryBusinessRef(Integer businessId, Integer businessType, int totalNeeded, Integer questionMethod) throws ServiceException {
        List<QuestionCategoryBusinessRef> categoryRefs = categoryBusinessRefBiz.getRefsByBusinessId(
                businessId,
                businessType
        );
        if (CollectionUtils.isEmpty(categoryRefs)) {
            return Collections.emptyList();
        }
        List<Question> selectedQuestions = new LinkedList<>();
        for (QuestionCategoryBusinessRef categoryRef : categoryRefs) {
            if (selectedQuestions.size() >= totalNeeded) {
                break;
            }
            QuestionCategory category = questionCategoryBiz.getBaseMapper().selectById(categoryRef.getQuestionCategoryId());
            if (category == null) {
                continue;
            }
            List<Question> questions = questionBiz.getAllQuestionsByCategory(category.getId());
            if (Objects.equals(questionMethod, QuestionMethodEnum.RANDOM.getValue())) {
                Collections.shuffle(questions);
            }
            int remainingNeeded = totalNeeded - selectedQuestions.size();
            int toTake = Math.min(remainingNeeded, questions.size());
            for (int i = 0; i < toTake; i++) {
                selectedQuestions.add(questions.get(i));
            }
        }
        return selectedQuestions;
    }



}