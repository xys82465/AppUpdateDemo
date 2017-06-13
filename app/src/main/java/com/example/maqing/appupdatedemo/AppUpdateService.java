package com.example.maqing.appupdatedemo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.io.File;

import cn.finalteam.okhttpfinal.FileDownloadCallback;
import cn.finalteam.okhttpfinal.HttpRequest;
/**
 * Created by maqing on 2017/6/2.
 * Email:2856992713@qq.com
 * App更新Service
 */
public class AppUpdateService extends Service {
    private NotificationManager mNotificationManager;
    private Notification mNotification;
    /**
     * 保存的路径
     */
    private String mSavePath;
    /**
     * 下载的Url
     */
    private String mDownloadUrl;

    private int mOldProgress=0;

    private String TAG = "AppUpdateService";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            mSavePath = intent.getStringExtra("save_path");
            mDownloadUrl = intent.getStringExtra("download_url");
            Log.e(TAG, mSavePath + "," + mDownloadUrl);
            mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            File saveFile = new File(mSavePath);
            HttpRequest.download(mDownloadUrl, saveFile, new FileDownloadCallback() {

                //开始下载
                @Override
                public void onStart() {
                    super.onStart();
                }

                //下载进度
                @Override
                public void onProgress(int progress, long networkSpeed) {
                    super.onProgress(progress, networkSpeed);
                    /**
                     * 这里的条件是为了避免Notification的频繁notify,因为Notification的频繁notify会导致
                     * 应用界面卡顿或卡死
                     */
                    if(mOldProgress==0||(mOldProgress>0&&(progress-mOldProgress>10)||progress==100)){
                        notifyUser(progress);
                    }
                }

                //下载失败
                @Override
                public void onFailure() {
                    super.onFailure();
                    Toast.makeText(getBaseContext(), "下载失败", Toast.LENGTH_SHORT).show();
                }

                //下载完成（下载成功）
                @Override
                public void onDone() {
                    super.onDone();
                    Toast.makeText(getBaseContext(), "下载成功", Toast.LENGTH_SHORT).show();

                    //关闭Service
                    stopSelf();

                }
            });
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void notifyUser(int progress) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(getString(R.string.app_name));
        if (progress > 0 && progress <= 100) {
            builder.setProgress(100, progress, false);
        } else {
            builder.setProgress(0, 0, false);
        }
        builder.setAutoCancel(true);
        builder.setWhen(System.currentTimeMillis());
//        builder.setTicker(result);
        builder.setContentIntent(progress >= 100 ? this.getContentIntent() :
                PendingIntent.getActivity(this, 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT));
        mNotification = builder.build();
        mNotificationManager.notify(0, mNotification);
    }

    /**
     * 进入安装
     *
     * @return
     */
    private PendingIntent getContentIntent() {
        File saveFile = new File(mSavePath);
        Intent install = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= 24) { //判读版本是否在7.0以上
            Uri apkUri = FileProvider.getUriForFile(getApplicationContext(), BuildConfig.APPLICATION_ID + ".fileprovider", saveFile);//在AndroidManifest中的android:authorities值
            install = new Intent(Intent.ACTION_VIEW);
            install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); //添加这一句表示对目标应用临时授权该Uri所代表的文件
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else {
            install.setDataAndType(Uri.fromFile(saveFile), "application/vnd.android.package-archive");
            install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, install, PendingIntent.FLAG_UPDATE_CURRENT);
        startActivity(install);
        return pendingIntent;
    }

    /**
     * @param context
     * @param savePath    保存到本地的路径
     * @param downloadUrl 下载的Url
     */
    public static void start(Context context, String savePath, String downloadUrl) {
        Intent intent = new Intent(context, AppUpdateService.class);
        intent.putExtra("save_path", savePath);
        intent.putExtra("download_url", downloadUrl);
        context.startService(intent);
    }

}