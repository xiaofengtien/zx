package com.zx.web.controller.paper;

import com.zx.common.annotation.Log;
import com.zx.common.core.controller.BaseController;
import com.zx.common.core.domain.AjaxResult;
import com.zx.common.enums.BusinessType;
import com.zx.student.archive.domain.bo.paper.ImportConfirmBO;
import com.zx.student.archive.domain.bo.paper.ImportSessionAddVolumeBO;
import com.zx.student.archive.domain.bo.paper.ImportSessionFinalizeBO;
import com.zx.student.archive.domain.dto.paper.ImportFinalizeResultDTO;
import com.zx.student.archive.domain.dto.paper.ImportSessionDTO;
import com.zx.student.archive.domain.dto.paper.ParseResultDTO;
import com.zx.student.archive.domain.dto.paper.ParseTaskDTO;
import com.zx.student.archive.service.paper.IPaperImportService;
import com.zx.student.archive.service.paper.IParseTaskService;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 试卷智能导入Controller
 * 
 * @author zx
 */
@Slf4j
@RestController
@RequestMapping("/paper/import")
public class PaperImportController extends BaseController {

    @Autowired
    private IPaperImportService paperImportService;

    @Autowired
    private IParseTaskService parseTaskService;

    /**
     * 提交解析任务（异步）
     * 
     * @param wordFile    Word 文档（必须）
     * @param audioFile   音频文件（可选）
     * @param audioOssUrl 音频 OSS URL（可选，用于 ASR 识别）
     * @return 任务ID
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:add')")
    @PostMapping("/submitTask")
    public AjaxResult submitTask(
            @RequestParam("wordFile") MultipartFile wordFile,
            @RequestParam(value = "audioFile", required = false) MultipartFile audioFile,
            @RequestParam(value = "audioOssUrl", required = false) String audioOssUrl) {
        try {
            // 验证文件
            if (wordFile == null || wordFile.isEmpty()) {
                return error("Word 文件不能为空");
            }

            String fileName = wordFile.getOriginalFilename();
            if (fileName == null || !fileName.toLowerCase().endsWith(".docx")) {
                return error("请上传 .docx 格式的 Word 文档");
            }

            // 提交异步任务（传递音频 OSS URL）
            String taskId = parseTaskService.submitTask(wordFile, audioFile, audioOssUrl);

            AjaxResult result = success("任务已提交");
            result.put("taskId", taskId);
            return result;

        } catch (Exception e) {
            log.error("提交解析任务失败", e);
            return error("提交失败：" + e.getMessage());
        }
    }

    /**
     * 获取解析任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态和结果
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:add')")
    @GetMapping("/task/{taskId}")
    public AjaxResult getTaskStatus(@PathVariable String taskId) {
        try {
            ParseTaskDTO task = parseTaskService.getTaskStatus(taskId);
            return success(task);
        } catch (Exception e) {
            log.error("获取任务状态失败", e);
            return error("获取状态失败：" + e.getMessage());
        }
    }

    /**
     * 取消解析任务
     * 
     * @param taskId 任务ID
     * @return 是否成功
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:add')")
    @PostMapping("/task/{taskId}/cancel")
    public AjaxResult cancelTask(@PathVariable String taskId) {
        try {
            boolean success = parseTaskService.cancelTask(taskId);
            if (success) {
                return success("任务已取消");
            } else {
                return error("无法取消任务（任务可能正在处理中或已完成）");
            }
        } catch (Exception e) {
            log.error("取消任务失败", e);
            return error("取消失败：" + e.getMessage());
        }
    }

    /**
     * 解析 Word 文件和音频文件，返回预览数据（同步方式，保留用于测试）
     * 
     * @param wordFile  Word 文档（必须）
     * @param audioFile 音频文件（可选）
     * @return 解析结果预览
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:add')")
    @PostMapping("/parse")
    public AjaxResult parseFiles(
            @RequestParam("wordFile") MultipartFile wordFile,
            @RequestParam(value = "audioFile", required = false) MultipartFile audioFile) {
        try {
            // 验证文件
            if (wordFile == null || wordFile.isEmpty()) {
                return error("Word 文件不能为空");
            }

            String fileName = wordFile.getOriginalFilename();
            if (fileName == null || !fileName.toLowerCase().endsWith(".docx")) {
                return error("请上传 .docx 格式的 Word 文档");
            }

            // 调用解析服务（同步接口不支持 ASR，传 null）
            ParseResultDTO result = paperImportService.parseFiles(wordFile, audioFile, null);
            return success(result);

        } catch (Exception e) {
            log.error("解析文件失败", e);
            return error("解析失败：" + e.getMessage());
        }
    }

    /**
     * 确认导入试卷
     * 
     * @param confirmBO 导入确认数据（包含解析结果和用户修改）
     * @return 导入的试卷ID
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:add')")
    @Log(title = "智能导入试卷", businessType = BusinessType.INSERT)
    @PostMapping("/confirm")
    public AjaxResult confirmImport(@RequestBody ImportConfirmBO confirmBO) {
        try {
            Integer paperId = paperImportService.confirmImport(confirmBO);
            return success(paperId);
        } catch (Exception e) {
            log.error("导入试卷失败", e);
            return error("导入失败：" + e.getMessage());
        }
    }

    /**
     * 批量创建题目（将解析结果保存到题库）
     * 
     * @param request 包含解析结果和分类ID
     * @return 创建结果，包含题目ID列表和更新后的解析结构
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:add')")
    @Log(title = "批量导入题目", businessType = BusinessType.INSERT)
    @PostMapping("/createQuestions")
    public AjaxResult createQuestions(@RequestBody CreateQuestionsRequest request) {
        try {
            if (request.getParseResult() == null) {
                return error("解析结果不能为空");
            }
            if (request.getCategoryId() == null) {
                return error("题目分类ID不能为空");
            }

            var result = paperImportService.createQuestions(
                    request.getParseResult(),
                    request.getCategoryId(),
                    request.getSubjectId(),
                    request.getDefaultQuestionType(),
                    request.getListeningOnly());
            return success(result);
        } catch (Exception e) {
            log.error("批量创建题目失败", e);
            return error("创建题目失败：" + e.getMessage());
        }
    }

    /**
     * 批量创建题目的请求体
     */
    @lombok.Data
    public static class CreateQuestionsRequest {
        /**
         * 解析结果
         */
        private ParseResultDTO parseResult;

