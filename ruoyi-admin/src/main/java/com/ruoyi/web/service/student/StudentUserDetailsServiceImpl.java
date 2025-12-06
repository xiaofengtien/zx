package com.ruoyi.web.service.student;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.UserStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.student.archive.domain.StudentArchive;
import com.ruoyi.student.archive.mapper.StudentArchiveMapper;
import com.ruoyi.system.service.ISysUserService;

/**
 * 学员用户验证处理
 *
 * @author ruoyi
 */
@Service("studentUserDetailsService")
public class StudentUserDetailsServiceImpl implements UserDetailsService
{
    private static final Logger log = LoggerFactory.getLogger(StudentUserDetailsServiceImpl.class);

    @Autowired
    private StudentArchiveMapper studentArchiveMapper;

    @Autowired
    private ISysUserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
    {
        // 先查询学员档案
        StudentArchive archive = studentArchiveMapper.selectStudentArchiveByStudentAccount(username);
        if (StringUtils.isNull(archive))
        {
            log.info("登录学员：{} 不存在.", username);
            throw new ServiceException(MessageUtils.message("user.not.exists"));
        }
        else if ("2".equals(archive.getDelFlag()))
        {
            log.info("登录学员：{} 已被删除.", username);
            throw new ServiceException(MessageUtils.message("user.password.delete"));
        }
        else if ("1".equals(archive.getStatus()))
        {
            log.info("登录学员：{} 已被停用.", username);
            throw new ServiceException(MessageUtils.message("user.blocked"));
        }

        // 如果学员档案关联了系统用户，则使用系统用户进行认证
        SysUser user = null;
        if (archive.getUserId() != null)
        {
            user = userService.selectUserById(archive.getUserId());
            if (StringUtils.isNull(user))
            {
                log.warn("学员档案关联的系统用户ID：{} 不存在，将使用学员档案直接认证", archive.getUserId());
            }
            else if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
            {
                log.info("登录学员关联的系统用户：{} 已被删除.", username);
                throw new ServiceException(MessageUtils.message("user.password.delete"));
            }
            else if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
            {
                log.info("登录学员关联的系统用户：{} 已被停用.", username);
                throw new ServiceException(MessageUtils.message("user.blocked"));
            }
        }

        // 如果有关联的系统用户，使用系统用户；否则创建临时SysUser对象用于认证
        if (user == null)
        {
            user = createTempSysUser(archive);
        }

        // 验证密码（使用学员档案的密码）
        // 从AuthenticationContextHolder获取密码
        org.springframework.security.core.Authentication authentication = com.ruoyi.framework.security.context.AuthenticationContextHolder.getContext();
        if (authentication != null && authentication.getCredentials() != null)
        {
            String rawPassword = authentication.getCredentials().toString();
            if (!SecurityUtils.matchesPassword(rawPassword, archive.getPassword()))
            {
                log.info("登录学员：{} 密码错误.", username);
                throw new ServiceException(MessageUtils.message("user.password.not.match"));
            }
        }

        return createLoginUser(user, archive);
    }

    /**
     * 创建临时SysUser对象（用于没有关联系统用户的学员）
     */
    private SysUser createTempSysUser(StudentArchive archive)
    {
        SysUser user = new SysUser();
        user.setUserId(archive.getId()); // 使用学员档案ID作为临时用户ID
        user.setUserName(archive.getStudentAccount());
        user.setNickName(archive.getStudentAccount());
        user.setPassword(archive.getPassword());
        user.setStatus(archive.getStatus());
        user.setDelFlag(archive.getDelFlag());
        user.setUserType("01"); // 学员类型
        return user;
    }

    /**
     * 创建LoginUser
     * 注意：权限会在Controller的getInfo方法中固定返回，这里返回空权限集合即可
     */
    private UserDetails createLoginUser(SysUser user, StudentArchive archive)
    {
        // 返回空权限集合，权限会在Controller中固定返回
        // 这样可以避免ruoyi-business依赖ruoyi-framework造成循环依赖
        return new LoginUser(user.getUserId(), null, user, new java.util.HashSet<>());
    }
}




