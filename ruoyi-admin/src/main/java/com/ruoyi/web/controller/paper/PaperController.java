package com.ruoyi.web.controller.paper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.student.archive.domain.bo.paper.*;
import com.ruoyi.student.archive.domain.dto.paper.PaperDTO;
import com.ruoyi.student.archive.domain.dto.paper.PackageTaskInfo;
import com.ruoyi.student.archive.service.paper.IPaperService;
import com.ruoyi.student.archive.service.paper.IPackageTaskService;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.ruoyi.common.utils.http.HttpUtils;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;

/**
 * 试卷管理Controller
 * 
 * @author ruoyi
 */
@Slf4j
@RestController
@RequestMapping("/paper")
public class PaperController extends BaseController {
    @Autowired
    private IPaperService paperService;

    @Autowired
    private IPackageTaskService packageTaskService;

    @Autowired
    private com.ruoyi.common.utils.oss.exam.question.OssUtil ossUtil;

    @Autowired
    private com.ruoyi.student.archive.service.paper.IChunkUploadService chunkUploadService;

    /**
     * 分页查询试卷列表
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:list')")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody PaperPageBO pageBO) {
        Page<PaperDTO> page = paperService.pageList(pageBO);
        TableDataInfo dataTable = new TableDataInfo();
        dataTable.setCode(200);
        dataTable.setMsg("查询成功");
        dataTable.setRows(page.getRecords());
        dataTable.setTotal(page.getTotal());
        return dataTable;
    }

    /**
     * 获取试卷详情（完整数据：包含卷别、大题、题目、中场配置）
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:query')")
    @PostMapping("/detail")
    public AjaxResult getPaper(@Validated @RequestBody PaperIdBO idBO) {
        PaperDTO paper = paperService.getPaperFullData(idBO.getId());
        return success(paper);
    }

    /**
     * 根据试卷编码获取试卷详情
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:query')")
    @PostMapping("/detailByCode")
    public AjaxResult getPaperByCode(@Validated @RequestBody PaperCodeBO codeBO) {
        PaperDTO paper = paperService.getPaperByCode(codeBO.getPaperCode());
        return success(paper);
    }

    /**
     * 根据业务类型和业务ID查询试卷列表
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:list')")
    @PostMapping("/listByBusiness")
    public AjaxResult listByBusiness(@Validated @RequestBody PaperBusinessBO businessBO) {
        List<PaperDTO> list = paperService.listByBusiness(
                businessBO.getBusinessType(),
                businessBO.getBusinessId());
        return success(list);
    }

    /**
     * 根据试卷ID列表查询试卷列表
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:list')")
    @PostMapping("/listByIds")
    public AjaxResult listByIds(@Validated @RequestBody PaperIdsBO idsBO) {
        List<PaperDTO> list = paperService.listByIds(idsBO.getIds());
        return success(list);
    }

    /**
     * 新增试卷（完整数据：试卷+卷别+大题+题目+中场配置）
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:add')")
    @Log(title = "试卷管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult createPaper(@Validated @RequestBody PaperFullDataBO fullDataBO) {
        Integer paperId = paperService.createPaperWithFullData(fullDataBO);
        return success(paperId);
    }

    /**
     * 修改试卷（完整数据，删除后新增）
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:edit')")
    @Log(title = "试卷管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult updatePaper(@Validated @RequestBody PaperFullDataBO fullDataBO) {
        paperService.updatePaperWithFullData(fullDataBO);
        return success();
    }

    /**
     * 删除试卷
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:remove')")
    @Log(title = "试卷管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public AjaxResult removePaper(@Validated @RequestBody PaperIdsBO idsBO) {
        paperService.batchDeletePaper(idsBO.getIds());
        return success();
    }

    /**
     * 生成试卷包（异步）
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:edit')")
    @Log(title = "试卷管理", businessType = BusinessType.UPDATE)
    @PostMapping("/generatePackage")
    public AjaxResult generatePackage(@Validated @RequestBody PaperIdBO idBO) {
        try {
            // 提交任务，立即返回
            paperService.generatePaperPackage(idBO.getId());
            return success("任务已提交，正在生成...");
        } catch (ServiceException e) {
            log.error("提交试卷包生成任务失败，试卷ID：{}", idBO.getId(), e);
            return error(e.getMessage());
        } catch (Exception e) {
            log.error("提交试卷包生成任务异常，试卷ID：{}", idBO.getId(), e);
            return error("提交任务失败：" + e.getMessage());
        }
    }

    /**
     * 查询试卷包生成任务状态
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:query')")
    @GetMapping("/getPackageTaskStatus")
    public AjaxResult getPackageTaskStatus(@RequestParam Integer paperId) {
        PackageTaskInfo taskInfo = packageTaskService.getTask(paperId);
        if (taskInfo == null) {
            return success(null);
        }
        return success(taskInfo);
    }

    /**
     * 取消试卷包生成任务
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:edit')")
    @PostMapping("/cancelPackageTask")
    public AjaxResult cancelPackageTask(@Validated @RequestBody PaperIdBO idBO) {
        boolean cancelled = packageTaskService.cancelTask(idBO.getId());
        if (cancelled) {
            return success("任务已取消");
        } else {
            return error("取消失败，任务可能已完成或不存在");
        }
    }

    /**
     * 获取所有任务列表（用于任务中心显示）
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:query')")
    @GetMapping("/getAllPackageTasks")
    public AjaxResult getAllPackageTasks() {
        List<PackageTaskInfo> allTasks = packageTaskService.getAllTasks();
        return success(allTasks);
    }

    /**
     * 流式下载试卷包（支持Range请求，用于大文件下载）
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:query')")
    @GetMapping("/downloadPackageStream")
    public void downloadPackageStream(
            @RequestParam Integer paperId,
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            javax.servlet.http.HttpServletResponse response) {
        java.io.InputStream inputStream = null;
        java.io.OutputStream outputStream = null;
        try {
            // 获取试卷信息
            PaperDTO paper = paperService.getPaperById(paperId);
            if (paper == null) {
                throw new RuntimeException("试卷不存在");
            }

            // 检查是否已生成试卷包
            if (com.ruoyi.common.utils.StringUtils.isEmpty(paper.getPackageHash()) || paper.getPackageSize() == null) {
                throw new RuntimeException("试卷包尚未生成，请先生成试卷包");
            }

            // 构建试卷包文件名
            Integer currentVersion = paper.getVersion() != null ? paper.getVersion() : 0;
            String packageFileName = "paper_packages/" + paper.getPaperCode() + "_v" + currentVersion + ".zip";

            // 获取文件大小
            long fileSize = ossUtil.getFileSize(packageFileName);

            // 解析Range请求
            long start = 0;
            long end = fileSize - 1;
            long contentLength = fileSize;
            int statusCode = 200;

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.substring(6).split("-");
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                }
                if (end >= fileSize) {
                    end = fileSize - 1;
                }
                contentLength = end - start + 1;
                statusCode = 206; // Partial Content
            }

            // 设置响应头
            response.setContentType("application/zip");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Length", String.valueOf(contentLength));

            if (statusCode == 206) {
                response.setStatus(javax.servlet.http.HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range",
                        String.format("bytes %d-%d/%d", start, end, fileSize));
            }

            // 文件名URL编码
            String fileName = "paper_" + paperId + ".zip";
            String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);

            // 流式下载
            inputStream = ossUtil.downloadFileToStream(packageFileName, start, end);
            outputStream = response.getOutputStream();

            // 使用缓冲区流式传输
            byte[] buffer = new byte[8192]; // 8KB缓冲区
            long bytesTransferred = 0;
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                bytesTransferred += bytesRead;
            }
            outputStream.flush();

            log.info("流式下载完成，试卷ID：{}，Range: {}-{}，传输大小: {} 字节",
                    paperId, start, end, bytesTransferred);

        } catch (Exception e) {
            log.error("流式下载试卷包失败，试卷ID：{}", paperId, e);
            if (!response.isCommitted()) {
                try {
                    response.sendError(javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            "下载失败：" + e.getMessage());
                } catch (Exception ex) {
                    log.error("发送错误响应失败", ex);
                }
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    log.warn("关闭输入流失败", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    log.warn("关闭输出流失败", e);
                }
            }
        }
    }

    /**
     * 下载试卷包（兼容旧接口，保留）
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:query')")
    @PostMapping("/downloadPackage")
    public void downloadPackage(@Validated @RequestBody PaperIdBO idBO,
            javax.servlet.http.HttpServletResponse response) {
        java.io.OutputStream out = null;
        try {
            byte[] packageData = paperService.downloadPaperPackage(idBO.getId());

            // 验证数据
            if (packageData == null || packageData.length == 0) {
                log.error("试卷包数据为空，试卷ID：{}", idBO.getId());
                throw new RuntimeException("试卷包数据为空");
            }

            // 验证ZIP文件头
            if (packageData.length < 4 || packageData[0] != 0x50 || packageData[1] != 0x4B) {
                log.error("下载的试卷包格式不正确，试卷ID：{}，大小：{} 字节", idBO.getId(), packageData.length);
                throw new RuntimeException("试卷包格式不正确");
            }

            log.info("准备下载试卷包，试卷ID：{}，大小：{} 字节", idBO.getId(), packageData.length);

            // 设置响应头
            response.setContentType("application/zip");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);

            // 文件名URL编码，避免中文文件名乱码
            String fileName = "paper_" + idBO.getId() + ".zip";
            String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
            response.setContentLength(packageData.length);

            // 写入响应流
            out = response.getOutputStream();
            out.write(packageData, 0, packageData.length);
            out.flush();

            log.info("试卷包下载完成，试卷ID：{}", idBO.getId());

        } catch (Exception e) {
            log.error("下载试卷包失败，试卷ID：{}", idBO.getId(), e);
            throw new RuntimeException("下载试卷包失败：" + e.getMessage());
        } finally {
            // 确保输出流关闭
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e) {
                    log.error("关闭输出流失败", e);
                }
            }
        }
    }

    /**
     * 初始化分片上传
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:edit')")
    @PostMapping("/initChunkUpload")
    public AjaxResult initChunkUpload(
            @Validated @RequestBody com.ruoyi.student.archive.domain.bo.paper.ChunkUploadInitBO initBO) {
        try {
            String uploadId = chunkUploadService.initChunkUpload(initBO);
            AjaxResult result = success();
            result.put("uploadId", uploadId);
            // 计算分片数量
            long chunkSize = initBO.getChunkSize() != null ? initBO.getChunkSize() : 10L * 1024 * 1024;
            int chunkCount = (int) Math.ceil((double) initBO.getFileSize() / chunkSize);
            result.put("chunkCount", chunkCount);
            result.put("chunkSize", chunkSize);
            return result;
        } catch (Exception e) {
            log.error("初始化分片上传失败", e);
            return error("初始化分片上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传分片
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:edit')")
    @PostMapping("/uploadChunk")
    public AjaxResult uploadChunk(
            @RequestParam String uploadId,
            @RequestParam Integer chunkIndex,
            @RequestParam("chunkFile") org.springframework.web.multipart.MultipartFile chunkFile) {
        try {
            chunkUploadService.uploadChunk(uploadId, chunkIndex, chunkFile);
            return success("分片上传成功");
        } catch (Exception e) {
            log.error("上传分片失败，上传ID: {}, 分片索引: {}", uploadId, chunkIndex, e);
            return error("上传分片失败: " + e.getMessage());
        }
    }

    /**
     * 完成分片上传
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:edit')")
    @PostMapping("/completeChunkUpload")
    public AjaxResult completeChunkUpload(
            @Validated @RequestBody com.ruoyi.student.archive.domain.bo.paper.ChunkUploadCompleteBO completeBO) {
        try {
            String fileUrl = chunkUploadService.completeChunkUpload(completeBO);
            AjaxResult result = success("分片上传完成");
            result.put("fileUrl", fileUrl);
            return result;
        } catch (Exception e) {
            log.error("完成分片上传失败", e);
            return error("完成分片上传失败: " + e.getMessage());
        }
    }

    /**
     * 取消分片上传
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:edit')")
    @PostMapping("/cancelChunkUpload")
    public AjaxResult cancelChunkUpload(@RequestParam String uploadId) {
        try {
            chunkUploadService.cancelChunkUpload(uploadId);
            return success("已取消分片上传");
        } catch (Exception e) {
            log.error("取消分片上传失败", e);
            return error("取消分片上传失败: " + e.getMessage());
        }
    }

    /**
     * TTS 工具接口转发
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:edit')")
    @PostMapping("/tool/tts")
    public AjaxResult toolTTS(@RequestBody Map<String, String> params) {
        String text = params.get("text");
        if (text == null || text.isEmpty()) {
            return error("文本不能为空");
        }

        try {
            // 调用 Python 服务
            String url = "http://127.0.0.1:8088/tools/tts";
            String jsonBody = JSON.toJSONString(params);

            String result = HttpUtils.sendPost(url, jsonBody, "application/json");

            JSONObject json = JSON.parseObject(result);
            if (json != null && json.getBooleanValue("success")) {
                return success(json.getJSONObject("data"));
            } else {
                String msg = json != null ? json.getString("message") : "未知错误";
                return error("TTS生成失败: " + msg);
            }
        } catch (Exception e) {
            log.error("TTS调用失败", e);
            return error("TTS调用失败: " + e.getMessage());
        }
    }
}
