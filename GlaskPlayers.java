package com.example.nicolas.glaskwifi;

import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Nicolas on 11-01-18.
 */

abstract class GlaskPlayers {
    List<Glask> players;
    List<Integer> playersID;
    List<String> stack;
    List<Integer> isFilled;
    List<Integer> isAffoning;
    List<Integer> isShaked;
    int color[][] = {{255,0,0},{0,255,0},{0,0,255},{255,255,255}};
    int i = 0;
    private Handler sending = new Handler();

    public GlaskPlayers()
    {
        this.players = new ArrayList<Glask>();
        this.playersID = new ArrayList<Integer>();
        this.isFilled = new ArrayList<Integer>();
        this.isAffoning = new ArrayList<Integer>();
        this.isShaked= new ArrayList<Integer>();
        this.stack = new ArrayList<String>();
        sending.postDelayed(runnable, 1000);
    }

    private Runnable runnable = new Runnable()
    {
        @Override
        public void run()
        {
            if(isOnline())
            {
                if(toPush())
                {
                    String s = push();
                    new HTTPAsyncTask().execute("http://192.168.4.1/void?arg=" + s);
                }
                else
                {
                    i = i + 1;
                    if(i == 3) {
                        new HTTPAsyncTask().execute("http://192.168.4.1/void");
                        i = 0;
                    }
                }
                sync();

            }
            else
            {
                new HTTPAsyncTask().execute("http://192.168.4.1/void");
            }
            sending.postDelayed(this, 300);
        }
    };

