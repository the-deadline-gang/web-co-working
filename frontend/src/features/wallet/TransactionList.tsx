"use client";
import { useState } from "react";
import {
  Plus,
  CalendarDays,
  Package,
  RotateCcw,
  TriangleAlert,
  X,
} from "lucide-react";
import type { Transaction, TxType } from "./mock";

// ปุ่มกรอง: value "all" = แสดงทุกรายการ
const FILTERS: { label: string; value: TxType | "all" }[] = [
  { label: "ทั้งหมด", value: "all" },
  { label: "เติมเงิน", value: "topup" },
  { label: "ค่าจอง", value: "booking" },
  { label: "ค่ายืม", value: "loan" },
];

// ชื่อไอคอนในข้อมูล → ไอคอน lucide จริง
const ICONS = {
  plus: Plus,
  calendar: CalendarDays,
  package: Package,
  refund: RotateCcw,
  alert: TriangleAlert,
  x: X,
};

// สีพื้นกล่องไอคอนตาม tone
const TONE_BG = {
  green: "bg-[#DCFCE7]",
  blue: "bg-[#DBEAFE]",
  yellow: "bg-[#FEF3C7]",
  red: "bg-[#FDDDE0]",
};

export function TransactionList({
  transactions,
}: {
  transactions: Transaction[];
}) {
  const [filter, setFilter] = useState<TxType | "all">("all");

  // รายการที่จะแสดงตามปุ่มที่เลือก
  const shown =
    filter === "all"
      ? transactions
      : transactions.filter((tx) => tx.type === filter);

  // นับจำนวนรายการของแต่ละปุ่ม → เลขเล็กๆ หลังชื่อปุ่ม ("ทั้งหมด 6")
  function countOf(value: TxType | "all") {
    return value === "all"
      ? transactions.length
      : transactions.filter((tx) => tx.type === value).length;
  }

  // สีของจำนวนเงิน: เงินเข้า = เขียว · ค่าปรับ (tone แดง + เงินออก) = แดง · ที่เหลือ = สีเข้ม
  function amountColor(tx: Transaction) {
    if (tx.amount > 0) return "text-[#15803D]";
    if (tx.amount < 0 && tx.tone === "red") return "text-[#D7263D]";
    return "text-[#451A03]";
  }

  return (
    <div className="space-y-6">
      {/* ปุ่มกรอง */}
      <div className="flex gap-3">
        {FILTERS.map((f) => (
          <button
            key={f.value}
            type="button"
            onClick={() => setFilter(f.value)}
            className={`rounded-xl border-2 px-5 py-2.5 text-[#451A03] ${
              filter === f.value
                ? "border-[#451A03] bg-[#F9B51F] font-bold shadow-[3px_3px_0_#451A03]"
                : "border-[#D9C49A] bg-white"
            }`}
          >
            {f.label}{" "}
            <span className="ml-1 font-mono text-sm">{countOf(f.value)}</span>
          </button>
        ))}
      </div>

      <h2 className="text-2xl font-bold text-[#451A03]">ตุลาคม 2569</h2>

      <div className="divide-y-2 divide-dashed divide-[#F4D9A0] rounded-2xl border-2 border-[#451A03] bg-white text-[#451A03]">
        {shown.map((tx) => {
          const Icon = ICONS[tx.icon];
          return (
            <div
              key={tx.id}
              className="grid grid-cols-[auto_1fr_230px_130px_160px] items-center gap-4 px-5 py-4"
            >
              <div
                className={`flex h-12 w-12 items-center justify-center rounded-xl border-2 border-[#451A03] ${TONE_BG[tx.tone]}`}
              >
                <Icon className="h-5 w-5" />
              </div>

              <div>
                <p className="font-bold">{tx.title}</p>
                <p className="text-sm">{tx.subtitle}</p>
              </div>

              <p className="font-mono text-sm">{tx.ref}</p>

              <p className="text-sm">{tx.time}</p>

              <div className="flex flex-col items-end gap-1">
                <p className={`font-mono text-xl font-bold ${amountColor(tx)}`}>
                  {tx.amount > 0 ? "+" : ""}
                  {tx.amount}
                </p>
              </div>
            </div>
          );
        })}

        {shown.length === 0 && (
          <p className="px-5 py-8 text-center text-sm">ยังไม่มีรายการ</p>
        )}
      </div>
    </div>
  );
}
