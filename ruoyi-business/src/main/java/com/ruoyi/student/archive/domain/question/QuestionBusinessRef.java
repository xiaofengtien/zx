package com.ruoyi.student.archive.domain.question;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("question_business_ref")
@AllArgsConstructor
@NoArgsConstructor
public class QuestionBusinessRef extends BaseEntity {
    private Integer id;
    @TableField("question_id")
    private Integer questionId;

    @TableField("business_id")
    private Integer businessId;

    @TableField("business_type")
    private Integer businessType;

    @TableField("order_num")
    private Integer orderNum;

}
