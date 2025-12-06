package com.ruoyi.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;

/**
 * 题目媒体文件BO（用于创建和更新）
 * 
 * @author ruoyi
 */
@Data
public class QuestionMediaBO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 媒体ID（更新时使用）
     */
    private Integer id;
    
    /**
     * 题目ID
     */
    private Integer questionId;
    
    /**
     * 试卷ID（关联paper.id，用于试听媒体等）
     */
    private Integer paperId;
    
    /**
     * 卷别ID（关联paper_volume.id，用于卷别名称音频）
     */
    private Integer volumeId;
    
    /**
     * 大题ID（关联paper_section.id，用于大题说明音频）
     */
    private Integer sectionId;
    
    /**
     * 中场配置ID（关联paper_intermission.id，用于中场音频）
     */
    private Integer intermissionId;
    
    /**
     * 媒体类型：
     * 1-题目媒体，2-选项媒体，3-辅助识图
     * 4-题目音频，5-讲解音频，6-讲解图片
     * 7-卷别名称音频，8-大题说明音频，9-中场音频
     * 10-试听音频，11-试听图片
     */
    private Integer mediaType;
    
    /**
     * 选项ID（如果是选项媒体，关联question_answer.id）
     */
    private Integer optionId;
    
    /**
     * 完形填空区域ID（如果是完形填空的选项媒体）
     */
    private Integer blankAreaId;
    
    /**
     * 素材库ID（兼容旧字段）
     */
    private Integer materialId;
    
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
     * 排序号（兼容旧字段）
     */
    private Integer sortNum;
}
