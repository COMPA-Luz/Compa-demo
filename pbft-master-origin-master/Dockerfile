# 使用Ubuntu 20.04作为基础镜像
FROM ubuntu:20.04

# 设置环境变量以避免安装过程中的任何交互式提示
ENV DEBIAN_FRONTEND=noninteractive

# 替换软件源（以下以阿里云为例，根据您的地理位置选择合适的镜像源）
RUN sed -i 's/http:\/\/archive.ubuntu.com/http:\/\/mirrors.aliyun.com/g' /etc/apt/sources.list && \
    sed -i 's/http:\/\/security.ubuntu.com/http:\/\/mirrors.aliyun.com/g' /etc/apt/sources.list

# 更新软件包列表，并安装Java运行环境
RUN apt-get update && \
    apt-get install -y --fix-missing openjdk-8-jre-headless && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*


# 设置工作目录
WORKDIR /app

# 将当前目录下的所有文件复制到工作目录
COPY . /app

# 给脚本文件赋予执行权限
RUN chmod +x /app/start_pbft.sh

# 容器启动时执行脚本
CMD ["/app/start_pbft.sh"]
