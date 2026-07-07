package com.zx.student.archive.factory;

import java.util.List;
import java.util.Map;

/**
 * @author xuezhi
 * Copyright (C), 2025, com.dbj
 * FileName: IAppMaterialBusinessStrategy
 * Date:     2025-05-09 13:44:43
 * Description: 表名： ,描述： 表
 */
public interface ICommonBusinessStrategy {
    Map<Integer,String> getBusinessName(List<Integer>businessId);
}
