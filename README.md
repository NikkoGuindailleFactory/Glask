# Glask
Glask : développement du code (ESP8266 et Java)

<p align="center">
  <img src="https://www.guindaillefactory.com/res/glask.png" width="350"/>
</p>


## Mise à jour
- 09/06/2018 : changement de librairie sur Android (Blue2Serial) utilée en raw bytes. Réception ok, envoi encore critique.
- 24/05/2018 : envoi/réception des données en bytes et en bluetooth.
- 19/05/2018 : requête HTTP avec arguments multiples, optimisation.

# Software : Android
## API

### Vibrate
```java
void vibrateById(id, state) 
void vibrateById(id, state1, state2, t) 
```
Met le vibreur de id à l’état state. <br>
Passe le vibreur de id à l’état state1 pendant le temps t et passe à state2. 

### RGB
```java
void RGBById(id, R, G, B)
void RGBByIdRandom(int id)
void RGBByIdRandom(List<Integer> id)
```
Allume la LED de id aux couleurs (R, G, B).<br>
Allume la LED de id dans une couleur aléatoire.<br>
Allume les LEDS des id dans une couleur aléatoire mais identique.

### isAffoning
```java
List <int> isAffoning()
```
Retourne une liste des id qui affonent.

### isFilled
```java
List <int> isFilled()
```
Retourne une liste des id qui sont remplis.

### isShaked
```java
List <int> isShaked()
```
Retourne une liste des id qui sont trinqués.


## Exemple minimaliste
```java
package com.example.nicolas.glaskwifi;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;

public class MainActivity extends AppCompatActivity {

    private GameManager game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        game = new GameManager();
    }
    
    private class GameManager extends GlaskPlayers {
        public void pushIsFilled(List<Integer> id) {
          // do something if filled
        }

        public void pushIsAffoning(List<Integer> id) {
          // do something if isAffoning
        }

        public void pushIsShaked(List<Integer> id) {
          // do something if shaked
        }
    }
}
```

# Software : ESP12
- Code tournant sur un gobelet Glask, le numéro unique est à changer en ligne 8. Fichier : Glask.ino
- Code tournant sur une base Glask. Fichier : Base.ino

## Bouncing et Interrupt
Le capteur de temps de consommation a toujours été un défi de taille : que ce soit au niveau exécution qu’au niveau précision. Rappelons qu’il s’agit, pour l’instant, du levelPin2, qui passe de l’état HIGH (1) à l’état LOW (0) lorsqu’on consomme. Le principe est alors, tout simplement, de mesurer le temps où l’état du pin est bas : rien de compliqué. Plusieurs solutions sont envisageables et certainement de nouvelles à découvrir.

La plus simple consiste à créer un listener dans le loop et qui écoute l’état du pin. Mais cette méthode est limitée par le délai accordé dans le loop. C’est à dire que si le loop tourne à 100 ms, on a une précision à 100 ms. Ce qui est loin d’être fameux. On peut également diminuer ce délai, ce qui semble être radical mais qui a tendance à déstabiliser l'ESP. 

La seconde solution consiste à utiliser des interrupts. Il est à rappeler que les interrupts sont une façon software ou hardware (dépendant du µC) de gérer du multi tâche : loop classique et “écoute” sur les pins. On peut facilement choisir quel type d’écoute on fait : LOW (de haut vers bas), RISING (de bas vers haut), CHANGE (changement d’état) sur l’ESP. Le pin est alors correctement configuré et une fonction s’exécute lors de l’interrupt. Mais ces fonctions sont victimes du phénomène de bouncing. Les raisons en sont toujours un peu floue (horloge interne ?) mais la conséquence est relativement simple : l’état du pin varie à haute fréquence alors que celui-ci ne change pas. Exemple relativement simple avec un switch (idem à notre capteur) :

<p align="center">
  <img src="https://www.guindaillefactory.com/res/bouncing.jpg" width="350"/>
</p>

