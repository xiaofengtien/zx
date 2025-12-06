package com.ruoyi.web.controller.paper;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.student.archive.biz.paper.IPaperQuestionBiz;
import com.ruoyi.student.archive.domain.bo.paper.PaperQuestionListBO;
import com.ruoyi.student.archive.domain.bo.paper.PaperQuestionAddBO;
import com.ruoyi.student.archive.domain.bo.paper.PaperQuestionRemoveBO;
import com.ruoyi.student.archive.domain.bo.paper.PaperQuestionBatchSaveBO;
import com.ruoyi.student.archive.domain.dto.paper.PaperQuestionDTO;
import com.ruoyi.student.archive.domain.paper.PaperQuestion;
import com.ruoyi.student.archive.service.question.QuestionService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 试卷题目关联管理Controller
 * 
 * @author ruoyi
 */
@Slf4j
@RestController
@RequestMapping("/paper/question")
public class PaperQuestionController extends BaseController
{
    @Autowired
    private IPaperQuestionBiz paperQuestionBiz;
    
    @Autowired
    private QuestionService questionService;

    /**
     * 获取试卷题目列表
     */
    @PreAuthorize("@ss.hasPermi('paper:question:list')")
    @PostMapping("/list")
    public AjaxResult list(@Validated @RequestBody PaperQuestionListBO listBO)
    {
        List<PaperQuestion> paperQuestions = paperQuestionBiz.listByPaperId(listBO.getPaperId());
        List<PaperQuestionDTO> dtoList = paperQuestions.stream()
            .map(pq -> {
                PaperQuestionDTO dto = new PaperQuestionDTO();
                BeanUtils.copyProperties(pq, dto);
                dto.setSortNum(pq.getSortOrder()); // 兼容旧字段
                dto.setSectionId(pq.getSectionId()); // 设置 sectionId
                dto.setSectionOrder(pq.getSectionOrder()); // 设置 sectionOrder
                
                // 查询题目详情，填充 title, type, subjectId 等字段
                if (pq.getQuestionId() != null) {
                    try {
                        com.ruoyi.student.archive.domain.bo.question.QuestionIdBO questionIdBO = 
                            new com.ruoyi.student.archive.domain.bo.question.QuestionIdBO();
                        questionIdBO.setId(pq.getQuestionId());
                        com.ruoyi.student.archive.domain.dto.question.QuestionInfoDTO questionDTO = 
                            questionService.getQuestion(questionIdBO);
                        if (questionDTO != null) {
                            dto.setTitle(questionDTO.getTitle());
                            dto.setType(questionDTO.getType());
                            dto.setTypeName(questionDTO.getTypeName());
                            dto.setSubjectId(questionDTO.getSubjectId());
                            dto.setSubjectName(questionDTO.getSubjectName());
                        }
                    } catch (Exception e) {
                        // 如果查询题目详情失败，只记录日志，不影响返回
                        log.warn("获取题目详情失败，questionId={}, error={}", pq.getQuestionId(), e.getMessage());
                    }
                }
                
                return dto;
            })
            .collect(Collectors.toList());
        return success(dtoList);
    }

    /**
     * 添加题目到试卷
     */
    @PreAuthorize("@ss.hasPermi('paper:question:add')")
    @Log(title = "试卷题目管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult addQuestion(@Validated @RequestBody PaperQuestionAddBO addBO)
    {
        List<PaperQuestion> paperQuestions = addBO.getQuestionIds().stream()
            .map(questionId -> {
                PaperQuestion pq = new PaperQuestion();
                pq.setPaperId(addBO.getPaperId());
                pq.setQuestionId(questionId);
                return pq;
            })
            .collect(Collectors.toList());
        
        boolean success = paperQuestionBiz.batchInsert(paperQuestions);
        return toAjax(success);
    }

    /**
     * 从试卷移除题目
     */
    @PreAuthorize("@ss.hasPermi('paper:question:remove')")
    @Log(title = "试卷题目管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public AjaxResult removeQuestion(@Validated @RequestBody PaperQuestionRemoveBO removeBO)
    {
        boolean success = paperQuestionBiz.batchDeleteByPaperIdAndQuestionIds(
            removeBO.getPaperId(), 
            removeBO.getQuestionIds()
        );
        return toAjax(success);
    }

    /**
     * 批量保存大题下的题目关联
     */
    @PreAuthorize("@ss.hasPermi('paper:question:edit')")
    @Log(title = "试卷题目管理", businessType = BusinessType.UPDATE)
    @PostMapping("/batchSave")
    public AjaxResult batchSave(@Validated @RequestBody PaperQuestionBatchSaveBO batchSaveBO)
    {
        // 先删除该大题下的所有题目关联（精确删除，不影响其他大题）
        paperQuestionBiz.deleteByPaperIdAndSectionId(batchSaveBO.getPaperId(), batchSaveBO.getSectionId());
        
        // 批量插入新的题目关联
        if (batchSaveBO.getQuestionList() != null && !batchSaveBO.getQuestionList().isEmpty()) {
            List<PaperQuestion> paperQuestions = batchSaveBO.getQuestionList().stream()
                .map(item -> {
                    PaperQuestion pq = new PaperQuestion();
                    pq.setPaperId(batchSaveBO.getPaperId());
                    pq.setQuestionId(item.getQuestionId());
                    pq.setSectionId(batchSaveBO.getSectionId());
                    pq.setSectionOrder(item.getSectionOrder() != null ? item.getSectionOrder() : 0);
                    pq.setScore(item.getScore() != null ? item.getScore() : java.math.BigDecimal.ZERO);
                    return pq;
                })
                .collect(Collectors.toList());
            
            boolean success = paperQuestionBiz.batchInsert(paperQuestions);
            return toAjax(success);
        }
        
        return success();
    }
}

