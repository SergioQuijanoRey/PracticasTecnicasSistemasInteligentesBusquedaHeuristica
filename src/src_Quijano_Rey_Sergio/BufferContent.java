package src_Quijano_Rey_Sergio;

import src_Quijano_Rey_Sergio.*;

import java.util.PriorityQueue;
import java.util.ArrayList;

/**
 * Contiene los datos que queremos guardar a modo de "buffer" cuando tenemos que detener una busqueda
 * Guardamos el conjunto de abiertos y cerrados de la busqueda de A*
 * */
public class BufferContent{
    private PriorityQueue<AStarNode> open_buffer;
    private ArrayList<AStarNode> closed_buffer;

    BufferContent(PriorityQueue<AStarNode> open_buffer, ArrayList<AStarNode> closed_buffer){
        this.open_buffer = open_buffer;;
        this.closed_buffer = closed_buffer;
    }

    // Getters
    public PriorityQueue<AStarNode> getOpen(){
        return this.open_buffer;
    }
    public ArrayList<AStarNode> getClosed(){
        return this.closed_buffer;
    }

}
