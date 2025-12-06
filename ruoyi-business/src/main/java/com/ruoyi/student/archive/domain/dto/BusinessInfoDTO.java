package com.ruoyi.student.archive.domain.dto;

import lombok.*;

import java.io.Serializable;
import java.util.Map;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class BusinessInfoDTO implements Serializable {

    private String businessName;

    private Map<String, Object> other;
}
