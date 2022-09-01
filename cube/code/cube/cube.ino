#include <Preferences.h>
#include "WiFi.h"
#include "BluetoothSerial.h"
#include "ArduinoJson.h"
#include <PubSubClient.h>
#include "DHT.h"

#define MQTT_MAX_BYTES 512

String mac_address = String(WiFi.macAddress());
String CUBE_ID = "Cube_"+mac_address;
String TEMPERATURE_ID = "Temp_"+mac_address;
String HUMIDITY_ID = "Hum_"+mac_address;
String LIGHT_ID = "Light_"+mac_address;
String MOVEMENT_ID = "Move_"+mac_address;


//config when to trigger updates for sensors in order to reduce spam
//update sensor if the current value is different than the last published one
#define SENSOR_TEMPERATURE_UPDATE_DIFF 0.3
#define SENSOR_HUMIDITY_UPDATE_DIFF 1.0
#define SENSOR_LIGHT_UPDATE_DIFF 100
//read temperature and humidity every 6 seconds
#define SENSOR_TEMPERATURE_READ_INTERVAL 1000*6
//read sensors every 0.5 seconds
#define SENSOR_MOVEMENT_READ_INTERVAL 1000*0.5

float last_published_temperature = -273.0;
float last_published_humidity = -SENSOR_HUMIDITY_UPDATE_DIFF;
int last_published_light = -SENSOR_LIGHT_UPDATE_DIFF;
int last_published_movement = 0;
long next_temperature_read_time =millis();
long next_movement_read_time =millis();
int lastButtonState=HIGH;
boolean is_relay_on = false;



//mqtt info
const char* mqtt_server = "broker.emqx.io";
const int mqtt_port = 1883;
char* mqtt_username = "emqx";
char* mqtt_password = "public";
char* devices_topic = "diplomatikiuth/autohome/devices";
char* app_topic = "diplomatikiuth/autohome/app";
char* server_topic = "diplomatikiuth/autohome/server";
WiFiClient espClient;
PubSubClient client(espClient);

//pins
#define TEMPERATURE_PIN 22
#define LIGHT_PIN 34
#define MOVEMENT_PIN 13
#define BUTTON_LED_PIN 17
#define BUTTON_PIN 16
#define RELAY_PIN 12
#define DHTTYPE DHT11

//BUFFERS
//password and ssid buffer
char ssid_buffer[32];
char pwd_buffer[32];
//json Document
StaticJsonDocument<MQTT_MAX_BYTES> JSONDoc;



BluetoothSerial SerialBT;
Preferences preferences;

DHT dht(TEMPERATURE_PIN, DHTTYPE);

void setup() {

  preferences.begin("cube", false); 

  //check if ssid and password are stored in memory
  String ssid = preferences.getString("ssid","");
  String pwd = preferences.getString("pwd","");
  
  if(ssid.length() > 0 && pwd.length() > 0){
    //connect to wifi
    ssid.toCharArray(ssid_buffer,32);
    pwd.toCharArray(pwd_buffer,32);
    WiFi.mode(WIFI_STA);
    WiFi.begin(ssid_buffer, pwd_buffer);
    while (WiFi.status() != WL_CONNECTED) {
      delay(500);
    }
    Serial.println("connected to wifi");
  }else{
    //connect with app and get wifi info
    getWifiInfoFromBluetooth();
  }
  preferences.end();

  //MQTT
  connectMQTT();
  
  //temperature and humidity sensor
  dht.begin();

  // Configure the pins as input or output
  pinMode(MOVEMENT_PIN, INPUT);
  pinMode(BUTTON_LED_PIN, OUTPUT);
  pinMode(BUTTON_PIN, INPUT_PULLUP);
  pinMode(RELAY_PIN, OUTPUT);

  //set initial state of relay to off
  toggleRelay(false);
}

void loop() {
  
  //MQTT loop
  client.loop();
  
  long timenow=millis();

  //read temperature/humidity and publish if necessary
  if(timenow >= next_temperature_read_time){
    next_temperature_read_time = timenow+SENSOR_TEMPERATURE_READ_INTERVAL;
    float t = dht.readTemperature();
    float h = dht.readHumidity();
    int l = analogRead(LIGHT_PIN);
    if(abs(t-last_published_temperature)>=SENSOR_TEMPERATURE_UPDATE_DIFF){
      publishSensorUpdate(TEMPERATURE_ID,String(t)+"°C");
      last_published_temperature=t;
    }
    if(abs(h-last_published_humidity)>=SENSOR_HUMIDITY_UPDATE_DIFF){
      publishSensorUpdate(HUMIDITY_ID,String(h)+"%");
      last_published_humidity=h;
    }
    if(abs(l-last_published_light)>=SENSOR_LIGHT_UPDATE_DIFF){
      int lightpercent = map(l, 0, 4095, 0, 100); //Convert analog values(0-4095) to percentage.
      publishSensorUpdate(LIGHT_ID,String(lightpercent)+"%");
      last_published_light=l;
    }
  }

  //movement sensor
  if(timenow >= next_movement_read_time){
    next_movement_read_time = timenow+SENSOR_MOVEMENT_READ_INTERVAL;
    boolean triggered = digitalRead(MOVEMENT_PIN)==HIGH;
    if(last_published_movement != triggered){
      publishSensorUpdate(MOVEMENT_ID,String(triggered?1:0));
      last_published_movement = triggered;
    }
    
  }

  //button only triggers from high to low, its when uses pushes it
  int buttonState = digitalRead(BUTTON_PIN);
  if(lastButtonState == HIGH && buttonState == LOW){
    toggleRelay(!is_relay_on);
  }
  lastButtonState=buttonState;
  
}

