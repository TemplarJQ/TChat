package com.templar.handler;

import com.templar.param.MsgType;
import com.templar.thread.ClientSocketThread;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;

public class Client {

    private static final Logger logger = Logger.getLogger(Client.class);

    private String host;
    private int port;
    private int retry;         // 最大重连次数
    private Socket socket;
    private InputStream is;
    private OutputStream os;
    private boolean alive;
    private boolean chatting;  // 是否处于聊天模式
    private String chatRoom;   // 存储当前属于的chat room


    /** Public Methods */

    public Client() {
        initFromProperties();
        addShutdownHook();
    }

    public void connect() {
        try {
            socket = new Socket(host, port);
            is = socket.getInputStream();
            os = socket.getOutputStream();
            alive = true;
            chatting = false;
            chatRoom = "";
            Thread socketThread = new ClientSocketThread(this);
            socketThread.start();
            logger.info("Connect to the chat room successfully");
            printInfo();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收用户终端输入
     */
    public void startGetUserInput() {
        Scanner scanner = new Scanner(System.in);
        while (alive) {
            String msg = scanner.nextLine();
            if (alive) { // 双重校验，可能在阻塞时候，服务器socket断开连接了
                handlerMsg(msg);
            } else {
                reconnect();
            }
        }

        logger.info("stop get user input");
    }

    public Socket getSocket() {
        return socket;
    }

    public InputStream getIs() {
        return is;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public boolean isChatting() {
        return chatting;
    }

    public void setChatting(boolean chatting) {
        this.chatting = chatting;
    }

    public String getChatRoom() {
        return chatRoom;
    }

    public void setChatRoom(String chatRoom) {
        this.chatRoom = chatRoom;
    }

    /** Private Methods */

    /**
     * 根据用户的不同输入选择不同操作
     */
    private void handlerMsg(String msg) {
        if (msg == null || "".equals(msg)) {
            return;
        }

        if (chatting) { // 处于聊天模式,直接将用户输入
            handlerMsgInChattingMode(msg);
            return;
        }

        if (msg.length() < 2 || msg.length() > 1000 || msg.charAt(0) != '#') {
            logger.info("无效操作!");
            printInfo();
            return;
        }


        char type = msg.charAt(1);
        handlerMsgWithType(type, msg.substring(1));
    }

    /**
     * 根据不同的消息类型，向socket写入不同的信息
     */
    private void handlerMsgWithType(char type, String msg) {
        if (type == MsgType.LIST_ROOM ) {
            sendMsg(type);
            return;
        }

        if (type == MsgType.QUIT_ROOM) {
            if ("".equals(chatRoom)) {
                logger.info("当前不在任何一个聊天室！");
                return;
            }

            sendMsg(type);
            return;
        }

        if (type == MsgType.JOIN_ROOM ) {
            if (!"".equals(chatRoom)) {
                logger.info("已经加入聊天室，请先退出~");
                return;
            }

            sendMsg(msg.replaceAll(" +", ""));  //去掉空格
            return;
        }

        if (type == MsgType.CREATE_ROOM) {
            if (!"".equals(chatRoom)) {
                logger.info("已经加入聊天室，请先退出~");
                return;
            }

            sendMsg(msg.replaceAll(" +", ""));  //去掉空格
            return;
        }

        if (type == MsgType.CHAT) {
            if ("".equals(chatRoom)) {
                logger.info("请先加入一个聊天室~");
                return;
            }

            chatting = true;
            logger.info("已经加入聊天室，请输入： '#exit' 来退出聊天室~");
            return;
        }

        if (type == MsgType.QUIT_SYSTEM) {
            close();
            return;
        }

        logger.info("无效信息");
        printInfo();
    }

    /**
     * 处理聊天模式下收到的信息
     */
    private void handlerMsgInChattingMode(String msg) {
        if (msg == null || "".equals(msg)) {
            return;
        }

        if("#exit".equals(msg)) {
            chatting = false;
            logger.info("退出聊天模式~");
            printInfo();
            return;
        }

        String protocolMsg = MsgType.CHAT + msg;
        sendMsg(protocolMsg);
    }

    private void sendMsg(String msg) {
        try {
            os.write(msg.getBytes());
        } catch (IOException e) {
            alive = false;
            reconnect();
            e.printStackTrace();
        }
    }

    private void sendMsg(char msg) {
        try {
            os.write(msg);
        } catch (IOException e) {
            alive = false;
            reconnect();
            e.printStackTrace();
        }
    }

    /**
     * socket断开后，隔一段时间尝试重新连接
     */
    private void reconnect() {
        int retry = 0;
        while (retry < 5) {
            logger.info("重连中..." + (++retry));
            connect();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (alive) {
                logger.info("重连成功！");
                return;
            }
        }

        logger.info("重连失败，关闭客户端~");
        close();
    }

    private void close() {
        try {
            alive = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void printInfo() {
        System.out.println("------------------------------");
        System.out.println("-1: 输入 '#1' 查看当前聊天室");
        System.out.println("-2: 输入 '#2 {roomName}' 加入聊天室");
        System.out.println("-3: 输入 '#3' 退出聊天室");
        System.out.println("-4: 输入 '#4 {roomName}' 创建聊天室");
        System.out.println("-5: 输入 '#5' 开始聊天");
        System.out.println("-6: 输入 '#6' 退出聊天系统");
        System.out.println("------------------------------");
    }

    private void initFromProperties() {
        Properties properties = new Properties();
        try {
            properties.load(this.getClass().getResourceAsStream("/client.properties"));
            this.port = Integer.valueOf(properties.getProperty("Port"));
            this.host = properties.getProperty("Host");
            this.retry = Integer.valueOf(properties.getProperty("Retry"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                logger.info("Shutdown hook");
                close();
            }
        });
    }
}

