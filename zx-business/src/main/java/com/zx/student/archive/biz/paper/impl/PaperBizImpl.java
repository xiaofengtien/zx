package com.zx.student.archive.biz.paper.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zx.common.exception.ServiceException;
import com.zx.common.utils.SecurityUtils;
import com.zx.common.utils.StringUtils;
import com.zx.student.archive.biz.paper.IPaperBiz;
import com.zx.student.archive.domain.paper.Paper;
import com.zx.student.archive.mapper.paper.PaperMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 试卷业务实现类
 * 
 * @author zx
 */
@Slf4j
@Service
public class PaperBizImpl extends ServiceImpl<PaperMapper, Paper> implements IPaperBiz {

    @Override
    public Paper getByPaperCode(String paperCode) {
        if (StringUtils.isEmpty(paperCode)) {
            return null;
        }
        return baseMapper.selectByPaperCode(paperCode);
    }

    @Override
    public Paper getByYearMonthProvinceType(Integer year, Integer month, String province, String paperType) {
        if (year == null || month == null || StringUtils.isEmpty(province) || StringUtils.isEmpty(paperType)) {
            return null;
        }
        return baseMapper.selectByYearMonthProvinceType(year, month, province, paperType);
    }

    @Override
    public List<Paper> listByBusiness(Integer businessType, Integer businessId) {
        return baseMapper.selectByBusiness(businessType, businessId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer createPaper(Paper paper) throws ServiceException {
        // 生成试卷编码
        if (StringUtils.isEmpty(paper.getPaperCode())) {
            paper.setPaperCode(generatePaperCode());
        }
        
        // 设置创建者
        if (StringUtils.isEmpty(paper.getCreateBy())) {
            paper.setCreateBy(SecurityUtils.getUsername());
        }
        
        // 设置默认值
        if (paper.getBusinessType() == null) {
            paper.setBusinessType(5); // 默认业务类型为5（题库）
        }
        if (paper.getStatus() == null) {
            paper.setStatus(1); // 默认启用
        }
        if (paper.getVersion() == null) {
            paper.setVersion(1); // 默认版本为1
        }
        if (paper.getAutoNextQuestion() == null) {
            paper.setAutoNextQuestion(1); // 默认自动跳转
        }
        if (paper.getShowAnswerImmediately() == null) {
            paper.setShowAnswerImmediately(0); // 默认不立即显示答案
        }
        if (paper.getAllowReview() == null) {
            paper.setAllowReview(1); // 默认允许回顾
        }
        
        // 保存试卷
        boolean success = this.save(paper);
        if (!success) {
            throw new ServiceException("创建试卷失败");
        }
        
        return paper.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePaper(Paper paper) throws ServiceException {
        if (paper.getId() == null) {
            throw new ServiceException("试卷ID不能为空");
        }
        
        // 设置更新者
        paper.setUpdateBy(SecurityUtils.getUsername());
        
        // 使用 lambdaUpdate 确保所有字段都被更新（包括 null 值）
        // 注意：这里只更新非 null 字段，如果需要更新 null 值，需要使用其他方式
        boolean success = this.updateById(paper);
        if (!success) {
            throw new ServiceException("更新试卷失败");
        }
        
        // 如果 paperType 或 paperDesc 为 null，需要单独更新
        // 但通常 updateById 会更新所有非 null 字段，所以这里主要是确保字段被正确传递
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeletePaper(List<Integer> ids) throws ServiceException {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("试卷ID列表不能为空");
        }
        
        // 逻辑删除
        this.lambdaUpdate()
            .in(Paper::getId, ids)
            .set(Paper::getDelFlag, "2") // 逻辑删除标志
            .set(Paper::getUpdateBy, SecurityUtils.getUsername())
            .set(Paper::getUpdateTime, new Date())
            .update();
    }

    @Override
    public String generatePaperCode() {
        // 旧格式：PAPER_YYYYMMDD_序号（向后兼容）
        // 新格式：{year}{month}_{province_code}_{paper_type}_{custom_name}
        // 注意：此方法已废弃，新的生成逻辑在PaperServiceImpl中
        // 保留此方法用于向后兼容
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());
        String prefix = "PAPER_" + dateStr + "_";
        
        // 查询当天已生成的试卷数量
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Paper::getPaperCode, prefix);
        long count = this.count(wrapper);
        
        // 生成序号（从1开始，3位数字）
        String sequence = String.format("%03d", count + 1);
        
        return prefix + sequence;
    }
}

