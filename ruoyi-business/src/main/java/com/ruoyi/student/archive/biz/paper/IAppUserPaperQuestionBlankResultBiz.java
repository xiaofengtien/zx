package com.ruoyi.student.archive.biz.paper;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.domain.bo.paper.CheckBlankAnsweredBO;
import com.ruoyi.student.archive.domain.bo.paper.QueryBlankResultBO;
import com.ruoyi.student.archive.domain.bo.paper.QueryErrorBlankResultsBO;
import com.ruoyi.student.archive.domain.bo.paper.SaveBlankResultBO;
import com.ruoyi.student.archive.domain.dto.paper.BlankResultDTO;
import com.ruoyi.student.archive.domain.paper.AppUserPaperQuestionBlankResult;

import java.util.List;

/**
 * 用户试卷题目空位结果业务接口
 */
public interface IAppUserPaperQuestionBlankResultBiz extends IService<AppUserPaperQuestionBlankResult> {
    
    /**
     * 保存空位作答结果
     *
     * @param saveBO 保存空位结果参数
     * @return 是否成功
     */
    boolean saveBlankResult(SaveBlankResultBO saveBO) throws ServiceException;

    /**
     * 获取空位作答结果列表
     *
     * @param queryBO 查询空位结果参数
     * @return 空位结果列表
     */
    List<BlankResultDTO> getBlankResults(QueryBlankResultBO queryBO) throws ServiceException;

    /**
     * 检查所有空位是否已作答
     *
     * @param checkBO 检查空位作答参数
     * @return 是否全部作答完成
     */
    boolean checkAllBlanksAnswered(CheckBlankAnsweredBO checkBO) throws ServiceException;
    
    /**
     * 批量查询用户错误空位答题结果
     *
     * @param queryBO 查询参数
     * @return 错误空位答题结果列表
     */
    List<AppUserPaperQuestionBlankResult> batchGetErrorResults(QueryErrorBlankResultsBO queryBO);
} 