package com.ruoyi.common.constant;

import lombok.Data;

/**
 * @author yatun
 * @version 1.0
 * @program dbj-classpal-admin-bus
 * @className ErroeCode
 * @description admin的错误编码由 ADMIN_1000000 开始 一直向后编写
 * @date 2025-03-12 14:01
 **/
@Data
public class AppErrorCode {

    /**-----------------------------------图书 start -----------------------------------------*/
    public static final String BOOK_INFO_NOT_EXIST_CODE = "BOOK_INFO_ERROR_10000001";
    public static final String BOOK_INFO_NOT_EXIST_MSG = "图书信息不存在";

    public static final String BOOK_INFO_RANK_CONTENTS_NOT_EXIST_CODE = "BOOK_INFO_RANK_CONTENTS_ERROR_10000001";
    public static final String BOOK_INFO_RANK_CONTENTS_NOT_EXIST_MSG = "当前目录信息不存在";
    public static final String BOOK_INFO_RANK_CONTENTS_EXIST_CHILDREN_CODE = "BOOK_INFO_RANK_CONTENTS_ERROR_10000002";
    public static final String BOOK_INFO_RANK_CONTENTS_NOT_EXIST_CHILDREN_MSG = "该目录下存在书内码，不允许被删除";
    public static final String BOOK_INFO_RANK_CONTENTS_DIRECTORY_NOT_CHANGE_CODE = "BOOK_INFO_RANK_CONTENTS_ERROR_10000003";
    public static final String BOOK_INFO_RANK_CONTENTS_DIRECTORY_NOT_CHANGE_MSG = "当前为目录不允许修改类型";
    public static final String BOOK_INFO_RANK_CONTENTS_NOT_CHANGE_CODE = "BOOK_INFO_RANK_CONTENTS_ERROR_10000004";
    public static final String BOOK_INFO_RANK_CONTENTS_NOT_CHANGE_MSG = "类型未发生变化,请确认后重试";
    public static final String BOOK_INFO_RANK_CONTENTS_TYPE_CODE = "BOOK_INFO_RANK_CONTENTS_ERROR_10000005";
    public static final String BOOK_INFO_RANK_CONTENTS_TYPE_MSG = "资源类型错误,请确认后重试";

    public static final String BOOK_INFO_RANK_CONTENTS_QUESTION_ERROR_CODE = "BOOK_INFO_RANK_CONTENTS_QUESTION_ERROR_10000001";
    public static final String BOOK_INFO_RANK_CONTENTS_QUESTION_ERROR_MSG = "章节发生变化,请确认后重试";


    public static final String BOOK_INFO_RANK_CONTENTS_FATHER_NOT_EXIST_CODE = "BOOK_INFO_RANK_CONTENTS_ERROR_10000006";
    public static final String BOOK_INFO_RANK_CONTENTS_FATHER_NOT_EXIST_MSG = "移动后的父目录信息不存在";
    public static final String BOOK_INFO_RANK_CONTENTS_FATHER_NOT_DIRECTORY_CODE = "BOOK_INFO_RANK_CONTENTS_ERROR_10000007";
    public static final String BOOK_INFO_RANK_CONTENTS_FATHER_NOT_DIRECTORY_MSG = "只能移动到目录下";


    public static final String BOOK_RANK_INFO_NOT_EXIST_CODE = "BOOK_RANK_INFO_ERROR_10000001";
    public static final String BOOK_RANK_INFO_NOT_EXIST_MSG = "册书信息不存在,请确认后重试";

    //存在业务数据关联，不能删除
    public static final String APP_QUESTION_HAS_REF_CODE = "APP_QUESTION_CATEGORY_HAS_BUSINESS_10000038";
    public static final String APP_QUESTION_HAS_REF_MSG = "存在业务数据关联，不能删除";

    public static final String BOOK_COMPATIBILITY_EXIST_CODE = "BOOK_COMPATIBILITY_ERROR_10000001";
    public static final String BOOK_COMPATIBILITY_EXIST_MSG = "该链接已存在,请确认后重试";

    public static final String BOOK_COMPATIBILITY_NOT_EXIST_CODE = "BOOK_COMPATIBILITY_ERROR_10000002";
    public static final String BOOK_COMPATIBILITY_NOT_EXIST_MSG = "数据不存在,请确认后重试";

    public static final String BOOK_COMPATIBILITY_INVALID_CODE = "BOOK_COMPATIBILITY_ERROR_10000003";
    public static final String BOOK_COMPATIBILITY_INVALID_MSG = "无效的二维码,请确认后重试";

    public static final String BOOK_CATEGORY_SYNC_CODE = "BOOK_CATEGORY_ERROR_10000001";
    public static final String BOOK_CATEGORY_SYNC_MSG = "正在同步中，请三分钟后在重试";

    public static final String BOOK_SAVE_MATERIAL_ONLY_PDF_CODE = "BOOK_SAVE_MATERIAL_ONLY_PDF_10000001";
    public static final String BOOK_SAVE_MATERIAL_ONLY_PDF_MSG = "图书资源文档类型只支持pdf,请确认后重试";

    public static final String BOOK_BUY_CHECKED_CODE = "BOOK_BUY_CHECKED_10000001";

    public static final String BOOK_RECEIVE_CHECKED_CODE = "BOOK_RECEIVE_CHECKED_10000001";

    /**-----------------------------------图书 end -----------------------------------------*/

    /**-----------------------------------素材中心 start -----------------------------------------*/
    public static final String FILE_TYPE_IMPORT_FAIL_CODE = "FILE_TYPE_IMPORT_FAIL_10000001";
    public static final String FILE_TYPE_IMPORT_FAIL_MSG = "文件格式不支持,请使用rar或zip打包上传";

    public static final String FILE_ANALYSIS_ERROR_CODE = "FILE_ANALYSIS_ERROR_10000001";
    public static final String FILE_ANALYSIS_ERROR_MSG = "文件上传分析模板失败";

    public static final String FILE_CONVERSION_ERROR_CODE = "FILE_CONVERSION_ERROR_10000001";
    public static final String FILE_CONVERSION_ERROR_MSG = "文件上传转码失败";

    public static final String FILE_DIRECTORY_NOT_EXIST_CODE = "FILE_DIRECTORY_NOT_EXIST_10000001";
    public static final String FILE_DIRECTORY_NOT_EXIST_MSG = "文件上传失败,文件夹不存在";

    public static final String FILE_SAME_PROCESSING_EXIST_CODE = "FILE_SAME_PROCESSING_EXIST_10000001";
    public static final String FILE_SAME_PROCESSING_EXIST_MSG = "存在相同正在处理中的文件,请处理完后再试";


    public static final String EXCEL_FILE_IMPORT_UPDATE_FAIL_CODE = "EXCEL_FILE_IMPORT_UPDATE_FAIL_10000001";
    public static final String EXCEL_FILE_IMPORT_UPDATE_FAIL_MSG = "修改文件状态失败,请确认后重试";

    public static final String APP_MATERIAL_SAVE_FAIL_CODE = "APP_MATERIAL_SAVE_FAIL_10000001";
    public static final String APP_MATERIAL_SAVE_FAIL_MSG = "上传资源失败,请确认后重试";

    public static final String APP_MATERIAL_MOVE_FAIL_SAME_DIR_CODE = "APP_MATERIAL_MOVE_FAIL_SAME_DIR_10000001";
    public static final String APP_MATERIAL_MOVE_FAIL_SAME_DIR_MSG = "目标文件夹和当前文件夹一致,移动失败";

    public static final String APP_MATERIAL_COPY_FAIL_SAME_DIR_CODE = "APP_MATERIAL_COPY_FAIL_SAME_DIR_10000001";
    public static final String APP_MATERIAL_COPY_FAIL_SAME_DIR_MSG = "目标文件夹和当前文件夹一致,复制失败";

    public static final String APP_MATERIAL_MOVE_FAIL_CHILDREN_DIR_CODE = "APP_MATERIAL_MOVE_FAIL_CHILDREN_DIR_10000001";
    public static final String APP_MATERIAL_MOVE_FAIL_CHILDREN_DIR_MSG = "目标目录不能是子目录,移动失败";

    public static final String APP_MATERIAL_MOVE_EXIST_CONTAINS_REF_CODE = "APP_MATERIAL_MOVE_EXIST_CONTAINS_REF_10000001";
    public static final String APP_MATERIAL_MOVE_EXIST_CONTAINS_REF_MSG = "选中的资源列表存在包含关系,请确认后重试";

    public static final String APP_MATERIAL_COPY_FAIL_CHILDREN_DIR_CODE = "APP_MATERIAL_COPY_FAIL_CHILDREN_DIR_10000001";
    public static final String APP_MATERIAL_COPY_FAIL_CHILDREN_DIR_MSG = "目标目录不能是子目录,复制失败";

    public static final String APP_MATERIAL_NOT_EXIST_CODE = "APP_MATERIAL_NOT_EXIST_10000001";
    public static final String APP_MATERIAL_NOT_EXIST_MSG = "目标资源不存在,复制失败";

    public static final String APP_MATERIAL_NOT_FOUND_CODE = "APP_MATERIAL_NOT_FOUND_10000001";
    public static final String APP_MATERIAL_NOT_FOUND_MSG = "未找到素材资源,请确认后重试";

    public static final String APP_MATERIAL_DIR_NOT_EXIST_CODE = "APP_MATERIAL_DIR_NOT_EXIST_10000001";
    public static final String APP_MATERIAL_DIR_NOT_EXIST_MSG = "文件夹不存在";

    public static final String APP_MATERIAL_COLLECT_NOT_EXIST_CODE = "APP_MATERIAL_COLLECT_NOT_EXIST_10000001";
    public static final String APP_MATERIAL_COLLECT_NOT_EXIST_MSG = "目标列表有不存在资源,复制失败";

    public static final String APP_MATERIAL_MKDIR_FAIL_CODE = "APP_MATERIAL_MKDIR_FAIL_10000001";
    public static final String APP_MATERIAL_MKDIR_FAIL_MSG = "新建文件夹失败,请确认后重试";

    public static final String APP_MATERIAL_MOVE_FAIL_CODE = "APP_MATERIAL_MOVE_FAIL_10000001";
    public static final String APP_MATERIAL_MOVE_FAIL_MSG = "移动资源失败,请确认后重试";

    public static final String APP_MATERIAL_COPY_FAIL_CODE = "APP_MATERIAL_COPY_FAIL_10000001";
    public static final String APP_MATERIAL_COPY_FAIL_MSG = "复制资源失败,请确认后重试";

    public static final String APP_MATERIAL_COLLECT_MOVE_FAIL_CODE = "APP_MATERIAL_COLLECT_MOVE_FAIL_10000001";
    public static final String APP_MATERIAL_COLLECT_MOVE_FAIL_MSG = "批量移动资源失败,请确认后重试";

    public static final String APP_MATERIAL_COLLECT_COPY_FAIL_CODE = "APP_MATERIAL_COLLECT_COPY_FAIL_10000001";
    public static final String APP_MATERIAL_COLLECT_COPY_FAIL_MSG = "批量复制资源失败,请确认后重试";

    public static final String APP_MATERIAL_RENAME_FAIL_CODE = "APP_MATERIAL_RENAME_FAIL_FAIL_10000001";
    public static final String APP_MATERIAL_RENAME_FAIL_MSG = "重命名失败,请确认后重试";

    public static final String APP_MATERIAL_EDIT_CAPTION_FAIL_CODE = "APP_MATERIAL_EDIT_CAPTION_FAIL_10000001";
    public static final String APP_MATERIAL_EDIT_CAPTION_FAIL_MSG = "编辑字幕,请确认后重试";

    public static final String APP_MATERIAL_DELETE_REF_FAIL_CODE = "APP_MATERIAL_DELETE_REF_FAIL_10000001";
    public static final String APP_MATERIAL_DELETE_REF_FAIL_MSG = "所选项中包含被引用文件,该类型文件无法直接删除,可取消文件的引用后重试";

    public static final String APP_MATERIAL_DELETE_FAIL_CODE = "APP_MATERIAL_DELETE_FAIL_10000001";
    public static final String APP_MATERIAL_DELETE_FAIL_MSG = "删除失败,请确认后重试";

