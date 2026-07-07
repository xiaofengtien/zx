package com.zx.student.archive.service.paper;

import com.zx.student.archive.domain.paper.PaperIntermission;

import java.util.List;

/**
 * 卷间中场配置服务接口
 * 
 * @author zx
 */
public interface IPaperIntermissionService {
    
    /**
     * 根据试卷ID查询中场配置列表
     * 
     * @param paperId 试卷ID
     * @return 中场配置列表
     */
    List<PaperIntermission> listByPaperId(Integer paperId);
    
    /**
     * 根据试卷ID、来源卷别和目标卷别查询中场配置
     * 
     * @param paperId 试卷ID
     * @param fromVolume 来源卷别
     * @param toVolume 目标卷别
     * @return 中场配置信息
     */
    PaperIntermission getByPaperIdAndVolumes(Integer paperId, String fromVolume, String toVolume);
    
    /**
     * 保存中场配置（新增或更新）
     * 
     * @param intermission 中场配置实体
     * @return 结果
     */
    boolean saveIntermission(PaperIntermission intermission);
    
    /**
     * 批量保存中场配置列表
     * 
     * @param intermissions 中场配置列表
     * @param paperId 试卷ID
     * @return 结果
     */
    boolean saveBatch(List<PaperIntermission> intermissions, Integer paperId);
    
    /**
     * 删除试卷的所有中场配置
     * 
     * @param paperId 试卷ID
     * @return 结果
     */
    boolean deleteByPaperId(Integer paperId);
    
    /**
     * 根据ID删除中场配置
     * 
     * @param id 中场配置ID
     * @return 结果
     */
    boolean deleteById(Integer id);
}



