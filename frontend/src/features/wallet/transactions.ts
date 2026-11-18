// แปลงรายการจาก API (WalletTransaction) → ข้อมูลพร้อมแสดงในหน้า Wallet (Transaction)
// API ส่งมาแค่ "ประเภท" กับ "ตัวเลข" → ชื่อรายการ ไอคอน สี กลุ่มปุ่มกรอง ต้องกำหนดเองที่นี่
// เรียกฝั่ง server แล้วส่งผลลัพธ์ (ข้อมูลธรรมดา) ให้ TransactionList ที่เป็น client
import type { WalletTransaction } from "@/types/api";

// กลุ่มของปุ่มกรอง (ทั้งหมด / เติมเงิน / ค่าจอง / ค่ายืม)
export type TxGroup = "topup" | "booking" | "loan";

export type Transaction = {
  id: string;
  group: TxGroup;
  title: string; // "เติมเงิน"
  subtitle: string; // "ประวัติการเติมเงิน"
  ref: string; // "TH-000001"
  time: string; // "16 พ.ย. 15:48"
  month: string; // "พฤศจิกายน 2569" → ใช้เป็นหัวข้อกลุ่ม
  amount: number; // + = เงินเข้า, - = เงินออก
  // ชื่อไอคอน (เป็นข้อความ เพราะ server ส่ง component ให้ client ไม่ได้) → แปลงเป็นไอคอนจริงใน TransactionList
  icon: "plus" | "calendar" | "package" | "refund";
  tone: "green" | "blue" | "yellow" | "red"; // สีพื้นกล่องไอคอน
};

// ประเภทจาก backend → หน้าตาที่แสดง
const TYPE_INFO: Record<
  WalletTransaction["type"],
  Pick<Transaction, "group" | "title" | "icon" | "tone">
> = {
  TOPUP: { group: "topup", title: "เติมเงิน", icon: "plus", tone: "green" },
  BOOKING: { group: "booking", title: "ค่าจอง", icon: "calendar", tone: "blue" },
  REFUND: { group: "booking", title: "คืนเงินการจอง", icon: "refund", tone: "green" },
  LOAN_FEE: { group: "loan", title: "ค่ายืมอุปกรณ์", icon: "package", tone: "yellow" },
  DEPOSIT_HOLD: { group: "loan", title: "กันเงินมัดจำ", icon: "package", tone: "yellow" },
  DEPOSIT_REFUND: { group: "loan", title: "คืนเงินมัดจำ", icon: "refund", tone: "green" },
};

// ที่มาของรายการ → คำอธิบายใต้ชื่อ + ตัวนำหน้าเลขอ้างอิง (ตามแบบ: TH- / BK- / BR-)
const REF_INFO: Record<WalletTransaction["refType"], { subtitle: string; prefix: string }> = {
  TOPUP: { subtitle: "ประวัติการเติมเงิน", prefix: "TH" },
  BOOKING: { subtitle: "ประวัติการจองบริการ", prefix: "BK" },
  LOAN: { subtitle: "บันทึกยืม/คืน", prefix: "BR" },
};

// ระบุเขตเวลาไทยเสมอ ไม่ว่า server จะตั้งเวลาเครื่องไว้แบบไหน
const TZ = "Asia/Bangkok";
const timeFormat = new Intl.DateTimeFormat("th-TH", {
  timeZone: TZ,
  day: "numeric",
  month: "short",
  hour: "2-digit",
  minute: "2-digit",
});
const monthFormat = new Intl.DateTimeFormat("th-TH", { timeZone: TZ, month: "long", year: "numeric" });

export function toTransaction(tx: WalletTransaction): Transaction {
  const info = TYPE_INFO[tx.type];
  const ref = REF_INFO[tx.refType];
  const date = new Date(tx.createdAt);

  return {
    id: String(tx.txnId),
    ...info, // group, title, icon, tone
    title: tx.note ?? info.title, // backend เขียนหมายเหตุเป็นข้อความเต็มมาให้ (เช่น "เติมเงิน 1000 บาท") ถ้าไม่มีใช้ชื่อตามประเภท
    subtitle: ref.subtitle,
    ref: `${ref.prefix}-${String(tx.refId).padStart(6, "0")}`, // 1 → "TH-000001"
    time: timeFormat.format(date), // "16 พ.ย. 15:48"
    month: monthFormat.format(date), // "พฤศจิกายน 2569"
    amount: Number(tx.amount),
  };
}
