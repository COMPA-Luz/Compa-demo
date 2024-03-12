package com.bsl.util;

import com.bsl.config.AllNodeCommonMsg;
import com.bsl.config.StartConfig;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.json.JSONUtil;
import com.bsl.dao.bean.ReplayJson;
import com.bsl.dao.node.Node;
import com.bsl.dao.node.NodeBasicInfo;
import com.bsl.dao.pbft.PbftMsg;
import lombok.extern.slf4j.Slf4j;

/**

 *
 * @author: kevin
 * @data: 2020/2/15 下午10:38
 * @description: pbft算法的工具类
 */
@Slf4j
public class PbftUtil {

    private static boolean flag = true;

    public static String ipJsonPath = StartConfig.basePath+"oldIp.json";


    public static boolean checkMsg(PbftMsg msg) {

        return true;
    }
    // 定义替换函数（编号）
    //用节点newNode（实际编号）替换节点oldNode（逻辑编号）
    public void replaceNode(int oldNode,  int newNode) {
        //检查要替换的节点编号是否存在
        //是否在逻辑编号0 1 2 3 中
        if (0 <= oldNode && oldNode <= 3 ) {
            // 替换节点信息
            AllNodeCommonMsg.replacedNodeList.add(AllNodeCommonMsg.activeNodeList.get(oldNode));
            AllNodeCommonMsg.activeNodeList.set(oldNode, newNode);
            AllNodeCommonMsg.backupNodeList.remove(newNode);
            log.info("节点清洗完成");
        }else{
            log.info("节点清洗失败");
        }
    }


    public static void save(PbftMsg msg) {
//        log.info(String.format("文件写入%s", msg));
    }

    /**
     * 信息写入文件和AllNodeCommonMsg.allNodeAddressMap
     *
     * @param node
     */
    synchronized public static void writeIpToFile(Node node) {
        if (!flag) {
            return;
        }
//        log.info(String.format("节点%s写入文件", node.getIndex()));
        FileWriter writer = new FileWriter(ipJsonPath);
        ReplayJson replayJson = new ReplayJson();
        replayJson.setIndex(node.getIndex());
        replayJson.setIp(node.getAddress().getIp());
        replayJson.setPort(node.getAddress().getPort());
        replayJson.setPublicKey(node.getPublicKey());
        String json = JSONUtil.toJsonStr(replayJson);
        writer.append(json + "\n");
        flag = false;
    }


}
