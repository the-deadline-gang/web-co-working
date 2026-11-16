// หน้า U3 · เติมเงินเข้า Wallet
// server: ดึงข้อมูลผู้ใช้ + ตั้งค่าการเติมเงิน แล้วส่งให้ฟอร์ม (client) ไปแสดง
import Link from "next/link";
import { redirect } from "next/navigation";
import { getMe } from "@/lib/api/me";
import { getTopupConfig } from "@/lib/api/wallet";
import { TopupForm } from "@/features/wallet/TopupForm";

export default async function TopupPage() {
  const user = await getMe();
  if (!user) redirect("/auth");

  const config = await getTopupConfig();
  if (!config) return <p>โหลดข้อมูลการเติมเงินไม่ได้ (wallet-service รันอยู่หรือเปล่า?)</p>;

  return (
    <div className="space-y-8">
      <div className="space-y-2 text-[#451A03]">
        <p className="text-sm">
          <Link href="/wallet" className="underline hover:text-[#1E44E6]">
            Wallet
          </Link>{" "}
          / เติมเงิน
        </p>
        <h1 className="text-5xl font-bold">เติมเงินเข้า Wallet</h1>
      </div>


      <TopupForm user={user} config={config} />
    </div>
  );
}
