package com.ruoyi.student.archive.biz.question;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.student.archive.domain.question.QuestionCategoryBusinessRef;

import java.util.List;

/**
 * <p>
 * 题库测验题库分类 服务类
 * </p>
 *
 * @author yatun
 * @since 2025-04-17
 */
public interface IQuestionCategoryBusinessRefBiz extends IService<QuestionCategoryBusinessRef> {

    /**
     * 检查分类是否被配套引用
     *
     * @param categoryIds 分类ID列表
     * @return true 如果存在配套引用
     */
    boolean hasBusinessRefs(List<Integer> categoryIds);

    /**
     * 根据业务ID和业务类型查询题库关联信息
     *
     * @param businessId 业务ID
     * @param businessType 业务类型
     * @return 题库关联信息列表
     * @throws BusinessException 业务异常
     */
    List<QuestionCategoryBusinessRef> getRefsByBusinessId(Integer businessId, Integer businessType) throws ServiceException;

    /**
     * 根据分类ID查询题库关联信息
     *
     * @return 题库关联信息列表
     * @throws ServiceException 业务异常
     */
    List<QuestionCategoryBusinessRef> getBusinessRefs(List<Integer> categoryIds);


}
