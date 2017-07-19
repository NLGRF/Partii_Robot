#include <MicroGear.h>
#include <ESP8266WiFi.h>
#include <stdlib.h>

// constants won't change. They're used here to 
// set pin numbers:
#define D0 16     // USER LED Wake and 
#define D1 5     //MotorA
#define D2 4      //MotorA
#define D3 0      //MotorB
#define D4 2      //MotorB
#define D5 14     //Standby
#define D6 12     //PWMA

#define D8 15     //MotorC
#define D9 3      //MotorC

#define ledPin D0
#define MotorA1  D1        // the number of the LED pin
#define MotorA2  D2        // the number of the LED pin

#define MotorB1  D3        // the number of the LED pin
#define MotorB2  D4        // the number of the LED pin

#define MotorC1  D8        // the number of the LED pin
#define MotorC2  D9        // the number of the LED pin

#define STANDBY  D5

#define PWMPORTA D6

int duty_cycle = 511; //0..1023 (0% ถึง 100%)
int updown = 0;

const char* ssid     = ""; // wifi name
const char* password = ""; // wifi password

#define APPID   "" // Netpie APP ID
#define KEY     "" // Netpie KEY
#define SECRET  "" // Netpie SECRET KEY

#define ALIAS   "NodeMCU1"
#define TargetWeb "DigitalOUTPUT_HTML_web"

WiFiClient client;
MicroGear microgear(client);

void onMsghandler(char *topic, uint8_t* msg, unsigned int msglen) 
{
  Serial.print("Incoming message --> ");
  Serial.print(topic);
  Serial.print(" : ");
  char strState[msglen];
  for (int i = 0; i < msglen; i++) 
  {
    strState[i] = (char)msg[i];
    Serial.print((char)msg[i]);
  }
  Serial.println();

  String stateStr = String(strState).substring(0, msglen);
  
  if(stateStr == "Go") 
  {
    digitalWrite(MotorA1, LOW);
    digitalWrite(MotorA2, HIGH);

    digitalWrite(MotorB1, LOW);
    digitalWrite(MotorB2, HIGH);
    microgear.chat(TargetWeb, "Go");
    
  } 
  else if (stateStr == "TurnLeft") 
  {
    digitalWrite(MotorA1, HIGH);
    digitalWrite(MotorA2, LOW);

    digitalWrite(MotorB1, LOW);
    digitalWrite(MotorB2, HIGH);
    microgear.chat(TargetWeb, "TurnLeft");
  }
  else if (stateStr == "TurnRight") 
  {
    digitalWrite(MotorA1, LOW);
    digitalWrite(MotorA2, HIGH);

    digitalWrite(MotorB1, HIGH);
    digitalWrite(MotorB2, LOW);
    microgear.chat(TargetWeb, "TurnRight");
  }
  else if (stateStr == "Upturn") 
  {
    digitalWrite(MotorA1, HIGH);
    digitalWrite(MotorA2, LOW);

    digitalWrite(MotorB1, HIGH);
    digitalWrite(MotorB2, LOW);
    microgear.chat(TargetWeb, "Upturn");
  }
  else if (stateStr == "Stop") 
  {
    digitalWrite(MotorA1, LOW);
    digitalWrite(MotorA2, LOW);

    digitalWrite(MotorB1, LOW);
    digitalWrite(MotorB2, LOW);
    microgear.chat(TargetWeb, "Stop");
  }
  else if (stateStr == "Hold") 
  {
    digitalWrite(MotorC1, LOW);
    digitalWrite(MotorC2, LOW);
    microgear.chat(TargetWeb, "Hold");
  }
  else if (stateStr == "Catch") 
  {
    
    digitalWrite(MotorC1, LOW);
    digitalWrite(MotorC2, HIGH);
    delay(3000);
    digitalWrite(MotorC1, LOW);
    digitalWrite(MotorC2, LOW);
    
    microgear.chat(TargetWeb, "Catch");
  }
  else if (stateStr == "Put") 
  {
    digitalWrite(MotorC1, HIGH);
    digitalWrite(MotorC2, LOW);
    delay(3000);
    digitalWrite(MotorC1, LOW);
    digitalWrite(MotorC2, LOW);
    
    microgear.chat(TargetWeb, "Put");
  }else if(stateStr == "IncreaseSpeed") 
  {
    duty_cycle = duty_cycle+100;
    String spmsg =  "IncreaseSpeed";
    Serial.print(spmsg);
    Serial.print(" : ");
    Serial.print(duty_cycle);
    Serial.println();
    microgear.chat(TargetWeb, spmsg);
  }
  else if(stateStr == "DecreaseSpeed") 
  {
    duty_cycle = duty_cycle-100;
    String spmsg =  "DecreaseSpeed";
    Serial.print(spmsg);
    Serial.print(" : ");
    Serial.print(duty_cycle);
    Serial.println();
    microgear.chat(TargetWeb, spmsg);
  }
  else if(stateStr.toInt() >= 0) 
  {
    duty_cycle = stateStr.toInt();
    String spmsg =  "ChangeSpeed:" + stateStr;
    microgear.chat(TargetWeb, spmsg);
  }
}

void onConnected(char *attribute, uint8_t* msg, unsigned int msglen) 
{
  Serial.println("Connected to NETPIE...");
  microgear.setAlias(ALIAS);
}

void setup() 
{
    /* Event listener */
    microgear.on(MESSAGE,onMsghandler);
    microgear.on(CONNECTED,onConnected);

    Serial.begin(115200);
    Serial.println("Starting...");

    WiFi.begin(ssid, password);
    while (WiFi.status() != WL_CONNECTED) 
    {
       delay(250);
       Serial.print(".");
    }

    Serial.println("WiFi connected");  
    Serial.println("IP address: ");
    Serial.println(WiFi.localIP());

    microgear.init(KEY,SECRET,ALIAS);
    microgear.connect(APPID);

    pinMode(ledPin,OUTPUT);
    pinMode(MotorA1,OUTPUT);
    pinMode(MotorA2,OUTPUT);
    
    pinMode(MotorB1,OUTPUT);
    pinMode(MotorB2,OUTPUT);
    
    pinMode(MotorC1,OUTPUT);
    pinMode(MotorC2,OUTPUT);
    
    pinMode(STANDBY,OUTPUT);
    pinMode(PWMPORTA,OUTPUT);

    digitalWrite(STANDBY,HIGH); //Motorn ON
    digitalWrite(ledPin,LOW); // Turn On LED
    
    digitalWrite(MotorA1, LOW);
    digitalWrite(MotorA2, LOW);

    digitalWrite(MotorB1, LOW);
    digitalWrite(MotorB2, LOW);

    digitalWrite(MotorC1, LOW);
    digitalWrite(MotorC2, LOW);

    
}

void loop() 
{ 
  
   // update the duty cycle of the PWM signal
   analogWrite( PWMPORTA, duty_cycle );
   delayMicroseconds(1000);


  if(microgear.connected()) 
  {
    microgear.loop();
    //Serial.println("connect...");
  } 
  else 
  {
    Serial.println("connection lost, reconnect...");
    microgear.connect(APPID);
  }
}
