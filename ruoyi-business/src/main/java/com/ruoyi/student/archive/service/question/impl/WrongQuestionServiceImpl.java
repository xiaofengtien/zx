package com.ruoyi.student.archive.service.question.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.AppErrorCode;
import com.ruoyi.common.enums.YesOrNoEnum;
import com.ruoyi.common.enums.question.OptionTypeEnum;
import com.ruoyi.common.enums.question.QuestionTypeEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.biz.question.*;
import com.ruoyi.system.service.ISysDictDataService;
import com.ruoyi.student.archive.domain.bo.question.*;
import com.ruoyi.student.archive.domain.dto.question.*;
import com.ruoyi.student.archive.domain.question.Question;
import com.ruoyi.student.archive.domain.question.QuestionAnswer;
import com.ruoyi.student.archive.domain.question.QuestionBlankArea;
import com.ruoyi.student.archive.domain.question.QuestionMedia;
import com.ruoyi.student.archive.domain.question.wrongquestion.AppUserWrongQuestionBlank;
import com.ruoyi.student.archive.domain.question.wrongquestion.AppUserWrongQuestionNormal;
import com.ruoyi.student.archive.service.question.WrongQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户错题本服务实现类
 */
@Slf4j
@Service
public class WrongQuestionServiceImpl implements WrongQuestionService {
    @Resource
    private IQuestionMediaBiz questionMediaBiz;
    @Resource
    private IAppUserWrongQuestionNormalBiz appUserWrongQuestionNormalBiz;
    
    @Resource
    private IAppUserWrongQuestionBlankBiz appUserWrongQuestionBlankBiz;

    @Resource
    private QuestionBiz questionBiz;

    @Resource
    private QuestionBlankAreaBiz questionBlankAreaBiz;
    
    @Resource
    private QuestionAnswerBiz questionAnswerBiz;

