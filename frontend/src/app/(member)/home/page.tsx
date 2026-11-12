import Image from "next/image";
import Link from "next/link";
import { ArrowRight, UserPlus, Volume2 } from "lucide-react";

// ===== ข้อมูลตัวอย่าง (mock) =====
// TODO: เปลี่ยนเป็นข้อมูลจริงเมื่อมี API
//   user          → GET /me (มีแล้ว)
//   upcoming      → booking-service (ยังไม่มี)
//   notifications → ยังไม่มีทั้งใน API และ DFD
//   loans         → booking-service (ยังไม่มี)
const user = { firstName: "กมลชนก", lastName: "ศรีสุข", displayId: "U-000123" };

const upcoming = {
  resourceCode: "B-ROOM-02",
  bookedAt: { date: "3 ต.ค.", time: "09:20" },
  startAt: "4 ต.ค. 13:00",
};

const notifications = [
  {
    id: 1,
    type: "noise" as const,
    title: "พื้นที่ของคุณเสียงดังเกินไป",
    detail: "ID พื้นที่ B-ROOM-02 · ระดับเสียง 78 dB",
    note: "ส่งโดยพนักงาน",
  },
  {
    id: 2,
    type: "invite" as const,
    title: "คุณได้รับคำเชิญเข้าร่วมห้องประชุม",
    detail: "จาก ธนพล ใจดี · 4 ต.ค. 13:00",
  },
];

const loans = [{ id: "EQ-L-014", name: "สาย Adapter HDMI", borrowedAt: "3 ต.ค. 09:15" }];

const expiringTokens = { amount: 300, expiresOn: "31 ต.ค." };

