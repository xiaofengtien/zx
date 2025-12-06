package com.ruoyi.common.utils.oss.exam.question;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.oss.IOssService;
import com.ruoyi.common.utils.oss.OssServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * OSS工具类（考试业务-题目相关）
 * 支持多厂商OSS（阿里云、七牛云等）
 * 文件存储路径：/exam/question/
 * 
 * @author ruoyi
 */
@Slf4j
@Component
public class OssUtil {

    @Autowired
    private OssServiceFactory ossServiceFactory;

    /**
     * 题目媒体文件基础路径
     */
    private static final String BASE_PATH = "exam/question/";

    /**
     * 获取OSS服务实例
     */
    private IOssService getOssService() {
        return ossServiceFactory.getOssService();
    }

    /**
     * 上传文件到OSS
     * 
     * @param file     文件
     * @param fileName 文件名（可选，如果为空则使用原文件名）
     * @return OSS文件URL
     */
    public String upload(MultipartFile file, String fileName) {
        return getOssService().upload(file, fileName);
    }

    /**
     * 上传文件流到OSS
     * 
     * @param inputStream 文件流
     * @param fileName    文件名
     * @param contentType 文件类型
     * @return OSS文件URL
     */
    public String upload(InputStream inputStream, String fileName, String contentType) {
        // 生成对象键
        String objectKey = generateObjectKey(fileName);
        return getOssService().upload(inputStream, objectKey, contentType);
    }

    /**
     * 上传字节数组到OSS
     * 
     * @param bytes       文件字节数组
     * @param objectKey   OSS对象键（完整路径，不自动生成）
     * @param contentType 文件类型
     * @return OSS文件URL
     */
    public String upload(byte[] bytes, String objectKey, String contentType) {
        // 直接使用提供的objectKey，不自动生成路径（用于试卷包等固定路径文件）
        return getOssService().upload(bytes, objectKey, contentType);
    }

    /**
     * 分片上传大文件到OSS（用于大文件上传，支持进度回调）
     * 
     * @param inputStream 文件输入流
     * @param objectKey   OSS对象键（完整路径，不自动生成）
     * @param contentType 文件类型
     * @param fileSize    文件大小（字节）
     * @param chunkSize   分片大小（字节），默认10MB
     * @param progressCallback 进度回调（已上传字节数, 总字节数），null表示不回调
     * @return OSS文件URL
     */
    public String uploadMultipart(InputStream inputStream, String objectKey, String contentType, 
                                 long fileSize, Long chunkSize, 
                                 java.util.function.BiConsumer<Long, Long> progressCallback) {
        // 直接使用提供的objectKey，不自动生成路径（用于试卷包等固定路径文件）
        return getOssService().uploadMultipart(inputStream, objectKey, contentType, 
                fileSize, chunkSize, progressCallback);
    }

    /**
     * 分片上传大文件到OSS（支持断点续传）
     * 
     * @param inputStream 文件输入流
     * @param objectKey   OSS对象键（完整路径，不自动生成）
     * @param contentType 文件类型
     * @param fileSize    文件大小（字节）
     * @param chunkSize   分片大小（字节）
     * @param uploadId    已存在的上传ID（用于断点续传，null表示新上传）
     * @param uploadedParts 已上传的分片列表（用于断点续传，null表示新上传）
     * @param progressCallback 进度回调（已上传字节数, 总字节数），null表示不回调
     * @return 上传结果，包含fileUrl和uploadId
     */
    public IOssService.UploadResult uploadMultipartWithResume(InputStream inputStream, String objectKey, String contentType, 
                                                              long fileSize, Long chunkSize, 
                                                              String uploadId, java.util.List<IOssService.PartInfo> uploadedParts,
                                                              java.util.function.BiConsumer<Long, Long> progressCallback) {
        return getOssService().uploadMultipartWithResume(inputStream, objectKey, contentType, 
                fileSize, chunkSize, uploadId, uploadedParts, progressCallback);
    }

    /**
     * 查询已上传的分片列表（用于断点续传）
     * 
     * @param objectKey OSS对象键（完整路径）
     * @param uploadId  上传ID
     * @return 已上传的分片列表
     */
    public java.util.List<IOssService.PartInfo> listUploadedParts(String objectKey, String uploadId) {
        return getOssService().listUploadedParts(objectKey, uploadId);
    }

    /**
     * 取消分片上传（清理未完成的上传任务）
     * 
     * @param objectKey OSS对象键（完整路径）
     * @param uploadId  上传ID
     * @return 是否取消成功
     */
    public boolean abortMultipartUpload(String objectKey, String uploadId) {
        return getOssService().abortMultipartUpload(objectKey, uploadId);
    }

    /**
     * 删除OSS文件
     * 
     * @param objectKey OSS对象键（完整路径或相对路径）
     * @return 是否删除成功
     */
    public boolean delete(String objectKey) {
        return getOssService().delete(objectKey);
    }

    /**
     * 批量删除OSS文件
     * 
     * @param objectKeys OSS对象键列表
     * @return 删除成功的数量
     */
    public int deleteBatch(String... objectKeys) {
        if (objectKeys == null || objectKeys.length == 0) {
            return 0;
        }

        int successCount = 0;
        for (String objectKey : objectKeys) {
            if (delete(objectKey)) {
                successCount++;
            }
        }

        log.info("批量删除完成: 成功 {} / 总数 {}", successCount, objectKeys.length);
        return successCount;
    }

