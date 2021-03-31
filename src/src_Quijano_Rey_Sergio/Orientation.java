package src_Quijano_Rey_Sergio;

import tools.Vector2d;

/**
 * Clase que representa comodamente la orientacion de un jugador.
 * La orientacion de un jugador, en GVGAI, es un Vector2d que es mas complicado
 * de interpretar
 *
 * @author Sergio Quijano Rey
 * */
public class Orientation{
    // Cuatro orientaciones, solo una puede ser true
    boolean left = false;
    boolean right = false;
    boolean down = false;
    boolean up = false;

    /**
     * @param orientation orientacion de GVGAI en formato Vector2d
     * */
    Orientation(Vector2d orientation){
        if(orientation.x < 0){
            this.left = true;
        }

        if(orientation.x > 0){
            this.right = true;
        }

        if(orientation.y > 0){
            this.up = true;
        }

        if(orientation.y < 0){
            this.down = true;
        }
    }

    // Getter para saber si tenemos una u otra orientacion
    public boolean isLookingLeft(){
        return this.left;
    }

    public boolean isLookingRight(){
        return this.right;
    }

    public boolean isLookingUp(){
        return this.up;
    }

    public boolean isLookingDown(){
        return this.down;
    }

}
