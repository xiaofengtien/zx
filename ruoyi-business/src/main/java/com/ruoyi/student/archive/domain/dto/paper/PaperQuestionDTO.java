package com.ruoyi.student.archive.domain.dto.paper;

import com.ruoyi.student.archive.domain.dto.question.QuestionAnswerDTO;
import com.ruoyi.student.archive.domain.dto.question.QuestionBlankAreaDTO;
import com.ruoyi.student.archive.domain.dto.question.QuestionMediaDTO;
import com.ruoyi.student.archive.domain.dto.question.QuestionRecognitionDTO;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 试卷题目DTO
 */
@Data
public class PaperQuestionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联ID（paper_question表的主键）
     */
    private Integer id;

    /**
     * 试卷ID
     */
    private Integer paperId;

    /**
     * 题目ID
     */
    private Integer questionId;

    /**
     * 所属大题ID（关联paper_section.id）
     */
    private Integer sectionId;

    /**
     * 在大题中的顺序（section_order）
     */
    private Integer sectionOrder;

    /**
     * 排序号（题目在试卷中的顺序）
     */
    private Integer sortOrder;

    /**
     * 分值
     */
    private BigDecimal score;

    /**
     * 题目标题
     */
    private String title;

    /**
     * 题目类型
     */
    private Integer type;

    /**
     * 题目类型名称
     */
    private String typeName;

    /**
     * 学科ID
     */
    private Integer subjectId;

    /**
     * 学科名称
     */
    private String subjectName;

    /**
     * 媒体文件类型
     */
    private Integer mediaType;

    /**
     * 选项类型
     */
    private Integer optionType;
    
    /**
     * 媒体文件url
     */
    private List<QuestionMediaDTO> mediaUrl;

    /**
     * 辅助识别URL
     */
    private List<QuestionRecognitionDTO> aidedRecognitionUrl;

    /**
     * 权重
     */
    private Integer weight;

    /**
     * 答案
     */
    private String answer;

    /**
     * 解析
     */
    private String analyzes;

    /**
     * 答案列表
     */
    private List<QuestionAnswerDTO> answers;

    /**
     * 完形填空区域列表
     */
    private List<QuestionBlankAreaDTO> blankAreas;

    /**
     * 答题结果：1-正确，0-错误
     */
    private Integer result;

    /**
     * 排序号（兼容旧字段，与sortOrder相同）
     */
    private Integer sortNum;

    /**
     * 类型数量
     */
    private Integer typeCount;

    /**
     * 类型索引
     */
    private Integer typeIndex;

    /**
     * 状态
     */
    private Integer status;
} 