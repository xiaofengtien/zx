package com.zx.web.controller.paper;

import com.zx.common.annotation.Log;
import com.zx.common.core.controller.BaseController;
import com.zx.common.core.domain.AjaxResult;
import com.zx.common.enums.BusinessType;
import com.zx.student.archive.domain.bo.paper.PaperQuestionGroupBO;
import com.zx.student.archive.domain.paper.PaperQuestionGroup;
import com.zx.student.archive.service.paper.IPaperQuestionGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 试卷题目组管理Controller
 * 
 * @author zx
 */
@Slf4j
@RestController
@RequestMapping("/paper/questionGroup")
@RequiredArgsConstructor
public class PaperQuestionGroupController extends BaseController {

    private final IPaperQuestionGroupService paperQuestionGroupService;
    private final com.zx.student.archive.biz.question.IQuestionGroupBiz questionGroupBiz;

    /**
     * 根据大题ID查询题目组列表
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:query')")
    @GetMapping("/list/{sectionId}")
    public AjaxResult listBySectionId(@PathVariable Integer sectionId) {
        List<PaperQuestionGroup> list = paperQuestionGroupService.listBySectionId(sectionId);
        return success(list);
    }

    /**
     * 根据试卷ID查询所有题目组
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:query')")
    @GetMapping("/listByPaper/{paperId}")
    public AjaxResult listByPaperId(@PathVariable Integer paperId) {
        List<PaperQuestionGroup> list = paperQuestionGroupService.listByPaperId(paperId);
        // 将 selectedQuestionIds JSON 字符串解析为数组返回给前端
        List<java.util.Map<String, Object>> result = list.stream().map(group -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", group.getId());
            map.put("sectionId", group.getSectionId());
            map.put("questionGroupId", group.getQuestionGroupId());
            map.put("groupOrder", group.getGroupOrder());
            map.put("groupName", group.getGroupName());
            map.put("answerTime", group.getAnswerTime());

            // 如果关联了题目组，优先使用题目组的信息
            if (group.getQuestionGroupId() != null) {
                try {
                    com.zx.student.archive.domain.question.QuestionGroup qGroup = questionGroupBiz
                            .getById(group.getQuestionGroupId());
                    if (qGroup != null) {
                        map.put("title", qGroup.getGroupName()); // 使用题目组名称作为标题
                        map.put("audioUrl", qGroup.getAudioUrl());
                        map.put("audioPath", qGroup.getAudioPath());
                        map.put("audioDuration", qGroup.getAudioDuration());
                        map.put("introText", qGroup.getDescription());
                        map.put("categoryId", qGroup.getCategoryId());

                        // 返回题目列表供前端展示
                        if (qGroup.getQuestions() != null) {
                            map.put("questions", qGroup.getQuestions());
                            map.put("questionCount", qGroup.getQuestions().size());
                        } else {
                            map.put("questionCount", 0);
                        }
                    }
                } catch (Exception e) {
                    log.error("获取关联题目组信息失败: {}", e.getMessage());
                }
            } else {
                map.put("title", "题目组"); // 默认名称
            }

            // 如果PaperQuestionGroup中有覆盖设置，则使用覆盖设置(保留原有逻辑)
            if (group.getAudioUrl() != null)
                map.put("audioUrl", group.getAudioUrl());
            if (group.getAudioPath() != null)
                map.put("audioPath", group.getAudioPath());
            if (group.getAudioDuration() != null)
                map.put("audioDuration", group.getAudioDuration());
            if (group.getIntroText() != null)
                map.put("introText", group.getIntroText());

            // 解析 selectedQuestionIds JSON 字符串为数组
            java.util.List<Integer> selectedQuestionIds = new java.util.ArrayList<>();
            String jsonStr = group.getSelectedQuestionIds();
            if (jsonStr != null && !jsonStr.isEmpty()) {
                try {
                    // 解析 "[1, 2, 3]" 格式的字符串
                    jsonStr = jsonStr.replace("[", "").replace("]", "").replace(" ", "");
                    if (!jsonStr.isEmpty()) {
                        for (String idStr : jsonStr.split(",")) {
                            if (!idStr.isEmpty()) {
                                selectedQuestionIds.add(Integer.parseInt(idStr.trim()));
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析题目组selectedQuestionIds失败: {}", e.getMessage());
                }
            }
            map.put("selectedQuestionIds", selectedQuestionIds);
            return map;
        }).collect(Collectors.toList());
        return success(result);
    }

    /**
     * 根据ID查询题目组
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:query')")
    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable Integer id) {
        PaperQuestionGroup group = paperQuestionGroupService.getById(id);
        return success(group);
    }

    /**
     * 新增题目组
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:add')")
    @Log(title = "试卷题目组管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@Validated @RequestBody PaperQuestionGroupBO groupBO) {
        PaperQuestionGroup group = new PaperQuestionGroup();
        group.setSectionId(groupBO.getSectionId());
        group.setQuestionGroupId(groupBO.getQuestionGroupId());
        group.setGroupOrder(groupBO.getGroupOrder());
        group.setStartQuestionNum(groupBO.getStartQuestionNum());
        group.setEndQuestionNum(groupBO.getEndQuestionNum());
        group.setAudioUrl(groupBO.getAudioUrl());
        group.setAudioPath(groupBO.getAudioPath());
        group.setAudioDuration(groupBO.getAudioDuration());
        group.setIntroText(groupBO.getIntroText());

        boolean result = paperQuestionGroupService.saveGroup(group);
        if (result) {
            return success(group.getId());
        }
        return error("新增失败");
    }

    /**
     * 修改题目组
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:edit')")
    @Log(title = "试卷题目组管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@Validated @RequestBody PaperQuestionGroupBO groupBO) {
        if (groupBO.getId() == null) {
            return error("题目组ID不能为空");
        }

        PaperQuestionGroup group = paperQuestionGroupService.getById(groupBO.getId());
        if (group == null) {
            return error("题目组不存在");
        }

        group.setSectionId(groupBO.getSectionId());
        group.setQuestionGroupId(groupBO.getQuestionGroupId());
        group.setGroupOrder(groupBO.getGroupOrder());
        group.setStartQuestionNum(groupBO.getStartQuestionNum());
        group.setEndQuestionNum(groupBO.getEndQuestionNum());
        group.setAudioUrl(groupBO.getAudioUrl());
        group.setAudioPath(groupBO.getAudioPath());
        group.setAudioDuration(groupBO.getAudioDuration());
        group.setIntroText(groupBO.getIntroText());

        boolean result = paperQuestionGroupService.saveGroup(group);
        return toAjax(result ? 1 : 0);
    }

    /**
     * 批量保存题目组
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:edit')")
    @Log(title = "试卷题目组管理", businessType = BusinessType.UPDATE)
    @PostMapping("/batchSave")
    public AjaxResult batchSave(@RequestBody List<PaperQuestionGroupBO> groupList) {
        if (groupList == null || groupList.isEmpty()) {
            return error("题目组列表不能为空");
        }

        // 转换为实体列表
        List<PaperQuestionGroup> groups = groupList.stream().map(bo -> {
            PaperQuestionGroup group = new PaperQuestionGroup();
            if (bo.getId() != null) {
                group.setId(bo.getId());
            }
            group.setSectionId(bo.getSectionId());
            group.setQuestionGroupId(bo.getQuestionGroupId());
            group.setGroupOrder(bo.getGroupOrder());
            group.setStartQuestionNum(bo.getStartQuestionNum());
            group.setEndQuestionNum(bo.getEndQuestionNum());
            group.setAudioUrl(bo.getAudioUrl());
            group.setAudioPath(bo.getAudioPath());
            group.setAudioDuration(bo.getAudioDuration());
            group.setIntroText(bo.getIntroText());
            return group;
        }).collect(Collectors.toList());

        // 获取大题ID（所有题目组应该属于同一大题）
        Integer sectionId = groupList.get(0).getSectionId();
        boolean result = paperQuestionGroupService.saveBatch(groups, sectionId);
        return toAjax(result ? 1 : 0);
    }

    /**
     * 删除题目组
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:remove')")
    @Log(title = "试卷题目组管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Integer id) {
        boolean result = paperQuestionGroupService.deleteById(id);
        return toAjax(result ? 1 : 0);
    }
}
