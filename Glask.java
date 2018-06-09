package com.example.nicolas.glaskagain;

import android.os.Handler;

/**
 * Created by Nicolas on 11-01-18.
 */

public class Glask {
    int id;
    int R;
    int G;
    int B;
    int vibrate;
    int shake;
    boolean isFilled;
    boolean isDrinking;
    double lastTime;
    double bestTime;
    boolean potentielAffond;
    boolean affond;
    int shaked;

    public Glask()
    {
        id = 0;
        R = 0;
        G = 0;
        B = 0;
        vibrate = 0;
        isFilled = false;
        isDrinking = false;
        lastTime = 0.0;
        bestTime = 0.0;
        potentielAffond = false;
        affond = false;
        shaked = 0;
    }

    // @pre : états d'un Glask complets
    // @post : créé un objet Glask
    public Glask(int id, int R, int G, int B, int vibrate, boolean isFilled, boolean isDrinking, double lastTime, int shaked)
    {
        this.id = id;
        this.RGB(R, G, B);
        this.vibrate = vibrate;
        this.isFilled = isFilled;
        this.isDrinking = isDrinking;
        this.lastTime = lastTime;
        this.bestTime = lastTime;
        this.shaked = shaked;
    }

    // @pre : états d'un Glask reçu par le jeu
    // @post : modifie un objet Glask pour le mettre à jour
    public void update(int id, boolean isFilled, boolean isDrinking, double lastTime, int shaked)
    {
        this.isFilled = isFilled;
        this.isDrinking = isDrinking;
        this.shaked = shaked;
        this.affond = false;
        if(lastTime < this.bestTime)
        {
            this.bestTime = lastTime;
        }
        this.lastTime = lastTime;

        if(isFilled && isDrinking && !this.potentielAffond)
        {
            this.potentielAffond = true;
        }

        if(potentielAffond && isFilled && !isDrinking)
        {
            this.potentielAffond = false;

        }

        if(!isFilled && !isDrinking && this.potentielAffond)
        {
            this.potentielAffond = false;
            this.affond = true;
        }
    }

    // @pre : -
    // @post : répond hello pour debug
    public String hello()
    {
        return "Hello, I'm Glask number " + Integer.toString(id);
    }

    // @pre : -
    // @post : une chaine de valeurs structurées pour l'électronique
    public byte[] formatOut()
    {
        byte response[] = {-1, (byte) this.id, (byte) (this.R/2), (byte) (this.G/2), (byte) (this.B/2), (byte) this.vibrate};
        return response;
        //return Integer.toString(this.id) + ";" + Integer.toString(this.R) + ";" + Integer.toString(this.G) + ";" + Integer.toString(this.B) + ";" + Integer.toString(this.vibrate) + ";";
    }

    // @pre : R, G et B des entiers
    // @post : définit la couleur de la LED quelque soit les valeurs de R,G et B (0-255 normalement)
    public void RGB(int R, int G, int B)
    {
        if(R <= 255 && R >= 0)
        {
            this.R = R;
        }
        else
        {
            this.R = 255;
        }

        if(G <= 255 && G >= 0)
        {
            this.G = G;
        }
        else
        {
            this.G = 255;
        }

        if(B <= 255 && B >= 0)
        {
            this.B = B;
        }
        else
        {
            this.B = 255;
        }
    }

    // @pre : entier de l'état du vibreur (in/out) et temps
    // @post : active ou désactive de vibreur pendant un laps de temps
    public void vibrate(int ... no)
    {
        if (no.length == 1) {
            int state = no[0];
            if (state == 1) {
                this.vibrate = 1;
            } else {
                this.vibrate = 0;
            }
        } else if (no.length == 3) {
            int state1 = no[0];
            vibrate(state1);
            final int state2 = no[1];
            final int t = no[2];
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    vibrate(state2);
                }
            }, t);
        }
    }

    public boolean isAffoning() { return this.affond; }

    public boolean isFilled() { return this.isFilled; }

    public int isShaked() { return this.shaked; }
}