package com.zx.student.archive.domain.dto.paper;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zx.common.annotation.Excel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 答题结果列表DTO
 * 
 * @author zx
 */
@Data
public class PaperResultListDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 答题记录ID
     */
    private Integer id;

    /**
     * 试卷ID
     */
    private Integer paperId;

    /**
     * 试卷名称
     */
    @Excel(name = "试卷名称", width = 30)
    private String paperName;

    /**
     * 试卷编码
     */
    @Excel(name = "试卷编码", width = 20)
    private String paperCode;

    /**
     * 用户ID（学员档案ID）
     */
    private Integer appUserId;

    /**
     * 学号（学员账号）
     */
    @Excel(name = "学号", width = 15)
    private String studentAccount;

    /**
     * 总分
     */
    @Excel(name = "总分", width = 10)
    private BigDecimal totalScore;

    /**
     * 得分
     */
    @Excel(name = "得分", width = 10)
    private BigDecimal score;

    /**
     * 正确率（百分比）
     */
    @Excel(name = "正确率", width = 10, suffix = "%")
    private Integer correctRate;

    /**
     * 提交状态：1-已提交，0-未提交
     */
    @Excel(name = "提交状态", width = 12, readConverterExp = "0=未提交,1=已提交")
    private Integer submitStatus;

    /**
     * 提交时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "提交时间", width = 20, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;
}

