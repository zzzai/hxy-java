# Booking Route and API Truth

Known high-value checks:
- frontend `GET /booking/technician/list-by-store` vs backend `GET /booking/technician/list`
- frontend `GET /booking/time-slot/list` vs backend `GET /booking/slot/list` or `list-by-technician`
- frontend `PUT /booking/order/cancel` vs backend `POST /booking/order/cancel`
- frontend `POST /booking/addon/create` vs backend `POST /app-api/booking/addon/create`

Runtime truth pages:
- `/pages/booking/technician-list`
- `/pages/booking/technician-detail`
- `/pages/booking/order-confirm`
- `/pages/booking/order-detail`
- `/pages/booking/order-list`
- `/pages/booking/addon`
