package com.ruoyi.web.controller.question;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.student.archive.domain.bo.question.*;
import com.ruoyi.student.archive.domain.dto.question.QuestionCategoryDTO;
import com.ruoyi.student.archive.domain.dto.question.QuestionCategoryRefDTO;
import com.ruoyi.student.archive.service.question.QuestionCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题目分类管理Controller
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/question/category")
public class QuestionCategoryController extends BaseController
{
    @Autowired
    private QuestionCategoryService questionCategoryService;

    /**
     * 获取分类详情
     */
    @PreAuthorize("@ss.hasPermi('question:category:query')")
    @PostMapping("/detail")
    public AjaxResult getCategory(@RequestBody QuestionCategoryIdBO apiBO)
    {
        QuestionCategoryDTO category = questionCategoryService.getCategory(apiBO);
        return success(category);
    }

    /**
     * 获取分类列表（根据父分类ID）
     */
    @PreAuthorize("@ss.hasPermi('question:category:list')")
    @PostMapping("/list")
    public AjaxResult getCategoryList(@Validated @RequestBody QuestionCategoryIdBO apiBO)
    {
        QuestionCategoryQueryBO queryBO = new QuestionCategoryQueryBO();
        queryBO.setFatherId(apiBO.getId());
        List<QuestionCategoryDTO> list = questionCategoryService.getCategoryTree(queryBO);
        return success(list);
    }

    /**
     * 获取分类树
     */
    @PreAuthorize("@ss.hasPermi('question:category:list')")
    @PostMapping("/tree")
    public AjaxResult getCategoryTree(@RequestBody QuestionCategoryQueryBO queryBO)
    {
        List<QuestionCategoryDTO> tree = questionCategoryService.getCategoryTree(queryBO);
        return success(tree);
    }

    /**
     * 获取分类引用
     */
    @PreAuthorize("@ss.hasPermi('question:category:query')")
    @PostMapping("/reference")
    public AjaxResult getBusinessRefs(@Validated @RequestBody QuestionCategoryIdQueryBO apiBO)
    {
        List<QuestionCategoryRefDTO> refs = questionCategoryService.getBusinessRefs(apiBO);
        return success(refs);
    }

    /**
     * 新增分类
     */
    @PreAuthorize("@ss.hasPermi('question:category:add')")
    @Log(title = "题目分类", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult createCategory(@Validated @RequestBody QuestionCategoryBO apiBO)
    {
        Integer categoryId = questionCategoryService.createCategory(apiBO);
        return success(categoryId);
    }

    /**
     * 修改分类
     */
    @PreAuthorize("@ss.hasPermi('question:category:edit')")
    @Log(title = "题目分类", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult updateCategory(@Validated @RequestBody QuestionCategoryBO apiBO)
    {
        questionCategoryService.updateCategory(apiBO);
        return success();
    }

    /**
     * 删除分类
     */
    @PreAuthorize("@ss.hasPermi('question:category:remove')")
    @Log(title = "题目分类", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public AjaxResult batchDeleteCategory(@Validated @RequestBody QuestionCategoryIdsBO apiBO)
    {
        questionCategoryService.batchDeleteCategory(apiBO);
        return success();
    }

    /**
     * 更新分类排序
     */
    @PreAuthorize("@ss.hasPermi('question:category:edit')")
    @Log(title = "题目分类", businessType = BusinessType.UPDATE)
    @PostMapping("/sort")
    public AjaxResult updateSort(@Validated @RequestBody QuestionCategorySortBO apiBO)
    {
        questionCategoryService.updateSort(apiBO);
        return success();
    }

    /**
     * 检查分类名称是否重复
     */
    @PreAuthorize("@ss.hasPermi('question:category:query')")
    @PostMapping("/checkName")
    public AjaxResult checkNameExists(@Validated @RequestBody QuestionCategoryBO apiBO)
    {
        boolean exists = questionCategoryService.checkNameExists(apiBO.getName(), apiBO.getFatherId(), apiBO.getId());
        return success(exists);
    }
}

