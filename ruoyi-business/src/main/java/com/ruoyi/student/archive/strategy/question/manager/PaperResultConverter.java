package com.ruoyi.student.archive.strategy.question.manager;


import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.AppErrorCode;
import com.ruoyi.common.utils.bean.BeanUtils;
import com.ruoyi.student.archive.domain.bo.paper.QueryBlankResultBO;
import com.ruoyi.student.archive.domain.bo.paper.QueryPaperResultBO;
import com.ruoyi.student.archive.domain.bo.paper.QueryQuestionResultBO;
import com.ruoyi.student.archive.domain.dto.paper.BlankResultDTO;
import com.ruoyi.student.archive.domain.dto.paper.PaperQuestionDTO;
import com.ruoyi.student.archive.domain.dto.paper.PaperQuestionResultDTO;
import com.ruoyi.student.archive.domain.dto.paper.QuestionResultDTO;
import com.ruoyi.student.archive.domain.dto.question.*;
import com.ruoyi.common.enums.question.OptionTypeEnum;
import com.ruoyi.common.enums.question.QuestionTypeEnum;
import com.ruoyi.student.archive.biz.paper.IAppUserPaperQuestionBlankResultBiz;
import com.ruoyi.student.archive.biz.paper.IAppUserPaperQuestionResultBiz;
import com.ruoyi.student.archive.biz.question.QuestionAnswerBiz;
import com.ruoyi.student.archive.biz.question.QuestionBiz;
import com.ruoyi.student.archive.biz.question.QuestionBlankAreaBiz;
import com.ruoyi.student.archive.biz.question.IQuestionMediaBiz;
import com.ruoyi.student.archive.domain.paper.AppUserPaperInfo;
import com.ruoyi.student.archive.domain.question.Question;
import com.ruoyi.student.archive.domain.question.QuestionAnswer;
import com.ruoyi.student.archive.domain.question.QuestionBlankArea;
import com.ruoyi.student.archive.domain.question.QuestionMedia;
import com.ruoyi.student.archive.mapper.paper.AppUserPaperInfoMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.enums.YesOrNoEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ruoyi.common.constant.AppErrorCode.*;

@Component
@Slf4j
public class PaperResultConverter {
    @Resource
    private AppUserPaperInfoMapper appUserPaperInfoMapper;

    @Resource
    protected IAppUserPaperQuestionResultBiz questionResultBiz;

    @Resource
    protected QuestionAnswerBiz questionAnswerBiz;

    @Resource
    private IQuestionMediaBiz questionMediaBiz;

    @Resource
    protected QuestionBiz questionBiz;

    @Resource
    private IAppUserPaperQuestionBlankResultBiz blankResultBiz;

    @Resource
    protected QuestionBlankAreaBiz questionBlankAreaBiz;
    
    public PaperQuestionResultDTO getPaperResult(QueryPaperResultBO queryBO) throws ServiceException {
       return getPaperQuestionResult(queryBO);
    }
    
    public List<PaperQuestionDTO> toPaperQuestionDTOs(List<Question> questions, Map<Integer, Integer> questionOrderMap) throws ServiceException {
        return buildPaperQuestionDTOs(questions, questionOrderMap);
    }

    public List<QuestionCorrectAnswerDTO> convertToCorrectAnswerDTOs(List<Question> questions) {
        if (CollectionUtils.isEmpty(questions)) {
            return Collections.emptyList();
        }

        List<QuestionCorrectAnswerDTO> result = new ArrayList<>();
        for (Question question : questions) {
            List<QuestionAnswer> answers = questionAnswerBiz.getAnswersByQuestionId(question.getId());
            result.add(buildCorrectAnswerDTO(question, answers));
        }
        return result;
    }

