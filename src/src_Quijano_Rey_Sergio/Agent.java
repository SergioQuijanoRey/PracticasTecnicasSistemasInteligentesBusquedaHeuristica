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
import java.util.Stack;
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
     * Acciones a realizar para ir de un punto a otro.
     * Usamos un stack porque es rapido de tomar elementos del final y eliminarlos y porque a la hora
     * de reconstruir un camino a partir del ultimo nodo de A* simplifica mucho las cosas (usando un
     * ArrayList tenia que invertir la lista o tomar elementos del principio y borrarlos en vez
     * de tomarlos del final)
     * */
    private Stack<Types.ACTIONS> plan = null;

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
        // Seleccionamos el objetivo
        if(this.current_objective == null){
            this.choose_objective(stateObs, elapsedTimer);
        }

        if(this.plan == null || this.plan.isEmpty() == true){
            this.plan = this.a_star(stateObs, elapsedTimer);
            // No hace falta hacer this.objective == null porque solo tenemos que alcanzar el portal,
            // el objetivo no cambia como en otros niveles
        }

        return this.plan.pop();
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
     * Algoritmo A* para devolver una lista de acciones que llevan al jugador al objetivo marcado
     *
     * @param stateObs estado del mundo
     * @param elapsedTimer para conocer cuanto tiempo hemos consumido. Permite
     * hacer consultas sobre el tiempo consumido o el tiempo que tenemos restante
     * @return la lista ordenada de acciones a ejecutar
     *
     * Cuando no tenemos un objetivo, se llama al metodo que elige el siguiente
     * objetivo
     *
     * Devolvemos un Stack porque asi es mas facil de extraer acciones de forma rapida en los otros
     * metodos. Se podria haber usado un ArrayList pero asi creo que va a funcionar mas rapido. Ademas
     * es mucho mas facil generar el camino sin tener que invertir un ArrayList (que llevaba mucho
     * mas tiempo)
     *
     * TODO -- comprobar los tiempos y parar cuando quede poco tiempo de computo
     * */
    Stack<Types.ACTIONS> a_star(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        // No tenemos objetivo, hay que elegir uno
        if(this.current_objective == null){
            this.choose_objective(stateObs, elapsedTimer);
        }

        // Usamos un priority queue para los nodos abiertos. Esto nos facilita tomar los nodos ordenados
        // por su funcion f = g + h  de forma sencilla y semi-automatica (mirar el codigo en
        // AStarNodeComparator)
        PriorityQueue<AStarNode> open = new PriorityQueue<AStarNode>(new AStarNodeComparator());

        // Añadimos el nodo inicial del que va a partir la busqueda
        // Para ello, tenemos que hacer unas cuantas consultas a GVGAI a traves de stateObs
        GridPosition starting_position = new GridPosition(stateObs.getAvatarPosition(), this.scale_factor);
        Orientation starting_orientation = new Orientation(stateObs.getAvatarOrientation());
        open.add(new AStarNode(starting_position, new GridPosition(this.current_objective, this.scale_factor), starting_orientation));

        // Conjunto de nodos cerrados
        // Uso un ArrayList porque he intentado usar otras estructuras de datos que me han dado
        // muchos problemas, y funciona en un tiempo razonable asi que no tengo la necesidad de cambiar
        // esta estructura
        ArrayList<AStarNode> closed = new ArrayList<AStarNode>();

        // TODO -- Sergio -- meter esto dentro del while para que sea mas idiomatico
        // TODO -- Sergio -- Hace que se parezca demasiado al codigo de lucia
        AStarNode current = null;

        // Iteramos en la busqueda
        while(open.isEmpty() == false){
            // Tommamos el mejor nodo de abiertos y lo eliminamos del conjunto
            current = open.poll();

            // Comprobamos que el nodo sea el nodo solucion
            if(current.isObjective() == true){
                System.out.println("HEMOS ENCONTRADO EL OBJETIVO");
                break;
            }

            // Generamos los hijos e iteramos sobre ellos
            for(AStarNode child: current.generate_childs(this.world_dimensions_grid, this.inmovable_grid_positions)){
                // Comprobamos que el hijo no sea lo mismo que el padre
                if(child.isSameAsParent() == true){
                    System.out.println("El hijo es lo mismo que el parent");;
                    continue;
                }

                // Si el nodo hijo ya ha sido explorado, y por tanto esta en cerrados
                if(closed.contains(child)){
                    // Tomamos el nodo que ya estaba en cerrados
                    int already_explored_node_index = closed.indexOf(child);
                    AStarNode already_explored_node = closed.get(already_explored_node_index);

                    // Comprobamos cual de los dos nodos tiene mejor g
                    // Si el nodo hijo tiene menor coste, quitamos el nodo de cerrados y lo metemos
                    // en abiertos para que sea explorado mas tarde
                    if(child.getCost() < already_explored_node.getCost()){
                        closed.remove(already_explored_node_index);
                        open.add(child);
                    }
                    continue;
                }

                // Miramos si el hijo esta o no esta en abiertos
                AStarNode already_open_node = getNodeByGridPositionAndOrientation(open, child.getPosition(), child.getOrientation());
                boolean already_open = already_open_node != null;

                System.out.println("Already opne vale " + already_open);

                // El hijo no ha sido explorado porque no esta en cerrados
                // Si el hijo no esta en abiertos, logicamente lo que tenemos que hacer es añadirlo
                // para que mas tarde sea explorado
                if(already_open == false){
                    System.out.println("Añadimos un nodo al conjunto de abiertos");
                    open.add(child);
                    continue;
                }

                // Hay otro nodo en abiertos con la misma orientacion y posicion
                // Tenemos que quedarnos con el nodo de mejor valor de coste
                if(already_open == true){
                    if(child.getCost() < already_open_node.getCost()){
                        // Cambiamos el que ya estaba por el nuevo nodo
                        open.remove(already_open_node);
                        open.add(child);
                    }
                }
            }

            System.out.println("Tenemos " + open.size() + " nodos abiertos");
        }

        // Comprobamos si hemos llegado a la solucion
        // Si no hemos llegado, devolvemos la accion nula para que el programa no de un fallo
        //if(current.isObjective() == false){
        //    System.out.println("El objetivo actual no es el objetivo buscado");
        //    System.out.println("La posicion final es " + current.getPosition());
        //    System.out.println("El objetivo era " + new GridPosition(this.current_objective, this.scale_factor));
        //    System.out.println("A* devuelve accion nula");
        //    System.out.println("");
        //    Stack<Types.ACTIONS> empty_path = new Stack<Types.ACTIONS>();
        //    empty_path.push(Types.ACTIONS.ACTION_NIL);
        //    return empty_path;
        //}

        // Devolvemos el conjunto de acciones a partir del nodo solucion
        System.out.println("A* ha encontrado un buen objetivo que estamos devolviendo");
        return reconstruct_path_to_actions(current);
    }

    /**
     * A partir de un nodo AStarNode solucion, reconstruye el camino que lleva desde la posicion
     * del avatar hasta la posicion objetivo
     * @param solution_node el nodo solucion
     * */
    Stack<Types.ACTIONS> reconstruct_path_to_actions(AStarNode solution_node){
        Stack<Types.ACTIONS> path = new Stack<Types.ACTIONS>();
        AStarNode curr = solution_node;
        while(curr.getParent() != null){

            // Añadimos una o dos acciones segun se necesite
            for(Types.ACTIONS action: curr.getActions()){
                path.push(action);
            }

            // Tomamos el siguiente elemento del camino
            curr = curr.getParent();
        }

        return path;
    }

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
     *
     * TODO -- Sergio -- mover a funciones auxiliares
     * */
    AStarNode getNodeByGridPositionAndOrientation(Iterable<AStarNode> node_set, GridPosition position, Orientation orientation){
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
