package com.zx.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Getter
@AllArgsConstructor
public enum BasicConfigBizTypeEnum {
    QUESTION_BANK("question", "题库",""),
    STUDY_MODULE("study-center", "学习模块","basicConfigStudyCenterRefBizStrategy"),
    SUBJECT("subject","学科","basicConfigSubjectRefBizStrategy"),
    EBOOK_SUBJECT("ebook-subject","样书学科","basicConfigEbookSubjectRefBizStrategy"),
    EBOOK_STAGE("ebook-stage","样书阶段","basicConfigEbookStageRefBizStrategy"),
    EBOOK_TEXTBOOK("ebook-textbook","样书教材","basicConfigEbookTextBookRefBizStrategy"),
    EBOOK_GRADES ("ebook-grades","样书年级","basicConfigEbookGradesRefBizStrategy"),
    POINT_READING_MODE("point-reading-mode","点读模式","basicConfigPointReadingModeRefBizStrategy")
    ,;

    private final String code;
    private final String desc;
    private final String strategy;

    public static BasicConfigBizTypeEnum fromCode(String code) {
        if (StringUtils.isEmpty(code)){
            return null;
        }
        for (BasicConfigBizTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
} 