    public static final String APP_MATERIAL_REF_NOT_EXIST_FAIL_CODE = "APP_MATERIAL_REF_NOT_EXIST_FAIL_10000001";
    public static final String APP_MATERIAL_REF_NOT_EXIST_FAIL_MSG = "未找到素材引用关联数据,请确认后重试";

    public static final String APP_MATERIAL_NOT_VIDEO_TYPE_ERROR_CODE = "APP_MATERIAL_NOT_VIDEO_TYPE_ERROR_10000001";
    public static final String APP_MATERIAL_NOT_VIDEO_TYPE_ERROR_MSG = "所选项包含非视频文件，请确认后重试";

    public static final String APP_MATERIAL_NOT_SELECTED_ERROR_CODE = "APP_MATERIAL_NOT_SELECTED_ERROR_10000001";
    public static final String APP_MATERIAL_NOT_SELECTED_ERROR_MSG = "请至少选择一条记录";

    public static final String APP_MATERIAL_COVER_NOT_DATA_CODE = "APP_MATERIAL_COVER_NOT_DATA_10000001";
    public static final String APP_MATERIAL_COVER_NOT_DATA_MSG = "没有要生成封面的视频素材";

    /**-----------------------------------素材中心 end -----------------------------------------*/

    /**-----------------------------------内容管理-专辑分类 start -----------------------------------------*/
    public static final String APP_ALBUM_MENUS_NOT_EXIST_CODE = "APP_ALBUM_MENUS_NOT_EXIST_10000001";
    public static final String APP_ALBUM_MENUS_NOT_EXIST_MSG = "专辑分类不存在,请确认后重试";

    public static final String APP_ALBUM_MENUS_NOT_ROOT_CODE = "APP_ALBUM_MENUS_NOT_ROOT_10000001";
    public static final String APP_ALBUM_MENUS_NOT_ROOT_MSG = "专辑分类不能是根目录,请确认后重试";

    public static final String APP_ALBUM_MENUS_DELETE_REF_FAIL_CODE = "APP_ALBUM_MENUS_DELETE_REF_FAIL_10000001";
    public static final String APP_ALBUM_MENUS_DELETE_REF_FAIL_MSG = "所选专辑分类下存在专辑,请删除专辑后重试";

    public static final String APP_ALBUM_MENUS_HAS_CHILD_DELETE_REF_FAIL_CODE = "APP_ALBUM_MENUS_HAS_CHILD_DELETE_REF_FAIL_10000001";
    public static final String APP_ALBUM_MENUS_HAS_CHILD_DELETE_REF_FAIL_MSG = "所选专辑分类下存在子分类,请删除子分类后重试";

    public static final String APP_ALBUM_MENUS_DELETE_FAIL_CODE = "APP_ALBUM_MENUS_DELETE_FAIL_10000001";
    public static final String APP_ALBUM_MENUS_DELETE_FAIL_MSG = "删除失败,请确认后重试";

    public static final String APP_ALBUM_MENUS_RENAME_FAIL_CODE = "APP_ALBUM_MENUS_RENAME_FAIL_10000001";
    public static final String APP_ALBUM_MENUS_RENAME_FAIL_MSG = "重命名失败,请确认后重试";

    public static final String APP_ALBUM_MENUS_SAVE_FAIL_CODE = "APP_ALBUM_MENUS_SAVE_FAIL_10000001";
    public static final String APP_ALBUM_MENUS_SAVE_FAIL_MSG = "新增失败,请确认后重试";

    public static final String APP_ALBUM_MENUS_RESET_ORDER_NUM_PARAM_ERROR_FAIL_CODE = "APP_ALBUM_MENUS_RESET_ORDER_NUM_PARAM_ERROR_FAIL_10000001";
    public static final String APP_ALBUM_MENUS_RESET_ORDER_NUM_PARAM_ERROR_FAIL_MSG = "请求参数错误,请检查后重试";

    public static final String APP_ALBUM_MENUS_ORDER_COVER_MAX_FAIL_CODE = "APP_ALBUM_MENUS_ORDER_COVER_MAX_FAIL_10000001";
    public static final String APP_ALBUM_MENUS_ORDER_COVER_MAX_FAIL_MSG = "专辑分类最大层级不能超过五层，保存失败";

    public static final String APP_ALBUM_MENUS_RESET_ORDER_NUM_FAIL_CODE = "APP_ALBUM_MENUS_RESET_ORDER_NUM_FAIL_10000001";
    public static final String APP_ALBUM_MENUS_RESET_ORDER_NUM_FAIL_MSG = "保存失败,请确认后重试";

    public static final String APP_ALBUM_MENUS_MOVE_FAIL_CODE = "APP_ALBUM_MENUS_MOVE_FAIL_10000001";
    public static final String APP_ALBUM_MENUS_MOVE_FAIL_MSG = "移动失败,请确认后重试";
    /**-----------------------------------内容管理-专辑分类 end -----------------------------------------*/

    /**-----------------------------------内容管理-专辑 start -----------------------------------------*/
    public static final String APP_ALBUM_ELEMENTS_SAVE_FAIL_CODE = "APP_ALBUM_ELEMENTS_SAVE_FAIL_10000001";
    public static final String APP_ALBUM_ELEMENTS_SAVE_FAIL_MSG = "新增失败,请确认后重试";

    public static final String APP_ALBUM_ELEMENTS_NOT_EXIST_FAIL_CODE = "APP_ALBUM_ELEMENTS_NOT_EXIST_10000001";
    public static final String APP_ALBUM_ELEMENTS_NOT_EXIST_FAIL_MSG = "专辑不存在,请确认后重试";

    public static final String APP_ALBUM_ELEMENTS_DELETE_REF_FAIL_CODE = "APP_ALBUM_ELEMENTS_DELETE_REF_FAIL_10000001";
    public static final String APP_ALBUM_ELEMENTS_DELETE_REF_FAIL_MSG = "所选专辑中包含被引用专辑,该专辑无法直接删除,可取消专辑的引用后重试";

    public static final String APP_ALBUM_ELEMENTS_DELETE_FAIL_CODE = "APP_ALBUM_ELEMENTS_DELETE_FAIL_10000001";
    public static final String APP_ALBUM_ELEMENTS_DELETE_FAIL_MSG = "删除失败,请确认后重试";

    public static final String APP_ALBUM_ELEMENTS_UPDATE_FAIL_CODE = "APP_ALBUM_ELEMENTS_UPDATE_FAIL_10000001";
    public static final String APP_ALBUM_ELEMENTS_UPDATE_FAIL_MSG = "修改失败,请确认后重试";

    public static final String APP_ALBUM_ELEMENTS_MOVE_FAIL_CODE = "APP_ALBUM_ELEMENTS_MOVE_FAIL_10000001";
    public static final String APP_ALBUM_ELEMENTS_MOVE_FAIL_MSG = "移动失败,请确认后重试";

    public static final String APP_ALBUM_ELEMENTS_COPY_FAIL_CODE = "APP_ALBUM_ELEMENTS_COPY_FAIL_10000001";
    public static final String APP_ALBUM_ELEMENTS_COPY_FAIL_MSG = "复制失败,请确认后重试";

    public static final String APP_ALBUM_ELEMENTS_REF_SAVE_SAME_FAIL_CODE = "APP_ALBUM_ELEMENTS_REF_SAVE_SAME_FAIL_10000001";
    public static final String APP_ALBUM_ELEMENTS_REF_SAVE_SAME_FAIL_MSG = "资源列表存在重复,请确认后重试";

    public static final String APP_ALBUM_ELEMENTS_REF_SAVE_FAIL_CODE = "APP_ALBUM_ELEMENTS_REF_SAVE_FAIL_10000001";
    public static final String APP_ALBUM_ELEMENTS_REF_SAVE_FAIL_MSG = "保存失败,请确认后重试";

    public static final String APP_ALBUM_ELEMENTS_REF_RENAME_FAIL_CODE = "APP_ALBUM_ELEMENTS_REF_RENAME_FAIL_10000001";
    public static final String APP_ALBUM_ELEMENTS_REF_RENAME_FAIL_MSG = "重命名失败,请确认后重试";

    public static final String APP_ALBUM_ELEMENTS_REF_REMOVE_FAIL_CODE = "APP_ALBUM_ELEMENTS_REF_REMOVE_FAIL_10000001";
    public static final String APP_ALBUM_ELEMENTS_REF_REMOVE_FAIL_MSG = "移除失败,请确认后重试";

    public static final String APP_ALBUM_ELEMENTS_REF_ORDER_NOT_EXIST_FAIL_CODE = "APP_ALBUM_ELEMENTS_REF_ORDER_NOT_EXIST_FAIL_10000001";
    public static final String APP_ALBUM_ELEMENTS_REF_ORDER_NOT_EXIST_FAIL_MSG = "关联资源不存在,排序失败,请确认后重试";

    public static final String APP_ALBUM_ELEMENTS_REF_ORDER_FAIL_CODE = "APP_ALBUM_ELEMENTS_REF_ORDER_NOT_EXIST_FAIL_10000001";
    public static final String APP_ALBUM_ELEMENTS_REF_ORDER_FAIL_MSG = "排序失败,请确认后重试";

    public static final String APP_ALBUM_ELEMENTS_REF_UPDATE_FAIL_CODE = "APP_ALBUM_ELEMENTS_REF_UPDATE_10000001";
    public static final String APP_ALBUM_ELEMENTS_REF_UPDATE_FAIL_MSG = "更换专辑失败,请确认后重试";

    public static final String APP_ALBUM_ELEMENTS_RECEIVE_CHECKED_CODE = "BOOK_RECEIVE_CHECKED_10000001";


    /**-----------------------------------内容管理-专辑 end -----------------------------------------*/

    /**-----------------------------------题库 start -----------------------------------------*/
    /**
     * 选项最小数量
     */
    private static final int MIN_OPTION_COUNT = 1;

    /**
     * 选项最大数量
     */
    private static final int MAX_OPTION_COUNT = 8;
    //分类参数不能为空
    public static final String APP_QUESTION_CATEGORY_PARAM_NOT_NULL_CODE = "APP_QUESTION_CATEGORY_PARAM_NOT_NULL_10000001";
    public static final String APP_QUESTION_CATEGORY_PARAM_NOT_NULL_MSG = "分类参数不能为空";
    //分类名称不能为空
    public static final String APP_QUESTION_CATEGORY_NAME_NOT_NULL_CODE = "APP_QUESTION_CATEGORY_NAME_NOT_NULL_10000002";
    public static final String APP_QUESTION_CATEGORY_NAME_NOT_NULL_MSG = "分类名称不能为空";
    //不能将分类修改为根节点
    public static final String APP_QUESTION_CATEGORY_NOT_ROOT_CODE = "APP_QUESTION_CATEGORY_NOT_ROOT_10000003";
    public static final String APP_QUESTION_CATEGORY_NOT_ROOT_MSG = "不能将分类修改为根节点";
    //分类不存在
    public static final String APP_QUESTION_CATEGORY_NOT_EXIST_CODE = "APP_QUESTION_CATEGORY_NOT_EXIST_10000004";
    public static final String APP_QUESTION_CATEGORY_NOT_EXIST_MSG = "分类不存在";

    public static final String APP_QUESTION_CATEGORY_MAX_FAIL_CODE = "APP_QUESTION_CATEGORY_MAX_FAIL_10000001";
    public static final String APP_QUESTION_CATEGORY_MAX_FAIL_MSG = "题库分类最大层级不能超过五层，保存失败";


