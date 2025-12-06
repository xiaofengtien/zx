<template>
  <div class="exam-result-skeleton">
    <!-- 统计卡片骨架屏 -->
    <div class="statistics-skeleton">
      <div class="stat-header-skeleton skeleton-animated"></div>
      <div class="stat-content-skeleton">
        <div v-for="i in 5" :key="i" class="stat-item-skeleton">
          <div class="stat-label-skeleton skeleton-animated"></div>
          <div class="stat-value-skeleton skeleton-animated"></div>
        </div>
      </div>
    </div>
    
    <!-- 详情卡片骨架屏 -->
    <div class="detail-skeleton">
      <div class="detail-header-skeleton">
        <div class="detail-title-skeleton skeleton-animated"></div>
        <div class="detail-count-skeleton skeleton-animated"></div>
      </div>
      <div class="detail-content-skeleton">
        <!-- 卷别分组骨架 -->
        <div v-for="i in 2" :key="i" class="volume-skeleton">
          <div class="volume-header-skeleton skeleton-animated"></div>
          <div class="section-skeleton">
            <div class="section-header-skeleton skeleton-animated"></div>
            <div class="table-skeleton">
              <div v-for="j in 4" :key="j" class="table-row-skeleton">
                <div class="table-cell-skeleton skeleton-animated" style="width: 60px;"></div>
                <div class="table-cell-skeleton skeleton-animated" style="flex: 1;"></div>
                <div class="table-cell-skeleton skeleton-animated" style="width: 80px;"></div>
                <div class="table-cell-skeleton skeleton-animated" style="width: 100px;"></div>
                <div class="table-cell-skeleton skeleton-animated" style="width: 100px;"></div>
                <div class="table-cell-skeleton skeleton-animated" style="width: 60px;"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 按钮骨架屏 -->
    <div class="buttons-skeleton">
      <div class="button-skeleton skeleton-animated"></div>
      <div class="button-skeleton skeleton-animated"></div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ExamResultSkeleton'
}
</script>

<style scoped>
.exam-result-skeleton {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 统计卡片骨架 */
.statistics-skeleton {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-header-skeleton {
  width: 100px;
  height: 24px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.stat-content-skeleton {
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
}

.stat-item-skeleton {
  text-align: center;
  min-width: 100px;
  padding: 10px;
}

.stat-label-skeleton {
  width: 60px;
  height: 14px;
  border-radius: 4px;
  margin: 0 auto 10px;
}

.stat-value-skeleton {
  width: 80px;
  height: 36px;
  border-radius: 4px;
  margin: 0 auto;
}

/* 详情卡片骨架 */
.detail-skeleton {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.detail-header-skeleton {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.detail-title-skeleton {
  width: 100px;
  height: 24px;
  border-radius: 4px;
}

.detail-count-skeleton {
  width: 80px;
  height: 16px;
  border-radius: 4px;
}

/* 卷别骨架 */
.volume-skeleton {
  margin-bottom: 24px;
  border: 1px solid #EBEEF5;
  border-radius: 8px;
  overflow: hidden;
}

.volume-header-skeleton {
  height: 48px;
  border-radius: 0;
}

.section-skeleton {
  padding: 0;
}

.section-header-skeleton {
  height: 40px;
  border-radius: 0;
}

/* 表格骨架 */
.table-skeleton {
  padding: 10px;
}

.table-row-skeleton {
  display: flex;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.table-row-skeleton:last-child {
  border-bottom: none;
}

.table-cell-skeleton {
  height: 20px;
  border-radius: 4px;
}

/* 按钮骨架 */
.buttons-skeleton {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 30px;
}

.button-skeleton {
  width: 120px;
  height: 40px;
  border-radius: 4px;
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
