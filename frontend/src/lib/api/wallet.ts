import { cache } from "react";
import { cookies } from "next/headers";
import type { Page, TopupConfig, Wallet, WalletTransaction } from "@/types/api";

export const getMyWallet = cache(async (): Promise<Wallet | null> => {
  const cookieStore = await cookies();

  const res = await fetch(`${process.env.BACKEND_URL}/me/wallet`, {
    headers: { cookie: cookieStore.toString() }, // ส่ง cookie access_token ต่อไปให้ gateway
    cache: "no-store", // ห้ามจำคำตอบเก่า ต้องถามใหม่ทุกครั้ง
  });

  if (!res.ok) return null; // 404 = ผู้ใช้นี้ยังไม่มี wallet
  return res.json();
});
// ตั้งค่าการเติมเงิน (ปุ่มจำนวนลัด, ขั้นต่ำ-สูงสุด, อัตราแลก Token)
export const getTopupConfig = cache(async (): Promise<TopupConfig | null> => {
  const cookieStore = await cookies();
  const res = await fetch(`${process.env.BACKEND_URL}/topups/config`, {
    headers: { cookie: cookieStore.toString() },
    cache: "no-store",
  });
  if (!res.ok) return null;
  return res.json();
});

// ประวัติรายการเคลื่อนไหว (ใหม่สุดก่อน) · ตอนนี้ดึงแค่หน้าแรก 50 รายการ ยังไม่มีปุ่ม "ดูเพิ่ม"
export const getMyTransactions = cache(async (): Promise<WalletTransaction[]> => {
  const cookieStore = await cookies();
  const res = await fetch(`${process.env.BACKEND_URL}/me/wallet/transactions?size=50`, {
    headers: { cookie: cookieStore.toString() },
    cache: "no-store",
  });
  if (!res.ok) return []; // ไม่มี wallet / ยังไม่ login → ถือว่าไม่มีรายการ
  const data: Page<WalletTransaction> = await res.json();
  return data.items;
});
