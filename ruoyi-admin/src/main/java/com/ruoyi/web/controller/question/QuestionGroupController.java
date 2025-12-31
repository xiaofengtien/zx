package com.ruoyi.web.controller.question;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.student.archive.biz.question.IQuestionGroupBiz;
import com.ruoyi.student.archive.domain.question.QuestionGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 题目组管理Controller
 * 
 * @author ruoyi
 */
@Slf4j
@RestController
@RequestMapping("/question/group")
@RequiredArgsConstructor
public class QuestionGroupController extends BaseController {

    private final IQuestionGroupBiz questionGroupBiz;

    /**
     * 根据分类ID查询题目组列表
     */
    @PreAuthorize("@ss.hasPermi('question:question:list')")
    @GetMapping("/listByCategory")
    public AjaxResult listByCategory(@RequestParam("categoryId") Integer categoryId) {
        if (categoryId == null) {
            return AjaxResult.error("分类ID不能为空");
        }
        List<QuestionGroup> list = questionGroupBiz.listByCategoryId(categoryId);
        return AjaxResult.success(list);
    }

    /**
     * 获取题目组详情（包含题目列表）
     */
    @PreAuthorize("@ss.hasPermi('question:question:query')")
    @GetMapping("/detail")
    public AjaxResult getDetail(@RequestParam("id") Integer groupId) {
        if (groupId == null) {
            return AjaxResult.error("题目组ID不能为空");
        }
        QuestionGroup group = questionGroupBiz.getGroupWithQuestions(groupId);
        if (group == null) {
            return AjaxResult.error("题目组不存在");
        }
        return AjaxResult.success(group);
    }

    /**
     * 创建题目组
     */
    @PreAuthorize("@ss.hasPermi('question:question:add')")
    @Log(title = "题目组管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Map<String, Object> params) {
        try {
            QuestionGroup group = new QuestionGroup();
            group.setGroupName((String) params.get("groupName"));
            group.setCategoryId((Integer) params.get("categoryId"));
            group.setAudioUrl((String) params.get("audioUrl"));
            group.setAudioPath((String) params.get("audioPath"));
            group.setAudioDuration((Integer) params.get("audioDuration"));
            group.setAudioLabel((String) params.get("audioLabel"));
            group.setDescription((String) params.get("description"));

            @SuppressWarnings("unchecked")
            List<Integer> questionIds = (List<Integer>) params.get("questionIds");

            Integer groupId = questionGroupBiz.createGroup(group, questionIds);
            return AjaxResult.success("创建成功", groupId);
        } catch (Exception e) {
            log.error("创建题目组失败", e);
            return AjaxResult.error("创建失败：" + e.getMessage());
        }
    }

    /**
     * 更新题目组
     */
    @PreAuthorize("@ss.hasPermi('question:question:edit')")
    @Log(title = "题目组管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Map<String, Object> params) {
        try {
            Integer groupId = (Integer) params.get("id");
            if (groupId == null) {
                return AjaxResult.error("题目组ID不能为空");
            }

            QuestionGroup group = questionGroupBiz.getById(groupId);
            if (group == null) {
                return AjaxResult.error("题目组不存在");
            }

            if (params.get("groupName") != null) {
                group.setGroupName((String) params.get("groupName"));
            }
            if (params.get("audioUrl") != null) {
                group.setAudioUrl((String) params.get("audioUrl"));
            }
            if (params.get("audioPath") != null) {
                group.setAudioPath((String) params.get("audioPath"));
            }
            if (params.get("audioDuration") != null) {
                group.setAudioDuration((Integer) params.get("audioDuration"));
            }
            if (params.get("audioLabel") != null) {
                group.setAudioLabel((String) params.get("audioLabel"));
            }
            if (params.get("description") != null) {
                group.setDescription((String) params.get("description"));
            }

            @SuppressWarnings("unchecked")
            List<Integer> questionIds = (List<Integer>) params.get("questionIds");

            questionGroupBiz.updateGroup(group, questionIds);
            return AjaxResult.success("更新成功");
        } catch (Exception e) {
            log.error("更新题目组失败", e);
            return AjaxResult.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除题目组
     */
    @PreAuthorize("@ss.hasPermi('question:question:remove')")
    @Log(title = "题目组管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public AjaxResult remove(@RequestBody Map<String, Object> params) {
        try {
            Integer groupId = (Integer) params.get("id");
            if (groupId == null) {
                return AjaxResult.error("题目组ID不能为空");
            }

            // 软删除
            QuestionGroup group = questionGroupBiz.getById(groupId);
            if (group != null) {
                group.setDelFlag("1");
                questionGroupBiz.updateById(group);
            }

            return AjaxResult.success("删除成功");
        } catch (Exception e) {
            log.error("删除题目组失败", e);
            return AjaxResult.error("删除失败：" + e.getMessage());
        }
    }
}
