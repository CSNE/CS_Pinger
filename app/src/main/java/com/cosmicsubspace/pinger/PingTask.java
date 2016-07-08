package com.cosmicsubspace.pinger;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Chan on 2015-11-06.
 */
public class PingTask extends AsyncTask<Void, Void, PingInfo>{
    public static final int ICMP_PING=19274;
    public static final int HTTP_REQUEST=1284;
    public static final int HTTP_SECURE_REQUEST=192748;

    public static final String LOG_TAG="CS_Pinger";

    int type;
    int timeout, slowThreshold;
    String address;
    PingReturnListener prl;

    public PingTask(String iPAddress, int timeout, int slowThreshold, int type){
        this.type=type;
        this.address=iPAddress;
        this.timeout=timeout;
        this.slowThreshold=slowThreshold;
    }
    public void setPingReturnListener(PingReturnListener prl){
        this.prl=prl;
    }
    @Override
    public void onPreExecute(){

    }
    @Override
    public PingInfo doInBackground(Void... v){
        PingInfo res=new PingInfo();
        if (type==ICMP_PING){
            res.setPingType(PingInfo.ICMP_PING);
            StringBuffer echo = new StringBuffer();
            Runtime runtime = Runtime.getRuntime();
            Log.v(LOG_TAG, "About to ping using runtime.exec");

            try {
                //String command="ping -c 1 " + address+" -w "+timeout/1000.0;
                String command="ping -c 1"+" -w "+timeout/1000.0+ " " + address;
                Log.v(LOG_TAG,"Executing: "+command);
                Process proc = runtime.exec(command);
                proc.waitFor();
                int exit = proc.exitValue();
                Log.v(LOG_TAG, "Exit Value: " + exit);

                if (exit == 0) {
                    InputStreamReader reader = new InputStreamReader(proc.getInputStream());
                    BufferedReader buffer = new BufferedReader(reader);
                    String line = new String();
                    while ((line = buffer.readLine()) != null) {
                        echo.append(line + "\n");
                    }

                    Log.v(LOG_TAG, "Console Out:\n" + echo.toString());

                    res.parseInfoFromConsole(echo.toString());

                } else {
                    Log.d(LOG_TAG,"Exit code "+exit);
                    res.setSuccess(false);
                }
            }catch(Exception e){
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                Log.e(LOG_TAG, "Exception on PingTask>doInBackground(ICMP)\n"+e.toString());
                res.setSuccess(false);
            }
        }else if (type==HTTP_REQUEST||type==HTTP_SECURE_REQUEST){
            if (type==HTTP_REQUEST) res.setPingType(PingInfo.HTTP_REQUEST);
            else res.setPingType(PingInfo.HTTP_SECURE);
            long startTime=System.currentTimeMillis();
            try {
                URL url;
                if (type==HTTP_REQUEST) {
                    url = new URL("http://"+address);
                }else{
                    url=new URL("https://"+address);
                }
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(timeout);
                urlConnection.setReadTimeout(timeout);
                urlConnection.setRequestProperty("Connection", "close");
                urlConnection.getInputStream();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                StringBuilder sb = new StringBuilder();
                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    sb.append("\n");
                }

                urlConnection.disconnect();
                long endTime=System.currentTimeMillis();
                long timeTaken=endTime-startTime;
                res.setPingTime(timeTaken);
                res.setSuccess(true);

            } catch (Exception e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                Log.d(LOG_TAG, "Error in GET.\n" + errors.toString().substring(0, 300) + "...(omitted)");
                res.setSuccess(false);
            }


        }
        res.setReturnTime(System.currentTimeMillis());
        res.setSlowThreshold(slowThreshold);
        return res;
    }

    @Override
    public void onPostExecute(PingInfo res){
        if (res!=null) Log.d(LOG_TAG, res.toString());
        if (prl!=null) prl.onReturn(res);
        prl=null;
    }
}
