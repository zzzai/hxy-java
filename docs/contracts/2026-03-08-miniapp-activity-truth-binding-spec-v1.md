# MiniApp Activity Truth Binding Spec v1 (2026-03-08)

## 1. Scope
Applies to coupon, points mall, flash sale/new-arrival labels, and home marketing cards.

## 2. Binding Rules
- Any marketing UI badge or countdown must bind to server-side `activityId` and effective window.
- "领券成功" animation must be triggered only after `POST /promotion/coupon/take` success.
- "限时/折扣/新品" text must map to backend activity type and validity.

## 3. Required Fields
- `activityId`
- `activityType`
- `startTime`
- `endTime`
- `serverNow`
- `status`

## 4. Validation
- If client time and server time differ, server time wins.
- Expired activity must not render actionable CTA.
- Unknown activity type should degrade to neutral label.
