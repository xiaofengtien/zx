package com.zx.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 删除错题参数
 */
@Data
public class WrongQuestionPkIdsBO implements Serializable {


    /**
     * 用户id
     */
    private Integer appUserId;

    /**
     * 错误问题主键ID列表
     */
    private List<Integer> ids;

} 