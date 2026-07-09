const { parse } = require("@vue/compiler-sfc");
const fs = require("fs");
const src = fs.readFileSync("src/views/detection/DataScreen.vue", "utf-8");
console.log("File length:", src.length);
const { descriptor } = parse(src, { filename: "test.vue" });
const script = descriptor.scriptSetup || descriptor.script;
if (script) {
  const idx = script.content.indexOf("裂缝");
  console.log("裂缝 in parsed SFC:", idx >= 0, idx);
  if (idx >= 0) {
    console.log("Context:", script.content.substring(Math.max(0, idx-5), idx+10));
  } else {
    const idx2 = script.content.indexOf("瑁");
    console.log("瑁 in parsed SFC:", idx2 >= 0);
    if (idx2 >= 0) console.log("Garbled context:", script.content.substring(idx2-5, idx2+10));
  }
} else {
  console.log("No script found");
}
