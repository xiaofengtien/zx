package com.zx.web.controller.student;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.zx.common.annotation.Log;
import com.zx.common.core.controller.BaseController;
import com.zx.common.core.domain.AjaxResult;
import com.zx.common.core.page.TableDataInfo;
import com.zx.common.enums.BusinessType;
import com.zx.student.archive.domain.StudentArchive;
import com.zx.student.archive.domain.dto.ArchiveIdDTO;
import com.zx.student.archive.domain.dto.ArchiveBatchIdsDTO;
import com.zx.student.archive.domain.dto.ResetPasswordDTO;
import com.zx.student.archive.domain.dto.ChangePasswordDTO;
import com.zx.student.archive.domain.dto.ForgotPasswordDTO;
import com.zx.student.archive.service.IStudentArchiveService;

/**
 * 学员档案操作处理
 * 
 * @author zx
 */
@RestController
@RequestMapping("/student/archive")
public class StudentArchiveController extends BaseController
{
    @Autowired
    private IStudentArchiveService studentArchiveService;

    /**
     * 查询学员档案列表
     */
    @PreAuthorize("@ss.hasPermi('system:archive:list')")
    @PostMapping("/listArchive")
    public TableDataInfo listArchive(@RequestBody StudentArchive studentArchive)
    {
        startPage();
        List<StudentArchive> list = studentArchiveService.selectStudentArchiveList(studentArchive);
        return getDataTable(list);
    }

    /**
     * 获取学员档案详细信息（单个）
     */
    @PreAuthorize("@ss.hasPermi('system:archive:query')")
    @PostMapping("/getArchive")
    public AjaxResult getArchive(@RequestBody ArchiveIdDTO dto)
    {
        if (dto.getArchiveId() == null)
        {
            return error("学员档案ID不能为空");
        }
        return success(studentArchiveService.selectStudentArchiveById(dto.getArchiveId()));
    }

    /**
     * 批量获取学员档案详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:archive:query')")
    @PostMapping("/getArchiveBatch")
    public AjaxResult getArchiveBatch(@RequestBody ArchiveBatchIdsDTO dto)
    {
        if (dto.getIds() == null || dto.getIds().isEmpty())
        {
            return error("学员档案ID列表不能为空");
        }
        List<StudentArchive> list = new ArrayList<>();
        for (Long id : dto.getIds())
        {
            StudentArchive archive = studentArchiveService.selectStudentArchiveById(id);
            if (archive != null)
            {
                list.add(archive);
            }
        }
        return success(list);
    }

    /**
     * 新增学员档案
     */
    @PreAuthorize("@ss.hasPermi('system:archive:add')")
    @Log(title = "学员档案", businessType = BusinessType.INSERT)
    @PostMapping("/addArchive")
    public AjaxResult addArchive(@Validated @RequestBody StudentArchive studentArchive)
    {
        if (!studentArchiveService.checkStudentAccountUnique(studentArchive))
        {
            return error("新增学员档案'" + studentArchive.getStudentAccount() + "'失败，学员账号已存在");
        }
        studentArchive.setCreateBy(getUsername());
        return toAjax(studentArchiveService.insertStudentArchive(studentArchive));
    }

    /**
     * 修改学员档案
     */
    @PreAuthorize("@ss.hasPermi('system:archive:edit')")
    @Log(title = "学员档案", businessType = BusinessType.UPDATE)
    @PostMapping("/updateArchive")
    public AjaxResult updateArchive(@Validated @RequestBody StudentArchive studentArchive)
    {
        if (!studentArchiveService.checkStudentAccountUnique(studentArchive))
        {
            return error("修改学员档案'" + studentArchive.getStudentAccount() + "'失败，学员账号已存在");
        }
        studentArchive.setUpdateBy(getUsername());
        return toAjax(studentArchiveService.updateStudentArchive(studentArchive));
    }

    /**
     * 删除学员档案（单个）
     */
    @PreAuthorize("@ss.hasPermi('system:archive:remove')")
    @Log(title = "学员档案", businessType = BusinessType.DELETE)
    @PostMapping("/delArchive")
    public AjaxResult delArchive(@RequestBody ArchiveIdDTO dto)
    {
        if (dto.getArchiveId() == null)
        {
            return error("学员档案ID不能为空");
        }
        return toAjax(studentArchiveService.deleteStudentArchiveById(dto.getArchiveId()));
    }

