package com.ruoyi.common.utils.oss.impl;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.DownloadUrl;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.ruoyi.common.config.QiniuOssConfig;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.oss.IOssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * 七牛云OSS服务实现
 * 
 * @author ruoyi
 */
@Slf4j
@Component
public class QiniuOssService implements IOssService {

    @Autowired
    private QiniuOssConfig qiniuOssConfig;

    private Auth auth;
    private UploadManager uploadManager;
    private BucketManager bucketManager;

    /**
     * 题目媒体文件基础路径
     */
    private static final String BASE_PATH = "exam/question/";

    /**
     * 初始化七牛云客户端
     */
    @PostConstruct
    public void init() {
        try {
            // 创建认证对象
            auth = Auth.create(qiniuOssConfig.getAccessKey(), qiniuOssConfig.getSecretKey());

            // 创建配置对象（华北-河北使用region1）
            Configuration cfg = new Configuration(Region.region1());
            // 启用分片上传V2（用于大文件上传）
            cfg.resumableUploadAPIVersion = Configuration.ResumableUploadAPIVersion.V2;
            // 设置分片上传并发数（4个并发，提高上传速度）
            cfg.resumableUploadMaxConcurrentTaskCount = 4;

            // 创建上传管理器
            uploadManager = new UploadManager(cfg);

            // 创建空间管理器
            bucketManager = new BucketManager(auth, cfg);

            log.info("七牛云OSS客户端初始化成功，Bucket: {}", qiniuOssConfig.getBucketName());
        } catch (Exception e) {
            log.error("七牛云OSS客户端初始化失败", e);
            throw new ServiceException("七牛云OSS客户端初始化失败: " + e.getMessage());
        }
    }

