// El paquete que se nos pide que usemos
package src_Quijano_Rey_Sergio;

/**
 * Referencias usadas para el desarrollo de la practica:
 *
 * [1] http://www.gvgai.net/cont.php:
 *  - Pequeño tutorial sobre como crear un agente
 * [2] http://www.gvgai.net/sampleControllers.php
 *  - Ejemplos de algunos controladores
 * */

// Tipos de datos que necesito para interactuar con el entorno GVGAI
import core.game.StateObservation;
import core.player.AbstractPlayer;
import core.game.Observation;

import tools.ElapsedCpuTimer;
import tools.Vector2d;

import ontology.Types;

// Para generar numeros aleatorios
import java.util.Random;

// Tipos de datos de java
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Exception;
import java.util.PriorityQueue;
import java.util.HashSet;
import java.awt.Dimension;
import java.util.Collections;   // Para iterar y tomar valores minimos

// Tipos de datos auxiliares que he programado
import src_Quijano_Rey_Sergio.GridPosition;
import src_Quijano_Rey_Sergio.AStarNode;
import src_Quijano_Rey_Sergio.AStarNodeComparator;
import src_Quijano_Rey_Sergio.Orientation;

// TODO -- Sergio -- Codigo de Lucia
import src_Salamanca_Lopez_Lucia.*;
import java.util.Stack;
import ontology.Types.ACTIONS;

/**
 * Codigo del agente inteligente que vamos a desarrollar para resolver el juego
 * de Boulder Dash
 *
 * Tenemos que extender core.player.AbstractPlayer.java como se indica en [1]
 * @author Sergio Quijano Rey
 * */
public class Agent extends core.player.AbstractPlayer{

    // TODO -- Sergio -- Codigo de Lucia
    //==============================================================================================
    Vector2d fescala;
    Vector2d portal;
    Vector2d gema = null;

    final Vector2d ARRIBA = new Vector2d(0.0,-1.0);
    final Vector2d ABAJO = new Vector2d(0.0,1.0);
    final Vector2d IZQ = new Vector2d(-1.0,0.0);
    final Vector2d DCHA = new Vector2d(1.0,0.0);
    Stack<ACTIONS> camino = new Stack<>();
    int nivel = 1;
    int num_gemas = 0;

    public Agent (StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        //Calculamos el factor de escala entre mundos (pixeles -> grid)
        fescala = new Vector2d(stateObs.getWorldDimension().width / stateObs.getObservationGrid().length,
                stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length);

        portal = stateObs.getPortalsPositions()[0].get(0).position;
        portal.x = Math.floor(portal.x / fescala.x);
        portal.y = Math.floor(portal.y / fescala.y);

        if (stateObs.getResourcesPositions(stateObs.getAvatarPosition())!= null){
            gema = next_gema(stateObs);
            gema.x = Math.floor(gema.x / fescala.x);
            gema.y = Math.floor(gema.y / fescala.y);
            camino = calcularCamino(stateObs,elapsedTimer,gema);
            num_gemas ++;
        }
        if (gema != null){
            camino = calcularCamino(stateObs,elapsedTimer,gema);
        }
        else{
            camino = calcularCamino(stateObs,elapsedTimer, portal);
        }


    }

    public void init(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

    }

    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        if(!camino.isEmpty()){
            return camino.pop();
        }
        else{
            if (stateObs.getResourcesPositions(stateObs.getAvatarPosition())!= null && num_gemas<9){
            gema = next_gema(stateObs);
            gema.x = Math.floor(gema.x / fescala.x);
            gema.y = Math.floor(gema.y / fescala.y);
            camino = calcularCamino(stateObs,elapsedTimer,gema);
            num_gemas ++;
            return camino.pop();
            }
            else{
                camino = calcularCamino(stateObs,elapsedTimer, portal);
            }
        }

