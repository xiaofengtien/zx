package com.zx.common.utils.oss.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.ListPartsRequest;
import com.aliyun.oss.model.PartListing;
import com.aliyun.oss.model.PartSummary;
import com.zx.common.config.OssConfig;
import com.zx.common.exception.ServiceException;
import com.zx.common.utils.StringUtils;
import com.zx.common.utils.oss.IOssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

/**
 * 阿里云OSS服务实现
 * 
 * @author zx
 */
@Slf4j
@Component
public class AliyunOssService implements IOssService {

    @Autowired
    private OssConfig ossConfig;

    private OSS ossClient;

    /**
     * 题目媒体文件基础路径
     */
    private static final String BASE_PATH = "exam/question/";

    /**
     * 初始化OSS客户端
     */
    @PostConstruct
    public void init() {
        try {
            ossClient = new OSSClientBuilder().build(
                    ossConfig.getEndpoint(),
                    ossConfig.getAccessKeyId(),
                    ossConfig.getAccessKeySecret());
            log.info("阿里云OSS客户端初始化成功，Bucket: {}", ossConfig.getBucketName());
        } catch (Exception e) {
            log.error("阿里云OSS客户端初始化失败", e);
            throw new ServiceException("阿里云OSS客户端初始化失败: " + e.getMessage());
        }
    }

