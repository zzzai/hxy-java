import request from '@/config/axios'

export interface TemplateValidateItem {
  attributeId: number
  attrRole: number
  required: boolean
  affectsPrice: boolean
  affectsStock: boolean
}

export interface ProductCategoryTemplateValidateReq {
  categoryId: number
  templateVersionId?: number
  items: TemplateValidateItem[]
}

export interface ProductCategoryTemplateValidateResp {
  pass: boolean
  errors: Array<{ code: string; message: string }>
  warnings: Array<{ code: string; message: string }>
}

export interface ProductSkuGeneratePreviewReq {
  spuId: number
  categoryId: number
  templateVersionId?: number
  baseSku: {
    price: number
    marketPrice: number
    costPrice: number
    stock: number
  }
  specSelections: Array<{
    attributeId: number
    optionIds: number[]
  }>
}

export interface ProductSkuGeneratePreviewResp {
  taskNo: string
  combinationCount: number
  truncated: boolean
  items: Array<{
    specHash: string
    specSummary: string
    existsSkuId?: number
    suggestedSku?: {
      price: number
      marketPrice: number
      stock: number
    }
  }>
}

export interface ProductSkuGenerateCommitReq {
  taskNo: string
  idempotencyKey: string
}

export interface ProductSkuGenerateCommitResp {
  taskNo: string
  status: number
  accepted: boolean
  idempotentHit: boolean
}

export const validateCategoryTemplate = (data: ProductCategoryTemplateValidateReq) => {
  return request.post<ProductCategoryTemplateValidateResp>({ url: '/product/template/validate', data })
}

export const previewSkuGenerate = (data: ProductSkuGeneratePreviewReq) => {
  return request.post<ProductSkuGeneratePreviewResp>({ url: '/product/template/sku-generator/preview', data })
}

export const commitSkuGenerate = (data: ProductSkuGenerateCommitReq) => {
  return request.post<ProductSkuGenerateCommitResp>({ url: '/product/template/sku-generator/commit', data })
}
