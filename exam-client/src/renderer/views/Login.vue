<template>
  <div class="login-container">
    <transition name="fade-scale">
    <div v-if="pageReady" class="login-box">
      <div class="login-header">
        <div class="logo-container">
          <img :src="logoImage" alt="择学" class="logo-img" />
        </div>
<!--        <h2 class="login-title">择学考试端</h2>-->
      </div>
      
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane v-if="isOnline" label="在线登录" name="online">
          <el-form ref="onlineForm" :model="onlineForm" :rules="onlineRules" label-width="100px">
            <el-form-item label="学员账号" prop="username">
              <el-input v-model="onlineForm.username" placeholder="请输入学员账号" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="onlineForm.password" type="password" placeholder="请输入密码" show-password />
            </el-form-item>
            <el-form-item label="验证码" prop="code">
              <div class="captcha-container">
                <el-input v-model="onlineForm.code" placeholder="请输入验证码" class="captcha-input" @keyup.enter.native="handleOnlineLogin" />
                <img :src="captchaImage" @click="refreshCaptcha" class="captcha-image" alt="验证码" />
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleOnlineLogin" :loading="loading">登录</el-button>
              <el-button @click="refreshCaptcha">刷新验证码</el-button>
            </el-form-item>
            <el-form-item>
              <el-link type="primary" @click="showForgotPasswordDialog" style="float: right;">忘记密码？</el-link>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <el-tab-pane v-if="!isOnline" label="离线登录" name="offline">
          <el-form ref="offlineForm" :model="offlineForm" :rules="offlineRules" label-width="100px">
            <el-form-item label="学员账号" prop="username">
              <el-input v-model="offlineForm.username" placeholder="请输入学员账号" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="offlineForm.password" type="password" placeholder="请输入密码" show-password @keyup.enter.native="handleOfflineLogin" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleOfflineLogin" :loading="loading">登录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 忘记密码对话框 -->
    <el-dialog
      title="忘记密码"
      :visible.sync="forgotPasswordDialogVisible"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form ref="forgotPasswordForm" :model="forgotPasswordForm" :rules="forgotPasswordRules" label-width="100px">
        <el-form-item label="学员账号" prop="studentAccount">
          <el-input v-model="forgotPasswordForm.studentAccount" placeholder="请输入学员账号" />
        </el-form-item>
        <el-form-item label="手机号码" prop="phoneNumber">
          <el-input v-model="forgotPasswordForm.phoneNumber" placeholder="请输入手机号码" maxlength="11" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="forgotPasswordForm.newPassword" type="password" placeholder="请输入新密码（6-20位）" show-password maxlength="20" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="forgotPasswordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password maxlength="20" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="forgotPasswordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleForgotPassword" :loading="forgotPasswordLoading">确定</el-button>
      </div>
    </el-dialog>

    <!-- 首次登录强制修改密码对话框 -->
    <el-dialog
      title="首次登录修改密码"
      :visible.sync="forceChangePasswordDialogVisible"
      width="450px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <el-alert
        title="为了账户安全，首次登录必须修改密码！"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 20px;"
      />
      <el-form ref="changePasswordForm" :model="changePasswordForm" :rules="changePasswordRules" label-width="100px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="changePasswordForm.oldPassword" type="password" placeholder="请输入当前密码" show-password maxlength="20" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="changePasswordForm.newPassword" type="password" placeholder="请输入新密码（6-20位）" show-password maxlength="20" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="changePasswordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password maxlength="20" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="handleForceChangePassword" :loading="changePasswordLoading">确定</el-button>
      </div>
    </el-dialog>
    </transition>
  </div>
</template>

<script>
const { ipcRenderer } = require('electron')
const axios = require('axios')

