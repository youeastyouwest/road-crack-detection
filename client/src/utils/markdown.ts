/**
 * Lightweight Markdown to HTML converter for agent replies.
 * Supports headings, emphasis, code, quotes, lists, links, and line breaks.
 */
export function renderMarkdown(text: string): string {
  if (!text) return ""

  text = text.replace(/<br\s*\/?>/gi, "\n")

  let html = escapeHtml(text)

  html = html.replace(/```([\s\S]*?)```/g, "<pre><code>$1</code></pre>")
  html = html.replace(/`([^`]+)`/g, "<code>$1</code>")
  html = html.replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")
  html = html.replace(/\*(.+?)\*/g, "<em>$1</em>")

  html = html.replace(/^######\s+(.+)$/gm, "<h6>$1</h6>")
  html = html.replace(/^#####\s+(.+)$/gm, "<h5>$1</h5>")
  html = html.replace(/^####\s+(.+)$/gm, "<h4>$1</h4>")
  html = html.replace(/^###\s+(.+)$/gm, "<h3>$1</h3>")
  html = html.replace(/^##\s+(.+)$/gm, "<h2>$1</h2>")
  html = html.replace(/^#\s+(.+)$/gm, "<h1>$1</h1>")

  html = html.replace(/^&gt;\s?(.+)$/gm, "<blockquote>$1</blockquote>")
  html = html.replace(/^(\s*)[-*]\s+(.+)$/gm, '<li data-list="ul">$2</li>')
  html = html.replace(/^(\s*)\d+\.\s+(.+)$/gm, '<li data-list="ol">$2</li>')

  html = wrapListBlocks(html, "ul")
  html = wrapListBlocks(html, "ol")

  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')

  html = html
    .split("\n")
    .map((line) => {
      const trimmed = line.trim()
      if (
        trimmed.startsWith("<pre>") ||
        trimmed.startsWith("</pre>") ||
        trimmed.startsWith("<blockquote>") ||
        trimmed.startsWith("</blockquote>") ||
        trimmed.startsWith("<h1>") ||
        trimmed.startsWith("<h2>") ||
        trimmed.startsWith("<h3>") ||
        trimmed.startsWith("<h4>") ||
        trimmed.startsWith("<h5>") ||
        trimmed.startsWith("<h6>") ||
        trimmed.startsWith("<ul>") ||
        trimmed.startsWith("</ul>") ||
        trimmed.startsWith("<ol>") ||
        trimmed.startsWith("</ol>") ||
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

function wrapListBlocks(html: string, listType: "ul" | "ol"): string {
  const itemPattern = new RegExp(`((?:<li data-list="${listType}">.*?<\\/li>\\n?)+)`, "g")
  const attrPattern = new RegExp(` data-list="${listType}"`, "g")
  return html.replace(itemPattern, (block) => `<${listType}>${block.replace(attrPattern, "")}</${listType}>`)
}
