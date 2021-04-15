package src_Quijano_Rey_Sergio;

// Tipos de datos de Java
import java.util.ArrayList;
import java.util.HashSet;

// Tipos de datos de GVGAI
import core.game.StateObservation;
import core.game.Observation;
import java.awt.Dimension;
import tools.Vector2d;
import ontology.Types;

// Mis tipos de datos auxiliares
import src_Quijano_Rey_Sergio.*;

/**
 * Clase que representa un nodo para la busqueda en A*.
 * @author Sergio Quijano Rey
 *
 * */
public class AStarNode{
    /**
     * Posicion en el mapa
     * */
    private GridPosition position = null;

    /**
     * Orientacion del avatar
     * */
    private Orientation orientation = null;

    /**
     * Objetivo a alcanzar
     * */
    private GridPosition objective = null;

    /**
     * Nodo padre de este nodo
     * */
    private AStarNode parent = null;

    /**
     * Accion necesaria para llegar desde el padre hasta el hijo
     * */
    private Types.ACTIONS action = null;

    /**
     * Indica si necesitamos repetir la accion (por un cambio de orientacion) para llegar del padre
     * al hijos
     * */
    private boolean needs_two_steps = false;

    /**
     * Coste acumulado para llegar a este nodo
     * */
    private int current_cost = 0;

    /**
     * Constructor que vamos a usar para iniciar el arbol de busqueda
     * */
    AStarNode(GridPosition starting_position, GridPosition objective_position, Orientation starting_orientation){
        // Tomamos los datos que se nos pasa por parametro
        this.position = starting_position;
        this.objective = objective_position;
        this.orientation = starting_orientation;
    }

    /**
     * Constructor que vamos a usar para, a partir de un nodo, construir los nodos hijos
     * */
    AStarNode(GridPosition current_pos, GridPosition objective_position, Orientation orientation, AStarNode parent, int cost, Types.ACTIONS action_to_get_here, boolean needs_two_steps){
        this.position = current_pos;
        this.objective = objective_position;
        this.orientation = orientation;
        this.parent = parent;
        this.current_cost = cost;
        this.action = action_to_get_here;
        this.needs_two_steps = needs_two_steps;
    }

    /**Getter del coste del nodo */
    public int getCost(){
        return this.current_cost;
    }

    /**Getter de la posicion */
    public GridPosition getPosition(){
        return this.position;
    }

    /**Getter de la orientacion */
    public Orientation getOrientation(){
        return this.orientation;
    }

    /**
     * Getter del padre de este nodo.
     * Puede devolver null
     * */
    public AStarNode getParent(){
        return this.parent;
    }

    /**
     * Valor de la funcion de ordenacion. Es la suma del coste acumulado y el valor heuristico hasta
     * el objetivo
     * */
    public int f(){
        return this.current_cost + this.heuristic_value();
    }

    /**
     * Calcula el valor heuristico del coste para llegar desde la posicion actual hasta el nodo
     * objetivo
     * */
    public int heuristic_value(){
        return GridPosition.manhattan_distance(this.position, this.objective);
    }

    /**
     * Comprueba si la posicion de este nodo es la posicion del objetivo.
     * En A* usamos esto para ver si hemos alcanzado uan solucion
     * */
    public boolean isObjective(){
        return this.position.equals(this.objective);

    }

    /**
     * Generamos los hijos validos de este nodo.
     * @param world_dimensions_grid para ver que los hijos generados caen dentro del mapa
     * @param inmovable_grid_positions para ver que los hijos no caen en muros u otras posiciones
     * no validas
     * */
    public ArrayList<AStarNode> generate_childs(GridPosition world_dimensions_grid, ArrayList<GridPosition> inmovable_grid_positions){
        // Todas las posibles variaciones en las posiciones del mapa
        int [][] deltas = {
            {-1, 0},
            {1, 0},
            {0, -1},
            {0, 1}
        };

        // Hijos validos que vamos a devolver
        ArrayList<AStarNode> valid_childs = new ArrayList<AStarNode>();

        for(int[] delta: deltas){
            // Movemos una posicion en el mapa
            int x_delta = delta[0];
            int y_delta = delta[1];

            // Generamos la nueva posicion y comprobamos que sea valido
            GridPosition new_position = new GridPosition(this.position.getX() + x_delta, this.position.getY() + y_delta);
            if(new_position.isValidToMove(world_dimensions_grid, inmovable_grid_positions) == false){
                // TODO -- Sergio -- no mostrar mensajes por pantalla
                continue;
            }

            // La posicion es valida asi que generamos el nodo asociado a esa posicion para añadirlo
            // a la lista de hijos validos
            // Calculos necesarios para generar el nuevo nodo:

            // Nueva orientacion
            Orientation new_orientation = new Orientation(x_delta, y_delta);

            // Necesitamos dos pasos?
            boolean new_needs_two_steps = false;
            if(this.orientation.equals(new_orientation) == false){
                new_needs_two_steps = true;
            }

            // Accion a realizar
            Types.ACTIONS new_action = new_orientation.getAction();

            // Nuevo coste
            int new_cost = this.current_cost + 1;
            if(new_needs_two_steps == true){
                new_cost = new_cost + 1;
            }

            // A partir de los calculos generamos el nuevo nodo hijo y lo añadimos
            AStarNode child = new AStarNode(new_position, this.objective, new_orientation, this, new_cost, new_action, new_needs_two_steps);
            valid_childs.add(child);
        }

        return valid_childs;
    }

    /**
     * Comprueba si el nodo hijo coincide con el nodo padre
     * */
    public boolean isSameAsParent(){
        return this == this.parent;
    }

    /**
     * Devuelve las acciones para llegar desde el padre hasta el hijo.
     * */
    public ArrayList<Types.ACTIONS> getActions(){
        ArrayList<Types.ACTIONS> actions = new ArrayList<Types.ACTIONS>();
        actions.add(this.action);
        if(this.needs_two_steps){
            actions.add(this.action);
        }

        return actions;
    }

}
