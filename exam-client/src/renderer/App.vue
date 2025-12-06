<template>
  <div id="app">
    <div v-if="loading" class="loading-container">
      <div class="loading-content">
        <i class="el-icon-loading"></i>
        <p>正在加载...</p>
      </div>
    </div>
    <router-view v-else/>
  </div>
</template>

<script>
export default {
  name: 'App',
  data() {
    return {
      loading: true
    }
  },
  mounted() {
    // 应用初始化，确保路由和组件都已加载
    this.$nextTick(() => {
      // 使用更短的延迟，因为启动画面已经提供了视觉反馈
      setTimeout(() => {
        this.loading = false
      }, 200) // 减少延迟，因为启动画面已经处理了初始加载
    })
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#app {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  width: 100%;
  height: 100vh;
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.loading-content {
  text-align: center;
  color: white;
}

.loading-content i {
  font-size: 48px;
  animation: rotating 2s linear infinite;
  display: block;
  margin-bottom: 20px;
}

.loading-content p {
  font-size: 16px;
  margin-top: 10px;
}

@keyframes rotating {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style>

