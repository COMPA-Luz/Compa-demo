package com.bsl.util;

import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.bsl.config.AllNodeCommonMsg;
import com.bsl.dao.node.Node;
import com.bsl.dao.pbft.MsgType;
import com.bsl.dao.pbft.PbftMsg;
import com.bsl.dao.vote.VoteBasicMsg;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**

 *
 * @author: kevin
 * @data: 2020/2/25 下午6:27
 * @description: 进行加密和签名的pbftmsg工具类
 */
@Slf4j
public class MsgUtil {
    /**
     * 直接的RSA
     */
    private static RSA selfRsa = new RSA(Node.getInstance().getPrivateKey(), Node.getInstance().getPublicKey());

    private static Map<Integer, String> publicKeyMap = AllNodeCommonMsg.publicKeyMap;
    /**
     * 更新投票信息
     *
     */
    public static void updateVoteMsg(PbftMsg msg) {
        //本地[<1,true>,<>,<>,<>]
        //收到[<0,true>,<>,<>,<>]
        //更新接收到的节点的选择
        //事件唯一编号
        String id = msg.getId();
        //监控的目标容器进程号
        String eventId = msg.getEventId();
        // 1. 查询本地的 AllNodeCommonMsg.allNodeState，找到与 msg 中事件ID对应的结果列表
        //[<1,true>,<>,<>,<>]
        List<VoteBasicMsg> defaultValue = new ArrayList<>();
        //对于这个进程的我的判断
        boolean myChoose;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        while(!AllNodeCommonMsg.haveUpdate){
//            try {
//                if( AllNodeCommonMsg.eventHasDone.get(msg.getId()) >1){
//                    break;
//                }
//                System.out.println("执行到cc");
//                Thread.sleep(1000); // 等待 1 秒钟
//            } catch (InterruptedException e) {
//                // 处理异常
//            }
//        }
        myChoose = AllNodeCommonMsg.myChoice.get(eventId);
//        System.out.println("读取到内容b "+ myChoose);
        defaultValue.add(new VoteBasicMsg(Node.getInstance().getIndex(),myChoose ));
        //新建一个[<1,true>,<>,<>,<>]
        //事件id对应表决
        List<VoteBasicMsg> localResultList = AllNodeCommonMsg.allNodeState.getOrDefault(id, defaultValue);
        //最终效果是收到广播对应的<id: [<1,true>,<>,<>,<>]> 被更新到map allNodeState中
        //localResultList: 本地[<1,true>,<>,<>,<>]
        // 2. 将本地的结果与接收到的结果合并
        //收到的结果: [<0,true>,<>,<>,<>]
        List<VoteBasicMsg> receivedResultList = msg.getNodeStates();
        //收到[<0,true>,<2,true>,<>,<>]
        //收到了eventId对应的表决结果List<VoteBasicMsg>
//        boolean useful = false;

        for (VoteBasicMsg receivedResult : receivedResultList) {
            boolean replaced = false;
            if(receivedResult.getNodeId() ==  msg.getNode()){
                //只记录收到的消息中来源节点的内容
                //[<0,true>]
//                useful = true;
                // 3. 更新本地的 AllNodeCommonMsg.allNodeState
                //[<0,true>,<1,true>,<>,<>]
                //[<0,true>,<2,true>,<>,<>]
                //[<0,true>,<3,true>,<>,<>]
                for(int i = 0; i < localResultList.size(); i++){
                    //已经有了就替换成新的
                    if(localResultList.get(i).getNodeId()== receivedResult.getNodeId()){
                        localResultList.set(i,receivedResult);
                        replaced = true;
                        break;
                    }
                }
                //没有就放进去
                if(!replaced){
                    localResultList.add(receivedResult);
                    break;
                }
                //本地[<1,true>,<0,true>,<2,true>,<>]
                //本地[<1,true>,<0,true>,<2,true>,<3,true>]
            }
        }

//        if (!useful) {
//            //收到的消息中没有来源节点的投票,则此消息无效
//            return;
//        }
        //有来源节点的投票,那么就是说会更新此消息
        //[<0,true>,<1,true>,<>,<>]
        AllNodeCommonMsg.allNodeState.put(id,localResultList);
        msg.setNodeStates(AllNodeCommonMsg.allNodeState.get(id));

//        System.out.println("update中的当前列表: " + AllNodeCommonMsg.allNodeState.get(id).toString());
    }


    /**
     * 确定表决结果
     */
    public static void countTrueFalse(PbftMsg msg) {
        List<VoteBasicMsg> resultList = msg.getNodeStates();
        int trueCount = 0;
        int falseCount = 0;

        for (VoteBasicMsg result : resultList) {
            if (result.isVote()) {
                trueCount++;
            } else {
                falseCount++;
            }
        }
        boolean result =  trueCount > falseCount;
        msg.setPassOrNot(result);
    }

    //打印列表
    public static void tablePrinter(List<VoteBasicMsg> voteList){
//        String json = "[{\"nodeId\":0,\"vote\":true},{\"nodeId\":1,\"vote\":true},{\"nodeId\":2,\"vote\":true},{\"nodeId\":4,\"vote\":true}]";
//        List<VoteBasicMsg> voteList = JSON.parseArray(json, VoteBasicMsg.class);

        int numRows = 2;
        int numCols = voteList.size() + 1;

        List<String[]> tableData = new ArrayList<>();
        String[] headerRow = new String[numCols];
        String[] voteRow = new String[numCols];

        headerRow[0] = "节点编号";
        voteRow[0] = "表决结果";

        for (int i = 0; i < voteList.size(); i++) {
            VoteBasicMsg vote = voteList.get(i);
            headerRow[i + 1] = String.valueOf(vote.getNodeId());
            voteRow[i + 1] = vote.isVote() ? "放行" : "拒止";
        }

        tableData.add(headerRow);
        tableData.add(voteRow);

        printTable(tableData);
    }
    //打印列表
    public static void nodePrinter(){
//        System.out.println("更新活动节点列表："+ Arrays.toString(AllNodeCommonMsg.activeNodeList.toArray())
//                +"，更新备份节点列表：" + Arrays.toString(AllNodeCommonMsg.backupNodeList.toArray()));
        System.out.println("节点列表更新：");
        List<Integer> activeNodeList = AllNodeCommonMsg.activeNodeList;
        List<Integer> backupNodeList = AllNodeCommonMsg.backupNodeList;
        int numCols1 = activeNodeList.size() + 1;
        int numCols2 = backupNodeList.size() + 1;
        String[] activeNodes = new String[numCols1];
        String[] backupNodes = new String[numCols2];
        activeNodes[0] = "活跃节点列表";
        backupNodes[0] = "备份节点列表";
        for (int i = 0; i < activeNodeList.size(); i++) {
            activeNodes[i + 1] = String.valueOf(activeNodeList.get(i));
        }
        for (int i = 0; i < backupNodeList.size(); i++) {
            backupNodes[i + 1] = String.valueOf(backupNodeList.get(i));
        }


        for(int j = 0 ; j< activeNodes.length-1;j++){
            System.out.print("+" + repeatChar('-', activeNodes.length + 3));
        }
        System.out.println("+");

        //打印活跃节点列表
        System.out.print(" | ");
        for(String s : activeNodes){
            System.out.print(s);
            System.out.print(" | ");
        }
        System.out.println();
        for(int j = 0 ; j< activeNodes.length-1;j++){
            System.out.print("+" + repeatChar('-', activeNodes.length + 3));
        }
        System.out.println("+");
        //打印备份节点列表
        System.out.print(" | ");
        for(String s : backupNodes){
            System.out.print(s);
            System.out.print(" | ");
        }
        System.out.println();
        for(int j = 0 ; j< activeNodes.length-1;j++){
            System.out.print("+" + repeatChar('-', activeNodes.length + 3));
        }
        System.out.println("+");


    }

    public static void printTable(List<String[]> tableData) {
        int numRows = tableData.size();
        int numCols = tableData.get(0).length;
        int[] colWidths = new int[numCols];

        // Calculate column widths
        for (String[] row : tableData) {
            for (int j = 0; j < numCols; j++) {
                int cellWidth = row[j].length();
                if (cellWidth > colWidths[j]) {
                    colWidths[j] = cellWidth;
                }
            }
        }
        // Print table
        printHorizontalLine(colWidths);
        for (int i=0; i<tableData.size();i++) {
            String[] row = tableData.get(i);
            printTableRow(row, colWidths,i);
            printHorizontalLine(colWidths);
        }
    }

    public static void printTableRow(String[] row, int[] colWidths,int why) {
        System.out.print("| ");
        for (int i = 0; i < row.length; i++) {
            if (i== 0 || why==1){
                String cell = row[i];
                System.out.print(padRight(cell, colWidths[i]));
                System.out.print(" | ");
                continue;
            }
            String cell = row[i];
            System.out.print(" ");
            System.out.print(padRight(cell, colWidths[i]));
            System.out.print(" | ");

        }
        System.out.println();
    }
    public static void printHorizontalLine(int[] colWidths) {
        for (int width : colWidths) {
            System.out.print("+" + repeatChar('-', width + 5));
        }
        System.out.println("+");
    }

    public static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }
    public static String repeatChar(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }


