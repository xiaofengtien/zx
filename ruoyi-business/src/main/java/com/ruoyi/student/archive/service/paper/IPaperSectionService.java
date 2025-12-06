package com.ruoyi.student.archive.service.paper;

import com.ruoyi.student.archive.domain.paper.PaperSection;

import java.util.List;

/**
 * 试卷大题服务接口
 * 
 * @author ruoyi
 */
public interface IPaperSectionService {
    
    /**
     * 根据试卷ID查询大题列表
     * 
     * @param paperId 试卷ID
     * @return 大题列表
     */
    List<PaperSection> listByPaperId(Integer paperId);
    
    /**
     * 根据试卷ID和卷别代码查询大题列表（兼容方法）
     * 
     * @param paperId 试卷ID
     * @param volumeCode 卷别代码
     * @return 大题列表
     */
    List<PaperSection> listByPaperIdAndVolumeCode(Integer paperId, String volumeCode);
    
    /**
     * 根据卷别ID查询大题列表
     * 
     * @param volumeId 卷别ID
     * @return 大题列表
     */
    List<PaperSection> listByVolumeId(Integer volumeId);
    
    /**
     * 根据ID查询大题
     * 
     * @param id 大题ID
     * @return 大题信息
     */
    PaperSection getById(Integer id);
    
    /**
     * 保存大题（新增或更新）
     * 
     * @param section 大题实体
     * @return 结果
     */
    boolean saveSection(PaperSection section);
    
    /**
     * 批量保存大题列表
     * 
     * @param sections 大题列表
     * @param paperId 试卷ID
     * @return 结果
     */
    boolean saveBatch(List<PaperSection> sections, Integer paperId);
    
    /**
     * 删除试卷的所有大题
     * 
     * @param paperId 试卷ID
     * @return 结果
     */
    boolean deleteByPaperId(Integer paperId);
    
    /**
     * 根据ID删除大题
     * 
     * @param id 大题ID
     * @return 结果
     */
    boolean deleteById(Integer id);
}


