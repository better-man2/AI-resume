import axios from 'axios'

// 创建 axios 实例
const instance = axios.create({
  baseURL: 'http://localhost:9090/', // 设置基础URL
  // 调用大模型的接口本身较慢（生成简历实测 30~60s，带思维链的模型更久），
  // 默认超时先给到 120s，生成/润色这类接口再单独放宽
  timeout: 120000,
  headers: {
    // 设置公共请求头y
    
    'Content-Type': 'application/json'
    // 如果需要设置其他请求头，可以在这里添加
  }
})

export default instance
