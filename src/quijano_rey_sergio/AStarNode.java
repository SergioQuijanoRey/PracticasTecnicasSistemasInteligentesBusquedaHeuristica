package quijano_rey_sergio;

// Tipos de datos de Java
import java.util.ArrayList;
import java.util.HashSet;

// Tipos de datos de GVGAI
import core.game.StateObservation;
import core.game.Observation;

// Mis tipos de datos auxiliares
import quijano_rey_sergio.GridPosition;

/**
 * Clase que representa un nodo para la busqueda en A*.
 * Este nodo se compone de un GridPosition y una lista de GridPosition, que nos
 * lleva desde la posicion inicial hasta esta posicion
 * @author Sergio Quijano Rey
 * */
public class AStarNode{
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
     * */
    public ArrayList<AStarNode> generate_childs(StateObservation stateObs){
        // Todos los hijos tienen como path el path del padre mas en nodo padre
        ArrayList<GridPosition> new_path = this.path_to_position;
        new_path.add(this.position);

        // Tomo los elementos del mapa que son inmovibles
        // Los convierto a un set de GridPosition para que sea mas rapido hacer
        // las comparaciones posteriores
        // TODO -- Sergio -- Pasar como parametro inmovable_grid_positions para
        // no repetir calculos en cada llamada de generate_childs
        // TODO -- Sergio -- Por que devolver array list y [] ?? Matrix ??
        ArrayList<Observation>[] inmovables_obs = stateObs.getImmovablePositions();
        HashSet<GridPosition> inmovable_grid_positions = new HashSet<GridPosition>();
        for(ArrayList<Observation> row : inmovables_obs){
            for(Observation obs : row){
                GridPosition current_inmovable_grid = new GridPosition(obs.position, stateObs);
                inmovable_grid_positions.add(current_inmovable_grid);
            }
        }

        // Genero todos los hijos posibles, y me quedo con los que sean validos
        ArrayList<AStarNode> valid_childs = new ArrayList<AStarNode>();
        int[] x_deltas = {-1, 1};
        int[] y_deltas = {-1, 1};
        for(int x_delta: x_deltas){
            for(int y_delta: y_deltas){

                // Posicion resultante de aplicar los deltas
                int new_x = this.position.x + x_delta;
                int new_y = this.position.y + y_delta;
                GridPosition new_position = new GridPosition(new_x, new_y);

                // Compruebo las condiciones para que un punto no sea valido
                // TODO -- Sergio -- No estoy comprobando que se salga por exceso del mapa
                if(new_x < 0 || new_y < 0){
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

    // Getters basicos
    //==========================================================================
    public GridPosition get_position(){
        return this.position;
    }

    public GridPosition get_objective(){
        return this.objective;
    }

    public ArrayList<GridPosition> get_path_to_position(){
        return this.get_path_to_position();
    }


}
