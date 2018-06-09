#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>

typedef signed char int8_t;

const char *ssid = "glask";
const char* password = "12345678";

const int maxPlayers = 15;
int players[maxPlayers][9];
int stack[maxPlayers];
int nStack = 0;
const int receveidBuffLen = 8*4; // 4 gobelets
String debug = "mon sexe";

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

void stackAdd(int pos)
{
  if(stack[pos] == 0)
  {
    stack[pos] = 1;
    nStack++;
  }
}

// Serie de cast pour passer d'un booleen, un temps nul ou un shaked nul à des bytes transmissibles (!= 0)
int cast(int bo)
{
  if(bo == 0) { return -10; }
  else { return 10; }
}

int castTime(int t)
{
  if(t < 256 ) { return 300;}
  else { return t; }  
}

int castShaked(int s)
{
  if(s == 0) { return -10; }
  else { return s; }
}

void handle_feed() 
{
  if(server.args() > 1)
  {
    int id = (server.arg(0)).toInt();
    int isfilled = (server.arg(1)).toInt();
    int isdrinking = (server.arg(2)).toInt();
    int lastime = (server.arg(3)).toInt();
    int shaked = (server.arg(4)).toInt();
  
    boolean updated = false;
    String line;
    // Cas ou le Glask est déjà dans le jeu
    for(int i = 0; i < maxPlayers && !updated; i++)
    {
      if(players[i][0] == id)
      {
        if(players[i][5] != isfilled || players[i][6] != isdrinking || players[i][7] != lastime || players[i][8] != shaked)
        {
          // L'information est différente, il faut le communiquer à l'app, on balance dans la stack
          stackAdd(i);
        }
        players[i][5] = cast(isfilled);
        players[i][6] = cast(isdrinking);
        players[i][7] = castTime(lastime);
        players[i][8] = castShaked(shaked);
        line = "#" + String(id) + ";" + String(players[i][1]) + ";" + String(players[i][2]) + ";" + String(players[i][3]) + ";" + String(players[i][4]);
        updated = true;
      }
    }   
    // Cas ou le Glask n'est pas encore dans le jeu : i.e il vient de se connecter
    if(!updated)
    {
      boolean added = false;
      for(int i = 0; i < maxPlayers && !added; i++)
      {
        if(players[i][0] == 0)
        {
          players[i][0] = id;
          players[i][5] = cast(isfilled);
          players[i][6] = cast(isdrinking);
          players[i][7] = castTime(lastime);
          players[i][8] = castShaked(shaked);
          line = "#" + String(id) + ";" + String(players[i][1]) + ";" + String(players[i][2]) + ";" + String(players[i][3]) + ";" + String(players[i][4]);
          stackAdd(i);
          //Serial.println("Player added by Glask");
          added = true;
        }
      }
      if(!added)
      {
        //Serial.println("Too much players, da fuck ?");
      }
    }
    server.send(200, "text/plain", line);
  }
  else
  {
    int id = (server.arg(0)).toInt();
    boolean found = false;
    for(int i = 0; i < maxPlayers && !found; i++)
    {
      if(players[i][0] == id)
      {
        found = true;
        String line = "#" + String(id) + ";" + String(players[i][1]) + ";" + String(players[i][2]) + ";" + String(players[i][3]) + ";" + String(players[i][4]);
        server.send(200, "text/plain", line);
      }
    }
  }
}

void handle_demo()
{
  String m = "Stack : " + String(nStack) + " = {";
  for(int i = 0; i < (sizeof(stack)/sizeof(int));i++)
  {
    m += String(stack[i]) + ", ";
  }
  m += "}";
  server.send(200, "text/plain", m);
}

