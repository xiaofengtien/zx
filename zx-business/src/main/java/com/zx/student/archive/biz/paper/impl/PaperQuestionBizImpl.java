package com.zx.student.archive.biz.paper.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zx.student.archive.biz.paper.IPaperQuestionBiz;
import com.zx.student.archive.domain.paper.PaperQuestion;
import com.zx.student.archive.mapper.paper.PaperQuestionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * 试卷题目关联业务实现类
 * 
 * @author zx
 */
@Slf4j
@Service
public class PaperQuestionBizImpl extends ServiceImpl<PaperQuestionMapper, PaperQuestion> implements IPaperQuestionBiz {

    @Override
    public List<PaperQuestion> listByPaperId(Integer paperId) {
        if (paperId == null) {
            return List.of();
        }
        return baseMapper.selectByPaperId(paperId);
    }

    @Override
    public List<PaperQuestion> listByQuestionId(Integer questionId) {
        if (questionId == null) {
            return List.of();
        }
        return baseMapper.selectByQuestionId(questionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchInsert(List<PaperQuestion> paperQuestions) {
        if (CollectionUtils.isEmpty(paperQuestions)) {
            return true;
        }
        
        // 设置创建时间
        Date now = new Date();
        for (PaperQuestion pq : paperQuestions) {
            if (pq.getCreateTime() == null) {
                pq.setCreateTime(now);
            }
        }
        
        return baseMapper.batchInsert(paperQuestions) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByPaperId(Integer paperId) {
        if (paperId == null) {
            return false;
        }
        return baseMapper.deleteByPaperId(paperId) >= 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteByPaperIdAndQuestionIds(Integer paperId, List<Integer> questionIds) {
        if (paperId == null || CollectionUtils.isEmpty(questionIds)) {
            return false;
        }
        
        LambdaQueryWrapper<PaperQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaperQuestion::getPaperId, paperId)
               .in(PaperQuestion::getQuestionId, questionIds);
        
        return this.remove(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByPaperIdAndSectionId(Integer paperId, Integer sectionId) {
        if (paperId == null || sectionId == null) {
            return false;
        }
        
        LambdaQueryWrapper<PaperQuestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaperQuestion::getPaperId, paperId)
               .eq(PaperQuestion::getSectionId, sectionId);
        
        return this.remove(wrapper);
    }
}


