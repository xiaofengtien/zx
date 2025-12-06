package com.ruoyi.common.utils;

import java.util.*;

/**
 * 树结构工具类
 * 
 * @author ruoyi
 */
public class TreeUtil
{
    /**
     * 将列表转换为树结构
     * 
     * @param list 列表数据
     * @param <T> 树节点类型，必须继承TreeVO
     * @return 树结构列表
     */
    public static <ID, T extends TreeVO<ID, T>> List<T> convertTree(List<T> list)
    {
        if (list == null || list.isEmpty())
        {
            return new ArrayList<>();
        }

        // 使用Map存储所有节点，key为节点ID
        Map<ID, T> nodeMap = new HashMap<>();
        for (T node : list)
        {
            nodeMap.put(node.getId(), node);
            if (node.getChildren() == null)
            {
                node.setChildren(new ArrayList<>());
            }
        }

        // 构建树结构
        List<T> rootList = new ArrayList<>();
        for (T node : list)
        {
            ID parentId = node.getParentId();
            if (parentId == null || !nodeMap.containsKey(parentId))
            {
                // 根节点
                rootList.add(node);
            }
            else
            {
                // 子节点，添加到父节点的children中
                T parent = nodeMap.get(parentId);
                if (parent != null)
                {
                    parent.getChildren().add(node);
                }
            }
        }

        return rootList;
    }
}

