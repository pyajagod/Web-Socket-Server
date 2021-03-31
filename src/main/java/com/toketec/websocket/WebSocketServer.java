package com.toketec.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

/**
 * @author wst
 * @version 1.0
 * @date 2018/11/26 11:04
 */
/**
 * @ServerEndpoint 注解是一个类层次的注解，它的功能主要是将目前的类定义成一个websocket服务器端,
 * 注解的值将被用于监听用户连接的终端访问URL地址,客户端可以通过这个URL来连接到WebSocket服务器端
 * @author
 */
@ServerEndpoint(value="/websocket/{username}")
public class WebSocketServer {
    //线程安全的静态变量，表示在线连接数
    private static volatile int  onlineCount = 0;

    //用来存放每个客户端对应的WebSocketTest对象，适用于同时与多个客户端通信
//    public static CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet<WebSocketServer>();
    //若要实现服务端与指定客户端通信的话，可以使用Map来存放，其中Key可以为用户标识
    public static ConcurrentHashMap<Session,Object> webSocketMap = new ConcurrentHashMap<Session,Object>();

    //与某个客户端的连接会话，通过它实现定向推送(只推送给某个用户)
    private Session session;

    /**
     * 连接建立成功调用的方法
     *
     * @param session 可选的参数。session为与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    @OnOpen
    public void onOpen(@PathParam("username") String username, Session session){
        this.session = session;
//        webSocketSet.add(this);     //加入set中
        webSocketMap.put(session,this); //加入map中
        addOnlineCount();    //在线数加1
        System.out.println("有新连接加入！当前在线人数为" + getOnlineCount());
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(Session closeSession) {
//        webSocketSet.remove(this); //从set中删除
        webSocketMap.remove(closeSession); //从map中删除
        subOnlineCount();          //在线数减1
        System.out.println("有一连接关闭！当前在线人数为" + getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     * @param mySession 可选的参数
     * @throws Exception
     */
    @OnMessage
    public void onMessage(@PathParam("username") String username,String message,Session mySession) throws Exception {

//        logger.info("来自客户端的消息:" + message);
        //--------------群发消息(多用于聊天室场景)
       for (Map.Entry item : webSocketMap.entrySet()) {
            try {
                ((WebSocketServer)item.getValue()).sendAllMessage("{\"user\":\""+username+"\",\"msg\":"+"\""+message+"\"}");
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
        }

        //推送给单个客户端
//        for (Session session : webSocketMap.keySet()) {
//            if (session.equals(mySession)) {
//                WebSocketServer item = (WebSocketServer) webSocketMap.get(mySession);
//                try {
//                    String msg="嗨，这是返回的信息";
//                    item.sendMessage(mySession,msg);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//
//        }


    }


    /**
     * 发生错误时调用
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        // error.printStackTrace();
    }


    //给所有客户端发送信息
    public void sendAllMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    //定向发送信息
    public void sendMessage(Session mySession,String message) throws IOException {
        synchronized(this) {try {
            if(mySession.isOpen()){//该session如果已被删除，则不执行发送请求，防止报错
                //this.session.getBasicRemote().sendText(message);
                mySession.getBasicRemote().sendText(message);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        }


    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
    }





}
