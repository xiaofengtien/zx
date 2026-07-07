package com.zx.student.archive.service;

import java.util.List;
import com.zx.student.archive.domain.StudentArchive;

/**
 * 学员档案 服务层
 * 
 * @author zx
 */
public interface IStudentArchiveService
{
    /**
     * 查询学员档案列表
     * 
     * @param studentArchive 学员档案
     * @return 学员档案集合
     */
    public List<StudentArchive> selectStudentArchiveList(StudentArchive studentArchive);

    /**
     * 通过学员档案ID查询学员档案
     * 
     * @param id 学员档案ID
     * @return 学员档案对象信息
     */
    public StudentArchive selectStudentArchiveById(Long id);

    /**
     * 校验学员账号是否唯一
     * 
     * @param studentArchive 学员档案
     * @return 结果
     */
    public boolean checkStudentAccountUnique(StudentArchive studentArchive);

    /**
     * 新增学员档案
     * 
     * @param studentArchive 学员档案
     * @return 结果
     */
    public int insertStudentArchive(StudentArchive studentArchive);

    /**
     * 修改学员档案
     * 
     * @param studentArchive 学员档案
     * @return 结果
     */
    public int updateStudentArchive(StudentArchive studentArchive);

    /**
     * 批量删除学员档案
     * 
     * @param ids 需要删除的学员档案ID
     * @return 结果
     */
    public int deleteStudentArchiveByIds(Long[] ids);

    /**
     * 删除学员档案信息
     * 
     * @param id 学员档案ID
     * @return 结果
     */
    public int deleteStudentArchiveById(Long id);

    /**
     * 重置学员密码
     * 
     * @param archiveId 学员档案ID
     * @param newPassword 新密码（未加密）
     * @return 结果
     */
    public int resetPassword(Long archiveId, String newPassword);

    /**
     * 修改学员密码
     * 
     * @param archiveId 学员档案ID
     * @param oldPassword 旧密码（未加密）
     * @param newPassword 新密码（未加密）
     * @return 结果
     */
    public int changePassword(Long archiveId, String oldPassword, String newPassword);

    /**
     * 忘记密码（通过手机号重置密码）
     * 
     * @param studentAccount 学员账号
     * @param phoneNumber 手机号码
     * @param newPassword 新密码（未加密）
     * @return 结果
     */
    public int forgotPassword(String studentAccount, String phoneNumber, String newPassword);

    /**
     * 通过系统用户ID查询学员档案
     * 
     * @param userId 系统用户ID
     * @return 学员档案
     */
    public StudentArchive getStudentArchiveByUserId(Long userId);
}

