# Health Data Red Lines

Allowed:
- G0 routine preference and non-sensitive recommendations
- Abstract recommendation output such as "recommend warming therapy"

Restricted:
- G1 inferred body-constitution or health tags from face/tongue diagnosis
- requires consent, purpose whitelist, audit record, and manual review threshold

Prohibited:
- G2 pathological diagnosis, discriminatory labels, denial-of-service labels, differential pricing, hidden score downgrades
- raw face/tongue images or raw physiological parameters flowing into marketing or centralized plaintext stores

Required audit fields:
- `tenantId`
- `purposeCode`
- `consentId`
- `runId/orderId/payRefundId/sourceBizNo/errorCode`
- label policy decision and reviewer when applicable
