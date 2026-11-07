# Builds the web app and serves it with nginx, which also forwards /api to the backend.
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Vite reads these while building. Left unset, the defaults in the source apply.
ARG VITE_INSTITUTION_NAME
ARG VITE_INSTITUTION_EMAIL_DOMAIN
ARG VITE_SHOW_DEMO_ACCOUNTS
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
