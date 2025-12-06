package com.ruoyi.common.enums.question;

import lombok.Getter;

import java.util.Objects;

@Getter
public enum QuestionAnswerResultEnum {
    /**
     *  错误
     */

    ANSWER_WRONG(0, "错误"),
    /**
     * 正确
     */
    ANSWER_CORRECT(1, "正确"),;

    /**
     * 类型值
     */
    private final Integer value;

    /**
     * 类型描述
     */
    private final String desc;

    QuestionAnswerResultEnum(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
    /**
     * 根据类型值获取枚举
     *
     * @param value 类型值
     * @return 对应的枚举值，如果未找到则返回null
     */
    public static QuestionAnswerResultEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (QuestionAnswerResultEnum type : values()) {
            if (Objects.equals(type.getValue(), value)) {
                return type;
            }
        }
        return null;
    }
}
