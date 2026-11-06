import Image from "next/image";
export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="grid grid-cols-2 min-h-screen">
      <div className="flex flex-col justify-between bg-[#1E44E6] p-16 text-white relative overflow-hidden">
        <div className="absolute -top-12 -right-16 h-80 w-80 rounded-full border-2 border-[#451A03] bg-[#F08A12]" />

        <div className="absolute top-48 right-28 h-24 w-24 rotate-12 rounded-2xl border-2 border-[#451A03] bg-[#F9B51F]" />
        <div className="relative z-10">
          <Image
            src="/brand/cosync-logo-white.svg"
            alt="CO-SYNC"
            width={177}
            height={36}
          />
        </div>

        <div className="relative z-10">
          <p className="text-sm font-semibold tracking-widest text-[#F9B51F]">
            SYSTEM CO-WORKING SPACE
          </p>
          <h1 className="text-5xl font-bold leading-tight">
            จองที่นั่ง
            <br />
            จ่ายด้วย Token
            <br />
            เข้างานด้วย{" "}
            <span className="rounded-lg border-2 border-[#451A03] bg-[#F9B51F] px-3 text-[#451A03] shadow-[4px_4px_0_#451A03]">
              QR
            </span>
          </h1>

          <div className="mt-10 grid grid-cols-3 gap-4">
            <div className="rounded-xl border-2 border-[#451A03] bg-[#FFFBEB] p-4 text-[#451A03] shadow-[4px_4px_0_#451A03]">
              <p className="text-sm font-semibold">Day Pass</p>
              <p className="text-2xl font-bold">
                150 <span className="text-xs font-normal">/วัน</span>
              </p>
            </div>

            <div className="rounded-xl border-2 border-[#451A03] bg-[#FFFBEB] p-4 text-[#451A03] shadow-[4px_4px_0_#451A03]">
              <p className="text-sm font-semibold">โต๊ะ</p>
              <p className="text-2xl font-bold">
                40 <span className="text-xs font-normal">/ชม.</span>
              </p>
            </div>

            <div className="rounded-xl border-2 border-[#451A03] bg-[#FFFBEB] p-4 text-[#451A03] shadow-[4px_4px_0_#451A03]">
              <p className="text-sm font-semibold">ห้อง</p>
              <p className="text-2xl font-bold">
                120 <span className="text-xs font-normal">/ชม.</span>
              </p>
            </div>
          </div>
        </div>
      </div>

      <div className="flex items-start justify-center bg-[#FFFBEB] p-16 pt-24">
        {children}
      </div>
    </div> 
  );
}
