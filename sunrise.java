import java.util.Calendar;
import static java.lang.Math.*;

public class sunrise {
    public sunrise(){
    }
    public String getSunrise (int N, double latitude, double longitude, Boolean morn){
        //specify zenith externally?
        double zenith = 96;
        double localoffset = 0;
        if (isSummerTime(N)){localoffset = localoffset -1;}
        double fromRad = 180/3.14159;
        double toRad = 3.14159/180;
        double lnghour = longitude/15;
        double t;
        if (morn){ t = N+((6-lnghour)/24); }
        else{ t = N+((18-lnghour)/24); }
        double M = (0.9856*t) - 3.289;
        double L = M + (1.916*(sin(toRad*M)))+(0.020*(sin(2*toRad*M))) +282.634;
        while (L > 360){ L=L-360; }
        while (L < 0){ L=L+360; }
        double RA = fromRad*atan(0.91764*tan(toRad*L));
        while (RA > 360){ RA=RA-360; }
        while (RA < 0){ RA=RA+360; }
        double Lquadrant = floor(L/90)*90;
        double RAquadrant = floor(RA/90)*90;
        RA = (RA + (Lquadrant - RAquadrant))/15;
        double sinDec = 0.39782*sin(toRad*L);
        double cosDec = cos(asin(sinDec));
        double cosH = (cos(toRad*zenith)-(sinDec*sin(toRad*latitude)))/(cosDec*cos(toRad*latitude));
        double H;
        if (morn){ H = (360-fromRad*acos(cosH))/15; }
        else{ H = (fromRad*acos(cosH))/15; }
        double T = H+RA-(0.06571*t)-6.622;
        double UT = T - lnghour;
        while (UT > 24){ UT=UT-24; }
        while (UT < 0){ UT=UT+24; }
        double localT = UT - localoffset;
        long hour = (long) localT;
        long minute = (long)((localT-hour) * 60);
        String pad;
        if (minute < 10) {pad = "0";} else {pad="";}
        return hour+":"+pad+minute;
    }
    public String getTomorrowsSunrise(double latitude, double longitude){
        Calendar cal = Calendar.getInstance();
        int N = cal.get(Calendar.DAY_OF_YEAR) +1;
        return getSunrise(N,latitude,longitude,true);
    }
    public String getTodaysSunset(double latitude, double longitude){
        Calendar cal = Calendar.getInstance();
        int N = cal.get(Calendar.DAY_OF_YEAR);
        return getSunrise(N,latitude,longitude,false);
    }
    public Boolean isSummerTime(int N){
        return (N > 90) && (N < 300);
    }
}