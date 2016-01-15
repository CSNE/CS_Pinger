package com.chancorp.midproj;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;

import android.os.IBinder;

import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by Chan on 2015-11-06.
 */
public class PingService extends Service implements PingReturnListener {
    public static final String LOG_TAG = "CS_Pinger";
    public static final String START_ICMP = "com.chancorp.midproj.PingService.START_ICMP";
    public static final String END_ICMP = "com.chancorp.midproj.PingService.END_ICMP";
    public static final String START_HTTP = "com.chancorp.midproj.PingService.START_HTTP";
    public static final String END_HTTP = "com.chancorp.midproj.PingService.END_HTTP";
    public static final String START_HTTPS = "com.chancorp.midproj.PingService.START_HTTPS";
    public static final String END_HTTPS = "com.chancorp.midproj.PingService.END_HTTPS";
    public static final String START_ALERT = "com.chancorp.midproj.PingService.START_ALERT";
    public static final String END_ALERT = "com.chancorp.midproj.PingService.END_ALERT";
    public static final String PING_BROADCAST="com.chancorp.midproj.PingService.PING_BROADCAST";

    private static final int GOOD=4123;
    private static final int BAD=49846;
    private static final int SLOW=54986;

    LocalBroadcastManager lbm;

    int notificationID = 462;
    int notifyThreshold=-1;
    Timer[] timers = new Timer[3];
    PingInfo[] pingInfos = new PingInfo[3];
    PingSettings[] pingSettingses=new PingSettings[3];
    boolean icmpActive=false,httpActive=false,httpsActive=false;
    boolean[] icmpFailHistory=new boolean[100];
    boolean[] httpFailHistory=new boolean[100];
    boolean[] httpsFailHistory=new boolean[100];
    boolean useless=false;
    boolean icmpAlerted=false, httpAlerted=false, httpsAlerted=false;

