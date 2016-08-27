package com.xiaolei.syncpic;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;


public class SearchServer extends ActionBarActivity {
    private String TAG="Serach Activity";
    public static final String ATCTION_DIS_SERVER = "ACTION_FIND_SERVER";

    private IntentFilter intentFilter;
    private NetMsgReceiver discoverMsgRcv;

    public static boolean bFindServer=false;
    public static String mServerIpAddr=null;
    private NetService mNetService;

    static String getmServerIpAddr(){
        if (bFindServer==true)
            return mServerIpAddr;
        else
            return null;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_server);

        mNetService = NetService.getInstance();

        intentFilter = new IntentFilter();
        intentFilter.addAction(ATCTION_DIS_SERVER);

        discoverMsgRcv = new NetMsgReceiver();
        registerReceiver(discoverMsgRcv, intentFilter);

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search_server, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    void showAlertCheckConnect(String ip){
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        dialog.setTitle("发现Server");
        dialog.setMessage("是否连接到Server:"+ip);

        dialog.setCancelable(true);
        dialog.setPositiveButton("连吧！",new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                bFindServer = true;

                SearchServer.this.finish();
            }
        });

        dialog.setNegativeButton("不连",new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialog, int which) {
                bFindServer = false;
            }
        });
        dialog.show();

    }

    class NetMsgReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            Log.d(TAG, "Action: "+action);

            if (action.equals(ATCTION_DIS_SERVER)){
                String data, srcAddr;
                data = intent.getStringExtra(NetService.EXTRA_NET_DATA);
                srcAddr = intent.getStringExtra(NetService.EXTRA_NET_ADDR);
                Log.d(TAG, "发现server,地址是 : " + srcAddr);

                showAlertCheckConnect(srcAddr);
                mServerIpAddr = srcAddr;
            }
        }
    }

    public void onClickSearchServer(View v){
//        mNetService.sendCtrlBroadcastPkg("Who is Server?".getBytes());
        mNetService.sendSearchServerUdp("Who is Server?".getBytes());
    }

}
