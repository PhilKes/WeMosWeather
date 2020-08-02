#include <ESP8266WiFi.h>
#include <ESPAsyncTCP.h>
#include <ESPAsyncWebServer.h>
#include <FS.h>
#include <Wire.h>
#include "Seeed_BME280.h"
#include <NTPClient.h>
#include <WiFiUdp.h>
#include "api.h"

//Connect D0 to RST for ESP.deepSleep awake!

// THINGSPEAK_ONLY = 1: Dont use AsyncWebServer + NTP
// THINGSPEAK_ONLY = 0: Use AsnycWebser + NTP + Thingspeak postData
#define THINGSPEAK_ONLY 1

const char* api_server = "api.thingspeak.com";

BME280 bme280;

WiFiClient client;

//unsigned int periodTimeInMs = 16000;
unsigned int periodTimeInMs = 300000;
unsigned int blinkDelay=200;

#if THINGSPEAK_ONLY != 1
NTPClient timeClient(ntpUDP, "0.de.pool.ntp.org", 7200, periodTimeInMs);
WiFiUDP ntpUDP;
AsyncWebServer server(80);
#endif

int idx = 0;
const int sizeData = 288;
String dataTimes[sizeData] = {""};

float humidity[sizeData] = {0};
float humidityMax = 100;

float temp[sizeData] = {0};
float tempMax = 50;

float pressure[sizeData] = {0};
float pressureMax = 1200;

float altitude[sizeData] = {0};
float altitudeMax = 1000;

String getTemperature() {
  return String(temp[idx]);
}

String getHumidity() {
  return String(humidity[idx]);
}

String getPressure() {
  return String(pressure[idx]);
}

String getAltitude() {
  return String(altitude[idx]);
}


String getDataTimesString() {
  String s = "";
  for (int i = 0; i <= idx; i++) {
    s += "\"" + dataTimes[i] + "\",";
  }
  s.remove(s.length() - 1);
  return s;
}

String getDataListString(float data[]) {
  String s = "";
  for (int i = 0; i <= idx; i++) {
    s += String(data[i]) + ",";
  }
  s.remove(s.length() - 1);
  return s;
}
#if THINGSPEAK_ONLY == 0
// Replaces placeholder with LED state value
String processor(const String& var) {
  Serial.println(var);
  if ( var ==  "TEMP")
  {
    return getTemperature();
  }
  else if ( var ==  "HUM")
  {
    return getHumidity();
  }
  else if ( var ==  "PRESS")
  {
    return getPressure();
  }
  else if ( var ==  "ALT")
  {
    return getAltitude();
  }
  else if ( var ==  "TEMP_MAX")
  {
    return String(tempMax);
  }
  else if ( var ==  "HUM_MAX")
  {
    return String(humidityMax);
  }
  else if ( var ==  "PRESS_MAX")
  {
    return String(pressureMax);
  }
  else if ( var ==  "ALT_MAX")
  {
    return String(altitudeMax);
  }
  else if ( var ==  "TEMP_PERCENT")
  {
    return String(temp[idx] / tempMax * 100);
  }
  else if ( var ==  "PRESS_PERCENT")
  {
    return String(pressure[idx] / pressureMax * 100);
  }
  else if ( var ==  "ALT_PERCENT")
  {
    return String(altitude[idx] / altitudeMax * 100);
  }
  else if ( var ==  "HUM_PERCENT")
  {
    return String(humidity[idx] / humidityMax * 100);
  }
  else if ( var ==  "DATA_TIMES")
  {
    return getDataTimesString();
  }
  else if ( var ==  "DATA_TEMP")
  {
    return getDataListString(temp);
  }
  else if ( var ==  "UPDATE_DELAY")
  {
    return String(periodTimeInMs);
  }
  else
  {
    return "DATA_NOT_FOUND";
  }

}

