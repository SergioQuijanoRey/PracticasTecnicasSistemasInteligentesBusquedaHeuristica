
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
import java.util.HashSet;
import java.awt.Dimension;
import java.util.Collections;   // Para iterar y tomar valores minimos

// Tipos de datos auxiliares que he programado
import src_Quijano_Rey_Sergio.GridPosition;
import src_Quijano_Rey_Sergio.AStarNode;
import src_Quijano_Rey_Sergio.AStarNodeComparator;
import src_Quijano_Rey_Sergio.Orientation;

/**
 * Codigo del agente inteligente que vamos a desarrollar para resolver el juego
 * de Boulder Dash
 *
 * Tenemos que extender core.player.AbstractPlayer.java como se indica en [1]
 * @author Sergio Quijano Rey
 * */
public class CachedAgent extends core.player.AbstractPlayer{

    /**
     * Nivel en el que nos encontramos.
     * Sabemos el nivel conociendo si hay o no gemas y si hay o no enemigos (y en
     * caso de haberlos, cuantos enemigos hay)
     *
     * Cuando current_level = -1, tenemos que calcular el nivel en el que estamos
     * */
    private int current_level = -1;

    /**
     * Plan construido para ir al objetivo actual.
     * Si plan = null, no tenemos ningun plan construido y hay que generar uno
     *
     * Trabajamos con posiciones en el mapa, que convertiremos a acciones para
     * pasar de una posicion a otra
     * */
    private ArrayList<GridPosition> plan = null;

    /**
     * Objetivo actual a perseguir.
     * Van a ser gemas y portales por los que escapar
     * Cuando current_objective = null, no tenemos objetivo y tendremos que decidir
     * cual queremos que sea nuestro siguiente objetivo
     * */
    private Vector2d current_objective = null;

    /**
     * Buffer de acciones a realizar. Esto se usa porque en ocasiones, para movernos
     * de una GridPosition a otra, necesitamos ejecutar mas de una accion (cambio
     * de sentido)
     * */
    private ArrayList<Types.ACTIONS> action_buffer = null;

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
     * Posiciones inamovibles (muros) del mapa.
     * Estas posiciones no se modifican a lo largo del juego asi que se calculan
     * una unica vez
     * */
    private HashSet<GridPosition> inmovable_grid_positions = null;

    /**
     * Constructor del agente.
     * Tiene que recibir esos parametros de entrada porque asi se indica en [1]
     * @param so estado del mundo, dado como una observacion
     * @param elapsedTimer timer que nos va a indicar el tiempo de computo que tenemos
     *
     * Tiene un segundo de computo
     * */
    public CachedAgent(StateObservation so, ElapsedCpuTimer elapsedTimer){

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


        // Conjunto de posiciones inamovibles. Necesario para calcular los
        // nodos hijos validos y no repetir constantemente este calculo, pues
        // las posiciones inamovibles no se modifican durante la partida
        ArrayList<Observation>[] inmovables_obs = so.getImmovablePositions();
        this.inmovable_grid_positions = new HashSet<GridPosition>();
        for(ArrayList<Observation> row : inmovables_obs){
            for(Observation obs : row){
                GridPosition current_inmovable_grid = new GridPosition(obs.position, so);
                this.inmovable_grid_positions.add(current_inmovable_grid);
            }
        }

        System.out.println("Posiciones inamovibles: " + this.inmovable_grid_positions);
        System.out.println("Tamaño del mapa: " + this.world_dimensions_grid);
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
                // TODO -- Sergio -- Quitar esto porque no es verdad
                return this.level1_act(stateObs, elapsedTimer);
            case 3:
            case 4:
                // TODO -- Sergio -- no mostrar mensajes por pantalla
                System.err.println("[Err] Todavia no hemos implementado este nivel");
                System.err.println("Devolvemos accion nula");
                return Types.ACTIONS.ACTION_NIL;
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
     *
     * */
    public Types.ACTIONS level1_act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        // Tenemos acciones en el buffer a realizar
        // Devolvemos la accion antes de cambiar la posicion del grid a la que nos movemos
        if(this.action_buffer != null && this.action_buffer.isEmpty() == false){
            return this.return_buffered_action();
        }

        // No tenemos un plan construido, hay que generarlo
        if(this.plan == null || this.plan.size() == 0){
            this.plan = this.a_star(stateObs, elapsedTimer);

            // Comprobamos que no hayamos generado un plan vacio. En tal caso,
            // devolvemos accion nula para que el programa no explote al hacer
            // un this.plan.get(0)
            if(this.plan == null || this.plan.size() == 0) {
                return Types.ACTIONS.ACTION_NIL;
            }
        }

        // Extraemos la siguiente posicion del mapa a la que nos tenemos que dirigir
        GridPosition next_position = this.plan.get(0);
        this.plan.remove(0);

        // Calculamos las acciones que tenemos que realizar para movernos
        this.calculate_actions_to_move(next_position, stateObs);

        // Devolvemos la accion a realizar
        return this.return_buffered_action();


    }