    //父分类不存在
    public static final String APP_QUESTION_CATEGORY_FATHER_NOT_EXIST_CODE = "APP_QUESTION_CATEGORY_FATHER_NOT_EXIST_10000005";
    public static final String APP_QUESTION_CATEGORY_FATHER_NOT_EXIST_MSG = "父分类不存在";
    //不允许跨父节点移动分类
    public static final String APP_QUESTION_CATEGORY_MOVE_NOT_SUPPORT_CODE = "APP_QUESTION_CATEGORY_MOVE_NOT_SUPPORT_10000006";
    public static final String APP_QUESTION_CATEGORY_MOVE_NOT_SUPPORT_MSG = "不允许跨父节点移动分类";
    //存在子分类，无法删除
    public static final String APP_QUESTION_CATEGORY_HAS_CHILDREN_CODE = "APP_QUESTION_CATEGORY_HAS_CHILDREN_10000007";
    public static final String APP_QUESTION_CATEGORY_HAS_CHILDREN_MSG = "存在子分类，无法删除";
    //分类下存在试题，无法删除
    public static final String APP_QUESTION_CATEGORY_HAS_QUESTIONS_CODE = "APP_QUESTION_CATEGORY_HAS_QUESTIONS_10000008";
    public static final String APP_QUESTION_CATEGORY_HAS_QUESTIONS_MSG = "分类下存在试题，无法删除";
    //分类已被配套引用，无法删除
    public static final String APP_QUESTION_CATEGORY_HAS_REF_CODE = "APP_QUESTION_CATEGORY_HAS_REF_10000009";
    public static final String APP_QUESTION_CATEGORY_HAS_REF_MSG = "分类已被配套引用，无法删除";
    //题目不能为空
    public static final String APP_QUESTION_NOT_NULL_CODE = "APP_QUESTION_NOT_NULL_10000010";
    public static final String APP_QUESTION_NOT_NULL_MSG = "题目不能为空";
    //题目类型不能为空
    public static final String APP_QUESTION_TYPE_NOT_NULL_CODE = "APP_QUESTION_TYPE_NOT_NULL_10000011";
    public static final String APP_QUESTION_TYPE_NOT_NULL_MSG = "题目类型不能为空";
    //不支持的题目类型
    public static final String APP_QUESTION_TYPE_NOT_SUPPORT_CODE = "APP_QUESTION_TYPE_NOT_SUPPORT_10000012";
    public static final String APP_QUESTION_TYPE_NOT_SUPPORT_MSG = "不支持的题目类型";
    //选择题选项不能为空
    public static final String APP_QUESTION_CHOICE_NOT_NULL_CODE = "APP_QUESTION_CHOICE_NOT_NULL_10000013";
    public static final String APP_QUESTION_CHOICE_NOT_NULL_MSG = "选择题选项不能为空";
    //"选择题选项数量必须在" + MIN_OPTION_COUNT + "到" + MAX_OPTION_COUNT + "之间"
    public static final String APP_QUESTION_CHOICE_COUNT_ERROR_CODE = "APP_QUESTION_CHOICE_COUNT_ERROR_10000014";
    public static final String APP_QUESTION_CHOICE_COUNT_ERROR_MSG =
            String.format("选择题选项数量必须在 %d 到 %d 之间", MIN_OPTION_COUNT, MAX_OPTION_COUNT);

    //填空题答案不能为空
    public static final String APP_QUESTION_FILL_BLANK_NOT_NULL_CODE = "APP_QUESTION_FILL_BLANK_NOT_NULL_10000016";
    public static final String APP_QUESTION_FILL_BLANK_NOT_NULL_MSG = "填空题答案不能为空";
    //完形填空答案不能为空
    public static final String APP_QUESTION_CLOZE_NOT_NULL_CODE = "APP_QUESTION_CLOZE_NOT_NULL_10000017";
    public static final String APP_QUESTION_CLOZE_NOT_NULL_MSG = "完形填空答案不能为空";
    //判断题只能有一个答案
    public static final String APP_QUESTION_TRUE_FALSE_ONLY_ONE_CODE = "APP_QUESTION_TRUE_FALSE_ONLY_ONE_10000018";
    public static final String APP_QUESTION_TRUE_FALSE_ONLY_ONE_MSG = "判断题只能有一个答案";
    //判断题答案不能为空
    public static final String APP_QUESTION_TRUE_FALSE_NOT_NULL_CODE = "APP_QUESTION_TRUE_FALSE_NOT_NULL_10000019";
    public static final String APP_QUESTION_TRUE_FALSE_NOT_NULL_MSG = "判断题答案不能为空";
    //判断题答案只能是'对'或'错'
    public static final String APP_QUESTION_TRUE_FALSE_ERROR_CODE = "APP_QUESTION_TRUE_FALSE_ERROR_10000020";
    public static final String APP_QUESTION_TRUE_FALSE_ERROR_MSG = "判断题答案只能是'对'或'错'";
    //排序题选项不能为空
    public static final String APP_QUESTION_SORT_NOT_NULL_CODE = "APP_QUESTION_SORT_NOT_NULL_10000021";
    public static final String APP_QUESTION_SORT_NOT_NULL_MSG = "排序题选项不能为空";
    //排序题选项数量必须在" + MIN_SORT_OPTION_COUNT + "到" + MAX_OPTION_COUNT + "之间"
    public static final String APP_QUESTION_SORT_COUNT_ERROR_CODE = "APP_QUESTION_SORT_COUNT_ERROR_10000022";
    public static final String APP_QUESTION_SORT_COUNT_ERROR_MSG =
            String.format("排序题选项数量必须在 %d 到 %d 之间", MIN_OPTION_COUNT, MAX_OPTION_COUNT);
    //选项内容和媒体至少填写一项
    public static final String APP_QUESTION_CONTENT_ERROR_CODE = "APP_QUESTION_SORT_CONTENT_ERROR_10000023";
    public static final String APP_QUESTION_CONTENT_ERROR_MSG = "选项内容或媒体文件至少填写一项";
    //单选题必须有且只有一个正确答案
    public static final String APP_QUESTION_SINGLE_CHOICE_ERROR_CODE = "APP_QUESTION_SINGLE_CHOICE_ERROR_10000024";
    public static final String APP_QUESTION_SINGLE_CHOICE_ERROR_MSG = "单选题必须有且只有一个正确答案";
    //多选题至少要有一个正确答案
    public static final String APP_QUESTION_MULTIPLE_CHOICE_ERROR_CODE = "APP_QUESTION_MULTIPLE_CHOICE_ERROR_10000025";
    public static final String APP_QUESTION_MULTIPLE_CHOICE_ERROR_MSG = "多选题至少要有一个正确答案";
    //每个选项只能包含一张图片
    public static final String APP_QUESTION_OPTION_IMAGE_ERROR_CODE = "APP_QUESTION_OPTION_IMAGE_ERROR_10000026";
    public static final String APP_QUESTION_OPTION_IMAGE_ERROR_MSG = "每个选项只能包含一张图片";
    //排序题选项序号必须从1开始连续
    public static final String APP_QUESTION_SORT_ORDER_ERROR_CODE = "APP_QUESTION_SORT_ORDER_ERROR_10000027";
    public static final String APP_QUESTION_SORT_ORDER_ERROR_MSG = "排序题选项序号必须从1开始连续";
    //答案ID不存在且无效
    public static final String APP_QUESTION_ANSWER_ID_NOT_EXIST_CODE = "APP_QUESTION_ANSWER_ID_NOT_EXIST_10000028";
    //校验答案ID是否在选项中不存在
    public static final String APP_QUESTION_ANSWER_ID_NOT_EXIST_IN_OPTIONS_CODE = "APP_QUESTION_ANSWER_ID_NOT_EXIST_IN_OPTIONS_10000029";
    //第%d个空位没有答案选项
    public static final String APP_QUESTION_CLOZE_BLANK_NOT_ANSWER_CODE = "APP_QUESTION_CLOZE_BLANK_NOT_ANSWER_10000030";
    //完形填空的空位序号必须从1开始连续
    public static final String APP_QUESTION_CLOZE_BLANK_ORDER_ERROR_CODE = "APP_QUESTION_CLOZE_BLANK_ORDER_ERROR_10000031";
    public static final String APP_QUESTION_CLOZE_BLANK_ORDER_ERROR_MSG = "完形填空的空位序号必须从1开始连续";
    //完形填空的空位不能为空
    public static final String APP_QUESTION_CLOZE_BLANK_NOT_NULL_CODE = "APP_QUESTION_CLOZE_BLANK_NOT_NULL_10000032";
    public static final String APP_QUESTION_CLOZE_BLANK_NOT_NULL_MSG = "完形填空的空位不能为空";
    //第%d个空位的选项数量必须在%d到%d之间
    public static final String APP_QUESTION_CLOZE_BLANK_OPTIONS_COUNT_ERROR_CODE = "APP_QUESTION_CLOZE_BLANK_OPTIONS_COUNT_ERROR_10000033";
    //业务关联信息不能为空
    public static final String APP_BUSINESS_RELATION_NOT_NULL_CODE = "APP_BUSINESS_RELATION_NOT_NULL_10000034";
    public static final String APP_BUSINESS_RELATION_NOT_NULL_MSG = "业务关联信息不能为空";
    //题目分类ID不能为空
    public static final String APP_QUESTION_CATEGORY_ID_NOT_NULL_CODE = "APP_QUESTION_CATEGORY_ID_NOT_NULL_10000035";
    public static final String APP_QUESTION_CATEGORY_ID_NOT_NULL_MSG = "题目分类ID不能为空";
    //业务ID不能为空
    public static final String APP_BUSINESS_ID_NOT_NULL_CODE = "APP_BUSINESS_ID_NOT_NULL_10000036";
    public static final String APP_BUSINESS_ID_NOT_NULL_MSG = "业务ID不能为空";
    //业务类型不能为空
    public static final String APP_BUSINESS_TYPE_NOT_NULL_CODE = "APP_BUSINESS_TYPE_NOT_NULL_10000037";
    public static final String APP_BUSINESS_TYPE_NOT_NULL_MSG = "业务类型不能为空";
    //评测项id不能为空
    public static final String APP_EVALUATION_NODE_ID_NOT_NULL_CODE = "APP_EVALUATION_NODE_ID_NOT_NULL_10000080";
    public static final String APP_EVALUATION_NODE_ID_NOT_NULL_MSG = "评测项ID不能为空";
    //题目不存在
    public static final String APP_QUESTION_NOT_EXIST_CODE = "APP_QUESTION_NOT_EXIST_CODE_10000038";
    public static final String APP_QUESTION_NOT_EXIST_MSG = "题目不存在,请确认后重试";
    //获取试卷结果失败
    public static final String APP_GET_PAPER_RESULT_FAIL_CODE = "APP_GET_PAPER_RESULT_FAIL_10000039";
    public static final String APP_GET_PAPER_RESULT_FAIL_MSG = "获取试卷结果失败,请确认后重试";
    //试卷不存在
    public static final String APP_PAPER_NOT_EXIST_CODE = "APP_PAPER_NOT_EXIST_10000040";
    public static final String APP_PAPER_NOT_EXIST_MSG = "试卷不存在,请确认后重试";
    //无权访问此试卷
    public static final String APP_PAPER_NO_PERMISSION_CODE = "APP_PAPER_NO_PERMISSION_10000041";
    public static final String APP_PAPER_NO_PERMISSION_MSG = "无权访问此试卷";
    //重新考试失败
    public static final String APP_RE_EXAM_FAIL_CODE = "APP_RE_EXAM_FAIL_10000042";
    public static final String APP_RE_EXAM_FAIL_MSG = "重新考试失败,请确认后重试";
    //参数不能为空
    public static final String APP_PARAM_NOT_NULL_CODE = "APP_PARAM_NOT_NULL_10000043";
    public static final String APP_PARAM_NOT_NULL_MSG = "参数不能为空";
    //创建试卷失败
    public static final String APP_CREATE_PAPER_FAIL_CODE = "APP_CREATE_PAPER_FAIL_10000044";
    public static final String APP_CREATE_PAPER_FAIL_MSG = "创建试卷失败,请确认后重试";
    //试卷ID不能为空
    public static final String APP_PAPER_ID_NOT_NULL_CODE = "APP_PAPER_ID_NOT_NULL_10000045";
    public static final String APP_PAPER_ID_NOT_NULL_MSG = "试卷ID不能为空";
    //获取试卷信息失败
    public static final String APP_GET_PAPER_INFO_ERROR_CODE = "APP_GET_PAPER_INFO_ERROR_10000046";
    public static final String APP_GET_PAPER_INFO_ERROR_MSG = "获取试卷信息失败,请确认后重试";
    //未提交任何答案
    public static final String APP_PAPER_NO_ANSWER_CODE = "APP_PAPER_NO_ANSWER_10000047";
    public static final String APP_PAPER_NO_ANSWER_MSG = "未提交任何答案";
    //保存题目作答结果失败
    public static final String APP_SAVE_QUESTION_RESULT_FAIL_CODE = "APP_SAVE_QUESTION_RESULT_FAIL_10000048";
    public static final String APP_SAVE_QUESTION_RESULT_FAIL_MSG = "保存题目作答结果失败,请确认后重试";
    //保存完形填空作答结果失败
    public static final String APP_SAVE_CLOZE_RESULT_FAIL_CODE = "APP_SAVE_CLOZE_RESULT_FAIL_10000049";
    public static final String APP_SAVE_CLOZE_RESULT_FAIL_MSG = "保存完形填空作答结果失败,请确认后重试";
    //获取空位作答结果失败
    public static final String APP_GET_BLANK_RESULT_FAIL_CODE = "APP_GET_BLANK_RESULT_FAIL_10000050";
    public static final String APP_GET_BLANK_RESULT_FAIL_MSG = "获取空位作答结果失败,请确认后重试";
    //保存答案列表失败
    public static final String APP_SAVE_ANSWER_LIST_FAIL_CODE = "APP_SAVE_ANSWER_LIST_FAIL_10000051";
    public static final String APP_SAVE_ANSWER_LIST_FAIL_MSG = "保存答案列表失败,请确认后重试";
    //题目ID不能为空
    public static final String APP_QUESTION_ID_NOT_NULL_CODE = "APP_QUESTION_ID_NOT_NULL_10000052";
    public static final String APP_QUESTION_ID_NOT_NULL_MSG = "题目ID不能为空";
    //更新答案列表失败
    public static final String APP_UPDATE_ANSWER_LIST_FAIL_CODE = "APP_UPDATE_ANSWER_LIST_FAIL_10000053";
    public static final String APP_UPDATE_ANSWER_LIST_FAIL_MSG = "更新答案列表失败,请确认后重试";
    //删除答案列表失败
    public static final String APP_DELETE_ANSWER_LIST_FAIL_CODE = "APP_DELETE_ANSWER_LIST_FAIL_10000054";
    public static final String APP_DELETE_ANSWER_LIST_FAIL_MSG = "删除答案列表失败,请确认后重试";
    //题库ID不能为空
    public static final String APP_QUESTION_BANK_ID_NOT_NULL_CODE = "APP_QUESTION_BANK_ID_NOT_NULL_10000055";
    public static final String APP_QUESTION_BANK_ID_NOT_NULL_MSG = "题库ID不能为空";
    //查询数量限制不能为空或小于等于0
    public static final String APP_QUERY_COUNT_NOT_NULL_CODE = "APP_QUERY_COUNT_NOT_NULL_10000056";
    public static final String APP_QUERY_COUNT_NOT_NULL_MSG = "查询数量限制不能为空或小于等于0";
    //业务关联参数不能为空
    public static final String APP_BUSINESS_RELATION_PARAM_NOT_NULL_CODE = "APP_BUSINESS_RELATION_PARAM_NOT_NULL_10000057";
    public static final String APP_BUSINESS_RELATION_PARAM_NOT_NULL_MSG = "业务关联参数不能为空";
    //题库ID列表不能为空
    public static final String APP_QUESTION_BANK_ID_LIST_NOT_NULL_CODE = "APP_QUESTION_BANK_ID_LIST_NOT_NULL_10000058";
    public static final String APP_QUESTION_BANK_ID_LIST_NOT_NULL_MSG = "题库ID列表不能为空";

