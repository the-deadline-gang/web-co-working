"use client";
import { useState } from "react";
import { useRouter } from "next/navigation";
export function LoginForm() {
  const [email, setEmail] = useState("");
  const [password, setPassWord] = useState("");
  const [error, setEror] = useState("");
  const router = useRouter();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    // ส่งข้อมูลไปที่ backend
    const res = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email,
        password,
      }),
    });

    // อ่านคำตอบจาก backend
    const data = await res.json();
    console.log(res.status, data);

    if (res.status === 200) {
      setEror("");
      router.push("/home")
    } else {
      setEror(data.message);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="w-full max-w-lg space-y-4">
      {error && (
        <div className="rounded-xl border-2 border-[#D7263D] bg-[#FDDDE0] px-4 py-3 text-sm text-[#D7263D]">
          {error}
        </div>
      )}

      <label className="block">
        <span className="mb-2 block text-sm font-semibold text-[#451A03]">
          mail
        </span>
        <input
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          className={`w-full rounded-xl border-2 bg-white px-4 py-3 text-[#451A03] outline-none focus:border-[#1E44E6] ${
            error ? "border-[#D7263D]" : "border-[#D9C49A]"
          }`}
        />
      </label>

      <label className="block">
        <span className="mb-2 block text-sm font-semibold text-[#451A03]">
          รหัสผ่าน
        </span>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassWord(e.target.value)}
          className={`w-full rounded-xl border-2 bg-white px-4 py-3 text-[#451A03] outline-none focus:border-[#1E44E6] ${
            error ? "border-[#D7263D]" : "border-[#D9C49A]"
          }`}
        />
      </label>

      <button
        type="submit"
        className="w-full rounded-xl border-2 border-[#451A03] bg-[#1E44E6] px-4 py-3 font-bold text-white shadow-[4px_4px_0_#451A03]"
      >
        เข้าสู่ระบบ
      </button>
    </form>
  );
}
