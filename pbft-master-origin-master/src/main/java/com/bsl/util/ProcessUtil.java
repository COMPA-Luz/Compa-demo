package com.bsl.util;

import com.bsl.config.AllNodeCommonMsg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProcessUtil {
    public static void testExecuteCommand(String command) {
        AllNodeCommonMsg.monitorPid = 1221212;
        System.out.println("监控模块进程号 PID: " + AllNodeCommonMsg.monitorPid);

    }
    //执行进程
    public static void executeCommand(String command, int id) {
        try {
            System.out.println("执行监控进程："+command);
//            ProcessBuilder builder = new ProcessBuilder("gnome-terminal", "--", "bash", "-c", command);
            ProcessBuilder builder = new ProcessBuilder("/bin/bash", "-c", command);
            Process process = builder.start();
//            Thread.sleep(3000);
            // 获取进程ID
//            AllNodeCommonMsg.monitorPid = getPid(process,id);
            List<Long> outputLines = findCommand(id);
            //有bcc的时候那个节点不用动
//            if(id == 1){
//                AllNodeCommonMsg.monitorPid = outputLines.get(0);
//            }else {
//                AllNodeCommonMsg.monitorPid = outputLines.get(id-1) +1;
//            }
            //没有bcc
            System.out.println("监控模块进程号 PID: " + outputLines.get(id));
            //bcc直接用
            AllNodeCommonMsg.monitorPid = outputLines.get(id);
            //tracee得+1
//            AllNodeCommonMsg.monitorPid = outputLines.get(id) +1;

            // 等待命令执行完成
            int exitCode = process.waitFor();
            System.out.println("Command exited with code " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // kill进程
    public static void testTerminateProcess(int pid) {
        System.out.println("Process terminated successfully." + pid);
    }
    // kill进程
    public static void terminateProcess(int pid) {
        try {
            String command = "kill " + pid;
            ProcessBuilder builder = new ProcessBuilder("bash", "-c", command);
            System.out.println("目标进程："+ pid);
            Process process = builder.start();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Process terminated successfully.");
            } else {
                System.out.println("Failed to terminate process.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void COMPAfeedbackProcess(boolean result) {
        AllNodeCommonMsg.voteFinished = result ? 1 : 0 ;
        System.out.println("Process terminated successfully.");
    }

    // 获取进程ID
//     "pgrep -f '/root/tracee/dist/tracee-rules --input-tracee format:json --input-tracee file:stdin --rules TRC-24'",
//    "pgrep -f 'python3 /root/bsl-escape/es_detector.py -U'",
    private static List<Long> findCommand(int id) throws IOException {
        String[] findCommands = new String[]{
                "pgrep -f 'python3 /root/bsl-escape/es_detector.py -U'",
                "pgrep -f 'python3 /root/bsl-escape/es_detector.py -U'",
                "pgrep -f 'python3 /root/bsl-escape/es_detector.py -U'",
                "pgrep -f 'python3 /root/bsl-escape/es_detector.py -U'"
        };

        List<Long> outputLines = new ArrayList<>();
        System.out.println("查询进程PID："+findCommands[id]);
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", findCommands[id]);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            long pid = Long.parseLong(line.trim());
            outputLines.add(pid);
        }

        reader.close();
        return outputLines;
    }


    public static void main(String[] args) {
        String command = "tail -F /home/ty/桌面/test/a.txt";
        executeCommand(command,0);
    }
}
