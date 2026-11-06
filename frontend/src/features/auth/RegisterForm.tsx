"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";

// ฟอร์มสมัครสมาชิก (U1)  →  POST /auth/register
// TODO: ส่งข้อมูล → ถ้าสำเร็จไปหน้า /verify-otp · ถ้า error แสดงใต้ช่องที่ผิด
export function RegisterForm() {
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [phon, setPhon] = useState("");
  const [age, setAge] = useState(""); // ใส่ไว้รอเลย ถ้าไม่ตรงตอนยิง API ค่อยเปลี่ยนให้ตรง
  const [sex, setSex] = useState("");
  const [password, setPassWord] = useState("");
  const [acceptTerms, setAcceptTerms] = useState(false); // ติ๊ก = true, ไม่ติ๊ก = false

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    console.log({ firstName, lastName });
  }

  return (
    <form onSubmit={handleSubmit} className="w-full max-w-lg space-y-4">
      <div className="grid grid-cols-2 gap-4">
        <label className="block">
          <span className="mb-2 block text-sm font-semibold text-[#451A03]">
            ชื่อ
          </span>
          <input
            value={firstName}
            onChange={(e) => setFirstName(e.target.value)}
            className="w-full rounded-xl border-2 border-[#D9C49A] bg-white px-4 py-3 text-[#451A03] outline-none focus:border-[#1E44E6]"
          />
        </label>

        <label className="block">
          <span className="mb-2 block text-sm font-semibold text-[#451A03]">
            นามสกุล
          </span>
          <input
            value={lastName}
            onChange={(e) => setLastName(e.target.value)}
            className="w-full rounded-xl border-2 border-[#D9C49A] bg-white px-4 py-3 text-[#451A03] outline-none focus:border-[#1E44E6]"
          />
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
          className="w-full rounded-xl border-2 border-[#D9C49A] bg-white px-4 py-3 text-[#451A03] outline-none focus:border-[#1E44E6]"
        />
      </label>

      <div className="grid grid-cols-4 gap-4">
        <label className="block col-span-3">
          <span className="mb-2 block text-sm font-semibold text-[#451A03]">
            เบอร์โทร
          </span>
          <input
            inputMode="tel"
            placeholder="0812345678"
            value={phon}
            onChange={(e) => setPhon(e.target.value)}
            className="w-full rounded-xl border-2 border-[#D9C49A] bg-white px-4 py-3 text-[#451A03] outline-none focus:border-[#1E44E6]"
          />
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
          className="w-full rounded-xl border-2 border-[#D9C49A] bg-white px-4 py-3 text-[#451A03] outline-none focus:border-[#1E44E6]"
        />
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

      <button
        type="submit"
        className="w-full rounded-xl border-2 border-[#451A03] bg-[#1E44E6] px-4 py-3 font-bold text-white shadow-[4px_4px_0_#451A03]"
        >
          สมัครสมาชิก
      </button>
    </form>
  );
}