    @Override
    public void onCreate() {
        super.onCreate();
        lbm=LocalBroadcastManager.getInstance(this);
        Log.d(LOG_TAG, "Service Created.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {

        Log.d(LOG_TAG, "Canceling notification...");
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(notificationID);
        Log.d(LOG_TAG, "Ending all TimerTasks...");
        endICMP();
        endHTTP();
        endHTTPS();
        Log.i(LOG_TAG, "Service Destroyed.");

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(LOG_TAG, "Service Starting");
        if (intent.getAction() == null) {
            //Do nothing.
        } else if (intent.getAction().equals(START_ICMP)) {
            Log.i(LOG_TAG, "Service: Starting ICMP Ping...");
            pingSettingses[0]=intent.getParcelableExtra("Ping Settings");
            startICMP();
        } else if (intent.getAction().equals(END_ICMP)) {
            Log.i(LOG_TAG, "Service: Ending ICMP Ping...");
            endICMP();
        }else if (intent.getAction().equals(START_HTTP)) {
            Log.i(LOG_TAG, "Service: Starting HTTP Ping...");
            pingSettingses[1]=intent.getParcelableExtra("Ping Settings");
            startHTTP();
        } else if (intent.getAction().equals(END_HTTP)) {
            Log.i(LOG_TAG, "Service: Ending HTTP Ping...");
            endHTTP();
        }else if (intent.getAction().equals(START_HTTPS)) {
            Log.i(LOG_TAG, "Service: Starting HTTPS Ping...");
            pingSettingses[2]=intent.getParcelableExtra("Ping Settings");
            startHTTPS();
        } else if (intent.getAction().equals(END_HTTPS)) {
            Log.i(LOG_TAG, "Service: Ending HTTPS Ping...");
            endHTTPS();
        }else if (intent.getAction().equals(START_ALERT)) {
            Log.i(LOG_TAG, "Service: Starting alert...");
            notifyThreshold=intent.getIntExtra("Threshold",-1);
        }else if (intent.getAction().equals(END_ALERT)) {
            Log.i(LOG_TAG, "Service: Ending alert...");
            notifyThreshold=-1;
        }
        Log.d(LOG_TAG, "Updating Notification....");
        updateNotification();
        Log.d(LOG_TAG, "Checking if useless....");
        checkIfUseless();
        Log.d(LOG_TAG, "Returning OnStartService...");

        return START_NOT_STICKY;
    }

    public void checkIfUseless(){
        if (isUseless()) stopSelf();
    }
    public boolean isUseless(){
        Log.d(LOG_TAG,"Checking is service is useless..."+icmpActive+httpActive+httpsActive);
        if ((!icmpActive)&&(!httpActive)&&(!httpsActive)){
            Log.i(LOG_TAG,"Service is useless!");
            return true;
        }
        return false;
    }

    public void startICMP() {
        if (icmpActive) {
            Log.d(LOG_TAG, "ICMP started but a task is already running! ignoring.");
            return;
        }
        icmpActive=true;
        timers[0] = new Timer();
        timers[0].scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                PingTask pt = new PingTask(pingSettingses[0].getDomain(),pingSettingses[0].getTimeout(),pingSettingses[0].getSlow(), PingTask.ICMP_PING);
                pt.setPingReturnListener(PingService.this);
                pt.execute();
            }
        }, 0, pingSettingses[0].getRepeat());
    }
    public void startHTTP() {
        if (httpActive) {
            Log.d(LOG_TAG, "HTTP started but a task is already running! ignoring.");
            return;
        }
        httpActive=true;
        timers[1] = new Timer();
        timers[1].scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                PingTask pt = new PingTask(pingSettingses[1].getDomain(),pingSettingses[1].getTimeout(),pingSettingses[1].getSlow(), PingTask.HTTP_REQUEST);
                pt.setPingReturnListener(PingService.this);
                pt.execute();
            }
        }, 0, pingSettingses[1].getRepeat());
    }
    public void startHTTPS() {
        if (httpsActive) {
            Log.d(LOG_TAG, "HTTPS started but a task is already running! ignoring.");
            return;
        }
        httpsActive=true;
        timers[2] = new Timer();
        timers[2].scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                PingTask pt = new PingTask(pingSettingses[2].getDomain(),pingSettingses[2].getTimeout(),pingSettingses[2].getSlow(), PingTask.HTTP_SECURE_REQUEST);
                pt.setPingReturnListener(PingService.this);
                pt.execute();
            }
        }, 0, pingSettingses[2].getRepeat());
    }



    public void endICMP() {
        if (!icmpActive) {
            Log.d(LOG_TAG, "ICMP end request but a task is not running! ignoring.");
            return;
        }
        icmpActive=false;
        if (timers[0]!=null) timers[0].cancel();
    }
    public void endHTTP() {
        if (!httpActive) {
            Log.d(LOG_TAG, "HTTP end request but a task is not running! ignoring.");
            return;
        }
        httpActive=false;
        if (timers[1]!=null) timers[1].cancel();
    }
    public void endHTTPS() {
        if (!httpsActive) {
            Log.d(LOG_TAG, "HTTPS end request but a task is not running! ignoring.");
            return;
        }
        httpsActive=false;
        if (timers[2]!=null) timers[2].cancel();
    }

    public int checkPingInfos(){
        for(PingInfo pi:pingInfos){
            if (pi!=null){
                if (!pi.isSuccess()) return BAD;
                if (pi.isSlow())  return SLOW;
            }
        }
        return GOOD;
    }

    @Override
    public void onReturn(PingInfo pi) {
        if (pi.getPingType()==PingInfo.ICMP_PING) {
            Log.v(LOG_TAG, "Callback on service(ICMP Ping)");
            pingInfos[0]=pi;
        }else if (pi.getPingType()==PingInfo.HTTP_REQUEST) {
            Log.v(LOG_TAG, "Callback on service(HTTP Ping)");
            pingInfos[1]=pi;
        }else if (pi.getPingType()==PingInfo.HTTP_SECURE) {
            Log.v(LOG_TAG, "Callback on service(HTTPS Ping)");
            pingInfos[2]=pi;
        }

        if (isUseless()) return;

        updateNotification();
        sendBroadcastToUI(pi);
        updateFailHistory(pi);
        checkIfAlert();
        checkIfSuccessAlert();

    }

    public void updateFailHistory(PingInfo pi){
        if (pi.getPingType()==PingInfo.ICMP_PING) {
            for (int i = icmpFailHistory.length - 1; i > 0; i--) {
                icmpFailHistory[i] = icmpFailHistory[i - 1];
            }
            icmpFailHistory[0] = pi.isSuccess();
        }else if (pi.getPingType()==PingInfo.HTTP_REQUEST) {
            for (int i = httpFailHistory.length - 1; i > 0; i--) {
                httpFailHistory[i] = httpFailHistory[i - 1];
            }
            httpFailHistory[0] = pi.isSuccess();
        }else if (pi.getPingType()==PingInfo.HTTP_SECURE) {
            for (int i = httpsFailHistory.length - 1; i > 0; i--) {
                httpsFailHistory[i] = httpsFailHistory[i - 1];
            }
            httpsFailHistory[0] = pi.isSuccess();
        }
    }

    public void checkIfAlert(){
        if (notifyThreshold<=0) return;
        boolean icmpAlert=true, httpAlert=true, httpsAlert=true;

        for (int i=0;i<notifyThreshold;i++){
            if (icmpFailHistory[i]) icmpAlert=false;
        }
        if (!icmpFailHistory[notifyThreshold]) icmpAlert=false;

        for (int i=0;i<notifyThreshold;i++){
            if (httpFailHistory[i]) httpAlert=false;
        }
        if (!httpFailHistory[notifyThreshold]) httpAlert=false;

        for (int i=0;i<notifyThreshold;i++){
            if (httpsFailHistory[i]) httpsAlert=false;
        }
        if (!httpsFailHistory[notifyThreshold]) httpsAlert=false;


        if (icmpAlert&&!icmpAlerted){
            alert("ICMP Ping Failed!",1000);
            icmpAlerted=true;
        }
        if (httpAlert&&!httpAlerted) {
            alert("HTTP Ping Failed!",1000);
            httpAlerted=true;
        }
        if (httpsAlert&&!httpsAlerted) {
            alert("HTTPS Ping Failed!",1000);
            httpsAlerted=true;
        }

    }

    public void checkIfSuccessAlert(){
        if (icmpFailHistory[0]&&icmpAlerted){
            alert("ICMP Ping Success!",300);
            icmpAlerted=false;
        }
        if (httpFailHistory[0]&&httpAlerted){
            alert("HTTP Ping Success!",300);
            httpAlerted=false;
        }
        if (httpsFailHistory[0]&&httpsAlerted){
            alert("HTTPS Ping Success!",300);
            httpsAlerted=false;
        }
    }

    public void alert(String message, int vibrateMilisec){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        if (vibrateMilisec>0) ((Vibrator)this.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(vibrateMilisec);
    }


    public void updateNotification() {
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this);
        switch (checkPingInfos()){
            case GOOD:
                nb.setSmallIcon(R.drawable.notif_good);
                break;
            case BAD:
                nb.setSmallIcon(R.drawable.notif_bad);
                break;
            case SLOW:
                nb.setSmallIcon(R.drawable.notif_slow);
        }

        PendingIntent pendInt=PendingIntent.getActivity(this, 18274,new Intent(this,MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        nb.setContentIntent(pendInt);
        nb.setContentTitle("CS Pinger");
        nb.setOngoing(true);

        String notificationString="";
        if (pingInfos[0]!=null) notificationString=notificationString+pingInfos[0].getNotificationString()+" ";
        if (pingInfos[1]!=null) notificationString=notificationString+pingInfos[1].getNotificationString()+" ";
        if (pingInfos[2]!=null) notificationString=notificationString+pingInfos[2].getNotificationString();
        nb.setContentText(notificationString);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
        mNotificationManager.notify(notificationID, nb.build());
    }

    public void sendBroadcastToUI(PingInfo pi){
        Intent itt=new Intent(PING_BROADCAST);
        itt.putExtra("New PingInfo",pi);

        lbm.sendBroadcast(itt);
    }


}
