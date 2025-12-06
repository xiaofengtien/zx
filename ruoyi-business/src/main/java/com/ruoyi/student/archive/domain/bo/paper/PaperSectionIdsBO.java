package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * 试卷大题ID列表参数
 * 
 * @author ruoyi
 */
@Data
public class PaperSectionIdsBO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 大题ID列表
     */
    @NotEmpty(message = "大题ID列表不能为空")
    private List<Integer> ids;
}