    //题库分类[%s]下没有可用的题目
    public static final String APP_QUESTION_BANK_CATEGORY_NOT_EXIST_CODE = "APP_QUESTION_BANK_CATEGORY_NOT_EXIST_10000059";
    public static final String APP_QUESTION_BANK_CATEGORY_NOT_EXIST_MSG = "题库分类[%s]下没有可用的题目";
    //以下题库已关联此业务
    public static final String APP_QUESTION_BANK_HAS_RELATION_CODE = "APP_QUESTION_BANK_HAS_RELATION_10000060";
    public static final String APP_QUESTION_BANK_HAS_RELATION_MSG = "以下题库已关联此业务";
    //创建业务关联失败
    public static final String APP_CREATE_BUSINESS_RELATION_FAIL_CODE = "APP_CREATE_BUSINESS_RELATION_FAIL_10000061";
    public static final String APP_CREATE_BUSINESS_RELATION_FAIL_MSG = "创建业务关联失败,请确认后重试";
    //学科名称已存在
    public static final String APP_SUBJECT_NAME_EXIST_CODE = "APP_SUBJECT_NAME_EXIST_10000062";
    public static final String APP_SUBJECT_NAME_EXIST_MSG = "存在相同的名称，请更换其他名称";
    //该学科下存在题目，无法删除
    public static final String APP_SUBJECT_HAS_QUESTIONS_CODE = "APP_SUBJECT_HAS_QUESTIONS_10000063";
    public static final String APP_SUBJECT_HAS_QUESTIONS_MSG = "该学科下存在题目，无法删除";

    //完形填空参数不能为空
    public static final String APP_QUESTION_CLOZE_PARAM_NOT_NULL_CODE = "APP_QUESTION_CLOZE_PARAM_NOT_NULL_10000064";
    public static final String APP_QUESTION_CLOZE_PARAM_NOT_NULL_MSG = "完形填空参数不能为空";
    //未找到对应的题目业务策略
    public static final String APP_QUESTION_BUSINESS_STRATEGY_NOT_EXIST_CODE = "APP_QUESTION_BUSINESS_STRATEGY_NOT_EXIST_10000065";
    public static final String APP_QUESTION_BUSINESS_STRATEGY_NOT_EXIST_MSG = "未找到对应的题目业务策略";
    //未找到对应的试卷业务策略
    public static final String APP_PAPER_BUSINESS_STRATEGY_NOT_EXIST_CODE = "APP_PAPER_BUSINESS_STRATEGY_NOT_EXIST_10000066";
    public static final String APP_PAPER_BUSINESS_STRATEGY_NOT_EXIST_MSG = "未找到对应的试卷业务策略";
    //获取题目空位区域失败
    public static final String APP_GET_BLANK_AREA_FAIL_CODE = "APP_GET_BLANK_AREA_FAIL_10000067";
    public static final String APP_GET_BLANK_AREA_FAIL_MSG = "获取题目空位区域失败,请确认后重试";
    //答题提交参数不能为空
    public static final String APP_SUBMIT_PARAM_NOT_NULL_CODE = "APP_SUBMIT_PARAM_NOT_NULL_10000068";
    public static final String APP_SUBMIT_PARAM_NOT_NULL_MSG = "答题提交参数不能为空";
    //用户ID不能为空
    public static final String APP_USER_ID_NOT_NULL_CODE = "APP_USER_ID_NOT_NULL_10000069";
    public static final String APP_USER_ID_NOT_NULL_MSG = "用户ID不能为空";
    //您已经提交过该试卷，不能重复提交
    public static final String APP_PAPER_SUBMIT_REPEAT_CODE = "APP_PAPER_SUBMIT_REPEAT_10000070";
    public static final String APP_PAPER_SUBMIT_REPEAT_MSG = "您已经提交过该试卷，不能重复提交";
    //保存试卷结果失败
    public static final String APP_SAVE_PAPER_RESULT_FAIL_CODE = "APP_SAVE_PAPER_RESULT_FAIL_10000071";
    public static final String APP_SAVE_PAPER_RESULT_FAIL_MSG = "保存试卷结果失败,请确认后重试";
    //查询参数不能为空
    public static final String APP_QUERY_PARAM_NOT_NULL_CODE = "APP_QUERY_PARAM_NOT_NULL_10000072";
    public static final String APP_QUERY_PARAM_NOT_NULL_MSG = "查询参数不能为空";
    //出题设置不存在
    public static final String APP_QUESTION_SETTING_NOT_EXIST_CODE = "APP_QUESTION_SETTING_NOT_EXIST_10000073";
    public static final String APP_QUESTION_SETTING_NOT_EXIST_MSG = "出题设置不存在,请确认后重试";
    //空位总数无效
    public static final String APP_BLANK_TOTAL_INVALID_CODE = "APP_BLANK_TOTAL_INVALID_10000074";
    public static final String APP_BLANK_TOTAL_INVALID_MSG = "空位总数无效";
    //检查空位作答完成情况失败
    public static final String APP_CHECK_BLANK_ANSWERED_FAIL_CODE = "APP_CHECK_BLANK_ANSWERED_FAIL_10000075";
    public static final String APP_CHECK_BLANK_ANSWERED_FAIL_MSG = "检查空位作答完成情况失败,请确认后重试";
    //获取题目失败
    public static final String APP_GET_QUESTION_FAIL_CODE = "APP_GET_QUESTION_FAIL_10000076";
    public static final String APP_GET_QUESTION_FAIL_MSG = "获取题目失败,请确认后重试";
    //选项内容不能为空

    public static final String APP_ANSWER_OPTION_CONTENT_FAIL_CODE = "APP_ANSWER_OPTION_CONTENT_FAIL_10000077";
    public static final String APP_ANSWER_OPTION_CONTENT_FAIL_MSG = "选项内容不能为空";
    //媒体文件类型不能为空

    public static final String APP_ANSWER_MEDIA_TYPE_FAIL_CODE = "APP_ANSWER_MEDIA_TYPE_FAIL_10000078";
    public static final String APP_ANSWER_MEDIA_TYPE_FAIL_MSG = "媒体文件类型不能为空";
    //提交试卷失败

    public static final String APP_SUBMIT_PARER_FAIL_CODE = "APP_ANSWER_MEDIA_TYPE_FAIL_10000079";
    public static final String APP_SUBMIT_PARER_FAIL_MSG = "提交试卷失败";
    //该分类下存在学习模块，无法删除

    public static final String APP_CATEGORY_HAS_MODULE_FAIL_CODE = "APP_CATEGORY_HAS_MODULE_10000080";
    public static final String APP_CATEGORY_HAS_MODULE_FAIL_MSG = "该分类下存在学习模块，无法删除";

    /**-----------------------------------广告 start -----------------------------------------*/
    public static final String ADVERTISEMENT_PARAMETER_VERIFICATION_FAIL_CODE = "ADVERTISEMENT_PARAMETER_VERIFICATION_FAIL_10000001";
    public static final String ADVERTISEMENT_NOT_EXIST_FAIL_CODE = "ADVERTISEMENT_NOT_EXIST_FAIL_10000002";
    public static final String ADVERTISEMENT_NOT_EXIST_FAIL_MSG = "广告策略不存在或已被删除";

    /**-----------------------------------图书 end -----------------------------------------*/

    /**-----------------------------------评测 start -----------------------------------------*/
    public static final String APP_EVALUATION_DELETE_FAIL_REF_CODE = "APP_EVALUATION_DELETE_FAIL_REF_10000001";
    public static final String APP_EVALUATION_DELETE_FAIL_REF_MSG = "评测表存在关联的评测结果，删除失败";

    public static final String APP_EVALUATION_NOT_EXIST_CODE = "APP_EVALUATION_NOT_EXIST_CODE_10000001";
    public static final String APP_EVALUATION_NOT_EXIST_MSG = "评测表不存在，请确认后重试";

    public static final String APP_EVALUATION_OPEN_LIMIT_CODE = "APP_EVALUATION_OPEN_LIMIT_10000001";
    public static final String APP_EVALUATION_OPEN_LIMIT_MSG = "只支持启用评测项在3~6个之间的评测表，请确认后重试";

    public static final String APP_EVALUATION_CLOSE_REF_FAIL_CODE = "APP_EVALUATION_CLOSE_REF_FAIL_10000001";
    public static final String APP_EVALUATION_CLOSE_REF_FAIL_MSG = "禁用项存在关联引用";
    /**-----------------------------------评测 end -----------------------------------------*/

    /**-----------------------------------评测项 start -----------------------------------------*/
    public static final String APP_EVALUATION_NODE_SAVE_FAIL_CODE = "APP_EVALUATION_NODE_SAVE_FAIL_10000001";
    public static final String APP_EVALUATION_NODE_SAVE_FAIL_MSG = "新增评测项失败，请确认后重试";


