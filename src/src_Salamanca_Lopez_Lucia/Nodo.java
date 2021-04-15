package src_Salamanca_Lopez_Lucia;

import core.game.Observation;
import java.util.Comparator;
import ontology.Types.ACTIONS;
import tools.Vector2d;

/**
 *
 * @author Lucía Salamanca López
 */
public class Nodo {
    private Vector2d position = null;
    private int g = 0; // función de coste
    private int h = 0; // función heurística
    private int f = 0;

    private Nodo padre = null; // nodo padre
    private ACTIONS accion = ACTIONS.ACTION_NIL; // accion para llegar al nodo desde el padre
    private boolean accion_dup = false; // la accion para llegar al nodo ha de ser realizada dos veces (debido a un cambio de orientacion)
    private Vector2d orientacion = null; // orientacion

    // Constructor para el nodo inicial
    public Nodo(Vector2d posicion, Vector2d destino, Vector2d orientation, ACTIONS action ){
        position = posicion;
        // Calculamos la distancia Manhattan para la heurística
        h = (int)(Math.abs(position.x - destino.x) + Math.abs(position.y-destino.y));
        f = g + h;
        padre = null;
        orientacion = orientation;
        accion = action;
    }

    public Nodo (Nodo n2){
        this.position = n2.position;
        this.g = n2.g;
        this.f = n2.f;
        this.h = n2.h;
        this.padre = n2.padre;
        this.accion = n2.accion;
        this.orientacion = n2.orientacion;
    }

    //Constructor
    public Nodo(Vector2d posicion, Vector2d destino, Nodo nodoPadre, Vector2d orientation, ACTIONS action ){
        position = posicion;
        // Calculamos la distancia Manhattan para la heurística
        h = (int)(Math.abs(position.x - destino.x) + Math.abs(position.y-destino.y));
        f = g + h;
        padre = nodoPadre; // esto puede dar error seguramente
        orientacion = orientation;
        accion = action;
    }

    public String toString(){
        return ("Posicion: "+position.toString()+" f: "+f+" orientacion: "+ orientacion.toString()+" accion "+ accion);
    }

    // Funcion para ver si dos nodos son iguales
    public boolean equals(Nodo n2){
        return ((position.x == n2.getPosition().x) &&(position.x == n2.getPosition().y) && (orientacion.x == n2.getOrientacion().x)&&(orientacion.y == n2.getOrientacion().y));
    }

    public Vector2d getOrientacion() {
        return orientacion;
    }

    public void setOrientacion(Vector2d orientacion) {
        this.orientacion = orientacion;
    }


    public Vector2d getPosition() {
        return position;
    }

    public void setPosition(Vector2d position) {
        this.position = position;
    }

    public int getG() {
        return g;
    }

    public void setG(int g) {
        this.g = g;
        this.f = this.g + this.h;
    }

    public int getF() {
        return f;
    }


    public Nodo getPadre() {
        return padre;
    }

    public void setPadre(Nodo padre) {
        this.padre = padre;
    }

    public ACTIONS getAccion() {
        return accion;
    }

    public void setAccion(ACTIONS accion) {
        this.accion = accion;
    }

    public boolean isAccion_dup() {
        return accion_dup;
    }

    public void setAccion_dup(boolean accion_dup) {
        this.accion_dup = accion_dup;
    }



}
