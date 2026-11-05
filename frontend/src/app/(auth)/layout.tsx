// Layout ของกลุ่มหน้าที่ยังไม่ login (U1 และหน้า OTP)
// TODO: ซ้าย = แผงสีน้ำเงิน (hero) · ขวา = ฟอร์ม
export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return <div>{children}</div>;
}
