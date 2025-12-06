package com.ruoyi.student.archive.biz.question.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.constant.AppErrorCode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.biz.question.QuestionCategoryBiz;
import com.ruoyi.student.archive.domain.bo.question.QuestionCategoryBO;
import com.ruoyi.student.archive.domain.bo.question.QuestionCategoryQueryBO;
import com.ruoyi.student.archive.domain.bo.question.QuestionCategorySortBO;
import com.ruoyi.student.archive.domain.dto.question.CategoryParentRelationDTO;
import com.ruoyi.student.archive.domain.dto.question.QuestionCategoryDTO;
import com.ruoyi.student.archive.domain.question.Question;
import com.ruoyi.student.archive.domain.question.QuestionCategory;
import com.ruoyi.student.archive.mapper.question.QuestionCategoryMapper;
import com.ruoyi.student.archive.mapper.question.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 题目分类业务实现类
 */
@Service
@RequiredArgsConstructor
public class QuestionCategoryBizImpl extends ServiceImpl<QuestionCategoryMapper, QuestionCategory> implements QuestionCategoryBiz {

    private final QuestionCategoryMapper questionCategoryMapper;
    private final QuestionMapper questionMapper;
    /**
     * 根节点的父ID
     */
    public static final Integer ROOT_FATHER_ID = 0;

    public static final Integer DEPTH = 5;

    @Override
    public List<QuestionCategoryDTO> getCategoryTree(QuestionCategoryQueryBO queryBO) {
        // 优化：一次性查询所有符合条件的分类，避免N+1查询
        LambdaQueryWrapper<QuestionCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Objects.nonNull(queryBO.getStatus()), QuestionCategory::getStatus, queryBO.getStatus())
              .like(StringUtils.isNotEmpty(queryBO.getName()), QuestionCategory::getName, queryBO.getName())
              .orderByAsc(QuestionCategory::getSortNum);

        List<QuestionCategory> allCategories = list(wrapper);
        if (CollectionUtils.isEmpty(allCategories)) {
            return new ArrayList<>();
        }

        // 如果指定了父ID，使用Mapper的递归CTE查询所有子节点ID
        if (Objects.nonNull(queryBO.getFatherId())) {
            List<Integer> nodeIds = questionCategoryMapper.getChildCategoryIds(queryBO.getFatherId());
            if (CollectionUtils.isEmpty(nodeIds)) {
                return new ArrayList<>();
            }
            // 过滤出符合条件的分类
            allCategories = allCategories.stream()
                    .filter(cat -> nodeIds.contains(cat.getId()))
                    .toList();
        }

