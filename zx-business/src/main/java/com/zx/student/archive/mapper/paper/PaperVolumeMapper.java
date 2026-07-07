package com.zx.student.archive.mapper.paper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zx.student.archive.domain.paper.PaperVolume;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 试卷卷别Mapper接口
 * 
 * @author zx
 */
@Mapper
public interface PaperVolumeMapper extends BaseMapper<PaperVolume> {
    
    /**
     * 根据试卷ID查询卷别列表
     * 
     * @param paperId 试卷ID
     * @return 卷别列表
     */
    List<PaperVolume> selectByPaperId(@Param("paperId") Integer paperId);
    
    /**
     * 根据试卷ID和卷别代码查询卷别
     * 
     * @param paperId 试卷ID
     * @param volumeCode 卷别代码
     * @return 卷别信息
     */
    PaperVolume selectByPaperIdAndVolumeCode(@Param("paperId") Integer paperId, @Param("volumeCode") String volumeCode);
    
    /**
     * 删除试卷的所有卷别
     * 
     * @param paperId 试卷ID
     * @return 结果
     */
    int deleteByPaperId(@Param("paperId") Integer paperId);
}



