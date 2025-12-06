package com.ruoyi.student.archive.biz.question.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.AppErrorCode;
import com.ruoyi.common.enums.BusinessTypeEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.biz.question.IQuestionCategoryBusinessRefBiz;
import com.ruoyi.student.archive.biz.question.IQuestionCategoryBusinessSettingsBiz;
import com.ruoyi.student.archive.domain.bo.question.QuestionCategoryBusinessRefBO;
import com.ruoyi.student.archive.domain.bo.question.QuestionCategoryBusinessSettingsBO;
import com.ruoyi.student.archive.domain.dto.question.QuestionCategoryBusinessRefDTO;
import com.ruoyi.student.archive.domain.dto.question.QuestionCategoryBusinessSettingsDTO;
import com.ruoyi.student.archive.domain.dto.question.QuestionCountDTO;
import com.ruoyi.student.archive.domain.question.QuestionCategory;
import com.ruoyi.student.archive.domain.question.QuestionCategoryBusinessRef;
import com.ruoyi.student.archive.domain.question.QuestionCategoryBusinessSettings;
import com.ruoyi.student.archive.mapper.question.QuestionCategoryBusinessRefMapper;
import com.ruoyi.student.archive.mapper.question.QuestionCategoryBusinessSettingsMapper;
import com.ruoyi.student.archive.mapper.question.QuestionCategoryMapper;
import com.ruoyi.student.archive.mapper.question.QuestionMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 题库业务设置统一管理 服务实现类
 *
 * @author chuyi
 * @since 2025-08-16
 */
