package com.ruoyi.student.archive.service.paper.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.oss.exam.question.OssUtil;
import com.ruoyi.student.archive.biz.paper.IPaperBiz;
import com.ruoyi.student.archive.biz.paper.IPaperQuestionBiz;
import com.ruoyi.student.archive.domain.bo.paper.*;
import com.ruoyi.student.archive.domain.dto.paper.PaperDTO;
import com.ruoyi.student.archive.domain.dto.paper.PaperQuestionDTO;
import com.ruoyi.student.archive.domain.dto.paper.PaperVolumeDTO;
import com.ruoyi.student.archive.domain.dto.paper.PaperSectionDTO;
import com.ruoyi.student.archive.domain.paper.Paper;
import com.ruoyi.student.archive.domain.paper.PaperQuestion;
import com.ruoyi.student.archive.domain.paper.PaperVolume;
import com.ruoyi.student.archive.domain.paper.PaperSection;
import com.ruoyi.student.archive.domain.paper.PaperIntermission;
import com.ruoyi.student.archive.service.paper.IPaperService;
import com.ruoyi.student.archive.service.paper.IPaperVolumeService;
import com.ruoyi.student.archive.service.paper.IPaperSectionService;
import com.ruoyi.student.archive.service.paper.IPaperIntermissionService;
import com.ruoyi.student.archive.service.paper.IPackageTaskService;
import com.ruoyi.student.archive.service.question.QuestionService;
import com.ruoyi.student.archive.domain.bo.question.QuestionIdBO;
import com.ruoyi.student.archive.domain.dto.question.QuestionInfoDTO;
import com.ruoyi.student.archive.utils.PaperCodeUtils;
import com.ruoyi.common.enums.PackageTaskStatus;
import com.ruoyi.system.service.ISysDictDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Objects;

