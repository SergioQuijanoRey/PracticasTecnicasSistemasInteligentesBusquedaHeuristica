// El paquete que se nos pide que usemos
package src_Quijano_Rey_Sergio;

/**
 * Referencias usadas para el desarrollo de la practica:
 *
 * [1] http://www.gvgai.net/cont.php:
 *  - Pequeño tutorial sobre como crear un agente
 * [2] http://www.gvgai.net/sampleControllers.php
 *  - Ejemplos de algunos controladores
 * */

// Tipos de datos que necesito para interactuar con el entorno GVGAI
import core.game.StateObservation;
import core.player.AbstractPlayer;
import core.game.Observation;

import tools.ElapsedCpuTimer;
import tools.Vector2d;

import ontology.Types;

// Para generar numeros aleatorios
import java.util.Random;

// Tipos de datos de java
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Exception;
import java.util.PriorityQueue;
import java.awt.Dimension;
import java.util.Collections;   // Para iterar y tomar valores minimos

// Tipos de datos auxiliares que he programado
import src_Quijano_Rey_Sergio.*;

/**
 * Codigo del agente inteligente que vamos a desarrollar para resolver el juego
 * de Boulder Dash
 *
 * Tenemos que extender core.player.AbstractPlayer.java como se indica en [1]
 * @author Sergio Quijano Rey
 * */
public class Agent extends core.player.AbstractPlayer{

    /**
     * Nivel en el que nos encontramos.
     * Sabemos el nivel conociendo si hay o no gemas y si hay o no enemigos (y en
     * caso de haberlos, cuantos enemigos hay)
     *
     * Cuando current_level = -1, tenemos que calcular el nivel en el que estamos
     * */
    private int current_level = -1;

    /**
     * Objetivo actual a perseguir.
     * Van a ser gemas y portales por los que escapar
     * Cuando current_objective = null, no tenemos objetivo y tendremos que decidir
     * cual queremos que sea nuestro siguiente objetivo
     * */
    private Vector2d current_objective = null;

    /**
     * Numero de gemas que hay que conseguir en ciertos niveles para escapar por
     * el portal
     * */
    private int number_of_gems_to_get = 9;

    /**
     * Dimensiones del mundo. Para saber donde estan los extremos del mapa
     * */
    private GridPosition world_dimensions_grid = null;

    /**
     * Factor de escala para convertir posiciones en Vector2d a posiciones GridPosition
     * */
    private Vector2d scale_factor = null;

    /**
     * Posiciones inamovibles (muros) del mapa.
     * Estas posiciones no se modifican a lo largo del juego asi que se calculan
     * una unica vez
     * */
    private ArrayList<GridPosition> inmovable_grid_positions = null;

