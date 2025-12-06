package com.ruoyi.student.archive.controller;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.student.archive.domain.AppUserPaperReset;
import com.ruoyi.student.archive.domain.StudentArchive;
import com.ruoyi.student.archive.service.IAppUserPaperResetService;
import com.ruoyi.student.archive.service.IStudentArchiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Date;
import java.util.List;

/**
 * 用户试卷练习次数重置Controller
 */
@RestController
@RequestMapping("/student/paper/reset")
public class AppUserPaperResetController extends BaseController {

    @Autowired
    private IAppUserPaperResetService resetService;

    /**
     * 查询重置记录列表（管理后台）
     */
    @PreAuthorize("@ss.hasPermi('student:paper:reset:list')")
    @GetMapping("/list")
    public TableDataInfo list(AppUserPaperReset query) {
        startPage();
        List<AppUserPaperReset> list = resetService.list(query);
        return getDataTable(list);
    }

    @Autowired
    private IStudentArchiveService archiveService;

    /**
     * 重置用户的试卷练习次数（管理后台）
     */
    @PreAuthorize("@ss.hasPermi('student:paper:reset:add')")
    @PostMapping
    public AjaxResult add(@RequestBody AppUserPaperReset reset) {
        Long userId = reset.getUserId();
        
        // 如果没有userId但有archiveId，通过archiveId查询userId
        if (userId == null && reset.getArchiveId() != null) {
            StudentArchive archive = archiveService.selectStudentArchiveById(reset.getArchiveId());
            if (archive != null) {
                userId = archive.getUserId();
            }
        }
        
        if (userId == null) {
            return AjaxResult.error("用户ID不能为空");
        }
        
        String resetBy = SecurityUtils.getUsername();
        resetService.resetUserPaper(userId, reset.getPaperId(), resetBy, reset.getRemark());
        return AjaxResult.success("重置成功");
    }

    /**
     * 删除重置记录（管理后台）
     */
    @PreAuthorize("@ss.hasPermi('student:paper:reset:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(resetService.delete(id));
    }

    /**
     * 获取当前用户的重置记录（客户端同步用）
     */
    @GetMapping("/sync")
    public AjaxResult sync(@RequestParam(required = false) Long sinceTime) {
        Long userId = SecurityUtils.getUserId();
        Date since = sinceTime != null ? new Date(sinceTime) : null;
        List<AppUserPaperReset> resets = resetService.getUserResets(userId, since);
        return AjaxResult.success(resets);
    }
}
