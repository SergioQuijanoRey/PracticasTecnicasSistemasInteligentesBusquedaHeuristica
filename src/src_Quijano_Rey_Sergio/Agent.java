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

// Tipos de datos de java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
    private HashSet<GridPosition> inmovable_grid_positions = null;

    /**
     * Acciones a realizar para ir de un punto a otro.
     * Usamos un stack porque es rapido de tomar elementos del final y eliminarlos y porque a la hora
     * de reconstruir un camino a partir del ultimo nodo de A* simplifica mucho las cosas (usando un
     * ArrayList tenia que invertir la lista o tomar elementos del principio y borrarlos en vez
     * de tomarlos del final)
     * */
    private Stack<Types.ACTIONS> plan = null;

    /**
     * Controla que A* haya encontrado la solucion en el tiempo correspondiente. En caso de que no de
     * tiempo, paramos de buscar, devolvemos ACTIONS.ACTION_NIL y seguimos iterando con los valores
     * anteriormente calculados
     * */
    private boolean a_star_was_succesfull = true;

    /**
     * Tiempo tope para dejar de buscar y guardar el estado
     * */
    private int millis_threshold = 1;

    /**
     * Si no tenemos tiempo, guardamos el conjunto de abiertos y cerrados para que en la siguiente
     * vez sigamos iterando
     * */
    private BufferContent buffer = null;


    /**
     * Radio de vision del jugador. Esto influye para tomar los elementos en una posicion que suman
     * en los valores del mapa del calor. Por ejemplo, si hay un enemigo mas alla del vision_radius,
     * no consideramos su presencia en el mapa de calor
     * */
    private int vision_radius = 15;

    /**
     * Radio de efecto de un muro. Esto influye a la hora de que un muro sume o no calor a una casilla
     * que este mas cerca o mas lejos que esta distancia
     * */
    private int wall_radius = 5;

    /**
     * Radio de accion del jugador. A la hora de buscar la zona a la que escapar, lejania de las
     * casillas a las que nos queremos mover
     * */
    private int action_radius = 4;

    /**
     * Radio de seguridad del jugador. Para el nivel compuesto deliberativo - reactivo. Determina
     * cuando tenemos que pasar a comportamiento reactivo al estar en peligro por la presencia de un
     * enemigo cercano
     * */
    private int safety_radius = 4;

    /**
     * Mapa de calor asociado a los muros, que precalculamos
     * */
    private HeatMap wall_heat_map = null;

    /**
     * Mapa de calor asociado a los enemigos. Tenemos que calcularlo en cada iteracion porque los
     * enemigos se mueven
     * */
    private HeatMap enemy_heat_map = null;

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
            // Cuando hay una excepcion porque no se ha hecho bien el calculo (como hemos programado),
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

        // Si tenemos enemigos, calculamos el mapa de calor
        if(this.current_level >= 3){
            this.precalculate_walls_heat_map();
        }

        // Aprovechamos para calcular el camino porque tenemos mucho tiempo de computo
        // En el caso en el que estemos en los dos primeros niveles
        // En el ultimo nivel, tambien tenemos que realizar A* para ir a por las gemas
        if(this.current_level == 1 || this.current_level == 2 || this.current_level == 5){
            this.choose_objective(so, elapsedTimer);
            this.a_star(so, elapsedTimer);
        }
    }

    /**
     * Precalcula el mapa de calor asociado a los muros.
     * Como los muros no son un componente dinamico, podemos calcular una unica vez el valor de
     * calor de cada celda asociado a los muros
     * */
    public void precalculate_walls_heat_map(){
        // Por si algun error intento calcular mas de una vez el mapa de calor
        if(this.wall_heat_map == null){
            this.wall_heat_map = new HeatMap(this.inmovable_grid_positions, this.wall_radius, this.world_dimensions_grid);
        }
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
        this.inmovable_grid_positions = new HashSet<GridPosition>();
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
                // El nivel no es valido, devolvemos una accion nula para que el programa java
                // no de un crash
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

        // Construimos un plan en caso de que no exista uno
        if(this.plan == null || this.plan.isEmpty() == true){
            this.plan = this.a_star(stateObs, elapsedTimer);
            // No hace falta hacer this.objective == null porque solo tenemos que alcanzar el portal,
            // el objetivo no cambia como en otros niveles
        }

        return this.plan.pop();
    }

    /**
     * Elige una accion en la situacion en la que estemos en el nivel 2.
     *
     * En este caso, hacemos A* a las gemas mas cercanas en cada momento, y al conseguir las 9 gemas
     * calculamos el camino a la salida
     *
     * Los calculos solo se hacen una vez. Cuando el plan ya esta construido (no
     * es vacio) simplemente devolvemos el siguiente elemento
     *
     * @param stateObs estado del mundo que nos aporta toda la informacion necesaria para la busqueda
     * @param elapsedTimer timer para saber cuanto tiempo de computo nos queda
     * */
    public Types.ACTIONS level2_act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        // Seleccionamos el objetivo
        if(this.current_objective == null){
            this.choose_objective(stateObs, elapsedTimer);
        }

        // Construimos un plan en caso de que no exista uno
        if(this.plan == null || this.plan.isEmpty() == true){

            // Modificamos el objetivo porque con el plan anterior ya lo vamos a alcanzar
            // Solo lo hacemos cuando A* ha tenido existo. Si no ha tenido exito, seguimos iterando
            // sin cambiar de objetivo
            if(this.a_star_was_succesfull == true){
                this.choose_objective(stateObs, elapsedTimer);
            }

            this.plan = this.a_star(stateObs, elapsedTimer);
        }

        return this.plan.pop();
    }

    /**
     * Elige una accion en la situacion en la que estemos en el nivel 3.
     *
     * En este caso, estamos en presencia de un enemigo del que tenemos que huir durante todos los
     * ticks que dura una partida de GVGAI
     *
     * @param stateObs estado del mundo que nos aporta toda la informacion necesaria sobre el mundo
     * y las posiciones de los enemigos
     * @param elapsedTimer timer para saber cuanto tiempo de computo nos queda. En estos niveles no
     * va a ser muy util porque el comportamiento reactivo es muy sencillo de ejecutar
     * */
    public Types.ACTIONS level3_act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        if(this.plan != null && this.plan.isEmpty() == false){
            return this.plan.pop();
        }

        // Calculamos el mapa de calor asociado a los enemigos
        this.calculate_enemy_heat_map(stateObs);

        // Posicion a la que nos queremos mover
        GridPosition lowest_heat_pos = this.calculate_lowest_heat_pos(stateObs);

        // Todas las posiciones tienen calor cero, no hacemos nada
        // O todas las posiciones tienen un calor mayor que la posicion actual, por lo que tampoco
        // hacemos nada en ese caso
        if(lowest_heat_pos == null){
            return Types.ACTIONS.ACTION_NIL;
        }

        // Hacemos A* para ir al punto de menor calor
        this.current_objective = lowest_heat_pos.toVector2d(this.scale_factor);
        this.plan = this.a_star(stateObs, elapsedTimer);
        this.plan.pop();

        return Types.ACTIONS.ACTION_UP;
    }

    /**
     * Calcula el punto del mapa con menor calor en el radio de accion respecto la posicion del
     * jugador. Para decidir a que posicion debe moverse el avatar
     * @param stateObs para consultar la posicion del jugador
     * */
    GridPosition calculate_lowest_heat_pos(StateObservation stateObs){
        GridPosition player_pos = this.getPlayerPosition(stateObs);
        GridPosition lowest_heat_pos = null;

        for(GridPosition curr_pos: player_pos.get_surroundings(this.action_radius, this.world_dimensions_grid)){
            // Tomamos el valor de menor calor
            if(lowest_heat_pos == null || this.heat(curr_pos) < this.heat(lowest_heat_pos)){
                lowest_heat_pos = curr_pos;
            }
        }

        // Lo mejor en esta situacion es movernos a la posicion de menor calor por unicamente
        // enemigos
        if(this.heat(player_pos) <= this.heat(lowest_heat_pos)){
            return null;
        }

        return lowest_heat_pos;
    }

    /**
     * Calcula el calor de una posicion dada. Combina el calor de los muros y el calor de los enemigos
     * @param pos la posicion cuyo calor calculamos
     * */
    double heat(GridPosition pos){
        double heat = 0.0;

        // Añadimos el calor de los muros
        heat += this.wall_heat_map.getHeat(pos);

        // Añadimos el calor de los enemigos
        heat += this.enemy_heat_map.getHeat(pos);

        return heat;

    }

    /**
     * Usamos el mismo comportamiento que cuando estamos en presencia de un unico enemigo. El nivel
     * 3 ya esta programado para considerar uno o dos enemigos, asi que no hay que hacer cambios para
     * que funcione con mas de un enemigo
     * */
    public Types.ACTIONS level4_act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        return this.level3_act(stateObs, elapsedTimer);
    }

    /**
     * Elige una accion en la situacion en la que estemos en el nivel 5.
     *
     * Tenemos que recoger nueve gemas y escapar por el portal, en presencia de enemigos
     * Cuando no hay enemigos cercanos, se aplica el comportamiento del nivel 2 (greedy para gemas,
     * A* para ir hasta las gemas). Cuando hay enemigos cercanos, empleamos el comportamiento del
     * nivel 3 (reactivo yendo hacia zonas de menor calor)
     *
     * @param stateObs estado del mundo que nos aporta toda la informacion necesaria sobre el mundo
     * y las posiciones de los enemigos
     * @param elapsedTimer timer para saber cuanto tiempo de computo nos queda. En este caso si va a
     * ser útil al estar lanzando A*, aunque lanzamos A* a una GridPosition de a lo sumo 4 casillas
     * de distancia
     * */
    public Types.ACTIONS level5_act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        // Comprobamos si estamos en peligro
        if(this.areWeInDanger(stateObs)){
            // Estamos escapando de un enemigo, asi que borramos el objetivo actual porque este puede
            // haber cambiado. Tambien borramos el plan, porque si no el reactivo usa lo que hayamos
            // guardado en el stack
            this.current_objective = null;
            this.plan = null;

            // Usamos el comportamiento reactivo para escapar
            return this.level3_act(stateObs, elapsedTimer);
        }

        // No estamos en peligro, asi que empleamos el comportamiento deliberativo
        // Notar que pueden quedar acciones en this.plan de la parte reactiva, por lo que se siguen
        // ejecutando. Cuando cambio el codigo para que esto no pase, el comportamiento obtenido es
        // peor, así que dejo este "problema" intencionalmente al aportar mejores resultados
        return this.level2_act(stateObs, elapsedTimer);
    }

    /**
     * Comprueba, para el nivel compuesto, si estamos en peligro. Cuando estamos en peligro, pasamos
     * de un comortamiento deliberativo a un comportamiento reactivo
     * @param stateObs estado del mundo que nos da la informacion sobre las posiciones de los enemigos
     * @return false, si estamos en una situacion segura
     *         true, si estamos en una situacion peligrosa
     * */
    public boolean areWeInDanger(StateObservation stateObs){
        // Tomamos las posiciones de los enemigos
        ArrayList<GridPosition> enemies_pos = this.getEnemiesPosition(stateObs);

        // Tomamos la posicion del jugador
        GridPosition player_pos = this.getPlayerPosition(stateObs);

        for(GridPosition enemy_pos: enemies_pos){
            if(GridPosition.manhattan_distance(player_pos, enemy_pos) <= this.safety_radius){
                // Estamos demasiado cerca de un enemigo, activamos el comportamiento reactivo
                return true;
            }
        }


        // No hay ningun enemigo cercano, estamos seguros
        return false;

    }

    /**
     * Calcula el mapa de calor asociado a los enemeigos presentes
     * */
    void calculate_enemy_heat_map(StateObservation stateObs){
        // Tomamos las posiciones en el grid de los enemigos
        ArrayList<GridPosition> enemies_pos = this.getEnemiesPosition(stateObs);

        // Calculamos el mapa de calor
        this.enemy_heat_map = new HeatMap(enemies_pos, this.vision_radius, this.world_dimensions_grid);
    }

    /**
     * Calculamos las posiciones de los enemigos en formato GridPosition
     * @param stateObs estado del mundo que nos proporciona la informacion necesaria
     * */
    ArrayList<GridPosition> getEnemiesPosition(StateObservation stateObs){
        ArrayList<GridPosition> enemies_pos = new ArrayList<GridPosition>();
        for(Observation enemy: stateObs.getNPCPositions()[0]){
            GridPosition enemi_grid_pos = new GridPosition(enemy.position, this.scale_factor);
            enemies_pos.add(enemi_grid_pos);
        }

        return enemies_pos;
    }

    /**
     * Calculamos la posicion del jugador
     * @param stateObs informacion del mundo que nos da la posicion del jugador
     * */
    GridPosition getPlayerPosition(StateObservation stateObs){
        Vector2d player_position = stateObs.getAvatarPosition();
        GridPosition player_grid_position = new GridPosition(player_position, this.scale_factor);
        return player_grid_position;
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
     * */
    void choose_objective(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        // En el primer nivel solo nos interesa ir al portal
        if(this.current_level == 1){
            this.choose_objective_as_closest_portal(stateObs, elapsedTimer);

        // En los niveles 2, 3 y 5, primero vamos a por las gemas y despues salimos por el portal
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
     * Establece la gema mas cercana al jugador como objetivo actual.
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
     * Puede tardar mas de una llamada de act en calcular. Cuando estamos a cierto tiempo de agotar
     * la iteracion, devolvemos ACTIONS.ACTION_NIL y guardamos el proceso de la busqueda hasta el
     * momento. Cuando A* tarda mas ticks de lo debido, estoy seguro de que este problema con los
     * tiempos es el causante.
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


        // En caso de que estemos continuando una busqueda previa que agoto su tiempo, cargamos los
        // datos del buffer
        if(this.a_star_was_succesfull == false){
            open = this.buffer.getOpen();
            closed = this.buffer.getClosed();
        }

        // Nodo actual de las iteraciones
        // Lo dejamos fuera del while para poder reconstruir el camino de acciones al acabar de
        // iterar en el bucle while
        AStarNode current = null;

        // Iteramos en la busqueda
        while(open.isEmpty() == false){
            // Tommamos el mejor nodo de abiertos y lo eliminamos del conjunto, que añadimos a cerrados
            current = open.poll();

            // Comprobamos que el nodo sea el nodo solucion
            if(current.isObjective() == true){
                break;
            }

            // Comprobacion de tiempos. Lo hago fuera del bucle de los hijos tambien porque el proceso
            // de generar los hijos puede considerarse muy lento
            if(this.can_we_continue(elapsedTimer) == false){
                this.a_star_was_succesfull = false;
                this.save_progress(open, closed);
                return construct_nil_path();
            }

            // Generamos los hijos e iteramos sobre ellos
            ArrayList<AStarNode> childs = current.generate_childs(this.world_dimensions_grid, this.inmovable_grid_positions);
            for(AStarNode child: childs){

                // Comprobacion de tiempos
                if(this.can_we_continue(elapsedTimer) == false){
                    this.a_star_was_succesfull = false;
                    this.save_progress(open, closed);
                    return construct_nil_path();
                }


                // Comprobamos que el hijo no sea lo mismo que el padre
                if(child.isSameAsParent() == true){
                    continue;
                }

                // IMPORTANTE -- La heuristica es admisible, por tanto, no es necesario mantener
                // la estructura de datos de cerrados. Una vez que un nodo entra en cerrados, al
                // ser admisible h, entra con el camino optimo hasta ese punto
                // IMPORTANTE -- Ademas, este mantenimiento es costoso, lo que hacia que necesitase
                // mas de una llamada a this.act para terminar A* (y teniendo un tick mas del coste
                // optimo). Con este cambio, esta situacion nunca ocurre
                // Aun asi, tenemos que comprobar que no tengamos el nodo en cerrado, y al terminar
                // añadirlo (lo que hacemos es que cuando metemos un nodo en cerrados, no lo volvemos
                // a sacar)

                // Si el nodo hijo ya ha sido explorado, y por tanto esta en cerrados
                //if(closed.contains(child)){
                //    // Tomamos el nodo que ya estaba en cerrados
                //    int already_explored_node_index = closed.indexOf(child);
                //    AStarNode already_explored_node = closed.get(already_explored_node_index);

                //    // Comprobamos cual de los dos nodos tiene mejor g
                //    // Si el nodo hijo tiene menor coste, quitamos el nodo de cerrados y lo metemos
                //    // en abiertos para que sea explorado mas tarde
                //    if(child.getCost() < already_explored_node.getCost()){
                //        closed.remove(already_explored_node_index);
                //        open.add(child);
                //    }
                //    continue;
                //}
                if(closed.contains(child)){
                    continue;
                }

                // Miramos si el hijo esta o no esta en abiertos
                AStarNode already_open_node = AuxiliarFunctions.getNodeByGridPositionAndOrientation(open, child.getPosition(), child.getOrientation());
                boolean already_open = already_open_node != null;

                // El hijo no ha sido explorado porque no esta en cerrados
                // Si el hijo no esta en abiertos, logicamente lo que tenemos que hacer es añadirlo
                // para que mas tarde sea explorado
                if(already_open == false){
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

            // Añadimos el nodo actual a cerrados porque ya ha sido explorado
            closed.add(current);
        }

        // Comprobamos si hemos llegado a la solucion
        // Si no hemos llegado, devolvemos la accion nula para que el programa no de un fallo
        if(current.isObjective() == false){
            return construct_nil_path();
        }

        // A* se ha ejecutado sin problemas
        this.notify_a_star_was_succesful();

        // Devolvemos el conjunto de acciones a partir del nodo solucion
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
     * Comprueba si tenemos suficiente tiempo para seguir operando
     * */
    boolean can_we_continue(ElapsedCpuTimer elapsedTimer){
        if(elapsedTimer.remainingTimeMillis() < this.millis_threshold){
            return false;
        }

        return true;

    }

    /**
     * Devuelve un path de acciones con solo una accion ACTIONS.ACTION_NIL
     * */
    Stack<Types.ACTIONS> construct_nil_path(){
        Stack<Types.ACTIONS> empty_path = new Stack<Types.ACTIONS>();
        empty_path.add(Types.ACTIONS.ACTION_NIL);
        return empty_path;
    }

    /**
     * Cuando paramos una busqueda, tenemos que guardar el progreso
     * */
    void save_progress(PriorityQueue<AStarNode> open, ArrayList<AStarNode> closed){
        this.buffer = new BufferContent(open, closed);
    }

    /**
     * Tomamos los contenidos que hemos guardado previamente en el buffer para seguir buscando
     * */
    BufferContent load_progress(){
        return this.buffer;
    }

    /**
     * Operaciones que tenemos que ejecutar cuando A* se ejecuta sin problemas en su franja de tiempo.
     * Ponemos el flag a true y borramos los buffers
     * */
    void notify_a_star_was_succesful(){
        // Flag para que sepamos que hemos encontrado solucion
        this.a_star_was_succesfull = true;

        // Borramos los contenidos del buffer
        this.buffer = null;
    }

}