La fonction paramétrée va s’exécuter le nombre de crête du “signal” précédent, ne permettant pas de délimiter correctement le début (activated) et la fin (deactivated). La solution la plus simple et valable sur Arduino (par exemple) est de définir un temps de bouncing inférieur au temps (minimum) d’activation du switch. On effectue des actions que si la différence entre le temps présent et le dernier bounce est inférieur au temps de bouncing. Tout ça peut sembler compliqué (surtout avec des mots) mais c’est relativement simple : le logiciel ne tient plus compte des variations infimes de signal. Le problème est que l’ESP se met à planter dès que l’on utilise un délai trop grand et la précision n’est même pas dingue (20 ms). 

Pour le capteur de choc, on utilise justement ce phénomène de bouncing pour quantifier le choc. Le fonctionnement du capteur est relativement simple mais donne un signal à pic très raide et très court. Il n’est pas à préciser que le capteur est dans son état le plus rudimentaire : sans chinoiserie tout autour qui étale le signal en durée. On ne peut pas utiliser les fonctions standards pour trouver l’état du pin : le logiciel n’est pas assez rapide (loop). On utilise alors un interrupt en mode RISING et on compte littéralement le nombre de bounce. Ce chiffre donne une idée de la longueur de vibration (dans le temps) et donc de son intensité. On peut directement envoyer cette information à la base, par exemple.

# Hardware
<p align="center">
  <img src="https://www.guindaillefactory.com/res/pcb_2.jpg" width="350"/>
</p>
<p align="center">			
  <table style="width:40%;">
				<tr style="text-align:center;">
					<th class="categorie" width="10%">#</th>
					<th class="categorie" width="40%">Pin</th>
					<th class="categorie" width="10%">#</th>
					<th class="categorie" width="40%">Pin</th>
				</tr>
				<tr style="text-align:center;">
					<td>1</td>
					<td><i>GND</i></td>
					<td>5</td>
					<td><i>TX</i></td>
				</tr>
				<tr style="text-align:center;">
					<td>2</td>
					<td><i>GND</i></td>
					<td>6</td>
					<td><i>RX</i></td>
				</tr>
				<tr style="text-align:center;">
					<td>3</td>
					<td><i>isFilled()</i></td>
					<td>7</td>
					<td><i>GND</i></td>
				</tr>
				<tr style="text-align:center;">
					<td>4</td>
					<td><i>isDrinking()</i></td>
					<td>8</td>
					<td><i>Vcc</i></td>
				</tr>
		</table> 
    </p>
    
## PCBs & schémas
  - 22/07/2017 :
  <p align="center">
  <img src="https://www.guindaillefactory.com/res/circuit1.jpg" width="500"/>
  </p>
  Simplification en remplaçant les transistors (qui permettait la détection avec un courant assez faible) par des pull-ups. R6, R7 sont dimensionnées expérimentalement et R1 semble inutile. R2, R3, R4 sont dimensionnées par calcul pour favoriser un courant élevé (20mA même si l’ESP limite à 12mA).
  <p align="center">
  <img src="https://www.guindaillefactory.com/res/circuit22.jpg" width="400"/>
  </p>  

  V1.0 du PCB, par Sacha Stinglhamber. Le premier PCB dont la complexité à surtout été l’orientation des composants par rapport à leurs couches. 
  <p align="center">
  <img src="https://www.guindaillefactory.com/res/pcb_22.jpg" width="600"/>
  </p>  
  
  Fichiers : <a href="https://www.guindaillefactory.com/res/pcb1.pdf">PDF</a> - <a href="https://www.guindaillefactory.com/res/pcb2.pdf">PDF (3/page)</a>

- 15/05/2017 :
  <p align="center">
  <img src="https://www.guindaillefactory.com/res/circuit2.jpg" width="500"/>
  </p>

## Plastique et 3D


<p align="center">
Tout droit réservé TheGuindailleFactory : Julien Dubois et Nicolas Colsoul
</p>
