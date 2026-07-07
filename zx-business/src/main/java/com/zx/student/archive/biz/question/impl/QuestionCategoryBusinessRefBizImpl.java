package com.zx.student.archive.biz.question.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zx.common.constant.AppErrorCode;
import com.zx.common.exception.ServiceException;
import com.zx.student.archive.biz.question.IQuestionCategoryBusinessRefBiz;
import com.zx.student.archive.domain.question.QuestionCategoryBusinessRef;
import com.zx.student.archive.mapper.question.QuestionCategoryBusinessRefMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * 题库业务关联实现类
 */
@Slf4j
@Service
public class QuestionCategoryBusinessRefBizImpl extends ServiceImpl<QuestionCategoryBusinessRefMapper, QuestionCategoryBusinessRef> 
    implements IQuestionCategoryBusinessRefBiz {


    @Override
    public boolean hasBusinessRefs(List<Integer> categoryIds) {
        if (CollectionUtils.isEmpty(categoryIds)) {
            return false;
        }

        Long count = this.baseMapper.selectCount(
                new LambdaQueryWrapper<QuestionCategoryBusinessRef>()
                        .in(QuestionCategoryBusinessRef::getQuestionCategoryId, categoryIds)
        );
        return count != null && count > 0;
    }


    @Override
    public List<QuestionCategoryBusinessRef> getRefsByBusinessId(Integer businessId, Integer businessType) throws ServiceException {
        if (businessId == null) {
            throw new ServiceException(AppErrorCode.APP_BUSINESS_ID_NOT_NULL_MSG);
        }
        if (businessType == null) {
            throw new ServiceException(AppErrorCode.APP_BUSINESS_TYPE_NOT_NULL_MSG);
        }
        List<QuestionCategoryBusinessRef> refs = this.baseMapper.selectList(
                new LambdaQueryWrapper<QuestionCategoryBusinessRef>()
                        .eq(QuestionCategoryBusinessRef::getBusinessId, businessId)
                        .eq(QuestionCategoryBusinessRef::getBusinessType, businessType)
                        .orderByAsc(QuestionCategoryBusinessRef::getSortNum,QuestionCategoryBusinessRef::getId)
        );

        return CollectionUtils.isEmpty(refs) ? Collections.emptyList() : refs;
    }

    @Override
    public List<QuestionCategoryBusinessRef> getBusinessRefs(List<Integer> categoryIds) {
        return this.baseMapper.selectList(
                new LambdaQueryWrapper<QuestionCategoryBusinessRef>()
                        .in(QuestionCategoryBusinessRef::getQuestionCategoryId, categoryIds));
    }
}