    public static boolean getResult(PbftMsg msg) {
        List<VoteBasicMsg> resultList = AllNodeCommonMsg.allNodeState.get(msg.getId());
        int trueCount = 0;
        int falseCount = 0;

        for (VoteBasicMsg result : resultList) {
            if (result.isVote()) {
                trueCount++;
            } else {
                falseCount++;
            }
        }
        if(trueCount == falseCount){
            System.out.println("出现平票，无法做出选择");
        }
        return  trueCount > falseCount;
    }
    /**u
     * 使用自己的私钥进行签名
     *
     * @param msg
     */
    public static void signMsg(PbftMsg msg) {
        String hash = String.valueOf(msg.hashCode());
        String sign = selfRsa.encryptBase64(hash, KeyType.PrivateKey);
        msg.setSign(sign);
    }

    /**
     * 使用对方的公钥进行加密
     *
     * @param msg
     */
    private static boolean encryptMsg(int index, PbftMsg msg) {
        String publicKey;
        if ((publicKey = publicKeyMap.get(index)) == null) {
            log.error("对方公钥为空");
            return false;
        }

        if (msg.getBody() == null) {
            log.warn("消息体为空，不进行加密");
            return true;
        }

        RSA encryptRsa;
        try {
            encryptRsa = new RSA(null, publicKey);
        } catch (Exception e) {
            log.error("使用对方公钥创建RSA对象失败");
            return false;
        }

        // 进行加密
        msg.setBody(encryptRsa.encryptBase64(msg.getBody(), KeyType.PublicKey));
        return true;
    }

