package com.chancorp.midproj;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;

public class PingSettingsGetter {

    public final static String LOG_TAG="CS_Pinger";


    EditText domain, repeat, timeout, slow;
    Spinner avatarSpinner;
    PingSettingsReturnListener psrl;
    Context c;
    String title;
    PingSettings defaults;

    public PingSettingsGetter(Context c){
        this.c=c;
        title="";
    }

    public void setTitle(String s){
        this.title=s;
    }

    public void setDefaults(PingSettings ps){
        this.defaults=ps;
    }

    public void setOnReturnListener(PingSettingsReturnListener psrl){
        this.psrl=psrl;
    }

    public void init(){

        AlertDialog.Builder builder = new AlertDialog.Builder(c);

        LayoutInflater inflater = (LayoutInflater) c.getSystemService( Context.LAYOUT_INFLATER_SERVICE );

        View view;

        view=inflater.inflate(R.layout.ping_settings_popup, null);


        builder.setView(view);


        domain = (EditText) view.findViewById(R.id.ping_sett_domain);
        repeat = (EditText) view.findViewById(R.id.ping_sett_repeat);
        timeout = (EditText) view.findViewById(R.id.ping_sett_timeout);
        slow = (EditText) view.findViewById(R.id.ping_sett_slow);

        if (defaults!=null){
            domain.setText(defaults.getDomain());
            repeat.setText(Integer.toString(defaults.getRepeat()));
            timeout.setText(Integer.toString(defaults.getTimeout()));
            slow.setText(Integer.toString(defaults.getSlow()));
        }



        builder.setMessage(title)
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        PingSettings ps = new PingSettings();

                        try {

                            ps.setDomain(domain.getText().toString());
                            ps.setRepeat(Integer.parseInt(repeat.getText().toString()));
                            ps.setSlow(Integer.parseInt(slow.getText().toString()));
                            ps.setTimeout(Integer.parseInt(timeout.getText().toString()));

                            if (psrl!=null) psrl.onDialogReturn(ps);
                        }catch(Exception e){
                            StringWriter errors = new StringWriter();
                            e.printStackTrace(new PrintWriter(errors));
                            Log.i(LOG_TAG, "Settings Parsing Failed.\n" + errors.toString());
                            Toast.makeText(c, "Ping Settings Parsing Failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

        builder.show();

    }

}
