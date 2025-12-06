package com.ruoyi.student.archive.domain.bo.common;

import lombok.*;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString
public class CommonIdsBO implements Serializable {
    @NotEmpty(message = "主键ID列表不能为空")
    private List<Integer> ids;
}
