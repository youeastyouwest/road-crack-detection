import fs from "fs"
const content = fs.readFileSync("src/views/detection/DataScreen.vue", "utf-8")

// Find the layers definition
const start = content.indexOf('id: "crack"')
if (start >= 0) {
  const layersDef = content.substring(start, start + 120)
  console.log("Source layers:", layersDef)
  
  // Now simulate what Vite does: parse SFC, extract script, compile
  const { parse, compileScript } = await import("@vue/compiler-sfc")
  const { descriptor } = parse(content, { filename: "test.vue" })
  const compiled = compileScript(descriptor, { id: "test" })
  
  // Check if the layers survived compilation
  const cIdx = compiled.content.indexOf("裂缝")
  console.log("\nAfter compileScript:")
  console.log("裂缝 preserved:", cIdx >= 0)
  if (cIdx >= 0) {
    console.log("Context:", compiled.content.substring(Math.max(0, cIdx-5), cIdx+15))
  }
}
