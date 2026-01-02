package com.ruoyi.web.controller.student;

import java.util.List;
import java.util.Set;

import com.ruoyi.common.core.controller.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.student.archive.service.IStudentLoginService;
import com.ruoyi.student.archive.service.IStudentArchiveService;
import com.ruoyi.student.archive.domain.StudentArchive;
import com.ruoyi.system.service.ISysMenuService;
import com.ruoyi.system.service.ISysDictTypeService;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.student.archive.service.question.QuestionService;
import com.ruoyi.student.archive.service.question.QuestionCategoryService;
import com.ruoyi.student.archive.service.paper.IPaperService;
import com.ruoyi.common.utils.oss.exam.question.OssUtil;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.student.archive.domain.bo.question.*;
import com.ruoyi.student.archive.domain.dto.question.QuestionInfoDTO;
import com.ruoyi.student.archive.domain.dto.question.QuestionCategoryDTO;
import com.ruoyi.student.archive.domain.dto.paper.PaperDTO;
import com.ruoyi.student.archive.domain.bo.paper.PaperIdBO;
import com.ruoyi.student.archive.domain.bo.paper.PaperIdsBO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.page.TableDataInfo;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;

/**
 * 学员登录验证
 * 
 * @author ruoyi
 */
@Slf4j
@RestController
@RequestMapping("/student")
public class StudentLoginController extends BaseController {
    @Autowired
    private IStudentLoginService studentLoginService;

    @Autowired
    private ISysMenuService menuService;

    @Autowired
    private SysPermissionService permissionService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private IStudentArchiveService studentArchiveService;

    @Autowired
    private ISysDictTypeService dictTypeService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private QuestionCategoryService questionCategoryService;

    @Autowired
    private IPaperService paperService;

    @Autowired
    private com.ruoyi.student.archive.service.paper.impl.PaperPackageService paperPackageService;

    @Autowired
    private OssUtil ossUtil;

    @Autowired
    private com.ruoyi.student.archive.service.paper.IAppUserPaperService appUserPaperService;

    /**
     * 学员在线登录（需要验证码）
     * 
     * @param loginBody 登录信息
     * @return 结果
     */
    @PostMapping("/onlineLogin")
    public AjaxResult onlineLogin(@RequestBody LoginBody loginBody) {
        AjaxResult ajax = AjaxResult.success();
        // 生成令牌
        String token = studentLoginService.onlineLogin(loginBody.getUsername(), loginBody.getPassword(),
                loginBody.getCode(), loginBody.getUuid());
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    /**
     * 学员离线登录（使用离线凭证或密码）
     * 
     * @param loginBody 登录信息（可包含offlineCredential字段）
     * @return 结果
     */
    @PostMapping("/offlineLogin")
    public AjaxResult offlineLogin(@RequestBody java.util.Map<String, String> loginBody) {
        AjaxResult ajax = AjaxResult.success();
        String studentAccount = loginBody.get("username");
        String password = loginBody.get("password");
        String offlineCredential = loginBody.get("offlineCredential");

        // 生成令牌
        String token = studentLoginService.offlineLogin(studentAccount, password, offlineCredential);
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    /**
     * 获取学员信息
     * 
     * @return 用户信息
     */
    @GetMapping("/getInfo")
    public AjaxResult getInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getUser();

        // 固定返回学员角色
        Set<String> roles = new java.util.HashSet<>();
        roles.add("student");

        // 固定返回学员权限（答题相关权限）
        Set<String> permissions = new java.util.HashSet<>();
        permissions.add("exam:paper:view"); // 答题页面查看权限
        permissions.add("exam:paper:start"); // 开始答题权限
        permissions.add("exam:paper:submit"); // 提交答案权限
        permissions.add("exam:paper:score"); // 查看成绩权限

        // 更新LoginUser的权限
        loginUser.setPermissions(permissions);
        tokenService.refreshToken(loginUser);

        // 获取学员档案信息（包含适用考卷类型）
        com.ruoyi.student.archive.domain.StudentArchive archive = null;
        if (user.getUserType() != null && "01".equals(user.getUserType())) {
            // 学员类型，尝试查询学员档案
            // 优先通过系统用户ID查询（如果学员关联了系统用户，user_id 就是系统用户ID）
            if (user.getUserId() != null) {
                archive = studentLoginService.getStudentArchiveByUserId(user.getUserId());
            }

            // 如果通过 user_id 没找到，可能是没有关联系统用户的情况
            // 此时 userId 可能就是 archiveId，尝试通过 archiveId 查询
            if (archive == null && user.getUserId() != null) {
                archive = studentLoginService.getStudentArchiveById(user.getUserId());
            }

            // 如果还是没找到，尝试通过用户名（学员账号）查询
            if (archive == null && user.getUserName() != null) {
                archive = studentLoginService.getStudentArchiveByAccount(user.getUserName());
            }
        }

        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);

        // 如果找到学员档案，返回档案信息
        if (archive != null) {
            java.util.List<String> applicablePapers = archive.getApplicablePapers();
            System.out.println("学员档案ID: " + archive.getId());
            System.out.println("学员账号: " + archive.getStudentAccount());
            System.out.println("适用试卷类型 (applicablePapers): " + applicablePapers);
            System.out.println("适用试卷类型数量: " + (applicablePapers != null ? applicablePapers.size() : 0));

            ajax.put("archiveId", archive.getId());
            ajax.put("applicablePapers", applicablePapers != null ? applicablePapers : new java.util.ArrayList<>());

            // 检查是否需要强制修改密码（首次登录：pwdUpdateDate为null）
            boolean needForceChangePassword = archive.getPwdUpdateDate() == null;
            ajax.put("needForceChangePassword", needForceChangePassword);
        } else {
            System.out.println("未找到学员档案，userType: " + user.getUserType() + ", userId: " + user.getUserId()
                    + ", userName: " + user.getUserName());
            ajax.put("needForceChangePassword", false);
        }

        return ajax;
    }

