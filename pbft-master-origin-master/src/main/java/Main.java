import com.bsl.config.AllNodeCommonMsg;
import com.bsl.config.StartConfig;
import com.bsl.control.FileMonitorThread;
import com.bsl.control.JavaHttpService;
import com.bsl.control.MapTrackerThread;
import com.bsl.control.StopWatch;
import com.bsl.dao.node.Node;
import com.bsl.dao.node.NodeAddress;
import com.bsl.dao.pbft.MsgType;
import com.bsl.dao.pbft.PbftMsg;
import com.bsl.util.ClientUtil;
import com.bsl.util.ProcessUtil;
import com.bsl.util.StartPbft;
import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;

import static com.bsl.util.DbUtil.deleteNodes;
import static com.bsl.util.PbftUtil.ipJsonPath;

/**
 *
 * @author: kevin
 * @data: 2020/1/22 下午2:46
 * @description: 程序运行开始类
 * 启动参数顺序：ip，port，index，认证请求消息
 */
@Slf4j
public class Main {
    static String ip;
    static int index;
    static int port;
    private static void ideaMod(String[] args){
        AllNodeCommonMsg.test = 0;
        int i =0 ;  //V0
        i= 1 ;  //V1
        i= 2 ;  //V2
        i= 3 ;  //V3
        i= 4 ;  //V4
        i= 5 ;  //V5
        i= 6 ;  //V6
        ip = "127.0.0.1";
        port = 18080 + i;
//        StartConfig.basePath = "E:\\pbft\\node"+ i +"\\";
        StartConfig.basePath = "D:\\temp_workstation_Luz\\pbft-master-origin-master\\pbft-master-origin-master\\";
        index = i;
        AllNodeCommonMsg.quorumSize = 4;
        //监控的容器进程
        AllNodeCommonMsg.targetPid = "2676774";
        //监控进程
        AllNodeCommonMsg.monitorPid = 1235553;
        //节点状态
//        String status = "normal";
//        String status = "damaged";
//        String status = "malicious";
//        String status = "down";
        String status = "normal";
        AllNodeCommonMsg.nodeStatus = AllNodeCommonMsg.getStatusFromArg(status);
        /* "normal":  "damaged": "malicious": "down":*/
        //是否开启互锁保护
        AllNodeCommonMsg.onOrOFF = true;
//        AllNodeCommonMsg.onOrOFF = false;
    }
    private static void jarMod(String[] args){
        AllNodeCommonMsg.test = 1;
        if (args.length != 7) {
            log.error("参数数量错误");
            return;
        }
//        程序启动ip地址
        ip = args[0];
//        程序启动index
        index = Integer.parseInt(args[1]);
        //        端口
        port = 18080 + index;
//        文件保存位置，在文件保存位置必须存在一个oldIp.json的文件
        StartConfig.basePath = args[2];
        //目标监控容器
        AllNodeCommonMsg.targetPid = args[3];
        //节点状态
        /* "normal":  "damaged": "malicious": "down":*/
        AllNodeCommonMsg.nodeStatus = AllNodeCommonMsg.getStatusFromArg(args[4]);
        //是否开启互锁保护
        String onOFF = args[5];
        AllNodeCommonMsg.onOrOFF = onOFF.equals("1");
        //中心网络节点数量
        AllNodeCommonMsg.quorumSize = Integer.parseInt(args[6]);
        /*
        使用jar包运行
        java -jar 包名 ip地址 端口号 序号 文件保存位置
        java -jar bsl_demo.jar 127.0.0.1 0 C:\\Users\\47661\\Desktop\\pbft-master-origin\\
        java -jar bsl_demo.jar 127.0.0.1 0 /tmp/javacode/bsl-demo/
        java -jar bsl_demo.jar 127.0.0.1 0 /home/ty/桌面/test/ 1
        java -jar bsl_demo.jar 127.0.0.1 0 /root/voteSystem/  67920  normal  1
        java -jar /home/ty/bsl/bsl_demo.jar 127.0.0.1 0 /home/ty/bsl/ 67920  normal  1

        */
    }

