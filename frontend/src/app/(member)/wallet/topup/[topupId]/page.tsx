// หน้ารอชำระเงิน + ผลการชำระเงิน (U4)
// [topupId] ในชื่อโฟลเดอร์ = ส่วนของ URL ที่เปลี่ยนได้ เช่น /wallet/topup/15 → topupId = "15"
import { TopupStatus } from "@/features/wallet/TopupStatus";

export default async function TopupStatusPage({
  params,
}: {
  params: Promise<{ topupId: string }>;
}) {
  const { topupId } = await params;
  return <TopupStatus topupId={topupId} />;
}