    public static final String APP_EVALUATION_NODE_EDIT_FAIL_CODE = "APP_EVALUATION_NODE_EDIT_FAIL_10000001";
    public static final String APP_EVALUATION_NODE_EDIT_FAIL_MSG = "修改评测项失败，请确认后重试";

    public static final String APP_EVALUATION_NODE_SAVE_FAIL_NO_DICT_CODE = "APP_EVALUATION_NODE_SAVE_FAIL_NO_DICT_10000001";
    public static final String APP_EVALUATION_NODE_SAVE_FAIL_NO_DICT_MSG = "未找到字典，新增失败，请确认后重试";

    public static final String APP_EVALUATION_NODE_DELETE_FAIL_REF_CODE = "APP_EVALUATION_NODE_DELETE_FAIL_REF_10000001";
    public static final String APP_EVALUATION_NODE_DELETE_FAIL_REF_MSG = "评测项下存在题目，删除失败，请删除题目后重试";

    public static final String APP_EVALUATION_NODE_DELETE_FAIL_CODE = "APP_EVALUATION_NODE_DELETE_FAIL_10000001";
    public static final String APP_EVALUATION_NODE_DELETE_FAIL_MSG = "删除评测项失败，请确认后重试";

    public static final String APP_EVALUATION_NODE_NO_ID_CODE = "APP_EVALUATION_NODE_NO_ID_10000001";
    public static final String APP_EVALUATION_NODE_NO_ID_MSG = "评测项ID不能为空，请确认后重试";

    public static final String APP_EVALUATION_NODE_SORT_FAIL_CODE = "APP_EVALUATION_NODE_SORT_FAIL_10000001";
    public static final String APP_EVALUATION_NODE_SORT_FAIL_MSG = "未找到评测项数据，请确认后重试";

    public static final String APP_EVALUATION_NODE_QUESTION_SORT_FAIL_NONE_CODE = "APP_EVALUATION_NODE_SORT_FAIL_NONE_10000001";
    public static final String APP_EVALUATION_NODE_QUESTION_SORT_FAIL_NONE_MSG = "未找到题目数据，请确认后重试";


    public static final String APP_EVALUATION_NODE_SAVE_QUESTION_NO_ID_CODE = "APP_EVALUATION_NODE_SAVE_QUESTION_NO_ID_10000001";
    public static final String APP_EVALUATION_NODE_SAVE_QUESTION_NO_ID_MSG = "题目列表不能为空，请确认后重试";

    public static final String APP_EVALUATION_NODE_SAVE_MAX_CODE = "APP_EVALUATION_NODE_SAVE_MAX_10000001";
    public static final String APP_EVALUATION_NODE_SAVE_MAX_MSG = "评测表最多支持新增10个评测项，请确认后重试";

    public static final String APP_EVALUATION_NODE_SAME_NAME_CODE = "APP_EVALUATION_NODE_SAME_NAME_10000001";
    public static final String APP_EVALUATION_NODE_SAME_NAME_MSG = "评测项名称已存在";

    /**-----------------------------------评测项 end -----------------------------------------*/

    /**-----------------------------------评测报告 start -----------------------------------------*/
    //保存题目作答结果失败
    public static final String APP_EVALUATION_SAVE_FAIL_CODE = "APP_EVALUATION_SAVE_FAIL_10000001";
    public static final String APP_EVALUATION_SAVE_FAIL_MSG = "保存评测记录失败,请确认后重试";

    public static final String APP_EVALUATION_RESET_FAIL_CODE = "APP_EVALUATION_RESET_FAIL_10000001";
    public static final String APP_EVALUATION_RESET_FAIL_MSG = "重新评测失败,请确认后重试";

    public static final String APP_EVALUATION_REPORT_NOT_EXIST_CODE = "APP_EVALUATION_REPORT_NOT_EXIST_10000001";
    public static final String APP_EVALUATION_REPORT_NOT_EXIST_MSG = "未找到评测记录";

    public static final String APP_EVALUATION_ANALYSIS_PARAMS_ERROR_CODE = "APP_EVALUATION_ANALYSIS_PARAMS_ERROR_10000001";
    public static final String APP_EVALUATION_ANALYSIS_PARAMS_ERROR_MSG = "获取评测报告分析数据失败,请确认后重试";


    public static final String APP_EVALUATION_ANALYSIS_SAVE_FAIL_CODE = "APP_EVALUATION_ANALYSIS_SAVE_FAIL_10000048";
    public static final String APP_EVALUATION_ANALYSIS_SAVE_FAIL_MSG = "保存评测项数据失败,请确认后重试";

    public static final String APP_EVALUATION_GENERATE_REPORT_FAIL_CODE = "APP_EVALUATION_GENERATE_REPORT_FAIL_10000048";
    public static final String APP_EVALUATION_GENERATE_REPORT_FAIL_MSG = "需所有评测完成答题后方可生成评测报告";

    public static final String APP_EVALUATION_GENERATE_FAIL_CODE = "APP_EVALUATION_GENERATE_FAIL_10000048";
    public static final String APP_EVALUATION_GENERATE_FAIL_MSG = "生成报告失败,请确认后重试";

    public static final String APP_EVALUATION_GENERATED_NOT_RETAKE_CODE = "APP_EVALUATION_GENERATED_NOT_RETAKE_10000001";
    public static final String APP_EVALUATION_GENERATED_NOT_RETAKE_MSG = "报告已生成,不支持重新答题哦~";

    /**-----------------------------------评测报告 end -----------------------------------------*/


    public static final String SHORT_FAIL_CODE = "SHORT_FAIL_10000001";
    public static final String SHORT_FAIL_MSG = "短链生成失败";
    public static final String SHORT_INVALID_CODE = "SHORT_INVALID_10000001";
    public static final String SHORT_INVALID_MSG = "无效的连接";

    /**---------------------------------openapi-------------------------------*/
    //应用注册失败
    public static final String OPENAPI_REGISTER_FAIL_CODE = "OPENAPI_REGISTER_FAIL_10000001";
    public static final String OPENAPI_REGISTER_FAIL_MSG = "应用注册失败";
    //获取应用信息时发生异常
    public static final String OPENAPI_GET_APP_INFO_EXCEPTION_CODE = "OPENAPI_GET_APP_INFO_EXCEPTION_10000001";
    public static final String OPENAPI_GET_APP_INFO_EXCEPTION_MSG = "获取应用信息时发生异常";
    //根据客户端ID获取应用信息失败
    public static final String OPENAPI_GET_APP_INFO_FAIL_CODE = "OPENAPI_GET_APP_INFO_FAIL_10000002";
    public static final String OPENAPI_GET_APP_INFO_FAIL_MSG = "根据客户端ID获取应用信息失败";
    //获取应用列表失败
    public static final String OPENAPI_GET_APP_LIST_FAIL_CODE = "OPENAPI_GET_APP_LIST_FAIL_10000003";
    public static final String OPENAPI_GET_APP_LIST_FAIL_MSG = "获取应用列表失败";
    //更新应用信息失败
    public static final String OPENAPI_UPDATE_APP_FAIL_CODE = "OPENAPI_UPDATE_APP_FAIL_10000004";
    public static final String OPENAPI_UPDATE_APP_FAIL_MSG = "更新应用信息失败";
    //无法刷新凭据：未初始化
    public static final String OPENAPI_REFRESH_CREDENTIAL_FAIL_CODE = "OPENAPI_REFRESH_CREDENTIAL_FAIL_10000001";
    public static final String OPENAPI_REFRESH_CREDENTIAL_FAIL_MSG = "无法刷新凭据：未初始化";
    //刷新应用密钥失败
    public static final String OPENAPI_REFRESH_APP_SECRET_FAIL_CODE = "OPENAPI_REFRESH_APP_SECRET_FAIL_10000005";
    public static final String OPENAPI_REFRESH_APP_SECRET_FAIL_MSG = "刷新应用密钥失败";
    //API调用失败
    public static final String OPENAPI_API_CALL_FAIL_CODE = "OPENAPI_API_CALL_FAIL_10000006";
    public static final String OPENAPI_API_CALL_FAIL_MSG = "API调用失败";
    //注册应用时发生异常
    public static final String OPENAPI_REGISTER_EXCEPTION_CODE = "OPENAPI_REGISTER_EXCEPTION_10000002";
    public static final String OPENAPI_REGISTER_EXCEPTION_MSG = "注册应用时发生异常";
    //获取图书分类信息失败
    public static final String OPENAPI_GET_BOOK_CATEGORY_FAIL_CODE = "OPENAPI_GET_BOOK_CATEGORY_FAIL_10000001";
    public static final String OPENAPI_GET_BOOK_CATEGORY_FAIL_MSG = "获取图书分类信息失败";
    //获取图书列表信息时发生异常
    public static final String OPENAPI_GET_BOOK_LIST_FAIL_CODE = "OPENAPI_GET_BOOK_LIST_FAIL_10000002";
    public static final String OPENAPI_GET_BOOK_LIST_FAIL_MSG = "获取图书列表信息失败";
    //获取图书详情信息失败
    public static final String OPENAPI_GET_BOOK_INFO_FAIL_CODE = "OPENAPI_GET_BOOK_INFO_FAIL_10000003";
    public static final String OPENAPI_GET_BOOK_INFO_FAIL_MSG = "获取图书详情信息失败";

    public static final String OPENAPI_GET_USER_INFO_FAIL_CODE = "OPENAPI_GET_USER_INFO_FAIL__10000001";
    public static final String OPENAPI_GET_USER_INFO_FAIL_MSG = "获取用户信息失败";

    public static final String OPENAPI_GET_USER_INFO_NOT_EXIST_CODE = "OPENAPI_GET_USER_INFO_NOT_EXIST_10000001";
    public static final String OPENAPI_GET_USER_INFO_NOT_EXIST_MSG = "用户不存在，请确认后重试";
    //获取图书分类信息失败
    public static final String OPENAPI_GET_JISHI_BOOK_FAIL_CODE = "OPENAPI_GET_JISHI_BOOK_FAIL_10000001";
    public static final String OPENAPI_GET_JISHI_BOOK_FAIL_MSG = "获取基石图书信息失败";

    /**-----------------------------------汉语拼音 start -----------------------------------------*/
    public static final String PINYIN_VERIFICATION_FAIL_CODE = "PINYIN_VERIFICATION_FAIL_10000001";
    public static final String PINYIN_NOT_EXIST_FAIL_CODE = "PINYIN_NOT_EXIST_FAIL_10000002";
    public static final String PINYIN_NOT_EXIST_FAIL_MSG = "拼音不存在或已被删除";
    /**-----------------------------------汉语拼音 end -----------------------------------------*/

    /**-----------------------------------树分类 start -----------------------------------------*/
    public static final String TREE_CLASSIFY_VERIFICATION_FAIL_CODE = "TREE_CLASSIFY_VERIFICATION_FAIL_10000001";
    /**-----------------------------------树分类 end -----------------------------------------*/

    /**-----------------------------------古诗文 start -----------------------------------------*/
    public static final String ANCIENT_POEM_VERIFICATION_FAIL_CODE = "ANCIENT_POEM_CLASSIFY_VERIFICATION_FAIL_10000001";
    /**-----------------------------------古诗文 end -----------------------------------------*/

    public static final String PARAM_NOT_NULL_CODE = "APP_PARAM_ERROR_10000001";
    public static final String PARAM_NOT_NULL_MSG = "参数不能为空";

    public static final String ID_NOT_NULL_CODE = "APP_ID_ERROR_10000002";
    public static final String ID_NOT_NULL_MSG = "ID不能为空";

    public static final String ID_LIST_NOT_EMPTY_CODE = "APP_ID_LIST_ERROR_10000003";
    public static final String ID_LIST_NOT_EMPTY_MSG = "ID列表不能为空";

    public static final String PAGE_INFO_NOT_NULL_CODE = "APP_PAGE_INFO_ERROR_10000004";
    public static final String PAGE_INFO_NOT_NULL_MSG = "分页参数不能为空";

    public static final String APP_MODULE_TITLE_REQUIRED_CODE = "APP_MODULE_TITLE_ERROR_10000005";
    public static final String APP_MODULE_TITLE_REQUIRED_MSG = "标题不能为空";

