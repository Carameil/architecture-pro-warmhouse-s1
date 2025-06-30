# Multi-stage build for Symfony application
FROM composer:2.8 AS composer_stage
WORKDIR /app

# Production stage
FROM php:8.4-fpm-alpine

# Install system dependencies and PHP extensions
ADD --chmod=0755 https://github.com/mlocati/docker-php-extension-installer/releases/latest/download/install-php-extensions /usr/local/bin/

RUN apk add --no-cache --virtual .build-deps \
        build-base \
    && apk add --no-cache \
        nginx \
        curl \
    && install-php-extensions \
        opcache \
    && apk del .build-deps \
    && rm -rf /var/cache/apk/* /tmp/* /var/tmp/*

# Create user and directories
RUN addgroup -g 1000 app && adduser -u 1000 -G app -s /bin/sh -D app \
    && mkdir -p /var/log/nginx /var/lib/nginx /run/nginx /var/www/html \
    && chown -R app:app /var/www/html \
    && chown -R nginx:nginx /var/log/nginx /var/lib/nginx /run/nginx

# Configure PHP-FPM to output logs to stdout/stderr
RUN echo '[global]' > /usr/local/etc/php-fpm.d/docker.conf \
    && echo 'error_log = /proc/self/fd/2' >> /usr/local/etc/php-fpm.d/docker.conf \
    && echo '[www]' >> /usr/local/etc/php-fpm.d/docker.conf \
    && echo 'access.log = /proc/self/fd/2' >> /usr/local/etc/php-fpm.d/docker.conf \
    && echo 'clear_env = no' >> /usr/local/etc/php-fpm.d/docker.conf \
    && echo 'catch_workers_output = yes' >> /usr/local/etc/php-fpm.d/docker.conf

# Nginx configuration
COPY <<EOF /etc/nginx/http.d/default.conf
server {
    listen 8081;
    server_name localhost;
    root /var/www/html/public;
    index index.php;

    location / {
        try_files \$uri \$uri/ /index.php\$is_args\$args;
    }

    location ~ ^/index\.php(/|$) {
        fastcgi_pass 127.0.0.1:9000;
        fastcgi_split_path_info ^(.+\.php)(/.*)$;
        include fastcgi_params;
        fastcgi_param SCRIPT_FILENAME \$realpath_root\$fastcgi_script_name;
        fastcgi_param DOCUMENT_ROOT \$realpath_root;
        internal;
    }

    location ~ \.php$ {
        return 404;
    }
}
EOF

# Set working directory
WORKDIR /var/www/html

# Copy composer
COPY --from=composer_stage /usr/bin/composer /usr/local/bin/composer

# Create simple startup script
COPY <<EOF /usr/local/bin/start.sh
#!/bin/sh
# Start PHP-FPM in background
php-fpm -D

# Start Nginx in foreground
exec nginx -g 'daemon off;'
EOF

RUN chmod +x /usr/local/bin/start.sh

EXPOSE 8081

# startup
CMD ["/usr/local/bin/start.sh"] 