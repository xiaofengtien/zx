package com.zx.student.archive.mapper.question;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zx.student.archive.domain.bo.question.QuestionBusinessRefPageBO;
import com.zx.student.archive.domain.dto.question.QuestionBusinessRefDTO;
import com.zx.student.archive.domain.question.QuestionBusinessRef;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface QuestionBusinessRefMapper extends BaseMapper<QuestionBusinessRef> {

    /**
     * 分页查询题目列表（包含学科信息）
     *
     * @param page 分页参数
     * @param condition 查询条件
     * @return 分页结果
     */
    Page<QuestionBusinessRefDTO> pageList(Page<QuestionBusinessRefDTO> page, @Param("condition") QuestionBusinessRefPageBO condition);

    /**
     * 查询最大排序号
     * @param questionBusinessRefPageBO
     * @return
     */
    Integer getMaxSortNum(@Param("bo")QuestionBusinessRefPageBO questionBusinessRefPageBO);
} 