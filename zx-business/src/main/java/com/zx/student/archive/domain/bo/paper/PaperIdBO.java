package com.zx.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 试卷ID参数
 * 
 * @author zx
 */
@Data
public class PaperIdBO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 试卷ID
     */
    @NotNull(message = "试卷ID不能为空")
    private Integer id;
}



