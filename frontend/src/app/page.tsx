import { redirect } from "next/navigation";

// หน้าแรก "/" → พาไปหน้า U1 (/auth)
// TODO: พอมีระบบ login แล้ว ถ้า login อยู่ให้ไป /home แทน
export default function RootPage() {
  redirect("/auth");
}
