package com.ruoyi.student.archive.domain.dto.paper;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户试卷信息
 */
@Data
@Builder
public class UserPaperCheckSubmitDTO implements Serializable {


    /**
     * 答题卡ID
     */
    
    private Integer paperId;

    /**
     * 是否提交
     */
    
    private Boolean isSubmit;

}