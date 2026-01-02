package com.ruoyi.student.archive.domain.paper;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 试卷题目关联实体
 * 
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("paper_question")
public class PaperQuestion extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 试卷ID
     */
    private Integer paperId;

    /**
     * 题目ID
     */
    private Integer questionId;

    /**
     * 所属大题ID（关联paper_section.id）
     */
    private Integer sectionId;

    /**
     * 在大题中的顺序（可拖拽调整，后台配置时确定）
     */
    private Integer sectionOrder;

    /**
     * 题目组ID（关联paper_question_group.id，NULL表示单题）
     */
    private Integer groupId;

    /**
     * 排序号（题目在试卷中的顺序，保留用于兼容）
     */
    private Integer sortOrder;

    /**
     * 分值
     */
    private BigDecimal score;

}
