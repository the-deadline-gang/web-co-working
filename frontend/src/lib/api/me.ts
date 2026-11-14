import { cache } from "react";
import { cookies } from "next/headers";
import type { Me } from "@/types/api";

export const getMe = cache(async (): Promise<Me | null> => {
  const cookieStore = await cookies();

  const res = await fetch(`${process.env.BACKEND_URL}/me`, {
    headers: { cookie: cookieStore.toString() }, // ส่ง cookie access_token ต่อไปให้ gateway
    cache: "no-store", // ห้ามจำคำตอบเก่า ต้องถามใหม่ทุกครั้ง
  });

  if (!res.ok) return null; // 401 = ยังไม่ login
  return res.json();
});