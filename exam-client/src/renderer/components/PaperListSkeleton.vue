<template>
  <div class="paper-list-skeleton">
    <div v-for="i in count" :key="i" class="paper-item-skeleton">
      <!-- 序号 -->
      <div class="paper-number-skeleton skeleton-animated"></div>
      
      <!-- 试卷信息 -->
      <div class="paper-info-skeleton">
        <div class="paper-name-skeleton skeleton-animated"></div>
        <div class="paper-details-skeleton">
          <div class="paper-time-skeleton skeleton-animated"></div>
          <div class="paper-status-skeleton skeleton-animated"></div>
        </div>
      </div>
      
      <!-- 按钮 -->
      <div class="paper-button-skeleton skeleton-animated"></div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PaperListSkeleton',
  props: {
    count: {
      type: Number,
      default: 3
    }
  }
}
</script>

<style scoped>
.paper-list-skeleton {
  padding: 0 20px;
}

.paper-item-skeleton {
  display: flex;
  align-items: center;
  padding: 20px;
  margin-bottom: 12px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.paper-number-skeleton {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  margin-right: 16px;
  flex-shrink: 0;
}

.paper-info-skeleton {
  flex: 1;
  min-width: 0;
}

.paper-name-skeleton {
  width: 200px;
  height: 20px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.paper-details-skeleton {
  display: flex;
  gap: 12px;
}

.paper-time-skeleton {
  width: 280px;
  height: 14px;
  border-radius: 4px;
}

.paper-status-skeleton {
  width: 80px;
  height: 22px;
  border-radius: 4px;
}

.paper-button-skeleton {
  width: 100px;
  height: 36px;
  border-radius: 4px;
  margin-left: 16px;
  flex-shrink: 0;
}

/* 骨架屏动画 */
.skeleton-animated {
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
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
</style>
