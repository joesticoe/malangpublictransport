package ap.mobile.malangpublictransport.utilities;

import android.content.Context;
import android.preference.PreferenceManager;

import ap.mobile.malangpublictransport.MapsActivity;

public class CDM {

    public static double cost = 4000D;

    public static String getApiBaseUrl(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("basepath", "http://175.45.187.243/routing");
    }

    public static Double getStandardCost() { return CDM.cost; }

    public static Double oneMeterInDegree() { return 0.00000898448D; }

    public static int getStandardDistance() {
        return 400;
    }

    public static int getDistance(Context context) {
        String cost = PreferenceManager.getDefaultSharedPreferences(context).getString("pref_walkingDistance",
                String.valueOf(CDM.getStandardDistance()));
        return Integer.valueOf(cost);
    }
}