export default {
  name: 'Login',
  data() {
    const validateConfirmPasswordForForgot = (rule, value, callback) => {
      if (value !== this.forgotPasswordForm.newPassword) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    const validateConfirmPasswordForChange = (rule, value, callback) => {
      if (value !== this.changePasswordForm.newPassword) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      activeTab: 'online',
      isOnline: true, // 网络状态，默认为true，会在mounted中更新
      isBackendAvailable: false, // 后端服务是否可用
      loading: false,
      captchaImage: '',
      captchaUuid: '',
      onlineForm: {
        username: '',
        password: '',
        code: '',
        uuid: ''
      },
      offlineForm: {
        username: '',
        password: ''
      },
      onlineRules: {
        username: [
          { required: true, message: '请输入学员账号', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ],
        code: [
          { required: true, message: '请输入验证码', trigger: 'blur' }
        ]
      },
      offlineRules: {
        username: [
          { required: true, message: '请输入学员账号', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' }
        ]
      },
      // 忘记密码对话框
      forgotPasswordDialogVisible: false,
      forgotPasswordLoading: false,
      forgotPasswordForm: {
        studentAccount: '',
        phoneNumber: '',
        newPassword: '',
        confirmPassword: ''
      },
      forgotPasswordRules: {
        studentAccount: [
          { required: true, message: '请输入学员账号', trigger: 'blur' }
        ],
        phoneNumber: [
          { required: true, message: '请输入手机号码', trigger: 'blur' },
          {
            pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
            message: '请输入正确的手机号码',
            trigger: 'blur'
          }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度必须介于 6 和 20 之间', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入新密码', trigger: 'blur' },
          { validator: validateConfirmPasswordForForgot, trigger: 'blur' }
        ]
      },
      // 强制修改密码对话框
      forceChangePasswordDialogVisible: false,
      changePasswordLoading: false,
      changePasswordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      changePasswordRules: {
        oldPassword: [
          { required: true, message: '请输入当前密码', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度必须介于 6 和 20 之间', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入新密码', trigger: 'blur' },
          { validator: validateConfirmPasswordForChange, trigger: 'blur' }
        ]
      },
      // 当前登录的用户信息（用于强制修改密码）
      currentUserInfo: null,
      // Logo图片 - file-loader返回字符串路径
      logoImage: require('@/assets/images/logo.png'),
      // 页面初始化状态
      pageReady: false
    }
  },
  async mounted() {
    // 获取网络状态
    try {
      const networkStatus = await ipcRenderer.invoke('app:getNetworkStatus')
      this.isOnline = networkStatus
      console.log('网络状态:', this.isOnline ? '在线' : '离线')
      
      // 如果网络在线，检查后端服务是否可用
      if (this.isOnline) {
        await this.checkBackendService()
        if (this.isBackendAvailable) {
          this.activeTab = 'online'
          this.refreshCaptcha()
        } else {
          // 后端服务不可用，切换到离线模式
          console.log('后端服务不可用，切换到离线登录模式')
          this.activeTab = 'offline'
        }
      } else {
        this.activeTab = 'offline'
      }
    } catch (error) {
      console.error('获取网络状态失败:', error)
      // 默认显示在线登录，但先检查后端服务
      this.isOnline = true
      await this.checkBackendService()
      if (this.isBackendAvailable) {
        this.refreshCaptcha()
      } else {
        this.activeTab = 'offline'
      }
    }
    
    // 页面初始化完成，显示内容
    this.$nextTick(() => {
      this.pageReady = true
    })
  },
  methods: {
    /**
     * 检查后端服务是否可用
     */
    async checkBackendService() {
      try {
        const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080'
        // 使用一个简单的健康检查接口，或者尝试获取验证码接口
        // 这里使用一个轻量级的请求来检查服务是否可用
        const response = await axios.get(`${API_BASE_URL}/captchaImage`, {
          headers: {
            isToken: false
          },
          timeout: 3000 // 3秒超时
        })
        
        // 如果能够成功连接（即使返回错误码也算服务可用）
        this.isBackendAvailable = true
        console.log('后端服务可用')
        return true
      } catch (error) {
        // 连接失败或超时，认为后端服务不可用
        this.isBackendAvailable = false
        console.log('后端服务不可用:', error.message)
        return false
      }
    },
    
    async refreshCaptcha() {
      // 离线模式下不需要获取验证码，直接返回
      if (!this.isOnline) {
        console.log('离线模式，跳过验证码获取')
        return
      }
      
      // 检查后端服务是否可用
      if (!this.isBackendAvailable) {
        console.log('后端服务不可用，跳过验证码获取')
        // 尝试重新检查后端服务
        const isAvailable = await this.checkBackendService()
        if (!isAvailable) {
          // 后端服务仍然不可用，切换到离线模式
          this.activeTab = 'offline'
          return
        }
      }
      
      try {
        // Electron 应用直接连接后端，不需要 /dev-api 前缀（该前缀仅用于前端代理）
        const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080'
        
        // 调用 API 获取验证码（返回 JSON，包含 base64 图片）
        const response = await axios.get(`${API_BASE_URL}/captchaImage`, {
          headers: {
            isToken: false
          }
        })
        
        if (response.data && response.data.code === 200) {
          // 后端返回的是 base64 编码的图片
          this.captchaImage = "data:image/gif;base64," + response.data.img
          this.onlineForm.uuid = response.data.uuid
          this.captchaUuid = response.data.uuid
        } else {
          throw new Error(response.data?.msg || '获取验证码失败')
        }
      } catch (error) {
        console.error('获取验证码失败:', error)
        // 离线模式下不显示错误提示
        if (!this.isOnline) {
          console.log('离线模式，验证码获取失败但不显示错误')
          return
        }
        
        // 更新后端服务状态
        this.isBackendAvailable = false
        
        // 如果是连接错误，切换到离线模式
        if (error.code === 'ECONNREFUSED' || 
            error.message.includes('ECONNREFUSED') || 
            error.message.includes('Network Error') ||
            error.code === 'ETIMEDOUT' ||
            error.message.includes('timeout')) {
          console.log('后端服务连接失败，切换到离线登录模式')
          this.activeTab = 'offline'
          // 不显示错误提示，因为已经切换到离线模式
          return
        }
        
        // 其他错误才显示错误提示
        this.$message.error('获取验证码失败：' + (error.message || '未知错误'))
        // 显示占位图片
        this.captchaImage = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTIwIiBoZWlnaHQ9IjQwIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciPjxyZWN0IHdpZHRoPSIxMjAiIGhlaWdodD0iNDAiIGZpbGw9IiNmNWY1ZjUiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1zaXplPSIxMiIgZmlsbD0iIzk5OSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPuaXoOazleWKoOi9veWbvueJhzwvdGV4dD48L3N2Zz4='
      }
    },
    
    generateUUID() {
      return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0
        const v = c === 'x' ? r : (r & 0x3 | 0x8)
        return v.toString(16)
      })
    },
    
    async handleTabClick(tab) {
      // 只有在在线模式下切换到在线登录标签页时才获取验证码
      if (tab.name === 'online' && this.isOnline) {
        // 先检查后端服务是否可用
        if (!this.isBackendAvailable) {
          await this.checkBackendService()
        }
        if (this.isBackendAvailable) {
          this.refreshCaptcha()
        } else {
          // 后端服务不可用，切换回离线模式
          this.$message.warning('后端服务不可用，已切换到离线登录模式')
          this.activeTab = 'offline'
        }
      }
    },
    
    async handleOnlineLogin() {
      this.$refs.onlineForm.validate(async (valid) => {
        if (!valid) return
        
        console.log('开始登录，表单数据:', {
          username: this.onlineForm.username,
          code: this.onlineForm.code,
          uuid: this.onlineForm.uuid
        })
        
        this.loading = true
        try {
          const result = await ipcRenderer.invoke('login:online', {
            username: this.onlineForm.username,
            password: this.onlineForm.password,
            code: this.onlineForm.code,
            uuid: this.onlineForm.uuid
          })
          
          console.log('IPC返回结果:', result)
          
          if (result && result.success) {
            console.log('登录成功，准备跳转')
            // 保存登录信息到本地存储
            if (result.token) {
              localStorage.setItem('token', result.token)
            }
            if (result.userInfo) {
              localStorage.setItem('userInfo', JSON.stringify(result.userInfo))
              // 检查是否需要强制修改密码
              if (result.userInfo.needForceChangePassword) {
                console.log('首次登录，需要强制修改密码')
                this.currentUserInfo = result.userInfo
                this.forceChangePasswordDialogVisible = true
                return // 不跳转，等待修改密码
              }
            }
            this.$message.success('登录成功')
            // 跳转到试卷选择页面
            this.$router.push('/paper-select')
          } else {
            console.error('登录失败:', result)
            this.$message.error(result?.message || '登录失败')
            this.refreshCaptcha()
          }
        } catch (error) {
          console.error('登录异常:', error)
          this.$message.error('登录失败：' + error.message)
          this.refreshCaptcha()
        } finally {
          this.loading = false
        }
      })
    },
    
    async handleOfflineLogin() {
      this.$refs.offlineForm.validate(async (valid) => {
        if (!valid) return
        
        this.loading = true
        try {
          const result = await ipcRenderer.invoke('login:offline', {
            username: this.offlineForm.username,
            password: this.offlineForm.password,
            offlineCredential: null
          })
          
          if (result.success) {
            this.$message.success('离线登录成功')
            // 保存登录信息
            localStorage.setItem('userInfo', JSON.stringify(result.userInfo))
            // 跳转到试卷选择页面
            this.$router.push('/paper-select')
          } else {
            this.$message.error(result.message || '离线登录失败')
          }
        } catch (error) {
          this.$message.error('离线登录失败：' + error.message)
        } finally {
          this.loading = false
        }
      })
    },
    
    // 显示忘记密码对话框
    showForgotPasswordDialog() {
      this.forgotPasswordDialogVisible = true
    },
    
    // 处理忘记密码
    async handleForgotPassword() {
      this.$refs.forgotPasswordForm.validate(async (valid) => {
        if (!valid) return
        
        this.forgotPasswordLoading = true
        try {
          const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080'
          const response = await axios.post(`${API_BASE_URL}/student/archive/forgotPassword`, {
            studentAccount: this.forgotPasswordForm.studentAccount,
            phoneNumber: this.forgotPasswordForm.phoneNumber,
            newPassword: this.forgotPasswordForm.newPassword
          })
          
          if (response.data && response.data.code === 200) {
            this.$message.success('密码重置成功，请使用新密码登录')
            this.forgotPasswordDialogVisible = false
            // 重置表单
            this.$refs.forgotPasswordForm.resetFields()
          } else {
            this.$message.error(response.data?.msg || '密码重置失败')
          }
        } catch (error) {
          console.error('忘记密码失败:', error)
          this.$message.error(error.response?.data?.msg || error.message || '密码重置失败')
        } finally {
          this.forgotPasswordLoading = false
        }
      })
    },
    
    // 处理强制修改密码
    async handleForceChangePassword() {
      this.$refs.changePasswordForm.validate(async (valid) => {
        if (!valid) return
        
        this.changePasswordLoading = true
        try {
          const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080'
          const token = localStorage.getItem('token')
          
          if (!token) {
            this.$message.error('登录已过期，请重新登录')
            this.forceChangePasswordDialogVisible = false
            return
          }
          
          // 调用修改密码接口
          const response = await axios.post(`${API_BASE_URL}/student/archive/changePwd`, {
            archiveId: this.currentUserInfo.archiveId,
            oldPassword: this.changePasswordForm.oldPassword,
            newPassword: this.changePasswordForm.newPassword
          }, {
            headers: {
              'Authorization': `Bearer ${token}`
            }
          })
          
          if (response.data && response.data.code === 200) {
            this.$message.success('密码修改成功')
            this.forceChangePasswordDialogVisible = false
            // 重置表单
            this.$refs.changePasswordForm.resetFields()
            // 重新获取用户信息（更新needForceChangePassword标识）
            try {
              const updatedUserInfo = await ipcRenderer.invoke('login:getStudentInfo', token)
              if (updatedUserInfo) {
                localStorage.setItem('userInfo', JSON.stringify(updatedUserInfo))
              }
            } catch (error) {
              console.error('重新获取用户信息失败:', error)
              // 即使失败也继续跳转
            }
            // 跳转到试卷选择页面
            this.$message.success('登录成功')
            this.$router.push('/paper-select')
          } else {
            this.$message.error(response.data?.msg || '密码修改失败')
          }
        } catch (error) {
          console.error('修改密码失败:', error)
          this.$message.error(error.response?.data?.msg || error.message || '密码修改失败')
        } finally {
          this.changePasswordLoading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 450px;
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo-container {
  display: flex;
  justify-content: center;
  margin-bottom: 15px;
}

.logo-img {
  width: 140px;
  height: 100px;
  object-fit: contain;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.15));
  border-radius: 4px;
  padding: 4px;
}

.login-title {
  margin: 0;
  color: #333;
  font-size: 24px;
  font-weight: 600;
}

.captcha-container {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.captcha-input {
  flex: 1;
  min-width: 0;
}

.captcha-image {
  width: 120px;
  height: 40px;
  flex-shrink: 0;
  cursor: pointer;
  border-radius: 4px;
  object-fit: contain;
  display: block;
}

/* 过渡动画 */
.fade-scale-enter-active,
.fade-scale-leave-active {
  transition: opacity 0.4s ease, transform 0.4s ease;
}

.fade-scale-enter,
.fade-scale-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>

