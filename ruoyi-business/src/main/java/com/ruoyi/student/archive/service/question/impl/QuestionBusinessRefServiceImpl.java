package com.ruoyi.student.archive.service.question.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.student.archive.biz.question.QuestionBusinessRefBiz;
import com.ruoyi.student.archive.domain.bo.question.QuestionBusinessRefPageBO;
import com.ruoyi.student.archive.domain.dto.question.QuestionBusinessRefDTO;
import com.ruoyi.student.archive.service.question.QuestionBusinessRefService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class QuestionBusinessRefServiceImpl implements QuestionBusinessRefService {
    @Resource
    private QuestionBusinessRefBiz questionBusinessRefBiz;

    @Override
    public Page<QuestionBusinessRefDTO> pageList(QuestionBusinessRefPageBO pageBO) {
        // TODO: 需要从请求参数中获取分页信息，暂时使用默认值
        Page<QuestionBusinessRefDTO> page = new Page<>(1, 10);
        return questionBusinessRefBiz.pageList(page, pageBO);
    }

    @Override
    public Integer getMaxSortNum(QuestionBusinessRefPageBO questionBusinessRefPageBO) {
        return questionBusinessRefBiz.getMaxSortNum(questionBusinessRefPageBO);
    }
}