    private String HttpGet(String myUrl) throws IOException {
        InputStream inputStream = null;
        String result = "";
        URL url = new URL(myUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.connect();
        inputStream = conn.getInputStream();
        if(inputStream != null)
            result = convertInputStreamToString(inputStream);
        return result;
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException{
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;
        inputStream.close();
        return result;

    }

    private class HTTPAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {
            try {
                return HttpGet(urls[0]);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }
        @Override
        protected void onPostExecute(String result) {
            if(result.contains("#"))
            {
                String[] sub = result.split("#");
                for(int i = 0; i < sub.length; i++)
                {
                    String[] playerStates = (sub[i]).split(";");
                    if (playerStates.length > 7)
                    {
                        int id = Integer.parseInt(playerStates[0]);
                        boolean isFilled = (Integer.parseInt(playerStates[5]) == 1);
                        boolean isDrinking = (Integer.parseInt(playerStates[6]) == 1);
                        double lastTime = Double.parseDouble(playerStates[7]);
                        int shaked = Integer.parseInt(playerStates[8]);
                        set(id, isFilled, isDrinking, lastTime, shaked);
                    }
                }
            }
        }
    }

    // @pre : un objet Glask
    // @post : met à jour les states du Glask s'il est dans le jeu, l'ajoute sinon
    public void set(int id, boolean isFilled, boolean isDrinking, double lastTime, int shaked)
    {
        if(playersID.contains(id))
        {
            Glask temp = this.players.get(this.playersID.indexOf(id));

            if(temp.isFilled() && !isFilled)
            {
                //call pushIsEmpty
            }
            else if(!temp.isFilled() && isFilled)
            {
                this.isFilled.add(id);
            }
            temp.update(id, isFilled, isDrinking, lastTime, shaked);
            if(temp.isAffoning())
            {
                this.isAffoning.add(id);
            }

            if(temp.isShaked() > 0)
            {
                this.isShaked.add(id);
            }

        }
        else
        {
            this.playersID.add(id);
            Glask temp = new Glask(id, 0, 0, 0, 0, isFilled, isDrinking, lastTime, shaked);
            this.players.add(temp);
        }
    }

    // @pre : -
    // @post : retourne le nombre de Glask dans le jeu
    public int numOfPlayers()
    {
        return this.playersID.size();
    }

    // @pre : l'id d'un Glask
    // @post : retourne l'objet Glask selon l'id donné dans le jeu
    public Glask getById(int id)
    {
        if(playersID.contains(id))
        {
            return this.players.get(this.playersID.indexOf(id));
        }
        else
        {
            return new Glask();
        }
    }

    // @pre : -
    // @post : toutes les chaines de valeurs structurées pour l'électronique
    public String[] formatOut()
    {
        String[] f = new String[this.numOfPlayers()];
        for(int i = 0; i < this.numOfPlayers(); i++)
        {
            f[i] = getById(i).formatOut();

        }
        return f;
    }

    // @pre : des entiers id, R, G et B
    // @post : applique la fonction RGB au Glask "id"
    public void RGBById(int id, int R, int G, int B)
    {
        getById(id).RGB(R, G, B);
        stack.add(this.players.get(this.playersID.indexOf(id)).formatOut());
    }

    public void RGBByIdRandom(int id)
    {
        Random r1 = new Random();
        int i = r1.nextInt(4);

        int u = 0;
        if(getById(id).R != color[i][0])
        {
            u++;
        }
        if(getById(id).G != color[i][1])
        {
            u++;
        }
        if(getById(id).B != color[i][2])
        {
            u++;
        }

        if(u > 0)
        {
            getById(id).RGB(color[i][0], color[i][1], color[i][2]);
            stack.add(this.players.get(this.playersID.indexOf(id)).formatOut());
        }
        else
        {
            RGBByIdRandom(id);
        }

    }

    public void RGBByIdRandom(List<Integer> id)
    {
        Random r1 = new Random();
        int i = r1.nextInt(4);

        int u = 0;
        if(getById(id.get(0)).R != color[i][0])
        {
            u++;
        }
        if(getById(id.get(0)).G != color[i][1])
        {
            u++;
        }
        if(getById(id.get(0)).B != color[i][2])
        {
            u++;
        }

        if(u > 0)
        {
            for(int j = 0; j < id.size(); j++)
            {
                getById(id.get(j)).RGB(color[i][0], color[i][1], color[i][2]);
                stack.add(getById(id.get(j)).formatOut());
            }
        }
        else
        {
            RGBByIdRandom(id);
        }


    }

    // @pre : des entiers id, state
    // @post : applique la fonction vibrate au Glask "id"
    public void vibrateById(int id, int ... state)
    {
        getById(id).vibrate(state);
        stack.add(this.players.get(this.playersID.indexOf(id)).formatOut());
    }

    public List<Integer> isSomeoneAffoning()
    {
        List<Integer> result = new ArrayList<Integer>();
        for(int i = 0; i < this.numOfPlayers(); i++)
        {
            int j = this.playersID.get(i);
            if(getById(j).isAffoning())
            {
                result.add(j);
            }
        }
        return result;
    }

    public List<Integer> isFilled()
    {
        List<Integer> result = new ArrayList<Integer>();
        for(int i = 0; i < this.numOfPlayers(); i++)
        {
            int j = this.playersID.get(i);
            if(getById(j).isFilled())
            {
                result.add(j);
            }
        }
        return result;
    }

    public String push()
    {
        String s = "";
        if(this.stack.size() > 0)
        {
            s = this.stack.get(0);
            this.stack.remove(0);
        }
        else { s = ""; }
        return s;
    }

    public void sync()
    {
        if(this.isFilled.size() > 0)
        {
            pushIsFilled(this.isFilled);
            this.isFilled.clear();
        }
        if(this.isAffoning.size() > 0)
        {
            pushIsAffoning(this.isAffoning);
            this.isAffoning.clear();
        }
        if(this.isShaked.size() > 0)
        {
            pushIsShaked(this.isShaked);
            this.isShaked.clear();
        }
    }
    public boolean toPush()
    {
        return this.stack.size() > 0;
    }

    public boolean isOnline()
    {
        return numOfPlayers() > 0;
    }

    public abstract void pushIsFilled(List<Integer> id);

    public abstract void pushIsAffoning(List<Integer> id);

    public abstract void pushIsShaked(List<Integer> id);


}