package com.zx.student.archive.biz.question.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zx.student.archive.biz.question.IQuestionMediaBiz;
import com.zx.student.archive.domain.question.QuestionMedia;
import com.zx.student.archive.mapper.question.QuestionMediaMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;

/**
 * 题目媒体文件业务实现类
 * 
 * @author zx
 */
@Slf4j
@Service
public class QuestionMediaBizImpl extends ServiceImpl<QuestionMediaMapper, QuestionMedia> implements IQuestionMediaBiz {

    @Override
    public List<QuestionMedia> listByQuestionId(Integer questionId) {
        if (questionId == null) {
            return List.of();
        }
        return baseMapper.selectByQuestionId(questionId);
    }

    @Override
    public List<QuestionMedia> listByQuestionIdAndType(Integer questionId, Integer mediaType) {
        if (questionId == null || mediaType == null) {
            return List.of();
        }
        return baseMapper.selectByQuestionIdAndType(questionId, mediaType);
    }

    @Override
    public QuestionMedia getByOptionId(Integer optionId) {
        if (optionId == null) {
            return null;
        }
        return baseMapper.selectByOptionId(optionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByQuestionId(Integer questionId) {
        if (questionId == null) {
            return false;
        }
        return baseMapper.deleteByQuestionId(questionId) >= 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSave(List<QuestionMedia> mediaList) {
        if (CollectionUtils.isEmpty(mediaList)) {
            return true;
        }
        
        // 设置创建时间
        Date now = new Date();
        for (QuestionMedia media : mediaList) {
            if (media.getCreateTime() == null) {
                media.setCreateTime(now);
            }
        }
        
        return this.saveBatch(mediaList);
    }
}



