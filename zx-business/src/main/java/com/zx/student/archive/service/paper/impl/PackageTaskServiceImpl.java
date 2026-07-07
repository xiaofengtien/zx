package com.zx.student.archive.service.paper.impl;

import com.zx.common.core.redis.RedisCache;
import com.zx.common.enums.PackageTaskStatus;
import com.zx.common.exception.ServiceException;
import com.zx.student.archive.domain.dto.paper.PackageTaskInfo;
import com.zx.student.archive.service.paper.IPackageTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 试卷包生成任务管理服务实现类（混合方案：内存+Redis）
 * 
 * @author zx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackageTaskServiceImpl implements IPackageTaskService {

    private final RedisCache redisCache;
    private final com.zx.student.archive.biz.paper.IPaperBiz paperBiz;

    /**
     * 是否启用Redis（生产环境建议启用）
     */
    @Value("${paper.package.task.use-redis:true}")
    private boolean useRedis;

    /**
     * 任务过期时间（秒），默认30天（2592000秒）
     */
    @Value("${paper.package.task.expire-time:2592000}")
    private int expireTime;

    /**
     * Redis key前缀
     */
    private static final String REDIS_KEY_PREFIX = "paper:package:task:";

    /**
     * 内存存储（用于快速访问和线程管理）
     */
    private final ConcurrentHashMap<Integer, PackageTaskInfo> memoryCache = new ConcurrentHashMap<>();

    /**
     * 服务启动时从Redis恢复所有任务到内存，并清理僵尸任务
     */
    @PostConstruct
    public void restoreTasksFromRedisOnStartup() {
        log.info("服务启动，开始从Redis恢复任务数据...");
        int restoredCount = 0;
        int cleanedCount = 0;

        if (useRedis) {
            try {
                // 获取所有任务的key
                Collection<String> keys = redisCache.keys(REDIS_KEY_PREFIX + "*");
                if (keys != null && !keys.isEmpty()) {
                    log.info("从Redis发现 {} 个任务，开始恢复...", keys.size());
                    
                    for (String redisKey : keys) {
                        try {
                            PackageTaskInfo taskInfo = redisCache.getCacheObject(redisKey);
                            if (taskInfo != null && taskInfo.getPaperId() != null) {
                                Integer paperId = taskInfo.getPaperId();
                                PackageTaskStatus status = taskInfo.getStatus();
                                
                                // 检查是否为僵尸任务（服务重启后，所有运行中的任务都是僵尸任务）
                                if (status == PackageTaskStatus.PENDING || status == PackageTaskStatus.RUNNING) {
                                    // 服务重启后，所有运行中的任务都应该标记为失败
                                    log.warn("发现运行中的任务（服务重启），标记为失败，试卷ID：{}，原状态：{}", paperId, status);
                                    taskInfo.setStatus(PackageTaskStatus.FAILED);
                                    taskInfo.setErrorMessage("服务重启，任务已中断");
                                    taskInfo.setFinishTime(System.currentTimeMillis());
                                    // 清除线程引用（因为线程已不存在）
                                    taskInfo.setTaskThread(null);
                                    saveTask(paperId, taskInfo);
                                    cleanedCount++;
                                } else {
                                    // 已完成的任务（SUCCESS/FAILED/CANCELLED），恢复到内存
                                    // 清除线程引用（因为线程已不存在）
                                    taskInfo.setTaskThread(null);
                                    memoryCache.put(paperId, taskInfo);
                                    restoredCount++;
                                    log.debug("恢复已完成的任务到内存，试卷ID：{}，状态：{}", paperId, status);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("恢复任务失败，Redis key: {}, 错误: {}", redisKey, e.getMessage());
                        }
                    }
                } else {
                    log.info("Redis中没有发现任务数据");
                }
            } catch (Exception e) {
                log.error("从Redis恢复任务失败", e);
            }
        }

        log.info("服务启动恢复完成，恢复了 {} 个已完成的任务，清理了 {} 个运行中的任务", restoredCount, cleanedCount);
    }

    /**
     * 检查任务是否为僵尸任务（状态为运行中但线程已不存在）
     * 
     * @param taskInfo 任务信息
     * @return true表示是僵尸任务，false表示正常
     */
    private boolean isZombieTask(PackageTaskInfo taskInfo) {
        if (taskInfo == null) {
            return false;
        }

        PackageTaskStatus status = taskInfo.getStatus();
        if (status == PackageTaskStatus.PENDING || status == PackageTaskStatus.RUNNING) {
            Thread taskThread = taskInfo.getTaskThread();

            // 如果线程不存在或已死亡
            if (taskThread == null || !taskThread.isAlive()) {
                // 检查任务创建时间，如果任务刚创建（5分钟内），线程可能还没设置，不算僵尸任务
                long currentTime = System.currentTimeMillis();
                long startTime = taskInfo.getStartTime() != null ? taskInfo.getStartTime() : currentTime;
                long elapsedMinutes = (currentTime - startTime) / (1000 * 60);

                // 只有当任务已经运行超过5分钟但线程还不存在时，才认为是僵尸任务
                if (elapsedMinutes > 5) {
                    log.warn("检测到僵尸任务，任务已运行{}分钟但线程不存在，试卷ID：{}", elapsedMinutes, taskInfo.getPaperId());
                    return true;
                }
                // 任务刚创建，线程还没设置，不算僵尸任务
                return false;
            }
        }
        return false;
    }

    @Override
    public PackageTaskInfo createTask(Integer paperId) throws ServiceException {
        // 检查任务是否正在运行中（先从内存检查，快速响应）
        PackageTaskInfo existingTask = memoryCache.get(paperId);
        if (existingTask != null) {
            PackageTaskStatus status = existingTask.getStatus();

            // 如果任务正在运行中（PENDING或RUNNING），检查是否为僵尸任务
            if (status == PackageTaskStatus.PENDING || status == PackageTaskStatus.RUNNING) {
                // 检查是否为僵尸任务（状态为运行中但线程已不存在）
                if (isZombieTask(existingTask)) {
                    log.warn("发现僵尸任务，清理后允许重新提交，试卷ID：{}", paperId);
                    existingTask.setStatus(PackageTaskStatus.FAILED);
                    existingTask.setErrorMessage("任务线程已不存在，任务已中断");
                    existingTask.setFinishTime(System.currentTimeMillis());
                    saveTask(paperId, existingTask);
                    // 清理旧任务，允许重新提交
                    removeTask(paperId);
                } else {
                    // 任务正在运行中且线程存活，不允许重复提交
                    throw new ServiceException("该试卷正在生成试卷包，请等待");
                }
            } else {
                // 如果任务已完成（SUCCESS/FAILED/CANCELLED），清理旧任务，允许重新提交
                log.info("清理已完成的任务，允许重新提交，试卷ID：{}，原状态：{}", paperId, status);
                removeTask(paperId);
            }
        }

        // 如果启用了Redis，也检查Redis（但不阻塞，如果Redis失败则继续）
        if (useRedis) {
            try {
                String redisKey = REDIS_KEY_PREFIX + paperId;
                PackageTaskInfo redisTask = redisCache.getCacheObject(redisKey);
                if (redisTask != null) {
                    PackageTaskStatus status = redisTask.getStatus();

                    // 如果任务正在运行中，检查是否为僵尸任务
                    if (status == PackageTaskStatus.PENDING || status == PackageTaskStatus.RUNNING) {
                        // 检查是否为僵尸任务（状态为运行中但线程已不存在）
                        if (isZombieTask(redisTask)) {
                            log.warn("从Redis发现僵尸任务，清理后允许重新提交，试卷ID：{}", paperId);
                            redisTask.setStatus(PackageTaskStatus.FAILED);
                            redisTask.setErrorMessage("任务线程已不存在，任务已中断");
                            redisTask.setFinishTime(System.currentTimeMillis());
                            saveTask(paperId, redisTask);
                            // 清理旧任务，允许重新提交
                            removeTask(paperId);
                        } else {
                            // 任务正在运行中且线程存活，不允许重复提交
                            // 同步到内存
                            memoryCache.put(paperId, redisTask);
                            throw new ServiceException("该试卷正在生成试卷包，请等待");
                        }
                    } else {
                        // 如果任务已完成，清理旧任务
                        log.info("清理Redis中已完成的任务，允许重新提交，试卷ID：{}，原状态：{}", paperId, status);
                        removeTask(paperId);
                    }
                }
            } catch (ServiceException e) {
                // 如果是业务异常（任务正在运行），直接抛出
                throw e;
            } catch (Exception e) {
                // Redis操作失败，记录日志但不阻塞，继续使用内存
                log.warn("检查Redis任务状态失败，继续使用内存存储，试卷ID：{}，错误：{}", paperId, e.getMessage());
            }
        }

        // 创建任务信息
        PackageTaskInfo taskInfo = new PackageTaskInfo();
        taskInfo.setPaperId(paperId);
        taskInfo.setStatus(PackageTaskStatus.PENDING);
        taskInfo.setProgress(0);
        taskInfo.setCurrentStep("任务已提交，等待处理...");
        taskInfo.setStartTime(System.currentTimeMillis());
        
        // 设置试卷名称和当前版本号（从数据库查询，用于任务显示）
        try {
            com.zx.student.archive.domain.paper.Paper paper = paperBiz.getById(paperId);
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

        // 存储到内存（优先，快速响应）
        memoryCache.put(paperId, taskInfo);

        // 存储到Redis（如果启用，失败不阻塞）
        if (useRedis) {
            try {
                String redisKey = REDIS_KEY_PREFIX + paperId;
                redisCache.setCacheObject(redisKey, taskInfo, expireTime, TimeUnit.SECONDS);
            } catch (Exception e) {
                // Redis操作失败，记录日志但不阻塞，继续使用内存
                log.warn("保存任务到Redis失败，继续使用内存存储，试卷ID：{}，错误：{}", paperId, e.getMessage());
            }
        }

        log.info("创建试卷包生成任务，试卷ID：{}", paperId);
        return taskInfo;
    }

    @Override
    public PackageTaskInfo getTask(Integer paperId) {
        // 先从内存获取
        PackageTaskInfo taskInfo = memoryCache.get(paperId);

        // 如果内存中没有且启用了Redis，从Redis获取
        if (taskInfo == null && useRedis) {
            try {
                String redisKey = REDIS_KEY_PREFIX + paperId;
                taskInfo = redisCache.getCacheObject(redisKey);
                // 如果从Redis获取到，同步到内存
                if (taskInfo != null) {
                    // 检查是否为僵尸任务
                    if (isZombieTask(taskInfo)) {
                        log.warn("从Redis获取到僵尸任务，自动清理，试卷ID：{}", paperId);
                        taskInfo.setStatus(PackageTaskStatus.FAILED);
                        taskInfo.setErrorMessage("任务线程已不存在，任务已中断");
                        taskInfo.setFinishTime(System.currentTimeMillis());
                        saveTask(paperId, taskInfo);
                    } else {
                        memoryCache.put(paperId, taskInfo);
                    }
                }
            } catch (Exception e) {
                log.warn("从Redis获取任务失败，试卷ID：{}，错误：{}", paperId, e.getMessage());
            }
        } else if (taskInfo != null) {
            // 如果从内存获取到，也检查是否为僵尸任务
            if (isZombieTask(taskInfo)) {
                log.warn("内存中发现僵尸任务，自动清理，试卷ID：{}", paperId);
                taskInfo.setStatus(PackageTaskStatus.FAILED);
                taskInfo.setErrorMessage("任务线程已不存在，任务已中断");
                taskInfo.setFinishTime(System.currentTimeMillis());
                saveTask(paperId, taskInfo);
            }
        }

        return taskInfo;
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
        // 从内存删除
        memoryCache.remove(paperId);

        // 从Redis删除（如果启用）
        if (useRedis) {
            try {
                String redisKey = REDIS_KEY_PREFIX + paperId;
                redisCache.deleteObject(redisKey);
            } catch (Exception e) {
                log.warn("从Redis删除任务失败，试卷ID：{}，错误：{}", paperId, e.getMessage());
            }
        }

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

    /**
     * 创建任务信息的副本（清除线程引用，用于序列化）
     */
    private PackageTaskInfo createTaskInfoCopy(PackageTaskInfo source) {
        if (source == null) {
            return null;
        }
        PackageTaskInfo copy = new PackageTaskInfo();
        copy.setPaperId(source.getPaperId());
        copy.setStatus(source.getStatus());
        copy.setProgress(source.getProgress());
        copy.setCurrentStep(source.getCurrentStep());
        copy.setErrorMessage(source.getErrorMessage());
        copy.setStartTime(source.getStartTime());
        copy.setFinishTime(source.getFinishTime());
        copy.setNewVersion(source.getNewVersion());
        copy.setCurrentVersion(source.getCurrentVersion()); // 复制当前版本号，用于任务显示
        copy.setPaperName(source.getPaperName()); // 复制试卷名称，用于任务显示
        copy.setUploadId(source.getUploadId()); // 复制上传ID，用于断点续传
        copy.setObjectKey(source.getObjectKey()); // 复制对象键，用于断点续传
        copy.setUploadedParts(source.getUploadedParts()); // 复制已上传分片列表，用于断点续传
        copy.setFileSize(source.getFileSize()); // 复制文件大小，用于断点续传
        copy.setChunkSize(source.getChunkSize()); // 复制分片大小，用于断点续传
        // 不复制线程引用（因为线程对象不能序列化）
        copy.setTaskThread(null);
        return copy;
    }

    @Override
    public List<PackageTaskInfo> getAllTasks() {
        List<PackageTaskInfo> allTasks = new ArrayList<>();
        
        if (useRedis) {
            try {
                // 从Redis获取所有任务（这是主要数据源）
                Collection<String> keys = redisCache.keys(REDIS_KEY_PREFIX + "*");
                if (keys != null && !keys.isEmpty()) {
                    for (String redisKey : keys) {
                        try {
                            PackageTaskInfo taskInfo = redisCache.getCacheObject(redisKey);
                            if (taskInfo != null && taskInfo.getPaperId() != null) {
                                // 创建副本（清除线程引用）
                                allTasks.add(createTaskInfoCopy(taskInfo));
                            }
                        } catch (Exception e) {
                            log.warn("获取任务失败，Redis key: {}, 错误: {}", redisKey, e.getMessage());
                        }
                    }
                }
                
                // 同时检查内存中的任务（可能还没有同步到Redis的新任务）
                for (PackageTaskInfo taskInfo : memoryCache.values()) {
                    if (taskInfo != null && taskInfo.getPaperId() != null) {
                        // 检查是否已经在列表中（避免重复）
                        boolean exists = allTasks.stream()
                                .anyMatch(t -> t.getPaperId() != null && t.getPaperId().equals(taskInfo.getPaperId()));
                        if (!exists) {
                            // 创建副本（清除线程引用）
                            allTasks.add(createTaskInfoCopy(taskInfo));
                        }
                    }
                }
            } catch (Exception e) {
                log.error("获取所有任务失败", e);
            }
        } else {
            // 如果没有启用Redis，只返回内存中的任务
            for (PackageTaskInfo taskInfo : memoryCache.values()) {
                if (taskInfo != null && taskInfo.getPaperId() != null) {
                    // 创建副本（清除线程引用）
                    allTasks.add(createTaskInfoCopy(taskInfo));
                }
            }
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
     * 保存任务信息（同时保存到内存和Redis）
     */
    @Override
    public void saveTask(Integer paperId, PackageTaskInfo taskInfo) {
        // 保存到内存（优先，快速响应）
        // 注意：保存到内存时，需要创建一个副本，避免线程引用被序列化
        PackageTaskInfo memoryTask = new PackageTaskInfo();
        memoryTask.setPaperId(taskInfo.getPaperId());
        memoryTask.setStatus(taskInfo.getStatus());
        memoryTask.setProgress(taskInfo.getProgress());
        memoryTask.setCurrentStep(taskInfo.getCurrentStep());
        memoryTask.setErrorMessage(taskInfo.getErrorMessage());
        memoryTask.setStartTime(taskInfo.getStartTime());
        memoryTask.setFinishTime(taskInfo.getFinishTime());
        memoryTask.setNewVersion(taskInfo.getNewVersion());
        memoryTask.setCurrentVersion(taskInfo.getCurrentVersion()); // 复制当前版本号
        memoryTask.setPaperName(taskInfo.getPaperName()); // 复制试卷名称
        memoryTask.setUploadId(taskInfo.getUploadId()); // 复制上传ID，用于断点续传
        memoryTask.setObjectKey(taskInfo.getObjectKey()); // 复制对象键，用于断点续传
        memoryTask.setUploadedParts(taskInfo.getUploadedParts()); // 复制已上传分片列表，用于断点续传
        memoryTask.setFileSize(taskInfo.getFileSize()); // 复制文件大小，用于断点续传
        memoryTask.setChunkSize(taskInfo.getChunkSize()); // 复制分片大小，用于断点续传
        // 保留线程引用（用于取消任务）
        memoryTask.setTaskThread(taskInfo.getTaskThread());
        memoryCache.put(paperId, memoryTask);

        // 保存到Redis（如果启用，失败不阻塞）
        // 注意：保存到Redis时，需要清除线程引用（因为线程对象不能序列化）
        if (useRedis) {
            try {
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
            } catch (Exception e) {
                // Redis操作失败，记录日志但不阻塞
                log.warn("保存任务到Redis失败，继续使用内存存储，试卷ID：{}，错误：{}", paperId, e.getMessage());
            }
        }
    }
}