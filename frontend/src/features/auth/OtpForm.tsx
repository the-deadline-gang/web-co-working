"use client";
// ฟอร์มกรอก OTP 6 หลัก  →  POST /auth/verify-otp
import { useEffect, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useRouter } from "next/navigation";
const INK = "#451A03";

export function OtpForm() {
  const params = useSearchParams();
  const email = params.get("email") ?? "";
  const expiresAt = params.get("expiresAt"); // เวลาหมดอายุของรหัส (ได้มาตอนสมัคร)
  const [otp, setOtp] = useState("");
  const [focused, setFocused] = useState(false); // ช่องกรอกถูกเลือกอยู่ไหม (ใช้โชว์เคอร์เซอร์)
  const router = useRouter();

  const remaining = 6 - otp.length;
  const secondsLeft = useCountdown(expiresAt, 10 * 60); // รหัสหมดอายุ (ปกติ 10 นาที)
  const resendIn = useCountdown(null, 60); // รอ 1 นาทีก่อนกดส่งรหัสใหม่ได้
  const expired = secondsLeft === 0;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    const res = await fetch("/api/auth/verify-otp",{
      method: "POST",
      headers: {"Content-Type" : "application/json"},
      body: JSON.stringify({email, otp}),
    });

    console.log(res.status);

    if(res.status === 204){
      router.push("/auth");


    }
  }

  return (
    <form onSubmit={handleSubmit} className="w-full max-w-lg space-y-6">
      <div className="flex items-center gap-3 rounded-2xl border-2 border-[#451A03] bg-white px-5 py-4 text-sm font-bold text-[#451A03]">
        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border-2 border-[#451A03] bg-[#1E44E6] text-white">
          <CheckIcon />
        </span>
        <span>กรอกข้อมูล</span>
        <span className="h-1 flex-1 rounded-full bg-[#1E44E6]" />

        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border-2 border-[#451A03] bg-[#F9B51F]">
          2
        </span>
        <span>ยืนยัน OTP</span>
        <span className="h-1 flex-1 rounded-full bg-[#EBDDBE]" />

        <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border-2 border-[#D9C49A] bg-white font-semibold text-[#9A8466]">
          3
        </span>
        <span className="font-semibold text-[#9A8466]">เริ่มใช้งาน</span>
      </div>

      {/* ===== ไอคอน + หัวข้อ + ข้อความ ===== */}
      <div className="flex items-center gap-5">
        <div className="flex h-20 w-20 shrink-0 items-center justify-center rounded-2xl border-2 border-[#451A03] bg-[#F9B51F] shadow-[4px_4px_0_#451A03]">
          <PhoneIcon />
        </div>
        <div>
          <h1 className="text-4xl font-bold text-[#451A03]">กรอกรหัส OTP</h1>
          <p className="mt-2 text-sm text-[#6B5A4A]">
            ส่งรหัส 6 หลักทางอีเมลไปที่{" "}
            <span className="font-mono font-bold text-[#451A03]">{maskEmail(email)}</span> แล้ว ·{" "}
            <Link href="/auth" className="font-bold text-[#1E44E6] underline">
              แก้ไขอีเมล
            </Link>
          </p>
        </div>
      </div>

      
      <div>
        <p className="mb-3 text-sm font-bold text-[#451A03]">รหัส OTP</p>
        <div className="relative">
          <input
            value={otp}
            onChange={(e) => setOtp(e.target.value.replace(/\D/g, "").slice(0, 6))} // รับแค่ตัวเลข ไม่เกิน 6 ตัว
            onFocus={() => setFocused(true)}
            onBlur={() => setFocused(false)}
            inputMode="numeric"
            autoComplete="one-time-code"
            aria-label="รหัส OTP 6 หลัก"
            autoFocus
            className="absolute inset-0 z-10 h-full w-full cursor-text opacity-0"
          />
          <div className="grid grid-cols-6 gap-3">
            {[0, 1, 2, 3, 4, 5].map((i) => {
              const filled = otp[i] !== undefined;
              const current = focused && i === otp.length; // กล่องที่กำลังจะพิมพ์
              return (
                <div
                  key={i}
                  className={`flex h-20 items-center justify-center rounded-xl border-2 bg-white text-3xl font-bold text-[#451A03] ${
                    filled
                      ? "border-[#451A03] shadow-[4px_4px_0_#451A03]"
                      : current
                        ? "border-[#1E44E6] shadow-[0_4px_0_#1E44E6]"
                        : "border-[#EBDDBE]"
                  }`}
                >
                  {filled ? otp[i] : current && <span className="h-8 w-0.5 animate-pulse bg-[#1E44E6]" />}
                </div>
              );
            })}
          </div>
        </div>
        <p className="mt-3 h-5 text-sm text-[#6B5A4A]">
          {remaining > 0 ? `กรอกอีก ${remaining} หลัก` : "กรอกครบแล้ว"}
        </p>
      </div>

      <div className="flex items-center justify-between rounded-2xl border-2 border-[#FDEEC4] bg-white px-5 py-4">
        <p className="flex items-center gap-2 text-sm font-semibold text-[#D7263D]">
          <TimerIcon />
          {expired ? (
            "รหัสหมดอายุแล้ว"
          ) : (
            <>
              รหัสหมดอายุใน
              <span className="font-mono font-bold">{formatTime(secondsLeft)}</span>
            </>
          )}
        </p>
        <button
          type="button"
          disabled
          className="rounded-xl border-2 border-[#D9C49A] bg-[#F5EBD8] px-4 py-2 text-sm font-bold text-[#9A8466] disabled:cursor-not-allowed"
        >
          ส่งรหัสอีกครั้ง{resendIn ? ` (${formatTime(resendIn)})` : ""}
        </button>
      </div>

      <button
        type="submit"
        disabled={otp.length !== 6 || expired}
        className="w-full rounded-2xl border-2 border-[#451A03] bg-[#1E44E6] px-4 py-4 text-lg font-bold text-white shadow-[4px_4px_0_#451A03] disabled:cursor-not-allowed disabled:border-[#D9C49A] disabled:bg-[#F5EBD8] disabled:text-[#9A8466] disabled:shadow-none"
      >
        ยืนยันรหัส
      </button>

      <div className="flex items-start gap-3 rounded-2xl bg-[#FEF3C7] px-5 py-4 text-sm text-[#451A03]">
        <InfoIcon />
        <p>
          ยืนยันแล้ว ระบบเปิด Wallet ให้ (ยอดเริ่มต้น 0 Token) และสร้าง QR ส่วนตัวให้อัตโนมัติ ·
          ไม่ได้รับรหัส? ตรวจโฟลเดอร์จดหมายขยะ แล้วรอกดส่งอีกครั้ง
        </p>
      </div>
    </form>
  );
}


function PhoneIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-9 w-9" fill="none" stroke={INK} strokeWidth={2} strokeLinecap="round" aria-hidden="true">
      <rect x="6" y="2.5" width="12" height="19" rx="2.5" />
      <circle cx="9.5" cy="8" r="0.7" fill={INK} />
      <circle cx="12" cy="8" r="0.7" fill={INK} />
      <circle cx="14.5" cy="8" r="0.7" fill={INK} />
      <line x1="10" y1="17.5" x2="14" y2="17.5" />
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={3} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M5 12.5l4.5 4.5L19 7.5" />
    </svg>
  );
}