@Slf4j
@Service
public class QuestionCategoryBusinessSettingsBizImpl extends ServiceImpl<QuestionCategoryBusinessSettingsMapper, QuestionCategoryBusinessSettings>
        implements IQuestionCategoryBusinessSettingsBiz {

    @Resource
    private IQuestionCategoryBusinessRefBiz questionCategoryBusinessRefBiz;
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private QuestionCategoryBusinessSettingsMapper questionSettingsMapper;
    @Resource
    private QuestionCategoryBusinessRefMapper businessRefMapper;
    @Resource
    private QuestionCategoryMapper questionCategoryMapper;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createSettings(QuestionCategoryBusinessSettingsBO bo) throws ServiceException {
        // 参数校验
        validateBusinessParams(bo.getBusinessId(), bo.getBusinessType());
        
        // 检查是否已存在配置
        if (hasSettings(bo.getBusinessId(), bo.getBusinessType())) {
            throw new ServiceException(AppErrorCode.APP_BUSINESS_SETTINGS_EXISTS_MSG);
        }

        // 创建设置记录
        QuestionCategoryBusinessSettings settings = new QuestionCategoryBusinessSettings();
        BeanUtil.copyProperties(bo, settings, "id");
        settings.setStatus(1);
        this.save(settings);

        // 创建题库关联记录
        if (CollectionUtil.isNotEmpty(bo.getQuestionCategories())) {
            createQuestionCategoryRefs(settings.getId(), bo.getBusinessType(), bo.getQuestionCategories());
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSettings(QuestionCategoryBusinessSettingsBO bo) throws ServiceException {
        // 参数校验
        validateBusinessParams(bo.getBusinessId(), bo.getBusinessType());
        
        if (bo.getId() == null) {
            throw new ServiceException(AppErrorCode.APP_SETTINGS_ID_NOT_NULL_MSG);
        }

        // 更新设置记录
        QuestionCategoryBusinessSettings settings = BeanUtil.copyProperties(bo, QuestionCategoryBusinessSettings.class);
        settings.setId(bo.getId());
        this.updateById(settings);

        // 删除原有题库关联记录
        questionCategoryBusinessRefBiz.remove(
                new LambdaQueryWrapper<QuestionCategoryBusinessRef>()
                        .eq(QuestionCategoryBusinessRef::getBusinessId, bo.getBusinessId())
                        .eq(QuestionCategoryBusinessRef::getBusinessType, bo.getBusinessType())
        );

        // 重新创建题库关联记录
        if (CollectionUtils.isNotEmpty(bo.getQuestionCategories())) {
            createQuestionCategoryRefs(bo.getBusinessId(), bo.getBusinessType(), bo.getQuestionCategories());
        }

        return true;
    }

    @Override
    public QuestionCategoryBusinessSettingsDTO getSettingsByBusiness(Integer businessId, Integer businessType) throws ServiceException {
        // 参数校验
        validateBusinessParams(businessId, businessType);

        // 查询设置记录
        QuestionCategoryBusinessSettings settings = this.getOne(
                new LambdaQueryWrapper<QuestionCategoryBusinessSettings>()
                        .eq(QuestionCategoryBusinessSettings::getBusinessId, businessId)
                        .eq(QuestionCategoryBusinessSettings::getBusinessType, businessType)
        );

        if (settings == null) {
            return null;
        }

        // 转换为DTO
        QuestionCategoryBusinessSettingsDTO dto = BeanUtil.copyProperties(settings, QuestionCategoryBusinessSettingsDTO.class);
        
        // 设置业务类型描述
        BusinessTypeEnum typeEnum = BusinessTypeEnum.getByCode(businessType);
        if (typeEnum != null) {
            dto.setBusinessTypeDesc(typeEnum.getName());
        }

        // 查询关联的题库分类
        List<QuestionCategoryBusinessRef> refs = questionCategoryBusinessRefBiz.getRefsByBusinessId(businessId, businessType);
        if (CollectionUtil.isNotEmpty(refs)) {
            List<QuestionCategoryBusinessRefDTO> refDTOs = refs.stream()
                    .map(ref -> BeanUtil.copyProperties(ref, QuestionCategoryBusinessRefDTO.class))
                    .collect(Collectors.toList());
            dto.setQuestionCategories(refDTOs);
        }

        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSettingsByBusiness(Integer businessId, Integer businessType) throws ServiceException {
        validateBusinessParams(businessId, businessType);

        this.remove(
                new LambdaQueryWrapper<QuestionCategoryBusinessSettings>()
                        .eq(QuestionCategoryBusinessSettings::getBusinessId, businessId)
                        .eq(QuestionCategoryBusinessSettings::getBusinessType, businessType)
        );

        questionCategoryBusinessRefBiz.remove(
                new LambdaQueryWrapper<QuestionCategoryBusinessRef>()
                        .eq(QuestionCategoryBusinessRef::getBusinessId, businessId)
                        .eq(QuestionCategoryBusinessRef::getBusinessType, businessType)
        );

        return true;
    }

    @Override
    public boolean hasSettings(Integer businessId, Integer businessType) {
        if (businessId == null || businessType == null) {
            return false;
        }

        long count = this.count(
                new LambdaQueryWrapper<QuestionCategoryBusinessSettings>()
                        .eq(QuestionCategoryBusinessSettings::getBusinessId, businessId)
                        .eq(QuestionCategoryBusinessSettings::getBusinessType, businessType)
        );

        return count > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createSettingsBatch(List<QuestionCategoryBusinessSettingsBO> settingsList) throws ServiceException {
        if (CollectionUtil.isEmpty(settingsList)) {
            return true;
        }

        // 批量创建设置记录
        List<QuestionCategoryBusinessSettings> settingsEntities = new ArrayList<>();
        List<QuestionCategoryBusinessRef> allRefs = new ArrayList<>();

        for (QuestionCategoryBusinessSettingsBO bo : settingsList) {
            // 参数校验
            validateBusinessParams(bo.getBusinessId(), bo.getBusinessType());

            // 检查是否已存在配置
            if (hasSettings(bo.getBusinessId(), bo.getBusinessType())) {
                log.warn("业务已存在题库设置，跳过创建，businessId：{}，businessType：{}", bo.getBusinessId(), bo.getBusinessType());
                continue;
            }

            // 创建设置记录 - 忽略ID字段避免主键冲突
            QuestionCategoryBusinessSettings settings = new QuestionCategoryBusinessSettings();
            BeanUtil.copyProperties(bo, settings, "id");
            settings.setStatus(1);
            settingsEntities.add(settings);
        }

        // 批量保存设置记录
        if (CollectionUtil.isNotEmpty(settingsEntities)) {
            this.saveBatch(settingsEntities);

            // 为每个设置记录创建题库关联 - 使用Map确保数据对应关系
            Map<String, QuestionCategoryBusinessSettingsBO> boMap = new HashMap<>();
            for (QuestionCategoryBusinessSettingsBO bo : settingsList) {
                // 使用业务ID+业务类型作为唯一标识
                String key = bo.getBusinessId() + "_" + bo.getBusinessType();
                boMap.put(key, bo);
            }

            for (QuestionCategoryBusinessSettings entity : settingsEntities) {
                String key = entity.getBusinessId() + "_" + entity.getBusinessType();
                QuestionCategoryBusinessSettingsBO bo = boMap.get(key);

                if (bo != null && CollectionUtil.isNotEmpty(bo.getQuestionCategories())) {
                    List<QuestionCategoryBusinessRef> refs = bo.getQuestionCategories().stream()
                            .map(categoryBO -> {
                                QuestionCategoryBusinessRef ref = new QuestionCategoryBusinessRef();
                                ref.setQuestionCategoryId(categoryBO.getQuestionCategoryId());
                                ref.setBusinessId(entity.getId());
                                ref.setBusinessType(bo.getBusinessType());
                                ref.setSortNum(categoryBO.getSortNum());
                                return ref;
                            })
                            .toList();
                    allRefs.addAll(refs);
                }
            }

            // 批量保存题库关联记录
            if (CollectionUtil.isNotEmpty(allRefs)) {
                questionCategoryBusinessRefBiz.saveBatch(allRefs);
            }
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSettingsBatch(List<Integer> businessIds, Integer businessType) throws ServiceException {
        if (CollectionUtil.isEmpty(businessIds) || businessType == null) {
            return true;
        }

        // 参数校验
        if (BusinessTypeEnum.isValid(businessType)) {
            throw new ServiceException(AppErrorCode.APP_BUSINESS_TYPE_INVALID_MSG);
        }

        // 批量删除设置记录
        this.remove(
                new LambdaQueryWrapper<QuestionCategoryBusinessSettings>()
                        .in(QuestionCategoryBusinessSettings::getBusinessId, businessIds)
                        .eq(QuestionCategoryBusinessSettings::getBusinessType, businessType)
        );

        // 批量删除题库关联记录
        questionCategoryBusinessRefBiz.remove(
                new LambdaQueryWrapper<QuestionCategoryBusinessRef>()
                        .in(QuestionCategoryBusinessRef::getBusinessId, businessIds)
                        .eq(QuestionCategoryBusinessRef::getBusinessType, businessType)
        );

        log.info("批量删除题库业务设置完成，业务类型：{}，业务ID数量：{}", businessType, businessIds.size());
        return true;
    }



    /**
     * 获取问题扩展信息
     *
     * @param businessIds  业务模块ID列表
     * @param businessType 业务模块类型
     * @return 模块ID到问题扩展信息的映射
     */
    @Override
    public Map<Integer, QuestionCategoryBusinessSettingsDTO> fetchQuestionCategorySettings(List<Integer> businessIds, Integer businessType) {
        if (CollectionUtils.isEmpty(businessIds)) {
            return Collections.emptyMap();
        }

        // 查询问题扩展信息
        List<QuestionCategoryBusinessSettings> questionExtList = questionSettingsMapper.selectList(new LambdaQueryWrapper<QuestionCategoryBusinessSettings>()
                .in(QuestionCategoryBusinessSettings::getBusinessId, businessIds)
                .eq(QuestionCategoryBusinessSettings::getBusinessType, businessType));
        Map<Integer, QuestionCategoryBusinessSettings> questionExtMap = questionExtList.stream().collect(Collectors.toMap(QuestionCategoryBusinessSettings::getBusinessId, e -> e, (a, b) -> a));

        // 如果没有扩展信息，直接返回空映射
        if (questionExtMap.isEmpty()) {
            return Collections.emptyMap();
        }

        // 查询关联的问题分类数据
        List<Integer> questionExtIds = questionExtList.stream().map(QuestionCategoryBusinessSettings::getId).collect(Collectors.toList());
        List<QuestionCategoryBusinessRef> allCategoryRefs = businessRefMapper.selectList(new LambdaQueryWrapper<QuestionCategoryBusinessRef>()
                .in(QuestionCategoryBusinessRef::getBusinessId, questionExtIds)
                .eq(QuestionCategoryBusinessRef::getBusinessType, businessType)
                .orderByAsc(QuestionCategoryBusinessRef::getId)
                .orderByAsc(QuestionCategoryBusinessRef::getSortNum));

        if (CollectionUtils.isEmpty(allCategoryRefs)) {
            // 如果没有关联的问题分类，仅返回基本的扩展信息
            return questionExtMap.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> BeanUtil.copyProperties(entry.getValue(), QuestionCategoryBusinessSettingsDTO.class)
                    ));
        }

        // 按模块ID分组
        Map<Integer, List<QuestionCategoryBusinessRef>> categoryRefMap = allCategoryRefs.stream()
                .collect(Collectors.groupingBy(QuestionCategoryBusinessRef::getBusinessId));

        // 收集所有问题分类ID
        List<Integer> allQuestionCategoryIds = allCategoryRefs.stream()
                .map(QuestionCategoryBusinessRef::getQuestionCategoryId)
                .collect(Collectors.toList());

        // 查询问题分类信息
        Map<Integer, String> questionCategoryMap = Collections.emptyMap();
        List<QuestionCategory> questionCategoryList = questionCategoryMapper.selectBatchIds(allQuestionCategoryIds);
        if (CollectionUtils.isNotEmpty(questionCategoryList)) {
            questionCategoryMap = questionCategoryList.stream()
                    .collect(Collectors.toMap(QuestionCategory::getId, QuestionCategory::getName));
        }

        // 查询问题数量统计
        Map<Integer, Integer> questionCountMap = Collections.emptyMap();
        List<QuestionCountDTO> questionCountDTOList = questionMapper.countByCategoryIds(allQuestionCategoryIds);
        if (CollectionUtils.isNotEmpty(questionCountDTOList)) {
            questionCountMap = questionCountDTOList.stream()
                    .collect(Collectors.toMap(QuestionCountDTO::getQuestionCategoryId, QuestionCountDTO::getNum));
        }

        // 构建最终结果
        Map<Integer, String> finalQuestionCategoryMap = questionCategoryMap;
        Map<Integer, Integer> finalQuestionCountMap = questionCountMap;

        return businessIds.stream()
                .filter(questionExtMap::containsKey)
                .collect(Collectors.toMap(
                        businessId -> businessId,
                        businessId -> {
                            QuestionCategoryBusinessSettings ext = questionExtMap.get(businessId);
                            QuestionCategoryBusinessSettingsDTO extDTO = BeanUtil.copyProperties(ext, QuestionCategoryBusinessSettingsDTO.class);
                            List<QuestionCategoryBusinessRef> refs = categoryRefMap.getOrDefault(ext.getId(), Collections.emptyList());
                            if (CollectionUtils.isNotEmpty(refs)) {
                                List<QuestionCategoryBusinessRefDTO> refDTOList = BeanUtil.copyToList(refs, QuestionCategoryBusinessRefDTO.class);

                                for (QuestionCategoryBusinessRefDTO refDTO : refDTOList) {
                                    refDTO.setQuestionCategoryName(finalQuestionCategoryMap.get(refDTO.getQuestionCategoryId()));
                                    refDTO.setQuestionNum(finalQuestionCountMap.getOrDefault(refDTO.getQuestionCategoryId(), 0));
                                }

                                extDTO.setQuestionCategories(refDTOList);
                            }

                            return extDTO;
                        }
                ));
    }

    /**
     * 校验业务参数
     */
    private void validateBusinessParams(Integer businessId, Integer businessType) throws ServiceException {
        if (businessId == null) {
            throw new ServiceException(AppErrorCode.APP_BUSINESS_ID_NOT_NULL_MSG);
        }
        if (businessType == null) {
            throw new ServiceException(AppErrorCode.APP_BUSINESS_TYPE_NOT_NULL_MSG);
        }
        if (BusinessTypeEnum.isValid(businessType)) {
            throw new ServiceException(AppErrorCode.APP_BUSINESS_TYPE_INVALID_MSG);
        }
    }

    /**
     * 创建题库分类关联记录
     */
    private void createQuestionCategoryRefs(Integer businessId, Integer businessType,
                                          List<QuestionCategoryBusinessRefBO> questionCategories) {
        List<QuestionCategoryBusinessRef> refs = questionCategories.stream()
                .map(categoryBO -> {
                    QuestionCategoryBusinessRef ref = new QuestionCategoryBusinessRef();
                    ref.setQuestionCategoryId(categoryBO.getQuestionCategoryId());
                    ref.setBusinessId(businessId);
                    ref.setBusinessType(businessType);
                    ref.setSortNum(categoryBO.getSortNum());
                    return ref;
                })
                .collect(Collectors.toList());

        questionCategoryBusinessRefBiz.saveBatch(refs);
    }
}
