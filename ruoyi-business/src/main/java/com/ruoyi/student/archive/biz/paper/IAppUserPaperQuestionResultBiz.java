package com.ruoyi.student.archive.biz.paper;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.domain.bo.paper.QueryErrorResultsBO;
import com.ruoyi.student.archive.domain.bo.paper.QueryQuestionResultBO;
import com.ruoyi.student.archive.domain.bo.paper.SaveQuestionResultBO;
import com.ruoyi.student.archive.domain.dto.paper.QuestionResultDTO;
import com.ruoyi.student.archive.domain.paper.AppUserPaperQuestionResult;

import java.util.List;

/**
 * 用户试卷题目结果业务接口
 */
public interface IAppUserPaperQuestionResultBiz extends IService<AppUserPaperQuestionResult> {
    
    /**
     * 保存用户答题结果
     *
     * @param saveBO 保存题目结果参数
     * @return 是否成功
     */
    boolean saveQuestionResult(SaveQuestionResultBO saveBO) throws ServiceException;

    /**
     * 批量获取用户答题结果
     *
     * @param queryBO 查询题目结果参数
     * @return 答题结果列表
     */
    List<QuestionResultDTO> getQuestionResults(QueryQuestionResultBO queryBO) throws ServiceException;
    
    /**
     * 批量查询用户错误答题结果
     *
     * @param queryBO 查询参数
     * @return 错误答题结果列表
     */
    List<AppUserPaperQuestionResult> batchGetErrorResults(QueryErrorResultsBO queryBO);
} 