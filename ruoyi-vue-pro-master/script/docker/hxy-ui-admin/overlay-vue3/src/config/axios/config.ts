const config: {
  base_url: string
  result_code: number | string
  default_headers: AxiosHeaders
  request_timeout: number
} = {
  /**
   * API 请求基础路径。通过 Nginx 同源代理转发到后端。
   */
  base_url: import.meta.env.VITE_BASE_URL + import.meta.env.VITE_API_URL,
  /**
   * 与 Vue3 原生响应拦截逻辑保持一致（内部以 200 作为成功分支）
   */
  result_code: 200,
  /**
   * 接口请求超时时间
   */
  request_timeout: 30000,
  /**
   * 默认接口请求类型
   */
  default_headers: 'application/json'
}

export { config }
