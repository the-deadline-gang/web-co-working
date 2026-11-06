import "./globals.css";

// ⚠️ Layout ครอบ "ทั้งเว็บ" (ทุกหน้า) — Next.js บังคับต้องมี <html> กับ <body>
// ไม่ต้องเขียนอะไรเพิ่มในไฟล์นี้ · ฝั่งซ้ายของหน้า U1 ไปเขียนที่ src/app/(auth)/layout.tsx
// TODO: ใส่ฟอนต์ (next/font) และ metadata
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="th">
      <body>{children}</body>
    </html>
  );
}
