import "./globals.css";

// Layout ครอบทั้งเว็บ (Next.js บังคับต้องมีไฟล์นี้)
// TODO: ใส่ฟอนต์ (next/font) และ metadata
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="th">
      <body>{children}</body>
    </html>
  );
}
