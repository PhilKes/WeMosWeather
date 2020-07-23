#include <ESP8266WiFi.h>
#include "Seeed_BME280.h"
#include <Wire.h>
#include <NTPClient.h>
#include <WiFiUdp.h>
BME280 bme280;
// Set password to "" for open networks.
char ssid[] = "WLAN Ke";
char pass[] = "3616949541664967";
WiFiServer server(80);

WiFiUDP ntpUDP;

NTPClient timeClient(ntpUDP, "0.de.pool.ntp.org", 7200, 5000);

int idx=0;
const int sizeData=100;
String dataTimes[sizeData]={""};

float humidity[sizeData]={0};
float humidityMax=100;

float temp[100]={0};
float tempMax=50;

float pressure[100]={0};
float pressureMax=1200;

float altitude[100]={0};
float altitudeMax=1000;

void setup()
{
  Serial.begin(115200);
  if(!bme280.init()){
    Serial.println("Device error!");
  }

  delay(10);
 
 
  // Connect to WiFi network
  Serial.println();
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);
 
  WiFi.begin(ssid, pass);
 
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.println("WiFi connected");
 
  // Start the server
  server.begin();
  Serial.println("Server started");
 
  // Print the IP address
  Serial.print("Use this URL : ");
  Serial.print("http://");
  Serial.print(WiFi.localIP());
  Serial.println("/");
  idx=0;
  timeClient.begin();
}


String page(){
  String page="";
  page += "<!doctype html>";
  page += "<html lang='en'>";
  page += " <head>";
  page += "   <!-- Required meta tags -->";
  page += "   <meta charset='utf-8'>";
  page += "   <meta name='viewport' content='width=device-width, initial-scale=1, shrink-to-fit=no'>";
  page += "   <!-- Bootstrap CSS -->";
  page += "   <link rel='stylesheet' href='https://stackpath.bootstrapcdn.com/bootstrap/4.5.0/css/bootstrap.min.css' integrity='sha384-9aIt2nRpC12Uk9gS9baDl411NQApFmC26EwAOH8WgZl5MYYxFfc+NcPb1dKGj7Sk' crossorigin='anonymous'>";
  page += "   <title>";
  page += "WeMosWeather";
  page += "   </title>";
  page += " </head>";
  page += " <body>";
  page += "   <div class='container-fluid'>";
  page += "     <div class='row'>";
  page += "       <div class='col-md-12'>";
  page += "         <span class='badge badge-default'>";
  page += "Temperature";
  page += "         </span>";
  page += "         <div class='progress'>";
  page += "           <div class='progress-bar progress-bar-striped'  style='width:"+String(temp[idx]/tempMax*100)+ "%; background-color: red !important;'>";
  page += String(temp[idx])+" C°";
  page += "           </div>";
  page += "         </div>";
  page += "         <span class='badge badge-default'>";
  page += "Pressure";
  page += "         </span>";
  page += "         <div class='progress'>";
  page += "           <div class='progress-bar progress-bar-striped' style='width:"+String(pressure[idx]/pressureMax*100)+ "%; background-color: purple !important;'>";
  page += String(pressure[idx])+" hPa";
  page += "           </div>";
  page += "         </div>";
  page += "         <span class='badge badge-default'>";
  page += "Altitude";
  page += "         </span>";
  page += "         <div class='progress'>";
  page += "           <div class='progress-bar progress-bar-striped' style='width:"+String(altitude[idx]/altitudeMax*100)+ "%; background-color: green !important;' >";
  page += String(altitude[idx])+" m";
  page += "           </div>";
  page += "         </div>";
  page += "         <span class='badge badge-default'>";
  page += "Humidity";
  page += "         </span>";
  page += "         <div class='progress'>";
  page += "           <div class='progress-bar progress-bar-striped' style='width:"+String(humidity[idx]/humidityMax*100)+ "%; background-color: blue !important;'>";
  page += String(humidity[idx])+" %";
  page += "           </div>";
  page += "         </div>";
  page += "       </div>";
  page += "     </div>";
  page += "     <div class='row'>";
  page += "       <div class='col-md-12'>";
  page += "         <canvas id='myChart' width='800' height='200' />";
  page += "       </div>";
  page += "     </div>";
  page += "   </div>";  
  page += "   <script src='https://code.jquery.com/jquery-3.5.1.slim.min.js' integrity='sha384-DfXdz2htPH0lsSSs5nCTpuj/zy4C+OGpamoFVy38MVBnE+IbbVYUew+OrCXaRkfj' crossorigin='anonymous' ></script>";
  page += "   <script src='https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js' integrity='sha384-Q6E9RHvbIyZFJoft+2mJbHaEWldlvI9IOYy5n3zV9zzTtmI3UksdQRVvoxMfooAo' crossorigin='anonymous'></script>";
  page += "   <script src='https://stackpath.bootstrapcdn.com/bootstrap/4.5.0/js/bootstrap.min.js' integrity='sha384-OgVRvuATP1z7JjHLkuOU7Xw704+h835Lr+6QL9UvYjZE3Ipu6Tp75j7Bh/kR0JKI' crossorigin='anonymous'></script>";
  page += "   <script src='https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.5.0/Chart.min.js'></script>";
  page += "   <script>";
  page += "     var xTime = [ "+ getDataTimesString()+" ];";
  page += "     var yTemp = [ "+ getDataTempString()+" ];";
  page += "     var ctx = document.getElementById('myChart');";
  page += "     var myChart = new Chart(ctx, {";
  page += "       type: 'line',";
  page += "       data: {";
  page += "       labels: xTime,";
  page += "       datasets: [";
  page += "         { ";
  page += "         data: yTemp,";
  page += "         label: 'Temperature in C°',";
  page += "         borderColor: 'red',";
  page += "         fill: false";
  page += "         }";
  page += "       ]";
  page += "       } ";
  page += "     });";
  page += "   </script>";
  page += " </body>";
  page += "</html>";
  return page;
}

