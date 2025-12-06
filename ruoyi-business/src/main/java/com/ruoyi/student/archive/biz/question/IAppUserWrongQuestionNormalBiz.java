package com.ruoyi.student.archive.biz.question;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.domain.bo.question.AddNormalWrongQuestionBO;
import com.ruoyi.student.archive.domain.question.wrongquestion.AppUserWrongQuestionNormal;

import java.util.List;
import java.util.Set;

/**
 * 用户普通题错题本业务接口
 */
public interface IAppUserWrongQuestionNormalBiz extends IService<AppUserWrongQuestionNormal> {

    /**
     * 批量添加错题（使用BO对象）
     *
     * @param boList 错题BO信息列表
     */
    void batchAddWrongQuestions(List<AddNormalWrongQuestionBO> boList) throws ServiceException;
    
    /**
     * 删除错题（软删除）
     *
     * @param appUserId   用户ID
     * @param ids 题目ID
     * @throws ServiceException 业务异常
     */
    void removeWrongQuestion(Integer appUserId, List<Integer> ids) throws ServiceException;
    
    /**
     * 获取用户错题列表
     *
     * @param appUserId 用户ID
     * @param subjectId 学科ID（可选）
     * @return 错题列表
     * @throws ServiceException 业务异常
     */
    List<AppUserWrongQuestionNormal> listUserWrongQuestions(Integer appUserId, Integer subjectId) throws ServiceException;

    List<AppUserWrongQuestionNormal> queryNormalWrongQuestionsWithJoin(Integer appUserId,
                                                                       Integer subjectId,
                                                                       Set<Integer> questionIds,
                                                                       Integer type);
}