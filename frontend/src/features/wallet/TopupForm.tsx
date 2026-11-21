"use client";
// ฟอร์มเติมเงิน (U3): ซ้าย = กรอกจำนวนเงิน · ขวา = การ์ดสรุป + ปุ่มสร้างคำขอชำระเงิน
import { useState } from "react";
import { useRouter } from "next/navigation";
import type { Me, Topup, TopupConfig } from "@/types/api";

export function TopupForm({ user, config }: { user: Me; config: TopupConfig }) {
  const router = useRouter();

  // จำนวนเงิน (เก็บเป็นข้อความ เพราะมาจากช่อง input) เริ่มที่ปุ่มลัดอันแรก
  const [amount, setAmount] = useState(String(config.quickAmounts[0] ?? config.minThb));
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const amountNumber = Number(amount) || 0;
  const tokens = amountNumber * config.rate; // ได้รับกี่ Token
  const tooLow = amountNumber < config.minThb;
  const tooHigh = amountNumber > config.maxThb;

  // กด "สร้างคำขอชำระเงิน" → backend สร้าง QR → ไปหน้ารอชำระเงิน /wallet/topup/{id}
  async function handleSubmit() {
    if (tooLow || tooHigh) return;
    setLoading(true);
    setError("");

    const res = await fetch("/api/topups", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ amountThb: amountNumber }),
    });
    const data = await res.json();

    if (res.status === 201) {
      const topup = data as Topup;
      router.push(`/wallet/topup/${topup.topupId}`);
    } else {
      setError(data.message);
      setLoading(false);
    }
  }

  return (
    <div className="grid grid-cols-[1fr_420px] items-start gap-8 text-[#451A03]">
      {/* ---------- ซ้าย: กรอกจำนวนเงิน ---------- */}
      <div className="space-y-4">
        <h2 className="text-xl font-bold">จำนวนเงินที่จะเติม (บาท)</h2>

        {/* ช่องกรอกใหญ่: กรอบอยู่ที่ label · input ข้างในไม่มีขอบ */}
        <label
          className={`flex items-center gap-4 rounded-2xl border-2 bg-white px-6 py-5 shadow-[4px_4px_0_#451A03] ${
            tooLow || tooHigh ? "border-[#D7263D]" : "border-[#451A03]"
          }`}
        >
          <span className="text-3xl font-bold text-[#451A03]/60">฿</span>
          <input
            inputMode="numeric"
            value={amount}
            onChange={(e) => setAmount(e.target.value.replace(/\D/g, ""))} // รับแต่ตัวเลข
            className="w-full bg-transparent font-mono text-4xl font-bold outline-none"
          />
        </label>

        {/* ปุ่มจำนวนลัด (มาจาก backend) */}
        <div className="grid grid-cols-5 gap-3">
          {config.quickAmounts.map((q) => (
            <button
              key={q}
              type="button"
              onClick={() => setAmount(String(q))}
              className={`rounded-xl border-2 py-3 font-mono font-bold ${
                amountNumber === q
                  ? "border-[#451A03] bg-[#F9B51F] shadow-[3px_3px_0_#451A03]"
                  : "border-[#D9C49A] bg-white"
              }`}
            >
              {q.toLocaleString()}
            </button>
          ))}
        </div>

        {/* ข้อความช่วย: กรอกเกินช่วงที่เติมได้ → สีแดง */}
        <p className={`text-sm ${tooLow || tooHigh ? "font-bold text-[#D7263D]" : ""}`}>
          เติมได้ {config.minThb.toLocaleString()} - {config.maxThb.toLocaleString()} บาท · 1 บาท ={" "}
          {config.rate} Token
        </p>
      </div>

      {/* ---------- ขวา: การ์ดสรุป (ticket) ---------- */}
      <div className="rounded-2xl border-2 border-[#451A03] bg-white shadow-[6px_6px_0_#451A03]">
        <div className="space-y-3 p-6">
          <p className="text-xs font-bold">ผู้เติมเงิน · ดึงอัตโนมัติ</p>
          <div className="flex justify-between">
            <span>ชื่อ - นามสกุล</span>
            <span className="font-bold">
              {user.firstName} {user.lastName}
            </span>
          </div>
          <div className="flex justify-between">
            <span>User ID</span>
            <span className="font-mono font-bold">{user.displayId}</span>
          </div>
        </div>

        {/* เส้นประ + รอยบากซ้ายขวา (วงกลมสีพื้นหลังหน้า ขอบเข้ม วางคร่อมขอบการ์ด แบบ ticket หน้า Home) */}
        <div className="relative">
          <div className="mx-6 border-t-2 border-dashed border-[#D9C49A]" />
          <span className="absolute -left-[13px] top-1/2 h-6 w-6 -translate-y-1/2 rounded-full border-2 border-[#451A03] bg-[#FFFBEB]" />
          <span className="absolute -right-[13px] top-1/2 h-6 w-6 -translate-y-1/2 rounded-full border-2 border-[#451A03] bg-[#FFFBEB]" />
        </div>

        <div className="space-y-3 p-6">
          <div className="flex justify-between">
            <span>จำนวนที่เติม</span>
            <span>
              <span className="font-mono font-bold">{amountNumber.toLocaleString()}</span> บาท
            </span>
          </div>
          <div className="flex justify-between">
            <span>ได้รับ</span>
            <span>
              <span className="font-mono font-bold">{tokens.toLocaleString()}</span> Token
            </span>
          </div>

          {/* กล่องเหลือง ยอดที่ต้องชำระ */}
          <div className="rounded-xl border-2 border-[#451A03] bg-[#F9B51F] px-4 py-3">
            <p className="text-xs font-bold">ยอดที่ต้องชำระ</p>
            <p className="font-mono text-4xl font-bold">
              ฿{amountNumber.toLocaleString(undefined, { minimumFractionDigits: 2 })}
            </p>
          </div>

          {/* error จาก backend */}
          {error && (
            <p className="rounded-xl border-2 border-[#D7263D] bg-[#FDDDE0] px-4 py-3 text-sm text-[#D7263D]">
              {error}
            </p>
          )}

          <button
            type="button"
            onClick={handleSubmit}
            disabled={loading || tooLow || tooHigh}
            className="w-full rounded-xl border-2 border-[#451A03] bg-[#1E44E6] py-4 text-lg font-bold text-white shadow-[4px_4px_0_#451A03] disabled:opacity-50"
          >
            {loading ? "กำลังสร้าง..." : "สร้างคำขอชำระเงิน"}
          </button>

          <p className="text-center text-xs text-[#451A03]/70">
            ชำระผ่าน QR PromptPay · QR หมดอายุใน {config.qrExpireMinutes} นาที
          </p>
        </div>
      </div>
    </div>
  );
}
