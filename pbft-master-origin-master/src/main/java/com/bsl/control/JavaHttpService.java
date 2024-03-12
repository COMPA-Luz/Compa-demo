package com.bsl.control;
import com.alibaba.fastjson.JSON;
import com.bsl.config.AllNodeCommonMsg;
import com.bsl.dao.node.Node;
import com.bsl.dao.pbft.MsgType;
import com.bsl.dao.pbft.PbftMsg;
import com.bsl.dao.vote.VoteBasicMsg;
import com.bsl.util.ClientUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaHttpService extends Thread {

    @Override
    public void run() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(12424), 0);
            server.createContext("/process", new MyHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
            System.out.println("Server started on port " + 12424);

            // Keep the server running until it's interrupted
            try {
                while (!Thread.interrupted()) {
                    Thread.sleep(1000); // sleep to prevent excessive CPU usage
                }
            } catch (InterruptedException e) {
                System.out.println("Server interrupted");
            } finally {
                server.stop(0); // stop the server when interrupted
                System.out.println("Server stopped");
            }
        } catch (Exception e) {
            System.out.println("Server failed to start: " + e.getMessage());
        }
    }

    static class MyHandler implements HttpHandler {
        Node node = Node.getInstance();
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);
                String query = br.readLine();
                Map<String, String> params = parseQuery(query);

                boolean result = callAuth(params.get("str"), params.get("object"));


                String response = result ? "1" : "0"; // 如果result为true, response为"1", 否则为"0"


                AllNodeCommonMsg.timer.start();
                // 创建包含所有文件更新内容的消息
//                    long timestamp = System.currentTimeMillis();
                EventData eventData = new EventData(AllNodeCommonMsg.targetPid, response);

                PbftMsg msg = new PbftMsg(MsgType.PRE_PREPARE, node.getIndex());

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
                while (AllNodeCommonMsg.voteFinished == -1){
                    Thread.yield();
                }
                String response_final;
                if(AllNodeCommonMsg.voteFinished == 0) {
                    response_final = "false";
                } else{
                    response_final = "true";
                }
                exchange.sendResponseHeaders(200, response_final.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                String response = "Method Not Allowed";
                exchange.sendResponseHeaders(405, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    private static boolean callAuth(String str, String objectSerialized) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("php", "/app/callAuthMethod.php", str, objectSerialized);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return "true".equals(output.toString().trim());
            }
            System.err.println("PHP script exited with code " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static Map<String, String> parseQuery(String query) throws IOException {
        Map<String, String> result = new HashMap<>();
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], URLDecoder.decode(entry[1], StandardCharsets.UTF_8.name()));
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }
}




