package com.ruoyi.student.archive.factory;

import com.ruoyi.student.archive.domain.dto.BusinessInfoDTO;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author xuezhi
 * Copyright (C), 2025, com.dbj
 * FileName: IAppMaterialBusinessStrategy
 * Date:     2025-05-09 13:04:54
 * Description: 表名： ,描述： 表
 */
@Service
public abstract class ICommonBusinessStrategyHandler implements ICommonBusinessStrategy {

    public Map<Integer, BusinessInfoDTO> getBusinessInfoMap(List<Integer> businessIds) {return Collections.emptyMap();}
}
