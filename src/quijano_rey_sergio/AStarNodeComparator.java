package quijano_rey_sergio;

// Tipos de datos de java
import java.util.Comparator;

// Mis tipos de datos
import quijano_rey_sergio.AStarNode;

/**
 * Clase para comparar nodos en la busqueda de A*.
 * En concreto, esto nos permite usar comodamente una PriorityQueue
 * Los nodos se comparan segun su valor heuristico (distancia Manhattan al objetivo
 * mas coste acumulado del nodo)
 * @author Sergio Quijano Rey
 * */
public class AStarNodeComparator implements Comparator<AStarNode>{
    /**
     * Compara dos nodos.
     * Induce una ordenacion ascendente de valor heuristico (primero el de menor valor)
     * @return 0 si ambos nodos tienen el mismo valor heuristico
     * @return 1 si tiene mas valor heuristico que
     * @return -1 en otro caso
     * */
    @Override
    public int compare(AStarNode first, AStarNode second){
        int heuristic_first = first.heuristic_value();
        int heuristic_second = second.heuristic_value();

        if(heuristic_first > heuristic_second){
            return 1;
        }

        if(heuristic_first < heuristic_second){
            return -1;
        }

        // Ambos tienen mismo valor heuristico
        return 0;
    }
}
