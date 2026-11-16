"use client";
// หน้ารอชำระเงิน (แสดง QR) → พอจ่ายแล้วเปลี่ยนเป็นหน้าผลการชำระเงิน (U4)
// ถาม backend ซ้ำทุก 3 วินาทีว่าจ่ายหรือยัง (polling) จนกว่าสถานะจะไม่ใช่ PENDING
import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Check, X, LoaderCircle } from "lucide-react";
import type { Topup } from "@/types/api";

export function TopupStatus({ topupId }: { topupId: string }) {
  const router = useRouter();
  const [topup, setTopup] = useState<Topup | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let timer: ReturnType<typeof setTimeout>;

    async function check() {
      const res = await fetch(`/api/topups/${topupId}`);
      const data = await res.json();
      if (!res.ok) {
        setError(data.message);
        return;
      }
      setTopup(data);

      if (data.status === "PENDING") {
        timer = setTimeout(check, 3000); // ยังไม่จ่าย → อีก 3 วิ ถามใหม่
      } else if (data.status === "PAID") {
        router.refresh(); // ให้ยอด Token ใน Sidebar อัปเดต
      }
    }

    check();
    return () => clearTimeout(timer); // ออกจากหน้านี้ → หยุดถาม
  }, [topupId, router]);

  // DEV: ขอให้ server จำลองว่าจ่ายแล้ว → อีกไม่เกิน 3 วิ การถามสถานะรอบถัดไปจะเห็นผลเอง
  async function simulatePay(status: "successful" | "failed") {
    const res = await fetch("/dev/pay-topup", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ topupId, status }),
    });
    if (!res.ok) {
      const data = await res.json();
      setError(data.message);
    }
  }

  if (error) {
    return (
      <div className="mx-auto max-w-xl rounded-xl border-2 border-[#D7263D] bg-[#FDDDE0] px-4 py-3 text-[#D7263D]">
        {error}
      </div>
    );
  }
  if (!topup) return <p className="text-center text-[#451A03]">กำลังโหลด...</p>;

  // ---------- ยังไม่จ่าย: แสดง QR (ไม่มีในแบบ ทำให้เข้าชุดกับการ์ด U4) ----------
  if (topup.status === "PENDING") {
    return (
      <div className="mx-auto max-w-md space-y-6 text-[#451A03]">
        <div className="overflow-hidden rounded-2xl border-2 border-[#451A03] bg-white text-center shadow-[6px_6px_0_#451A03]">
          <div className="border-b-2 border-[#451A03] bg-[#F9B51F] px-6 py-5">
            <h1 className="text-2xl font-bold">สแกนเพื่อชำระเงิน</h1>
            <p className="font-mono text-sm">{topup.displayId}</p>
          </div>

          <div className="space-y-4 p-6">
            {/* ใช้ <img> ธรรมดา เพราะ QR เป็นรูปแบบ data:image/png;base64 */}
            {topup.qrImage && (
              // eslint-disable-next-line @next/next/no-img-element
              <img
                src={topup.qrImage}
                alt="QR PromptPay"
                className="mx-auto h-64 w-64 rounded-xl border-2 border-[#D9C49A] p-2"
              />
            )}

            <p className="font-mono text-4xl font-bold">฿{topup.amountThb.toLocaleString()}</p>
            <p className="text-sm">
              QR หมดอายุเวลา{" "}
              <span className="font-mono font-bold">
                {new Date(topup.expiresAt).toLocaleTimeString("th-TH", { hour: "2-digit", minute: "2-digit" })}
              </span>
            </p>

            <p className="flex items-center justify-center gap-2 rounded-xl bg-[#FDEEC4] py-2 text-sm font-bold">
              <LoaderCircle className="h-4 w-4 animate-spin" />
              กำลังรอการชำระเงิน...
            </p>
          </div>
        </div>

        <Link
          href="/wallet"
          className="block rounded-xl border-2 border-[#451A03] bg-white py-3 text-center font-bold"
        >
          ยกเลิก
        </Link>

        {/* ⚠️ ปุ่มทดสอบ: โผล่เฉพาะตอน npm run dev · ขึ้นเว็บจริงจะหายไปเอง */}
        {process.env.NODE_ENV === "development" && (
          <div className="space-y-2 rounded-xl border-2 border-dashed border-[#F08A12] bg-[#FDEEC4] p-4">
            <p className="text-center text-xs font-bold">DEV · จำลองผลการชำระเงิน (ไม่ต้องสแกนจริง)</p>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => simulatePay("successful")}
                className="rounded-lg border-2 border-[#15803D] bg-white py-2 text-sm font-bold text-[#15803D]"
              >
                จ่ายสำเร็จ
              </button>
              <button
                type="button"
                onClick={() => simulatePay("failed")}
                className="rounded-lg border-2 border-[#B0102A] bg-white py-2 text-sm font-bold text-[#B0102A]"
              >
                จ่ายไม่สำเร็จ
              </button>
            </div>
          </div>
        )}
      </div>
    );
  }

  // ---------- U4 ผลการชำระเงิน ----------
  const success = topup.status === "PAID";

  return (
    <div className="mx-auto max-w-xl space-y-6 text-[#451A03]">
      <div className="overflow-hidden rounded-2xl border-2 border-[#451A03] bg-white shadow-[6px_6px_0_#451A03]">
        {/* หัวการ์ด: สำเร็จ = เหลือง · ไม่สำเร็จ = แดงอ่อน */}
        <div
          className={`border-b-2 border-[#451A03] px-6 py-8 text-center ${
            success ? "bg-[#F9B51F]" : "bg-[#FDDDE0]"
          }`}
        >
          <div
            className={`mx-auto flex h-16 w-16 items-center justify-center rounded-xl border-2 border-[#451A03] text-white ${
              success ? "bg-[#15803D]" : "bg-[#B0102A]"
            }`}
          >
            {success ? <Check className="h-8 w-8" strokeWidth={3} /> : <X className="h-8 w-8" strokeWidth={3} />}
          </div>
          <h1 className="mt-4 text-4xl font-bold">{success ? "ชำระเงินสำเร็จ" : "ชำระเงินไม่สำเร็จ"}</h1>
          <p className="mt-2">
            {success
              ? `เติม ${topup.tokenAmount.toLocaleString()} Token เข้า Wallet แล้ว`
              : topup.status === "EXPIRED"
                ? "QR หมดอายุแล้ว ยังไม่ได้เติม Token เข้า Wallet ลองชำระใหม่อีกครั้ง"
                : "ยังไม่ได้เติม Token เข้า Wallet ลองชำระใหม่อีกครั้ง"}
          </p>
        </div>

        {/* รายละเอียด */}
        <div className="space-y-3 px-7 py-6">
          <div className="flex items-center justify-between">
            <span>ID การชำระเงิน</span>
            <span className="font-mono font-bold">{topup.displayId}</span>
          </div>
          <div className="flex items-center justify-between">
            <span>สถานะการชำระเงิน</span>
            <span
              className={`rounded-md border-2 px-2 py-0.5 text-sm font-bold ${
                success
                  ? "border-[#15803D] bg-[#DCFCE7] text-[#15803D]"
                  : "border-[#B0102A] bg-[#FDDDE0] text-[#B0102A]"
              }`}
            >
              {success ? "สำเร็จ" : "ไม่สำเร็จ"}
            </span>
          </div>

          {success && (
            <>
              <div className="flex items-center justify-between">
                <span>จำนวนเงิน</span>
                <span className="font-mono font-bold text-[#15803D]">
                  +{topup.tokenAmount.toLocaleString()} Token
                </span>
              </div>
              {topup.balanceAfter !== null && (
                <div className="flex items-center justify-between">
                  <span>ยอดคงเหลือ</span>
                  <span className="font-mono font-bold">{topup.balanceAfter.toLocaleString()} Token</span>
                </div>
              )}
              {topup.paidAt && (
                <div className="flex items-center justify-between">
                  <span>Update ล่าสุด</span>
                  <span>
                    {new Date(topup.paidAt).toLocaleString("th-TH", { dateStyle: "medium", timeStyle: "short" })}
                  </span>
                </div>
              )}
            </>
          )}
        </div>
      </div>

      {/* ปุ่มล่าง: ซ้าย = ขาว · ขวา = น้ำเงิน เงาแข็ง */}
      <div className="grid grid-cols-2 gap-4">
        <Link
          href="/home"
          className="rounded-xl border-2 border-[#451A03] bg-white py-4 text-center font-bold"
        >
          กลับหน้าหลัก
        </Link>
        <Link
          href={success ? "/wallet" : "/wallet/topup"}
          className="rounded-xl border-2 border-[#451A03] bg-[#1E44E6] py-4 text-center font-bold text-white shadow-[4px_4px_0_#451A03]"
        >
          {success ? "ไปที่ Wallet" : "ลองใหม่"}
        </Link>
      </div>
    </div>
  );
}
