<template>
  <div class="history-list">
    <div class="history-header">
      <h3>历史对话</h3>
      <a-button type="text" size="small" @click="refreshList" :loading="loading">
        刷新
      </a-button>
    </div>

    <a-spin :spinning="loading">
      <div class="room-list">
        <div
          v-for="room in rooms"
          :key="room.id || room.roomId"
          class="room-item"
          @click="selectRoom(room)"
        >
          <div class="room-info">
            <div class="room-title">对话 {{ room.id || room.roomId }}</div>
            <div class="room-meta">房间 #{{ room.id || room.roomId }}</div>
          </div>
        </div>

        <div v-if="rooms.length === 0 && !loading" class="empty-state">
          暂无历史对话
        </div>
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Button as AButton, Spin as ASpin } from 'ant-design-vue';
import { chatApi, ChatRoom } from '../api/chat';

const emit = defineEmits<{
  selectRoom: [room: ChatRoom]
}>();

const rooms = ref<ChatRoom[]>([]);
const loading = ref(false);

const LOCAL_KEY = 'ainaojin_rooms_v1';

const getLocalRooms = (): ChatRoom[] => {
  try {
    const raw = localStorage.getItem(LOCAL_KEY);
    if (!raw) return [];
    return JSON.parse(raw) as ChatRoom[];
  } catch (e) {
    return [];
  }
};

const setLocalRooms = (list: ChatRoom[]) => {
  try {
    localStorage.setItem(LOCAL_KEY, JSON.stringify(list));
  } catch (e) {
    // noop
  }
};

const mergeRooms = (backendList: any[]): ChatRoom[] => {
  const local = getLocalRooms();
  const map = new Map<string, any>();
  // add backend rooms
  (Array.isArray(backendList) ? backendList : (backendList ? [backendList] : [])).forEach((r: any) => {
    const id = String(r.id ?? r.roomId ?? '');
    map.set(id, { ...r, id: Number(r.id ?? r.roomId ?? 0) });
  });
  // merge local rooms (preserve messages)
  local.forEach((r: any) => {
    const id = String(r.id ?? r.roomId ?? '');
    const exist = map.get(id) || { id: Number(r.id ?? r.roomId ?? 0) };
    map.set(id, { ...exist, ...r, messages: r.messages ?? exist.messages ?? [] });
  });
  const merged = Array.from(map.values()) as ChatRoom[];
  return merged.map((r: any) => ({ ...r, _idNum: Number(r.id ?? r.roomId ?? 0) }))
    .sort((a: any, b: any) => b._idNum - a._idNum);
};

const loadRooms = async () => {
  try {
    loading.value = true;
    const data = await chatApi.getChatRoomList();
    const merged = mergeRooms(data);
    rooms.value = merged;
    // persist merged list locally as well
    setLocalRooms(merged.map((r: any) => ({ id: r.id ?? r.roomId, messages: r.messages ?? [] })) as any);
  } catch (error) {
    console.error('Failed to load rooms:', error);
    // fallback to local only
    rooms.value = getLocalRooms();
  } finally {
    loading.value = false;
  }
};

const refreshList = () => {
  loadRooms();
};

const selectRoom = (room: ChatRoom) => {
  emit('selectRoom', room);
};

onMounted(() => {
  loadRooms();
});

// expose loadRooms so parent can refresh the list after creating a room
defineExpose({ loadRooms });
</script>

<style scoped>
.history-list {
  width: 250px;
  background: white;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
}

.history-header {
  padding: 20px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fafafa;
}

.history-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.room-list {
  flex: 1;
  overflow-y: auto;
}

.room-item {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.2s ease;
}

.room-item:hover {
  background: #f5f5f5;
}

.room-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.room-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.room-meta {
  font-size: 12px;
  color: #999;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
  color: #999;
  font-size: 14px;
}
</style>
