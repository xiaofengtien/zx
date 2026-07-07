package com.zx.common.utils;

import java.util.List;

/**
 * 树节点基类
 * 
 * @param <ID> 节点ID类型
 * @param <T> 节点类型
 * @author zx
 */
public class TreeVO<ID, T extends TreeVO<ID, T>>
{
    /**
     * 节点ID
     */
    protected ID id;

    /**
     * 父节点ID
     */
    protected ID parentId;

    /**
     * 子节点列表
     */
    protected List<T> children;

    public ID getId()
    {
        return id;
    }

    public void setId(ID id)
    {
        this.id = id;
    }

    public ID getParentId()
    {
        return parentId;
    }

    public void setParentId(ID parentId)
    {
        this.parentId = parentId;
    }

    public List<T> getChildren()
    {
        return children;
    }

    public void setChildren(List<T> children)
    {
        this.children = children;
    }
}