    public static final String APP_MODULE_DESC_REQUIRED_CODE = "APP_MODULE_DESC_ERROR_10000006";
    public static final String APP_MODULE_DESC_REQUIRED_MSG = "简介不能为空";

    public static final String APP_MODULE_CATEGORY_REQUIRED_CODE = "APP_MODULE_CATEGORY_ERROR_10000007";
    public static final String APP_MODULE_CATEGORY_REQUIRED_MSG = "所属模块不能为空";


    public static final String APP_MODULE_NAME_EXIST_CODE = "APP_MODULE_NAME_EXIST_10000008";
    public static final String APP_MODULE_NAME_EXIST_MSG = "模块名称已存在";

    public static final String APP_MODULE_STATUS_REQUIRED_CODE = "APP_MODULE_CATEGORY_ERROR_10000009";
    public static final String APP_MODULE_STATUS_REQUIRED_MSG = "上架状态不能为空";

    public static final String APP_MODULE_VISIBLE_REQUIRED_CODE = "APP_MODULE_CATEGORY_ERROR_10000010";
    public static final String APP_MODULE_VISIBLE_REQUIRED_MSG = "是否显示不能为空";

    public static final String APP_RESOURCE_REL_MODULE_ID_REQUIRED_CODE = "APP_RESOURCE_REL_MODULE_ID_ERROR_10000009";
    public static final String APP_RESOURCE_REL_MODULE_ID_REQUIRED_MSG = "模块ID不能为空";

    public static final String APP_RESOURCE_REL_RESOURCE_ID_REQUIRED_CODE = "APP_RESOURCE_REL_RESOURCE_ID_ERROR_10000010";
    public static final String APP_RESOURCE_REL_RESOURCE_ID_REQUIRED_MSG = "资源ID不能为空";

    public static final String APP_RESOURCE_REL_RESOURCE_TYPE_REQUIRED_CODE = "APP_RESOURCE_REL_RESOURCE_TYPE_ERROR_10000011";
    public static final String APP_RESOURCE_REL_RESOURCE_TYPE_REQUIRED_MSG = "资源类型不能为空";

    public static final String APP_RESOURCE_REL_SINGLE_RESOURCE_LIMIT_CODE = "APP_RESOURCE_REL_SINGLE_RESOURCE_LIMIT_10000012";
    public static final String APP_RESOURCE_REL_SINGLE_RESOURCE_LIMIT_MSG = "该类型只能选择一条资源";

    public static final String APP_QUESTION_EXT_EXIST_CODE = "APP_QUESTION_EXT_EXIST_CODE_10000013";
    public static final String APP_QUESTION_EXT_EXIST_MSG = "答题设置不能为空";

    public static final String APP_RESOURCE_REL_RESOURCE_EXIST_CODE = "APP_RESOURCE_REL_RESOURCE_EXIST_CODE_10000014";
    public static final String APP_RESOURCE_REL_RESOURCE_EXIST_MSG = "关联资源不能为空";


    public static final String ANCIENT_POEM_RECITE_CATEGORY_NOT_EXIST_CODE = "ANCIENT_POEM_RECITE_CATEGORY_NOT_ERROR_10000001";
    public static final String ANCIENT_POEM_RECITE_CATEGORY_NOT_EXIST_MSG = "古诗文背诵分类不存在";
    public static final String ANCIENT_POEM_RECITE_CATEGORY_EXIST_COLLECTION_CODE = "ANCIENT_POEM_RECITE_CATEGORY_ERROR_10000002";
    public static final String ANCIENT_POEM_RECITE_CATEGORY_EXIST_COLLECTION_MSG = "古诗分类下存在合辑，请先删除合辑";

    public static final String ANCIENT_POEM_RECITE_COLLECTION_NOT_EXIST_CODE = "ANCIENT_POEM_RECITE_COLLECTION_ERROR_10000001";
    public static final String ANCIENT_POEM_RECITE_COLLECTION_NOT_EXIST_MSG = "古诗文背诵分类不存在";
    public static final String ANCIENT_POEM_RECITE_COLLECTION_EXIST_POEM_CODE = "ANCIENT_POEM_RECITE_COLLECTION_ERROR_10000002";
    public static final String ANCIENT_POEM_RECITE_COLLECTION_EXIST_POEM_MSG = "古诗文背诵分类合辑下存在古诗，请先删除古诗";

    public static final String ANCIENT_POEM_BUSINESS_REF_NOT_EXIST_CODE = "ANCIENT_POEM_BUSINESS_REF_NOT_EXIST_ERROR_10000001";
    public static final String ANCIENT_POEM_BUSINESS_REF_NOT_EXIST_MSG = "又不存在的古诗文，请确认后重试";


    /**-----------------------------------电子样书-样书配置-水印模板 start -----------------------------------------*/
    public static final String APP_EBOOKS_CONFIG_WATERMARK_TEMPLATE_SAME_NAME_FAILED_CODE = "APP_EBOOKS_CONFIG_WATERMARK_TEMPLATE_SAME_NAME_FAILED_10000001";
    public static final String APP_EBOOKS_CONFIG_WATERMARK_TEMPLATE_SAME_NAME_FAILED_MSG = "存在相同名称的水印模板";

    /**-----------------------------------电子样书-样书配置-水印模板 end -----------------------------------------*/

    /**-----------------------------------电子样书-样书配置-图书分类 start -----------------------------------------*/
    public static final String APP_EBOOKS_CONFIG_CATEGORY_SAME_NAME_FAILED_CODE = "APP_EBOOKS_CONFIG_CATEGORY_SAME_NAME_FAILED_10000001";
    public static final String APP_EBOOKS_CONFIG_CATEGORY_SAME_NAME_FAILED_MSG = "存在相同名称分类，请更换其他名称";

    public static final String APP_EBOOKS_CONFIG_CATEGORY_EXIST_CHILD_FAILED_CODE = "APP_EBOOKS_CONFIG_CATEGORY_EXIST_CHILD_FAILED_10000001";
    public static final String APP_EBOOKS_CONFIG_CATEGORY_EXIST_CHILD_FAILED_MSG = "存在子图书分类，请删除子分类后重试";

    public static final String APP_EBOOKS_CONFIG_CATEGORY_EXIST_REF_FAILED_CODE = "APP_EBOOKS_CONFIG_CATEGORY_EXIST_REF_FAILED_10000001";
    public static final String APP_EBOOKS_CONFIG_CATEGORY_EXIST_REF_FAILED_MSG = "存在关联的样书，请在取消关联后重试";


    public static final String APP_EBOOKS_CONFIG_CATEGORY_NO_PARENT_ID_CODE = "APP_EBOOKS_CONFIG_CATEGORY_NO_PARENT_ID_CODE_10000001";
    public static final String APP_EBOOKS_CONFIG_CATEGORY_NO_PARENT_ID_MSG = "父节点ID不能为空";
    /**-----------------------------------电子样书-样书配置-图书分类 end -----------------------------------------*/


    /**-----------------------------------电子样书-样书配置-适用年级 start -----------------------------------------*/
    public static final String APP_EBOOKS_CONFIG_GRADE_SAME_NAME_FAILED_CODE = "APP_EBOOKS_CONFIG_GRADE_SAME_NAME_FAILED_10000001";
    public static final String APP_EBOOKS_CONFIG_GRADE_SAME_NAME_FAILED_MSG = "存在相同名称年级，请更换其他名称";

    public static final String APP_EBOOKS_CONFIG_GRADE_EXIST_REF_FAILED_CODE = "APP_EBOOKS_CONFIG_GRADE_EXIST_REF_FAILED_10000001";
    public static final String APP_EBOOKS_CONFIG_GRADE_EXIST_REF_FAILED_MSG = "选中项中包含已关联的年级，请确认后重试";

    /**-----------------------------------电子样书-样书配置-适用年级 end -----------------------------------------*/


    /**-----------------------------------电子样书-样书配置-阶段 start -----------------------------------------*/
    public static final String APP_EBOOKS_CONFIG_STAGE_EXIST_REF_FAILED_CODE = "APP_EBOOKS_CONFIG_STAGE_EXIST_REF_FAILED_10000001";
    public static final String APP_EBOOKS_CONFIG_STAGE_EXIST_REF_FAILED_MSG = "阶段存在关联引用,无法删除";

    /**-----------------------------------电子样书-样书配置-阶段 end -----------------------------------------*/

    /**-----------------------------------电子样书-样书配置-学科 start -----------------------------------------*/
    public static final String APP_EBOOKS_CONFIG_SUBJECT_EXIST_REF_FAILED_CODE = "APP_EBOOKS_CONFIG_SUBJECT_EXIST_REF_FAILED_10000001";
    public static final String APP_EBOOKS_CONFIG_SUBJECT_EXIST_REF_FAILED_MSG = "选项中包含已关联的学科,请确认后重试";

    /**-----------------------------------电子样书-样书配置-学科 end -----------------------------------------*/

    /**-----------------------------------电子样书-样书配置-教材版本 start -----------------------------------------*/
    public static final String APP_EBOOKS_CONFIG_TEXT_BOOK_EXIST_REF_FAILED_CODE = "APP_EBOOKS_CONFIG_TEXT_BOOK_EXIST_REF_FAILED_10000001";
    public static final String APP_EBOOKS_CONFIG_TEXT_BOOK_EXIST_REF_FAILED_MSG = "选项中包含已关联的教材版本,请确认后重试";

    /**-----------------------------------电子样书-样书配置-教材版本 end -----------------------------------------*/
    public static final String ANCIENT_POEM_BUSINESS_REF_ALREADY_EXIST_CODE = "ANCIENT_POEM_BUSINESS_REF_ALREADY_EXIST_ERROR_10000001";

    public static final String ANCIENT_POEM_BUSINESS_REF_ALREADY_EXIST_MSG = "古诗文重复";

    /**-----------------------------------电子书 start -----------------------------------------*/
    // 电子书相关错误码
    public static final String EBOOK_NOT_EXIST_CODE = "EBOOK_NOT_EXIST_10000001";
    public static final String EBOOK_NOT_EXIST_MSG = "单书不存在";

    public static final String EBOOK_ID_NOT_NULL_CODE = "EBOOK_ID_NOT_NULL_10000002";
    public static final String EBOOK_ID_NOT_NULL_MSG = "单书ID不能为空";

    public static final String EBOOK_SHELF_ASSOCIATED_CODE = "EBOOK_SHELF_ASSOCIATED_10000003";
    public static final String EBOOK_SHELF_ASSOCIATED_MSG = "图书已关联书架，请先从书架中移除后再删除";

    public static final String EBOOK_FILE_URL_NAME_EMPTY_CODE = "EBOOK_FILE_URL_NAME_EMPTY_10000004";
    public static final String EBOOK_FILE_URL_NAME_EMPTY_MSG = "文件地址和文件名不能为空";

    public static final String EBOOK_FILE_PROCESSING_CODE = "EBOOK_FILE_PROCESSING_10000005";
    public static final String EBOOK_FILE_PROCESSING_MSG = "存在文件正在处理中或处理失败的图书，请稍后重试";

    public static final String EBOOK_BUSINESS_TYPE_NOT_SUPPORT_CODE = "EBOOK_BUSINESS_TYPE_NOT_SUPPORT_10000006";
    public static final String EBOOK_BUSINESS_TYPE_NOT_SUPPORT_MSG = "不支持的业务类型";

    public static final String EBOOK_BATCH_QUERY_RESOURCE_FAILED_CODE = "EBOOK_BATCH_QUERY_RESOURCE_FAILED_10000007";
    public static final String EBOOK_BATCH_QUERY_RESOURCE_FAILED_MSG = "批量查询书籍资源失败";

    public static final String EBOOK_ENABLED_DELETE_CODE = "EBOOK_ENABLED_DELETE_10000008";
    public static final String EBOOK_ENABLED_DELETE_MSG = "存在已启用的图书，请将图书禁用后重试";

    // 书城相关错误码
    public static final String BOOKSTORE_ID_NOT_NULL_CODE = "BOOKSTORE_ID_NOT_NULL_10000001";
    public static final String BOOKSTORE_ID_NOT_NULL_MSG = "书城ID不能为空";

    public static final String BOOKSTORE_NOT_EXIST_CODE = "BOOKSTORE_NOT_EXIST_10000002";
    public static final String BOOKSTORE_NOT_EXIST_MSG = "书城不存在";

