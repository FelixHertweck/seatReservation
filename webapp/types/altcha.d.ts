import * as React from "react";

declare global {
  namespace JSX {
    interface IntrinsicElements {
      "altcha-widget": React.DetailedHTMLProps<
        React.HTMLAttributes<HTMLElement>,
        HTMLElement
      > & {
        challenge?: string;
        auto?: string;
        display?: string;
        hidefooter?: boolean | string;
        hidelogo?: boolean | string;
        name?: string;
      };
    }
  }
}

declare module "react" {
  namespace JSX {
    interface IntrinsicElements {
      "altcha-widget": React.DetailedHTMLProps<
        React.HTMLAttributes<HTMLElement>,
        HTMLElement
      > & {
        challenge?: string;
        auto?: string;
        display?: string;
        hidefooter?: boolean | string;
        hidelogo?: boolean | string;
        name?: string;
      };
    }
  }
}
