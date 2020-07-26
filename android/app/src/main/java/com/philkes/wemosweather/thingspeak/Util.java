package com.philkes.wemosweather.thingspeak;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Util {

    private static GsonBuilder gsonBuilder=new GsonBuilder();
    public static Gson gson;

    static {
        gsonBuilder.registerTypeAdapter(DataEntry.class, new DataEntry.DataEntryAdapter());
        gsonBuilder.registerTypeAdapter(DataSet.class, new DataSet.DataSetAdapter());
        gson=gsonBuilder.create();
    }

    public static final String READ_KEY="OO615HXB4VWR8TKR";
    public static final String CHANNEL_ID="1093516";
    public static final String THINGSPEAK_URL="https://api.thingspeak.com/";
    public static final String TIME_ZOME_PARAM= "&timezone=Europe/Berlin";

    public static String getLastChannelEntryURL() {
        return THINGSPEAK_URL + "/channels/" + CHANNEL_ID + "/feeds/last.json?api_key="
                + READ_KEY + TIME_ZOME_PARAM;
    }
    public static String getChannelFeedsURL() {
        return THINGSPEAK_URL + "/channels/" + CHANNEL_ID + "/feeds.json?api_key=" +
                READ_KEY +"&days=7"+ TIME_ZOME_PARAM;
    }
}
