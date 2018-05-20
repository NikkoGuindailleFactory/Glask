#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>

const char *ssid = "glask";
const char* password = "12345678";
const int maxPlayers = 4;
int players[maxPlayers][9];

ESP8266WebServer server(80);

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

String BTtoGlask(int id)
{
    boolean found = false;
    for(int i = 0; i < maxPlayers && !found; i++)
    {
      if(players[i][0] == id)
      {
        found = true;
        return "#" + String(id) + ";" + String(players[i][1]) + ";" + String(players[i][2]) + ";" + String(players[i][3]) + ";" + String(players[i][4]);
      }
    }
    if(!found)
    {
      Serial.println("Joueur non trouvé dans la base");  
      return "Joueur non trouvé dans la base";
    }
}


void BTtoBase(String commandIn)
{
  char command[commandIn.length() + 1];
  char* pch;
  
  commandIn.toCharArray(command, commandIn.length() + 1);
  pch = strtok(command, "#");
  while(pch != NULL) 
  {
    // Bourrin :
    String uCommand = String(pch);
    int id = (getValue(uCommand,';',0)).toInt();
    int R = (getValue(uCommand,';',1)).toInt();
    int G = (getValue(uCommand,';',2)).toInt();
    int B = (getValue(uCommand,';',3)).toInt();
    int vibration = (getValue(uCommand,';',4)).toInt();      
    boolean updated = false;
    
    for(int i = 0; i < maxPlayers && !updated; i++)
    {
      if(players[i][0] == id)
      {
        players[i][1] = R;
        players[i][2] = G;
        players[i][3] = B;
        players[i][4] = vibration;
        updated = true;
      }
    }   
    if(!updated)
    {
      boolean added = false;
      for(int i = 0; i < maxPlayers && !added; i++)
      {
        if(players[i][0] == 0)
        {
        players[i][0] = id;
        players[i][1] = R;
        players[i][2] = G;
        players[i][3] = B;
        players[i][4] = vibration;
        //Serial.println("Player added by server : isn't normal but done");
        added = true;
        }
      }
      if(!added)
      {
        //Serial.println("Too much players, da fuck ?");
      }  
    }
    pch = strtok(NULL, "#");
  }
}

void handle_demo()
{
  server.send(200, "text/plain", "hello from Glask base \n Glask powered \n \r The Guindaille Factory \n");
}

void handle_void() 
{
  String commandIn = server.arg(0);
  int id = (getValue(commandIn,';',0)).toInt();
  int R = (getValue(commandIn,';',1)).toInt();
  int G = (getValue(commandIn,';',2)).toInt();
  int B = (getValue(commandIn,';',3)).toInt();
  int vibration = (getValue(commandIn,';',4)).toInt();

  boolean updated = false;
  
  for(int i = 0; i < maxPlayers && !updated; i++)
  {
    if(players[i][0] == id)
    {
      players[i][1] = R;
      players[i][2] = G;
      players[i][3] = B;
      players[i][4] = vibration;
      updated = true;
    }
  }   
  
  String result = "";
  
  for(int i = 0; i < maxPlayers; i++)
  {

    if(players[i][0] != 0)
    {
      String line;
      for(int j = 0; j < 9; j++)
      {
        line = line + String(players[i][j]) + ";";
      }
      result = result + "#" + line + "\n";
    }
  }
  server.send(200, "text/plain", result);
}


void handle_feed() 
{
  //String commandIn = server.arg(0);
  //int id = (getValue(commandIn,';',0)).toInt();
  //int isfilled = (getValue(commandIn,';',1)).toInt();
  //int isdrinking = (getValue(commandIn,';',2)).toInt();
  //int lastime = (getValue(commandIn,';',3)).toInt();
  //int shaked = (getValue(commandIn,';',4)).toInt();

  if(server.args() > 1)
  {
    int id = (server.arg(0)).toInt();
    int isfilled = (server.arg(1)).toInt();
    int isdrinking = (server.arg(2)).toInt();
    int lastime = (server.arg(3)).toInt();
    int shaked = (server.arg(4)).toInt();
  
    boolean updated = false;
    
    for(int i = 0; i < maxPlayers && !updated; i++)
    {
      if(players[i][0] == id)
      {
        players[i][5] = isfilled;
        players[i][6] = isdrinking;
        players[i][7] = lastime;
        players[i][8] = shaked;
        updated = true;
      }
    }   
    if(!updated)
    {
      boolean added = false;
      for(int i = 0; i < maxPlayers && !added; i++)
      {
        if(players[i][0] == 0)
        {
          players[i][0] = id;
          players[i][5] = isfilled;
          players[i][6] = isdrinking;
          players[i][7] = lastime;
          players[i][8] = shaked;
          //Serial.println("Player added by Glask");
          added = true;
        }
      }
      if(!added)
      {
        //Serial.println("Too much players, da fuck ?");
      }
    }
    server.send(200, "text/plain", BTtoGlask(id));
  }
  else
  {
    int id = (server.arg(0)).toInt();
    server.send(200, "text/plain", BTtoGlask(id));
  }
}


void setup() 
{
  Serial.begin(9600);
  delay(10);
  Serial.print("Configuring access point...");
  WiFi.mode(WIFI_AP); 
  boolean result = WiFi.softAP(ssid, password);
  
  if(result == true) Serial.println("Ready");
  else  Serial.println("Failed!");
  
  IPAddress myIP = WiFi.softAPIP();
  Serial.print("AP IP address: ");
  Serial.println(myIP);
  Serial.println("TCP server started");

  server.on("/feed", handle_feed);
  server.on("/demo", handle_demo);
  server.on("/void", handle_void);
  server.begin();
  Serial.println("HTTP server started");

}

void loop() 
{
  server.handleClient();
  //String commandIn = Serial.readString();
  //if(commandIn.length() > 2)
  //{
  //  BTtoBase(commandIn);
  //}
  
  
 // for(int i = 0; i < maxPlayers; i++)
 // {

  //  if(players[i][0] != 0)
 //   {
 //     String line;
 //     for(int j = 0; j < 9; j++)
  //    {
  //      line = line + String(players[i][j]) + ";";
  //    }
  //    Serial.print("#" + line);
  //    Serial.println("");
  //  }
  //}
  delay(100);
}
