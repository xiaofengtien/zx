<template>
  <div class="forgot-password">
    <el-form ref="forgotForm" :model="forgotForm" :rules="forgotRules" class="forgot-form">
      <h3 class="title">忘记密码</h3>
      <el-form-item prop="studentAccount">
        <el-input
          v-model="forgotForm.studentAccount"
          type="text"
          auto-complete="off"
          placeholder="请输入学员账号"
        >
          <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="phoneNumber">
        <el-input
          v-model="forgotForm.phoneNumber"
          type="text"
          auto-complete="off"
          placeholder="请输入手机号码"
        >
          <svg-icon slot="prefix" icon-class="phone" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="newPassword">
        <el-input
          v-model="forgotForm.newPassword"
          type="password"
          auto-complete="off"
          placeholder="请输入新密码（6-20位）"
          show-password
        >
          <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="confirmPassword">
        <el-input
          v-model="forgotForm.confirmPassword"
          type="password"
          auto-complete="off"
          placeholder="请再次输入新密码"
          show-password
          @keyup.enter.native="handleSubmit"
        >
          <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item style="width:100%;">
        <el-button
          :loading="loading"
          size="medium"
          type="primary"
          style="width:100%;"
          @click.native.prevent="handleSubmit"
        >
          <span v-if="!loading">重置密码</span>
          <span v-else>提交中...</span>
        </el-button>
        <div style="float: right; margin-top: 10px;">
          <el-link type="primary" @click="goBack">返回登录</el-link>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import { forgotPassword } from "@/api/student/archive"

export default {
  name: "ForgotPassword",
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.forgotForm.newPassword) {
        callback(new Error("两次输入的密码不一致"))
      } else {
        callback()
      }
    }
    return {
      title: process.env.VUE_APP_TITLE,
      forgotForm: {
        studentAccount: "",
        phoneNumber: "",
        newPassword: "",
        confirmPassword: ""
      },
      forgotRules: {
        studentAccount: [
          { required: true, trigger: "blur", message: "请输入学员账号" }
        ],
        phoneNumber: [
          { required: true, trigger: "blur", message: "请输入手机号码" },
          {
            pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/,
            message: "请输入正确的手机号码",
            trigger: "blur"
          }
        ],
        newPassword: [
          { required: true, trigger: "blur", message: "请输入新密码" },
          { min: 6, max: 20, message: "密码长度必须介于 6 和 20 之间", trigger: "blur" }
        ],
        confirmPassword: [
          { required: true, trigger: "blur", message: "请再次输入新密码" },
          { validator: validateConfirmPassword, trigger: "blur" }
        ]
      },
      loading: false
    }
  },
  methods: {
    handleSubmit() {
      this.$refs.forgotForm.validate(valid => {
        if (valid) {
          this.loading = true
          const data = {
            studentAccount: this.forgotForm.studentAccount,
            phoneNumber: this.forgotForm.phoneNumber,
            newPassword: this.forgotForm.newPassword
          }
          forgotPassword(data).then(() => {
            this.$modal.msgSuccess("密码重置成功，请使用新密码登录")
            this.goBack()
          }).catch(() => {
            this.loading = false
          })
        }
      })
    },
    goBack() {
      this.$router.push({ path: "/login" })
    }
  }
}
</script>

<style rel="stylesheet/scss" lang="scss">
.forgot-password {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  background-image: url("../assets/images/login-background.jpg");
  background-size: cover;
}
.forgot-form {
  border-radius: 6px;
  background: #ffffff;
  width: 400px;
  padding: 25px 25px 5px 25px;
  .el-input {
    height: 38px;
    input {
      height: 38px;
    }
  }
  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 2px;
  }
}
.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: #707070;
}
.login-code {
  width: 33%;
  height: 38px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}
.login-code-img {
  height: 38px;
  padding-left: 5px;
}
</style>

