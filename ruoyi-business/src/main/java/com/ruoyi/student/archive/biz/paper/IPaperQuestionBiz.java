package com.ruoyi.student.archive.biz.paper;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.student.archive.domain.paper.PaperQuestion;

import java.util.List;

/**
 * 试卷题目关联业务接口
 * 
 * @author ruoyi
 */
public interface IPaperQuestionBiz extends IService<PaperQuestion> {
    
    /**
     * 根据试卷ID查询题目列表（按排序号排序）
     * 
     * @param paperId 试卷ID
     * @return 题目关联列表
     */
    List<PaperQuestion> listByPaperId(Integer paperId);
    
    /**
     * 根据题目ID查询试卷列表
     * 
     * @param questionId 题目ID
     * @return 试卷关联列表
     */
    List<PaperQuestion> listByQuestionId(Integer questionId);
    
    /**
     * 批量插入试卷题目关联
     * 
     * @param paperQuestions 试卷题目关联列表
     * @return 是否成功
     */
    boolean batchInsert(List<PaperQuestion> paperQuestions);
    
    /**
     * 根据试卷ID删除所有题目关联
     * 
     * @param paperId 试卷ID
     * @return 是否成功
     */
    boolean deleteByPaperId(Integer paperId);
    
    /**
     * 根据试卷ID和题目ID列表批量删除关联
     * 
     * @param paperId 试卷ID
     * @param questionIds 题目ID列表
     * @return 是否成功
     */
    boolean batchDeleteByPaperIdAndQuestionIds(Integer paperId, List<Integer> questionIds);
    
    /**
     * 根据试卷ID和大题ID删除该大题下的所有题目关联
     * 
     * @param paperId 试卷ID
     * @param sectionId 大题ID
     * @return 是否成功
     */
    boolean deleteByPaperIdAndSectionId(Integer paperId, Integer sectionId);
}


