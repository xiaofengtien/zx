package com.zx.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * 试卷中场配置ID列表参数
 * 
 * @author zx
 */
@Data
public class PaperIntermissionIdsBO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 中场配置ID列表
     */
    @NotEmpty(message = "中场配置ID列表不能为空")
    private List<Integer> ids;
}



