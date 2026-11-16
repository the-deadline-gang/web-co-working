import { redirect } from "next/navigation";
import { Sidebar } from "@/components/layout/Sidebar";
import { getMe } from "@/lib/api/me";
import { getMyWallet } from "@/lib/api/wallet";

export default async function MemberLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const user = await getMe();
  if (!user) {
    redirect("/auth"); // ยังไม่ล็อคอินหรือหมดอายุจะไปหน้าสู่ระบบใหม่
  }

  // ยอด Token ใน Sidebar (null = ยังไม่มี wallet → Sidebar แสดง 0 ไม่ต้อง redirect)
  const wallet = await getMyWallet();

  return (
    <div className="grid min-h-screen grid-cols-[230px_1fr]">
      <Sidebar user={user} wallet={wallet} />

      <main className="bg-[#FFFBEB] px-14 py-10">{children}</main>
    </div>
  );
}
