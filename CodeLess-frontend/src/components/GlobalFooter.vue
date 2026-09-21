<template>
  <a-layout-footer class="global-footer">
    <span>零代码应用生成平台 by </span>
    <a
      class="global-footer__link"
      href="https://github.com/Echo-love928?tab=repositories"
      target="_blank"
      rel="noopener noreferrer"
    >
      hjq
    </a>
  </a-layout-footer>
</template>

<style scoped>
.global-footer {
  padding: 20px 24px;
  border-top: 1px solid #e7edf6;
  background: #ffffff;
  color: #718096;
  font-size: 14px;
  text-align: center;
}

.global-footer__link {
  color: #1677ff;
  font-weight: 600;
  text-decoration: none;
}

.global-footer__link:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}

.global-footer__link:focus-visible {
  border-radius: 2px;
  outline: 2px solid #1677ff;
  outline-offset: 3px;
}
</style>
