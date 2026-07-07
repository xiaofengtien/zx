package com.zx.student.archive.domain.dto.question;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 题目媒体文件DTO
 * 
 * @author zx
 */
@Data
public class QuestionMediaDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 媒体ID
     */
    private Integer id;

    /**
     * 题目ID
     */
    private Integer questionId;

    /**
     * 媒体类型：1-题目媒体，2-选项媒体
     */
    private Integer mediaType;

    /**
     * 选项ID（如果是选项媒体）
     */
    private Integer optionId;

    /**
     * 完形填空区域ID（如果是完形填空的选项媒体）
     */
    private Integer blankAreaId;

    /**
     * 媒体文件名
     */
    private String mediaName;

    /**
     * 媒体路径（服务器路径）
     */
    private String mediaPath;

    /**
     * 访问URL（CDN/OSS）
     */
    private String mediaUrl;

    /**
     * 文件大小（字节）
     */
    private Integer mediaSize;

    /**
     * 文件格式：jpg/png/mp3/mp4等
     */
    private String mediaFormat;

    /**
     * 媒体时长（秒，音频/视频）
     */
    private Integer mediaDuration;

    /**
     * 是否压缩：1-是，0-否
     */
    private Integer isCompressed;

    /**
     * 存储类型：0-在线路径，1-SQLite BLOB，2-文件系统
     */
    private Integer storageType;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 素材ID（兼容旧字段，用于AppMaterialBusinessRef关联）
     */
    private Integer materialId;

    /**
     * 排序号（兼容旧字段，用于AppMaterialBusinessRef关联）
     */
    private Integer sortNum;
}
