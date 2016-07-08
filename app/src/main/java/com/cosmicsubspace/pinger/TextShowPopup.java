package com.cosmicsubspace.pinger;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Chan on 2015-11-18.
 */
public class TextShowPopup {
    Context c;
    String msg, title;
    public TextShowPopup(Context c){
        this.c=c;

    }
    public void setText(String s){
        this.msg=s;
    }
    public void setTitle(String s){this.title=s;}
    public void init(){

        AlertDialog.Builder builder = new AlertDialog.Builder(c);

        LayoutInflater inflater = (LayoutInflater) c.getSystemService( Context.LAYOUT_INFLATER_SERVICE );

        View view;

        view=inflater.inflate(R.layout.scrollable_text, null);

        TextView tv=(TextView)view.findViewById(R.id.scrollable_text_body);
        tv.setText(msg);


        builder.setView(view);

        builder.setMessage(title).setPositiveButton("OK",null);


        builder.show();

    }
}
