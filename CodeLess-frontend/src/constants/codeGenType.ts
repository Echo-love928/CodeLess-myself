// 与后端 CodeGenTypeEnum 的 value/text 保持一致。
export const CODE_GEN_TYPE_OPTIONS = [
  { value: 'html', label: '原生 HTML 模式' },
  { value: 'multi_file', label: '原生多文件模式' },
] as const

export const getCodeGenTypeLabel = (value?: string): string =>
  CODE_GEN_TYPE_OPTIONS.find((option) => option.value === value)?.label ?? value ?? '-'
