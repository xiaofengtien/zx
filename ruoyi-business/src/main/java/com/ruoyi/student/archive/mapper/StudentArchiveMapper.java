package com.ruoyi.student.archive.mapper;

import java.util.List;
import com.ruoyi.student.archive.domain.StudentArchive;

/**
 * 学员档案 数据层
 * 
 * @author ruoyi
 */
public interface StudentArchiveMapper
{
    /**
     * 查询学员档案列表
     * 
     * @param studentArchive 学员档案
     * @return 学员档案集合
     */
    public List<StudentArchive> selectStudentArchiveList(StudentArchive studentArchive);

    /**
     * 根据学员账号查询学员档案
     * 
     * @param studentAccount 学员账号
     * @return 学员档案
     */
    public StudentArchive selectStudentArchiveByStudentAccount(String studentAccount);

    /**
     * 通过学员档案ID查询学员档案
     * 
     * @param id 学员档案ID
     * @return 学员档案对象信息
     */
    public StudentArchive selectStudentArchiveById(Long id);

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
     * 删除学员档案
     * 
     * @param id 学员档案ID
     * @return 结果
     */
    public int deleteStudentArchiveById(Long id);

    /**
     * 批量删除学员档案
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteStudentArchiveByIds(Long[] ids);

    /**
     * 校验学员账号是否唯一
     * 
     * @param studentAccount 学员账号
     * @return 结果
     */
    public StudentArchive checkStudentAccountUnique(String studentAccount);

    /**
     * 通过系统用户ID查询学员档案
     * 
     * @param userId 系统用户ID
     * @return 学员档案
     */
    public StudentArchive selectStudentArchiveByUserId(Long userId);
}


