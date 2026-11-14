"use client";
import { useState } from "react";
import type { Me } from "@/types/api";

export function ProfileForm({ user }: { user: Me }) {
  const [firstName, setFirstName] = useState(user.firstName);
  const [lastName, setLastName] = useState(user.lastName);
  const [phone, setPhone] = useState(user.phone);
  const [email, setEmail] = useState(user.email);
  // อายุ / เพศ ยังไม่มีใน API → เริ่มเป็นค่าว่าง
  const [age, setAge] = useState("");
  const [gender, setGender] = useState("");

  const [message, setMessage] = useState("");

  // ปุ่ม "ยกเลิก" → คืนทุกช่องกลับเป็นข้อมูลจาก /me
  function handleCancel() {
    setFirstName(user.firstName);
    setLastName(user.lastName);
    setPhone(user.phone);
    setEmail(user.email);
    setAge("");
    setGender("");
    setMessage("");
  }

  // TODO: backend ยังไม่มี API แก้ไขข้อมูลส่วนตัว (เช่น PUT /me) → ตอนนี้แค่แจ้งเตือน
  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    console.log({ firstName, lastName, phone, email, age, gender });
    setMessage("ยังบันทึกไม่ได้ ระบบยังไม่รองรับการแก้ไขข้อมูล");
  }

  
  const inputClass =
    "w-full rounded-xl border-2 border-[#451A03] bg-white px-4 py-3 text-[#451A03] outline-none focus:border-[#1E44E6]";
  const labelClass = "mb-2 block text-sm font-bold text-[#451A03]";

  function genderClass(value: string) {
    return `w-32 rounded-xl border-2 py-3 text-[#451A03] ${
      gender === value
        ? "border-[#451A03] bg-[#F9B51F] font-bold shadow-[3px_3px_0_#451A03]"
        : "border-[#D9C49A] bg-white"
    }`;
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="space-y-5 rounded-2xl border-2 border-[#451A03] bg-white p-8 shadow-[6px_6px_0_#451A03]"
    >
      <h2 className="text-2xl font-bold text-[#451A03]">ข้อมูลส่วนตัว</h2>

      {/* ชื่อ | นามสกุล */}
      <div className="grid grid-cols-2 gap-4">
        <label className="block">
          <span className={labelClass}>ชื่อ</span>
          <input value={firstName} onChange={(e) => setFirstName(e.target.value)} className={inputClass} />
        </label>
        <label className="block">
          <span className={labelClass}>นามสกุล</span>
          <input value={lastName} onChange={(e) => setLastName(e.target.value)} className={inputClass} />
        </label>
      </div>

      {/* เบอร์โทร | อายุ */}
      <div className="grid grid-cols-2 gap-4">
        <label className="block">
          <span className={labelClass}>เบอร์โทร</span>
          <input
            inputMode="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
            className={`${inputClass} font-mono`}
          />
        </label>
        <label className="block">
          <span className={labelClass}>อายุ</span>
          <input
            inputMode="numeric"
            placeholder="ปี"
            value={age}
            onChange={(e) => setAge(e.target.value)}
            className={`${inputClass} font-mono`}
          />
        </label>
      </div>

      <label className="block">
        <span className={labelClass}>mail</span>
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} className={inputClass} />
      </label>

      <div>
        <span className={labelClass}>เพศ</span>
        <div className="flex gap-3">
          <button type="button" onClick={() => setGender("male")} className={genderClass("male")}>
            ชาย
          </button>
          <button type="button" onClick={() => setGender("female")} className={genderClass("female")}>
            หญิง
          </button>
          <button type="button" onClick={() => setGender("unspecified")} className={genderClass("unspecified")}>
            ไม่ระบุ
          </button>
        </div>
      </div>

      {message && (
        <p className="rounded-xl border-2 border-[#D7263D] bg-[#FDDDE0] px-4 py-3 text-sm text-[#D7263D]">
          {message}
        </p>
      )}

      <div className="flex justify-end gap-3">
        <button
          type="button"
          onClick={handleCancel}
          className="rounded-xl border-2 border-[#451A03] bg-white px-6 py-3 font-bold text-[#451A03]"
        >
          ยกเลิก
        </button>
        <button
          type="submit"
          className="rounded-xl border-2 border-[#451A03] bg-[#1E44E6] px-7 py-3 font-bold text-white shadow-[4px_4px_0_#451A03]"
        >
          บันทึก
        </button>
      </div>
    </form>
  );
}
