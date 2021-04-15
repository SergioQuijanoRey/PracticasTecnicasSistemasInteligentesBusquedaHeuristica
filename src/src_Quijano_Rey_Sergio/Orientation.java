package src_Quijano_Rey_Sergio;

import tools.Vector2d;
import src_Quijano_Rey_Sergio.GridPosition;
import ontology.Types;

/**
 * Clase que representa comodamente la orientacion de un jugador.
 * La orientacion de un jugador, en GVGAI, es un Vector2d que es mas complicado
 * de interpretar
 *
 * @author Sergio Quijano Rey
 * */
public class Orientation{
    // Cuatro orientaciones, solo una puede ser true
    private boolean left = false;
    private boolean right = false;
    private boolean down = false;
    private boolean up = false;

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

        if(orientation.y < 0){
            this.up = true;
        }

        if(orientation.y > 0){
            this.down = true;
        }
    }

    /**
     * Orientacion en formato enteros.
     * Util cuando queremos ver la orientacion de un delta
     * */
    Orientation(int x_delta, int y_delta){
        GridPosition orientation = new GridPosition(x_delta, y_delta);
        if(orientation.getX() < 0){
            this.left = true;
        }

        if(orientation.getX() > 0){
            this.right = true;
        }

        if(orientation.getY() < 0){
            this.up = true;
        }

        if(orientation.getY() > 0){
            this.down = true;
        }
    }


    /**
     * @param orientation orientacion en formato GridPosition
     * Util cuando calculamos cambios entre un GridPosition y otro
     * */
    Orientation(GridPosition orientation){
        if(orientation.getX() < 0){
            this.left = true;
        }

        if(orientation.getX() > 0){
            this.right = true;
        }

        if(orientation.getY() < 0){
            this.up = true;
        }

        if(orientation.getY() > 0){
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

    @Override
    public boolean equals(Object o){
        // Para evitar errores de puntero nulo
        if(o == null){
            return false;
        }

        // Para comprobar que estamos comparando con la misma clase
        if(this.getClass() != o.getClass()){
            return false;
        }

        // Casteamos y devolvemos la igualdad que queremos
        Orientation other = (Orientation) o;
        return this.up == other.up && this.down == other.down && this.left == other.left && this.right == other.right;
    }

    @Override
    public String toString(){
        if(this.left){
            return "Izquierda";
        }

        if(this.right){
            return "Derecha";
        }

        if(this.up){
            return "Arriba";
        }

        if(this.down){
            return "Abajo";
        }

        return "Sin orientacion";
    }

    /**
     * Comprueba si la orientacion es valida. Para debuggear el codigo. El proyecto
     * final no usa esta utilidad.
     * */
    boolean check_valid(){
        int activated_orientations = 0;
        if(this.left){
            activated_orientations = activated_orientations + 1;
        }

        if(this.right){
            activated_orientations = activated_orientations + 1;
        }

        if(this.up){
            activated_orientations = activated_orientations + 1;
        }

        if(this.down){
            activated_orientations = activated_orientations + 1;
        }


        return activated_orientations == 0 || activated_orientations == 1;
    }

    /**
     * Devuelve la accion asociada a esta orientacion (ie. si mira a la izquierda devuelve la accion
     * moverse a la izquierda)
     * */
    public Types.ACTIONS getAction(){
        if(this.left){
            return Types.ACTIONS.ACTION_LEFT;
        }

        if(this.right){
            return Types.ACTIONS.ACTION_RIGHT;
        }

        if(this.up){
            return Types.ACTIONS.ACTION_UP;
        }

        if(this.down){
            return Types.ACTIONS.ACTION_DOWN;
        }

        return Types.ACTIONS.ACTION_NIL;

    }
}
