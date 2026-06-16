<template>
  <div class="property-master-container">
    <div class="header">
      <div class="back-button" @click="goBack">返回</div>
      <h1 class="title">AI房产分析助手</h1>
      <div class="chat-id">会话ID: {{ chatId }}</div>
    </div>
    
    <div class="content-wrapper">
      <div class="chat-area">
        <ChatRoom 
          :messages="messages" 
          :connection-status="connectionStatus"
          ai-type="property"
          @send-message="sendMessage"
        />
      </div>
    </div>
    
    <div class="footer-container">
      <AppFooter />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import ChatRoom from '../components/ChatRoom.vue'
import AppFooter from '../components/AppFooter.vue'
import { chatWithPropertyApp } from '../api'

// 设置页面标题和元数据
useHead({
  title: 'AI房产分析助手 - xiapi AI超级智能体应用平台',
  meta: [
    {
      name: 'description',
      content: 'AI房产分析助手是xiapi AI超级智能体应用平台的专业房产顾问，帮你分析房产走势、评估小区优劣、推荐合适房源'
    }
  ]
})

const router = useRouter()
const messages = ref([
  {
    role: 'assistant',
    content: '你好！我是AI房产分析助手，擅长分析房产走势、评估小区优劣。无论你是买房、卖房还是租房，都可以向我咨询！',
    time: Date.now()
  }
])
const connectionStatus = ref('connected')
const chatId = ref(generateChatId())
let eventSource = null

function generateChatId() {
  return 'chat_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

function goBack() {
  if (eventSource) {
    eventSource.close()
  }
  router.push('/')
}

async function sendMessage(message) {
  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: message,
    time: Date.now()
  })
  
  // 添加助手消息占位
  const assistantMessage = {
    role: 'assistant',
    content: '',
    time: Date.now()
  }
  messages.value.push(assistantMessage)
  
  // 使用SSE连接
  const params = {
    message: message,
    chatId: chatId.value
  }
  
  eventSource = chatWithPropertyApp(params,
    // 收到消息
    (data) => {
      if (data === '[DONE]') {
        connectionStatus.value = 'connected'
        return
      }
      assistantMessage.content += data
    },
    // 错误处理
    (error) => {
      console.error('SSE error:', error)
      connectionStatus.value = 'disconnected'
    }
  )
}

onBeforeUnmount(() => {
  if (eventSource) {
    eventSource.close()
  }
})

// 重置会话
onMounted(() => {
  // 可以在组件挂载时做一些初始化工作
})
</script>

<style scoped>
.property-master-container {
  max-width: 900px;
  margin: 0 auto;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  position: relative;
}

.back-button {
  cursor: pointer;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 6px;
  font-size: 14px;
  transition: background 0.2s;
}

.back-button:hover {
  background: rgba(255, 255, 255, 0.3);
}

.title {
  flex: 1;
  text-align: center;
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.chat-id {
  position: absolute;
  right: 20px;
  font-size: 11px;
  opacity: 0.7;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-area {
  flex: 1;
  overflow: hidden;
}

.footer-container {
  margin-top: auto;
}
</style>