    @Override
    public String upload(MultipartFile file, String fileName) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }

        try {
            String objectKey = generateObjectKey(fileName != null ? fileName : file.getOriginalFilename());

            // 生成上传凭证（设置最大文件大小 20MB，给前端10MB限制留余量）
            com.qiniu.util.StringMap policy = new com.qiniu.util.StringMap();
            policy.put("fsizeLimit", 20L * 1024 * 1024); // 20MB
            policy.put("insertOnly", 0); // 允许覆盖已存在的文件
            String upToken = auth.uploadToken(qiniuOssConfig.getBucketName(), null, 3600, policy);

            // 上传文件
            Response response = uploadManager.put(file.getInputStream(), objectKey, upToken, null,
                    determineContentType(file.getOriginalFilename(), file.getContentType()));

            if (!response.isOK()) {
                throw new ServiceException("七牛云OSS上传失败: " + response.error);
            }

            String fileUrl = buildFileUrl(objectKey);
            log.info("七牛云OSS文件上传成功: {} -> {}, 文件大小: {} MB", objectKey, fileUrl, file.getSize() / 1024.0 / 1024.0);

            return fileUrl;

        } catch (QiniuException e) {
            log.error("七牛云OSS文件上传失败", e);
            throw new ServiceException("文件上传失败: " + e.response.toString());
        } catch (Exception e) {
            log.error("七牛云OSS文件上传失败", e);
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

            // 生成上传凭证（设置最大文件大小 20MB，给前端10MB限制留余量）
            com.qiniu.util.StringMap policy = new com.qiniu.util.StringMap();
            policy.put("fsizeLimit", 20L * 1024 * 1024); // 20MB
            policy.put("insertOnly", 0); // 允许覆盖已存在的文件
            String upToken = auth.uploadToken(qiniuOssConfig.getBucketName(), null, 3600, policy);
            
            String finalContentType = StringUtils.isNotEmpty(contentType)
                    ? contentType
                    : determineContentType(objectKey, null);

            Response response = uploadManager.put(inputStream, objectKey, upToken, null, finalContentType);

            if (!response.isOK()) {
                throw new ServiceException("七牛云OSS上传失败: " + response.error);
            }

            String fileUrl = buildFileUrl(objectKey);
            log.info("七牛云OSS文件流上传成功: {} -> {}", objectKey, fileUrl);

            return fileUrl;

        } catch (QiniuException e) {
            log.error("七牛云OSS文件流上传失败", e);
            throw new ServiceException("文件流上传失败: " + e.response.toString());
        } catch (Exception e) {
            log.error("七牛云OSS文件流上传失败", e);
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

            // 生成上传凭证（设置最大文件大小 20MB，给前端10MB限制留余量）
            com.qiniu.util.StringMap policy = new com.qiniu.util.StringMap();
            policy.put("fsizeLimit", 20L * 1024 * 1024); // 20MB
            policy.put("insertOnly", 0); // 允许覆盖已存在的文件
            String upToken = auth.uploadToken(qiniuOssConfig.getBucketName(), null, 3600, policy);
            
            String finalContentType = StringUtils.isNotEmpty(contentType)
                    ? contentType
                    : determineContentType(objectKey, null);

            // 将字节数组转换为InputStream
            try (InputStream inputStream = new java.io.ByteArrayInputStream(bytes)) {
                Response response = uploadManager.put(inputStream, objectKey, upToken, null, finalContentType);

                if (!response.isOK()) {
                    throw new ServiceException("七牛云OSS上传失败: " + response.error);
                }

                String fileUrl = buildFileUrl(objectKey);
                log.info("七牛云OSS字节数组上传成功: {} -> {}, 大小: {} MB", objectKey, fileUrl, bytes.length / 1024.0 / 1024.0);

                return fileUrl;
            }

        } catch (QiniuException e) {
            log.error("七牛云OSS字节数组上传失败", e);
            throw new ServiceException("字节数组上传失败: " + e.response.toString());
        } catch (Exception e) {
            log.error("七牛云OSS字节数组上传失败", e);
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
            bucketManager.delete(qiniuOssConfig.getBucketName(), key);
            log.info("七牛云OSS文件删除成功: {}", key);
            return true;
        } catch (QiniuException e) {
            log.error("七牛云OSS文件删除失败: {}", objectKey, e);
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
            bucketManager.stat(qiniuOssConfig.getBucketName(), key);
            return true;
        } catch (QiniuException e) {
            if (e.code() == 612) {
                // 612表示文件不存在
                return false;
            }
            log.error("七牛云OSS检查文件是否存在失败: {}", objectKey, e);
            return false;
        }
    }

    @Override
    public byte[] downloadToBytes(String objectKey) {
        try {
            String key = extractObjectKey(objectKey);

            // 路径格式规范化：处理 exam/YYYY/MM/DD/question/ 格式
            // 转换为 exam/question/YYYY/MM/DD/ 格式
            if (key.matches("exam/\\d{4}/\\d{2}/\\d{2}/question/.*")) {
                // 提取日期和文件名
                String[] parts = key.split("/");
                if (parts.length >= 6) {
                    String year = parts[1];
                    String month = parts[2];
                    String day = parts[3];
                    // parts[4] 是 "question"
                    String fileName = String.join("/", java.util.Arrays.copyOfRange(parts, 5, parts.length));
                    String normalizedKey = "exam/question/" + year + "/" + month + "/" + day + "/" + fileName;
                    log.info("路径格式转换: {} -> {}", key, normalizedKey);
                    key = normalizedKey;
                }
            }

            // 先检查文件是否存在
            try {
                com.qiniu.storage.model.FileInfo fileInfo = bucketManager.stat(qiniuOssConfig.getBucketName(), key);
                log.info("文件存在，对象键: {}, 大小: {} 字节, 类型: {}", key, fileInfo.fsize, fileInfo.mimeType);
            } catch (com.qiniu.common.QiniuException e) {
                if (e.code() == 612) {
                    log.error("文件不存在！对象键: {}", key);
                    log.error("请在七牛云控制台搜索该文件，确认实际的对象键是否正确");
                    throw new ServiceException("文件不存在：" + key);
                }
                log.warn("检查文件失败，继续尝试下载: {}", e.getMessage());
            }

            // 获取域名（用于构建下载URL）
            String domain = qiniuOssConfig.getUrlPrefix();
            boolean useHttps = false;
            
            if (StringUtils.isEmpty(domain)) {
                domain = qiniuOssConfig.getDomain();
                if (StringUtils.isEmpty(domain)) {
                    throw new ServiceException("七牛云OSS域名配置为空，无法构建文件URL");
                }
                // 检查是否使用HTTPS
                useHttps = domain.startsWith("https://");
                // 移除协议前缀（DownloadUrl类会自动添加）
                domain = domain.replace("http://", "").replace("https://", "");
            } else {
                // 检查是否使用HTTPS
                useHttps = domain.startsWith("https://");
                // 移除协议前缀和尾部斜杠
                domain = domain.replace("http://", "").replace("https://", "");
                if (domain.endsWith("/")) {
                    domain = domain.substring(0, domain.length() - 1);
                }
            }

            log.info("七牛云OSS下载文件，对象键: {}, 域名: {}, 使用HTTPS: {}", key, domain, useHttps);

            // 使用DownloadUrl类生成私有下载链接（支持中文文件名）
            // DownloadUrl类会自动处理URL编码和签名，正确处理中文文件名
            long expirationSeconds = 3600; // 1小时有效期
            long deadline = System.currentTimeMillis() / 1000 + expirationSeconds;
            
            // 创建DownloadUrl对象（使用对象键，不是完整URL）
            // 第二个参数：useHttps - 是否使用HTTPS协议
            DownloadUrl downloadUrl = new DownloadUrl(domain, useHttps, key);
            String privateUrl = downloadUrl.buildURL(auth, deadline);

            log.info("七牛云OSS下载文件，使用私有链接: {}", privateUrl);

            // 尝试下载（先尝试HTTPS，如果失败则降级到HTTP）
            try {
                return downloadWithUrl(privateUrl, key);
            } catch (ServiceException e) {
                // 如果是403错误且URL是HTTPS，尝试降级到HTTP
                if (e.getMessage().contains("403") && privateUrl.startsWith("https://")) {
                    log.warn("HTTPS下载返回403，尝试降级到HTTP");
                    String httpUrl = privateUrl.replace("https://", "http://");
                    log.info("使用HTTP链接重试: {}", httpUrl);
                    return downloadWithUrl(httpUrl, key);
                }
                throw e;
            }

        } catch (Exception e) {
            log.error("七牛云OSS文件下载失败: {}", objectKey, e);
            throw new ServiceException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadToStream(String objectKey, long start, long end) {
        try {
            String key = extractObjectKey(objectKey);

            // 路径格式规范化（与downloadToBytes保持一致）
            if (key.matches("exam/\\d{4}/\\d{2}/\\d{2}/question/.*")) {
                String[] parts = key.split("/");
                if (parts.length >= 6) {
                    String year = parts[1];
                    String month = parts[2];
                    String day = parts[3];
                    String fileName = String.join("/", java.util.Arrays.copyOfRange(parts, 5, parts.length));
                    String normalizedKey = "exam/question/" + year + "/" + month + "/" + day + "/" + fileName;
                    key = normalizedKey;
                }
            }

            // 获取域名
            String domain = qiniuOssConfig.getUrlPrefix();
            boolean useHttps = false;
            
            if (StringUtils.isEmpty(domain)) {
                domain = qiniuOssConfig.getDomain();
                if (StringUtils.isEmpty(domain)) {
                    throw new ServiceException("七牛云OSS域名配置为空");
                }
                useHttps = domain.startsWith("https://");
                domain = domain.replace("http://", "").replace("https://", "");
            } else {
                useHttps = domain.startsWith("https://");
                domain = domain.replace("http://", "").replace("https://", "");
                if (domain.endsWith("/")) {
                    domain = domain.substring(0, domain.length() - 1);
                }
            }

            // 生成私有下载链接
            long expirationSeconds = 3600;
            long deadline = System.currentTimeMillis() / 1000 + expirationSeconds;
            DownloadUrl downloadUrl = new DownloadUrl(domain, useHttps, key);
            String privateUrl = downloadUrl.buildURL(auth, deadline);

            // 构建Range请求URL
            String rangeUrl = privateUrl + "&range=" + start + "-" + end;

            log.debug("流式下载文件，对象键: {}, Range: {}-{}", key, start, end);

            // 创建HTTP连接
            java.net.URL url = new java.net.URL(rangeUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Range", "bytes=" + start + "-" + end);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(120000);

            // SSL处理
            if (connection instanceof javax.net.ssl.HttpsURLConnection) {
                javax.net.ssl.HttpsURLConnection httpsConnection = (javax.net.ssl.HttpsURLConnection) connection;
                javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
                sslContext.init(null, new javax.net.ssl.TrustManager[] {
                    new javax.net.ssl.X509TrustManager() {
                        @Override public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                        @Override public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[] {}; }
                    }
                }, new java.security.SecureRandom());
                httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                httpsConnection.setHostnameVerifier((hostname, session) -> true);
            }

            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != 200 && responseCode != 206) { // 206是Partial Content
                connection.disconnect();
                throw new ServiceException("流式下载失败，HTTP状态码: " + responseCode);
            }

            return connection.getInputStream();

        } catch (Exception e) {
            log.error("七牛云OSS流式下载失败: {}, Range: {}-{}", objectKey, start, end, e);
            throw new ServiceException("流式下载失败: " + e.getMessage());
        }
    }

    @Override
    public long getFileSize(String objectKey) {
        try {
            String key = extractObjectKey(objectKey);

            // 路径格式规范化
            if (key.matches("exam/\\d{4}/\\d{2}/\\d{2}/question/.*")) {
                String[] parts = key.split("/");
                if (parts.length >= 6) {
                    String year = parts[1];
                    String month = parts[2];
                    String day = parts[3];
                    String fileName = String.join("/", java.util.Arrays.copyOfRange(parts, 5, parts.length));
                    String normalizedKey = "exam/question/" + year + "/" + month + "/" + day + "/" + fileName;
                    key = normalizedKey;
                }
            }

            // 获取文件信息
            com.qiniu.storage.model.FileInfo fileInfo = bucketManager.stat(qiniuOssConfig.getBucketName(), key);
            return fileInfo.fsize;

        } catch (com.qiniu.common.QiniuException e) {
            if (e.code() == 612) {
                log.error("文件不存在，无法获取大小: {}", objectKey);
                throw new ServiceException("文件不存在：" + objectKey);
            }
            log.error("获取文件大小失败: {}", objectKey, e);
            throw new ServiceException("获取文件大小失败: " + e.getMessage());
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
            if (StringUtils.isEmpty(objectKey)) {
                objectKey = generateObjectKey(null);
            }

            // 对于分片上传，先尝试删除已存在的文件（避免 file exists 错误）
            try {
                bucketManager.delete(qiniuOssConfig.getBucketName(), objectKey);
                log.info("已删除旧文件: {}", objectKey);
            } catch (QiniuException e) {
                // 如果文件不存在（612错误），忽略；其他错误记录日志但继续上传
                if (e.code() == 612) {
                    log.debug("文件不存在，无需删除: {}", objectKey);
                } else {
                    log.warn("删除旧文件失败，继续上传: {}, 错误: {}", objectKey, e.getMessage());
                }
            }
            
            // 生成上传凭证（根据文件类型设置不同的大小限制）
            com.qiniu.util.StringMap policy = new com.qiniu.util.StringMap();
            // ZIP包（试卷包）：最大 500MB，其他文件（媒体文件）：最大 20MB
            long fsizeLimit = objectKey.startsWith("paper_packages/") 
                ? 500L * 1024 * 1024  // 500MB for ZIP packages
                : 20L * 1024 * 1024;  // 20MB for media files (前端限制10MB)
            policy.put("fsizeLimit", fsizeLimit);
            String upToken = auth.uploadToken(qiniuOssConfig.getBucketName(), null, 3600, policy);
            
            String finalContentType = StringUtils.isNotEmpty(contentType)
                    ? contentType
                    : determineContentType(objectKey, null);

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
                Response response = uploadManager.put(inputStream, objectKey, upToken, null, finalContentType);
                if (!response.isOK()) {
                    throw new ServiceException("七牛云OSS上传失败: " + response.error);
                }
                String fileUrl = buildFileUrl(objectKey);
                log.info("七牛云OSS文件上传成功: {} -> {}", objectKey, fileUrl);
                return fileUrl;
            }

            // 大文件使用分片上传
            log.info("文件大小 {} MB，使用分片上传，分片大小: {} MB", 
                    fileSize / 1024.0 / 1024.0, chunkSizeBytes / 1024.0 / 1024.0);

            // 七牛云的UploadManager会自动处理大文件的分片上传
            // 但我们需要手动实现进度回调
            // 由于UploadManager的put方法不直接支持进度回调，我们使用包装的InputStream来跟踪进度
            java.io.InputStream progressInputStream = new java.io.FilterInputStream(inputStream) {
                private long bytesRead = 0;
                private long lastReportTime = System.currentTimeMillis();

                @Override
                public int read(byte[] b, int off, int len) throws java.io.IOException {
                    int n = super.read(b, off, len);
                    if (n > 0) {
                        bytesRead += n;
                        // 每上传1MB或每5秒报告一次进度
                        long currentTime = System.currentTimeMillis();
                        if (progressCallback != null && 
                            (bytesRead % (1024 * 1024) == 0 || currentTime - lastReportTime > 5000)) {
                            progressCallback.accept(bytesRead, fileSize);
                            lastReportTime = currentTime;
                        }
                    }
                    return n;
                }
            };

            Response response = uploadManager.put(progressInputStream, objectKey, upToken, null, finalContentType);

            if (!response.isOK()) {
                throw new ServiceException("七牛云OSS分片上传失败: " + response.error);
            }

            // 最终进度回调
            if (progressCallback != null) {
                progressCallback.accept(fileSize, fileSize);
            }

            String fileUrl = buildFileUrl(objectKey);
            log.info("七牛云OSS分片上传成功: {} -> {}, 文件大小: {} MB", 
                    objectKey, fileUrl, fileSize / 1024.0 / 1024.0);

            return fileUrl;

        } catch (QiniuException e) {
            log.error("七牛云OSS分片上传失败: {}", objectKey, e);
            throw new ServiceException("分片上传失败: " + e.response.toString());
        } catch (Exception e) {
            log.error("七牛云OSS分片上传失败: {}", objectKey, e);
            throw new ServiceException("分片上传失败: " + e.getMessage());
        }
    }

    @Override
    public UploadResult uploadMultipartWithResume(InputStream inputStream, String objectKey, String contentType, 
                                                  long fileSize, Long chunkSize, 
                                                  String uploadId, java.util.List<PartInfo> uploadedParts,
                                                  java.util.function.BiConsumer<Long, Long> progressCallback) {
        // 七牛云的分片上传由UploadManager自动处理，不支持手动管理uploadId
        // 如果提供了uploadId，说明是续传，但七牛云SDK不支持，需要重新上传
        if (uploadId != null && !uploadId.isEmpty()) {
            log.warn("七牛云OSS不支持手动管理uploadId，将重新上传。objectKey: {}", objectKey);
        }
        
        // 使用普通分片上传方法（七牛云SDK会自动处理）
        String fileUrl = uploadMultipart(inputStream, objectKey, contentType, fileSize, chunkSize, progressCallback);
        return new UploadResult(fileUrl, null, null);
    }

    @Override
    public java.util.List<PartInfo> listUploadedParts(String objectKey, String uploadId) {
        // 七牛云OSS不支持查询已上传分片列表
        log.warn("七牛云OSS不支持查询已上传分片列表");
        return new java.util.ArrayList<>();
    }

    @Override
    public boolean abortMultipartUpload(String objectKey, String uploadId) {
        // 七牛云OSS不支持手动取消分片上传
        log.warn("七牛云OSS不支持手动取消分片上传");
        return false;
    }

    /**
     * 使用指定URL下载文件
     */
    private byte[] downloadWithUrl(String fileUrl, String objectKey) throws Exception {
        // 注意：七牛云SDK生成的私有下载链接已经包含了正确的编码和签名
        // 不要再次对URL进行编码，否则会导致签名验证失败
        // 直接使用原始URL创建连接
        java.net.URL url;
        try {
            url = new java.net.URL(fileUrl);
        } catch (java.net.MalformedURLException e) {
            // 如果URL格式有问题（可能包含未编码的中文字符），尝试编码
            log.warn("URL格式异常，尝试编码: {}", fileUrl);
            try {
                java.net.URI uri = new java.net.URI(fileUrl);
                url = uri.toURL();
            } catch (Exception e2) {
                log.error("URL编码失败: {}", fileUrl, e2);
                throw new ServiceException("URL格式错误: " + fileUrl);
            }
        }

        java.net.URLConnection urlConnection = url.openConnection();

        // 如果是HTTPS连接，需要处理SSL证书验证
        if (urlConnection instanceof javax.net.ssl.HttpsURLConnection) {
            javax.net.ssl.HttpsURLConnection httpsConnection = (javax.net.ssl.HttpsURLConnection) urlConnection;

            // 创建SSL上下文，跳过证书验证（仅用于解决证书域名不匹配问题）
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, new javax.net.ssl.TrustManager[] { new TrustAnyTrustManager() },
                    new java.security.SecureRandom());
            httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());

            // 设置主机名验证器，允许所有主机名
            httpsConnection.setHostnameVerifier(new TrustAnyHostnameVerifier());
        }

        // 设置超时（处理大文件）
        urlConnection.setConnectTimeout(10000); // 10秒连接超时
        urlConnection.setReadTimeout(120000); // 120秒读取超时（处理7.52MB的图片文件）

        // 如果是HTTP/HTTPS连接，设置请求方法
        if (urlConnection instanceof java.net.HttpURLConnection) {
            java.net.HttpURLConnection httpConnection = (java.net.HttpURLConnection) urlConnection;
            httpConnection.setRequestMethod("GET");
            log.info("正在连接: {}", fileUrl);
            httpConnection.connect();

            int responseCode = httpConnection.getResponseCode();
            log.info("HTTP响应码: {}, 对象键: {}", responseCode, objectKey);
            if (responseCode != 200) {
                // 如果是403错误，记录详细信息
                if (responseCode == 403) {
                    log.error("下载链接返回403错误");
                    log.error("对象键: {}", objectKey);
                    log.error("下载URL: {}", fileUrl);
                    log.error("请检查：1) 七牛云存储空间是否为私有空间 2) AccessKey和SecretKey是否正确 3) 域名配置是否正确");
                }
                throw new ServiceException("文件下载失败，HTTP状态码: " + responseCode);
            }

            try (InputStream inputStream = httpConnection.getInputStream()) {
                log.info("开始读取文件内容: {}", objectKey);

                // 使用缓冲区读取，并记录进度
                java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[8192]; // 8KB缓冲区
                int bytesRead;
                long totalBytesRead = 0;
                long startTime = System.currentTimeMillis();

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    // 每下载1MB记录一次进度
                    if (totalBytesRead % (1024 * 1024) == 0) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        double speed = (totalBytesRead / 1024.0 / 1024.0) / (elapsed / 1000.0);
                        log.info("下载进度: {} MB, 速度: {:.2f} MB/s", totalBytesRead / 1024 / 1024, speed);
                    }
                }

                byte[] bytes = outputStream.toByteArray();
                long elapsed = System.currentTimeMillis() - startTime;
                double speed = (bytes.length / 1024.0 / 1024.0) / (elapsed / 1000.0);
                log.info("文件下载成功，对象键: {}, 大小: {} 字节, 耗时: {} 毫秒, 平均速度: {:.2f} MB/s",
                        objectKey, bytes.length, elapsed, speed);
                return bytes;
            } finally {
                httpConnection.disconnect();
            }
        } else {
            // 非HTTP连接（理论上不应该发生）
            urlConnection.connect();
            try (InputStream inputStream = urlConnection.getInputStream()) {
                return org.apache.commons.io.IOUtils.toByteArray(inputStream);
            }
        }
    }

    /**
     * 使用公开URL下载文件（备用方案）
     */
    private byte[] downloadWithPublicUrl(String fileUrl) throws Exception {
        java.net.URL url = new java.net.URL(fileUrl);
        java.net.URLConnection urlConnection = url.openConnection();

        // 如果是HTTPS连接，需要处理SSL证书验证
        if (urlConnection instanceof javax.net.ssl.HttpsURLConnection) {
            javax.net.ssl.HttpsURLConnection httpsConnection = (javax.net.ssl.HttpsURLConnection) urlConnection;

            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, new javax.net.ssl.TrustManager[] { new TrustAnyTrustManager() },
                    new java.security.SecureRandom());
            httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
            httpsConnection.setHostnameVerifier(new TrustAnyHostnameVerifier());
        }

        urlConnection.setConnectTimeout(30000);
        urlConnection.setReadTimeout(300000);

        if (urlConnection instanceof java.net.HttpURLConnection) {
            java.net.HttpURLConnection httpConnection = (java.net.HttpURLConnection) urlConnection;
            httpConnection.setRequestMethod("GET");
            httpConnection.connect();

            int responseCode = httpConnection.getResponseCode();
            if (responseCode != 200) {
                throw new ServiceException("文件下载失败，HTTP状态码: " + responseCode);
            }

            try (InputStream inputStream = httpConnection.getInputStream()) {
                return org.apache.commons.io.IOUtils.toByteArray(inputStream);
            } finally {
                httpConnection.disconnect();
            }
        } else {
            urlConnection.connect();
            try (InputStream inputStream = urlConnection.getInputStream()) {
                return org.apache.commons.io.IOUtils.toByteArray(inputStream);
            }
        }
    }

    /**
     * 信任所有证书的TrustManager（用于解决SSL证书域名不匹配问题）
     */
    private static class TrustAnyTrustManager implements javax.net.ssl.X509TrustManager {
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
            // 信任所有客户端证书
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
            // 信任所有服务器证书
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[] {};
        }
    }

    /**
     * 信任所有主机名的HostnameVerifier（用于解决SSL证书域名不匹配问题）
     */
    private static class TrustAnyHostnameVerifier implements javax.net.ssl.HostnameVerifier {
        @Override
        public boolean verify(String hostname, javax.net.ssl.SSLSession session) {
            // 信任所有主机名
            return true;
        }
    }

    @Override
    public String generatePresignedUrl(String objectKey, long expirationSeconds) {
        if (StringUtils.isEmpty(objectKey)) {
            throw new ServiceException("对象键不能为空");
        }

        try {
            String key = extractObjectKey(objectKey);
            
            // 获取域名（用于构建下载URL）
            String domain = qiniuOssConfig.getUrlPrefix();
            boolean useHttps = false;
            
            if (StringUtils.isEmpty(domain)) {
                domain = qiniuOssConfig.getDomain();
                if (StringUtils.isEmpty(domain)) {
                    throw new ServiceException("七牛云OSS域名配置为空，无法构建文件URL");
                }
                // 检查是否使用HTTPS
                useHttps = domain.startsWith("https://");
                // 移除协议前缀（DownloadUrl类会自动添加）
                domain = domain.replace("http://", "").replace("https://", "");
            } else {
                // 检查是否使用HTTPS
                useHttps = domain.startsWith("https://");
                // 移除协议前缀和尾部斜杠
                domain = domain.replace("http://", "").replace("https://", "");
                if (domain.endsWith("/")) {
                    domain = domain.substring(0, domain.length() - 1);
                }
            }
            
            // 使用DownloadUrl类生成私有下载链接（支持中文文件名）
            // DownloadUrl类会自动处理URL编码和签名，正确处理中文文件名
            long deadline = System.currentTimeMillis() / 1000 + expirationSeconds;
            DownloadUrl downloadUrl = new DownloadUrl(domain, useHttps, key);
            String privateUrl = downloadUrl.buildURL(auth, deadline);
            return privateUrl;
        } catch (Exception e) {
            log.error("七牛云OSS生成预签名URL失败: {}", objectKey, e);
            throw new ServiceException("生成预签名URL失败: " + e.getMessage());
        }
    }

    @Override
    public String extractObjectKey(String url) {
        if (StringUtils.isEmpty(url)) {
            return url;
        }

        log.debug("提取对象键 - 原始URL: {}", url);

        // 移除URL中的查询参数（如 ?e=xxx&token=xxx）
        int queryIndex = url.indexOf('?');
        if (queryIndex >= 0) {
            url = url.substring(0, queryIndex);
            log.debug("移除查询参数后: {}", url);
        }

        if (url.startsWith(BASE_PATH) || url.startsWith("paper_packages/")) {
            log.debug("直接返回对象键: {}", url);
            return url;
        }

        // 先尝URL解码，处理URL编码的情况
        String decodedUrl = url;
        try {
            decodedUrl = java.net.URLDecoder.decode(url, "UTF-8");
            log.debug("URL解码后: {}", decodedUrl);
        } catch (Exception e) {
            log.warn("URL解码失败，使用原始URL: {}", url);
        }

        try {
            // 移除CDN域名（使用解码后URL）
            if (StringUtils.isNotEmpty(qiniuOssConfig.getCdn()) && decodedUrl.startsWith(qiniuOssConfig.getCdn())) {
                String key = decodedUrl.substring(qiniuOssConfig.getCdn().length());
                if (key.startsWith("/")) {
                    key = key.substring(1);
                }
                log.debug("从CDN域名提取对象键: {}", key);
                return key;
            }

            // 移除OSS域名（使用解码后URL）
            String urlPrefix = qiniuOssConfig.getUrlPrefix();
            if (StringUtils.isNotEmpty(urlPrefix) && decodedUrl.startsWith(urlPrefix)) {
                String key = decodedUrl.substring(urlPrefix.length());
                if (key.startsWith("/")) {
                    key = key.substring(1);
                }
                log.debug("从OSS域名提取对象键: {}", key);
                return key;
            }

            // 查找BASE_PATH（使用解码后URL）
            int basePathIndex = decodedUrl.indexOf(BASE_PATH);
            if (basePathIndex >= 0) {
                String key = decodedUrl.substring(basePathIndex);
                log.debug("从BASE_PATH提取对象键: {}", key);
                return key;
            }

            int paperPackagesIndex = decodedUrl.indexOf("paper_packages/");
            if (paperPackagesIndex >= 0) {
                String key = decodedUrl.substring(paperPackagesIndex);
                log.debug("从paper_packages提取对象键: {}", key);
                return key;
            }
        } catch (Exception e) {
            log.warn("提取对象键失败，使用原始值: {}", url);
        }

        return url;
    }

    @Override
    public String buildFileUrl(String objectKey) {
        // 对于私有下载链接，必须使用OSS域名（不是CDN域名）
        // CDN域名不支持私有下载链接的签名验证
        String urlPrefix = qiniuOssConfig.getUrlPrefix();
        if (StringUtils.isEmpty(urlPrefix)) {
            // 如果没有配置urlPrefix，使用domain构建
            String domain = qiniuOssConfig.getDomain();
            if (StringUtils.isEmpty(domain)) {
                throw new ServiceException("七牛云OSS域名配置为空，无法构建文件URL");
            }
            // 优先使用HTTPS，但如果HTTPS有问题，代码会自动降级到HTTP
            urlPrefix = "https://" + domain + "/";
        }
        if (!urlPrefix.endsWith("/")) {
            urlPrefix += "/";
        }

        // 注意：不要对objectKey进行URL编码！
        // 七牛云SDK的 auth.privateDownloadUrl() 会自己处理编码和签名
        // 如果我们提前编码，会导致签名错误，返回404
        return urlPrefix + objectKey;
    }

    /**
     * 构建公开访问URL（用于CDN加速，如果存储空间是公开的）
     */
    public String buildPublicFileUrl(String objectKey) {
        // 优先使用CDN域名（如果配置了）
        if (StringUtils.isNotEmpty(qiniuOssConfig.getCdn())) {
            String cdn = qiniuOssConfig.getCdn();
            if (!cdn.endsWith("/")) {
                cdn += "/";
            }
            return cdn + objectKey;
        }

        // 使用OSS域名
        return buildFileUrl(objectKey);
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
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());

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
        // 生成上传凭证,有效期1小时
        // 设置上传策略：最大文件大小 20MB（媒体文件，前端限制10MB）
        // 注意：前端直接上传主要用于媒体文件，ZIP包通过后端上传
        com.qiniu.util.StringMap policy = new com.qiniu.util.StringMap();
        policy.put("fsizeLimit", 20L * 1024 * 1024); // 20MB
        policy.put("insertOnly", 0); // 允许覆盖已存在的文件
        return auth.uploadToken(qiniuOssConfig.getBucketName(), null, 3600, policy);
    }

    @Override
    public String getDomain() {
        // 优先返回CDN域名,如果没有则返回OSS域名
        if (StringUtils.isNotEmpty(qiniuOssConfig.getCdn())) {
            return qiniuOssConfig.getCdn();
        }
        return qiniuOssConfig.getUrlPrefix();
    }

    /**
     * 获取七牛云上传地址（根据配置的区域）
     * 
     * @return 上传地址
     */
    public String getUploadUrl() {
        // 根据配置的区域返回对应的上传地址
        // region1 (华北-河北) -> up-z1.qiniup.com
        // region0 (华东) -> up-z0.qiniup.com
        // region2 (华南) -> up-z2.qiniup.com
        // regionNa0 (北美) -> up-na0.qiniup.com
        // regionAs0 (东南亚) -> up-as0.qiniup.com
        // 默认使用 region1 (华北)
        return "https://up-z1.qiniup.com";
    }
}
