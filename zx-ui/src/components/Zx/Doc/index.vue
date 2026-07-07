<template>
  <div>
    <svg-icon icon-class="question" @click="goto" />
  </div>
</template>

<script>
export default {
  name: 'ZxDoc',
  data() {
    return {
      url: 'http://doc.zx.vip/zx-vue'
    }
  },
  methods: {
    goto() {
      window.open(this.url)
    }
  }
}
</script>