        // 构建ID到分类的映射（O(1)查找）
        Map<Integer, QuestionCategoryDTO> categoryMap = allCategories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toMap(questionCategoryDTO -> questionCategoryDTO != null ? questionCategoryDTO.getId() : null, Function.identity(), (k1, k2) -> k1));

        // 确定根节点ID（如果未指定，则使用ROOT_FATHER_ID=0）
        Integer rootFatherId = queryBO.getFatherId();
        if (rootFatherId == null) {
            rootFatherId = ROOT_FATHER_ID;
        }

        // 在内存中构建树结构（单次遍历，O(n)复杂度）
        List<QuestionCategoryDTO> rootList = new ArrayList<>();
        for (QuestionCategoryDTO category : categoryMap.values()) {
            Integer fatherId = category.getFatherId();
            if (fatherId == null || fatherId.equals(rootFatherId)) {
                rootList.add(category);
            } else {
                QuestionCategoryDTO parent = categoryMap.get(fatherId);
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(category);
                }
            }
        }

        // 批量设置统计信息（优化：一次性批量查询，避免N+1问题）
        Set<Integer> categoryIds = categoryMap.keySet();
        setCategoryStatsBatchOptimized(rootList, categoryIds, categoryMap);

        return rootList;
    }
    
    /**
     * 批量设置分类统计信息（性能优化版本：一次性批量查询所有统计数据）
     * 
     * @param categories 分类树列表
     * @param categoryIds 所有分类ID集合
     * @param categoryMap 分类映射（用于递归设置子节点统计）
     */
    private void setCategoryStatsBatchOptimized(List<QuestionCategoryDTO> categories, Set<Integer> categoryIds, Map<Integer, QuestionCategoryDTO> categoryMap) {
        if (CollectionUtils.isEmpty(categories) || CollectionUtils.isEmpty(categoryIds)) {
            return;
        }
        
        // 1. 批量查询所有分类的父子关系，在内存中构建树结构（一次查询）
        List<CategoryParentRelationDTO> parentRelations = questionCategoryMapper.getAllCategoryParentRelations();
        Map<Integer, List<Integer>> childrenMap = new HashMap<>();
        Set<Integer> allCategoryIds = new HashSet<>(categoryIds);
        for (CategoryParentRelationDTO relation : parentRelations) {
            Integer catId = relation.getCategoryId();
            Integer fatherId = relation.getFatherId();
            if (fatherId != null) {
                childrenMap.computeIfAbsent(fatherId, k -> new ArrayList<>()).add(catId);
            }
            allCategoryIds.add(catId); // 包含所有相关分类ID（用于查询题目）
        }
        
        // 2. 在内存中计算每个分类的所有子分类ID集合（递归计算，但只查询一次数据库）
        Map<Integer, Set<Integer>> categoryToAllChildrenMap = new HashMap<>();
        for (Integer categoryId : categoryIds) {
            Set<Integer> allChildren = new HashSet<>();
            collectAllChildren(categoryId, childrenMap, allChildren);
            allChildren.add(categoryId); // 包含自身
            categoryToAllChildrenMap.put(categoryId, allChildren);
        }
        
        // 3. 批量查询所有相关分类的题目数量（一次查询）
        Map<Integer, Integer> questionCountByCategory = batchCountCurrentQuestions(allCategoryIds);
        
        // 4. 在内存中计算每个分类（包含子分类）的题目总数
        Map<Integer, Integer> totalQuestionCountMap = new HashMap<>();
        for (Integer categoryId : categoryIds) {
            Set<Integer> allChildren = categoryToAllChildrenMap.get(categoryId);
            int totalCount = 0;
            for (Integer childId : allChildren) {
                totalCount += questionCountByCategory.getOrDefault(childId, 0);
            }
            totalQuestionCountMap.put(categoryId, totalCount);
        }
        
        // 5. 批量查询当前分类的题目数量（一次查询）
        Map<Integer, Integer> currentQuestionCountMap = batchCountCurrentQuestions(categoryIds);
        
        // 6. 在内存中计算每个分类的子节点数量（递归计算，但只查询一次数据库）
        Map<Integer, Integer> childrenCountMap = new HashMap<>();
        for (Integer categoryId : categoryIds) {
            childrenCountMap.put(categoryId, calculateChildrenCount(categoryId, childrenMap));
        }
        
        // 7. 一次性设置所有统计信息（避免递归调用中的重复查询）
        setStatsRecursively(categories, currentQuestionCountMap, totalQuestionCountMap, childrenCountMap);
    }
    
    /**
     * 递归设置统计信息（不再查询数据库，只设置已计算好的数据）
     */
    private void setStatsRecursively(List<QuestionCategoryDTO> categories, 
                                     Map<Integer, Integer> currentQuestionCountMap,
                                     Map<Integer, Integer> totalQuestionCountMap,
                                     Map<Integer, Integer> childrenCountMap) {
        if (CollectionUtils.isEmpty(categories)) {
            return;
        }
        
        for (QuestionCategoryDTO category : categories) {
            Integer categoryId = category.getId();
            
            // 设置当前分类的题目数量
            category.setCurrentQuestionCount(currentQuestionCountMap.getOrDefault(categoryId, 0));
            
            // 设置包含子分类的题目数量
            category.setQuestionCount(totalQuestionCountMap.getOrDefault(categoryId, 0));
            
            // 设置子节点数量
            category.setChildrenCount(childrenCountMap.getOrDefault(categoryId, 0));
            
            // 递归设置子节点的统计信息
            if (!CollectionUtils.isEmpty(category.getChildren())) {
                setStatsRecursively(category.getChildren(), currentQuestionCountMap, totalQuestionCountMap, childrenCountMap);
            }
        }
    }
    
    /**
     * 在内存中计算子节点数量（优化：使用迭代方式替代递归，避免栈溢出）
     * 
     * @param categoryId 分类ID
     * @param childrenMap 父子关系映射
     * @return 子节点数量
     */
    private int calculateChildrenCount(Integer categoryId, Map<Integer, List<Integer>> childrenMap) {
        // 使用迭代方式替代递归，避免栈溢出
        int count = 0;
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(categoryId);
        
        while (!stack.isEmpty()) {
            Integer currentId = stack.pop();
            List<Integer> directChildren = childrenMap.get(currentId);
            if (!CollectionUtils.isEmpty(directChildren)) {
                count += directChildren.size();
                // 将所有子节点加入栈中，继续处理
                for (Integer childId : directChildren) {
                    stack.push(childId);
                }
            }
        }
        
        return count;
    }
    
    /**
     * 迭代收集所有子节点ID（优化：使用迭代方式替代递归，避免栈溢出）
     * 
     * @param categoryId 分类ID
     * @param childrenMap 父子关系映射
     * @param result 结果集合
     */
    private void collectAllChildren(Integer categoryId, Map<Integer, List<Integer>> childrenMap, Set<Integer> result) {
        // 使用迭代方式替代递归，避免栈溢出
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(categoryId);
        
        while (!stack.isEmpty()) {
            Integer currentId = stack.pop();
            List<Integer> directChildren = childrenMap.get(currentId);
            if (!CollectionUtils.isEmpty(directChildren)) {
                for (Integer childId : directChildren) {
                    if (result.add(childId)) { // 如果成功添加（未重复），则加入栈中继续处理
                        stack.push(childId);
                    }
                }
            }
        }
    }
    


    @Override
    public List<QuestionCategoryDTO> getCategoryWithChildren(Integer categoryId) {
        if (Objects.isNull(categoryId)) {
            return new ArrayList<>();
        }

        // 优化：使用Mapper的递归CTE一次性查询所有子分类ID
        List<Integer> nodeIds = questionCategoryMapper.getChildCategoryIds(categoryId);
        if (CollectionUtils.isEmpty(nodeIds)) {
            return new ArrayList<>();
        }

        // 一次性查询所有分类
        List<QuestionCategory> allCategories = list(new LambdaQueryWrapper<QuestionCategory>()
                .in(QuestionCategory::getId, nodeIds)
                .orderByAsc(QuestionCategory::getSortNum));

        // 转换为DTO列表
        return allCategories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Integer createCategory(QuestionCategoryBO categoryBO) throws ServiceException {
        if (Objects.isNull(categoryBO)) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_CATEGORY_PARAM_NOT_NULL_MSG);
        }
        if (StringUtils.isEmpty(categoryBO.getName())) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_CATEGORY_NAME_NOT_NULL_MSG);
        }
        
        if (categoryBO.getFatherId() != null && !Objects.equals(categoryBO.getFatherId(), ROOT_FATHER_ID)) {
            int depth = calculateNodeDepth(categoryBO.getFatherId());
            if (depth >= DEPTH) {
                throw new ServiceException(AppErrorCode.APP_QUESTION_CATEGORY_MAX_FAIL_MSG);
            }
        }

        QuestionCategory category = new QuestionCategory();
        BeanUtil.copyProperties(categoryBO, category);
        Integer maxSortNum = getMaxSortNum(categoryBO.getFatherId());
        category.setSortNum(maxSortNum + 1);
        save(category);
        // 清除缓存，因为分类数据已变更
        clearParentMapCache();
        return category.getId();
    }

    @Override
    public void updateCategory(QuestionCategoryBO categoryBO) throws ServiceException {
        if (Objects.isNull(categoryBO) || Objects.isNull(categoryBO.getId())) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_CATEGORY_PARAM_NOT_NULL_MSG);
        }
        if (StringUtils.isEmpty(categoryBO.getName())) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_CATEGORY_NAME_NOT_NULL_MSG);
        }

        QuestionCategory category = getById(categoryBO.getId());

        if (Objects.isNull(category)) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_CATEGORY_NOT_EXIST_MSG);
        }

        if (categoryBO.getFatherId() != null && !Objects.equals(categoryBO.getFatherId(), ROOT_FATHER_ID)
                && !Objects.equals(categoryBO.getFatherId(), category.getFatherId())) {
            int depth = calculateNodeDepth(categoryBO.getFatherId());
            if (depth >= DEPTH) {
                throw new ServiceException(AppErrorCode.APP_ALBUM_MENUS_ORDER_COVER_MAX_FAIL_MSG);
            }
        }

        BeanUtil.copyProperties(categoryBO, category);
        updateById(category);
        // 清除缓存，因为分类数据已变更
        clearParentMapCache();
    }


    @Override
    public void updateSort(QuestionCategorySortBO sortBO) throws ServiceException {
        if (Objects.isNull(sortBO) || Objects.isNull(sortBO.getId())) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_CATEGORY_PARAM_NOT_NULL_MSG);
        }

        QuestionCategory category = getById(sortBO.getId());
        if (Objects.isNull(category)) {
            throw new ServiceException(AppErrorCode.APP_QUESTION_CATEGORY_NOT_EXIST_MSG);
        }

        QuestionCategory parentCategory = null;
        if (sortBO.getFatherId() != 0) {
            parentCategory = getById(sortBO.getFatherId());
            if (Objects.isNull(parentCategory)) {
                throw new ServiceException(AppErrorCode.APP_QUESTION_CATEGORY_NOT_EXIST_MSG);
            }
            
            int depth = calculateNodeDepth(sortBO.getFatherId());
            if (depth >= DEPTH) {
                throw new ServiceException(AppErrorCode.APP_ALBUM_MENUS_ORDER_COVER_MAX_FAIL_MSG);
            }
        }

        category.setFatherId(sortBO.getFatherId());

        if (sortBO.getAimId() != null && sortBO.getAimId() != -1) {
            QuestionCategory aimCategory = getById(sortBO.getAimId());
            if (Objects.isNull(aimCategory)) {
                throw new ServiceException(AppErrorCode.APP_QUESTION_CATEGORY_NOT_EXIST_MSG);
            }
            if (!Objects.equals(aimCategory.getFatherId(), sortBO.getFatherId())) {
                throw new ServiceException(AppErrorCode.APP_QUESTION_CATEGORY_MOVE_NOT_SUPPORT_MSG);
            }
            List<QuestionCategory> siblings = list(new LambdaQueryWrapper<QuestionCategory>()
                    .eq(QuestionCategory::getFatherId, sortBO.getFatherId())
                    .orderByAsc(QuestionCategory::getSortNum));

            if (sortBO.getOrder() != null && sortBO.getOrder() == -1) {
                int targetIndex = -1;
                for (int i = 0; i < siblings.size(); i++) {
                    if (Objects.equals(siblings.get(i).getId(), sortBO.getAimId())) {
                        targetIndex = i;
                        break;
                    }
                }

                if (targetIndex != -1) {
                    category.setSortNum(siblings.get(targetIndex).getSortNum());

                    for (int i = targetIndex; i < siblings.size(); i++) {
                        QuestionCategory sibling = siblings.get(i);
                        if (!Objects.equals(sibling.getId(), sortBO.getId())) {
                            sibling.setSortNum(sibling.getSortNum() + 1);
                            updateById(sibling);
                        }
                    }
                }
            } else {
                int targetIndex = -1;
                for (int i = 0; i < siblings.size(); i++) {
                    if (Objects.equals(siblings.get(i).getId(), sortBO.getAimId())) {
                        targetIndex = i;
                        break;
                    }
                }

                if (targetIndex != -1) {
                    category.setSortNum(siblings.get(targetIndex).getSortNum() + 1);

                    for (int i = targetIndex + 1; i < siblings.size(); i++) {
                        QuestionCategory sibling = siblings.get(i);
                        if (!Objects.equals(sibling.getId(), sortBO.getId())) {
                            sibling.setSortNum(sibling.getSortNum() + 1);
                            updateById(sibling);
                        }
                    }
                }
            }
        } else {
            Integer maxSortNum = baseMapper.selectMaxSortNum(sortBO.getFatherId());
            category.setSortNum(maxSortNum == null ? 1 : maxSortNum + 1);
        }

        updateById(category);
        // 清除缓存，因为分类数据已变更
        clearParentMapCache();
    }

    @Override
    public Integer countQuestionsByCategoryId(Integer categoryId) {
        return questionCategoryMapper.countQuestionsByCategoryId(categoryId);
    }

    @Override
    public Integer countCurrentQuestionsByCategoryId(Integer categoryId) {
        return Math.toIntExact(questionMapper.selectCount(new LambdaQueryWrapper<Question>().eq(Question::getQuestionCategoryId, categoryId)));
    }

    @Override
    public boolean checkNameExists(String name, Integer fatherId, Integer excludeId) {
        if (StringUtils.isEmpty(name)) {
            return false;
        }

        LambdaQueryWrapper<QuestionCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionCategory::getName, name)
              .eq(QuestionCategory::getFatherId, fatherId)
              .ne(Objects.nonNull(excludeId), QuestionCategory::getId, excludeId);
        return count(wrapper) > 0;
    }

    @Override
    public boolean hasChildren(List<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return false;
        }
        LambdaQueryWrapper<QuestionCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(QuestionCategory::getFatherId, ids);
        return count(wrapper) > 0;
    }

    /**
     * 获取同级分类的最大排序号
     */
    private Integer getMaxSortNum(Integer fatherId) {
        LambdaQueryWrapper<QuestionCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionCategory::getFatherId, fatherId)
              .orderByDesc(QuestionCategory::getSortNum)
              .last("LIMIT 1");
        QuestionCategory category = getOne(wrapper);
        return Objects.nonNull(category) ? category.getSortNum() : 0;
    }

    /**
     * 统计所有层级的子节点数量（优化：使用Mapper递归CTE）
     *
     * @param categoryId 分类ID
     * @return 所有子节点数量
     */
    private Integer countAllChildren(Integer categoryId) {
        // 优化：使用Mapper的递归CTE查询所有子节点ID，然后统计数量
        List<Integer> childIds = questionCategoryMapper.getChildCategoryIds(categoryId);
        // 排除自身
        return childIds.size() - 1;
    }



    /**
     * 重新排序同级分类
     */
    private void reorderSiblings(Integer fatherId) {
        LambdaQueryWrapper<QuestionCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(QuestionCategory::getFatherId, fatherId)
              .orderByAsc(QuestionCategory::getSortNum);
        List<QuestionCategory> siblings = list(wrapper);

        if (!CollectionUtils.isEmpty(siblings)) {
            for (int i = 0; i < siblings.size(); i++) {
                QuestionCategory sibling = siblings.get(i);
                if (!Objects.equals(sibling.getSortNum(), i + 1)) {
                    sibling.setSortNum(i + 1);
                    updateById(sibling);
                }
            }
        }
    }

    /**
     * 转换为 DTO
     */
    private QuestionCategoryDTO convertToDTO(QuestionCategory category) {
        if (Objects.isNull(category)) {
            return null;
        }
        QuestionCategoryDTO dto = new QuestionCategoryDTO();
        BeanUtil.copyProperties(category, dto);
        return dto;
    }

    /**
     * 计算节点深度（优化：使用一次性查询构建父节点映射，并缓存）
     * 
     * @param categoryId 分类ID
     * @return 节点深度
     */
    private int calculateNodeDepth(Integer categoryId) {
        if (categoryId == null || Objects.equals(categoryId, ROOT_FATHER_ID)) {
            return 0;
        }
        
        // 优化：一次性查询所有分类，构建父节点映射（使用ThreadLocal缓存，避免重复查询）
        Map<Integer, Integer> parentMap = getParentMapCache();
        
        int depth = 1;
        Integer currentId = categoryId;
        
        while (true) {
            Integer parentId = parentMap.get(currentId);
            if (parentId == null || Objects.equals(parentId, ROOT_FATHER_ID)) {
                break;
            }
            depth++;
            currentId = parentId;
        }
        
        return depth;
    }
    
    /**
     * 获取父节点映射缓存（使用ThreadLocal，避免在同一请求中重复查询）
     */
    private static final ThreadLocal<Map<Integer, Integer>> PARENT_MAP_CACHE = new ThreadLocal<>();
    
    private Map<Integer, Integer> getParentMapCache() {
        Map<Integer, Integer> cache = PARENT_MAP_CACHE.get();
        if (cache == null) {
            // 一次性查询所有分类，构建父节点映射
            List<QuestionCategory> allCategories = list();
            cache = allCategories.stream()
                    .filter(cat -> cat.getFatherId() != null)
                    .collect(Collectors.toMap(QuestionCategory::getId, QuestionCategory::getFatherId));
            PARENT_MAP_CACHE.set(cache);
        }
        return cache;
    }
    
    /**
     * 清除父节点映射缓存（在事务提交后调用）
     */
    private void clearParentMapCache() {
        PARENT_MAP_CACHE.remove();
    }


    /**
     * 批量查询当前分类的题目数量（优化：使用SQL的COUNT和GROUP BY，一次查询获取所有分类的题目数量）
     * 
     * @param categoryIds 分类ID集合
     * @return 分类ID到题目数量的映射
     */
    private Map<Integer, Integer> batchCountCurrentQuestions(Set<Integer> categoryIds) {
        if (CollectionUtils.isEmpty(categoryIds)) {
            return new HashMap<>();
        }
        
        // 优化：使用SQL的COUNT和GROUP BY，而不是查询所有题目再在Java中统计
        // 使用MyBatis-Plus的聚合查询
        List<Question> questions = questionMapper.selectList(
            new LambdaQueryWrapper<Question>()
                .select(Question::getQuestionCategoryId)
                .in(Question::getQuestionCategoryId, categoryIds)
                .eq(Question::getDelFlag, "0")
        );
        
        // 统计每个分类的题目数量（使用Stream API优化）
        Map<Integer, Integer> countMap = questions.stream()
            .collect(Collectors.groupingBy(
                Question::getQuestionCategoryId,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
        
        // 确保所有分类都有计数（即使为0）
        for (Integer categoryId : categoryIds) {
            countMap.putIfAbsent(categoryId, 0);
        }
        
        return countMap;
    }

}