void initializeData()
{
    for(int i=0;i<sizeData;i++){
        temp[i]=0;
        pressure[idx]=0;
        altitude[idx]=0;
        humidity[idx]=0;
        dataTimes[idx]="";
    }
    idx=0;
}

String getDataTimesString(){
  String s="";
  for(int i=0;i<idx;i++){
    s+="\""+dataTimes[i]+"\",";  
  }
  s.remove(s.length()-1);
  return s;
}

String getDataTempString(){
  String s="";
  for(int i=0;i<idx;i++){
    s+=String(temp[i])+",";  
  }
  s.remove(s.length()-1);
  return s;
}

unsigned int lastTime=0;
unsigned int periodTimeInMs=5000;
void loop()
{

  if(millis()-lastTime >= periodTimeInMs){
    idx++;
    if(idx>=sizeData)
    {
      initializeData();
    }
    
    timeClient.update();
    dataTimes[idx]=timeClient.getFormattedTime();
    
    //get and print temperatures
    temp[idx] = bme280.getTemperature();
    //Serial.print("Temp: ");
    //Serial.print(temp);
    //Serial.println("C");//The unit for  Celsius because original arduino don't support speical symbols
    //get and print atmospheric pressure data
    pressure[idx] = bme280.getPressure()/100.0 ; // pressure in hPa
    //Serial.print("Pressure: ");
    //Serial.print(p);
    //Serial.println("hPa");
    //get and print altitude data
    altitude[idx] = bme280.calcAltitude(pressure[idx]);
    //Serial.print("Altitude: ");
    //Serial.print(altitude);
    //Serial.println("m");
    humidity[idx] = bme280.getHumidity();
    //Serial.print("Humidity: ");
    //Serial.print(humidity);
    //Serial.println("%");
    //ESP.deepSleep(6 * 1000); // deepSleep time is defined in microseconds.
     // Check if a client has connected
     Serial.println(dataTimes[idx]+"  "+"Temp: "+String(temp[idx])+"C° Humidity: "+String(humidity[idx])+" Pressure: "+String(pressure[idx])+"hPa"+" Altitude: "+String(altitude[idx]));
     lastTime=millis();
  }
   
  WiFiClient client = server.available();
  if (!client) {
    return;
  }
 
  // Wait until the client sends some data
  Serial.println("new client");
  while(!client.available()){
    delay(1);
  }
 
  // Read the first line of the request
  String request = client.readStringUntil('\r');
  Serial.println(request);
  client.flush();
 
  // Match the request
 
  int value = LOW;
  if (request.indexOf("/LED=ON") != -1) {
    value = HIGH;
  } 
  if (request.indexOf("/LED=OFF") != -1){
    value = LOW;
  }

  // Return the response
  client.println("HTTP/1.1 200 OK");
  client.println("Content-Type: text/html");
  client.println(""); //  do not forget this one
  client.println(page());
  /*
  client.println("<!DOCTYPE HTML>");
  client.println("<html>");
 
  client.print("Led pin is now: ");
 
  if(value == HIGH) {
    client.print("On");  
  } else {
    client.print("Off");
  }
  client.println("<br><br>");
  client.println("Click <a href=\"/LED=ON\">here</a> turn the LED on pin 5 ON<br>");
  client.println("Click <a href=\"/LED=OFF\">here</a> turn the LED on pin 5 OFF<br>");
  client.println("</html>");*/
  delay(1);
  Serial.println("Client disconnected");
  Serial.println("");
 
}
