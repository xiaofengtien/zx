package com.zx.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 检查空位是否已作答参数
 */
@Data
public class CheckSubmitBO implements Serializable {

    
    private Integer appUserId;

    
    @NotNull(message = "业务ID不能为空")
    private Integer businessId;

    
    @NotNull(message = "业务类型不能为空")
    private Integer businessType;
} 