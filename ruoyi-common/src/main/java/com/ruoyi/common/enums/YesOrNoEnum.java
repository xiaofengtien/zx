package com.ruoyi.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 是/否枚举
 * 
 * @author ruoyi
 */
@Getter
@AllArgsConstructor
public enum YesOrNoEnum {
    
    /**
     * 是
     */
    YES(2, "是"),
    
    /**
     * 否
     */
    NO(0, "否");
    
    /**
     * 编码
     */
    private final Integer code;
    
    /**
     * 描述
     */
    private final String desc;
}



