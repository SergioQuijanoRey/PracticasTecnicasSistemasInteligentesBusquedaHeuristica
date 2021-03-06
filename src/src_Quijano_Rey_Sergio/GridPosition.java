package src_Quijano_Rey_Sergio;

/**
 * Enlaces usados:
 *
 * [1] http://users.csc.calpoly.edu/~gfisher/classes/102/info/howToOverrideEquals.html#:~:text=Notice%20the%20use%20of%20the,String%20objects%20character%20by%20character.&text=Remember%2C%20every%20class%20in%20Java,an%20Object%20(via%20inheritance)
 *  - Tutorial para hacer la sobrecarga del operador de igualdad
 * [2] http://lineadecodigo.com/java/usando-las-clases-hashset-y-hashmap/
 *  - Tutorial donde veo que tambien tengo que sobreescribir el operador de hash
 * */

// Clases de GVGAI
import tools.Vector2d;
import core.game.StateObservation;


import java.util.ArrayList;
import java.util.HashSet;


/**
 * Representa la posicion en el grid de un mapa, conociendo los pixeles de la posicion
 * @author Sergio Quijano Rey
 *
 * En muchos casos del juego, las posiciones en pixeles nos van a servir. Pero para
 * otros casos, como la busqueda A*, tener la representacion exacta en el grid
 * va a mejorar el rendimiento de los algorimtos
 * */
public class GridPosition{
    // Las coordenadas en el Grid
    private int x;
    private int y;

    /**
     * Constructor por defecto
     * */
    GridPosition(){
        this.x = 0;
        this.y = 0;
    }

    /**
     * Constructor al que pasamos las dos coordenadas
     * */
    GridPosition(int x, int y){
        this.x = x;
        this.y = y;
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
        this.x = (int) Math.floor(position.x / vector_escala.x);
        this.y = (int) Math.floor(position.y / vector_escala.y);
    }

    /**
     * A partir de una posicion en pixeles, genera la posicion en el grid.
     * @param position la posicion en pixeles a convertir
     * @param scale_factor factor de escala para realizar la conversion
     *
     * Codigo basado en el dado en el tutorial de GVGAI de los profesores de practicas
     * */
    GridPosition(Vector2d position, Vector2d scale_factor){
        // Usamos el vector de escala para realizar la conversion
        this.x = (int) Math.floor(position.x / scale_factor.x);
        this.y = (int) Math.floor(position.y / scale_factor.y);
    }

    /**
     * A partir de una posicion en pixeles enteros, genera la posicion en el grid.
     * @param position la posicion en
     * @param stateObs observacion del mundo. Necesario para conocer el tamaño
     * del mundo y con ello hacer la conversion
     *
     * Codigo basado en el dado en el tutorial de GVGAI de los profesores de practicas
     * */
    GridPosition(int pixel_x, int pixel_y, StateObservation stateObs){
        // Factor de escala de pixeles a grid
        Vector2d vector_escala = new Vector2d(
            stateObs.getWorldDimension().width / stateObs.getObservationGrid().length,
            stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length
        );

        // Usamos el vector de escala para realizar la conversion
        this.x = (int) Math.floor(pixel_x / vector_escala.x);
        this.y = (int) Math.floor(pixel_y / vector_escala.y);
    }

    // Getters basicos
    public int getX(){
        return this.x;
    }

    public int getY(){
        return this.y;
    }

    /**
     * Distancia Manhattan entre dos posiciones.
     * Sera usado como valor heuristico en la funcion f (junto al coste acumulado
     * de una posicion)
     * @param first primera posicion
     * @param second segunda posicion
     * */
    static int manhattan_distance(GridPosition first, GridPosition second){
        int x_diff = Math.abs(first.x - second.x);
        int y_diff = Math.abs(first.y - second.y);
        return x_diff + y_diff;

    }

    /**
     * Para debuggear posiciones por la terminal. No tiene impacto en el proyecto
     * final porque no podemos mostrar mensajes por pantalla
     * */
    @Override
    public String toString(){
        String msg = "GridPosition(x: " + this.x + " y: " + this.y + ")";
        return msg;
    }

