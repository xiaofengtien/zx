package com.zx.web.controller.question;

import com.zx.common.annotation.Log;
import com.zx.common.core.controller.BaseController;
import com.zx.common.core.domain.AjaxResult;
import com.zx.common.enums.BusinessType;
import com.zx.student.archive.domain.bo.question.*;
import com.zx.student.archive.domain.dto.question.QuestionCorrectAnswerDTO;
import com.zx.student.archive.domain.dto.question.WrongQuestionResultDTO;
import com.zx.student.archive.domain.dto.question.WrongQuestionSubjectDTO;
import com.zx.student.archive.service.question.WrongQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 错题本管理Controller
 * 
 * @author zx
 */
@RestController
@RequestMapping("/question/wrong")
public class WrongQuestionController extends BaseController
{
    @Autowired
    private WrongQuestionService wrongQuestionService;

    /**
     * 添加错题
     */
    @PreAuthorize("@ss.hasPermi('question:wrong:add')")
    @Log(title = "错题本", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult addWrongQuestion(@Validated @RequestBody AddWrongQuestionBO addWrongQuestionBO)
    {
        boolean result = wrongQuestionService.addWrongQuestion(addWrongQuestionBO);
        return toAjax(result ? 1 : 0);
    }

    /**
     * 批量添加错题
     */
    @PreAuthorize("@ss.hasPermi('question:wrong:add')")
    @Log(title = "错题本", businessType = BusinessType.INSERT)
    @PostMapping("/batchAdd")
    public AjaxResult batchAddWrongQuestions(@Validated @RequestBody List<AddWrongQuestionBO> addWrongQuestionBOList)
    {
        boolean result = wrongQuestionService.batchAddWrongQuestions(addWrongQuestionBOList);
        return toAjax(result ? 1 : 0);
    }

    /**
     * 删除错题
     */
    @PreAuthorize("@ss.hasPermi('question:wrong:remove')")
    @Log(title = "错题本", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public AjaxResult removeWrongQuestion(@Validated @RequestBody WrongQuestionPkIdsBO removeBO)
    {
        boolean result = wrongQuestionService.removeWrongQuestion(removeBO);
        return toAjax(result ? 1 : 0);
    }

    /**
     * 查询用户错题列表（统一接口，返回普通题和完形填空题）
     */
    @PreAuthorize("@ss.hasPermi('question:wrong:list')")
    @PostMapping("/list")
    public AjaxResult listWrongQuestions(@Validated @RequestBody QueryWrongQuestionsBO queryBO)
    {
        List<WrongQuestionResultDTO> list = wrongQuestionService.listWrongQuestions(queryBO);
        return success(list);
    }

    /**
     * 查询错题本中题目的正确答案
     */
    @PreAuthorize("@ss.hasPermi('question:wrong:query')")
    @PostMapping("/correctAnswers")
    public AjaxResult getWrongQuestionsCorrectAnswers(@RequestBody WrongQuestionIdsBO idsBO)
    {
        List<QuestionCorrectAnswerDTO> answers = wrongQuestionService.getWrongQuestionsCorrectAnswers(
            idsBO.getAppUserId(), idsBO.getQuestionIds());
        return success(answers);
    }

    /**
     * 查询错题本中题目的学科
     */
    @PreAuthorize("@ss.hasPermi('question:wrong:list')")
    @PostMapping("/subjects")
    public AjaxResult listSubject(@RequestParam Integer appUserId)
    {
        List<WrongQuestionSubjectDTO> subjects = wrongQuestionService.listSubject(appUserId);
        return success(subjects);
    }
}



