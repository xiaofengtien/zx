package com.ruoyi.student.archive.mapper.question;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.student.archive.domain.question.wrongquestion.AppUserWrongQuestionNormal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 用户普通题错题本Mapper接口
 */
@Mapper
public interface AppUserWrongQuestionNormalMapper extends BaseMapper<AppUserWrongQuestionNormal> {

    /**
     * 关联查询普通题型的错题
     * @param appUserId 用户ID
     * @param subjectId 学科ID
     * @param questionIds 题目ID列表
     * @param type 题目类型
     * @return 错题列表
     */
    List<AppUserWrongQuestionNormal> queryNormalWrongQuestionsWithJoin(
            @Param("appUserId") Integer appUserId,
            @Param("subjectId") Integer subjectId,
            @Param("questionIds") Set<Integer> questionIds,
            @Param("bizType") String bizType,
            @Param("type") Integer type);
}