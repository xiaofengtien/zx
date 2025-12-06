<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch">
      <el-form-item label="分类名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入分类名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['question:category:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-sort"
          size="mini"
          @click="toggleExpandAll"
        >展开/折叠</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table
      v-if="refreshTable"
      v-loading="loading"
      :data="categoryList"
      row-key="id"
      :default-expand-all="isExpandAll"
      :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
      style="width: 100%"
    >
      <el-table-column prop="name" label="分类名称" min-width="200"></el-table-column>
      <el-table-column prop="sortNum" label="排序" width="100" align="center"></el-table-column>
      <el-table-column prop="currentQuestionCount" label="当前题目数" width="120" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.currentQuestionCount || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="questionCount" label="总题目数" width="120" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.questionCount || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="childrenCount" label="子分类数" width="120" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.childrenCount || 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220" fixed="right">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['question:category:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-plus"
            @click="handleAdd(scope.row)"
            v-hasPermi="['question:category:add']"
          >新增</el-button>
          <el-button
            v-if="scope.row.fatherId != 0 && scope.row.isDefault != 1"
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['question:category:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改分类对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24" v-if="form.fatherId !== 0">
            <el-form-item label="上级分类" prop="fatherId">
              <treeselect 
                v-model="form.fatherId" 
                :options="categoryOptions" 
                :normalizer="normalizer" 
                placeholder="选择上级分类"
                :disabled="form.id !== undefined"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="分类名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { 
  getCategoryTree, 
  getCategory, 
  addCategory, 
  updateCategory, 
  deleteCategory,
  checkCategoryName
} from "@/api/exam/questionCategory"
import Treeselect from "@riophae/vue-treeselect"
import "@riophae/vue-treeselect/dist/vue-treeselect.css"

export default {
  name: "QuestionCategory",
  components: { Treeselect },
  data() {
    // 自定义分类名称验证（优化：防抖处理，避免重复提交）
    const validateName = (rule, value, callback) => {
      if (!value) {
        callback(new Error('分类名称不能为空'))
        return
      }
      
      // 清除之前的定时器
      if (this.nameCheckTimer) {
        clearTimeout(this.nameCheckTimer)
      }
      
      // 防抖：延迟300ms执行，避免频繁请求
      this.nameCheckTimer = setTimeout(() => {
        // 检查名称是否重复
        const checkData = {
          name: value,
          fatherId: this.form.fatherId || 0,
          id: this.form.id
        }
        checkCategoryName(checkData).then(response => {
          if (response.data) {
            callback(new Error('分类名称已存在，请使用其他名称'))
          } else {
            callback()
          }
        }).catch(() => {
          // 验证失败时不阻止提交，由后端再次验证
          callback()
        })
      }, 300)
    }

    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 表格树数据
      categoryList: [],
      // 分类树选项
      categoryOptions: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 是否展开，默认全部展开
      isExpandAll: true,
      // 重新渲染表格状态
      refreshTable: true,
      // 查询参数
      queryParams: {
        name: undefined,
        fatherId: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        name: [
          { required: true, message: "分类名称不能为空", trigger: "blur" },
          { validator: validateName, trigger: "blur" }
        ]
      },
      // 名称验证防抖定时器
      nameCheckTimer: null
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询分类列表 */
    getList() {
      this.loading = true
      const queryBO = {
        fatherId: this.queryParams.fatherId || undefined,
        name: this.queryParams.name || undefined
      }
      getCategoryTree(queryBO).then(response => {
        // 后端已经返回树形结构，直接使用，不需要再用handleTree处理
        this.categoryList = response.data || []
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    /** 转换分类数据结构 */
    normalizer(node) {
      if (node.children && !node.children.length) {
        delete node.children
      }
      return {
        id: node.id,
        label: node.name,
        children: node.children
      }
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        id: undefined,
        fatherId: 0,
        name: undefined
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    /** 新增按钮操作 */
    handleAdd(row) {
      this.reset()
      if (row != undefined) {
        this.form.fatherId = row.id
      } else {
        this.form.fatherId = 0
      }
      this.open = true
      this.title = "添加分类"
      // 加载分类树选项（后端已返回树形结构）
      getCategoryTree({}).then(response => {
        this.categoryOptions = response.data || []
      })
    },
    /** 展开/折叠操作 */
    toggleExpandAll() {
      this.refreshTable = false
      this.isExpandAll = !this.isExpandAll
      this.$nextTick(() => {
        this.refreshTable = true
      })
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const categoryIdBO = { id: row.id }
      getCategory(categoryIdBO).then(response => {
        this.form = {
          id: response.data.id,
          fatherId: response.data.fatherId || 0,
          name: response.data.name
        }
        this.open = true
        this.title = "修改分类"
        // 加载分类树选项（排除当前节点及其子节点，后端已返回树形结构）
        getCategoryTree({}).then(response => {
          const allCategories = response.data || []
          // 过滤掉当前节点及其子节点
          this.categoryOptions = this.filterTreeExcludeNode(allCategories, row.id)
          if (this.categoryOptions.length == 0) {
            // 如果没有可选父节点，添加根节点
            const rootNode = { id: 0, name: '根分类', children: [] }
            this.categoryOptions.push(rootNode)
          }
        })
      })
    },
    /** 过滤树节点（排除指定节点及其子节点） */
    filterTreeExcludeNode(tree, excludeId) {
      return tree.filter(node => {
        if (node.id === excludeId) {
          return false
        }
        if (node.children && node.children.length > 0) {
          node.children = this.filterTreeExcludeNode(node.children, excludeId)
        }
        return true
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // 清除名称验证定时器，避免与提交请求冲突
          if (this.nameCheckTimer) {
            clearTimeout(this.nameCheckTimer)
            this.nameCheckTimer = null
          }
          
          const categoryBO = {
            id: this.form.id,
            fatherId: this.form.fatherId || 0,
            name: this.form.name
          }
          if (this.form.id != undefined) {
            updateCategory(categoryBO).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            }).catch(() => {})
          } else {
            addCategory(categoryBO).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            }).catch(() => {})
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const categoryName = row.name
      this.$modal.confirm('是否确认删除名称为"' + categoryName + '"的分类？删除后，该分类下的所有子分类和题目也将被删除。').then(() => {
        const categoryIdsBO = {
          ids: [row.id]
        }
        return deleteCategory(categoryIdsBO)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container {
  width: 100%;
  
  .el-table {
    width: 100%;
  }
}
</style>

