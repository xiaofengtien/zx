package com.ruoyi.student.archive.biz.question;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.domain.bo.question.AddBlankWrongQuestionBO;
import com.ruoyi.student.archive.domain.question.wrongquestion.AppUserWrongQuestionBlank;

import java.util.List;
import java.util.Set;

/**
 * 用户完形填空错题本业务接口
 */
public interface IAppUserWrongQuestionBlankBiz extends IService<AppUserWrongQuestionBlank> {

    /**
     * 批量添加完形填空错题（使用BO对象）
     *
     * @param boList 错题BO信息列表
     */
    void batchAddWrongQuestions(List<AddBlankWrongQuestionBO> boList) throws ServiceException;
    
    /**
     * 删除完形填空错题（软删除）
     *
     * @param appUserId 用户ID
     * @param ids       ID
     * @throws ServiceException 业务异常
     */
    void removeWrongQuestion(Integer appUserId, List<Integer> ids) throws ServiceException;
    
    /**
     * 获取用户完形填空错题列表
     *
     * @param appUserId 用户ID
     * @param subjectId 学科ID（可选）
     * @return 错题列表
     * @throws ServiceException 业务异常
     */
    List<AppUserWrongQuestionBlank> listUserWrongQuestions(Integer appUserId, Integer subjectId) throws ServiceException;
    
    /**
     * 根据题目ID获取所有空位的错题
     *
     * @param appUserId 用户ID
     * @param questionId 题目ID
     * @return 错题列表
     * @throws ServiceException 业务异常
     */
    List<AppUserWrongQuestionBlank> listQuestionBlanks(Integer appUserId, Integer questionId) throws ServiceException;


    List<AppUserWrongQuestionBlank> queryBlankWrongQuestionsWithJoin(Integer appUserId,
                                                                       Integer subjectId,
                                                                       Set<Integer> questionIds,
                                                                       Integer type);
} 