package com.ruoyi.student.archive.service.paper;

import com.ruoyi.student.archive.domain.bo.paper.ImportConfirmBO;
import com.ruoyi.student.archive.domain.bo.paper.ImportSessionAddVolumeBO;
import com.ruoyi.student.archive.domain.bo.paper.ImportSessionFinalizeBO;
import com.ruoyi.student.archive.domain.dto.paper.ImportFinalizeResultDTO;
import com.ruoyi.student.archive.domain.dto.paper.ImportQuestionsResultDTO;
import com.ruoyi.student.archive.domain.dto.paper.ImportSessionDTO;
import com.ruoyi.student.archive.domain.dto.paper.ParseResultDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 试卷智能导入服务接口
 *
 * @author ruoyi
 */
public interface IPaperImportService {

    /**
     * 解析 Word 文件和音频文件
     *
     * @param wordFile    Word 文档
     * @param audioFile   音频文件（可选）
     * @param audioOssUrl 音频 OSS URL（可选，用于 ASR 识别）
     * @return 解析结果
     */
    ParseResultDTO parseFiles(MultipartFile wordFile, MultipartFile audioFile, String audioOssUrl);

    /**
     * 批量创建题目（将解析结果保存到 question 表）
     * 
     * @param parseResult         解析结果
     * @param categoryId          题目分类ID
     * @param subjectId           学科ID
     * @param defaultQuestionType 默认题目类型（-1 表示使用解析结果）
     * @param listeningOnly       是否仅导入听力部分
     * @return 创建结果，包含题目ID列表
     */
    ImportQuestionsResultDTO createQuestions(ParseResultDTO parseResult, Integer categoryId, Integer subjectId,
            Integer defaultQuestionType, Boolean listeningOnly);

    /**
     * 确认导入试卷
     *
     * @param confirmBO 导入确认数据
     * @return 导入的试卷ID
     */
    Integer confirmImport(ImportConfirmBO confirmBO);

    /**
     * 检查解析服务是否可用
     *
     * @return true-可用，false-不可用
     */
    boolean isParseServiceAvailable();

    // ============ 多卷别顺序导入（会话管理）============

    /**
     * 开始导入会话
     * 
     * @return 会话标识（UUID）
     */
    String startImportSession();

    /**
     * 添加卷别到导入会话
     * 
     * @param bo 添加卷别参数
     * @return 当前已添加的卷别数量
     */
    Integer addVolumeToSession(ImportSessionAddVolumeBO bo);

    /**
     * 获取导入会话状态
     * 
     * @param sessionKey 会话标识
     * @return 会话状态
     */
    ImportSessionDTO getImportSession(String sessionKey);

    /**
     * 完成导入会话（创建试卷和题目）
     * 
     * @param bo 完成导入参数
     * @return 导入结果（包含试卷ID和题目数量）
     */
    ImportFinalizeResultDTO finalizeImportSession(ImportSessionFinalizeBO bo);
}
