package com.zx.student.archive.service.question;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zx.student.archive.domain.bo.question.QuestionBusinessRefPageBO;
import com.zx.student.archive.domain.dto.question.QuestionBusinessRefDTO;

/**
 * 题目业务引用服务接口
 */
public interface QuestionBusinessRefService {
    /**
     * 分页查询题目列表
     */
    Page<QuestionBusinessRefDTO> pageList(QuestionBusinessRefPageBO pageBO);


    /**
     * 查询最大排序号
     * @param questionBusinessRefPageBO
     * @return
     */
    Integer getMaxSortNum(QuestionBusinessRefPageBO questionBusinessRefPageBO);
} 