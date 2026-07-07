package com.zx.student.archive.domain.question;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zx.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("question_blank_area")
@AllArgsConstructor
@NoArgsConstructor
public class QuestionBlankArea extends BaseEntity implements Serializable {

    private Integer id;
    /**
     * 题目id
     */
    private Integer questionId;

    /**
     * 空位序号
     */
    private Integer blankIndex;
    /**
     * 正确答案ID（多个英文逗号隔开）
     */
    private String answerIds;

    @TableField(exist = false)
    private List<QuestionAnswer> answers;
    /**
     * 是否启用 1-是 0-否
     */
    private Integer status;
} 