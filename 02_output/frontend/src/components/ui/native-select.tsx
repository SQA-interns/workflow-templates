import * as React from "react";
import { cn } from "@/lib/utils";

// The browser's select with the look of Input (UI-13): keyboard, screen readers and mobile pickers
// work without extra code; label and error are rendered by the form field around it.
function NativeSelect({ className, ...props }: React.ComponentProps<"select">) {
  return (
    <select
      className={cn(
        "min-h-11 w-full rounded-md border border-input bg-background px-3 text-base text-text aria-invalid:border-danger",
        className,
      )}
      {...props}
    />
  );
}

export { NativeSelect };