    private static void COMPAMod(String[] args) {
        if (args.length != 2) {
            log.error("参数数量错误");
            return;
        }
//        程序启动ip地址
        ip = "127.0.0.1";
//        程序启动index
        index = Integer.parseInt(args[0]);
        //        端口
        port = 18080 + index;
//        文件保存位置，在文件保存位置必须存在一个oldIp.json的文件
        StartConfig.basePath = "/app/";
        //目标监控容器
        AllNodeCommonMsg.targetPid = "2676774";
        //监控进程
        AllNodeCommonMsg.monitorPid = 1235553;
        //节点状态
        /* "normal":  "damaged": "malicious": "down":*/
        AllNodeCommonMsg.nodeStatus = AllNodeCommonMsg.getStatusFromArg(args[1]);
        //是否开启互锁保护
        AllNodeCommonMsg.onOrOFF = true;
//        AllNodeCommonMsg.onOrOFF = false;
        /*
        使用jar包运行
        java -jar 包名 ip地址 端口号 序号 文件保存位置
        java -jar bsl_demo.jar 127.0.0.1 0 C:\\Users\\47661\\Desktop\\pbft-master-origin\\
        java -jar bsl_demo.jar 127.0.0.1 0 /tmp/javacode/bsl-demo/
        java -jar bsl_demo.jar 127.0.0.1 0 /home/ty/桌面/test/ 1
        java -jar bsl_demo.jar 127.0.0.1 0 /root/voteSystem/  67920  normal  1
        java -jar /home/ty/bsl/bsl_demo.jar 127.0.0.1 0 /home/ty/bsl/ 67920  normal  1

        */
    }
    private static void vote_start(){
        //监控输入
//        int nodeIndex = Node.getInstance().getIndex() + 1;
//        String fileName = "node" + nodeIndex + "_file.txt";
        // 创建并启动文件监控线程
        FileMonitorThread fileMonitorThread = new FileMonitorThread();
        fileMonitorThread.start();
        // 创建结果监控线程
//        MapTrackerThread trackerThread = new MapTrackerThread(AllNodeCommonMsg.myChoice);
//        trackerThread.start();
        //java执行C


    }

    public static void main(String[] args) {
////   在IDEA运行
//      ideaMod(args);

////   使用jar包运行
//      jarMod(args);

//     COMPA适用版
        COMPAMod(args);

        Node node = Node.getInstance();
        node.setIndex(index);
        NodeAddress nodeAddress = new NodeAddress();
        nodeAddress.setIp(ip);
        nodeAddress.setPort(port);
        node.setAddress(nodeAddress);
        if(!StartPbft.start()){
            return;
        }

        // 程序退出时，Shutdown Hook会执行deleteNodes函数
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteNodes(index, ipJsonPath)));

        while (!AllNodeCommonMsg.activeNodeList.contains(index)) {
            try {
                System.out.println("Waiting for current node to become active...");
                Thread.sleep(1000); // 等待1秒后再次检查
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //java执行cmd
//        "python3 es_detector.py -U -n cat > /tmp/bcc_log.txt",
//        "while true; do echo \"python3 es_detector.py -U\"; sleep 1; done  > /tmp/bcc_log.txt 2>&1 &",
//        "python3 /root/bsl-escape/es_detector.py -U -n cat  > /tmp/node1_log.txt 2>&1 &",
//         "/root/tracee/dist/tracee-ebpf --output json --trace container --output option:detect-syscall --output option:parse-arguments --output option:exec-env --trace event=$(/root/tracee/dist/tracee-rules --rules TRC-24 --list-events) | /root/tracee/dist/tracee-rules --input-tracee format:json --input-tracee file:stdin --rules TRC-24  > /tmp/node1_log.txt 2>&1 &",
        String[] command = new String[]{
                "python3 /root/bsl-escape/es_detector.py -U -n cat  > /tmp/node1_log.txt 2>&1 &",
                "python3 /root/bsl-escape/es_detector.py -U -n cat  > /tmp/node2_log.txt 2>&1 &",
                "python3 /root/bsl-escape/es_detector.py -U -n cat  > /tmp/node3_log.txt 2>&1 &",
                "python3 /root/bsl-escape/es_detector.py -U -n cat  > /tmp/node4_log.txt 2>&1 &",
        };

        String[] commands = StartPbft.expandArray(String.class,command);
//        String[] authcommand = new String[]{
//                "python3 /root/bsl-escape/es_detector.py -U -n cat  > /tmp/node1_log.txt 2>&1 &",
//                "python3 /root/bsl-escape/es_detector.py -U -n cat  > /tmp/node2_log.txt 2>&1 &",
//                "python3 /root/bsl-escape/es_detector.py -U -n cat  > /tmp/node3_log.txt 2>&1 &",
//                "python3 /root/bsl-escape/es_detector.py -U -n cat  > /tmp/node4_log.txt 2>&1 &",
//        };
//
//        String[] authcommands = StartPbft.expandArray(String.class,command);
        //逻辑编号，对应执行那个命令
        int id = AllNodeCommonMsg.activeNodeList.indexOf(node.getIndex());

        switch (AllNodeCommonMsg.test){

            //测试用
            case 0:
                ProcessUtil.testExecuteCommand(commands[id]);
                vote_start();
                break;

            //bsl用
            case 1:
                ProcessUtil.executeCommand(commands[id],id);
                vote_start();
                break;
            //compa
            default:
                JavaHttpService httpService = new JavaHttpService();
                httpService.start();
        }
        //可以在这里发送消息
        Scanner input =new Scanner(System.in);
        while (true) {
            String str = input.next();
            System.out.println("input str -> " + str);
            PbftMsg msg = new PbftMsg(MsgType.PRE_PREPARE, 0);
            msg.setBody(str);
            AllNodeCommonMsg.timer.start();
            ClientUtil.prePrepare(msg);
        }

    }
}

