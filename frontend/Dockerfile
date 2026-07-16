# Build Stage
FROM node:20-alpine AS build
WORKDIR /app

# Copy package.json and package-lock.json
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy source code and config files
COPY . .

# Vite inlines VITE_* vars at build time. Setting it here (rather than relying on a
# .env file that may or may not exist in the build context) guarantees the container
# always calls the nginx-proxied same-origin /api path, regardless of any local
# frontend/.env left over from `npm run dev` against a directly-run backend.
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Build Vite application
RUN npm run build

# Run Stage - nginx-unprivileged runs entirely as a non-root user (unlike nginx:alpine,
# whose master process still starts as root before workers drop privileges)
FROM nginxinc/nginx-unprivileged:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080

# Healthcheck for frontend
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://127.0.0.1:8080/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