        /**
         * 题目分类ID
         */
        private Integer categoryId;

        /**
         * 学科ID
         */
        private Integer subjectId;

        /**
         * 默认题目类型（-1 表示使用解析结果）
         */
        private Integer defaultQuestionType;

        /**
         * 是否仅导入听力部分
         */
        private Boolean listeningOnly;
    }

    /**
     * 获取解析服务状态
     */
    @GetMapping("/status")
    public AjaxResult getParseServiceStatus() {
        try {
            boolean available = paperImportService.isParseServiceAvailable();
            AjaxResult result = success();
            result.put("available", available);
            result.put("message", available ? "解析服务正常" : "解析服务不可用");
            return result;
        } catch (Exception e) {
            log.error("检查解析服务状态失败", e);
            return error("检查失败：" + e.getMessage());
        }
    }

    // ============ 多卷别顺序导入（会话管理）============

    /**
     * 开始导入会话
     * 
     * @return 会话标识
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:add')")
    @PostMapping("/session/start")
    public AjaxResult startSession() {
        try {
            String sessionKey = paperImportService.startImportSession();
            AjaxResult result = success("会话创建成功");
            result.put("sessionKey", sessionKey);
            return result;
        } catch (Exception e) {
            log.error("创建导入会话失败", e);
            return error("创建会话失败：" + e.getMessage());
        }
    }

    /**
     * 添加卷别到导入会话
     * 
     * @param bo 添加卷别参数
     * @return 当前已添加的卷别数量
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:add')")
    @PostMapping("/session/addVolume")
    public AjaxResult addVolume(@RequestBody @Validated ImportSessionAddVolumeBO bo) {
        try {
            Integer volumeCount = paperImportService.addVolumeToSession(bo);
            AjaxResult result = success("卷别添加成功");
            result.put("volumeCount", volumeCount);
            return result;
        } catch (Exception e) {
            log.error("添加卷别失败", e);
            return error("添加卷别失败：" + e.getMessage());
        }
    }

    /**
     * 获取导入会话状态
     * 
     * @param sessionKey 会话标识
     * @return 会话状态
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:add')")
    @GetMapping("/session/{sessionKey}")
    public AjaxResult getSession(@PathVariable String sessionKey) {
        try {
            ImportSessionDTO session = paperImportService.getImportSession(sessionKey);
            return success(session);
        } catch (Exception e) {
            log.error("获取会话状态失败", e);
            return error("获取会话状态失败：" + e.getMessage());
        }
    }

    /**
     * 完成导入会话（创建试卷和题目）
     * 
     * @param bo 完成导入参数
     * @return 导入结果（包含试卷ID和题目数量）
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:add')")
    @Log(title = "多卷别导入试卷", businessType = BusinessType.INSERT)
    @PostMapping("/session/finalize")
    public AjaxResult finalizeSession(@RequestBody @Validated ImportSessionFinalizeBO bo) {
        try {
            ImportFinalizeResultDTO result = paperImportService.finalizeImportSession(bo);
            return success(result);
        } catch (Exception e) {
            log.error("完成导入失败", e);
            return error("完成导入失败：" + e.getMessage());
        }
    }
}
