package com.zx.student.archive.domain.question;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zx.common.core.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * 题目实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("question")
@AllArgsConstructor
@NoArgsConstructor
public class Question extends BaseEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Integer id;
    /**
     * 分类id
     */
    private Integer questionCategoryId;

    /**
     * 题目
     */
    private String title;

    /**
     * 媒体文件类型 1-文本 2-图片,3-音频,4-视频
     */
    private Integer mediaType;
    /**
     * 学科ID
     */
    private Integer subjectId;

    /**
     * 题目类型 0-单选题 1-多选题 2-判断题 3-填空题 4-排序题，5-完形填空
     */
    private Integer type;

    /**
     * 选项类型 1-文本 2-图片,3-音频,4-视频
     */
    private Integer optionType;

    /**
     * 权重
     */
    private Integer weight;

    /**
     * 选择题答案id（多个英文逗号隔开）
     */
    private String answer;

    /**
     * 解析
     */
    private String analyzes;

    /**
     * 是否有答题讲解（0-否，1-是）
     */
    private Integer explanationEnabled;

    /**
     * 讲解文字
     */
    private String explanationText;

    /**
     * 讲解显示延迟（秒，播放完试卷名称音频后等待时间，默认2秒）
     */
    private Integer explanationDelaySeconds;

    /**
     * 是否启用 1-是 0-否
     */
    private Integer status;

    /**
     * 答案列表
     */
    @TableField(exist = false)
    private List<QuestionAnswer> answers;

    /**
     * 完形填空区域列表
     */
    @TableField(exist = false)
    private List<QuestionBlankArea> blankAreas;

    // ========== 作文题字段 ==========

    /**
     * 词数限制（0表示不限制）
     */
    private Integer wordLimit;

    /**
     * 写作要求（JSON数组格式存储）
     */
    private String requirements;

    /**
     * 参考范文
     */
    private String sampleAnswer;
}