    /**
     * Sobrecargamos el operador de igualdad.
     * De otra forma, se comprueba que el puntero de un objeto sea igual que
     * otro puntero del objeto con el que se compara. Esto da problemas en el
     * HashSet de posiciones que usamos en A*
     * Gracias a [1] consigo programar esta sobrecarga
     * */
    @Override
    public boolean equals(Object o){
        // Para evitar errores de puntero nulo
        if(o == null){
            return false;
        }

        // Para comprobar que estamos comparando con la misma clase
        if(this.getClass() != o.getClass()){
            return false;
        }

        // Casteamos y devolvemos la igualdad que queremos
        GridPosition o_casted = (GridPosition) o;
        return this.x == o_casted.x && this.y == o_casted.y;
    }

    /**
     * Genera un hash para la clase.
     * Es necesario para usar un HashSet sobre esta clase y comprobar si una posicion pertenece
     * al conjunto de posiciones inamovibles de forma rapida
     * */
    @Override
    public int hashCode(){
        return this.x * this.y;

    }

    /**
     * Util para calcular la diferencia entre dos posiciones, y con ello, saber
     * en que direccion movernos para pasar de la primera posicion a la segunda
     * */
    public GridPosition minus(GridPosition other){
        return new GridPosition(this.x - other.x, this.y - other.y);
    }

    /**
     * Util para calcular desplazamientos de posiciones
     * */
    public GridPosition plus(GridPosition other){
        return new GridPosition(this.x + other.x, this.y + other.y);
    }

    /**
     * Comprueba si la posicion es valida para realizar un movimiento
     * @param world_dimensions_grid dimensiones del mundo, para comprobar si nos salimos del mapa
     * @param inmovable_grid_positions muros del mundo que no podemos atravesar
     * */
    public boolean isValidToMove(GridPosition world_dimensions_grid, HashSet<GridPosition> inmovable_grid_positions){
        // Nos salimos del mapa por arriba o por la izquierda
        if(this.x < 0 || this.y < 0){
            return false;
        }

        // Nos salimos del mapa por la derecha o por abajo
        if(this.x >= world_dimensions_grid.x || this.y >= world_dimensions_grid.y){
            return false;
        }

        // Comprobamos que no sea una posicion inamovible
        if(inmovable_grid_positions.contains(this)){
            return false;
        }

        // Todas las comprobaciones han pasado sin problemas
        return true;
    }

    /**
     * Toma los GridPosition dentro del mapa que estan a un radio dado
     * @param vision_radius radio de vision para devolver elementos
     * @pre vision_radius >= 0
     * @param world_dimensions para saber si una posicion esta dentro del mapa o no
     * */
    public ArrayList<GridPosition> get_surroundings(int vision_radius, GridPosition world_dimensions){
        ArrayList<GridPosition> surroundings = new ArrayList<GridPosition>();

        for(int row_delta = -vision_radius; row_delta <= vision_radius; row_delta++){
            for(int col_delta = -vision_radius; col_delta <= vision_radius; col_delta++){

                // Generamos la nueva posicion y comprobamos que este dentro del mapa
                GridPosition new_pos = new GridPosition(this.x + col_delta, this.y + row_delta);
                if(new_pos.isInsideMap(world_dimensions) == false){
                    continue;
                }

                // Añadimos la posicion al conjunto que devolvemos
                surroundings.add(new_pos);
            }
        }

        return surroundings;
    }

    /**
     * Comprueba si este GridPosition esta dentro de los confines del mapa.
     * @param world_dimensions dimensiones del mapa para comprobar si estamos dentro o no
     * */
    public boolean isInsideMap(GridPosition world_dimensions){
        if(this.x < 0 || this.y < 0){
            return false;
        }

        if(this.x >= world_dimensions.x || this.y >= world_dimensions.y){
            return false;
        }

        return true;
    }

    /**
     * Devuelve el Vector2d original asociado a esta GridPosition
     * @param scale_factor el factor de escala necesario para la reconversion
     * */
    public Vector2d toVector2d(Vector2d scale_factor){
        return new Vector2d(this.x * scale_factor.x, this.y * scale_factor.y);
    }
}