    /**
     * 获取路由信息
     * 
     * @return 路由信息
     */
    @GetMapping("/getRouters")
    public AjaxResult getRouters() {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return AjaxResult.success(menuService.buildMenus(menus));
    }

    /**
     * 学员客户端同步数据接口（使用学员 token）
     * 返回所有学员档案数据和字典数据，用于客户端离线使用
     * 
     * @return 同步数据（包含学员档案列表和字典数据）
     */
    @GetMapping("/syncData")
    public AjaxResult syncData() {
        AjaxResult ajax = AjaxResult.success();

        try {
            // 1. 获取所有学员档案（只返回正常状态的）
            StudentArchive queryArchive = new StudentArchive();
            queryArchive.setStatus("0"); // 只查询正常状态的
            List<StudentArchive> archiveList = studentArchiveService.selectStudentArchiveList(queryArchive);

            // 2. 获取字典数据（需要同步的字典类型）
            java.util.List<String> dictTypes = java.util.Arrays.asList(
                    "paper_type", // 试卷类型
                    "sys_user_sex", // 性别
                    "grade", // 学段
                    "pupil", // 小学年级
                    "middle", // 中学年级
                    "high" // 高中年级
            );

            java.util.Map<String, List<SysDictData>> dictDataMap = new java.util.HashMap<>();
            for (String dictType : dictTypes) {
                List<SysDictData> dictDataList = dictTypeService.selectDictDataByType(dictType);
                if (dictDataList != null && !dictDataList.isEmpty()) {
                    dictDataMap.put(dictType, dictDataList);
                }
            }

            // 3. 返回数据
            ajax.put("archives", archiveList);
            ajax.put("dictData", dictDataMap);
            ajax.put("archiveCount", archiveList != null ? archiveList.size() : 0);
            ajax.put("dictTypeCount", dictDataMap.size());

            return ajax;
        } catch (Exception e) {
            return AjaxResult.error("同步数据失败：" + e.getMessage());
        }
    }