    private PaperQuestionResultDTO getPaperQuestionResult(QueryPaperResultBO queryBO) throws ServiceException {
        log.info("获取试卷结果, 入参:{}", JSON.toJSONString(queryBO));
        validateParam(queryBO);
        try {
            PaperQuestionResultDTO resultDTO = new PaperQuestionResultDTO();

            AppUserPaperInfo paperInfo = appUserPaperInfoMapper.selectOne(
                    new LambdaQueryWrapper<AppUserPaperInfo>()
                            .eq(AppUserPaperInfo::getId, queryBO.getPaperId())
                            .eq(AppUserPaperInfo::getAppUserId, queryBO.getAppUserId())
                            .eq(AppUserPaperInfo::getDelFlag, YesOrNoEnum.NO.getCode())
            );

            List<Integer> savedOrder;
            if (paperInfo != null && StringUtils.isNotEmpty(paperInfo.getQuestionOrder())) {
                savedOrder = Arrays.stream(paperInfo.getQuestionOrder().split(","))
                        .map(Integer::parseInt)
                        .collect(Collectors.toList());
            } else {
                savedOrder = null;
            }
            List<QuestionResultDTO> questionResults = queryPaperQuestionResults(QueryQuestionResultBO.builder()
                    .paperId(queryBO.getPaperId())
                    .appUserId(queryBO.getAppUserId())
                    .build());
            List<BlankResultDTO> blankResults = getBlankResults(QueryBlankResultBO.builder()
                    .paperId(queryBO.getPaperId())
                    .appUserId(queryBO.getAppUserId())
                    .build());

            Set<Integer> questionIds = new LinkedHashSet<>();
            questionResults.forEach(result -> questionIds.add(result.getQuestionId()));
            blankResults.forEach(result -> questionIds.add(result.getQuestionId()));

            if (questionIds.isEmpty()) {
                resultDTO.setQuestionResults(Collections.emptyList());
                resultDTO.setCorrectCount(0);
                resultDTO.setWrongCount(0);
                return resultDTO;
            }

            Map<Integer, QuestionResultDTO> questionMap = getQuestionMap(questionIds);
            List<QuestionResultDTO> normalResults = convertQuestionResults(questionResults, questionMap);
            Map<Integer, Map<Integer, List<QuestionAnswer>>> blankAreaAnswerMap = questionBiz.getBlankAreaAnswerMap(questionIds);
            Map<Integer, List<BlankResultDTO>> blankResultMap = convertBlankResults(blankResults, questionMap, blankAreaAnswerMap);

            // 合并普通题和完形填空题结果
            List<QuestionResultDTO> finalResults = new ArrayList<>(normalResults);

            // 设置完形填空题的空白区域结果
            for (QuestionResultDTO result : normalResults) {
                if (QuestionTypeEnum.CLOZE.getValue().equals(result.getType())) {
                    List<BlankResultDTO> blankList = blankResultMap.get(result.getQuestionId());
                    if (blankList != null) {
                        result.setBlankResults(blankList);
                    }
                }
            }

            // 处理不在normalResults中的完形填空题
            Set<Integer> processedQuestionIds = normalResults.stream()
                    .map(QuestionResultDTO::getQuestionId)
                    .collect(Collectors.toSet());

            for (Map.Entry<Integer, List<BlankResultDTO>> entry : blankResultMap.entrySet()) {
                Integer questionId = entry.getKey();
                if (processedQuestionIds.contains(questionId)) {
                    continue;
                }
                QuestionResultDTO questionInfo = questionMap.get(questionId);
                if (questionInfo != null && QuestionTypeEnum.CLOZE.getValue().equals(questionInfo.getType())) {
                    QuestionResultDTO clozeResult = new QuestionResultDTO();
                    BeanUtil.copyProperties(questionInfo, clozeResult);
                    List<BlankResultDTO> blankList = entry.getValue();
                    clozeResult.setBlankResults(blankList);
                    finalResults.add(clozeResult);
                }
            }

            // 统一处理所有题目资源
            processQuestionResources(finalResults);
            if (savedOrder != null && !savedOrder.isEmpty()) {
                Map<Integer, QuestionResultDTO> resultMap = finalResults.stream()
                        .collect(Collectors.toMap(QuestionResultDTO::getQuestionId, Function.identity(), (v1, v2) -> v1));

                List<QuestionResultDTO> orderedResults = new ArrayList<>();
                for (Integer qid : savedOrder) {
                    if (resultMap.containsKey(qid)) {
                        orderedResults.add(resultMap.get(qid));
                    }
                }

                // 添加可能不在顺序中的题目
                finalResults.forEach(r -> {
                    if (!savedOrder.contains(r.getQuestionId())) {
                        orderedResults.add(r);
                    }
                });

                finalResults = orderedResults;
            }
            resultDTO.setQuestionResults(finalResults);
            int correctCount = 0;
            int wrongCount = 0;
            for (QuestionResultDTO result : finalResults) {
                if (QuestionTypeEnum.CLOZE.getValue().equals(result.getType())) {
                    List<BlankResultDTO> questionBlankResults = result.getBlankResults();
                    if (!CollectionUtils.isEmpty(questionBlankResults)) {
                        boolean allCorrect = questionBlankResults.stream()
                                .allMatch(blank -> blank.getResult().equals(YesOrNoEnum.YES.getCode()));
                        if (allCorrect) {
                            correctCount++;
                        } else {
                            wrongCount++;
                        }
                    } else {
                        wrongCount++;
                    }
                } else {
                    if (YesOrNoEnum.YES.getCode().equals(result.getResult())) {
                        correctCount++;
                    } else {
                        wrongCount++;
                    }
                }
            }

            resultDTO.setCorrectCount(correctCount);
            resultDTO.setWrongCount(wrongCount);
            return resultDTO;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取试卷结果失败", e);
            throw new ServiceException(APP_GET_PAPER_RESULT_FAIL_MSG);
        }
    }

