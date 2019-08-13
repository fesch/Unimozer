
import bsh.EvalError;
import java.util.logging.Level;
import java.util.logging.Logger;
import lu.fisch.unimozer.Runtime5;

/**
 *
 * @author robert.fisch
 */
public class Unimozer {

    public static void monitor(Object object, String name)
    {
        lu.fisch.unimozer.Objectizer.getInstance().addObject(name, object);
    }
    
    public static void monitor(String name)
    {
        try {
            lu.fisch.unimozer.Objectizer.getInstance().addObject(name, Runtime5.getInstance().getObject(name));
        } catch (EvalError ex) {
            System.err.println(ex.getMessage());
        }
    }

    
    public static void refresh()
    {
        lu.fisch.unimozer.Objectizer.getInstance().repaint();
    }
    
}
