"use client";
import { RegisterForm } from "@/features/auth/RegisterForm";
import { useState } from "react";
import { LoginForm } from "@/features/auth/LoginForm";
export default function AuthPage() {
  const [tab, setTab] = useState("register");

  return (
    <div className="w-full max-w-lg space-y-6 ">
      
      <div className="grid grid-cols-2 overflow-hidden rounded-xl border-2 border-[#451A03]">
        <button
          type="button"
          onClick={() => setTab("register")}
          className={`py-3 font-bold text-[#451A03] ${tab === "register" ? "bg-[#F9B51F]" : "bg-white"}`}
        >
          สมัครสมาชิก
        </button>
        <button
          type="button"
          onClick={() => setTab("login")}
          className={`border-l-2 border-[#451A03] py-3 font-bold text-[#451A03] ${tab === "login" ? "bg-[#F9B51F]" : "bg-white"}`}
        >
          เข้าสู่ระบบ
        </button>
      </div>
      {tab === "register" ? <RegisterForm/> : <LoginForm/>}
    </div>

  );
}
