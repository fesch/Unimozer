/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package lu.fisch.unimozer.compilation;

/**
 *
 * @author robertfisch
 */
public class CompilationError
{
    private String className;
    private int line;
    private String message;

    public CompilationError(String className, int line, String message)
    {
        this.className=className;
        this.line=line;
        this.message=message;
    }

    @Override
    public String toString()
    {
        String message = getMessage();
        String remove = getClassName().replace('.','/')+":"+getLine()+": ";
        message = message.replace(remove,"");
        
        if(getClassName().equals("<NONE>"))
            return getMessage();
        else
            return getClassName()+" @ line "+getLine()+": "+message;
    }

    /**
     * @return the className
     */
    public String getClassName()
    {
        return className;
    }

    /**
     * @return the line
     */
    public int getLine()
    {
        return line;
    }

    /**
     * @return the message
     */
    public String getMessage()
    {
        return message;
    }

}
