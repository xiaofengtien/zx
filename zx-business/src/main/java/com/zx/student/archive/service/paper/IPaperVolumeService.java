package com.zx.student.archive.service.paper;

import com.zx.student.archive.domain.paper.PaperVolume;

import java.util.List;

/**
 * 试卷卷别服务接口
 * 
 * @author zx
 */
public interface IPaperVolumeService {
    
    /**
     * 根据试卷ID查询卷别列表
     * 
     * @param paperId 试卷ID
     * @return 卷别列表
     */
    List<PaperVolume> listByPaperId(Integer paperId);
    
    /**
     * 根据试卷ID和卷别代码查询卷别
     * 
     * @param paperId 试卷ID
     * @param volumeCode 卷别代码
     * @return 卷别信息
     */
    PaperVolume getByPaperIdAndVolumeCode(Integer paperId, String volumeCode);
    
    /**
     * 保存卷别（新增或更新）
     * 
     * @param volume 卷别实体
     * @return 结果
     */
    boolean saveVolume(PaperVolume volume);
    
    /**
     * 批量保存卷别列表
     * 
     * @param volumes 卷别列表
     * @param paperId 试卷ID
     * @return 结果
     */
    boolean saveBatch(List<PaperVolume> volumes, Integer paperId);
    
    /**
     * 删除试卷的所有卷别
     * 
     * @param paperId 试卷ID
     * @return 结果
     */
    boolean deleteByPaperId(Integer paperId);
    
    /**
     * 根据ID删除卷别
     * 
     * @param id 卷别ID
     * @return 结果
     */
    boolean deleteById(Integer id);
}



