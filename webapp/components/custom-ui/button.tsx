"use client";

// Drop-in replacement for `@/components/ui/button` that adds an `isLoading`
// prop: while true, the button's content is hidden (but keeps its layout
// space, so the button doesn't change width) behind a centered spinner, and
// the button is disabled. Import this instead of the ui/button primitive for
// any button that triggers an async action (form submit, delete, export, …).
// Ignored when `asChild` is set, since then the button merges its props onto
// a single child (e.g. a `Link`) instead of rendering its own content.

import * as React from "react";
import { Loader2Icon } from "lucide-react";
import { Button as BaseButton, buttonVariants } from "@/components/ui/button";
import { cn } from "@/lib/utils";

type BaseButtonProps = React.ComponentProps<typeof BaseButton>;

type AnimatedIconHandle = {
  startAnimation?: () => void;
  stopAnimation?: () => void;
};

// lucide-animated icons only animate on hover of their own (often tiny) SVG.
// This walks the button's direct children, grabs a ref to any forwardRef
// icon among them (duck-typed via startAnimation/stopAnimation), and drives
// it from the button's own hover state so the whole button is the hit area.
function useIconHover(children: React.ReactNode) {
  // Only ever mutated from ref callbacks (commit phase), never during render.
  const handles = React.useRef<Map<number, AnimatedIconHandle>>(new Map());

  const content = React.useMemo(() => {
    let nextIndex = 0;
    return React.Children.map(children, (child) => {
      if (
        !React.isValidElement(child) ||
        (child.type as { $$typeof?: symbol } | undefined)?.$$typeof !==
          Symbol.for("react.forward_ref")
      ) {
        return child;
      }
      const index = nextIndex++;
      return React.cloneElement(
        child as React.ReactElement<{ ref?: React.Ref<AnimatedIconHandle> }>,
        {
          ref: (handle: AnimatedIconHandle | null) => {
            if (handle) handles.current.set(index, handle);
            else handles.current.delete(index);
          },
        },
      );
    });
  }, [children]);

  return {
    content,
    onMouseEnter: () => handles.current.forEach((h) => h.startAnimation?.()),
    onMouseLeave: () => handles.current.forEach((h) => h.stopAnimation?.()),
  };
}

function Button({
  className,
  isLoading = false,
  disabled,
  asChild = false,
  children,
  onMouseEnter,
  onMouseLeave,
  ...props
}: BaseButtonProps & { isLoading?: boolean }) {
  // asChild merges our props onto a single child (e.g. a Link) instead of
  // rendering our own content - the icon we want to wire up then lives one
  // level deeper, inside that child's own children.
  const singleChild =
    asChild && React.isValidElement(children) ? children : null;
  const iconSource = singleChild
    ? (singleChild.props as { children?: React.ReactNode }).children
    : children;

  const icon = useIconHover(iconSource);

  const handleMouseEnter = (e: React.MouseEvent<HTMLButtonElement>) => {
    icon.onMouseEnter();
    onMouseEnter?.(e);
  };
  const handleMouseLeave = (e: React.MouseEvent<HTMLButtonElement>) => {
    icon.onMouseLeave();
    onMouseLeave?.(e);
  };

  if (asChild) {
    const content = singleChild
      ? React.cloneElement(
          singleChild as React.ReactElement<{ children?: React.ReactNode }>,
          { children: icon.content },
        )
      : children;
    return (
      <BaseButton
        asChild
        className={className}
        disabled={disabled}
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
        {...props}
      >
        {content}
      </BaseButton>
    );
  }

  if (!isLoading) {
    return (
      <BaseButton
        className={className}
        disabled={disabled}
        onMouseEnter={handleMouseEnter}
        onMouseLeave={handleMouseLeave}
        {...props}
      >
        {icon.content}
      </BaseButton>
    );
  }

  return (
    <BaseButton
      className={cn("relative", className)}
      disabled
      aria-busy="true"
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      {...props}
    >
      <Loader2Icon
        className="absolute inset-0 m-auto animate-spin"
        aria-hidden="true"
      />
      <span className="contents invisible">{icon.content}</span>
    </BaseButton>
  );
}

export { Button, buttonVariants };
