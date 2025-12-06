package com.ruoyi.student.archive.mapper.paper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.student.archive.domain.paper.PaperPackageTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 试卷包生成任务Mapper接口
 * 
 * @author ruoyi
 */
@Mapper
public interface PaperPackageTaskMapper extends BaseMapper<PaperPackageTask> {
    
    /**
     * 根据试卷ID查询最新的运行中任务（PENDING或RUNNING状态）
     * 
     * @param paperId 试卷ID
     * @return 任务信息
     */
    PaperPackageTask selectLatestRunningTaskByPaperId(@Param("paperId") Integer paperId);
    
    /**
     * 根据试卷ID查询最新的任务（任意状态）
     * 
     * @param paperId 试卷ID
     * @return 任务信息
     */
    PaperPackageTask selectLatestTaskByPaperId(@Param("paperId") Integer paperId);
    
    /**
     * 查询所有未删除的任务（按创建时间倒序）
     * 
     * @return 任务列表
     */
    List<PaperPackageTask> selectAllTasks();
    
    /**
     * 根据试卷ID查询所有未删除的任务（按创建时间倒序）
     * 
     * @param paperId 试卷ID
     * @return 任务列表
     */
    List<PaperPackageTask> selectTasksByPaperId(@Param("paperId") Integer paperId);
    
    /**
     * 逻辑删除任务（设置del_flag为'2'）
     * 
     * @param id 任务ID
     * @return 结果
     */
    int deleteTaskById(@Param("id") Long id);
    
    /**
     * 根据试卷ID逻辑删除所有任务
     * 
     * @param paperId 试卷ID
     * @return 结果
     */
    int deleteTasksByPaperId(@Param("paperId") Integer paperId);
}









