import { marked, type Tokens } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

// 配置 marked 使用 highlight.js 进行代码高亮
marked.use({
  renderer: {
    code({ text, lang }: Tokens.Code) {
      const language = lang && hljs.getLanguage(lang) ? lang : 'plaintext'
      const highlighted = hljs.highlight(text, { language }).value
      return `<pre><code class="hljs language-${language}">${highlighted}</code></pre>`
    }
  }
})

/**
 * 预处理 Markdown 内容，修复常见的格式问题
 * 主要用于处理流式输出时不完整的 Markdown 语法
 */
function preprocessMarkdown(content: string): string {
  if (!content) return ''

  // 修复标题格式：###1. -> ### 1.（确保 # 后面有空格）
  content = content.replace(/^(#{1,6})([^\s#])/gm, '$1 $2')

  // 修复列表格式：-xxx 或 *xxx -> - xxx 或 * xxx
  content = content.replace(/^([\-\*])([^\s\-\*])/gm, '$1 $2')

  // 修复有序列表格式：1.xxx -> 1. xxx
  content = content.replace(/^(\d+\.)([^\s\.])/gm, '$1 $2')

  return content
}

export function renderMarkdown(content: string): string {
  if (!content) return ''

  // 预处理内容，修复格式问题
  const processedContent = preprocessMarkdown(content)

  // 配置 marked 选项
  const options = {
    breaks: true,        // 允许换行符转换为 <br>
    gfm: true,           // 启用 GitHub Flavored Markdown
    headerIds: false,    // 禁用标题 ID（避免重复 ID 问题）
    mangle: false        // 禁用邮件地址混淆
  }

  return marked.parse(processedContent, options) as string
}
