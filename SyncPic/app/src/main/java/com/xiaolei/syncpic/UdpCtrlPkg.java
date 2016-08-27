package com.xiaolei.syncpic;

import android.util.Log;

/**
 * Created by Administrator on 2016/2/4.
 * leif: define the control protocol package format
 *
 */

/*
  所有的控制报文格式均为：
  AAA+TYPE+CMD+PAYLOAD
 */

public class UdpCtrlPkg {
    private static final String TAG = "DECODE PKG";
    public final String UDPCTRL_PREHEAD="AAA";
    public final String UDPCTRL_CTRLWORD_DISSERVER="1";

    public String ctrlHead;
    public String ctrlWordType;
    public String ctrlWordCmd;
    public String ctrlPayload;
    public final static int SERVERPORT = 22222;

    UdpCtrlPkg(String data){
        String[] mRcvDecodePkg;
        mRcvDecodePkg=data.split("\\+");
        if (!(mRcvDecodePkg[0].equals(UDPCTRL_PREHEAD))||(mRcvDecodePkg.length!=4)){
            Log.e(TAG, "Receive a invalid packge!   "+mRcvDecodePkg[0]+"  "+mRcvDecodePkg.length);

            return ;
        }

        ctrlHead = mRcvDecodePkg[0];
        ctrlWordType = mRcvDecodePkg[1];
        ctrlWordCmd = mRcvDecodePkg[2];
        ctrlPayload = mRcvDecodePkg[3];

        Log.d(TAG, ctrlHead+ctrlWordType+ctrlWordCmd);
    }


}