    /**
     * 公共同步接口（无需token，用于客户端启动时同步所有数据）
     * 返回所有业务数据（学员档案）和字典数据
     * 
     * @return 同步数据（包含学员档案列表和字典数据）
     */
    @GetMapping("/syncPublicData")
    public AjaxResult syncPublicData() {
        AjaxResult ajax = AjaxResult.success();

        try {
            // 1. 获取所有学员档案（只返回正常状态的）
            StudentArchive queryArchive = new StudentArchive();
            queryArchive.setStatus("0"); // 只查询正常状态的
            List<StudentArchive> archiveList = studentArchiveService.selectStudentArchiveList(queryArchive);

            // 2. 获取字典数据（需要同步的字典类型）
            java.util.List<String> dictTypes = java.util.Arrays.asList(
                    "paper_type", // 试卷类型
                    "sys_user_sex", // 性别
                    "grade", // 学段
                    "pupil", // 小学年级
                    "middle", // 中学年级
                    "high" // 高中年级
            );

            java.util.Map<String, List<SysDictData>> dictDataMap = new java.util.HashMap<>();
            for (String dictType : dictTypes) {
                List<SysDictData> dictDataList = dictTypeService.selectDictDataByType(dictType);
                if (dictDataList != null && !dictDataList.isEmpty()) {
                    dictDataMap.put(dictType, dictDataList);
                }
            }

            // 3. 返回数据
            ajax.put("archives", archiveList);
            ajax.put("dictData", dictDataMap);
            ajax.put("archiveCount", archiveList != null ? archiveList.size() : 0);
            ajax.put("dictTypeCount", dictDataMap.size());

            return ajax;
        } catch (Exception e) {
            return AjaxResult.error("同步数据失败：" + e.getMessage());
        }
    }

    /**
     * 客户端同步：获取题目分类树（学员专用，无需后台管理权限）
     * 
     * @return 分类树列表
     */
    @PostMapping("/sync/category/tree")
    public AjaxResult syncCategoryTree(@RequestBody(required = false) java.util.Map<String, Object> params) {
        try {
            // 验证学员身份
            SecurityUtils.getLoginUser();

            QuestionCategoryQueryBO queryBO = new QuestionCategoryQueryBO();
            if (params != null && params.containsKey("status")) {
                queryBO.setStatus((Integer) params.get("status"));
            } else {
                queryBO.setStatus(0); // 默认只获取启用状态的分类
            }

            List<QuestionCategoryDTO> tree = questionCategoryService.getCategoryTree(queryBO);
            return AjaxResult.success(tree);
        } catch (Exception e) {
            return AjaxResult.error("获取分类树失败：" + e.getMessage());
        }
    }

    /**
     * 客户端同步：分页获取题目列表（学员专用，无需后台管理权限）
     * 
     * @param pageBO 分页参数
     * @return 题目列表
     */
    @PostMapping("/sync/question/list")
    public TableDataInfo syncQuestionList(@RequestBody QuestionPageBO pageBO) {
        try {
            // 验证学员身份
            SecurityUtils.getLoginUser();

            if (pageBO == null) {
                pageBO = new QuestionPageBO();
            }

            // 默认只获取启用状态的题目
            if (pageBO.getStatus() == null) {
                pageBO.setStatus(1);
            }

            Page<QuestionInfoDTO> page = questionService.pageList(pageBO);
            TableDataInfo dataTable = getDataTable(page.getRecords());
            dataTable.setTotal(page.getTotal());
            return dataTable;
        } catch (Exception e) {
            TableDataInfo dataTable = new TableDataInfo();
            dataTable.setCode(500);
            dataTable.setMsg("获取题目列表失败：" + e.getMessage());
            return dataTable;
        }
    }

    /**
     * 客户端同步：获取题目详情（学员专用，无需后台管理权限）
     * 
     * @param idBO 题目ID
     * @return 题目详情
     */
    @PostMapping("/sync/question/detail")
    public AjaxResult syncQuestionDetail(@RequestBody QuestionIdBO idBO) {
        try {
            // 验证学员身份
            SecurityUtils.getLoginUser();

            if (idBO == null || idBO.getId() == null) {
                return AjaxResult.error("题目ID不能为空");
            }

            QuestionInfoDTO question = questionService.getQuestion(idBO);
            return AjaxResult.success(question);
        } catch (Exception e) {
            return AjaxResult.error("获取题目详情失败：" + e.getMessage());
        }
    }

