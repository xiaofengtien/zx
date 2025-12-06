package com.ruoyi.student.archive.domain.question;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("question_answer")
@AllArgsConstructor
@NoArgsConstructor
public class QuestionAnswer extends BaseEntity implements Serializable {

    private Integer id;

    /**
     * 题目id
     */
    private Integer questionId;
    /**
     * 完形填空区域id
     */
    private Integer blankAreaId;

    /**
     * 序号
     */
    private Integer serialNo;

    /**
     * 选项名称
     */
    private String optionName;

    /**
     * 选项内容
     */
    private String optionContent;

    /**
     * 是否答案（0-否 1-是）
     */
    private Integer isAnswer;

    /**
     * 是否启用 1-是 0-否
     */
    private Integer status;
} 