String getJSONDataWithTime(float data[])
{
    String s="{\"data\":["+getDataListString(data)+"],";
    s+= "\"time\":["+getDataTimesString()+"]}";
    return s;
}
#endif
void setup() {
  // Serial port for debugging purposes
  Serial.begin(115200);
  pinMode(BUILTIN_LED, OUTPUT);
  // Initialize the sensor
  if (!bme280.init()) {
    Serial.println("Could not find a valid BME280 sensor, check wiring!");
    while (1);
  }

  // Initialize SPIFFS
  if (!SPIFFS.begin()) {
    Serial.println("An Error has occurred while mounting SPIFFS");
    return;
  }

  // Connect to Wi-Fi
  WiFi.begin(ssid, pass);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi..");
  }

  // Print ESP32 Local IP Address
  Serial.println(WiFi.localIP());

#if THINGSPEAK_ONLY == 0
  // Route for root / web page
  server.on("/", HTTP_GET, [](AsyncWebServerRequest * request) {
    request->send(SPIFFS, "/index.html", String(), false, processor);
  });

  // Route to load style.css file
  server.on("/style.css", HTTP_GET, [](AsyncWebServerRequest * request) {
    request->send(SPIFFS, "/style.css", "text/css");
  });

  server.on("/temperature", HTTP_GET, [](AsyncWebServerRequest * request) {
    request->send_P(200, "text/plain", getTemperature().c_str());
  });

  server.on("/humidity", HTTP_GET, [](AsyncWebServerRequest * request) {
    request->send_P(200, "text/plain", getHumidity().c_str());
  });

  server.on("/pressure", HTTP_GET, [](AsyncWebServerRequest * request) {
    request->send_P(200, "text/plain", getPressure().c_str());
  });

  server.on("/altitude", HTTP_GET, [](AsyncWebServerRequest * request) {
    request->send_P(200, "text/plain", getAltitude().c_str());
  });

 server.on("/temperaturedata", HTTP_GET, [](AsyncWebServerRequest * request) {
    request->send_P(200, "application/json",(getJSONDataWithTime(temp)).c_str());
  });

  timeClient.begin();
  // Start server
  server.begin();
#endif



  initializeData();
}

void initializeData()
{
  for (int i = 0; i < sizeData; i++) {
    temp[i] = 0;
    pressure[idx] = 0;
    altitude[idx] = 0;
    humidity[idx] = 0;
    dataTimes[idx] = "";
  }
  idx = -1;
}

void readNewData(){
  idx++;
  if (idx >= sizeData)
  {
    initializeData();
  }
#if THINGSPEAK_ONLY == 0
  timeClient.update();
  dataTimes[idx] = timeClient.getFormattedTime();
#endif
  temp[idx] = bme280.getTemperature();
  pressure[idx] = bme280.getPressure() / 100.0 ; // pressure in hPa
  altitude[idx] = bme280.calcAltitude(pressure[idx]);
  humidity[idx] = bme280.getHumidity();

  Serial.println("Temp: " + String(temp[idx]) + "C° Humidity: " + String(humidity[idx]) + " Pressure: " + String(pressure[idx]) + "hPa" + " Altitude: " + String(altitude[idx]));
}

void postData(float temperature, float humidity, float pressure){
  digitalWrite(BUILTIN_LED, LOW);
  delay(blinkDelay);
  digitalWrite(BUILTIN_LED, HIGH);
  // Send data to ThingSpeak
  if (client.connect(api_server,80)) {
  Serial.println("Connect to ThingSpeak - OK"); 

  String dataToThingSpeak = "";
  dataToThingSpeak+="GET /update.json?api_key=";
  dataToThingSpeak+=api_key;
   
  dataToThingSpeak+="&field1=";
  dataToThingSpeak+=String(temperature);

  dataToThingSpeak+="&field2=";
  dataToThingSpeak+=String(pressure);

  dataToThingSpeak+="&field3=";
  dataToThingSpeak+=String(humidity);
   
  dataToThingSpeak+=" HTTP/1.1\r\nHost: a.c.d\r\nConnection: close\r\n\r\n";
  dataToThingSpeak+="";
  client.print(dataToThingSpeak);

  int timeout = millis() + 5000;
  while (client.available() == 0) {
    if (timeout - millis() < 0) {
      Serial.println("Error: Client Timeout!");
      client.stop();
      return;
    }
  }
}
 while(client.available()){
    String line = client.readStringUntil('\r');
    Serial.print(line);
  }
}

void loop() {
     readNewData();
     postData(temp[idx], humidity[idx], pressure[idx]);
     delay(periodTimeInMs-blinkDelay);
     
}
