package com.philkes.wemosweather.prediction;

import com.philkes.wemosweather.R;

public enum Weather {
    SUNNY("Sunny",R.drawable.weather_sunny),
    CLOUDY("Cloudy",R.drawable.weather_cloudy),
    RAIN_LIGHT("Light Rain",R.drawable.weather_rain_light),
    RAIN_HEAVY("Heavy Rain",R.drawable.weather_rain_heavy),
    STORM("Storm",R.drawable.weather_storm);

    public int drawableId;
    public String text;

    private Weather(String text,int drawableId) {
        this.drawableId=drawableId;
        this.text=text;
    }
}
