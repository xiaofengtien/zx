package com.zx.web.controller.question;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zx.common.annotation.Log;
import com.zx.common.core.controller.BaseController;
import com.zx.common.core.domain.AjaxResult;
import com.zx.common.core.page.TableDataInfo;
import com.zx.common.enums.BusinessType;
import com.zx.student.archive.domain.bo.question.QuestionBusinessRefPageBO;
import com.zx.student.archive.domain.dto.question.QuestionBusinessRefDTO;
import com.zx.student.archive.service.question.QuestionBusinessRefService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 题目业务关联管理Controller
 * 
 * @author zx
 */
@RestController
@RequestMapping("/question/businessRef")
public class QuestionBusinessRefController extends BaseController
{
    @Autowired
    private QuestionBusinessRefService questionBusinessRefService;

    /**
     * 分页查询题目业务关联列表
     */
    @PreAuthorize("@ss.hasPermi('question:businessRef:list')")
    @PostMapping("/list")
    public TableDataInfo pageList(@Validated @RequestBody QuestionBusinessRefPageBO pageBO)
    {
        Page<QuestionBusinessRefDTO> page = questionBusinessRefService.pageList(pageBO);
        TableDataInfo dataTable = new TableDataInfo();
        dataTable.setCode(200);
        dataTable.setMsg("查询成功");
        dataTable.setRows(page.getRecords());
        dataTable.setTotal(page.getTotal());
        return dataTable;
    }

    /**
     * 查询最大排序号
     */
    @PreAuthorize("@ss.hasPermi('question:businessRef:query')")
    @PostMapping("/maxSortNum")
    public AjaxResult getMaxSortNum(@Validated @RequestBody QuestionBusinessRefPageBO pageBO)
    {
        Integer maxSortNum = questionBusinessRefService.getMaxSortNum(pageBO);
        return success(maxSortNum);
    }
}



