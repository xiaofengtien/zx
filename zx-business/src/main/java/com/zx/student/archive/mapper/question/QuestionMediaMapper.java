package com.zx.student.archive.mapper.question;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zx.student.archive.domain.question.QuestionMedia;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 题目媒体文件Mapper接口
 * 
 * @author zx
 */
@Mapper
public interface QuestionMediaMapper extends BaseMapper<QuestionMedia> {
    
    /**
     * 根据题目ID查询媒体文件列表
     * 
     * @param questionId 题目ID
     * @return 媒体文件列表
     */
    List<QuestionMedia> selectByQuestionId(@Param("questionId") Integer questionId);
    
    /**
     * 根据题目ID和媒体类型查询媒体文件列表
     * 
     * @param questionId 题目ID
     * @param mediaType 媒体类型：1-题目媒体，2-选项媒体，3-辅助识图
     * @return 媒体文件列表
     */
    List<QuestionMedia> selectByQuestionIdAndType(@Param("questionId") Integer questionId, @Param("mediaType") Integer mediaType);
    
    /**
     * 根据选项ID查询媒体文件
     * 
     * @param optionId 选项ID
     * @return 媒体文件
     */
    QuestionMedia selectByOptionId(@Param("optionId") Integer optionId);
    
    /**
     * 根据题目ID删除所有媒体文件
     * 
     * @param questionId 题目ID
     * @return 删除数量
     */
    int deleteByQuestionId(@Param("questionId") Integer questionId);
}

