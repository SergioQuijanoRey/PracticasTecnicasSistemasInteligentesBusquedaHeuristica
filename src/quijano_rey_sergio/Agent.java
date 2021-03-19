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
     * Nivel actual en el que nos encontramos.
     * Lo conocemos viendo si hay gemas en el mapa y si hay enemigos en el mapa
     * Segun el nivel en el que nos encontremos actuamos de un modo u otro
     * */
    int current_level = -1;

    /**
     * Constructor del agente.
     * Tiene que recibir esos parametros de entrada porque asi se indica en [1]
     * @param so estado del mundo, dado como una observacion
     * @param elapsedTimer timer que nos va a indicar el tiempo de computo que tenemos
     *
     * Tiene un segundo de computo
     * */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer){
        // TODO -- Sergio -- borrar estos mensajes por pantalla
        System.out.println("Estamos construyendo la clase");

        // Mirando si hay gemas y enemigos, establecemos el nivel en el que
        // nos encontramos
        this.current_level = this.get_level(so, elapsedTimer);
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
                return this.leven1_act(stateObs, elapsedTimer);
            case 2:
            case 3:
            case 4:
            case 5:
                // TODO -- Sergio -- no mostrar mensajes por pantalla
                System.err.println("[Err] Todavia no hemos implementado este nivel");
                System.err.println("Devolvemos accion nula");
                return Types.ACTIONS.ACTION_NIL;
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
    public Types.ACTIONS leven1_act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
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
     * Con los datos del entorno, establece en que nivel nos encontramos.
     * Esto se hace viendo si hay no hay gemas en el mapa y viendo si hay o no
     * hay enemigos en el mapa
     *
     * @param stateObs estado del mundo, del que tomamos los datos de enemigos y
     * gemas
     * @param elapsedTimer para conocer cuanto tiempo hemos consumido. Permite
     * hacer consultas sobre el tiempo consumido o el tiempo que tenemos restante
     * */
    int get_level(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
        return 1;
    }

}
