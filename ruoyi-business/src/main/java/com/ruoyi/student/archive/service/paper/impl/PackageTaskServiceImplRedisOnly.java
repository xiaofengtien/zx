package com.ruoyi.student.archive.service.paper.impl;

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.enums.PackageTaskStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.domain.dto.paper.PackageTaskInfo;
import com.ruoyi.student.archive.service.paper.IPackageTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 试卷包生成任务管理服务实现类（Redis-Only方案）
 * 所有任务数据存储在Redis中,确保跨服务器重启和页面刷新的持久化
 * 
 * @author ruoyi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackageTaskServiceImplRedisOnly implements IPackageTaskService {

    private final RedisCache redisCache;
    private final com.ruoyi.student.archive.biz.paper.IPaperBiz paperBiz;

    /**
     * 任务过期时间（秒），默认30天
     */
    @Value("${paper.package.task.expire-time:2592000}")
    private int expireTime;

    /**
     * Redis key前缀
     */
    private static final String REDIS_KEY_PREFIX = "paper:package:task:";

    /**
     * 服务启动时清理所有运行中的任务（因为服务重启后，这些任务不可能还在运行）
     */
    @PostConstruct
    public void cleanupStaleTasksOnStartup() {
        log.info("服务启动，开始清理Redis中所有运行中的任务...");
        int cleanedCount = 0;

        try {
            // 获取所有任务的key
            Collection<String> keys = redisCache.keys(REDIS_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                for (String key : keys) {
                    PackageTaskInfo taskInfo = redisCache.getCacheObject(key);
                    if (taskInfo != null) {
                        PackageTaskStatus status = taskInfo.getStatus();
                        if (status == PackageTaskStatus.PENDING || status == PackageTaskStatus.RUNNING) {
                            // 标记为失败
                            log.warn("发现僵尸任务（服务重启），清理任务，试卷ID：{}，原状态：{}", taskInfo.getPaperId(), status);
                            taskInfo.setStatus(PackageTaskStatus.FAILED);
                            taskInfo.setErrorMessage("服务重启，任务已中断");
                            taskInfo.setFinishTime(System.currentTimeMillis());
                            redisCache.setCacheObject(key, taskInfo, expireTime, TimeUnit.SECONDS);
                            cleanedCount++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("清理Redis任务失败", e);
        }

        log.info("服务启动清理完成，清理了 {} 个僵尸任务", cleanedCount);
    }

    @Override
    public PackageTaskInfo createTask(Integer paperId) throws ServiceException {
        String redisKey = REDIS_KEY_PREFIX + paperId;

        // 从Redis检查任务是否存在
        PackageTaskInfo existingTask = redisCache.getCacheObject(redisKey);
        if (existingTask != null) {
            PackageTaskStatus status = existingTask.getStatus();

            // 如果任务正在运行中（PENDING或RUNNING），不允许重复提交
            if (status == PackageTaskStatus.PENDING || status == PackageTaskStatus.RUNNING) {
                throw new ServiceException("该试卷正在生成试卷包，请等待");
            }

            // 如果任务已完成（SUCCESS/FAILED/CANCELLED），清理旧任务，允许重新提交
            log.info("清理已完成的任务，允许重新提交，试卷ID：{}，原状态：{}", paperId, status);
            removeTask(paperId);
        }

        // 创建新任务信息
        PackageTaskInfo taskInfo = new PackageTaskInfo();
        taskInfo.setPaperId(paperId);
        taskInfo.setStatus(PackageTaskStatus.PENDING);
        taskInfo.setProgress(0);
        taskInfo.setCurrentStep("任务已提交，等待处理...");
        taskInfo.setStartTime(System.currentTimeMillis());
        
        // 设置试卷名称和当前版本号（从数据库查询，用于任务显示）
        try {
            com.ruoyi.student.archive.domain.paper.Paper paper = paperBiz.getById(paperId);
            if (paper != null) {
                if (paper.getPaperName() != null) {
                    taskInfo.setPaperName(paper.getPaperName());
                }
                // 获取当前版本号（任务开始时的版本号）
                taskInfo.setCurrentVersion(paper.getVersion() != null ? paper.getVersion() : 0);
            }
        } catch (Exception e) {
            log.warn("获取试卷信息失败，试卷ID：{}，错误：{}", paperId, e.getMessage());
            // 如果获取失败，paperName 和 currentVersion 为 null，前端会使用默认名称
        }

        // 存储到Redis
        redisCache.setCacheObject(redisKey, taskInfo, expireTime, TimeUnit.SECONDS);

        log.info("创建试卷包生成任务，试卷ID：{}", paperId);
        return taskInfo;
    }

    @Override
    public PackageTaskInfo getTask(Integer paperId) {
        String redisKey = REDIS_KEY_PREFIX + paperId;
        return redisCache.getCacheObject(redisKey);
    }

    @Override
    public void updateStatus(Integer paperId, PackageTaskStatus status) {
        PackageTaskInfo taskInfo = getTask(paperId);
        if (taskInfo != null) {
            taskInfo.setStatus(status);
            if (status == PackageTaskStatus.SUCCESS || status == PackageTaskStatus.FAILED
                    || status == PackageTaskStatus.CANCELLED) {
                taskInfo.setFinishTime(System.currentTimeMillis());
            }
            saveTask(paperId, taskInfo);
        }
    }

    @Override
    public void updateProgress(Integer paperId, Integer progress, String currentStep) {
        PackageTaskInfo taskInfo = getTask(paperId);
        if (taskInfo != null) {
            taskInfo.setProgress(progress);
            taskInfo.setCurrentStep(currentStep);
            if (taskInfo.getStatus() == PackageTaskStatus.PENDING) {
                taskInfo.setStatus(PackageTaskStatus.RUNNING);
            }
            saveTask(paperId, taskInfo);
        }
    }

    @Override
    public void updateSuccess(Integer paperId, Integer newVersion) {
        PackageTaskInfo taskInfo = getTask(paperId);
        if (taskInfo != null) {
            taskInfo.setStatus(PackageTaskStatus.SUCCESS);
            taskInfo.setProgress(100);
            taskInfo.setCurrentStep("试卷包生成成功");
            taskInfo.setNewVersion(newVersion);
            taskInfo.setFinishTime(System.currentTimeMillis());
            saveTask(paperId, taskInfo);
            log.info("试卷包生成任务成功，试卷ID：{}，版本：{}", paperId, newVersion);
        }
    }

    @Override
    public void updateFailed(Integer paperId, String errorMessage) {
        PackageTaskInfo taskInfo = getTask(paperId);
        if (taskInfo != null) {
            taskInfo.setStatus(PackageTaskStatus.FAILED);
            taskInfo.setErrorMessage(errorMessage);
            taskInfo.setFinishTime(System.currentTimeMillis());
            saveTask(paperId, taskInfo);
            log.error("试卷包生成任务失败，试卷ID：{}，错误：{}", paperId, errorMessage);
        }
    }

    @Override
    public boolean cancelTask(Integer paperId) {
        PackageTaskInfo taskInfo = getTask(paperId);
        if (taskInfo != null) {
            PackageTaskStatus currentStatus = taskInfo.getStatus();
            if (currentStatus == PackageTaskStatus.PENDING || currentStatus == PackageTaskStatus.RUNNING) {
                // 中断任务线程
                Thread taskThread = taskInfo.getTaskThread();
                if (taskThread != null && taskThread.isAlive()) {
                    taskThread.interrupt();
                    log.info("已中断任务线程，试卷ID：{}", paperId);
                }

                // 更新任务状态
                taskInfo.setStatus(PackageTaskStatus.CANCELLED);
                taskInfo.setCurrentStep("任务已取消");
                taskInfo.setFinishTime(System.currentTimeMillis());
                saveTask(paperId, taskInfo);

                log.info("取消试卷包生成任务，试卷ID：{}", paperId);
                return true;
            }
        }
        return false;
    }

    @Override
    public void removeTask(Integer paperId) {
        String redisKey = REDIS_KEY_PREFIX + paperId;
        redisCache.deleteObject(redisKey);
        log.debug("删除试卷包生成任务，试卷ID：{}", paperId);
    }

    @Override
    public boolean isTaskRunning(Integer paperId) {
        PackageTaskInfo taskInfo = getTask(paperId);
        if (taskInfo != null) {
            PackageTaskStatus status = taskInfo.getStatus();
            return status == PackageTaskStatus.PENDING || status == PackageTaskStatus.RUNNING;
        }
        return false;
    }

    @Override
    public List<PackageTaskInfo> getAllTasks() {
        List<PackageTaskInfo> allTasks = new ArrayList<>();
        
        try {
            // 从Redis获取所有任务
            Collection<String> keys = redisCache.keys(REDIS_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                for (String redisKey : keys) {
                    try {
                        PackageTaskInfo taskInfo = redisCache.getCacheObject(redisKey);
                        if (taskInfo != null && taskInfo.getPaperId() != null) {
                            // 清除线程引用（因为线程对象不能序列化）
                            taskInfo.setTaskThread(null);
                            allTasks.add(taskInfo);
                        }
                    } catch (Exception e) {
                        log.warn("获取任务失败，Redis key: {}, 错误: {}", redisKey, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取所有任务失败", e);
        }
        
        // 按创建时间倒序排序（最新的在前）
        allTasks.sort((a, b) -> {
            long timeA = a.getStartTime() != null ? a.getStartTime() : 0;
            long timeB = b.getStartTime() != null ? b.getStartTime() : 0;
            return Long.compare(timeB, timeA);
        });
        
        log.debug("获取所有任务列表，共 {} 个任务", allTasks.size());
        return allTasks;
    }

    /**
     * 保存任务信息到Redis
     */
    @Override
    public void saveTask(Integer paperId, PackageTaskInfo taskInfo) {
        // 清除线程引用（因为线程对象不能序列化到Redis）
        PackageTaskInfo redisTask = new PackageTaskInfo();
        redisTask.setPaperId(taskInfo.getPaperId());
        redisTask.setStatus(taskInfo.getStatus());
        redisTask.setProgress(taskInfo.getProgress());
        redisTask.setCurrentStep(taskInfo.getCurrentStep());
        redisTask.setErrorMessage(taskInfo.getErrorMessage());
        redisTask.setStartTime(taskInfo.getStartTime());
        redisTask.setFinishTime(taskInfo.getFinishTime());
        redisTask.setNewVersion(taskInfo.getNewVersion());
        redisTask.setCurrentVersion(taskInfo.getCurrentVersion()); // 复制当前版本号
        redisTask.setPaperName(taskInfo.getPaperName()); // 复制试卷名称
        redisTask.setUploadId(taskInfo.getUploadId()); // 复制上传ID，用于断点续传
        redisTask.setObjectKey(taskInfo.getObjectKey()); // 复制对象键，用于断点续传
        redisTask.setUploadedParts(taskInfo.getUploadedParts()); // 复制已上传分片列表，用于断点续传
        redisTask.setFileSize(taskInfo.getFileSize()); // 复制文件大小，用于断点续传
        redisTask.setChunkSize(taskInfo.getChunkSize()); // 复制分片大小，用于断点续传
        // 不保存线程引用到Redis（因为线程对象不能序列化）
        redisTask.setTaskThread(null);
        
        String redisKey = REDIS_KEY_PREFIX + paperId;
        redisCache.setCacheObject(redisKey, redisTask, expireTime, TimeUnit.SECONDS);
    }
}
