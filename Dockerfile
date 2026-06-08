# syntax=docker/dockerfile:1
# Static site (plain HTML/CSS/JS) served by nginx. A container must listen on a
# port for ECS/App Runner to route traffic to it — a bare .html file can't be
# "deployed" on its own, so we wrap it in a tiny nginx web server.
FROM nginx:1.27-alpine

# Serve on nginx's native port 80; do NOT rewrite the config. The old sed-based
# port rewrite silently failed on newer nginx images, leaving nginx on 80 while
# the target group expected another port -> failed ELB health checks -> 502.
COPY . /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
