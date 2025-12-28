<template>
  <div class="chat-page">
    <HistoryList ref="historyListRef" @select-room="handleSelectRoom" />

    <div class="chat-container">
      <div class="chat-header">
        <h2>AI 脑筋急转弯</h2>
        <div class="room-id">房间号: {{ roomId }}</div>
      </div>

      <div class="chat-messages" ref="messagesContainer">
        <div
          v-for="(message, index) in messages"
          :key="index"
          :class="['message', message.type]"
        >
          <div class="avatar">
            <div class="avatar-circle" :class="message.type">
              {{ message.type === 'ai' ? 'AI' : '我' }}
            </div>
          </div>
          <div class="message-bubble">
            {{ message.content }}
          </div>
        </div>
      </div>

      <div class="chat-controls">
        <div class="control-buttons">
          <a-button
            type="primary"
            @click="handleStart"
            :disabled="gameStarted && !gameEnded"
            :loading="loading"
          >
            开始
          </a-button>
          <a-button
            danger
            @click="handleEnd"
            :disabled="gameEnded"
            :loading="loading"
          >
            结束
          </a-button>
        </div>

        <div class="input-area">
          <a-input
            v-model:value="inputMessage"
            placeholder="请输入内容"
            @pressEnter="handleSend"
            :disabled="!gameStarted || gameEnded || loading"
            size="large"
          />
          <a-button
            type="primary"
            @click="handleSend"
            :disabled="!inputMessage.trim() || !gameStarted || gameEnded || loading"
            :loading="loading"
            size="large"
          >
            发送
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { Button as AButton, Input as AInput, message as AMessage } from 'ant-design-vue';
import { chatApi } from '../api/chat';
import HistoryList from '../components/HistoryList.vue';

const route = useRoute();
const roomId = ref<number>(Number(route.params.roomId));
const messages = ref<Array<{ type: 'ai' | 'user', content: string }>>([]);
const inputMessage = ref('');
const gameStarted = ref(false);
const gameEnded = ref(false);
const loading = ref(false);
const messagesContainer = ref<HTMLElement | null>(null);
const historyListRef = ref<any>(null);

const LOCAL_KEY = 'ainaojin_rooms_v1';

const saveRoomMessages = (roomIdNum: number, msgs: Array<{ type: string; content: string }>) => {
  try {
    const raw = localStorage.getItem(LOCAL_KEY);
    const list = raw ? (JSON.parse(raw) as any[]) : [];
    const id = String(roomIdNum);
    const idx = list.findIndex((r) => String(r.id ?? r.roomId) === id);
    const entry = { id: roomIdNum, messages: msgs };
    if (idx >= 0) {
      list[idx] = { ...list[idx], ...entry };
    } else {
      list.push(entry);
    }
    localStorage.setItem(LOCAL_KEY, JSON.stringify(list));
  } catch (e) {
    // noop
  }
};

const loadLocalRoom = (roomIdNum: number) => {
  try {
    const raw = localStorage.getItem(LOCAL_KEY);
    if (!raw) return null;
    const list = JSON.parse(raw) as any[];
    const id = String(roomIdNum);
    return list.find((r) => String(r.id ?? r.roomId) === id) ?? null;
  } catch (e) {
    return null;
  }
};

const scrollToBottom = async () => {
  await nextTick();
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
  }
};

const handleStart = async () => {
  try {
    loading.value = true;
    // if previously ended, allow restart: clear messages and reset ended flag
    if (gameEnded.value) {
      messages.value = [];
      gameEnded.value = false;
    }
    const response = await chatApi.sendMessage(roomId.value, '开始');
    messages.value.push({
      type: 'ai',
      content: response
    });
    // persist to local
    saveRoomMessages(roomId.value, messages.value as any);
    gameStarted.value = true;
    scrollToBottom();
    // 刷新历史列表以确保新房间出现在左侧
    await refreshHistory();
  } catch (error) {
    AMessage.error('发送消息失败');
    console.error(error);
  } finally {
    loading.value = false;
  }
};

// refresh history list after actions; awaitable and with short delay
const sleep = (ms: number) => new Promise((res) => setTimeout(res, ms));
const refreshHistory = async () => {
  try {
    const loader = (historyListRef.value as any)?.loadRooms;
    const maxAttempts = 6;
    for (let i = 0; i < maxAttempts; i++) {
      // ask backend for room list and check if current room exists
      try {
        const data = await chatApi.getChatRoomList();
        const list = Array.isArray(data) ? data : (data ? [data] : []);
        const found = list.some((r: any) => String(r.id ?? r.roomId) === String(roomId.value));
        if (found) {
          if (loader) await loader();
          return true;
        }
      } catch (e) {
        // ignore and retry
      }
      // wait before next attempt
      await sleep(300);
    }

    // final attempt to refresh UI even if room not found yet
    if (loader) await loader();
    return false;
  } catch (e) {
    console.error('refreshHistory failed', e);
    return false;
  }
};

