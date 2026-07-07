package com.zx.student.archive.domain.bo.paper;

import lombok.Data;

import java.io.Serializable;

/**
 * 答题结果分页查询参数
 * 
 * @author zx
 */
@Data
public class PaperResultPageBO implements Serializable {
    
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
     * 学员档案ID（保留用于兼容）
     */
    private Integer appUserId;

    /**
     * 学号（学员账号，studentAccount）
     */
    private String studentAccount;

    /**
     * 提交状态：1-已提交，0-未提交
     */
    private Integer submitStatus;
}

