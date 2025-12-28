<template>
  <div class="first-page">
    <div class="content">
      <h1 class="title">AI 脑筋急转弯</h1>
      <a-button type="primary" size="large" @click="startGame" class="start-button">
        开始游戏
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { Button as AButton } from 'ant-design-vue';

const router = useRouter();

const startGame = () => {
  const roomId = Math.floor(Math.random() * 1000000);
  router.push(`/chat/${roomId}`);
};
</script>

<style scoped>
.first-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.content {
  text-align: center;
}

.title {
  font-size: 48px;
  font-weight: 700;
  color: white;
  margin-bottom: 80px;
  letter-spacing: 2px;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
}

.start-button {
  height: 60px;
  padding: 0 60px;
  font-size: 24px;
  border-radius: 30px;
  border: none;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
}

.start-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.3);
}
</style>