    /**
     * Devuelve la primera accion guardada en this.action_buffer y la elimina
     * de la lista
     * */
    Types.ACTIONS return_buffered_action(){
        if(this.action_buffer != null && this.action_buffer.size() > 0){
            Types.ACTIONS next = this.action_buffer.get(0);
            this.action_buffer.remove(0);
            return next;
        }else{
            // TODO -- Sergio -- Borrar estos mensajes por pantalla
            System.out.println("No hay accion en el buffer, se devuelve la accion nula");
            return Types.ACTIONS.ACTION_NIL;
        }
    }

    /**
     * Calcula las acciones (que guarda en this.action_buffer) para movernos a
     * la posicion dada como parametro
     * @param next_position posicion a la que nos queremos mover
     * @param stateObs estado del mundo, para saber donde se encuentra nuestro avatar
     *
     * TODO -- Sergio -- No pasar stateObs, sino la posicion anterior, porque es
     * mas ligero de ejecutar
     * */
    void calculate_actions_to_move(GridPosition next_position, StateObservation stateObs){
        // Si quedan acciones por ejecutar, no calculamos nada porque las acciones
        // del buffer deben ser consumidas previamente
        if(this.action_buffer != null && this.action_buffer.isEmpty() == false){
            return;
        }

        // Limpiamos el buffer de acciones, o lo instanciamos si antes era null
        this.action_buffer = new ArrayList<Types.ACTIONS>();

        // Orientacion del jugador. Nos indica si tenemos que realizar una o
        // dos acciones
        Vector2d current_orientation = stateObs.getAvatarOrientation();
        Orientation orientation = new Orientation(current_orientation);

        // Calculamos el desplazamiento del jugador
        Vector2d avatar_position = stateObs.getAvatarPosition();
        GridPosition avatar_position_at_grid = new GridPosition(avatar_position, stateObs);
        GridPosition movement = next_position.minus(avatar_position_at_grid);

        System.out.println("Estoy en " + avatar_position_at_grid + " y quiero ir a " + next_position);

        // Devolvemos la accion segun lo dado
        // Primero miramos desplazamientos verticales y despues desplazamientos
        // horizontales. Esto deberia dar igual pues en la planificacion no consideramos
        // movimientos diagonales en el grid
        // TODO -- Sergio -- Comprobar que esto este bien
        if(movement.getX() < 0){
            this.action_buffer.add(Types.ACTIONS.ACTION_LEFT);

            // Mala orientacion, tenemos que realizar dos acciones
            if(orientation.isLookingLeft() == false){
                this.action_buffer.add(Types.ACTIONS.ACTION_LEFT);
            }
        }else if(movement.getX() > 0){
            this.action_buffer.add(Types.ACTIONS.ACTION_RIGHT);

            // Mala orientacion, tenemos que realizar dos acciones
            if(orientation.isLookingRight() == false){
                this.action_buffer.add(Types.ACTIONS.ACTION_RIGHT);
            }
        }else if(movement.getY() > 0){
            this.action_buffer.add(Types.ACTIONS.ACTION_DOWN);

            // Mala orientacion, tenemos que realizar dos acciones
            if(orientation.isLookingDown() == false){
                this.action_buffer.add(Types.ACTIONS.ACTION_DOWN);
            }
        }else if(movement.getY() < 0){
            this.action_buffer.add(Types.ACTIONS.ACTION_UP);

            // Mala orientacion, tenemos que realizar dos acciones
            if(orientation.isLookingUp() == false){
                this.action_buffer.add(Types.ACTIONS.ACTION_UP);
            }
        }

        // No hay movimiento a realizar, devolvemos accion nula
        this.action_buffer.add(Types.ACTIONS.ACTION_NIL);
    }

    /**
     * Elige una accion segun la situacion en la que estemos en el nivel 5.
     *
     * En este caso, hacemos A* para calcular el camino hacia la salida y devolvemos
     * en cada paso la accion correspondiente
     *
     * Los calculos solo se hacen una vez. Cuando el plan ya esta construido (no
     * es vacio) simplemente devolvemos el siguiente elemento
     *
     *
     * */
    public Types.ACTIONS level5_act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        // TODO -- Sergio -- hay que implementar esto
        return this.level1_act(stateObs, elapsedTimer);
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
        System.out.println("==============> Calculando la gema mas cercana");
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

        // No tenemos objetivo, por lo que tenemos que decidir a donde nos queremos
        // dirigir
        if(this.current_objective == null){
            this.choose_objective(stateObs, elapsedTimer);
        }

        // Conjunto de posiciones abiertas
        // Es un conjunto ordenado segun la suma de coste acumulado de la posicion
        // y la distancia manhattan al objetivo
        PriorityQueue<AStarNode> open = new PriorityQueue<AStarNode>(new AStarNodeComparator());

        // Conjunto de posiciones cerradas
        ArrayList<AStarNode> closed = new ArrayList<AStarNode>();

        // Posiciones de partida y de llegada
        GridPosition start_position = new GridPosition(stateObs.getAvatarPosition(), stateObs);
        GridPosition objective_position = new GridPosition(this.current_objective, stateObs);

