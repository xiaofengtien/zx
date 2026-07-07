package com.zx.student.archive.biz.question.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zx.student.archive.biz.question.QuestionBusinessRefBiz;
import com.zx.student.archive.domain.bo.question.QuestionBusinessRefPageBO;
import com.zx.student.archive.domain.dto.question.QuestionBusinessRefDTO;
import com.zx.student.archive.domain.question.QuestionBusinessRef;
import com.zx.student.archive.mapper.question.QuestionBusinessRefMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 题目业务实现类
 */
@Slf4j
@Service
public class QuestionBusinessRefBizImpl extends ServiceImpl<QuestionBusinessRefMapper, QuestionBusinessRef> implements QuestionBusinessRefBiz {

    @Resource
    private QuestionBusinessRefMapper questionBusinessRefMapper;

    @Override
    public Page<QuestionBusinessRefDTO> pageList(Page<QuestionBusinessRefDTO> page, QuestionBusinessRefPageBO condition) {
        return questionBusinessRefMapper.pageList(page,condition);
    }

    @Override
    public Integer getMaxSortNum(QuestionBusinessRefPageBO questionBusinessRefPageBO) {
        return questionBusinessRefMapper.getMaxSortNum(questionBusinessRefPageBO);
    }
}