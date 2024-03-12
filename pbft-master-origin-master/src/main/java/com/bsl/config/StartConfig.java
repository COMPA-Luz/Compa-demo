package com.bsl.config;

import cn.hutool.core.io.file.FileReader;
import com.alibaba.fastjson.JSON;
import com.bsl.dao.bean.ReplayJson;
import com.bsl.dao.node.Node;
import com.bsl.dao.node.NodeAddress;
import com.bsl.dao.node.NodeBasicInfo;
import lombok.extern.slf4j.Slf4j;
import org.tio.client.ClientChannelContext;
import org.tio.server.ServerTioConfig;
import org.tio.server.TioServer;
import org.tio.server.intf.ServerAioHandler;
import org.tio.server.intf.ServerAioListener;
import com.bsl.p2p.P2PConnectionMsg;
import com.bsl.p2p.common.Const;
import com.bsl.p2p.server.P2PServerAioHandler;
import com.bsl.p2p.server.ServerListener;
import com.bsl.util.ClientUtil;
import com.bsl.util.PbftUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**

 *
 * @author: kevin
 * @data: 2020/2/13 下午7:20
 * @description: 启动的时候需要进行的配置
 * 在这里会进行下面的操作：
 * 初始化网络链接 使用tio构建P2P网络
 */
@Slf4j
public class StartConfig {
    private Node node = Node.getInstance();
    public static String basePath;

    /**
     * 初始化操作
     *
     * @return 成功返回true，other false
     */
    public boolean startConfig() {
        return initAddress() && initServer() && initClient() && initQuorum();
    }

    /**
     * 开启客户端
     *
     * @return
     */
    private boolean initClient() {
        P2PConnectionMsg.CLIENTS = new ConcurrentHashMap<>(AllNodeCommonMsg.allNodeAddressMap.size());

        // allNodeAddressMap保存了结点index和address信息。
        for (NodeBasicInfo basicInfo : AllNodeCommonMsg.allNodeAddressMap.values()) {
            NodeAddress address = basicInfo.getAddress();
            log.info(String.format("节点%d尝试链接%s", node.getIndex(), basicInfo));
            ClientChannelContext context = ClientUtil.clientConnect(address.getIp(), address.getPort());
            if (context != null) {
                // 将通道进行保存
                ClientUtil.addClient(basicInfo.getIndex(), context);
            } else {
                log.warn(String.format("结点%d-->%s：%d连接失败", basicInfo.getIndex(), address.getIp(), address.getPort()));
            }
        }
//        log.info("client链接服务端的数量" + P2PConnectionMsg.CLIENTS.size());
        return true;
    }


