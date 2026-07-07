package com.zx.student.archive.service.paper;

import com.zx.student.archive.domain.paper.PaperQuestionGroup;

import java.util.List;

/**
 * 试卷题目组服务接口
 * 
 * @author zx
 */
public interface IPaperQuestionGroupService {

    /**
     * 根据大题ID查询题目组列表
     * 
     * @param sectionId 大题ID
     * @return 题目组列表
     */
    List<PaperQuestionGroup> listBySectionId(Integer sectionId);

    /**
     * 根据试卷ID查询所有题目组
     * 
     * @param paperId 试卷ID
     * @return 题目组列表
     */
    List<PaperQuestionGroup> listByPaperId(Integer paperId);

    /**
     * 根据ID查询题目组
     * 
     * @param id 题目组ID
     * @return 题目组信息
     */
    PaperQuestionGroup getById(Integer id);

    /**
     * 保存题目组（新增或更新）
     * 
     * @param group 题目组实体
     * @return 结果
     */
    boolean saveGroup(PaperQuestionGroup group);

    /**
     * 批量保存题目组列表
     * 
     * @param groups    题目组列表
     * @param sectionId 大题ID
     * @return 结果
     */
    boolean saveBatch(List<PaperQuestionGroup> groups, Integer sectionId);

    /**
     * 删除大题的所有题目组
     * 
     * @param sectionId 大题ID
     * @return 结果
     */
    boolean deleteBySectionId(Integer sectionId);

    /**
     * 删除试卷的所有题目组
     * 
     * @param paperId 试卷ID
     * @return 结果
     */
    boolean deleteByPaperId(Integer paperId);

    /**
     * 根据ID删除题目组
     * 
     * @param id 题目组ID
     * @return 结果
     */
    boolean deleteById(Integer id);
}
