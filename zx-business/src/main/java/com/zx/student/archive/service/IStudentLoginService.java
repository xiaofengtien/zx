package com.zx.student.archive.service;

import com.zx.student.archive.domain.StudentArchive;

/**
 * 学员登录Service接口
 *
 * @author zx
 */
public interface IStudentLoginService
{
    /**
     * 在线登录（需要验证码）
     *
     * @param studentAccount 学员账号
     * @param password 密码
     * @param code 验证码
     * @param uuid 唯一标识
     * @return token
     */
    String onlineLogin(String studentAccount, String password, String code, String uuid);

    /**
     * 离线登录（使用离线凭证或密码）
     *
     * @param studentAccount 学员账号
     * @param password 密码（可选，如果提供了离线凭证则不需要）
     * @param offlineCredential 离线凭证（可选）
     * @return token
     */
    String offlineLogin(String studentAccount, String password, String offlineCredential);

    /**
     * 通过 archiveId 查询学员档案
     *
     * @param archiveId 学员档案ID
     * @return 学员档案
     */
    StudentArchive getStudentArchiveById(Long archiveId);

    /**
     * 通过学员账号查询学员档案
     *
     * @param studentAccount 学员账号
     * @return 学员档案
     */
    StudentArchive getStudentArchiveByAccount(String studentAccount);

    /**
     * 通过系统用户ID查询学员档案
     *
     * @param userId 系统用户ID
     * @return 学员档案
     */
    StudentArchive getStudentArchiveByUserId(Long userId);
}

