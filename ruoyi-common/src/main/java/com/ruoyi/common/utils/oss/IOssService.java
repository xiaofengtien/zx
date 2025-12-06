package com.ruoyi.common.utils.oss;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.Serializable;

/**
 * OSS服务接口
 * 支持多厂商OSS（阿里云、七牛云等）
 * 
 * @author ruoyi
 */
public interface IOssService {

    /**
     * 上传文件到OSS
     * 
     * @param file     文件
     * @param fileName 文件名（可选，如果为空则使用原文件名）
     * @return OSS文件URL
     */
    String upload(MultipartFile file, String fileName);

    /**
     * 上传文件流到OSS
     * 
     * @param inputStream 文件流
     * @param objectKey   OSS对象键（路径）
     * @param contentType 文件类型
     * @return OSS文件URL
     */
    String upload(InputStream inputStream, String objectKey, String contentType);

    /**
     * 上传字节数组到OSS
     * 
     * @param bytes       文件字节数组
     * @param objectKey   OSS对象键（路径）
     * @param contentType 文件类型
     * @return OSS文件URL
     */
    String upload(byte[] bytes, String objectKey, String contentType);

    /**
     * 分片上传大文件到OSS（用于大文件上传，支持断点续传）
     * 
     * @param inputStream 文件输入流
     * @param objectKey   OSS对象键（路径）
     * @param contentType 文件类型
     * @param fileSize    文件大小（字节）
     * @param chunkSize   分片大小（字节），默认10MB
     * @param progressCallback 进度回调（可选，null表示不回调）
     * @return OSS文件URL
     */
    String uploadMultipart(InputStream inputStream, String objectKey, String contentType, 
                          long fileSize, Long chunkSize, 
                          java.util.function.BiConsumer<Long, Long> progressCallback);

    /**
     * 分片上传大文件到OSS（支持断点续传）
     * 
     * @param inputStream 文件输入流
     * @param objectKey   OSS对象键（路径）
     * @param contentType 文件类型
     * @param fileSize    文件大小（字节）
     * @param chunkSize   分片大小（字节）
     * @param uploadId    已存在的上传ID（用于断点续传，null表示新上传）
     * @param uploadedParts 已上传的分片列表（用于断点续传，null表示新上传）
     * @param progressCallback 进度回调（可选，null表示不回调）
     * @return 上传结果，包含fileUrl和uploadId
     */
    UploadResult uploadMultipartWithResume(InputStream inputStream, String objectKey, String contentType, 
                                          long fileSize, Long chunkSize, 
                                          String uploadId, java.util.List<PartInfo> uploadedParts,
                                          java.util.function.BiConsumer<Long, Long> progressCallback);

    /**
     * 查询已上传的分片列表（用于断点续传）
     * 
     * @param objectKey OSS对象键（路径）
     * @param uploadId  上传ID
     * @return 已上传的分片列表
     */
    java.util.List<PartInfo> listUploadedParts(String objectKey, String uploadId);

    /**
     * 取消分片上传（清理未完成的上传任务）
     * 
     * @param objectKey OSS对象键（路径）
     * @param uploadId  上传ID
     * @return 是否取消成功
     */
    boolean abortMultipartUpload(String objectKey, String uploadId);

    /**
     * 上传结果
     */
    class UploadResult implements Serializable {
        private static final long serialVersionUID = 1L;
        private String fileUrl;
        private String uploadId;
        private java.util.List<PartInfo> uploadedParts;

        public UploadResult(String fileUrl, String uploadId, java.util.List<PartInfo> uploadedParts) {
            this.fileUrl = fileUrl;
            this.uploadId = uploadId;
            this.uploadedParts = uploadedParts;
        }

        public String getFileUrl() { return fileUrl; }
        public String getUploadId() { return uploadId; }
        public java.util.List<PartInfo> getUploadedParts() { return uploadedParts; }
    }

    /**
     * 分片信息
     */
    class PartInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private int partNumber;
        private String eTag;
        private long size;

        public PartInfo(int partNumber, String eTag, long size) {
            this.partNumber = partNumber;
            this.eTag = eTag;
            this.size = size;
        }

        public int getPartNumber() { return partNumber; }
        public String getETag() { return eTag; }
        public long getSize() { return size; }
    }

    /**
     * 删除OSS文件
     * 
     * @param objectKey OSS对象键（路径）
     * @return 是否删除成功
     */
    boolean delete(String objectKey);

    /**
     * 检查文件是否存在
     * 
     * @param objectKey OSS对象键（路径）
     * @return 是否存在
     */
    boolean exists(String objectKey);

    /**
     * 下载文件为字节数组
     * 
     * @param objectKey OSS对象键（路径）
     * @return 文件字节数组
     */
    byte[] downloadToBytes(String objectKey);

    /**
     * 流式下载文件（支持Range请求）
     * 
     * @param objectKey OSS对象键（路径）
     * @param start     起始位置（字节）
     * @param end       结束位置（字节，包含）
     * @return 文件输入流
     */
    InputStream downloadToStream(String objectKey, long start, long end);

    /**
     * 获取文件大小
     * 
     * @param objectKey OSS对象键（路径）
     * @return 文件大小（字节）
     */
    long getFileSize(String objectKey);

    /**
     * 生成预签名URL（用于临时访问）
     * 
     * @param objectKey         OSS对象键（路径）
     * @param expirationSeconds 过期时间（秒）
     * @return 预签名URL
     */
    String generatePresignedUrl(String objectKey, long expirationSeconds);

    /**
     * 从URL中提取对象键
     * 
     * @param url 文件URL
     * @return 对象键
     */
    String extractObjectKey(String url);

    /**
     * 构建文件访问URL（优先使用CDN域名）
     * 
     * @param objectKey OSS对象键（路径）
     * @return 文件访问URL
     */
    String buildFileUrl(String objectKey);

    /**
     * 获取OSS上传凭证（用于前端直接上传）
     * 
     * @return 上传凭证(token)
     */
    String getUploadToken();

    /**
     * 获取OSS域名（用于前端拼接完整URL）
     * 
     * @return OSS域名
     */
    String getDomain();
}

