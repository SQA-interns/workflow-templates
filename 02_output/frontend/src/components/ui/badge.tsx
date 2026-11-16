import * as React from "react";
import { cn } from "@/lib/utils";

// shadcn/ui Badge: a Tag or a filter chip (specification §9.3), pill shaped, from the tokens.
function Badge({ className, ...props }: React.ComponentProps<"span">) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full border border-border bg-surface px-2 py-0.5 text-sm text-text",
        className,
      )}
      {...props}
    />
  );
}

export { Badge };
