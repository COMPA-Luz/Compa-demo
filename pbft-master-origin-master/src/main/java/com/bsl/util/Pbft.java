package com.bsl.util;

import com.bsl.config.AllNodeCommonMsg;
import com.bsl.dao.node.Node;
import com.bsl.dao.pbft.MsgType;
import com.bsl.dao.pbft.PbftMsg;
import lombok.extern.slf4j.Slf4j;

/**

 *
 * @author: kevin
 * @data: 2020/1/22 下午3:53
 * @description: pbft的工作流程
 * this is the most important thing
 * 这个是整个算法的流程
 */
@Slf4j
public class Pbft {

    private Node node = Node.getInstance();

    /**
     * 发送view请求
     *
     * @return
     */
    public boolean pubView() {

        TestUtil.startTime = System.currentTimeMillis();
        /**
         * 如果区块链中的网络节点小于3
         */
        if (AllNodeCommonMsg.allNodeAddressMap.size() < 3) {
            log.warn("网络中节点数："+ AllNodeCommonMsg.allNodeAddressMap.size());
            node.setViewOK(true);
            // 将节点消息广播出去
            ClientUtil.publishIpPort(node.getIndex(), node.getAddress().getIp(), node.getAddress().getPort());
            return true;
        }

        log.info("结点开始进行view同步操作");
        // 初始化view的msg
        PbftMsg view = new PbftMsg(MsgType.GET_VIEW, node.getIndex());
        ClientUtil.clientPublish(view);
        return true;
    }

    /**
     * 视图发送该表
     *
     * @return
     */
    public boolean changeView() {

        return true;
    }
}
