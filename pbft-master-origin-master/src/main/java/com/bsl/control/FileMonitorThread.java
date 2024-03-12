package com.bsl.control;


import com.alibaba.fastjson.JSON;
import com.bsl.config.AllNodeCommonMsg;
import com.bsl.config.StartConfig;
import com.bsl.dao.node.Node;
import com.bsl.dao.pbft.MsgType;
import com.bsl.dao.pbft.PbftMsg;
import com.bsl.dao.vote.VoteBasicMsg;
import com.bsl.util.ClientUtil;
import com.bsl.util.ProcessUtil;
import com.bsl.util.StartPbft;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FileMonitorThread extends Thread {
    //
//    String a = "C:\\Users\\47661\\Desktop\\pbft-master-origin\\";

    Node node = Node.getInstance();
    //            "tracee-24-",
//            "bcc-",
//            "tracee-25-",
//            "tracee-25-"
    String[] file = new String[]{
        "bcc-",
        "bcc-",
        "bcc-",
        "bcc-",
    };

//    private String filePath = StartConfig.basePath +
//            files[AllNodeCommonMsg.activeNodeList.indexOf(node.getIndex())]+
//            AllNodeCommonMsg.monitorPid + ".txt";


//   private String filePath = a  + "node"+ fileId+ "_file.txt";
    private Long lastModifiedTime;
    public FileMonitorThread() {
        lastModifiedTime= 0L;
    }

    @Override
    public void run() {
        String filePath ;
        String[] files = StartPbft.expandArray(String.class,file);
        switch (AllNodeCommonMsg.test){

            //test模式
            case 0:
                int[] fileId = new int[]{1,2,3,4,5,6,7,8,9};
                filePath = "D:\\temp_workstation_Luz\\pbft-master-origin-master\\pbft-master-origin-master\\node-"+
                        fileId[AllNodeCommonMsg.activeNodeList.indexOf(node.getIndex())]
                        + ".txt";
                break;

            //bsl项目
            case 1:
                filePath = "/tmp/" +
                        files[AllNodeCommonMsg.activeNodeList.indexOf(node.getIndex())]+
                        AllNodeCommonMsg.monitorPid + ".txt";
                break;
            //COMPA模式
            default:
                int[] fileId2 = new int[]{1,2,3,4,5,6,7,8,9};
                filePath = "/app/node-"+
                        fileId2[AllNodeCommonMsg.activeNodeList.indexOf(node.getIndex())]
                        + ".txt";

        }

//        log.info("正在监测"+filePath);
        System.out.println("正在监测"+filePath);
        AllNodeCommonMsg.filePath = filePath;
        while (true) {
            try {
                Thread.sleep(1000); // 每隔1秒检查文件更新
                //获得逻辑编号
                int nodeId = node.getIndex();
                File file = new File(filePath);
                long currentModifiedTime = file.lastModified();

                if (currentModifiedTime > lastModifiedTime) {
                    Thread.sleep(1000);
                    lastModifiedTime = currentModifiedTime;
                    // 文件已更新，读取更新的内容
                    String updatedContent = readUpdatedContent(filePath);

                    AllNodeCommonMsg.timer.start();
                    // 创建包含所有文件更新内容的消息
//                    long timestamp = System.currentTimeMillis();
                    EventData eventData = new EventData(AllNodeCommonMsg.targetPid, updatedContent);

                    PbftMsg msg = new PbftMsg(MsgType.PRE_PREPARE, nodeId);

                    if (AllNodeCommonMsg.nodeStatus != null) {
                        System.out.println("当前节点为：" + AllNodeCommonMsg.nodeStatus.getDescription());
                    }
                    if (AllNodeCommonMsg.nodeStatus == AllNodeCommonMsg.NodeStatus.NORMAL) {
                        //正常节点
                        System.out.println("进行正常判断");
                        msg.setPassOrNot(eventData.getResult());
                    }else if (AllNodeCommonMsg.nodeStatus == AllNodeCommonMsg.NodeStatus.DOWN){
                        //宕机
                        System.out.println("默认选择拒止");
                        msg.setPassOrNot(false);
                    }else if (AllNodeCommonMsg.nodeStatus == AllNodeCommonMsg.NodeStatus.DAMAGED ||
                            AllNodeCommonMsg.nodeStatus == AllNodeCommonMsg.NodeStatus.MALICIOUS ){
                        //受损或恶意
                        System.out.println("默认选择放行");
                        msg.setPassOrNot(true);
                    }

                    msg.setBody(JSON.toJSONString(eventData));
                    //监控目标pid
                    msg.setEventId(eventData.getEventId());
                    //本地存储关于这个PID的我的选择
                    AllNodeCommonMsg.myChoice.put(eventData.getEventId(),msg.isPassOrNot());
                    AllNodeCommonMsg.haveUpdate = true;
                    // 调用ClientUtil.clientPublish(msg)处理更新的内容
//                    System.out.println("input str -> " + combinedContent);
                    //存储自己的表决结果

                    VoteBasicMsg v = new VoteBasicMsg(msg.getNode(),msg.isPassOrNot());
                    List<VoteBasicMsg> nodeStateList = new ArrayList<>();
                    nodeStateList.add(v);

                    AllNodeCommonMsg.allNodeState.put(msg.getId(), nodeStateList);

//                    msg.setAllNodeState(AllNodeCommonMsg.allNodeState);
                    msg.setNodeStates(nodeStateList);
                    System.out.println( "检测到输入:"+JSON.toJSON(msg.getNodeStates()));
//                    System.out.println( "当前手中的列表大小为"+AllNodeCommonMsg.getSizeOfAllNodeState(msg.getEventId() ));
                    //广播自己的结果
                    ClientUtil.prePrepare(msg);

//                    //测试
//                    nodeStateList.add(new VoteBasicMsg(0,true));
//                    nodeStateList.add(new VoteBasicMsg(2,true));
//                    nodeStateList.add(new VoteBasicMsg(2,true));
//                    System.out.println(AllNodeCommonMsg.getSizeOfAllNodeState(combinedContent.getEventId()));
//                    System.out.println(JSON.toJSONString(msg ));
//                    System.out.println(JSON.toJSONString(msg, SerializerFeature.PrettyFormat));

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private String readUpdatedContent(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return contentBuilder.toString();
    }


    public static void main(String[] args) {
//        int nodeIndex = Node.getInstance().getIndex();
//        String fileName = "node" + 1 + "_file.txt";
        // 创建并启动文件监控线程
        FileMonitorThread fileMonitorThread = new FileMonitorThread();
        fileMonitorThread.start();
    }
}
