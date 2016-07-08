package com.cosmicsubspace.pinger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //TODO help

    static final String LOG_TAG = "CS_Pinger";

    GraphView gv;
    Button icmp_start_btn, icmp_end_btn, icmp_set_btn, http_start_btn, http_end_btn, http_set_btn, https_start_btn, https_end_btn, https_set_btn;
    TextView icmp_information, http_information, https_information, icmp_result, http_result, https_result;
    ImageView icmp_icon, http_icon, https_icon;

    CheckBox alertToggle;
    EditText alertThresh;

    PointsGraphSeries<DataPoint> icmpLineSeries, httpLineSeries, httpsLineSeries;
    PingReceiver pingReceiver;
    ArrayList<PingInfo> IcmpInformationArray = new ArrayList<PingInfo>();
    ArrayList<PingInfo> HttpInformationArray = new ArrayList<PingInfo>();
    ArrayList<PingInfo> HttpsInformationArray = new ArrayList<PingInfo>();
    PingSettings icmpPingSettings, httpPingSettings, httpsPingSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "---New Session---");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        icmpPingSettings = new PingSettings();
        icmpPingSettings.setTimeout(1000);
        icmpPingSettings.setRepeat(3000);
        icmpPingSettings.setDomain("www.google.com");
        icmpPingSettings.setSlow(300);

        httpPingSettings = new PingSettings();
        httpPingSettings.setTimeout(1500);
        httpPingSettings.setRepeat(3000);
        httpPingSettings.setDomain("www.google.com");
        httpPingSettings.setSlow(500);

        httpsPingSettings = new PingSettings();
        httpsPingSettings.setTimeout(2000);
        httpsPingSettings.setRepeat(3000);
        httpsPingSettings.setDomain("www.google.com");
        httpsPingSettings.setSlow(1000);


        pingReceiver = new PingReceiver();


        gv = (GraphView) findViewById(R.id.graph_view);

        gv.setTitle("Ping Times");
        gv.getGridLabelRenderer().setHighlightZeroLines(true);
        gv.getGridLabelRenderer().setPadding(36);

        icmpLineSeries = new PointsGraphSeries<DataPoint>();
        icmpLineSeries.setColor(Color.parseColor("#A00000"));
        icmpLineSeries.setSize(10);
        icmpLineSeries.setTitle("ICMP");
        gv.addSeries(icmpLineSeries);

        httpLineSeries = new PointsGraphSeries<DataPoint>();
        httpLineSeries.setColor(Color.parseColor("#00A000"));
        httpLineSeries.setSize(10);
        httpLineSeries.setTitle("HTTP");
        gv.addSeries(httpLineSeries);

        httpsLineSeries = new PointsGraphSeries<DataPoint>();
        httpsLineSeries.setColor(Color.parseColor("#0000A0"));
        httpsLineSeries.setSize(10);
        httpsLineSeries.setTitle("HTTPS");
        gv.addSeries(httpsLineSeries);

        gv.getLegendRenderer().setVisible(true);
        gv.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.MIDDLE);

        gv.getViewport().setXAxisBoundsManual(true);
        gv.getViewport().setMinX(0);
        gv.getViewport().setMaxX(60);

        //gv.getViewport().setYAxisBoundsManual(true);
        //gv.getViewport().setMinY(0);
        //gv.getViewport().setMaxY(1);


        alertToggle = (CheckBox) findViewById(R.id.alert_toggle);
        alertThresh = (EditText) findViewById(R.id.alert_input);
        alertToggle.setOnClickListener(this);


        icmp_set_btn = (Button) findViewById(R.id.icmp_settings);
        icmp_start_btn = (Button) findViewById(R.id.icmp_start);
        icmp_end_btn = (Button) findViewById(R.id.icmp_end);
        icmp_information = (TextView) findViewById(R.id.icmp_information);
        icmp_result = (TextView) findViewById(R.id.icmp_result);
        icmp_icon = (ImageView) findViewById(R.id.icmp_icon);

        icmp_start_btn.setOnClickListener(this);
        icmp_end_btn.setOnClickListener(this);
        icmp_set_btn.setOnClickListener(this);


        http_set_btn = (Button) findViewById(R.id.http_settings);
        http_start_btn = (Button) findViewById(R.id.http_start);
        http_end_btn = (Button) findViewById(R.id.http_end);
        http_information = (TextView) findViewById(R.id.http_information);
        http_result = (TextView) findViewById(R.id.http_result);
        http_icon = (ImageView) findViewById(R.id.http_icon);

        http_set_btn.setOnClickListener(this);
        http_start_btn.setOnClickListener(this);
        http_end_btn.setOnClickListener(this);

        https_set_btn = (Button) findViewById(R.id.https_settings);
        https_start_btn = (Button) findViewById(R.id.https_start);
        https_end_btn = (Button) findViewById(R.id.https_end);
        https_information = (TextView) findViewById(R.id.https_information);
        https_result = (TextView) findViewById(R.id.https_result);
        https_icon = (ImageView) findViewById(R.id.https_icon);

        https_set_btn.setOnClickListener(this);
        https_start_btn.setOnClickListener(this);
        https_end_btn.setOnClickListener(this);

        endHTTP();
        endHTTPS();
        endICMP();

        updateInformation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(pingReceiver, new IntentFilter(PingService.PING_BROADCAST));
        loadFromFile();
        updateInformation();
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(pingReceiver);
        saveToFile();
        super.onStop();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
