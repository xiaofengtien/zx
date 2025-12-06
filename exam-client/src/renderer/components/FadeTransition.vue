<template>
  <transition
    :name="transitionName"
    :mode="mode"
    @before-enter="beforeEnter"
    @enter="enter"
    @after-enter="afterEnter"
  >
    <slot></slot>
  </transition>
</template>

<script>
export default {
  name: 'FadeTransition',
  props: {
    // 过渡类型: fade, fade-up, fade-down, fade-scale
    type: {
      type: String,
      default: 'fade'
    },
    // 过渡模式
    mode: {
      type: String,
      default: 'out-in'
    },
    // 过渡时长(ms)
    duration: {
      type: Number,
      default: 300
    }
  },
  computed: {
    transitionName() {
      return this.type
    }
  },
  methods: {
    beforeEnter(el) {
      el.style.transitionDuration = `${this.duration}ms`
    },
    enter(el, done) {
      // 强制重绘
      el.offsetHeight
      done()
    },
    afterEnter(el) {
      el.style.transitionDuration = ''
    }
  }
}
</script>

<style>
/* 基础淡入 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter,
.fade-leave-to {
  opacity: 0;
}

/* 向上淡入 */
.fade-up-enter-active,
.fade-up-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-up-enter,
.fade-up-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

/* 向下淡入 */
.fade-down-enter-active,
.fade-down-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-down-enter,
.fade-down-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

/* 缩放淡入 */
.fade-scale-enter-active,
.fade-scale-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-scale-enter,
.fade-scale-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>