        return ACTIONS.ACTION_NIL;
    }

    // Gema a la que ir más cercana con la distancia Manhattan
    public Vector2d next_gema(StateObservation stateObs){
        ArrayList<Integer> distancias = new ArrayList<>();
        Vector2d pos = new Vector2d(stateObs.getAvatarPosition().x / fescala.x, stateObs.getAvatarPosition().y / fescala.y);
        for (Observation gema : stateObs.getResourcesPositions()[0]){
            distancias.add((int)(Math.abs((Math.floor(gema.position.x / fescala.x)) - pos.x) + Math.abs((Math.floor(gema.position.y / fescala.y))-pos.y)));
        }
        int minIndex = distancias.indexOf(Collections.min(distancias));
        return stateObs.getResourcesPositions()[0].get(minIndex).position;
    }
    // Vemos si la casilla es un muro
    public boolean isMuro (StateObservation stateObs, int x, int y){
            if (stateObs.getObservationGrid()[x][y].size()>0){
                if (stateObs.getObservationGrid()[x][y].get(0).itype == 0) {
                    return true;
                }
            }
        return false;
    }


    // Vemos si un nodo está en abiertos y devolvemos el índice, si no está devolvemos -1
    private int isAbiertos (Nodo[] abiertos, Vector2d pos, Vector2d ori){

        for( int i=0; i< abiertos.length; i++){
            Vector2d posicion = abiertos[i].getPosition();
            Vector2d orientacion = abiertos[i].getOrientacion();
            if((posicion.x == pos.x) && (posicion.y==pos.y) && (orientacion.x==ori.x) && (orientacion.y==ori.y))
                return i;

        }
        return -1;
    }



    // Expande un nodo y devuelve los vecinos de dicho nodo
    ArrayList<Nodo> expandirNodo (Nodo n, StateObservation stateObs, Vector2d destino){
        // ArrayList que vamos a devolver con los hijos del nodo
        ArrayList<Nodo> hijos = new ArrayList<>();

        // Posicion y orientacion del padre de dichos hijos
        Vector2d posicion = n.getPosition();
        Vector2d ori = n.getOrientacion();

        // El coste del camino que lleva realizado el padre
        int costePadre = n.getG();

        Nodo hijo = null;

        // Calculamos las nuevas posiciones en el caso de que sea posible llegar a dicha posicion
        // Si no se sale del tablero
        if (posicion.y - 1 >= 0) {
            // Si es un muro no lo expandimos
            if(isMuro(stateObs,(int)posicion.x, (int)posicion.y-1)){}
            else{
                hijo = new Nodo(new Vector2d(posicion.x, posicion.y-1), destino, n, ARRIBA, ACTIONS.ACTION_UP);
                // Si estamos orientados a dicha casilla solo necesitaremos realizar una accion
                if ((ori.x == ARRIBA.x) && (ori.y == ARRIBA.y)){
                    hijo.setAccion_dup(false);
                    hijo.setG(costePadre+1);
                }
                // Si no estamos orientados, giramos hacia esa casilla por lo que necesitamos
                // realizar dos acciones (por ello el coste es dos)
                else{
                    hijo.setAccion_dup(true);
                    hijo.setG(costePadre+2);
                }

                hijos.add(hijo);
            }
        }
        // Vemos que no sobrepasa el alto
        if (posicion.y + 1 <= stateObs.getObservationGrid()[0].length-1) {
            // Procedemos como el de arriba
            if(isMuro(stateObs,(int)posicion.x, (int)posicion.y+1)){}
            else{
                hijo = new Nodo(new Vector2d(posicion.x, posicion.y+1), destino, n, ABAJO, ACTIONS.ACTION_DOWN);
                if ((ori.x == ABAJO.x) && (ori.y == ABAJO.y)){
                    hijo.setAccion_dup(false);
                    hijo.setG(costePadre+1);
                }
                else{
                    hijo.setAccion_dup(true);
                    hijo.setG(costePadre+2);
                }

                hijos.add(hijo);
            }
        }
        if (posicion.x - 1 >= 0) {
            if(isMuro(stateObs,(int)posicion.x-1, (int)posicion.y)){}
            else{
                hijo = new Nodo(new Vector2d(posicion.x - 1, posicion.y), destino, n, IZQ, ACTIONS.ACTION_LEFT);
                if ((ori.x == IZQ.x) && (ori.y == IZQ.y)){
                    hijo.setAccion_dup(false);
                    hijo.setG(costePadre+1);
                }
                else{
                    hijo.setAccion_dup(true);
                    hijo.setG(costePadre+2);
                }

                hijos.add(hijo);
            }
        }
        // vemos que no sobrepasa el ancho
        if (posicion.x + 1 <= stateObs.getObservationGrid().length - 1) {
            if(isMuro(stateObs,(int)posicion.x+1, (int)posicion.y)){}
            else{
                hijo = new Nodo(new Vector2d(posicion.x + 1, posicion.y), destino, n, DCHA, ACTIONS.ACTION_RIGHT);
                if ((ori.x == DCHA.x) && (ori.y == DCHA.y)){
                    hijo.setAccion_dup(false);
                    hijo.setG(costePadre+1);
                }
                else{
                    hijo.setAccion_dup(true);
                    hijo.setG(costePadre+2);
                }

                hijos.add(hijo);
            }
        }

        return hijos;
    }

    // Función A* inicial (luego iré cambiándola)
    private Stack<ACTIONS>  calcularCamino(StateObservation stateObs, ElapsedCpuTimer elapsedTimer, Vector2d destino){
        Stack<ACTIONS>  camino = new Stack<>();

        // Calculamos la posicion actual del avatar
        Vector2d avatar =  new Vector2d(stateObs.getAvatarPosition().x / fescala.x,
        		stateObs.getAvatarPosition().y / fescala.y);
        // Obtenemos la orientacion del avatar
        Vector2d orientacion = stateObs.getAvatarOrientation();
        // Creamos el nodo inicial
        Nodo inicial = new Nodo(avatar, destino, orientacion, ACTIONS.ACTION_NIL);

        // Creamos el vector de abiertos
        PriorityQueue<Nodo> abiertos = new PriorityQueue<Nodo>(new NodoComparator());
        abiertos.add(inicial);

        // Creamos el vector de cerrados
        ArrayList<Nodo> cerrados = new ArrayList<Nodo>();


        Nodo nodo = null;

        while (!abiertos.isEmpty()){
            // Cogemos el nodo con mejor f(n)
            nodo = abiertos.peek();

            // Si el nodo es el nodo destino, termina
            if ((nodo.getPosition().x == destino.x) && (nodo.getPosition().y == destino.y)){
                break;
            }

            // Creamos un vector de los sucesores del nodo
            ArrayList<Nodo> hijos = expandirNodo(nodo, stateObs, destino);

            for (int i=0; i<hijos.size(); i++){
                Nodo sucesor = hijos.get(i);

                // si el sucesor es el mismo que el padre no hacemos nada
                if(sucesor==sucesor.getPadre()){}
                else {
                    // Vemos si el nodo esta en cerrados y guardamos el indice
                    int indice_c = cerrados.indexOf(sucesor);
                    // Vemos si el nodo esta en abiertos y guardamos el indice
                    int indice_a = isAbiertos(abiertos.toArray(new Nodo[0]),sucesor.getPosition(), sucesor.getOrientacion());

                    // Si el nodo esta en cerrados
                    if(indice_c != -1){
                       // Vemos si el sucesor tiene menor g(n) que el que esta en cerrados
                       if(cerrados.get(indice_c).getG() > sucesor.getG()){
                           // si es asi lo quitamos de cerrados y lo metemos en abiertos
                           cerrados.remove(i);
                           abiertos.add(sucesor);
                       }
                    }
                    // Si el sucesor no esta en abiertos lo añadimos
                    else if (indice_a == -1){
                        abiertos.add(sucesor);
                    }
                    // Si el sucesor esta en abiertos y tiene menor g(n) que el que esta en cerrados
                    else if (abiertos.toArray(new Nodo[0])[indice_a].getG() > sucesor.getG()){
                        // Borramos el nodo que teniamos en abiertos y añadimos el nuevo sucesor
                        abiertos.remove(abiertos.toArray(new Nodo[0])[indice_a]);
                        abiertos.add(sucesor);
                    }

                }
            }

            // Eliminamos el nodo que acabamos de explorar de abiertos
            abiertos.remove(nodo);



        }
        // Si no llegase a encontrar el camino al destino devolvemos ACTION_NIL
        if((nodo.getPosition().x!=destino.x)||(nodo.getPosition().y!=destino.y)){
            camino.add(ACTIONS.ACTION_NIL);

        }
        else{
            // Calculamos el camino viendo la accion que hemos necesitado para llegar a cada nodo
            while(nodo.getPadre() != null){
                camino.push(nodo.getAccion());
                // En el caso en el que hayamos necesitado repetir la accion al estar en otra orientacion,
                // la volvemos a añadir
                if(nodo.isAccion_dup())
                    camino.push(nodo.getAccion());
                nodo = nodo.getPadre();
            }
        }

        return camino;

    }

}
