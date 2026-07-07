package com.zx.student.archive.domain.typehandler;

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

/**
 * List<String> 与数据库字符串（逗号分隔）的类型处理器
 * 
 * @author zx
 */
@MappedTypes({List.class})
@MappedJdbcTypes(JdbcType.VARCHAR)
public class ListStringTypeHandler extends BaseTypeHandler<List<String>>
{
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException
    {
        // 将 List 转换为逗号分隔的字符串存储到数据库
        if (parameter != null && !parameter.isEmpty())
        {
            ps.setString(i, String.join(",", parameter));
        }
        else
        {
            ps.setString(i, null);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException
    {
        // 从数据库读取逗号分隔字符串，转换为 List
        String value = rs.getString(columnName);
        return convertToList(value);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException
    {
        String value = rs.getString(columnIndex);
        return convertToList(value);
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException
    {
        String value = cs.getString(columnIndex);
        return convertToList(value);
    }

    /**
     * 将逗号分隔的字符串转换为 List
     */
    private List<String> convertToList(String value)
    {
        if (value == null || value.trim().isEmpty())
        {
            return new ArrayList<>();
        }
        List<String> list = new ArrayList<>();
        String[] items = value.split(",");
        for (String item : items)
        {
            String trimmed = item.trim();
            if (!trimmed.isEmpty())
            {
                list.add(trimmed);
            }
        }
        return list;
    }
}




