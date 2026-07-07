package com.zx.student.archive.biz.paper;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zx.common.exception.ServiceException;
import com.zx.student.archive.domain.paper.Paper;

import java.util.List;

/**
 * 试卷业务接口
 * 
 * @author zx
 */
public interface IPaperBiz extends IService<Paper> {
    
    /**
     * 根据试卷编码查询试卷
     * 
     * @param paperCode 试卷编码
     * @return 试卷信息
     */
    Paper getByPaperCode(String paperCode);
    
    /**
     * 根据年份、月份、省份、试卷类型查询试卷（用于唯一性检查）
     * 
     * @param year 年份
     * @param month 月份
     * @param province 省份编码
     * @param paperType 试卷类型
     * @return 试卷信息（如果存在）
     */
    Paper getByYearMonthProvinceType(Integer year, Integer month, String province, String paperType);
    
    /**
     * 根据业务类型和业务ID查询试卷列表
     * 
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @return 试卷列表
     */
    List<Paper> listByBusiness(Integer businessType, Integer businessId);
    
    /**
     * 创建试卷
     * 
     * @param paper 试卷实体
     * @return 新创建的试卷ID
     * @throws ServiceException 业务异常
     */
    Integer createPaper(Paper paper) throws ServiceException;
    
    /**
     * 更新试卷
     * 
     * @param paper 试卷实体
     * @throws ServiceException 业务异常
     */
    void updatePaper(Paper paper) throws ServiceException;
    
    /**
     * 批量删除试卷（逻辑删除）
     * 
     * @param ids 试卷ID列表
     * @throws ServiceException 业务异常
     */
    void batchDeletePaper(List<Integer> ids) throws ServiceException;
    
    /**
     * 生成试卷编码
     * 
     * @return 试卷编码（格式：PAPER_YYYYMMDD_序号）
     */
    String generatePaperCode();
}

