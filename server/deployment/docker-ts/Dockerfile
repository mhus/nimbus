# Build stage - builds all packages once
FROM node:20-alpine AS build

WORKDIR /build

# Install pnpm
RUN corepack enable && corepack prepare pnpm@latest --activate

# Copy workspace files
COPY client/package.json client/pnpm-lock.yaml client/pnpm-workspace.yaml ./

# Copy all package.json files for dependency installation
COPY client/packages/shared/package.json ./packages/shared/
COPY client/packages/engine/package.json ./packages/engine/
COPY client/packages/controls/package.json ./packages/controls/

# Install all dependencies
RUN pnpm install --frozen-lockfile

# Copy source code
COPY client/packages/shared/ ./packages/shared/
COPY client/packages/engine/ ./packages/engine/
COPY client/packages/controls/ ./packages/controls/

# Set NODE_ENV to production
ENV NODE_ENV=production

# Build shared package first
RUN cd packages/shared && pnpm build

# Build engine in viewer mode
RUN cd packages/engine && pnpm build:viewer

# Build engine in editor mode
RUN cd packages/engine && pnpm build:editor

# Build controls
RUN cd packages/controls && pnpm build

# Runtime stage for viewer
FROM nginx:alpine AS viewer

# Copy built viewer files
COPY --from=build /build/packages/engine/dist/viewer /usr/share/nginx/html

# Copy custom nginx config
COPY server/deployment/docker-ts/nginx-viewer.conf /etc/nginx/conf.d/default.conf

EXPOSE 3000

CMD ["nginx", "-g", "daemon off;"]

# Runtime stage for editor
FROM nginx:alpine AS editor

# Copy built editor files
COPY --from=build /build/packages/engine/dist/editor /usr/share/nginx/html

# Copy custom nginx config
COPY server/deployment/docker-ts/nginx-editor.conf /etc/nginx/conf.d/default.conf

EXPOSE 3001

CMD ["nginx", "-g", "daemon off;"]

# Runtime stage for controls
FROM nginx:alpine AS controls

# Copy built controls files
COPY --from=build /build/packages/controls/dist /usr/share/nginx/html

# Copy entrypoint script from controls package
COPY client/packages/controls/docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# Copy custom nginx config
COPY server/deployment/docker-ts/nginx-controls.conf /etc/nginx/conf.d/default.conf

# Set default environment variable
ENV VITE_CONTROL_API_URL=http://localhost:9043

EXPOSE 3002

# Use custom entrypoint
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
