/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package lu.fisch.unimozer;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

/**
 *
 * @author robertfisch
 */
public class MyField
{
    protected Field field;
    protected String name;
    protected Object object;
    protected Diagram diagram;
    
    public final static String NULL = "<i>NULL</i>";

    public MyField(String name, Field field, Object object, Diagram diagram)
    {
        this.field=field;
        this.name=name;
        this.object=object;
        this.diagram=diagram;

        //if (field!=null)
        //    System.err.println("Field: "+field.getType().getSimpleName()+" - Object: "+object.getClass().getSimpleName());

    }

    @Override
    public String toString()
    {
        String type = new String();
        if (object!=null) type=object.getClass().getCanonicalName();
        if (field!=null) type=field.getType().getCanonicalName();

        if (!diagram.isUML())
            return "<html><font color=#808080>"+type+"</font> <font color=#000000>"+name+"</font></html>";
        else
            return "<html><font color=#000000>"+name+"</font> : <font color=#808080>"+type+"</font></html>";
    }

    public Class getType()
    {
        if (field!=null) return field.getType();
        if (object!=null) return object.getClass();
        return null;
    }

    public String getName()
    {
        return name;
    }

    public Object getObject()
    {
        if (field!=null)
        {
            field.setAccessible(true);
            if (object!=null)
            try
            {
                Object o = field.get(object);
                return o;
            }
            catch (Exception ex)
            {
            }
            return null;
        }
        return null;
    }

    public String getValue()
    {
        //System.err.println("Value of: "+name);
        if (field!=null)
        {
            field.setAccessible(true);
            String display = "<i>?</i>";
            if (object!=null)
            try
            {
                Object o = field.get(object);
                if(o!=null)
                    display = o.toString();
                else
                    display = NULL;
            }
            catch (Exception ex)
            {
            }
            return display;
        }
        /*else if (object.getClass().isArray())
        {
            return Array.get(object, Integer.valueOf(name)).toString();
        }*/
        else return "";
    }

    public String getArray(Integer index)
    {
        String display = "<i>?</i>";
        if (field!=null && object!=null)
        {
            try
            {
                // get the array
                Object o = field.get(object);
                // get the element of the array
                o = Array.get(o, Integer.valueOf(index));
                if(o!=null)
                    display = o.toString();
                else
                    display = NULL;
            }
            catch (Exception ex)
            {
                //ex.printStackTrace();
            }
        }
        return display;
    }

}
