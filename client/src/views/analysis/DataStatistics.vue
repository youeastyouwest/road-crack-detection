<template>
  <div>
    <div style="margin-bottom:20px"><h2 style="font-size:20px;font-weight:600;color:#1a202c;margin-bottom:4px">数据统计</h2><p style="font-size:13px;color:#94a3b8">检测数据多维统计</p></div>
    <el-row :gutter="16" style="margin-bottom:20px">
      <el-col :span="12"><el-card shadow="never" style="border:1px solid #eef0f4;border-radius:10px"><template #header><span style="font-weight:600;font-size:14px;color:#1a202c">每月检测数量</span></template><div ref="barRef" style="height:300px"></div></el-card></el-col>
      <el-col :span="12"><el-card shadow="never" style="border:1px solid #eef0f4;border-radius:10px"><template #header><span style="font-weight:600;font-size:14px;color:#1a202c">各类型占比</span></template><div ref="pieRef" style="height:300px"></div></el-card></el-col>
    </el-row>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from "vue"
import * as echarts from "echarts"
const barRef = ref<HTMLElement>()
const pieRef = ref<HTMLElement>()
onMounted(() => {
  if (barRef.value) echarts.init(barRef.value).setOption({
    tooltip:{trigger:"axis"},grid:{left:40,right:20,bottom:30,top:20},
    xAxis:{type:"category",data:["1月","2月","3月","4月","5月","6月"],axisLabel:{color:"#94a3b8",fontSize:11}},
    yAxis:{type:"value",axisLabel:{color:"#94a3b8"},splitLine:{lineStyle:{color:"#f1f5f9"}}},
    series:[{type:"bar",data:[],itemStyle:{color:"#4361ee",borderRadius:[4,4,0,0]}}]
  })
  if (pieRef.value) echarts.init(pieRef.value).setOption({
    tooltip:{trigger:"item"},
    series:[{type:"pie",radius:["40%","70%"],label:{color:"#4a5568",fontSize:12},data:[]}]
  })
})
</script>
