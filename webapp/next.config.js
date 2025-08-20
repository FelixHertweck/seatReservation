// @ts-check

// eslint-disable-next-line @typescript-eslint/no-require-imports
const { PHASE_DEVELOPMENT_SERVER } = require("next/constants");
const process = require("process");

module.exports = (phase, { defaultConfig }) => {
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
