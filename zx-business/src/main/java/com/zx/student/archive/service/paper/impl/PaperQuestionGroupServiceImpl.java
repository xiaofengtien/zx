package com.zx.student.archive.service.paper.impl;

import com.zx.student.archive.domain.paper.PaperQuestionGroup;
import com.zx.student.archive.mapper.paper.PaperQuestionGroupMapper;
import com.zx.student.archive.service.paper.IPaperQuestionGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 试卷题目组服务实现类
 * 
 * @author zx
 */
@Service
@RequiredArgsConstructor
public class PaperQuestionGroupServiceImpl implements IPaperQuestionGroupService {

    private final PaperQuestionGroupMapper paperQuestionGroupMapper;

    @Override
    public List<PaperQuestionGroup> listBySectionId(Integer sectionId) {
        return paperQuestionGroupMapper.selectBySectionId(sectionId);
    }

    @Override
    public List<PaperQuestionGroup> listByPaperId(Integer paperId) {
        return paperQuestionGroupMapper.selectByPaperId(paperId);
    }

    @Override
    public PaperQuestionGroup getById(Integer id) {
        return paperQuestionGroupMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveGroup(PaperQuestionGroup group) {
        if (group.getId() == null) {
            return paperQuestionGroupMapper.insert(group) > 0;
        } else {
            return paperQuestionGroupMapper.updateById(group) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(List<PaperQuestionGroup> groups, Integer sectionId) {
        // 先删除该大题的所有题目组
        if (sectionId != null) {
            paperQuestionGroupMapper.deleteBySectionId(sectionId);
        }

        // 批量插入新的题目组
        if (groups != null && !groups.isEmpty()) {
            for (PaperQuestionGroup group : groups) {
                if (group.getSectionId() == null && sectionId != null) {
                    group.setSectionId(sectionId);
                }
                paperQuestionGroupMapper.insert(group);
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBySectionId(Integer sectionId) {
        return paperQuestionGroupMapper.deleteBySectionId(sectionId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByPaperId(Integer paperId) {
        // 获取试卷下所有大题，然后删除这些大题下的所有题目组
        // 这里简化处理：通过查询试卷下所有题目组并删除
        List<PaperQuestionGroup> groups = paperQuestionGroupMapper.selectByPaperId(paperId);
        if (groups != null && !groups.isEmpty()) {
            for (PaperQuestionGroup group : groups) {
                paperQuestionGroupMapper.deleteById(group.getId());
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Integer id) {
        return paperQuestionGroupMapper.deleteById(id) > 0;
    }
}