void callback(char *topic, byte *payload, unsigned int length) {

  DeserializationError err = deserializeJson(JSONDoc, payload);
  if(err) return;
  char id[CUBE_ID.length() + 1];
  CUBE_ID.toCharArray(id, CUBE_ID.length() + 1);
  //check if message is for this cube
  if(strcmp(JSONDoc["id"],id) != 0) return;
  if(strcmp(JSONDoc["action"],"trigger_device") == 0){
    toggleRelay(JSONDoc["trigger"]);
  }else if(strcmp(JSONDoc["action"],"remove_device") == 0){
    wipePreferences();
    JSONDoc.clear();
    JSONDoc["action"]="remove_device";
    JSONDoc["id"]=CUBE_ID;
    char output[MQTT_MAX_BYTES];
    serializeJson(JSONDoc, output);
    client.publish(server_topic,  output);
    delay(500);
    ESP.restart();
  }
  
}

//publish sensorid value to server and app
void publishSensorUpdate(String sensor_id,String value){
  JSONDoc.clear();
  JSONDoc["action"]="device_status_update";
  JSONDoc["id"]=sensor_id;
  JSONDoc["status"]=value;
  char output[MQTT_MAX_BYTES];
  serializeJson(JSONDoc, output);
  client.publish(app_topic,  output);
  client.publish(server_topic,  output);
}


void toggleRelay(boolean turn_on){
  JSONDoc.clear();
  JSONDoc["action"]="device_status_update";
  JSONDoc["id"]=CUBE_ID;
  if(turn_on){
    JSONDoc["status"]=1;
    digitalWrite(BUTTON_LED_PIN, HIGH);
    digitalWrite(RELAY_PIN, LOW);
    is_relay_on=true;
  }else{
    JSONDoc["status"]=0;
    digitalWrite(BUTTON_LED_PIN, LOW);
    digitalWrite(RELAY_PIN, HIGH);
    is_relay_on=false;
  }

  //publish to server and app
  char output[MQTT_MAX_BYTES];
  serializeJson(JSONDoc, output);
  client.publish(app_topic,  output);
  client.publish(server_topic,  output);
  
}

void connectMQTT(){

  client.setBufferSize(MQTT_MAX_BYTES*8);
  client.setServer(mqtt_server, mqtt_port);//connecting to mqtt server
  client.setCallback(callback);
  
  while(!client.connected()){
    client.connect(CUBE_ID.c_str(), mqtt_username, mqtt_password);
  }

  client.subscribe(devices_topic);
  
  //publish device adoption, will get ignored by the server if its already in the database
  JSONDoc.clear();
  JSONDoc["action"]="device_adoption";
  JSONDoc["id"]=CUBE_ID;
  JsonObject sensor_temperature = JSONDoc.createNestedObject("sensor_temperature");
  JsonObject sensor_humidity = JSONDoc.createNestedObject("sensor_humidity");
  JsonObject sensor_light = JSONDoc.createNestedObject("sensor_light");
  JsonObject sensor_movement = JSONDoc.createNestedObject("sensor_movement");
  sensor_temperature["id"]= TEMPERATURE_ID;
  sensor_humidity["id"] = HUMIDITY_ID;
  sensor_light["id"] = LIGHT_ID;
  sensor_movement["id"] = MOVEMENT_ID;
  sensor_temperature["status"]= "";
  sensor_humidity["status"] = "";
  sensor_light["status"] = "";
  sensor_movement["status"] = "";
  char output[MQTT_MAX_BYTES];
  serializeJson(JSONDoc, output);
  client.publish(server_topic,  output);

}

void getWifiInfoFromBluetooth(){
  SerialBT.begin(CUBE_ID);
  while(true){
    if(SerialBT.available()){
      String btData = SerialBT.readString();
      auto error = deserializeJson(JSONDoc, btData);
      //Check for errors in parsing
      if (!error) {
        const char* ssid = JSONDoc["ssid"];
        const char* pass = JSONDoc["pwd"];
        //write to preferences 
        preferences.putString("ssid",String(ssid));
        preferences.putString("pwd",String(pass));
        preferences.end();
        //restart after 0.5 sec
        delay(500);
        ESP.restart();
      }
    }
  }

}

void wipePreferences(){
  preferences.begin("cube", false);
  preferences.clear();
  preferences.end();
}
