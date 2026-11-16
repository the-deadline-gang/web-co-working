// รูปแบบข้อมูลที่รับจาก backend (ดูจาก docs/openapi/identity.yaml)

/** ข้อมูลผู้ใช้ที่ login อยู่ → GET /me */
export type Me = {
  userId: number;
  displayId: string; // "U-000006"
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
};
export type Wallet = {
  walletId: number;
  displayId: string; // "W-000018"
  balance: number;
};
/** ตั้งค่าการเติมเงิน → GET /topups/config */
export type TopupConfig = {
  rate: number; // 1 บาท ได้กี่ Token
  minThb: number; // เติมต่ำสุด (บาท)
  maxThb: number; // เติมสูงสุด (บาท)
  quickAmounts: number[]; // ปุ่มจำนวนลัด เช่น [200, 500, 1000, 2000]
  qrExpireMinutes: number; // QR หมดอายุในกี่นาที
};

/** คำขอเติมเงิน 1 ครั้ง → POST /topups และ GET /topups/{id} */
export type Topup = {
  topupId: number;
  displayId: string; // "PAY-20261003-0087"
  amountThb: number;
  tokenAmount: number;
  status: "PENDING" | "PAID" | "EXPIRED" | "FAILED";
  qrImage: string | null; // รูป QR (มีเฉพาะตอน PENDING) ใช้ใส่ <img src> ได้เลย
  expiresAt: string; // เวลา QR หมดอายุ
  paidAt: string | null;
  balanceAfter: number | null; // ยอดหลังเติม (มีเฉพาะตอน PAID)
};
