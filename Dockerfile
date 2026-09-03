# ==============================================================================
# Stage 1: Build the React web frontend
# ==============================================================================
FROM node:20-bookworm-slim AS web-builder

WORKDIR /app/web

# Install dependencies using package-lock for reproducible builds
COPY web/package.json web/package-lock.json ./
RUN npm ci

# Copy web source and build production bundle
COPY web/ ./
# Empty VITE_API_URL ensures same-origin API requests in production
ENV VITE_API_URL=""
RUN npm run build

# ==============================================================================
# Stage 2: Production Node.js + Express server runtime
# ==============================================================================
FROM node:20-bookworm-slim AS runtime

# Production environment variables
ENV NODE_ENV=production \
    PORT=3000 \
    DATABASE_PATH=/app/data/bankpoker.db \
    CLIENT_DIST_PATH=/app/web/dist

WORKDIR /app

# Install production server dependencies
COPY server/package.json server/package-lock.json ./server/
WORKDIR /app/server
RUN npm ci --omit=dev

# Copy server application code
COPY server/src ./src

# Copy built React frontend assets from web-builder stage
COPY --from=web-builder /app/web/dist /app/web/dist

# Create persistent database directory and grant ownership to node user
RUN mkdir -p /app/data && chown -R node:node /app

# Switch to non-root user for security
USER node

# Expose internal Express port
EXPOSE 3000

# Container healthcheck testing /api/health
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD node -e "require('http').get('http://localhost:' + (process.env.PORT || 3000) + '/api/health', (r) => { process.exit(r.statusCode === 200 ? 0 : 1); }).on('error', () => process.exit(1));"

# Launch the production Express server
CMD ["node", "src/server.js"]
