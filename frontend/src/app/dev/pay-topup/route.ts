// ⚠️ ใช้ตอน dev เท่านั้น: จำลองว่า "จ่ายเงินแล้ว" ให้คำขอเติมเงิน โดยไม่ต้องสแกน QR จริง
// เรียกสคริปต์ scripts/dev-pay-topup.sh (ยิง webhook ที่เซ็นด้วย secret ใน .env)
// ทำฝั่ง server เพราะ secret ห้ามส่งไปถึงเบราว์เซอร์
// อยู่นอก /api เพราะ /api/* ถูกส่งต่อไป backend ทั้งหมด
import { execFile } from "node:child_process";
import path from "node:path";
import { promisify } from "node:util";

const run = promisify(execFile);

export async function POST(req: Request) {
  // ขึ้นเว็บจริง (npm run build / start) → ไม่มี route นี้
  if (process.env.NODE_ENV !== "development") {
    return new Response(null, { status: 404 });
  }

  const { topupId, status } = await req.json();
  // รับแค่ตัวเลข และสถานะ 2 แบบ กันค่าแปลกๆ หลุดเข้าไปในสคริปต์
  if (!/^\d+$/.test(String(topupId)) || !["successful", "failed"].includes(status)) {
    return Response.json({ message: "ข้อมูลไม่ถูกต้อง" }, { status: 400 });
  }

  const script = path.join(process.cwd(), "scripts", "dev-pay-topup.sh");
  try {
    const { stdout } = await run(script, [String(topupId), status]);
    return Response.json({ message: stdout.trim() });
  } catch (e) {
    return Response.json({ message: String(e) }, { status: 500 });
  }
}
