// รูปแบบข้อมูลที่รับจาก backend (ดูจาก docs/openapi/identity.yaml)

/** ข้อมูลผู้ใช้ที่ login อยู่ → GET /me */
export type Me = {
  userId: number;
  displayId: string; // "U-000006"
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
};