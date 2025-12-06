package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户试卷信息
 */
@Data
public class UserPaperBO implements Serializable {

    /**
     * 主键ID
     */
    
    private Integer id;

    /**
     * 业务类型
     */
    
    private Integer businessType;

    /**
     * 业务ID
     */
    
    private Integer businessId;

    /**
     * 试卷名称
     */
    
    private String paperName;

    /**
     * 用户ID
     */
    
    private Integer appUserId;

    /**
     * 是否提交
     */
    
    private Integer isSubmit;

    /**
     * 状态
     */
    
    private Integer status;
}