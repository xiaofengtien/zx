package com.zx.student.archive.service.paper;

import com.zx.student.archive.domain.dto.paper.PackageTaskInfo;
import com.zx.common.enums.PackageTaskStatus;

/**
 * 试卷包生成任务管理服务接口
 * 
 * @author zx
 */
public interface IPackageTaskService {

    /**
     * 创建任务
     * 
     * @param paperId 试卷ID
     * @return 任务信息
     * @throws com.zx.common.exception.ServiceException 如果任务已存在
     */
    PackageTaskInfo createTask(Integer paperId) throws com.zx.common.exception.ServiceException;

    /**
     * 获取任务信息
     * 
     * @param paperId 试卷ID
     * @return 任务信息，如果不存在返回null
     */
    PackageTaskInfo getTask(Integer paperId);

    /**
     * 更新任务状态
     * 
     * @param paperId 试卷ID
     * @param status 状态
     */
    void updateStatus(Integer paperId, PackageTaskStatus status);

    /**
     * 更新任务进度
     * 
     * @param paperId 试卷ID
     * @param progress 进度（0-100）
     * @param currentStep 当前步骤描述
     */
    void updateProgress(Integer paperId, Integer progress, String currentStep);

    /**
     * 更新任务为成功
     * 
     * @param paperId 试卷ID
     * @param newVersion 新版本号
     */
    void updateSuccess(Integer paperId, Integer newVersion);

    /**
     * 更新任务为失败
     * 
     * @param paperId 试卷ID
     * @param errorMessage 错误信息
     */
    void updateFailed(Integer paperId, String errorMessage);

    /**
     * 取消任务
     * 
     * @param paperId 试卷ID
     * @return 是否取消成功
     */
    boolean cancelTask(Integer paperId);

    /**
     * 删除任务（清理）
     * 
     * @param paperId 试卷ID
     */
    void removeTask(Integer paperId);

    /**
     * 检查任务是否存在且正在运行
     * 
     * @param paperId 试卷ID
     * @return 是否存在正在运行的任务
     */
    boolean isTaskRunning(Integer paperId);

    /**
     * 获取所有任务列表（用于任务中心显示）
     * 
     * @return 所有任务列表
     */
    java.util.List<PackageTaskInfo> getAllTasks();

    /**
     * 保存任务信息（用于更新任务的所有字段，包括上传状态等）
     * 
     * @param paperId 试卷ID
     * @param taskInfo 任务信息
     */
    void saveTask(Integer paperId, PackageTaskInfo taskInfo);
}
