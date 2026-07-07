package com.zx.student.archive.service.paper.impl;

import com.zx.common.core.redis.RedisCache;
import com.zx.common.exception.ServiceException;
import com.zx.common.utils.oss.exam.question.OssUtil;
import com.zx.student.archive.domain.bo.paper.ChunkUploadCompleteBO;
import com.zx.student.archive.domain.bo.paper.ChunkUploadInitBO;
import com.zx.student.archive.service.paper.IChunkUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * ???????????????
 * 
 * @author zx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkUploadServiceImpl implements IChunkUploadService {
    
    private final RedisCache redisCache;
    private final OssUtil ossUtil;
    
    /**
     * ????????????????????1???
     */
    @Value("${paper.chunk.upload.expire-time:3600}")
    private int expireTime;
    
    /**
     * Redis key??
     */
    private static final String REDIS_KEY_PREFIX = "paper:chunk:upload:";
    
    /**
     * ????????????????????????
     */
    private final ConcurrentHashMap<String, ChunkUploadInfo> memoryCache = new ConcurrentHashMap<>();
    
    /**
     * ?????????????????Serializable???????Redis??
     */
    public static class ChunkUploadInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        Integer paperId;
        String fileName;
        Long fileSize;
        Long chunkSize;
        Integer chunkCount;
        java.util.Map<Integer, byte[]> chunks = new java.util.concurrent.ConcurrentHashMap<>();
        Integer uploadedChunks = 0; // ??Integer???AtomicInteger?AtomicInteger??????
        Long createTime;
        
        // ?????????????
        synchronized int incrementUploadedChunks() {
            uploadedChunks = (uploadedChunks == null ? 0 : uploadedChunks) + 1;
            return uploadedChunks;
        }
        
        // ????????
        int getUploadedChunks() {
            return uploadedChunks == null ? 0 : uploadedChunks;
        }
    }
    
    @Override
    public String initChunkUpload(ChunkUploadInitBO initBO) {
        String uploadId = UUID.randomUUID().toString();
        
        // ??????????
        long chunkSize = initBO.getChunkSize() != null ? initBO.getChunkSize() : 10L * 1024 * 1024;
        int chunkCount = (int) Math.ceil((double) initBO.getFileSize() / chunkSize);
        
        ChunkUploadInfo info = new ChunkUploadInfo();
        info.paperId = initBO.getPaperId();
        info.fileName = initBO.getFileName();
        info.fileSize = initBO.getFileSize();
        info.chunkSize = chunkSize;
        info.chunkCount = chunkCount;
        info.createTime = System.currentTimeMillis();
        
        // ????????
        memoryCache.put(uploadId, info);
        
        // ?????Redis??????????????????
        String redisKey = REDIS_KEY_PREFIX + uploadId;
        redisCache.setCacheObject(redisKey, info, expireTime, TimeUnit.SECONDS);
        
        log.info("????????????????ID: {}, ???ID: {}, ???????: {} ???, ???????: {}", 
                uploadId, initBO.getPaperId(), initBO.getFileSize(), chunkCount);
        
        return uploadId;
    }
    
    @Override
    public void uploadChunk(String uploadId, Integer chunkIndex, MultipartFile chunkFile) {
        ChunkUploadInfo info = getChunkUploadInfo(uploadId);
        
        if (info == null) {
            throw new ServiceException("???ID????????????: " + uploadId);
        }
        
        if (chunkIndex < 0 || chunkIndex >= info.chunkCount) {
            throw new ServiceException("???????????????: " + chunkIndex + ", ??????: " + info.chunkCount);
        }
        
        try {
            // ??????????
            byte[] chunkData = chunkFile.getBytes();
            
            // ??????
            info.chunks.put(chunkIndex, chunkData);
            int uploadedCount = info.incrementUploadedChunks();
            
            log.debug("??????????????ID: {}, ???????: {}, ???????: {} ???, ?????: {}/{}", 
                    uploadId, chunkIndex, chunkData.length, uploadedCount, info.chunkCount);
            
            // ????Redis
            String redisKey = REDIS_KEY_PREFIX + uploadId;
            redisCache.setCacheObject(redisKey, info, expireTime, TimeUnit.SECONDS);
            
        } catch (IOException e) {
            log.error("?????????????????ID: {}, ???????: {}", uploadId, chunkIndex, e);
            throw new ServiceException("?????????????: " + e.getMessage());
        }
    }
    
    @Override
    public String completeChunkUpload(ChunkUploadCompleteBO completeBO) {
        String uploadId = completeBO.getUploadId();
        ChunkUploadInfo info = getChunkUploadInfo(uploadId);
        
        if (info == null) {
            throw new ServiceException("???ID????????????: " + uploadId);
        }
        
        // ????????????????????
        if (info.getUploadedChunks() != info.chunkCount) {
            throw new ServiceException("??????????????????: " + info.getUploadedChunks() + 
                    ", ??????: " + info.chunkCount);
        }
        
        try {
            // ??????
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            for (int i = 0; i < info.chunkCount; i++) {
                byte[] chunk = info.chunks.get(i);
                if (chunk == null) {
                    throw new ServiceException("?????: " + i);
                }
                outputStream.write(chunk);
            }
            byte[] fileData = outputStream.toByteArray();
            
            // ??????????
            if (fileData.length != info.fileSize) {
                log.warn("??????????????????????: {}, ???: {}", info.fileSize, fileData.length);
            }
            
            // ?????OSS
            String objectKey = "paper_packages/" + info.fileName;
            String fileUrl = ossUtil.upload(fileData, objectKey, "application/zip");
            
            log.info("?????????????ID: {}, ???ID: {}, ???URL: {}", 
                    uploadId, info.paperId, fileUrl);
            
            // ???????????
            cleanup(uploadId);
            
            return fileUrl;
            
        } catch (Exception e) {
            log.error("???????????????ID: {}", uploadId, e);
            throw new ServiceException("???????????: " + e.getMessage());
        }
    }
    
    @Override
    public void cancelChunkUpload(String uploadId) {
        cleanup(uploadId);
        log.info("??????????????ID: {}", uploadId);
    }
    
    /**
     * ????????????
     */
    private ChunkUploadInfo getChunkUploadInfo(String uploadId) {
        // ????????
        ChunkUploadInfo info = memoryCache.get(uploadId);
        
        // ???????????????Redis???
        if (info == null) {
            String redisKey = REDIS_KEY_PREFIX + uploadId;
            info = redisCache.getCacheObject(redisKey);
            if (info != null) {
                memoryCache.put(uploadId, info);
            }
        }
        
        return info;
    }
    
    /**
     * ???????????
     */
    private void cleanup(String uploadId) {
        memoryCache.remove(uploadId);
        String redisKey = REDIS_KEY_PREFIX + uploadId;
        redisCache.deleteObject(redisKey);
    }
}









