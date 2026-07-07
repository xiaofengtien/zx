package com.zx.web.controller.question;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zx.common.annotation.Log;
import com.zx.common.core.controller.BaseController;
import com.zx.common.core.domain.AjaxResult;
import com.zx.common.core.page.TableDataInfo;
import com.zx.common.enums.BusinessType;
import com.zx.common.utils.StringUtils;
import com.zx.student.archive.domain.bo.question.*;
import com.zx.student.archive.domain.dto.question.QuestionBlankContentDTO;
import com.zx.student.archive.domain.dto.question.QuestionInfoDTO;
import com.zx.student.archive.service.question.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题目管理Controller
 * 
 * @author zx
 */
@RestController
@RequestMapping("/question")
public class QuestionController extends BaseController
{
    @Autowired
    private QuestionService questionService;

    /**
     * 分页查询题目列表
     */
    @PreAuthorize("@ss.hasPermi('question:question:list')")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody QuestionPageBO pageBO)
    {
        Page<QuestionInfoDTO> page = questionService.pageList(pageBO);
        TableDataInfo dataTable = new TableDataInfo();
        dataTable.setCode(200);
        dataTable.setMsg("查询成功");
        dataTable.setRows(page.getRecords());
        dataTable.setTotal(page.getTotal());
        return dataTable;
    }

    /**
     * 获取题目详情
     */
    @PreAuthorize("@ss.hasPermi('question:question:query')")
    @PostMapping("/detail")
    public AjaxResult getQuestion(@Validated @RequestBody QuestionIdBO idBO)
    {
        QuestionInfoDTO question = questionService.getQuestion(idBO);
        return success(question);
    }

    /**
     * 根据分类ID获取题目列表
     */
    @PreAuthorize("@ss.hasPermi('question:question:category:list')")
    @PostMapping("/by/category/list")
    public AjaxResult getQuestionList(@Validated @RequestBody QuestionCategoryIdQueryBO idBO)
    {
        List<QuestionInfoDTO> list = questionService.getQuestionList(idBO);
        return success(list);
    }

    /**
     * 新增题目
     */
    @PreAuthorize("@ss.hasPermi('question:question:add')")
    @Log(title = "题目管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult createQuestion(@Validated @RequestBody QuestionBO question)
    {
        Integer questionId = questionService.createQuestion(question);
        return success(questionId);
    }

    /**
     * 修改题目
     */
    @PreAuthorize("@ss.hasPermi('question:question:edit')")
    @Log(title = "题目管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult updateQuestion(@Validated @RequestBody QuestionBO question)
    {
        questionService.updateQuestion(question);
        return success();
    }

    /**
     * 删除题目
     */
    @PreAuthorize("@ss.hasPermi('question:question:remove')")
    @Log(title = "题目管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public AjaxResult batchDeleteQuestion(@Validated @RequestBody QuestionIdsBO idsBO)
    {
        questionService.batchDeleteQuestion(idsBO);
        return success();
    }

    /**
     * 获取题目正确答案
     */
    @PreAuthorize("@ss.hasPermi('question:question:query')")
    @PostMapping("/answer")
    public AjaxResult getQuestionAnswer(@Validated @RequestBody QuestionIdBO idBO)
    {
        List<String> answers = questionService.getQuestionAnswer(idBO);
        return success(answers);
    }

    /**
     * 获取完形填空题内容
     */
    @PreAuthorize("@ss.hasPermi('question:question:query')")
    @PostMapping("/blank-content")
    public AjaxResult getQuestionBlankContent(@Validated @RequestBody QuestionIdBO idBO)
    {
        QuestionBlankContentDTO content = questionService.getQuestionBlankContent(idBO);
        return success(content);
    }

    /**
     * 批量复制题目
     */
    @PreAuthorize("@ss.hasPermi('question:question:edit')")
    @Log(title = "题目管理", businessType = BusinessType.UPDATE)
    @PostMapping("/copy")
    public AjaxResult batchCopyQuestion(@Validated @RequestBody QuestionCopyBO copyBO)
    {
        questionService.batchCopyQuestion(copyBO);
        return success();
    }

    /**
     * 批量移动题目
     */
    @PreAuthorize("@ss.hasPermi('question:question:edit')")
    @Log(title = "题目管理", businessType = BusinessType.UPDATE)
    @PostMapping("/move")
    public AjaxResult batchMoveQuestion(@Validated @RequestBody QuestionMoveBO moveBO)
    {
        questionService.batchMoveQuestion(moveBO);
        return success();
    }
}



