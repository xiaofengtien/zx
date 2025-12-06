package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import java.io.Serializable;

/**
 * 试卷分页查询参数
 * 
 * @author ruoyi
 */
@Data
public class PaperPageBO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 当前页码（默认1）
     */
    private Long current = 1L;

    /**
     * 每页显示数量（默认10）
     */
    private Long size = 10L;

    /**
     * 前端分页参数：页码（兼容前端）
     */
    private Integer pageNum;

    /**
     * 前端分页参数：每页数量（兼容前端）
     */
    private Integer pageSize;

    /**
     * 试卷名称（模糊查询）
     */
    private String paperName;

    /**
     * 试卷编码
     */
    private String paperCode;

    /**
     * 业务类型
     */
    private Integer businessType;

    /**
     * 业务ID
     */
    private Integer businessId;

    /**
     * 状态：1-启用，0-禁用
     */
    private Integer status;

    /**
     * 试卷类型（字典编码：paper_type）
     */
    private String paperType;
}

