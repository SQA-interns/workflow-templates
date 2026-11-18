import * as React from "react";
import { cn } from "@/lib/utils";

// shadcn/ui Input (UI-13): label and error are rendered by the form field around it.
function Input({ className, ...props }: React.ComponentProps<"input">) {
  return (
    <input
      className={cn(
        "min-h-11 w-full rounded-md border border-input bg-surface-raised px-3 text-base text-text placeholder:text-text-muted aria-invalid:border-danger",
        className,
      )}
      {...props}
    />
  );
}

export { Input };
