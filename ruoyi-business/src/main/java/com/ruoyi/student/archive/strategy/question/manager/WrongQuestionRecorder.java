package com.ruoyi.student.archive.strategy.question.manager;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.enums.YesOrNoEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.domain.bo.paper.SubmitBlankResultBO;
import com.ruoyi.student.archive.domain.bo.paper.SubmitPaperBO;
import com.ruoyi.student.archive.domain.bo.paper.SubmitQuestionResultBO;
import com.ruoyi.student.archive.domain.bo.question.AddWrongQuestionBO;
import com.ruoyi.student.archive.domain.bo.question.WrongQuestionBlankBO;
import com.ruoyi.student.archive.service.question.WrongQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class WrongQuestionRecorder {
    @Resource
    private WrongQuestionService wrongQuestionService;

    public void record(SubmitPaperBO submitBO, Integer sourceId, Integer sourceType) throws ServiceException {
        saveWrongQuestion(submitBO,sourceId,sourceType);
    }


    private void saveWrongQuestion(SubmitPaperBO submitBO,Integer sourceId,Integer sourceType) throws ServiceException {
        List<SubmitQuestionResultBO> normalResults = submitBO.getQuestionResults().stream().filter(e -> e.getResult().equals(YesOrNoEnum.NO.getCode())).toList();
        List<SubmitBlankResultBO> blankResults = submitBO.getBlankResults().stream().filter(e -> e.getResult().equals(YesOrNoEnum.NO.getCode())).collect(Collectors.toList());
        List<AddWrongQuestionBO> addWrongQuestionBOList = new ArrayList<>();
        for (SubmitQuestionResultBO questionResult : normalResults) {
            AddWrongQuestionBO wrongQuestionBO = new AddWrongQuestionBO();
            wrongQuestionBO.setSourceId(sourceId);
            wrongQuestionBO.setSourceType(sourceType);
            wrongQuestionBO.setAppUserId(submitBO.getAppUserId());
            wrongQuestionBO.setQuestionId(questionResult.getQuestionId());
            wrongQuestionBO.setUserAnswer(questionResult.getUserAnswer());
            wrongQuestionBO.setUserAnswerIds(questionResult.getAnswerIds());

            addWrongQuestionBOList.add(wrongQuestionBO);
        }
        if(CollectionUtils.isNotEmpty(blankResults)){
            AddWrongQuestionBO wrongQuestionBO = new AddWrongQuestionBO();
            wrongQuestionBO.setAppUserId(submitBO.getAppUserId());
            List<WrongQuestionBlankBO> wrongQuestionBlankBOS = new ArrayList<>();
            for (SubmitBlankResultBO blankResult : blankResults) {
                WrongQuestionBlankBO wrongQuestionBlankBO = new WrongQuestionBlankBO();
                wrongQuestionBlankBO.setBlankAreaId(blankResult.getBlankAreaId());
                wrongQuestionBlankBO.setQuestionId(blankResult.getQuestionId());
                wrongQuestionBlankBO.setBlankIndex(blankResult.getBlankIndex());
                wrongQuestionBlankBO.setUserAnswer(blankResult.getAnswerIds());
                wrongQuestionBlankBO.setUserAnswerIds(blankResult.getAnswerIds());
                wrongQuestionBlankBOS.add(wrongQuestionBlankBO);
            }
            wrongQuestionBO.setBlankResults(wrongQuestionBlankBOS);
            addWrongQuestionBOList.add(wrongQuestionBO);
        }
        if(CollectionUtils.isNotEmpty(addWrongQuestionBOList)){
            log.info("开始批量增加错题：{}", JSON.toJSONString(addWrongQuestionBOList));
            wrongQuestionService.batchAddWrongQuestions(addWrongQuestionBOList);
        }
    }
}