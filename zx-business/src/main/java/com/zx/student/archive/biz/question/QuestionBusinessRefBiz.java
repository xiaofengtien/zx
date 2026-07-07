package com.zx.student.archive.biz.question;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zx.student.archive.domain.bo.question.QuestionBusinessRefPageBO;
import com.zx.student.archive.domain.dto.question.QuestionBusinessRefDTO;
import com.zx.student.archive.domain.question.QuestionBusinessRef;

/**
 * 题目基础操作接口
 */
public interface QuestionBusinessRefBiz extends IService<QuestionBusinessRef> {
    Page<QuestionBusinessRefDTO> pageList(Page<QuestionBusinessRefDTO> page, QuestionBusinessRefPageBO condition) ;

    /**
     * 查询最大排序号
     * @param questionBusinessRefPageBO
     * @return
     */
    Integer getMaxSortNum(QuestionBusinessRefPageBO questionBusinessRefPageBO);
}