package com.zx.student.archive.service.paper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zx.common.exception.ServiceException;
import com.zx.student.archive.domain.bo.paper.*;
import com.zx.student.archive.domain.dto.paper.PaperDTO;

import java.util.List;

/**
 * 试卷服务接口
 * 
 * @author zx
 */
public interface IPaperService {

    /**
     * 分页查询试卷列表
     * 
     * @param pageBO 分页查询参数
     * @return 分页结果
     */
    Page<PaperDTO> pageList(PaperPageBO pageBO);

    /**
     * 根据ID获取试卷详情
     * 
     * @param id 试卷ID
     * @return 试卷详情
     * @throws ServiceException 业务异常
     */
    PaperDTO getPaperById(Integer id) throws ServiceException;

    /**
     * 根据试卷编码获取试卷详情
     * 
     * @param paperCode 试卷编码
     * @return 试卷详情
     * @throws ServiceException 业务异常
     */
    PaperDTO getPaperByCode(String paperCode) throws ServiceException;

    /**
     * 创建试卷
     * 
     * @param createBO 创建参数
     * @return 新创建的试卷ID
     * @throws ServiceException 业务异常
     */
    Integer createPaper(PaperCreateBO createBO) throws ServiceException;

    /**
     * 更新试卷
     * 
     * @param updateBO 更新参数
     * @throws ServiceException 业务异常
     */
    void updatePaper(PaperUpdateBO updateBO) throws ServiceException;

    /**
     * 批量删除试卷（逻辑删除）
     * 
     * @param ids 试卷ID列表
     * @throws ServiceException 业务异常
     */
    void batchDeletePaper(List<Integer> ids) throws ServiceException;

    /**
     * 根据业务类型和业务ID查询试卷列表
     * 
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @return 试卷列表
     */
    List<PaperDTO> listByBusiness(Integer businessType, Integer businessId);

    /**
     * 根据试卷ID列表查询试卷列表
     * 
     * @param ids 试卷ID列表
     * @return 试卷列表
     */
    List<PaperDTO> listByIds(List<Integer> ids);

    /**
     * 生成试卷包
     * 
     * @param paperId 试卷ID
     * @throws ServiceException 业务异常
     */
    void generatePaperPackage(Integer paperId) throws ServiceException;

    /**
     * 下载快速启动包（包含manifest.json、trial_listen/、intro/等）
     * 
     * @param paperId 试卷ID
     * @return 快速启动包字节数组
     * @throws ServiceException 业务异常
     */
    byte[] downloadQuickStartPackage(Integer paperId) throws ServiceException;

    /**
     * 下载试卷包
     * 
     * @param paperId 试卷ID
     * @return ZIP包字节数组
     * @throws ServiceException 业务异常
     */
    byte[] downloadPaperPackage(Integer paperId) throws ServiceException;

    /**
     * 创建试卷（包含完整数据：试卷+卷别+大题+题目+中场配置）
     * 
     * @param fullDataBO 完整数据
     * @return 新创建的试卷ID
     * @throws ServiceException 业务异常
     */
    Integer createPaperWithFullData(PaperFullDataBO fullDataBO) throws ServiceException;

    /**
     * 更新试卷（删除后新增：先删除旧的卷别/大题/题目，再创建新数据）
     * 
     * @param fullDataBO 完整数据
     * @throws ServiceException 业务异常
     */
    void updatePaperWithFullData(PaperFullDataBO fullDataBO) throws ServiceException;

    /**
     * 获取试卷完整数据（包含卷别、大题、题目、中场配置）
     * 
     * @param paperId 试卷ID
     * @return 完整数据DTO
     * @throws ServiceException 业务异常
     */
    PaperDTO getPaperFullData(Integer paperId) throws ServiceException;
}
