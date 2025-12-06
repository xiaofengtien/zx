package com.ruoyi.student.archive.service.paper.impl;

import com.ruoyi.student.archive.domain.paper.PaperIntermission;
import com.ruoyi.student.archive.mapper.paper.PaperIntermissionMapper;
import com.ruoyi.student.archive.service.paper.IPaperIntermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 卷间中场配置服务实现类
 * 
 * @author ruoyi
 */
@Service
@RequiredArgsConstructor
public class PaperIntermissionServiceImpl implements IPaperIntermissionService {

    private final PaperIntermissionMapper paperIntermissionMapper;

    @Override
    public List<PaperIntermission> listByPaperId(Integer paperId) {
        return paperIntermissionMapper.selectByPaperId(paperId);
    }

    @Override
    public PaperIntermission getByPaperIdAndVolumes(Integer paperId, String fromVolume, String toVolume) {
        return paperIntermissionMapper.selectByPaperIdAndVolumes(paperId, fromVolume, toVolume);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveIntermission(PaperIntermission intermission) {
        if (intermission.getId() == null) {
            return paperIntermissionMapper.insert(intermission) > 0;
        } else {
            return paperIntermissionMapper.updateById(intermission) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(List<PaperIntermission> intermissions, Integer paperId) {
        // 先删除该试卷的所有中场配置
        if (paperId != null) {
            paperIntermissionMapper.deleteByPaperId(paperId);
        }
        
        // 批量插入新的中场配置
        if (intermissions != null && !intermissions.isEmpty()) {
            for (PaperIntermission intermission : intermissions) {
                if (intermission.getPaperId() == null && paperId != null) {
                    intermission.setPaperId(paperId);
                }
                paperIntermissionMapper.insert(intermission);
            }
        }
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByPaperId(Integer paperId) {
        return paperIntermissionMapper.deleteByPaperId(paperId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Integer id) {
        return paperIntermissionMapper.deleteById(id) > 0;
    }
}



