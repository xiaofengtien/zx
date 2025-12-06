package com.ruoyi.student.archive.domain.dto;

import java.util.List;

/**
 * 学员档案批量删除参数对象
 * 
 * @author ruoyi
 */
public class ArchiveBatchIdsDTO
{
    /** 学员档案ID列表 */
    private List<Long> ids;

    public List<Long> getIds()
    {
        return ids;
    }

    public void setIds(List<Long> ids)
    {
        this.ids = ids;
    }
}

