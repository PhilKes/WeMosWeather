// https://github.com/PhilKes/WeMosWeather
#include <ESP8266WiFi.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BME280.h>
#include <WiFiUdp.h>

#define SEALEVELPRESSURE_HPA (1013.25)

//Connect D0 to RST for ESP.deepSleep awake!

// BME280 <-> Wemos
// SDA - D2
// SCL - D1
// GND - GND
// VIN 3.3V

char ssid[] = "WLAN Ke";
char pass[] = "3616949541664967";

const char* api_server = "api.thingspeak.com";
const char* api_key = "OO615HXB4VWR8TKR";

Adafruit_BME280 bme280;

WiFiClient client;

//unsigned int periodTimeInMs = 16000;
unsigned int periodTimeInMs = 3000;
unsigned int blinkDelay = 200;

float humidity = 0;
float temp = 0;
float pressure = 0;
float altitude = 0;

void initializeData()
{
  temp = 0;
  pressure = 0;
  humidity = 0;
}

void readNewData() {

  temp = bme280.readTemperature();
  pressure = bme280.readPressure() / 100.0 ; // pressure in hPa
  humidity = bme280.readHumidity();
  altitude = bme280.readAltitude(SEALEVELPRESSURE_HPA);

  Serial.println("Temp: " + String(temp) + "C° Humidity: " + String(humidity) + " Pressure: " + String(pressure) + "hPa"+ "Altitude: "+String(altitude));
}



void setup() {
  // Serial port for debugging purposes
  Serial.begin(115200);
  pinMode(BUILTIN_LED, OUTPUT);
  // Initialize the sensor
  bme280.begin(0x76);

 
  initializeData();

  readNewData();
  ESP.deepSleep((periodTimeInMs - blinkDelay)*1000);
}


void loop() {

}
