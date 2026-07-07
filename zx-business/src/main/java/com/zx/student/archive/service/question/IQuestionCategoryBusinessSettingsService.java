package com.zx.student.archive.service.question;

import com.zx.common.exception.ServiceException;
import com.zx.student.archive.domain.bo.question.QuestionCategoryBusinessSettingsBO;
import com.zx.student.archive.domain.dto.question.QuestionCategoryBusinessSettingsDTO;

import java.util.List;

/**
 * 题库业务设置统一管理 服务接口
 *
 * @author chuyi
 * @since 2025-08-16
 */
public interface IQuestionCategoryBusinessSettingsService {

    /**
     * 创建题库业务设置
     *
     * @param bo 题库业务设置BO
     * @return 是否成功
     * @throws BusinessException 业务异常
     */
    boolean createSettings(QuestionCategoryBusinessSettingsBO bo) throws ServiceException;

    /**
     * 更新题库业务设置
     *
     * @param bo 题库业务设置BO
     * @return 是否成功
     * @throws ServiceException 业务异常
     */
    boolean updateSettings(QuestionCategoryBusinessSettingsBO bo) throws ServiceException;

    /**
     * 根据业务ID和业务类型获取设置详情
     *
     * @param businessId 业务ID
     * @param businessType 业务类型
     * @return 设置详情
     * @throws ServiceException 业务异常
     */
    QuestionCategoryBusinessSettingsDTO getSettingsByBusiness(Integer businessId, Integer businessType) throws ServiceException;

    /**
     * 根据业务ID和业务类型删除设置
     *
     * @param businessId 业务ID
     * @param businessType 业务类型
     * @return 是否成功
     * @throws ServiceException 业务异常
     */
    boolean deleteSettingsByBusiness(Integer businessId, Integer businessType) throws ServiceException;

    /**
     * 检查业务是否已配置题库设置
     *
     * @param businessId 业务ID
     * @param businessType 业务类型
     * @return 是否已配置
     */
    boolean hasSettings(Integer businessId, Integer businessType);

    /**
     * 批量创建题库业务设置
     *
     * @param settingsList 题库业务设置列表
     * @return 是否成功
     * @throws BusinessException 业务异常
     */
    boolean createSettingsBatch(List<QuestionCategoryBusinessSettingsBO> settingsList) throws ServiceException;

    /**
     * 批量删除题库业务设置
     *
     * @param businessIds 业务ID列表
     * @param businessType 业务类型
     * @return 是否成功
     * @throws ServiceException 业务异常
     */
    boolean deleteSettingsBatch(List<Integer> businessIds, Integer businessType) throws ServiceException;
}