    @Resource
    private ISysDictDataService dictDataService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addWrongQuestion(AddWrongQuestionBO addWrongQuestionBO) throws ServiceException {
        validateParam(addWrongQuestionBO);
        
        // 单个添加转为批量添加处理
        List<AddWrongQuestionBO> boList = Collections.singletonList(addWrongQuestionBO);
        return batchAddWrongQuestions(boList);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeWrongQuestion(WrongQuestionPkIdsBO removeBO) throws ServiceException {
        validateParam(removeBO);
        
        Integer appUserId = removeBO.getAppUserId();
        List<Integer> ids = removeBO.getIds();

        appUserWrongQuestionNormalBiz.removeWrongQuestion(appUserId,ids);
        appUserWrongQuestionBlankBiz.removeWrongQuestion(appUserId, ids);
        return true;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchAddWrongQuestions(List<AddWrongQuestionBO> addWrongQuestionBOList) throws ServiceException {
        validateParam(addWrongQuestionBOList);

        try {
            // 将请求中的普通题和完形填空题分开处理
            Map<Integer, List<AddWrongQuestionBO>> userNormalQuestionMap = new HashMap<>(); // 普通题
            Map<Integer, List<WrongQuestionBlankBO>> userBlankQuestionMap = new HashMap<>(); // 完形填空题
            Set<Integer> normalQuestionIds = new LinkedHashSet<>(); // 普通题ID集合
            Set<Integer> blankQuestionIds = new LinkedHashSet<>(); // 完形填空题ID集合
            Set<Integer> blankAreaIds = new LinkedHashSet<>(); // 空白区域ID集合

            // 第一步：分离普通题和完形填空题
            for (AddWrongQuestionBO bo : addWrongQuestionBOList) {
                // 处理主题目（普通题）
                Integer appUserId = bo.getAppUserId();
                Integer questionId = bo.getQuestionId();

                if (questionId != null) {
                    normalQuestionIds.add(questionId);
                    userNormalQuestionMap.computeIfAbsent(appUserId, k -> new ArrayList<>()).add(bo);
                }

                // 处理完形填空题
                if (bo.getBlankResults() != null && !bo.getBlankResults().isEmpty()) {
                    for (WrongQuestionBlankBO blankBO : bo.getBlankResults()) {
                        if (blankBO.getQuestionId() != null) {
                            // 收集完形填空题ID
                            blankQuestionIds.add(blankBO.getQuestionId());

                            // 收集空白区域ID
                            if (blankBO.getBlankAreaId() != null) {
                                blankAreaIds.add(blankBO.getBlankAreaId());
                            }

                            // 按用户ID分组
                            userBlankQuestionMap.computeIfAbsent(appUserId, k -> new ArrayList<>()).add(blankBO);
                        }
                    }
                }
            }

            if (normalQuestionIds.isEmpty() && blankQuestionIds.isEmpty()) {
                log.info("批量添加错题：无需要处理的题目");
                return true;
            }

            log.info("批量添加错题开始：用户数={}，普通题数={}，完形填空题数={}",
                    userNormalQuestionMap.size() + userBlankQuestionMap.size(),
                    normalQuestionIds.size(), blankQuestionIds.size());

            // 查询所有题目信息
            Map<Integer, Question> allQuestionMap = new HashMap<>();
            if (!normalQuestionIds.isEmpty()) {
                Map<Integer, Question> normalQuestionMap = batchQueryQuestions(normalQuestionIds);
                allQuestionMap.putAll(normalQuestionMap);
            }
            if (!blankQuestionIds.isEmpty()) {
                Map<Integer, Question> blankQuestionMap = batchQueryQuestions(blankQuestionIds);
                allQuestionMap.putAll(blankQuestionMap);
            }

            // 查询空白区域信息
            Map<Integer, QuestionBlankArea> blankAreaMap = Collections.emptyMap();
            if (!blankAreaIds.isEmpty()) {
                blankAreaMap = batchQueryBlankAreas(blankAreaIds);
            }

            // 处理普通题错题
            for (Map.Entry<Integer, List<AddWrongQuestionBO>> entry : userNormalQuestionMap.entrySet()) {
                Integer appUserId = entry.getKey();
                List<AddWrongQuestionBO> normalQuestionBOs = entry.getValue();

                // 过滤出真正的普通题（非完形填空题）
                List<AddWrongQuestionBO> filteredNormalQuestionBOs = new ArrayList<>();
                for (AddWrongQuestionBO bo : normalQuestionBOs) {
                    Question question = allQuestionMap.get(bo.getQuestionId());
                    if (question != null && (question.getType() == null ||
                            !question.getType().equals(QuestionTypeEnum.CLOZE.getValue()))) {
                        filteredNormalQuestionBOs.add(bo);
                    }
                }

                if (!filteredNormalQuestionBOs.isEmpty()) {
                    processNormalWrongQuestions(appUserId, filteredNormalQuestionBOs, allQuestionMap);
                }
            }

            // 处理完形填空题错题
            for (Map.Entry<Integer, List<WrongQuestionBlankBO>> entry : userBlankQuestionMap.entrySet()) {
                Integer appUserId = entry.getKey();
                List<WrongQuestionBlankBO> blankBOs = entry.getValue();

                if (!blankBOs.isEmpty()) {
                    processBlankWrongQuestions(appUserId, blankBOs, allQuestionMap, blankAreaMap);
                }
            }

            logProcessResult(userNormalQuestionMap.size(), normalQuestionIds.size(), blankAreaIds.size());
            return true;
        } catch (Exception e) {
            log.error("批量添加错题失败", e);
            throw new ServiceException("添加错题失败：" + e.getMessage());
        }
    }

    /**
     * 处理普通题错题
     *
     * @param appUserId 用户ID
     * @param normalQuestionBOs 普通题错题列表
     * @param questionMap 题目映射
     * @throws ServiceException 业务异常
     */
    private void processNormalWrongQuestions(Integer appUserId, List<AddWrongQuestionBO> normalQuestionBOs,
                                             Map<Integer, Question> questionMap) throws ServiceException {
        List<AddNormalWrongQuestionBO> normalWrongQuestionBOs = new ArrayList<>();

        for (AddWrongQuestionBO bo : normalQuestionBOs) {
            Integer questionId = bo.getQuestionId();
            Question question = questionMap.get(questionId);

            if (question == null) {
                log.error("题目不存在: {}", questionId);
                continue;
            }

            // 构建普通错题BO，直接使用请求中的数据
            AddNormalWrongQuestionBO normalWrongQuestionBO = new AddNormalWrongQuestionBO();
            normalWrongQuestionBO.setAppUserId(appUserId);
            normalWrongQuestionBO.setQuestionId(questionId);
            normalWrongQuestionBO.setSourceId(bo.getSourceId());
            normalWrongQuestionBO.setSourceType(bo.getSourceType());
            normalWrongQuestionBO.setUserAnswer(bo.getUserAnswer());
            normalWrongQuestionBO.setAnswerIds(bo.getUserAnswerIds());
            String uuid = UUID.randomUUID().toString().replace("-", "");
            normalWrongQuestionBO.setStateId(uuid);
            normalWrongQuestionBOs.add(normalWrongQuestionBO);
        }

        // 批量添加普通错题
        if (!normalWrongQuestionBOs.isEmpty()) {
            appUserWrongQuestionNormalBiz.batchAddWrongQuestions(normalWrongQuestionBOs);
        }
    }

    /**
     * 处理完形填空题错题
     *
     * @param appUserId 用户ID
     * @param blankBOs 完形填空错题列表
     * @param questionMap 题目映射
     * @param blankAreaMap 空白区域映射
     * @throws ServiceException 业务异常
     */
    private void processBlankWrongQuestions(Integer appUserId, List<WrongQuestionBlankBO> blankBOs,
                                            Map<Integer, Question> questionMap,
                                            Map<Integer, QuestionBlankArea> blankAreaMap) throws ServiceException {
        List<AddBlankWrongQuestionBO> blankWrongQuestions = new ArrayList<>();
        Map<Integer,String > stateId = MapUtil.newHashMap();
        Set<Integer> questionIds = blankBOs.stream().map(WrongQuestionBlankBO::getQuestionId).collect(Collectors.toSet());
        for (Integer questionId : questionIds) {
            String uuid = UUID.randomUUID().toString().replace("-", "");
            stateId.put(questionId,uuid);
        }

        // 直接处理每个空白区域错题
        for (WrongQuestionBlankBO blankBO : blankBOs) {
            Integer questionId = blankBO.getQuestionId();
            Integer blankAreaId = blankBO.getBlankAreaId();

            if (questionId == null) {
                log.error("题目ID为空");
                continue;
            }

            if (blankAreaId == null) {
                log.error("空白区域ID为空: questionId={}", questionId);
                continue;
            }

            Question question = questionMap.get(questionId);
            if (question == null) {
                log.error("完形填空题不存在: {}", questionId);
                continue;
            }

            QuestionBlankArea blankArea = blankAreaMap.get(blankAreaId);
            if (blankArea == null) {
                log.error("空白区域不存在: blankAreaId={}", blankAreaId);
                continue;
            }

            // 每个空白区域对应一条记录
            AddBlankWrongQuestionBO blankWrongQuestion = new AddBlankWrongQuestionBO();
            blankWrongQuestion.setAppUserId(appUserId);
            blankWrongQuestion.setQuestionId(questionId);
            blankWrongQuestion.setBlankAreaId(blankAreaId);
            blankWrongQuestion.setBlankIndex(blankBO.getBlankIndex());
            blankWrongQuestion.setUserAnswer(blankBO.getUserAnswer());
            blankWrongQuestion.setAnswerIds(blankBO.getUserAnswerIds());
            blankWrongQuestion.setStateId(stateId.get(questionId));
            blankWrongQuestions.add(blankWrongQuestion);
        }

        // 批量添加完形填空错题
        if (!blankWrongQuestions.isEmpty()) {
            appUserWrongQuestionBlankBiz.batchAddWrongQuestions(blankWrongQuestions);
        }
    }
    /**
     * 批量查询题目信息
     *
     * @param questionIds 题目ID集合
     * @return 题目ID到题目对象的映射
     * @throws ServiceException 业务异常
     */
    private Map<Integer, Question> batchQueryQuestions(Set<Integer> questionIds) throws ServiceException {
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Question::getId, questionIds);
        List<Question> questions = questionBiz.list(queryWrapper);

        if (questions.size() != questionIds.size()) {
            log.error("部分题目不存在，请求题目ID: {}，查询结果题目数: {}", questionIds, questions.size());
            throw new ServiceException("部分题目不存在");
        }

        // 题目ID到题目对象的映射
        return questions.stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
    }
    /**
     * 批量查询空白区域信息
     *
     * @param blankAreaIds 空白区域ID集合
     * @return 空白区域ID到空白区域对象的映射
     * @throws ServiceException 业务异常
     */
    private Map<Integer, QuestionBlankArea> batchQueryBlankAreas(Set<Integer> blankAreaIds) throws ServiceException {
        if (blankAreaIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<QuestionBlankArea> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(QuestionBlankArea::getId, blankAreaIds);
        List<QuestionBlankArea> blankAreas = questionBlankAreaBiz.list(queryWrapper);

        if (blankAreas.size() != blankAreaIds.size()) {
            log.error("部分空白区域不存在，请求ID: {}，查询结果数: {}", blankAreaIds, blankAreas.size());
            throw new ServiceException("部分空白区域不存在");
        }

        // 空白区域ID到空白区域对象的映射
        return blankAreas.stream()
                .collect(Collectors.toMap(QuestionBlankArea::getId, Function.identity()));
    }

    @Override
    public List<QuestionCorrectAnswerDTO> getWrongQuestionsCorrectAnswers(Integer appUserId, Set<Integer> questionIds) throws ServiceException {
        if (appUserId == null) {
            log.error("获取错题正确答案失败：用户ID为空");
            throw new ServiceException(AppErrorCode.PARAMS_NOT_NULL_MSG);
        }

        if (CollectionUtils.isEmpty(questionIds)) {
            log.warn("获取错题正确答案：题目ID列表为空, appUserId={}", appUserId);
            return Collections.emptyList();
        }

        try {
            log.info("开始获取错题正确答案: appUserId={}, questionCount={}", appUserId, questionIds.size());

            // 查询题目信息
            List<Question> questions = getQuestionsByIds(questionIds);
            if (CollectionUtils.isEmpty(questions)) {
                log.warn("未找到任何题目信息: appUserId={}, questionIds={}", appUserId, questionIds);
                return Collections.emptyList();
            }

            // 按题目类型分组，提高处理效率
            Map<Integer, List<Question>> questionTypeMap = questions.stream()
                    .collect(Collectors.groupingBy(q -> q.getType() != null ? q.getType() : -1));

            List<QuestionCorrectAnswerDTO> resultList = new ArrayList<>(questions.size());

            // 处理普通题目
            List<Question> normalQuestions = new ArrayList<>();
            questionTypeMap.forEach((type, typeQuestions) -> {
                if (type != null && !QuestionTypeEnum.CLOZE.getValue().equals(type) 
                    && !QuestionTypeEnum.FILL_BLANK.getValue().equals(type)
                    && !QuestionTypeEnum.SORT.getValue().equals(type)) {
                    normalQuestions.addAll(typeQuestions);
                }
            });

            if (!normalQuestions.isEmpty()) {
                resultList.addAll(processNormalQuestions(normalQuestions));
            }

            // 处理填空题和排序题
            List<Question> fillBlankAndSortQuestions = new ArrayList<>();
            if (questionTypeMap.containsKey(QuestionTypeEnum.FILL_BLANK.getValue())) {
                fillBlankAndSortQuestions.addAll(questionTypeMap.get(QuestionTypeEnum.FILL_BLANK.getValue()));
            }
            if (questionTypeMap.containsKey(QuestionTypeEnum.SORT.getValue())) {
                fillBlankAndSortQuestions.addAll(questionTypeMap.get(QuestionTypeEnum.SORT.getValue()));
            }
            
            if (!fillBlankAndSortQuestions.isEmpty()) {
                resultList.addAll(processNormalQuestions(fillBlankAndSortQuestions));
            }

            // 处理完形填空题
            List<Question> clozeQuestions = questionTypeMap.getOrDefault(QuestionTypeEnum.CLOZE.getValue(), Collections.emptyList());
            if (!clozeQuestions.isEmpty()) {
                resultList.addAll(processFillBlankQuestions(clozeQuestions));
            }

            log.info("获取错题正确答案完成: appUserId={}, 返回结果数={}", appUserId, JSON.toJSONString(resultList));
            return resultList;
        } catch (Exception e) {
            log.error("获取错题正确答案异常: appUserId={}, questionIds={}", appUserId, questionIds, e);
            throw new ServiceException("获取错题正确答案失败: " + e.getMessage());
        }
    }

    @Override
    public List<WrongQuestionSubjectDTO> listSubject(Integer appUserId) {
        List<AppUserWrongQuestionNormal> normalWrongQuestions = appUserWrongQuestionNormalBiz.getBaseMapper()
                .selectList(new LambdaQueryWrapper<AppUserWrongQuestionNormal>()
                        .eq(AppUserWrongQuestionNormal::getAppUserId,appUserId));
        List<AppUserWrongQuestionBlank> blankWrongQuestions = appUserWrongQuestionBlankBiz.getBaseMapper()
                .selectList(new LambdaQueryWrapper<AppUserWrongQuestionBlank>()
                        .eq(AppUserWrongQuestionBlank::getAppUserId,appUserId));
        if(CollectionUtils.isEmpty(normalWrongQuestions) && CollectionUtils.isEmpty(blankWrongQuestions)){
            return Collections.emptyList();
        }
        List<Integer> normalQuestionIds = normalWrongQuestions.stream().map(AppUserWrongQuestionNormal::getQuestionId).collect(Collectors.toList());
        List<Integer> blankQuestionIds = blankWrongQuestions.stream().map(AppUserWrongQuestionBlank::getQuestionId).collect(Collectors.toList());

        if(CollectionUtils.isEmpty(normalQuestionIds) && CollectionUtils.isEmpty(blankQuestionIds)){
            return Collections.emptyList();
        }

        List<Integer> questionIds = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(normalQuestionIds)){
            questionIds.addAll(normalQuestionIds);
        }
        if(CollectionUtils.isNotEmpty(blankQuestionIds)){
            questionIds.addAll(blankQuestionIds);
        }

        List<Question> questions = questionBiz.listByIds(questionIds);
        Set<Integer> subjectIds = questions.stream()
                .map(Question::getSubjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        List<WrongQuestionSubjectDTO> result = new ArrayList<>();
        for (Integer subjectId : subjectIds) {
            WrongQuestionSubjectDTO subjectDTO = new WrongQuestionSubjectDTO();
            subjectDTO.setSubjectId(subjectId);
            // 使用字典表获取学科名称（字典类型：subject）
            String subjectName = dictDataService.selectDictLabel("subject", String.valueOf(subjectId));
            subjectDTO.setSubjectName(subjectName != null ? subjectName : "");
            result.add(subjectDTO);
        }
        return result.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 根据ID列表获取题目信息
     *
     * @param questionIds 题目ID集合
     * @return 题目列表
     */
    private List<Question> getQuestionsByIds(Set<Integer> questionIds) {
        if (CollectionUtils.isEmpty(questionIds)) {
            return Collections.emptyList();
        }
        
        LambdaQueryWrapper<Question> questionWrapper = new LambdaQueryWrapper<>();
        questionWrapper.in(Question::getId, questionIds);
        return questionBiz.list(questionWrapper);
    }

    /**
     * 处理普通题目的正确答案
     *
     * @param questions 普通题目列表
     * @return 题目正确答案DTO列表
     */
    private List<QuestionCorrectAnswerDTO> processNormalQuestions(List<Question> questions) {
        if (CollectionUtils.isEmpty(questions)) {
            return Collections.emptyList();
        }
        
        // 批量获取所有题目的答案
        Map<Integer, List<QuestionAnswer>> questionAnswerMap = new HashMap<>();
        Set<Integer> questionIds = questions.stream().map(Question::getId).collect(Collectors.toSet());
        
        // 批量查询所有题目的答案
        for (Integer questionId : questionIds) {
            List<QuestionAnswer> answers = questionAnswerBiz.getAnswersByQuestionId(questionId);
            if (!CollectionUtils.isEmpty(answers)) {
                questionAnswerMap.put(questionId, answers);
            }
        }
        
        // 构建结果
        List<QuestionCorrectAnswerDTO> resultList = new ArrayList<>(questions.size());
        for (Question question : questions) {
            QuestionCorrectAnswerDTO answerDTO = new QuestionCorrectAnswerDTO();
            answerDTO.setQuestionId(question.getId());
            answerDTO.setTitle(question.getTitle());
            answerDTO.setType(question.getType());
            answerDTO.setCorrectAnswerIds(question.getAnswer());
            // 使用字典表获取学科名称（字典类型：subject）
            if (question.getSubjectId() != null) {
                String subjectName = dictDataService.selectDictLabel("subject", String.valueOf(question.getSubjectId()));
                answerDTO.setSubjectName(subjectName != null ? subjectName : "");
            } else {
                answerDTO.setSubjectName("");
            }

            
            List<QuestionAnswer> answers = questionAnswerMap.getOrDefault(question.getId(), Collections.emptyList());
            if (!CollectionUtils.isEmpty(answers) && question.getAnswer() != null) {
                // 设置空白区域的正确答案ID
                List<Integer> correctAnswerIds = answers.stream()
                        .filter(answer -> YesOrNoEnum.YES.getCode().equals(answer.getIsAnswer()))
                        .map(QuestionAnswer::getId)
                        .toList();
                answerDTO.setCorrectAnswerIds(correctAnswerIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")));

                List<String> correctContents = new ArrayList<>();
                for (QuestionAnswer answer : answers) {
                    if (answer.getIsAnswer().equals(YesOrNoEnum.YES.getCode())) {
                        List<QuestionMediaDTO> mediaList = queryAnswerMediaResources(answer.getId());
                        processDisplayOption(correctContents,question.getType(),question.getOptionType(),answer.getOptionContent(),answer.getOptionName(),mediaList);
                    }
                }
                answerDTO.setCorrectAnswer(String.join(",", correctContents));
            } else {
                answerDTO.setCorrectAnswer("");
            }
            
            resultList.add(answerDTO);
        }
        
        return resultList;
    }

    /**
     * 处理填空题的正确答案
     *
     * @param questions 填空题列表
     * @return 题目正确答案DTO列表
     */
    private List<QuestionCorrectAnswerDTO> processFillBlankQuestions(List<Question> questions) {
        if (CollectionUtils.isEmpty(questions)) {
            return Collections.emptyList();
        }

        Set<Integer> questionIds = questions.stream().map(Question::getId).collect(Collectors.toSet());

        // 获取所有填空题的空白区域
        Map<Integer, List<QuestionBlankArea>> blankAreasMap = getBlankAreasByQuestionIds(questionIds);

        Map<Integer, Map<Integer, List<QuestionAnswer>>> blankAreaAnswerMap = questionBiz.getBlankAreaAnswerMap(questionIds);
        // 构建结果
        List<QuestionCorrectAnswerDTO> resultList = new ArrayList<>(questions.size());
        for (Question question : questions) {
            QuestionCorrectAnswerDTO answerDTO = new QuestionCorrectAnswerDTO();
            answerDTO.setQuestionId(question.getId());
            answerDTO.setTitle(question.getTitle());
            answerDTO.setType(question.getType());

            List<QuestionBlankArea> blankAreas = blankAreasMap.getOrDefault(question.getId(), Collections.emptyList());
            if (!CollectionUtils.isEmpty(blankAreas)) {
                List<BlankCorrectAnswerDTO> blankResults = new ArrayList<>(blankAreas.size());

                // 获取当前题目的所有空白区域答案映射
                Map<Integer, List<QuestionAnswer>> questionBlankAreaMap = blankAreaAnswerMap.getOrDefault(question.getId(), Collections.emptyMap());
                for (QuestionBlankArea area : blankAreas) {
                    BlankCorrectAnswerDTO blankDto = new BlankCorrectAnswerDTO();
                    blankDto.setQuestionId(area.getQuestionId());
                    blankDto.setBlankAreaId(area.getId());
                    blankDto.setBlankIndex(area.getBlankIndex());
                    // 获取空位ID
                    Integer blankAreaId = area.getId();
                    // 获取空位的答案列表，确保是从正确的题目中获取
                    List<QuestionAnswer> answers = questionBlankAreaMap.getOrDefault(blankAreaId, Collections.emptyList());
                    if (!CollectionUtils.isEmpty(answers)) {

                        // 设置空白区域的正确答案ID
                        List<Integer> correctAnswerIds = answers.stream()
                                .filter(answer -> YesOrNoEnum.YES.getCode().equals(answer.getIsAnswer()))
                                .map(QuestionAnswer::getId)
                                .toList();
                        blankDto.setCorrectAnswerIds(correctAnswerIds.stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining(",")));

                        List<String> correctContents = new ArrayList<>();
                        for (QuestionAnswer answer : answers) {
                            if(answer.getIsAnswer().equals(YesOrNoEnum.YES.getCode())){
                                List<QuestionMediaDTO> mediaList = queryAnswerMediaResources(answer.getId());
                                processDisplayOption(correctContents,question.getType(),question.getOptionType(),answer.getOptionContent(),answer.getOptionName(),mediaList);
                            }

                        }

                        blankDto.setCorrectAnswer(String.join(",", correctContents));

                    } else {
                        blankDto.setCorrectAnswer("");
                    }

                    blankResults.add(blankDto);
                }

                // 按blankIndex排序确保空白区域顺序正确
                blankResults.sort(Comparator.comparing(BlankCorrectAnswerDTO::getBlankIndex,
                        Comparator.nullsLast(Comparator.naturalOrder())));

                answerDTO.setBlankResults(blankResults);
            }

            resultList.add(answerDTO);
        }

        return resultList;
    }
    /**
     * 根据题目ID获取空白区域映射
     *
     * @param questionIds 题目ID集合
     * @return 题目ID到空白区域列表的映射
     */
    private Map<Integer, List<QuestionBlankArea>> getBlankAreasByQuestionIds(Set<Integer> questionIds) {
        if (CollectionUtils.isEmpty(questionIds)) {
            return Collections.emptyMap();
        }
        
        try {
            LambdaQueryWrapper<QuestionBlankArea> blankAreaWrapper = new LambdaQueryWrapper<>();
            blankAreaWrapper.in(QuestionBlankArea::getQuestionId, questionIds);
            List<QuestionBlankArea> blankAreas = questionBlankAreaBiz.list(blankAreaWrapper);
            
            if (CollectionUtils.isEmpty(blankAreas)) {
                return Collections.emptyMap();
            }
            
            return blankAreas.stream()
                    .collect(Collectors.groupingBy(QuestionBlankArea::getQuestionId));
        } catch (Exception e) {
            log.error("查询空白区域信息异常: questionIds={}", questionIds, e);
            return Collections.emptyMap();
        }
    }



    @Override
    public List<WrongQuestionResultDTO> listWrongQuestions(QueryWrongQuestionsBO queryBO) throws ServiceException {
        if (queryBO == null || queryBO.getAppUserId() == null) {
            log.error("查询错题列表参数错误：用户ID为空");
            throw new ServiceException(AppErrorCode.PARAMS_NOT_NULL_MSG);
        }
        
        Integer appUserId = queryBO.getAppUserId();
        Integer subjectId = queryBO.getSubjectId();
        Integer sourceType = queryBO.getSourceType();
        Integer sourceId = queryBO.getSourceId();
        Integer type = queryBO.getType();
        Set<Integer> questionIds = queryBO.getQuestionIds();
        boolean includeAnswers = queryBO.getIncludeAnswers() != null && queryBO.getIncludeAnswers();
        
        log.info("开始查询用户错题列表: appUserId={}, subjectId={}, sourceType={}, sourceId={}, type={}, includeAnswers={}",
                appUserId, subjectId, sourceType, sourceId, type, includeAnswers);
                
        try {
            // 1. 查询错题列表
            List<AppUserWrongQuestionNormal> normalWrongQuestions = appUserWrongQuestionNormalBiz.queryNormalWrongQuestionsWithJoin(appUserId, subjectId, questionIds, type);
            List<AppUserWrongQuestionBlank> blankWrongQuestions = appUserWrongQuestionBlankBiz.queryBlankWrongQuestionsWithJoin(appUserId, subjectId, questionIds, type);
            
            // 2. 收集所有题目ID，用于后续查询题目详细信息
            Set<Integer> allQuestionIds = new LinkedHashSet<>();
            if (!normalWrongQuestions.isEmpty()) {
                allQuestionIds.addAll(normalWrongQuestions.stream()
                        .map(AppUserWrongQuestionNormal::getQuestionId)
                        .collect(Collectors.toSet()));
            }
            
            if (!blankWrongQuestions.isEmpty()) {
                allQuestionIds.addAll(blankWrongQuestions.stream()
                        .map(AppUserWrongQuestionBlank::getQuestionId)
                        .collect(Collectors.toSet()));
            }
            
            if (allQuestionIds.isEmpty()) {
                log.info("未找到用户错题: appUserId={}", appUserId);
                return Collections.emptyList();
            }
            

            // 3. 查询题目详细信息
            Map<Integer, Question> questionMap = getQuestionMapByIds(allQuestionIds);
            if (questionMap.isEmpty()) {
                log.warn("未找到任何题目信息: questionIds={}", allQuestionIds);
                return Collections.emptyList();
            }
            
            // 4. 查询填空题空白区域信息（如果有填空题）
            Map<Integer, List<QuestionBlankArea>> blankAreasMap = Collections.emptyMap();
            Set<Integer> blankQuestionIds = blankWrongQuestions.stream()
                    .map(AppUserWrongQuestionBlank::getQuestionId)
                    .collect(Collectors.toSet());

            if (!blankQuestionIds.isEmpty()) {
                blankAreasMap = getBlankAreasByQuestionIds(blankQuestionIds);
            }

            log.info("查询到用户错题数量: 普通题={}, 填空题={}, 总题目数={}",
                    normalWrongQuestions.size(), blankQuestionIds.size(), allQuestionIds.size());


            // 5. 准备填空题空位ID到错题的映射（用于合并同一题目的多个空位）
            // 按 stateId + questionId 分组
            Map<String, Map<Integer, List<AppUserWrongQuestionBlank>>> stateQuestionMap = blankWrongQuestions.stream()
                    .collect(Collectors.groupingBy(AppUserWrongQuestionBlank::getStateId,
                            Collectors.groupingBy(AppUserWrongQuestionBlank::getQuestionId)));
            
            // 6. 准备结果列表
            List<WrongQuestionResultDTO> resultList = new ArrayList<>();
            
            // 7. 处理普通题错题
            for (AppUserWrongQuestionNormal normalWrong : normalWrongQuestions) {
                Question question = questionMap.get(normalWrong.getQuestionId());
                // 使用字典表获取学科名称（字典类型：subject）
                if (question != null && question.getSubjectId() != null) {
                    String subjectName = dictDataService.selectDictLabel("subject", String.valueOf(question.getSubjectId()));
                    normalWrong.setSubjectName(subjectName != null ? subjectName : "");
                } else {
                    normalWrong.setSubjectName("");
                }
                WrongQuestionResultDTO dto = processNormalWrongQuestion(normalWrong, question, includeAnswers);
                if (dto != null) {
                    resultList.add(dto);
                }
            }
            // 8. 处理填空题错题
            for (Map<Integer, List<AppUserWrongQuestionBlank>> questionMapByState : stateQuestionMap.values()) {
                for (Map.Entry<Integer, List<AppUserWrongQuestionBlank>> entry : questionMapByState.entrySet()) {
                    Integer questionId = entry.getKey();
                    List<AppUserWrongQuestionBlank> blanks = entry.getValue();
                    Question question = questionMap.get(questionId);

                    WrongQuestionResultDTO dto = processBlankWrongQuestion(blanks, questionId, question, blankAreasMap, includeAnswers);
                    if (dto != null) {
                        resultList.add(dto);
                    }
                }
            }

            // 9. 对错题结果进行排序
            sortWrongQuestionResults(resultList);
            
            // 10. 格式化错题结果的日期
            formatResultDates(resultList);
            
            // 11. 如果需要包含答案信息，设置正确答案
            if (includeAnswers && !resultList.isEmpty()) {
                setCorrectAnswers(appUserId, resultList, includeAnswers);
            }
            
            log.info("查询用户错题列表完成: appUserId={}, 返回结果数={}", appUserId, JSON.toJSONString(resultList));
            return resultList;
        } catch (Exception e) {
            log.error("查询错题列表异常: appUserId={}", appUserId, e);
            throw new ServiceException("查询错题列表失败: " + e.getMessage());
        }
    }



    private void validateParam(Object param) throws ServiceException {
        if (param == null) {
            log.error("参数为空");
            throw new ServiceException(AppErrorCode.PARAMS_NOT_NULL_MSG);
        }
    }
    
    private void validateParam(List<?> params) throws ServiceException {
        if (CollectionUtils.isEmpty(params)) {
            log.error("参数列表为空");
            throw new ServiceException(AppErrorCode.PARAMS_NOT_NULL_MSG);
        }
        
        for (Object param : params) {
            validateParam(param);
        }
    }

    /**
     * 记录处理结果
     *
     * @param userCount 用户数量
     * @param questionCount 题目数量
     * @param blankAreaCount 空白区域数量
     */
    private void logProcessResult(int userCount, int questionCount, int blankAreaCount) {
        log.info("批量添加错题完成：处理用户数={}, 处理题目数={}, 处理空白区域数={}", 
                userCount, questionCount, blankAreaCount);
    }

    /**
     * 根据题目ID集合获取题目信息映射
     *
     * @param questionIds 题目ID集合
     * @return 题目ID到题目对象的映射
     */
    private Map<Integer, Question> getQuestionMapByIds(Set<Integer> questionIds) {
        if (CollectionUtils.isEmpty(questionIds)) {
            return Collections.emptyMap();
        }
        
        try {
            LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(Question::getId, questionIds);
            List<Question> questions = questionBiz.list(queryWrapper);
            
            if (CollectionUtils.isEmpty(questions)) {
                return Collections.emptyMap();
            }
            
            return questions.stream()
                    .collect(Collectors.toMap(Question::getId, Function.identity()));
        } catch (Exception e) {
            log.error("查询题目信息异常: questionIds={}", questionIds, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 处理普通题错题，构建结果DTO
     *
     * @param normalWrong 普通题错题
     * @param question 题目信息
     * @param includeAnswers 是否包含答案
     * @return 错题结果DTO
     */
    private WrongQuestionResultDTO processNormalWrongQuestion(AppUserWrongQuestionNormal normalWrong, Question question, boolean includeAnswers) {
        if (question == null) {
            log.warn("题目不存在，跳过: questionId={}", normalWrong.getQuestionId());
            return null;
        }
        
        WrongQuestionResultDTO dto = new WrongQuestionResultDTO();
        BeanUtil.copyProperties(question,dto);
        dto.setId(normalWrong.getId());
        dto.setQuestionId(normalWrong.getQuestionId());
        dto.setSubjectId(normalWrong.getSubjectId());
        dto.setSubjectName(normalWrong.getSubjectName());
        dto.setSourceType(normalWrong.getSourceType());
        dto.setSourceId(normalWrong.getSourceId());
        dto.setCreateTime(normalWrong.getCreateTime());
        
        // 设置用户答案IDs（用于标识选择了哪些选项）
        dto.setUserAnswerIds(normalWrong.getAnswerIds());

        // 处理用户答案，根据选项类型和题目类型
        if (normalWrong.getAnswerIds() != null && !normalWrong.getAnswerIds().isEmpty()) {
            // 如果存在用户答案ID，尝试获取对应的选项内容
            List<String> userAnswerIds = Arrays.stream(normalWrong.getAnswerIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            List<QuestionAnswer> answers = questionAnswerBiz.getAnswersByQuestionId(question.getId());
            Map<String, Integer> orderMap = new HashMap<>();
            for (int i = 0; i < userAnswerIds.size(); i++) {
                orderMap.put(userAnswerIds.get(i), i);
            }

            answers.sort((a1, a2) -> {
                Integer idx1 = orderMap.getOrDefault(a1.getId().toString(), Integer.MAX_VALUE);
                Integer idx2 = orderMap.getOrDefault(a2.getId().toString(), Integer.MAX_VALUE);
                return idx1.compareTo(idx2);
            });
            if (!CollectionUtils.isEmpty(answers)) {
                List<String> userAnswerContents = new ArrayList<>();
                
                for (QuestionAnswer answer : answers) {
                    if (userAnswerIds.contains(String.valueOf(answer.getId()))) {
                        List<QuestionMediaDTO> mediaList = queryAnswerMediaResources(answer.getId());
                        processDisplayOption(userAnswerContents,question.getType(),question.getOptionType(),answer.getOptionContent(),answer.getOptionName(),mediaList);
                    }
                }
                
                // 如果通过答案ID找到了内容，则使用内容；否则使用原始用户答案
                if (!userAnswerContents.isEmpty()) {
                    dto.setUserAnswer(String.join(",", userAnswerContents));
                } else {
                    dto.setUserAnswer(normalWrong.getUserAnswer());
                }
            } else {
                // 如果没有找到答案选项，使用原始用户答案
                dto.setUserAnswer(normalWrong.getUserAnswer());
            }
        } else {
            // 如果没有答案ID，直接使用原始用户答案
            dto.setUserAnswer(normalWrong.getUserAnswer());
        }
        
        // 查询并设置题目的媒体资源
        dto.setMediaUrl(queryQuestionMediaResources(question.getId()));
        
        // 查询并设置辅助识图资源
        dto.setAidedRecognitionUrl(queryQuestionRecognitionResources(question.getId()));
        
        // 查询选项列表（非完形填空题）
        if (!Objects.equals(question.getType(), QuestionTypeEnum.CLOZE.getValue())) {
            dto.setOptions(queryQuestionOptions(question));
        }
        
        return dto;
    }

    /**
     * 查询题目的媒体资源
     *
     * @param questionId 题目ID
     * @return 媒体资源列表
     */
    private List<QuestionMediaDTO> queryQuestionMediaResources(Integer questionId) {
        // 获取题目的媒体文件（使用新的QuestionMedia表）
        List<QuestionMedia> questionMediaList = questionMediaBiz.listByQuestionIdAndType(questionId, 1);
        
        if (CollectionUtils.isEmpty(questionMediaList)) {
            return Collections.emptyList();
        }
        
        return questionMediaList.stream()
                .map(media -> {
                    QuestionMediaDTO mediaDTO = new QuestionMediaDTO();
                    BeanUtil.copyProperties(media, mediaDTO);
                    // 兼容字段
                    mediaDTO.setMaterialId(media.getId());
                    mediaDTO.setSortNum(0); // 新表结构中没有排序号，使用默认值
                    return mediaDTO;
                })
                .collect(Collectors.toList());
    }

    /**
     * 查询题目的辅助识图资源
     *
     * @param questionId 题目ID
     * @return 辅助识图资源列表
     */
    private List<QuestionRecognitionDTO> queryQuestionRecognitionResources(Integer questionId) {
        // 从数据库获取辅助识图（使用新的QuestionMedia表，mediaType=3）
        List<QuestionMedia> recognitionMediaList = questionMediaBiz.listByQuestionIdAndType(questionId, 3);
        if (CollectionUtils.isEmpty(recognitionMediaList)) {
            return Collections.emptyList();
        }
        
        return recognitionMediaList.stream()
                .map(media -> {
                    QuestionRecognitionDTO recognition = new QuestionRecognitionDTO();
                    recognition.setMaterialId(media.getId());
                    recognition.setMaterialPath(media.getMediaPath());
                    recognition.setMaterialName(media.getMediaName());
                    recognition.setSortNum(0); // 新表结构中没有排序号，使用默认值
                    return recognition;
                })
                .collect(Collectors.toList());
    }

    /**
     * 查询题目的选项列表
     *
     * @param question 题目
     * @return 选项列表
     */
    private List<WrongQuestionOptionDTO> queryQuestionOptions(Question question) {
        List<QuestionAnswer> answers = questionAnswerBiz.getAnswersByQuestionId(question.getId());
        if (CollectionUtils.isEmpty(answers)) {
            return Collections.emptyList();
        }
        
        List<WrongQuestionOptionDTO> options = answers.stream()
                .map(answer -> {
                    WrongQuestionOptionDTO optionDTO = new WrongQuestionOptionDTO();
                    optionDTO.setId(answer.getId());
                    optionDTO.setQuestionId(answer.getQuestionId());
                    optionDTO.setSerialNo(answer.getSerialNo());
                    optionDTO.setOptionName(answer.getOptionName());
                    optionDTO.setOptionContent(answer.getOptionContent());
                    optionDTO.setIsAnswer(answer.getIsAnswer());
                    
                    // 查询选项的媒体资源
                    optionDTO.setMediaUrl(queryAnswerMediaResources(answer.getId()));
                    
                    return optionDTO;
                })
                .collect(Collectors.toList());
        
        // 为排序题打乱选项顺序
        if (QuestionTypeEnum.SORT.getValue().equals(question.getType())) {
            Collections.shuffle(options);
        }
        
        return options;
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

    /**
     * 处理填空题错题，构建结果DTO
     *
     * @param blanks 填空题错题列表（同一题目的不同空位）
     * @param questionId 题目ID
     * @param question 题目信息
     * @param blankAreasMap 空白区域映射
     * @param includeAnswers 是否包含答案
     * @return 错题结果DTO
     */
    private WrongQuestionResultDTO processBlankWrongQuestion(List<AppUserWrongQuestionBlank> blanks, Integer questionId, 
                                                          Question question, Map<Integer, List<QuestionBlankArea>> blankAreasMap, 
                                                          boolean includeAnswers) {
        if (question == null || blanks.isEmpty()) {
            log.warn("题目不存在或空位列表为空，跳过: questionId={}", questionId);
            return null;
        }
        // 使用第一个空位的信息作为基础信息
        AppUserWrongQuestionBlank firstBlank = blanks.get(0);
        // 使用字典表获取学科名称（字典类型：subject）
        if (question.getSubjectId() != null) {
            String subjectName = dictDataService.selectDictLabel("subject", String.valueOf(question.getSubjectId()));
            firstBlank.setSubjectName(subjectName != null ? subjectName : "");
        } else {
            firstBlank.setSubjectName("");
        }

        WrongQuestionResultDTO dto = new WrongQuestionResultDTO();
        BeanUtil.copyProperties(question,dto);
        dto.setId(firstBlank.getId());
        dto.setQuestionId(questionId);
        dto.setSubjectId(firstBlank.getSubjectId());
        dto.setSubjectName(firstBlank.getSubjectName());
        dto.setSourceType(firstBlank.getSourceType());
        dto.setSourceId(firstBlank.getSourceId());
        dto.setCreateTime(firstBlank.getCreateTime());
        
        // 查询并设置题目的媒体资源
        dto.setMediaUrl(queryQuestionMediaResources(question.getId()));
        
        // 查询并设置辅助识图资源
        dto.setAidedRecognitionUrl(queryQuestionRecognitionResources(question.getId()));
        
        // 处理空位信息
        List<WrongQuestionBlankDTO> blankResults = new ArrayList<>();
        List<QuestionBlankArea> blankAreas = blankAreasMap.getOrDefault(questionId, Collections.emptyList());
        
        Map<Integer, QuestionBlankArea> blankAreaMap = blankAreas.stream()
                .collect(Collectors.toMap(QuestionBlankArea::getId, Function.identity(), (k1, k2) -> k1));
        
        for (AppUserWrongQuestionBlank blank : blanks) {
            WrongQuestionBlankDTO blankDto = new WrongQuestionBlankDTO();
            blankDto.setBlankAreaId(blank.getBlankAreaId());
            blankDto.setQuestionId(questionId);
            blankDto.setBlankIndex(blank.getBlankIndex());
            
            // 设置用户答案IDs
            blankDto.setUserAnswerIds(blank.getAnswerIds());
            
            // 处理用户答案，根据选项类型
            if (blank.getAnswerIds() != null && !blank.getAnswerIds().isEmpty()) {
                // 如果存在用户答案ID，尝试获取对应的选项内容
                Set<String> userAnswerIds = Arrays.stream(blank.getAnswerIds().split(","))
                        .map(String::trim)
                        .collect(Collectors.toSet());
                
                List<QuestionAnswer> answers = questionAnswerBiz.getAnswersByBlankAreaId(questionId, blank.getBlankAreaId());
                if (!CollectionUtils.isEmpty(answers)) {
                    List<String> userAnswerContents = new ArrayList<>();

                    for (QuestionAnswer answer : answers) {
                        if (userAnswerIds.contains(String.valueOf(answer.getId()))) {
                            List<QuestionMediaDTO> mediaList = queryAnswerMediaResources(answer.getId());
                            processDisplayOption(userAnswerContents,question.getType(),question.getOptionType(),answer.getOptionContent(),answer.getOptionName(),mediaList);
                        }
                    }
                    
                    // 如果通过答案ID找到了内容，则使用内容；否则使用原始用户答案
                    if (!userAnswerContents.isEmpty()) {
                        blankDto.setUserAnswer(String.join(",", userAnswerContents));
                    } else {
                        blankDto.setUserAnswer(blank.getUserAnswer());
                    }
                } else {
                    // 如果没有找到答案选项，使用原始用户答案
                    blankDto.setUserAnswer(blank.getUserAnswer());
                }
            } else {
                // 如果没有答案ID，直接使用原始用户答案
                blankDto.setUserAnswer(blank.getUserAnswer());
            }
            
            blankResults.add(blankDto);
        }
        
        // 设置空位结果列表
        dto.setBlankResults(blankResults);
        
        return dto;
    }


    /**
     * 对错题结果进行排序
     * 按学科ID分类，同一学科内按创建时间倒序
     *
     * @param resultList 错题结果列表
     */
    private void sortWrongQuestionResults(List<WrongQuestionResultDTO> resultList) {
        if (CollectionUtils.isEmpty(resultList)) {
            return;
        }
        
        resultList.sort((a, b) -> {
            // 首先按学科ID排序
            int subjectCompare = 0;
            if (a.getSubjectId() != null && b.getSubjectId() != null) {
                subjectCompare = a.getSubjectId().compareTo(b.getSubjectId());
            } else if (a.getSubjectId() != null) {
                subjectCompare = -1;
            } else if (b.getSubjectId() != null) {
                subjectCompare = 1;
            }
            
            if (subjectCompare != 0) {
                return subjectCompare;
            }
            
            // 同一学科内，按创建时间倒序排序
            if (a.getCreateTime() != null && b.getCreateTime() != null) {
                return b.getCreateTime().compareTo(a.getCreateTime());
            } else if (a.getCreateTime() != null) {
                return -1;
            } else if (b.getCreateTime() != null) {
                return 1;
            }
            
            return 0;
        });
    }

    /**
     * 格式化错题结果的日期
     * 当年显示MM-DD，非当年显示YYYY-MM-DD
     *
     * @param resultList 错题结果列表
     */
    private void formatResultDates(List<WrongQuestionResultDTO> resultList) {
        if (CollectionUtils.isEmpty(resultList)) {
            return;
        }
        
        int currentYear = LocalDateTime.now().getYear();
        
        for (WrongQuestionResultDTO dto : resultList) {
            if (dto.getCreateTime() != null) {
                //todo
                LocalDateTime createTime = null;//dto.getCreateTime();
                int createYear = createTime.getYear();
                
                String formattedDate;
                if (createYear == currentYear) {
                    // 当年，显示MM-DD
                    formattedDate = String.format("%02d-%02d", createTime.getMonthValue(), createTime.getDayOfMonth());
                } else {
                    // 非当年，显示YYYY-MM-DD
                    formattedDate = String.format("%04d-%02d-%02d", createYear, createTime.getMonthValue(), createTime.getDayOfMonth());
                }
                
                // 设置格式化后的日期
                dto.setFormattedDate(formattedDate);
            }
        }
    }

    /**
     * 设置错题结果的正确答案
     *
     * @param appUserId 用户ID
     * @param resultList 错题结果列表
     * @param includeAnswers 是否包含答案
     */
    private void setCorrectAnswers(Integer appUserId, List<WrongQuestionResultDTO> resultList, boolean includeAnswers) throws ServiceException {
        if (CollectionUtils.isEmpty(resultList) || !includeAnswers) {
            return;
        }
        
        // 收集所有题目ID
        Set<Integer> resultQuestionIds = resultList.stream()
                .map(WrongQuestionResultDTO::getQuestionId)
                .collect(Collectors.toSet());
        
        // 使用已有的getWrongQuestionsCorrectAnswers方法获取正确答案
        List<QuestionCorrectAnswerDTO> correctAnswers = getWrongQuestionsCorrectAnswers(appUserId, resultQuestionIds);
        
        if (correctAnswers.isEmpty()) {
            return;
        }
        
        // 创建题目ID到正确答案的映射
        Map<Integer, QuestionCorrectAnswerDTO> correctAnswerMap = correctAnswers.stream()
                .collect(Collectors.toMap(QuestionCorrectAnswerDTO::getQuestionId, Function.identity(), (k1, k2) -> k1));

        // 设置正确答案信息
        for (WrongQuestionResultDTO dto : resultList) {
            QuestionCorrectAnswerDTO correctAnswer = correctAnswerMap.get(dto.getQuestionId());
            if (correctAnswer != null) {
                dto.setCorrectAnswer(correctAnswer.getCorrectAnswer());
                dto.setCorrectAnswerIds(correctAnswer.getCorrectAnswerIds());
                
                // 对于填空题，设置空位的正确答案
                if (dto.getType() != null && dto.getType().equals(QuestionTypeEnum.CLOZE.getValue())
                        && !CollectionUtils.isEmpty(dto.getBlankResults()) 
                        && !CollectionUtils.isEmpty(correctAnswer.getBlankResults())) {
                    for (WrongQuestionBlankDTO blankResult : dto.getBlankResults()) {
                        for (BlankCorrectAnswerDTO correctAnswerDTO : correctAnswer.getBlankResults()) {
                            if (Objects.equals(blankResult.getQuestionId(), correctAnswerDTO.getQuestionId()) &&
                                    Objects.equals(blankResult.getBlankAreaId(), correctAnswerDTO.getBlankAreaId()) &&
                                    Objects.equals(blankResult.getBlankIndex(), correctAnswerDTO.getBlankIndex())) {
                                blankResult.setCorrectAnswer(correctAnswerDTO.getCorrectAnswer());
                                blankResult.setCorrectAnswerIds(correctAnswerDTO.getCorrectAnswerIds());
                            }
                        }
                    }
                    aggregateClozeAnswers(dto);
                }
            }
        }
    }



    private void processDisplayOption(List<String> displayContents ,Integer questionType, Integer optionType,String optionContent,String optionName,List<QuestionMediaDTO> mediaList){
        if(QuestionTypeEnum.displayOptionContent(questionType)){
            if(optionType.equals(OptionTypeEnum.TEXT.getCode())){
                displayContents.add(optionContent);
            }else if (Objects.requireNonNull(OptionTypeEnum.getByCode(optionType)).isMedia()){
                // 处理图片类型选项
                if (!CollectionUtils.isEmpty(mediaList)) {
                    mediaList.forEach(media -> displayContents.add(media.getMediaPath()));
                }
            }
        }else if(QuestionTypeEnum.displayOptionName(questionType)){
            displayContents.add(optionName);
        }
    }

    /**
     * 聚合空位答案，为主题目汇总所有空位区域的用户答案信息
     */
    protected void aggregateClozeAnswers(WrongQuestionResultDTO result) {
        List<WrongQuestionBlankDTO> blankResults = result.getBlankResults();
        if (CollectionUtils.isEmpty(blankResults)) {
            return;
        }
        blankResults.sort(Comparator.comparing(WrongQuestionBlankDTO::getBlankIndex, Comparator.nullsLast(Comparator.naturalOrder())));
        List<String> allUserAnswers = new ArrayList<>();
        List<String> allUserAnswerIds = new ArrayList<>();
        List<String> allCorrectAnswers = new ArrayList<>();
        List<String> allCorrectAnswerIds = new ArrayList<>();
        for (WrongQuestionBlankDTO blank : blankResults) {
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
}