/**
 * 试卷服务实现类
 * 
 * @author ruoyi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperServiceImpl implements IPaperService {

    private final IPaperBiz paperBiz;
    private final IPaperQuestionBiz paperQuestionBiz;
    private final PaperPackageService paperPackageService;
    private final OssUtil ossUtil;
    private final ISysDictDataService dictDataService;
    private final IPaperVolumeService volumeService;
    private final IPaperSectionService sectionService;
    private final IPaperIntermissionService intermissionService;
    private final QuestionService questionService;
    private final IPackageTaskService packageTaskService;
    private final ApplicationContext applicationContext;

    @Override
    public Page<PaperDTO> pageList(PaperPageBO pageBO) {
        // 构建查询条件
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Paper::getDelFlag, "0"); // 未删除

        if (StringUtils.isNotEmpty(pageBO.getPaperName())) {
            wrapper.like(Paper::getPaperName, pageBO.getPaperName());
        }
        if (StringUtils.isNotEmpty(pageBO.getPaperCode())) {
            wrapper.eq(Paper::getPaperCode, pageBO.getPaperCode());
        }
        if (pageBO.getBusinessType() != null) {
            wrapper.eq(Paper::getBusinessType, pageBO.getBusinessType());
        }
        if (pageBO.getBusinessId() != null) {
            wrapper.eq(Paper::getBusinessId, pageBO.getBusinessId());
        }
        if (pageBO.getStatus() != null) {
            wrapper.eq(Paper::getStatus, pageBO.getStatus());
        }
        if (StringUtils.isNotEmpty(pageBO.getPaperType())) {
            wrapper.eq(Paper::getPaperType, pageBO.getPaperType());
        }

        wrapper.orderByDesc(Paper::getCreateTime);

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
        Page<Paper> page = new Page<>(current, size);
        Page<Paper> paperPage = paperBiz.page(page, wrapper);

        // 转换为DTO
        Page<PaperDTO> dtoPage = new Page<>(paperPage.getCurrent(), paperPage.getSize(), paperPage.getTotal());
        List<PaperDTO> dtoList = paperPage.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }

    @Override
    public PaperDTO getPaperById(Integer id) throws ServiceException {
        if (id == null) {
            throw new ServiceException("试卷ID不能为空");
        }

        Paper paper = paperBiz.getById(id);
        if (paper == null || "2".equals(paper.getDelFlag())) {
            throw new ServiceException("试卷不存在");
        }

        return convertToDTO(paper);
    }

    @Override
    public PaperDTO getPaperByCode(String paperCode) throws ServiceException {
        if (StringUtils.isEmpty(paperCode)) {
            throw new ServiceException("试卷编码不能为空");
        }

        Paper paper = paperBiz.getByPaperCode(paperCode);
        if (paper == null || "2".equals(paper.getDelFlag())) {
            throw new ServiceException("试卷不存在");
        }

        return convertToDTO(paper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createPaper(PaperCreateBO createBO) throws ServiceException {
        // 参数校验
        if (StringUtils.isEmpty(createBO.getPaperType())) {
            throw new ServiceException("试卷类型不能为空");
        }
        if (createBO.getYear() == null) {
            throw new ServiceException("年份不能为空");
        }
        if (StringUtils.isEmpty(createBO.getProvince())) {
            throw new ServiceException("省份不能为空");
        }

        // 验证年份范围
        if (createBO.getYear() < 2000 || createBO.getYear() > 2050) {
            throw new ServiceException("年份必须在2000-2050之间");
        }

        // 验证月份范围（如果提供了月份）
        if (createBO.getMonth() != null && (createBO.getMonth() < 1 || createBO.getMonth() > 12)) {
            throw new ServiceException("月份必须在1-12之间");
        }

        // 注意：不再检查唯一性，允许同一 (year, month, province, paper_type) 组合创建多条记录
        // 通过 custom_name 区分不同的试卷（如：模拟一、模拟二等）
        // 客户端同步时会选择该组合中版本最新的记录

        // 生成paper_name（只包含有值的字段）
        String paperName = generatePaperName(
                createBO.getYear(),
                createBO.getMonth(),
                createBO.getProvince(),
                createBO.getPaperType(),
                createBO.getCustomName());

        // 生成paper_code（简化格式：PAPER_YYYYMMDD_序号，仅用于同步标识）
        String paperCode = generateSimplePaperCode();

        // 转换为实体
        Paper paper = new Paper();
        BeanUtils.copyProperties(createBO, paper);
        paper.setPaperCode(paperCode);
        paper.setPaperName(paperName);

        log.info("创建试卷 - paperCode: {}, paperName: {}, paperType: {}", paperCode, paperName, createBO.getPaperType());

        // 设置默认值
        if (createBO.getBusinessType() == null) {
            paper.setBusinessType(5); // 默认业务类型为5（题库）
        }
        if (createBO.getStatus() == null) {
            paper.setStatus(1); // 默认启用
        }
        if (createBO.getAutoNextQuestion() == null) {
            paper.setAutoNextQuestion(1); // 默认自动跳转
        }
        if (createBO.getShowAnswerImmediately() == null) {
            paper.setShowAnswerImmediately(0); // 默认不立即显示答案
        }
        if (createBO.getAllowReview() == null) {
            paper.setAllowReview(1); // 默认允许回顾
        }

        // 创建试卷
        Integer paperId = paperBiz.createPaper(paper);

        // 关联题目
        if (!CollectionUtils.isEmpty(createBO.getQuestionIds())) {
            savePaperQuestions(paperId, createBO.getQuestionIds(), createBO.getScores());
            // 更新题目总数
            updateTotalQuestions(paperId);
        } else {
            // 如果没有题目，设置题目总数为0
            Paper updatePaper = new Paper();
            updatePaper.setId(paperId);
            updatePaper.setTotalQuestions(0);
            paperBiz.updateById(updatePaper);
        }

        return paperId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePaper(PaperUpdateBO updateBO) throws ServiceException {
        if (updateBO.getId() == null) {
            throw new ServiceException("试卷ID不能为空");
        }

        // 检查试卷是否存在
        Paper existPaper = paperBiz.getById(updateBO.getId());
        if (existPaper == null || "2".equals(existPaper.getDelFlag())) {
            throw new ServiceException("试卷不存在");
        }

        // 参数校验
        if (StringUtils.isEmpty(updateBO.getPaperType())) {
            throw new ServiceException("试卷类型不能为空");
        }
        if (updateBO.getYear() == null) {
            throw new ServiceException("年份不能为空");
        }
        if (updateBO.getMonth() == null) {
            throw new ServiceException("月份不能为空");
        }
        if (StringUtils.isEmpty(updateBO.getProvince())) {
            throw new ServiceException("省份不能为空");
        }

        // 验证年份范围
        if (updateBO.getYear() < 2000 || updateBO.getYear() > 2050) {
            throw new ServiceException("年份必须在2000-2050之间");
        }

        // 验证月份范围
        if (updateBO.getMonth() < 1 || updateBO.getMonth() > 12) {
            throw new ServiceException("月份必须在1-12之间");
        }

        // 生成新的paper_code（简化格式：PAPER_YYYYMMDD_序号，仅用于同步标识）
        // 注意：paper_code 不再用于唯一性检查，唯一性检查使用 (year, month, province, paper_type) 组合
        // 更新时保持原有的 paper_code 不变（除非没有 paper_code）
        String newPaperCode = existPaper.getPaperCode();
        if (StringUtils.isEmpty(newPaperCode)) {
            // 如果没有 paper_code，生成新的
            newPaperCode = generateSimplePaperCode();
            // 检查是否与其他试卷冲突（最多重试3次）
            int retryCount = 0;
            while (retryCount < 3) {
                Paper conflictPaper = paperBiz.getByPaperCode(newPaperCode);
                if (conflictPaper == null || conflictPaper.getId().equals(updateBO.getId())
                        || "2".equals(conflictPaper.getDelFlag())) {
                    break;
                }
                newPaperCode = generateSimplePaperCode();
                retryCount++;
            }
        }

        // 生成新的paper_name
        String newPaperName = generatePaperName(
                updateBO.getYear(),
                updateBO.getMonth(),
                updateBO.getProvince(),
                updateBO.getPaperType(),
                updateBO.getCustomName());

        // 转换为实体
        Paper paper = new Paper();
        BeanUtils.copyProperties(updateBO, paper);
        paper.setPaperCode(newPaperCode);
        paper.setPaperName(newPaperName);

        log.info("更新试卷 - paperCode: {}, paperName: {}, paperType: {}", newPaperCode, newPaperName,
                updateBO.getPaperType());

        // 更新试卷
        paperBiz.updatePaper(paper);

        // 更新题目关联（如果提供了题目列表）
        if (updateBO.getQuestionIds() != null) {
            // 删除原有关联
            paperQuestionBiz.deleteByPaperId(updateBO.getId());
            // 添加新关联
            if (!updateBO.getQuestionIds().isEmpty()) {
                savePaperQuestions(updateBO.getId(), updateBO.getQuestionIds(), updateBO.getScores());
            }
            // 更新题目总数
            updateTotalQuestions(updateBO.getId());
            // 同步更新到paper实体
            paper.setTotalQuestions(updateBO.getQuestionIds().size());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeletePaper(List<Integer> ids) throws ServiceException {
        if (CollectionUtils.isEmpty(ids)) {
            throw new ServiceException("试卷ID列表不能为空");
        }

        paperBiz.batchDeletePaper(ids);
    }

    @Override
    public List<PaperDTO> listByBusiness(Integer businessType, Integer businessId) {
        List<Paper> papers = paperBiz.listByBusiness(businessType, businessId);
        return papers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaperDTO> listByIds(List<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }

        // 使用LambdaQueryWrapper查询多个ID
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Paper::getId, ids);
        wrapper.eq(Paper::getDelFlag, "0"); // 未删除
        wrapper.orderByDesc(Paper::getCreateTime);

        List<Paper> papers = paperBiz.list(wrapper);
        return papers.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void generatePaperPackage(Integer paperId) throws ServiceException {
        if (paperId == null) {
            throw new ServiceException("试卷ID不能为空");
        }

        // 快速检查试卷是否存在（不查询完整数据，只检查ID和删除标记）
        Paper paper = paperBiz.getById(paperId);
        if (paper == null || "2".equals(paper.getDelFlag())) {
            throw new ServiceException("试卷不存在");
        }

        // 创建任务（如果任务已存在会抛出异常）
        // 注意：这里只做快速检查，不阻塞
        packageTaskService.createTask(paperId);

        // 异步执行生成任务（立即返回，不等待）
        // 注意：必须通过代理对象调用，否则 @Async 注解不会生效
        PaperServiceImpl proxy = applicationContext.getBean(PaperServiceImpl.class);
        proxy.executeGeneratePackageAsync(paperId);
        
        // 方法立即返回，不等待异步任务完成
    }

    /**
     * 异步执行试卷包生成任务
     * 
     * @param paperId 试卷ID
     */
    @Async("packageTaskExecutor")
    public void executeGeneratePackageAsync(Integer paperId) {
        // 获取当前线程并保存到任务信息中（用于取消）
        Thread currentThread = Thread.currentThread();
        com.ruoyi.student.archive.domain.dto.paper.PackageTaskInfo taskInfo = packageTaskService.getTask(paperId);
        if (taskInfo != null) {
            taskInfo.setTaskThread(currentThread);
            // 保存线程引用到任务服务（用于取消功能）
            packageTaskService.saveTask(paperId, taskInfo);
        }

        try {
            // 检查是否被取消
            checkInterrupted(paperId);

            // 1. 查询试卷信息（5%）
            packageTaskService.updateProgress(paperId, 5, "正在查询试卷信息...");
            Paper paper = paperBiz.getById(paperId);
            if (paper == null || "2".equals(paper.getDelFlag())) {
                throw new ServiceException("试卷不存在");
            }
            checkInterrupted(paperId);

            // 2. 查询卷别列表（10%）
            packageTaskService.updateProgress(paperId, 10, "正在查询卷别列表...");
            checkInterrupted(paperId);

            // 3. 查询大题列表（15%）
            packageTaskService.updateProgress(paperId, 15, "正在查询大题列表...");
            checkInterrupted(paperId);

            // 4. 查询题目列表（20%）
            packageTaskService.updateProgress(paperId, 20, "正在查询题目列表...");
            checkInterrupted(paperId);

            // 5. 生成快速启动包（20-40%）
            packageTaskService.updateProgress(paperId, 25, "正在生成快速启动包...");
            byte[] quickStartBytes = paperPackageService.generateQuickStartPackage(paper);
            checkInterrupted(paperId);

            // 6. 生成完整ZIP包（40-60%）
            packageTaskService.updateProgress(paperId, 40, "正在生成完整试卷包...");
            byte[] zipBytes = paperPackageService.generatePaperPackage(paper);
            checkInterrupted(paperId);

            // 7. 计算哈希值和大小（60-70%）
            packageTaskService.updateProgress(paperId, 60, "正在计算文件哈希值...");
            String quickStartHash = paperPackageService.calculateHash(quickStartBytes);
            Long quickStartSize = (long) quickStartBytes.length;
            String packageHash = paperPackageService.calculateHash(zipBytes);
            Long packageSize = (long) zipBytes.length;
            checkInterrupted(paperId);

            // 8. 上传快速启动包到OSS（70-80%）
            packageTaskService.updateProgress(paperId, 70, "正在上传快速启动包到OSS...");
            Integer newVersion = (paper.getVersion() != null ? paper.getVersion() : 0) + 1;
            String quickStartFileName = buildQuickStartPackageFileName(paper.getPaperCode(), newVersion);
            
            log.info("准备上传快速启动包到OSS，试卷ID：{}，文件名：{}，版本：{}，文件大小：{} MB", 
                    paperId, quickStartFileName, newVersion, quickStartSize / 1024.0 / 1024.0);

            String quickStartUrl;
            try {
                java.io.InputStream quickStartInputStream = new java.io.ByteArrayInputStream(quickStartBytes);
                com.ruoyi.common.utils.oss.IOssService.UploadResult quickStartUploadResult = ossUtil.uploadMultipartWithResume(
                        quickStartInputStream,
                        quickStartFileName,
                        "application/zip",
                        quickStartSize,
                        null, // 快速启动包通常较小，不需要分片
                        null,
                        null,
                        (uploaded, total) -> {
                            if (total > 0) {
                                int uploadProgress = (int) (70 + (uploaded * 5.0 / total)); // 70-75%用于快速启动包上传
                                packageTaskService.updateProgress(paperId, uploadProgress, 
                                        String.format("正在上传快速启动包... (%d KB / %d KB)", 
                                                uploaded / 1024, total / 1024));
                            }
                        });
                quickStartUrl = quickStartUploadResult.getFileUrl();
                log.info("快速启动包上传成功，试卷ID：{}，URL：{}", paperId, quickStartUrl);
            } catch (Exception e) {
                log.error("快速启动包上传到OSS失败，试卷ID：{}，文件名：{}", paperId, quickStartFileName, e);
                throw new ServiceException("快速启动包上传失败：" + e.getMessage());
            }
            checkInterrupted(paperId);

            // 9. 上传完整ZIP包到OSS（80-90%）
            packageTaskService.updateProgress(paperId, 80, "正在上传完整试卷包到OSS...");
            String packageFileName = buildPackageFileName(paper.getPaperCode(), newVersion);

            log.info("准备上传完整试卷包到OSS，试卷ID：{}，文件名：{}，版本：{}，文件大小：{} MB", 
                    paperId, packageFileName, newVersion, packageSize / 1024.0 / 1024.0);

            String packageUrl;
            try {
                // 检查是否有未完成的上传任务（断点续传）
                // 注意：taskInfo 已在方法开始处定义，这里重新获取最新的任务信息
                taskInfo = packageTaskService.getTask(paperId);
                String existingUploadId = null;
                java.util.List<com.ruoyi.common.utils.oss.IOssService.PartInfo> existingUploadedParts = null;
                Long existingChunkSize = null;
                boolean isResume = false;
                
                if (taskInfo != null && taskInfo.getUploadId() != null && !taskInfo.getUploadId().isEmpty() 
                        && taskInfo.getObjectKey() != null && taskInfo.getObjectKey().equals(packageFileName)) {
                    // 检查上传状态是否有效（确保是同一个文件）
                    try {
                        // 查询OSS上已上传的分片
                        java.util.List<com.ruoyi.common.utils.oss.IOssService.PartInfo> ossParts = 
                                ossUtil.listUploadedParts(packageFileName, taskInfo.getUploadId());
                        if (ossParts != null && !ossParts.isEmpty()) {
                            // 有未完成的上传，使用断点续传
                            existingUploadId = taskInfo.getUploadId();
                            existingUploadedParts = ossParts;
                            existingChunkSize = taskInfo.getChunkSize();
                            isResume = true;
                            log.info("检测到未完成的上传任务，使用断点续传。试卷ID：{}，UploadId：{}，已上传分片数：{}", 
                                    paperId, existingUploadId, existingUploadedParts.size());
                        } else {
                            // OSS上没有分片，清除本地状态，重新上传
                            log.warn("OSS上未找到已上传的分片，清除本地状态，重新上传。试卷ID：{}，UploadId：{}", 
                                    paperId, taskInfo.getUploadId());
                            taskInfo.setUploadId(null);
                            taskInfo.setObjectKey(null);
                            taskInfo.setUploadedParts(null);
                            taskInfo.setFileSize(null);
                            taskInfo.setChunkSize(null);
                            packageTaskService.saveTask(paperId, taskInfo);
                        }
                    } catch (Exception resumeCheckEx) {
                        log.warn("检查断点续传状态失败，重新上传。试卷ID：{}，错误：{}", paperId, resumeCheckEx.getMessage());
                        // 清除状态，重新上传
                        taskInfo.setUploadId(null);
                        taskInfo.setObjectKey(null);
                        taskInfo.setUploadedParts(null);
                        taskInfo.setFileSize(null);
                        taskInfo.setChunkSize(null);
                        packageTaskService.saveTask(paperId, taskInfo);
                    }
                } else if (taskInfo != null && taskInfo.getUploadId() != null && !taskInfo.getUploadId().isEmpty()) {
                    // 有uploadId但objectKey不匹配，说明是不同文件的上传，清除状态
                    log.warn("检测到不同文件的上传任务，清除旧状态，重新上传。试卷ID：{}，旧objectKey：{}，新objectKey：{}", 
                            paperId, taskInfo.getObjectKey(), packageFileName);
                    taskInfo.setUploadId(null);
                    taskInfo.setObjectKey(null);
                    taskInfo.setUploadedParts(null);
                    taskInfo.setFileSize(null);
                    taskInfo.setChunkSize(null);
                    packageTaskService.saveTask(paperId, taskInfo);
                }
                
                // 使用分片上传（支持断点续传）
                java.io.InputStream zipInputStream = new java.io.ByteArrayInputStream(zipBytes);
                // 分片大小设为null，让OSS服务根据文件大小自动优化（如果续传，使用之前的分片大小）
                Long chunkSize = isResume ? existingChunkSize : null;
                
                // 进度回调：更新任务进度，并保存上传状态
                java.util.function.BiConsumer<Long, Long> progressCallback = (uploaded, total) -> {
                    if (total > 0) {
                        int uploadProgress = (int) (80 + (uploaded * 10.0 / total)); // 80-90%用于完整包上传
                        String stepMsg = String.format("正在上传完整试卷包到OSS... (%d MB / %d MB)", 
                                uploaded / 1024 / 1024, total / 1024 / 1024);
                        packageTaskService.updateProgress(paperId, uploadProgress, stepMsg);
                    }
                };
                
                // 使用支持断点续传的上传方法
                com.ruoyi.common.utils.oss.IOssService.UploadResult uploadResult;
                try {
                    uploadResult = ossUtil.uploadMultipartWithResume(
                            zipInputStream,
                            packageFileName,
                            "application/zip",
                            packageSize,
                            chunkSize,
                            existingUploadId,
                            existingUploadedParts,
                            progressCallback);
                    
                    packageUrl = uploadResult.getFileUrl();
                    
                    // 上传成功，保存上传状态（用于后续可能的续传，直到任务完成）
                    if (taskInfo != null) {
                        taskInfo.setUploadId(uploadResult.getUploadId());
                        taskInfo.setObjectKey(packageFileName);
                        // 将已上传分片列表转换为JSON字符串保存
                        if (uploadResult.getUploadedParts() != null && !uploadResult.getUploadedParts().isEmpty()) {
                            java.util.List<java.util.Map<String, Object>> partsJson = new java.util.ArrayList<>();
                            for (com.ruoyi.common.utils.oss.IOssService.PartInfo part : uploadResult.getUploadedParts()) {
                                java.util.Map<String, Object> partMap = new java.util.HashMap<>();
                                partMap.put("partNumber", part.getPartNumber());
                                partMap.put("eTag", part.getETag());
                                partMap.put("size", part.getSize());
                                partsJson.add(partMap);
                            }
                            taskInfo.setUploadedParts(com.alibaba.fastjson2.JSON.toJSONString(partsJson));
                        }
                        taskInfo.setFileSize(packageSize);
                        taskInfo.setChunkSize(chunkSize != null ? chunkSize : 
                                (packageSize < 50L * 1024 * 1024 ? 10L * 1024 * 1024 : 
                                 packageSize < 200L * 1024 * 1024 ? 20L * 1024 * 1024 :
                                 packageSize < 500L * 1024 * 1024 ? 50L * 1024 * 1024 : 100L * 1024 * 1024));
                        packageTaskService.saveTask(paperId, taskInfo);
                    }
                } catch (Exception uploadEx) {
                    // 上传失败，保存当前状态以便续传
                    if (taskInfo != null && existingUploadId != null) {
                        // 保留已有的uploadId，以便后续续传
                        taskInfo.setUploadId(existingUploadId);
                        taskInfo.setObjectKey(packageFileName);
                        taskInfo.setFileSize(packageSize);
                        taskInfo.setChunkSize(chunkSize != null ? chunkSize : 
                                (packageSize < 50L * 1024 * 1024 ? 10L * 1024 * 1024 : 
                                 packageSize < 200L * 1024 * 1024 ? 20L * 1024 * 1024 :
                                 packageSize < 500L * 1024 * 1024 ? 50L * 1024 * 1024 : 100L * 1024 * 1024));
                        // 保存已上传的分片（如果有）
                        if (existingUploadedParts != null && !existingUploadedParts.isEmpty()) {
                            java.util.List<java.util.Map<String, Object>> partsJson = new java.util.ArrayList<>();
                            for (com.ruoyi.common.utils.oss.IOssService.PartInfo part : existingUploadedParts) {
                                java.util.Map<String, Object> partMap = new java.util.HashMap<>();
                                partMap.put("partNumber", part.getPartNumber());
                                partMap.put("eTag", part.getETag());
                                partMap.put("size", part.getSize());
                                partsJson.add(partMap);
                            }
                            taskInfo.setUploadedParts(com.alibaba.fastjson2.JSON.toJSONString(partsJson));
                        }
                        packageTaskService.saveTask(paperId, taskInfo);
                    }
                    throw uploadEx;
                }
                
                log.info("试卷包已上传到OSS，试卷ID：{}，URL：{}，是否续传：{}", 
                        paperId, packageUrl, isResume);
                packageTaskService.updateProgress(paperId, 85, "ZIP包上传完成，正在验证...");
            } catch (Exception e) {
                log.error("试卷包上传到OSS失败，试卷ID：{}，文件名：{}", paperId, packageFileName, e);
                // 上传失败时不清除上传状态，允许后续续传
                // 但需要更新错误信息和保存当前上传状态
                // 注意：taskInfo 已在 try 块中定义，这里直接使用或重新获取
                com.ruoyi.student.archive.domain.dto.paper.PackageTaskInfo errorTaskInfo = packageTaskService.getTask(paperId);
                if (errorTaskInfo != null) {
                    errorTaskInfo.setErrorMessage("上传失败：" + e.getMessage());
                    // 如果使用了断点续传，保存当前的上传状态（uploadId等）
                    // 注意：这里不设置uploadId，因为上传失败时uploadId可能无效
                    // 但如果之前有uploadId，保留它以便后续续传
                    packageTaskService.saveTask(paperId, errorTaskInfo);
                }
                throw new ServiceException("试卷包上传失败：" + e.getMessage());
            }
            checkInterrupted(paperId);

            // 8. 验证文件是否真的存在于OSS（90%）
            packageTaskService.updateProgress(paperId, 90, "正在验证文件...");
            try {
                boolean exists = ossUtil.exists(packageFileName);
                if (!exists) {
                    log.error("上传后验证失败，文件不存在于OSS，试卷ID：{}，文件名：{}", paperId, packageFileName);
                    throw new ServiceException("上传后验证失败，文件不存在于OSS");
                }
                log.info("上传后验证成功，文件存在于OSS，试卷ID：{}，文件名：{}", paperId, packageFileName);
            } catch (ServiceException e) {
                throw e;
            } catch (Exception e) {
                log.warn("上传后验证时出错，但继续执行，试卷ID：{}，文件名：{}", paperId, packageFileName, e);
            }
            checkInterrupted(paperId);

            // 10. 验证快速启动包是否真的存在于OSS（90%）
            packageTaskService.updateProgress(paperId, 90, "正在验证快速启动包...");
            try {
                boolean quickStartExists = ossUtil.exists(quickStartFileName);
                if (!quickStartExists) {
                    log.error("快速启动包上传后验证失败，文件不存在于OSS，试卷ID：{}，文件名：{}", paperId, quickStartFileName);
                    throw new ServiceException("快速启动包上传后验证失败，文件不存在于OSS");
                }
                log.info("快速启动包上传后验证成功，文件存在于OSS，试卷ID：{}，文件名：{}", paperId, quickStartFileName);
            } catch (ServiceException e) {
                throw e;
            } catch (Exception e) {
                log.warn("快速启动包上传后验证时出错，但继续执行，试卷ID：{}，文件名：{}", paperId, quickStartFileName, e);
            }
            checkInterrupted(paperId);

            // 11. 验证完整包是否真的存在于OSS（92%）
            packageTaskService.updateProgress(paperId, 92, "正在验证完整试卷包...");
            try {
                boolean exists = ossUtil.exists(packageFileName);
                if (!exists) {
                    log.error("完整包上传后验证失败，文件不存在于OSS，试卷ID：{}，文件名：{}", paperId, packageFileName);
                    throw new ServiceException("完整包上传后验证失败，文件不存在于OSS");
                }
                log.info("完整包上传后验证成功，文件存在于OSS，试卷ID：{}，文件名：{}", paperId, packageFileName);
            } catch (ServiceException e) {
                throw e;
            } catch (Exception e) {
                log.warn("完整包上传后验证时出错，但继续执行，试卷ID：{}，文件名：{}", paperId, packageFileName, e);
            }
            checkInterrupted(paperId);

            // 12. 更新试卷信息（95%）
            packageTaskService.updateProgress(paperId, 95, "正在更新试卷信息...");
            paper.setVersion(newVersion);
            paper.setPackageHash(packageHash);
            paper.setPackageSize(packageSize);
            paper.setLastPackageTime(new Date());
            paperBiz.updateById(paper);
            checkInterrupted(paperId);

            // 13. 完成（100%）
            packageTaskService.updateSuccess(paperId, newVersion);
            
            // 清除上传状态（任务完成，不再需要续传）
            com.ruoyi.student.archive.domain.dto.paper.PackageTaskInfo finalTaskInfo = packageTaskService.getTask(paperId);
            if (finalTaskInfo != null) {
                finalTaskInfo.setUploadId(null);
                finalTaskInfo.setObjectKey(null);
                finalTaskInfo.setUploadedParts(null);
                finalTaskInfo.setFileSize(null);
                finalTaskInfo.setChunkSize(null);
                packageTaskService.saveTask(paperId, finalTaskInfo);
            }

            log.info("试卷包生成成功，试卷ID：{}，版本：{}，快速启动包：{} 字节（{}），完整包：{} 字节（{}）",
                    paperId, newVersion, quickStartSize, quickStartFileName, packageSize, packageFileName);

        } catch (ServiceException e) {
            packageTaskService.updateFailed(paperId, e.getMessage());
            log.error("试卷包生成失败，试卷ID：{}", paperId, e);
        } catch (InterruptedException e) {
            // 任务被取消
            packageTaskService.updateStatus(paperId, PackageTaskStatus.CANCELLED);
            log.info("试卷包生成任务被取消，试卷ID：{}", paperId);
            Thread.currentThread().interrupt(); // 恢复中断状态
        } catch (Exception e) {
            packageTaskService.updateFailed(paperId, "生成失败：" + e.getMessage());
            log.error("试卷包生成失败，试卷ID：{}", paperId, e);
        }
    }

    /**
     * 检查线程是否被中断（用于取消任务）
     */
    private void checkInterrupted(Integer paperId) throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("任务已被取消");
        }
        // 检查任务状态是否已变为CANCELLED
        com.ruoyi.student.archive.domain.dto.paper.PackageTaskInfo taskInfo = packageTaskService.getTask(paperId);
        if (taskInfo != null && taskInfo.getStatus() == PackageTaskStatus.CANCELLED) {
            throw new InterruptedException("任务已被取消");
        }
    }

    @Override
    /**
     * 下载快速启动包
     * 
     * @param paperId 试卷ID
     * @return 快速启动包字节数组
     * @throws ServiceException 业务异常
     */
    public byte[] downloadQuickStartPackage(Integer paperId) throws ServiceException {
        if (paperId == null) {
            throw new ServiceException("试卷ID不能为空");
        }

        Paper paper = paperBiz.getById(paperId);
        if (paper == null || "2".equals(paper.getDelFlag())) {
            throw new ServiceException("试卷不存在");
        }

        // 检查是否已生成试卷包（快速启动包和完整包一起生成）
        if (StringUtils.isEmpty(paper.getPackageHash()) || paper.getPackageSize() == null) {
            throw new ServiceException("试卷包尚未生成，请先生成试卷包");
        }

        // 从OSS下载快速启动包
        Integer currentVersion = paper.getVersion() != null ? paper.getVersion() : 0;
        String quickStartFileName = buildQuickStartPackageFileName(paper.getPaperCode(), currentVersion);

        try {
            byte[] zipBytes = ossUtil.downloadFileToBytes(quickStartFileName);
            log.info("从OSS下载快速启动包成功，试卷ID：{}，版本：{}，包大小：{} 字节", paperId, currentVersion, zipBytes.length);

            // 验证下载的文件大小
            if (zipBytes.length == 0) {
                log.error("下载的快速启动包为空，试卷ID：{}", paperId);
                throw new ServiceException("下载的快速启动包为空");
            }

            // 验证ZIP文件头
            if (zipBytes.length < 4 || zipBytes[0] != 0x50 || zipBytes[1] != 0x4B) {
                log.error("下载的快速启动包格式不正确，试卷ID：{}，大小：{} 字节", paperId, zipBytes.length);
                throw new ServiceException("快速启动包格式不正确");
            }

            return zipBytes;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("从OSS下载快速启动包失败，试卷ID：{}，文件名：{}", paperId, quickStartFileName, e);
            throw new ServiceException("下载快速启动包失败：" + e.getMessage());
        }
    }

    public byte[] downloadPaperPackage(Integer paperId) throws ServiceException {
        if (paperId == null) {
            throw new ServiceException("试卷ID不能为空");
        }

        Paper paper = paperBiz.getById(paperId);
        if (paper == null || "2".equals(paper.getDelFlag())) {
            throw new ServiceException("试卷不存在");
        }

        // 检查是否已生成试卷包
        if (StringUtils.isEmpty(paper.getPackageHash()) || paper.getPackageSize() == null) {
            throw new ServiceException("试卷包尚未生成，请先生成试卷包");
        }

        // 从OSS下载ZIP包
        // 使用 paper_code 构建路径（与生成时一致）
        Integer currentVersion = paper.getVersion() != null ? paper.getVersion() : 0;

        // 使用 paper_code 作为文件名（唯一编码）
        String packageFileName = buildPackageFileName(paper.getPaperCode(), currentVersion);

        try {
            byte[] zipBytes = ossUtil.downloadFileToBytes(packageFileName);
            log.info("从OSS下载试卷包成功，试卷ID：{}，版本：{}，包大小：{} 字节", paperId, currentVersion, zipBytes.length);

            // 验证下载的文件大小（如果为空，可能是错误响应）
            if (zipBytes.length == 0) {
                log.error("下载的ZIP包为空，可能是错误响应，尝试重新生成");
                // 重新生成试卷包
                generatePaperPackage(paperId);
                // 重新获取试卷信息
                paper = paperBiz.getById(paperId);
                currentVersion = paper.getVersion() != null ? paper.getVersion() : 0;
                packageFileName = buildPackageFileName(paper.getPaperCode(), currentVersion);
                zipBytes = ossUtil.downloadFileToBytes(packageFileName);
                log.info("重新生成后下载成功，试卷ID：{}，版本：{}，包大小：{} 字节", paperId, currentVersion, zipBytes.length);
            }

            // 记录下载的文件信息（用于调试）
            log.info("从OSS下载的ZIP包信息 - 试卷ID：{}，文件大小：{} 字节", paperId, zipBytes.length);

            // 验证ZIP文件头（PK开头）
            if (zipBytes.length < 4 || zipBytes[0] != 0x50 || zipBytes[1] != 0x4B) {
                log.error("下载的内容不是ZIP文件，前4字节: {} {} {} {}",
                        zipBytes.length > 0 ? zipBytes[0] : 0,
                        zipBytes.length > 1 ? zipBytes[1] : 0,
                        zipBytes.length > 2 ? zipBytes[2] : 0,
                        zipBytes.length > 3 ? zipBytes[3] : 0);
                throw new ServiceException("下载的内容不是ZIP文件格式");
            }

            // 验证hash（如果数据库中有hash）
            if (StringUtils.isNotEmpty(paper.getPackageHash())) {
                String actualHash = paperPackageService.calculateHash(zipBytes);
                String expectedHash = paper.getPackageHash();

                // 验证hash格式（应该是64个十六进制字符）
                if (actualHash == null || actualHash.length() != 64) {
                    log.error("后端计算的hash格式错误: 长度={}, hash={}",
                            actualHash != null ? actualHash.length() : 0, actualHash);
                    throw new ServiceException("后端hash计算错误");
                }
                if (expectedHash.length() != 64) {
                    log.error("数据库中的hash格式错误: 长度={}, hash={}", expectedHash.length(), expectedHash);
                    throw new ServiceException("数据库中的hash格式错误");
                }

                log.info("后端hash验证 - 期望hash: {}, 实际hash: {}, 文件大小: {} 字节",
                        expectedHash, actualHash, zipBytes.length);

                if (!actualHash.equals(expectedHash)) {
                    log.warn("下载的ZIP包hash不匹配，期望：{}，实际：{}，文件大小：{} 字节",
                            expectedHash, actualHash, zipBytes.length);
                    log.warn("可能原因：1) OSS上的文件已更新但数据库未更新 2) 下载的文件损坏 3) OSS上的文件与生成时不一致");
                    log.warn("尝试重新生成试卷包...");

                    // 重新生成试卷包
                    generatePaperPackage(paperId);
                    // 重新获取试卷信息
                    paper = paperBiz.getById(paperId);
                    currentVersion = paper.getVersion() != null ? paper.getVersion() : 0;
                    packageFileName = buildPackageFileName(paper.getPaperCode(), currentVersion);
                    zipBytes = ossUtil.downloadFileToBytes(packageFileName);

                    // 再次验证hash
                    actualHash = paperPackageService.calculateHash(zipBytes);
                    expectedHash = paper.getPackageHash();
                    if (!actualHash.equals(expectedHash)) {
                        log.error("重新生成后hash仍然不匹配，期望：{}，实际：{}", expectedHash, actualHash);
                        throw new ServiceException("ZIP包hash验证失败，请检查OSS上的文件");
                    }

                    log.info("重新生成后hash验证通过，试卷ID：{}，版本：{}，包大小：{} 字节",
                            paperId, currentVersion, zipBytes.length);
                } else {
                    log.info("ZIP包hash验证通过，试卷ID：{}，版本：{}", paperId, currentVersion);
                }
            }

            log.info("试卷包下载成功，试卷ID：{}，版本：{}，包大小：{} 字节", paperId, currentVersion, zipBytes.length);
            return zipBytes;
        } catch (ServiceException e) {
            // 如果当前版本不存在，尝试前一个版本（可能是生成后版本号更新了）
            if (e.getMessage().contains("不存在") && currentVersion > 0) {
                String prevPackageFileName = buildPackageFileName(paper.getPaperCode(), currentVersion - 1);
                try {
                    byte[] zipBytes = ossUtil.downloadFileToBytes(prevPackageFileName);
                    log.warn("使用前一个版本的试卷包，试卷ID：{}，版本：{} -> {}，包大小：{} 字节",
                            paperId, currentVersion, currentVersion - 1, zipBytes.length);

                    // 验证文件大小和格式
                    if (zipBytes.length < 1024) {
                        log.error("前一个版本的ZIP包也太小（{}字节），尝试重新生成", zipBytes.length);
                        throw new ServiceException("ZIP包文件太小");
                    }

                    // 验证ZIP文件头
                    if (zipBytes.length < 4 || zipBytes[0] != 0x50 || zipBytes[1] != 0x4B) {
                        log.error("前一个版本的内容不是ZIP文件格式");
                        throw new ServiceException("ZIP包格式不正确");
                    }

                    return zipBytes;
                } catch (ServiceException e2) {
                    log.warn("前一个版本的试卷包也不存在，尝试重新生成，试卷ID：{}", paperId);
                    // 如果前一个版本也不存在，尝试重新生成
                    generatePaperPackage(paperId);
                    // 重新获取试卷信息（版本号已更新）
                    paper = paperBiz.getById(paperId);
                    currentVersion = paper.getVersion() != null ? paper.getVersion() : 0;
                    packageFileName = buildPackageFileName(paper.getPaperCode(), currentVersion);
                    return ossUtil.downloadFileToBytes(packageFileName);
                }
            } else if (e.getMessage().contains("不存在")) {
                // 如果版本号为0也不存在，尝试重新生成
                log.warn("OSS中不存在试卷包，尝试重新生成，试卷ID：{}", paperId);
                generatePaperPackage(paperId);
                // 重新获取试卷信息（版本号已更新）
                paper = paperBiz.getById(paperId);
                currentVersion = paper.getVersion() != null ? paper.getVersion() : 0;
                packageFileName = buildPackageFileName(paper.getPaperCode(), currentVersion);
                return ossUtil.downloadFileToBytes(packageFileName);
            }
            throw e;
        }
    }

    /**
     * 保存试卷题目关联
     */
    private void savePaperQuestions(Integer paperId, List<Integer> questionIds, List<BigDecimal> scores) {
        List<PaperQuestion> paperQuestions = new ArrayList<>();
        for (int i = 0; i < questionIds.size(); i++) {
            PaperQuestion pq = new PaperQuestion();
            pq.setPaperId(paperId);
            pq.setQuestionId(questionIds.get(i));
            pq.setSortOrder(i + 1); // 排序号从1开始
            if (scores != null && i < scores.size()) {
                pq.setScore(scores.get(i));
            } else {
                pq.setScore(BigDecimal.ZERO); // 默认分值为0
            }
            paperQuestions.add(pq);
        }
        paperQuestionBiz.batchInsert(paperQuestions);
    }

    /**
     * 更新试卷的题目总数
     */
    private void updateTotalQuestions(Integer paperId) {
        List<PaperQuestion> paperQuestions = paperQuestionBiz.listByPaperId(paperId);
        int totalQuestions = paperQuestions != null ? paperQuestions.size() : 0;

        Paper updatePaper = new Paper();
        updatePaper.setId(paperId);
        updatePaper.setTotalQuestions(totalQuestions);
        paperBiz.updateById(updatePaper);

        log.debug("更新试卷题目总数，试卷ID：{}，题目总数：{}", paperId, totalQuestions);
    }

    /**
     * 转换为DTO
     */
    private PaperDTO convertToDTO(Paper paper) {
        PaperDTO dto = new PaperDTO();
        BeanUtils.copyProperties(paper, dto);

        // 查询关联的题目列表
        List<PaperQuestion> paperQuestions = paperQuestionBiz.listByPaperId(paper.getId());

        // 如果totalQuestions为null，从paper_question表统计
        if (dto.getTotalQuestions() == null) {
            dto.setTotalQuestions(paperQuestions != null ? paperQuestions.size() : 0);
        }

        // 注意：convertToDTO 方法用于列表查询，不需要返回嵌套结构
        // 嵌套结构只在 getPaperFullData 方法中返回

        return dto;
    }

    /**
     * 生成试卷名称
     * 只包含有值的字段，避免出现 "2025null河北中考模拟" 这种情况
     * 格式：{year}年{month}月{province}{paper_type_label}{custom}
     * 示例：2023年12月河北中考英语听力模拟一（如果所有字段都有值）
     * 示例：2023年河北中考英语听力模拟一（如果 month 为 null）
     * 
     * @param year       年份（必填）
     * @param month      月份（可选）
     * @param province   省份编码（字典值，必填）
     * @param paperType  试卷类型编码（字典值，必填）
     * @param customName 自定义名称（可选）
     * @return 试卷名称
     */
    private String generatePaperName(Integer year, Integer month, String province,
            String paperType, String customName) {
        StringBuilder name = new StringBuilder();

        // 年份（必填）
        if (year != null) {
            name.append(year).append("年");
        }

        // 月份（可选，如果有值才添加）
        if (month != null) {
            name.append(month).append("月");
        }

        // 省份（必填，从字典获取标签）
        if (StringUtils.isNotEmpty(province)) {
            String provinceLabel = dictDataService.selectDictLabel("paper_province", province);
            if (StringUtils.isEmpty(provinceLabel)) {
                provinceLabel = province; // 如果字典中没有，使用编码
            }
            name.append(provinceLabel);
        }

        // 试卷类型（必填，从字典获取标签）
        if (StringUtils.isNotEmpty(paperType)) {
            String paperTypeLabel = dictDataService.selectDictLabel("paper_type", paperType);
            if (StringUtils.isEmpty(paperTypeLabel)) {
                paperTypeLabel = paperType; // 如果字典中没有，使用编码
            }
            name.append(paperTypeLabel);
        }

        // 自定义名称（可选，如果有值才添加）
        if (StringUtils.isNotEmpty(customName)) {
            name.append(customName);
        }

        return name.toString();
    }

    /**
     * 构建快速启动包文件名（OSS路径）
     * 格式：paper_packages/{paper_code}_v{version}_quick.zip
     * 
     * @param paperCode 试卷编码（唯一标识）
     * @param version   版本号
     * @return OSS文件路径
     */
    private String buildQuickStartPackageFileName(String paperCode, Integer version) {
        if (StringUtils.isEmpty(paperCode)) {
            throw new ServiceException("试卷编码不能为空");
        }
        return String.format("paper_packages/%s_v%d_quick.zip", paperCode, version);
    }

    /**
     * 构建试卷包文件名（OSS路径）
     * 新格式：paper_packages/{paper_code}_v{version}.zip（使用唯一编码）
     * 
     * @param paperCode 试卷编码（唯一标识）
     * @param version   版本号
     * @return OSS文件路径
     */
    private String buildPackageFileName(String paperCode, Integer version) {
        if (StringUtils.isEmpty(paperCode)) {
            throw new ServiceException("试卷编码不能为空");
        }
        // 直接使用 paper_code，不需要清理特殊字符（paper_code 本身就是标准格式）
        return String.format("paper_packages/%s_v%d.zip", paperCode, version);
    }

    /**
     * 生成简化的试卷编码（格式：PAPER_YYYYMMDD_序号）
     * 仅用于同步标识，不再用于业务唯一性检查
     * 
     * @return 试卷编码
     */
    private String generateSimplePaperCode() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        String prefix = "PAPER_" + dateStr + "_";

        // 查询当天已生成的试卷数量
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Paper::getPaperCode, prefix);
        wrapper.eq(Paper::getDelFlag, "0"); // 只统计未删除的
        long count = paperBiz.count(wrapper);

        // 生成序号（从1开始，3位数字）
        String sequence = String.format("%03d", count + 1);
        return prefix + sequence;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createPaperWithFullData(PaperFullDataBO fullDataBO) throws ServiceException {
        if (fullDataBO == null || fullDataBO.getPaper() == null) {
            throw new ServiceException("试卷数据不能为空");
        }

        PaperBasicInfoBO paperBO = fullDataBO.getPaper();

        // 1. 创建试卷基本信息
        Paper paper = new Paper();
        BeanUtils.copyProperties(paperBO, paper);

        // 生成paper_name和paper_code
        String paperName = generatePaperName(
                paperBO.getYear(),
                paperBO.getMonth(),
                paperBO.getProvince(),
                paperBO.getPaperType(),
                paperBO.getCustomName());
        String paperCode = generateSimplePaperCode();

        paper.setPaperName(paperName);
        paper.setPaperCode(paperCode);

        // 设置默认值
        if (paper.getBusinessType() == null) {
            paper.setBusinessType(5);
        }
        if (paper.getStatus() == null) {
            paper.setStatus(1);
        }
        if (paper.getAutoNextQuestion() == null) {
            paper.setAutoNextQuestion(0);
        }
        if (paper.getShowAnswerImmediately() == null) {
            paper.setShowAnswerImmediately(0);
        }
        if (paper.getAllowReview() == null) {
            paper.setAllowReview(0);
        }

        log.info("创建试卷（完整数据） - paperCode: {}, paperName: {}", paperCode, paperName);

        // 自动计算总分和题目总数
        BigDecimal totalScore = BigDecimal.ZERO;
        int totalQuestions = 0;

        // 从嵌套结构中计算题目总数和总分
        if (fullDataBO.getVolumes() != null && !fullDataBO.getVolumes().isEmpty()) {
            for (VolumeDataBO volumeBO : fullDataBO.getVolumes()) {
                if (volumeBO.getSections() != null && !volumeBO.getSections().isEmpty()) {
                    for (SectionDataBO sectionBO : volumeBO.getSections()) {
                        if (sectionBO.getQuestions() != null && !sectionBO.getQuestions().isEmpty()) {
                            totalQuestions += sectionBO.getQuestions().size();
                            for (QuestionDataBO question : sectionBO.getQuestions()) {
                        if (question.getScore() != null) {
                            totalScore = totalScore.add(question.getScore());
                                }
                            }
                        }
                    }
                }
            }
        }

        // 如果前端没有传递或者值为0，则使用自动计算的值
        if (paper.getTotalScore() == null || paper.getTotalScore().compareTo(BigDecimal.ZERO) == 0) {
            paper.setTotalScore(totalScore);
        }
        if (paper.getTotalQuestions() == null || paper.getTotalQuestions() == 0) {
            paper.setTotalQuestions(totalQuestions);
        }

        Integer paperId = paperBiz.createPaper(paper);

        // 2. 创建卷别、大题和题目（层级嵌套结构）
        // 构建临时ID到真实ID的映射，用于中场配置关联
        Map<String, Integer> volumeIdMap = new HashMap<>();
        
        if (fullDataBO.getVolumes() != null && !fullDataBO.getVolumes().isEmpty()) {
            log.info("开始创建卷别 - 数量: {}", fullDataBO.getVolumes().size());
            List<PaperQuestion> allPaperQuestions = new ArrayList<>();
            int questionSortOrder = 1;
            
            for (VolumeDataBO volumeBO : fullDataBO.getVolumes()) {
                PaperVolume volume = new PaperVolume();
                BeanUtils.copyProperties(volumeBO, volume);
                volume.setPaperId(paperId);

                // 如果前端没有提供volumeCode，根据volumeOrder自动生成（A, B, C...）
                if (StringUtils.isEmpty(volume.getVolumeCode())) {
                    int order = volumeBO.getVolumeOrder() != null ? volumeBO.getVolumeOrder() : 1;
                    volume.setVolumeCode(String.valueOf((char) ('A' + order - 1)));
                }

                // 单个保存以确保获取ID
                volumeService.saveVolume(volume);
                Integer volumeId = volume.getId(); // 获取保存后的实际ID
                
                // 如果前端提供了临时ID，建立映射关系
                if (volumeBO.getTempId() != null) {
                    volumeIdMap.put(volumeBO.getTempId(), volumeId);
                    log.debug("建立卷别临时ID映射: {} -> {}", volumeBO.getTempId(), volumeId);
                }

                // 处理该卷别下的大题（嵌套结构）
                if (volumeBO.getSections() != null && !volumeBO.getSections().isEmpty()) {
                    log.info("开始创建卷别 {} 下的大题 - 数量: {}", volumeId, volumeBO.getSections().size());
                    for (SectionDataBO sectionBO : volumeBO.getSections()) {
                PaperSection section = new PaperSection();
                BeanUtils.copyProperties(sectionBO, section);
                section.setPaperId(paperId);
                        section.setVolumeId(volumeId); // 直接使用刚保存的卷别ID

                // 单个保存以确保获取ID
                sectionService.saveSection(section);
                        Integer sectionId = section.getId(); // 获取保存后的实际ID

                        // 处理该大题下的题目（嵌套结构）
                        if (sectionBO.getQuestions() != null && !sectionBO.getQuestions().isEmpty()) {
                            log.info("开始创建大题 {} 下的题目 - 数量: {}", sectionId, sectionBO.getQuestions().size());
                            for (QuestionDataBO questionBO : sectionBO.getQuestions()) {
                    PaperQuestion pq = new PaperQuestion();
                    pq.setPaperId(paperId);
                    pq.setQuestionId(questionBO.getQuestionId());
                                pq.setSectionId(sectionId); // 直接使用刚保存的大题ID
                                pq.setSectionOrder(questionBO.getSectionOrder());
                                pq.setScore(questionBO.getScore());
                                pq.setSortOrder(questionSortOrder++); // 设置排序号
                                pq.setCreateTime(new Date());
                                
                                allPaperQuestions.add(pq);
                            }
                            log.info("大题 {} 下的题目创建完成", sectionId);
                        }
                    }
                    log.info("卷别 {} 下的大题创建完成", volumeId);
                }
            }
            
            // 批量保存所有题目关联
            if (!allPaperQuestions.isEmpty()) {
                paperQuestionBiz.batchInsert(allPaperQuestions);
                log.info("创建题目关联成功 - 试卷ID: {}, 题目数量: {}", paperId, allPaperQuestions.size());
            // 更新题目总数
            updateTotalQuestions(paperId);
        } else {
            log.info("前端未提交任何题目数据 - 试卷ID: {}", paperId);
            }
            
            log.info("卷别创建完成");
        }

        // 5. 创建中场配置（如果有）
        // 注意：需要在卷别保存完成后，使用真实的 volumeId 来关联中场配置
        if (fullDataBO.getIntermissions() != null && !fullDataBO.getIntermissions().isEmpty()) {
            List<PaperIntermission> intermissions = new ArrayList<>();
            for (IntermissionDataBO intermissionBO : fullDataBO.getIntermissions()) {
                PaperIntermission intermission = new PaperIntermission();
                intermission.setPaperId(paperId);
                intermission.setIntermissionText(intermissionBO.getIntermissionText());
                intermission.setIntermissionAudioUrl(intermissionBO.getIntermissionAudioUrl());
                intermission.setIntermissionAudioPath(intermissionBO.getIntermissionAudioPath());
                intermission.setIntermissionAudioDuration(intermissionBO.getIntermissionAudioDuration());
                intermission.setCanSkip(intermissionBO.getCanSkip() != null ? intermissionBO.getCanSkip() : 0);
                intermission.setFromVolume(intermissionBO.getFromVolume()); // 保留用于显示
                intermission.setToVolume(intermissionBO.getToVolume()); // 保留用于显示
                
                // 处理 fromVolumeId：优先使用真实ID，如果没有则通过 tempId 查找
                Integer fromVolumeId = intermissionBO.getFromVolumeId();
                if (fromVolumeId == null && intermissionBO.getFromVolumeTempId() != null) {
                    // 通过临时ID查找真实的 volumeId
                    fromVolumeId = volumeIdMap.get(intermissionBO.getFromVolumeTempId());
                    if (fromVolumeId == null) {
                        log.warn("无法找到来源卷别的真实ID，临时ID: {}", intermissionBO.getFromVolumeTempId());
                        continue; // 跳过这个中场配置
                    }
                }
                intermission.setFromVolumeId(fromVolumeId);
                
                // 处理 toVolumeId：优先使用真实ID，如果没有则通过 tempId 查找
                Integer toVolumeId = intermissionBO.getToVolumeId();
                if (toVolumeId == null && intermissionBO.getToVolumeTempId() != null) {
                    // 通过临时ID查找真实的 volumeId
                    toVolumeId = volumeIdMap.get(intermissionBO.getToVolumeTempId());
                    if (toVolumeId == null) {
                        log.warn("无法找到目标卷别的真实ID，临时ID: {}", intermissionBO.getToVolumeTempId());
                        continue; // 跳过这个中场配置
                    }
                }
                intermission.setToVolumeId(toVolumeId);
                
                intermissions.add(intermission);
            }

            if (!intermissions.isEmpty()) {
                intermissionService.saveBatch(intermissions, paperId);
                log.info("创建中场配置 - 试卷ID: {}, 中场配置数量: {}", paperId, intermissions.size());
            }
        }

        log.info("试卷创建成功（完整数据） - 试卷ID: {}", paperId);
        return paperId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePaperWithFullData(PaperFullDataBO fullDataBO) throws ServiceException {
        if (fullDataBO == null || fullDataBO.getPaper() == null) {
            throw new ServiceException("试卷数据不能为空");
        }

        PaperBasicInfoBO paperBO = fullDataBO.getPaper();

        if (paperBO.getId() == null) {
            throw new ServiceException("试卷ID不能为空");
        }

        Integer paperId = paperBO.getId();

        // 检查试卷是否存在
        Paper existPaper = paperBiz.getById(paperId);
        if (existPaper == null || "2".equals(existPaper.getDelFlag())) {
            throw new ServiceException("试卷不存在");
        }

        log.info("更新试卷（完整数据，删除后新增） - 试卷ID: {}", paperId);

        // 1. 删除旧数据（按照依赖关系从下往上删除）
        // 1.1 删除题目关联
        paperQuestionBiz.deleteByPaperId(paperId);
        log.info("删除旧题目关联 - 试卷ID: {}", paperId);

        // 1.2 删除大题
        sectionService.deleteByPaperId(paperId);
        log.info("删除旧大题 - 试卷ID: {}", paperId);

        // 1.3 删除卷别
        volumeService.deleteByPaperId(paperId);
        log.info("删除旧卷别 - 试卷ID: {}", paperId);

        // 1.4 删除中场配置
        intermissionService.deleteByPaperId(paperId);

        log.info("删除旧中场配置 - 试卷ID: {}", paperId);

        // 2. 更新试卷基本信息
        Paper paper = new Paper();
        BeanUtils.copyProperties(paperBO, paper);

        // 自动计算总分和题目总数
        java.math.BigDecimal totalScore = java.math.BigDecimal.ZERO;
        int totalQuestions = 0;

        // 从嵌套结构中计算题目总数和总分
        if (fullDataBO.getVolumes() != null && !fullDataBO.getVolumes().isEmpty()) {
            for (VolumeDataBO volumeBO : fullDataBO.getVolumes()) {
                if (volumeBO.getSections() != null && !volumeBO.getSections().isEmpty()) {
                    for (SectionDataBO sectionBO : volumeBO.getSections()) {
                        if (sectionBO.getQuestions() != null && !sectionBO.getQuestions().isEmpty()) {
                            totalQuestions += sectionBO.getQuestions().size();
                            for (QuestionDataBO question : sectionBO.getQuestions()) {
                        if (question.getScore() != null) {
                            totalScore = totalScore.add(question.getScore());
                                }
                            }
                        }
                    }
                }
            }
        }

        // 如果前端没有传递或者值为0，则使用自动计算的值
        if (paper.getTotalScore() == null || paper.getTotalScore().compareTo(java.math.BigDecimal.ZERO) == 0) {
            paper.setTotalScore(totalScore);
        }
        if (paper.getTotalQuestions() == null || paper.getTotalQuestions() == 0) {
            paper.setTotalQuestions(totalQuestions);
        }

        // 重新生成paper_name（保持paper_code不变）
        String paperName = generatePaperName(
                paperBO.getYear(),
                paperBO.getMonth(),
                paperBO.getProvince(),
                paperBO.getPaperType(),
                paperBO.getCustomName());
        paper.setPaperName(paperName);
        paper.setPaperCode(existPaper.getPaperCode()); // 保持原有code

        log.info("更新试卷基本信息 - 试卷ID: {}, paperName: {}", paperId, paperName);

        paperBiz.updatePaper(paper);

        // 3. 创建新数据（层级嵌套结构：volumes -> sections -> questions）
        // 构建临时ID到真实ID的映射，用于中场配置关联
        Map<String, Integer> volumeIdMap = new HashMap<>();
        
        if (fullDataBO.getVolumes() != null && !fullDataBO.getVolumes().isEmpty()) {
            log.info("开始创建卷别 - 数量: {}", fullDataBO.getVolumes().size());
            List<PaperQuestion> allPaperQuestions = new ArrayList<>();
            int questionSortOrder = 1;
            
            for (VolumeDataBO volumeBO : fullDataBO.getVolumes()) {
                PaperVolume volume = new PaperVolume();
                BeanUtils.copyProperties(volumeBO, volume);
                volume.setPaperId(paperId);
                volume.setId(null); // 确保是新增

                // 如果前端没有提供volumeCode，根据volumeOrder自动生成（A, B, C...）
                if (StringUtils.isEmpty(volume.getVolumeCode())) {
                    int order = volumeBO.getVolumeOrder() != null ? volumeBO.getVolumeOrder() : 1;
                    volume.setVolumeCode(String.valueOf((char) ('A' + order - 1)));
                }

                // 单个保存以确保获取ID
                volumeService.saveVolume(volume);
                Integer volumeId = volume.getId(); // 获取保存后的实际ID
                
                // 如果前端提供了临时ID，建立映射关系
                if (volumeBO.getTempId() != null) {
                    volumeIdMap.put(volumeBO.getTempId(), volumeId);
                    log.debug("建立卷别临时ID映射: {} -> {}", volumeBO.getTempId(), volumeId);
                }

                // 处理该卷别下的大题（嵌套结构）
                if (volumeBO.getSections() != null && !volumeBO.getSections().isEmpty()) {
                    log.info("开始创建卷别 {} 下的大题 - 数量: {}", volumeId, volumeBO.getSections().size());
                    for (SectionDataBO sectionBO : volumeBO.getSections()) {
                PaperSection section = new PaperSection();
                BeanUtils.copyProperties(sectionBO, section);
                section.setPaperId(paperId);
                section.setId(null); // 确保是新增
                        section.setVolumeId(volumeId); // 直接使用刚保存的卷别ID

                // 单个保存以确保获取ID
                sectionService.saveSection(section);
                        Integer sectionId = section.getId(); // 获取保存后的实际ID

                        // 处理该大题下的题目（嵌套结构）
                        if (sectionBO.getQuestions() != null && !sectionBO.getQuestions().isEmpty()) {
                            log.info("开始创建大题 {} 下的题目 - 数量: {}", sectionId, sectionBO.getQuestions().size());
                            for (QuestionDataBO questionBO : sectionBO.getQuestions()) {
                    PaperQuestion pq = new PaperQuestion();
                    pq.setPaperId(paperId);
                    pq.setQuestionId(questionBO.getQuestionId());
                                pq.setSectionId(sectionId); // 直接使用刚保存的大题ID
                                pq.setSectionOrder(questionBO.getSectionOrder());
                                pq.setScore(questionBO.getScore());
                                pq.setSortOrder(questionSortOrder++); // 设置排序号
                                pq.setCreateTime(new Date());
                                
                                allPaperQuestions.add(pq);
                            }
                            log.info("大题 {} 下的题目创建完成", sectionId);
                        }
                    }
                    log.info("卷别 {} 下的大题创建完成", volumeId);
                }
            }
            
            // 批量保存所有题目关联
            if (!allPaperQuestions.isEmpty()) {
                paperQuestionBiz.batchInsert(allPaperQuestions);
                log.info("更新题目关联成功 - 试卷ID: {}, 题目数量: {}", paperId, allPaperQuestions.size());
            // 更新题目总数
            updateTotalQuestions(paperId);
        } else {
            log.info("前端未提交任何题目数据 - 试卷ID: {}", paperId);
            }
            
            log.info("卷别创建完成");
        }

        // 3.4 创建中场配置
        // 注意：需要在卷别保存完成后，使用真实的 volumeId 来关联中场配置
        if (fullDataBO.getIntermissions() != null && !fullDataBO.getIntermissions().isEmpty()) {
            List<PaperIntermission> intermissions = new ArrayList<>();
            for (IntermissionDataBO intermissionBO : fullDataBO.getIntermissions()) {
                PaperIntermission intermission = new PaperIntermission();
                intermission.setPaperId(paperId);
                intermission.setIntermissionText(intermissionBO.getIntermissionText());
                intermission.setIntermissionAudioUrl(intermissionBO.getIntermissionAudioUrl());
                intermission.setIntermissionAudioPath(intermissionBO.getIntermissionAudioPath());
                intermission.setIntermissionAudioDuration(intermissionBO.getIntermissionAudioDuration());
                intermission.setCanSkip(intermissionBO.getCanSkip() != null ? intermissionBO.getCanSkip() : 0);
                intermission.setFromVolume(intermissionBO.getFromVolume()); // 保留用于显示
                intermission.setToVolume(intermissionBO.getToVolume()); // 保留用于显示
                
                // 处理 fromVolumeId：优先使用真实ID，如果没有则通过 tempId 查找
                Integer fromVolumeId = intermissionBO.getFromVolumeId();
                if (fromVolumeId == null && intermissionBO.getFromVolumeTempId() != null) {
                    // 通过临时ID查找真实的 volumeId
                    fromVolumeId = volumeIdMap.get(intermissionBO.getFromVolumeTempId());
                    if (fromVolumeId == null) {
                        log.warn("无法找到来源卷别的真实ID，临时ID: {}", intermissionBO.getFromVolumeTempId());
                        continue; // 跳过这个中场配置
                    }
                }
                intermission.setFromVolumeId(fromVolumeId);
                
                // 处理 toVolumeId：优先使用真实ID，如果没有则通过 tempId 查找
                Integer toVolumeId = intermissionBO.getToVolumeId();
                if (toVolumeId == null && intermissionBO.getToVolumeTempId() != null) {
                    // 通过临时ID查找真实的 volumeId
                    toVolumeId = volumeIdMap.get(intermissionBO.getToVolumeTempId());
                    if (toVolumeId == null) {
                        log.warn("无法找到目标卷别的真实ID，临时ID: {}", intermissionBO.getToVolumeTempId());
                        continue; // 跳过这个中场配置
                    }
                }
                intermission.setToVolumeId(toVolumeId);
                
                intermissions.add(intermission);
            }

            if (!intermissions.isEmpty()) {
                intermissionService.saveBatch(intermissions, paperId);
                log.info("创建新中场配置 - 试卷ID: {}, 中场配置数量: {}", paperId, intermissions.size());
            }
        }

        log.info("试卷更新成功（完整数据，删除后新增） - 试卷ID: {}", paperId);
    }

    @Override
    public PaperDTO getPaperFullData(Integer paperId) throws ServiceException {
        if (paperId == null) {
            throw new ServiceException("试卷ID不能为空");
        }

        // 1. 查询试卷基本信息
        Paper paper = paperBiz.getById(paperId);
        if (paper == null || "2".equals(paper.getDelFlag())) {
            throw new ServiceException("试卷不存在");
        }

        // 2. 转换为DTO
        PaperDTO dto = new PaperDTO();
        BeanUtils.copyProperties(paper, dto);

        // 3. 查询卷别列表
        List<PaperVolume> volumes = volumeService.listByPaperId(paperId);

        // 4. 构建嵌套结构：volumes -> sections -> questions
        // 按嵌套结构查询，更严谨：根据 paperId + volumeId 查询大题，根据 paperId + sectionId 查询题目
        List<PaperVolumeDTO> volumeDTOs = new ArrayList<>();
        if (!CollectionUtils.isEmpty(volumes)) {
            // 按 volumeOrder 排序 volumes
            List<PaperVolume> sortedVolumes = new ArrayList<>(volumes);
            sortedVolumes.sort((a, b) -> {
                int orderA = a.getVolumeOrder() != null ? a.getVolumeOrder() : 0;
                int orderB = b.getVolumeOrder() != null ? b.getVolumeOrder() : 0;
                return Integer.compare(orderA, orderB);
            });
            
            // 批量获取所有题目ID，用于一次性查询题目详情
            Set<Integer> allQuestionIds = new HashSet<>();
            
            // 遍历每个卷别，构建嵌套结构
            for (PaperVolume volume : sortedVolumes) {
                PaperVolumeDTO volumeDTO = new PaperVolumeDTO();
                volumeDTO.setId(volume.getId());
                volumeDTO.setPaperId(volume.getPaperId());
                volumeDTO.setVolumeCode(volume.getVolumeCode());
                volumeDTO.setVolumeName(volume.getVolumeName());
                volumeDTO.setVolumeOrder(volume.getVolumeOrder());
                volumeDTO.setVolumeAudioUrl(volume.getVolumeAudioUrl());
                volumeDTO.setVolumeAudioPath(volume.getVolumeAudioPath());
                volumeDTO.setVolumeAudioDuration(volume.getVolumeAudioDuration());
                
                // 根据 paperId + volumeId 查询该卷别下的大题列表（更严谨）
                List<PaperSection> volumeSections = sectionService.listByVolumeId(volume.getId());
                
                // 构建该卷别下的大题列表
                List<PaperSectionDTO> sectionDTOs = new ArrayList<>();
                if (!CollectionUtils.isEmpty(volumeSections)) {
                    // 按 sectionOrder 排序
                    volumeSections.sort((a, b) -> {
                        int orderA = a.getSectionOrder() != null ? a.getSectionOrder() : 0;
                        int orderB = b.getSectionOrder() != null ? b.getSectionOrder() : 0;
                        return Integer.compare(orderA, orderB);
                    });
                    
                    for (PaperSection section : volumeSections) {
                        PaperSectionDTO sectionDTO = new PaperSectionDTO();
                        sectionDTO.setId(section.getId());
                        sectionDTO.setPaperId(section.getPaperId());
                        sectionDTO.setVolumeId(section.getVolumeId());
                        sectionDTO.setVolumeCode(section.getVolumeCode());
                        sectionDTO.setSectionName(section.getSectionName());
                        sectionDTO.setSectionOrder(section.getSectionOrder());
                        sectionDTO.setQuestionCount(section.getQuestionCount());
                        sectionDTO.setTotalScore(section.getTotalScore());
                        sectionDTO.setScorePerQuestion(section.getScorePerQuestion());
                        sectionDTO.setInstructionText(section.getInstructionText());
                        sectionDTO.setAnswerTime(section.getAnswerTime() != null ? section.getAnswerTime() : 5);
                        sectionDTO.setInstructionAudioUrl(section.getInstructionAudioUrl());
                        sectionDTO.setInstructionAudioPath(section.getInstructionAudioPath());
                        sectionDTO.setInstructionAudioDuration(section.getInstructionAudioDuration());
                        sectionDTO.setAudioPlayCount(section.getAudioPlayCount() != null ? section.getAudioPlayCount() : 1);
                        sectionDTO.setAnswerTime(section.getAnswerTime() != null ? section.getAnswerTime() : 5);
                        
                        // 根据 paperId + sectionId 查询该大题下的题目列表（更严谨）
                        // 使用 LambdaQueryWrapper 查询
                        List<PaperQuestion> sectionQuestions = paperQuestionBiz.list(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaperQuestion>()
                                .eq(PaperQuestion::getPaperId, paperId)
                                .eq(PaperQuestion::getSectionId, section.getId())
                                .orderByAsc(PaperQuestion::getSectionOrder)
                        );
                        
                        // 收集题目ID，用于批量查询题目详情
                        if (!CollectionUtils.isEmpty(sectionQuestions)) {
                            for (PaperQuestion pq : sectionQuestions) {
                                if (pq.getQuestionId() != null) {
                                    allQuestionIds.add(pq.getQuestionId());
                                }
                            }
                        }
                        
                        // 先设置为空列表，后续填充
                        sectionDTO.setQuestions(new ArrayList<>());
                        sectionDTOs.add(sectionDTO);
                    }
                }
                volumeDTO.setSections(sectionDTOs);
                volumeDTOs.add(volumeDTO);
            }
            
            // 批量获取所有题目详情（用于填充title和type）
            Map<Integer, QuestionInfoDTO> questionInfoMap = new HashMap<>();
            if (!allQuestionIds.isEmpty()) {
                for (Integer questionId : allQuestionIds) {
                    try {
                        QuestionIdBO idBO = new QuestionIdBO();
                        idBO.setId(questionId);
                        QuestionInfoDTO questionInfo = questionService.getQuestion(idBO);
                        if (questionInfo != null) {
                            questionInfoMap.put(questionId, questionInfo);
                        }
                    } catch (Exception e) {
                        log.warn("获取题目详情失败，跳过：questionId={}, error={}", questionId, e.getMessage());
                    }
                }
            }
            
            // 填充题目详情到嵌套结构中
            for (PaperVolumeDTO volumeDTO : volumeDTOs) {
                for (PaperSectionDTO sectionDTO : volumeDTO.getSections()) {
                    // 根据 paperId + sectionId 查询该大题下的题目列表
                    List<PaperQuestion> sectionQuestions = paperQuestionBiz.list(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaperQuestion>()
                            .eq(PaperQuestion::getPaperId, paperId)
                            .eq(PaperQuestion::getSectionId, sectionDTO.getId())
                            .orderByAsc(PaperQuestion::getSectionOrder)
                    );
                    
                    // 转换为DTO并填充题目详情
                    List<PaperQuestionDTO> questionDTOs = new ArrayList<>();
                    if (!CollectionUtils.isEmpty(sectionQuestions)) {
                        for (PaperQuestion pq : sectionQuestions) {
                PaperQuestionDTO qdto = new PaperQuestionDTO();
                qdto.setId(pq.getId());
                qdto.setPaperId(pq.getPaperId());
                qdto.setQuestionId(pq.getQuestionId());
                qdto.setSectionId(pq.getSectionId());
                qdto.setSectionOrder(pq.getSectionOrder());
                qdto.setSortOrder(pq.getSortOrder());
                qdto.setScore(pq.getScore());

                            // 填充题目详情（title, type, answers等）
                            if (pq.getQuestionId() != null) {
                                QuestionInfoDTO questionInfo = questionInfoMap.get(pq.getQuestionId());
                                if (questionInfo != null) {
                                    qdto.setTitle(questionInfo.getTitle());
                                    qdto.setType(questionInfo.getType());
                                    qdto.setSubjectId(questionInfo.getSubjectId());
                                    qdto.setSubjectName(questionInfo.getSubjectName());
                                    qdto.setAnswers(questionInfo.getAnswers()); // 填充选项列表
                                    qdto.setAnswer(questionInfo.getAnswer()); // 填充答案
                                    qdto.setAnalyzes(questionInfo.getAnalyzes()); // 填充解析
                                    qdto.setMediaType(questionInfo.getMediaType()); // 填充媒体类型
                                    qdto.setOptionType(questionInfo.getOptionType()); // 填充选项类型
                                }
                            }
                            
                            questionDTOs.add(qdto);
                        }
                    }
                    sectionDTO.setQuestions(questionDTOs);
                }
            }
        }
        
        // 7. 设置到DTO（嵌套结构）
        dto.setVolumes(volumeDTOs);

        // 8. 查询中场配置列表
        List<PaperIntermission> intermissions = intermissionService.listByPaperId(paperId);
        dto.setIntermissions(intermissions);

        // 9. 更新题目总数（如果为null）
        int totalQuestions = 0;
        int totalSections = 0;
        for (PaperVolumeDTO volumeDTO : volumeDTOs) {
            if (volumeDTO.getSections() != null) {
                totalSections += volumeDTO.getSections().size();
                for (PaperSectionDTO sectionDTO : volumeDTO.getSections()) {
                    totalQuestions += sectionDTO.getQuestions() != null ? sectionDTO.getQuestions().size() : 0;
                }
            }
        }
        if (dto.getTotalQuestions() == null) {
            dto.setTotalQuestions(totalQuestions);
        }

        log.info("查询试卷完整数据 - 试卷ID: {}, 卷别数: {}, 大题数: {}, 题目数: {}, 中场配置数: {}",
                paperId,
                volumeDTOs != null ? volumeDTOs.size() : 0,
                totalSections,
                totalQuestions,
                intermissions != null ? intermissions.size() : 0);

        return dto;
    }
}
