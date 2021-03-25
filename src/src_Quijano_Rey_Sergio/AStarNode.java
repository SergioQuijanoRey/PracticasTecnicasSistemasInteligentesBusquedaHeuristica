package src_Quijano_Rey_Sergio;

// Tipos de datos de Java
import java.util.ArrayList;
import java.util.HashSet;

// Tipos de datos de GVGAI
import core.game.StateObservation;
import core.game.Observation;
import java.awt.Dimension;
import tools.Vector2d;

// Mis tipos de datos auxiliares
import src_Quijano_Rey_Sergio.GridPosition;

/**
 * Clase que representa un nodo para la busqueda en A*.
 * Este nodo se compone de un GridPosition y una lista de GridPosition, que nos
 * lleva desde la posicion inicial hasta esta posicion
 * @author Sergio Quijano Rey
 * */
public class AStarNode implements Comparable<AStarNode>{
    /**
     * Posicion del mapa que representa este nodo
     * */
    GridPosition position;

    /**
     * Posicion que queremos alcanzar.
     * Necesaria para calcular el valor heuristico de este nodo
     * */
    GridPosition objective;

    /**
     * Posiciones que nos llevan desde la posicion de inicio hasta la posicion
     * que representa este nodo
     * */
    ArrayList<GridPosition> path_to_position;

    /**
     * Constructor
     * */
    AStarNode(GridPosition position, GridPosition objective, ArrayList<GridPosition> path_to_position){
        this.position = position;
        this.objective = objective;
        this.path_to_position = path_to_position;
    }

    /**
     * Genera todos los nodos hijos de este nodo.
     * No tiene en cuenta si ya han sido explorados o no, eso no es responsabilidad
     * del nodo.
     * Si que se comprueba que las posiciones que se devuelven sean validas (que
     * no sean muros, rocas, vacios...)
     *
     * @param stateObs observacion del estado del mundo. Es necesaria para saber
     * si una determianda posicion es o no valida
     * @param inmovable_grid_positions las posiciones a las que no nos podemos
     * desplazar. Pasandolo como parametro hace que no repitamos constantemnte
     * el calculo de estas posiciones
     * @param world_dimensions_grid dimensiones en posiciones grid del mapa, para
     * saber si las posiciones con las que trabajamos estan o no dentro del mapa
     * */
    public ArrayList<AStarNode> generate_childs(StateObservation stateObs, HashSet<GridPosition> inmovable_grid_positions, GridPosition world_dimensions_grid){
        // Todos los hijos tienen como path el path del padre mas en nodo padre
        ArrayList<GridPosition> new_path = this.path_to_position;
        new_path.add(this.position);

        // Genero todos los hijos posibles, y me quedo con los que sean validos
        ArrayList<AStarNode> valid_childs = new ArrayList<AStarNode>();
        int[] x_deltas = {-1, 0, 1};
        int[] y_deltas = {-1, 0, 1};
        for(int x_delta: x_deltas){
            for(int y_delta: y_deltas){

                // No queremos generar repetitivamente el padre
                if(x_delta == 0 && y_delta == 0){
                    continue;
                }

                // Posicion resultante de aplicar los deltas
                int new_x = this.position.x + x_delta;
                int new_y = this.position.y + y_delta;
                GridPosition new_position = new GridPosition(new_x, new_y);

                // Comprobamos que no nos salgamos por la izquierda o por arriba
                // del mapa
                if(new_x < 0 || new_y < 0){
                    continue;
                }

                // Comprobamos que no nos salgamos por la derecha o por debajo
                // del mapa
                if(new_x >= world_dimensions_grid.x || new_y >= world_dimensions_grid.y){
                    continue;
                }

                // Compruebo que no sea una posicion inmovible
                if(inmovable_grid_positions.contains(new_position)){
                    continue;
                }

                // Todas las condiciones son validas, asi que añado el nodo a la
                // lista de hijos validos
                valid_childs.add(new AStarNode(new_position, this.objective, new_path));
            }
        }

        return valid_childs;
    }

    /**
     * Calcula el valor heuristico del nodo.
     * Esto es, valor acumulado del camino al nodo actual mas la distancia Manhattan
     * al objetivo
     * */
    int heuristic_value(){
        return this.path_to_position.size() + GridPosition.manhattan_distance(this.position, this.objective);
    }

    /**
     * Compara dos nodos.
     * Lo hacemos para que se induzca una ordenacion ascendente por el valor
     * heuristico (primero el de menor valor)
     */
    @Override
    public int compareTo(AStarNode other){
        int heuristic_first = this.heuristic_value();
        int heuristic_second = other.heuristic_value();

        if(heuristic_first > heuristic_second){
            return 1;
        }

        if(heuristic_first < heuristic_second){
            return -1;
        }

        // Ambos tienen mismo valor heuristico
        return 0;
    }

    // Getters basicos
    //==========================================================================
    public GridPosition get_position(){
        return this.position;
    }

    public GridPosition get_objective(){
        return this.objective;
    }

    public ArrayList<GridPosition> get_path_to_position(){
        return this.path_to_position;
    }


}
