package com.zx.student.archive.domain.dto.paper;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 导入会话状态DTO
 *
 * @author zx
 */
@Data
public class ImportSessionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话标识
     */
    private String sessionKey;

    /**
     * 已添加的卷别数量
     */
    private Integer volumeCount;

    /**
     * 已添加的卷别列表
     */
    private List<VolumeInfo> volumes;

    /**
     * 会话状态：0-进行中，1-已完成，2-已过期
     */
    private Integer status;

    /**
     * 卷别简要信息
     */
    @Data
    public static class VolumeInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 卷别名称
         */
        private String volumeName;

        /**
         * 卷别顺序
         */
        private Integer volumeOrder;

        /**
         * 题目数量
         */
        private Integer questionCount;

        /**
         * 大题数量
         */
        private Integer sectionCount;
    }
}
