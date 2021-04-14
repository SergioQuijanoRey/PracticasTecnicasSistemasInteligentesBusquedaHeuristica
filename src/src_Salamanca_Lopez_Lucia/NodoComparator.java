package src_Salamanca_Lopez_Lucia;

import java.util.Comparator;

/**
 *
 * @author Lucía Salamanca López
 */
public class NodoComparator implements Comparator<Nodo>{
    // Lo usaremos para ordenar los nodos por su funcion f
    @Override
    public int compare(Nodo n1, Nodo n2){
        if (n1.getF()<n2.getF())
            return -1;
        else if (n1.getF()>n2.getF())
            return 1;
        
        return 0;
    
    }
}
