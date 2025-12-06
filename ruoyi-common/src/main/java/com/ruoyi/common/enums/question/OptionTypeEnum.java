package com.ruoyi.common.enums.question;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum OptionTypeEnum {

    TEXT(1,"文本"),
    PIC(2,"图片"),
    AUDIO(3,"音频"),
    VIDEO(4,"视频"),;

    private Integer code;
    private String desc;
    public boolean isMedia() {
        return Objects.equals(this.code, PIC.getCode())
                || Objects.equals(this.code, AUDIO.getCode())
                || Objects.equals(this.code, VIDEO.getCode());
    }

    public static OptionTypeEnum getByCode(Integer code) {
        for (OptionTypeEnum typeEnum : OptionTypeEnum.values()) {
            if (Objects.equals(typeEnum.getCode(), code)) {
                return typeEnum;
            }
        }
        return null;
    }
}
