import * as React from "react";
import { cn } from "@/lib/utils";

// shadcn/ui Card: grouped content (UI-11), 12 px radius (UI-07).
function Card({ className, ...props }: React.ComponentProps<"section">) {
  return (
    <section
      className={cn("rounded-lg border bg-card p-4 text-card-foreground shadow-sm", className)}
      {...props}
    />
  );
}

export { Card };
