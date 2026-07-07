package com.zx.student.archive.service.paper.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zx.student.archive.domain.paper.PaperVolume;
import com.zx.student.archive.mapper.paper.PaperVolumeMapper;
import com.zx.student.archive.service.paper.IPaperVolumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 试卷卷别服务实现类
 * 
 * @author zx
 */
@Service
@RequiredArgsConstructor
public class PaperVolumeServiceImpl implements IPaperVolumeService {

    private final PaperVolumeMapper paperVolumeMapper;

    @Override
    public List<PaperVolume> listByPaperId(Integer paperId) {
        return paperVolumeMapper.selectByPaperId(paperId);
    }

    @Override
    public PaperVolume getByPaperIdAndVolumeCode(Integer paperId, String volumeCode) {
        return paperVolumeMapper.selectByPaperIdAndVolumeCode(paperId, volumeCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveVolume(PaperVolume volume) {
        if (volume.getId() == null) {
            return paperVolumeMapper.insert(volume) > 0;
        } else {
            return paperVolumeMapper.updateById(volume) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(List<PaperVolume> volumes, Integer paperId) {
        // 先删除该试卷的所有卷别
        if (paperId != null) {
            paperVolumeMapper.deleteByPaperId(paperId);
        }
        
        // 批量插入新的卷别
        if (volumes != null && !volumes.isEmpty()) {
            for (PaperVolume volume : volumes) {
                if (volume.getPaperId() == null && paperId != null) {
                    volume.setPaperId(paperId);
                }
                paperVolumeMapper.insert(volume);
            }
        }
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByPaperId(Integer paperId) {
        return paperVolumeMapper.deleteByPaperId(paperId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Integer id) {
        return paperVolumeMapper.deleteById(id) > 0;
    }
}



