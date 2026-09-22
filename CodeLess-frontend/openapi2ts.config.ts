import type { GenerateServiceProps } from '@umijs/openapi'

// 根据后端接口生成前端请求和 TS 模型代码。
// Java Long 在浏览器中必须按字符串传递，否则 Snowflake ID 会丢失精度。
type MutableSchema = {
  type?: string
  format?: string
  properties?: Record<string, MutableSchema>
}

type MutableOperation = {
  parameters?: Array<{ name?: string; schema?: MutableSchema }>
}

type MutableOpenApi = {
  components?: { schemas?: Record<string, MutableSchema> }
  paths?: Record<string, Record<string, MutableOperation>>
}

const snowflakeFieldNames = new Set(['id', 'appId', 'userId'])

const markAsString = (schema?: MutableSchema) => {
  if (!schema || schema.format !== 'int64') return
  schema.type = 'string'
  delete schema.format
}

const config: GenerateServiceProps = {
  requestLibPath: "import request from '@/request'",
  schemaPath: 'http://localhost:8123/api/v3/api-docs',
  serversPath: './src',
  hook: {
    afterOpenApiDataInited(openAPIData) {
      const document = openAPIData as unknown as MutableOpenApi
      const schemas = document.components?.schemas ?? {}

      for (const [schemaName, schema] of Object.entries(schemas)) {
        for (const [fieldName, fieldSchema] of Object.entries(schema.properties ?? {})) {
          if (snowflakeFieldNames.has(fieldName)) markAsString(fieldSchema)
        }

        // 新增应用、新增用户的返回值也是 Snowflake ID。
        if (schemaName === 'BaseResponseLong') markAsString(schema.properties?.data)
      }

      for (const pathItem of Object.values(document.paths ?? {})) {
        for (const operation of Object.values(pathItem)) {
          for (const parameter of operation.parameters ?? []) {
            if (parameter.name && snowflakeFieldNames.has(parameter.name)) {
              markAsString(parameter.schema)
            }
          }
        }
      }

      return openAPIData
    },
  },
}

export default config
