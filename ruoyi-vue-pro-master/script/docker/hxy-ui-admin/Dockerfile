FROM node:16-alpine AS build

ARG NODE_ENV=production
ARG UI_REPO_URL=https://gitee.com/yudaocode/yudao-ui-admin-vue3.git
ARG UI_REPO_REF=master
ARG PUBLIC_PATH=/
ARG VUE_APP_TITLE=荷小悦
ARG VUE_APP_BASE_API=/prod-api
ARG VUE_APP_APP_NAME=/
ARG VUE_APP_TENANT_ENABLE=true
ARG VUE_APP_CAPTCHA_ENABLE=true
ARG VUE_APP_DOC_ENABLE=true
ARG VUE_APP_BAIDU_CODE=fadc1bd5db1a1d6f581df60a1807f8ab
ARG VITE_APP_TITLE=荷小悦
ARG VITE_BASE_URL=
ARG VITE_API_URL=/prod-api/admin-api
ARG VITE_BASE_PATH=/

WORKDIR /app

COPY overlay/ /tmp/hxy-ui-overlay-vue2/
COPY overlay-vue3/ /tmp/hxy-ui-overlay-vue3/

RUN apk add --no-cache git
RUN git clone --depth=1 --branch "${UI_REPO_REF}" "${UI_REPO_URL}" .
RUN if [ -f /app/vite.config.ts ]; then \
      if [ -d /tmp/hxy-ui-overlay-vue3/src ]; then cp -R /tmp/hxy-ui-overlay-vue3/src/* /app/src/; fi; \
    else \
      if [ -d /tmp/hxy-ui-overlay-vue2/src ]; then cp -R /tmp/hxy-ui-overlay-vue2/src/* /app/src/; fi; \
    fi

ENV NODE_ENV=${NODE_ENV} \
    PUBLIC_PATH=${PUBLIC_PATH} \
    VUE_APP_TITLE=${VUE_APP_TITLE} \
    VUE_APP_BASE_API=${VUE_APP_BASE_API} \
    VUE_APP_APP_NAME=${VUE_APP_APP_NAME} \
    VUE_APP_TENANT_ENABLE=${VUE_APP_TENANT_ENABLE} \
    VUE_APP_CAPTCHA_ENABLE=${VUE_APP_CAPTCHA_ENABLE} \
    VUE_APP_DOC_ENABLE=${VUE_APP_DOC_ENABLE} \
    VUE_APP_BAIDU_CODE=${VUE_APP_BAIDU_CODE} \
    VITE_APP_TITLE=${VITE_APP_TITLE} \
    VITE_BASE_URL=${VITE_BASE_URL} \
    VITE_API_URL=${VITE_API_URL} \
    VITE_BASE_PATH=${VITE_BASE_PATH}

RUN if [ -f /app/vite.config.ts ]; then \
      npm config set registry https://registry.npmmirror.com \
      && npm i -g pnpm@8 \
      && pnpm config set registry https://registry.npmmirror.com \
      && pnpm install --frozen-lockfile=false --prod=false \
      && pnpm build:prod; \
    else \
      npm config set registry https://registry.npmmirror.com \
      && npm install --legacy-peer-deps --include=dev \
      && npm run build:prod; \
    fi

RUN rm -rf /app/dist-out \
    && if [ -d /app/dist-prod ]; then cp -R /app/dist-prod /app/dist-out; \
       elif [ -d /app/dist ]; then cp -R /app/dist /app/dist-out; \
       else echo "no dist output found" >&2; exit 1; fi

FROM nginx:1.25-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist-out /usr/share/nginx/html
