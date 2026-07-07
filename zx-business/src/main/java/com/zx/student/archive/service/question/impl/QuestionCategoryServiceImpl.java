package com.zx.student.archive.service.question.impl;

import cn.hutool.core.bean.BeanUtil;
import com.zx.common.constant.AppErrorCode;
import com.zx.common.enums.BusinessTypeEnum;
import com.zx.common.exception.ServiceException;
import com.zx.student.archive.biz.question.*;
import com.zx.student.archive.domain.bo.question.*;
import com.zx.student.archive.domain.dto.question.QuestionCategoryDTO;
import com.zx.student.archive.domain.dto.question.QuestionCategoryRefDTO;
import com.zx.student.archive.domain.question.QuestionCategoryBusinessRef;
import com.zx.student.archive.service.question.QuestionCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 题目分类服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionCategoryServiceImpl implements QuestionCategoryService {

    private final QuestionCategoryBiz questionCategoryBiz;
    private final QuestionBiz questionBiz;
    private final IQuestionCategoryBusinessRefBiz questionCategoryBusinessRefBiz;

    private static final  Integer CATEGORY_REF_CODE = 1;
    private static final  String CATEGORY_REF_NAME = "答题";


    @Override
    public List<QuestionCategoryDTO> getCategoryTree(QuestionCategoryQueryBO queryBO) {
        return questionCategoryBiz.getCategoryTree(queryBO);
    }

    @Override
    public QuestionCategoryDTO getCategory(QuestionCategoryIdBO idBO) {
        if (Objects.isNull(idBO)) {
            return null;
        }
        List<QuestionCategoryDTO> categories = questionCategoryBiz.getCategoryWithChildren(idBO.getId());
        return CollectionUtils.isEmpty(categories) ? null : categories.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createCategory(QuestionCategoryBO categoryBO) throws ServiceException {
        return questionCategoryBiz.createCategory(categoryBO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(QuestionCategoryBO categoryBO) throws ServiceException {
        questionCategoryBiz.updateCategory(categoryBO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteCategory(QuestionCategoryIdsBO idsBO) throws ServiceException {
        if (Objects.isNull(idsBO)) {
            return;
        }

        // 检查是否存在子分类
        if (questionCategoryBiz.hasChildren(idsBO.getIds())) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_CATEGORY_HAS_CHILDREN_MSG);
        }

        // 检查是否有关联的试题
        if (questionBiz.hasCategoryQuestions(idsBO.getIds())) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_CATEGORY_HAS_QUESTIONS_MSG);
        }

        // 检查分类是否已被配套引用
        if (questionCategoryBusinessRefBiz.hasBusinessRefs(idsBO.getIds())) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_CATEGORY_HAS_REF_MSG);
        }

        // 删除分类
        questionCategoryBiz.removeByIds(idsBO.getIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSort(QuestionCategorySortBO sortBO) throws ServiceException {
        questionCategoryBiz.updateSort(sortBO);
    }

    @Override
    public boolean checkNameExists(String name, Integer fatherId, Integer excludeId) {
        return questionCategoryBiz.checkNameExists(name, fatherId, excludeId);
    }

    @Override
    public List<QuestionCategoryRefDTO> getBusinessRefs(QuestionCategoryIdQueryBO queryBO){
        List<QuestionCategoryBusinessRef> entityList = questionCategoryBusinessRefBiz.getBusinessRefs(List.of(queryBO.getCategoryId()));
        List<QuestionCategoryRefDTO> result = new ArrayList<>();

        for (QuestionCategoryBusinessRef businessRef : entityList) {
            QuestionCategoryRefDTO dto = new QuestionCategoryRefDTO();
            BeanUtil.copyProperties(businessRef, dto);

            BusinessTypeEnum typeEnum = BusinessTypeEnum.getByCode(dto.getBusinessType());
            if (typeEnum == null) {
                log.warn("未知的业务类型: {}", dto.getBusinessType());
                continue;
            }

            dto.setBusinessTypeStr(typeEnum.getName());
            dto.setAppMaterialType(CATEGORY_REF_CODE);
            dto.setAppMaterialTypeStr(CATEGORY_REF_NAME);

            // TODO: processorManager需要后续实现
            // try {
            //     processorManager.processBusinessInfo(dto, businessRef);
            //     result.add(dto);
            // } catch (Exception e) {
            //     log.error("处理业务引用信息失败, businessId: {}, businessType: {}",
            //         businessRef.getBusinessId(), businessRef.getBusinessType(), e);
            // }
            result.add(dto);
        }

        return result;
    }


} 