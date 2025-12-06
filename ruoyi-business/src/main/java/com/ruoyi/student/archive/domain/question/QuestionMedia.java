package com.ruoyi.student.archive.domain.question;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 题目媒体文件实体
 * 
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("question_media")
public class QuestionMedia extends BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 媒体ID
     */
    @TableId(type = IdType.AUTO)
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
     * 媒体类型（仅存储题目相关的媒体）：
     * 1-题目媒体（题目内容相关的图片、视频等）
     * 2-选项媒体（选项相关的音频、图片等）
     * 3-辅助识图
     * 4-题目音频（题目本身的音频，如听力题的题目音频）
     * 5-讲解音频（答题讲解的音频）
     * 6-讲解图片（答题讲解的图片）
     * 
     * 注意：试卷相关的媒体（卷别音频、大题音频、中场音频等）存储在对应的实体表中
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
     * 媒体文件名
     */
    private String mediaName;

    /**
     * 媒体路径（服务器路径，用于在线）
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

}

