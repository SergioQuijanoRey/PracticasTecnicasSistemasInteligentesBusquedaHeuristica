package src_Quijano_Rey_Sergio;

import java.util.Comparator;
import src_Quijano_Rey_Sergio.*;

public class AStarNodeComparator implements Comparator<AStarNode> {
    @Override
    /**
     * Funcion de ordenacion.
     *
     * Necesario para el PriorityQueue que usa A*
     *
     * @param first  el primer nodo
     * @param second el segundo nodo
     * @return un entero codificando la comparacion de los dos nodos
     */
    public int compare(AStarNode first, AStarNode second) {
        if (first.heuristic_value() < second.heuristic_value()) {
            return -1;
        }

        if (first.heuristic_value() > second.heuristic_value()) {
            return 1;
        }

        return 0;
    }

}
