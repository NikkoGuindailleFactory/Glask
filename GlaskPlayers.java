package com.example.nicolas.glaskbt;

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
    List<Integer> stack;
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
        this.stack = new ArrayList<Integer>();
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
            if(id > 0)
            {
                this.playersID.add(id);
                Glask temp = new Glask(id, 0, 0, 0, 0, isFilled, isDrinking, lastTime, shaked);
                this.players.add(temp);
            }
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
    public int formatOut()
    {
//        String[] f = new String[this.numOfPlayers()];
//        for(int i = 0; i < this.numOfPlayers(); i++)
//        {
//            f[i] = getById(i).formatOut();
//
//        }
        return -1;
    }

    // @pre : des entiers id, R, G et B
    // @post : applique la fonction RGB au Glask "id"
    public void RGBById(int id, int R, int G, int B)
    {
        getById(id).RGB(R, G, B);
        stack.add(id);
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
            stack.add(id);
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
                stack.add(id.get(j));
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
        stack.add(id);
    }

    public List<Integer> isAffoning()
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

    public int push()
    {
        int id;
        if(this.stack.size() > 0)
        {
            id = this.stack.get(0);
            this.stack.remove(0);
        }
        else { id = -1; }
        return id;
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