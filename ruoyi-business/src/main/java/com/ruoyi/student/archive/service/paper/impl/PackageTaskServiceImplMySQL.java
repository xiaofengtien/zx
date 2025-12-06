package com.ruoyi.student.archive.service.paper.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.enums.PackageTaskStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.student.archive.domain.dto.paper.PackageTaskInfo;
import com.ruoyi.student.archive.domain.paper.PaperPackageTask;
import com.ruoyi.student.archive.mapper.paper.PaperPackageTaskMapper;
import com.ruoyi.student.archive.service.paper.IPackageTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 试卷包生成任务管理服务实现类（MySQL方案）
 * 所有任务数据存储在MySQL表中，支持显示多条任务记录
 * 
 * @author ruoyi
 */
@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class PackageTaskServiceImplMySQL implements IPackageTaskService {

    private final PaperPackageTaskMapper taskMapper;
    private final com.ruoyi.student.archive.biz.paper.IPaperBiz paperBiz;

    /**
     * 内存存储（用于快速访问和线程管理）
     * key: paperId, value: taskId（用于快速查找当前运行的任务）
     */
    private final ConcurrentHashMap<Integer, Long> paperIdToTaskIdMap = new ConcurrentHashMap<>();

    /**
     * 线程存储（用于取消任务）
     * key: taskId, value: Thread
     */
    private final ConcurrentHashMap<Long, Thread> taskThreadMap = new ConcurrentHashMap<>();

    /**
     * 服务启动时清理所有运行中的任务（因为服务重启后，这些任务不可能还在运行）
     */
    @PostConstruct
    public void cleanupStaleTasksOnStartup() {
        log.info("服务启动，开始清理MySQL中所有运行中的任务...");
        int cleanedCount = 0;

        try {
            // 查询所有运行中的任务（PENDING或RUNNING状态）
            LambdaQueryWrapper<PaperPackageTask> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PaperPackageTask::getDelFlag, "0")
                   .in(PaperPackageTask::getStatus, "PENDING", "RUNNING");
            
            List<PaperPackageTask> runningTasks = taskMapper.selectList(wrapper);
            
            if (runningTasks != null && !runningTasks.isEmpty()) {
                for (PaperPackageTask task : runningTasks) {
                    // 标记为失败
                    log.warn("发现僵尸任务（服务重启），清理任务，试卷ID：{}，任务ID：{}，原状态：{}", 
                            task.getPaperId(), task.getId(), task.getStatus());
                    
                    task.setStatus(PackageTaskStatus.FAILED.name());
                    task.setErrorMessage("服务重启，任务已中断");
                    task.setFinishTime(System.currentTimeMillis());
                    task.setUpdateTime(new Date());
                    
                    taskMapper.updateById(task);
                    cleanedCount++;
                }
            }
        } catch (Exception e) {
            log.error("清理MySQL任务失败", e);
        }

        log.info("服务启动清理完成，清理了 {} 个僵尸任务", cleanedCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PackageTaskInfo createTask(Integer paperId) throws ServiceException {
        // 检查是否有运行中的任务
        PaperPackageTask existingTask = taskMapper.selectLatestRunningTaskByPaperId(paperId);
        if (existingTask != null) {
            PackageTaskStatus status = PackageTaskStatus.valueOf(existingTask.getStatus());
            
            // 如果任务正在运行中（PENDING或RUNNING），不允许重复提交
            if (status == PackageTaskStatus.PENDING || status == PackageTaskStatus.RUNNING) {
                throw new ServiceException("该试卷正在生成试卷包，请等待");
            }
        }

        // 创建新任务实体
        PaperPackageTask task = new PaperPackageTask();
        task.setPaperId(paperId);
        task.setStatus(PackageTaskStatus.PENDING.name());
        task.setProgress(0);
        task.setCurrentStep("任务已提交，等待处理...");
        task.setStartTime(System.currentTimeMillis());
        task.setDelFlag("0");
        task.setCreateTime(new Date());
        task.setUpdateTime(new Date());
        
        // 设置创建者
        try {
            String username = SecurityUtils.getUsername();
            if (username != null) {
                task.setCreateBy(username);
                task.setUpdateBy(username);
            }
        } catch (Exception e) {
            log.warn("获取当前用户失败，使用默认值", e);
        }
        
        // 设置试卷名称和当前版本号（从数据库查询，用于任务显示）
        try {
            com.ruoyi.student.archive.domain.paper.Paper paper = paperBiz.getById(paperId);
            if (paper != null) {
                if (paper.getPaperName() != null) {
                    task.setPaperName(paper.getPaperName());
                }
                // 获取当前版本号（任务开始时的版本号）
                task.setCurrentVersion(paper.getVersion() != null ? paper.getVersion() : 0);
            }
        } catch (Exception e) {
            log.warn("获取试卷信息失败，试卷ID：{}，错误：{}", paperId, e.getMessage());
            // 如果获取失败，paperName 和 currentVersion 为 null，前端会使用默认名称
        }

        // 保存到数据库
        taskMapper.insert(task);
        
        // 更新内存映射
        paperIdToTaskIdMap.put(paperId, task.getId());

        log.info("创建试卷包生成任务，试卷ID：{}，任务ID：{}", paperId, task.getId());
        
        // 转换为DTO返回
        return convertToDTO(task);
    }

    @Override
    public PackageTaskInfo getTask(Integer paperId) {
        // 优先查询运行中的任务
        PaperPackageTask task = taskMapper.selectLatestRunningTaskByPaperId(paperId);
        
        // 如果没有运行中的任务，查询最新的任务（任意状态）
        if (task == null) {
            task = taskMapper.selectLatestTaskByPaperId(paperId);
        }
        
        if (task == null) {
            return null;
        }
        
        // 更新内存映射（确保paperId到taskId的映射是最新的）
        Long taskId = task.getId();
        paperIdToTaskIdMap.put(paperId, taskId);
        
        // 转换为DTO
        PackageTaskInfo dto = convertToDTO(task);
        
        // 从内存中获取线程引用（如果存在）
        Thread thread = taskThreadMap.get(taskId);
        if (thread != null) {
            dto.setTaskThread(thread);
        }
        
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer paperId, PackageTaskStatus status) {
        Long taskId = paperIdToTaskIdMap.get(paperId);
        if (taskId == null) {
            // 如果内存中没有，尝试从数据库查询
            PaperPackageTask task = taskMapper.selectLatestRunningTaskByPaperId(paperId);
            if (task == null) {
                task = taskMapper.selectLatestTaskByPaperId(paperId);
            }
            if (task != null) {
                taskId = task.getId();
                paperIdToTaskIdMap.put(paperId, taskId);
            }
        }
        
        if (taskId != null) {
            PaperPackageTask task = taskMapper.selectById(taskId);
            if (task != null) {
                task.setStatus(status.name());
                if (status == PackageTaskStatus.SUCCESS || status == PackageTaskStatus.FAILED
                        || status == PackageTaskStatus.CANCELLED) {
                    task.setFinishTime(System.currentTimeMillis());
                }
                task.setUpdateTime(new Date());
                
                try {
                    String username = SecurityUtils.getUsername();
                    if (username != null) {
                        task.setUpdateBy(username);
                    }
                } catch (Exception e) {
                    // 忽略
                }
                
                taskMapper.updateById(task);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProgress(Integer paperId, Integer progress, String currentStep) {
        Long taskId = paperIdToTaskIdMap.get(paperId);
        if (taskId == null) {
            // 如果内存中没有，尝试从数据库查询
            PaperPackageTask task = taskMapper.selectLatestRunningTaskByPaperId(paperId);
            if (task == null) {
                task = taskMapper.selectLatestTaskByPaperId(paperId);
            }
            if (task != null) {
                taskId = task.getId();
                paperIdToTaskIdMap.put(paperId, taskId);
            }
        }
        
        if (taskId != null) {
            PaperPackageTask task = taskMapper.selectById(taskId);
            if (task != null) {
                task.setProgress(progress);
                task.setCurrentStep(currentStep);
                if (PackageTaskStatus.PENDING.name().equals(task.getStatus())) {
                    task.setStatus(PackageTaskStatus.RUNNING.name());
                }
                task.setUpdateTime(new Date());
                
                try {
                    String username = SecurityUtils.getUsername();
                    if (username != null) {
                        task.setUpdateBy(username);
                    }
                } catch (Exception e) {
                    // 忽略
                }
                
                taskMapper.updateById(task);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSuccess(Integer paperId, Integer newVersion) {
        Long taskId = paperIdToTaskIdMap.get(paperId);
        if (taskId == null) {
            // 如果内存中没有，尝试从数据库查询
            PaperPackageTask task = taskMapper.selectLatestRunningTaskByPaperId(paperId);
            if (task == null) {
                task = taskMapper.selectLatestTaskByPaperId(paperId);
            }
            if (task != null) {
                taskId = task.getId();
                paperIdToTaskIdMap.put(paperId, taskId);
            }
        }
        
        if (taskId != null) {
            PaperPackageTask task = taskMapper.selectById(taskId);
            if (task != null) {
                task.setStatus(PackageTaskStatus.SUCCESS.name());
                task.setProgress(100);
                task.setCurrentStep("试卷包生成成功");
                task.setNewVersion(newVersion);
                task.setFinishTime(System.currentTimeMillis());
                task.setUpdateTime(new Date());
                
                try {
                    String username = SecurityUtils.getUsername();
                    if (username != null) {
                        task.setUpdateBy(username);
                    }
                } catch (Exception e) {
                    // 忽略
                }
                
                taskMapper.updateById(task);
                
                // 清理内存映射（任务已完成）
                paperIdToTaskIdMap.remove(paperId);
                taskThreadMap.remove(taskId);
                
                log.info("试卷包生成任务成功，试卷ID：{}，任务ID：{}，版本：{}", paperId, taskId, newVersion);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFailed(Integer paperId, String errorMessage) {
        Long taskId = paperIdToTaskIdMap.get(paperId);
        if (taskId == null) {
            // 如果内存中没有，尝试从数据库查询
            PaperPackageTask task = taskMapper.selectLatestRunningTaskByPaperId(paperId);
            if (task == null) {
                task = taskMapper.selectLatestTaskByPaperId(paperId);
            }
            if (task != null) {
                taskId = task.getId();
                paperIdToTaskIdMap.put(paperId, taskId);
            }
        }
        
        if (taskId != null) {
            PaperPackageTask task = taskMapper.selectById(taskId);
            if (task != null) {
                task.setStatus(PackageTaskStatus.FAILED.name());
                task.setErrorMessage(errorMessage);
                task.setFinishTime(System.currentTimeMillis());
                task.setUpdateTime(new Date());
                
                try {
                    String username = SecurityUtils.getUsername();
                    if (username != null) {
                        task.setUpdateBy(username);
                    }
                } catch (Exception e) {
                    // 忽略
                }
                
                taskMapper.updateById(task);
                
                // 清理内存映射（任务已完成）
                paperIdToTaskIdMap.remove(paperId);
                taskThreadMap.remove(taskId);
                
                log.error("试卷包生成任务失败，试卷ID：{}，任务ID：{}，错误：{}", paperId, taskId, errorMessage);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelTask(Integer paperId) {
        Long taskId = paperIdToTaskIdMap.get(paperId);
        if (taskId == null) {
            // 如果内存中没有，尝试从数据库查询
            PaperPackageTask task = taskMapper.selectLatestRunningTaskByPaperId(paperId);
            if (task != null) {
                taskId = task.getId();
                paperIdToTaskIdMap.put(paperId, taskId);
            }
        }
        
        if (taskId != null) {
            PaperPackageTask task = taskMapper.selectById(taskId);
            if (task != null) {
                PackageTaskStatus currentStatus = PackageTaskStatus.valueOf(task.getStatus());
                if (currentStatus == PackageTaskStatus.PENDING || currentStatus == PackageTaskStatus.RUNNING) {
                    // 中断任务线程
                    Thread taskThread = taskThreadMap.get(taskId);
                    if (taskThread != null && taskThread.isAlive()) {
                        taskThread.interrupt();
                        log.info("已中断任务线程，试卷ID：{}，任务ID：{}", paperId, taskId);
                    }

                    // 更新任务状态
                    task.setStatus(PackageTaskStatus.CANCELLED.name());
                    task.setCurrentStep("任务已取消");
                    task.setFinishTime(System.currentTimeMillis());
                    task.setUpdateTime(new Date());
                    
                    try {
                        String username = SecurityUtils.getUsername();
                        if (username != null) {
                            task.setUpdateBy(username);
                        }
                    } catch (Exception e) {
                        // 忽略
                    }
                    
                    taskMapper.updateById(task);
                    
                    // 清理内存映射（任务已完成）
                    paperIdToTaskIdMap.remove(paperId);
                    taskThreadMap.remove(taskId);

                    log.info("取消试卷包生成任务，试卷ID：{}，任务ID：{}", paperId, taskId);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTask(Integer paperId) {
        // 逻辑删除该试卷的所有任务
        taskMapper.deleteTasksByPaperId(paperId);
        
        // 清理内存映射
        Long taskId = paperIdToTaskIdMap.remove(paperId);
        if (taskId != null) {
            taskThreadMap.remove(taskId);
        }
        
        log.debug("删除试卷包生成任务，试卷ID：{}", paperId);
    }

    @Override
    public boolean isTaskRunning(Integer paperId) {
        PaperPackageTask task = taskMapper.selectLatestRunningTaskByPaperId(paperId);
        if (task != null) {
            PackageTaskStatus status = PackageTaskStatus.valueOf(task.getStatus());
            return status == PackageTaskStatus.PENDING || status == PackageTaskStatus.RUNNING;
        }
        return false;
    }

    @Override
    public List<PackageTaskInfo> getAllTasks() {
        List<PackageTaskInfo> allTasks = new ArrayList<>();
        
        try {
            // 从数据库查询所有未删除的任务
            List<PaperPackageTask> tasks = taskMapper.selectAllTasks();
            
            if (tasks != null && !tasks.isEmpty()) {
                for (PaperPackageTask task : tasks) {
                    try {
                        PackageTaskInfo dto = convertToDTO(task);
                        // 清除线程引用（因为线程对象不能序列化）
                        dto.setTaskThread(null);
                        allTasks.add(dto);
                    } catch (Exception e) {
                        log.warn("转换任务失败，任务ID：{}，错误：{}", task.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取所有任务失败", e);
        }
        
        log.debug("获取所有任务列表，共 {} 个任务", allTasks.size());
        return allTasks;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTask(Integer paperId, PackageTaskInfo taskInfo) {
        Long taskId = paperIdToTaskIdMap.get(paperId);
        if (taskId == null) {
            // 如果内存中没有，尝试从数据库查询
            PaperPackageTask task = taskMapper.selectLatestRunningTaskByPaperId(paperId);
            if (task == null) {
                task = taskMapper.selectLatestTaskByPaperId(paperId);
            }
            if (task != null) {
                taskId = task.getId();
                paperIdToTaskIdMap.put(paperId, taskId);
            }
        }
        
        if (taskId != null) {
            PaperPackageTask task = taskMapper.selectById(taskId);
            if (task == null) {
                // 如果任务不存在，创建新任务
                task = new PaperPackageTask();
                task.setPaperId(paperId);
                task.setDelFlag("0");
                task.setCreateTime(new Date());
            }
            
            // 更新任务信息
            task.setPaperName(taskInfo.getPaperName());
            task.setCurrentVersion(taskInfo.getCurrentVersion());
            task.setNewVersion(taskInfo.getNewVersion());
            task.setStatus(taskInfo.getStatus() != null ? taskInfo.getStatus().name() : PackageTaskStatus.PENDING.name());
            task.setProgress(taskInfo.getProgress());
            task.setCurrentStep(taskInfo.getCurrentStep());
            task.setErrorMessage(taskInfo.getErrorMessage());
            task.setStartTime(taskInfo.getStartTime());
            task.setFinishTime(taskInfo.getFinishTime());
            task.setUploadId(taskInfo.getUploadId());
            task.setObjectKey(taskInfo.getObjectKey());
            task.setUploadedParts(taskInfo.getUploadedParts());
            task.setFileSize(taskInfo.getFileSize());
            task.setChunkSize(taskInfo.getChunkSize());
            task.setUpdateTime(new Date());
            
            try {
                String username = SecurityUtils.getUsername();
                if (username != null) {
                    if (task.getCreateBy() == null) {
                        task.setCreateBy(username);
                    }
                    task.setUpdateBy(username);
                }
            } catch (Exception e) {
                // 忽略
            }
            
            if (task.getId() == null) {
                // 新任务，插入
                taskMapper.insert(task);
                taskId = task.getId();
                paperIdToTaskIdMap.put(paperId, taskId);
            } else {
                // 已存在，更新
                taskMapper.updateById(task);
            }
            
            // 保存线程引用到内存（如果存在）
            if (taskInfo.getTaskThread() != null) {
                taskThreadMap.put(taskId, taskInfo.getTaskThread());
            }
        } else {
            // 如果找不到任务，创建新任务
            PaperPackageTask task = new PaperPackageTask();
            task.setPaperId(paperId);
            task.setPaperName(taskInfo.getPaperName());
            task.setCurrentVersion(taskInfo.getCurrentVersion());
            task.setNewVersion(taskInfo.getNewVersion());
            task.setStatus(taskInfo.getStatus() != null ? taskInfo.getStatus().name() : PackageTaskStatus.PENDING.name());
            task.setProgress(taskInfo.getProgress());
            task.setCurrentStep(taskInfo.getCurrentStep());
            task.setErrorMessage(taskInfo.getErrorMessage());
            task.setStartTime(taskInfo.getStartTime());
            task.setFinishTime(taskInfo.getFinishTime());
            task.setUploadId(taskInfo.getUploadId());
            task.setObjectKey(taskInfo.getObjectKey());
            task.setUploadedParts(taskInfo.getUploadedParts());
            task.setFileSize(taskInfo.getFileSize());
            task.setChunkSize(taskInfo.getChunkSize());
            task.setDelFlag("0");
            task.setCreateTime(new Date());
            task.setUpdateTime(new Date());
            
            try {
                String username = SecurityUtils.getUsername();
                if (username != null) {
                    task.setCreateBy(username);
                    task.setUpdateBy(username);
                }
            } catch (Exception e) {
                // 忽略
            }
            
            taskMapper.insert(task);
            taskId = task.getId();
            paperIdToTaskIdMap.put(paperId, taskId);
            
            // 保存线程引用到内存（如果存在）
            if (taskInfo.getTaskThread() != null) {
                taskThreadMap.put(taskId, taskInfo.getTaskThread());
            }
        }
    }

    /**
     * 将实体转换为DTO
     */
    private PackageTaskInfo convertToDTO(PaperPackageTask task) {
        if (task == null) {
            return null;
        }
        
        PackageTaskInfo dto = new PackageTaskInfo();
        dto.setPaperId(task.getPaperId());
        dto.setPaperName(task.getPaperName());
        dto.setCurrentVersion(task.getCurrentVersion());
        dto.setNewVersion(task.getNewVersion());
        
        // 转换状态枚举
        try {
            dto.setStatus(PackageTaskStatus.valueOf(task.getStatus()));
        } catch (Exception e) {
            log.warn("任务状态转换失败，任务ID：{}，状态：{}", task.getId(), task.getStatus());
            dto.setStatus(PackageTaskStatus.PENDING);
        }
        
        dto.setProgress(task.getProgress());
        dto.setCurrentStep(task.getCurrentStep());
        dto.setErrorMessage(task.getErrorMessage());
        dto.setStartTime(task.getStartTime());
        dto.setFinishTime(task.getFinishTime());
        dto.setUploadId(task.getUploadId());
        dto.setObjectKey(task.getObjectKey());
        dto.setUploadedParts(task.getUploadedParts());
        dto.setFileSize(task.getFileSize());
        dto.setChunkSize(task.getChunkSize());
        
        return dto;
    }
}
