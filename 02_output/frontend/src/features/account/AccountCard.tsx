import type { ReactNode } from "react";
import { Card } from "@/components/ui/card";

/** The narrow centred card of the sign-in screens (WF-01, WF-02, WF-03). */
export function AccountCard({ children }: { children: ReactNode }) {
  return (
    <div className="mx-auto w-full max-w-md">
      <Card className="flex flex-col gap-4">{children}</Card>
    </div>
  );
}
