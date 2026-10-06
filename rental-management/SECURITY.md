# Authentication rate limiting

The application applies in-memory fixed-window limits to `POST /auth/login` (10 requests per client IP per 15 minutes) and `POST /auth/change-password` (5 requests per client IP and authenticated user per 15 minutes). Expired windows are cleaned during requests, access is synchronized, and the limiter stores no passwords or tokens. The map is capped at 10,000 keys; while full, new keys are denied until cleanup frees capacity.

This is suitable for the current single-instance deployment. With multiple application instances, each instance would have a separate counter; use a shared/distributed limiter such as Redis or enforce the limits at an edge/reverse proxy before scaling horizontally.
