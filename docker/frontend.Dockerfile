FROM node:24-bookworm-slim AS build
ARG APP_DIR
WORKDIR /workspace/frontend/${APP_DIR}
COPY frontend/${APP_DIR}/package.json frontend/${APP_DIR}/package-lock.json ./
RUN npm ci --include=optional --no-audit --no-fund && node -e "import('rolldown')"
COPY frontend/${APP_DIR}/ ./
ARG VITE_BLADE_INSPECTION=false
RUN npm run build

FROM nginx:1.28-alpine
ARG APP_DIR
COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/frontend/${APP_DIR}/dist /usr/share/nginx/html
