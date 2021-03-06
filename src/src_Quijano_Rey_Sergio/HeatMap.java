package src_Quijano_Rey_Sergio;

import java.util.HashSet;
import java.util.ArrayList;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import core.game.Observation;

/**
 * Representa un mapa de calor para el comportamiento reactivo
 * */
public class HeatMap{
    // Tipos de elementos que proporcionan calor
    private int wall_type = 1;
    private int enemy_type = 2;

    /**
     * Los enemigos son mas prioritarios que los muros. Por eso este factor de escala que debiera ser
     * mayor o igual que uno
     * */
    private double enemy_scale_factor = 100.0;

    // Valores del calor
    private double[][] heat_map;

    /***
     * Constructor del mapa de calor asociado a los muros del mapa.
     * @param walls el hash set con los muros
     * @param wall_radius radio de efecto de los muros
     * @param world_dimensions dimensiones del mundo, para saber si nos salimos del mundo
     * */
    HeatMap(HashSet<GridPosition> walls, int wall_radius, GridPosition world_dimensions){
        // Inicializamos la matriz de valores double
        this.heat_map = new double[world_dimensions.getY()][world_dimensions.getX()];

        // Iteramos sobre los muros, en vez de sobre posiciones del grid, porque asi es mas rapido
        for(GridPosition current_wall: walls){
            // Tomamos las posiciones que se ven en el radio dado desde el muro actual
            for(GridPosition current_element: current_wall.get_surroundings(wall_radius, world_dimensions)){
                this.heat_map[current_element.getY()][current_element.getX()] += this.heat_by_distance(current_wall, current_element, this.wall_type);
            }
        }
    }

    /***
     * Constructor del mapa de calor para enemigos
     * @param enemies_position posiciones de los enemigos
     * @param vision_radius radio de vision
     * @param world_dimensions dimensiones del mundo, para saber si nos salimos del mundo
     * */
    HeatMap(ArrayList<GridPosition> enemies_position, int vision_radius, GridPosition world_dimensions){
        // Inicializamos la matriz de valores double
        this.heat_map = new double[world_dimensions.getY()][world_dimensions.getX()];

        // Iteramos sobre los muros, en vez de sobre posiciones del grid, porque asi es mas rapido
        for(GridPosition curr_enemy_pos: enemies_position){
            // Tomamos las posiciones que se ven en el radio dado desde el muro actual
            for(GridPosition current_element: curr_enemy_pos.get_surroundings(vision_radius, world_dimensions)){
                this.heat_map[current_element.getY()][current_element.getX()] += this.heat_by_distance(curr_enemy_pos, current_element, this.enemy_type);
            }
        }
    }

    /**
     * Calcula el calor asociado a la distancia entre dos elementos.
     * @param first primera posicion
     * @param second segunda posicion
     * @param type tipo de elemento que proporciona el calor
     *        type == 1 -> Muro
     *        type == 2 -> Enemigo
     * */
    double heat_by_distance(GridPosition first, GridPosition second, int type){
        if(type == this.wall_type){
            return this.heat_by_distance_wall(first, second);
        }

        if(type == this.enemy_type){
            return this.heat_by_distance_enemy(first, second);
        }

        // Esta situacion no deberia ocurrir
        return 0.0;
    }

    /** Funcion de calor para los muros */
    double heat_by_distance_wall(GridPosition first, GridPosition second){
        // Calor inversamente proporcional al cuadrado de la distancia manhattan
        int manhattan_distance = GridPosition.manhattan_distance(first, second);
        return 1.0 / (double)(manhattan_distance * manhattan_distance);
    }

    /** Funcion de calor para los enemigos */
    double heat_by_distance_enemy(GridPosition first, GridPosition second){
        // Calor inversamente proporcional al cuadrado de la distancia manhattan
        int manhattan_distance = GridPosition.manhattan_distance(first, second);
        return (1.0 / (double)(manhattan_distance * manhattan_distance)) * this.enemy_scale_factor;
    }

    @Override
    public String toString(){
        String result = "HeatMap:\n";
        for(int row = 0; row < this.heat_map.length; row++){
            for(int col = 0; col < this.heat_map[row].length; col++){
               result = result + this.heat_map[row][col] + ", ";
            }

            result = result + "\n";
        }

        return result;
    }

    /**
     * Devuelve el calor de una posicion dada
     * @param pos la posicion cuyo calor devolvemos
     * */
    double getHeat(GridPosition pos){
        return this.heat_map[pos.getY()][pos.getX()];
    }
}