    /**
     * Constructor del agente.
     * Tiene que recibir esos parametros de entrada porque asi se indica en [1]
     * @param so estado del mundo, dado como una observacion
     * @param elapsedTimer timer que nos va a indicar el tiempo de computo que tenemos
     *
     * Tiene un segundo de computo
     * */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer){

        // Miramos en que nivel nos encontramos
        try {
            // Mirando si hay gemas y enemigos, establecemos el nivel en el que
            // nos encontramos
            this.set_level(so, elapsedTimer);
        } catch (Exception ex) {
            // TODO -- Sergio -- mirar como hacer esto mejor
            // Cuando hay una excepcion porque no se ha hecho bien el calculo,
            // empleamos la planificacion mas avanzada que tenemos
            this.current_level = 5;
        }

        // Calculamos las dimensiones del mundo
        Dimension world_dimensions = so.getWorldDimension();
        this.world_dimensions_grid = new GridPosition(world_dimensions.width, world_dimensions.height, so);

        // Calculamos el factor de escala
        this.calculate_scale_factor(so);

        // Calculamos las posiciones inamovibles (muros)
        this.calculate_inamovable_positions(so);

        System.out.println("Posiciones inamovibles: " + this.inmovable_grid_positions);
        System.out.println("Tamaño del mapa: " + this.world_dimensions_grid);
    }

    /**
     * Calculamos el conjunto de GridPosition que son posiciones muro
     * @param stateObs observacion del mundo de la que sacamos la informacion de los muros
     * */
    void calculate_inamovable_positions(StateObservation stateObs){
        // Conjunto de posiciones inamovibles. Necesario para calcular los
        // nodos hijos validos y no repetir constantemente este calculo, pues
        // las posiciones inamovibles no se modifican durante la partida
        ArrayList<Observation>[] inmovables_obs = stateObs.getImmovablePositions();
        this.inmovable_grid_positions = new ArrayList<GridPosition>();
        for(ArrayList<Observation> row : inmovables_obs){
            for(Observation obs : row){
                GridPosition current_inmovable_grid = new GridPosition(obs.position, this.scale_factor);
                this.inmovable_grid_positions.add(current_inmovable_grid);
            }
        }
    }

    /**
     * Calcula el factor de escala que vamos a usar para convertir Vector2d a GridPosition
     * @param stateObs para tomar el tamaño del mundo y hacer el calculo
     * */
    void calculate_scale_factor(StateObservation stateObs){
        this.scale_factor = new Vector2d(
            stateObs.getWorldDimension().width / stateObs.getObservationGrid().length,
            stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length
        );
    }

    /**
     * Para saber cuantas gemas tenemos actualmente.
     * @param stateObs estado del mundo
     * @param elapsedTimer para conocer cuanto tiempo hemos consumido. Permite
     * hacer consultas sobre el tiempo consumido o el tiempo que tenemos restante
     * @return el numero de gemas que tenemos actualmente
     * */
    int get_current_gems(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        // En otros juegos puede haber mas de un rescurso, asi que esta llamada
        // nos devuelve HashMap<idRecurso, cantidadRecurso>
        HashMap<Integer, Integer> resources = stateObs.getAvatarResources();

        // Cuando no tenemos recursos se devuelve un HashMap vacio
        if(resources.isEmpty()){
            return 0;
        }

        // Devolvemos el valor guardado en el Hash
        // La clave para las gemas es 6 (se puede ver facilmente
        // haciendo un resources.toString())
        return resources.get(6);
    }

   /**
     * Elige una accion conociendo el estado del mundo, el nivel y el tiempo que ya esta consumido
     * @param stateObs estado del mundo
     * @param elapsedTimer para conocer cuanto tiempo hemos consumido. Permite
     * hacer consultas sobre el tiempo consumido o el tiempo que tenemos restante
     * @return la accion que debe realizar nuestro agente
     *
     * Dependiendo del nivel en el que estemos, llamamos a uno de los metodos
     * que eligen accion segun el nivel
     * */
    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        switch(this.current_level){
            case 1:
                return this.level1_act(stateObs, elapsedTimer);
            case 2:
                return this.level2_act(stateObs, elapsedTimer);
            case 3:
                return this.level3_act(stateObs, elapsedTimer);
            case 4:
                return this.level4_act(stateObs, elapsedTimer);
            case 5:
                return this.level5_act(stateObs, elapsedTimer);
            default:
                // TODO -- Sergio -- no mostrar mensajes por pantalla
                System.err.println("[Err] El valor actual de nivel no es valido");
                System.err.println("Devolvemos accion nula");
                return Types.ACTIONS.ACTION_NIL;
        }
    }

    /**
     * Elige una accion en la situacion en la que estemos en el nivel 1.
     *
     * En este caso, hacemos A* para calcular el camino hacia la salida y devolvemos
     * en cada paso la accion correspondiente
     *
     * Los calculos solo se hacen una vez. Cuando el plan ya esta construido (no
     * es vacio) simplemente devolvemos el siguiente elemento
     *
     * @param stateObs estado del mundo que nos aporta toda la informacion necesaria para la busqueda
     * @param elapsedTimer timer para saber cuanto tiempo de computo nos queda
     * */
    public Types.ACTIONS level1_act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        return Types.ACTIONS.ACTION_DOWN;
    }

    public Types.ACTIONS level2_act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        return Types.ACTIONS.ACTION_UP;
    }

    public Types.ACTIONS level3_act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        return Types.ACTIONS.ACTION_UP;
    }
    public Types.ACTIONS level4_act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        return Types.ACTIONS.ACTION_UP;
    }
    public Types.ACTIONS level5_act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        return Types.ACTIONS.ACTION_UP;
    }

    /**
     * Con los datos del entorno, establece en que nivel nos encontramos.
     * Esto se hace viendo si hay no hay gemas en el mapa y viendo si hay o no
     * hay enemigos en el mapa
     *
     * @param stateObs estado del mundo, del que tomamos los datos de enemigos y
     * gemas
     * @param elapsedTimer para conocer cuanto tiempo hemos consumido. Permite
     * hacer consultas sobre el tiempo consumido o el tiempo que tenemos restante
     *
     * Modifico el valor de this.current_level
     * */
    void set_level(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) throws Exception{
        // Posicion del jugador
        // Lo necesitamos para ver si hay NPCs, porque se pasa como parametro
        // para devolverlos ordenados ascendentemente por distancia al jugador [2]
        // Tambien lo necesitamos para ordenadr las gemas por orden ascendente
        // de distancia al jugador [2]
        Vector2d player_position = stateObs.getAvatarPosition();

        // Tomamos los enemigos ordenados por la posicion
        ArrayList<Observation>[] enemies = stateObs.getNPCPositions(player_position);

        // Tomamos ahora las posiciones de las gemas
        ArrayList<Observation>[] gems = stateObs.getResourcesPositions(player_position);

        // Decidimos cual es el nivel con los datos recogidos
        Boolean enemies_exist =  enemies != null && enemies.length > 0;
        Boolean gems_exist = gems != null && gems.length > 0;

        if(gems_exist == false && enemies_exist == false){
            this.current_level = 1;
        }
        else if(gems_exist == true && enemies_exist == false){
            this.current_level = 2;
        }
        else if(gems_exist == false && enemies_exist == true){
            // El nivel depende de que haya un enemigo o dos
            if(enemies.length == 1){
                this.current_level = 3;
            }else{
                this.current_level = 4;
            }
        }
        else if(gems_exist == true && enemies_exist == true){
            this.current_level = 5;
        }else{
            // Esto no puede pasar porque deja al programa en un estado invalido
            throw new Exception("El calculo de nivel que ha hecho el agente no es valido");
        }

        System.out.println("Estamos en el nivel " + this.current_level);
    }

    /**
     * Calcula el siguiente objetivo.
     * Cuando no tenemos suficientes gemas, elige como objetivo la gema mas cercana
     * Cuando tenemos todas las gemas (o cuando no hay gemas en el mapa), elige
     * el portal mas cercano al jugador
     *
     * TODO -- Sergio -- Queda por implementar para otros niveles
     * */
    void choose_objective(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        if(this.current_level == 1){
            this.choose_objective_as_closest_portal(stateObs, elapsedTimer);
        }else if(this.current_level >= 2){

            // Tenemos todas las gemas, tenemos que ir al portal
            // Puede ser que tengamos mas de las gemas necesarias porque casualmente
            // pasemos por encima de una gema de camino a otro objetivo
            if(this.get_current_gems(stateObs, elapsedTimer) >= this.number_of_gems_to_get){
                this.choose_objective_as_closest_portal(stateObs, elapsedTimer);

                // Hacemos return para no llamar a this.choose_objective_as_closest_gem
                // que es lo que ocurre cuando no se entra en el if, para dejar
                // el codigo mas limpio
                return;
            }

            // No tenemos todas las gemas, tenemos que elegir la siguiente mas cercana
            this.choose_objective_as_closest_gem(stateObs, elapsedTimer);
        }
    }

    /**
     * Establece el portal mas cercano al jugador como objetivo actual.
     *
     * @param stateObs estado del mundo
     * @param elapsedTimer para conocer cuanto tiempo hemos consumido. Permite
     * hacer consultas sobre el tiempo consumido o el tiempo que tenemos restante
     * */
    void choose_objective_as_closest_portal(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        // Posicion del jugador
        // La tomamos para poder hacer la llamada que nos devuelve los portales
        // ordenados por distancia ascendente a la referencia que pasemos
        Vector2d player_position = stateObs.getAvatarPosition();

        // Tomamos las posiciones de los portales
        ArrayList<Observation>[] portals = stateObs.getPortalsPositions(player_position);

        // Establezco la localizacion del portal mas cercano como objetivo
        // Se devuelve un array de arraylist, por eso tenemos que usar dos veces
        // el indice cero
        this.current_objective = portals[0].get(0).position;
    }

    /**
     * Establece la gema mas cercano al jugador como objetivo actual.
     * Cuidado con la cercania, porque GVGAI devuelve un array con las gemas ordenadas por distancia,
     * pero usando la distancia euclidea. Asi que hay que hacer las comprobaciones de las distancias
     * a mano computando la distancia manhattan
     *
     * @param stateObs estado del mundo
     * @param elapsedTimer para conocer cuanto tiempo hemos consumido. Permite
     * hacer consultas sobre el tiempo consumido o el tiempo que tenemos restante
     * */
    void choose_objective_as_closest_gem(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        // Posicion del jugador. Para hacer la comparacion con las posiciones de las gemas
        // Usamos gridposition para la distancia manhattan de forma sencilla
        Vector2d player_position = stateObs.getAvatarPosition();
        GridPosition player_grid_position = new GridPosition(player_position, stateObs);

        // Gemas actuales del mundo. Hacemos esto para no tener que hacer recalculos
        ArrayList<Observation> curr_gems = stateObs.getResourcesPositions()[0];

        // Iteramos sobre las gemas calculando las distancias
        ArrayList<Integer> manhattan_distances = new ArrayList<Integer>();
        for(Observation gem: curr_gems){
            GridPosition gem_grid_position = new GridPosition(gem.position, stateObs);
            Integer curr_distance = GridPosition.manhattan_distance(player_grid_position, gem_grid_position);
            manhattan_distances.add(curr_distance);
        }

        // Tomo el indice de la gema que esta mas cercana con la distancia manhattan
        int index_of_closest_gem = manhattan_distances.indexOf(Collections.min(manhattan_distances));

        // Establecemos la gema como objetivo actual
        this.current_objective = curr_gems.get(index_of_closest_gem).position;
    }

    /**
     * Algoritmo A* para devolver una lista de posiciones para llevar al jugador
     * al objetivo marcado.
     *
     * @param stateObs estado del mundo
     * @param elapsedTimer para conocer cuanto tiempo hemos consumido. Permite
     * hacer consultas sobre el tiempo consumido o el tiempo que tenemos restante
     * @return la lista ordenada de posiciones en el grid que representan el camino
     *
     * Se devuelven posiciones. Por tanto, el metodo que llame a este metodo es
     * responsable de convertir las posiciones en acciones
     *
     * Cuando no tenemos un objetivo, se llama al metodo que elige el siguiente
     * objetivo
     *
     * TODO -- comprobar los tiempos y parar cuando quede poco tiempo de computo
     * TODO -- este A* esta mal hecho, porque con otras planificaciones si que va
     * */
    ArrayList<GridPosition> a_star(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        ArrayList<GridPosition> path = new ArrayList<GridPosition>();
        path.add(new GridPosition(0, 0));
        return path;
    }

    // TODO -- Sergio -- no estoy usando esta funcion
    ///**
    // * Funcion auxiliar para encontrar el nodo que representa una determinada
    // * posicion.
    // *
    // * @param node_set conjunto de nodos en un conjunto iterable, representan
    // * nodos cerrados o nodos abiertos
    // * @param position posicion con la que hacemos las comprobaciones
    // * @pre debe comprobarse previamente que exista el nodo buscado. En otro caso
    // * se devuelve null
    // * @return el AStarNode cuya posicion es position
    // *
    // * TODO -- Sergio -- Usar otro tipo de estructura de datos distinta a HashSet para
    // * que esta busqueda sea mejor que O(n)
    // * */
    //AStarNode getNodeByGridPosition(Iterable<AStarNode> node_set, GridPosition position){
    //    for(AStarNode current_node : node_set){
    //        if(current_node.get_position().equals(position)){
    //            return current_node;
    //        }
    //    }

    //    // No se ha encontrado el nodo, se devuelve null
    //    // Esto no deberia pasar por las precondiciones
    //    return null;
    //}
}
