package com.bignerdranch.android.photogallery;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class PollService extends IntentService {
    private static final String TAG = "PollService";
    
    private static final int POLL_INTERVAL = 1000 * 60 * 5; // 5 minutes
    public static final String PREF_IS_ALARM_ON = "isAlarmOn";

    public static final String ACTION_SHOW_NOTIFICATION = "com.bignerdranch.android.photogallery.SHOW_NOTIFICATION";
    
    public static final String PERM_PRIVATE = "com.bignerdranch.android.photogallery.PRIVATE";
    
    public PollService() {
        super(TAG);
    }

    /**
     * 后台服务
     */
    @Override
    public void onHandleIntent(Intent intent) {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        // 检查后台网络的可用性，如果不可用，退出
        @SuppressWarnings("deprecation")
        boolean isNetworkAvailable = cm.getBackgroundDataSetting() &&
            cm.getActiveNetworkInfo() != null;        
        if (!isNetworkAvailable) return; 
        // 从默认SharedPreferences中获取当前查询结果以及上一次结果ID
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String query = prefs.getString(FlickrFetchr.PREF_SEARCH_QUERY, null);
        String lastResultId = prefs.getString(FlickrFetchr.PREF_LAST_RESULT_ID, null);
        // 使用FlickrFetchr类获取最新结果集
        ArrayList<GalleryItem> items;
        if (query != null) {
            items = new FlickrFetchr().search(query);
        } else {
            items = new FlickrFetchr().fetchItems();
        }
        // 如果有结果返回，抓取结果的第一条
        if (items.size() == 0) 
            return;
        String resultId = items.get(0).getId();
        // 检查确认是否不同于上一次结果ID，如不一样，进行下边操作
        if (!resultId.equals(lastResultId)) {
            Log.i(TAG, "Got a new result: " + resultId);

            Resources r = getResources();
            PendingIntent pi = PendingIntent
                .getActivity(this, 0, new Intent(this, PhotoGalleryActivity.class), 0);

            // 创建Notification对象
            Notification notification = new NotificationCompat.Builder(this)
                .setTicker(r.getString(R.string.new_pictures_title))
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle(r.getString(R.string.new_pictures_title))
                .setContentText(r.getString(R.string.new_pictures_text))
                .setContentIntent(pi) //用户点击通知信息，触发PendingIntent
                .setAutoCancel(true)
                .build();

            showBackgroundNotification(0, notification); //发送通知信息
        }
        // 将结果保存
        prefs.edit()
            .putString(FlickrFetchr.PREF_LAST_RESULT_ID, resultId)
            .commit();
    }
    
    /**
     *  定时器是静态方法，允许其他系统部件调用它
     */
    public static void setServiceAlarm(Context context, boolean isOn) {
        Intent i = new Intent(context, PollService.class);
        //创建PendingIntent
        PendingIntent pi = PendingIntent.getService(
                context, 0, i, 0);

        AlarmManager alarmManager = (AlarmManager)
                context.getSystemService(Context.ALARM_SERVICE);
        if (isOn) {
            // 设置定时器，定时发送pi
            alarmManager.setRepeating(AlarmManager.RTC, 
                    System.currentTimeMillis(), POLL_INTERVAL, pi);
        } else {
        	// 取消定时器
            alarmManager.cancel(pi);
            pi.cancel();
        }
        // 添加定时器状态Preference
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(PollService.PREF_IS_ALARM_ON, isOn)
            .commit();
    }

    /**
     * 通过检查PendingIntent是否存在，确认定时器激活与否
     */
    public static boolean isServiceAlarmOn(Context context) {
        Intent i = new Intent(context, PollService.class);
        // 如果pi不存在，pi=null
        PendingIntent pi = PendingIntent.getService(
                context, 0, i, PendingIntent.FLAG_NO_CREATE); 
        return pi != null;
    }
    
    /**
     * 广播接收器
     */
    private static BroadcastReceiver sNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            Log.i(TAG, "received result: " + getResultCode());
            if (getResultCode() != Activity.RESULT_OK)
                // 如果前边set cancel
                return;
            
            int requestCode = i.getIntExtra("REQUEST_CODE", 0);
            Notification notification = (Notification)i.getParcelableExtra("NOTIFICATION");
            
            NotificationManager notificationManager = (NotificationManager)
                    c.getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(requestCode, notification);
        }
    };
    
    /**
     * 发送有序broadcast
     */
    void showBackgroundNotification(int requestCode, Notification notification) {
        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra("REQUEST_CODE", requestCode);
        i.putExtra("NOTIFICATION", notification);
        
        //发送有序的带有权限的broadcast
        sendOrderedBroadcast(i, PERM_PRIVATE, sNotificationReceiver, null, Activity.RESULT_OK, null, null);
    }
}
