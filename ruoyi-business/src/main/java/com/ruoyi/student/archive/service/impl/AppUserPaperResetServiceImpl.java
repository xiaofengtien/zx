package com.ruoyi.student.archive.service.impl;

import com.ruoyi.student.archive.domain.AppUserPaperReset;
import com.ruoyi.student.archive.mapper.AppUserPaperResetMapper;
import com.ruoyi.student.archive.service.IAppUserPaperResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.List;

/**
 * 用户试卷练习次数重置Service实现
 */
@Service
public class AppUserPaperResetServiceImpl implements IAppUserPaperResetService {

    @Autowired
    private AppUserPaperResetMapper resetMapper;

    @Override
    public void resetUserPaper(Long userId, Long paperId, String resetBy, String remark) {
        AppUserPaperReset reset = new AppUserPaperReset();
        reset.setUserId(userId);
        reset.setPaperId(paperId);
        reset.setResetTime(new Date());
        reset.setResetBy(resetBy);
        reset.setRemark(remark);
        resetMapper.insert(reset);
    }

    @Override
    public List<AppUserPaperReset> getUserResets(Long userId, Date sinceTime) {
        return resetMapper.selectByUserId(userId, sinceTime);
    }

    @Override
    public Date getLatestResetTime(Long userId, Long paperId) {
        return resetMapper.selectLatestResetTime(userId, paperId);
    }

    @Override
    public List<AppUserPaperReset> list(AppUserPaperReset query) {
        return resetMapper.selectList(query);
    }

    @Override
    public int delete(Long id) {
        return resetMapper.deleteById(id);
    }
}
