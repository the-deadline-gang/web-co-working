#!/usr/bin/env bash
# จำลองว่า "จ่ายเงินแล้ว" ให้คำขอเติมเงินที่ค้างอยู่ (ใช้ตอน PAYMENT_PROVIDER=mock บนเครื่องตัวเองเท่านั้น)
# ทำแบบเดียวกับ Bruno "6 Webhook / 07 Pay topup": ยิง webhook ที่เซ็นด้วย PAYMENT_WEBHOOK_SECRET
#
# วิธีใช้ (รันจากโฟลเดอร์ frontend):
#   ./scripts/dev-pay-topup.sh            → จ่ายคำขอ PENDING ล่าสุด
#   ./scripts/dev-pay-topup.sh 3          → จ่ายคำขอ topup_id = 3
#   ./scripts/dev-pay-topup.sh 3 failed   → จำลองว่าจ่ายไม่สำเร็จ
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SECRET=$(grep '^PAYMENT_WEBHOOK_SECRET=' "$ROOT/.env" | cut -d= -f2-)
STATUS="${2:-successful}"
SQL="mysql -ucosync -pcosync -N -B wallet_db"

if [ -n "${1:-}" ]; then
  WHERE="topup_id = $1"
else
  WHERE="status = 'PENDING' ORDER BY topup_id DESC LIMIT 1"
fi
ROW=$(docker exec web-co-working-mysql-1 $SQL -e "SELECT topup_id, gateway_ref, amount_thb FROM topup WHERE $WHERE" 2>/dev/null)
if [ -z "$ROW" ]; then echo "ไม่เจอคำขอเติมเงินที่ค้างอยู่"; exit 1; fi
read -r ID REF AMOUNT <<< "$ROW"

BODY="{\"eventId\":\"evt_dev_$(date +%s)\",\"gatewayRef\":\"$REF\",\"status\":\"$STATUS\",\"amountThb\":$AMOUNT,\"paidAt\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"}"
SIG=$(printf '%s' "$BODY" | openssl dgst -sha256 -hmac "$SECRET" | awk '{print $NF}')

echo "topup_id=$ID amount=$AMOUNT status=$STATUS"
curl -s -o /dev/null -w "webhook → %{http_code}\n" -X POST http://localhost:8082/webhooks/payment \
  -H "Content-Type: application/json" -H "X-Signature: $SIG" --data "$BODY"
