<template>
  <div class="skeleton-wrapper" :class="{ 'skeleton-animated': animated }">
    <!-- 通用骨架屏插槽 -->
    <slot>
      <!-- 默认骨架屏内容 -->
      <div v-if="type === 'text'" class="skeleton-text" :style="textStyle"></div>
      <div v-else-if="type === 'avatar'" class="skeleton-avatar" :style="avatarStyle"></div>
      <div v-else-if="type === 'button'" class="skeleton-button" :style="buttonStyle"></div>
      <div v-else-if="type === 'card'" class="skeleton-card">
        <div class="skeleton-card-header">
          <div class="skeleton-avatar skeleton-avatar-sm"></div>
          <div class="skeleton-text-group">
            <div class="skeleton-text skeleton-text-title"></div>
            <div class="skeleton-text skeleton-text-subtitle"></div>
          </div>
        </div>
        <div class="skeleton-card-body">
          <div class="skeleton-text"></div>
          <div class="skeleton-text"></div>
          <div class="skeleton-text skeleton-text-short"></div>
        </div>
      </div>
      <div v-else-if="type === 'list'" class="skeleton-list">
        <div v-for="i in rows" :key="i" class="skeleton-list-item">
          <div class="skeleton-avatar skeleton-avatar-sm"></div>
          <div class="skeleton-text-group">
            <div class="skeleton-text"></div>
            <div class="skeleton-text skeleton-text-short"></div>
          </div>
        </div>
      </div>
    </slot>
  </div>
</template>

<script>
export default {
  name: 'Skeleton',
  props: {
    // 骨架屏类型: text, avatar, button, card, list
    type: {
      type: String,
      default: 'text'
    },
    // 是否显示动画
    animated: {
      type: Boolean,
      default: true
    },
    // 文本宽度
    width: {
      type: String,
      default: '100%'
    },
    // 文本高度
    height: {
      type: String,
      default: '16px'
    },
    // 列表行数
    rows: {
      type: Number,
      default: 3
    },
    // 头像大小
    avatarSize: {
      type: String,
      default: '40px'
    }
  },
  computed: {
    textStyle() {
      return {
        width: this.width,
        height: this.height
      }
    },
    avatarStyle() {
      return {
        width: this.avatarSize,
        height: this.avatarSize
      }
    },
    buttonStyle() {
      return {
        width: this.width,
        height: this.height
      }
    }
  }
}
</script>

<style scoped>
.skeleton-wrapper {
  width: 100%;
}

/* 基础骨架屏样式 */
.skeleton-text,
.skeleton-avatar,
.skeleton-button {
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  border-radius: 4px;
}

.skeleton-avatar {
  border-radius: 50%;
}

.skeleton-button {
  border-radius: 4px;
  height: 32px;
}

/* 动画效果 */
.skeleton-animated .skeleton-text,
.skeleton-animated .skeleton-avatar,
.skeleton-animated .skeleton-button {
  animation: skeleton-loading 1.5s infinite ease-in-out;
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* 文本变体 */
.skeleton-text {
  height: 16px;
  margin-bottom: 8px;
}

.skeleton-text-title {
  width: 40%;
  height: 20px;
}

.skeleton-text-subtitle {
  width: 60%;
  height: 14px;
}

.skeleton-text-short {
  width: 60%;
}

/* 头像变体 */
.skeleton-avatar-sm {
  width: 32px;
  height: 32px;
}

.skeleton-avatar-lg {
  width: 64px;
  height: 64px;
}

/* 卡片骨架屏 */
.skeleton-card {
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.skeleton-card-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.skeleton-card-header .skeleton-avatar {
  margin-right: 12px;
  flex-shrink: 0;
}

.skeleton-text-group {
  flex: 1;
}

.skeleton-card-body .skeleton-text:last-child {
  margin-bottom: 0;
}

/* 列表骨架屏 */
.skeleton-list-item {
  display: flex;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.skeleton-list-item:last-child {
  border-bottom: none;
}

.skeleton-list-item .skeleton-avatar {
  margin-right: 12px;
  flex-shrink: 0;
}
</style>
