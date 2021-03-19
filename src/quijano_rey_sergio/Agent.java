// El paquete que se nos pide que usemos
package quijano_rey_sergio;

/**
 * Referencias usadas para el desarrollo de la practica:
 *
 * [1] http://www.gvgai.net/cont.php:
 *  - Pequeño tutorial sobre como crear un agente
 * */

// TODO -- Sergio -- limpiar todo esto porque no se si sobran

// Tipos de datos que necesito para interactuar con el entorno GVGAI
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.Direction;
import tools.ElapsedCpuTimer;


// Para generar numeros aleatorios
import java.util.Random;

/**
 * Codigo del agente inteligente que vamos a desarrollar para resolver el juego
 * de Boulder Dash
 *
 * Tenemos que extender core.player.AbstractPlayer.java como se indica en [1]
 * @author Sergio Quijano Rey
 * */
public class Agent extends core.player.AbstractPlayer{
    /**
     * Constructor del agente.
     * Tiene que recibir esos parametros de entrada porque asi se indica en [1]
     * @param so estado del mundo, dado como una observacion
     * @param elapsedTimer timer que nos va a indicar el tiempo de computo que tenemos
     *
     * Tiene un segundo de computo
     * */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer){
        System.out.println("Estamos construyendo la clase");
    }

    /**
     * Metodo que se llama al inicio de cada nivel de un juego
     * Para realizar computos al inicio de un juego
     * @param sso observacion del juego al inicio de este
     * @param elapsedTimer timer que nos da 1s de computo
     * */
    // TODO -- Sergio -- Este codigo esta comentado, borrarlo
    //@Override
    @Override
    public void init(SerializableStateObservation sso, ElapsedCpuTimer elapsedTimer){
        System.out.println("Estoy haciendo preprocesados");
    }


    /**
     * Elige una accion conociendo el estado del mundo y el tiempo que ya esta consumido
     * @param stateObs estado del mundo
     * @param elapsedTimer para conocer cuanto tiempo hemos consumido. Permite
     * hacer consultas sobre el tiempo consumido o el tiempo que tenemos restante
     * @return la accion que debe realizar nuestro agente
     *
     * De momento estamos eligiendo una eleccion al azar entre las dos que tenemos
     * */
    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){

        //Direction move = Utils.processMovementActionKeys(Game.ki.getMask(), Types.DEFAULT_SINGLE_PLAYER_KEYIDX); //use primary set of keys, idx = 0
        //boolean useOn = Utils.processUseKey(Game.ki.getMask(), Types.DEFAULT_SINGLE_PLAYER_KEYIDX); //use primary set of keys, idx = 0

        //Types.ACTIONS action = Types.ACTIONS.fromVector(move);
        //if(action == Types.ACTIONS.ACTION_NIL && useOn){
        //    action = Types.ACTIONS.ACTION_USE;


        //}

        // Generamos una accion aleatoria y devolvemos una accion segun eso
        Random rand = new Random();
        int random_value = rand.nextInt(5);
        Types.ACTIONS action;

        if(random_value == 0){
            action = Types.ACTIONS.ACTION_UP;
        }else if(random_value == 1){
            action = Types.ACTIONS.ACTION_RIGHT;
        }else if(random_value == 2){
            action = Types.ACTIONS.ACTION_DOWN;
        }else{
            action = Types.ACTIONS.ACTION_LEFT;
        }

        return action;
    }

    /**
     * Metodo que se llama cuando una partida se ha acabado.
     * Para realizar computos a posteriori (teardown o procesar datos)
     *
     * @param sso el estado del juego al terminar
     * @param elapsedTimer timer con restriccion hasta llegar a CompetitionParameters.TOTAL_LEARNING_TIME
     * @return el siguiente nivel del juego actual para jugar
     * */
    @Override
    public int result(SerializableStateObservation sso, ElapsedCpuTimer elapsedTimer){
        System.out.println("Estamos viendo que hacer cuando ha acabado la partida");
        Random r = new Random();
        Integer level = r.nextInt(3);

        return level;
    }
}
