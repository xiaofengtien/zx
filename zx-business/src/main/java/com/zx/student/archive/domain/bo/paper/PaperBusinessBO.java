package com.zx.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 试卷业务查询参数
 * 
 * @author zx
 */
@Data
public class PaperBusinessBO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 业务类型
     */
    @NotNull(message = "业务类型不能为空")
    private Integer businessType;

    /**
     * 业务ID
     */
    @NotNull(message = "业务ID不能为空")
    private Integer businessId;
}



