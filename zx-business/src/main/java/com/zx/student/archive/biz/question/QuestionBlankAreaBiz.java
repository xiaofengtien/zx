package com.zx.student.archive.biz.question;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zx.common.exception.ServiceException;
import com.zx.student.archive.domain.question.QuestionBlankArea;

import java.util.List;

public interface QuestionBlankAreaBiz extends IService<QuestionBlankArea> {
    
    /**
     * 获取题目的空位区域列表
     */
    List<QuestionBlankArea> getBlankAreasByQuestionId(Integer questionId);

    /**
     * 批量保存空位区域
     */
    void saveBlankAreas(List<QuestionBlankArea> blankAreas);

    /**
     * 更新题目的空位区域
     */
    void updateBlankAreas(Integer questionId, List<QuestionBlankArea> blankAreas);

    /**
     * 删除题目的空位区域
     */
    void deleteByQuestionId(List<Integer> questionIds);

    List<QuestionBlankArea> getBlankAreas(Integer questionId) throws ServiceException;
}