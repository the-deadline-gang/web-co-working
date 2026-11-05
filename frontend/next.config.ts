import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // หน้าเว็บเรียก /api/... แล้ว Next.js ส่งต่อไปที่ backend ให้เอง
  // ไม่ติด CORS และ cookie access_token ใช้งานได้เพราะอยู่ origin เดียวกัน
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${process.env.BACKEND_URL}/:path*`,
      },
    ];
  },
};

export default nextConfig;
