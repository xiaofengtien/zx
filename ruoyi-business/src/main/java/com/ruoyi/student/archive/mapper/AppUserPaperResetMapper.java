package com.ruoyi.student.archive.mapper;

import com.ruoyi.student.archive.domain.AppUserPaperReset;
import org.apache.ibatis.annotations.*;
import java.util.Date;
import java.util.List;

/**
 * 用户试卷练习次数重置Mapper
 */
@Mapper
public interface AppUserPaperResetMapper {

    /**
     * 插入重置记录
     */
    @Insert("INSERT INTO app_user_paper_reset (user_id, paper_id, reset_time, reset_by, remark, create_time) " +
            "VALUES (#{userId}, #{paperId}, #{resetTime}, #{resetBy}, #{remark}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AppUserPaperReset reset);

    /**
     * 查询用户的重置记录（指定时间之后的）
     */
    @Select("<script>" +
            "SELECT * FROM app_user_paper_reset WHERE user_id = #{userId} " +
            "<if test='sinceTime != null'>AND reset_time > #{sinceTime}</if> " +
            "ORDER BY reset_time DESC" +
            "</script>")
    List<AppUserPaperReset> selectByUserId(@Param("userId") Long userId, @Param("sinceTime") Date sinceTime);

    /**
     * 查询用户某试卷的最新重置时间
     */
    @Select("SELECT MAX(reset_time) FROM app_user_paper_reset " +
            "WHERE user_id = #{userId} AND (paper_id = #{paperId} OR paper_id IS NULL)")
    Date selectLatestResetTime(@Param("userId") Long userId, @Param("paperId") Long paperId);

    /**
     * 删除重置记录
     */
    @Delete("DELETE FROM app_user_paper_reset WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 查询重置记录列表（管理后台用）
     */
    @Select("<script>" +
            "SELECT r.*, u.nick_name as userName, p.paper_name as paperName " +
            "FROM app_user_paper_reset r " +
            "LEFT JOIN sys_user u ON r.user_id = u.user_id " +
            "LEFT JOIN paper p ON r.paper_id = p.id " +
            "<where>" +
            "<if test='userId != null'>AND r.user_id = #{userId}</if>" +
            "<if test='paperId != null'>AND r.paper_id = #{paperId}</if>" +
            "</where>" +
            "ORDER BY r.reset_time DESC" +
            "</script>")
    List<AppUserPaperReset> selectList(AppUserPaperReset query);
}