    /**
     * 客户端同步：分页获取试卷列表（学员专用，无需后台管理权限）
     * 根据学员档案的适用试卷类型自动过滤
     * 
     * @param pageBO 分页参数
     * @return 试卷列表
     */
    @PostMapping("/sync/paper/list")
    public TableDataInfo syncPaperList(@RequestBody com.ruoyi.student.archive.domain.bo.paper.PaperPageBO pageBO) {
        try {
            // 验证学员身份
            com.ruoyi.common.core.domain.model.LoginUser loginUser = SecurityUtils.getLoginUser();

            if (pageBO == null) {
                pageBO = new com.ruoyi.student.archive.domain.bo.paper.PaperPageBO();
            }

            // 默认只获取启用状态的试卷
            if (pageBO.getStatus() == null) {
                pageBO.setStatus(1);
            }

            // 根据学员档案的适用试卷类型自动过滤
            // 获取学员档案
            com.ruoyi.student.archive.domain.StudentArchive archive = null;
            if (loginUser.getUserId() != null) {
                archive = studentArchiveService.getStudentArchiveByUserId(loginUser.getUserId());
            }

            // 如果学员档案存在且有适用试卷类型，则根据适用试卷类型过滤
            if (archive != null && archive.getApplicablePapers() != null && !archive.getApplicablePapers().isEmpty()) {
                // 如果前端没有指定试卷类型，则使用学员档案的适用试卷类型
                // 如果前端指定了试卷类型，则验证是否在学员档案的适用试卷类型中
                if (pageBO.getPaperType() == null || pageBO.getPaperType().trim().isEmpty()) {
                    // 前端未指定试卷类型，需要查询所有适用试卷类型的试卷
                    // 注意：这里需要修改pageList方法支持IN查询，或者分别查询后合并
                    // 为了简化，这里先只支持单个试卷类型查询
                    // 如果学员有多个适用试卷类型，客户端需要分别调用接口
                    if (archive.getApplicablePapers().size() == 1) {
                        pageBO.setPaperType(archive.getApplicablePapers().get(0));
                    }
                    // 如果有多个适用试卷类型，不设置paperType，返回所有试卷（由前端过滤）
                    // 或者可以修改pageList方法支持IN查询
                } else {
                    // 前端指定了试卷类型，验证是否在学员档案的适用试卷类型中
                    if (!archive.getApplicablePapers().contains(pageBO.getPaperType())) {
                        // 学员没有权限访问该试卷类型
                        TableDataInfo dataTable = new TableDataInfo();
                        dataTable.setCode(403);
                        dataTable.setMsg("您没有权限访问该试卷类型");
                        return dataTable;
                    }
                }
            }

            Page<PaperDTO> page = paperService.pageList(pageBO);
            TableDataInfo dataTable = getDataTable(page.getRecords());
            dataTable.setTotal(page.getTotal());
            return dataTable;
        } catch (Exception e) {
            TableDataInfo dataTable = new TableDataInfo();
            dataTable.setCode(500);
            dataTable.setMsg("获取试卷列表失败：" + e.getMessage());
            return dataTable;
        }
    }

    /**
     * 客户端同步：获取试卷详情（学员专用，无需后台管理权限）
     * 
     * @param idBO 试卷ID
     * @return 试卷详情
     */
    @PostMapping("/sync/paper/detail")
    public AjaxResult syncPaperDetail(@RequestBody com.ruoyi.student.archive.domain.bo.paper.PaperIdBO idBO) {
        try {
            // 验证学员身份
            SecurityUtils.getLoginUser();

            if (idBO == null || idBO.getId() == null) {
                return AjaxResult.error("试卷ID不能为空");
            }

            PaperDTO paper = paperService.getPaperById(idBO.getId());
            return AjaxResult.success(paper);
        } catch (Exception e) {
            return AjaxResult.error("获取试卷详情失败：" + e.getMessage());
        }
    }

