package com.zx.student.archive.biz.question;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zx.student.archive.domain.question.QuestionMedia;

import java.util.List;

/**
 * 题目媒体文件业务接口
 * 
 * @author zx
 */
public interface IQuestionMediaBiz extends IService<QuestionMedia> {
    
    /**
     * 根据题目ID查询媒体文件列表
     * 
     * @param questionId 题目ID
     * @return 媒体文件列表
     */
    List<QuestionMedia> listByQuestionId(Integer questionId);
    
    /**
     * 根据题目ID和媒体类型查询媒体文件列表
     * 
     * @param questionId 题目ID
     * @param mediaType 媒体类型：1-题目媒体，2-选项媒体，3-辅助识图
     * @return 媒体文件列表
     */
    List<QuestionMedia> listByQuestionIdAndType(Integer questionId, Integer mediaType);
    
    /**
     * 根据选项ID查询媒体文件
     * 
     * @param optionId 选项ID
     * @return 媒体文件
     */
    QuestionMedia getByOptionId(Integer optionId);
    
    /**
     * 根据题目ID删除所有媒体文件
     * 
     * @param questionId 题目ID
     * @return 是否成功
     */
    boolean deleteByQuestionId(Integer questionId);
    
    /**
     * 批量保存媒体文件
     * 
     * @param mediaList 媒体文件列表
     * @return 是否成功
     */
    boolean batchSave(List<QuestionMedia> mediaList);
}

