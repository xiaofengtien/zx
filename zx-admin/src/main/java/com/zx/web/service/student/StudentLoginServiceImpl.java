package com.zx.web.service.student;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import com.zx.common.constant.CacheConstants;
import com.zx.common.constant.Constants;
import com.zx.common.constant.UserConstants;
import com.zx.common.core.domain.model.LoginUser;
import com.zx.common.core.redis.RedisCache;
import com.zx.common.exception.ServiceException;
import com.zx.common.exception.user.CaptchaException;
import com.zx.common.exception.user.CaptchaExpireException;
import com.zx.common.exception.user.UserNotExistsException;
import com.zx.common.exception.user.UserPasswordNotMatchException;
import com.zx.common.utils.MessageUtils;
import com.zx.common.utils.StringUtils;
import com.zx.framework.manager.AsyncManager;
import com.zx.framework.manager.factory.AsyncFactory;
import com.zx.framework.security.context.AuthenticationContextHolder;
import com.zx.framework.web.service.TokenService;
import com.zx.student.archive.domain.StudentArchive;
import com.zx.student.archive.mapper.StudentArchiveMapper;
import com.zx.student.archive.service.IStudentLoginService;
import com.zx.student.archive.service.IStudentOfflineCredentialService;
import com.zx.system.service.ISysUserService;

/**
 * 学员登录Service实现
 *
 * @author zx
 */
@Service
public class StudentLoginServiceImpl implements IStudentLoginService
{
    @Autowired
    private TokenService tokenService;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private StudentArchiveMapper studentArchiveMapper;

    @Autowired
    @Qualifier("studentUserDetailsService")
    private UserDetailsService studentUserDetailsService;

    @Autowired
    private IStudentOfflineCredentialService offlineCredentialService;

    @Autowired
    private ISysUserService userService;

    /**
     * 在线登录（需要验证码）
     *
     * @param studentAccount 学员账号
     * @param password 密码
     * @param code 验证码
     * @param uuid 唯一标识
     * @return token
     */
    @Override
    public String onlineLogin(String studentAccount, String password, String code, String uuid)
    {
        // 验证码校验
        validateCaptcha(studentAccount, code, uuid);
        // 登录前置校验
        loginPreCheck(studentAccount, password);
        // 用户验证
        Authentication authentication = authenticate(studentAccount, password);
        // 记录登录信息
        recordLoginInfo(studentAccount);
        // 生成token
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String token = tokenService.createToken(loginUser);
        
        // 生成离线凭证（用于桌面应用离线登录）
        generateOfflineCredential(studentAccount);
        
        return token;
    }

    /**
     * 生成离线凭证
     */
    private void generateOfflineCredential(String studentAccount)
    {
        try
        {
            StudentArchive archive = studentArchiveMapper.selectStudentArchiveByStudentAccount(studentAccount);
            if (archive != null)
            {
                // 生成离线凭证（有效期30天）
                String credential = offlineCredentialService.generateOfflineCredential(
                    archive.getStudentAccount(),
                    archive.getPassword(),
                    archive.getId(),
                    archive.getUserId(),
                    30
                );
                // TODO: 可以将离线凭证返回给前端，由前端保存到本地
                // 或者存储到Redis中，供后续离线登录使用
            }
        }
        catch (Exception e)
        {
            // 生成离线凭证失败不影响在线登录
            // 可以记录日志
        }
    }

    /**
     * 离线登录（使用离线凭证或密码）
     *
     * @param studentAccount 学员账号
     * @param password 密码（可选，如果提供了离线凭证则不需要）
     * @param offlineCredential 离线凭证（可选）
     * @return token
     */
    @Override
    public String offlineLogin(String studentAccount, String password, String offlineCredential)
    {
        // 如果提供了离线凭证，使用离线凭证验证
        if (StringUtils.isNotEmpty(offlineCredential))
        {
            com.zx.student.archive.domain.dto.OfflineCredential credential = 
                offlineCredentialService.verifyOfflineCredential(offlineCredential);
            
            if (credential == null)
            {
                throw new ServiceException("离线凭证无效或已过期");
            }

            // 验证账号和密码
            if (StringUtils.isEmpty(password))
            {
                throw new ServiceException("离线登录需要提供密码");
            }

            if (!offlineCredentialService.verifyOfflineLogin(studentAccount, password, credential))
            {
                throw new UserPasswordNotMatchException();
            }

            // 查询学员档案
            StudentArchive archive = studentArchiveMapper.selectStudentArchiveByStudentAccount(studentAccount);
            if (archive == null)
            {
                throw new ServiceException("学员不存在");
            }

            // 创建临时认证对象
            LoginUser loginUser = createLoginUserFromArchive(archive);
            return tokenService.createToken(loginUser);
        }
        else
        {
            // 如果没有离线凭证，使用密码验证（需要网络连接）
            loginPreCheck(studentAccount, password);
            Authentication authentication = authenticate(studentAccount, password);
            recordLoginInfo(studentAccount);
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            return tokenService.createToken(loginUser);
        }
    }