    /**
     * 检查文件是否存在
     * 
     * @param objectKey OSS对象键
     * @return 是否存在
     */
    public boolean exists(String objectKey) {
        return getOssService().exists(objectKey);
    }

    /**
     * 生成预签名URL（用于临时访问）
     * 
     * @param objectKey  OSS对象键
     * @param expiration 过期时间（秒），默认1小时
     * @return 预签名URL
     */
    public String generatePresignedUrl(String objectKey, long expiration) {
        return getOssService().generatePresignedUrl(objectKey, expiration);
    }

    /**
     * 生成预签名URL（默认1小时）
     * 
     * @param objectKey OSS对象键
     * @return 预签名URL
     */
    public String generatePresignedUrl(String objectKey) {
        return generatePresignedUrl(objectKey, 3600);
    }

    /**
     * 生成OSS对象键（路径）
     * 
     * @param fileName 原始文件名
     * @return OSS对象键
     */
    private String generateObjectKey(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            fileName = UUID.randomUUID().toString();
        }

        // 如果文件名以 paper_packages/ 开头，说明是试卷包文件，直接使用原始路径，不修改
        if (fileName.startsWith("paper_packages/")) {
            return fileName;
        }

        // 生成唯一文件名（避免重名）
        String extension = "";
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = fileName.substring(lastDotIndex);
            fileName = fileName.substring(0, lastDotIndex);
        }

        // 使用UUID + 时间戳生成唯一文件名
        String uniqueFileName = fileName + "_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 8) + extension;

        // 按日期分目录存储
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());

        return BASE_PATH + datePath + "/" + uniqueFileName;
    }

    /**
     * 构建文件访问URL
     * 
     * @param objectKey OSS对象键
     * @return 文件URL
     */
    private String buildFileUrl(String objectKey) {
        return getOssService().buildFileUrl(objectKey);
    }

    /**
     * 从完整URL中提取对象键
     * 
     * @param urlOrKey URL或对象键
     * @return 对象键
     */
    private String extractObjectKey(String urlOrKey) {
        return getOssService().extractObjectKey(urlOrKey);
    }

    /**
     * 获取OSS对象键（从URL中提取或直接返回）
     * 
     * @param urlOrKey URL或对象键
     * @return OSS对象键
     */
    public String getObjectKey(String urlOrKey) {
        return extractObjectKey(urlOrKey);
    }

    /**
     * 获取文件访问URL（从对象键构建）
     * 
     * @param objectKey OSS对象键
     * @return 文件URL
     */
    public String getFileUrl(String objectKey) {
        return buildFileUrl(objectKey);
    }

    /**
     * 从OSS下载文件到字节数组
     * 
     * @param objectKey OSS对象键
     * @return 文件字节数组
     * @throws ServiceException 业务异常
     */
    public byte[] downloadFileToBytes(String objectKey) throws ServiceException {
        return getOssService().downloadToBytes(objectKey);
    }

    /**
     * 流式下载文件（支持Range请求）
     * 
     * @param objectKey OSS对象键
     * @param start     起始位置（字节）
     * @param end       结束位置（字节，包含）
     * @return 文件输入流
     * @throws ServiceException 业务异常
     */
    public InputStream downloadFileToStream(String objectKey, long start, long end) throws ServiceException {
        return getOssService().downloadToStream(objectKey, start, end);
    }

    /**
     * 获取文件大小
     * 
     * @param objectKey OSS对象键
     * @return 文件大小（字节）
     * @throws ServiceException 业务异常
     */
    public long getFileSize(String objectKey) throws ServiceException {
        return getOssService().getFileSize(objectKey);
    }

    /**
     * 获取OSS上传凭证（用于前端直接上传）
     * 
     * @return 上传凭证(token)
     */
    public String getUploadToken() {
        return getOssService().getUploadToken();
    }

    /**
     * 获取OSS域名（用于前端拼接完整URL）
     * 
     * @return OSS域名
     */
    public String getDomain() {
        return getOssService().getDomain();
    }

    /**
     * 获取OSS上传地址（用于前端直接上传）
     * 
     * @return 上传地址
     */
    public String getUploadUrl() {
        IOssService ossService = getOssService();
        // 如果是七牛云，返回区域对应的上传地址
        if (ossService instanceof com.ruoyi.common.utils.oss.impl.QiniuOssService) {
            return ((com.ruoyi.common.utils.oss.impl.QiniuOssService) ossService).getUploadUrl();
        }
        // 阿里云的上传地址在 token 中返回
        return null;
    }

    /**
     * 获取私有文件的临时下载URL
     * 
     * @param objectKey  OSS对象键
     * @param expiration 过期时间（秒）
     * @return 带签名的临时下载URL
     */
    public String getPrivateDownloadUrl(String objectKey, long expiration) {
        return generatePresignedUrl(objectKey, expiration);
    }

    /**
     * 获取当前OSS类型（用于前端选择上传方式）
     * 
     * @return OSS类型（qiniu/aliyun）
     */
    public String getOssType() {
        return ossServiceFactory.getCurrentProvider();
    }

}
