package com.zx.common.enums.question;

import lombok.Getter;

/**
 * 出题方式
 */
@Getter
public enum QuestionMethodEnum {
    /**
     *  随机出题
     */

    RANDOM(0, "随机出题"),
    /**
     * 顺序
     */
    ORDER(1, "顺序"),;

    /**
     * 类型值
     */
    private final Integer value;

    /**
     * 类型描述
     */
    private final String desc;

    QuestionMethodEnum(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    /**
     * 根据类型值获取枚举
     *
     * @param value 类型值
     * @return 对应的枚举值，如果未找到则返回null
     */
    public static QuestionMethodEnum getByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (QuestionMethodEnum type : values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return null;
    }

} 