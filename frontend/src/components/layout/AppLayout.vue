<template>
  <div class="app-layout">
    <AppHeader />
    <main class="main-content">
      <router-view />
    </main>
    <footer class="copyright-footer">
      <span>版权归长沙创赛赢教育科技有限公司所有</span>
      <span class="footer-contact">合作联系：微信号327172508（添加好友时，注明单位+姓名）&nbsp;&nbsp;使用问题：微信号1175560106（添加好友时，注明单位+姓名）</span>
    </footer>
  </div>
</template>

<script setup>
import AppHeader from './AppHeader.vue'
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}
.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px;
  flex: 1;
  width: 100%;
  box-sizing: border-box;
}
.copyright-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 16px;
  padding: 16px 20px;
  color: #909399;
  font-size: 13px;
  border-top: 1px solid #ebeef5;
  background: #fafafa;
}
.footer-contact {
  color: #a8abb2;
  font-size: 12px;
}
</style>
