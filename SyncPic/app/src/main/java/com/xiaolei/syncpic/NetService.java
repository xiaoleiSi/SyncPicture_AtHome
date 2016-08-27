package com.xiaolei.syncpic;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import android.os.Handler;

public class NetService extends Service {
    public static String EXTRA_NET_DATA="net_DATA";
    public static String EXTRA_NET_ADDR="DES_ADDRESS";
    public static String EXTRA_SRC_ADDR="SRC_INET_ADDRESS";
    private final int bufferMaxSize=256;
    private String TAG ="netService";
    private DatagramPacket dataPacket;
    private DatagramSocket udpSocket;

    private DatagramPacket rcvPacket;

    private netBinder mSerBinder= new netBinder();
    private byte[] buffer;
    private byte[] rcvbuffer;

    public static NetService mService;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    public void onCreate(){
        super.onCreate();
        try{
            udpSocket = new DatagramSocket();

            buffer = new byte[bufferMaxSize];
            dataPacket = new DatagramPacket(buffer, buffer.length);

            rcvbuffer = new byte[bufferMaxSize];
            rcvPacket =  new DatagramPacket(rcvbuffer, rcvbuffer.length);

//            //send thread
//            mThread = new netThread();
//            new Thread(mThread).start();

            //start up receive udp thread
            threadRcvUdp();
        }
        catch (SocketException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mService = this;
    }


    public void sendSearchServerUdp(byte[] out){
        try {
            InetAddress broadcastAddr;

            broadcastAddr = getBroadcastAddress();

            sendCtrlPkg(broadcastAddr, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

        return mSerBinder;
    }

    public void sendCtrlPkg(InetAddress Addr, byte[] out){
        dataPacket.setData(out);
        dataPacket.setLength(out.length);
        dataPacket.setAddress(Addr);
        dataPacket.setPort(UdpCtrlPkg.SERVERPORT);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    udpSocket.send(dataPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

        //check
    private void decodeRcvUdpPkg(String data,InetAddress address) {
        UdpCtrlPkg ctrlPkg;
        ctrlPkg =  new UdpCtrlPkg(data);
        Log.d(TAG, "DATA: "+data);
        if (ctrlPkg.ctrlHead==null){
            return;
        }
        if ((ctrlPkg.ctrlWordType).equals(ctrlPkg.UDPCTRL_CTRLWORD_DISSERVER)){
            Intent intent = new Intent(SearchServer.ATCTION_DIS_SERVER);
            intent.putExtra(EXTRA_NET_DATA, ctrlPkg.ctrlPayload);

            intent.putExtra(EXTRA_NET_ADDR, address.getHostAddress());
            sendBroadcast(intent);
            Log.e(TAG, "Receive the Server response!");
        }
    }

    //thread : receive udp packet
    private void threadRcvUdp(){
            new Thread(new Runnable() {

                @Override
                public void run() {
                    while(true){
                        try {
                            Arrays.fill(rcvbuffer, (byte) 0);
                            udpSocket.receive(rcvPacket);
                            Log.d(TAG,"receive a packet!!!");

                            byte []data = rcvPacket.getData();
                            decodeRcvUdpPkg(new String(data, 0, rcvPacket.getLength()), rcvPacket.getAddress());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
    }


    public class netBinder extends Binder{
        public NetService getService(){
            return NetService.this;
        }
    }

    public static NetService getInstance(){
        return mService;
    }
    private String getlocalip(){
        WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        Log.d(TAG, "int ip "+ipAddress);
        if(ipAddress==0)return null;
        return ((ipAddress & 0xff)+"."+(ipAddress>>8 & 0xff)+"."
                +(ipAddress>>16 & 0xff)+"."+(ipAddress>>24 & 0xff));
    }
    private InetAddress getBroadcastAddress() throws IOException {
        WifiManager myWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        DhcpInfo myDhcpInfo = myWifiManager.getDhcpInfo();
        if (myDhcpInfo == null) {
            Log.e(TAG, "Could not get broadcast address");
            return null;
        }
        int broadcast = (myDhcpInfo.ipAddress & myDhcpInfo.netmask)
                | ~myDhcpInfo.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        return InetAddress.getByAddress(quads);
    }

    private String FileName;
    private String destIpaddr;
    public void sendTCPFile(String imageFile, String ipAddr){
        FileName = imageFile;
        destIpaddr = ipAddr;
        new Thread(new Runnable() {
            @Override
            public void run() {
                File file = new File(FileName);
                try {
                    byte[] buf = new byte[1024];
                    int len;

                    Socket socket = new Socket(destIpaddr, 8111);
                    InputStream inputStream = new FileInputStream(FileName);
                    OutputStream out = socket.getOutputStream();
                    //write the file name firstly
                    String msgHeader;
                    msgHeader = "put," + file.getName();
                    out.write(msgHeader.getBytes());
                    out.flush();

                    while ((len = inputStream.read(buf)) != -1)
                        out.write(buf, 0, len);

                    out.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void uploadFile(File imageFile, String ipAddr) throws IOException {
        Log.i(TAG, "upload start");
        String requestUrl = "http://"+ipAddr+":8081/";
        try {

            Log.e(TAG, requestUrl);
            //请求普通信息
            Map<String, String> params = new HashMap<String, String>();
            params.put("username", "张三");
            params.put("pwd", "zhangsan");
            params.put("age", "21");
            params.put("fileName", imageFile.getName());
            //上传文件
            String fileName=imageFile.getName();
            Log.d(TAG, "file name: "+fileName);
            FormFile formfile = new FormFile(fileName, imageFile, "image", "file");

            SocketHttpRequester.post(requestUrl, params, formfile);
            Log.i(TAG, "upload success");
        } catch (Exception e) {
            Log.i(TAG, "upload error");
            e.printStackTrace();
        }
//        URL url = new URL(requestUrl);
//
//        URLConnection rulConnection = url.openConnection();
//        // 此处的urlConnection对象实际上是根据URL的
//        // 请求协议(此处是http)生成的URLConnection类
//        // 的子类HttpURLConnection,故此处最好将其转化
//        // 为HttpURLConnection类型的对象,以便用到
//        // HttpURLConnection更多的API.如下:
//
//        HttpURLConnection httpUrlConnection = (HttpURLConnection) rulConnection;
//// 设置是否向httpUrlConnection输出，因为这个是post请求，参数要放在
//        // http正文内，因此需要设为true, 默认情况下是false;
//        httpUrlConnection.setDoOutput(true);
//
//        // 设置是否从httpUrlConnection读入，默认情况下是true;
//        httpUrlConnection.setDoInput(true);
//
//        // Post 请求不能使用缓存
//        httpUrlConnection.setUseCaches(false);
//
//        // 设定传送的内容类型是可序列化的java对象
//        // (如果不设此项,在传送序列化对象时,当WEB服务默认的不是这种类型时可能抛java.io.EOFException)
////        httpUrlConnection.setRequestProperty("Content-type", "application/x-java-serialized-object");
//
//        // 设定请求的方法为"POST"，默认是GET
//        httpUrlConnection.setRequestMethod("POST");
//
//        // 连接，从上述第2条中url.openConnection()至此的配置必须要在connect之前完成，
////        httpUrlConnection.connect();
//
//        // 此处getOutputStream会隐含的进行connect(即：如同调用上面的connect()方法，
//        /// 所以在开发中不调用上述的connect()也可以)。
//        OutputStream outStrm = httpUrlConnection.getOutputStream();
//        // 现在通过输出流对象构建对象输出流对象，以实现输出可序列化的对象。
//         ObjectOutputStream objOutputStrm = new ObjectOutputStream(outStrm);
//
//         // 向对象输出流写出数据，这些数据将存到内存缓冲区中
////         objOutputStrm.writeObject(imageFile);
//        objOutputStrm.writeObject(imageFile);
//
//         // 刷新对象输出流，将任何字节都写入潜在的流中（些处为ObjectOutputStream）
//        objOutputStrm.flush();
//
//          // 关闭流对象。此时，不能再向对象输出流写入任何数据，先前写入的数据存在于内存缓冲区中,
//          // 在调用下边的getInputStream()函数时才把准备好的http请求正式发送到服务器
//        objOutputStrm.close();
//
//          // 调用HttpURLConnection连接对象的getInputStream()函数,
//          // 将内存缓冲区中封装好的完整的HTTP请求电文发送到服务端。
//           // <===注意，实际发送请求的代码段就在这里
//           httpUrlConnection.getInputStream();
//
//          // 上边的httpConn.getInputStream()方法已调用,本次HTTP请求已结束,下边向对象输出流的输出已无意义，
//          // 既使对象输出流没有调用close()方法，下边的操作也不会向对象输出流写入任何数据.
//          // 因此，要重新发送数据时需要重新创建连接、重新设参数、重新创建流对象、重新写数据、
//          // 重新发送数据(至于是否不用重新这些操作需要再研究)
////        objOutputStrm.writeObject(new String("xxxxx"));
////        httpUrlConnection.getInputStream();

        Log.i(TAG, "upload end");
    }

    public static final int E_TCP_SENDIMAGE =0;
    public static final int E_UDP_SENDSEARCH=1;

    public Handler netHandler = new Handler(){
        public void handleMessage(Message msg){
            switch(msg.what){
                case E_TCP_SENDIMAGE:
                    Log.d(TAG,"Receive a msg need update file to server");

                    break;

            }
        }
    };
}
