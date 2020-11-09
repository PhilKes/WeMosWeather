# WeMosWeather <img src="/assets/logo2.png" height="56"> 
## 3D Printed Solar powered Weather station
## Station

<img src="/assets/3dmodel.PNG" width="500">

* 3D Printed Case (PLA) (created with [Autodesk 123Design](https://autodesk-123d-design.en.lo4d.com/windows))
* [BME 280](https://de.aliexpress.com/item/32801639254.html?src=google&src=google&albch=shopping&acnt=494-037-6276&isdl=y&slnk=&plac=&mtctp=&albbt=Gploogle_7_shopping&aff_atform=google&aff_short_key=UneMJZVf&&albagn=888888&albcp=1705854617&albag=67310370915&trgt=743612850714&crea=de32801639254&netw=u&device=c&albpg=743612850714&albpd=de32801639254&gclid=EAIaIQobChMIqMSEpN7e6gIVkLt3Ch35YAZTEAQYASABEgLxBfD_BwE&gclsrc=aw.ds)
* [WeMos D1 Mini](https://docs.wemos.cc/en/latest/d1/d1_mini.html)
* Light Sensor
* 3.7V Lithium Ion Battery 18650

## Sketch
* Arduino .ino Sketch for WeMos D1 mini/ESP ([sketch_solar_weather_thingspeak](/sketch_solar_weather_thingspeak))
* Reads and Posts Sensor Data to Thingspeak in interval (every 5 min)
* Uses ESP.deepSleep to save most Power

#### Dependencies
* [ESPAsyncWebServer](https://github.com/me-no-dev/ESPAsyncWebServer)
* [ESPAsyncTCP](https://github.com/me-no-dev/ESPAsyncTCP)
* [ESP8266-FS-Uploader](https://randomnerdtutorials.com/install-esp8266-filesystem-uploader-arduino-ide/)

## Android App

<img src="/assets/dashboard.png" width="260">

* Fetches Data from Thingspeak Server
* Displays Data on UI Dashboard
* Shows History of all collected Data

### TODOs
* Home Screen Widget
* Weather Prediction based on Pressure values

#### Dependencies
* [HelloCharts](https://github.com/lecho/hellocharts-android)
* [Material Design](https://material.io/components)

## Thingspeak Api
* Create a free Account on Thingspeak
* Add a private Channel with Field 1 (Temperature), Field 2 (Pressure), Field 3 (Humidity)
* To allow the Android App and WeMos Sketch to post/read Data to/from Thingspeak add the following files:

/sketch/solar_weather_thingspeak/**api.h**:

    const char* api_key = <YOUR_THINGSPEAK_CHANNEL_API_KEY>;
    char ssid[] = <YOUR_WIFI>;
    char pass[] = <WIFI_PASSWORD>;

/android/app/src/main/res/values/**api.xml**:

    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <item name="THINGSPEAK_READ_KEY" type="string"><YOUR_THINGSPEAK_CHANNEL_READ_KEY>/item>
        <item name="THINGSPEAK_CHANNEL" type="string"><YOUR_THINGSPEAK_CHANNEL_ID></item>
    </resources>

## Web Page
* Alternative [sketch_solar_weather_webasync](/sketch_solar_weather_webasync)
* Basic static HTML Page to View Station Data directly from the WeMos
* Bootstrap Design
* URL: http://<WEMOS_IP_ADRESS>

### Sources
* [Thingspeak](https://thingspeak.com/)
* [Instructables](https://www.instructables.com/id/Solar-Powered-WiFi-Weather-Station/)
* [Esp8266 Bootstrap Example](https://diyprojects.io/bootstrap-create-beautiful-web-interface-projects-esp8266/#.XxcEGJ4zaUk)
