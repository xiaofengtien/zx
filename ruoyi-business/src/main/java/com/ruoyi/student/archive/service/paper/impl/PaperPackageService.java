package com.ruoyi.student.archive.service.paper.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.oss.exam.question.OssUtil;
import com.ruoyi.student.archive.biz.question.IQuestionMediaBiz;
import com.ruoyi.student.archive.domain.question.QuestionMedia;
import com.ruoyi.student.archive.domain.paper.Paper;
import com.ruoyi.student.archive.domain.paper.PaperIntermission;
import com.ruoyi.student.archive.domain.paper.PaperQuestion;
import com.ruoyi.student.archive.domain.paper.PaperSection;
import com.ruoyi.student.archive.domain.paper.PaperVolume;
import com.ruoyi.student.archive.service.paper.IPaperIntermissionService;
import com.ruoyi.student.archive.service.paper.IPaperSectionService;
import com.ruoyi.student.archive.service.paper.IPaperVolumeService;
import com.ruoyi.student.archive.service.question.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.IOUtils;

/**
 * 试卷包服务类
 * 负责试卷包的生成和下载
 * 
 * @author ruoyi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperPackageService {

    private final OssUtil ossUtil;
    private final QuestionService questionService;
    private final IQuestionMediaBiz questionMediaBiz;
    private final IPaperVolumeService paperVolumeService;
    private final IPaperSectionService paperSectionService;
    private final IPaperIntermissionService paperIntermissionService;
    private final com.ruoyi.student.archive.biz.paper.IPaperQuestionBiz paperQuestionBiz;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成快速启动包（包含manifest.json、trial_listen/、intro/、notes等）
     * 用于快速显示试卷列表和操作提示页面
     * 
     * @param paper 试卷实体
     * @return ZIP包字节数组
     * @throws ServiceException 业务异常
     */
    public byte[] generateQuickStartPackage(Paper paper) throws ServiceException {
        if (paper == null) {
            throw new ServiceException("试卷不能为空");
        }

        log.info("开始生成快速启动包，试卷ID：{}", paper.getId());

        try {
            // 1. 获取卷别列表（仅用于构建manifest）
            List<PaperVolume> volumes = paperVolumeService.listByPaperId(paper.getId());

            // 2. 获取大题列表（仅用于构建manifest）
            List<PaperSection> sections = paperSectionService.listByPaperId(paper.getId());

            // 3. 获取中场配置（仅用于构建manifest）
            List<PaperIntermission> intermissions = paperIntermissionService.listByPaperId(paper.getId());

            // 4. 获取题目关联列表（仅用于构建manifest，不需要实际题目数据）
            List<PaperQuestion> paperQuestions = paperQuestionBiz.listByPaperId(paper.getId());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = null;
            try {
                zos = new ZipOutputStream(baos, StandardCharsets.UTF_8);

                // 5. 生成 manifest.json（试卷元数据）
                Map<String, Object> manifest = buildManifest(paper, volumes, sections, intermissions, paperQuestions);
                addJsonToZip(zos, "manifest.json", manifest);

                // 6. 打包快速启动所需的媒体文件
                // 6.1 试听媒体（trial_listen/）
                List<QuestionMedia> trialMedia = questionMediaBiz.list(
                        new LambdaQueryWrapper<QuestionMedia>()
                                .eq(QuestionMedia::getPaperId, paper.getId())
                                .in(QuestionMedia::getMediaType, Arrays.asList(10, 11)));
                packMediaFiles(zos, trialMedia, "trial_listen/");

                // 6.2 试听音频（从 Paper 实体字段获取）
                // 试听旁白音频（介绍音频，自动播放）- 独立判断，不依赖trialListenEnabled
                if (StringUtils.isNotEmpty(paper.getTrialIntroAudioUrl())
                        || StringUtils.isNotEmpty(paper.getTrialIntroAudioPath())) {
                    packMediaFileFromEntity(zos, paper.getTrialIntroAudioUrl(), paper.getTrialIntroAudioPath(),
                            "trial_intro/");
                }
                
                // 试听相关音频和图片（仅在trialListenEnabled为1时打包）
                if (paper.getTrialListenEnabled() != null && paper.getTrialListenEnabled() == 1) {
                    // 试听音频（用户点击播放）
                    if (StringUtils.isNotEmpty(paper.getTrialListenAudioUrl())
                            || StringUtils.isNotEmpty(paper.getTrialListenAudioPath())) {
                        packMediaFileFromEntity(zos, paper.getTrialListenAudioUrl(), paper.getTrialListenAudioPath(),
                                "trial_listen/");
                    }
                    // 操作提示图片
                    if (StringUtils.isNotEmpty(paper.getOperateListenImageUrl())
                            || StringUtils.isNotEmpty(paper.getOperateListenImagePath())) {
                        packMediaFileFromEntity(zos, paper.getOperateListenImageUrl(), paper.getOperateListenImagePath(),
                                "operate_listen/");
                    }
                }

                // 6.3 试卷独白音频（intro/）
                if (StringUtils.isNotEmpty(paper.getIntroAudioUrl())
                        || StringUtils.isNotEmpty(paper.getIntroAudioPath())) {
                    packMediaFileFromEntity(zos, paper.getIntroAudioUrl(), paper.getIntroAudioPath(), "intro/");
                }

                // 确保所有数据都写入并关闭ZIP流
                zos.finish();
                zos.flush();

            } catch (IOException e) {
                log.error("生成快速启动包ZIP流失败，试卷ID：{}", paper.getId(), e);
                throw new ServiceException("生成快速启动包ZIP流失败：" + e.getMessage());
            } finally {
                if (zos != null) {
                    try {
                        zos.close();
                    } catch (IOException e) {
                        log.warn("关闭快速启动包ZIP流时出错，试卷ID：{}", paper.getId(), e);
                    }
                }
            }

            byte[] zipBytes = baos.toByteArray();
            baos.close();

            // 验证ZIP文件格式
            if (zipBytes.length < 22) {
                throw new ServiceException("生成的快速启动包大小异常，可能生成失败，实际大小：" + zipBytes.length + " 字节");
            }

            if (zipBytes[0] != 0x50 || zipBytes[1] != 0x4B) {
                log.error("快速启动包ZIP文件头不正确");
                throw new ServiceException("生成的快速启动包格式不正确");
            }

            log.info("快速启动包生成成功，试卷ID：{}，包大小：{} 字节（{} MB）", 
                    paper.getId(), zipBytes.length, zipBytes.length / 1024.0 / 1024.0);
            return zipBytes;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成快速启动包失败，试卷ID：{}", paper.getId(), e);
            throw new ServiceException("生成快速启动包失败：" + e.getMessage());
        }
    }

    /**
     * 生成完整试卷包（使用新的表结构）
     * 注意：此方法会排除快速启动包中已包含的文件（trial_listen/、intro/、manifest.json）
     * 
     * @param paper 试卷实体
     * @return ZIP包字节数组
     * @throws ServiceException 业务异常
     */
    public byte[] generatePaperPackage(Paper paper) throws ServiceException {
        if (paper == null) {
            throw new ServiceException("试卷不能为空");
        }

        // 获取试卷题目关联列表
        List<PaperQuestion> paperQuestions = paperQuestionBiz.listByPaperId(paper.getId());
        if (paperQuestions == null || paperQuestions.isEmpty()) {
            throw new ServiceException("题目列表不能为空");
        }

        log.info("开始生成试卷包，试卷ID：{}，题目数量：{}", paper.getId(), paperQuestions.size());

        try {
            // 1. 获取卷别列表
            List<PaperVolume> volumes = paperVolumeService.listByPaperId(paper.getId());

            // 2. 获取大题列表
            List<PaperSection> sections = paperSectionService.listByPaperId(paper.getId());

            // 3. 获取中场配置
            List<PaperIntermission> intermissions = paperIntermissionService.listByPaperId(paper.getId());

            // 4. 获取题目ID列表
            List<Integer> questionIds = paperQuestions.stream()
                    .map(PaperQuestion::getQuestionId)
                    .collect(Collectors.toList());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = null;
            try {
                zos = new ZipOutputStream(baos, StandardCharsets.UTF_8);

                // 5. 生成 manifest.json（试卷元数据，使用新的表结构）
                // 注意：完整包也包含manifest.json，以便独立使用
                Map<String, Object> manifest = buildManifest(paper, volumes, sections, intermissions, paperQuestions);
                addJsonToZip(zos, "manifest.json", manifest);

                // 6. 生成 questions.json（题目数据，使用新的表结构）
                List<Map<String, Object>> questionsData = buildQuestionsData(questionIds, paperQuestions);
                addJsonToZip(zos, "questions.json", questionsData);

                // 7. 下载并打包媒体文件（使用新的表结构和 media_type）
                // 注意：完整包包含所有媒体文件，包括快速启动包中的文件（以便独立使用）
                downloadAndPackMediaFiles(zos, paper, volumes, sections, intermissions, questionIds);

                // 确保所有数据都写入并关闭ZIP流
                zos.finish();
                zos.flush();

            } catch (IOException e) {
                log.error("生成ZIP流失败，试卷ID：{}", paper.getId(), e);
                throw new ServiceException("生成ZIP流失败：" + e.getMessage());
            } finally {
                // 确保ZIP流正确关闭
                if (zos != null) {
                    try {
                        zos.close();
                    } catch (IOException e) {
                        log.warn("关闭ZIP流时出错，试卷ID：{}", paper.getId(), e);
                    }
                }
            }

            // 在关闭流之后获取字节数组
            byte[] zipBytes = baos.toByteArray();
            baos.close();

            // 验证ZIP文件格式
            if (zipBytes.length < 22) {
                throw new ServiceException("生成的ZIP包大小异常，可能生成失败，实际大小：" + zipBytes.length + " 字节");
            }

            // 检查ZIP文件头（ZIP文件以 PK 开头）
            if (zipBytes[0] != 0x50 || zipBytes[1] != 0x4B) {
                log.error("ZIP文件头不正确，前4个字节：{} {} {} {}",
                        String.format("%02X", zipBytes[0] & 0xFF),
                        String.format("%02X", zipBytes[1] & 0xFF),
                        zipBytes.length > 2 ? String.format("%02X", zipBytes[2] & 0xFF) : "N/A",
                        zipBytes.length > 3 ? String.format("%02X", zipBytes[3] & 0xFF) : "N/A");
                throw new ServiceException("生成的ZIP包格式不正确");
            }

            log.info("试卷包生成成功，试卷ID：{}，包大小：{} 字节，ZIP文件头验证通过", paper.getId(), zipBytes.length);
            return zipBytes;

        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成试卷包失败，试卷ID：{}", paper.getId(), e);
            throw new ServiceException("生成试卷包失败：" + e.getMessage());
        }
    }

    /**
     * 构建 manifest.json 数据（使用新的表结构）
     */
    private Map<String, Object> buildManifest(Paper paper,
            List<PaperVolume> volumes,
            List<PaperSection> sections,
            List<PaperIntermission> intermissions,
            List<PaperQuestion> paperQuestions) {
        Map<String, Object> manifest = new HashMap<>();

        // 1. 基本信息
        manifest.put("paperId", paper.getId());
        manifest.put("paperCode", paper.getPaperCode());
        manifest.put("paperName", paper.getPaperName());
        manifest.put("paperType", paper.getPaperType());
        manifest.put("practiceLimit", paper.getPracticeLimit() != null ? paper.getPracticeLimit() : 0);
        manifest.put("totalScore", paper.getTotalScore() != null ? paper.getTotalScore().intValue() : 100);
        manifest.put("totalQuestions", paper.getTotalQuestions() != null ? paper.getTotalQuestions() : 0);
        manifest.put("duration", paper.getDuration() != null ? paper.getDuration() : 0);
        manifest.put("year", paper.getYear());
        manifest.put("month", paper.getMonth());
        manifest.put("province", paper.getProvince());
        manifest.put("notes", paper.getNotes());
        manifest.put("notesDisplayMode", paper.getNotesDisplayMode());
        
        // 1.1 注意事项音频（使用 introAudio 字段）
        manifest.put("introAudioUrl", paper.getIntroAudioUrl());
        // 使用相对路径（从URL或Path中提取文件名）
        if (StringUtils.isNotEmpty(paper.getIntroAudioUrl()) || StringUtils.isNotEmpty(paper.getIntroAudioPath())) {
            manifest.put("introAudioPath", 
                    "intro/" + getMediaFileName(paper.getIntroAudioUrl(), paper.getIntroAudioPath()));
        } else {
            manifest.put("introAudioPath", null);
        }
        manifest.put("introAudioDuration", paper.getIntroAudioDuration());

        // 2. 试听配置（从 question_media 表查询 media_type=10,11）
        if (paper.getTrialListenEnabled() != null && paper.getTrialListenEnabled() == 1) {
            List<QuestionMedia> trialMedia = questionMediaBiz.list(
                    new LambdaQueryWrapper<QuestionMedia>()
                            .eq(QuestionMedia::getPaperId, paper.getId())
                            .in(QuestionMedia::getMediaType, Arrays.asList(10, 11)));
            manifest.put("trialListenEnabled", true);
            manifest.put("trialListenAudioUrl", paper.getTrialListenAudioUrl());
            // 使用相对路径（从URL或Path中提取文件名）
            if (StringUtils.isNotEmpty(paper.getTrialListenAudioUrl()) || StringUtils.isNotEmpty(paper.getTrialListenAudioPath())) {
                manifest.put("trialListenAudioPath", 
                        "trial_listen/" + getMediaFileName(paper.getTrialListenAudioUrl(), paper.getTrialListenAudioPath()));
            } else {
                manifest.put("trialListenAudioPath", null);
            }
            manifest.put("trialListenAudioDuration", paper.getTrialListenAudioDuration());
            manifest.put("trialListenAudioText", paper.getTrialListenAudioText());
            manifest.put("trialIntroAudioUrl", paper.getTrialIntroAudioUrl());
            // 使用相对路径（从URL或Path中提取文件名）
            if (StringUtils.isNotEmpty(paper.getTrialIntroAudioUrl()) || StringUtils.isNotEmpty(paper.getTrialIntroAudioPath())) {
                manifest.put("trialIntroAudioPath",
                        "trial_intro/" + getMediaFileName(paper.getTrialIntroAudioUrl(), paper.getTrialIntroAudioPath()));
            } else {
                manifest.put("trialIntroAudioPath", null);
            }
            manifest.put("trialIntroAudioDuration", paper.getTrialIntroAudioDuration());
            manifest.put("operateListenText", paper.getOperateListenText());
            manifest.put("operateListenImageUrl", paper.getOperateListenImageUrl());
            // 使用相对路径（从URL或Path中提取文件名）
            if (StringUtils.isNotEmpty(paper.getOperateListenImageUrl()) || StringUtils.isNotEmpty(paper.getOperateListenImagePath())) {
                manifest.put("operateListenImagePath", 
                        "operate_listen/" + getMediaFileName(paper.getOperateListenImageUrl(), paper.getOperateListenImagePath()));
            } else {
                manifest.put("operateListenImagePath", null);
            }
            manifest.put("trialListenMedia", buildMediaList(trialMedia, "trial_listen/"));
        } else {
            manifest.put("trialListenEnabled", false);
        }

        // 3. 卷别列表（从 paper_volume 表查询，包含卷别媒体）
        List<Map<String, Object>> volumesData = new ArrayList<>();
        for (PaperVolume volume : volumes) {
            Map<String, Object> volumeData = new HashMap<>();
            volumeData.put("id", volume.getId()); // 添加 volume ID
            volumeData.put("volumeCode", volume.getVolumeCode());
            volumeData.put("volumeName", volume.getVolumeName());
            volumeData.put("volumeOrder", volume.getVolumeOrder());

            // 从 PaperVolume 实体字段获取卷别音频信息（不是从 question_media 表）
            if (StringUtils.isNotEmpty(volume.getVolumeAudioUrl())
                    || StringUtils.isNotEmpty(volume.getVolumeAudioPath())) {
                Map<String, Object> volumeMediaData = new HashMap<>();
                volumeMediaData.put("mediaUrl", volume.getVolumeAudioUrl());
                volumeMediaData.put("mediaPath",
                        "volumes/" + getMediaFileName(volume.getVolumeAudioUrl(), volume.getVolumeAudioPath()));
                volumeMediaData.put("mediaDuration", volume.getVolumeAudioDuration());
                volumeData.put("volumeAudio", volumeMediaData);
            } else {
                volumeData.put("volumeAudio", null);
            }
            volumesData.add(volumeData);
        }
        manifest.put("volumes", volumesData);

        // 4. 中场配置（从 paper_intermission 表查询，包含中场媒体）
        List<Map<String, Object>> intermissionsData = new ArrayList<>();
        for (PaperIntermission intermission : intermissions) {
            Map<String, Object> intermissionData = new HashMap<>();
            intermissionData.put("id", intermission.getId());
            intermissionData.put("fromVolumeId", intermission.getFromVolumeId());
            intermissionData.put("toVolumeId", intermission.getToVolumeId());
            intermissionData.put("fromVolume", intermission.getFromVolume());
            intermissionData.put("toVolume", intermission.getToVolume());
            intermissionData.put("intermissionText", intermission.getIntermissionText());
            intermissionData.put("canSkip", intermission.getCanSkip() != null && intermission.getCanSkip() == 1);

            // 从 PaperIntermission 实体字段获取中场音频信息（不是从 question_media 表）
            if (StringUtils.isNotEmpty(intermission.getIntermissionAudioUrl())
                    || StringUtils.isNotEmpty(intermission.getIntermissionAudioPath())) {
                intermissionData.put("intermissionAudioUrl", intermission.getIntermissionAudioUrl());
                intermissionData.put("intermissionAudioPath",
                        "intermission/" + getMediaFileName(intermission.getIntermissionAudioUrl(),
                                intermission.getIntermissionAudioPath()));
                intermissionData.put("intermissionAudioDuration", intermission.getIntermissionAudioDuration());
            } else {
                intermissionData.put("intermissionAudioUrl", null);
                intermissionData.put("intermissionAudioPath", null);
                intermissionData.put("intermissionAudioDuration", null);
            }
            intermissionsData.add(intermissionData);
        }
        manifest.put("intermissions", intermissionsData);

        // 5. 大题列表（从 paper_section 表查询，包含大题说明媒体）
        List<Map<String, Object>> sectionsData = new ArrayList<>();
        for (PaperSection section : sections) {
            Map<String, Object> sectionData = new HashMap<>();
            sectionData.put("id", section.getId());
            sectionData.put("volumeId", section.getVolumeId()); // 添加 volumeId，用于与 volumes 的 id 匹配
            // 保留 volumeCode 用于显示（可选）
            sectionData.put("volumeCode", section.getVolumeCode() != null ? section.getVolumeCode() : "");
            sectionData.put("sectionName", section.getSectionName());
            sectionData.put("sectionOrder", section.getSectionOrder());
            sectionData.put("questionCount", section.getQuestionCount());
            sectionData.put("totalScore", section.getTotalScore());
            sectionData.put("instructionText", section.getInstructionText());
            sectionData.put("answerTime", section.getAnswerTime() != null ? section.getAnswerTime() : 5);
            sectionData.put("audioPlayCount", section.getAudioPlayCount() != null ? section.getAudioPlayCount() : 1);

            // 从 PaperSection 实体字段获取大题说明音频信息（不是从 question_media 表）
            if (StringUtils.isNotEmpty(section.getInstructionAudioUrl())
                    || StringUtils.isNotEmpty(section.getInstructionAudioPath())) {
                sectionData.put("instructionAudioUrl", section.getInstructionAudioUrl());
                sectionData.put("instructionAudioPath", "sections/"
                        + getMediaFileName(section.getInstructionAudioUrl(), section.getInstructionAudioPath()));
                sectionData.put("instructionAudioDuration", section.getInstructionAudioDuration());
            } else {
                sectionData.put("instructionAudioUrl", null);
                sectionData.put("instructionAudioPath", null);
                sectionData.put("instructionAudioDuration", null);
            }

            // 查询大题下的题目ID列表（按 section_order 排序）
            List<Integer> questionIds = paperQuestions.stream()
                    .filter(pq -> pq.getSectionId() != null && pq.getSectionId().equals(section.getId()))
                    .sorted(Comparator.comparing(PaperQuestion::getSectionOrder,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(PaperQuestion::getQuestionId)
                    .collect(Collectors.toList());
            sectionData.put("questions", questionIds);

            sectionsData.add(sectionData);
        }
        manifest.put("sections", sectionsData);

        manifest.put("version", paper.getVersion() != null ? paper.getVersion() : 0);
        manifest.put("createTime", paper.getCreateTime() != null ? paper.getCreateTime().getTime() : null);
        manifest.put("updateTime", paper.getUpdateTime() != null ? paper.getUpdateTime().getTime() : null);

        return manifest;
    }

    /**
     * 构建媒体文件列表（用于JSON序列化）
     */
    private List<Map<String, Object>> buildMediaList(List<QuestionMedia> mediaList, String basePath) {
        return mediaList.stream().map(media -> {
            Map<String, Object> mediaData = new HashMap<>();
            mediaData.put("mediaType", media.getMediaType());
            mediaData.put("mediaPath", basePath + media.getMediaName()); // 构建ZIP中的相对路径
            mediaData.put("mediaFormat", media.getMediaFormat());
            if (media.getMediaDuration() != null) {
                mediaData.put("mediaDuration", media.getMediaDuration());
            }
            return mediaData;
        }).collect(Collectors.toList());
    }

    /**
     * 构建 questions.json 数据（使用新的表结构）
     */
    private List<Map<String, Object>> buildQuestionsData(List<Integer> questionIds,
            List<PaperQuestion> paperQuestions) throws ServiceException {
        List<Map<String, Object>> questionsData = new ArrayList<>();

        // 构建题目ID到sectionId和sectionOrder的映射
        Map<Integer, PaperQuestion> questionMap = paperQuestions.stream()
                .collect(Collectors.toMap(PaperQuestion::getQuestionId, pq -> pq, (v1, v2) -> v1));

        for (Integer questionId : questionIds) {
            // 通过QuestionService获取题目详情（包含答案选项等完整信息）
            com.ruoyi.student.archive.domain.bo.question.QuestionIdBO idBO = new com.ruoyi.student.archive.domain.bo.question.QuestionIdBO();
            idBO.setId(questionId);

            com.ruoyi.student.archive.domain.dto.question.QuestionInfoDTO questionDTO;
            try {
                questionDTO = questionService.getQuestion(idBO);
            } catch (Exception e) {
                log.warn("获取题目详情失败，跳过：questionId={}, error={}", questionId, e.getMessage());
                continue;
            }

            if (questionDTO == null) {
                log.warn("题目不存在，跳过：{}", questionId);
                continue;
            }

            PaperQuestion paperQuestion = questionMap.get(questionId);

            Map<String, Object> questionData = new HashMap<>();
            questionData.put("id", questionDTO.getId());
            questionData.put("sectionId", paperQuestion != null ? paperQuestion.getSectionId() : null);
            questionData.put("sectionOrder", paperQuestion != null ? paperQuestion.getSectionOrder() : null);
            questionData.put("title", questionDTO.getTitle());
            questionData.put("type", questionDTO.getType());
            questionData.put("explanationEnabled",
                    questionDTO.getExplanationEnabled() != null && questionDTO.getExplanationEnabled() == 1);
            questionData.put("explanationText", questionDTO.getExplanationText());
            questionData.put("explanationDelaySeconds",
                    questionDTO.getExplanationDelaySeconds() != null ? questionDTO.getExplanationDelaySeconds() : 2);

            // 查询题目媒体文件（media_type=1,4,5,6）
            // 注意：兼容旧数据，media_type=1 且 option_id=null 也表示题目音频
            List<QuestionMedia> questionMedia = questionMediaBiz.list(
                    new LambdaQueryWrapper<QuestionMedia>()
                            .eq(QuestionMedia::getQuestionId, questionId)
                            .in(QuestionMedia::getMediaType, Arrays.asList(1, 4, 5, 6))
                            .isNull(QuestionMedia::getOptionId)); // 排除选项媒体

            // 分别处理题目音频（media_type=1或4）和讲解音频（media_type=5）
            // 按题目ID分组存储，路径格式：questions/q_{questionId}/文件名.mp3
            List<Map<String, Object>> mediaList = new ArrayList<>();
            for (QuestionMedia media : questionMedia) {
                Map<String, Object> mediaData = new HashMap<>();
                // 兼容处理：将 media_type=1 转换为 media_type=4（题目音频）
                Integer mediaType = media.getMediaType();
                if (mediaType == 1) {
                    mediaType = 4; // 统一转换为题目音频类型
                }
                mediaData.put("mediaType", mediaType);

                // 根据转换后的 mediaType 确定路径，包含题目ID目录
                String mediaPath;
                if (mediaType == 4) {
                    // 题目音频：questions/q_{questionId}/文件名.mp3
                    mediaPath = "questions/q_" + questionId + "/" + media.getMediaName();
                } else if (mediaType == 5) {
                    // 讲解音频：explanations/q_{questionId}/文件名.mp3
                    mediaPath = "explanations/q_" + questionId + "/" + media.getMediaName();
                } else {
                    // 其他类型：questions/q_{questionId}/文件名.mp3
                    mediaPath = "questions/q_" + questionId + "/" + media.getMediaName();
                }

                mediaData.put("mediaPath", mediaPath);
                mediaData.put("mediaFormat", media.getMediaFormat());
                if (media.getMediaDuration() != null) {
                    mediaData.put("mediaDuration", media.getMediaDuration());
                }
                mediaList.add(mediaData);
            }
            questionData.put("media", mediaList);

            // 获取答案选项（包含选项媒体 media_type=2）
            if (questionDTO.getAnswers() != null && !questionDTO.getAnswers().isEmpty()) {
                List<Map<String, Object>> answersData = new ArrayList<>();
                for (com.ruoyi.student.archive.domain.dto.question.QuestionAnswerDTO answer : questionDTO
                        .getAnswers()) {
                    Map<String, Object> answerData = new HashMap<>();
                    answerData.put("id", answer.getId());
                    answerData.put("text", answer.getOptionContent());
                    // 添加选项名称（A、B、C...）
                    answerData.put("optionName", answer.getOptionName());
                    // 添加是否正确答案标识（2-正确答案）
                    answerData.put("isAnswer", answer.getIsAnswer() != null && answer.getIsAnswer() == 2);

                    // 查询选项媒体文件（media_type=2）
                    List<QuestionMedia> optionMedia = questionMediaBiz.list(
                            new LambdaQueryWrapper<QuestionMedia>()
                                    .eq(QuestionMedia::getOptionId, answer.getId())
                                    .eq(QuestionMedia::getMediaType, 2));

                    // 选项音频：按题目ID分组存储，路径格式：options/q_{questionId}/文件名.mp3
                    List<Map<String, Object>> optionMediaList = optionMedia.stream().map(media -> {
                        Map<String, Object> mediaData = new HashMap<>();
                        mediaData.put("mediaType", media.getMediaType());
                        // 选项音频路径包含题目ID目录
                        mediaData.put("mediaPath", "options/q_" + questionId + "/" + media.getMediaName());
                        mediaData.put("mediaFormat", media.getMediaFormat());
                        if (media.getMediaDuration() != null) {
                            mediaData.put("mediaDuration", media.getMediaDuration());
                        }
                        return mediaData;
                    }).collect(Collectors.toList());

                    answerData.put("media", optionMediaList);
                    answersData.add(answerData);
                }
                questionData.put("answers", answersData);
            }

            questionsData.add(questionData);
        }

        return questionsData;
    }

    /**
     * 下载并打包媒体文件（使用新的表结构和 media_type）
     */
    private void downloadAndPackMediaFiles(ZipOutputStream zos, Paper paper,
            List<PaperVolume> volumes,
            List<PaperSection> sections,
            List<PaperIntermission> intermissions,
            List<Integer> questionIds) throws Exception {

        // 1. 试听媒体（从 question_media 表查询 media_type=10,11，以及从 Paper 实体字段获取试听音频）
        List<QuestionMedia> trialMedia = questionMediaBiz.list(
                new LambdaQueryWrapper<QuestionMedia>()
                        .eq(QuestionMedia::getPaperId, paper.getId())
                        .in(QuestionMedia::getMediaType, Arrays.asList(10, 11)));
        packMediaFiles(zos, trialMedia, "trial_listen/");

        // 试听音频（从 Paper 实体字段获取）
        // 试听旁白音频（介绍音频，自动播放）- 独立判断，不依赖trialListenEnabled
        if (StringUtils.isNotEmpty(paper.getTrialIntroAudioUrl())
                || StringUtils.isNotEmpty(paper.getTrialIntroAudioPath())) {
            packMediaFileFromEntity(zos, paper.getTrialIntroAudioUrl(), paper.getTrialIntroAudioPath(),
                    "trial_intro/");
        }
        
        // 试听相关音频和图片（仅在trialListenEnabled为1时打包）
        if (paper.getTrialListenEnabled() != null && paper.getTrialListenEnabled() == 1) {
            // 试听音频（用户点击播放）
            if (StringUtils.isNotEmpty(paper.getTrialListenAudioUrl())
                    || StringUtils.isNotEmpty(paper.getTrialListenAudioPath())) {
                packMediaFileFromEntity(zos, paper.getTrialListenAudioUrl(), paper.getTrialListenAudioPath(),
                        "trial_listen/");
            }
            // 操作提示图片
            if (StringUtils.isNotEmpty(paper.getOperateListenImageUrl())
                    || StringUtils.isNotEmpty(paper.getOperateListenImagePath())) {
                packMediaFileFromEntity(zos, paper.getOperateListenImageUrl(), paper.getOperateListenImagePath(),
                        "operate_listen/");
            }
        }

        // 注意事项音频（intro/）
        if (StringUtils.isNotEmpty(paper.getIntroAudioUrl())
                || StringUtils.isNotEmpty(paper.getIntroAudioPath())) {
            packMediaFileFromEntity(zos, paper.getIntroAudioUrl(), paper.getIntroAudioPath(), "intro/");
        }

        // 2. 卷别名称音频（从 PaperVolume 实体字段获取）
        for (PaperVolume volume : volumes) {
            if (StringUtils.isNotEmpty(volume.getVolumeAudioUrl())
                    || StringUtils.isNotEmpty(volume.getVolumeAudioPath())) {
                packMediaFileFromEntity(zos, volume.getVolumeAudioUrl(), volume.getVolumeAudioPath(), "volumes/");
            }
        }

        // 3. 大题说明音频（从 PaperSection 实体字段获取）
        for (PaperSection section : sections) {
            if (StringUtils.isNotEmpty(section.getInstructionAudioUrl())
                    || StringUtils.isNotEmpty(section.getInstructionAudioPath())) {
                packMediaFileFromEntity(zos, section.getInstructionAudioUrl(), section.getInstructionAudioPath(),
                        "sections/");
            }
        }

        // 4. 中场音频（从 PaperIntermission 实体字段获取）
        for (PaperIntermission intermission : intermissions) {
            if (StringUtils.isNotEmpty(intermission.getIntermissionAudioUrl())
                    || StringUtils.isNotEmpty(intermission.getIntermissionAudioPath())) {
                packMediaFileFromEntity(zos, intermission.getIntermissionAudioUrl(),
                        intermission.getIntermissionAudioPath(), "intermission/");
            }
        }

        // 5. 题目音频和讲解（media_type=1,4,5,6，关联到 question）
        // 注意：兼容旧数据，media_type=1 且 option_id=null 也表示题目音频
        log.info("开始打包题目音频，题目总数: {}", questionIds.size());
        int questionAudioCount = 0;
        int questionWithoutMediaCount = 0;
        for (Integer questionId : questionIds) {
            List<QuestionMedia> questionMedia = questionMediaBiz.list(
                    new LambdaQueryWrapper<QuestionMedia>()
                            .eq(QuestionMedia::getQuestionId, questionId)
                            .in(QuestionMedia::getMediaType, Arrays.asList(1, 4, 5, 6))
                            .isNull(QuestionMedia::getOptionId)); // 排除选项媒体

            if (!questionMedia.isEmpty()) {
                log.info("题目 {} 找到 {} 个媒体文件（media_type=1,4,5,6）", questionId, questionMedia.size());
            } else {
                log.warn("题目 {} 没有找到媒体文件（media_type=1,4,5,6，且option_id=null），将不会打包题目音频", questionId);
                questionWithoutMediaCount++;
            }

            // 分别处理题目音频和讲解音频，按题目ID分组存储
            for (QuestionMedia media : questionMedia) {
                String basePath;
                // 兼容处理：media_type=1 或 4 都视为题目音频
                if (media.getMediaType() == 1 || media.getMediaType() == 4) {
                    // 题目音频：存储到 questions/q_{questionId}/ 目录
                    basePath = "questions/q_" + questionId + "/";
                    questionAudioCount++;
                    log.info("打包题目音频: questionId={}, mediaType={}, mediaName={}, path={}", 
                            questionId, media.getMediaType(), media.getMediaName(), basePath);
                } else if (media.getMediaType() == 5) {
                    // 讲解音频：存储到 explanations/q_{questionId}/ 目录
                    basePath = "explanations/q_" + questionId + "/";
                } else {
                    // 其他类型：存储到 questions/q_{questionId}/ 目录
                    basePath = "questions/q_" + questionId + "/";
                }
                packMediaFiles(zos, Arrays.asList(media), basePath);
            }
        }
        log.info("题目音频打包完成，共打包 {} 个题目音频文件，{} 个题目没有媒体文件", questionAudioCount, questionWithoutMediaCount);

        // 6. 选项音频（media_type=2，关联到 question_answer）
        for (Integer questionId : questionIds) {
            // 获取题目的所有答案选项
            com.ruoyi.student.archive.domain.bo.question.QuestionIdBO idBO = new com.ruoyi.student.archive.domain.bo.question.QuestionIdBO();
            idBO.setId(questionId);

            com.ruoyi.student.archive.domain.dto.question.QuestionInfoDTO questionDTO;
            questionDTO = questionService.getQuestion(idBO);

            if (questionDTO != null && questionDTO.getAnswers() != null) {
                for (com.ruoyi.student.archive.domain.dto.question.QuestionAnswerDTO answer : questionDTO
                        .getAnswers()) {
                    List<QuestionMedia> optionMedia = questionMediaBiz.list(
                            new LambdaQueryWrapper<QuestionMedia>()
                                    .eq(QuestionMedia::getOptionId, answer.getId())
                                    .eq(QuestionMedia::getMediaType, 2));
                    // 选项音频：存储到 options/q_{questionId}/ 目录
                    packMediaFiles(zos, optionMedia, "options/q_" + questionId + "/");
                }
            }
        }
    }

    /**
     * 打包媒体文件到ZIP（从QuestionMedia列表）
     */
    private void packMediaFiles(ZipOutputStream zos, List<QuestionMedia> mediaList, String basePath) throws Exception {
        for (QuestionMedia media : mediaList) {
            if (StringUtils.isEmpty(media.getMediaUrl()) && StringUtils.isEmpty(media.getMediaPath())) {
                log.warn("媒体文件URL和Path都为空，跳过: mediaId={}, mediaName={}", media.getId(), media.getMediaName());
                continue;
            }

            try {
                // 从OSS下载文件
                String objectKey = StringUtils.isNotEmpty(media.getMediaPath())
                        ? media.getMediaPath()
                        : ossUtil.getObjectKey(media.getMediaUrl());

                log.info("开始下载媒体文件: mediaId={}, objectKey={}, basePath={}", media.getId(), objectKey, basePath);

                byte[] fileBytes = ossUtil.downloadFileToBytes(objectKey);

                // 构建ZIP中的文件路径
                String zipPath = basePath + media.getMediaName();

                // 添加到ZIP
                ZipEntry entry = new ZipEntry(zipPath);
                entry.setSize(fileBytes.length);
                entry.setTime(System.currentTimeMillis());
                zos.putNextEntry(entry);
                zos.write(fileBytes, 0, fileBytes.length);
                zos.closeEntry();

                log.info("媒体文件已添加到ZIP：{}, 大小: {} 字节", zipPath, fileBytes.length);

            } catch (Exception e) {
                log.error("下载媒体文件失败：mediaId={}, mediaName={}, url={}, path={}, basePath={}", 
                        media.getId(), media.getMediaName(), media.getMediaUrl(), media.getMediaPath(), basePath, e);
                // 继续处理下一个文件，不中断整个打包流程
            }
        }
    }

    /**
     * 从实体字段打包单个媒体文件到ZIP
     */
    private void packMediaFileFromEntity(ZipOutputStream zos, String mediaUrl, String mediaPath, String basePath)
            throws Exception {
        if (StringUtils.isEmpty(mediaUrl) && StringUtils.isEmpty(mediaPath)) {
            return;
        }

        String urlOrPath = StringUtils.isNotEmpty(mediaPath) ? mediaPath : mediaUrl;
        String objectKey = null;
        byte[] fileBytes = null;

        try {
            // 从 OSS 下载文件
            // 注意：mediaPath 和 mediaUrl 都可能是完整 URL，需要提取对象键
            objectKey = ossUtil.getObjectKey(urlOrPath);

            log.info("下载媒体文件，原始路径: {}, 提取的对象键: {}, basePath: {}", urlOrPath, objectKey, basePath);

            // 先检查文件是否存在（可选，用于提前发现问题）
            try {
                boolean exists = ossUtil.exists(objectKey);
                if (!exists) {
                    log.warn("文件不存在（通过exists检查），对象键: {}, 原始路径: {}", objectKey, urlOrPath);
                    // 不立即失败，继续尝试下载，因为某些OSS服务可能不支持exists检查
                } else {
                    log.info("文件存在（通过exists检查），对象键: {}", objectKey);
                }
            } catch (Exception e) {
                log.warn("检查文件是否存在失败，继续尝试下载: {}", e.getMessage());
            }

            // 下载文件（带超时控制）
            long startTime = System.currentTimeMillis();
            try {
                fileBytes = ossUtil.downloadFileToBytes(objectKey);
                long downloadTime = System.currentTimeMillis() - startTime;
                log.info("文件下载成功（通过OSS），对象键: {}, 大小: {} 字节, 耗时: {} 毫秒", objectKey, fileBytes.length, downloadTime);
            } catch (Exception e) {
                // 如果OSS下载失败，且原始URL是HTTP/HTTPS URL，尝试直接下载
                if ((urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) 
                        && (e.getMessage() == null || e.getMessage().contains("404") || e.getMessage().contains("不存在"))) {
                    log.warn("OSS下载失败，尝试直接使用原始URL下载: {}", urlOrPath);
                    fileBytes = downloadFileFromUrl(urlOrPath);
                    long downloadTime = System.currentTimeMillis() - startTime;
                    log.info("文件下载成功（通过直接URL），大小: {} 字节, 耗时: {} 毫秒", fileBytes.length, downloadTime);
                } else {
                    throw e;
                }
            }

            // 验证下载的文件不为空
            if (fileBytes == null || fileBytes.length == 0) {
                throw new ServiceException("下载的文件为空，对象键: " + objectKey);
            }

            // 构建 ZIP 中的文件路径
            String fileName = getMediaFileName(mediaUrl, mediaPath);
            String zipPath = basePath + fileName;

            // 添加到 ZIP
            ZipEntry entry = new ZipEntry(zipPath);
            entry.setSize(fileBytes.length);
            entry.setTime(System.currentTimeMillis());
            zos.putNextEntry(entry);
            zos.write(fileBytes, 0, fileBytes.length);
            zos.closeEntry();

            log.info("媒体文件已添加到 ZIP：{}, 大小: {} 字节", zipPath, fileBytes.length);

        } catch (com.ruoyi.common.exception.ServiceException e) {
            // 业务异常，直接抛出
            log.error("下载媒体文件失败（业务异常），url={}, path={}, objectKey={}, 错误: {}", 
                    mediaUrl, mediaPath, objectKey, e.getMessage());
            throw e;
        } catch (java.net.SocketTimeoutException e) {
            // 超时异常
            log.error("下载媒体文件超时，url={}, path={}, objectKey={}, 错误: {}", 
                    mediaUrl, mediaPath, objectKey, e.getMessage());
            throw new ServiceException("下载媒体文件超时，请检查网络连接或文件大小: " + e.getMessage());
        } catch (java.io.IOException e) {
            // IO异常（可能是404、403等）
            String errorMsg = e.getMessage();
            log.error("下载媒体文件IO异常，url={}, path={}, objectKey={}, 错误: {}", 
                    mediaUrl, mediaPath, objectKey, errorMsg);
            
            if (errorMsg != null && (errorMsg.contains("404") || errorMsg.contains("不存在"))) {
                log.error("404错误 - 文件不存在！原始路径: {}, 提取的对象键: {}", urlOrPath, objectKey);
                log.error("请检查：1) 文件是否已上传到OSS 2) 对象键是否正确 3) 文件是否已被删除");
                log.error("提示：如果浏览器可以访问该URL，可能是对象键提取不正确，请检查 extractObjectKey 方法");
            } else if (errorMsg != null && errorMsg.contains("403")) {
                log.error("403错误 - 访问被拒绝！原始路径: {}, 提取的对象键: {}", urlOrPath, objectKey);
                log.error("请检查：1) OSS存储空间是否为私有空间 2) AccessKey和SecretKey是否正确 3) 签名是否正确");
            }
            
            throw new ServiceException("下载媒体文件失败: " + errorMsg);
        } catch (Exception e) {
            // 其他异常
            log.error("下载媒体文件失败（未知异常），url={}, path={}, objectKey={}, 错误类型: {}, 错误信息: {}", 
                    mediaUrl, mediaPath, objectKey, e.getClass().getName(), e.getMessage(), e);
            throw new ServiceException("下载媒体文件失败: " + e.getMessage());
        }
    }

    /**
     * 直接从HTTP/HTTPS URL下载文件（备用方案）
     * 当OSS下载失败时使用
     */
    private byte[] downloadFileFromUrl(String url) throws Exception {
        log.info("开始从URL直接下载文件: {}", url);
        
        java.net.URL fileUrl = new java.net.URL(url);
        java.net.URLConnection urlConnection = fileUrl.openConnection();
        
        // 设置超时（避免卡死）
        urlConnection.setConnectTimeout(10000); // 10秒连接超时
        urlConnection.setReadTimeout(120000); // 120秒读取超时
        
        // 如果是HTTPS连接，需要处理SSL证书验证
        if (urlConnection instanceof javax.net.ssl.HttpsURLConnection) {
            javax.net.ssl.HttpsURLConnection httpsConnection = (javax.net.ssl.HttpsURLConnection) urlConnection;
            
            // 创建SSL上下文，跳过证书验证（仅用于解决证书域名不匹配问题）
            javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
            sslContext.init(null, new javax.net.ssl.TrustManager[] { 
                new javax.net.ssl.X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[] {};
                    }
                }
            }, new java.security.SecureRandom());
            httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
            httpsConnection.setHostnameVerifier((hostname, session) -> true);
        }
        
        // 设置请求头
        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0");
        
        if (urlConnection instanceof java.net.HttpURLConnection) {
            java.net.HttpURLConnection httpConnection = (java.net.HttpURLConnection) urlConnection;
            httpConnection.setRequestMethod("GET");
            httpConnection.connect();
            
            int responseCode = httpConnection.getResponseCode();
            log.info("HTTP响应码: {}", responseCode);
            
            if (responseCode != 200) {
                throw new ServiceException("从URL下载文件失败，HTTP状态码: " + responseCode);
            }
            
            try (InputStream inputStream = httpConnection.getInputStream()) {
                return org.apache.commons.io.IOUtils.toByteArray(inputStream);
            } finally {
                httpConnection.disconnect();
            }
        } else {
            urlConnection.connect();
            try (InputStream inputStream = urlConnection.getInputStream()) {
                return org.apache.commons.io.IOUtils.toByteArray(inputStream);
            }
        }
    }

    /**
     * 从URL或路径提取文件名
     */
    private String getMediaFileName(String mediaUrl, String mediaPath) {
        if (StringUtils.isNotEmpty(mediaPath)) {
            // 从路径中提取文件名
            int lastSlash = mediaPath.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < mediaPath.length() - 1) {
                return mediaPath.substring(lastSlash + 1);
            }
            return mediaPath;
        } else if (StringUtils.isNotEmpty(mediaUrl)) {
            // 从URL中提取文件名
            int lastSlash = mediaUrl.lastIndexOf('/');
            int queryIndex = mediaUrl.indexOf('?');
            String urlPath = queryIndex > 0 ? mediaUrl.substring(0, queryIndex) : mediaUrl;
            if (lastSlash >= 0 && lastSlash < urlPath.length() - 1) {
                return urlPath.substring(lastSlash + 1);
            }
            return urlPath;
        }
        return "unknown";
    }

    /**
     * 添加JSON数据到ZIP
     */
    private void addJsonToZip(ZipOutputStream zos, String entryName, Object data) throws Exception {
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        ZipEntry entry = new ZipEntry(entryName);
        entry.setSize(jsonBytes.length);
        entry.setTime(System.currentTimeMillis()); // 设置时间戳，确保ZIP格式正确
        zos.putNextEntry(entry);
        zos.write(jsonBytes, 0, jsonBytes.length);
        zos.closeEntry();
    }

    /**
     * 计算文件哈希值（SHA256）
     */
    public String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("计算哈希值失败", e);
            return null;
        }
    }
}
