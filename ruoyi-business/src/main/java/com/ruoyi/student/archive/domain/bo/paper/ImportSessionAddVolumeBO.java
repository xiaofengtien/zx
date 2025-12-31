package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;

/**
 * 添加卷别到导入会话的请求参数
 *
 * @author ruoyi
 */
@Data
public class ImportSessionAddVolumeBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话标识
     */
    @NotBlank(message = "会话标识不能为空")
    private String sessionKey;

    /**
     * 卷别名称（如"第I卷"）
     */
    @NotBlank(message = "卷别名称不能为空")
    private String volumeName;

    /**
     * 卷别顺序（1, 2, 3...）
     */
    @NotNull(message = "卷别顺序不能为空")
    private Integer volumeOrder;

    /**
     * 解析结果（Python服务返回的完整结果）
     */
    @NotNull(message = "解析结果不能为空")
    private Map<String, Object> parseResult;
}
