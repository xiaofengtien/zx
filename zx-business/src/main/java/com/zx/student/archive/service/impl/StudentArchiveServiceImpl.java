package com.zx.student.archive.service.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.zx.common.constant.CacheConstants;
import com.zx.common.constant.Constants;
import com.zx.common.constant.UserConstants;
import com.zx.common.core.redis.RedisCache;
import com.zx.common.exception.user.CaptchaException;
import com.zx.common.exception.user.CaptchaExpireException;
import com.zx.common.utils.StringUtils;
import com.zx.common.utils.SecurityUtils;
import com.zx.common.core.domain.entity.SysUser;
import com.zx.student.archive.domain.StudentArchive;
import com.zx.student.archive.mapper.StudentArchiveMapper;
import com.zx.student.archive.service.IStudentArchiveService;
import com.zx.system.service.ISysUserService;

/**
 * 学员档案 服务层处理
 * 
 * @author zx
 */
@Service
public class StudentArchiveServiceImpl implements IStudentArchiveService
{
    @Autowired
    private StudentArchiveMapper studentArchiveMapper;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private RedisCache redisCache;

    /**
     * 查询学员档案列表
     * 
     * @param studentArchive 学员档案
     * @return 学员档案集合
     */
    @Override
    public List<StudentArchive> selectStudentArchiveList(StudentArchive studentArchive)
    {
        return studentArchiveMapper.selectStudentArchiveList(studentArchive);
    }

    /**
     * 通过学员档案ID查询学员档案
     * 
     * @param id 学员档案ID
     * @return 学员档案对象信息
     */
    @Override
    public StudentArchive selectStudentArchiveById(Long id)
    {
        return studentArchiveMapper.selectStudentArchiveById(id);
    }

