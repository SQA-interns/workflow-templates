import { cva, type VariantProps } from "class-variance-authority";
import { Slot } from "radix-ui";
import * as React from "react";
import { cn } from "@/lib/utils";

// shadcn/ui Button: three levels (UI-12) with hover, focus, disabled states; 44 px touch target (UI-20).
const buttonVariants = cva(
  "inline-flex min-h-11 items-center justify-center gap-2 rounded-md border px-4 text-sm font-semibold whitespace-nowrap transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      variant: {
        primary: "border-primary bg-primary text-primary-foreground hover:opacity-90",
        secondary: "border-border bg-surface text-text hover:bg-surface-raised",
        ghost: "border-transparent bg-transparent text-text hover:bg-surface",
      },
    },
    defaultVariants: { variant: "primary" },
  },
);

type ButtonProps = React.ComponentProps<"button"> &
  VariantProps<typeof buttonVariants> & { asChild?: boolean };

function Button({ className, variant, asChild = false, ...props }: ButtonProps) {
  const Comp = asChild ? Slot.Root : "button";
  return <Comp className={cn(buttonVariants({ variant }), className)} {...props} />;
}

export { Button, buttonVariants };