    /**
     * 使用自己的私钥进行解密
     *
     * @param msg
     */
    public static boolean decryptMsg(PbftMsg msg) {
        if (msg.getBody() == null) {
            log.warn("消息体为空，不进行解密");
            return true;
        }
        String body;
        try {
            body = selfRsa.decryptStr(msg.getBody(), KeyType.PrivateKey);
//            System.out.println("收到消息"+body);
        } catch (Exception e) {
//            log.error(String.format("私钥解密失败!%s", e.getMessage()));
            return false;
        }
        msg.setBody(body);
        return true;
    }

    /**
     * 消息的前置处理：
     * ①：进行加密
     * ②：进行签名
     *
     * @param index
     * @param msg
     * @return
     */
    public static boolean preMsg(int index, PbftMsg msg) {
        if (!encryptMsg(index, msg)) {
            return false;
        }
        signMsg(msg);
        return true;
    }

    /**
     * 对消息进行后置处理
     *
     * @param msg
     * @return
     */
    public static boolean afterMsg(PbftMsg msg) {
        if (!isRealMsg(msg) || !decryptMsg(msg)) {
            return false;
        }
        return true;
    }

    /**
     * 判断消息是否被改变
     * 首先判断签名是否有问题
     *
     * @param msg 解密之前的消息！！！！！
     * @return
     */
    public static boolean isRealMsg(PbftMsg msg) {
        String publicKey = publicKeyMap.get(msg.getNode());
        RSA rsa;
        try {
            rsa = new RSA(null, publicKey);
        } catch (Exception e) {
            log.error(String.format("创建公钥RSA失败%s", e.getMessage()));
            return false;
        }
        // 获得此时消息的hash值
        String nowHash = String.valueOf(msg.hashCode());

        String sign = msg.getSign();
        try {
            // 获得hash值
            String hash = rsa.decryptStr(sign, KeyType.PublicKey);
            if (nowHash.equals(hash)) {
                return true;
            }
        } catch (Exception e) {
            log.warn(String.format("验证签名失效%s", e.getMessage()));
        }
        return false;
    }

    public static void main(String[] args) {
//        AllNodeCommonMsg.activeNodeList.set(0,0);
//        AllNodeCommonMsg.activeNodeList.set(1,1);
//        AllNodeCommonMsg.activeNodeList.set(2,2);
//        AllNodeCommonMsg.backupNodeList.add(4);
//        nodePrinter();
        List<VoteBasicMsg> list = new ArrayList<>();
        list.add(new VoteBasicMsg(1, true ));
        list.add(new VoteBasicMsg(2, true ));
        list.add(new VoteBasicMsg(3, true ));
        list.add(new VoteBasicMsg(4, true ));
        list.add(new VoteBasicMsg(5, true ));
        list.add(new VoteBasicMsg(6, true ));
        tablePrinter(list);
    }
}
