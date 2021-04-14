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
import src_Quijano_Rey_Sergio.Orientation;

/**
 * Clase que representa un nodo para la busqueda en A*.
 * Este nodo se compone de un GridPosition y una lista de GridPosition, que nos
 * lleva desde la posicion inicial hasta esta posicion
 * @author Sergio Quijano Rey
 *
 * */
public class AStarNode{
    /**
     * Posicion del mapa que representa este nodo
     * */
    private GridPosition position;

    /**
     * Posicion que queremos alcanzar.
     * Necesaria para calcular el valor heuristico de este nodo
     * */
    private GridPosition objective;

    /**
     * Posiciones que nos llevan desde la posicion de inicio hasta la posicion
     * que representa este nodo
     * */
    private ArrayList<GridPosition> path_to_position;

    /**
     * Coste del camino a la posicion actual. Tenemos que guardarlo aparte y no
     * calcularlo a paritr de path_to_position porque los cambios de direccion
     * añaden costes adicionales
     * */
    private int path_cost;

    /**
     * Orientacion del jugador. Necesitamos guardar este estado porque los cambios
     * de orientacion influyen en los costes.
     * */
    private Orientation orientation;

    /**
     * Constructor
     * */
    AStarNode(GridPosition position, GridPosition objective, ArrayList<GridPosition> path_to_position, int path_cost, Orientation orientation){
        this.position = position;
        this.objective = objective;
        this.path_to_position = path_to_position;
        this.path_cost = path_cost;
        this.orientation = orientation;
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

        // Posibles variaciones en las coordenadas
        int[][] deltas = {
            {0, -1},
            {0, 1},
            {-1, 0},
            {1, 0}
        };

        for(int[] delta : deltas){

            // Variacion descompuesta en las coordenadas
            int x_delta = delta[0];
            int y_delta = delta[1];

            // Orientacion asociada al delta que aplicamos
            Orientation new_orientation = new Orientation(new GridPosition(x_delta, y_delta));

            // Posicion resultante de aplicar los deltas
            int new_x = this.get_position().getX() + x_delta;
            int new_y = this.get_position().getY() + y_delta;
            GridPosition new_position = new GridPosition(new_x, new_y);

            // Comprobamos que no nos salgamos por la izquierda o por arriba
            // del mapa
            if(new_x < 0 || new_y < 0){
                continue;
            }

            // Comprobamos que no nos salgamos por la derecha o por debajo
            // del mapa
            if(new_x >= world_dimensions_grid.getX() || new_y >= world_dimensions_grid.getY()){
                continue;
            }

            // Compruebo que no sea una posicion inmovible, es decir, un muro
            if(inmovable_grid_positions.contains(new_position)){
                continue;
            }

            // Coste extra derivado de cambiar de direccion
            int extra_cost = 0;
            if(this.orientation.equals(new_orientation) == false){
                extra_cost = 1;
            }

            // Coste del nodo hijo
            int new_cost = this.path_cost + 1 + extra_cost;

            // Todas las condiciones son validas, asi que añado el nodo a la
            // lista de hijos validos
            valid_childs.add(new AStarNode(new_position, this.objective, new_path, new_cost, new_orientation));
        }

        return valid_childs;
    }

    /**
     * Calcula el valor heuristico del nodo.
     * Esto es, valor acumulado del camino al nodo actual mas la distancia Manhattan
     * al objetivo
     * */
    int heuristic_value(){
        // TODO -- Sergio -- cambiando esto por this.path_to_position.size() soluciona
        // el comportamiento erratico del agente
        return this.path_cost + GridPosition.manhattan_distance(this.position, this.objective);
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

    public int get_path_cost(){
        return this.path_cost;
    }
}
