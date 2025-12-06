package com.ruoyi.student.archive.service.paper.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.student.archive.biz.paper.IAppUserPaperInfoBiz;
import com.ruoyi.student.archive.biz.paper.IPaperBiz;
import com.ruoyi.student.archive.domain.StudentArchive;
import com.ruoyi.student.archive.domain.bo.paper.PaperResultPageBO;
import com.ruoyi.student.archive.domain.bo.paper.QueryPaperResultBO;
import com.ruoyi.student.archive.domain.dto.paper.PaperQuestionResultDTO;
import com.ruoyi.student.archive.domain.dto.paper.PaperResultListDTO;
import com.ruoyi.student.archive.domain.paper.AppUserPaperInfo;
import com.ruoyi.student.archive.domain.paper.Paper;
import com.ruoyi.student.archive.mapper.StudentArchiveMapper;
import com.ruoyi.student.archive.service.paper.IAppUserPaperService;
import com.ruoyi.student.archive.service.paper.IPaperResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 答题结果服务实现类
 * 
 * @author ruoyi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperResultServiceImpl implements IPaperResultService {

    private final IAppUserPaperInfoBiz appUserPaperInfoBiz;
    private final IAppUserPaperService appUserPaperService;
    private final IPaperBiz paperBiz;
    private final StudentArchiveMapper studentArchiveMapper;

    @Override
    public Page<PaperResultListDTO> pageList(PaperResultPageBO pageBO) {
        // 构建查询条件
        LambdaQueryWrapper<AppUserPaperInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppUserPaperInfo::getDelFlag, "0"); // 未删除
        
        if (StringUtils.isNotEmpty(pageBO.getPaperName())) {
            wrapper.like(AppUserPaperInfo::getPaperName, pageBO.getPaperName());
        }
        
        // 优先使用学号（studentAccount）查询，如果没有则使用appUserId
        if (StringUtils.isNotEmpty(pageBO.getStudentAccount())) {
            // 根据学号查询学员档案ID
            StudentArchive archive = studentArchiveMapper.selectStudentArchiveByStudentAccount(pageBO.getStudentAccount());
            if (archive != null) {
                wrapper.eq(AppUserPaperInfo::getAppUserId, archive.getId().intValue());
            } else {
                // 如果找不到对应的学员档案，返回空结果
                wrapper.eq(AppUserPaperInfo::getAppUserId, -1);
            }
        } else if (pageBO.getAppUserId() != null) {
            // 兼容旧的查询方式（直接使用appUserId）
            wrapper.eq(AppUserPaperInfo::getAppUserId, pageBO.getAppUserId());
        }
        
        if (pageBO.getSubmitStatus() != null) {
            wrapper.eq(AppUserPaperInfo::getIsSubmit, pageBO.getSubmitStatus());
        }
        
        wrapper.orderByDesc(AppUserPaperInfo::getSubmitTime);
        
        // 分页查询（兼容前端pageNum/pageSize参数）
        long current = 1L;
        long size = 10L;
        if (pageBO.getPageNum() != null && pageBO.getPageNum() > 0) {
            current = pageBO.getPageNum().longValue();
        } else if (pageBO.getCurrent() != null && pageBO.getCurrent() > 0) {
            current = pageBO.getCurrent();
        }
        if (pageBO.getPageSize() != null && pageBO.getPageSize() > 0) {
            size = pageBO.getPageSize().longValue();
        } else if (pageBO.getSize() != null && pageBO.getSize() > 0) {
            size = pageBO.getSize();
        }
        Page<AppUserPaperInfo> page = new Page<>(current, size);
        Page<AppUserPaperInfo> paperInfoPage = appUserPaperInfoBiz.page(page, wrapper);
        
        // 转换为DTO
        Page<PaperResultListDTO> dtoPage = new Page<>(paperInfoPage.getCurrent(), paperInfoPage.getSize(), paperInfoPage.getTotal());
        
        // 批量查询学员档案，减少数据库访问次数
        List<AppUserPaperInfo> paperInfoList = paperInfoPage.getRecords();
        List<Integer> archiveIds = paperInfoList.stream()
            .map(AppUserPaperInfo::getAppUserId)
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toList());
        
        // 批量查询学员档案（优化：使用MyBatis-Plus的批量查询）
        java.util.Map<Integer, StudentArchive> archiveMap = new java.util.HashMap<>();
        if (!archiveIds.isEmpty()) {
            // 使用MyBatis-Plus的selectBatchIds方法进行批量查询
            List<Long> longIds = archiveIds.stream()
                .map(Integer::longValue)
                .collect(Collectors.toList());
            List<StudentArchive> archives = longIds.stream()
                .map(id -> studentArchiveMapper.selectStudentArchiveById(id))
                .filter(archive -> archive != null)
                .collect(Collectors.toList());
            archiveMap = archives.stream()
                .collect(Collectors.toMap(archive -> archive.getId().intValue(), archive -> archive));
        }
        
        // 转换为DTO，使用缓存的学员档案信息
        final java.util.Map<Integer, StudentArchive> finalArchiveMap = archiveMap;
        List<PaperResultListDTO> dtoList = paperInfoList.stream()
            .map(paperInfo -> convertToDTO(paperInfo, finalArchiveMap))
            .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }

    @Override
    public PaperQuestionResultDTO getPaperResultDetail(Integer id) throws ServiceException {
        if (id == null) {
            throw new ServiceException("答题记录ID不能为空");
        }
        
        AppUserPaperInfo paperInfo = appUserPaperInfoBiz.getById(id);
        if (paperInfo == null || "2".equals(paperInfo.getDelFlag())) {
            throw new ServiceException("答题记录不存在");
        }
        
        // 使用现有的服务获取答题结果详情
        QueryPaperResultBO queryBO = new QueryPaperResultBO();
        queryBO.setPaperId(paperInfo.getPaperId());
        queryBO.setAppUserId(paperInfo.getAppUserId());
        queryBO.setBusinessType(paperInfo.getBusinessType());
        queryBO.setBusinessId(paperInfo.getBusinessId());
        
        PaperQuestionResultDTO resultDTO = appUserPaperService.getPaperQuestionResult(queryBO);
        
        // 补充试卷基本信息（用于前端显示）
        if (resultDTO != null) {
            // 通过反射或扩展DTO来添加这些字段
            // 由于PaperQuestionResultDTO没有这些字段，我们需要创建一个扩展的DTO
            // 暂时先返回resultDTO，前端可以通过其他方式获取这些信息
        }
        
        return resultDTO;
    }

    /**
     * 转换为DTO（使用缓存的学员档案信息）
     */
    private PaperResultListDTO convertToDTO(AppUserPaperInfo paperInfo, java.util.Map<Integer, StudentArchive> archiveMap) {
        PaperResultListDTO dto = new PaperResultListDTO();
        BeanUtils.copyProperties(paperInfo, dto);
        
        // 设置提交状态
        dto.setSubmitStatus(paperInfo.getIsSubmit());
        
        // 通过paperId查询Paper表获取paperCode
        if (paperInfo.getPaperId() != null) {
            Paper paper = paperBiz.getById(paperInfo.getPaperId());
            if (paper != null) {
                dto.setPaperCode(paper.getPaperCode());
            }
        }
        
        // 通过缓存的学员档案信息获取学号（studentAccount）
        if (paperInfo.getAppUserId() != null && archiveMap != null) {
            StudentArchive archive = archiveMap.get(paperInfo.getAppUserId());
            if (archive != null) {
                dto.setStudentAccount(archive.getStudentAccount());
            }
        }
        
        // 计算正确率
        if (paperInfo.getTotalScore() != null && paperInfo.getTotalScore().compareTo(BigDecimal.ZERO) > 0 
            && paperInfo.getUserScore() != null) {
            BigDecimal correctRate = paperInfo.getUserScore()
                .divide(paperInfo.getTotalScore(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
            dto.setCorrectRate(correctRate.intValue());
        } else {
            dto.setCorrectRate(0);
        }
        
        // 设置得分
        dto.setScore(paperInfo.getUserScore() != null ? paperInfo.getUserScore() : BigDecimal.ZERO);
        
        return dto;
    }
    
    /**
     * 转换为DTO（兼容旧方法，用于单个对象转换）
     */
    private PaperResultListDTO convertToDTO(AppUserPaperInfo paperInfo) {
        return convertToDTO(paperInfo, null);
    }
}

