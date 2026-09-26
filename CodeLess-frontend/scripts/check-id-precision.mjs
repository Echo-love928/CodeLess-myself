import { readdir, readFile } from 'node:fs/promises'
import { extname, join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const projectRoot = fileURLToPath(new URL('..', import.meta.url))
const sourceExtensions = new Set(['.ts', '.vue', '.js', '.mts', '.mjs'])
const excludedDirectories = new Set(['.git', 'dist', 'memory', 'node_modules'])

const rules = [
  {
    message: '禁止将 appId 传入 Number、parseInt 或 parseFloat',
    pattern: /\b(?:Number|parseInt|parseFloat)\s*\([^)]*\bappId\b/giu,
  },
  {
    message: '禁止将应用路由 ID 传入 Number、parseInt 或 parseFloat',
    pattern: /\b(?:Number|parseInt|parseFloat)\s*\([^)]*\broute\.params\.id\b/giu,
  },
  {
    message: '禁止对 appId 使用一元加号转数字',
    pattern: /(^|[^+])\+\s*appId(?:\.value)?\b/gmu,
  },
  {
    message: 'appId 的 TypeScript 类型必须是 string',
    pattern: /\bappId\s*\??:\s*number\b/giu,
  },
  {
    message: '禁止将 appId 断言为 number',
    pattern: /\bappId(?:\.value)?\s+as\s+number\b/giu,
  },
]

const listSourceFiles = async (directory) => {
  const entries = await readdir(directory, { withFileTypes: true })
  const nestedFiles = await Promise.all(
    entries.map((entry) => {
      const path = join(directory, entry.name)
      if (entry.isDirectory()) {
        return excludedDirectories.has(entry.name) ? [] : listSourceFiles(path)
      }
      return [path]
    }),
  )
  return nestedFiles.flat().filter((path) => sourceExtensions.has(extname(path)))
}

const failures = []
for (const path of await listSourceFiles(projectRoot)) {
  const source = await readFile(path, 'utf8')
  for (const rule of rules) {
    rule.pattern.lastIndex = 0
    for (const match of source.matchAll(rule.pattern)) {
      const line = source.slice(0, match.index).split(/\r?\n/u).length
      failures.push(`${relative(projectRoot, path)}:${line} ${rule.message}`)
    }
  }
}

if (failures.length > 0) {
  console.error('\nSnowflake ID 精度检查失败：')
  failures.forEach((failure) => console.error(`- ${failure}`))
  process.exitCode = 1
} else {
  console.log('Snowflake ID 精度检查通过：appId 始终保持为字符串。')
}
