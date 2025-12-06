package com.ruoyi.web.controller.paper;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.student.archive.domain.bo.paper.PaperIdBO;
import com.ruoyi.student.archive.domain.bo.paper.PaperVolumeBO;
import com.ruoyi.student.archive.domain.bo.paper.PaperVolumeIdsBO;
import com.ruoyi.student.archive.domain.paper.PaperVolume;
import com.ruoyi.student.archive.service.paper.IPaperVolumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 试卷卷别管理Controller
 * 
 * @author ruoyi
 */
@Slf4j
@RestController
@RequestMapping("/paper/volume")
@RequiredArgsConstructor
public class PaperVolumeController extends BaseController {

    private final IPaperVolumeService paperVolumeService;

    /**
     * 根据试卷ID查询卷别列表
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:query')")
    @PostMapping("/list")
    public AjaxResult list(@Validated @RequestBody PaperIdBO idBO) {
        List<PaperVolume> list = paperVolumeService.listByPaperId(idBO.getId());
        return success(list);
    }

    /**
     * 获取卷别详细信息
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:query')")
    @PostMapping("/detail")
    public AjaxResult getInfo(@Validated @RequestBody PaperVolumeIdsBO idsBO) {
        if (idsBO.getIds().size() != 1) {
            return error("只能查询单个卷别信息");
        }
        // 通过试卷ID查询所有卷别，然后过滤出目标卷别
        // 注意：这里需要知道试卷ID，但idsBO只有卷别ID列表
        // 更好的做法是添加一个根据卷别ID查询的方法
        // 暂时先返回错误，提示需要提供试卷ID
        return error("请使用 /list 接口查询卷别列表，或提供试卷ID");
    }

    /**
     * 新增卷别
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:add')")
    @Log(title = "试卷卷别管理", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    public AjaxResult add(@Validated @RequestBody PaperVolumeBO volumeBO) {
        PaperVolume volume = new PaperVolume();
        volume.setPaperId(volumeBO.getPaperId());
        
        // 如果未提供卷别代码，自动生成（A、B、C...）
        String volumeCode = volumeBO.getVolumeCode();
        if (volumeCode == null || volumeCode.trim().isEmpty()) {
            // 查询该试卷下已有的卷别数量，生成下一个字母
            List<PaperVolume> existingVolumes = paperVolumeService.listByPaperId(volumeBO.getPaperId());
            int nextIndex = existingVolumes.size();
            volumeCode = String.valueOf((char) ('A' + nextIndex));
        }
        volume.setVolumeCode(volumeCode);
        volume.setVolumeName(volumeBO.getVolumeName());
        volume.setVolumeOrder(volumeBO.getVolumeOrder());
        
        boolean result = paperVolumeService.saveVolume(volume);
        return toAjax(result ? 1 : 0);
    }

    /**
     * 修改卷别
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:edit')")
    @Log(title = "试卷卷别管理", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    public AjaxResult edit(@Validated @RequestBody PaperVolumeBO volumeBO) {
        if (volumeBO.getId() == null) {
            return error("卷别ID不能为空");
        }
        
        // 查询现有卷别（通过试卷ID和卷别ID）
        List<PaperVolume> volumes = paperVolumeService.listByPaperId(volumeBO.getPaperId());
        PaperVolume volume = volumes.stream()
            .filter(v -> v.getId().equals(volumeBO.getId()))
            .findFirst()
            .orElse(null);
        if (volume == null) {
            return error("卷别不存在");
        }
        
        volume.setVolumeCode(volumeBO.getVolumeCode());
        volume.setVolumeName(volumeBO.getVolumeName());
        volume.setVolumeOrder(volumeBO.getVolumeOrder());
        
        boolean result = paperVolumeService.saveVolume(volume);
        return toAjax(result ? 1 : 0);
    }

    /**
     * 批量保存卷别
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:edit')")
    @Log(title = "试卷卷别管理", businessType = BusinessType.UPDATE)
    @PostMapping("/batchSave")
    public AjaxResult batchSave(@Validated @RequestBody List<PaperVolumeBO> volumeList) {
        if (volumeList == null || volumeList.isEmpty()) {
            return error("卷别列表不能为空");
        }
        
        // 转换为实体列表
        List<PaperVolume> volumes = volumeList.stream().map(bo -> {
            PaperVolume volume = new PaperVolume();
            if (bo.getId() != null) {
                volume.setId(bo.getId());
            }
            volume.setPaperId(bo.getPaperId());
            volume.setVolumeCode(bo.getVolumeCode());
            volume.setVolumeName(bo.getVolumeName());
            volume.setVolumeOrder(bo.getVolumeOrder());
            return volume;
        }).collect(java.util.stream.Collectors.toList());
        
        // 获取试卷ID（所有卷别应该属于同一试卷）
        Integer paperId = volumeList.get(0).getPaperId();
        boolean result = paperVolumeService.saveBatch(volumes, paperId);
        return toAjax(result ? 1 : 0);
    }

    /**
     * 删除卷别
     */
    @PreAuthorize("@ss.hasPermi('paper:paper:remove')")
    @Log(title = "试卷卷别管理", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    public AjaxResult remove(@Validated @RequestBody PaperVolumeIdsBO idsBO) {
        int successCount = 0;
        for (Integer id : idsBO.getIds()) {
            if (paperVolumeService.deleteById(id)) {
                successCount++;
            }
        }
        return toAjax(successCount);
    }
}

