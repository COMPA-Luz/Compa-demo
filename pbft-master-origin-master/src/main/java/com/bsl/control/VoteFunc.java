package com.bsl.control;

import com.bsl.config.AllNodeCommonMsg;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * @ClassName VoteFun
 * @Description 投票裁决
 * @Author ty
 * @Date 2023/5/10 22:57
 * @Version 1.0
 **/
public class VoteFunc {


    private void vote(){
//        String[] resultFiles = {"node-1.txt", "node-2.txt", "node-3.txt", "node-4.txt"};
        String[] resultFiles = new String[AllNodeCommonMsg.quorumSize];

        for (int i = 0; i < resultFiles.length; ) {
            int tmp = i +1;
            resultFiles[i] = tmp + ".txt";
        }

//        AllNodeCommonMsg.quorumSize
        int[] resultCounts = new int[10]; // 用于统计结果值的出现次数，假设结果值为0到9

        // 读取四个节点的结果文件并进行统计
        for (String file : resultFiles) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int result = Integer.parseInt(line);
                    resultCounts[result]++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 找到出现次数最多的结果值
        int maxCount = 0;
        int maxResult = -1;
        for (int i = 0; i < resultCounts.length; i++) {
            if (resultCounts[i] > maxCount) {
                maxCount = resultCounts[i];
                maxResult = i;
            }
        }

        System.out.println("最终结果为：" + maxResult);
    }

    public static void main(String[] args) {
        VoteFunc voteFunc = new VoteFunc();
        voteFunc.vote();
    }
}
