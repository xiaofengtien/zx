package com.zx.student.archive.mapper.paper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zx.student.archive.domain.paper.PaperIntermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 卷间中场配置Mapper接口
 * 
 * @author zx
 */
@Mapper
public interface PaperIntermissionMapper extends BaseMapper<PaperIntermission> {
    
    /**
     * 根据试卷ID查询中场配置列表
     * 
     * @param paperId 试卷ID
     * @return 中场配置列表
     */
    List<PaperIntermission> selectByPaperId(@Param("paperId") Integer paperId);
    
    /**
     * 根据试卷ID、来源卷别和目标卷别查询中场配置
     * 
     * @param paperId 试卷ID
     * @param fromVolume 来源卷别
     * @param toVolume 目标卷别
     * @return 中场配置信息
     */
    PaperIntermission selectByPaperIdAndVolumes(@Param("paperId") Integer paperId, 
                                                @Param("fromVolume") String fromVolume, 
                                                @Param("toVolume") String toVolume);
    
    /**
     * 删除试卷的所有中场配置
     * 
     * @param paperId 试卷ID
     * @return 结果
     */
    int deleteByPaperId(@Param("paperId") Integer paperId);
}



