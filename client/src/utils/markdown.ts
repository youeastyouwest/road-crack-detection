/**
 * 轻量 Markdown → HTML 转换器（无需额外依赖）
 * 支持：**粗体**、*斜体*、`代码`、> 引用、列表、链接、换行
 */
export function renderMarkdown(text: string): string {
  if (!text) return ""

  // 把输入里已有的 HTML 换行标签先转成普通换行，避免转义后暴露 <br> 文本
  text = text.replace(/<br\s*\/?>/gi, "\n")

  let html = escapeHtml(text)

  // 代码块 ```...```
  html = html.replace(/```([\s\S]*?)```/g, "<pre><code>$1</code></pre>")

  // 行内代码 `...`
  html = html.replace(/`([^`]+)`/g, "<code>$1</code>")

  // 粗体 **...**
  html = html.replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")

  // 斜体 *...*（排除已处理的 strong 标签）
  html = html.replace(/\*(.+?)\*/g, "<em>$1</em>")

  // 引用 > ...
  html = html.replace(/^&gt;\s?(.+)$/gm, "<blockquote>$1</blockquote>")

  // 无序列表 - ... 或 * ...
  html = html.replace(/^(\s*)[-\*]\s+(.+)$/gm, "<li>$2</li>")

  // 包裹相邻的 <li> 为 <ul>
  html = html.replace(/(<li>.*<\/li>)(\s*(?!<li>))/g, "<ul>$1</ul>$2")

  // 链接 [text](url)
  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')

  // 换行 → <br>（排除已包裹在块级元素内的）
  html = html
    .split("\n")
    .map((line) => {
      const trimmed = line.trim()
      if (
        trimmed.startsWith("<pre>") ||
        trimmed.startsWith("</pre>") ||
        trimmed.startsWith("<blockquote>") ||
        trimmed.startsWith("</blockquote>") ||
        trimmed.startsWith("<ul>") ||
        trimmed.startsWith("</ul>") ||
        trimmed.startsWith("<li>") ||
        trimmed === ""
      ) {
        return line
      }
      return line + "<br>"
    })
    .join("\n")

  return html
}

function escapeHtml(str: string): string {
  return str
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
}
