import * as React from "react";
import { cn } from "@/lib/utils";

// Textarea with the look of Input (UI-13): label and error are rendered by the form field around it.
function Textarea({ className, ...props }: React.ComponentProps<"textarea">) {
  return (
    <textarea
      className={cn(
        "min-h-24 w-full rounded-md border border-input bg-surface-raised px-3 py-2 text-base text-text placeholder:text-text-muted aria-invalid:border-danger",
        className,
      )}
      {...props}
    />
  );
}

export { Textarea };
