#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <ESP8266HTTPClient.h>
//#define Serial Serial


const int id = 2;
const char* ssid = "glask";       // WiFi SSID
const char* password = "12345678";  // WiFi password

const int vibratePin = 4;
const int sensorPin = 13;
const int redPin = 12;
const int greenPin = 14;
const int bluePin = 16;
const int levelPin1 = 5;
const int levelPin2 = 0;

unsigned long startTime = 0;
unsigned long stopTime = 0;
unsigned long lastTime = 0;
int timeShaked = 0;
boolean isAffoning = false;
boolean isFilled = false;
boolean isDrinking = false;
int shaked = 0;
int vibrating = 0;
int R = 0;
int G = 0;
int B = 0;

boolean toUpdate = true; // permet d'ajouter automatiquement le glask dans l'array des joueurs à la base

ESP8266WebServer server (80);

//@pre : recoit le string data contenant des donnees et eventuellement des occurences du separator.
//@post : renvoie la substring situee apres la index-ieme occurence du separator et avant la suivante
String getValue(String data, char separator, int index)
{
  int found = 0;
  int strIndex[] = {0, -1};
  int maxIndex = data.length()-1;

  for(int i=0; i<=maxIndex && found<=index; i++){
    if(data.charAt(i)==separator || i==maxIndex){
        found++;
        strIndex[0] = strIndex[1]+1;
        strIndex[1] = (i == maxIndex) ? i+1 : i;
    }
  }

  return found>index ? data.substring(strIndex[0], strIndex[1]) : "";
}


//Glask Function
//  levelPin1 = 5; Niveau bas plein/vide
//  levelPin2 = 0; Consommation ou non consommation
//  vibratePin = 4; Vibreur
//  redPin = 12 ; greePin = 14 ; bluePin = 16

// @pre : R, G, B = int de 0 à 255
// @post : allume la LED RGB
void RGB(int Re, int Gr, int Bl) {  
  R = Re;
  G = Gr;
  B = Bl;
  analogWrite(redPin, R);
  analogWrite(greenPin, G);
  analogWrite(bluePin, B); 
}

// @pre : levelPin1
// @post : true si le Glask est plein, false sinon ; toUpdate si changement d'état
boolean isFilledF()
{
  if(digitalRead(levelPin1) == 0)
  {
    if(!isFilled)
    {
      toUpdate = true;
    }
    isFilled = true;
  }
  else
  {
    if(isFilled)
    {
      toUpdate = true;
    }
    isFilled = false;
  }
}

// @pre : levelPin2
// @post : true si le Glask est en cours de consommation, false sinon ; toUpdate si changement d'état
boolean isDrinkingF()
{
  if(digitalRead(levelPin2) == 0)
  {
    if(!isDrinking)
    {
      toUpdate = true;
    }
    isDrinking = true;
  }
  else
  {
    if(isDrinking)
    {
      toUpdate = true;
    }
    isDrinking = false;
  }
}

// @pre : - (se lance dans le loop)
// @post : modifie lastTime, le temps de consommation en millisecondes
void chrono()
{
  if(isDrinking && !isAffoning)
  {
    //Serial.println("Start!");
    startTime = millis();
    isAffoning = true;
  }
  if(isAffoning && !isDrinking)
  {
    //Serial.println("Stop!");
    stopTime = millis();
    isAffoning = false;
    unsigned long elapsed = stopTime - startTime; //utilite? pk pas direct lastTime
    delay(500);
    lastTime = elapsed;
    toUpdate = true;
  }
}

// @pre : - (interrupt)
// @post : ajoute 1 à shaked (= compteur d'interrupt)
void shaking()
{
  shaked = shaked + 1;
}


// @pre : vibratePin
// @post : fait vibrer le glask ou l'arrete. L'arrete automatiquement apres 500 cycles sans reset de la part de la base (+ de 5sec)
void vibration(int vib)
{
  if(vib == 1)
  {
	if(vibrating < 500){
		digitalWrite(vibratePin, HIGH);
		vibrating++;
	} else{
		digitalWrite(vibratePin, LOW);
	}
  }
  else
  {
    digitalWrite(vibratePin, LOW);
	vibrating=0;
  }
}

