package com.ruoyi.student.archive.service.paper.impl;

import com.ruoyi.student.archive.domain.dto.paper.ParseResultDTO;
import com.ruoyi.student.archive.domain.dto.paper.ParseTaskDTO;
import com.ruoyi.student.archive.service.paper.IPaperImportService;
import com.ruoyi.student.archive.service.paper.IParseTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 解析任务管理服务实现
 * 使用内存缓存存储任务状态（生产环境建议使用 Redis）
 * 
 * @author ruoyi
 */
@Slf4j
@Service
public class ParseTaskServiceImpl implements IParseTaskService {

    @Autowired
    private IPaperImportService paperImportService;

    /**
     * 自注入：用于调用带有 @Async 的方法（绕过 Spring AOP 代理问题）
     * 注意：必须使用接口类型，因为 Spring 使用 JDK 动态代理
     */
    @Lazy
    @Autowired
    private IParseTaskService self;

    /**
     * 任务缓存（生产环境建议使用 Redis）
     */
    private static final Map<String, ParseTaskDTO> TASK_CACHE = new ConcurrentHashMap<>();

    /**
     * 文件临时存储（用于异步处理）
     */
    private static final Map<String, FileHolder> FILE_CACHE = new ConcurrentHashMap<>();

    /**
     * 任务最大保留时间（毫秒）- 1小时
     */
    private static final long TASK_EXPIRE_TIME = 60 * 60 * 1000;

    @Override
    public String submitTask(MultipartFile wordFile, MultipartFile audioFile, String audioOssUrl) {
        // 生成任务ID
        String taskId = UUID.randomUUID().toString().replace("-", "");

        // 创建任务状态
        ParseTaskDTO task = new ParseTaskDTO();
        task.setTaskId(taskId);
        task.setStatus("PENDING");
        task.setProgress(0);
        task.setCurrentStep("任务已提交，等待处理...");
        task.setCreateTime(LocalDateTime.now());

        // 保存任务状态
        TASK_CACHE.put(taskId, task);

        // 保存文件到临时缓存
        try {
            FileHolder holder = new FileHolder();
            holder.wordFileName = wordFile.getOriginalFilename();
            holder.wordFileBytes = wordFile.getBytes();
            if (audioFile != null && !audioFile.isEmpty()) {
                holder.audioFileName = audioFile.getOriginalFilename();
                holder.audioFileBytes = audioFile.getBytes();
            }
            // 保存音频 OSS URL（用于 ASR 识别）
            holder.audioOssUrl = audioOssUrl;
            FILE_CACHE.put(taskId, holder);
        } catch (IOException e) {
            task.setStatus("FAILED");
            task.setErrorMessage("文件读取失败: " + e.getMessage());
            log.error("文件读取失败", e);
            return taskId;
        }

        // 通过代理调用异步方法（关键！使用 self 而不是 this）
        self.executeParseAsync(taskId);

        log.info("解析任务已提交: taskId={}, audioOssUrl={}", taskId, audioOssUrl);
        return taskId;
    }

    @Override
    @Async
    public void executeParseAsync(String taskId) {
        ParseTaskDTO task = TASK_CACHE.get(taskId);
        FileHolder files = FILE_CACHE.get(taskId);

        if (task == null || files == null) {
            log.error("任务或文件不存在: taskId={}", taskId);
            return;
        }

        try {
            // 更新状态：处理中
            task.setStatus("PROCESSING");
            task.setProgress(10);
            task.setCurrentStep("正在解析 Word 文档...");

            // 创建临时 MultipartFile
            MultipartFile wordFile = new ByteArrayMultipartFile(
                    files.wordFileName,
                    files.wordFileBytes,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

            MultipartFile audioFile = null;
            if (files.audioFileBytes != null) {
                task.setProgress(20);
                task.setCurrentStep("正在处理音频文件...");
                audioFile = new ByteArrayMultipartFile(
                        files.audioFileName,
                        files.audioFileBytes,
                        "audio/mpeg");
            }

            task.setProgress(30);
            task.setCurrentStep("正在调用解析服务...");

            // 调用解析服务（传递音频 OSS URL 用于 ASR）
            ParseResultDTO result = paperImportService.parseFiles(wordFile, audioFile, files.audioOssUrl);

            // 更新状态：完成
            task.setStatus("COMPLETED");
            task.setProgress(100);
            task.setCurrentStep("解析完成");
            task.setResult(result);
            task.setCompleteTime(LocalDateTime.now());

            log.info("解析任务完成: taskId={}, questionCount={}", taskId, result.getQuestionCount());

        } catch (Exception e) {
            // 更新状态：失败
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            task.setCompleteTime(LocalDateTime.now());
            log.error("解析任务失败: taskId={}", taskId, e);
        } finally {
            // 清理文件缓存
            FILE_CACHE.remove(taskId);
        }
    }

    @Override
    public ParseTaskDTO getTaskStatus(String taskId) {
        ParseTaskDTO task = TASK_CACHE.get(taskId);
        if (task == null) {
            ParseTaskDTO notFound = new ParseTaskDTO();
            notFound.setTaskId(taskId);
            notFound.setStatus("NOT_FOUND");
            notFound.setErrorMessage("任务不存在或已过期");
            return notFound;
        }

        // 检查是否过期
        if (task.getCreateTime() != null) {
            long elapsed = System.currentTimeMillis() -
                    java.sql.Timestamp.valueOf(task.getCreateTime()).getTime();
            if (elapsed > TASK_EXPIRE_TIME &&
                    ("COMPLETED".equals(task.getStatus()) || "FAILED".equals(task.getStatus()))) {
                TASK_CACHE.remove(taskId);
                ParseTaskDTO expired = new ParseTaskDTO();
                expired.setTaskId(taskId);
                expired.setStatus("EXPIRED");
                expired.setErrorMessage("任务已过期");
                return expired;
            }
        }

        return task;
    }

    @Override
    public boolean cancelTask(String taskId) {
        ParseTaskDTO task = TASK_CACHE.get(taskId);
        if (task != null && "PENDING".equals(task.getStatus())) {
            task.setStatus("CANCELLED");
            task.setErrorMessage("任务已取消");
            FILE_CACHE.remove(taskId);
            return true;
        }
        return false;
    }

    /**
     * 文件临时存储
     */
    private static class FileHolder {
        String wordFileName;
        byte[] wordFileBytes;
        String audioFileName;
        byte[] audioFileBytes;
        String audioOssUrl; // 音频 OSS URL（用于 ASR 识别）
    }

    /**
     * 字节数组实现的 MultipartFile
     */
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final String name;
        private final byte[] content;
        private final String contentType;

        public ByteArrayMultipartFile(String name, byte[] content, String contentType) {
            this.name = name;
            this.content = content;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() {
            return content;
        }

        @Override
        public java.io.InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
