package com.zx.student.archive.biz.paper.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zx.common.constant.AppErrorCode;
import com.zx.common.exception.ServiceException;
import com.zx.student.archive.biz.paper.IAppUserPaperInfoBiz;
import com.zx.student.archive.domain.bo.paper.CreateUserPaperBO;
import com.zx.student.archive.domain.dto.paper.UserPaperDTO;
import com.zx.student.archive.domain.paper.AppUserPaperInfo;
import com.zx.student.archive.mapper.paper.AppUserPaperInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 用户试卷信息业务实现类
 */
@Slf4j
@Service
public class AppUserPaperInfoBizImpl  extends ServiceImpl<AppUserPaperInfoMapper, AppUserPaperInfo> implements IAppUserPaperInfoBiz {

    @Resource
    private AppUserPaperInfoMapper paperInfoMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserPaperDTO createPaper(CreateUserPaperBO createBO) throws ServiceException {

        try {
            AppUserPaperInfo paperInfo = new AppUserPaperInfo();
            BeanUtil.copyProperties(createBO, paperInfo);
            paperInfoMapper.insert(paperInfo);
            UserPaperDTO paperDTO = new UserPaperDTO();
            BeanUtil.copyProperties(paperInfo, paperDTO);
            return paperDTO;
        } catch (Exception e) {
            log.error("创建试卷失败", e);
            throw new ServiceException(AppErrorCode.APP_CREATE_PAPER_FAIL_MSG);
        }
    }

    @Override
    public UserPaperDTO getPaperInfo(Integer paperId) throws ServiceException {
        if (paperId == null) {
            return null;
        }

        try {
            AppUserPaperInfo paperInfo = paperInfoMapper.selectById(paperId);
            if (paperInfo == null) {
                throw new ServiceException(AppErrorCode.APP_PAPER_NOT_EXIST_MSG);
            }
            UserPaperDTO paperDTO = new UserPaperDTO();
            BeanUtil.copyProperties(paperInfo, paperDTO);
            return paperDTO;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取试卷信息失败", e);
            throw new ServiceException(AppErrorCode.APP_GET_PAPER_INFO_ERROR_MSG);
        }
    }

} 