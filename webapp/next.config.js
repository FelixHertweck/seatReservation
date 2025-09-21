import { PHASE_DEVELOPMENT_SERVER } from "next/constants.js";
import process from "process";

const nextConfig = (phase) => {
  const isDev = phase === PHASE_DEVELOPMENT_SERVER;
  const buildMode = process.env.BUILD_MODE;
  if (isDev) {
    return {
      async rewrites() {
        return [
          {
            source: "/api/:path*",
            destination: `http://localhost:8080/api/:path*`,
          },
        ];
      },
    };
  }

  if (buildMode === "static") {
    return {
      output: "export",
      images: { unoptimized: true },
    };
  }

  if (buildMode === "standalone") {
    return {
      output: "standalone",
    };
  }
  return {};
};

export default nextConfig;