void handle_debug()
{
  server.send(200, "text/plain", debug);
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

void setup()
{
  Serial.begin(9600);
  for(int i=0; i < maxPlayers;i++)
  {
    stack[i] = 0;
  }
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
  server.on("/void", handle_void);
  server.on("/demo", handle_demo);
  server.on("/debug", handle_debug);
  server.begin();
  Serial.println("HTTP server started");


  //stack[0] = 1; //=!= id
  //stack[1] = 2;

  //players[0][0] = 27;
  //players[0][1] = 200; //R
  //players[0][2] = 255; //G
  //players[0][3] = 255; //B
  //players[0][4] = 0; //vib
  //players[0][5] = 10; //isFilled
  //players[0][6] = -10; //isDrinking
  //players[0][7] = 1500; //lastTime
  //players[0][8] = 72; //shaked

  //players[1][0] = 18;
  //players[1][1] = 200; //R
  //players[1][2] = 108; //G
  //players[1][3] = 255; //B
  //players[1][4] = 1; //vib
  //players[1][5] = -10; //isFilled
  //players[1][6] = 10; //isDrinking
  //players[1][7] = 2000; //lastTime
  //players[1][8] = 64; //shaked

  //stack[0] = 0; //=!= id
  //stack[1] = 1;
  
  //nStack = 2;
}


void loop()
{
  server.handleClient();
  if (Serial.available() > 0) 
  {
    byte buff[receveidBuffLen];
    Serial.readBytes(buff, receveidBuffLen);
    boolean stop = false;
    for(int i=0; i < receveidBuffLen && !stop;i++)
    {
      // Si l'app envoie -2, c'est qu'elle se tait et que c'est à la base de parler
      debug += String(((int) buff[i])) + ";";
      if(buff[i] == (byte) -2)
      {
        //if(nStack > 0)
        //{
        //  short key = -1;
        //  boolean push = true;
        //  char message[nStack*8 + 2];
        //  for(int i=0; i < maxPlayers;i++)
        //  {
        //    if(stack[i] != 0)
        //    {
        //      message[i*8 + 0] = -34;//                                                    0 8
        //      message[i*8 + 1] = key;//                                                    1 9 
        //      message[i*8 + 2] = players[stack[i]][0]; //id //i+1                          2 10
        //      message[i*8 + 3] = players[stack[i]][5]; //isFilled //i+2                    3 11
        //      message[i*8 + 4] = players[stack[i]][6]; //isDrinking //i+3                  4 12
        //      message[i*8 + 5] = (players[stack[i]][7] & 0xFF); //lastTime //i+4           5 13
        //      message[i*8 + 6] = ((players[stack[i]][7] >> 8) & 0xFF); //lastTime          6 14
        //      message[i*8 + 7] = players[stack[i]][8]; //shaked                            7 15
        //      stack[i] = 0; // on retire l'action faite de la stack       
        //    }
        //  }
        //  message[nStack*8] = -2;  // 16
        //  message[nStack*8 + 1] = -3; //17
        //  //byte message[] = {(byte) 22, (byte) key, (byte) id, (byte) isFilled, (byte) isDrinking, (byte) isAffoning, (byte) (lastTime & 0xFF), (byte) ((lastTime >> 8) & 0xFF)};
        //  Serial.write(message, nStack*8 + 2); //16+2=18
        //  //Serial.write(message, nStack*8 + 2);
        //  //nStack = 0;
        //}

        if(nStack > 0)
        {
            byte message[8*nStack + 2];
            for(int i = 0; i < nStack;i++)
            {
              if(stack[i] == 1)
              {
                message[i*8 + 0] = (byte) -34;
                message[i*8 + 1] = (byte) -1;
                message[i*8 + 2] = (byte) players[i][0];
                message[i*8 + 3] = (byte) players[i][5];
                message[i*8 + 4] = (byte) players[i][6];
                message[i*8 + 5] = (byte) (players[i][7] & 0xFF);
                message[i*8 + 6] = (byte) ((players[i][7] >> 8) & 0xFF);
                message[i*8 + 7] = (byte) players[i][8];
                stack[i] = 0;
              }
            }
            
            message[8*nStack] = (byte) -2;
            message[8*nStack + 1] = (byte) -3;
            delay(10);
            Serial.write(message, 8*nStack + 2); //16+2=18
            Serial.flush();
            nStack = 0;
        }
        else
        {
            Serial.write(-2); //16+2=18
            Serial.write(-3); //16+2=18
            Serial.flush();
        }
        stop = true;
      }
      // On décode ce que l'app envoie
      else if(buff[i] == (byte) -34 && buff[i + 1] == (byte) -1)
      {
          //debug = "Coucou"; //String(buff[i + 1]) + " - "  + String(buff[i + 2]) + " - "  + String(buff[i + 3]) + " - "  + String(buff[i + 4]) + " - "  + String(buff[i + 5]);
          boolean find = false;
          for(int i=0; i < maxPlayers && !find; i++)
          {
              if(players[i][0] == (int) buff[i + 2])
              {
                  debug = "lala";
                  find = true;
                  players[i][1] = (int) buff[i + 3]*2; //R
                  players[i][2] = (int) buff[i + 4]*2; //G
                  players[i][3] = (int) buff[i + 5]*2; //B
                  players[i][4] = (int) buff[i + 6]; //vib
                  i = i + 5;
              }
          }
      }
    }
    debug += " | ";
  }
  delay(10);
}
