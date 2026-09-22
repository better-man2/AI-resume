<template>
  <div class="login-box">
    <el-form
        ref="ruleFormRef"
        :model="ruleForm"
        status-icon
        :rules="rules"
        label-width="80px"
        class="demo-ruleForm"
    >
      <form  class="inform-box">
        <img src="../assets/logo.png" style="width: 40px;position: relative; top: 13px;right: 6px">
        <span style="color: #f86a33;font-size: 20px;">Ai简历生成与职位推荐系统</span>
        <br><br>
        <el-form-item label="账号：" prop="email" style="width: 420px;">
          <el-input placeholder="请输入邮箱" v-model="ruleForm.email" autocomplete="off" />
        </el-form-item>
        <el-form-item label="密码：" prop="password" style="width: 420px;">
          <el-input placeholder="请输入密码" v-model="ruleForm.password" type="password" autocomplete="off" />
        </el-form-item>

        <el-form-item label="验证码:" prop="sidentifyMode" style="width: 500px">
          <template #default>
            <div class="code-wrapper">
              <el-input
                  placeholder="请输入验证码"
                  v-model="sidentifyMode"
                  clearable
                  class="code-input"
              ></el-input>
              <div class="code" @click="refreshCode">
                <SIdentify :identifyCode="identifyCode"></SIdentify>
              </div>
            </div>
          </template>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="login-btn" @click="submitForm()">登录</el-button>
          <el-button type="success" class="login-btn" @click="register()">注册</el-button>
        </el-form-item>
      </form>
    </el-form>
  </div>
</template>

<script setup>
import SIdentify from '@/utils/SidentifyView.vue'
import { ElMessage } from 'element-plus'
import { ref } from 'vue'
import axios from '@/utils/axios-config.js'
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

//获取路由器
let sidentifyMode = ref('') //输入框验证码
let identifyCode = ref('') //图形验证码
let identifyCodes = ref('1234567890abcdefjhijklmnopqrstuvwxyz') //验证码出现的数字和字母

//组件挂载
onMounted(() => {
  identifyCode.value = ''
  makeCode(identifyCodes.value, 4)
})

// 生成随机数
const randomNum = (min, max) => {
  max = max + 1
  return Math.floor(Math.random() * (max - min) + min)
}
// 随机生成验证码字符串
const makeCode = (o, l) => {
  for (let i = 0; i < l; i++) {
    identifyCode.value += o[randomNum(0, o.length)]
  }
}
// 更新验证码
const refreshCode = () => {
  identifyCode.value = ''
  makeCode(identifyCodes.value, 4)
}

const ruleForm = ref({
  email: '',
  password: ''
})

const submitForm = () => {
  //验证验证码不为空
  if (!sidentifyMode.value) {
    ElMessage({ type: 'error', message: '验证码不能为空！' })
    return
  }
  //验证验证码是否正确
  if (sidentifyMode.value != identifyCode.value) {
    ElMessage({ type: 'error', message: '验证码错误' })
    refreshCode()
    return
  } else {
    const requestData = {
      email: ruleForm.value.email,
      password: ruleForm.value.password
    }

    console.log('请求数据' + requestData.email + ' ' + requestData.password)

    axios
        .post('user/login', requestData)
        .then(function (response) {
          console.log('响应数据：', response.data)
          if (response.data !== null ) {
            ElMessage({ type: 'success', message: '登录成功' })
            localStorage.setItem('username', response.data.username)
            localStorage.setItem('userId', response.data.id)
            localStorage.setItem('email', response.data.email)
            console.log('localStorage.getItem(username):' + localStorage.getItem('username'))
            router.push('/wschatView')
          } else {
            ElMessage({ type: 'error', message: response.data.msg })
          }
        })
        .catch(function (error) {
          console.error('请求出错：', error)
        })
  }
}

const register = () => {
  router.push('/register')
}
</script>

<style lang="scss" scoped>
html, body {
  height: 100%;
  margin: 0;
  overflow: hidden; /* 隐藏滚动条 */
}
.login-box {
  width: 100%;
  height: 100%;
  //background: linear-gradient(to bottom, aliceblue, #dcefff, white);
  background: url('../../src/assets/login.png') no-repeat center center fixed; /* 使用fixed定位确保图像随窗口滚动固定 */
  -webkit-background-size: cover; /* Chrome, Safari */
  -moz-background-size: cover;    /* Firefox */
  -o-background-size: cover;      /* Opera */
  background-size: cover;         /* W3C 标准 */
  text-align: center;
  padding: 1px;

  .demo-ruleForm {
    width: 450px;
    margin: 200px auto;
    background: #ffffff;
    padding: 40px;
    border-radius: 5px;
  }

  .code-wrapper {
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .code-input {
    flex: 1;
    margin-right: 20px;
  }
  .code {
    text-align: center;
    margin-bottom: 10px;
  }
  .login-btn {
    width: 40%;
  }
  h2 {
    margin-bottom: 20px;
  }
}
</style>