    /**
     * 客户端同步：根据试卷ID列表查询试卷列表（学员专用，无需后台管理权限）
     * 根据学员档案的适用试卷ID列表验证权限，只返回学员有权限的试卷
     * 
     * @param idsBO 试卷ID列表参数
     * @return 试卷列表
     */
    @PostMapping("/sync/paper/listByIds")
    public AjaxResult syncPaperListByIds(
            @Validated @RequestBody com.ruoyi.student.archive.domain.bo.paper.PaperIdsBO idsBO) {
        try {
            // 验证学员身份
            com.ruoyi.common.core.domain.model.LoginUser loginUser = SecurityUtils.getLoginUser();

            if (idsBO == null || idsBO.getIds() == null || idsBO.getIds().isEmpty()) {
                return AjaxResult.error("试卷ID列表不能为空");
            }

            // 获取学员档案
            com.ruoyi.student.archive.domain.StudentArchive archive = null;
            if (loginUser.getUserId() != null) {
                archive = studentArchiveService.getStudentArchiveByUserId(loginUser.getUserId());
            }

            // 如果学员档案不存在，返回空列表
            if (archive == null) {
                log.warn("学员档案不存在，userId: {}", loginUser.getUserId());
                return AjaxResult.success(new java.util.ArrayList<>());
            }

            // 获取学员配置的适用试卷ID列表
            java.util.List<Integer> applicablePaperIds = archive.getApplicablePaperIds();
            if (applicablePaperIds == null || applicablePaperIds.isEmpty()) {
                log.warn("学员未配置适用试卷ID列表，userId: {}", loginUser.getUserId());
                return AjaxResult.success(new java.util.ArrayList<>());
            }

            // 验证请求的试卷ID列表是否在学员的适用试卷ID列表中（安全验证）
            java.util.List<Integer> requestedIds = idsBO.getIds();
            java.util.List<Integer> validIds = new java.util.ArrayList<>();
            for (Integer id : requestedIds) {
                if (applicablePaperIds.contains(id)) {
                    validIds.add(id);
                } else {
                    log.warn("学员尝试访问未授权的试卷ID: {}, userId: {}", id, loginUser.getUserId());
                }
            }

            // 如果没有有效的试卷ID，返回空列表
            if (validIds.isEmpty()) {
                log.warn("学员请求的试卷ID列表中没有有效的试卷，userId: {}, requestedIds: {}", loginUser.getUserId(), requestedIds);
                return AjaxResult.success(new java.util.ArrayList<>());
            }

            // 根据有效的试卷ID列表查询试卷
            java.util.List<PaperDTO> papers = paperService.listByIds(validIds);

            log.info("学员查询试卷列表成功，userId: {}, 请求数量: {}, 有效数量: {}, 返回数量: {}",
                    loginUser.getUserId(), requestedIds.size(), validIds.size(), papers.size());

            return AjaxResult.success(papers);
        } catch (Exception e) {
            log.error("根据试卷ID列表查询试卷失败", e);
            return AjaxResult.error("获取试卷列表失败：" + e.getMessage());
        }
    }

