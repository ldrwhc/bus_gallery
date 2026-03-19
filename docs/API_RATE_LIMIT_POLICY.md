# Interface Rate Limits (2026-03-19)

All over-limit responses are HTTP 429 (code A0429).

## Auth

| Interface | Dimension | Limit | Window |
|---|---|---:|---|
| POST /api/auth/login | global | 1200 | 1 min |
| POST /api/auth/login | ip | 120 | 1 min |
| POST /api/auth/login | account | 30 | 1 min |
| POST /api/auth/register/send-email-code | ip | 30 | 1 min |
| POST /api/auth/register/send-email-code | email | 12 | 1 hour |
| POST /api/auth/register/send-email-code | ip | 200 | 1 day |
| POST /api/auth/register/send-email-code | email | 40 | 1 day |
| POST /api/auth/register/send-email-code | account | 40 | 1 day |
| POST /api/auth/password/forgot/send-email-code | ip | 30 | 1 min |
| POST /api/auth/password/forgot/send-email-code | identifier | 15 | 1 hour |
| POST /api/auth/password/forgot/send-email-code | ip | 200 | 1 day |
| POST /api/auth/password/forgot/send-email-code | account | 40 | 1 day |
| POST /api/auth/email/bind/send-email-code | ip | 30 | 1 min |
| POST /api/auth/email/bind/send-email-code | email | 12 | 1 hour |
| POST /api/auth/email/bind/send-email-code | ip | 200 | 1 day |
| POST /api/auth/email/bind/send-email-code | email | 40 | 1 day |
| POST /api/auth/email/bind/send-email-code | account | 40 | 1 day |
| POST /api/auth/password/change/send-email-code | ip | 30 | 1 min |
| POST /api/auth/password/change/send-email-code | email | 12 | 1 hour |
| POST /api/auth/password/change/send-email-code | ip | 200 | 1 day |
| POST /api/auth/password/change/send-email-code | account | 40 | 1 day |

## Captcha Trigger

| Flow | Trigger | Threshold | Window |
|---|---|---:|---|
| login | failed attempts by ip/account | 5 | 30 min |
| forgot password | request count by ip/account | 6 | 15 min |

## Upload

| Interface | Dimension | Limit | Window |
|---|---|---:|---|
| POST /api/upload and /api/images/upload | global | 300 | 1 min |
| POST /api/upload and /api/images/upload | ip | 30 | 1 min |
| POST /api/upload and /api/images/upload | user | 20 | 1 min |
| POST /api/upload and /api/images/upload | user uploads | 120 | 1 day |
| POST /api/upload | pending per user | 30 | instant |
| POST /api/upload | pending global | 5000 | instant |

## Nginx

| Location | IP rate | Global rate | Burst |
|---|---:|---:|---|
| /api/auth/ | 12 r/s | 120 r/s | 30 / 220 |
| /api/upload | 6 r/s | 40 r/s | 12 / 80 |
| /api/ | 25 r/s | 180 r/s | 40 / 240 |