export default function HomePage() {
  return (
    <div className="space-y-8">
      {/* ===== แถว 1: หัวข้อ + ปุ่มลัด ===== */}
      <div className="flex items-center justify-between">
        <h1 className="text-5xl font-bold text-[#451A03]">สวัสดี คุณ{user.firstName}</h1>

        <div className="flex gap-3">
          <Link
            href="/booking"
            className="flex h-12 items-center justify-center whitespace-nowrap rounded-xl border-2 border-[#451A03] bg-[#1E44E6] px-6 font-bold text-white shadow-[4px_4px_0_#451A03]"
          >
            + จอง
          </Link>
          <Link
            href="/wallet/topup"
            className="flex h-12 items-center justify-center whitespace-nowrap rounded-xl border-2 border-[#451A03] bg-white px-6 font-bold text-[#451A03]"
          >
            เติมเงิน
          </Link>
          <Link
            href="/qr"
            className="flex h-12 items-center justify-center rounded-xl border-2 border-[#451A03] bg-white px-6 font-bold text-[#451A03]"
          >
            QR
          </Link>
        </div>
      </div>

      {/* ===== แถว 2: การ์ดเหลือง (ซ้าย กว้างกว่า) | การ์ดการจอง (ขวา) ===== */}
      <div className="grid grid-cols-[1.7fr_1fr] gap-6">
        {/* การ์ดเหลือง */}
        <div className="relative h-80 overflow-hidden rounded-2xl border-2 border-[#451A03] bg-[#FEF3C7]">
          <Image src="/illustrations/designed/u2-home-hero.svg" alt="" fill priority className="object-cover" />

          <div className="absolute inset-x-5 bottom-5 flex items-center justify-between rounded-xl border-2 border-[#451A03] bg-[#FFFBEB] px-6 py-4">
            <div>
              <p className="text-xl font-bold text-[#451A03]">พื้นที่ทำงานของ CO-SYNC</p>
              <p className="text-sm text-[#6B5A4A]">Day Pass · โต๊ะ · ห้อง</p>
            </div>
            <Link
              href="/booking"
              className="flex h-12 shrink-0 items-center whitespace-nowrap rounded-xl border-2 border-[#451A03] bg-[#1E44E6] px-6 font-bold text-white shadow-[4px_4px_0_#451A03]"
            >
              จองเลย
            </Link>
          </div>
        </div>

        {/* การ์ดการจอง (แบบตั๋ว): ส่วนบนชิดบน ส่วนล่างชิดล่าง */}
        <div className="relative flex h-80 flex-col justify-between rounded-2xl border-2 border-[#451A03] bg-white p-6 shadow-[4px_4px_0_#451A03]">
          <div>
            <p className="text-sm font-bold text-[#451A03]">การจองที่ใกล้ถึง</p>
            <p className="mt-3 font-mono text-3xl font-bold text-[#451A03]">{upcoming.resourceCode}</p>
            <p className="mt-1 text-sm text-[#6B5A4A]">
              จองเมื่อ {upcoming.bookedAt.date} <span className="font-mono">{upcoming.bookedAt.time}</span>
            </p>

            {/* เส้นประ + รอยเว้าซ้ายขวา */}
            <div className="relative mt-5">
              <div className="border-t-2 border-dashed border-[#D9C49A]" />
              <span className="absolute -left-9 top-1/2 h-6 w-6 -translate-y-1/2 rounded-full border-2 border-[#451A03] bg-[#FFFBEB]" />
              <span className="absolute -right-9 top-1/2 h-6 w-6 -translate-y-1/2 rounded-full border-2 border-[#451A03] bg-[#FFFBEB]" />
            </div>
          </div>

          <div className="flex items-end justify-between">
            <div>
              <p className="mb-2 text-sm text-[#451A03]">เริ่มใช้งาน</p>
              <p className="inline-block whitespace-nowrap rounded-xl border-2 border-[#451A03] bg-[#F9B51F] px-4 py-2 font-mono text-2xl font-bold text-[#451A03]">
                {upcoming.startAt}
              </p>
            </div>
            <Link href="/my-bookings" aria-label="ดูการจอง">
              <ArrowRight className="h-6 w-6 text-[#451A03]" />
            </Link>
          </div>
        </div>
      </div>

      {/* ===== แถว 3: แจ้งเตือน | อุปกรณ์ที่ยืมอยู่ ===== */}
      <div className="grid grid-cols-2 gap-6">
        {/* แจ้งเตือน */}
        <section className="space-y-4">
          <h2 className="text-xl font-bold text-[#451A03]">แจ้งเตือน</h2>

          {notifications.map((n) =>
            n.type === "noise" ? (
              // แจ้งเตือนสำคัญ (ส้ม) เช่น เสียงดังเกินไป
              <div
                key={n.id}
                className="flex gap-4 rounded-2xl border-2 border-[#9A2B12] bg-[#FDE8D2] p-5"
              >
                <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[#8A2512] text-white">
                  <Volume2 className="h-5 w-5" />
                </span>
                <div className="text-[#451A03]">
                  <p className="font-bold">{n.title}</p>
                  <p className="mt-1 text-sm">{n.detail}</p>
                  {n.note && <p className="text-sm">{n.note}</p>}
                </div>
              </div>
            ) : (
              // แจ้งเตือนทั่วไป (ขาว) เช่น ได้รับคำเชิญ
              <div
                key={n.id}
                className="flex gap-4 rounded-2xl border-2 border-[#F5E3B3] bg-white p-5"
              >
                <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[#DCE6FF] text-[#1E44E6]">
                  <UserPlus className="h-5 w-5" />
                </span>
                <div className="text-[#451A03]">
                  <p className="font-bold">{n.title}</p>
                  <p className="mt-1 text-sm">{n.detail}</p>
                  <Link href="/qr" className="mt-1 inline-block text-sm font-bold text-[#1E44E6] underline">
                    ดู QR เข้าใช้งาน
                  </Link>
                </div>
              </div>
            ),
          )}
        </section>

        {/* อุปกรณ์ที่ยืมอยู่ */}
        <section className="space-y-4">
          <div className="flex items-end justify-between">
            <h2 className="text-xl font-bold text-[#451A03]">อุปกรณ์ที่ยืมอยู่</h2>
            <Link href="/loans" className="text-sm font-bold text-[#1E44E6] underline">
              จัดการ
            </Link>
          </div>

          {loans.map((loan) => (
            <div
              key={loan.id}
              className="flex items-center justify-between rounded-2xl border-2 border-[#F5E3B3] bg-white p-5"
            >
              <div>
                <p className="font-bold text-[#451A03]">{loan.name}</p>
                <p className="mt-1 font-mono text-sm text-[#6B5A4A]">
                  {loan.id} · {loan.borrowedAt}
                </p>
              </div>
              <span className="rounded-lg border-2 border-[#9A2B12] bg-[#FDE8D2] px-3 py-1 text-xs font-bold text-[#9A2B12]">
                ยังไม่คืน
              </span>
            </div>
          ))}

          {/* Token ที่ใกล้หมดอายุ (กรอบประ) */}
          <div className="rounded-2xl border-2 border-dashed border-[#E8D49A] px-5 py-4 text-sm text-[#451A03]">
            <span className="font-mono font-bold">{expiringTokens.amount}</span> Token จะหมดอายุ{" "}
            {expiringTokens.expiresOn}
          </div>
        </section>
      </div>
    </div>
  );
}
