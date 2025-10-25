# syntax=docker.io/docker/dockerfile:1

FROM node:25-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

# Create user
RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs

COPY webapp/public ./public

# Copy pre-built application
COPY --chown=nextjs:nodejs webapp/.next/standalone ./
COPY --chown=nextjs:nodejs webapp/.next/static ./.next/static

USER nextjs

EXPOSE 3000

ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

CMD ["node", "server.js"]