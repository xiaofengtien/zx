package com.zx.student.archive.mapper.paper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zx.student.archive.domain.paper.PaperQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 试卷题目关联Mapper接口
 * 
 * @author zx
 */
@Mapper
public interface PaperQuestionMapper extends BaseMapper<PaperQuestion> {
    
    /**
     * 根据试卷ID查询题目列表（按排序号排序）
     * 
     * @param paperId 试卷ID
     * @return 题目关联列表
     */
    List<PaperQuestion> selectByPaperId(@Param("paperId") Integer paperId);
    
    /**
     * 根据题目ID查询试卷列表
     * 
     * @param questionId 题目ID
     * @return 试卷关联列表
     */
    List<PaperQuestion> selectByQuestionId(@Param("questionId") Integer questionId);
    
    /**
     * 批量插入试卷题目关联
     * 
     * @param paperQuestions 试卷题目关联列表
     * @return 插入数量
     */
    int batchInsert(@Param("list") List<PaperQuestion> paperQuestions);
    
    /**
     * 根据试卷ID删除所有题目关联
     * 
     * @param paperId 试卷ID
     * @return 删除数量
     */
    int deleteByPaperId(@Param("paperId") Integer paperId);
}



