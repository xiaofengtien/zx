package com.zx.web.controller.paper;

import com.zx.common.annotation.Log;
import com.zx.common.core.controller.BaseController;
import com.zx.common.core.domain.AjaxResult;
import com.zx.common.enums.BusinessType;
import com.zx.student.archive.domain.bo.paper.PaperIdBO;
import com.zx.student.archive.domain.bo.paper.PaperSectionBO;
import com.zx.student.archive.domain.bo.paper.PaperSectionIdsBO;
import com.zx.student.archive.domain.paper.PaperSection;
import com.zx.student.archive.domain.paper.PaperVolume;
import com.zx.student.archive.service.paper.IPaperSectionService;
import com.zx.student.archive.service.paper.IPaperVolumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 试卷大题管理Controller
 * 
 * @author zx
 */
@Slf4j
@RestController
@RequestMapping("/paper/section")
@RequiredArgsConstructor
public class PaperSectionController extends BaseController {

    private final IPaperSectionService paperSectionService;
    private final IPaperVolumeService paperVolumeService;

    /**
     * 根据试卷ID查询大题列表
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:query')")
    @PostMapping("/list")
    public AjaxResult list(@Validated @RequestBody PaperIdBO idBO) {
        List<PaperSection> list = paperSectionService.listByPaperId(idBO.getId());
        return success(list);
    }

    /**
     * 新增大题
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:add')")
    @Log(title = "试卷大题管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@Validated @RequestBody PaperSectionBO sectionBO) {
        // 根据 volumeId 或 volumeCode 获取卷别ID
        Integer volumeId = sectionBO.getVolumeId();
        if (volumeId == null && sectionBO.getVolumeCode() != null) {
            // 如果只提供了 volumeCode，需要查找对应的 volumeId
            PaperVolume volume = paperVolumeService.getByPaperIdAndVolumeCode(sectionBO.getPaperId(), sectionBO.getVolumeCode());
            if (volume == null) {
                return error("卷别不存在");
            }
            volumeId = volume.getId();
        }
        if (volumeId == null) {
            return error("卷别ID不能为空");
        }
        
        PaperSection section = new PaperSection();
        section.setPaperId(sectionBO.getPaperId());
        section.setVolumeId(volumeId);
        section.setVolumeCode(sectionBO.getVolumeCode()); // 保留用于显示
        section.setSectionName(sectionBO.getSectionName());
        section.setSectionOrder(sectionBO.getSectionOrder());
        section.setQuestionCount(sectionBO.getQuestionCount() != null ? sectionBO.getQuestionCount() : 0);
        section.setTotalScore(sectionBO.getTotalScore() != null ? sectionBO.getTotalScore() : java.math.BigDecimal.ZERO);
        section.setScorePerQuestion(sectionBO.getScorePerQuestion());
        section.setInstructionText(sectionBO.getInstructionText());
        section.setAnswerTime(sectionBO.getAnswerTime() != null ? sectionBO.getAnswerTime() : 5);
        section.setAudioPlayCount(sectionBO.getAudioPlayCount() != null ? sectionBO.getAudioPlayCount() : 1);
        
        boolean result = paperSectionService.saveSection(section);
        return toAjax(result ? 1 : 0);
    }

    /**
     * 修改大题
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:edit')")
    @Log(title = "试卷大题管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@Validated @RequestBody PaperSectionBO sectionBO) {
        if (sectionBO.getId() == null) {
            return error("大题ID不能为空");
        }
        
        // 查询现有大题（通过试卷ID和大题ID）
        List<PaperSection> sections = paperSectionService.listByPaperId(sectionBO.getPaperId());
        PaperSection section = sections.stream()
            .filter(s -> s.getId().equals(sectionBO.getId()))
            .findFirst()
            .orElse(null);
        if (section == null) {
            return error("大题不存在");
        }
        
        // 根据 volumeId 或 volumeCode 更新卷别ID
        Integer volumeId = sectionBO.getVolumeId();
        if (volumeId == null && sectionBO.getVolumeCode() != null) {
            // 如果只提供了 volumeCode，需要查找对应的 volumeId
            PaperVolume volume = paperVolumeService.getByPaperIdAndVolumeCode(sectionBO.getPaperId(), sectionBO.getVolumeCode());
            if (volume == null) {
                return error("卷别不存在");
            }
            volumeId = volume.getId();
        }
        if (volumeId == null) {
            return error("卷别ID不能为空");
        }
        
        section.setVolumeId(volumeId);
        section.setVolumeCode(sectionBO.getVolumeCode()); // 保留用于显示
        section.setSectionName(sectionBO.getSectionName());
        section.setSectionOrder(sectionBO.getSectionOrder());
        section.setQuestionCount(sectionBO.getQuestionCount());
        section.setTotalScore(sectionBO.getTotalScore());
        section.setScorePerQuestion(sectionBO.getScorePerQuestion());
        section.setInstructionText(sectionBO.getInstructionText());
        section.setAnswerTime(sectionBO.getAnswerTime() != null ? sectionBO.getAnswerTime() : 5);
        section.setAudioPlayCount(sectionBO.getAudioPlayCount() != null ? sectionBO.getAudioPlayCount() : 1);
        
        boolean result = paperSectionService.saveSection(section);
        return toAjax(result ? 1 : 0);
    }

    /**
     * 批量保存大题
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:edit')")
    @Log(title = "试卷大题管理", businessType = BusinessType.UPDATE)
    @PostMapping("/batchSave")
    public AjaxResult batchSave(@Validated @RequestBody List<PaperSectionBO> sectionList) {
        if (sectionList == null || sectionList.isEmpty()) {
            return error("大题列表不能为空");
        }
        
        // 转换为实体列表
        List<PaperSection> sections = sectionList.stream().map(bo -> {
            // 根据 volumeId 或 volumeCode 获取卷别ID
            Integer volumeId = bo.getVolumeId();
            if (volumeId == null && bo.getVolumeCode() != null) {
                // 如果只提供了 volumeCode，需要查找对应的 volumeId
                PaperVolume volume = paperVolumeService.getByPaperIdAndVolumeCode(bo.getPaperId(), bo.getVolumeCode());
                if (volume != null) {
                    volumeId = volume.getId();
                }
            }
            
            PaperSection section = new PaperSection();
            if (bo.getId() != null) {
                section.setId(bo.getId());
            }
            section.setPaperId(bo.getPaperId());
            section.setVolumeId(volumeId);
            section.setVolumeCode(bo.getVolumeCode()); // 保留用于显示
            section.setSectionName(bo.getSectionName());
            section.setSectionOrder(bo.getSectionOrder());
            section.setQuestionCount(bo.getQuestionCount() != null ? bo.getQuestionCount() : 0);
            section.setTotalScore(bo.getTotalScore() != null ? bo.getTotalScore() : java.math.BigDecimal.ZERO);
            section.setScorePerQuestion(bo.getScorePerQuestion());
            section.setInstructionText(bo.getInstructionText());
            section.setAnswerTime(bo.getAnswerTime() != null ? bo.getAnswerTime() : 5);
            section.setAudioPlayCount(bo.getAudioPlayCount() != null ? bo.getAudioPlayCount() : 1);
            return section;
        }).collect(Collectors.toList());
        
        // 获取试卷ID（所有大题应该属于同一试卷）
        Integer paperId = sectionList.get(0).getPaperId();
        boolean result = paperSectionService.saveBatch(sections, paperId);
        return toAjax(result ? 1 : 0);
    }

    /**
     * 删除大题
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:remove')")
    @Log(title = "试卷大题管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public AjaxResult remove(@Validated @RequestBody PaperSectionIdsBO idsBO) {
        int successCount = 0;
        for (Integer id : idsBO.getIds()) {
            if (paperSectionService.deleteById(id)) {
                successCount++;
            }
        }
        return toAjax(successCount);
    }
}


