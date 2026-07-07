package com.zx.student.archive.service.paper.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zx.common.exception.ServiceException;
import com.zx.common.utils.SecurityUtils;
import com.zx.common.utils.oss.OssServiceFactory;
import com.zx.student.archive.biz.question.QuestionAnswerBiz;
import com.zx.student.archive.biz.question.QuestionBiz;
import com.zx.student.archive.domain.bo.paper.*;
import com.zx.student.archive.domain.dto.paper.*;
import com.zx.student.archive.domain.paper.PaperImportSession;
import com.zx.student.archive.domain.question.Question;
import com.zx.student.archive.domain.question.QuestionAnswer;
import com.zx.student.archive.mapper.paper.PaperImportSessionMapper;
import com.zx.student.archive.service.paper.IPaperImportService;
import com.zx.student.archive.service.paper.IPaperService;
import com.zx.system.service.ISysDictDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 试卷智能导入服务实现
 *
 * @author zx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperImportServiceImpl implements IPaperImportService {

    private final RestTemplate restTemplate;
    private final IPaperService paperService;
    private final ObjectMapper objectMapper;
    private final OssServiceFactory ossServiceFactory;
    private final QuestionBiz questionBiz;
    private final QuestionAnswerBiz questionAnswerBiz;
    private final PaperImportSessionMapper importSessionMapper;
    private final ISysDictDataService dictDataService;
    private final com.zx.student.archive.biz.question.IQuestionMediaBiz questionMediaBiz;
    private final com.zx.student.archive.biz.question.IQuestionGroupBiz questionGroupBiz;

    @Value("${paper.import.parse-service-url:http://localhost:8088}")
    private String parseServiceUrl;

    /** 会话过期时间（小时） */
    private static final int SESSION_EXPIRE_HOURS = 24;

    @Override
    public ParseResultDTO parseFiles(MultipartFile wordFile, MultipartFile audioFile, String audioOssUrl) {
        try {
            // 构建 multipart 请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // 添加 Word 文件
            body.add("word_file", new MultipartFileResource(wordFile));

            // 添加音频文件（如果有）
            if (audioFile != null && !audioFile.isEmpty()) {
                body.add("audio_file", new MultipartFileResource(audioFile));
            }

            // 添加音频 OSS URL（用于 ASR 识别）
            // 注意：七牛云私有 Bucket 需要生成带签名的临时 URL
            if (audioOssUrl != null && !audioOssUrl.isEmpty()) {
                try {
                    // 从 URL 中提取对象键
                    String objectKey = ossServiceFactory.getOssService().extractObjectKey(audioOssUrl);
                    // 生成带签名的临时 URL（2小时有效期，ASR 处理可能需要较长时间）
                    String presignedUrl = ossServiceFactory.getOssService().generatePresignedUrl(objectKey, 7200);
                    body.add("audio_oss_url", presignedUrl);
                    log.info("音频 OSS URL (已签名): {}", presignedUrl);
                } catch (Exception e) {
                    // 如果生成签名 URL 失败，使用原始 URL（可能是公开 Bucket）
                    log.warn("生成签名 URL 失败，使用原始 URL: {}", e.getMessage());
                    body.add("audio_oss_url", audioOssUrl);
                }
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 调用 Python 服务
            String url = parseServiceUrl + "/parse/word-audio";
            log.info("调用解析服务: {}", url);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                // DEBUG: 打印完整响应数据以排查问题
                try {
                    log.info("PYTHON_RAW_RESPONSE_DUMP: {}", objectMapper.writeValueAsString(responseBody));
                } catch (Exception e) {
                    log.error("Failed to dump response", e);
                }

                // 转换为 DTO
                ParseResultDTO result = convertToParseResult(responseBody);
                log.info("解析完成，题目数量: {}", result.getQuestionCount());
                return result;
            } else {
                throw new ServiceException("解析服务返回失败");
            }

        } catch (Exception e) {
            log.error("调用解析服务失败", e);
            throw new ServiceException("解析失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer confirmImport(ImportConfirmBO confirmBO) {
        try {
            // 获取完整的试卷数据
            PaperFullDataBO fullData = confirmBO.getFullData();

            if (fullData == null) {
                throw new ServiceException("试卷数据不能为空");
            }

            // 设置试卷基本信息
            if (fullData.getPaper() == null) {
                fullData.setPaper(new PaperBasicInfoBO());
            }

            if (confirmBO.getPaperName() != null) {
                fullData.getPaper().setCustomName(confirmBO.getPaperName());
            }
            if (confirmBO.getBusinessType() != null) {
                fullData.getPaper().setBusinessType(confirmBO.getBusinessType());
            }
            if (confirmBO.getBusinessId() != null) {
                fullData.getPaper().setBusinessId(confirmBO.getBusinessId());
            }

            // 设置数据来源标识：智能导入的试卷标记为 asr_import
            fullData.getPaper().setRemark("asr_import");

            // 调用现有服务创建试卷
            Integer paperId = paperService.createPaperWithFullData(fullData);
            log.info("试卷导入成功，ID: {}", paperId);

            return paperId;

        } catch (Exception e) {
            log.error("导入试卷失败", e);
            throw new ServiceException("导入失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isParseServiceAvailable() {
        try {
            String url = parseServiceUrl + "/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("解析服务不可用: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportQuestionsResultDTO createQuestions(ParseResultDTO parseResult, Integer categoryId, Integer subjectId,
            Integer defaultQuestionType, Boolean listeningOnly) {
        ImportQuestionsResultDTO result = new ImportQuestionsResultDTO();
        List<Integer> questionIds = new ArrayList<>();

        try {
            // 遍历所有卷别和大题中的题目
            if (parseResult.getVolumes() != null) {
                for (ParseResultDTO.ParsedVolumeDTO volume : parseResult.getVolumes()) {
                    // 如果选择仅导入听力部分，过滤非听力卷别
                    if (Boolean.TRUE.equals(listeningOnly)) {
                        // 使用 Python 基于内容（音频）检测的 isListening 字段
                        Boolean isListening = volume.getIsListening();
                        // Fallback: 如果 Python 没有标记，则检查卷名是否包含"听力"
                        if (isListening == null) {
                            String volumeName = volume.getName() != null ? volume.getName() : "";
                            isListening = volumeName.contains("听力");
                        }
                        if (!Boolean.TRUE.equals(isListening)) {
                            log.debug("跳过非听力卷别: {}", volume.getName());
                            continue;
                        }
                    }

                    if (volume.getSections() != null) {
                        for (ParseResultDTO.ParsedSectionDTO section : volume.getSections()) {
                            // 调试日志：打印大题的音频时长
                            log.info("[createQuestionsFromParse] 大题 {} 音频时长: introAudioDuration={}",
                                    section.getName(), section.getIntroAudioDuration());

                            // 创建 section 直接包含的题目
                            if (section.getQuestions() != null) {
                                for (ParseResultDTO.ParsedQuestionDTO parsedQ : section.getQuestions()) {
                                    Integer questionId = createSingleQuestion(parsedQ, categoryId, subjectId,
                                            defaultQuestionType);
                                    questionIds.add(questionId);
                                    parsedQ.setQuestionId(questionId);
                                }
                            }

                            // 创建题目组中的题目，并创建题目组记录
                            if (section.getQuestionGroups() != null) {
                                for (ParseResultDTO.ParsedQuestionGroupDTO group : section.getQuestionGroups()) {
                                    if (group.getQuestions() != null) {
                                        log.info("创建题目组 {}-{} 中的题目，数量: {}",
                                                group.getStartIndex(), group.getEndIndex(),
                                                group.getQuestions().size());

                                        // 收集题目组内的题目ID
                                        List<Integer> groupQuestionIds = new ArrayList<>();

                                        for (ParseResultDTO.ParsedQuestionDTO parsedQ : group.getQuestions()) {
                                            Integer questionId = createSingleQuestion(parsedQ, categoryId, subjectId,
                                                    defaultQuestionType);
                                            questionIds.add(questionId);
                                            groupQuestionIds.add(questionId);
                                            parsedQ.setQuestionId(questionId);
                                        }

                                        // 创建题目组记录
                                        com.zx.student.archive.domain.question.QuestionGroup questionGroup = new com.zx.student.archive.domain.question.QuestionGroup();
                                        questionGroup.setCategoryId(categoryId);
                                        questionGroup.setGroupName(group.getTitle() != null ? group.getTitle()
                                                : "Questions " + group.getStartIndex() + "-" + group.getEndIndex());
                                        questionGroup.setAudioUrl(group.getAudioUrl());
                                        questionGroup.setAudioPath(group.getAudioUrl()); // OSS时path与url相同
                                        questionGroup.setAudioDuration(
                                                group.getAudioDuration() != null ? group.getAudioDuration().intValue()
                                                        : null);
                                        questionGroup.setAudioLabel(
                                                "Questions " + group.getStartIndex() + "-" + group.getEndIndex());

                                        Integer groupId = questionGroupBiz.createGroup(questionGroup, groupQuestionIds);
                                        group.setQuestionGroupId(groupId); // 保存题目组ID供后续使用
                                        log.info("创建题目组记录: groupId={}, questionCount={}, audioUrl={}",
                                                groupId, groupQuestionIds.size(), group.getAudioUrl());
                                    }
                                }
                            }
                        }
                    }
                }
            }

            result.setSuccess(true);
            result.setMessage("题目创建成功");
            result.setCreatedCount(questionIds.size());
            result.setQuestionIds(questionIds);

            // 如果选择仅导入听力部分，过滤掉非听力卷别再返回
            if (Boolean.TRUE.equals(listeningOnly) && parseResult.getVolumes() != null) {
                List<ParseResultDTO.ParsedVolumeDTO> filteredVolumes = parseResult.getVolumes().stream()
                        .filter(vol -> {
                            // 使用 Python 基于内容（音频）检测的 isListening 字段
                            Boolean isListening = vol.getIsListening();
                            // Fallback: 如果 Python 没有标记，则检查卷名是否包含"听力"
                            if (isListening == null) {
                                String volumeName = vol.getName() != null ? vol.getName() : "";
                                isListening = volumeName.contains("听力");
                            }
                            return Boolean.TRUE.equals(isListening);
                        })
                        .collect(java.util.stream.Collectors.toList());
                parseResult.setVolumes(filteredVolumes);
            }
            result.setPaperStructure(parseResult); // 返回更新后的解析结果（包含 questionId）

            log.info("批量创建题目成功，数量: {}", questionIds.size());

        } catch (Exception e) {
            log.error("批量创建题目失败", e);
            throw new ServiceException("创建题目失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 创建单个题目并返回 questionId
     */
    private Integer createSingleQuestion(ParseResultDTO.ParsedQuestionDTO parsedQ, Integer categoryId,
            Integer subjectId, Integer defaultQuestionType) {
        // 计算内容哈希（用于查重/复用）
        String contentHash = calculateQuestionHash(parsedQ);
        log.debug("题目内容哈希: {} -> {}", parsedQ.getTitle(), contentHash);

        // 创建题目实体
        Question question = new Question();
        question.setQuestionCategoryId(categoryId);
        question.setSubjectId(subjectId != null ? subjectId : 3); // 默认英语（字典值3）
        question.setTitle(parsedQ.getTitle());

        // 题目类型：优先使用用户指定的默认类型，否则使用解析结果
        if (defaultQuestionType != null && defaultQuestionType >= 0) {
            question.setType(defaultQuestionType);
        } else {
            question.setType(parsedQ.getType() != null ? parsedQ.getType() : 0);
        }

        question.setMediaType(parsedQ.getHasAudio() != null && parsedQ.getHasAudio() ? 3 : 1); // 3-音频, 1-文本
        question.setOptionType(1); // 默认文本选项
        question.setAnswer(parsedQ.getAnswer());
        question.setAnalyzes(parsedQ.getAnalysis());
        question.setStatus(1); // 默认启用
        question.setWeight(1);

        // TODO: 如果 Question 实体增加了 hash 字段，可以在此处设置
        // question.setHash(contentHash);

        // 作文题特殊字段
        if (parsedQ.getType() != null && parsedQ.getType() == 6) {
            if (parsedQ.getWordLimit() != null && !parsedQ.getWordLimit().isEmpty()) {
                try {
                    question.setWordLimit(Integer.parseInt(parsedQ.getWordLimit()));
                } catch (NumberFormatException e) {
                    question.setWordLimit(0);
                }
            }
            question.setSampleAnswer(parsedQ.getAnswer());
        }

        // 保存题目
        Integer questionId = questionBiz.createQuestion(question);

        // 保存选项
        if (parsedQ.getOptions() != null && !parsedQ.getOptions().isEmpty()) {
            int serialNo = 1;
            for (ParseResultDTO.ParsedOptionDTO parsedOpt : parsedQ.getOptions()) {
                QuestionAnswer answer = new QuestionAnswer();
                answer.setQuestionId(questionId);
                answer.setSerialNo(serialNo++);
                answer.setOptionName(parsedOpt.getLabel());
                answer.setOptionContent(parsedOpt.getContent());
                answer.setIsAnswer(parsedOpt.getIsAnswer() != null && parsedOpt.getIsAnswer() ? 2 : 0);
                answer.setStatus(1);
                questionAnswerBiz.save(answer);
            }
        }

        // 保存单题音频（如果有）
        if (parsedQ.getAudioUrl() != null && !parsedQ.getAudioUrl().isEmpty()) {
            com.zx.student.archive.domain.question.QuestionMedia media = new com.zx.student.archive.domain.question.QuestionMedia();
            media.setQuestionId(questionId);
            media.setMediaType(4); // 4 = 题目音频
            media.setMediaUrl(parsedQ.getAudioUrl());
            media.setMediaPath(parsedQ.getAudioUrl()); // OSS存储时 path 与 url 相同
            // 从URL提取文件名作为 mediaName
            String audioUrl = parsedQ.getAudioUrl();
            String mediaName = audioUrl.substring(audioUrl.lastIndexOf('/') + 1);
            media.setMediaName(mediaName);
            // 从文件名提取格式
            if (mediaName.contains(".")) {
                media.setMediaFormat(mediaName.substring(mediaName.lastIndexOf('.') + 1));
            }
            if (parsedQ.getAudioDuration() != null) {
                media.setMediaDuration(parsedQ.getAudioDuration().intValue());
            }
            questionMediaBiz.save(media);
            log.info("创建题目音频成功: questionId={}, mediaName={}, audioUrl={}", questionId, mediaName,
                    parsedQ.getAudioUrl());
        }

        log.debug("创建题目成功: questionId={}, title={}, hash={}", questionId, parsedQ.getTitle(), contentHash);
        return questionId;
    }

    /**
     * 计算题目内容哈希 (MD5)
     * 规则: MD5(Type + Title + Options + Answer)
     */
    private String calculateQuestionHash(ParseResultDTO.ParsedQuestionDTO q) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(q.getType()).append("_");
            sb.append(q.getTitle()).append("_");
            if (q.getOptions() != null) {
                for (ParseResultDTO.ParsedOptionDTO opt : q.getOptions()) {
                    sb.append(opt.getLabel()).append(":").append(opt.getContent()).append("_");
                }
            }
            sb.append(q.getAnswer());

            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(sb.toString().getBytes("UTF-8"));

            // 转为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.warn("计算题目哈希失败", e);
            return "";
        }
    }

    /**
     * 将响应 Map 转换为 ParseResultDTO
     * 
     * Python 返回格式：
     * {
     * "success": true,
     * "message": "解析成功",
     * "data": {
     * "paper_name": "xxx",
     * "volumes": [...],
     * "questions": [...],
     * ...
     * }
     * }
     */
    @SuppressWarnings("unchecked")
    private ParseResultDTO convertToParseResult(Map<String, Object> response) {
        ParseResultDTO result = new ParseResultDTO();

        result.setSuccess((Boolean) response.getOrDefault("success", true));
        result.setMessage((String) response.get("message"));

        // 重要：实际数据在 "data" 字段中
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null) {
            log.warn("Python 响应中没有 data 字段，尝试直接从 response 读取");
            data = response;
        }

        log.info("解析响应数据: paper_name={}, question_count={}, volumes_count={}",
                data.get("paper_name"),
                data.get("question_count"),
                data.get("volumes") != null ? ((List<?>) data.get("volumes")).size() : 0);

        result.setPaperName((String) data.get("paper_name"));
        result.setQuestionCount(toInteger(data.get("question_count")));

        // 转换警告信息
        Object warningsObj = data.get("warnings");
        if (warningsObj instanceof List) {
            result.setWarnings((List<String>) warningsObj);
        }

        // 转换卷别
        Object volumesObj = data.get("volumes");
        if (volumesObj instanceof List) {
            result.setVolumes(convertVolumes((List<Map<String, Object>>) volumesObj));
        }

        // 转换题目
        Object questionsObj = data.get("questions");
        if (questionsObj instanceof List) {
            result.setQuestions(convertQuestions((List<Map<String, Object>>) questionsObj));
        }

        // 转换音频片段 - 暂时跳过，后续优化
        // TODO: 音频切分数量过多（177段 vs 81题），需要优化 VAD 参数
        Object audioChunksObj = data.get("audio_chunks");
        if (audioChunksObj instanceof List) {
            List<Map<String, Object>> audioChunks = (List<Map<String, Object>>) audioChunksObj;
            log.info("处理音频片段，共 {} 段", audioChunks.size());

            // 提取卷别音频（test_audio）和大题音频（section_intro）
            String testAudioUrl = null;
            Map<Integer, String> sectionIntroAudioMap = new HashMap<>(); // section_number -> oss_url

            for (Map<String, Object> chunk : audioChunks) {
                String chunkType = (String) chunk.get("chunk_type");
                String ossUrl = (String) chunk.get("oss_url");

                if ("test_audio".equals(chunkType) && ossUrl != null) {
                    testAudioUrl = ossUrl;
                    log.info("  提取卷别音频(test_audio): {}", ossUrl);
                } else if ("section_intro".equals(chunkType) && ossUrl != null) {
                    Integer sectionNumber = toInteger(chunk.get("number"));
                    sectionIntroAudioMap.put(sectionNumber, ossUrl);
                    log.info("  提取大题音频(section_intro_{}): {}", sectionNumber, ossUrl);
                }
            }

            // 填充卷别音频：test_audio -> 听力卷的 audioUrl
            if (testAudioUrl != null && result.getVolumes() != null) {
                for (ParseResultDTO.ParsedVolumeDTO volume : result.getVolumes()) {
                    // 只填充听力卷（基于内容检测或卷名判断）
                    Boolean isListening = volume.getIsListening();
                    String volumeName = volume.getName() != null ? volume.getName() : "";
                    if (Boolean.TRUE.equals(isListening) || volumeName.contains("听力") || volumeName.contains("III")) {
                        volume.setAudioUrl(testAudioUrl);
                        log.info("  填充卷别音频: {} -> {}", volume.getName(), testAudioUrl);
                    }
                }
            }

            // 填充大题音频：section_intro_N -> 听力卷第N个大题的 introAudioUrl
            if (!sectionIntroAudioMap.isEmpty() && result.getVolumes() != null) {
                for (ParseResultDTO.ParsedVolumeDTO volume : result.getVolumes()) {
                    // 只处理听力卷
                    Boolean isListening = volume.getIsListening();
                    String volumeName = volume.getName() != null ? volume.getName() : "";
                    if (!Boolean.TRUE.equals(isListening) && !volumeName.contains("听力")
                            && !volumeName.contains("III")) {
                        continue;
                    }

                    if (volume.getSections() != null) {
                        int sectionIndex = 1; // 大题序号从1开始
                        for (ParseResultDTO.ParsedSectionDTO section : volume.getSections()) {
                            String introAudioUrl = sectionIntroAudioMap.get(sectionIndex);
                            if (introAudioUrl != null) {
                                section.setIntroAudioUrl(introAudioUrl);
                                log.info("  填充大题音频: {} -> {}", section.getName(), introAudioUrl);
                            }
                            sectionIndex++;
                        }
                    }
                }
            }
        }

        // 转换试卷类型
        Object paperTypeObj = data.get("paper_type");
        if (paperTypeObj instanceof Map) {
            result.setPaperType(convertPaperType((Map<String, Object>) paperTypeObj));
        }

        // 处理题目组音频映射（从 question_group_audio_map 填充到 ParsedQuestionGroupDTO）
        Object audioMapObj = data.get("question_group_audio_map");
        if (audioMapObj instanceof Map && result.getVolumes() != null) {
            Map<String, Map<String, Object>> audioMap = (Map<String, Map<String, Object>>) audioMapObj;
            log.info("处理题目组音频映射，共 {} 个映射", audioMap.size());

            for (ParseResultDTO.ParsedVolumeDTO volume : result.getVolumes()) {
                if (volume.getSections() == null)
                    continue;
                for (ParseResultDTO.ParsedSectionDTO section : volume.getSections()) {
                    if (section.getQuestionGroups() == null)
                        continue;
                    for (ParseResultDTO.ParsedQuestionGroupDTO group : section.getQuestionGroups()) {
                        // 构建映射键：start-end
                        // 构建映射键：start-end
                        String key = group.getStartIndex() + "-" + group.getEndIndex();
                        Map<String, Object> audioInfo = null;
                        String matchedKey = key;

                        if (audioMap.containsKey(key)) {
                            audioInfo = audioMap.get(key);
                        } else {
                            // 尝试模糊匹配：查找以 "startIndex-" 开头的键
                            // Python端可能无法精确识别结束索引，例如 ASR 识别到 "67-67"，但题目组实际上是 "67-69"
                            String prefix = group.getStartIndex() + "-";
                            for (String mapKey : audioMap.keySet()) {
                                if (mapKey.startsWith(prefix)) {
                                    audioInfo = audioMap.get(mapKey);
                                    matchedKey = mapKey;
                                    log.info("  题目组 {} 模糊匹配到音频键: {}", key, mapKey);
                                    break;
                                }
                            }
                        }

                        if (audioInfo != null) {
                            group.setAudioUrl((String) audioInfo.get("oss_url"));
                            group.setAudioPath((String) audioInfo.get("oss_key"));
                            Object duration = audioInfo.get("duration");
                            if (duration instanceof Number) {
                                group.setAudioDuration(((Number) duration).doubleValue());
                            }
                            log.info("  题目组 {} 关联音频: {}", matchedKey, group.getAudioUrl());
                        }
                    }
                }
            }
        }

        // 处理单题音频映射（基于顺序匹配：single_question_audio_list[i] -> 听力卷第i道直接题目）
        Object singleAudioListObj = data.get("single_question_audio_list");
        if (singleAudioListObj instanceof List && result.getVolumes() != null) {
            List<Map<String, Object>> singleAudioList = (List<Map<String, Object>>) singleAudioListObj;
            log.info("处理单题音频列表，共 {} 个音频（按顺序匹配）", singleAudioList.size());

            // 收集听力卷中所有的直接题目（非题组内的题目），按顺序排列
            List<ParseResultDTO.ParsedQuestionDTO> listeningDirectQuestions = new ArrayList<>();
            for (ParseResultDTO.ParsedVolumeDTO volume : result.getVolumes()) {
                // 只处理听力卷（第III卷）
                String volumeName = volume.getName() != null ? volume.getName() : "";
                if (!volumeName.contains("III") && !volumeName.contains("听力") && !volumeName.contains("三")) {
                    continue;
                }
                if (volume.getSections() == null)
                    continue;

                for (ParseResultDTO.ParsedSectionDTO section : volume.getSections()) {
                    // 只收集 section.questions（直接题目），不包括 questionGroups 中的题目
                    if (section.getQuestions() != null) {
                        listeningDirectQuestions.addAll(section.getQuestions());
                    }
                }
            }
            log.info("听力卷直接题目数量: {}", listeningDirectQuestions.size());

            // 按下标顺序匹配：audio[i] -> question[i]
            int matchCount = Math.min(singleAudioList.size(), listeningDirectQuestions.size());
            for (int i = 0; i < matchCount; i++) {
                Map<String, Object> audioInfo = singleAudioList.get(i);
                ParseResultDTO.ParsedQuestionDTO q = listeningDirectQuestions.get(i);

                q.setAudioUrl((String) audioInfo.get("oss_url"));
                Object duration = audioInfo.get("duration");
                if (duration instanceof Number) {
                    q.setAudioDuration(((Number) duration).doubleValue());
                }
                log.info("  单题音频[{}] -> 第{}题: {}", i, q.getIndex(), q.getAudioUrl());
            }

            if (singleAudioList.size() != listeningDirectQuestions.size()) {
                log.warn("音频数量({})与题目数量({})不匹配，仅匹配了{}个",
                        singleAudioList.size(), listeningDirectQuestions.size(), matchCount);
            }
        }

        return result;
    }

    private Integer toInteger(Object obj) {
        if (obj == null)
            return 0;
        if (obj instanceof Integer)
            return (Integer) obj;
        if (obj instanceof Number)
            return ((Number) obj).intValue();
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private List<ParseResultDTO.ParsedVolumeDTO> convertVolumes(List<Map<String, Object>> volumesList) {
        List<ParseResultDTO.ParsedVolumeDTO> volumes = new ArrayList<>();
        for (Map<String, Object> v : volumesList) {
            ParseResultDTO.ParsedVolumeDTO volume = new ParseResultDTO.ParsedVolumeDTO();
            volume.setCode((String) v.get("code"));
            volume.setName((String) v.get("name"));
            volume.setTotalScore(toDouble(v.get("total_score")));
            // 读取 Python 标记的 is_listening 字段
            volume.setIsListening((Boolean) v.get("is_listening"));

            // 读取音频字段
            volume.setAudioUrl((String) v.get("volume_audio_url"));
            volume.setVolumeAudioPath((String) v.get("volume_audio_path"));
            volume.setVolumeAudioDuration(toDouble(v.get("volume_audio_duration")));

            Object sectionsObj = v.get("sections");
            if (sectionsObj instanceof List) {
                volume.setSections(convertSections((List<Map<String, Object>>) sectionsObj));
            }

            volumes.add(volume);
        }
        return volumes;
    }

    @SuppressWarnings("unchecked")
    private List<ParseResultDTO.ParsedSectionDTO> convertSections(List<Map<String, Object>> sectionsList) {
        List<ParseResultDTO.ParsedSectionDTO> sections = new ArrayList<>();
        log.info("==== [convertSections] 开始转换大题，共 {} 个 ====", sectionsList.size());

        for (int i = 0; i < sectionsList.size(); i++) {
            Map<String, Object> s = sectionsList.get(i);
            ParseResultDTO.ParsedSectionDTO section = new ParseResultDTO.ParsedSectionDTO();

            String name = (String) s.get("name");
            section.setName(name);
            log.info("[大题 {}] name = '{}'", i + 1, name);

            // 读取音频字段
            section.setIntroAudioUrl((String) s.get("intro_audio_url"));
            section.setIntroAudioPath((String) s.get("intro_audio_path"));
            section.setIntroAudioDuration(toDouble(s.get("intro_audio_duration")));

            // 尝试获取大题说明（支持多种字段名）
            String instruction = (String) s.get("instruction");
            if (instruction == null || instruction.isEmpty()) {
                instruction = (String) s.get("tintro");
            }
            if (instruction == null || instruction.isEmpty()) {
                instruction = (String) s.get("intro");
            }
            if (instruction == null || instruction.isEmpty()) {
                instruction = (String) s.get("description");
            }
            section.setInstruction(instruction);
            log.info("[大题 {}] instruction = '{}'", i + 1,
                    instruction != null
                            ? (instruction.length() > 50 ? instruction.substring(0, 50) + "..." : instruction)
                            : "null");

            section.setScorePerQuestion(toDouble(s.get("score_per_question")));

            Object questionsObj = s.get("questions");
            if (questionsObj instanceof List) {
                List<Map<String, Object>> questionsList = (List<Map<String, Object>>) questionsObj;
                section.setQuestions(convertQuestions(questionsList));
                log.info("[大题 {}] questions 数量 = {}", i + 1, questionsList.size());
            }

            // 处理题目组（支持多种字段名）
            Object questionGroupsObj = s.get("question_groups");
            if (questionGroupsObj == null) {
                questionGroupsObj = s.get("questionGroups");
            }
            if (questionGroupsObj == null) {
                questionGroupsObj = s.get("groups");
            }

            if (questionGroupsObj instanceof List) {
                List<Map<String, Object>> groupsList = (List<Map<String, Object>>) questionGroupsObj;
                log.info("[大题 {}] question_groups 数量 = {}", i + 1, groupsList.size());
                if (!groupsList.isEmpty()) {
                    // 打印第一个题目组的详情
                    Map<String, Object> firstGroup = groupsList.get(0);
                    log.info("[大题 {}] 第一个题目组: intro_text = '{}', questions 数量 = {}",
                            i + 1,
                            firstGroup.get("intro_text"),
                            firstGroup.get("questions") instanceof List ? ((List<?>) firstGroup.get("questions")).size()
                                    : 0);
                }
                section.setQuestionGroups(convertQuestionGroups(groupsList));
            } else {
                log.info("[大题 {}] question_groups 为空或类型错误", i + 1);
            }

            sections.add(section);
        }
        log.info("==== [convertSections] 转换完成 ====");
        return sections;
    }

    @SuppressWarnings("unchecked")
    private List<ParseResultDTO.ParsedQuestionDTO> convertQuestions(List<Map<String, Object>> questionsList) {
        List<ParseResultDTO.ParsedQuestionDTO> questions = new ArrayList<>();
        for (Map<String, Object> q : questionsList) {
            ParseResultDTO.ParsedQuestionDTO question = new ParseResultDTO.ParsedQuestionDTO();
            question.setIndex((Integer) q.get("index"));
            question.setTitle((String) q.get("title"));
            question.setType((Integer) q.get("type"));
            question.setAnswer((String) q.get("answer"));
            question.setAnalysis((String) q.get("analysis"));
            question.setOriginalText((String) q.get("original_text"));
            question.setHasAudio((Boolean) q.get("has_audio"));
            question.setWordLimit((String) q.get("word_limit"));

            Object optionsObj = q.get("options");
            if (optionsObj instanceof List) {
                question.setOptions(convertOptions((List<Map<String, Object>>) optionsObj));
            }

            questions.add(question);
        }
        return questions;
    }

    private List<ParseResultDTO.ParsedOptionDTO> convertOptions(List<Map<String, Object>> optionsList) {
        List<ParseResultDTO.ParsedOptionDTO> options = new ArrayList<>();
        for (Map<String, Object> o : optionsList) {
            ParseResultDTO.ParsedOptionDTO option = new ParseResultDTO.ParsedOptionDTO();
            option.setLabel((String) o.get("label"));
            option.setContent((String) o.get("content"));
            option.setIsAnswer((Boolean) o.get("is_answer"));
            options.add(option);
        }
        return options;
    }

    @SuppressWarnings("unchecked")
    private List<ParseResultDTO.ParsedQuestionGroupDTO> convertQuestionGroups(List<Map<String, Object>> groupsList) {
        List<ParseResultDTO.ParsedQuestionGroupDTO> groups = new ArrayList<>();
        for (Map<String, Object> g : groupsList) {
            ParseResultDTO.ParsedQuestionGroupDTO group = new ParseResultDTO.ParsedQuestionGroupDTO();
            group.setIntroText((String) g.get("intro_text"));
            group.setHasAudio((Boolean) g.get("has_audio"));
            group.setStartIndex(toInteger(g.get("start_index")));
            group.setStartIndex(toInteger(g.get("start_index")));
            group.setEndIndex(toInteger(g.get("end_index")));

            // 支持 snake_case (Python) 和 camelCase (Frontend)
            String groupName = (String) g.get("group_name");
            if (groupName == null)
                groupName = (String) g.get("groupName");
            group.setGroupName(groupName);

            Object answerTimeObj = g.get("answer_time");
            if (answerTimeObj == null)
                answerTimeObj = g.get("answerTime");
            group.setAnswerTime(toInteger(answerTimeObj));

            // 转换组内的题目
            Object questionsObj = g.get("questions");
            if (questionsObj instanceof List) {
                group.setQuestions(convertQuestions((List<Map<String, Object>>) questionsObj));
            }

            groups.add(group);
            log.debug("转换题目组: introText前50字符={}, 题目数={}",
                    group.getIntroText() != null
                            ? group.getIntroText().substring(0, Math.min(50, group.getIntroText().length()))
                            : "null",
                    group.getQuestions() != null ? group.getQuestions().size() : 0);
        }
        return groups;
    }

    private List<ParseResultDTO.AudioChunkDTO> convertAudioChunks(List<Map<String, Object>> chunksList) {
        List<ParseResultDTO.AudioChunkDTO> chunks = new ArrayList<>();
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());

        for (Map<String, Object> c : chunksList) {
            ParseResultDTO.AudioChunkDTO chunk = new ParseResultDTO.AudioChunkDTO();
            chunk.setIndex((Integer) c.get("index"));
            chunk.setStartTime(toDouble(c.get("start_time")));
            chunk.setEndTime(toDouble(c.get("end_time")));
            chunk.setDuration(toDouble(c.get("duration")));
            chunk.setFilePath((String) c.get("file_path"));
            chunk.setFileName((String) c.get("file_name"));
            chunk.setBase64Data((String) c.get("base64_data"));

            // 如果有 base64 数据，上传到 OSS
            String base64Data = chunk.getBase64Data();
            if (StringUtils.hasText(base64Data)) {
                try {
                    byte[] audioBytes = Base64.getDecoder().decode(base64Data);
                    String fileName = chunk.getFileName();
                    if (!StringUtils.hasText(fileName)) {
                        fileName = "chunk_" + chunk.getIndex() + ".mp3";
                    }
                    String objectKey = "exam/import_audio/" + datePath + "/" + System.currentTimeMillis() + "_"
                            + fileName;
                    String ossUrl = ossServiceFactory.getOssService().upload(audioBytes, objectKey, "audio/mpeg");
                    chunk.setOssUrl(ossUrl);
                    log.info("音频片段上传成功: {} -> {}", chunk.getIndex(), ossUrl);
                    // 清空 base64 数据减少内存占用
                    chunk.setBase64Data(null);
                } catch (Exception e) {
                    log.error("音频片段上传失败: {}", chunk.getIndex(), e);
                }
            }

            chunks.add(chunk);
        }
        return chunks;
    }

    private ParseResultDTO.PaperTypeDTO convertPaperType(Map<String, Object> typeMap) {
        ParseResultDTO.PaperTypeDTO paperType = new ParseResultDTO.PaperTypeDTO();
        paperType.setType((String) typeMap.get("type"));
        paperType.setTypeName((String) typeMap.get("type_name"));
        paperType.setConfidence((String) typeMap.get("confidence"));
        return paperType;
    }

    private Double toDouble(Object value) {
        if (value == null)
            return null;
        if (value instanceof Double)
            return (Double) value;
        if (value instanceof Integer)
            return ((Integer) value).doubleValue();
        if (value instanceof Number)
            return ((Number) value).doubleValue();
        return null;
    }
    // ============ 多卷别顺序导入（会话管理）实现 ============

    @Override
    public String startImportSession() {
        String sessionKey = UUID.randomUUID().toString().replace("-", "");
        Long userId = SecurityUtils.getUserId();

        PaperImportSession session = new PaperImportSession();
        session.setSessionKey(sessionKey);
        session.setUserId(userId);
        session.setVolumeCount(0);
        session.setVolumeData("[]");
        session.setStatus(PaperImportSession.STATUS_IN_PROGRESS);

        // 设置过期时间
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, SESSION_EXPIRE_HOURS);
        session.setExpireTime(cal.getTime());

        importSessionMapper.insert(session);
        log.info("创建导入会话: sessionKey={}, userId={}", sessionKey, userId);

        return sessionKey;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer addVolumeToSession(ImportSessionAddVolumeBO bo) {
        PaperImportSession session = importSessionMapper.selectBySessionKey(bo.getSessionKey());
        if (session == null) {
            throw new ServiceException("导入会话不存在或已过期");
        }
        if (session.getStatus() != PaperImportSession.STATUS_IN_PROGRESS) {
            throw new ServiceException("导入会话已完成或已过期");
        }

        try {
            // 解析现有卷别数据
            List<Map<String, Object>> volumeList = objectMapper.readValue(
                    session.getVolumeData(),
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            // 添加新卷别
            Map<String, Object> newVolume = new HashMap<>();
            newVolume.put("volumeName", bo.getVolumeName());
            newVolume.put("volumeOrder", bo.getVolumeOrder());
            newVolume.put("parseResult", bo.getParseResult());

            // 统计题目数量
            int questionCount = 0;
            int sectionCount = 0;
            Map<String, Object> parseResult = bo.getParseResult();
            if (parseResult != null && parseResult.get("volumes") instanceof List) {
                List<?> volumes = (List<?>) parseResult.get("volumes");
                for (Object vol : volumes) {
                    if (vol instanceof Map) {
                        Map<?, ?> volMap = (Map<?, ?>) vol;
                        if (volMap.get("sections") instanceof List) {
                            List<?> sections = (List<?>) volMap.get("sections");
                            sectionCount += sections.size();
                            for (Object sec : sections) {
                                if (sec instanceof Map) {
                                    Map<?, ?> secMap = (Map<?, ?>) sec;
                                    if (secMap.get("questions") instanceof List) {
                                        questionCount += ((List<?>) secMap.get("questions")).size();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            newVolume.put("questionCount", questionCount);
            newVolume.put("sectionCount", sectionCount);

            volumeList.add(newVolume);

            // 更新会话
            session.setVolumeData(objectMapper.writeValueAsString(volumeList));
            session.setVolumeCount(volumeList.size());
            importSessionMapper.updateById(session);

            log.info("添加卷别到会话: sessionKey={}, volumeName={}, volumeOrder={}, questionCount={}",
                    bo.getSessionKey(), bo.getVolumeName(), bo.getVolumeOrder(), questionCount);

            return volumeList.size();

        } catch (Exception e) {
            log.error("添加卷别失败", e);
            throw new ServiceException("添加卷别失败: " + e.getMessage());
        }
    }

    @Override
    public ImportSessionDTO getImportSession(String sessionKey) {
        PaperImportSession session = importSessionMapper.selectBySessionKey(sessionKey);
        if (session == null) {
            throw new ServiceException("导入会话不存在");
        }

        ImportSessionDTO dto = new ImportSessionDTO();
        dto.setSessionKey(session.getSessionKey());
        dto.setVolumeCount(session.getVolumeCount());
        dto.setStatus(session.getStatus());

        try {
            List<Map<String, Object>> volumeList = objectMapper.readValue(
                    session.getVolumeData(),
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            List<ImportSessionDTO.VolumeInfo> volumes = new ArrayList<>();
            for (Map<String, Object> vol : volumeList) {
                ImportSessionDTO.VolumeInfo info = new ImportSessionDTO.VolumeInfo();
                info.setVolumeName((String) vol.get("volumeName"));
                info.setVolumeOrder((Integer) vol.get("volumeOrder"));
                info.setQuestionCount((Integer) vol.get("questionCount"));
                info.setSectionCount((Integer) vol.get("sectionCount"));
                volumes.add(info);
            }
            dto.setVolumes(volumes);

        } catch (Exception e) {
            log.error("解析会话数据失败", e);
            dto.setVolumes(new ArrayList<>());
        }

        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportFinalizeResultDTO finalizeImportSession(ImportSessionFinalizeBO bo) {
        PaperImportSession session = importSessionMapper.selectBySessionKey(bo.getSessionKey());
        if (session == null) {
            throw new ServiceException("导入会话不存在或已过期");
        }
        if (session.getStatus() == PaperImportSession.STATUS_COMPLETED) {
            throw new ServiceException("导入会话已完成，请勿重复提交");
        }
        if (session.getStatus() == PaperImportSession.STATUS_EXPIRED) {
            throw new ServiceException("导入会话已过期");
        }

        ImportFinalizeResultDTO result = new ImportFinalizeResultDTO();
        int totalQuestionCount = 0;

        try {
            // 1. 解析所有卷别数据
            List<Map<String, Object>> volumeList = objectMapper.readValue(
                    session.getVolumeData(),
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            if (volumeList.isEmpty()) {
                throw new ServiceException("没有添加任何卷别");
            }

            // 2. 构建试卷完整数据
            PaperFullDataBO fullData = new PaperFullDataBO();

            // 2.1 试卷基本信息
            PaperBasicInfoBO paperInfo = new PaperBasicInfoBO();
            paperInfo.setCustomName(bo.getPaperName());
            paperInfo.setPaperType("high_school_english_listening");
            paperInfo.setBusinessType(5); // 题库
            paperInfo.setStatus(1);
            // 补充必填字段
            paperInfo.setPaperDesc("智能导入试卷");
            paperInfo.setDuration(30); // 默认30分钟
            paperInfo.setNotes("请认真作答");
            // 设置默认有效期（从现在到一年后）
            paperInfo.setEnableStartTime(new java.util.Date());
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.add(java.util.Calendar.YEAR, 1);
            paperInfo.setEnableEndTime(cal.getTime());
            // 设置数据来源标识：智能导入
            paperInfo.setRemark("asr_import");
            fullData.setPaper(paperInfo);

            // 2.2 卷别列表
            List<VolumeDataBO> volumeBOs = new ArrayList<>();
            List<IntermissionDataBO> intermissionBOs = new ArrayList<>();

            for (int i = 0; i < volumeList.size(); i++) {
                Map<String, Object> volData = volumeList.get(i);
                String volumeName = (String) volData.get("volumeName");
                Integer volumeOrder = (Integer) volData.get("volumeOrder");

                @SuppressWarnings("unchecked")
                Map<String, Object> parseResult = (Map<String, Object>) volData.get("parseResult");

                // 将解析结果转为 ParseResultDTO
                ParseResultDTO parsedResult = convertToParseResult(parseResult);

                // 创建题目到题库
                ImportQuestionsResultDTO questionsResult = createQuestions(
                        parsedResult,
                        bo.getCategoryId().intValue(),
                        bo.getSubjectId(),
                        bo.getDefaultQuestionType(),
                        bo.getListeningOnly());
                totalQuestionCount += questionsResult.getCreatedCount();

                // 构建卷别BO
                VolumeDataBO volumeBO = new VolumeDataBO();
                volumeBO.setTempId("temp_vol_" + volumeOrder);
                volumeBO.setVolumeName(volumeName);
                volumeBO.setVolumeOrder(volumeOrder);

                // 调试日志：打印解析结果中的卷别音频信息
                log.info("=== 开始关联卷别[{}]音频 ===", volumeName);
                if (parsedResult.getVolumes() != null && !parsedResult.getVolumes().isEmpty()) {
                    log.info("parsedResult.volumes 数量: {}", parsedResult.getVolumes().size());
                    for (int vi = 0; vi < parsedResult.getVolumes().size(); vi++) {
                        ParseResultDTO.ParsedVolumeDTO v = parsedResult.getVolumes().get(vi);
                        log.info("  volumes[{}]: name='{}', audioUrl='{}'", vi, v.getName(), v.getAudioUrl());
                    }

                    // 尝试通过名称匹配找到对应的 VolumeDTO
                    ParseResultDTO.ParsedVolumeDTO matchedVol = null;
                    if (volumeName != null) {
                        for (ParseResultDTO.ParsedVolumeDTO v : parsedResult.getVolumes()) {
                            if (volumeName.equals(v.getName())) {
                                matchedVol = v;
                                log.info("通过名称匹配到卷别: '{}'", volumeName);
                                break;
                            }
                        }
                    }
                    // 如果名称未匹配，尝试通过索引匹配
                    if (matchedVol == null && i < parsedResult.getVolumes().size()) {
                        matchedVol = parsedResult.getVolumes().get(i);
                        log.info("通过索引[{}]匹配到卷别: '{}'", i, matchedVol.getName());
                    }

                    // 如果找到了匹配的卷别，且有音频信息，则设置
                    if (matchedVol != null) {
                        String audioUrl = matchedVol.getAudioUrl();
                        log.info("匹配到的卷别 audioUrl='{}', path='{}', duration={}",
                                audioUrl, matchedVol.getVolumeAudioPath(), matchedVol.getVolumeAudioDuration());
                        volumeBO.setVolumeAudioUrl(audioUrl);
                        volumeBO.setVolumeAudioPath(matchedVol.getVolumeAudioPath());
                        if (matchedVol.getVolumeAudioDuration() != null) {
                            volumeBO.setVolumeAudioDuration(matchedVol.getVolumeAudioDuration().intValue());
                        }
                        log.info("已设置 volumeBO.volumeAudioUrl='{}'", volumeBO.getVolumeAudioUrl());
                    } else {
                        log.warn("未能匹配到卷别[{}]的 DTO", volumeName);
                    }
                } else {
                    log.warn("parsedResult.volumes 为空或 null");
                }

                // 构建大题列表（从更新后的解析结果获取）
                ParseResultDTO updatedResult = questionsResult.getPaperStructure();
                List<SectionDataBO> sectionBOs = buildSectionsFromParseResult(updatedResult, volumeBO.getTempId(),
                        bo.getListeningOnly());
                volumeBO.setSections(sectionBOs);

                log.info("构建卷别 {} 完成: 大题数={}, 每大题题目数={}, 音频URL={}",
                        volumeName,
                        sectionBOs.size(),
                        sectionBOs.stream().mapToInt(s -> s.getQuestions() != null ? s.getQuestions().size() : 0)
                                .sum(),
                        volumeBO.getVolumeAudioUrl());

                // 打印题目组信息
                for (SectionDataBO sec : sectionBOs) {
                    if (sec.getQuestionGroups() != null && !sec.getQuestionGroups().isEmpty()) {
                        log.info("  大题 {} 包含 {} 个题目组", sec.getSectionName(), sec.getQuestionGroups().size());
                    }
                }

                volumeBOs.add(volumeBO);

                // 如果不是第一个卷别，创建中场配置
                if (i > 0) {
                    IntermissionDataBO intermission = new IntermissionDataBO();
                    intermission.setFromVolumeTempId("temp_vol_" + (i));
                    intermission.setToVolumeTempId("temp_vol_" + (i + 1));
                    // 设置fromVolume/toVolume（数据库必填字段）
                    intermission.setFromVolume(String.valueOf((char) ('A' + i - 1)));
                    intermission.setToVolume(String.valueOf((char) ('A' + i)));
                    intermission.setIntermissionText(getDefaultIntermissionText());
                    intermission.setIntermissionAudioDuration(getDefaultIntermissionDuration());
                    intermission.setCanSkip(getDefaultIntermissionCanSkip());
                    intermissionBOs.add(intermission);
                }
            }

            fullData.setVolumes(volumeBOs);
            fullData.setIntermissions(intermissionBOs);

            // 3. 创建试卷
            Integer paperId = paperService.createPaperWithFullData(fullData);

            // 4. 标记会话完成
            session.setStatus(PaperImportSession.STATUS_COMPLETED);
            importSessionMapper.updateById(session);

            result.setPaperId(paperId);
            result.setQuestionCount(totalQuestionCount);
            result.setVolumeCount(volumeList.size());

            log.info("导入会话完成: sessionKey={}, paperId={}, questionCount={}, volumeCount={}",
                    bo.getSessionKey(), paperId, totalQuestionCount, volumeList.size());

        } catch (Exception e) {
            log.error("完成导入会话失败", e);
            throw new ServiceException("完成导入失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 从解析结果构建大题列表
     */
    private List<SectionDataBO> buildSectionsFromParseResult(ParseResultDTO parseResult, String volumeTempId,
            Boolean listeningOnly) {
        List<SectionDataBO> sections = new ArrayList<>();

        if (parseResult == null || parseResult.getVolumes() == null) {
            return sections;
        }

        int sectionOrder = 1;
        for (ParseResultDTO.ParsedVolumeDTO vol : parseResult.getVolumes()) {
            // 如果选择仅导入听力部分，过滤非听力卷别
            if (Boolean.TRUE.equals(listeningOnly)) {
                // 使用 Python 基于内容（音频）检测的 isListening 字段
                Boolean isListening = vol.getIsListening();
                // Fallback: 如果 Python 没有标记，则检查卷名是否包含"听力"
                if (isListening == null) {
                    String volumeName = vol.getName() != null ? vol.getName() : "";
                    isListening = volumeName.contains("听力");
                }
                if (!Boolean.TRUE.equals(isListening)) {
                    log.debug("buildSectionsFromParseResult: 跳过非听力卷别: {}", vol.getName());
                    continue;
                }
            }

            if (vol.getSections() == null)
                continue;

            for (ParseResultDTO.ParsedSectionDTO sec : vol.getSections()) {
                SectionDataBO sectionBO = new SectionDataBO();
                sectionBO.setTempId("temp_sec_" + sectionOrder);
                sectionBO.setVolumeTempId(volumeTempId);
                sectionBO.setSectionName(sec.getName());
                sectionBO.setSectionOrder(sectionOrder);
                sectionBO.setScorePerQuestion(
                        sec.getScorePerQuestion() != null ? new java.math.BigDecimal(sec.getScorePerQuestion())
                                : new java.math.BigDecimal("1.5"));

                // 映射音频字段
                sectionBO.setInstructionAudioUrl(sec.getIntroAudioUrl());
                sectionBO.setInstructionAudioPath(sec.getIntroAudioPath());
                if (sec.getIntroAudioDuration() != null) {
                    sectionBO.setInstructionAudioDuration(sec.getIntroAudioDuration().intValue());
                }

                // 构建题目关联（section.questions 中的直接题目）
                List<QuestionDataBO> questionBOs = new ArrayList<>();
                if (sec.getQuestions() != null) {
                    int qOrder = 1;
                    for (ParseResultDTO.ParsedQuestionDTO q : sec.getQuestions()) {
                        if (q.getQuestionId() != null) {
                            QuestionDataBO qBO = new QuestionDataBO();
                            qBO.setSectionTempId(sectionBO.getTempId());
                            qBO.setSectionOrder(qOrder++);
                            qBO.setQuestionId(q.getQuestionId());
                            qBO.setScore(sectionBO.getScorePerQuestion());
                            questionBOs.add(qBO);
                        }
                    }
                }

                // 构建题目组（section.questionGroups 中的分组题目）
                List<PaperQuestionGroupBO> groupBOs = new ArrayList<>();
                if (sec.getQuestionGroups() != null) {
                    int groupOrder = 1;
                    for (ParseResultDTO.ParsedQuestionGroupDTO group : sec.getQuestionGroups()) {
                        PaperQuestionGroupBO groupBO = new PaperQuestionGroupBO();
                        groupBO.setTempId("temp_grp_" + sectionOrder + "_" + groupOrder);
                        groupBO.setSectionTempId(sectionBO.getTempId());
                        groupBO.setGroupOrder(groupOrder++);
                        groupBO.setStartQuestionNum(group.getStartIndex());
                        groupBO.setEndQuestionNum(group.getEndIndex());
                        groupBO.setIntroText(group.getIntroText());
                        groupBO.setAudioUrl(group.getAudioUrl());
                        groupBO.setAudioPath(group.getAudioPath());
                        if (group.getAudioDuration() != null) {
                            groupBO.setAudioDuration(group.getAudioDuration().intValue());
                        }

                        // 这2个字段之前缺失导致没保存
                        groupBO.setGroupName(group.getGroupName());
                        groupBO.setAnswerTime(group.getAnswerTime());

                        // 收集组内题目的 questionId
                        List<Integer> selectedIds = new ArrayList<>();
                        if (group.getQuestions() != null) {
                            int qOrder = questionBOs.size() + 1;
                            for (ParseResultDTO.ParsedQuestionDTO q : group.getQuestions()) {
                                if (q.getQuestionId() != null) {
                                    selectedIds.add(q.getQuestionId());
                                    // 同时添加到 section 的题目列表
                                    QuestionDataBO qBO = new QuestionDataBO();
                                    qBO.setSectionTempId(sectionBO.getTempId());
                                    qBO.setSectionOrder(qOrder++);
                                    qBO.setQuestionId(q.getQuestionId());
                                    qBO.setScore(sectionBO.getScorePerQuestion());
                                    questionBOs.add(qBO);
                                }
                            }
                        }
                        groupBO.setSelectedQuestionIds(selectedIds);
                        groupBOs.add(groupBO);

                        log.debug("构建题目组: {}-{}, 题目数={}, audioUrl={}",
                                group.getStartIndex(), group.getEndIndex(),
                                selectedIds.size(), group.getAudioUrl());
                    }
                }
                sectionBO.setQuestionGroups(groupBOs);

                sectionBO.setQuestions(questionBOs);
                sectionBO.setQuestionCount(questionBOs.size());

                sections.add(sectionBO);
                sectionOrder++;
            }
        }

        return sections;
    }

    /**
     * 获取中场默认提示文字
     */
    private String getDefaultIntermissionText() {
        try {
            return dictDataService.selectDictLabel("paper_intermission_default", "中场默认提示文字");
        } catch (Exception e) {
            return "请翻页，准备作答下一卷";
        }
    }

    /**
     * 获取中场默认时长
     */
    private Integer getDefaultIntermissionDuration() {
        try {
            String value = dictDataService.selectDictLabel("paper_intermission_default", "中场默认时长(秒)");
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 30;
        }
    }

    /**
     * 获取中场是否可跳过
     */
    private Integer getDefaultIntermissionCanSkip() {
        try {
            String value = dictDataService.selectDictLabel("paper_intermission_default", "是否可跳过");
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * MultipartFile 资源包装类
     */
    private static class MultipartFileResource extends ByteArrayResource {
        private final String filename;

        public MultipartFileResource(MultipartFile file) throws Exception {
            super(file.getBytes());
            this.filename = file.getOriginalFilename();
        }

        @Override
        public String getFilename() {
            return this.filename;
        }
    }
}
