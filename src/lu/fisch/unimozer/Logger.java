/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package lu.fisch.unimozer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import static lu.fisch.unimozer.Ini.getDirname;

/**
 *
 * @author robertfisch
 */
public class Logger 
{
    private static Logger logger;
    
    private Logger() {}
    
    public static Logger getInstance()
    {
        if(logger==null) logger=new Logger();
        
        return logger;
    }
    
    public void log(Object message)
    {
        String dirname = getDirname(); 
        String filename = dirname+System.getProperty("file.separator")+"debug.log";
        try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)))) 
        {
            out.println(message);
        }
        catch (IOException e) 
        {
            //exception handling left as an exercise for the reader
        }        
    }
            
}
