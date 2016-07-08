package com.cosmicsubspace.pinger;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Chan on 2015-11-06.
 */
public class PingInfo implements Parcelable {
    public static final int FAIL=-124;
    public static final int ICMP_PING=5838;
    public static final int HTTP_REQUEST=1937;
    public static final int HTTP_SECURE=192744;



    int TTL;
    float pingTime;
    boolean success;
    int pingType;
    long returnTime;
    int slowThreshold;


    public PingInfo(){
        TTL=0;
        pingTime =1;
        success=true;
        pingType=-1;
    }



    public PingInfo(Parcel p){
        TTL=p.readInt();
        pingTime =p.readFloat();
        success=p.readInt()!=0;
        pingType=p.readInt();
    }

    public double timeDifferenceSeconds(long currentTime){
        return (currentTime-returnTime)/1000.0;
    }



    public void parseInfoFromConsole(String str){
        String[] ss=str.split("/");
        String average_time=ss[ss.length-3];
        setPingTime(Float.parseFloat(average_time));
        setTTL(0); //TODO actual parsing
        setSuccess(true);

    }

    public String typeToString(){
        if (pingType==ICMP_PING) return "ICMP Ping";
        if (pingType==HTTP_REQUEST) return "HTTP Request";
        if (pingType==HTTP_SECURE) return "HTTPS Request";
        else return "Unknown type.";
    }

    public String getResultsString(){
        if (success) return ""+pingTime+"ms";
        else return "Ping Failed.";
    }

    public String toString(){
        String res="";
        if (isSuccess()) res="Ping: "+typeToString()+" | "+ getPingTime()+"ms | TTL: "+getTTL();
        else res="Ping Failed.";
        return res;
    }

    public String getNotificationString(){
        String res="";
        if (pingType==ICMP_PING) res=res+"ICMP:";
        if (pingType==HTTP_REQUEST) res=res+"HTTP:";
        if (pingType==HTTP_SECURE) res=res+"HTTPS:";

        if (isSuccess()) res=res+(int)getPingTime()+"ms";

        else res=res+"Fail";
        return res;
    }

    public boolean isSlow(){

            if (pingTime>slowThreshold) return true;
            else return false;

    }





    public int getStatusDrawable(){
        if (!isSuccess()) return R.drawable.status_bad;
        else{
            if (isSlow()) return R.drawable.status_slow;
            else return R.drawable.status_good;
        }
    }
    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(TTL);
        parcel.writeFloat(pingTime);
        parcel.writeInt(success ? 1 : 0);
        parcel.writeInt(pingType);
    }
    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<PingInfo> CREATOR
            =new Parcelable.Creator<PingInfo>() {
        public PingInfo createFromParcel(Parcel in){
            return new PingInfo(in);
        }
        public PingInfo[] newArray(int size){
            return new PingInfo[size];
        }
    };



    public int getPingType() {
        return pingType;
    }

    public void setPingType(int pingType) {
        this.pingType = pingType;
    }
    public int getTTL() {
        return TTL;
    }

    public void setTTL(int TTL) {
        this.TTL = TTL;
    }

    public float getPingTime() {
        return pingTime;
    }
    public float getPingTimeSec(){
        return pingTime/1000.0f;
    }

    public void setPingTime(float pingTime) {
        this.pingTime = pingTime;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
    public int getSlowThreshold() {
        return slowThreshold;
    }

    public void setSlowThreshold(int slowThreshold) {
        this.slowThreshold = slowThreshold;
    }



    public long getReturnTime() {
        return returnTime;
    }

    public void setReturnTime(long returnTime) {
        this.returnTime = returnTime;
    }
}
