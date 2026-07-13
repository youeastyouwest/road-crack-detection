import esbuild from "esbuild"

// Test if esbuild correctly handles Chinese
const testCode = "const layers = [{ id: 'crack', label: '裂缝测试', color: '#ef4444' }]"
const result = await esbuild.transform(testCode, { loader: "ts", minify: true })
console.log("Input:", testCode)
console.log("Output:", result.code)
console.log("Chinese preserved:", result.code.includes("裂缝测试"))
