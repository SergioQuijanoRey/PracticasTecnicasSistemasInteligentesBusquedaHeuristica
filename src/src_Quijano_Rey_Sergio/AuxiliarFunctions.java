package src_Quijano_Rey_Sergio;

import core.game.StateObservation;
import tools.Vector2d;

import src_Quijano_Rey_Sergio.*;

/**
 * Clase para programar funciones auxiliares que no tengan mucha relacion con la
 * logica de nuestro agente inteligente.
 */
public class AuxiliarFunctions{
    /**
     * Funcion auxiliar para encontrar el nodo que representa una determinada
     * posicion y orientacion
     *
     * @param node_set conjunto de nodos en un conjunto iterable, representan
     * nodos cerrados o nodos abiertos
     * @param position posicion con la que hacemos las comprobaciones
     * @pre debe comprobarse previamente que exista el nodo buscado. En otro caso
     * se devuelve null
     * @return el AStarNode cuya posicion es position y orientacion es orientation
     * */
    public static AStarNode getNodeByGridPositionAndOrientation(Iterable<AStarNode> node_set, GridPosition position, Orientation orientation){
        for(AStarNode current_node : node_set){
            if(current_node.getPosition().equals(position) && current_node.getOrientation().equals(orientation)){
                return current_node;
            }
        }

        // No se ha encontrado el nodo, se devuelve null
        // Esto no deberia pasar por las precondiciones
        return null;
    }
}
