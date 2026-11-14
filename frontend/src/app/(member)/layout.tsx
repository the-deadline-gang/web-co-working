import { redirect } from "next/navigation"; // ① เพิ่ม
import { Sidebar } from "@/components/layout/Sidebar";
import { getMe } from "@/lib/api/me";

export default async function MemberLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const user = await getMe();
  if (!user) {
    redirect("/auth"); // ยังไม่ล็อคอินหรือหมดอายุจะไปหน้าสู่ระบบใหม่
  }

  return (
    <div className="grid min-h-screen grid-cols-[230px_1fr]">
      <Sidebar user={user} />

      <main className="bg-[#FFFBEB] px-14 py-10">{children}</main>
    </div>
  );
}
