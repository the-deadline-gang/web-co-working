"use client";
import Image from "next/image";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { House, CirclePlus, CalendarDays, QrCode, Wallet, Package, User } from "lucide-react";


const MENU = [
  { label: "หน้าหลัก", href: "/home", icon: House }, 
  { label: "จองบริการ", href: "/booking", icon: CirclePlus }, 
  { label: "การจองของฉัน", href: "/my-bookings", icon: CalendarDays },
  { label: "QR ของฉัน", href: "/qr", icon: QrCode }, 
  { label: "Wallet", href: "/wallet", icon: Wallet }, 
  { label: "ยืม-คืนอุปกรณ์", href: "/loans", icon: Package }, 
  { label: "โปรไฟล์", href: "/profile", icon: User },
];

export function Sidebar() {
  const pathname = usePathname(); // URL ปัจจุบัน เช่น "/home" → ใช้เช็คว่าเมนูไหนกำลังเปิดอยู่

  const user = {
    firstName: "กมลชนก",
    lastName: "ศรีสุข",
    displayId: "U-000123",
  };
  const wallet = { balance: 1250, walletNo: "W-000123" };

  return (
    
    <aside className="sticky top-0 flex h-screen flex-col gap-6 border-r-2 border-[#451A03] bg-[#F08A12] p-4">
      <div className="px-2 pt-2">
        <Image src="/brand/cosync-logo.svg" alt="CO-SYNC" width={150} height={30} priority />
      </div>

      <div className="rounded-xl border-2 border-[#451A03] bg-[#1E44E6] p-4 text-white shadow-[4px_4px_0_#451A03]">
        <p className="text-sm font-bold">ยอด Token</p>
        <p className="font-mono text-3xl font-bold">{wallet.balance.toLocaleString()}</p>
        <p className="text-xs">
          {wallet.walletNo} ·{" "}
          <Link href="/wallet" className="hover:underline">
            ดู Wallet
          </Link>
        </p>
      </div>

      <nav className="flex flex-col gap-1">
        {MENU.map((item) => {
          const active = pathname === item.href || pathname.startsWith(item.href + "/");
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`flex items-center gap-3 rounded-xl border-2 px-4 py-3 text-[#451A03] ${
                active
                  ? "border-[#451A03] bg-white font-bold shadow-[3px_3px_0_#451A03]"
                  : "border-transparent hover:bg-[#F5A54A]"
              }`}
            >
            <item.icon className="h-5 w-5" />  
            {item.label}          
            </Link>
          );
        })}
      </nav>

      <div className="mt-auto flex items-center gap-3 rounded-xl bg-[#F7B267] p-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border-2 border-[#451A03] bg-[#1E44E6]">
          <span className="text-lg font-bold text-white">{user.firstName[0]}</span>
        </div>
        <div>
          <p className="text-sm font-bold text-[#451A03]">
            {user.firstName} {user.lastName}
          </p>
          <p className="font-mono text-xs text-[#451A03]">{user.displayId}</p>
        </div>
      </div>
    </aside>
  );
}