    /**
     * 校验学员账号是否唯一
     * 
     * @param studentArchive 学员档案
     * @return 结果
     */
    @Override
    public boolean checkStudentAccountUnique(StudentArchive studentArchive)
    {
        Long id = StringUtils.isNull(studentArchive.getId()) ? -1L : studentArchive.getId();
        StudentArchive info = studentArchiveMapper.checkStudentAccountUnique(studentArchive.getStudentAccount());
        if (StringUtils.isNotNull(info) && info.getId().longValue() != id.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 新增学员档案
     * 
     * @param studentArchive 学员档案
     * @return 结果
     */
    @Override
    @Transactional
    public int insertStudentArchive(StudentArchive studentArchive)
    {
        // 如果密码为空，设置默认密码为 123456
        String rawPassword = studentArchive.getPassword();
        if (StringUtils.isEmpty(rawPassword))
        {
            rawPassword = "123456";
            studentArchive.setPassword(rawPassword);
        }
        // 密码加密
        studentArchive.setPassword(SecurityUtils.encryptPassword(rawPassword));
        // 设置默认状态
        if (StringUtils.isEmpty(studentArchive.getStatus()))
        {
            studentArchive.setStatus("0");
        }
        
        // 先插入学员档案，获取自增ID
        int rows = studentArchiveMapper.insertStudentArchive(studentArchive);
        
        // 自动创建关联的系统用户
        if (rows > 0 && studentArchive.getId() != null)
        {
            SysUser sysUser = createSysUserFromArchive(studentArchive, rawPassword);
            int userRows = userService.insertUser(sysUser);
            if (userRows > 0)
            {
                // 更新学员档案的user_id
                studentArchive.setUserId(sysUser.getUserId());
                studentArchiveMapper.updateStudentArchive(studentArchive);
            }
        }
        
        return rows;
    }

    /**
     * 根据学员档案创建系统用户
     * 
     * @param archive 学员档案
     * @param rawPassword 原始密码（未加密）
     * @return 系统用户对象
     */
    private SysUser createSysUserFromArchive(StudentArchive archive, String rawPassword)
    {
        SysUser user = new SysUser();
        user.setUserName(archive.getStudentAccount());
        user.setNickName(archive.getStudentAccount());
        user.setUserType("01"); // 学员类型
        user.setEmail(StringUtils.isEmpty(archive.getPhoneNumber()) ? "" : archive.getPhoneNumber() + "@student.local");
        user.setPhonenumber(archive.getPhoneNumber());
        user.setSex(archive.getSex());
        user.setStatus(archive.getStatus());
        user.setDelFlag("0");
        // 如果提供了原始密码，使用原始密码；否则使用已加密的密码
        if (StringUtils.isNotEmpty(rawPassword))
        {
            user.setPassword(SecurityUtils.encryptPassword(rawPassword));
        }
        else
        {
            user.setPassword(archive.getPassword());
        }
        user.setCreateBy(archive.getCreateBy());
        user.setCreateTime(archive.getCreateTime());
        user.setRemark("学员用户，关联学员档案ID: " + archive.getId());
        return user;
    }

    /**
     * 修改学员档案
     * 
     * @param studentArchive 学员档案
     * @return 结果
     */
    @Override
    @Transactional
    public int updateStudentArchive(StudentArchive studentArchive)
    {
        // 如果密码不为空且不是密文占位符，则加密
        String rawPassword = null;
        if (StringUtils.isNotEmpty(studentArchive.getPassword()) && !"******".equals(studentArchive.getPassword()))
        {
            rawPassword = studentArchive.getPassword();
            studentArchive.setPassword(SecurityUtils.encryptPassword(rawPassword));
        }
        else
        {
            // 如果密码为空或是密文占位符，则不更新密码字段
            studentArchive.setPassword(null);
        }
        
        int rows = studentArchiveMapper.updateStudentArchive(studentArchive);
        
        // 同步更新关联的系统用户
        if (rows > 0 && studentArchive.getUserId() != null)
        {
            SysUser sysUser = userService.selectUserById(studentArchive.getUserId());
            if (sysUser != null)
            {
                updateSysUserFromArchive(sysUser, studentArchive, rawPassword);
                userService.updateUser(sysUser);
            }
            else
            {
                // 如果系统用户不存在，则创建新的
                sysUser = createSysUserFromArchive(studentArchive, rawPassword);
                int userRows = userService.insertUser(sysUser);
                if (userRows > 0)
                {
                    studentArchive.setUserId(sysUser.getUserId());
                    studentArchiveMapper.updateStudentArchive(studentArchive);
                }
            }
        }
        else if (rows > 0 && studentArchive.getUserId() == null)
        {
            // 如果学员档案没有关联系统用户，则创建新的
            SysUser sysUser = createSysUserFromArchive(studentArchive, rawPassword);
            int userRows = userService.insertUser(sysUser);
            if (userRows > 0)
            {
                studentArchive.setUserId(sysUser.getUserId());
                studentArchiveMapper.updateStudentArchive(studentArchive);
            }
        }
        
        return rows;
    }

    /**
     * 根据学员档案更新系统用户
     * 
     * @param user 系统用户对象（需要更新）
     * @param archive 学员档案
     * @param rawPassword 原始密码（未加密，如果为null则不更新密码）
     */
    private void updateSysUserFromArchive(SysUser user, StudentArchive archive, String rawPassword)
    {
        user.setUserName(archive.getStudentAccount());
        user.setNickName(archive.getStudentAccount());
        user.setEmail(StringUtils.isEmpty(archive.getPhoneNumber()) ? "" : archive.getPhoneNumber() + "@student.local");
        user.setPhonenumber(archive.getPhoneNumber());
        user.setSex(archive.getSex());
        user.setStatus(archive.getStatus());
        // 如果提供了原始密码，则更新密码
        if (StringUtils.isNotEmpty(rawPassword))
        {
            user.setPassword(SecurityUtils.encryptPassword(rawPassword));
        }
        user.setUpdateBy(archive.getUpdateBy());
        user.setUpdateTime(archive.getUpdateTime());
        user.setRemark("学员用户，关联学员档案ID: " + archive.getId());
    }

    /**
     * 批量删除学员档案
     * 
     * @param ids 需要删除的学员档案ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteStudentArchiveByIds(Long[] ids)
    {
        // 先查询所有要删除的学员档案，获取关联的user_id
        for (Long id : ids)
        {
            StudentArchive archive = studentArchiveMapper.selectStudentArchiveById(id);
            if (archive != null && archive.getUserId() != null)
            {
                // 同步删除/停用关联的系统用户
                userService.deleteUserById(archive.getUserId());
            }
        }
        return studentArchiveMapper.deleteStudentArchiveByIds(ids);
    }

    /**
     * 删除学员档案信息
     * 
     * @param id 学员档案ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteStudentArchiveById(Long id)
    {
        // 查询学员档案，获取关联的user_id
        StudentArchive archive = studentArchiveMapper.selectStudentArchiveById(id);
        if (archive != null && archive.getUserId() != null)
        {
            // 同步删除/停用关联的系统用户
            userService.deleteUserById(archive.getUserId());
        }
        return studentArchiveMapper.deleteStudentArchiveById(id);
    }

    /**
     * 重置学员密码
     * 
     * @param archiveId 学员档案ID
     * @param newPassword 新密码（未加密）
     * @return 结果
     */
    @Override
    @Transactional
    public int resetPassword(Long archiveId, String newPassword)
    {
        if (StringUtils.isEmpty(newPassword))
        {
            newPassword = "123456"; // 默认密码
        }
        
        StudentArchive archive = studentArchiveMapper.selectStudentArchiveById(archiveId);
        if (archive == null)
        {
            throw new com.zx.common.exception.ServiceException("学员档案不存在");
        }
        
        // 加密新密码
        String encryptedPassword = SecurityUtils.encryptPassword(newPassword);
        archive.setPassword(encryptedPassword);
        // 更新密码修改时间
        archive.setPwdUpdateDate(com.zx.common.utils.DateUtils.getNowDate());
        
        // 更新学员档案密码
        int rows = studentArchiveMapper.updateStudentArchive(archive);
        
        // 同步更新关联的系统用户密码
        if (rows > 0 && archive.getUserId() != null)
        {
            SysUser sysUser = userService.selectUserById(archive.getUserId());
            if (sysUser != null)
            {
                sysUser.setPassword(encryptedPassword);
                userService.resetUserPwd(sysUser.getUserId(), encryptedPassword);
            }
        }
        
        return rows;
    }

    /**
     * 修改学员密码
     * 
     * @param archiveId 学员档案ID
     * @param oldPassword 旧密码（未加密）
     * @param newPassword 新密码（未加密）
     * @return 结果
     */
    @Override
    @Transactional
    public int changePassword(Long archiveId, String oldPassword, String newPassword)
    {
        if (StringUtils.isEmpty(oldPassword) || StringUtils.isEmpty(newPassword))
        {
            throw new com.zx.common.exception.ServiceException("旧密码和新密码不能为空");
        }
        
        StudentArchive archive = studentArchiveMapper.selectStudentArchiveById(archiveId);
        if (archive == null)
        {
            throw new com.zx.common.exception.ServiceException("学员档案不存在");
        }
        
        // 验证旧密码
        if (!SecurityUtils.matchesPassword(oldPassword, archive.getPassword()))
        {
            throw new com.zx.common.exception.ServiceException("旧密码错误");
        }
        
        // 检查新密码是否与旧密码相同
        if (SecurityUtils.matchesPassword(newPassword, archive.getPassword()))
        {
            throw new com.zx.common.exception.ServiceException("新密码不能与旧密码相同");
        }
        
        // 加密新密码
        String encryptedPassword = SecurityUtils.encryptPassword(newPassword);
        archive.setPassword(encryptedPassword);
        // 更新密码修改时间
        archive.setPwdUpdateDate(com.zx.common.utils.DateUtils.getNowDate());
        
        // 更新学员档案密码
        int rows = studentArchiveMapper.updateStudentArchive(archive);
        
        // 同步更新关联的系统用户密码
        if (rows > 0 && archive.getUserId() != null)
        {
            SysUser sysUser = userService.selectUserById(archive.getUserId());
            if (sysUser != null)
            {
                sysUser.setPassword(encryptedPassword);
                userService.resetUserPwd(sysUser.getUserId(), encryptedPassword);
            }
        }
        
        return rows;
    }

    /**
     * 忘记密码（通过手机号重置密码）
     * 
     * @param studentAccount 学员账号
     * @param phoneNumber 手机号码
     * @param newPassword 新密码（未加密）
     * @return 结果
     */
    @Override
    @Transactional
    public int forgotPassword(String studentAccount, String phoneNumber, String newPassword)
    {
        // 参数校验
        if (StringUtils.isEmpty(studentAccount))
        {
            throw new com.zx.common.exception.ServiceException("学员账号不能为空");
        }
        if (StringUtils.isEmpty(phoneNumber))
        {
            throw new com.zx.common.exception.ServiceException("手机号码不能为空");
        }
        if (StringUtils.isEmpty(newPassword))
        {
            throw new com.zx.common.exception.ServiceException("新密码不能为空");
        }
        if (newPassword.length() < 6 || newPassword.length() > 20)
        {
            throw new com.zx.common.exception.ServiceException("密码长度必须介于 6 和 20 之间");
        }

        // 查询学员档案
        StudentArchive archive = studentArchiveMapper.selectStudentArchiveByStudentAccount(studentAccount);
        if (archive == null)
        {
            throw new com.zx.common.exception.ServiceException("学员账号不存在");
        }

        // 验证手机号是否匹配
        if (!phoneNumber.equals(archive.getPhoneNumber()))
        {
            throw new com.zx.common.exception.ServiceException("手机号码与账号不匹配");
        }

        // 加密新密码
        String encryptedPassword = SecurityUtils.encryptPassword(newPassword);
        archive.setPassword(encryptedPassword);
        // 更新密码修改时间
        archive.setPwdUpdateDate(com.zx.common.utils.DateUtils.getNowDate());

        // 更新学员档案密码
        int rows = studentArchiveMapper.updateStudentArchive(archive);

        // 同步更新关联的系统用户密码
        if (rows > 0 && archive.getUserId() != null)
        {
            SysUser sysUser = userService.selectUserById(archive.getUserId());
            if (sysUser != null)
            {
                sysUser.setPassword(encryptedPassword);
                userService.resetUserPwd(sysUser.getUserId(), encryptedPassword);
            }
        }

        return rows;
    }

    /**
     * 通过系统用户ID查询学员档案
     * 
     * @param userId 系统用户ID
     * @return 学员档案
     */
    @Override
    public StudentArchive getStudentArchiveByUserId(Long userId)
    {
        if (userId == null)
        {
            return null;
        }
        return studentArchiveMapper.selectStudentArchiveByUserId(userId);
    }

    /**
     * 校验验证码
     */
    private void validateCaptcha(String studentAccount, String code, String uuid)
    {
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
        String captcha = redisCache.getCacheObject(verifyKey);
        if (captcha == null)
        {
            throw new CaptchaExpireException();
        }
        redisCache.deleteObject(verifyKey);
        if (!code.equalsIgnoreCase(captcha))
        {
            throw new CaptchaException();
        }
    }
}

