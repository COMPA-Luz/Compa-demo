#!/bin/bash

echo "start_pbft开始执行"

# 检查COUNTS环境变量是否已设置
if [ -z "$COUNTS" ]; then
    echo "COUNTS not set"
    exit 1
fi

# 创建日志文件夹确保其存在
mkdir -p logs

# 清空之前的PID记录文件
echo "" > java_pids.txt

for ((i=0; i<$COUNTS; i++))
do
    if [ $QUORUMS -gt 0 ] && [ $EVILS -gt 0 ]; then
        # 在后台运行Java进程，并将输出重定向到日志文件
        java -jar bsl_demo-jar-with-dependencies.jar $i malicious > logs/malicious_$i.log 2>&1 &
        # 获取刚启动的后台进程的PID，并保存
        echo $! >> java_pids.txt
        ((QUORUMS--))
        ((EVILS--))

    elif [ $QUORUMS -gt 0 ] && [ $EVILS -le 0 ]; then
        # 在后台运行Java进程，并将输出重定向到日志文件
        java -jar bsl_demo-jar-with-dependencies.jar $i normal > logs/normal_$i.log 2>&1 &
        # 获取刚启动的后台进程的PID，并保存
        echo $! >> java_pids.txt
        ((QUORUMS--))

    else
        # 在后台运行Java进程，并将输出重定向到日志文件
        java -jar bsl_demo-jar-with-dependencies.jar $i normal > logs/normal_$i.log 2>&1 &
        # 获取刚启动的后台进程的PID，并保存
        echo $! >> java_pids.txt
    fi
    # 为每个节点设置环境变量并启动
    # "normal":  "damaged": "malicious": "down"
    echo "Yeah! Node $i started."
done

echo "All nodes have been started."