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

// Tipos de datos auxiliares que he programado
import src_Quijano_Rey_Sergio.GridPosition;
import src_Quijano_Rey_Sergio.AStarNode;

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
    int current_level = -1;

    /**
     * Plan construido para ir al objetivo actual.
     * Si plan = null, no tenemos ningun plan construido y hay que generar uno
     *
     * TODO -- Sergio -- usar Vector2D en vez de acciones, y de alguna forma,
     * convertir una posicion en una accion
     * */
    ArrayList<Types.ACTIONS> plan = null;

    /**
     * Objetivo actual a perseguir.
     * Van a ser gemas y portales por los que escapar
     * Cuando current_objective = null, no tenemos objetivo y tendremos que decidir
     * cual queremos que sea nuestro siguiente objetivo
     * */
    Vector2d current_objective = null;

    /**
     * Numero de gemas que hay que conseguir en ciertos niveles para escapar por
     * el portal
     * */
    int number_of_gems_to_get = 9;

    /**
     * Constructor del agente.
     * Tiene que recibir esos parametros de entrada porque asi se indica en [1]
     * @param so estado del mundo, dado como una observacion
     * @param elapsedTimer timer que nos va a indicar el tiempo de computo que tenemos
     *
     * Tiene un segundo de computo
     * */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer){

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
        // No tenemos un plan construido, hay que generarlo
        if(this.plan == null || this.plan.size() == 0){
            this.generate_planning(stateObs, elapsedTimer);
        }

        // Tenemos un plan construido. Tomamos una accion del plan y la quitamos
        // del arraylist que representa el plan
        Types.ACTIONS next_action = this.plan.get(0);
        this.plan.remove(0);
        return next_action;

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
        Boolean gems_exist = gems.length > 0;

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
     * Calcula un plan con A* para ir de un punto a otro
     *
     * @param stateObs estado del mundo
     * @param elapsedTimer para conocer cuanto tiempo hemos consumido. Permite
     * hacer consultas sobre el tiempo consumido o el tiempo que tenemos restante
     *
     * Si no tenemos un objetivo, se decidie cual es nuestro objetivo para poder
     * hacer un plan que nos lleve desde la posicion del jugador hasta el objetivo
     *
     * TODO -- Sergio -- Comprobar que tenemos tiempo de sobra. Cuando nos estemos
     * quedando sin tiempo, parar la busqueda este la cosa como este
     * */
    void generate_planning(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        // TODO -- Sergio -- Borrar estos mensajes por pantalla
        System.out.println("Mostrando resultado de A*");
        //System.out.println(this.a_star(stateObs, elapsedTimer));

        // Si no tenemos objetivo, debemos decidir hacia donde dirigirnos
        if(this.current_objective == null){
            this.choose_objective(stateObs, elapsedTimer);
        }

        // Seteamos el plan a un arraylist vacio
        this.plan = new ArrayList<Types.ACTIONS>();

        // Estado temporal. Operamos con el para saber cuales son las consecuencias
        // de nuestras acciones al construir un plan
        // TODO -- Sergio -- No se si el copy hace falta. Si no hace falta, quitarlo
        // porque puede ser que estemos perdiendo tiempo
        StateObservation current_state = stateObs.copy();


        // Construimos aleatoriamente un plan
        // TODO -- Sergio -- Quitar esta cota y comprobar los tiempos
        int max_steps = 70;
        for(int i = 0; i < max_steps; i++){
            Types.ACTIONS action;

            // Posicion del jugador en el estado actual, despues de haber
            // tomado las acciones que hemos ido construyendo
            Vector2d player_position = current_state.getAvatarPosition();

            // Calculamos el vector entre el objetivo y el personaje
            Vector2d diff_vec = this.current_objective.copy();
            diff_vec.subtract(player_position);

            // Calculamos la accion segun el vector diferencia
            double x_diff = diff_vec.x;
            double y_diff = diff_vec.y;

            // No nos podemos mover en diagonal. Primero me muevo en horizontal,
            // despues en vertical
            if(x_diff < 0){
                action = Types.ACTIONS.ACTION_LEFT;

            }else if(x_diff > 0){
                action = Types.ACTIONS.ACTION_RIGHT;

            // Hemos terminado de movernos en horizontal, ahora nos movemos en vertical
            }else{
                if(y_diff < 0){
                    action = Types.ACTIONS.ACTION_UP;
                }else if(y_diff > 0){
                    action = Types.ACTIONS.ACTION_DOWN;

                // Posicion correcta de X e Y, hemos llegado al objetivo
                }else{
                    // Establecemos el objetivo a null para que en la siguiente
                    // iteracion se calcule un nuevo objetivo
                    this.current_objective = null;
                    break;
                }
            }


            // Añadimos la accion al plan
            this.plan.add(action);

            // Miramos como se queda el estado del mundo tras realizar la accion
            // elegida
            current_state.advance(action);
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
                System.out.println("Establecemos como objetivo el portal porque ya tenemos todas las gemas");
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
     *
     * @param stateObs estado del mundo
     * @param elapsedTimer para conocer cuanto tiempo hemos consumido. Permite
     * hacer consultas sobre el tiempo consumido o el tiempo que tenemos restante
     * */
    void choose_objective_as_closest_gem(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        // Posicion del jugador
        // La tomamos para poder hacer la llamada que nos devuelve las gemas
        // ordenados por distancia ascendente a la referencia que pasemos
        Vector2d player_position = stateObs.getAvatarPosition();

        // Tomamos las posiciones de las gemas
        ArrayList<Observation>[] gems = stateObs.getResourcesPositions(player_position);

        // Establezco la localizacion del portal mas cercano como objetivo
        // Se devuelve un array de arraylist, por eso tenemos que usar dos veces
        // el indice cero
        this.current_objective = gems[0].get(0).position;
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
     * */
    ArrayList<GridPosition> a_star(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        // TODO -- Sergio -- Anotaciones sobre el codigo
        // - Quiero hacer a_star sobre posiciones, no sobre estados, porque los estados
        // consumen mucha mas memoria
        // - Terminamos cuando un nodo que sacamos de abiertos es el objetivo
        // [ ] Necesito un mecanismo para pasar de posiciones a acciones. Puedo tomar
        // el estado actual stateObs para saber donde esta el muñeco, la posicion
        // a la que quiero ir, y con eso saber hacia donde necesito moverme
        // [ ] Trabajar con posiciones en pixeles porque asi va a ser mas facil
        // [ ] Abiertos: PriorityQueue que almacene una posicion y todo el camino que me ha
        // llevado hasta esa posicion. El PriorityQueue se ordena por la funcion
        // coste acumulado (1 por cada paso del camino) y la distancia manhattan
        // al objetivo

        // No tenemos objetivo, por lo que tenemos que decidir a donde nos queremos
        // dirigir
        if(this.current_objective == null){
            this.choose_objective(stateObs, elapsedTimer);
        }

        // Conjunto de posiciones abiertas
        // Es un conjunto ordenado segun la suma de coste acumulado de la posicion
        // y la distancia manhattan al objetivo
        // Cuando especificamos un comparador customizado, hay que especificar
        // tambien la capacidad del PriorityQueue
        // TODO -- Sergio -- calcular el tamaño del mundo en gridpositions y
        // tomar una capacidad en base a ello
        int initial_capacity = 100;
        PriorityQueue<AStarNode> open = new PriorityQueue<AStarNode>();

        // Conjunto de posiciones cerradas
        // Le heuristica que vamos a usar es admisible. Esto, junto a que todas
        // las acciones tienen el mismo coste (no hay tipos de suelo que modifiquen
        // los costes), hace que no tengamos que modificar el conjunto de cerrados
        // TODO -- Sergio -- Asegurarme de que esto que estoy diciendo es correcto
        HashSet<GridPosition> closed = new HashSet<GridPosition>();


        // Para arrancar, el nodo asociado a la posicion inicial (la posicion del jugador)
        // es añadido al conjunto de abiertos
        GridPosition start_position = new GridPosition(stateObs.getAvatarPosition(), stateObs);
        GridPosition objective_position = new GridPosition(this.current_objective, stateObs);
        open.add(new AStarNode(start_position, objective_position, new ArrayList<GridPosition>()));

        // Conjunto de posiciones inamovibles. Necesario para calcular los
        // nodos hijos validos y no repetir constantemente este calculo, pues
        // las posiciones inamovibles no se modifican
        ArrayList<Observation>[] inmovables_obs = stateObs.getImmovablePositions();
        HashSet<GridPosition> inmovable_grid_positions = new HashSet<GridPosition>();
        for(ArrayList<Observation> row : inmovables_obs){
            for(Observation obs : row){
                GridPosition current_inmovable_grid = new GridPosition(obs.position, stateObs);
                inmovable_grid_positions.add(current_inmovable_grid);
            }
        }

        // TODO -- Sergio -- Borrar este counter
        int counter = 0;
        while(open.isEmpty() == false && counter < 5000){
            counter = counter + 1;
            // Tomo el siguiente elemento de abiertos. Esto es, el elemento mas
            // prometedor segun la heuristica que ya hemos indicado. Esta operacion
            // hace que el nodo salga del conjunto de abiertos
            AStarNode current = open.poll();

            // El elemento pasa de abiertos a cerrados. Como trabajamos con posiciones
            // solo metemos la posicion, no toda la estructura de nodo
            closed.add(current.get_position());

            // Compruebo si la posicion actual es la posicion objetivo
            if(current.get_position().x == objective_position.x && current.get_position().y == objective_position.y){
                System.out.println("DEBUG: hemos encontrado el objetivo");
                System.out.println("Iteraciones consumidas: " + counter);
                // Hemos encontrado la solucion. Devuelvo el path al nodo actual
                // juntandole este nodo
                ArrayList<GridPosition> solution_path = current.get_path_to_position();
                System.out.println("Tamaño del path encontrado: " + solution_path.size());
                solution_path.add(current.get_position());
                return solution_path;
            }


            // Expando el nodo actual y paso los hijos del nodo al conjunto de abiertos
            // en caso de que la posicion que representa el nodo hijo no haya sido
            // ya explorada
            ArrayList<AStarNode> childs = current.generate_childs(stateObs, inmovable_grid_positions);
            for(AStarNode child: childs){
                boolean child_already_explored = closed.contains(child.get_position());
                if(child_already_explored == false){
                    open.add(child);
                }
            }
        }

        System.out.println("No hemos encontrado el objetivo, counter = " + counter);

        // La busqueda ha terminado porque hemos agotado los nodos de la lista de abiertos,
        // no porque hayamos encontrado el objetivo. Ha fracasado la busqueda, asi que
        // devolvemos un ArrayList vacio para representar este hecho
        return new ArrayList<GridPosition>();

    }
}
