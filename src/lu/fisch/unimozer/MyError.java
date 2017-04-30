/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package lu.fisch.unimozer;

/**
 *
 * @author robertfisch
 */
public class MyError
{
    public static void display(Exception ex)
    {
        display(ex.getMessage());
    }

    public static void display(Throwable ex)
    {
        display(ex.getMessage());
    }

    public static void display(String message)
    {
        final String msg = message;
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                System.err.println("ERROR: "+msg);
            }
        };
        new Thread(runnable).start();
    }
}