/*
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.db_1) {
            saveToFile();
        } else if (id == R.id.db_2) {
            loadFromFile();
            updateInformation();
        } else if (id == R.id.help) {
            TextShowPopup tsp = new TextShowPopup(this);
            tsp.setTitle("Help");
            tsp.setText(getResources().getString(R.string.help));
            tsp.init();
        }else*/ if (id == R.id.gateway) {
            TextShowPopup tsp = new TextShowPopup(this);
            tsp.setTitle("Gateway Information");
            if (isWifiConnected()) {
                tsp.setText("Gateway IP: " + getGatewayIP() + "\n\n(ICMP Ping this address to check if your connection with the router is valid.)");
            }else{
                tsp.setText("Wifi Not connected.");
            }
            tsp.init();
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean isWifiConnected(){
        try {
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (mWifi.isConnected()) {
                return true;
            } else return false;
        }catch(Exception e){
            Toast.makeText(this, "CS Pinger internal error.", Toast.LENGTH_LONG).show();
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            Log.d(LOG_TAG, "Error in isWifiConnected:\n"+errors.toString());
            return false;
        }
    }
    public String getGatewayIP() {
        try {
            WifiManager wifii;
            wifii = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            int gt = wifii.getDhcpInfo().gateway;
            return intToIp(gt);
        } catch (Exception e) {
            Toast.makeText(this, "CS Pinger internal error.", Toast.LENGTH_LONG).show();
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            Log.d(LOG_TAG, "Error in isWifiConnected:\n"+errors.toString());
            return "???";
        }
    }

    public String intToIp(int addr) {
        return ((addr & 0xFF) + "." +
                ((addr >>>= 8) & 0xFF) + "." +
                ((addr >>>= 8) & 0xFF) + "." +
                ((addr >>>= 8) & 0xFF));
    }//From http://www.devdaily.com/java/jwarehouse/android/core/java/android/net/DhcpInfo.java.shtml


    public void startICMP() {
        Log.i(LOG_TAG, "Starting ICMP....");
        Intent itt = new Intent(this, PingService.class);
        itt.putExtra("Ping Settings", (Parcelable) icmpPingSettings);
        itt.setAction(PingService.START_ICMP);
        startService(itt);


        icmp_set_btn.setEnabled(false);
        icmp_set_btn.setAlpha(0.5f);
        icmp_end_btn.setEnabled(true);
        icmp_end_btn.setAlpha(1.0f);
        icmp_start_btn.setEnabled(false);
        icmp_start_btn.setAlpha(0.5f);
    }

    public void endICMP() {
        Log.i(LOG_TAG, "Ending ICMP....");
        Intent itt = new Intent(this, PingService.class);
        itt.setAction(PingService.END_ICMP);
        startService(itt);


        icmp_set_btn.setEnabled(true);
        icmp_set_btn.setAlpha(1.0f);
        icmp_end_btn.setEnabled(false);
        icmp_end_btn.setAlpha(0.5f);
        icmp_start_btn.setEnabled(true);
        icmp_start_btn.setAlpha(1.0f);
    }

    public void startHTTP() {
        Log.i(LOG_TAG, "Starting HTTP....");
        Intent itt = new Intent(this, PingService.class);
        itt.putExtra("Ping Settings", (Parcelable) httpPingSettings);
        itt.setAction(PingService.START_HTTP);
        startService(itt);


        http_set_btn.setEnabled(false);
        http_set_btn.setAlpha(0.5f);
        http_end_btn.setEnabled(true);
        http_end_btn.setAlpha(1.0f);
        http_start_btn.setEnabled(false);
        http_start_btn.setAlpha(0.5f);
    }

    public void endHTTP() {
        Log.i(LOG_TAG, "Ending HTTP....");
        Intent itt = new Intent(this, PingService.class);
        itt.setAction(PingService.END_HTTP);
        startService(itt);


        http_set_btn.setEnabled(true);
        http_set_btn.setAlpha(1.0f);
        http_end_btn.setEnabled(false);
        http_end_btn.setAlpha(0.5f);
        http_start_btn.setEnabled(true);
        http_start_btn.setAlpha(1.0f);
    }

    public void startHTTPS() {
        Log.i(LOG_TAG, "Starting HTTPS....");
        Intent itt = new Intent(this, PingService.class);
        itt.putExtra("Ping Settings", (Parcelable) httpsPingSettings);
        itt.setAction(PingService.START_HTTPS);
        startService(itt);


        https_set_btn.setEnabled(false);
        https_set_btn.setAlpha(0.5f);
        https_end_btn.setEnabled(true);
        https_end_btn.setAlpha(1.0f);
        https_start_btn.setEnabled(false);
        https_start_btn.setAlpha(0.5f);
    }

    public void endHTTPS() {
        Log.i(LOG_TAG, "Ending HTTPS....");
        Intent itt = new Intent(this, PingService.class);
        itt.setAction(PingService.END_HTTPS);
        startService(itt);

        https_set_btn.setEnabled(true);
        https_set_btn.setAlpha(1.0f);
        https_end_btn.setEnabled(false);
        https_end_btn.setAlpha(0.5f);
        https_start_btn.setEnabled(true);
        https_start_btn.setAlpha(1.0f);
    }

    public void updateAlert() {
        if (alertToggle.isChecked()) {
            enableAlert();
        } else {
            disableAlert();
        }
    }

    public void enableAlert() {
        int threshold;
        try {
            threshold = Integer.parseInt(alertThresh.getText().toString());
        } catch (Exception e) {
            Toast.makeText(this, "Enter valid threshold.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(LOG_TAG, "Enabling Alert....");
        Intent itt = new Intent(this, PingService.class);
        itt.setAction(PingService.START_ALERT);
        itt.putExtra("Threshold", threshold);
        startService(itt);
    }

    public void disableAlert() {
        Log.d(LOG_TAG, "Disabling Alert....");
        Intent itt = new Intent(this, PingService.class);
        itt.setAction(PingService.END_ALERT);
        startService(itt);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.icmp_start) {
            startICMP();
            updateAlert();
        } else if (id == R.id.icmp_end) {
            endICMP();
        } else if (id == R.id.http_start) {
            startHTTP();
            updateAlert();
        } else if (id == R.id.http_end) {
            endHTTP();
        } else if (id == R.id.https_start) {
            startHTTPS();
            updateAlert();
        } else if (id == R.id.https_end) {
            endHTTPS();
        } else if (id == R.id.icmp_settings) {
            PingSettingsGetter psg = new PingSettingsGetter(this);
            psg.setTitle("ICMP Ping Settings");
            psg.setDefaults(icmpPingSettings);
            psg.setOnReturnListener(new PingSettingsReturnListener() {
                @Override
                public void onDialogReturn(PingSettings ps) {
                    icmpPingSettings = ps;
                    Log.d(LOG_TAG, ps.toString());
                    updateInformation();
                }
            });
            psg.init();
        } else if (id == R.id.http_settings) {
            PingSettingsGetter psg = new PingSettingsGetter(this);
            psg.setTitle("HTTP Ping Settings");
            psg.setDefaults(httpPingSettings);
            psg.setOnReturnListener(new PingSettingsReturnListener() {
                @Override
                public void onDialogReturn(PingSettings ps) {
                    httpPingSettings = ps;
                    Log.d(LOG_TAG, ps.toString());
                    updateInformation();
                }
            });
            psg.init();
        } else if (id == R.id.https_settings) {
            PingSettingsGetter psg = new PingSettingsGetter(this);
            psg.setTitle("HTTPS Ping Settings");
            psg.setDefaults(httpsPingSettings);
            psg.setOnReturnListener(new PingSettingsReturnListener() {
                @Override
                public void onDialogReturn(PingSettings ps) {
                    httpsPingSettings = ps;
                    Log.d(LOG_TAG, ps.toString());
                    updateInformation();
                }
            });
            psg.init();
        } else if (id == R.id.alert_toggle) {
            updateAlert();
        }
    }

    public void appendData(PingInfo pi) {

        if (pi == null) {

        } else if (pi.getPingType() == PingInfo.ICMP_PING) {

            int minimum_size = (int) (60000.0 / icmpPingSettings.getRepeat()) + 1;

            IcmpInformationArray.add(pi);
            while (IcmpInformationArray.size() > minimum_size) IcmpInformationArray.remove(0);

        } else if (pi.getPingType() == PingInfo.HTTP_REQUEST) {

            int minimum_size = (int) (60000.0 / httpPingSettings.getRepeat()) + 1;

            HttpInformationArray.add(pi);
            while (HttpInformationArray.size() > minimum_size) HttpInformationArray.remove(0);

        } else if (pi.getPingType() == PingInfo.HTTP_SECURE) {

            int minimum_size = (int) (60000.0 / httpsPingSettings.getRepeat()) + 1;

            HttpsInformationArray.add(pi);
            while (HttpsInformationArray.size() > minimum_size) HttpsInformationArray.remove(0);

        }
    }

    public void trimData(long time, int threshold) {
        for (int i = IcmpInformationArray.size() - 1; i >= 0; i--) {
            if (IcmpInformationArray.get(i).timeDifferenceSeconds(time) > threshold) {
                IcmpInformationArray.remove(i);
            }
        }
        for (int i = HttpInformationArray.size() - 1; i >= 0; i--) {
            if (HttpInformationArray.get(i).timeDifferenceSeconds(time) > threshold) {
                HttpInformationArray.remove(i);
            }
        }
        for (int i = HttpsInformationArray.size() - 1; i >= 0; i--) {
            if (HttpsInformationArray.get(i).timeDifferenceSeconds(time) > threshold) {
                HttpsInformationArray.remove(i);
            }
        }
    }

    public void redrawGraphs() {
        long currentTime = System.currentTimeMillis();

        trimData(currentTime, 60);
        //double maxY=1;

        ArrayList<DataPoint> IcmpDataPts = new ArrayList<DataPoint>();
        for (int i = IcmpInformationArray.size() - 1; i >= 0; i--) {
            PingInfo currentInfo = IcmpInformationArray.get(i);
            if (currentInfo != null) {
                if (currentInfo.isSuccess()) {
                    IcmpDataPts.add(new DataPoint(currentInfo.timeDifferenceSeconds(currentTime), currentInfo.getPingTimeSec()));
                    //if (currentInfo.getPingTimeSec()>maxY) maxY=currentInfo.getPingTimeSec();
                }
            }
        }
        icmpLineSeries.resetData(IcmpDataPts.toArray(new DataPoint[0]));

        ArrayList<DataPoint> HttpDataPts = new ArrayList<DataPoint>();
        for (int i = HttpInformationArray.size() - 1; i >= 0; i--) {
            PingInfo currentInfo = HttpInformationArray.get(i);
            if (currentInfo != null) {
                if (currentInfo.isSuccess()) {
                    HttpDataPts.add(new DataPoint(currentInfo.timeDifferenceSeconds(currentTime), currentInfo.getPingTimeSec()));
                    //if (currentInfo.getPingTimeSec()>maxY) maxY=currentInfo.getPingTimeSec();
                }
            }
        }
        httpLineSeries.resetData(HttpDataPts.toArray(new DataPoint[0]));

        ArrayList<DataPoint> HttpsDataPts = new ArrayList<DataPoint>();
        for (int i = HttpsInformationArray.size() - 1; i >= 0; i--) {
            PingInfo currentInfo = HttpsInformationArray.get(i);
            if (currentInfo != null) {
                if (currentInfo.isSuccess()) {
                    HttpsDataPts.add(new DataPoint(currentInfo.timeDifferenceSeconds(currentTime), currentInfo.getPingTimeSec()));
                    //if (currentInfo.getPingTimeSec()>maxY) maxY=currentInfo.getPingTimeSec();
                }
            }
        }
        httpsLineSeries.resetData(HttpsDataPts.toArray(new DataPoint[0]));

        //gv.getViewport().setMaxY(maxY);

    }

    public void updateResults(PingInfo pi) {

        if (pi == null) {

        } else if (pi.getPingType() == PingInfo.ICMP_PING) {
            icmp_result.setText(pi.getResultsString());
            icmp_icon.setImageResource(pi.getStatusDrawable());
        } else if (pi.getPingType() == PingInfo.HTTP_REQUEST) {
            http_result.setText(pi.getResultsString());
            http_icon.setImageResource(pi.getStatusDrawable());
        } else if (pi.getPingType() == PingInfo.HTTP_SECURE) {
            https_result.setText(pi.getResultsString());
            https_icon.setImageResource(pi.getStatusDrawable());
        }

    }

    public void updateInformation() {
        https_information.setText(httpsPingSettings.informationString());
        http_information.setText(httpPingSettings.informationString());
        icmp_information.setText(icmpPingSettings.informationString());
    }

    public void saveToFile() {
        Log.i(LOG_TAG, "Saving to file...");
        try {
            FileOutputStream fos = openFileOutput("cs_pinger_icmp", Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(icmpPingSettings);
            os.close();
            fos.close();

            fos = openFileOutput("cs_pinger_http", Context.MODE_PRIVATE);
            os = new ObjectOutputStream(fos);
            os.writeObject(httpPingSettings);
            os.close();
            fos.close();

            fos = openFileOutput("cs_pinger_https", Context.MODE_PRIVATE);
            os = new ObjectOutputStream(fos);
            os.writeObject(httpsPingSettings);
            os.close();
            fos.close();
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            Log.e(LOG_TAG, "File write error!\n" + errors.toString());
        }
    }

    public void loadFromFile() {
        Log.i(LOG_TAG, "Loading from file...");
        try {
            FileInputStream fis = openFileInput("cs_pinger_icmp");
            ObjectInputStream is = new ObjectInputStream(fis);
            PingSettings readPS = (PingSettings) is.readObject();
            is.close();
            fis.close();
            icmpPingSettings = readPS;

            fis = openFileInput("cs_pinger_http");
            is = new ObjectInputStream(fis);
            readPS = (PingSettings) is.readObject();
            is.close();
            fis.close();
            httpPingSettings = readPS;

            fis = openFileInput("cs_pinger_https");
            is = new ObjectInputStream(fis);
            readPS = (PingSettings) is.readObject();
            is.close();
            fis.close();
            httpsPingSettings = readPS;
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            Log.d(LOG_TAG, "File read error!\n" + errors.toString());
        }
    }

    class PingReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            PingInfo receivedInfo = intent.getParcelableExtra("New PingInfo");
            Log.v(LOG_TAG, "MainActivity, received intent.");


            updateResults(receivedInfo);
            appendData(receivedInfo);
            redrawGraphs();
        }
    }


}
