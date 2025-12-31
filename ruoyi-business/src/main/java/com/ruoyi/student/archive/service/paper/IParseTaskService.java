package com.ruoyi.student.archive.service.paper;

import com.ruoyi.student.archive.domain.dto.paper.ParseTaskDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 解析任务管理服务接口
 * 
 * @author ruoyi
 */
public interface IParseTaskService {

    /**
     * 提交解析任务
     * 
     * @param wordFile    Word文件
     * @param audioFile   音频文件（可选）
     * @param audioOssUrl 音频 OSS URL（可选，用于 ASR 识别）
     * @return 任务ID
     */
    String submitTask(MultipartFile wordFile, MultipartFile audioFile, String audioOssUrl);

    /**
     * 获取任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态
     */
    ParseTaskDTO getTaskStatus(String taskId);

    /**
     * 取消任务
     * 
     * @param taskId 任务ID
     * @return 是否成功
     */
    boolean cancelTask(String taskId);

    /**
     * 异步执行解析任务（内部使用）
     * 
     * @param taskId 任务ID
     */
    void executeParseAsync(String taskId);
}
