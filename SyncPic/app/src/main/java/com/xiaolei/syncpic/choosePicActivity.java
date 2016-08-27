package com.xiaolei.syncpic;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.nereo.imagechoose.*;
import android.os.Handler;
import android.widget.ProgressBar;

import static com.xiaolei.syncpic.SearchServer.mServerIpAddr;

public class choosePicActivity extends ActionBarActivity {
    private static final int REQUEST_IMAGE = 2;
    private static final String TAG = "选择图片";
    private ProgressBar loadProgressBar;

    private static List<String> mListSelectPicPath;
    private NetService mNetService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNetService = NetService.getInstance();


        setContentView(R.layout.activity_choose_pic);
        loadProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        loadProgressBar.setVisibility(View.GONE);
//        Intent intent = new Intent();
//                /* 开启Pictures画面Type设定为image */
//        intent.setType("image/*");
//                /* 使用Intent.ACTION_GET_CONTENT这个Action */
//        intent.setAction(Intent.ACTION_GET_CONTENT);
//                /* 取得相片后返回本画面 */
//        startActivityForResult(intent, 1);
        Intent intent = new Intent(this, MultiImageSelectorActivity.class);

// whether show camera
        intent.putExtra(MultiImageSelectorActivity.EXTRA_SHOW_CAMERA, true);

// max select image amount
        intent.putExtra(MultiImageSelectorActivity.EXTRA_SELECT_COUNT, 9999);

// select mode (MultiImageSelectorActivity.MODE_SINGLE OR MultiImageSelectorActivity.MODE_MULTI)
        intent.putExtra(MultiImageSelectorActivity.EXTRA_SELECT_MODE, MultiImageSelectorActivity.MODE_MULTI);

        startActivityForResult(intent, REQUEST_IMAGE);


    }
    public void onClickUpload(View v){
        loadProgressBar.setVisibility(View.VISIBLE);
        sendImageInit();
    }


    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_choose_pic, menu);
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


        //加载文件， 调用http接口发送
    private void LoadFile(String path){
        File file;
        file = new File(path);

        Message msgToNet = new Message();
        msgToNet.what = mNetService.E_TCP_SENDIMAGE;
        mNetService.netHandler.sendMessage(msgToNet);

        mNetService.sendTCPFile(path, SearchServer.mServerIpAddr);
    }

    //init the network prepare sending the images
    private void sendImageInit(){

        //if every thing is ok, enable the thread
        startUploadThread();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_IMAGE){
            if(resultCode == RESULT_OK){
                // Get the result list of select image paths
                mListSelectPicPath = data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT);

            }
        }

        //启动网络TCP
        //leif: 需要优化效率
//        sendImageInit();


    }

    public void startUploadThread(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                int totalSize = mListSelectPicPath.size();
                double rate;

                for (int i =0;i<totalSize;i++){
                    Log.e(TAG, "path: "+mListSelectPicPath.get(i));
                    LoadFile(mListSelectPicPath.get(i));
                    rate = ((float)i+1)/(float)totalSize;
                    Log.d(TAG, "已经上传："+rate);
                    loadProgressBar.setProgress((int) (rate*100));

                    try {
                        //为保险，休眠一段时间
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }).start();
    }
}