void formatOut(String &dest) //Methode chelou d'utilisation des pointeurs mais why not :p
{
  String uniqueid = String(id);
  String red = String(R);
  String green = String(G);
  String blue = String(B);
  String isfilled = String(isFilled);
  String isdrinking = String(isDrinking);
  String lastime = String(lastTime);
  String nshaked = String(shaked);
  dest = uniqueid + ";" + red + ";" + green + ";" + blue + ";" + isfilled + ";" + isdrinking + ";" + lastime + ";" + nshaked;
} 

void handleRoot(){ 
  server.send(200, "text/plain", "Hello, I'm glask");
}

void show(){ 
  String format;
  formatOut(format);
  server.send(200, "text/plain", format);
}

void setup() 
{
  //Serial.begin(9600);
  pinMode(vibratePin, OUTPUT);
  pinMode(redPin, OUTPUT);
  pinMode(greenPin, OUTPUT);
  pinMode(bluePin, OUTPUT);  
  pinMode(levelPin1, INPUT);
  pinMode(levelPin2, INPUT);
  pinMode(sensorPin, INPUT_PULLUP);
  RGB(0, 0, 0);
  attachInterrupt(digitalPinToInterrupt(sensorPin), shaking, RISING);
  //Serial.println();
  //Serial.println("GPIO OK");
  
  WiFi.mode(WIFI_STA);
  WiFi.begin (ssid, password);
  while (WiFi.status() != WL_CONNECTED) 
  {
    RGB(255, 0, 0);
    delay(250); 
    RGB(0, 0, 0);
    delay(250); 
    //Serial.print(".");
  }
  //Serial.println(""); 
  //Serial.print("Connected to "); 
  //Serial.println(ssid );
  //Serial.print("IP address: "); 
  //Serial.println(WiFi.localIP());
}

//Performances ok mais besoin d'interrupts pour faire le truc proprement. Probablement pas utile.
void loop() 
{
  isDrinkingF();
  isFilledF();
  chrono();
  server.handleClient();
  
  if((WiFi.status() == WL_CONNECTED)) 
  {
    HTTPClient http;
    //http.begin("http://192.168.4.1/feed?arg=" + String(id) + ";" + String(isFilled()) + ";" + String(isDrinking()) + ";" + String(lastTime) + ";" + String(shaked)); 
    if(toUpdate)
    {
      http.begin("http://192.168.4.1/feed?id=" + String(id) + "&isFilled=" + String(isFilled) + "&isDrinking=" + String(isDrinking) + "&lastTime=" + String(lastTime) + "&shaked=" + String(shaked)); 
      toUpdate = false;
    }
    else
    {
      http.begin("http://192.168.4.1/feed?id=" + String(id));
    }
    int httpCode = http.GET();
    if(httpCode > 0) 
    {
      String payload = http.getString();
      //Serial.println(payload);
      int R = (getValue(payload,';',1)).toInt();
      int G = (getValue(payload,';',2)).toInt();
      int B = (getValue(payload,';',3)).toInt();
      RGB(R, G, B);
      int vib = (getValue(payload,';',4)).toInt();
      vibration(vib);  //Ajouter un timeout a la vibration jusqu'a ce qu'elle soit reset
    } 
    else 
    {
      //Serial.printf("[HTTP] GET... failed, error: %s\n", http.errorToString(httpCode).c_str());
    }
    http.end();
  }

  delay(10);
  // Les 2 conditions permettent de ne "retenir" le shaked que le temps de 10 loop
  // !! Modifie par jide le 5/7/18

  if(shaked > 0)
  {
	if(timeShaked > 5)
	{
		shaked = 0; 
		timeShaked = 0;
	} else{
		timeShaked++;
	}
    toUpdate = true;
  }
  
}
