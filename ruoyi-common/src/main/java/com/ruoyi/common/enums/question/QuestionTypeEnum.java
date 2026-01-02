package com.ruoyi.common.enums.question;

import lombok.Getter;

/**
 * 题目类型枚举
 */
@Getter
public enum QuestionTypeEnum {
    SINGLE_CHOICE(0, "单选题", 1),
    MULTIPLE_CHOICE(1, "多选题", 2),
    TRUE_FALSE(2, "判断题", 3),
    FILL_BLANK(3, "填空题", 4),
    SORT(4, "排序题", 5),
    CLOZE(5, "完形填空", 6),
    ESSAY(6, "作文/简答", 7),
    READING_COMPREHENSION(7, "阅读理解", 8);

    /**
     * 类型值
     */
    private final Integer value;

    /**
     * 类型描述
     */
    private final String desc;

    /**
     * 类型序号
     */
    private final Integer order;

    QuestionTypeEnum(int value, String desc, int order) {
        this.value = value;
        this.desc = desc;
        this.order = order;
    }

    public static boolean displayOptionContent(Integer questionType) {
        return QuestionTypeEnum.FILL_BLANK.getValue().equals(questionType)
                || QuestionTypeEnum.SORT.getValue().equals(questionType);
    }

    public static boolean displayOptionName(Integer questionType) {
        return QuestionTypeEnum.SINGLE_CHOICE.getValue().equals(questionType)
                || QuestionTypeEnum.MULTIPLE_CHOICE.getValue().equals(questionType)
                || QuestionTypeEnum.TRUE_FALSE.getValue().equals(questionType)
                || QuestionTypeEnum.CLOZE.getValue().equals(questionType);
    }

    /**
     * 根据类型值获取枚举
     *
     * @param value 类型值
     * @return 对应的枚举值，如果未找到则返回null
     */
    public static QuestionTypeEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (QuestionTypeEnum type : values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 判断是否为有效的题目类型值
     *
     * @param value 类型值
     * @return true: 有效, false: 无效
     */
    public static boolean isValid(Integer value) {
        return getByValue(value) != null;
    }

    /**
     * 获取题目类型的序号
     *
     * @param value 类型值
     * @return 对应的序号
     */
    public static Integer getOrderByValue(Integer value) {
        QuestionTypeEnum type = getByValue(value);
        return type != null ? type.getOrder() : 0;
    }
}