    /**
     * 从学员档案创建LoginUser（用于离线登录）
     */
    private LoginUser createLoginUserFromArchive(StudentArchive archive)
    {
        com.zx.common.core.domain.entity.SysUser user = null;
        if (archive.getUserId() != null)
        {
            user = userService.selectUserById(archive.getUserId());
        }
        
        if (user == null)
        {
            user = new com.zx.common.core.domain.entity.SysUser();
            user.setUserId(archive.getId());
            user.setUserName(archive.getStudentAccount());
            user.setNickName(archive.getStudentAccount());
            user.setUserType("01");
        }

        // 返回固定权限（在Controller中会覆盖）
        return new LoginUser(user.getUserId(), null, user, new java.util.HashSet<>());
    }

    /**
     * 用户验证
     */
    private Authentication authenticate(String studentAccount, String password)
    {
        Authentication authentication = null;
        try
        {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(studentAccount, password);
            AuthenticationContextHolder.setContext(authenticationToken);
            // 使用学员专用的UserDetailsService进行验证
            LoginUser loginUser = (LoginUser) studentUserDetailsService.loadUserByUsername(studentAccount);
            // 手动创建认证对象
            authentication = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        }
        catch (Exception e)
        {
            if (e instanceof BadCredentialsException || e instanceof ServiceException)
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(studentAccount, Constants.LOGIN_FAIL, e.getMessage()));
                throw new UserPasswordNotMatchException();
            }
            else
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(studentAccount, Constants.LOGIN_FAIL, e.getMessage()));
                throw new ServiceException(e.getMessage());
            }
        }
        finally
        {
            AuthenticationContextHolder.clearContext();
        }
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(studentAccount, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success")));
        return authentication;
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
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(studentAccount, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire")));
            throw new CaptchaExpireException();
        }
        redisCache.deleteObject(verifyKey);
        if (!code.equalsIgnoreCase(captcha))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(studentAccount, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error")));
            throw new CaptchaException();
        }
    }

    /**
     * 登录前置校验
     */
    private void loginPreCheck(String studentAccount, String password)
    {
        // 用户名或密码为空 错误
        if (StringUtils.isEmpty(studentAccount) || StringUtils.isEmpty(password))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(studentAccount, Constants.LOGIN_FAIL, MessageUtils.message("not.null")));
            throw new UserNotExistsException();
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(studentAccount, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // 用户名不在指定范围内 错误
        if (studentAccount.length() < UserConstants.USERNAME_MIN_LENGTH
                || studentAccount.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(studentAccount, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
    }

    /**
     * 记录登录信息
     */
    private void recordLoginInfo(String studentAccount)
    {
        // TODO: 如果需要记录学员登录信息，可以在这里实现
        // StudentArchive archive = studentArchiveMapper.selectStudentArchiveByStudentAccount(studentAccount);
        // if (archive != null) {
        //     // 更新登录信息
        // }
    }

    /**
     * 通过 archiveId 查询学员档案
     *
     * @param archiveId 学员档案ID
     * @return 学员档案
     */
    @Override
    public com.zx.student.archive.domain.StudentArchive getStudentArchiveById(Long archiveId)
    {
        if (archiveId == null)
        {
            return null;
        }
        return studentArchiveMapper.selectStudentArchiveById(archiveId);
    }

    /**
     * 通过学员账号查询学员档案
     *
     * @param studentAccount 学员账号
     * @return 学员档案
     */
    @Override
    public com.zx.student.archive.domain.StudentArchive getStudentArchiveByAccount(String studentAccount)
    {
        if (com.zx.common.utils.StringUtils.isEmpty(studentAccount))
        {
            return null;
        }
        return studentArchiveMapper.selectStudentArchiveByStudentAccount(studentAccount);
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
        StudentArchive archive = studentArchiveMapper.selectStudentArchiveByUserId(userId);
        if (archive != null)
        {
            System.out.println("查询到学员档案，ID: " + archive.getId() + ", 账号: " + archive.getStudentAccount());
            System.out.println("适用试卷类型: " + archive.getApplicablePapers());
            System.out.println("适用试卷类型是否为null: " + (archive.getApplicablePapers() == null));
            if (archive.getApplicablePapers() != null)
            {
                System.out.println("适用试卷类型数量: " + archive.getApplicablePapers().size());
            }
        }
        return archive;
    }
}


