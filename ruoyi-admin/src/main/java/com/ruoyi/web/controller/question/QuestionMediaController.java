package com.ruoyi.web.controller.question;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.oss.exam.question.OssUtil;
import com.ruoyi.student.archive.biz.question.IQuestionMediaBiz;
import com.ruoyi.student.archive.domain.question.QuestionMedia;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 题目媒体文件管理Controller
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/question/media")
@RequiredArgsConstructor
@Slf4j
public class QuestionMediaController extends BaseController {
    private final OssUtil ossUtil;
    private final IQuestionMediaBiz questionMediaBiz;

    /**
     * 上传题目媒体文件到OSS
     */
    @PreAuthorize("@ss.hasPermi('question:question:add')")
    @Log(title = "题目媒体文件", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public AjaxResult uploadMedia(@RequestParam("file") MultipartFile file) {
        try {
            String url = ossUtil.upload(file, null);
            AjaxResult ajax = AjaxResult.success();
            ajax.put("url", url);
            ajax.put("fileName", file.getOriginalFilename());
            return ajax;
        } catch (Exception e) {
            return AjaxResult.error("上传失败：" + e.getMessage());
        }
    }

    /**
     * 获取OSS上传凭证（用于前端直接上传）
     */
    @PreAuthorize("@ss.hasPermi('question:question:add')")
    @GetMapping("/getUploadToken")
    public AjaxResult getUploadToken() {
        try {
            String uploadToken = ossUtil.getUploadToken();
            String domain = ossUtil.getDomain(); // OSS域名,用于拼接完整URL
            String ossType = ossUtil.getOssType(); // OSS类型（qiniu/aliyun）
            String uploadUrl = ossUtil.getUploadUrl(); // OSS上传地址（用于前端直接上传）
            
            AjaxResult ajax = AjaxResult.success();
            ajax.put("token", uploadToken);
            ajax.put("domain", domain);
            ajax.put("ossType", ossType); // 添加OSS类型，前端根据类型选择上传方式
            ajax.put("uploadUrl", uploadUrl); // 添加上传地址（七牛云需要，阿里云为null）
            return ajax;
        } catch (Exception e) {
            log.error("获取上传凭证失败", e);
            return AjaxResult.error("获取上传凭证失败：" + e.getMessage());
        }
    }

    /**
     * 获取私有文件的临时下载URL（用于预览）
     */
    @GetMapping("/getDownloadUrl")
    public AjaxResult getDownloadUrl(@RequestParam("url") String url) {
        try {
            // 从URL中提取ObjectKey
            String objectKey = ossUtil.getObjectKey(url);
            // 生成带签名的临时下载URL（有效期1小时）
            String downloadUrl = ossUtil.getPrivateDownloadUrl(objectKey, 3600);
            AjaxResult ajax = AjaxResult.success();
            ajax.put("downloadUrl", downloadUrl);
            return ajax;
        } catch (Exception e) {
            log.error("获取下载URL失败: {}", url, e);
            return AjaxResult.error("获取下载URL失败：" + e.getMessage());
        }
    }

    /**
     * 删除题目媒体文件
     */
    @PreAuthorize("@ss.hasPermi('question:question:edit')")
    @Log(title = "题目媒体文件", businessType = BusinessType.DELETE)
    @PostMapping("/delete")
    public AjaxResult deleteMedia(@RequestBody Map<String, String> params) {
        try {
            String url = params.get("url");
            if (url == null || url.isEmpty()) {
                return AjaxResult.error("文件URL不能为空");
            }
            ossUtil.delete(url);
            return AjaxResult.success();
        } catch (Exception e) {
            return AjaxResult.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 根据试卷ID查询媒体文件列表（支持新的media_type：7-卷别名称音频，8-大题说明音频，9-中场音频，10-试听音频，11-试听图片）
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:query')")
    @PostMapping("/listByPaperId")
    public AjaxResult listByPaperId(@RequestBody Map<String, Object> params) {
        Integer paperId = (Integer) params.get("paperId");
        if (paperId == null) {
            return AjaxResult.error("试卷ID不能为空");
        }

        List<Integer> mediaTypes = null;
        if (params.get("mediaTypes") != null) {
            @SuppressWarnings("unchecked")
            List<Integer> types = (List<Integer>) params.get("mediaTypes");
            mediaTypes = types;
        }

        LambdaQueryWrapper<QuestionMedia> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionMedia::getPaperId, paperId);
        if (mediaTypes != null && !mediaTypes.isEmpty()) {
            wrapper.in(QuestionMedia::getMediaType, mediaTypes);
        }
        wrapper.orderByAsc(QuestionMedia::getMediaType, QuestionMedia::getId);

        List<QuestionMedia> list = questionMediaBiz.list(wrapper);
        return AjaxResult.success(list);
    }

    /**
     * 根据卷别ID查询媒体文件列表（media_type=7：卷别名称音频）
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:query')")
    @PostMapping("/listByVolumeId")
    public AjaxResult listByVolumeId(@RequestBody Map<String, Object> params) {
        Integer volumeId = (Integer) params.get("volumeId");
        if (volumeId == null) {
            return AjaxResult.error("卷别ID不能为空");
        }

        LambdaQueryWrapper<QuestionMedia> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionMedia::getVolumeId, volumeId)
                .eq(QuestionMedia::getMediaType, 7)
                .orderByAsc(QuestionMedia::getId);

        List<QuestionMedia> list = questionMediaBiz.list(wrapper);
        return AjaxResult.success(list);
    }

    /**
     * 根据大题ID查询媒体文件列表（media_type=8：大题说明音频）
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:query')")
    @PostMapping("/listBySectionId")
    public AjaxResult listBySectionId(@RequestBody Map<String, Object> params) {
        Integer sectionId = (Integer) params.get("sectionId");
        if (sectionId == null) {
            return AjaxResult.error("大题ID不能为空");
        }

        LambdaQueryWrapper<QuestionMedia> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionMedia::getSectionId, sectionId)
                .eq(QuestionMedia::getMediaType, 8)
                .orderByAsc(QuestionMedia::getId);

        List<QuestionMedia> list = questionMediaBiz.list(wrapper);
        return AjaxResult.success(list);
    }

    /**
     * 根据中场配置ID查询媒体文件列表（media_type=9：中场音频）
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:query')")
    @PostMapping("/listByIntermissionId")
    public AjaxResult listByIntermissionId(@RequestBody Map<String, Object> params) {
        Integer intermissionId = (Integer) params.get("intermissionId");
        if (intermissionId == null) {
            return AjaxResult.error("中场配置ID不能为空");
        }

        LambdaQueryWrapper<QuestionMedia> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionMedia::getIntermissionId, intermissionId)
                .eq(QuestionMedia::getMediaType, 9)
                .orderByAsc(QuestionMedia::getId);

        List<QuestionMedia> list = questionMediaBiz.list(wrapper);
        return AjaxResult.success(list);
    }

    /**
     * 根据题目ID和媒体类型查询媒体文件列表（支持新的media_type：4-题目音频，5-讲解音频，6-讲解图片）
     */
    @PreAuthorize("@ss.hasPermi('question:question:query')")
    @PostMapping("/listByQuestionIdAndType")
    public AjaxResult listByQuestionIdAndType(@RequestBody Map<String, Object> params) {
        Integer questionId = (Integer) params.get("questionId");
        if (questionId == null) {
            return AjaxResult.error("题目ID不能为空");
        }

        Integer mediaType = (Integer) params.get("mediaType");
        if (mediaType == null) {
            return AjaxResult.error("媒体类型不能为空");
        }

        // 支持新的媒体类型：4-题目音频，5-讲解音频，6-讲解图片
        if (mediaType >= 4 && mediaType <= 6) {
            LambdaQueryWrapper<QuestionMedia> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(QuestionMedia::getQuestionId, questionId)
                    .eq(QuestionMedia::getMediaType, mediaType)
                    .orderByAsc(QuestionMedia::getId);

            List<QuestionMedia> list = questionMediaBiz.list(wrapper);
            return AjaxResult.success(list);
        } else {
            // 使用原有的方法（支持1-题目媒体，2-选项媒体，3-辅助识图）
            List<QuestionMedia> list = questionMediaBiz.listByQuestionIdAndType(questionId, mediaType);
            return AjaxResult.success(list);
        }
    }

    /**
     * 保存题目媒体文件记录（支持新的media_type和关联字段）
     */
    @PreAuthorize("@ss.hasPermi('question:question:add')")
    @Log(title = "题目媒体文件", businessType = BusinessType.INSERT)
    @PostMapping("/save")
    public AjaxResult saveMedia(@RequestBody com.ruoyi.student.archive.domain.bo.question.QuestionMediaBO mediaBO) {
        try {
            com.ruoyi.student.archive.domain.question.QuestionMedia media = new com.ruoyi.student.archive.domain.question.QuestionMedia();
            media.setQuestionId(mediaBO.getQuestionId());
            media.setPaperId(mediaBO.getPaperId());
            media.setVolumeId(mediaBO.getVolumeId());
            media.setSectionId(mediaBO.getSectionId());
            media.setIntermissionId(mediaBO.getIntermissionId());
            media.setMediaType(mediaBO.getMediaType());
            media.setOptionId(mediaBO.getOptionId());
            media.setBlankAreaId(mediaBO.getBlankAreaId());
            media.setMediaName(mediaBO.getMediaName());
            media.setMediaPath(mediaBO.getMediaPath());
            media.setMediaUrl(mediaBO.getMediaUrl());
            media.setMediaSize(mediaBO.getMediaSize());
            media.setMediaFormat(mediaBO.getMediaFormat());
            media.setMediaDuration(mediaBO.getMediaDuration());
            media.setIsCompressed(mediaBO.getIsCompressed());
            media.setStorageType(mediaBO.getStorageType());
            // sortNum 字段在 QuestionMedia 实体中不存在，已移除

            boolean success = questionMediaBiz.save(media);
            if (success) {
                return AjaxResult.success(media.getId());
            } else {
                return AjaxResult.error("保存失败");
            }
        } catch (Exception e) {
            return AjaxResult.error("保存失败：" + e.getMessage());
        }
    }

    /**
     * 删除题目媒体文件记录（根据ID）
     */
    @PreAuthorize("@ss.hasPermi('question:question:edit')")
    @Log(title = "题目媒体文件", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public AjaxResult removeMedia(@RequestBody Map<String, Object> params) {
        try {
            Integer id = (Integer) params.get("id");
            if (id == null) {
                return AjaxResult.error("媒体ID不能为空");
            }
            boolean success = questionMediaBiz.removeById(id);
            if (success) {
                return AjaxResult.success();
            } else {
                return AjaxResult.error("删除失败");
            }
        } catch (Exception e) {
            return AjaxResult.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 代理访问媒体文件（解决CDN防盗链403问题）
     * 允许匿名访问,用于图片预览
     * 注意：由于Spring Security白名单配置问题,暂时通过token参数来绕过认证
     */
    @GetMapping("/proxy")
    public void proxyMedia(
            @RequestParam("url") String url,
            @RequestParam(value = "token", required = false) String token,
            javax.servlet.http.HttpServletRequest request,
            javax.servlet.http.HttpServletResponse response) {
        log.info("代理访问媒体文件请求: {}, token存在: {}", url, token != null);

        try {
            // 从CDN下载文件（使用私有签名URL，避免403）
            log.info("开始下载文件: {}", url);
            // 先提取ObjectKey，再下载，避免URL编码问题
            String objectKey = ossUtil.getObjectKey(url);
            log.info("提取ObjectKey: {}", objectKey);
            byte[] fileBytes = ossUtil.downloadFileToBytes(objectKey);
            log.info("文件下载成功，大小: {} bytes", fileBytes.length);

            // 根据URL确定Content-Type
            String contentType = determineContentTypeFromUrl(url);
            log.info("Content-Type: {}", contentType);
            response.setContentType(contentType);

            // 设置缓存头（缓存1小时）
            response.setHeader("Cache-Control", "public, max-age=3600");

            // 设置内容长度
            response.setContentLength(fileBytes.length);

            // 写入响应
            response.getOutputStream().write(fileBytes);
            response.getOutputStream().flush();

            log.info("文件代理成功: {}", url);

        } catch (Exception e) {
            log.error("代理访问媒体文件失败: {}", url, e);
            if (!response.isCommitted()) {
                try {
                    response.sendError(javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "文件代理失败: " + e.getMessage());
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * 根据URL确定Content-Type
     */
    private String determineContentTypeFromUrl(String url) {
        String urlLower = url.toLowerCase();
        if (urlLower.endsWith(".jpg") || urlLower.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (urlLower.endsWith(".png")) {
            return "image/png";
        } else if (urlLower.endsWith(".gif")) {
            return "image/gif";
        } else if (urlLower.endsWith(".webp")) {
            return "image/webp";
        } else if (urlLower.endsWith(".bmp")) {
            return "image/bmp";
        } else if (urlLower.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (urlLower.endsWith(".wav")) {
            return "audio/wav";
        } else if (urlLower.endsWith(".mp4")) {
            return "video/mp4";
        }
        return "application/octet-stream";
    }
}
