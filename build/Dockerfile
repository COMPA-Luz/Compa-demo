FROM amd64/php:7.4.19-apache
ENV APACHE_DOCUMENT_ROOT /code/www
RUN apt-get update && apt-get install -y --no-install-recommends \
  curl \
  git \
  libmemcached-dev \
  libpng-dev \
  unzip \
  zlib1g-dev \
  libonig-dev \
  && rm -rf /var/lib/apt/lists/*

  # ... 前面的安装依赖等命令

# RUN docker-php-ext-install -j5 gd mbstring mysqli pdo pdo_mysql sockets

# # 安装 PECL 扩展
# RUN pecl install memcached redis \
#     && echo "extension=memcached.so" >> $(php --ini | grep "Loaded Configuration" | sed -e "s|.*:\s*||") \
#     && echo "extension=redis.so" >> $(php --ini | grep "Loaded Configuration" | sed -e "s|.*:\s*||") \
#     && docker-php-ext-enable memcached redis xdebug
# 安装并配置 Xdebug（确保指定正确的版本）
# RUN pecl install docker-php-ext-enable memcached redis xdebug
RUN docker-php-ext-install -j5 gd mbstring mysqli pdo pdo_mysql sockets \
  && pecl install memcached redis xdebug \
  && echo 'extension=memcached.so' >> /usr/local/etc/php/php.ini \
  && echo 'extension=redis.so' >> /usr/local/etc/php/php.ini \
  && echo 'zend_extension=xdebug.so' >> /usr/local/etc/php/php.ini \
  && docker-php-ext-enable memcached redis xdebug \
RUN curl -sS https://getcomposer.org/installer | php -- --install-dir=/usr/local/bin --filename=composer
RUN a2dismod mpm_event && a2enmod mpm_prefork
COPY apache2.conf /etc/apache2/
COPY mpm_prefork.conf /etc/apache2/mod_available/
COPY startup.sh /

# Debug Setup
RUN echo 'zend_extension="/usr/local/lib/php/extensions/no-debug-non-zts-20151012/xdebug.so"' >> /usr/local/etc/php/php.ini
RUN echo 'xdebug.remote_enable=1' >> /usr/local/etc/php/php.ini
RUN echo 'xdebug.remote_connect_back=1' >> /usr/local/etc/php/php.ini
RUN echo 'xdebug.mode=debug' >> /usr/local/etc/php/php.ini
RUN echo 'xdebug.start_with_request=yes' >> /usr/local/etc/php/php.ini
RUN echo 'xdebug.client_port=9000' >> /usr/local/etc/php/php.ini
RUN echo 'xdebug.client_host=host.docker.internal' >> /usr/local/etc/php/php.ini


ENTRYPOINT ["/startup.sh"]
CMD ["php", "-S", "0.0.0.0:8732", "-t", "/code/www"]