    /**
     * 参数校验
     */
    protected void validateParam(Object param) throws ServiceException {
        if (param == null) {
            throw new ServiceException(APP_PARAM_NOT_NULL_MSG);
        }
    }
    /**
     * 转换普通题目结果
     *
     * @param questionResults 题目作答结果列表
     * @param questionMap 题目信息Map
     * @return 转换后的题目结果列表
     */
    private List<QuestionResultDTO> convertQuestionResults(List<QuestionResultDTO> questionResults,
                                                           Map<Integer, QuestionResultDTO> questionMap) {
        if (CollectionUtils.isEmpty(questionResults)) {
            return Collections.emptyList();
        }

        // 复制并补充题目信息
        List<QuestionResultDTO> result = new ArrayList<>(questionResults.size());
        for (QuestionResultDTO userResult : questionResults) {
            QuestionResultDTO questionInfo = questionMap.get(userResult.getQuestionId());
            if (questionInfo != null) {
                QuestionResultDTO dto = new QuestionResultDTO();
                BeanUtil.copyProperties(userResult, dto);

                // 复制题目属性
                dto.setTitle(questionInfo.getTitle());
                dto.setType(questionInfo.getType());
                dto.setMediaType(questionInfo.getMediaType());
                dto.setOptionType(questionInfo.getOptionType());
                dto.setAnalyzes(questionInfo.getAnalyzes());
                dto.setCorrectAnswerIds(questionInfo.getCorrectAnswerIds());

                // 保留基本选项信息，完整数据由processQuestionResources处理
                if (!CollectionUtils.isEmpty(questionInfo.getOptions())) {
                    dto.setOptions(questionInfo.getOptions());
                }
                List<QuestionAnswer> answers = questionAnswerBiz.getAnswersByQuestionId(userResult.getQuestionId());
                if (!CollectionUtils.isEmpty(answers)) {
                    List<QuestionAnswerDTO> answerDTOs = answers.stream().map(answer -> {
                        QuestionAnswerDTO answerDTO = new QuestionAnswerDTO();
                        BeanUtil.copyProperties(answer, answerDTO);
                        List<QuestionMediaDTO> mediaList = getAnswerMediaResources(answer.getId());
                        answerDTO.setMediaUrl(mediaList);
                        return answerDTO;
                    }).collect(Collectors.toList());
                    dto.setOptions(answerDTOs);

                    List<Integer> correctAnswerIds = answers.stream()
                            .filter(answer -> YesOrNoEnum.YES.getCode().equals(answer.getIsAnswer()))
                            .map(QuestionAnswer::getId)
                            .toList();
                    dto.setCorrectAnswerIds(correctAnswerIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(",")));
                }
                result.add(dto);
            }
        }
        return result;
    }

    /**
     * 转换填空题结果
     *
     * @param blankResults 填空题作答结果列表
     * @param questionMap 题目信息Map
     * @param blankAreaAnswerMap 空位区域答案Map，按questionId和blankAreaId两级分组
     * @return 按题目ID分组的填空题作答结果
     */
    protected Map<Integer, List<BlankResultDTO>> convertBlankResults(List<BlankResultDTO> blankResults,
                                                                     Map<Integer, QuestionResultDTO> questionMap,
                                                                     Map<Integer, Map<Integer, List<QuestionAnswer>>> blankAreaAnswerMap) throws ServiceException {
        if (CollectionUtils.isEmpty(blankResults)) {
            return Collections.emptyMap();
        }

        // 按题目ID分组
        Map<Integer, List<BlankResultDTO>> resultMap = blankResults.stream()
                .collect(Collectors.groupingBy(BlankResultDTO::getQuestionId));

        // 处理每个题目的空位结果
        for (Map.Entry<Integer, List<BlankResultDTO>> entry : resultMap.entrySet()) {
            Integer questionId = entry.getKey();
            List<BlankResultDTO> questionBlankResults = entry.getValue();

            // 获取题目信息
            QuestionResultDTO questionInfo = questionMap.get(questionId);
            if (questionInfo == null) {
                continue;
            }

            // 获取该题目的所有空白区域答案
            Map<Integer, List<QuestionAnswer>> questionBlankAreaMap = blankAreaAnswerMap.getOrDefault(questionId, Collections.emptyMap());

            // 处理每个空位结果
            for (BlankResultDTO blankResult : questionBlankResults) {
                // 获取空位ID
                Integer blankAreaId = blankResult.getBlankAreaId();

                // 获取空位的答案列表，确保是从正确的题目中获取
                List<QuestionAnswer> answers = questionBlankAreaMap.getOrDefault(blankAreaId, Collections.emptyList());
                if (!CollectionUtils.isEmpty(answers)) {
                    // 处理选项信息
                    List<QuestionAnswerDTO> answerDTOs = answers.stream().map(answer -> {
                        QuestionAnswerDTO answerDTO = new QuestionAnswerDTO();
                        BeanUtil.copyProperties(answer, answerDTO);
                        answerDTO.setBlankAreaId(blankAreaId);
                        return answerDTO;
                    }).collect(Collectors.toList());

                    // 设置空白区域的正确答案ID
                    List<Integer> correctAnswerIds = answers.stream()
                            .filter(answer -> YesOrNoEnum.YES.getCode().equals(answer.getIsAnswer()))
                            .map(QuestionAnswer::getId)
                            .toList();

                    blankResult.setCorrectAnswerIds(correctAnswerIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(",")));

                    blankResult.setOptions(answerDTOs);

                }
            }
        }

        return resultMap;
    }

    public List<BlankResultDTO> getBlankResults(QueryBlankResultBO queryBO) throws ServiceException {
        try {
            return blankResultBiz.getBlankResults(queryBO);
        } catch (ServiceException e) {
            log.error("获取空位作答结果失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("获取空位作答结果发生异常", e);
            throw new ServiceException(AppErrorCode.APP_GET_BLANK_RESULT_FAIL_MSG);
        }
    }

    /**
     * 获取题目信息Map
     *
     * @param questionIds 题目ID列表
     * @return 题目信息Map
     */
    private Map<Integer, QuestionResultDTO> getQuestionMap(Set<Integer> questionIds) {
        if (CollectionUtils.isEmpty(questionIds)) {
            return Collections.emptyMap();
        }

        List<Question> questions = questionBiz.getBaseMapper().selectList(
                new LambdaQueryWrapper<Question>()
                        .in(Question::getId, questionIds)
        );

        if (CollectionUtils.isEmpty(questions)) {
            return Collections.emptyMap();
        }

        Map<Integer, QuestionResultDTO> resultMap = new HashMap<>();
        for (Question question : questions) {
            QuestionResultDTO dto = new QuestionResultDTO();
            dto.setQuestionId(question.getId());
            dto.setTitle(question.getTitle());
            dto.setType(question.getType());
            dto.setMediaType(question.getMediaType());
            dto.setOptionType(question.getOptionType());
            dto.setAnalyzes(question.getAnalyzes());

            resultMap.put(question.getId(), dto);
        }

        return resultMap;
    }

    /**
     * 获取用户试卷普通题目作答结果
     *
     * @param questionResultBO 查询参数
     * @return 普通题目作答结果列表
     */
    private List<QuestionResultDTO> queryPaperQuestionResults(QueryQuestionResultBO questionResultBO) throws ServiceException {
        List<QuestionResultDTO> entities = questionResultBiz.getQuestionResults(questionResultBO);
        if (CollectionUtils.isEmpty(entities)) {
            return Collections.emptyList();
        }
        return entities;
    }

    /**
     * 统一处理题目资源，包括媒体文件、辅助识图和选项等
     *
     * @param questionResultList 题目结果列表
     */
    private void processQuestionResources(List<QuestionResultDTO> questionResultList) {
        if (CollectionUtils.isEmpty(questionResultList)) {
            return;
        }

        for (QuestionResultDTO result : questionResultList) {
            if (result == null || result.getQuestionId() == null) {
                continue;
            }

            // 设置题目的媒体文件和辅助识图
            setQuestionMediaResources(result.getQuestionId(), result);
            setQuestionRecognitionResources(result.getQuestionId(), result);

            // 确保选项数据存在
            if (!QuestionTypeEnum.CLOZE.getValue().equals(result.getType())) {
                List<QuestionAnswer> answers = questionAnswerBiz.getAnswersByQuestionId(result.getQuestionId());
                if (!CollectionUtils.isEmpty(answers)) {
                    List<QuestionAnswerDTO> answerDTOs = answers.stream().map(answer -> {
                        QuestionAnswerDTO answerDTO = new QuestionAnswerDTO();
                        BeanUtil.copyProperties(answer, answerDTO);
                        List<QuestionMediaDTO> mediaList = getAnswerMediaResources(answer.getId());
                        answerDTO.setMediaUrl(mediaList);
                        return answerDTO;
                    }).collect(Collectors.toList());

                    // 如果是排序题，打乱选项顺序
                    if (QuestionTypeEnum.SORT.getValue().equals(result.getType())) {
                        Collections.shuffle(answerDTOs);
                    }
                    result.setOptions(answerDTOs);
                }
            }

            // 处理普通题目答案显示
            if (!QuestionTypeEnum.CLOZE.getValue().equals(result.getType())) {
                processAnswerDisplay(result);
            }

            // 处理完形填空题
            if (QuestionTypeEnum.CLOZE.getValue().equals(result.getType())) {
                List<BlankResultDTO> blankList = result.getBlankResults();
                if (!CollectionUtils.isEmpty(blankList)) {
                    for (BlankResultDTO blank : blankList) {
                        if (blank == null || blank.getBlankAreaId() == null) {
                            continue;
                        }

                        List<QuestionAnswer> blankAnswers = questionAnswerBiz.getAnswersByBlankAreaId(blank.getQuestionId(), blank.getBlankAreaId());
                        if (!CollectionUtils.isEmpty(blankAnswers)) {
                            List<QuestionAnswerDTO> blankAnswerDTOs = blankAnswers.stream().map(answer -> {
                                QuestionAnswerDTO answerDTO = new QuestionAnswerDTO();
                                BeanUtil.copyProperties(answer, answerDTO);
                                answerDTO.setBlankAreaId(blank.getBlankAreaId());
                                List<QuestionMediaDTO> mediaList = getAnswerMediaResources(answer.getId());
                                answerDTO.setMediaUrl(mediaList);
                                return answerDTO;
                            }).collect(Collectors.toList());
                            blank.setOptions(blankAnswerDTOs);
                        }

                        processBlankAnswerDisplay(blank, result.getOptionType());
                    }
                    aggregateClozeAnswers(result);
                }
            }
        }
    }

    /**
     * 获取并设置题目的媒体文件
     *
     * @param questionId 题目ID
     * @param questionInfo 题目信息
     */
    private void setQuestionMediaResources(Integer questionId, QuestionResultDTO questionInfo) {
        // 如果题目信息中已有媒体文件，不需要再次查询
        if (!CollectionUtils.isEmpty(questionInfo.getMediaUrl())) {
            return;
        }

        // 从数据库获取媒体文件（使用新的QuestionMedia表）
        List<QuestionMedia> questionMediaList = questionMediaBiz.listByQuestionIdAndType(questionId, 1);
        if (!CollectionUtils.isEmpty(questionMediaList)) {
            List<QuestionMediaDTO> mediaList = questionMediaList.stream()
                    .map(media -> {
                        QuestionMediaDTO mediaDTO = new QuestionMediaDTO();
                        BeanUtil.copyProperties(media, mediaDTO);
                        // 兼容字段
                        mediaDTO.setMaterialId(media.getId());
                        mediaDTO.setSortNum(0); // 新表结构中没有排序号，使用默认值
                        return mediaDTO;
                    })
                    .collect(Collectors.toList());
            questionInfo.setMediaUrl(mediaList);
        }
    }

    /**
     * 获取并设置题目的辅助识图
     *
     * @param questionId 题目ID
     * @param questionInfo 题目信息
     */
    private void setQuestionRecognitionResources(Integer questionId, QuestionResultDTO questionInfo) {
        // 如果题目信息中已有辅助识图，不需要再次查询
        if (!CollectionUtils.isEmpty(questionInfo.getAidedRecognitionUrl())) {
            return;
        }

        // 从数据库获取辅助识图（使用新的QuestionMedia表，mediaType=3）
        List<QuestionMedia> recognitionMediaList = questionMediaBiz.listByQuestionIdAndType(questionId, 3);
        if (!CollectionUtils.isEmpty(recognitionMediaList)) {
            List<QuestionRecognitionDTO> recognitionList = recognitionMediaList.stream()
                    .map(media -> {
                        QuestionRecognitionDTO recognition = new QuestionRecognitionDTO();
                        recognition.setMaterialId(media.getId());
                        recognition.setMaterialPath(media.getMediaPath());
                        recognition.setMaterialName(media.getMediaName());
                        recognition.setSortNum(0); // 新表结构中没有排序号，使用默认值
                        return recognition;
                    })
                    .collect(Collectors.toList());
            questionInfo.setAidedRecognitionUrl(recognitionList);
        }
    }

    /**
     * 处理普通题目的答案显示
     */
    protected void processAnswerDisplay(QuestionResultDTO result) {
        if (!CollectionUtils.isEmpty(result.getOptions()) && StringUtils.isNotEmpty(result.getCorrectAnswerIds())) {
            List<String> correctAnswers = new ArrayList<>();
            String[] correctIds = result.getCorrectAnswerIds().split(",");
            for (String correctId : correctIds) {
                try {
                    Integer id = Integer.valueOf(correctId.trim());
                    result.getOptions().stream()
                            .filter(option -> option.getId().equals(id))
                            .findFirst()
                            .ifPresent(option -> {
                                if(result.getOptionType().equals(OptionTypeEnum.TEXT.getCode())){
                                    correctAnswers.add(QuestionTypeEnum.displayOptionContent(result.getType()) ? option.getOptionContent() : option.getOptionName());
                                }else if (Objects.requireNonNull(OptionTypeEnum.getByCode(result.getOptionType())).isMedia()){
                                    for (QuestionMediaDTO mediaDTO : option.getMediaUrl()) {
                                        correctAnswers.add(mediaDTO.getMediaPath());
                                    }
                                }
                            });
                } catch (NumberFormatException ignored) {}
            }
            result.setCorrectAnswer(String.join(", ", correctAnswers));
        }
        if (!CollectionUtils.isEmpty(result.getOptions()) && StringUtils.isNotEmpty(result.getUserAnswerIds())) {
            List<String> userAnswers = new ArrayList<>();
            String[] userIds = result.getUserAnswerIds().split(",");
            for (String userId : userIds) {
                try {
                    Integer id = Integer.valueOf(userId.trim());
                    result.getOptions().stream()
                            .filter(option -> option.getId().equals(id))
                            .findFirst()
                            .ifPresent(option -> {
                                if(result.getOptionType().equals(OptionTypeEnum.TEXT.getCode())){
                                    userAnswers.add(QuestionTypeEnum.displayOptionContent(result.getType())  ? option.getOptionContent() : option.getOptionName());
                                }else if (Objects.requireNonNull(OptionTypeEnum.getByCode(result.getOptionType())).isMedia()){
                                    for (QuestionMediaDTO mediaDTO : option.getMediaUrl()) {
                                        userAnswers.add(mediaDTO.getMediaPath());
                                    }
                                }
                            });
                } catch (NumberFormatException ignored) {}
            }
            result.setUserAnswer(String.join(", ", userAnswers));
        }
    }
    /**
     * 处理填空题答案显示
     */
    private void processBlankAnswerDisplay(BlankResultDTO blank,Integer optionType) {
        if (!CollectionUtils.isEmpty(blank.getOptions()) && StringUtils.isNotEmpty(blank.getCorrectAnswerIds())) {
            List<String> correctAnswers = new ArrayList<>();
            String[] correctIds = blank.getCorrectAnswerIds().split(",");
            for (String correctId : correctIds) {
                try {
                    Integer id = Integer.valueOf(correctId.trim());
                    blank.getOptions().stream()
                            .filter(option -> option.getId().equals(id))
                            .findFirst()
                            .ifPresent(option ->{
                                if(optionType.equals(OptionTypeEnum.TEXT.getCode())){
                                    correctAnswers.add(QuestionTypeEnum.displayOptionContent(optionType)  ? option.getOptionContent() : option.getOptionName());
                                }else if (Objects.requireNonNull(OptionTypeEnum.getByCode(optionType)).isMedia()){
                                    for (QuestionMediaDTO mediaDTO : option.getMediaUrl()) {
                                        correctAnswers.add(mediaDTO.getMediaPath());
                                    }
                                }
                            });
                } catch (NumberFormatException ignored) {}
            }
            blank.setCorrectAnswer(String.join(", ", correctAnswers));
        }
        if (!CollectionUtils.isEmpty(blank.getOptions()) && StringUtils.isNotEmpty(blank.getUserAnswerIds())) {
            List<String> userAnswers = new ArrayList<>();
            String[] userIds = blank.getUserAnswerIds().split(",");
            for (String userId : userIds) {
                try {
                    Integer id = Integer.valueOf(userId.trim());
                    blank.getOptions().stream()
                            .filter(option -> option.getId().equals(id))
                            .findFirst()
                            .ifPresent(option -> {
                                if(optionType.equals(OptionTypeEnum.TEXT.getCode())){
                                    userAnswers.add(QuestionTypeEnum.displayOptionContent(optionType)  ? option.getOptionContent() : option.getOptionName());
                                }else if (Objects.requireNonNull(OptionTypeEnum.getByCode(optionType)).isMedia()){
                                    for (QuestionMediaDTO mediaDTO : option.getMediaUrl()) {
                                        userAnswers.add(mediaDTO.getMediaPath());
                                    }
                                }
                            });
                } catch (NumberFormatException ignored) {}
            }
            blank.setUserAnswer(String.join(", ", userAnswers));
        }
        if (!CollectionUtils.isEmpty(blank.getOptions()) && blank.getBlankAreaId() != null) {
            for (QuestionAnswerDTO option : blank.getOptions()) {
                if (option.getBlankAreaId() == null) {
                    option.setBlankAreaId(blank.getBlankAreaId());
                }
            }
        }
    }

    /**
     * 聚合空位答案，为主题目汇总所有空位区域的用户答案信息
     */
    private void aggregateClozeAnswers(QuestionResultDTO result) {
        List<BlankResultDTO> blankResults = result.getBlankResults();
        if (CollectionUtils.isEmpty(blankResults)) {
            return;
        }
        blankResults.sort(Comparator.comparing(BlankResultDTO::getBlankIndex, Comparator.nullsLast(Comparator.naturalOrder())));
        List<String> allUserAnswers = new ArrayList<>();
        List<String> allUserAnswerIds = new ArrayList<>();
        List<String> allCorrectAnswers = new ArrayList<>();
        List<String> allCorrectAnswerIds = new ArrayList<>();
        for (BlankResultDTO blank : blankResults) {
            if (StringUtils.isNotEmpty(blank.getUserAnswer())) {
                allUserAnswers.add(blank.getUserAnswer());
            }
            if (StringUtils.isNotEmpty(blank.getUserAnswerIds())) {
                allUserAnswerIds.add(blank.getUserAnswerIds());
            }
            if (StringUtils.isNotEmpty(blank.getCorrectAnswer())) {
                allCorrectAnswers.add(blank.getCorrectAnswer());
            }
            if (StringUtils.isNotEmpty(blank.getCorrectAnswerIds())) {
                allCorrectAnswerIds.add(blank.getCorrectAnswerIds());
            }
        }
        if (!allUserAnswers.isEmpty()) {
            result.setUserAnswer(String.join(",", allUserAnswers));
        } else {
            result.setUserAnswer("");
        }
        if (!allUserAnswerIds.isEmpty()) {
            result.setUserAnswerIds(String.join(",", allUserAnswerIds));
        } else {
            result.setUserAnswerIds("");
        }

        if (!allCorrectAnswers.isEmpty()) {
            result.setCorrectAnswer(String.join(",", allCorrectAnswers));
        } else {
            result.setCorrectAnswer("");
        }
        if (!allCorrectAnswerIds.isEmpty()) {
            result.setCorrectAnswerIds(String.join(",", allCorrectAnswerIds));
        } else {
            result.setCorrectAnswerIds("");
        }
    }

    /**
     * 获取并设置选项的媒体资源
     *
     * @param answerId 选项ID
     * @return 媒体资源列表
     */
    private List<QuestionMediaDTO> getAnswerMediaResources(Integer answerId) {
        // 获取选项的媒体文件（使用新的QuestionMedia表）
        List<QuestionMedia> answerMediaList = questionMediaBiz.list(
            new LambdaQueryWrapper<QuestionMedia>()
                .eq(QuestionMedia::getOptionId, answerId)
                .eq(QuestionMedia::getMediaType, 2)
        );
        if (CollectionUtils.isEmpty(answerMediaList)) {
            return Collections.emptyList();
        }

        return answerMediaList.stream()
                .map(media -> {
                    QuestionMediaDTO mediaDTO = new QuestionMediaDTO();
                    BeanUtil.copyProperties(media, mediaDTO);
                    // 兼容字段
                    mediaDTO.setMaterialId(media.getId());
                    mediaDTO.setSortNum(0);
                    return mediaDTO;
                })
                .collect(Collectors.toList());
    }


    protected QuestionCorrectAnswerDTO buildCorrectAnswerDTO(Question question, List<QuestionAnswer> answers) {
        QuestionCorrectAnswerDTO dto = new QuestionCorrectAnswerDTO();
        dto.setQuestionId(question.getId());
        dto.setTitle(question.getTitle());
        dto.setType(question.getType());
        dto.setCorrectAnswerIds(question.getAnswer());

        if (QuestionTypeEnum.CLOZE.getValue().equals(question.getType())) {
            List<QuestionBlankArea> blankAreas = questionBlankAreaBiz.getBlankAreasByQuestionId(question.getId());
            if (!CollectionUtils.isEmpty(blankAreas)) {
                List<BlankCorrectAnswerDTO> blankResults = new ArrayList<>();
                Map<Integer, Map<Integer, List<QuestionAnswer>>> blankAnswerMap = questionBiz.getBlankAreaAnswerMap(Set.of(question.getId()));
                for (QuestionBlankArea area : blankAreas) {
                    BlankCorrectAnswerDTO blankDto = new BlankCorrectAnswerDTO();
                    blankDto.setBlankAreaId(area.getId());
                    blankDto.setBlankIndex(area.getBlankIndex());
                    if (area.getAnswerIds() != null) {
                        List<QuestionAnswer> blankAnswers = blankAnswerMap.getOrDefault(question.getId(), Collections.emptyMap())
                                .getOrDefault(area.getId(), Collections.emptyList());
                        if (!CollectionUtils.isEmpty(blankAnswers)) {
                            Set<String> correctIds = Arrays.stream(area.getAnswerIds().split(",")).collect(Collectors.toSet());
                            List<String> correctContents = new ArrayList<>();
                            for (QuestionAnswer answer : blankAnswers) {
                                if (correctIds.contains(String.valueOf(answer.getId()))) {
                                    if (OptionTypeEnum.TEXT.getCode().equals(question.getOptionType())) {
                                        correctContents.add(QuestionTypeEnum.displayOptionContent(question.getOptionType())  ? answer.getOptionContent() : answer.getOptionName());
                                    } else if (Objects.requireNonNull(OptionTypeEnum.getByCode(question.getOptionType())).isMedia()) {
                                        List<QuestionMediaDTO> mediaList = queryAnswerMediaResources(answer.getId());
                                        if (!CollectionUtils.isEmpty(mediaList)) {
                                            mediaList.forEach(media -> correctContents.add(media.getMediaPath()));
                                        }
                                    }
                                }
                            }
                            blankDto.setCorrectAnswer(String.join(",", correctContents));
                        } else {
                            blankDto.setCorrectAnswer("");
                        }
                    } else {
                        blankDto.setCorrectAnswer("");
                    }
                    blankResults.add(blankDto);
                }
                dto.setBlankResults(blankResults);
            }
            dto.setCorrectAnswer("");
        } else {
            if (!CollectionUtils.isEmpty(answers) && question.getAnswer() != null) {
                Set<String> correctIds = Arrays.stream(question.getAnswer().split(",")).collect(Collectors.toSet());
                List<String> correctContents = new ArrayList<>();
                for (QuestionAnswer answer : answers) {
                    if (correctIds.contains(String.valueOf(answer.getId()))) {
                        if (OptionTypeEnum.TEXT.getCode().equals(question.getOptionType())) {
                            correctContents.add(QuestionTypeEnum.displayOptionContent(question.getOptionType())  ? answer.getOptionContent() : answer.getOptionName());
                        } else if (Objects.requireNonNull(OptionTypeEnum.getByCode(question.getOptionType())).isMedia()) {
                            List<QuestionMediaDTO> mediaList = queryAnswerMediaResources(answer.getId());
                            if (!CollectionUtils.isEmpty(mediaList)) {
                                mediaList.forEach(media -> correctContents.add(media.getMediaPath()));
                            }
                        }
                    }
                }
                dto.setCorrectAnswer(String.join(",", correctContents));
            } else {
                dto.setCorrectAnswer("");
            }
        }
        return dto;
    }

    /**
     * 查询选项的媒体资源
     *
     * @param answerId 选项ID
     * @return 媒体资源列表
     */
    private List<QuestionMediaDTO> queryAnswerMediaResources(Integer answerId) {
        // 获取选项的媒体文件（使用新的QuestionMedia表）
        List<QuestionMedia> answerMediaList = questionMediaBiz.list(
            new LambdaQueryWrapper<QuestionMedia>()
                .eq(QuestionMedia::getOptionId, answerId)
                .eq(QuestionMedia::getMediaType, 2)
        );

        if (CollectionUtils.isEmpty(answerMediaList)) {
            return Collections.emptyList();
        }

        return answerMediaList.stream()
                .map(media -> {
                    QuestionMediaDTO mediaDTO = new QuestionMediaDTO();
                    BeanUtil.copyProperties(media, mediaDTO);
                    // 兼容字段
                    mediaDTO.setMaterialId(media.getId());
                    mediaDTO.setSortNum(0);
                    return mediaDTO;
                })
                .collect(Collectors.toList());
    }


    private List<PaperQuestionDTO> buildPaperQuestionDTOs(List<Question> questions, Map<Integer, Integer> questionOrderMap) throws ServiceException {
        if (questions == null || questions.isEmpty()) {
            throw new ServiceException("题目列表为空");
        }
        Map<Integer, Integer> typeCountMap = new LinkedHashMap<>();
        for (Question question : questions) {
            typeCountMap.merge(question.getType(), 1, Integer::sum);
        }
        Map<Integer, Integer> typeIndexMap = new LinkedHashMap<>();
        List<PaperQuestionDTO> result = new LinkedList<>();
        int sortNum = 1;
        for (Question question : questions) {
            PaperQuestionDTO dto = new PaperQuestionDTO();
            BeanUtils.copyProperties(question, dto);
            dto.setTypeName(QuestionTypeEnum.getByValue(question.getType()).getDesc());
            if (questionOrderMap != null && questionOrderMap.containsKey(question.getId())) {
                dto.setSortNum(questionOrderMap.get(question.getId()));
            } else {
                dto.setSortNum(sortNum++);
            }
            int currentIndex = typeIndexMap.getOrDefault(question.getType(), 0) + 1;
            typeIndexMap.put(question.getType(), currentIndex);
            dto.setTypeCount(typeCountMap.get(question.getType()));
            dto.setTypeIndex(currentIndex);

            // 获取题目的媒体文件（使用新的QuestionMedia表）
            List<QuestionMedia> questionMediaList = questionMediaBiz.listByQuestionIdAndType(question.getId(), 1);
            if (!CollectionUtils.isEmpty(questionMediaList)) {
                List<QuestionMediaDTO> mediaList = questionMediaList.stream()
                        .map(media -> {
                            QuestionMediaDTO mediaDTO = new QuestionMediaDTO();
                            BeanUtil.copyProperties(media, mediaDTO);
                            // 兼容字段
                            mediaDTO.setMaterialId(media.getId());
                            mediaDTO.setSortNum(0); // 新表结构中没有排序号，使用默认值
                            return mediaDTO;
                        })
                        .collect(Collectors.toList());
                dto.setMediaUrl(mediaList);
            }
            
            // 获取题目的辅助识图（使用新的QuestionMedia表，mediaType=3）
            List<QuestionMedia> recognitionMediaList = questionMediaBiz.listByQuestionIdAndType(question.getId(), 3);
            if (!CollectionUtils.isEmpty(recognitionMediaList)) {
                List<QuestionRecognitionDTO> recognitionList = recognitionMediaList.stream()
                        .map(media -> {
                            QuestionRecognitionDTO recognition = new QuestionRecognitionDTO();
                            recognition.setMaterialId(media.getId());
                            recognition.setMaterialPath(media.getMediaPath());
                            recognition.setMaterialName(media.getMediaName());
                            recognition.setSortNum(0); // 新表结构中没有排序号，使用默认值
                            return recognition;
                        })
                        .collect(Collectors.toList());
                dto.setAidedRecognitionUrl(recognitionList);
            }

            List<QuestionAnswer> answers = questionAnswerBiz.getAnswersByQuestionId(question.getId());
            if (!CollectionUtils.isEmpty(answers) && !Objects.equals(question.getType(), QuestionTypeEnum.CLOZE.getValue()) ) {
                List<QuestionAnswerDTO> answerDTOs = answers.stream()
                        .map(answer -> {
                            QuestionAnswerDTO answerDTO = new QuestionAnswerDTO();
                            BeanUtil.copyProperties(answer, answerDTO);
                            // 获取选项的媒体文件（使用新的QuestionMedia表）
                            List<QuestionMedia> answerMediaList = questionMediaBiz.list(
                                new LambdaQueryWrapper<QuestionMedia>()
                                    .eq(QuestionMedia::getOptionId, answer.getId())
                                    .eq(QuestionMedia::getMediaType, 2)
                            );
                            if (!CollectionUtils.isEmpty(answerMediaList)) {
                                List<QuestionMediaDTO> mediaList = answerMediaList.stream()
                                        .map(media -> {
                                            QuestionMediaDTO mediaDTO = new QuestionMediaDTO();
                                            BeanUtil.copyProperties(media, mediaDTO);
                                            // 兼容字段
                                            mediaDTO.setMaterialId(media.getId());
                                            mediaDTO.setSortNum(0);
                                            return mediaDTO;
                                        })
                                        .collect(Collectors.toList());
                                answerDTO.setMediaUrl(mediaList);
                            }
                            return answerDTO;
                        })
                        .collect(Collectors.toList());
                if (QuestionTypeEnum.SORT.getValue().equals(question.getType())) {
                    Collections.shuffle(answerDTOs);
                }
                dto.setAnswers(answerDTOs);
            } else if (Objects.equals(question.getType(), QuestionTypeEnum.CLOZE.getValue())) {
                List<QuestionBlankArea> blankAreas = questionBlankAreaBiz.getBlankAreasByQuestionId(question.getId());
                if (!CollectionUtils.isEmpty(blankAreas)) {
                    List<QuestionBlankAreaDTO> blankAreaDTOs = new ArrayList<>();
                    for (QuestionBlankArea area : blankAreas) {
                        QuestionBlankAreaDTO areaDTO = new QuestionBlankAreaDTO();
                        BeanUtil.copyProperties(area, areaDTO);
                        List<QuestionAnswer> blankAnswers = questionAnswerBiz.getAnswersByBlankAreaId(question.getId(), area.getId());
                        if (!CollectionUtils.isEmpty(blankAnswers)) {
                            List<QuestionAnswerDTO> blankAnswerDTOs = blankAnswers.stream()
                                    .map(blankAnswer -> {
                                        QuestionAnswerDTO answerDTO = new QuestionAnswerDTO();
                                        BeanUtil.copyProperties(blankAnswer, answerDTO);
                                        // 获取完形填空选项的媒体文件（使用新的QuestionMedia表）
                                        List<QuestionMedia> blankAnswerMediaList = questionMediaBiz.list(
                                            new LambdaQueryWrapper<QuestionMedia>()
                                                .eq(QuestionMedia::getOptionId, blankAnswer.getId())
                                                .eq(QuestionMedia::getMediaType, 2)
                                        );
                                        if (!CollectionUtils.isEmpty(blankAnswerMediaList)) {
                                            List<QuestionMediaDTO> mediaList = blankAnswerMediaList.stream()
                                                    .map(media -> {
                                                        QuestionMediaDTO mediaDTO = new QuestionMediaDTO();
                                                        BeanUtil.copyProperties(media, mediaDTO);
                                                        // 兼容字段
                                                        mediaDTO.setMaterialId(media.getId());
                                                        mediaDTO.setSortNum(0);
                                                        return mediaDTO;
                                                    })
                                                    .collect(Collectors.toList());
                                            answerDTO.setMediaUrl(mediaList);
                                        }
                                        return answerDTO;
                                    })
                                    .collect(Collectors.toList());
                            areaDTO.setAnswers(blankAnswerDTOs);
                        }
                        blankAreaDTOs.add(areaDTO);
                    }
                    dto.setBlankAreas(blankAreaDTOs);
                }
            }
            result.add(dto);
        }
        result.sort(Comparator.comparing(PaperQuestionDTO::getSortNum).thenComparing(PaperQuestionDTO::getId));
        return result;
    }
}