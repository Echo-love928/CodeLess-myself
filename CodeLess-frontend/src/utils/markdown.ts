import DOMPurify from 'dompurify'
import hljs from 'highlight.js/lib/core'
import bash from 'highlight.js/lib/languages/bash'
import css from 'highlight.js/lib/languages/css'
import javascript from 'highlight.js/lib/languages/javascript'
import json from 'highlight.js/lib/languages/json'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
import MarkdownIt from 'markdown-it'

hljs.registerLanguage('html', xml)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('css', css)
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('ts', typescript)
hljs.registerLanguage('json', json)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('shell', bash)

const languageLabels: Record<string, string> = {
  html: 'HTML',
  xml: 'HTML',
  css: 'CSS',
  javascript: 'JavaScript',
  js: 'JavaScript',
  typescript: 'TypeScript',
  ts: 'TypeScript',
  json: 'JSON',
  bash: 'Shell',
  shell: 'Shell',
}

const escapeHtml = (code: string) =>
  code.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;')

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  typographer: false,
})

markdown.renderer.rules.fence = (tokens, index) => {
  const token = tokens[index]!
  const language = (token.info.trim().split(/\s+/)[0]?.toLowerCase() || '').replace(
    /[^a-z0-9_-]/g,
    '',
  )
  const highlighted =
    language && hljs.getLanguage(language)
      ? hljs.highlight(token.content, { language, ignoreIllegals: true }).value
      : escapeHtml(token.content)
  const label = languageLabels[language] || (language ? language.toUpperCase() : 'CODE')
  const languageClass = language ? ` language-${language}` : ''

  return `<div class="md-code-block"><div class="md-code-header"><span>${label}</span></div><pre><code class="hljs${languageClass}">${highlighted}</code></pre></div>`
}

const defaultLinkOpen = markdown.renderer.rules.link_open
markdown.renderer.rules.link_open = (tokens, index, options, env, self) => {
  tokens[index]?.attrSet('target', '_blank')
  tokens[index]?.attrSet('rel', 'noopener noreferrer')
  return defaultLinkOpen
    ? defaultLinkOpen(tokens, index, options, env, self)
    : self.renderToken(tokens, index, options)
}

/**
 * 将 AI 返回的 Markdown 转换为安全 HTML。
 * Markdown-it 能容忍流式阶段尚未闭合的 fenced code block，适合实时刷新。
 */
export const renderMarkdown = (content: string) =>
  DOMPurify.sanitize(markdown.render(content), {
    ADD_ATTR: ['target'],
    FORBID_TAGS: ['script', 'style', 'iframe', 'object', 'embed'],
    FORBID_ATTR: ['style', 'onerror', 'onload'],
  })