    /**
     * 客户端同步：下载试卷包（学员专用，无需后台管理权限）
     * 支持HTTP Range断点续传（流式下载，优化大文件下载性能）
     * 根据学员档案的适用试卷ID列表验证权限
     * 
     * @param idBO        试卷ID
     * @param rangeHeader Range请求头（可选）
     * @param response    HTTP响应
     */
    /**
     * 下载快速启动包（用于快速显示试卷列表和操作提示页面）
     */
    @PostMapping("/sync/paper/package/downloadQuick")
    public void syncDownloadQuickStartPackage(
            @RequestBody PaperIdBO idBO,
            HttpServletResponse response) {
        java.io.OutputStream outputStream = null;
        try {
            // 验证学员身份
            com.ruoyi.common.core.domain.model.LoginUser loginUser = SecurityUtils.getLoginUser();

            if (idBO == null || idBO.getId() == null) {
                throw new RuntimeException("试卷ID不能为空");
            }

            // 下载快速启动包（使用注入的paperService）
            byte[] packageData = paperService.downloadQuickStartPackage(idBO.getId());

            // 验证数据
            if (packageData == null || packageData.length == 0) {
                log.error("快速启动包数据为空，试卷ID：{}", idBO.getId());
                throw new RuntimeException("快速启动包数据为空");
            }

            // 验证ZIP文件头
            if (packageData.length < 4 || packageData[0] != 0x50 || packageData[1] != 0x4B) {
                log.error("下载的快速启动包格式不正确，试卷ID：{}，大小：{} 字节", idBO.getId(), packageData.length);
                throw new RuntimeException("快速启动包格式不正确");
            }

            // 设置响应头
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"quick_start.zip\"");
            response.setContentLength(packageData.length);

            // 写入响应
            outputStream = response.getOutputStream();
            outputStream.write(packageData);
            outputStream.flush();

            log.info("快速启动包下载成功，试卷ID：{}，大小：{} 字节", idBO.getId(), packageData.length);
        } catch (Exception e) {
            log.error("下载快速启动包失败，试卷ID：{}", idBO != null ? idBO.getId() : null, e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"msg\":\"下载快速启动包失败：" + e.getMessage() + "\"}");
            } catch (Exception ex) {
                log.error("写入错误响应失败", ex);
            }
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    log.error("关闭输出流失败", e);
                }
            }
        }
    }

    @PostMapping("/sync/paper/package/download")
    public void syncDownloadPaperPackage(
            @RequestBody PaperIdBO idBO,
            @org.springframework.web.bind.annotation.RequestHeader(value = "Range", required = false) String rangeHeader,
            HttpServletResponse response) {
        java.io.InputStream inputStream = null;
        java.io.OutputStream outputStream = null;
        try {
            // 验证学员身份
            com.ruoyi.common.core.domain.model.LoginUser loginUser = SecurityUtils.getLoginUser();

            if (idBO == null || idBO.getId() == null) {
                throw new RuntimeException("试卷ID不能为空");
            }

            // 获取试卷信息
            PaperDTO paper = paperService.getPaperById(idBO.getId());
            if (paper == null || "2".equals(paper.getDelFlag())) {
                throw new RuntimeException("试卷不存在");
            }

            // 验证学员是否有权限下载该试卷包（根据适用试卷ID列表）
            // 获取学员档案
            com.ruoyi.student.archive.domain.StudentArchive archive = null;
            if (loginUser.getUserId() != null) {
                archive = studentArchiveService.getStudentArchiveByUserId(loginUser.getUserId());
            }

            if (archive == null) {
                throw new RuntimeException("未找到学员档案");
            }

            // 检查试卷ID是否在学员的适用试卷ID列表中
            java.util.List<Integer> applicablePaperIds = archive.getApplicablePaperIds();
            if (applicablePaperIds == null || applicablePaperIds.isEmpty()) {
                throw new RuntimeException("学员未配置适用试卷ID列表");
            }

            if (!applicablePaperIds.contains(idBO.getId())) {
                log.warn("学员尝试下载未授权的试卷包，userId: {}, paperId: {}, applicablePaperIds: {}",
                        loginUser.getUserId(), idBO.getId(), applicablePaperIds);
                throw new RuntimeException("您没有权限下载该试卷包（试卷ID不在适用列表中）");
            }

            // 检查是否已生成试卷包
            if (com.ruoyi.common.utils.StringUtils.isEmpty(paper.getPackageHash()) || paper.getPackageSize() == null) {
                throw new RuntimeException("试卷包尚未生成，请先生成试卷包");
            }

            // 构建试卷包文件名
            Integer currentVersion = paper.getVersion() != null ? paper.getVersion() : 0;
            String packageFileName = "paper_packages/" + paper.getPaperCode() + "_v" + currentVersion + ".zip";

            // 获取文件大小
            long fileSize = ossUtil.getFileSize(packageFileName);

            // 解析Range请求
            long start = 0;
            long end = fileSize - 1;
            long contentLength = fileSize;
            int statusCode = 200;

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.substring(6).split("-");
                start = Long.parseLong(ranges[0]);
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                }
                if (end >= fileSize) {
                    end = fileSize - 1;
                }
                contentLength = end - start + 1;
                statusCode = 206; // Partial Content
            }

            // 设置响应头
            response.setContentType("application/zip");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Accept-Ranges", "bytes");
            response.setHeader("Content-Length", String.valueOf(contentLength));

            if (statusCode == 206) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range",
                        String.format("bytes %d-%d/%d", start, end, fileSize));
            }

            // 文件名URL编码
            String fileName = "paper_" + idBO.getId() + ".zip";
            String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);

            // 流式下载（支持Range请求）
            inputStream = ossUtil.downloadFileToStream(packageFileName, start, end);
            outputStream = response.getOutputStream();

            // 使用缓冲区流式传输
            byte[] buffer = new byte[8192]; // 8KB缓冲区
            long bytesTransferred = 0;
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                bytesTransferred += bytesRead;
            }
            outputStream.flush();

            log.info("客户端流式下载完成，试卷ID：{}，Range: {}-{}，传输大小: {} 字节",
                    idBO.getId(), start, end, bytesTransferred);

        } catch (Exception e) {
            log.error("客户端下载试卷包失败，试卷ID：{}", idBO.getId(), e);
            // 如果响应还没有提交，设置错误状态码和错误信息
            if (!response.isCommitted()) {
                try {
                    response.reset(); // 重置响应
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType("application/json;charset=UTF-8");
                    response.setCharacterEncoding("UTF-8");

                    java.io.PrintWriter writer = response.getWriter();
                    writer.write("{\"code\":500,\"msg\":\"下载试卷包失败：" + e.getMessage() + "\"}");
                    writer.flush();
                } catch (Exception ex) {
                    // 如果无法写入错误响应，记录日志
                    log.error("无法写入错误响应", ex);
                }
            } else {
                // 响应已提交，只能记录日志
                log.error("下载试卷包失败，但响应已提交", e);
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    log.error("关闭输入流失败", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    log.error("关闭输出流失败", e);
                }
            }
        }
    }

    /**
     * 客户端提交答题结果（学员专用，支持离线同步）
     * 接收客户端的答题数据并保存到数据库
     * 
     * @param submitBO 提交试卷参数
     * @return 结果
     */
    @PostMapping("/paper/submit")
    public AjaxResult submitPaper(
            @Validated @RequestBody com.ruoyi.student.archive.domain.bo.paper.SubmitPaperBO submitBO) {
        try {
            // 验证学员身份
            com.ruoyi.common.core.domain.model.LoginUser loginUser = SecurityUtils.getLoginUser();

            if (submitBO == null) {
                return AjaxResult.error("提交数据不能为空");
            }

            // 如果客户端没有传递 appUserId，使用当前登录用户的ID
            if (submitBO.getAppUserId() == null) {
                // 获取学员档案以获取正确的 appUserId
                com.ruoyi.student.archive.domain.StudentArchive archive = null;
                if (loginUser.getUserId() != null) {
                    archive = studentArchiveService.getStudentArchiveByUserId(loginUser.getUserId());
                }

                if (archive != null) {
                    submitBO.setAppUserId(archive.getId().intValue());
                } else {
                    // 如果没有找到学员档案，使用系统用户ID
                    submitBO.setAppUserId(loginUser.getUserId().intValue());
                }
            }

            log.info("客户端提交答题结果，appUserId: {}, businessType: {}, businessId: {}, questionCount: {}",
                    submitBO.getAppUserId(),
                    submitBO.getBusinessType(),
                    submitBO.getBusinessId(),
                    submitBO.getQuestionResults() != null ? submitBO.getQuestionResults().size() : 0);

            // 调用服务层保存答题结果
            com.ruoyi.student.archive.domain.dto.paper.UserPaperDTO result = appUserPaperService.submitPaper(submitBO);

            log.info("答题结果保存成功，paperInfoId: {}", result != null ? result.getId() : "null");

            return AjaxResult.success("答题结果提交成功", result);
        } catch (com.ruoyi.common.exception.ServiceException e) {
            log.error("提交答题结果失败：{}", e.getMessage());
            return AjaxResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("提交答题结果失败", e);
            return AjaxResult.error("提交答题结果失败：" + e.getMessage());
        }
    }
}
