package com.ruoyi.student.archive.domain.typehandler;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import com.alibaba.fastjson2.JSON;

/**
 * List<Integer> 与数据库JSON字符串的类型处理器
 * 用于处理 applicable_paper_ids 字段（JSON数组格式，如：[1,2,3]）
 * 
 * @author ruoyi
 */
@MappedTypes({List.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public class ListIntegerTypeHandler extends BaseTypeHandler<List<Integer>>
{
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Integer> parameter, JdbcType jdbcType) throws SQLException
    {
        // 将 List<Integer> 转换为JSON字符串存储到数据库
        if (parameter != null && !parameter.isEmpty())
        {
            ps.setString(i, JSON.toJSONString(parameter));
        }
        else
        {
            ps.setString(i, "[]");
        }
    }

    @Override
    public List<Integer> getNullableResult(ResultSet rs, String columnName) throws SQLException
    {
        // 从数据库读取JSON字符串，转换为 List<Integer>
        String value = rs.getString(columnName);
        return convertToList(value);
    }

    @Override
    public List<Integer> getNullableResult(ResultSet rs, int columnIndex) throws SQLException
    {
        String value = rs.getString(columnIndex);
        return convertToList(value);
    }

    @Override
    public List<Integer> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException
    {
        String value = cs.getString(columnIndex);
        return convertToList(value);
    }

    /**
     * 将JSON字符串转换为 List<Integer>
     */
    private List<Integer> convertToList(String value)
    {
        if (value == null || value.trim().isEmpty() || value.trim().equals("[]"))
        {
            return new ArrayList<>();
        }
        
        try
        {
            // 尝试解析JSON数组
            List<Integer> list = JSON.parseArray(value, Integer.class);
            return list != null ? list : new ArrayList<>();
        }
        catch (Exception e)
        {
            // 如果JSON解析失败，尝试按逗号分隔处理（兼容旧数据）
            List<Integer> list = new ArrayList<>();
            String[] items = value.split(",");
            for (String item : items)
            {
                String trimmed = item.trim();
                if (!trimmed.isEmpty())
                {
                    try
                    {
                        list.add(Integer.parseInt(trimmed));
                    }
                    catch (NumberFormatException ex)
                    {
                        // 忽略无效的数字
                    }
                }
            }
            return list;
        }
    }
}



