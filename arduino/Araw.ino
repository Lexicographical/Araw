#include <Arduino.h>
#include <Wire.h>
#include <Adafruit_Sensor.h>
#include <Adafruit_BMP085_U.h>
#include <DHT.h>

Adafruit_BMP085_Unified bmp = Adafruit_BMP085_Unified(10085);

int DHTPIN = 2; 
DHT dht(DHTPIN, DHT22);

int co_d = 3;
int alarm;

int chk;
float hum; 
float temp;
float heatIndex;
float pressure;
float SEALEVELHPA = 1013.25;
int co;
bool dht_ready = true; // get dht every other loop because max 0.5Hz rate


void setup()
{
  Serial.begin(9600);
  Serial.println("Begin setup");
  pinMode(co_d, INPUT);
  dht.begin();
  bmp.begin();
  Serial.println("Setup complete.");
}

void loop()
{

  alarm = digitalRead(co_d) + 1;
  bmp.getPressure(&pressure);
  bmp.getTemperature(&temp);

  if (dht_ready) {
    hum = dht.readHumidity();
    heatIndex = dht.computeHeatIndex(temp, hum, false);
    dht_ready = false;
  } else {
    dht_ready = true;
  }
  String request = "";
  request += temp;
  request += " ";
  request += hum;
  request += " ";
  request += heatIndex;
  request += " ";
  request += pressure;
  request += " ";
  request += alarm;
  request += "\n";

  Serial.print(request);
  
  delay(500);
}
