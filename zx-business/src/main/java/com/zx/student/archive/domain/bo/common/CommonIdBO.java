package com.zx.student.archive.domain.bo.common;

import lombok.*;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.io.Serializable;


@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString
public class CommonIdBO implements Serializable {
    @NotNull(message = "主键ID不能为空")
    private Integer id;
}
