package com.zx.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * 试卷卷别ID列表参数
 * 
 * @author zx
 */
@Data
public class PaperVolumeIdsBO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 卷别ID列表
     */
    @NotEmpty(message = "卷别ID列表不能为空")
    private List<Integer> ids;
}



