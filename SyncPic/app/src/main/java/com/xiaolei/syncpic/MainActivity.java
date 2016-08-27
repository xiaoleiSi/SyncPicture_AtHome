package com.xiaolei.syncpic;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.logging.Handler;
import java.util.logging.LogRecord;


public class MainActivity extends ActionBarActivity {
    private Button btnSelectPic;
    private Button btnSearchServer;
    private NetService mUdpService;
    private final String TAG="MainActivity";
    private String mServerIp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        //choose pic
        btnSelectPic = (Button)findViewById(R.id.btnSelectPic);
        btnSearchServer = (Button)findViewById(R.id.btnSearchServer);

        //enable network
        Intent startIntent = new Intent(this, NetService.class);
        bindService(startIntent, netServiceConn, BIND_AUTO_CREATE);


        mServerIp = SearchServer.getmServerIpAddr();
        if (mServerIp ==null){
            Log.e(TAG, "没有找到有效的Server地址！");
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("未能找到Server");
            dialog.setMessage("请重新搜索Server IP");
            dialog.setCancelable(true);

            dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //trigger start search Server in the same wifi
                    Intent intent  = new Intent(MainActivity.this, SearchServer.class);

                    startActivity(intent);
                }
            });

            dialog.show();

        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        mServerIp = SearchServer.getmServerIpAddr();
        if (mServerIp != null) {
            Toast.makeText(this, "已经连接到服务器", Toast.LENGTH_LONG).show();
        }
    }
    public void onClickSearchServer(View v){
        //trigger start search Server in the same wifi
        Intent intent  = new Intent(this, SearchServer.class);

        startActivity(intent);
    }
    //点击进入界面，用于选择哪些图片需要进行同步
    public void onClickChoose(View v){
        Intent intent  = new Intent(this, choosePicActivity.class);

        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    private ServiceConnection netServiceConn = new ServiceConnection(){


        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            Log.d(TAG, "Net service connected!");
            mUdpService = ((NetService.netBinder)service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

}
