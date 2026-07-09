<template>
  <div>
    <div style="margin-bottom:20px"><h2 style="font-size:20px;font-weight:600;color:#1a202c;margin-bottom:4px">告警管理</h2><p style="font-size:13px;color:#94a3b8">异常预警与处理</p></div>
    <el-row :gutter="16" style="margin-bottom:20px">
      <el-col :span="8" v-for="s in alertStats" :key="s.label">
        <el-card shadow="never" style="border:1px solid #eef0f4;border-radius:10px;text-align:center">
          <div :style="{fontSize:'28px',fontWeight:'700',color:s.color}">{{s.value}}</div>
          <div style="font-size:12px;color:#94a3b8;margin-top:4px">{{s.label}}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card shadow="never" style="border:1px solid #eef0f4;border-radius:10px">
      <template #header><span style="font-weight:600;font-size:14px;color:#1a202c">告警记录</span></template>
      <el-empty description="暂无告警记录" :image-size="80" />
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { ref } from "vue"
const alertStats = ref([{label:"严重告警",value:"—",color:"#dc2626"},{label:"中等告警",value:"—",color:"#d97706"},{label:"一般告警",value:"—",color:"#6366f1"}])
</script>
