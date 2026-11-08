import { Sidebar } from "@/components/layout/Sidebar";

// Layout ของกลุ่มหน้าที่ต้อง login แล้ว (U2–U12)
// ซ้าย = sidebar ส้ม (ใช้ร่วมกันทุกหน้า) · ขวา = เนื้อหาของแต่ละหน้า
export default function MemberLayout({ children }: { children: React.ReactNode }) {
  return (
    // แบ่ง 2 คอลัมน์: sidebar กว้างตายตัว 230px · เนื้อหากินที่เหลือ
    <div className="grid min-h-screen grid-cols-[230px_1fr]">
      <Sidebar />

      {/* ฝั่งเนื้อหา: พื้นครีม + เว้นขอบ */}
      <main className="bg-[#FFFBEB] px-14 py-10">{children}</main>
    </div>
  );
}
