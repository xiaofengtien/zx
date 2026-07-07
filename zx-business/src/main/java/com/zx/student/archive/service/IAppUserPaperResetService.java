package com.zx.student.archive.service;

import com.zx.student.archive.domain.AppUserPaperReset;
import java.util.Date;
import java.util.List;

/**
 * 用户试卷练习次数重置Service接口
 */
public interface IAppUserPaperResetService {

    /**
     * 重置用户的试卷练习次数
     * @param userId 用户ID
     * @param paperId 试卷ID（为空表示重置所有试卷）
     * @param resetBy 操作人
     * @param remark 备注
     */
    void resetUserPaper(Long userId, Long paperId, String resetBy, String remark);

    /**
     * 获取用户的重置记录（客户端同步用）
     * @param userId 用户ID
     * @param sinceTime 从什么时间开始（为空返回所有）
     */
    List<AppUserPaperReset> getUserResets(Long userId, Date sinceTime);

    /**
     * 获取用户某试卷的最新重置时间
     */
    Date getLatestResetTime(Long userId, Long paperId);

    /**
     * 查询重置记录列表（管理后台用）
     */
    List<AppUserPaperReset> list(AppUserPaperReset query);

    /**
     * 删除重置记录
     */
    int delete(Long id);
}
