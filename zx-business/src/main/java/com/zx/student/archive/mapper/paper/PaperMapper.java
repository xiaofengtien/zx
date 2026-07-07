package com.zx.student.archive.mapper.paper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zx.student.archive.domain.paper.Paper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 试卷Mapper接口
 * 
 * @author zx
 */
@Mapper
public interface PaperMapper extends BaseMapper<Paper> {
    
    /**
     * 根据试卷编码查询试卷
     * 
     * @param paperCode 试卷编码
     * @return 试卷信息
     */
    Paper selectByPaperCode(@Param("paperCode") String paperCode);
    
    /**
     * 根据业务类型和业务ID查询试卷列表
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 试卷列表
     */
    List<Paper> selectByBusiness(@Param("businessType") Integer businessType, @Param("businessId") Integer businessId);
    
    /**
     * 根据年份、月份、省份、试卷类型查询试卷（用于唯一性检查）
     * 
     * @param year 年份
     * @param month 月份
     * @param province 省份编码
     * @param paperType 试卷类型
     * @return 试卷信息（如果存在）
     */
    Paper selectByYearMonthProvinceType(@Param("year") Integer year, 
                                        @Param("month") Integer month,
                                        @Param("province") String province,
                                        @Param("paperType") String paperType);
}

