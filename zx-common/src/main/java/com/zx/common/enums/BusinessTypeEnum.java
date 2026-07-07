package com.zx.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum BusinessTypeEnum {

    DEFAULT_BUSINESS(5,"默认答题","默认答题",0),
    // 题目业务相关枚举已废弃，题目业务已改用QuestionMedia表
    // QUESTION_MEDIA_BUSINESS(10,"题目-媒体文件","题库",0),
    // ANSWER_MEDIA_BUSINESS(15,"题目-答案","题库",0),
    // QUESTION_RECOGNITION_BUSINESS(20,"题目-辅助识图","题库",0),

    ;

    private int code;
    private String type;
    private String name;
    private int albumType;

    public static BusinessTypeEnum getByCode(Integer code) {
        for (BusinessTypeEnum businessTypeEnum : BusinessTypeEnum.values()) {
            if (businessTypeEnum.getCode() == code) {
                return businessTypeEnum;
            }
        }
        return null;
    }

    /**
      * 检查值是否有效
      *
      * @param value 值
      * @return 是否有效
      */
    public static boolean isValid(Integer value) {
        return getByCode(value) == null;
    }
}
