package com.zx.student.archive.service.question;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zx.common.exception.ServiceException;
import com.zx.student.archive.domain.bo.question.*;
import com.zx.student.archive.domain.dto.question.QuestionBlankContentDTO;
import com.zx.student.archive.domain.dto.question.QuestionInfoDTO;

import java.util.List;

public interface QuestionService {
    /**
     * 获取题目详情
     */
    QuestionInfoDTO getQuestion(QuestionIdBO idBO) throws ServiceException;

    /**
     * 根据分类ID获取题目列表
     */
    List<QuestionInfoDTO> getQuestionList(QuestionCategoryIdQueryBO idBO);

    /**
     * 创建题目
     */
    Integer createQuestion(QuestionBO question) throws ServiceException;

    /**
     * 更新题目
     */
    void updateQuestion(QuestionBO question) throws ServiceException;

    /**
     * 删除题目
     */
    void batchDeleteQuestion(QuestionIdsBO idsBO) throws ServiceException;

    /**
     * 获取题目正确答案
     */
    List<String> getQuestionAnswer(QuestionIdBO idBO);

    /**
     * 获取完形填空题内容
     */
    QuestionBlankContentDTO getQuestionBlankContent(QuestionIdBO idBO) throws ServiceException;

    /**
     * 批量复制题目
     *
     * @param copyBO 复制参数
     */
    void batchCopyQuestion(QuestionCopyBO copyBO) throws ServiceException;

    /**
     * 批量移动题目
     *
     * @param moveBO 移动参数
     */
    void batchMoveQuestion(QuestionMoveBO moveBO);

    /**
     * 分页查询题目列表
     */
    Page<QuestionInfoDTO> pageList(QuestionPageBO pageBO);
} 