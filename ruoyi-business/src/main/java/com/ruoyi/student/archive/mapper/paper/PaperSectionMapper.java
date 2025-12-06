package com.ruoyi.student.archive.mapper.paper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.student.archive.domain.paper.PaperSection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 试卷大题Mapper接口
 * 
 * @author ruoyi
 */
@Mapper
public interface PaperSectionMapper extends BaseMapper<PaperSection> {
    
    /**
     * 根据试卷ID查询大题列表
     * 
     * @param paperId 试卷ID
     * @return 大题列表
     */
    List<PaperSection> selectByPaperId(@Param("paperId") Integer paperId);
    
    /**
     * 根据试卷ID和卷别代码查询大题列表（兼容方法）
     * 
     * @param paperId 试卷ID
     * @param volumeCode 卷别代码
     * @return 大题列表
     */
    List<PaperSection> selectByPaperIdAndVolumeCode(@Param("paperId") Integer paperId, @Param("volumeCode") String volumeCode);
    
    /**
     * 根据卷别ID查询大题列表
     * 
     * @param volumeId 卷别ID
     * @return 大题列表
     */
    List<PaperSection> selectByVolumeId(@Param("volumeId") Integer volumeId);
    
    /**
     * 删除试卷的所有大题
     * 
     * @param paperId 试卷ID
     * @return 结果
     */
    int deleteByPaperId(@Param("paperId") Integer paperId);
}


