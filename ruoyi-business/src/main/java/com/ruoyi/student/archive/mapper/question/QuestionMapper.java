package com.ruoyi.student.archive.mapper.question;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.student.archive.domain.dto.question.QuestionCountDTO;
import com.ruoyi.student.archive.domain.question.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 分页查询题目列表（包含学科信息）
     *
     * @param page 分页参数
     * @param condition 查询条件
     * @return 分页结果
     */
    Page<Question> pageList(Page<Question> page, @Param("condition") Question condition,@Param("bizType") String bizType);


    List<QuestionCountDTO> countByCategoryIds(@Param("categoryIds") List<Integer> categoryIds);
} 