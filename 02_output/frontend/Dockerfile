# Frontend image (architecture.md deploys_as; ADR-05, ADR-11). Build first: npm ci && npm run build
FROM nginx:1.30.6-alpine
# D-11/D-12: distribution package upgrade at build time
RUN apk upgrade --no-cache \
 && rm -f /etc/nginx/conf.d/default.conf \
 && chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx
COPY --chown=nginx:nginx dist/ /usr/share/nginx/html/
COPY nginx/nginx.conf.template /etc/nginx/nginx.conf.template
COPY --chmod=0755 nginx/start.sh /start.sh
# SB-11: non-root
USER nginx
EXPOSE 8080
ENTRYPOINT ["/start.sh"]
