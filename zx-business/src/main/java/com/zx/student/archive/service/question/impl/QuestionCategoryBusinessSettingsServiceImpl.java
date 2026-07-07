package com.zx.student.archive.service.question.impl;

import com.zx.common.exception.ServiceException;
import com.zx.student.archive.biz.question.IQuestionCategoryBusinessSettingsBiz;
import com.zx.student.archive.domain.bo.question.QuestionCategoryBusinessSettingsBO;
import com.zx.student.archive.domain.dto.question.QuestionCategoryBusinessSettingsDTO;
import com.zx.student.archive.service.question.IQuestionCategoryBusinessSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 题库业务设置统一管理 服务实现类
 *
 * @author chuyi
 * @since 2025-08-16
 */
@Slf4j
@Service
public class QuestionCategoryBusinessSettingsServiceImpl implements IQuestionCategoryBusinessSettingsService {

    @Resource
    private IQuestionCategoryBusinessSettingsBiz questionCategoryBusinessSettingsBiz;

    @Override
    public boolean createSettings(QuestionCategoryBusinessSettingsBO bo) throws ServiceException {
        return questionCategoryBusinessSettingsBiz.createSettings(bo);
    }

    @Override
    public boolean updateSettings(QuestionCategoryBusinessSettingsBO bo) throws ServiceException {
        return questionCategoryBusinessSettingsBiz.updateSettings(bo);
    }

    @Override
    public QuestionCategoryBusinessSettingsDTO getSettingsByBusiness(Integer businessId, Integer businessType) throws ServiceException {
        return questionCategoryBusinessSettingsBiz.getSettingsByBusiness(businessId, businessType);
    }

    @Override
    public boolean deleteSettingsByBusiness(Integer businessId, Integer businessType) throws ServiceException {
        return questionCategoryBusinessSettingsBiz.deleteSettingsByBusiness(businessId, businessType);
    }

    @Override
    public boolean hasSettings(Integer businessId, Integer businessType) {
        return questionCategoryBusinessSettingsBiz.hasSettings(businessId, businessType);
    }

    @Override
    public boolean createSettingsBatch(List<QuestionCategoryBusinessSettingsBO> settingsList) throws ServiceException {
        return questionCategoryBusinessSettingsBiz.createSettingsBatch(settingsList);
    }

    @Override
    public boolean deleteSettingsBatch(List<Integer> businessIds, Integer businessType) throws ServiceException {
        return questionCategoryBusinessSettingsBiz.deleteSettingsBatch(businessIds, businessType);
    }
}
