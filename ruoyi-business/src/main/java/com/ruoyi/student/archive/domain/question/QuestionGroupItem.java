package com.ruoyi.student.archive.domain.question;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 题目组与题目关联实体
 * 
 * @author ruoyi
 */
@Data
@TableName("question_group_item")
public class QuestionGroupItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 题目组ID
     */
    private Integer groupId;

    /**
     * 题目ID
     */
    private Integer questionId;

    /**
     * 组内排序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    private Date createTime;
}
