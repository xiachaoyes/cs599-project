import axios from 'axios'

// 根据环境变量设置 API 基础 URL
const API_BASE_URL = process.env.NODE_ENV === 'production' 
 ? '/api' // 生产环境使用相对路径，适用于前后端部署在同一域名下
 : 'http://localhost:8123/api' // 开发环境指向本地后端服务

// 创建axios实例
const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000
})

// 封装SSE连接
export const connectSSE = (url, params, onMessage, onError) => {
  // 构建带参数的URL
  const queryString = Object.keys(params)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')
  
  const fullUrl = `${API_BASE_URL}${url}?${queryString}`
  
  // 创建EventSource
  const eventSource = new EventSource(fullUrl)
  
  eventSource.onmessage = event => {
    let data = event.data
    
    // 检查是否是特殊标记
    if (data === '[DONE]') {
      if (onMessage) onMessage('[DONE]')
    } else {
      // 处理普通消息
      if (onMessage) onMessage(data)
    }
  }
  
  eventSource.onerror = error => {
    if (onError) onError(error)
    eventSource.close()
  }
  
  // 返回eventSource实例，以便后续可以关闭连接
  return eventSource
}

// AI房产分析助手聊天
export const chatWithPropertyApp = (params, onMessage, onError) => {
  return connectSSE('/ai/property_app/chat/sse', { message: params.message, chatId: params.chatId }, onMessage, onError)
}

// AI超级智能体聊天（兼容两种调用方式）
export const chatWithManus = (messageOrParams, onMessage, onError) => {
  const params = typeof messageOrParams === 'string'
    ? { message: messageOrParams }
    : messageOrParams
  return connectSSE('/ai/manus/chat', { message: params.message }, onMessage, onError)
}

export default {
  chatWithPropertyApp,
  chatWithManus
} 