    /**
     * 批量删除学员档案
     */
    @PreAuthorize("@ss.hasPermi('system:archive:remove')")
    @Log(title = "学员档案", businessType = BusinessType.DELETE)
    @PostMapping("/delArchiveBatch")
    public AjaxResult delArchiveBatch(@RequestBody ArchiveBatchIdsDTO dto)
    {
        if (dto.getIds() == null || dto.getIds().isEmpty())
        {
            return error("删除失败，ID列表不能为空");
        }
        
        // 将List转换为Long[]
        Long[] ids = dto.getIds().toArray(new Long[0]);
        return toAjax(studentArchiveService.deleteStudentArchiveByIds(ids));
    }

    /**
     * 重置学员密码
     */
    @PreAuthorize("@ss.hasPermi('system:archive:resetPwd')")
    @Log(title = "学员档案", businessType = BusinessType.UPDATE)
    @PostMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody ResetPasswordDTO dto)
    {
        if (dto.getArchiveId() == null)
        {
            return error("学员档案ID不能为空");
        }
        return toAjax(studentArchiveService.resetPassword(dto.getArchiveId(), dto.getNewPassword()));
    }

    /**
     * 修改学员密码
     * 允许学员修改自己的密码，管理员可以修改任何学员的密码
     */
    @Log(title = "学员档案", businessType = BusinessType.UPDATE)
    @PostMapping("/changePwd")
    public AjaxResult changePwd(@RequestBody ChangePasswordDTO dto)
    {
        if (dto.getArchiveId() == null)
        {
            return error("学员档案ID不能为空");
        }
        if (dto.getOldPassword() == null || dto.getOldPassword().isEmpty())
        {
            return error("旧密码不能为空");
        }
        if (dto.getNewPassword() == null || dto.getNewPassword().isEmpty())
        {
            return error("新密码不能为空");
        }
        
        // 获取当前登录用户
        com.zx.common.core.domain.model.LoginUser loginUser = com.zx.common.utils.SecurityUtils.getLoginUser();
        com.zx.common.core.domain.entity.SysUser currentUser = loginUser.getUser();
        
        // 如果是学员，验证是否修改自己的密码
        if (currentUser.getUserType() != null && "01".equals(currentUser.getUserType()))
        {
            // 学员只能修改自己的密码
            com.zx.student.archive.domain.StudentArchive archive = studentArchiveService.selectStudentArchiveById(dto.getArchiveId());
            if (archive == null)
            {
                return error("学员档案不存在");
            }
            
            // 验证是否是自己的档案
            // 方式1：通过userId关联（优先）
            if (archive.getUserId() != null)
            {
                if (!archive.getUserId().equals(currentUser.getUserId()))
                {
                    return error("没有权限修改其他学员的密码");
                }
            }
            else
            {
                // 方式2：如果userId为null，通过学员账号验证
                if (!archive.getStudentAccount().equals(currentUser.getUserName()))
                {
                    return error("没有权限修改其他学员的密码");
                }
            }
        }
        // 管理员可以修改任何学员的密码，不需要额外验证
        
        return toAjax(studentArchiveService.changePassword(dto.getArchiveId(), dto.getOldPassword(), dto.getNewPassword()));
    }

    /**
     * 忘记密码（客户端使用，无需权限验证）
     */
    @Log(title = "学员档案", businessType = BusinessType.UPDATE)
    @PostMapping("/forgotPassword")
    public AjaxResult forgotPassword(@RequestBody ForgotPasswordDTO dto)
    {
        if (dto.getStudentAccount() == null || dto.getStudentAccount().isEmpty())
        {
            return error("学员账号不能为空");
        }
        if (dto.getPhoneNumber() == null || dto.getPhoneNumber().isEmpty())
        {
            return error("手机号码不能为空");
        }
        if (dto.getNewPassword() == null || dto.getNewPassword().isEmpty())
        {
            return error("新密码不能为空");
        }
        return toAjax(studentArchiveService.forgotPassword(dto.getStudentAccount(), dto.getPhoneNumber(), dto.getNewPassword()));
    }
}

