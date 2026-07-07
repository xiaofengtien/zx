package com.zx.web.controller.paper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zx.common.annotation.Log;
import com.zx.common.core.controller.BaseController;
import com.zx.common.core.domain.AjaxResult;
import com.zx.common.core.page.TableDataInfo;
import com.zx.common.enums.BusinessType;
import com.zx.common.utils.poi.ExcelUtil;
import com.zx.student.archive.domain.bo.paper.PaperIdBO;
import com.zx.student.archive.domain.bo.paper.PaperResultPageBO;
import com.zx.student.archive.domain.dto.paper.PaperQuestionResultDTO;
import com.zx.student.archive.domain.dto.paper.PaperResultListDTO;
import com.zx.student.archive.service.paper.IPaperResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 答题结果查看Controller
 * 
 * @author zx
 */
@Slf4j
@RestController
@RequestMapping("/paper/result")
public class PaperResultController extends BaseController
{
    @Autowired
    private IPaperResultService paperResultService;

    /**
     * 分页查询答题结果列表
     */
    @PreAuthorize("@ss.hasPermi('paper:result:list')")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody PaperResultPageBO pageBO)
    {
        // 转换分页参数（前端使用pageNum/pageSize，后端使用current/size）
        if (pageBO.getCurrent() == null && pageBO.getSize() == null) {
            // 如果前端传的是pageNum和pageSize，需要转换
            // 这里假设前端会传pageNum和pageSize，但BO中使用的是current和size
            // 需要检查前端实际传的参数名
        }
        Page<PaperResultListDTO> page = paperResultService.pageList(pageBO);
        TableDataInfo dataTable = new TableDataInfo();
        dataTable.setCode(200);
        dataTable.setMsg("查询成功");
        dataTable.setRows(page.getRecords());
        dataTable.setTotal(page.getTotal());
        return dataTable;
    }

    /**
     * 获取答题结果详情
     */
    @PreAuthorize("@ss.hasPermi('paper:result:query')")
    @PostMapping("/detail")
    public AjaxResult getPaperResult(@Validated @RequestBody PaperIdBO idBO)
    {
        PaperQuestionResultDTO result = paperResultService.getPaperResultDetail(idBO.getId());
        return success(result);
    }

    /**
     * 导出答题结果
     */
    @PreAuthorize("@ss.hasPermi('paper:result:export')")
    @Log(title = "答题结果", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(@RequestBody PaperResultPageBO pageBO, HttpServletResponse response) throws IOException
    {
        try {
            // 查询所有符合条件的数据（不分页）
            // 设置一个很大的分页大小，获取所有数据
            pageBO.setPageNum(1);
            pageBO.setPageSize(Integer.MAX_VALUE);
            
            Page<PaperResultListDTO> page = paperResultService.pageList(pageBO);
            List<PaperResultListDTO> list = page.getRecords();
            
            // 使用ExcelUtil导出
            ExcelUtil<PaperResultListDTO> util = new ExcelUtil<>(PaperResultListDTO.class);
            util.exportExcel(response, list, "答题结果", "答题结果数据");
            
            log.info("答题结果导出成功，共{}条记录", list.size());
        } catch (Exception e) {
            log.error("导出答题结果失败", e);
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.getWriter().write("{\"code\":500,\"msg\":\"导出失败：" + e.getMessage() + "\"}");
        }
    }
}