    /**
     * 销毁OSS客户端
     */
    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
            log.info("阿里云OSS客户端已关闭");
        }
    }

    @Override
    public String upload(MultipartFile file, String fileName) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }

        try {
            String objectKey = generateObjectKey(fileName != null ? fileName : file.getOriginalFilename());

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            String contentType = determineContentType(file.getOriginalFilename(), file.getContentType());
            metadata.setContentType(contentType);

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossConfig.getBucketName(),
                    objectKey,
                    file.getInputStream(),
                    metadata);

            ossClient.putObject(putObjectRequest);

            String fileUrl = buildFileUrl(objectKey);
            log.info("阿里云OSS文件上传成功: {} -> {}", objectKey, fileUrl);

            return fileUrl;

        } catch (Exception e) {
            log.error("阿里云OSS文件上传失败", e);
            throw new ServiceException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public String upload(InputStream inputStream, String objectKey, String contentType) {
        if (inputStream == null) {
            throw new ServiceException("文件流不能为空");
        }

        try {
            if (StringUtils.isEmpty(objectKey)) {
                objectKey = generateObjectKey(null);
            }

            ObjectMetadata metadata = new ObjectMetadata();
            String finalContentType = StringUtils.isNotEmpty(contentType)
                    ? contentType
                    : determineContentType(objectKey, null);
            metadata.setContentType(finalContentType);

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossConfig.getBucketName(),
                    objectKey,
                    inputStream,
                    metadata);

            ossClient.putObject(putObjectRequest);

            String fileUrl = buildFileUrl(objectKey);
            log.info("阿里云OSS文件流上传成功: {} -> {}", objectKey, fileUrl);

            return fileUrl;

        } catch (Exception e) {
            log.error("阿里云OSS文件流上传失败", e);
            throw new ServiceException("文件流上传失败: " + e.getMessage());
        }
    }

    @Override
    public String upload(byte[] bytes, String objectKey, String contentType) {
        if (bytes == null || bytes.length == 0) {
            throw new ServiceException("文件字节数组不能为空");
        }

        try {
            if (StringUtils.isEmpty(objectKey)) {
                objectKey = generateObjectKey(null);
            }

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(bytes.length);
            String finalContentType = StringUtils.isNotEmpty(contentType)
                    ? contentType
                    : determineContentType(objectKey, null);
            metadata.setContentType(finalContentType);

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    ossConfig.getBucketName(),
                    objectKey,
                    new java.io.ByteArrayInputStream(bytes),
                    metadata);

            ossClient.putObject(putObjectRequest);

            String fileUrl = buildFileUrl(objectKey);
            log.info("阿里云OSS字节数组上传成功: {} -> {}", objectKey, fileUrl);

            return fileUrl;

        } catch (Exception e) {
            log.error("阿里云OSS字节数组上传失败", e);
            throw new ServiceException("字节数组上传失败: " + e.getMessage());
        }
    }

    @Override
    public boolean delete(String objectKey) {
        if (StringUtils.isEmpty(objectKey)) {
            return false;
        }

        try {
            String key = extractObjectKey(objectKey);
            ossClient.deleteObject(ossConfig.getBucketName(), key);
            log.info("阿里云OSS文件删除成功: {}", key);
            return true;
        } catch (Exception e) {
            log.error("阿里云OSS文件删除失败: {}", objectKey, e);
            return false;
        }
    }

    @Override
    public boolean exists(String objectKey) {
        if (StringUtils.isEmpty(objectKey)) {
            return false;
        }

        try {
            String key = extractObjectKey(objectKey);
            return ossClient.doesObjectExist(ossConfig.getBucketName(), key);
        } catch (Exception e) {
            log.error("阿里云OSS检查文件是否存在失败: {}", objectKey, e);
            return false;
        }
    }

    @Override
    public byte[] downloadToBytes(String objectKey) {
        try {
            String key = extractObjectKey(objectKey);
            if (!ossClient.doesObjectExist(ossConfig.getBucketName(), key)) {
                throw new ServiceException("文件不存在: " + key);
            }

            com.aliyun.oss.model.OSSObject ossObject = ossClient.getObject(ossConfig.getBucketName(), key);
            try (InputStream inputStream = ossObject.getObjectContent()) {
                return org.apache.commons.io.IOUtils.toByteArray(inputStream);
            }
        } catch (Exception e) {
            log.error("阿里云OSS文件下载失败: {}", objectKey, e);
            throw new ServiceException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadToStream(String objectKey, long start, long end) {
        try {
            String key = extractObjectKey(objectKey);
            if (!ossClient.doesObjectExist(ossConfig.getBucketName(), key)) {
                throw new ServiceException("文件不存在: " + key);
            }

            // 创建GetObjectRequest，设置Range
            com.aliyun.oss.model.GetObjectRequest getObjectRequest = new com.aliyun.oss.model.GetObjectRequest(
                    ossConfig.getBucketName(), key);
            getObjectRequest.setRange(start, end);

            // 获取对象
            com.aliyun.oss.model.OSSObject ossObject = ossClient.getObject(getObjectRequest);
            return ossObject.getObjectContent();

        } catch (Exception e) {
            log.error("阿里云OSS流式下载失败: {}, Range: {}-{}", objectKey, start, end, e);
            throw new ServiceException("流式下载失败: " + e.getMessage());
        }
    }

    @Override
    public long getFileSize(String objectKey) {
        try {
            String key = extractObjectKey(objectKey);
            if (!ossClient.doesObjectExist(ossConfig.getBucketName(), key)) {
                throw new ServiceException("文件不存在: " + key);
            }

            // 获取对象元数据
            com.aliyun.oss.model.ObjectMetadata metadata = ossClient.getObjectMetadata(
                    ossConfig.getBucketName(), key);
            return metadata.getContentLength();

        } catch (Exception e) {
            log.error("获取文件大小失败: {}", objectKey, e);
            throw new ServiceException("获取文件大小失败: " + e.getMessage());
        }
    }

    @Override
    public String uploadMultipart(InputStream inputStream, String objectKey, String contentType,
            long fileSize, Long chunkSize,
            java.util.function.BiConsumer<Long, Long> progressCallback) {
        if (inputStream == null) {
            throw new ServiceException("文件流不能为空");
        }

        try {
            String key = extractObjectKey(objectKey);
            if (StringUtils.isEmpty(key)) {
                key = generateObjectKey(null);
            }

            String finalContentType = StringUtils.isNotEmpty(contentType)
                    ? contentType
                    : determineContentType(key, null);

            // 动态计算分片大小（根据文件大小优化）
            long chunkSizeBytes;
            if (chunkSize != null && chunkSize > 0) {
                chunkSizeBytes = chunkSize;
            } else {
                // 根据文件大小动态调整分片大小
                if (fileSize < 50L * 1024 * 1024) {
                    chunkSizeBytes = 10L * 1024 * 1024; // 50MB以下用10MB分片
                } else if (fileSize < 200L * 1024 * 1024) {
                    chunkSizeBytes = 20L * 1024 * 1024; // 200MB以下用20MB分片
                } else if (fileSize < 500L * 1024 * 1024) {
                    chunkSizeBytes = 50L * 1024 * 1024; // 500MB以下用50MB分片
                } else {
                    chunkSizeBytes = 100L * 1024 * 1024; // 500MB以上用100MB分片
                }
            }

            // 如果文件大小小于等于50MB，使用普通上传（更快）
            if (fileSize <= 50L * 1024 * 1024) {
                log.info("文件大小 {} MB，使用普通上传", fileSize / 1024.0 / 1024.0);
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(fileSize);
                metadata.setContentType(finalContentType);
                PutObjectRequest putObjectRequest = new PutObjectRequest(
                        ossConfig.getBucketName(), key, inputStream, metadata);
                ossClient.putObject(putObjectRequest);
                String fileUrl = buildFileUrl(key);
                log.info("阿里云OSS文件上传成功: {} -> {}", key, fileUrl);
                return fileUrl;
            }

            // 大文件使用分片上传（Multipart Upload）
            log.info("文件大小 {} MB，使用分片上传，分片大小: {} MB",
                    fileSize / 1024.0 / 1024.0, chunkSizeBytes / 1024.0 / 1024.0);

            // 1. 初始化分片上传（新上传）
            com.aliyun.oss.model.InitiateMultipartUploadRequest initRequest = new com.aliyun.oss.model.InitiateMultipartUploadRequest(
                    ossConfig.getBucketName(), key);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(finalContentType);
            initRequest.setObjectMetadata(metadata);

            com.aliyun.oss.model.InitiateMultipartUploadResult initResult = ossClient
                    .initiateMultipartUpload(initRequest);
            String uploadId = initResult.getUploadId();

            log.debug("初始化分片上传成功，UploadId: {}", uploadId);

            // 2. 先读取所有分片数据（用于并发上传）
            java.util.List<ChunkData> chunks = new java.util.ArrayList<>();
            byte[] buffer = new byte[(int) chunkSizeBytes];
            int bytesRead;
            int partNumber = 1;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] chunkData = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                chunks.add(new ChunkData(partNumber, chunkData));
                partNumber++;
            }

            log.debug("已读取 {} 个分片，准备并发上传", chunks.size());

            // 3. 并发上传分片（使用线程池）
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(4); // 4个并发线程
            java.util.List<java.util.concurrent.Future<com.aliyun.oss.model.PartETag>> futures = new java.util.ArrayList<>();
            java.util.concurrent.atomic.AtomicLong uploadedBytes = new java.util.concurrent.atomic.AtomicLong(0);
            java.util.List<com.aliyun.oss.model.PartETag> partETags = new java.util.ArrayList<>();

            try {
                // 提交所有分片上传任务
                for (ChunkData chunk : chunks) {
                    String finalKey = key;
                    java.util.concurrent.Future<com.aliyun.oss.model.PartETag> future = executor.submit(() -> {
                        java.io.ByteArrayInputStream partInputStream = new java.io.ByteArrayInputStream(chunk.data);

                        com.aliyun.oss.model.UploadPartRequest uploadPartRequest = new com.aliyun.oss.model.UploadPartRequest();
                        uploadPartRequest.setBucketName(ossConfig.getBucketName());
                        uploadPartRequest.setKey(finalKey);
                        uploadPartRequest.setUploadId(uploadId);
                        uploadPartRequest.setInputStream(partInputStream);
                        uploadPartRequest.setPartNumber(chunk.partNumber);
                        uploadPartRequest.setPartSize(chunk.data.length);

                        com.aliyun.oss.model.UploadPartResult uploadPartResult = ossClient
                                .uploadPart(uploadPartRequest);

                        long currentUploaded = uploadedBytes.addAndGet(chunk.data.length);

                        // 进度回调
                        if (progressCallback != null) {
                            progressCallback.accept(currentUploaded, fileSize);
                        }

                        log.debug("上传分片 {} 成功，已上传: {} / {} MB",
                                chunk.partNumber, currentUploaded / 1024.0 / 1024.0, fileSize / 1024.0 / 1024.0);

                        return new com.aliyun.oss.model.PartETag(
                                chunk.partNumber, uploadPartResult.getETag());
                    });
                    futures.add(future);
                }

                // 等待所有分片上传完成
                for (java.util.concurrent.Future<com.aliyun.oss.model.PartETag> future : futures) {
                    partETags.add(future.get());
                }

                // 按partNumber排序（确保顺序正确）
                partETags.sort((a, b) -> Integer.compare(a.getPartNumber(), b.getPartNumber()));

                executor.shutdown();

            } catch (Exception e) {
                executor.shutdownNow();
                // 如果上传失败，不清除已上传的分片（支持断点续传）
                // 注意：这里不再自动取消上传，而是保留已上传的分片，允许后续续传
                log.warn("分片上传过程中出现异常，已上传的分片将保留，支持断点续传。UploadId: {}, 错误: {}",
                        uploadId, e.getMessage());
                throw e;
            }

            // 4. 完成分片上传（使用排序后的partETags）
            com.aliyun.oss.model.CompleteMultipartUploadRequest completeRequest = new com.aliyun.oss.model.CompleteMultipartUploadRequest(
                    ossConfig.getBucketName(), key, uploadId, partETags);
            com.aliyun.oss.model.CompleteMultipartUploadResult completeResult = ossClient
                    .completeMultipartUpload(completeRequest);

            // 最终进度回调
            if (progressCallback != null) {
                progressCallback.accept(fileSize, fileSize);
            }

            String fileUrl = buildFileUrl(key);
            log.info("阿里云OSS分片上传成功: {} -> {}, 文件大小: {} MB, 分片数: {}",
                    key, fileUrl, fileSize / 1024.0 / 1024.0, partETags.size());

            return fileUrl;

        } catch (Exception e) {
            log.error("阿里云OSS分片上传失败: {}", objectKey, e);
            throw new ServiceException("分片上传失败: " + e.getMessage());
        }
    }

    @Override
    public UploadResult uploadMultipartWithResume(InputStream inputStream, String objectKey, String contentType,
            long fileSize, Long chunkSize,
            String uploadId, java.util.List<PartInfo> uploadedParts,
            java.util.function.BiConsumer<Long, Long> progressCallback) {
        if (inputStream == null) {
            throw new ServiceException("文件流不能为空");
        }

        try {
            String key = extractObjectKey(objectKey);
            if (StringUtils.isEmpty(key)) {
                key = generateObjectKey(null);
            }

            String finalContentType = StringUtils.isNotEmpty(contentType)
                    ? contentType
                    : determineContentType(key, null);

            // 动态计算分片大小
            long chunkSizeBytes;
            if (chunkSize != null && chunkSize > 0) {
                chunkSizeBytes = chunkSize;
            } else {
                if (fileSize < 50L * 1024 * 1024) {
                    chunkSizeBytes = 10L * 1024 * 1024;
                } else if (fileSize < 200L * 1024 * 1024) {
                    chunkSizeBytes = 20L * 1024 * 1024;
                } else if (fileSize < 500L * 1024 * 1024) {
                    chunkSizeBytes = 50L * 1024 * 1024;
                } else {
                    chunkSizeBytes = 100L * 1024 * 1024;
                }
            }

            // 如果文件大小小于等于50MB，使用普通上传
            if (fileSize <= 50L * 1024 * 1024) {
                log.info("文件大小 {} MB，使用普通上传", fileSize / 1024.0 / 1024.0);
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(fileSize);
                metadata.setContentType(finalContentType);
                PutObjectRequest putObjectRequest = new PutObjectRequest(
                        ossConfig.getBucketName(), key, inputStream, metadata);
                ossClient.putObject(putObjectRequest);
                String fileUrl = buildFileUrl(key);
                log.info("阿里云OSS文件上传成功: {} -> {}", key, fileUrl);
                return new UploadResult(fileUrl, null, null);
            }

            // 大文件使用分片上传（支持断点续传）
            boolean isResume = uploadId != null && !uploadId.isEmpty() && uploadedParts != null;
            String finalUploadId;
            java.util.List<com.aliyun.oss.model.PartETag> existingPartETags = new java.util.ArrayList<>();

            if (isResume) {
                // 断点续传：使用已有的uploadId
                log.info("使用断点续传，UploadId: {}，已上传分片数: {}", uploadId, uploadedParts.size());
                finalUploadId = uploadId;

                // 查询OSS上已上传的分片（确保数据一致性）
                java.util.List<PartInfo> ossParts = listUploadedParts(key, uploadId);
                java.util.Map<Integer, PartInfo> ossPartsMap = new java.util.HashMap<>();
                for (PartInfo part : ossParts) {
                    ossPartsMap.put(part.getPartNumber(), part);
                }

                // 合并已上传的分片信息（优先使用OSS上的数据，因为OSS是权威数据源）
                // 只使用OSS上实际存在的分片
                for (PartInfo ossPart : ossParts) {
                    existingPartETags.add(new com.aliyun.oss.model.PartETag(
                            ossPart.getPartNumber(), ossPart.getETag()));
                }

                log.info("断点续传：已确认 {} 个分片已上传（从OSS查询）", existingPartETags.size());
            } else {
                // 新上传：初始化分片上传
                log.info("文件大小 {} MB，使用分片上传，分片大小: {} MB",
                        fileSize / 1024.0 / 1024.0, chunkSizeBytes / 1024.0 / 1024.0);

                com.aliyun.oss.model.InitiateMultipartUploadRequest initRequest = new com.aliyun.oss.model.InitiateMultipartUploadRequest(
                        ossConfig.getBucketName(), key);
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentType(finalContentType);
                initRequest.setObjectMetadata(metadata);

                com.aliyun.oss.model.InitiateMultipartUploadResult initResult = ossClient
                        .initiateMultipartUpload(initRequest);
                finalUploadId = initResult.getUploadId();

                log.debug("初始化分片上传成功，UploadId: {}", finalUploadId);
            }

            // 计算需要上传的分片
            int totalParts = (int) Math.ceil((double) fileSize / chunkSizeBytes);
            java.util.Set<Integer> uploadedPartNumbers = new java.util.HashSet<>();
            for (com.aliyun.oss.model.PartETag tag : existingPartETags) {
                uploadedPartNumbers.add(tag.getPartNumber());
            }

            // 读取所有分片数据
            java.util.List<ChunkData> chunks = new java.util.ArrayList<>();
            byte[] buffer = new byte[(int) chunkSizeBytes];
            int bytesRead;
            int partNumber = 1;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] chunkData = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                chunks.add(new ChunkData(partNumber, chunkData));
                partNumber++;
            }

            // 过滤出需要上传的分片（跳过已上传的）
            java.util.List<ChunkData> chunksToUpload = new java.util.ArrayList<>();
            for (ChunkData chunk : chunks) {
                if (!uploadedPartNumbers.contains(chunk.partNumber)) {
                    chunksToUpload.add(chunk);
                }
            }

            log.info("总分片数: {}，已上传: {}，待上传: {}",
                    totalParts, existingPartETags.size(), chunksToUpload.size());

            // 并发上传剩余分片
            java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(4);
            java.util.List<java.util.concurrent.Future<com.aliyun.oss.model.PartETag>> futures = new java.util.ArrayList<>();
            java.util.concurrent.atomic.AtomicLong uploadedBytes = new java.util.concurrent.atomic.AtomicLong(0);

            // 计算已上传的字节数
            for (com.aliyun.oss.model.PartETag tag : existingPartETags) {
                // 估算已上传的字节数（使用分片大小）
                uploadedBytes.addAndGet(chunkSizeBytes);
            }

            try {
                for (ChunkData chunk : chunksToUpload) {
                    String finalKey = key;
                    java.util.concurrent.Future<com.aliyun.oss.model.PartETag> future = executor.submit(() -> {
                        java.io.ByteArrayInputStream partInputStream = new java.io.ByteArrayInputStream(chunk.data);

                        com.aliyun.oss.model.UploadPartRequest uploadPartRequest = new com.aliyun.oss.model.UploadPartRequest();
                        uploadPartRequest.setBucketName(ossConfig.getBucketName());
                        uploadPartRequest.setKey(finalKey);
                        uploadPartRequest.setUploadId(finalUploadId);
                        uploadPartRequest.setInputStream(partInputStream);
                        uploadPartRequest.setPartNumber(chunk.partNumber);
                        uploadPartRequest.setPartSize(chunk.data.length);

                        com.aliyun.oss.model.UploadPartResult uploadPartResult = ossClient
                                .uploadPart(uploadPartRequest);

                        long currentUploaded = uploadedBytes.addAndGet(chunk.data.length);

                        // 进度回调
                        if (progressCallback != null) {
                            progressCallback.accept(currentUploaded, fileSize);
                        }

                        log.debug("上传分片 {} 成功，已上传: {} / {} MB",
                                chunk.partNumber, currentUploaded / 1024.0 / 1024.0, fileSize / 1024.0 / 1024.0);

                        return new com.aliyun.oss.model.PartETag(
                                chunk.partNumber, uploadPartResult.getETag());
                    });
                    futures.add(future);
                }

                // 等待所有分片上传完成
                java.util.List<com.aliyun.oss.model.PartETag> newPartETags = new java.util.ArrayList<>();
                for (java.util.concurrent.Future<com.aliyun.oss.model.PartETag> future : futures) {
                    newPartETags.add(future.get());
                }

                // 合并所有分片（已上传的 + 新上传的）
                existingPartETags.addAll(newPartETags);
                existingPartETags.sort((a, b) -> Integer.compare(a.getPartNumber(), b.getPartNumber()));

                executor.shutdown();

            } catch (Exception e) {
                executor.shutdownNow();
                // 上传失败，不清除已上传的分片（支持断点续传）
                log.warn("分片上传过程中出现异常，已上传的分片将保留，支持断点续传。UploadId: {}, 错误: {}",
                        finalUploadId, e.getMessage());
                throw e;
            }

            // 完成分片上传
            com.aliyun.oss.model.CompleteMultipartUploadRequest completeRequest = new com.aliyun.oss.model.CompleteMultipartUploadRequest(
                    ossConfig.getBucketName(), key, finalUploadId, existingPartETags);
            com.aliyun.oss.model.CompleteMultipartUploadResult completeResult = ossClient
                    .completeMultipartUpload(completeRequest);

            // 最终进度回调
            if (progressCallback != null) {
                progressCallback.accept(fileSize, fileSize);
            }

            String fileUrl = buildFileUrl(key);
            log.info("阿里云OSS分片上传成功: {} -> {}, 文件大小: {} MB, 分片数: {}",
                    key, fileUrl, fileSize / 1024.0 / 1024.0, existingPartETags.size());

            // 转换为PartInfo列表
            java.util.List<PartInfo> partInfoList = new java.util.ArrayList<>();
            for (com.aliyun.oss.model.PartETag tag : existingPartETags) {
                partInfoList.add(new PartInfo(tag.getPartNumber(), tag.getETag(), chunkSizeBytes));
            }

            return new UploadResult(fileUrl, finalUploadId, partInfoList);

        } catch (Exception e) {
            log.error("阿里云OSS分片上传失败: {}", objectKey, e);
            throw new ServiceException("分片上传失败: " + e.getMessage());
        }
    }

    @Override
    public java.util.List<PartInfo> listUploadedParts(String objectKey, String uploadId) {
        try {
            String key = extractObjectKey(objectKey);
            if (StringUtils.isEmpty(key)) {
                throw new ServiceException("对象键不能为空");
            }
            if (StringUtils.isEmpty(uploadId)) {
                throw new ServiceException("上传ID不能为空");
            }

            com.aliyun.oss.model.ListPartsRequest listPartsRequest = new com.aliyun.oss.model.ListPartsRequest(
                    ossConfig.getBucketName(), key, uploadId);
            com.aliyun.oss.model.PartListing partListing = ossClient.listParts(listPartsRequest);

            java.util.List<PartInfo> partInfoList = new java.util.ArrayList<>();
            for (com.aliyun.oss.model.PartSummary part : partListing.getParts()) {
                partInfoList.add(new PartInfo(
                        part.getPartNumber(),
                        part.getETag(),
                        part.getSize()));
            }

            log.debug("查询已上传分片成功，UploadId: {}，分片数: {}", uploadId, partInfoList.size());
            return partInfoList;
        } catch (Exception e) {
            log.error("查询已上传分片失败: {}, UploadId: {}", objectKey, uploadId, e);
            throw new ServiceException("查询已上传分片失败: " + e.getMessage());
        }
    }

    @Override
    public boolean abortMultipartUpload(String objectKey, String uploadId) {
        try {
            String key = extractObjectKey(objectKey);
            if (StringUtils.isEmpty(key)) {
                throw new ServiceException("对象键不能为空");
            }
            if (StringUtils.isEmpty(uploadId)) {
                throw new ServiceException("上传ID不能为空");
            }

            com.aliyun.oss.model.AbortMultipartUploadRequest abortRequest = new com.aliyun.oss.model.AbortMultipartUploadRequest(
                    ossConfig.getBucketName(), key, uploadId);
            ossClient.abortMultipartUpload(abortRequest);

            log.info("取消分片上传成功，UploadId: {}", uploadId);
            return true;
        } catch (Exception e) {
            log.error("取消分片上传失败: {}, UploadId: {}", objectKey, uploadId, e);
            return false;
        }
    }

    /**
     * 分片数据内部类（用于并发上传）
     */
    private static class ChunkData {
        final int partNumber;
        final byte[] data;

        ChunkData(int partNumber, byte[] data) {
            this.partNumber = partNumber;
            this.data = data;
        }
    }

    @Override
    public String generatePresignedUrl(String objectKey, long expirationSeconds) {
        if (StringUtils.isEmpty(objectKey)) {
            throw new ServiceException("对象键不能为空");
        }

        try {
            String key = extractObjectKey(objectKey);
            Date expirationDate = new Date(System.currentTimeMillis() + expirationSeconds * 1000);
            URL url = ossClient.generatePresignedUrl(ossConfig.getBucketName(), key, expirationDate);
            String signedUrl = url.toString();

            // 强制使用 HTTPS（阿里云 SDK 默认可能返回 HTTP）
            if (signedUrl.startsWith("http://")) {
                signedUrl = "https://" + signedUrl.substring(7);
            }

            return signedUrl;
        } catch (Exception e) {
            log.error("阿里云OSS生成预签名URL失败: {}", objectKey, e);
            throw new ServiceException("生成预签名URL失败: " + e.getMessage());
        }
    }

    /**
     * 为指定桶生成预签名URL（用于跨桶下载，如 asr-temp-audio）
     * 
     * @param bucketName        桶名
     * @param objectKey         对象键
     * @param expirationSeconds 过期时间（秒）
     * @return 带签名的临时下载URL
     */
    public String generatePresignedUrlForBucket(String bucketName, String objectKey, long expirationSeconds) {
        if (StringUtils.isEmpty(bucketName) || StringUtils.isEmpty(objectKey)) {
            throw new ServiceException("桶名和对象键不能为空");
        }

        try {
            String key = extractObjectKey(objectKey);
            Date expirationDate = new Date(System.currentTimeMillis() + expirationSeconds * 1000);
            URL url = ossClient.generatePresignedUrl(bucketName, key, expirationDate);
            String signedUrl = url.toString();

            // 强制使用 HTTPS
            if (signedUrl.startsWith("http://")) {
                signedUrl = "https://" + signedUrl.substring(7);
            }

            log.debug("为桶 {} 生成签名URL: {}", bucketName, signedUrl);
            return signedUrl;
        } catch (Exception e) {
            log.error("为桶 {} 生成预签名URL失败: {}", bucketName, objectKey, e);
            throw new ServiceException("生成预签名URL失败: " + e.getMessage());
        }
    }

    @Override
    public String extractObjectKey(String url) {
        if (StringUtils.isEmpty(url)) {
            return url;
        }

        if (url.startsWith(BASE_PATH) || url.startsWith("paper_packages/")) {
            return url;
        }

        try {
            // 统一转换为 https 进行比较
            String normalizedUrl = url;
            if (url.startsWith("http://")) {
                normalizedUrl = "https://" + url.substring(7);
            }

            if (StringUtils.isNotEmpty(ossConfig.getCdn()) && normalizedUrl.startsWith(ossConfig.getCdn())) {
                String key = normalizedUrl.substring(ossConfig.getCdn().length());
                if (key.startsWith("/")) {
                    key = key.substring(1);
                }
                return key;
            }

            String urlPrefix = ossConfig.getUrlPrefix();
            if (StringUtils.isNotEmpty(urlPrefix)) {
                // 尝试匹配 urlPrefix（支持 http/https）
                String normalizedPrefix = urlPrefix;
                if (normalizedPrefix.startsWith("http://")) {
                    normalizedPrefix = "https://" + normalizedPrefix.substring(7);
                }
                if (normalizedUrl.startsWith(normalizedPrefix)) {
                    String key = normalizedUrl.substring(normalizedPrefix.length());
                    if (key.startsWith("/")) {
                        key = key.substring(1);
                    }
                    return key;
                }
            }

            // 尝试直接从 bucket.oss-region.aliyuncs.com/key 格式提取
            String bucketDomain = ossConfig.getBucketName() + "." + ossConfig.getEndpoint();
            int domainIndex = normalizedUrl.indexOf(bucketDomain);
            if (domainIndex >= 0) {
                String key = normalizedUrl.substring(domainIndex + bucketDomain.length());
                if (key.startsWith("/")) {
                    key = key.substring(1);
                }
                return key;
            }

            int basePathIndex = url.indexOf(BASE_PATH);
            if (basePathIndex >= 0) {
                return url.substring(basePathIndex);
            }

            int paperPackagesIndex = url.indexOf("paper_packages/");
            if (paperPackagesIndex >= 0) {
                return url.substring(paperPackagesIndex);
            }
        } catch (Exception e) {
            log.warn("提取对象键失败，使用原始值: {}", url);
        }

        return url;
    }

    @Override
    public String buildFileUrl(String objectKey) {
        if (StringUtils.isNotEmpty(ossConfig.getCdn())) {
            String cdn = ossConfig.getCdn();
            if (!cdn.endsWith("/")) {
                cdn += "/";
            }
            return cdn + objectKey;
        }

        String urlPrefix = ossConfig.getUrlPrefix();
        if (StringUtils.isEmpty(urlPrefix)) {
            urlPrefix = "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint() + "/";
        }
        if (!urlPrefix.endsWith("/")) {
            urlPrefix += "/";
        }
        return urlPrefix + objectKey;
    }

    /**
     * 生成OSS对象键（路径）
     */
    private String generateObjectKey(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            fileName = UUID.randomUUID().toString();
        }

        if (fileName.startsWith("paper_packages/")) {
            return fileName;
        }

        String extension = "";
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = fileName.substring(lastDotIndex);
            fileName = fileName.substring(0, lastDotIndex);
        }

        String uniqueFileName = fileName + "_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8) + extension;
        String datePath = new java.text.SimpleDateFormat("yyyy/MM/dd").format(new Date());

        return BASE_PATH + datePath + "/" + uniqueFileName;
    }

    /**
     * 根据文件名确定 Content-Type
     */
    private String determineContentType(String fileName, String defaultContentType) {
        if (StringUtils.isEmpty(fileName)) {
            return defaultContentType != null ? defaultContentType : "application/octet-stream";
        }

        String fileNameLower = fileName.toLowerCase();

        if (fileNameLower.endsWith(".flac")) {
            return "audio/flac";
        } else if (fileNameLower.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (fileNameLower.endsWith(".wav")) {
            return "audio/wav";
        } else if (fileNameLower.endsWith(".ogg")) {
            return "audio/ogg";
        } else if (fileNameLower.endsWith(".m4a")) {
            return "audio/mp4";
        } else if (fileNameLower.endsWith(".aac")) {
            return "audio/aac";
        } else if (fileNameLower.endsWith(".mp4")) {
            return "video/mp4";
        } else if (fileNameLower.endsWith(".avi")) {
            return "video/x-msvideo";
        } else if (fileNameLower.endsWith(".mov")) {
            return "video/quicktime";
        } else if (fileNameLower.endsWith(".wmv")) {
            return "video/x-ms-wmv";
        } else if (fileNameLower.endsWith(".flv")) {
            return "video/x-flv";
        } else if (fileNameLower.endsWith(".webm")) {
            return "video/webm";
        } else if (fileNameLower.endsWith(".jpg") || fileNameLower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileNameLower.endsWith(".png")) {
            return "image/png";
        } else if (fileNameLower.endsWith(".gif")) {
            return "image/gif";
        } else if (fileNameLower.endsWith(".bmp")) {
            return "image/bmp";
        } else if (fileNameLower.endsWith(".webp")) {
            return "image/webp";
        }

        return defaultContentType != null ? defaultContentType : "application/octet-stream";
    }

    @Override
    public String getUploadToken() {
        try {
            long expireTime = 3600;
            long expireEndTime = System.currentTimeMillis() + expireTime * 1000;
            Date expiration = new Date(expireEndTime);

            com.aliyun.oss.model.PolicyConditions policyConds = new com.aliyun.oss.model.PolicyConditions();
            policyConds.addConditionItem(com.aliyun.oss.model.PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0,
                    1048576000);
            policyConds.addConditionItem(com.aliyun.oss.model.MatchMode.StartWith,
                    com.aliyun.oss.model.PolicyConditions.COND_KEY, BASE_PATH);

            String postPolicy = ossClient.generatePostPolicy(expiration, policyConds);
            byte[] binaryData = postPolicy.getBytes("utf-8");
            String encodedPolicy = com.aliyun.oss.common.utils.BinaryUtil.toBase64String(binaryData);
            String postSignature = ossClient.calculatePostSignature(postPolicy);

            java.util.Map<String, String> respMap = new java.util.LinkedHashMap<>();
            respMap.put("accessid", ossConfig.getAccessKeyId());
            respMap.put("policy", encodedPolicy);
            respMap.put("signature", postSignature);
            respMap.put("dir", BASE_PATH);
            // Host: https://bucket-name.endpoint
            String host = "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint();
            respMap.put("host", host);
            respMap.put("expire", String.valueOf(expireEndTime / 1000));

            return com.alibaba.fastjson2.JSON.toJSONString(respMap);
        } catch (Exception e) {
            log.error("获取阿里云OSS上传凭证失败", e);
            throw new ServiceException("获取上传凭证失败");
        }
    }

    @Override
    public String getDomain() {
        if (StringUtils.isNotEmpty(ossConfig.getCdn())) {
            return ossConfig.getCdn();
        }
        return "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint();
    }
}
