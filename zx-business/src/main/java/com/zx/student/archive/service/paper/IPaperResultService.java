package com.zx.student.archive.service.paper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zx.common.exception.ServiceException;
import com.zx.student.archive.domain.bo.paper.PaperResultPageBO;
import com.zx.student.archive.domain.dto.paper.PaperQuestionResultDTO;
import com.zx.student.archive.domain.dto.paper.PaperResultListDTO;

/**
 * 答题结果服务接口
 * 
 * @author zx
 */
public interface IPaperResultService {
    
    /**
     * 分页查询答题结果列表
     * 
     * @param pageBO 分页查询参数
     * @return 分页结果
     */
    Page<PaperResultListDTO> pageList(PaperResultPageBO pageBO);
    
    /**
     * 获取答题结果详情
     * 
     * @param id 答题记录ID
     * @return 答题结果详情
     * @throws ServiceException 业务异常
     */
    PaperQuestionResultDTO getPaperResultDetail(Integer id) throws ServiceException;
}



