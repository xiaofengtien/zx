package com.zx.student.archive.domain.question;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zx.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_category")
public class QuestionCategory extends BaseEntity {
    /**
     * 分类ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 类型名称
     */
    private String name;

    /**
     * 父级id
     */
    private Integer fatherId;

    /**
     * 是否默认分类：0 否 1 是
     */
    private Boolean isDefault;

    /**
     * 排序号
     */
    private Integer sortNum;

    /**
     * 状态 0-正常 1-停用
     */
    private Integer status;
} 