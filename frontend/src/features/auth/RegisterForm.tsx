"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";

export function RegisterForm() {
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [age, setAge] = useState(""); // ใส่ไว้รอเลย ถ้าไม่ตรงตอนยิง API ค่อยเปลี่ยนให้ตรง
  const [sex, setSex] = useState("");
  const [password, setPassWord] = useState("");
  const [acceptTerms, setAcceptTerms] = useState(false); // ติ๊ก = true, ไม่ติ๊ก = false
  const router = useRouter();

  const [form, setForm] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setForm("");
    setFieldErrors({});

    // ส่งข้อมูลไปที่ backend
    const res = await fetch("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        firstName,
        lastName,
        email,
        phone,
        password,
        acceptTerms,
      }),
    });

    // อ่านคำตอบจาก backend
    const data = await res.json();
    console.log(res.status, data);

    if (res.status === 201) {
      router.push(
        `/verify-otp?email=${encodeURIComponent(email)}&expiresAt=${encodeURIComponent(data.otpExpiresAt)}`,
      );
    } else if (data.code === "EMAIL_TAKEN") {
      setFieldErrors({
        email: "email นี้ถูกใช้งานแล้ว ลองเข้าสู่ระบบแทนหรือใช้ email อื่น",
      });
    } else if (data.code === "PHONE_TAKEN") {
      setFieldErrors({ phone: data.message });
    } else {
      setFieldErrors(data.details ?? {});
      setForm("กรอกข้อมูลไม่ครบ กรุณากรอกช่องที่มีกรอบสีแดง");
    }
  }

  return (
    <form onSubmit={handleSubmit} className="w-full max-w-lg space-y-4">
      {form && (
        <div className="rounded-xl border-2 border-[#D7263D] bg-[#FDDDE0] px-4 py-3 text-sm text-[#D7263D]">
          {form}
        </div>
      )}
      <div className="grid grid-cols-2 gap-4">
        <label className="block">
          <span className="mb-2 block text-sm font-semibold text-[#451A03]">
            ชื่อ
          </span>
          <input
            value={firstName}
            onChange={(e) => setFirstName(e.target.value)}
            className={`w-full rounded-xl border-2 bg-white px-4 py-3 text-[#451A03] outline-none focus:border-[#1E44E6] ${
            fieldErrors.firstName ? "border-[#D7263D]" : "border-[#D9C49A]"
          }`}
          />
          {fieldErrors.firstName && (
            <p className="mt-1 text-sm text-[#D7263D]">{fieldErrors.firstName}</p>
          )}
        </label>

        <label className="block">
          <span className="mb-2 block text-sm font-semibold text-[#451A03]">
            นามสกุล
          </span>
          <input
            value={lastName}
            onChange={(e) => setLastName(e.target.value)}
            className={`w-full rounded-xl border-2 bg-white px-4 py-3 text-[#451A03] outline-none focus:border-[#1E44E6] ${
            fieldErrors.lastName ? "border-[#D7263D]" : "border-[#D9C49A]"
          }`}
          />
          {fieldErrors.lastName && (
            <p className="mt-1 text-sm text-[#D7263D]">{fieldErrors.lastName}</p>
          )}
        </label>
      </div>

      <label className="block">
        <span className="mb-2 block text-sm font-semibold text-[#451A03]">
          mail
        </span>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className={`w-full rounded-xl border-2 bg-white px-4 py-3 text-[#451A03] outline-none focus:border-[#1E44E6] ${
            fieldErrors.email ? "border-[#D7263D]" : "border-[#D9C49A]"
          }`}
        />
          {fieldErrors.email && (
            <p className="mt-1 text-sm text-[#D7263D]">{fieldErrors.email}</p>
          )}
      </label>

      <div className="grid grid-cols-4 gap-4">
        <label className="block col-span-3">
          <span className="mb-2 block text-sm font-semibold text-[#451A03]">
            เบอร์โทร
          </span>
          <input
            inputMode="tel"
            placeholder="0812345678"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            className={`w-full rounded-xl border-2 bg-white px-4 py-3 text-[#451A03] outline-none focus:border-[#1E44E6] ${
            fieldErrors.phone ? "border-[#D7263D]" : "border-[#D9C49A]"
          }`}
          />
          {fieldErrors.phone && (
            <p className="mt-1 text-sm text-[#D7263D]">{fieldErrors.phone}</p>
          )}
        </label>

        <label className="block ">
          <span className="mb-2 block text-sm font-semibold text-[#451A03]">
            อายุ
          </span>
          <input
            placeholder="ปี"
            inputMode="numeric"
            value={age}
            onChange={(e) => setAge(e.target.value)}
            className="w-full rounded-xl border-2 border-[#D9C49A] bg-white px-4 py-3 text-[#451A03] outline-none focus:border-[#1E44E6]"
          />
        </label>
      </div>

      <div>
        <span className="mb-2 block text-sm font-semibold text-[#451A03]">
          เพศ
        </span>

        <div className="grid grid-cols-3 gap-3">
          <button
            type="button"
            onClick={() => setSex("male")}
            className={`w-full rounded-xl border-2 px-4 py-3 text-[#451A03] ${sex === "male" ? "border-[#451A03] bg-[#F9B51F] font-bold" : "border-[#D9C49A] bg-white"}`}
          >
            ชาย
          </button>

          <button
            type="button"
            onClick={() => setSex("female")}
            className={`w-full rounded-xl border-2 px-4 py-3 text-[#451A03] ${sex === "female" ? "border-[#451A03] bg-[#F9B51F] font-bold" : "border-[#D9C49A] bg-white"}`}
          >
            หญิง
          </button>

          <button
            type="button"
            onClick={() => setSex("unspecified")}
            className={`w-full rounded-xl border-2 px-4 py-3 text-[#451A03] ${sex === "unspecified" ? "border-[#451A03] bg-[#F9B51F] font-bold" : "border-[#D9C49A] bg-white"}`}
          >
            ไม่ระบุ
          </button>
        </div>
      </div>

      <label className="block">
        <span className="mb-2 block text-sm font-semibold text-[#451A03]">
          รหัสผ่าน
        </span>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassWord(e.target.value)}
          className={`w-full rounded-xl border-2 bg-white px-4 py-3 text-[#451A03] outline-none focus:border-[#1E44E6] ${
            fieldErrors.password ? "border-[#D7263D]" : "border-[#D9C49A]"
          }`}
        />
          {fieldErrors.password && (
            <p className="mt-1 text-sm text-[#D7263D]">{fieldErrors.password}</p>
          )}
      </label>

      <p className="rounded-xl bg-[#FDEEC4] px-4 py-3 text-sm text-[#451A03]">
        สมัครเสร็จ ระบบจะเปิด Wallet ให้ (ยอดเริ่มต้น 0 Token) และสร้าง QR
        ส่วนตัวให้อัตโนมัติ
      </p>

      <label className="flex items-center gap-2 text-sm text-[#451A03]">
        <input
          type="checkbox"
          checked={acceptTerms}
          onChange={(e) => setAcceptTerms(e.target.checked)}
          className="h-4 w-4"
        />
        ยอมรับเงื่อนไขการใช้งานและนโยบายความเป็นส่วนตัว
      </label>
      {fieldErrors.acceptTerms && (
        <p className="-mt-2 text-sm text-[#D7263D]">{fieldErrors.acceptTerms}</p>
      )}

      <button
        type="submit"
        className="w-full rounded-xl border-2 border-[#451A03] bg-[#1E44E6] px-4 py-3 font-bold text-white shadow-[4px_4px_0_#451A03]"
      >
        สมัครสมาชิก
      </button>
    </form>
  );
}
