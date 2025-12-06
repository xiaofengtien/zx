package com.ruoyi.student.archive.domain.question;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;


@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("question_category_business_settings")
@AllArgsConstructor
@NoArgsConstructor
public class QuestionCategoryBusinessSettings extends BaseEntity implements Serializable {
    private Integer id;
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 业务ID
     */
    private Integer businessId;

    /**
     * 业务类型 5-默认答题
     */
    private Integer businessType;

    private String coverUrlId;
    
    private String coverUrl;

    private String coverName;

    /**
     * 出题方式
     */
    private Integer questionMethod;

    /**
     * 题目数量
     */
    private Integer questionNum;

    /**
     * 状态（1-启用 0-禁用）
     */
    private Integer status;
}
