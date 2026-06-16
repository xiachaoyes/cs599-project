<template>
  <footer class="app-footer">
    <div class="footer-bottom">
      <p>© {{ currentYear }} xiapi AI超级智能体应用平台</p>
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'

const currentYear = computed(() => new Date().getFullYear())
</script>

<style scoped>
.app-footer {
  background-color: transparent;
  padding: 16px 0;
  color: #999;
  width: 100%;
  margin-top: auto;
  text-align: center;
}

.footer-bottom p {
  font-size: 12px;
  margin: 0;
}
</style>
