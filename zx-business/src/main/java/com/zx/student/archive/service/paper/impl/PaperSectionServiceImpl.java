package com.zx.student.archive.service.paper.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zx.student.archive.domain.paper.PaperSection;
import com.zx.student.archive.mapper.paper.PaperSectionMapper;
import com.zx.student.archive.service.paper.IPaperSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 试卷大题服务实现类
 * 
 * @author zx
 */
@Service
@RequiredArgsConstructor
public class PaperSectionServiceImpl implements IPaperSectionService {

    private final PaperSectionMapper paperSectionMapper;

    @Override
    public List<PaperSection> listByPaperId(Integer paperId) {
        return paperSectionMapper.selectByPaperId(paperId);
    }

    @Override
    public List<PaperSection> listByPaperIdAndVolumeCode(Integer paperId, String volumeCode) {
        return paperSectionMapper.selectByPaperIdAndVolumeCode(paperId, volumeCode);
    }

    @Override
    public List<PaperSection> listByVolumeId(Integer volumeId) {
        return paperSectionMapper.selectByVolumeId(volumeId);
    }

    @Override
    public PaperSection getById(Integer id) {
        return paperSectionMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveSection(PaperSection section) {
        if (section.getId() == null) {
            return paperSectionMapper.insert(section) > 0;
        } else {
            return paperSectionMapper.updateById(section) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(List<PaperSection> sections, Integer paperId) {
        // 先删除该试卷的所有大题
        if (paperId != null) {
            paperSectionMapper.deleteByPaperId(paperId);
        }
        
        // 批量插入新的大题
        if (sections != null && !sections.isEmpty()) {
            for (PaperSection section : sections) {
                if (section.getPaperId() == null && paperId != null) {
                    section.setPaperId(paperId);
                }
                paperSectionMapper.insert(section);
            }
        }
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByPaperId(Integer paperId) {
        return paperSectionMapper.deleteByPaperId(paperId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Integer id) {
        return paperSectionMapper.deleteById(id) > 0;
    }
}


