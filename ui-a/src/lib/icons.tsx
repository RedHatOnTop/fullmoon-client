import type { SVGProps, ReactNode } from "react";

type P = SVGProps<SVGSVGElement> & { size?: number };
const svg =
  (paths: ReactNode, vb = 24) =>
  ({ size = 18, ...rest }: P) =>
    (
      <svg
        width={size}
        height={size}
        viewBox={`0 0 ${vb} ${vb}`}
        fill="none"
        stroke="currentColor"
        strokeWidth={1.7}
        strokeLinecap="round"
        strokeLinejoin="round"
        {...rest}
      >
        {paths}
      </svg>
    );

// brand mark — a feather whose rachis doubles as a gear tooth
export const Pinion = ({ size = 22, ...rest }: P) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none" {...rest}>
    <path
      d="M20 4C11 5 6.5 9.5 5 15c-1 3.6-1.2 4.6-1.2 4.6l1.6.4s.9-.2 4.4-1.1C15.6 17.4 19 12.8 20 4Z"
      fill="currentColor"
      opacity="0.14"
    />
    <path
      d="M20 4C11 5 6.5 9.5 5 15c-1 3.6-1.2 4.6-1.2 4.6"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
    />
    <path
      d="M18.4 6.6 8.2 16.8M16.9 5.6l-2.4 2.4M15.6 8.7l-3.5 3.5M13.9 12l-3.6 3.6M11.9 14.8l-3 3"
      stroke="currentColor"
      strokeWidth="1.4"
      strokeLinecap="round"
      opacity="0.85"
    />
  </svg>
);

export const Home = svg(
  <>
    <path d="M3 10.5 12 3l9 7.5" />
    <path d="M5 9.5V20h14V9.5" />
    <path d="M9.5 20v-6h5v6" />
  </>,
);

export const Cube = svg(
  <>
    <path d="M12 2.8 20 7v10l-8 4.2L4 17V7l8-4.2Z" />
    <path d="M4 7l8 4.2M20 7l-8 4.2M12 11.2V21" />
  </>,
);

export const Shirt = svg(
  <>
    <path d="M8 3 4 6l2 3 2-1v11h8V8l2 1 2-3-4-3-2 2h-4L8 3Z" />
  </>,
);

export const Gear = svg(
  <>
    <circle cx="12" cy="12" r="3.2" />
    <path d="M12 2v3M12 19v3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M2 12h3M19 12h3M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1" />
  </>,
);

export const Server = svg(
  <>
    <rect x="3" y="4" width="18" height="7" rx="2" />
    <rect x="3" y="13" width="18" height="7" rx="2" />
    <path d="M7 7.5h.01M7 16.5h.01" />
  </>,
);

export const Play = ({ size = 18, ...rest }: P) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" {...rest}>
    <path d="M7 4.5v15l13-7.5-13-7.5Z" />
  </svg>
);

export const Chevron = svg(<path d="m6 9 6 6 6-6" />);
export const Search = svg(
  <>
    <circle cx="11" cy="11" r="7" />
    <path d="m20 20-3.2-3.2" />
  </>,
);
export const Bolt = ({ size = 18, ...rest }: P) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="currentColor" {...rest}>
    <path d="M13 2 4 14h6l-1 8 9-12h-6l1-8Z" />
  </svg>
);
export const Wifi = svg(
  <>
    <path d="M2 8.5a15 15 0 0 1 20 0M5 12a10 10 0 0 1 14 0M8.5 15.5a5 5 0 0 1 7 0" />
    <path d="M12 19h.01" />
  </>,
);
export const Folder = svg(<path d="M3 6a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6Z" />);
export const Min = svg(<path d="M5 12h14" />);
export const Max = svg(<rect x="5" y="5" width="14" height="14" rx="1.5" />);
export const Close = svg(<path d="M6 6l12 12M18 6 6 18" />);
