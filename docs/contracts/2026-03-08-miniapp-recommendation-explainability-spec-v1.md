# MiniApp Recommendation Explainability Spec v1 (2026-03-08)

## 1. Goal
Any recommendation block must expose source tags so operators and users can understand why an item appears.

## 2. Required Fields
- `recommendSourceType` (behavior, campaign, manual, hot)
- `recommendSourceId`
- `recommendReasonTag`
- `rankScore`
- `traceId`

## 3. Rendering Rules
- UI should render at least one reason tag (e.g. "根据你的浏览").
- If reason is missing, degrade to "为你推荐" and mark as generic.
- Recommendation module failure must not block page core content.

## 4. Audit
- Recommendation click logs must carry `traceId` and `route`.
