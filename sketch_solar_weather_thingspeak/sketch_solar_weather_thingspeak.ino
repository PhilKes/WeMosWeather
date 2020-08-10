// https://github.com/PhilKes/WeMosWeather
#include <ESP8266WiFi.h>
#include <Wire.h>
#include "Seeed_BME280.h"
#include <WiFiUdp.h>
#include "api.h"

//Connect D0 to RST for ESP.deepSleep awake!
//BME 280 <-> Wemos
// SDA - D2
// SCL - D1
// VIN - 3.3V
// GND - G

const char* api_server = "api.thingspeak.com";

BME280 bme280;

WiFiClient client;

//unsigned int periodTimeInMs = 16000;
unsigned int periodTimeInMs = 300000;
unsigned int blinkDelay = 200;

const int LED_BRIGHTNESS=A0;

float humidity = 0;
float temp = 0;
float pressure = 0;
float brightness= 0;

void initializeData()
{
  temp = 0;
  pressure = 0;
  humidity = 0;
}

void readNewData() {

  temp = bme280.getTemperature();
  pressure = bme280.getPressure() / 100.0 ; // pressure in hPa
  humidity = bme280.getHumidity();
  brightness= analogRead(A0);

  Serial.println("Temp: " + String(temp) + "C° Humidity: " + String(humidity) + " Pressure: " + String(pressure) + "hPa Brightness: "+String(brightness));
}


void postData(float temperature, float humidity, float pressure) {
  digitalWrite(BUILTIN_LED, LOW);
  delay(blinkDelay);
  digitalWrite(BUILTIN_LED, HIGH);

  // Send data to ThingSpeak
  if (client.connect(api_server, 80)) {
    Serial.println("Connect to ThingSpeak - OK");

    String dataToThingSpeak = "";
    dataToThingSpeak += "GET /update.json?api_key=";
    dataToThingSpeak += api_key;

    dataToThingSpeak += "&field1=";
    dataToThingSpeak += String(temperature);

    dataToThingSpeak += "&field2=";
    dataToThingSpeak += String(pressure);

    dataToThingSpeak += "&field3=";
    dataToThingSpeak += String(humidity);

    dataToThingSpeak += "&field4=";
    dataToThingSpeak += String(brightness);

    dataToThingSpeak += " HTTP/1.1\r\nHost: a.c.d\r\nConnection: close\r\n\r\n";
    dataToThingSpeak += "";
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
  while (client.available()) {
    String line = client.readStringUntil('\r');
    Serial.print(line);
  }
}

void setup() {
  // Serial port for debugging purposes
  Serial.begin(115200);
  pinMode(BUILTIN_LED, OUTPUT);
  pinMode(LED_BRIGHTNESS,INPUT);
  // Initialize the sensor
  if (!bme280.init()) {
    Serial.println("Could not find a valid BME280 sensor, check wiring!");
    while (1);
  }

  // Connect to Wi-Fi
  WiFi.begin(ssid, pass);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi..");
  }

  // Print ESP32 Local IP Address
  Serial.println(WiFi.localIP());

  initializeData();

  readNewData();
  postData(temp, humidity, pressure);
  ESP.deepSleep((periodTimeInMs - blinkDelay)*1000);
}


void loop() {

}
