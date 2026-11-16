import { cache } from "react";
import { cookies } from "next/headers";
import type { Wallet } from "@/types/api";

export const getMyWallet = cache(async (): Promise<Wallet | null> => {
  const cookieStore = await cookies();

  const res = await fetch(`${process.env.BACKEND_URL}/me/wallet`, {
    headers: { cookie: cookieStore.toString() }, // ส่ง cookie access_token ต่อไปให้ gateway
    cache: "no-store", // ห้ามจำคำตอบเก่า ต้องถามใหม่ทุกครั้ง
  });

  if (!res.ok) return null; // 404 = ผู้ใช้นี้ยังไม่มี wallet
  return res.json();
});