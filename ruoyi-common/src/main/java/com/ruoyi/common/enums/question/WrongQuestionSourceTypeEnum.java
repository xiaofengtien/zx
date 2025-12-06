package com.ruoyi.common.enums.question;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum WrongQuestionSourceTypeEnum {
    /**
     * 用户练习
     */

    USER_PRACTICE(1, "用户答题练习"),
    /**
     * 用户评测
     */
    USER_EVALUATION(2, "用户评测答题"),

    STUDY_CENTER(3, "学习中心答题"),
    ;

    private Integer code;
    private String desc;

    public static WrongQuestionSourceTypeEnum getByCode(Integer code) {
        for (WrongQuestionSourceTypeEnum typeEnum : WrongQuestionSourceTypeEnum.values()) {
            if (Objects.equals(typeEnum.getCode(), code)) {
                return typeEnum;
            }
        }
        return null;
    }
}
