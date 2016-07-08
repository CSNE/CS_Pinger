package com.cosmicsubspace.pinger;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by Chan on 11/12/2015.
 */
public class PingSettings implements Parcelable, Serializable{
    String domain;
    int repeat;
    int timeout;
    int slow;

    public PingSettings(){

    }
    public PingSettings(Parcel p){
        domain=p.readString();
        repeat=p.readInt();
        timeout=p.readInt();
        slow=p.readInt();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(domain);
        parcel.writeInt(repeat);
        parcel.writeInt(timeout);
        parcel.writeInt(slow);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final transient Parcelable.Creator<PingSettings> CREATOR
            =new Parcelable.Creator<PingSettings>() {
        public PingSettings createFromParcel(Parcel in){
            return new PingSettings(in);
        }
        public PingSettings[] newArray(int size){
            return new PingSettings[size];
        }
    };

    public String toString(){
        return "Domain: "+domain+" | Repeat: "+repeat+" | Timeout: "+timeout+" | Slow: "+slow;
    }

    public String informationString(){
        //return "Checking ["+domain+"]"+ " every "+timeout+"ms"+"\n"+"Timeout "+timeout+" ms, slow when greater than "+slow+"ms";
        return "Domain: "+domain+" | Repeat: "+repeat+"\nTimeout: "+timeout+" | Slow Threshold: "+slow;
    }






    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public int getRepeat() {
        return repeat;
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getSlow() {
        return slow;
    }

    public void setSlow(int slow) {
        this.slow = slow;
    }
}
