package com.zx.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum MaterialTypeEnum {

    FILE_TYPE_DIR(1,"文件夹","icon-wenjianjiaicon"),
    FILE_TYPE_PIC(2,"图片","icon-tupianicon"),
    FILE_TYPE_AUDIO(3,"音频","icon-yinpinicon"),
    FILE_TYPE_VIDEO(4,"视频","icon-shipinicon"),
    FILE_TYPE_DOC(5,"文档","icon-pdfwendangicon"),
    FILE_TYPE_PAC(6,"压缩包","icon-yasuobaowendangicon");

    private Integer code;
    private String desc;
    private String icon;

    public static MaterialTypeEnum getByCode(Integer code) {
        for (MaterialTypeEnum typeEnum : MaterialTypeEnum.values()) {
            if (Objects.equals(typeEnum.getCode(), code)) {
                return typeEnum;
            }
        }
        return null;
    }
}