    public static final String BOOKSHELF_NOT_EXIST_CODE = "BOOKSHELF_NOT_EXIST_10000003";
    public static final String BOOKSHELF_NOT_EXIST_MSG = "书架不存在";

    public static final String BOOKSHELF_COVER_URL_EMPTY_CODE = "BOOKSHELF_COVER_URL_EMPTY_10000004";
    public static final String BOOKSHELF_COVER_URL_EMPTY_MSG = "书架封面URL为空";

    public static final String COVER_URL_BOOKID_EMPTY_CODE = "COVER_URL_BOOKID_EMPTY_10000005";
    public static final String COVER_URL_BOOKID_EMPTY_MSG = "封面URL和书架ID不能同时为空";

    public static final String PARAMS_NOT_NULL_CODE = "PARAMS_NOT_NULL_10000006";
    public static final String PARAMS_NOT_NULL_MSG = "参数不能为空";

    public static final String BOOKSHELF_ALREADY_EXIST_CODE = "BOOKSHELF_ALREADY_EXIST_10000007";
    public static final String BOOKSHELF_ALREADY_EXIST_MSG = "书架已存在于书城,不允许重复添加";

    // 书架相关错误码
    public static final String BOOKSHELF_ID_NOT_NULL_CODE = "BOOKSHELF_ID_NOT_NULL_10000001";
    public static final String BOOKSHELF_ID_NOT_NULL_MSG = "书架ID不能为空";

    public static final String BOOK_COVER_URL_EMPTY_CODE = "BOOK_COVER_URL_EMPTY_10000002";
    public static final String BOOK_COVER_URL_EMPTY_MSG = "单书封面URL为空";

    public static final String COVER_URL_BOOKID_EMPTY_SHELF_CODE = "COVER_URL_BOOKID_EMPTY_SHELF_10000003";
    public static final String COVER_URL_BOOKID_EMPTY_SHELF_MSG = "封面URL和单书ID不能同时为空";

    public static final String BOOK_ALREADY_EXIST_CODE = "BOOK_ALREADY_EXIST_10000004";
    public static final String BOOK_ALREADY_EXIST_MSG = "书籍已存在于书架,不允许重复添加";

    // 资源相关错误码
    public static final String RESOURCE_BATCH_QUERY_FAILED_CODE = "RESOURCE_BATCH_QUERY_FAILED_10000001";
    public static final String RESOURCE_BATCH_QUERY_FAILED_MSG = "按书架批量查询资源失败";
    /**-----------------------------------电子书 end -----------------------------------------*/

    /**-----------------------------------点读书 start -----------------------------------------*/
    // 点读书分类相关错误码
    public static final String POINT_READING_CATEGORY_NOT_EXIST_CODE = "POINT_READING_CATEGORY_NOT_EXIST_10000001";
    public static final String POINT_READING_CATEGORY_NOT_EXIST_MSG = "点读书分类不存在";

    public static final String POINT_READING_CATEGORY_NAME_EXIST_CODE = "POINT_READING_CATEGORY_NAME_EXIST_10000002";
    public static final String POINT_READING_CATEGORY_NAME_EXIST_MSG = "同级分类名称已存在";

    public static final String POINT_READING_CATEGORY_HAS_CHILDREN_CODE = "POINT_READING_CATEGORY_HAS_CHILDREN_10000003";
    public static final String POINT_READING_CATEGORY_HAS_CHILDREN_MSG = "存在子分类，无法删除";

    public static final String POINT_READING_CATEGORY_PARENT_NOT_EXIST_CODE = "POINT_READING_CATEGORY_PARENT_NOT_EXIST_10000004";
    public static final String POINT_READING_CATEGORY_PARENT_NOT_EXIST_MSG = "父级分类不存在";

    public static final String POINT_READING_CATEGORY_SELF_PARENT_CODE = "POINT_READING_CATEGORY_SELF_PARENT_10000005";
    public static final String POINT_READING_CATEGORY_SELF_PARENT_MSG = "不能将自己设为父级分类";

    public static final String POINT_READING_CATEGORY_DEFAULT_EDIT_CODE = "POINT_READING_CATEGORY_DEFAULT_EDIT_CODE_10000006";
    public static final String POINT_READING_CATEGORY_DEFAULT_EDIT_MSG = "默认分类不能编辑";

    public static final String POINT_READING_CATEGORY_DEFAULT_DELETE_CODE = "POINT_READING_CATEGORY_DEFAULT_DELETE_CODE_10000007";
    public static final String POINT_READING_CATEGORY_DEFAULT_DELETE_MSG = "默认分类不能删除";


    public static final String POINT_READING_CATEGORY_MOVE_NOT_SUPPORT_CODE = "POINT_READING_CATEGORY_MOVE_NOT_SUPPORT_CODE_10000008";


    // 点读书相关错误码
    public static final String POINT_READING_BOOK_NOT_EXIST_CODE = "POINT_READING_BOOK_NOT_EXIST_10000001";
    public static final String POINT_READING_BOOK_NOT_EXIST_MSG = "点读书不存在";

    public static final String POINT_READING_BOOK_NAME_EXIST_CODE = "POINT_READING_BOOK_NAME_EXIST_10000002";
    public static final String POINT_READING_BOOK_NAME_EXIST_MSG = "同分类下点读书名称已存在";

    public static final String POINT_READING_BOOK_MD5_EXIST_CODE = "POINT_READING_BOOK_MD5_EXIST_10000003";
    public static final String POINT_READING_BOOK_MD5_EXIST_MSG = "文件已存在，请勿重复上传";

    public static final String POINT_READING_BOOK_CATEGORY_NOT_EXIST_CODE = "POINT_READING_BOOK_CATEGORY_NOT_EXIST_10000004";
    public static final String POINT_READING_BOOK_CATEGORY_NOT_EXIST_MSG = "所属分类不存在";

    public static final String POINT_READING_FILE_URL_NAME_EMPTY_CODE = "POINT_READING_CHAPTER_BOOK_NOT_EXIST_10000005";
    public static final String POINT_READING_URL_NAME_EMPTY_MSG = "文件地址和文件名不能为空";

    public static final String POINT_READING_BOOK_FILE_PROCESSING_CODE = "POINT_READING_BOOK_FILE_PROCESSING_10000006";
    public static final String POINT_READING_BOOK_FILE_PROCESSING_MSG = "存在文件正在处理中或处理失败的图书，请稍后重试";

    public static final String POINT_READING_BOOK_STATUS_DELETE_CODE = "POINT_READING_BOOK_STATUS_DELETE_CODE_10000007";
    public static final String POINT_READING_BOOK_STATUS_DELETE_MSG = "存在已启用的点读书，请将点读书禁用后重试";

    // 点读书目录相关错误码
    public static final String POINT_READING_MENU_NOT_EXIST_CODE = "POINT_READING_MENU_NOT_EXIST_10000001";
    public static final String POINT_READING_MENU_NOT_EXIST_MSG = "点读书目录不存在";

    public static final String POINT_READING_MENU_BOOK_NOT_EXIST_CODE = "POINT_READING_MENU_BOOK_NOT_EXIST_10000002";
    public static final String POINT_READING_MENU_BOOK_NOT_EXIST_MSG = "所属点读书不存在";

    public static final String POINT_READING_MENU_NAME_EXIST_CODE = "POINT_READING_MENU_NAME_EXIST_10000003";
    public static final String POINT_READING_MENU_NAME_EXIST_MSG = "同级目录名称已存在";

    public static final String POINT_READING_MENU_PARENT_NOT_EXIST_CODE = "POINT_READING_MENU_PARENT_NOT_EXIST_10000004";
    public static final String POINT_READING_MENU_PARENT_NOT_EXIST_MSG = "父级目录不存在";

    public static final String POINT_READING_MENU_PARENT_BOOK_MISMATCH_CODE = "POINT_READING_MENU_PARENT_BOOK_MISMATCH_10000005";
    public static final String POINT_READING_MENU_PARENT_BOOK_MISMATCH_MSG = "父级目录必须属于同一本点读书";

    public static final String POINT_READING_MENU_SELF_PARENT_CODE = "POINT_READING_MENU_SELF_PARENT_10000006";
    public static final String POINT_READING_MENU_SELF_PARENT_MSG = "不能设置自己为父级目录";

    public static final String POINT_READING_MENU_HAS_CHILDREN_CODE = "POINT_READING_MENU_HAS_CHILDREN_10000007";
    public static final String POINT_READING_MENU_HAS_CHILDREN_MSG = "存在子目录，无法删除";

    public static final String POINT_READING_MENU_TYPE_ERR_CODE = "POINT_READING_MENU_TYPE_ERR_10000008";
    public static final String POINT_READING_MENU_TYPE_ERR_MSG = "点读书目录不能创建页面";

    public static final String POINT_READING_MENU_MOVE_NOT_SUPPORT_CODE = "POINT_READING_MENU_MOVE_NOT_SUPPORT_CODE_10000009";

    // 点读书章节相关错误码
    public static final String POINT_READING_CHAPTER_NOT_EXIST_CODE = "POINT_READING_CHAPTER_NOT_EXIST_10000001";
    public static final String POINT_READING_CHAPTER_NOT_EXIST_MSG = "点读书章节不存在";

    public static final String POINT_READING_CHAPTER_MENU_BOOK_MISMATCH_CODE = "POINT_READING_CHAPTER_MENU_BOOK_MISMATCH_10000002";
    public static final String POINT_READING_CHAPTER_MENU_BOOK_MISMATCH_MSG = "章节目录必须属于同一本点读书";

    public static final String POINT_READING_CHAPTER_NAME_EXIST_CODE = "POINT_READING_CHAPTER_NAME_EXIST_10000003";
    public static final String POINT_READING_CHAPTER_NAME_EXIST_MSG = "同目录下章节名称已存在";

    // 点读书热点相关错误码
    public static final String POINT_READING_HOTSPOT_NOT_EXIST_CODE = "POINT_READING_HOTSPOT_NOT_EXIST_10000001";
    public static final String POINT_READING_HOTSPOT_NOT_EXIST_MSG = "点读书热点不存在";

    public static final String POINT_READING_HOTSPOT_NAME_EXIST_CODE = "POINT_READING_HOTSPOT_NAME_EXIST_10000002";
    public static final String POINT_READING_HOTSPOT_NAME_EXIST_MSG = "同章节下热点名称已存在";

    public static final String POINT_READING_HOTSPOT_PARENT_NOT_EXIST_CODE = "POINT_READING_HOTSPOT_PARENT_NOT_EXIST_10000003";
    public static final String POINT_READING_HOTSPOT_PARENT_NOT_EXIST_MSG = "父节点不存在";

    public static final String POINT_READING_HOTSPOT_LEVEL_ERROR_CODE = "POINT_READING_HOTSPOT_LEVEL_ERROR_10000004";
    public static final String POINT_READING_HOTSPOT_LEVEL_ERROR_MSG = "子节点层级必须大于父节点层级";

    public static final String POINT_READING_HOTSPOT_ROOT_LEVEL_ERROR_CODE = "POINT_READING_HOTSPOT_ROOT_LEVEL_ERROR_10000005";
    public static final String POINT_READING_HOTSPOT_ROOT_LEVEL_ERROR_MSG = "根节点层级必须为1";

    public static final String POINT_READING_HOTSPOT_MEDIA_SAVE_FAIL_CODE = "POINT_READING_HOTSPOT_MEDIA_SAVE_FAIL_10000006";
    public static final String POINT_READING_HOTSPOT_MEDIA_SAVE_FAIL_MSG = "保存热点媒体引用失败";

    public static final String POINT_READING_HOTSPOT_MEDIA_UPDATE_FAIL_CODE = "POINT_READING_HOTSPOT_MEDIA_UPDATE_FAIL_10000007";
    public static final String POINT_READING_HOTSPOT_MEDIA_UPDATE_FAIL_MSG = "更新热点媒体引用失败";

    // 点读书模式配置相关错误码
    public static final String POINT_READING_MODE_CONFIG_NOT_EXIST_CODE = "POINT_READING_MODE_CONFIG_NOT_EXIST_10000001";
    public static final String POINT_READING_MODE_CONFIG_NOT_EXIST_MSG = "点读书模式配置不存在";

