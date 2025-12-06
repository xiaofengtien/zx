package com.ruoyi.student.archive.utils;

import com.ruoyi.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 试卷编码工具类
 * 用于生成和解析paper_code
 * 
 * paper_code格式：{year}{month}_{province_code}_{paper_type}_{custom_name}
 * 示例：202312_hebei_junior_high_school_english_listening_模拟一
 * 
 * @author ruoyi
 */
@Slf4j
public class PaperCodeUtils {

    /**
     * 生成试卷编码
     * 
     * @param year 年份（2000-2050）
     * @param month 月份（1-12）
     * @param province 省份编码（字典值，如：hebei）
     * @param paperType 试卷类型（字典值，如：junior_high_school_english_listening）
     * @param customName 自定义名称（可选，如：模拟一）
     * @return 试卷编码
     */
    public static String generatePaperCode(Integer year, Integer month, String province, 
                                           String paperType, String customName) {
        if (year == null || month == null || StringUtils.isEmpty(province) || StringUtils.isEmpty(paperType)) {
            throw new IllegalArgumentException("年份、月份、省份、试卷类型不能为空");
        }

        // 验证年份范围
        if (year < 2000 || year > 2050) {
            throw new IllegalArgumentException("年份必须在2000-2050之间");
        }

        // 验证月份范围
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("月份必须在1-12之间");
        }

        // 构建paper_code
        // 格式：{year}{month}_{province_code}_{paper_type}_{custom_name}
        StringBuilder code = new StringBuilder();
        code.append(year);
        code.append(String.format("%02d", month)); // 月份补零，如：01, 02, ..., 12
        code.append("_");
        code.append(province);
        code.append("_");
        code.append(paperType);
        
        // 如果有自定义名称，添加到末尾
        if (StringUtils.isNotEmpty(customName)) {
            // 清理自定义名称中的特殊字符（只保留中文、英文、数字、下划线、横线）
            String cleanCustomName = customName.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9_-]", "");
            if (StringUtils.isNotEmpty(cleanCustomName)) {
                code.append("_");
                code.append(cleanCustomName);
            }
        }

        return code.toString();
    }

    /**
     * 解析试卷编码
     * 
     * @param paperCode 试卷编码
     * @return PaperCodeInfo 包含解析后的信息，如果解析失败返回null
     */
    public static PaperCodeInfo parsePaperCode(String paperCode) {
        if (StringUtils.isEmpty(paperCode)) {
            return null;
        }

        try {
            // 格式：{year}{month}_{province_code}_{paper_type}_{custom_name}
            // 示例：202312_hebei_junior_high_school_english_listening_模拟一
            
            String[] parts = paperCode.split("_", 4);
            if (parts.length < 3) {
                log.warn("试卷编码格式不正确，无法解析: {}", paperCode);
                return null;
            }

            // 第一部分：年月（如：202312）
            String yearMonth = parts[0];
            if (yearMonth.length() < 6) {
                log.warn("试卷编码年份月份部分格式不正确: {}", yearMonth);
                return null;
            }

            Integer year = Integer.parseInt(yearMonth.substring(0, 4));
            Integer month = Integer.parseInt(yearMonth.substring(4, 6));

            // 第二部分：省份编码
            String province = parts[1];

            // 第三部分：试卷类型
            String paperType = parts[2];

            // 第四部分：自定义名称（可选）
            String customName = parts.length > 3 ? parts[3] : null;

            return new PaperCodeInfo(year, month, province, paperType, customName);
        } catch (Exception e) {
            log.error("解析试卷编码失败: {}", paperCode, e);
            return null;
        }
    }

    /**
     * 从paper_code提取分组键（用于paper-package分组）
     * 分组键格式：{year}{month}_{province_code}_{paper_type}
     * 
     * @param paperCode 试卷编码
     * @return 分组键，如果解析失败返回null
     */
    public static String getGroupKey(String paperCode) {
        PaperCodeInfo info = parsePaperCode(paperCode);
        if (info == null) {
            return null;
        }

        // 分组键：年月_省份_类型
        return String.format("%d%02d_%s_%s", info.getYear(), info.getMonth(), info.getProvince(), info.getPaperType());
    }

    /**
     * 试卷编码信息
     */
    public static class PaperCodeInfo {
        private Integer year;
        private Integer month;
        private String province;
        private String paperType;
        private String customName;

        public PaperCodeInfo(Integer year, Integer month, String province, String paperType, String customName) {
            this.year = year;
            this.month = month;
            this.province = province;
            this.paperType = paperType;
            this.customName = customName;
        }

        public Integer getYear() {
            return year;
        }

        public Integer getMonth() {
            return month;
        }

        public String getProvince() {
            return province;
        }

        public String getPaperType() {
            return paperType;
        }

        public String getCustomName() {
            return customName;
        }
    }
}



