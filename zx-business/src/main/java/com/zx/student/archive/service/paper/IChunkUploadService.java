package com.zx.student.archive.service.paper;

import com.zx.student.archive.domain.bo.paper.ChunkUploadCompleteBO;
import com.zx.student.archive.domain.bo.paper.ChunkUploadInitBO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 分片上传服务接口
 * 
 * @author zx
 */
public interface IChunkUploadService {
    
    /**
     * 初始化分片上传
     * 
     * @param initBO 初始化参数
     * @return 上传ID
     */
    String initChunkUpload(ChunkUploadInitBO initBO);
    
    /**
     * 上传分片
     * 
     * @param uploadId 上传ID
     * @param chunkIndex 分片索引
     * @param chunkFile 分片文件
     */
    void uploadChunk(String uploadId, Integer chunkIndex, MultipartFile chunkFile);
    
    /**
     * 完成分片上传（合并分片并上传到OSS）
     * 
     * @param completeBO 完成参数
     * @return OSS文件URL
     */
    String completeChunkUpload(ChunkUploadCompleteBO completeBO);
    
    /**
     * 取消分片上传（清理临时数据）
     * 
     * @param uploadId 上传ID
     */
    void cancelChunkUpload(String uploadId);
}