    /**
     * 开启服务端
     *
     * @return
     */
    private boolean initServer() {
        // 处理消息
        ServerAioHandler handler = new P2PServerAioHandler();
        // 监听
        ServerAioListener listener = new ServerListener();
        // 配置
        ServerTioConfig config = new ServerTioConfig("服务端", handler, listener);
        // 设置timeout，如果一定时间内client没有消息发送过来，则断开与client的连接
        config.setHeartbeatTimeout(Const.TIMEOUT);
        TioServer tioServer = new TioServer(config);
        try {
            // 启动
            log.info("服务端启动"  + node.getAddress().getIp() + node.getAddress().getPort());
            tioServer.start(node.getAddress().getIp(), node.getAddress().getPort());
        } catch (IOException e) {
            log.error("服务端启动错误！！" + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * 获得结点的ip地址
     *
     * @return 成功返回true
     */
    private boolean initAddress() {
        FileReader fileReader = new FileReader(PbftUtil.ipJsonPath);
        List<String> ipJsonStr = fileReader.readLines();
        for (String s : ipJsonStr) {
            ReplayJson replayJson = JSON.parseObject(s, ReplayJson.class);
            NodeAddress nodeAddress = new NodeAddress();
            nodeAddress.setIp(replayJson.getIp());
            nodeAddress.setPort(replayJson.getPort());
            NodeBasicInfo nodeBasicInfo = new NodeBasicInfo();
            nodeBasicInfo.setAddress(nodeAddress);
            nodeBasicInfo.setIndex(replayJson.getIndex());
            AllNodeCommonMsg.allNodeAddressMap.put(replayJson.getIndex(), nodeBasicInfo);
            AllNodeCommonMsg.publicKeyMap.put(replayJson.getIndex(), replayJson.getPublicKey());
        }
        // 将自己节点信息写入文件
        if (AllNodeCommonMsg.allNodeAddressMap.values().size() < 3 && !AllNodeCommonMsg.allNodeAddressMap.containsKey(node.getIndex())) {
            PbftUtil.writeIpToFile(node);
        }
        if (AllNodeCommonMsg.allNodeAddressMap.containsKey(node.getIndex())) {
            log.warn("已经存在此节点");
            return false;
        }
//        log.info(String.format("ip.json文件数量%s", ipJsonStr.size()));
//        log.info(String.format("内存地址%s", AllNodeCommonMsg.allNodeAddressMap.values().size()));
        return AllNodeCommonMsg.allNodeAddressMap.values().size() == ipJsonStr.size();
    }
//    private boolean initAddress() {
//        FileReader fileReader = new FileReader(PbftUtil.ipJsonPath);
//        List<String> ipJsonStr = fileReader.readLines();
//
//        int initialSize = AllNodeCommonMsg.allNodeAddressMap.size(); // 记录初始节点数量
//
//        for (String s : ipJsonStr) {
//            ReplayJson replayJson = JSON.parseObject(s, ReplayJson.class);
//            int nodeIndex = replayJson.getIndex();
//            NodeAddress nodeAddress = new NodeAddress();
//            nodeAddress.setIp(replayJson.getIp());
//            nodeAddress.setPort(replayJson.getPort());
//
//            NodeBasicInfo nodeBasicInfo = new NodeBasicInfo();
//            nodeBasicInfo.setAddress(nodeAddress);
//            nodeBasicInfo.setIndex(nodeIndex);
//
//            AllNodeCommonMsg.allNodeAddressMap.put(nodeIndex, nodeBasicInfo);
//            AllNodeCommonMsg.publicKeyMap.put(nodeIndex, replayJson.getPublicKey());
//        }
//        //
//        if (AllNodeCommonMsg.allNodeAddressMap.containsKey(node.getIndex())) {
//            log.info("节点已存在，进行替换: {}", node.getIndex());
//            AllNodeCommonMsg.allNodeAddressMap.remove(node.getIndex());
//            AllNodeCommonMsg.publicKeyMap.remove(node.getIndex());
//        }else {
//            // 将自己节点信息写入文件
//            PbftUtil.writeIpToFile(node);
//        }
//
//        int finalSize = AllNodeCommonMsg.allNodeAddressMap.size(); // 记录最终节点数量
//        int addedNodes = finalSize - initialSize; // 计算新增节点数量
//
//        log.info(String.format("ip.json中节点数量%s", ipJsonStr.size()));
//        log.info("新增节点数量: {}", addedNodes);
//        return AllNodeCommonMsg.allNodeAddressMap.values().size() == ipJsonStr.size();
//    }


    /**
     * 初始化节点清洗模块
     *
     * @return
     */
    private boolean initQuorum() {
        List<Integer> activeNodeList = AllNodeCommonMsg.activeNodeList;
        List<Integer> serialNumberList = AllNodeCommonMsg.serialNumberList;
        List<Integer> backupNodeList = AllNodeCommonMsg.backupNodeList;
        List<Integer> sortedKeys = new ArrayList<>(AllNodeCommonMsg.allNodeAddressMap.keySet());
        Collections.sort(sortedKeys); // 按照键的升序排序

        int count = 0; // 计数器
        for (Integer key : sortedKeys) {
            serialNumberList.add(key);
            if (count < AllNodeCommonMsg.quorumSize) {
                //活动节点列表（ActiveNodeList）：这个数据结构用于跟踪当前活动的节点。
                //逻辑编号就是list中的位置 0 1 2 3
                //       对应实际节点编号 A B C D
                activeNodeList.set(count,key); // 达到指定数量，跳出循环
                count++;
            }else{
                //备份节点列表（BackupNodeList）：这个数据结构用于存储备份节点的信息。
                //存储实际编号 A B C D
                backupNodeList.add(key);
            }
        }
        if (count < AllNodeCommonMsg.quorumSize) {
            //活动节点列表（ActiveNodeList）：这个数据结构用于跟踪当前活动的节点。
            //逻辑编号就是list中的位置 0 1 2 3
            //       对应实际节点编号 A B C D
            activeNodeList.set(count,node.getIndex()); // 达到指定数量，跳出循环
        }else{
            //备份节点列表（BackupNodeList）：这个数据结构用于存储备份节点的信息。
            //存储实际编号 A B C D
            backupNodeList.add(node.getIndex());
        }
//        log.info(String.format("节点列表更新完毕，活动节点列表%s，备份节点列表%s",
//                Arrays.toString(activeNodeList.toArray()),Arrays.toString(backupNodeList.toArray()) ));


        return true;
    }
}