function TimerIcon() {
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill="none" stroke="currentColor" strokeWidth={2.2} strokeLinecap="round" aria-hidden="true">
      <circle cx="12" cy="13" r="8" />
      <path d="M12 9v4l2.5 2M9.5 2.5h5" />
    </svg>
  );
}

function InfoIcon() {
  return (
    <svg viewBox="0 0 24 24" className="mt-0.5 h-5 w-5 shrink-0" fill="none" stroke="#1E44E6" strokeWidth={2} strokeLinecap="round" aria-hidden="true">
      <circle cx="12" cy="12" r="9.5" />
      <path d="M12 11v6M12 7.5v.01" />
    </svg>
  );
}


/** "kamon@mail.com" → "ka***@mail.com" (ซ่อนกลางอีเมลแบบเดียวกับเบอร์ 081-xxx-5678 ในดีไซน์) */
function maskEmail(email: string) {
  const [name, domain] = email.split("@");
  if (!domain) return email;
  return `${name.slice(0, 2)}***@${domain}`;
}

/**
 * นับถอยหลัง คืนค่าเป็นจำนวนวินาทีที่เหลือ
 * - ถ้ามี isoTime → นับถึงเวลานั้น
 * - ถ้าไม่มี → นับ fallbackSeconds วินาทีจากตอนเปิดหน้า
 * คืน null ในรอบแรก เพราะ Next.js สร้างหน้าที่ server ก่อน ถ้าคำนวณเวลาตั้งแต่ตอนนั้น
 * server กับเบราว์เซอร์จะได้คนละค่า → React เตือน hydration mismatch
 */
function useCountdown(isoTime: string | null, fallbackSeconds: number) {
  const [deadline, setDeadline] = useState<number | null>(null);
  const [now, setNow] = useState<number | null>(null);

  // useEffect: ทำงานหลังหน้าแสดงผลในเบราว์เซอร์ · ตั้งนาฬิกาให้อัปเดต now ทุก 1 วินาที
  useEffect(() => {
    setDeadline(isoTime ? new Date(isoTime).getTime() : Date.now() + fallbackSeconds * 1000);
    setNow(Date.now());
    const id = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(id); // ออกจากหน้า → หยุดนาฬิกา
  }, [isoTime, fallbackSeconds]);

  if (deadline === null || now === null) return null; // ยังไม่เริ่มนับ
  return Math.max(0, Math.floor((deadline - now) / 1000));
}

/** 272 → "04:32" · null → "--:--" */
function formatTime(totalSeconds: number | null) {
  if (totalSeconds === null) return "--:--";
  const m = Math.floor(totalSeconds / 60);
  const s = totalSeconds % 60;
  return `${String(m).padStart(2, "0")}:${String(s).padStart(2, "0")}`;
}
