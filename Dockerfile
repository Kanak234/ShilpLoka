# Multi-stage build for ShilpLoka Production Web Server
FROM node:20-alpine AS builder

WORKDIR /app

# Copy dependency manifests
COPY package.json package-lock.json ./

# Deterministic dependency installation
RUN npm ci

# Copy application sources
COPY . .

# Build production distribution into dist/
RUN npm run build

# Runtime Stage: Hardened, unprivileged Nginx web server
FROM nginx:alpine-slim AS runner

# Hardening: prepare directories and permissions for unprivileged nginx user
RUN mkdir -p /var/cache/nginx /var/log/nginx /usr/share/nginx/html /var/run && \
    touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid /var/cache/nginx /var/log/nginx /usr/share/nginx/html

# Copy custom Nginx configuration listening on 8080
COPY --chown=nginx:nginx nginx.conf /etc/nginx/conf.d/default.conf

# Copy build artifacts
COPY --from=builder --chown=nginx:nginx /app/dist/ /usr/share/nginx/html/

USER nginx

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
  CMD wget -q -O /dev/null http://localhost:8080/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
