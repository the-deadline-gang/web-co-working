import Link from "next/link";
import { redirect } from "next/navigation";
import { getMe } from "@/lib/api/me";
import { ProfileForm } from "@/features/profile/ProfileForm";
import Image from "next/image";
import {
  CalendarDays,
  Wallet,
  QrCode,
  Package,
  Bell,
  ChevronRight,
  TriangleAlert,
} from "lucide-react";

const QUICK_LINKS = [
  { label: "การจองของฉัน", href: "/my-bookings", icon: CalendarDays },
  { label: "Wallet และประวัติการเติมเงิน", href: "/wallet", icon: Wallet },
  { label: "QR ของฉัน", href: "/qr", icon: QrCode },
  { label: "อุปกรณ์ที่ยืม", href: "/loans", icon: Package },
  { label: "การแจ้งเตือน", href: "/notifications", icon: Bell },
];

export default async function ProfilePage() {
  const user = await getMe();
  if (!user) redirect("/auth");

  const walletNo = "W-000123";

  return (
    <div className="space-y-8">
      <h1 className="text-5xl font-bold ">โปรไฟล์</h1>

      <div className="grid grid-cols-[380px_1fr] items-start gap-6">
        <div className="space-y-6">
          <div className="relative overflow-hidden rounded-2xl border-2 border-[#451A03] bg-[#1E44E6] p-6 text-white shadow-[4px_4px_0_#451A03]">
            <div className="absolute -right-12 -top-16 h-44 w-44 rounded-full border-2 border-[#451A03] bg-[#F9B51F]" />

            <div className="relative z-10 flex items-center justify-between">
              <Image
                src="/brand/cosync-logo-white.svg"
                alt="CO-SYNC"
                width={125}
                height={25}
                priority
              />
              <span className="rounded-full border-2 border-[#451A03] bg-white px-3 py-0.5 text-xs font-bold text-[#451A03]">
                MEMBER
              </span>
            </div>

            <div className="relative z-10 mt-6 flex items-center gap-4">
              <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-xl border-2 border-[#451A03] bg-[#F9B51F]">
                <span className="text-3xl font-bold text-[#451A03]">
                  {user.firstName[0]}
                </span>
              </div>
              <div>
                <p className="text-2xl font-bold">
                  {user.firstName} {user.lastName}
                </p>
                <p className="text-sm">{user.email}</p>
              </div>
            </div>

            {/* เส้นประ */}
            <div className="mt-6 border-t-2 border-dashed border-white/50" />

            <div className="mt-4 flex justify-between">
              <div>
                <p className="text-xs">User ID</p>
                <p className="font-mono font-bold">{user.displayId}</p>
              </div>
              <div className="text-right">
                <p className="text-xs">Wallet</p>
                <p className="font-mono font-bold">{walletNo}</p>
              </div>
            </div>
          </div>

          <nav className="divide-y-2 divide-dashed divide-[#D9C49A] overflow-hidden rounded-2xl border-2 border-[#451A03] bg-white shadow-[4px_4px_0_#451A03]">
            {QUICK_LINKS.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                className="flex items-center gap-3 px-5 py-4 font-semibold text-[#451A03] hover:bg-[#FFFBEB]"
              >
                <link.icon className="h-5 w-5" />
                {link.label}
                <ChevronRight className="ml-auto h-4 w-4" />
              </Link>
            ))}
          </nav>
        </div>

        <div className="space-y-6">
          {/* ข้อมูลส่วนตัว (ฟอร์ม · ส่งข้อมูลจริงจาก /me เข้าไปเป็นค่าเริ่มต้น) */}
          <ProfileForm user={user} />

          {/* เปลี่ยนรหัสผ่าน — ยังไม่มี API (ในแบบติดป้าย "ไม่มีใน DFD") */}
          <section className="rounded-2xl border-2 border-dashed border-[#D9C49A] p-6">
            <div className="flex items-center gap-3">
              <h2 className="text-xl font-bold text-[#451A03]">
                เปลี่ยนรหัสผ่าน
              </h2>
            </div>

            <div className="mt-4 grid grid-cols-2 gap-4">
              <label className="block">
                <span className="mb-2 block text-sm font-bold text-[#451A03]">
                  รหัสผ่านปัจจุบัน
                </span>
                <input
                  type="password"
                  className="w-full rounded-xl border-2 border-[#D9C49A] bg-white px-4 py-3 text-[#451A03] outline-none focus:border-[#1E44E6]"
                />
              </label>
              <label className="block">
                <span className="mb-2 block text-sm font-bold text-[#451A03]">
                  รหัสผ่านใหม่
                </span>
                <input
                  type="password"
                  className="w-full rounded-xl border-2 border-[#D9C49A] bg-white px-4 py-3 text-[#451A03] outline-none focus:border-[#1E44E6]"
                />
              </label>
            </div>
          </section>

          {/* ข้อมูลออกใบกำกับภาษี — ยังไม่มีทั้ง API และฟิลด์ในแบบ */}
          <section className="rounded-2xl border-2 border-dashed border-[#D9C49A] p-6">
            <div className="flex items-center gap-3">
              <h2 className="text-xl font-bold text-[#451A03]">
                ข้อมูลออกใบกำกับภาษี
              </h2>
            </div>
            <p className="mt-3 text-sm text-[#451A03]/70">
              [ฟิลด์ข้อมูลออกใบกำกับภาษี]
            </p>
          </section>
        </div>
      </div>
    </div>
  );
}
