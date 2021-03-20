package quijano_rey_sergio;

import core.game.StateObservation;
import tools.Vector2d;

/**
 * Clase para programar funciones auxiliares que no tengan mucha relacion con la
 * logica de nuestro agente inteligente.
 */
public class AuxiliarFunctions{

    /**
     * Convertimos una observacion a pixeles
     * @param position la posicion que queremos convertir
     * @param stateObs una observacion en la que nos encontramos. Una posicion depende
     * del tamaño del munndo en el que nos encontremos, que conocemos con la observacion
     * @return Integer[2] con los dos pixeles
     * */
    static Integer[] getPixel(Vector2d position, StateObservation stateObs){
        return new Integer[] {0, 0};
    }

}
