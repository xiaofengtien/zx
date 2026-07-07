package com.zx.student.archive.domain.dto.material;

import com.zx.common.utils.TreeVO;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ToString
public class AppMaterialQueryDicTreeDTO extends TreeVO<Integer,AppMaterialQueryDicTreeDTO> implements Serializable {

    /**
     * 是否根节点
     */
    private Integer isRoot;

    /**
     * 素材名称
     */
    private String materialName;

    /**
     * 素材类型
     */
    private Integer materialType;

    /**
     * 素材路径
     */
    private String materialPath;

    /**
     * 素材大小
     */
    private Double materialSize;

    /**
     * 排序号
     */
    private Integer orderNum;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
