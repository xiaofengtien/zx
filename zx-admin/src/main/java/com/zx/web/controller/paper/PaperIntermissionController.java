package com.zx.web.controller.paper;

import com.zx.common.annotation.Log;
import com.zx.common.core.controller.BaseController;
import com.zx.common.core.domain.AjaxResult;
import com.zx.common.enums.BusinessType;
import com.zx.student.archive.domain.bo.paper.PaperIdBO;
import com.zx.student.archive.domain.bo.paper.PaperIntermissionBO;
import com.zx.student.archive.domain.bo.paper.PaperIntermissionIdsBO;
import com.zx.student.archive.domain.paper.PaperIntermission;
import com.zx.student.archive.service.paper.IPaperIntermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 试卷中场配置管理Controller
 * 
 * @author zx
 */
@Slf4j
@RestController
@RequestMapping("/paper/intermission")
@RequiredArgsConstructor
public class PaperIntermissionController extends BaseController {

    private final IPaperIntermissionService paperIntermissionService;

    /**
     * 根据试卷ID查询中场配置列表
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:query')")
    @PostMapping("/list")
    public AjaxResult list(@Validated @RequestBody PaperIdBO idBO) {
        List<PaperIntermission> list = paperIntermissionService.listByPaperId(idBO.getId());
        return success(list);
    }

    /**
     * 新增中场配置
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:add')")
    @Log(title = "试卷中场配置管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@Validated @RequestBody PaperIntermissionBO intermissionBO) {
        PaperIntermission intermission = new PaperIntermission();
        intermission.setPaperId(intermissionBO.getPaperId());
        intermission.setFromVolumeId(intermissionBO.getFromVolumeId());
        intermission.setToVolumeId(intermissionBO.getToVolumeId());
        intermission.setFromVolume(intermissionBO.getFromVolume());
        intermission.setToVolume(intermissionBO.getToVolume());
        intermission.setIntermissionText(intermissionBO.getIntermissionText());
        intermission.setCanSkip(intermissionBO.getCanSkip() != null ? intermissionBO.getCanSkip() : 0);
        
        boolean result = paperIntermissionService.saveIntermission(intermission);
        if (result) {
            // MyBatis-Plus 的 insert 会自动填充 ID，返回新增的ID
            Integer id = intermission.getId();
            log.debug("新增中场配置成功，ID: {}", id);
            if (id != null && id > 0) {
                return success(id);
            } else {
                log.warn("新增中场配置成功，但ID为空或无效: {}", id);
                return error("新增成功，但无法获取ID");
            }
        } else {
            return error("新增失败");
        }
    }

    /**
     * 修改中场配置
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:edit')")
    @Log(title = "试卷中场配置管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@Validated @RequestBody PaperIntermissionBO intermissionBO) {
        if (intermissionBO.getId() == null) {
            return error("中场配置ID不能为空");
        }
        
        // 查询现有中场配置（通过试卷ID和中场配置ID）
        List<PaperIntermission> intermissions = paperIntermissionService.listByPaperId(intermissionBO.getPaperId());
        PaperIntermission intermission = intermissions.stream()
            .filter(i -> i.getId().equals(intermissionBO.getId()))
            .findFirst()
            .orElse(null);
        if (intermission == null) {
            return error("中场配置不存在");
        }
        
        intermission.setFromVolumeId(intermissionBO.getFromVolumeId());
        intermission.setToVolumeId(intermissionBO.getToVolumeId());
        intermission.setFromVolume(intermissionBO.getFromVolume());
        intermission.setToVolume(intermissionBO.getToVolume());
        intermission.setIntermissionText(intermissionBO.getIntermissionText());
        intermission.setCanSkip(intermissionBO.getCanSkip() != null ? intermissionBO.getCanSkip() : 0);
        
        boolean result = paperIntermissionService.saveIntermission(intermission);
        return toAjax(result ? 1 : 0);
    }

    /**
     * 批量保存中场配置
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:edit')")
    @Log(title = "试卷中场配置管理", businessType = BusinessType.UPDATE)
    @PostMapping("/batchSave")
    public AjaxResult batchSave(@Validated @RequestBody List<PaperIntermissionBO> intermissionList) {
        if (intermissionList == null || intermissionList.isEmpty()) {
            return error("中场配置列表不能为空");
        }
        
        // 转换为实体列表
        List<PaperIntermission> intermissions = intermissionList.stream().map(bo -> {
            PaperIntermission intermission = new PaperIntermission();
            if (bo.getId() != null) {
                intermission.setId(bo.getId());
            }
            intermission.setPaperId(bo.getPaperId());
            intermission.setFromVolume(bo.getFromVolume());
            intermission.setToVolume(bo.getToVolume());
            intermission.setIntermissionText(bo.getIntermissionText());
            intermission.setCanSkip(bo.getCanSkip() != null ? bo.getCanSkip() : 0);
            return intermission;
        }).collect(Collectors.toList());
        
        // 获取试卷ID（所有中场配置应该属于同一试卷）
        Integer paperId = intermissionList.get(0).getPaperId();
        boolean result = paperIntermissionService.saveBatch(intermissions, paperId);
        return toAjax(result ? 1 : 0);
    }

    /**
     * 删除中场配置
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:remove')")
    @Log(title = "试卷中场配置管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public AjaxResult remove(@Validated @RequestBody PaperIntermissionIdsBO idsBO) {
        int successCount = 0;
        for (Integer id : idsBO.getIds()) {
            if (paperIntermissionService.deleteById(id)) {
                successCount++;
            }
        }
        return toAjax(successCount);
    }
}



