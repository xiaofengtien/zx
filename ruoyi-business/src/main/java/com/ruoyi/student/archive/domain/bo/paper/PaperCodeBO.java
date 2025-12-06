package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 试卷编码参数
 * 
 * @author ruoyi
 */
@Data
public class PaperCodeBO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 试卷编码
     */
    @NotBlank(message = "试卷编码不能为空")
    private String paperCode;
}



