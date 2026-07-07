package com.zx.student.archive.domain.question;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zx.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 题目组模板实体
 * 
 * @author zx
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("question_group")
public class QuestionGroup extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 题目组名称
     */
    private String groupName;

    /**
     * 所属分类ID
     */
    private Integer categoryId;

    /**
     * 音频OSS地址
     */
    private String audioUrl;

    /**
     * 音频存储路径
     */
    private String audioPath;

    /**
     * 音频时长(秒)
     */
    private Integer audioDuration;

    /**
     * 音频标签(如:Questions 67-69)
     */
    private String audioLabel;

    /**
     * 题目数量
     */
    private Integer questionCount;

    /**
     * 描述
     */
    private String description;


    /**
     * 题目列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<Question> questions;

    /**
     * 题目ID列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<Integer> questionIds;
}
