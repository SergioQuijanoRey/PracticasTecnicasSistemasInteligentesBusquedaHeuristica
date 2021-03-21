package quijano_rey_sergio;

import tools.Vector2d;
import core.game.StateObservation;

/**
 * Representa la posicion en el grid de un mapa, conociendo los pixeles de la posicion
 * @author Sergio Quijano Rey
 * */
public class GridPosition{
    int x;
    int y;

    /**
     * Constructor por defecto
     * */
    GridPosition(){
        this.x = 0;
        this.y = 0;
    }

    /**
     * A partir de una posicion en pixeles, genera la posicion en el grid.
     * @param position la posicion en
     * @param stateObs observacion del mundo. Necesario para conocer el tamaño
     * del mundo y con ello hacer la conversion
     *
     * Codigo basado en el dado en el tutorial de GVGAI de los profesores de practicas
     * */
    GridPosition(Vector2d position, StateObservation stateObs){
        // Factor de escala de pixeles a grid
        Vector2d vector_escala = new Vector2d(
            stateObs.getWorldDimension().width / stateObs.getObservationGrid().length,
            stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length
        );

        // Usamos el vector de escala para realizar la conversion
        this.x = Math.floor(position.x / vector_escala.x);
        this.y = Math.floor(position.y / vector_escala.y);
    }

    /**
     * Distancia Manhattan entre dos posiciones.
     * Sera usado como valor heuristico en la funcion f (junto al coste acumulado
     * de una posicion)
     * @param first primera posicion
     GridPosition @param second segunda posicion
     * */
    static int manhattan_distance(GridPosition first, GridPosition second){
        int x_diff = Math.abs(first.x - second.x);
        int y_diff = Math.abs(first.y - second.y);
        return x_diff + y_diff;

    }

}
