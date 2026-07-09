import { defineConfig } from "vite"
import vue from "@vitejs/plugin-vue"
import { resolve } from "path"
// ─── Inline Mock API Plugin ───
function mockPlugin() {
  // ── Extended user list for multi-role login ──
    const allUsers = [
    { id:1, username:"admin", password:"admin123", realName:"超级管理员", email:"admin@road.com", phone:"13800000000", roleCode:"ADMIN", roles:["ROLE_ADMIN"], status:"ENABLED", createdAt:"2026-01-01" },
    { id:2, username:"roadadmin", password:"123456", realName:"周道路", email:"roadadmin@road.com", phone:"13800000005", roleCode:"ROAD_ADMIN", roles:["ROLE_ROAD_ADMIN"], status:"ENABLED", deptCode:"ROAD_ADMIN", createdAt:"2026-01-05" },
    { id:3, username:"sanitadmin", password:"123456", realName:"吴环卫", email:"sanitadmin@road.com", phone:"13800000006", roleCode:"SANIT_ADMIN", roles:["ROLE_SANIT_ADMIN"], status:"ENABLED", deptCode:"SANITATION", createdAt:"2026-01-06" },
    { id:4, username:"trafficadmin", password:"123456", realName:"郑交管", email:"trafficadmin@road.com", phone:"13800000007", roleCode:"TRAFFIC_ADMIN", roles:["ROLE_TRAFFIC_ADMIN"], status:"ENABLED", deptCode:"TRAFFIC_POLICE", createdAt:"2026-01-07" },
    { id:5, username:"maintainer", password:"123456", realName:"冯维修", email:"maintainer@road.com", phone:"13800000008", roleCode:"MAINTAINER", roles:["ROLE_MAINTAINER"], status:"ENABLED", createdAt:"2026-01-08" },
    { id:6, username:"crowd", password:"123456", realName:"赵众包", email:"crowd@road.com", phone:"13800000004", roleCode:"CROWDSOURCE", roles:["ROLE_CROWDSOURCE"], status:"ENABLED", createdAt:"2026-02-01" },
  ];
  const deptTree = [ { id:1, name:"总经办", code:"HQ", parentId:0, status:"ENABLED", children:[ { id:2, name:"道路管理部", code:"ROAD_ADMIN", parentId:1, status:"ENABLED", children:[] }, { id:3, name:"环卫部", code:"SANITATION", parentId:1, status:"ENABLED", children:[] }, { id:4, name:"交警部", code:"TRAFFIC_POLICE", parentId:1, status:"ENABLED", children:[] } ] } ];
  const departments = [ { id:1, name:"总经办", code:"HQ", parentId:0, status:"ENABLED" }, { id:2, name:"道路管理部", code:"ROAD_ADMIN", parentId:1, status:"ENABLED" }, { id:3, name:"环卫部", code:"SANITATION", parentId:1, status:"ENABLED" }, { id:4, name:"交警部", code:"TRAFFIC_POLICE", parentId:1, status:"ENABLED" } ];
  const roles = [ { id:1, name:"超级管理员", code:"ADMIN", description:"系统超级管理员", status:"ENABLED" }, { id:2, name:"道路管理员", code:"ROAD_ADMIN", description:"道路管理部门", status:"ENABLED" }, { id:3, name:"环卫人员", code:"SANITATION", description:"环卫部门", status:"ENABLED" }, { id:4, name:"交警人员", code:"TRAFFIC", description:"交警部门", status:"ENABLED" }, { id:5, name:"众包人员", code:"CROWD", description:"众包巡查员", status:"ENABLED" } ];
  function ok(res, data) { res.writeHead(200,{"Content-Type":"application/json","Access-Control-Allow-Origin":"*"}); res.end(JSON.stringify({code:200,message:"success",data,timestamp:Date.now()})); }
  function page(l, q) { const p=parseInt(q.page)||1, s=parseInt(q.size)||20; return {records:l.slice((p-1)*s,(p-1)*s+s),total:l.length,page:p,size:s}; }
  return {
    name: "mock",
    configureServer(svr) {
      svr.middlewares.use((req,res,next) => {
        if (!req.url.startsWith("/api/")) return next();
        const url = new URL(req.url,"http://localhost");
        const path = url.pathname, q = Object.fromEntries(url.searchParams), m = req.method.toUpperCase();
        let body = "";
        req.on("data",c=>body+=c);
        req.on("end",()=>{
          try {
            // ─── Auth: Multi-role login ───
            if (path==="/api/auth/login"&&m==="POST") {
              const { username, password } = JSON.parse(body);
              const user = allUsers.find(u => u.username === username && u.password === password);
              if (!user) return ok(res,{accessToken:"mock-token",refreshToken:"mock-refresh",tokenType:"Bearer",expiresIn:7200,userId:1,username:"admin",realName:"超级管理员",roles:["ROLE_ADMIN"]});
              return ok(res,{
                accessToken:"mock-token-"+user.username,
                refreshToken:"mock-refresh-"+user.username,
                tokenType:"Bearer",expiresIn:7200,
                userId:user.id, username:user.username,
                realName:user.realName,
                roles:user.roles,
              });
            }
            if (path==="/api/auth/refresh"&&m==="POST") return ok(res,{accessToken:"mock-token",refreshToken:"mock-refresh",tokenType:"Bearer",expiresIn:7200,userId:1,username:"admin",realName:"超级管理员",roles:["ROLE_ADMIN"]});
            if (path==="/api/auth/send-code"&&m==="POST") return ok(res,"123456");
            if (path==="/api/auth/register"&&m==="POST") return ok(res,null);
            if (path==="/api/auth/change-password"&&m==="PUT") return ok(res,null);
            if (path==="/api/auth/reset-password"&&m==="POST") return ok(res,null);
            // ─── User ───
            if (path==="/api/user/page"&&m==="GET") return ok(res,page(allUsers,q));
            if (path==="/api/user/current"&&m==="GET") {
              const auth = req.headers["authorization"] || "";
              const tokenUser = auth ? allUsers.find(u => u.roles.some(r => "mock-token-"+u.username === auth.replace("Bearer ",""))) : null;
              const u = tokenUser || allUsers[0];
              return ok(res,{...u, roles:u.roles, permissions:["*:*:*"]});
            }
            const u=path.match(/^\/api\/user\/(\d+)$/), us=path.match(/^\/api\/user\/(\d+)\/status$/), ur=path.match(/^\/api\/user\/(\d+)\/reset-password$/);
            if (u&&m==="GET") return ok(res,allUsers[0]); if (u&&m==="PUT") return ok(res,null); if (u&&m==="DELETE") return ok(res,null);
            if (us&&m==="PUT") return ok(res,null); if (ur&&m==="PUT") return ok(res,null); if (path==="/api/user"&&m==="POST") return ok(res,null);
            // ─── Department ───
            if (path==="/api/department/tree"&&m==="GET") return ok(res,deptTree);
            if (path==="/api/department/list"&&m==="GET") return ok(res,departments);
            const d=path.match(/^\/api\/department\/(\d+)$/); if (d&&m==="GET") return ok(res,departments[0]); if (path==="/api/department"&&m==="POST") return ok(res,null); if (d&&m==="PUT") return ok(res,null); if (d&&m==="DELETE") return ok(res,null);
            // ─── Role ───
            if (path==="/api/role/list"&&m==="GET") return ok(res,roles);
            const r=path.match(/^\/api\/role\/(\d+)$/); if (r&&m==="GET") return ok(res,roles[0]); if (path==="/api/role"&&m==="POST") return ok(res,null); if (r&&m==="PUT") return ok(res,null); if (r&&m==="DELETE") return ok(res,null);
            // ─── File Upload ───
            if (path==="/api/file/upload"&&m==="POST") return ok(res,{fileId:1,fileUrl:"https://picsum.photos/id/1043/800/600",fileName:"uploaded_image.jpg",fileSize:2048576,uploadedAt:new Date().toISOString()});
                        // ─── Detection Tasks ───
            if (path==="/api/detection-tasks"&&m==="GET") {
              const taskList = globalThis.__mockTasks || []
              return ok(res, page(taskList, q));
            }
            if (path==="/api/detection-tasks"&&m==="POST") {
              const td = JSON.parse(body || "{}")
              const allTasks = globalThis.__mockTasks || []
              const maxId = allTasks.reduce((m,t)=>Math.max(m,t.id||0),0)
              const newId = maxId + 1
              const newTask = {
                id: newId, taskCode: "DT-"+new Date().toISOString().slice(0,10).replace(/-/g,"")+"-"+String(newId).padStart(6,"0"),
                dataSourceType: td.dataSourceType || "IMAGE", fileName: td.fileName || "unknown",
                fileUrl: td.fileUrl || "", location: td.location || "",
                remark: td.remark || "", submittedBy: td.submittedBy || "admin",
                status: "PENDING", createdAt: new Date().toISOString(),
              }
              if (!globalThis.__mockTasks) globalThis.__mockTasks = []
              globalThis.__mockTasks.push(newTask)
              return ok(res, newTask)
            }
            const dtGet = path.match(/^\/api\/detection-tasks\/(\d+)$/)
            if (dtGet && m==="GET") {
              const tid = parseInt(dtGet[1])
              const tasks = globalThis.__mockTasks || []
              const task = tasks.find(t=>t.id===tid)
              return ok(res, task || {id:tid,status:"PENDING"})
            }
            const dtExec = path.match(/^\/api\/detection-tasks\/(\d+)\/execute$/)
            if (dtExec && m==="POST") {
              const tid2 = parseInt(dtExec[1])
              const tasks2 = globalThis.__mockTasks || []
              const task2 = tasks2.find(t=>t.id===tid2)
              if (task2) { task2.status = "PROCESSING" }
              setTimeout(() => {
                if (task2) {
                  task2.status = "COMPLETED";
                  task2.completedAt = new Date().toISOString();
                  task2.crackCount = 1;
                  const types = ["TRANSVERSE_CRACK","LONGITUDINAL_CRACK","NET_CRACK","POTHOLE"];
                  const sevs = ["LOW","MEDIUM","HIGH"];
                  const dt = types[Math.floor(Math.random()*types.length)];
                  const sv = sevs[Math.floor(Math.random()*3)];
                  task2.severityLevel = sv;
                  task2.damageType = dt;
                  const confMap: Record<string,number> = { LOW:0.71+Math.random()*0.15, MEDIUM:0.78+Math.random()*0.12, HIGH:0.85+Math.random()*0.1 };
                  const sugMap: Record<string,string> = { LOW:"轻微病害，继续观察，纳入日常养护计划", MEDIUM:"中等病害，建议尽快安排养护维修，防止病害扩展", HIGH:"严重病害，需立即派单维修处理，存在安全隐患" };
                  const wMap: Record<string,number> = { LOW:30+Math.random()*80, MEDIUM:50+Math.random()*150, HIGH:100+Math.random()*200 };
                  const hMap: Record<string,number> = { LOW:10+Math.random()*30, MEDIUM:20+Math.random()*60, HIGH:30+Math.random()*100 };
                  task2._resultData = {
                    taskId: task2.id,
                    summary: "AI检测已完成，识别到1处疑似病害 - "+({TRANSVERSE_CRACK:"横向裂缝",LONGITUDINAL_CRACK:"纵向裂缝",NET_CRACK:"网状裂缝",POTHOLE:"坑槽"}[dt]||dt),
                    items: [{
                      damageType: dt, severityLevel: sv,
                      confidence: confMap[sv],
                      boundingBox: { x:Math.floor(Math.random()*200), y:Math.floor(Math.random()*150), w:Math.floor(wMap[sv]), h:Math.floor(hMap[sv]) },
                      lengthMm: Math.floor(50+Math.random()*500),
                      widthMm: Math.floor(2+Math.random()*15),
                      areaMm2: Math.floor(200+Math.random()*5000),
                      suggestion: sugMap[sv],
                    }],
                    completedAt: task2.completedAt,
                  };
                }
              }, 1500)
              return ok(res, null)
            }
            const dtResult = path.match(/^\/api\/detection-tasks\/(\d+)\/result$/)
            if (dtResult && m==="GET") {
              const tid3 = parseInt(dtResult[1])
              const task3 = (globalThis.__mockTasks || []).find(x=>x.id===tid3)
              if (task3 && task3._resultData) return ok(res, task3._resultData)
              return ok(res, { taskId:tid3, summary:"暂无结果数据", items:[], completedAt:new Date().toISOString() })
            }
            // ─── Work Orders (deptCode filtered mock) ───
            if (path==="/api/work-orders"&&m==="GET") {
              const allOrders = globalThis.__mockOrders || (globalThis.__mockOrders = []);
              return ok(res, page(allOrders, q));
            }
            if (path==="/api/work-orders"&&m==="POST") {
              const body2 = JSON.parse(body || "{}");
              const allOrders2 = globalThis.__mockOrders || [];
              const maxId = allOrders2.reduce((m,o)=>Math.max(m,o.id||0),0);
              const newOrder = { id:maxId+1, workOrderCode:"WO"+new Date().toISOString().slice(0,10).replace(/-/g,"")+String(maxId+1).padStart(3,"0"), ...body2, status:"PENDING_ASSIGNMENT", assignee:"", createdAt:new Date().toISOString().slice(0,10) };
              allOrders2.unshift(newOrder);
              return ok(res, { id: newOrder.id });
            }
            // ─── Work Order: cancel / assign / status ───
            const woCancel = path.match(/^\/api\/work-orders\/(\d+)\/cancel$/);
            if (woCancel && m==="PUT") {
              const allOrders3 = globalThis.__mockOrders || [];
              const wo = allOrders3.find(o=>o.id===parseInt(woCancel[1]));
              if (wo) { wo.status = "CANCELLED"; }
              return ok(res, wo || null);
            }
            const woAssign = path.match(/^\/api\/work-orders\/(\d+)\/assign$/);
            if (woAssign && m==="PUT") {
              const body3 = JSON.parse(body || "{}");
              const allOrders4 = globalThis.__mockOrders || [];
              const wo2 = allOrders4.find(o=>o.id===parseInt(woAssign[1]));
              if (wo2) { wo2.status = "ASSIGNED"; wo2.assignee = body3.assignee || wo2.assignee; }
              return ok(res, wo2 || null);
            }
            const woStatus = path.match(/^\/api\/work-orders\/(\d+)\/status$/);
            if (woStatus && m==="PUT") {
              const body4 = JSON.parse(body || "{}");
              const allOrders5 = globalThis.__mockOrders || [];
              const wo3 = allOrders5.find(o=>o.id===parseInt(woStatus[1]));
              if (wo3) wo3.status = body4.status || wo3.status;
              return ok(res, wo3 || null);
            }
            // ─── Reports ───
            if (path==="/api/maintenance-reports"&&m==="GET") return ok(res,page([],q));
            if (path==="/api/maintenance-reports"&&m==="POST") return ok(res,{id:2});
            const mr=path.match(/^\/api\/maintenance-reports\/(\d+)$/); if (mr&&m==="GET") return ok(res,{id:1});
            // ─── Audit Log ───
            if (path==="/api/audit-log/page"&&m==="GET") return ok(res,page([],q));
            // ─── System Config ───
            if (path==="/api/system-config"&&m==="GET") return ok(res,{siteName:"途安智巡道路裂缝检测系统",logo:"/logo.png",detectionInterval:30,alertThreshold:3,maintenanceNotify:true,dataRetentionDays:180,maxUploadSize:50,allowRegister:false,emailNotify:true,smsNotify:false,darkMode:false,language:"zh-CN"});
            if (path==="/api/system-config"&&m==="PUT") return ok(res,null);
            // ─── Statistics ───
            if (path==="/api/statistics/dashboard"&&m==="GET") {
              const t = globalThis.__mockTasks || [];
              const o = globalThis.__mockOrders || [];
              const today = new Date().toISOString().slice(0,10);
              const detToday = t.filter(x=>x.createdAt?.startsWith(today)).length;
              const totalCracks = t.filter(x=>x.status==="COMPLETED").length;
              const pendingAlerts = t.filter(x=>x.status==="COMPLETED"&&x.severityLevel==="HIGH").length;
              return ok(res,{totalRoads:0,monitoredRoads:0,detectionToday:detToday,totalCracksDetected:totalCracks,totalWorkOrders:o.length});
            }
            if (path==="/api/statistics/trend"&&m==="GET") {
              const t = globalThis.__mockTasks || [];
              const grouped: Record<string,number> = {};
              t.forEach(x => {
                if (!x.createdAt) return;
                const d = x.createdAt.slice(0,10);
                grouped[d] = (grouped[d] || 0) + 1;
              });
              const data = Object.entries(grouped).sort(([a],[b])=>a.localeCompare(b)).map(([date,count])=>({date,count}));
              return ok(res, data);
            }
            if (path==="/api/statistics/crack-type"&&m==="GET") {
              return ok(res, [
                { type:"TRANSVERSE_CRACK", count:0, percentage:0 },
                { type:"LONGITUDINAL_CRACK", count:0, percentage:0 },
                { type:"NET_CRACK", count:0, percentage:0 },
              ]);
            }
            if (path==="/api/statistics/severity"&&m==="GET") return ok(res,[]);
            if (path==="/api/statistics/department-workload"&&m==="GET") return ok(res,[]);
            // ─── Alerts ───
            if (path==="/api/alerts"&&m==="GET") return ok(res,page([],q));
            if (path==="/api/alerts/recent"&&m==="GET") return ok(res,[]);
            // ─── Roads ───
            if (path==="/api/roads"&&m==="GET") return ok(res,page([],q));
            res.writeHead(404,{"Content-Type":"application/json"}); res.end(JSON.stringify({code:404,message:"Not found"}));
          } catch(e) { res.writeHead(500,{"Content-Type":"application/json"}); res.end(JSON.stringify({code:500,message:e.message})); }
        });
      });
    },
  };
}
export default defineConfig({
  plugins: [vue(), mockPlugin()],
  resolve: { alias: { "@": resolve(__dirname, "src") } },
  server: { port: 5173 },
})




