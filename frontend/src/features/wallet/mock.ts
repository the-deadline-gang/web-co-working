// ข้อมูลตัวอย่างของหน้า Wallet (U11)
// TODO: ลบไฟล์นี้ทิ้งเมื่อ backend มี wallet-service แล้วเปลี่ยนไปดึงจาก API
// ประเภทของรายการ → ใช้กับปุ่มกรอง (ทั้งหมด / เติมเงิน / ค่าจอง / ค่ายืม)
export type TxType = "topup" | "booking" | "loan";

export type Transaction = {
  id: string;
  type: TxType;
  title: string; // "เติมเงิน · สำเร็จ"
  subtitle: string; // "ประวัติการเติมเงิน"
  ref: string; // "TH-000451 · PAY-…0087"
  time: string; // "3 ต.ค. 09:31"
  amount: number; // + = เงินเข้า, - = เงินออก, 0 = ไม่สำเร็จ
  // ชื่อไอคอน (เป็นข้อความ เพราะ server ส่ง component ให้ client ไม่ได้) → แปลงเป็นไอคอนจริงใน TransactionList
  icon: "plus" | "calendar" | "package" | "refund" | "alert" | "x";
  tone: "green" | "blue" | "yellow" | "red"; // สีพื้นกล่องไอคอน
  warning?: string; // ป้าย ⚠ ใต้จำนวนเงิน (ไม่มีก็ได้)
};

export const WALLET = {
  balance: 1010,
  walletNo: "W-000123",
  updatedAt: "3 ต.ค. 2569 · 09:31",
};

export const TRANSACTIONS: Transaction[] = [
  { id: "1", type: "topup", title: "เติมเงิน · สำเร็จ", subtitle: "ประวัติการเติมเงิน", ref: "TH-000451 · PAY-…0087", time: "3 ต.ค. 09:31", amount: 200, icon: "plus", tone: "green" },
  { id: "2", type: "booking", title: "ค่าจอง · ห้อง", subtitle: "ประวัติการจองบริการ", ref: "BK-000192 · B-ROOM-02", time: "3 ต.ค. 09:20", amount: -240, icon: "calendar", tone: "blue", warning: "ไม่มีจำนวนเงินที่ตัด" },
  { id: "3", type: "loan", title: "ค่ายืม · Adapter HDMI", subtitle: "บันทึกยืม/คืน", ref: "BR-000771", time: "3 ต.ค. 09:15", amount: -20, icon: "package", tone: "yellow", warning: "ไม่มีจำนวน Token" },
  { id: "4", type: "loan", title: "คืนค่ายืม 50% · เมาส์", subtitle: "บันทึกยืม/คืน", ref: "BR-000736", time: "1 ต.ค. 18:02", amount: 10, icon: "refund", tone: "green", warning: "ไม่มีจำนวน Token" },
  { id: "5", type: "loan", title: "ค่าปรับอุปกรณ์เสียหาย", subtitle: "บันทึกยืม/คืน", ref: "BR-000702", time: "28 ก.ย. 17:40", amount: -200, icon: "alert", tone: "red", warning: "ไม่มีจำนวน Token" },
  { id: "6", type: "topup", title: "เติมเงิน · ไม่สำเร็จ", subtitle: "ประวัติการเติมเงิน", ref: "TH-000449", time: "27 ก.ย. 12:05", amount: 0, icon: "x", tone: "red" },
];