        // Orientacion de la que parte el avatar
        Orientation starting_orientation = new Orientation(stateObs.getAvatarOrientation());

        // Añadimos la posicion de partida al conjunto de abiertos
        open.add(new AStarNode(start_position, objective_position, new ArrayList<GridPosition>(), 0, starting_orientation));

        while(open.isEmpty() == false){
            // Tomo el siguiente elemento de abiertos. Esto es, el elemento mas
            // prometedor segun la heuristica que ya hemos indicado. Esta operacion
            // hace que el nodo salga del conjunto de abiertos
            AStarNode current = open.poll();

            // Compruebo si la posicion actual es la posicion objetivo, lo que pararia
            // la iteracion antes de realizar mas operaciones y devuelve la solucion
            if(current.get_position().equals(objective_position)){
                // Hemos encontrado la solucion. Devuelvo el path al nodo actual
                // juntandole este nodo
                ArrayList<GridPosition> solution_path = current.get_path_to_position();
                solution_path.add(current.get_position());

                // Ademas, borro el objetivo actual para que en la siguiente
                // busqueda se escoja el siguiente objetivo
                this.current_objective = null;

                System.out.println("A* acierta");

                return solution_path;
            }

            // Expando el nodo actual y paso los hijos del nodo al conjunto de abiertos
            // en caso de que la posicion que representa el nodo hijo no haya sido
            // ya explorada
            ArrayList<AStarNode> childs = current.generate_childs(stateObs, this.inmovable_grid_positions, this.world_dimensions_grid);
            for(AStarNode child: childs){
                // Hijo coincide con el padre, asi que no hacemos comprobaciones, no se considera
                // TODO -- Sergio -- Creo que como genero los hijos esto no puede pasar
                // asi que borrar este if
                if(child.get_position().equals(current.get_position())){
                    continue;
                }

                // Hemos encontrado un mejor camino a una posicion del mapa (posicion
                // del mapa ya explorada pero encontramos un camino de menor coste)
                // Asi que sacamos la posicion de cerrados y la metemos en abiertos,
                // esta vez con el mejor coste
                // Este contains comprueba que coincidan en las posiciones del mapa
                // que representan, como se puede ver en AStarNode.hashCode()
                if(closed.contains(child)){
                    // Tomo el nodo de cerrados cuya GridPosition coincide con la del nodo child
                    AStarNode node_already_in_closed = getNodeByGridPosition(closed, child.get_position());

                    if(child.get_path_cost() < node_already_in_closed.get_path_cost()){
                        closed.remove(node_already_in_closed);
                        open.add(child);
                        continue;
                    }
                }

                // Este nodo no ha sido visitado, asi que lo añadimos al conjunto de
                // abiertos. Recordar de nuevo que contains comprueba las posiciones
                // del mapa, no comprueba otros metadatos del nodo (como el coste acumulado,
                // la orientacion del padre...)
                if(closed.contains(child) == false && open.contains(child) == false){
                    open.add(child);
                    continue;
                }

                // La posicion estaba en abiertos (de nuevo, solo comprobamos posiciones)
                // Si este hijo tiene mejor coste a esa posicion, actualizamos el
                // elemento de abiertos para quedarnos con el mejor camino hasta esa posicion
                if(open.contains(child)){
                    System.out.println("==> El conjunto de abiertos ya contiene a este abierto");
                    AStarNode node_already_in_open = getNodeByGridPosition(open, child.get_position());
                    if(child.get_path_cost() < node_already_in_open.get_path_cost()){
                        // Actualizamos, child tiene mejor coste que el nodo que ya
                        // estaba en abiertos
                        open.remove(node_already_in_open);
                        open.add(child);
                        continue;
                    }
                }
            }

            // El elemento pasa de abiertos a cerrados. Como trabajamos con posiciones
            // solo metemos la posicion, no toda la estructura de nodo
            closed.add(current);
        }

        // La busqueda ha terminado porque hemos agotado los nodos de la lista de abiertos,
        // no porque hayamos encontrado el objetivo. Ha fracasado la busqueda, asi que
        // devolvemos un ArrayList vacio para representar este hecho
        System.out.println("A* falla");
        return new ArrayList<GridPosition>();
    }

    /**
     * Funcion auxiliar para encontrar el nodo que representa una determinada
     * posicion.
     *
     * @param node_set conjunto de nodos en un conjunto iterable, representan
     * nodos cerrados o nodos abiertos
     * @param position posicion con la que hacemos las comprobaciones
     * @pre debe comprobarse previamente que exista el nodo buscado. En otro caso
     * se devuelve null
     * @return el AStarNode cuya posicion es position
     *
     * TODO -- Sergio -- Usar otro tipo de estructura de datos distinta a HashSet para
     * que esta busqueda sea mejor que O(n)
     * */
    AStarNode getNodeByGridPosition(Iterable<AStarNode> node_set, GridPosition position){
        for(AStarNode current_node : node_set){
            if(current_node.get_position().equals(position)){
                return current_node;
            }
        }

        // No se ha encontrado el nodo, se devuelve null
        // Esto no deberia pasar por las precondiciones
        return null;
    }
}
