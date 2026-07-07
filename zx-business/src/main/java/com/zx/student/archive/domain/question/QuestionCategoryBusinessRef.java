package com.zx.student.archive.domain.question;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zx.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 题目分类业务关联表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("question_category_business_ref")
public class QuestionCategoryBusinessRef extends BaseEntity implements Serializable {
    private Integer id;

    /**
     * 题目分类ID
     */
    private Integer questionCategoryId;

    /**
     * 业务ID
     */
    private Integer businessId;

    /**
     * 业务类型（1-音频专辑 2-视频专辑 3-图书）
     */
    private Integer businessType;

    /**
     * 排序
     */
    private Integer sortNum;
    /**
     * 状态（1-启用 0-禁用）
     */
    private Integer status;
}
