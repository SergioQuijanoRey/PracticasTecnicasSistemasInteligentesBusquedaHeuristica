package src_Quijano_Rey_Sergio;

import tools.Vector2d;
import src_Quijano_Rey_Sergio.GridPosition;

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

        if(orientation.y < 0){
            this.up = true;
        }

        if(orientation.y > 0){
            this.down = true;
        }
    }

    /**
     * @param orientation orientacion en formato GridPosition
     * Util cuando calculamos cambios entre un GridPosition y otro
     * */
    Orientation(GridPosition orientation){
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

}
