
const int maxPlayers = 15;
int players[maxPlayers][9];
int stack[maxPlayers];
int nStack = 0;
const int key_start = -38;
const int key_stop = -29;
const int receveidBuffLen = 8*4; // 4 gobelets

void setup()
{
  pinMode(LED_BUILTIN, OUTPUT);
  Serial.begin(9600);
  for(int i=0; i < maxPlayers;i++)
  {
    stack[i] = 0;
  }
  players[1][0] = 27;
  players[1][1] = 200; //R
  players[1][2] = 255; //G
  players[1][3] = 255; //B
  players[1][4] = 0; //vib
  players[1][5] = 10; //isFilled
  players[1][6] = -10; //isDrinking
  players[1][7] = 1500; //lastTime
  players[1][8] = 72; //shaked

  players[2][0] = 18;
  players[2][1] = 200; //R
  players[2][2] = 108; //G
  players[2][3] = 255; //B
  players[2][4] = 1; //vib
  players[2][5] = -10; //isFilled
  players[2][6] = 10; //isDrinking
  players[2][7] = 2000; //lastTime
  players[2][8] = 64; //shaked

  stack[0] = 1; //=!= id
  stack[1] = 2;
  nStack = 2;
}


void loop()
{
  if (Serial.available() > 0) 
  {
    byte buff[receveidBuffLen];
    Serial.readBytes(buff, receveidBuffLen);
    for(int i=0; i < receveidBuffLen;i++)
    {
      // Si l'app envoie -2, c'est qu'elle se tait et que c'est à la base de parler
      if(buff[i] == (byte) -2)
      {
          short key = -1;
          boolean push = true;
          byte message[nStack*8 + 2];
          for(int i=0; i < 2 && push;i++)
          {
            if(stack[i] != 0)
            {
              message[i*8 + 0] = (byte) -34;//                                                    0 8
              message[i*8 + 1] = (byte) key;//                                                    1 9 
              message[i*8 + 2] = (byte) players[stack[i]][0]; //id //i+1                          2 10
              message[i*8 + 3] = (byte) players[stack[i]][5]; //isFilled //i+2                    3 11
              message[i*8 + 4] = (byte) players[stack[i]][6]; //isDrinking //i+3                  4 12
              message[i*8 + 5] = (byte) (players[stack[i]][7] & 0xFF); //lastTime //i+4           5 13
              message[i*8 + 6] = (byte) ((players[stack[i]][7] >> 8) & 0xFF); //lastTime          6 14
              message[i*8 + 7] = (byte) players[stack[i]][8]; //shaked                            7 15
              //stack[i] = 0; // on retire l'action faite de la stack         
            }
            else
            {
              push = false;
            }
          }
          message[nStack*8] = (byte) -2;  // 16
          message[nStack*8 + 1] = (byte) -3; //17
          //byte message[] = {(byte) 22, (byte) key, (byte) id, (byte) isFilled, (byte) isDrinking, (byte) isAffoning, (byte) (lastTime & 0xFF), (byte) ((lastTime >> 8) & 0xFF)};
          Serial.write(message, nStack*8 + 2); //16+2=18
      }
      // On décode ce que l'app envoie
      else if(buff[i] == (byte) -1)
      {
        boolean find = false;
        for(int i=0; i < maxPlayers && !find; i++)
        {
          if(players[i][0] == (int) buff[i + 1])
          {
            find = true;
            players[i][1] = buff[i + 2]; //R
            players[i][2] = buff[i + 3]; //G
            players[i][3] = buff[i + 4]; //B
            players[i][4] = buff[i + 5]; //vib
            i = i + 5;
          }
        }
      }
    }
  }
  delay(500);
}
