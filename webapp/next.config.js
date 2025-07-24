// @ts-check

// eslint-disable-next-line @typescript-eslint/no-require-imports
const { PHASE_DEVELOPMENT_SERVER } = require("next/constants");

module.exports = (phase, { defaultConfig }) => {
  if (phase === PHASE_DEVELOPMENT_SERVER) {
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

  return {
    output: "export",
    images: {
      unoptimized: true,
    },
  };
};