    public static final String POINT_READING_MODE_CONFIG_EXIST_CODE = "POINT_READING_MODE_CONFIG_EXIST_10000002";
    public static final String POINT_READING_MODE_CONFIG_EXIST_MSG = "该点读书下已存在相同的模式配置";

    // 点读书业务关联相关错误码
    public static final String POINT_READING_BUSINESS_REF_NOT_EXIST_CODE = "POINT_READING_BUSINESS_REF_NOT_EXIST_10000001";
    public static final String POINT_READING_BUSINESS_REF_NOT_EXIST_MSG = "业务关联不存在";

    public static final String POINT_READING_BUSINESS_REF_EXIST_CODE = "POINT_READING_BUSINESS_REF_EXIST_10000002";
    public static final String POINT_READING_BUSINESS_REF_EXIST_MSG = "该业务关联已存在";

    public static final String POINT_READING_BUSINESS_TYPE_INVALID_CODE = "POINT_READING_BUSINESS_TYPE_INVALID_10000003";
    public static final String POINT_READING_BUSINESS_TYPE_INVALID_MSG = "无效的业务类型";


    // 点读书章节相关错误码

    public static final String POINT_READING_CHAPTER_BOOK_NOT_EXIST_CODE = "POINT_READING_CHAPTER_BOOK_NOT_EXIST_10000002";
    public static final String POINT_READING_CHAPTER_BOOK_NOT_EXIST_MSG = "所属点读书不存在";

    // 点读书热点区域相关错误码

    public static final String POINT_READING_HOTSPOT_PAGE_NOT_EXIST_CODE = "POINT_READING_HOTSPOT_PAGE_NOT_EXIST_10000002";
    public static final String POINT_READING_HOTSPOT_PAGE_NOT_EXIST_MSG = "所属章节不存在";

    // 点读书热点验证相关错误码
    public static final String POINT_READING_HOTSPOT_DATA_NULL_CODE = "POINT_READING_HOTSPOT_DATA_NULL_10000010";
    public static final String POINT_READING_HOTSPOT_DATA_NULL_MSG = "热点数据不能为空";

    public static final String POINT_READING_HOTSPOT_ID_NULL_CODE = "POINT_READING_HOTSPOT_ID_NULL_10000011";
    public static final String POINT_READING_HOTSPOT_ID_NULL_MSG = "热点ID不能为空";

    public static final String POINT_READING_HOTSPOT_CHAPTER_ID_NULL_CODE = "POINT_READING_HOTSPOT_CHAPTER_ID_NULL_10000012";
    public static final String POINT_READING_HOTSPOT_CHAPTER_ID_NULL_MSG = "所属章节ID不能为空";

    public static final String POINT_READING_HOTSPOT_NAME_NULL_CODE = "POINT_READING_HOTSPOT_NAME_NULL_10000013";
    public static final String POINT_READING_HOTSPOT_NAME_NULL_MSG = "热点名称不能为空";

    public static final String POINT_READING_HOTSPOT_EVENT_TYPE_NULL_CODE = "POINT_READING_HOTSPOT_EVENT_TYPE_NULL_10000014";
    public static final String POINT_READING_HOTSPOT_EVENT_TYPE_NULL_MSG = "事件类型不能为空";

    public static final String POINT_READING_HOTSPOT_EVENT_TYPE_INVALID_CODE = "POINT_READING_HOTSPOT_EVENT_TYPE_INVALID_10000015";
    public static final String POINT_READING_HOTSPOT_EVENT_TYPE_INVALID_MSG = "无效的事件类型";

    public static final String POINT_READING_HOTSPOT_EVENT_TYPE_NOT_SUPPORT_CODE = "POINT_READING_HOTSPOT_EVENT_TYPE_NOT_SUPPORT_10000016";
    public static final String POINT_READING_HOTSPOT_EVENT_TYPE_NOT_SUPPORT_MSG = "不支持的事件类型：%s";

    public static final String POINT_READING_HOTSPOT_MEDIA_SOURCE_NULL_CODE = "POINT_READING_HOTSPOT_MEDIA_SOURCE_NULL_10000017";
    public static final String POINT_READING_HOTSPOT_MEDIA_SOURCE_NULL_MSG = "点读模式下媒体来源不能为空";

    public static final String POINT_READING_HOTSPOT_MEDIA_LIST_NULL_CODE = "POINT_READING_HOTSPOT_MEDIA_LIST_NULL_10000018";
    public static final String POINT_READING_HOTSPOT_MEDIA_LIST_NULL_MSG = "点读模式下媒体文件不能为空";

    public static final String POINT_READING_HOTSPOT_MEDIA_URL_NULL_CODE = "POINT_READING_HOTSPOT_MEDIA_URL_NULL_10000019";
    public static final String POINT_READING_HOTSPOT_MEDIA_URL_NULL_MSG = "媒体文件地址不能为空";

    public static final String POINT_READING_HOTSPOT_MEDIA_TYPE_NULL_CODE = "POINT_READING_HOTSPOT_MEDIA_TYPE_NULL_10000020";
    public static final String POINT_READING_HOTSPOT_MEDIA_TYPE_NULL_MSG = "媒体文件类型不能为空";

    public static final String POINT_READING_HOTSPOT_QUESTION_SETTINGS_NULL_CODE = "POINT_READING_HOTSPOT_QUESTION_SETTINGS_NULL_10000021";
    public static final String POINT_READING_HOTSPOT_QUESTION_SETTINGS_NULL_MSG = "答题模式下答题设置不能为空";

    public static final String POINT_READING_HOTSPOT_BUSINESS_ID_NULL_CODE = "POINT_READING_HOTSPOT_BUSINESS_ID_NULL_10000022";
    public static final String POINT_READING_HOTSPOT_BUSINESS_ID_NULL_MSG = "答题模式下业务ID不能为空";

    public static final String POINT_READING_HOTSPOT_BUSINESS_TYPE_NULL_CODE = "POINT_READING_HOTSPOT_BUSINESS_TYPE_NULL_10000023";
    public static final String POINT_READING_HOTSPOT_BUSINESS_TYPE_NULL_MSG = "答题模式下业务类型不能为空";

    public static final String POINT_READING_HOTSPOT_QUESTION_COUNT_INVALID_CODE = "POINT_READING_HOTSPOT_QUESTION_COUNT_INVALID_10000024";
    public static final String POINT_READING_HOTSPOT_QUESTION_COUNT_INVALID_MSG = "答题模式下出题数量必须大于0";

    public static final String POINT_READING_HOTSPOT_QUESTION_ORDER_NULL_CODE = "POINT_READING_HOTSPOT_QUESTION_ORDER_NULL_10000025";
    public static final String POINT_READING_HOTSPOT_QUESTION_ORDER_NULL_MSG = "答题模式下出题顺序不能为空";

    public static final String POINT_READING_HOTSPOT_VERIFY_TEXT_NULL_CODE = "POINT_READING_HOTSPOT_VERIFY_TEXT_NULL_10000026";
    public static final String POINT_READING_HOTSPOT_VERIFY_TEXT_NULL_MSG = "跟读模式下校验文本不能为空";

    public static final String POINT_READING_HOTSPOT_FOLLOW_READ_INVALID_CODE = "POINT_READING_HOTSPOT_FOLLOW_READ_INVALID_10000027";
    public static final String POINT_READING_HOTSPOT_FOLLOW_READ_INVALID_MSG = "跟读模式下必须启用跟读功能";

    public static final String POINT_READING_HOTSPOT_FILL_BLANK_CONTENT_NULL_CODE = "POINT_READING_HOTSPOT_FILL_BLANK_CONTENT_NULL_10000028";
    public static final String POINT_READING_HOTSPOT_FILL_BLANK_CONTENT_NULL_MSG = "填空内容不能为空";

    // 点读书目录树形结构相关错误码
    public static final String POINT_READING_MENU_TREE_PARAM_NULL_CODE = "POINT_READING_MENU_TREE_PARAM_NULL_10000029";
    public static final String POINT_READING_MENU_TREE_PARAM_NULL_MSG = "树形结构参数不能为空";

    public static final String POINT_READING_MENU_TREE_PARSE_FAIL_CODE = "POINT_READING_MENU_TREE_PARSE_FAIL_10000030";
    public static final String POINT_READING_MENU_TREE_PARSE_FAIL_MSG = "树形结构解析失败";

    public static final String POINT_READING_MENU_TYPE_HIERARCHY_ERROR_CODE = "POINT_READING_MENU_TYPE_HIERARCHY_ERROR_10000031";
    public static final String POINT_READING_MENU_TYPE_HIERARCHY_ERROR_MSG = "页面类型节点下不能有子节点";

    public static final String POINT_READING_MENU_DIRECTORY_CHILD_TYPE_ERROR_CODE = "POINT_READING_MENU_DIRECTORY_CHILD_TYPE_ERROR_10000032";
    public static final String POINT_READING_MENU_DIRECTORY_CHILD_TYPE_ERROR_MSG = "目录类型节点下不能有目录类型的子节点";

    public static final String POINT_READING_MENU_TREE_ALREADY_EXISTS_CODE = "POINT_READING_MENU_TREE_ALREADY_EXISTS_10000033";
    public static final String POINT_READING_MENU_TREE_ALREADY_EXISTS_MSG = "该点读书已存在目录树，不能重复新增";

    public static final String POINT_READING_HOTSPOT_FILL_BLANK_METHOD_NULL_CODE = "POINT_READING_HOTSPOT_FILL_BLANK_METHOD_NULL_CODE_10000034";
    public static final String POINT_READING_HOTSPOT_FILL_BLANK_METHOD_NULL_MSG = "填空模式不能为空";

    public static final String POINT_READING_HOTSPOT_FILL_BLANK_NUM_NULL_CODE = "POINT_READING_HOTSPOT_FILL_BLANK_NUM_NULL_CODE_10000035";
    public static final String POINT_READING_HOTSPOT_FILL_BLANK_NUM_NULL_MSG = "填空数量不能为空";

    // 图书分类业务关联相关错误码
    public static final String BOOKS_CATEGORY_BUSINESS_REF_NOT_EXIST_CODE = "BOOKS_CATEGORY_BUSINESS_REF_NOT_EXIST_10000001";
    public static final String BOOKS_CATEGORY_BUSINESS_REF_NOT_EXIST_MSG = "图书分类业务关联不存在";

    public static final String BOOKS_CATEGORY_BUSINESS_REF_EXIST_CODE = "BOOKS_CATEGORY_BUSINESS_REF_EXIST_10000002";
    public static final String BOOKS_CATEGORY_BUSINESS_REF_EXIST_MSG = "图书分类业务关联已存在";

    // 商品销售配置相关错误码
    public static final String PRODUCT_SALES_CONFIG_NOT_EXIST_CODE = "PRODUCT_SALES_CONFIG_NOT_EXIST_10000001";
    public static final String PRODUCT_SALES_CONFIG_NOT_EXIST_MSG = "商品销售配置不存在";

    public static final String PRODUCT_SALES_CONFIG_PRODUCT_NOT_EXIST_CODE = "PRODUCT_SALES_CONFIG_PRODUCT_NOT_EXIST_10000002";
    public static final String PRODUCT_SALES_CONFIG_PRODUCT_NOT_EXIST_MSG = "关联商品不存在";

    // 题库业务设置相关错误码
    public static final String APP_BUSINESS_SETTINGS_EXISTS_CODE = "APP_BUSINESS_SETTINGS_EXISTS_10000001";
    public static final String APP_BUSINESS_SETTINGS_EXISTS_MSG = "该业务已存在题库设置";

    public static final String APP_SETTINGS_ID_NOT_NULL_CODE = "APP_SETTINGS_ID_NOT_NULL_10000002";
    public static final String APP_SETTINGS_ID_NOT_NULL_MSG = "设置ID不能为空";

    public static final String APP_BUSINESS_TYPE_INVALID_CODE = "APP_BUSINESS_TYPE_INVALID_10000003";
    public static final String APP_BUSINESS_TYPE_INVALID_MSG = "无效的业务类型";
    /**-----------------------------------点读书 end -----------------------------------------*/

}
