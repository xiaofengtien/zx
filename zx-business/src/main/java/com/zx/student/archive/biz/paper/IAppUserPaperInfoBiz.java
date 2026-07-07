package com.zx.student.archive.biz.paper;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zx.common.exception.ServiceException;
import com.zx.student.archive.domain.bo.paper.CreateUserPaperBO;
import com.zx.student.archive.domain.dto.paper.UserPaperDTO;
import com.zx.student.archive.domain.paper.AppUserPaperInfo;

/**
 * 用户试卷信息业务接口
 */
public interface IAppUserPaperInfoBiz extends IService<AppUserPaperInfo> {
    
    /**
     * 创建试卷
     *
     * @param createBO 创建试卷参数
     * @return 试卷信息
     */
    UserPaperDTO createPaper(CreateUserPaperBO createBO) throws ServiceException;

    /**
     * 获取试卷信息
     *
     * @param paperId 试卷ID
     * @return 试卷信息
     */
    UserPaperDTO getPaperInfo(Integer paperId) throws ServiceException;
} 