const handleEnd = async () => {
  try {
    loading.value = true;
    const response = await chatApi.sendMessage(roomId.value, '结束');
    messages.value.push({
      type: 'user',
      content: '结束'
    });
    messages.value.push({
      type: 'ai',
      content: response
    });
    if (response.includes('【游戏已结束】')) {
      gameEnded.value = true;
    } else {
      gameEnded.value = true; // treat explicit end as ended by default
    }
    // mark stopped
    gameStarted.value = false;
    // persist messages so history can show conversation
    saveRoomMessages(roomId.value, messages.value as any);
    // 刷新历史列表
    await refreshHistory();
    scrollToBottom();
  } catch (error) {
    AMessage.error('发送消息失败');
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const handleSend = async () => {
  if (!inputMessage.value.trim()) return;

  // 特殊关键字处理：开始 / 结束
  const trimmed = inputMessage.value.trim();

  if (!gameStarted.value) {
    if (trimmed === '开始') {
      inputMessage.value = '';
      await handleStart();
      // 刷新历史并返回（handleStart 已刷新一次，但再确保一次）
      await refreshHistory();
      return;
    }
    AMessage.warning('请先点击开始游戏或输入“开始”');
    return;
  }

  if (gameEnded.value) return;

  if (trimmed === '结束') {
    inputMessage.value = '';
    await handleEnd();
    return;
  }

  const userInput = inputMessage.value;
  inputMessage.value = '';

  try {
    loading.value = true;
    messages.value.push({ type: 'user', content: userInput });
    scrollToBottom();

    const response = await chatApi.sendMessage(roomId.value, userInput);
    messages.value.push({ type: 'ai', content: response });

    if (response.includes('【游戏已结束】')) {
      gameEnded.value = true;
    }

    // persist messages and refresh history
    saveRoomMessages(roomId.value, messages.value as any);
    await refreshHistory();

    scrollToBottom();
  } catch (error) {
    AMessage.error('发送消息失败');
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const normalizeRoomMessages = (room: any) => {
  if (!room) return [];
  const list = room.messages ?? room.history ?? room.conversation ?? [];
  if (!Array.isArray(list)) return [];
  return list.map((m: any) => {
    const content = m.content ?? m.text ?? m.message ?? (typeof m === 'string' ? m : '');
    // 优先使用显式保存的 type 字段
    if (m && (m.type === 'user' || m.type === 'ai')) {
      return { type: m.type as 'ai' | 'user', content };
    }
    const senderRaw = (m.sender ?? m.role ?? m.from ?? '').toString().toLowerCase();
    let type: 'ai' | 'user' = 'ai';
    if (senderRaw.includes('user') || senderRaw.includes('me') || senderRaw.includes('player') || senderRaw.includes('human')) {
      type = 'user';
    } else if (senderRaw.includes('ai') || senderRaw.includes('assistant') || senderRaw.includes('bot')) {
      type = 'ai';
    } else if (m.isUser) {
      type = 'user';
    } else if (m.isAi) {
      type = 'ai';
    }
    return { type, content };
  });
};

const handleSelectRoom = (payload: any) => {
  const selectedRoom = payload && typeof payload === 'object' ? payload : { id: payload };
  const id = Number(selectedRoom.id ?? selectedRoom.roomId ?? selectedRoom);
  roomId.value = id;
  // try to load messages from the emitted room object
  const loaded = normalizeRoomMessages(selectedRoom);
  messages.value = loaded.length ? loaded : [];
  gameStarted.value = false;
  gameEnded.value = false;
  inputMessage.value = '';
  // scroll after DOM update
  scrollToBottom();
};

onMounted(() => {
  scrollToBottom();
  // 页面进入时自动发送开始请求（如果路由中有 roomId）
  // 不再在挂载时自动开始，避免路由/切换房间时误触发
});
</script>

<style scoped>
.chat-page {
  display: flex;
  height: 100vh;
  background: #f5f5f5;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
}

.chat-header {
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.chat-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  color: white;
}

.room-id {
  font-size: 14px;
  opacity: 0.9;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fafafa;
}

.message {
  display: flex;
  margin-bottom: 20px;
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message.ai {
  justify-content: flex-start;
}

.message.user {
  justify-content: flex-end;
}

.avatar {
  margin: 0 12px;
}

.avatar-circle {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  color: white;
}

.avatar-circle.ai {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.avatar-circle.user {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.message-bubble {
  max-width: 60%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.5;
  word-wrap: break-word;
}

.message.ai .message-bubble {
  background: white;
  border: 1px solid #e8e8e8;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.message.user .message-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.chat-controls {
  padding: 20px;
  background: white;
  border-top: 1px solid #e8e8e8;
}

.control-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.control-buttons button {
  flex: 1;
  height: 40px;
  font-size: 16px;
  border-radius: 8px;
}

.input-area {
  display: flex;
  gap: 12px;
}

.input-area :deep(.ant-input) {
  flex: 1;
  border-radius: 8px;
}

.input-area button {
  border-radius: 8px;
  padding: 0 24px;
}
</style>
