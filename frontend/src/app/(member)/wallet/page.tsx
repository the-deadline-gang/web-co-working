// หน้า U11 · Wallet
// ยอดเงินมาจาก wallet-service จริง · ประวัติรายการยังใช้ mock จาก features/wallet/mock.ts
import Link from "next/link";
import { Plus } from "lucide-react";
import { TRANSACTIONS } from "@/features/wallet/mock";
import { getMyWallet } from "@/lib/api/wallet";
import { TransactionList } from "@/features/wallet/TransactionList";

export default async function WalletPage() {
  const wallet = await getMyWallet();

  return (
    <div className="space-y-8 ">
      <h1 className="text-5xl font-bold">Wallet</h1>

      <div className="relative overflow-hidden rounded-2xl border-2 border-[#451A03] bg-[#1E44E6] px-8 py-9 text-white shadow-[6px_6px_0_#451A03]">
        <div className="absolute -bottom-14 right-32 h-36 w-32 rotate-[18deg] rounded-2xl border-2 border-[#451A03] bg-[#F08A12]" />
        <div className="absolute -right-8 -top-8 h-60 w-60 rounded-full border-2 border-[#451A03] bg-[#F9B51F]" />

        <div className="relative z-10">
          <p className="text-sm font-bold">ยอดคงเหลือ</p>

          <p className="mt-2 flex items-baseline gap-3">
            <span className="font-mono text-7xl font-bold leading-none">
              {(wallet?.balance ?? 0).toLocaleString()}
            </span>
            <span className="text-2xl font-bold">Token</span>
          </p>

          <p className="mt-4 text-sm">
            เลขที่ Wallet <span className="font-mono">{wallet?.displayId ?? "-"}</span>
          </p>
        </div>

        <Link
          href="/wallet/topup"
          className="absolute right-8 top-14 z-10 flex items-center gap-2 rounded-xl border-2 border-[#451A03] bg-white px-6 py-3 text-lg font-bold text-[#451A03] shadow-[4px_4px_0_#451A03]"
        >
          <Plus className="h-5 w-5" />
          เติมเงิน
        </Link>
      </div>

      {/* ปุ่มกรอง + รายการ (client component เพราะต้องจำว่ากดปุ่มไหน) */}
      <TransactionList transactions={TRANSACTIONS} />
    </div>
  );
}
