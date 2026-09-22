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
      <img src="../assets/logo.png" style="width: 40px;position: relative; top: 13px;right: 6px">
      <span style="color: #f86a33;font-size: 20px;">Ai简历生成与职位推荐系统</span>
      <br><br>
      <el-form-item label="用户名：" prop="username" style="width: 420px;">
        <el-input placeholder="请输入用户名" v-model="ruleForm.username" autocomplete="off" />
      </el-form-item>
      <el-form-item label="账号：" prop="email" style="width: 420px;">
        <el-input placeholder="请输入邮箱" v-model="ruleForm.email" autocomplete="off" />
      </el-form-item>
      <el-form-item label="电话号：" prop="phone" style="width: 420px;">
        <el-input placeholder="请输入电话号" v-model="ruleForm.phone" autocomplete="off" />
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
        <el-button type="success" class="login-btn" @click="register()">注册</el-button>
        <el-button type="primary" class="login-btn" @click="login()">登录</el-button>
      </el-form-item>
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

let sidentifyMode = ref('') //输入框验证码
let identifyCode = ref('') //图形验证码
let identifyCodes = ref('1234567890abcdefjhijklinopqrsduvwxyz') //验证码出现的数字和字母

onMounted(() => {
  identifyCode.value = ''
  makeCode(identifyCodes.value, 4)
})

const randomNum = (min, max) => {
  max = max + 1
  return Math.floor(Math.random() * (max - min) + min)
}

const makeCode = (o, l) => {
  for (let i = 0; i < l; i++) {
    identifyCode.value += o[randomNum(0, o.length)]
  }
}

const refreshCode = () => {
  identifyCode.value = ''
  makeCode(identifyCodes.value, 4)
}

const ruleForm = ref({
  username: '',
  email: '',
  password: '',
  phone: ''
})

const rules = ref({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 15, message: '用户名长度在 2 到 15 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
  ],
  phone: [
    { required: true, message: '请输入电话号', trigger: 'blur' },
    { min: 11, max: 11, message: '请输入正确的11位电话号码', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ]
})

const ruleFormRef = ref(null)

const register = () => {
  ruleFormRef.value.validate((valid) => {
    if (!sidentifyMode.value) {
      ElMessage({ type: 'error', message: '验证码不能为空！' })
      return
    }
    if (sidentifyMode.value !== identifyCode.value) {
      ElMessage({ type: 'error', message: '验证码错误' })
      refreshCode()
      return
    }
    if (valid) {
      const requestData = {
        username: ruleForm.value.username,
        email: ruleForm.value.email,
        password: ruleForm.value.password,
        phone: ruleForm.value.phone
      }
      axios.post('user/add', requestData)
          .then((response) => {
            if (response.data === "新增用户成功！") {
              ElMessage({ type: 'success', message: '注册成功' })
              setTimeout(() => {
                router.push('/login')
              }, 1000) // 等待1秒后跳转到登录界面
            } else {
              ElMessage({ type: 'error', message: '注册失败' })
            }
          })
          .catch((error) => {
            ElMessage({ type: 'error', message: '请求出错：' + error })
          })
    } else {
      ElMessage({ type: 'error', message: '请检查输入内容' })
      return false
    }
  })
}

const login = () => {
  router.push('/login')
}
</script>

<style lang="scss" scoped>
.login-box {
  width: 100%;
  height: 100%;
 // background: linear-gradient(to bottom, aliceblue, #dcefff, white);
  background: url('../../src/assets/login.png') ;
  -webkit-background-size: cover;
  -moz-background-size: cover;
  -o-background-size: cover;
  background-size: cover;
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
