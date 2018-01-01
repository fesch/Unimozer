/*
    Unimozer
    Unimozer intends to be a universal modelizer for Java™. It allows the user
    to draw UML diagrams and generates the relative Java™ code automatically
    and vice-versa.

    Copyright (C) 2009  Bob Fisch

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or any
    later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package lu.fisch.unimozer;

import bsh.EvalError;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.Timer;
import lu.fisch.unimozer.console.Console;
import lu.fisch.unimozer.dialogs.MethodInputs;
import lu.fisch.unimozer.interactiveproject.MyInteractiveObject;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

/**
 * Class responsible for displaying the objects in the bottom left panel.
 * 
 * @author robertfisch
 */
public class Objectizer extends JPanel implements MouseListener, ActionListener, WindowListener
{
    /** holds references to the generated objects */
    private LinkedHashMap<String,MyObject> objects = new LinkedHashMap<String,MyObject>();
    /** popup which is diplayed on e right click */
    private JPopupMenu popup = new JPopupMenu();
    /** a reference to the selected object (with the mouse) */
    private MyObject selected = null;
    /** a back-reference to the diagram object */
    private Diagram diagram = null;
    /** a back-reference to the mainframe object */
    private Mainform frame = null;
    /** a reference to the calling label where a message is displayed during the execution of a method */
    private JLabel calling = null;
    /** holds the references to the different thread created */
    private Vector<Thread> threads = new Vector<Thread>();
    
    private static Objectizer self = null;

    private Timer refreshTimer = new Timer(200, new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e)
        {
            Objectizer.this.repaint();
        }
    });
    
    public void setAutoRefresh(int delay)
    {
        if(delay<=0)
        {
            if(refreshTimer.isRunning())
                    refreshTimer.stop();
        }
        else
        {    
            refreshTimer.setDelay(delay);
            if(!refreshTimer.isRunning())
               refreshTimer.start();
        }
    }
    
    /** create a new object */
    public Objectizer()
    {
        // call the super constructio (we inherit from JPanel!)
        super();
        // add this as mouselistener
        this.addMouseListener(this);
        // add another listener for the popup */
        this.addMouseListener(new PopupListener());
        // add the popup menu to this panel
        this.add(popup);
        // make the panel receive the focus
        this.setFocusable(true);
        
        self=this;
    }
    
    public static Objectizer getInstance()
    {
        return self;
    }

    /** 
     * Adds a new object to the Objectizer.
     * This can be any object. All objects inside the Objectizer are
     * being wrapped into a "MyObject" object.
     * @param objectName    the name to display for this object
     * @param object        the reference to the object
     * @return              the wrapped object
     */
    public MyObject addObject(String objectName, Object object)
    {
        // if it is a JFrame
        if(object instanceof JFrame) 
        {
            // set the name
            ((JFrame) object).setName(objectName);
            // make the window not to close everything
            if (((JFrame) object).getDefaultCloseOperation()==JFrame.EXIT_ON_CLOSE ||
                ((JFrame) object).getDefaultCloseOperation()==JFrame.DISPOSE_ON_CLOSE)
            {
                // add this class as listener
                ((JFrame) object).addWindowListener(this);
                ((JFrame) object).setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            }
        }
        // wrap the object
        MyObject myo = new MyObject(objectName,object,diagram);
        // put it into the list
        objects.put(objectName,myo);
        
        try 
        {
            Runtime5.getInstance().setObject(objectName, object);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        // repaint the Objectizer
        repaint();
        // return the wrapped object
        return myo;
    }
    
   public MyInteractiveObject addInteractiveObject(String objectName, Object object)
   {
       // if it is a JFrame
        if(object instanceof JFrame) 
        {
            // set the name
            ((JFrame) object).setName(objectName);
            // make the window not to close everything
            if (((JFrame) object).getDefaultCloseOperation()==JFrame.EXIT_ON_CLOSE ||
                ((JFrame) object).getDefaultCloseOperation()==JFrame.DISPOSE_ON_CLOSE)
            {
                // add this class as listener
                ((JFrame) object).addWindowListener(this);
                ((JFrame) object).setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            }
        }
        // wrap the object
        MyInteractiveObject myo = new MyInteractiveObject(objectName,object,diagram, diagram.getInteractiveProject().getInteractableClass().getFullName());
        // put it into the list
        objects.put(objectName,myo);
        
        try 
        {
            Runtime5.getInstance().setObject(objectName, object);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        // repaint the Objectizer
        repaint();
        // return the wrapped object
        return myo;
   }
   public void removeObject(String name)
    {
        MyObject mo = objects.get(name);
        
        for(int i=0;i<mo.children.size();i++)
        {
            removeObject(mo.children.get(i).getName());
        }
        
        objects.remove(name);        
    }

    /**
     * Test is the Objectizer holds an object with a given name
     * @param name  the name of the object to search for
     * @return      true if the object is found, false otherwise
     */
    public boolean hasObject(String name)
    {
        return objects.containsKey(name);
    }

    /**
     * Tests if the console (output) is active
     * @return      true if the console is active, false otherwise
     */
    public boolean isConsoleActive()
    {
        // disconnect the standard error ouput in order to
        // suppress errors eventually generate by the following code
        Console.disconnectErr();

        /* no longer needed
        // stop threads
        while(!threads.isEmpty())
        {
            try
            {
                Thread t = threads.get(0);
                threads.remove(0);
                if (t.isAlive())
                {
                    System.err.println("Thread is alive: "+t.getName());
                    t.interrupt();
                }

            }
            catch (Error e)
            {
                //e.printStackTrace();
            }
        }
        */

        // we suppose the console is active (worst case)
        boolean result = true;
        // if there is an instance of the console running
        if(Console.getInstance()!=null)
        {
            // prepare it
            Console.getInstance().prepare();
            // get the result
            result=!Console.getInstance().isActive();
        }
        // reconnect the standard error
        Console.connectErr();
        // return the result
        return result;
    }

    /** Stops all registered running threads. */
    public void stopAllThreads()
    {
        // disconnect the standard error output
        Console.disconnectErr();
        // stop threads
        while(!threads.isEmpty())
        {
            try
            {
                // get a thrad
                Thread t = threads.get(0);
                // remove if from the list
                threads.remove(0);
                // if it is alive
                if (t.isAlive())
                {
                    // interrupt it
                    t.interrupt();
                    // stop it
                    t.stop();
                }
            }
            catch (Error e) {} // ignore any error
        }

        // put the console in an inactive state
        Console.getInstance().activate(false);
        // reconnect the standard error output
        Console.connectErr();
    }

    public void removeAllObjects()
    {
        if(diagram.getInteractiveProject()!=null)
            diagram.getInteractiveProject().clean();
        
        // stop threads
        //stopAllThreadsAndPRepareConsole();
        // remove objects
        Object [] keys = objects.keySet().toArray();
        for(int i=keys.length-1;i>=0;i--)
        {
            String key = (String) keys[i];
            MyObject myObj = objects.get(key);
            Object obj = myObj.getObject();
            String className = obj.getClass().getSimpleName();
            MyObject o = objects.get(key);
            o.cleanUp();
            objects.remove(key);
            o=null;
        }
        System.gc();
        System.gc();
        repaint();
    }

/*
 public void removeByClassName(MyClass mc)
    {
        String name = mc.getShortName();
        Object [] keys = objects.keySet().toArray();
        for(int i=keys.length-1;i>=0;i--)
        {
            String key = (String) keys[i];
            MyObject myObj = objects.get(key);
            Object obj = myObj.getObject();
            String className = obj.getClass().getSimpleName();
            if(className.equals(name)) 
            {
                MyObject o = objects.get(key);
                o.cleanUp();
                objects.remove(key);
                o=null;
            }
        }
        System.gc();
        System.gc();
        repaint();
    }
*/
    
    @Override
    public void paint(Graphics graphics)
    {
        super.paint(graphics);

        Graphics2D g = (Graphics2D) graphics;
        // set anti-aliasing rendering
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        // clear background
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0,0,getWidth(),getHeight());
        g.setColor(Color.BLACK);

        // On windos 7 64bit there is an error produced by these lines:
        // java.security.AccessControlException: access denied
        // >>>>      at lu.fisch.unimozer.MyObject.paint(MyObject.java:195)
        // >>>>      at lu.fisch.unimozer.Objectizer.paint(Objectizer.java:236)
        try
        {
            int left = 8;
            for(int o=0;o<objects.keySet().size();o++)
            //for(String objectName : objects.keySet())
            {
                String objectName = (String) objects.keySet().toArray()[o];
                MyObject myObj = objects.get(objectName);
                left+=myObj.paint(g, left, 8, diagram.isUML())+8;
            }
        }
        catch (Error e)
        {
            // ignore
        }
        /*Color color = new Color(255,100,100);

        Set<String> set = objects.keySet();
        Iterator<String> itr = set.iterator();
        while (itr.hasNext())
        {
          String objectName = itr.next();
          MyObject myObj = objects.get(objectName);
          Object obj = myObj.getObject();
          String className = obj.getClass().getSimpleName();

          int wo = (int) g.getFont().getStringBounds(objectName, g.getFontRenderContext()).getWidth();
          g.setFont(new Font(g.getFont().getName(),Font.BOLD,g.getFont().getSize()));
          int wc = (int) g.getFont().getStringBounds(className, g.getFontRenderContext()).getWidth();
          g.setFont(new Font(g.getFont().getName(),Font.PLAIN,g.getFont().getSize()));
          int width = Math.max(wo,wc)+2*8;

          myObj.setPosition(new Point(left,8));
          myObj.setWidth(width);
          myObj.setHeight(this.getHeight()-2*8);

          g.setColor(color);
          g.fillRoundRect(left, 8, width, this.getHeight()-2*8, 8,8);
          g.setColor(Color.BLACK);
          g.drawRoundRect(left, 8, width, this.getHeight()-2*8, 8,8);

          g.setColor(Color.BLACK);
          g.setFont(new Font(g.getFont().getName(),Font.BOLD,g.getFont().getSize()));
          g.drawString(className, left+(width-wc)/2, 4*8);
          g.setFont(new Font(g.getFont().getName(),Font.PLAIN,g.getFont().getSize()));
          g.drawString(objectName, left+(width-wo)/2, 6*8);

          left+=width+8;
        }*/

    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
    }

    // Taken from OpenJDF7
    // ParameterizedTypeImpl.java
    private String toString(ParameterizedTypeImpl t)
    {
        StringBuilder sb = new StringBuilder();

        if (t.getOwnerType() != null)
        {
            sb.append(t.getRawType().getSimpleName().replace(((ParameterizedTypeImpl) t.getOwnerType()).getRawType().getName() + "$",
                        ""));
        } else
        {
            sb.append(t.getRawType().getSimpleName());
        }

        if (t.getActualTypeArguments() != null
                && t.getActualTypeArguments().length > 0)
        {
            sb.append("<");
            boolean first = true;
            for (Type tt : t.getActualTypeArguments())
            {
                if (!first)
                {
                    sb.append(", ");
                }
                if (tt instanceof Class)
                {
                    sb.append(((Class) tt).getName().replace("java.lang.",""));
                } else
                {
                    sb.append(tt.toString().replace("java.lang.",""));
                }
                first = false;
            }
            sb.append(">");
        }

        return sb.toString();
    }

    // Taken from OpenJDF7
    // ParameterizedTypeImpl.java
    private String toStringReplaced(ParameterizedTypeImpl t, MyObject myObj)
    {
        StringBuilder sb = new StringBuilder();

        if (t.getOwnerType() != null)
        {
            sb.append(t.getRawType().getSimpleName().replace(((ParameterizedTypeImpl) t.getOwnerType()).getRawType().getName() + "$",
                        ""));
        } else
        {
            sb.append(t.getRawType().getSimpleName());
        }

        if (t.getActualTypeArguments() != null
                && t.getActualTypeArguments().length > 0)
        {
            sb.append("<");
            boolean first = true;
            for (Type tt : t.getActualTypeArguments())
            {
                if (!first)
                {
                    sb.append(", ");
                }
                if (tt instanceof Class)
                {
                    if(myObj!=null)
                    {
                        if(myObj.generics.containsKey(((Class) tt).getName()))
                            sb.append(myObj.generics.get(((Class) tt).getName()));
                        else    
                         sb.append(((Class) tt).getName().replace("java.lang.",""));
                    }
                    else    
                     sb.append(((Class) tt).getName().replace("java.lang.",""));
                } else
                {
                    if(myObj!=null)
                    {
                        if(myObj.generics.containsKey(tt.toString()))
                            sb.append(myObj.generics.get(tt.toString()));
                        else    
                            sb.append(tt.toString().replace("java.lang.",""));
                    }
                    else    
                        sb.append(tt.toString().replace("java.lang.",""));
                }
                first = false;
            }
            sb.append(">");
        }

        return sb.toString();
    }

    // Taken from OpenJDF7
    // Class "Field"
    private String getTypeName(Class<?> type)
    {
        if (type.isArray())
        {
            try
            {
                Class<?> cl = type;
                int dimensions = 0;
                while (cl.isArray())
                {
                    dimensions++;
                    cl = cl.getComponentType();
                }
                StringBuffer sb = new StringBuffer();
                sb.append(cl.getSimpleName());
                for (int i = 0; i < dimensions; i++)
                {
                    sb.append("[]");
                }
                return sb.toString();
            } catch (Throwable e)
            { /*FALLTHRU*/ }
        }
        return type.getSimpleName();
    }

    // Taken from OpenJDF7
    // Class "Method"
    public boolean isVarArgs(Method m)
    {
        return (m.getModifiers() & 0x00000080) != 0;
    }

    // Taken from OpenJDF7
    // Class "Method"
    private String toGenericString(Method m)
    {
        final int METHOD_MODIFIERS =
                Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE
                | Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL
                | Modifier.SYNCHRONIZED | Modifier.NATIVE | Modifier.STRICT;
        try
        {
            StringBuilder sb = new StringBuilder();
            int mod = m.getModifiers() & METHOD_MODIFIERS;
            if (mod != 0)
            {
                sb.append(Modifier.toString(mod)).append(' ');
            }
            TypeVariable<?>[] typeparms = m.getTypeParameters();
            if (typeparms.length > 0)
            {
                boolean first = true;
                sb.append('<');
                for (TypeVariable<?> typeparm : typeparms)
                {
                    if (!first)
                    {
                        sb.append(',');
                    }
                    // Class objects can't occur here; no need to test
                    // and call Class.getName().
                    sb.append(typeparm.toString().replace("java.lang.",""));
                    first = false;
                }
                sb.append("> ");
            }
            Type genRetType = m.getGenericReturnType();
            sb.append(((genRetType instanceof Class<?>)
                    ? getTypeName((Class<?>) genRetType) : genRetType.toString())).append(' ');

            sb.append(getTypeName(m.getDeclaringClass())).append('.');
            sb.append(m.getName()).append('(');
            Type[] params = m.getGenericParameterTypes();
            for (int j = 0; j < params.length; j++)
            {
                String param = (params[j] instanceof Class)
                        ? getTypeName((Class) params[j])
                        : (params[j].toString());
                if (isVarArgs(m) && (j == params.length - 1)) // replace T[] with T...
                {
                    param = param.replaceFirst("\\[\\]$", "...");
                }
                sb.append(param);
                if (j < (params.length - 1))
                {
                    sb.append(',');
                }
            }
            sb.append(')');
            Type[] exceptions = m.getGenericExceptionTypes();
            if (exceptions.length > 0)
            {
                sb.append(" throws ");
                for (int k = 0; k < exceptions.length; k++)
                {
                    sb.append((exceptions[k] instanceof Class)
                            ? ((Class) exceptions[k]).getName()
                            : exceptions[k].toString());
                    if (k < (exceptions.length - 1))
                    {
                        sb.append(',');
                    }
                }
            }
            return sb.toString();
        } catch (Exception e)
        {
            return "<" + e + ">";
        }
    }


    // Taken from OpenJDF7
    // Class "Method"
    public String toFullString(Method m)
    {
        final int METHOD_MODIFIERS =
                Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE
                | Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL
                | Modifier.SYNCHRONIZED | Modifier.NATIVE | Modifier.STRICT;
        try
        {
            StringBuilder sb = new StringBuilder();
            //int mod = m.getModifiers() & METHOD_MODIFIERS;
            //if (mod != 0)
            //{
            //    sb.append(Modifier.toString(mod)).append(' ');
            //}
            TypeVariable<?>[] typeparms = m.getTypeParameters();
            if (typeparms.length > 0)
            {
                boolean first = true;
                sb.append('<');
                for (TypeVariable<?> typeparm : typeparms)
                {
                    if (!first)
                    {
                        sb.append(',');
                    }
                    // Class objects can't occur here; no need to test
                    // and call Class.getName().
                    sb.append(typeparm.toString().replace("java.lang.",""));
                    first = false;
                }
                sb.append("> ");
            }

            Type genRetType = m.getGenericReturnType();
            sb.append(((genRetType instanceof Class<?>)
                    ? getTypeName((Class<?>) genRetType) : 
                        genRetType instanceof ParameterizedTypeImpl ?
                        toString((ParameterizedTypeImpl) genRetType) :
                        genRetType.toString())).append(' ');

            //sb.append(getTypeName(m.getDeclaringClass())).append('.');
            sb.append(m.getName()).append('(');
            Type[] params = m.getGenericParameterTypes();
            for (int j = 0; j < params.length; j++)
            {
                String param = (params[j] instanceof Class)
                        ? getTypeName((Class) params[j]) :
                            (params[j].toString());
                // override
                if (params[j] instanceof ParameterizedTypeImpl)
                {
                    param = toString((ParameterizedTypeImpl) params[j]);
                }
                
                if (isVarArgs(m) && (j == params.length - 1)) // replace T[] with T...
                {
                    param = param.replaceFirst("\\[\\]$", "...");
                }

                sb.append(param);
                if (j < (params.length - 1))
                {
                    sb.append(',');
                }
            }
            sb.append(')');
            /*Type[] exceptions = m.getGenericExceptionTypes();
            if (exceptions.length > 0)
            {
                sb.append(" throws ");
                for (int k = 0; k < exceptions.length; k++)
                {
                    sb.append((exceptions[k] instanceof Class)
                            ? ((Class) exceptions[k]).getName()
                            : exceptions[k].toString());
                    if (k < (exceptions.length - 1))
                    {
                        sb.append(',');
                    }
                }
            }*/
            return sb.toString();
        } 
        catch (Exception e)
        {
            return "<" + e + ">";
        }
    }

        // Taken from OpenJDF7
    // Class "Method"
    public String constToFullString(Constructor m)
    {
        final int METHOD_MODIFIERS =
                Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE
                | Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL
                | Modifier.SYNCHRONIZED | Modifier.NATIVE | Modifier.STRICT;
        try
        {
            StringBuilder sb = new StringBuilder();
            //int mod = m.getModifiers() & METHOD_MODIFIERS;
            //if (mod != 0)
            //{
            //    sb.append(Modifier.toString(mod)).append(' ');
            //}
            TypeVariable<?>[] typeparms = m.getTypeParameters();
            if (typeparms.length > 0)
            {
                boolean first = true;
                sb.append('<');
                for (TypeVariable<?> typeparm : typeparms)
                {
                    if (!first)
                    {
                        sb.append(',');
                    }
                    // Class objects can't occur here; no need to test
                    // and call Class.getName().
                    sb.append(typeparm.toString().replace("java.lang.",""));
                    first = false;
                }
                sb.append("> ");
            }

            sb.append(' ');
            //sb.append(getTypeName(m.getDeclaringClass())).append('.');
            //sb.append(m.getName()).append('(');
            sb.append(m.getDeclaringClass().getSimpleName()).append('(');
            Type[] params = m.getGenericParameterTypes();
            for (int j = 0; j < params.length; j++)
            {
                String param = (params[j] instanceof Class)
                        ? getTypeName((Class) params[j]) :
                            (params[j].toString());
                // override
                if (params[j] instanceof ParameterizedTypeImpl)
                {
                    param = toString((ParameterizedTypeImpl) params[j]);
                }
                
                sb.append(param);
                if (j < (params.length - 1))
                {
                    sb.append(',');
                }
            }
            sb.append(')');
            /*Type[] exceptions = m.getGenericExceptionTypes();
            if (exceptions.length > 0)
            {
                sb.append(" throws ");
                for (int k = 0; k < exceptions.length; k++)
                {
                    sb.append((exceptions[k] instanceof Class)
                            ? ((Class) exceptions[k]).getName()
                            : exceptions[k].toString());
                    if (k < (exceptions.length - 1))
                    {
                        sb.append(',');
                    }
                }
            }*/
            return sb.toString().trim();
        } 
        catch (Exception e)
        {
            return "<" + e + ">";
        }
    }

    // Taken from OpenJDF7
    // Class "Method"
    private String toFullStringReplaced(Method m, MyObject myObj)
    {
        final int METHOD_MODIFIERS =
                Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE
                | Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL
                | Modifier.SYNCHRONIZED | Modifier.NATIVE | Modifier.STRICT;
        try
        {
            StringBuilder sb = new StringBuilder();
            //int mod = m.getModifiers() & METHOD_MODIFIERS;
            //if (mod != 0)
            //{
            //    sb.append(Modifier.toString(mod)).append(' ');
            //}
            TypeVariable<?>[] typeparms = m.getTypeParameters();
            if (typeparms.length > 0)
            {
                boolean first = true;
                sb.append('<');
                for (TypeVariable<?> typeparm : typeparms)
                {
                    if (!first)
                    {
                        sb.append(',');
                    }
                    // Class objects can't occur here; no need to test
                    // and call Class.getName().
                    if(myObj!=null)
                        if(myObj.generics.containsKey(typeparm.toString().replace("java.lang.","")))
                            sb.append(myObj.generics.get(typeparm.toString().replace("java.lang.","")));
                        else
                            sb.append(typeparm.toString().replace("java.lang.",""));
                    else
                        sb.append(typeparm.toString().replace("java.lang.",""));
                    first = false;
                }
                sb.append("> ");
            }

            Type genRetType = m.getGenericReturnType();
            String retP = (((genRetType instanceof Class<?>)
                    ? getTypeName((Class<?>) genRetType) : 
                        genRetType instanceof ParameterizedTypeImpl ?
                        toStringReplaced((ParameterizedTypeImpl) genRetType,myObj) :
                        genRetType.toString()));
            if(myObj!=null)
                if(myObj.generics.containsKey(retP))
                    sb.append(myObj.generics.get(retP));
                else
                    sb.append(retP);
            else
                sb.append(retP);
            
            sb.append(' ');
            
            //sb.append(getTypeName(m.getDeclaringClass())).append('.');
            sb.append(m.getName()).append('(');
            Type[] params = m.getGenericParameterTypes();
            for (int j = 0; j < params.length; j++)
            {
                String param = (params[j] instanceof Class)
                        ? getTypeName((Class) params[j]) :
                            (params[j].toString());
                // override
                if (params[j] instanceof ParameterizedTypeImpl)
                {
                    param = toStringReplaced((ParameterizedTypeImpl) params[j],myObj);
                }
                
                if (isVarArgs(m) && (j == params.length - 1)) // replace T[] with T...
                {
                    param = param.replaceFirst("\\[\\]$", "...");
                }

                if(myObj!=null)
                    if(myObj.generics.containsKey(param))
                        sb.append(myObj.generics.get(param));
                    else
                        sb.append(param);
                else
                    sb.append(param);

                    
                if (j < (params.length - 1))
                {
                    sb.append(',');
                }
            }
            sb.append(')');
            /*Type[] exceptions = m.getGenericExceptionTypes();
            if (exceptions.length > 0)
            {
                sb.append(" throws ");
                for (int k = 0; k < exceptions.length; k++)
                {
                    sb.append((exceptions[k] instanceof Class)
                            ? ((Class) exceptions[k]).getName()
                            : exceptions[k].toString());
                    if (k < (exceptions.length - 1))
                    {
                        sb.append(',');
                    }
                }
            }*/
            return sb.toString();
        } 
        catch (Exception e)
        {
            return "<" + e + ">";
        }
    }

    public LinkedHashMap<String,String> getInputsReplaced(Method m, MyObject myObj)
    {
        LinkedHashMap<String,String> ret = new LinkedHashMap<String,String>();
        try
        {
            Type[] params = m.getGenericParameterTypes();
            for (int j = 0; j < params.length; j++)
            {
                String param = (params[j] instanceof Class)
                        ? getTypeName((Class) params[j]) :
                            (params[j].toString());
                // override
                if (params[j] instanceof ParameterizedTypeImpl)
                {
                    param = toStringReplaced((ParameterizedTypeImpl) params[j],myObj);
                }
                
                if (isVarArgs(m) && (j == params.length - 1)) // replace T[] with T...
                {
                    param = param.replaceFirst("\\[\\]$", "...");
                }
                
                ret.put("param"+j,param);
            }
        } 
        catch (Exception e)
        {
        }
        return ret;
    }

    // Taken from OpenJDF7
    // Class "Method"
    private String toFullStringReplacedNoReturn(Method m, MyObject myObj)
    {
        final int METHOD_MODIFIERS =
                Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE
                | Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL
                | Modifier.SYNCHRONIZED | Modifier.NATIVE | Modifier.STRICT;
        try
        {
            StringBuilder sb = new StringBuilder();
            //int mod = m.getModifiers() & METHOD_MODIFIERS;
            //if (mod != 0)
            //{
            //    sb.append(Modifier.toString(mod)).append(' ');
            //}
            /*TypeVariable<?>[] typeparms = m.getTypeParameters();
            if (typeparms.length > 0)
            {
                boolean first = true;
                sb.append('<');
                for (TypeVariable<?> typeparm : typeparms)
                {
                    if (!first)
                    {
                        sb.append(',');
                    }
                    // Class objects can't occur here; no need to test
                    // and call Class.getName().
                    if(myObj.generics.contains(typeparm.toString()))
                        sb.append(myObj.generics.get(typeparm.toString()));
                    else
                        sb.append(typeparm.toString());
                    first = false;
                }
                sb.append("> ");
            }

            Type genRetType = m.getGenericReturnType();
            sb.append(((genRetType instanceof Class<?>)
                    ? getTypeName((Class<?>) genRetType) : 
                        genRetType instanceof ParameterizedTypeImpl ?
                        toStringReplaced((ParameterizedTypeImpl) genRetType,myObj) :
                        genRetType.toString())).append(' ');
            */
            //sb.append(getTypeName(m.getDeclaringClass())).append('.');
            sb.append(m.getName()).append('(');
            Type[] params = m.getGenericParameterTypes();
            for (int j = 0; j < params.length; j++)
            {
                String param = (params[j] instanceof Class)
                        ? getTypeName((Class) params[j]) :
                            (params[j].toString());
                // override
                if (params[j] instanceof ParameterizedTypeImpl)
                {
                    param = toStringReplaced((ParameterizedTypeImpl) params[j],myObj);
                }
                
                if (isVarArgs(m) && (j == params.length - 1)) // replace T[] with T...
                {
                    param = param.replaceFirst("\\[\\]$", "...");
                }

                if(myObj!=null)
                    if(myObj.generics.containsKey(param))
                        sb.append(myObj.generics.get(param));
                    else
                        sb.append(param);
                else
                    sb.append(param);
                
                if (j < (params.length - 1))
                {
                    sb.append(',');
                }
            }
            sb.append(')');
            /*Type[] exceptions = m.getGenericExceptionTypes();
            if (exceptions.length > 0)
            {
                sb.append(" throws ");
                for (int k = 0; k < exceptions.length; k++)
                {
                    sb.append((exceptions[k] instanceof Class)
                            ? ((Class) exceptions[k]).getName()
                            : exceptions[k].toString());
                    if (k < (exceptions.length - 1))
                    {
                        sb.append(',');
                    }
                }
            }*/
            return sb.toString();
        } 
        catch (Exception e)
        {
            return "<" + e + ">";
        }
    }

    // Taken from OpenJDF7
    // Class "Method"
    private String toCompleteString(Method m)
    {
        final int METHOD_MODIFIERS =
                Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE
                | Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL
                | Modifier.SYNCHRONIZED | Modifier.NATIVE | Modifier.STRICT;
        try
        {
            StringBuilder sb = new StringBuilder();
            int mod = m.getModifiers() & METHOD_MODIFIERS;
            if (mod != 0)
            {
                sb.append(Modifier.toString(mod)).append(' ');
            }
            TypeVariable<?>[] typeparms = m.getTypeParameters();
            if (typeparms.length > 0)
            {
                boolean first = true;
                sb.append('<');
                for (TypeVariable<?> typeparm : typeparms)
                {
                    if (!first)
                    {
                        sb.append(',');
                    }
                    // Class objects can't occur here; no need to test
                    // and call Class.getName().
                    sb.append(typeparm.toString());
                    first = false;
                }
                sb.append("> ");
            }

            Type genRetType = m.getGenericReturnType();
            sb.append(((genRetType instanceof Class<?>)
                    ? getTypeName((Class<?>) genRetType) : 
                        genRetType instanceof ParameterizedTypeImpl ?
                        toString((ParameterizedTypeImpl) genRetType) :
                        genRetType.toString())).append(' ');

            //sb.append(getTypeName(m.getDeclaringClass())).append('.');
            sb.append(m.getName()).append('(');
            Type[] params = m.getGenericParameterTypes();
            for (int j = 0; j < params.length; j++)
            {
                String param = (params[j] instanceof Class)
                        ? getTypeName((Class) params[j]) :
                            (params[j].toString());
                // override
                if (params[j] instanceof ParameterizedTypeImpl)
                {
                    param = toString((ParameterizedTypeImpl) params[j]);
                }
                
                if (isVarArgs(m) && (j == params.length - 1)) // replace T[] with T...
                {
                    param = param.replaceFirst("\\[\\]$", "...");
                }

                sb.append(param);
                if (j < (params.length - 1))
                {
                    sb.append(',');
                }
            }
            sb.append(')');
            /*Type[] exceptions = m.getGenericExceptionTypes();
            if (exceptions.length > 0)
            {
                sb.append(" throws ");
                for (int k = 0; k < exceptions.length; k++)
                {
                    sb.append((exceptions[k] instanceof Class)
                            ? ((Class) exceptions[k]).getName()
                            : exceptions[k].toString());
                    if (k < (exceptions.length - 1))
                    {
                        sb.append(',');
                    }
                }
            }*/
            return sb.toString();
        } 
        catch (Exception e)
        {
            return "<" + e + ">";
        }
    }

    // Taken from OpenJDF7
    // Class "Method"
    public String constToCompleteString(Constructor m)
    {
        final int METHOD_MODIFIERS =
                Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE
                | Modifier.ABSTRACT | Modifier.STATIC | Modifier.FINAL
                | Modifier.SYNCHRONIZED | Modifier.NATIVE | Modifier.STRICT;
        try
        {
            StringBuilder sb = new StringBuilder();
            int mod = m.getModifiers() & METHOD_MODIFIERS;
            if (mod != 0)
            {
                sb.append(Modifier.toString(mod)).append(' ');
            }
            TypeVariable<?>[] typeparms = m.getTypeParameters();
            if (typeparms.length > 0)
            {
                boolean first = true;
                sb.append('<');
                for (TypeVariable<?> typeparm : typeparms)
                {
                    if (!first)
                    {
                        sb.append(',');
                    }
                    // Class objects can't occur here; no need to test
                    // and call Class.getName().
                    sb.append(typeparm.toString());
                    first = false;
                }
                sb.append("> ");
            }

            sb.append(' ');

            //sb.append(getTypeName(m.getDeclaringClass())).append('.');
            //sb.append(m.getName()).append('(');
            sb.append(m.getDeclaringClass().getSimpleName()).append('(');
            Type[] params = m.getGenericParameterTypes();
            for (int j = 0; j < params.length; j++)
            {
                String param = (params[j] instanceof Class)
                        ? getTypeName((Class) params[j]) :
                            (params[j].toString());
                // override
                if (params[j] instanceof ParameterizedTypeImpl)
                {
                    param = toString((ParameterizedTypeImpl) params[j]);
                }
                
                sb.append(param);
                if (j < (params.length - 1))
                {
                    sb.append(',');
                }
            }
            sb.append(')');
            /*Type[] exceptions = m.getGenericExceptionTypes();
            if (exceptions.length > 0)
            {
                sb.append(" throws ");
                for (int k = 0; k < exceptions.length; k++)
                {
                    sb.append((exceptions[k] instanceof Class)
                            ? ((Class) exceptions[k]).getName()
                            : exceptions[k].toString());
                    if (k < (exceptions.length - 1))
                    {
                        sb.append(',');
                    }
                }
            }*/
            return sb.toString().trim();
        } 
        catch (Exception e)
        {
            return "<" + e + ">";
        }
    }

    private void fillPopup(JComponent popup, Class c, MyObject myObj)
    {
        //System.out.println("FP: ************ Class: "+c.getName());

        int lastFreeIndex = popup.getComponentCount();

        MyClass mc = diagram.getClass(c.getName());
        //boolean unimozerClass = (mc!=null);

        // get all methods this class declares
        Method m[] = c.getDeclaredMethods();
        Hashtable<String,String> items = new Hashtable<String,String>();

        // loop the methods
        for (int i = 0; i < m.length; i++)
        {
            //System.out.println("FP: Found method: "+m[i].toGenericString());

            String full = toFullString(m[i]);
            String complete = toCompleteString(m[i]);
/*           
            // getting generics of return type
            String generics = "";
            TypeVariable[] tv = m[i].getReturnType().getTypeParameters();
            if(tv.length>0)
            {
                LinkedHashMap<String,String> gms = new LinkedHashMap<String,String>();
                for(int tt=0;tt<tv.length;tt++)
                {
                    gms.put(tv[tt].getName(),"");
                }

                // build the string
                generics = "<";
                Object[] keys = gms.keySet().toArray();
                for(int in=0;in<keys.length;in++)
                {
                    String kname = (String) keys[in];
                        generics+=kname+",";
                }
                generics=generics.substring(0, generics.length()-1);
                generics+=">";                                        
            }            
            //System.out.println("FP: Generics = "+generics);

            String full = "";
            full+= m[i].getReturnType().getSimpleName()+generics;
            full+=" ";
            full+= m[i].getName();
            full+= "(";
            Type[] tvm = m[i].getParameterTypes();
            for(int t=0;t<tvm.length;t++)
            {
                // getting generics of parameters
                generics = "";
                tv = ((Class) tvm[t]).getTypeParameters();
                if(tv.length>0)
                {
                    LinkedHashMap<String,String> gms = new LinkedHashMap<String,String>();
                    for(int tt=0;tt<tv.length;tt++)
                    {
                        gms.put(tv[tt].getName(),"");
                    }

                    // build the string
                    generics = "<";
                    Object[] keys = gms.keySet().toArray();
                    for(int in=0;in<keys.length;in++)
                    {
                        String kname = (String) keys[in];
                        //generics+=myObj.generics.get(kname)+",";
                        generics+=kname+",";
                    }
                    generics=generics.substring(0, generics.length()-1);
                    generics+=">";                                        
                }            
                //System.out.println("FP: Generics = "+generics);

                String sn = tvm[t].toString();
                sn=sn.substring(sn.lastIndexOf('.')+1,sn.length());
                if(sn.startsWith("class")) sn=sn.substring(5).trim();
                // array is shown as ";"  ???
                if(sn.endsWith(";"))
                {
                    sn=sn.substring(0,sn.length()-1)+"[]";
                }
                full+= sn+generics+", ";
            }
            if(tvm.length>0) full=full.substring(0,full.length()-2);
            full+= ")";
*/
//            String backup = new String(full);
//            String complete = new String(Modifier.toString(m[i].getModifiers())+" "+full);
//            String noReturnType = full.substring(m[i].getReturnType().getSimpleName().length()).trim();

            //System.out.println("Full     = "+full);
            //System.out.println("Complete = "+complete);
            //System.out.println("NRT      = "+noReturnType);

            //if (base==true) System.err.println("Complete: "+complete);

            // get the real full name from the "MyObject" representation
            //int pos = -1;
            /*mc = diagram.getClass(c.getName());
            if(mc!=null)
            {
                //System.out.println("FP: Found class: "+mc.getFullName());
                complete = mc.getCompleteSignatureBySignature(full);
                full = mc.getFullSignatureBySignature(full);
                //if(myObj!=null) myObj.setMyClass(mc);
                // position in source code
                //pos = mc.getCompleteSignatureBySignaturePos(full);
                //pos = mc.getFullSignatureBySignaturePos(full);
                //if(pos!=-1) pos=pos2;
                //System.err.println(pos);
            }
            if(full.trim().equals("")) full=backup;*/

            //if((!complete.startsWith("private") || base==true)) // && (!complete.contains("static")))
            if(complete.startsWith("public")==true)
            {
                //System.err.println("Adding: "+complete);
                //JMenuItem item = new JMenuItem(full);
                //item.addActionListener(this);
                //item.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_method.png")));

                /*if(pos!=-1)
                {
                    if(pos+lastFreeIndex<popup.getComponentCount()) popup.add(item, pos+lastFreeIndex+1);
                    else popup.add(item, popup.getComponentCount());
                }
                else popup.add(item);
                 */
                /*if(unimozerClass) items[pos]=item;
                else popup.add(item);
                 */
                //items.put(noReturnType,full);
                //System.out.println(toFullStringReplaced(m[i], myObj));
                //System.err.println(toFullStringReplacedNoReturn(m[i], myObj));
                items.put(toFullStringReplacedNoReturn(m[i], myObj),
                          toFullStringReplaced(m[i], myObj));
            }
        }
        
        //Sort using a TreeSet ??
        Iterator<String> tit = new TreeSet (items.keySet()).iterator();
        while (tit.hasNext())
        {
            String noReturnType = tit.next();
            String full = items.get(noReturnType);

            JMenuItem item = new JMenuItem(full);
            item.addActionListener(this);
            item.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_method.png")));
            popup.add(item);
        }
        /*
        if(unimozerClass) for (int i = 0; i < m.length; i++)
        {
            popup.add(items[i]);
        }
         */
    }

    private void fillPopupFields(JComponent popup, Class c, MyObject myObj)
    {
        Field m[] = c.getDeclaredFields();
        boolean found = false;
        for (int i = 0; i < m.length; i++)
        {
            //if(!m[i].getType().isPrimitive() && Modifier.toString(m[i].getModifiers()).contains("public"))
            /*if (diagram.findByShortName(m[i].getType().getSimpleName())!=null)
            {
                String name = m[i].getType().getSimpleName()+" "+m[i].getName();
                JMenu item = new JMenu(name);
                item.addActionListener(this);
                item.setName("field");
                item.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_field.png")));
                popup.add(item);
                fillPopup(item, m[i].getType(), null);
                found=true;
            }
            else //if(m[i].getType().isPrimitive())*/
            {
                String name = m[i].getType().getSimpleName()+" "+m[i].getName();
                JMenuItem item = new JMenuItem(name);
                item.addActionListener(this);
                item.setName("primitiveField");
                item.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_field.png")));
                popup.add(item);
                found=true;
            }
        }
        if (found==true)
        {
            JSeparator sep = new JSeparator();
            popup.add(sep);
        }
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        selected=null;
        Set<String> set = objects.keySet();
        Iterator<String> itr = set.iterator();
        while (itr.hasNext())
        {
          String objectName = itr.next();
          MyObject myObj = objects.get(objectName);
          if(myObj.isInside(e.getPoint()))
          {
            Object obj = myObj.getObject();
            selected = myObj;
            //System.out.println("Sel "+selected.getName());
            try
            {
                Class c = obj.getClass();
                // clean popup
                popup.removeAll();
                // inspect itrm
                JMenuItem item = new JMenuItem("Inspect");
                item.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/iconfinder_inspect_lgpl_everaldo_coelho.png")));
                item.addActionListener(this);
                popup.add(item);

                JSeparator sep = new JSeparator();
                popup.add(sep);

                do
                {
                    c=c.getSuperclass();
                    if(c!=null)
                    {
                        JMenu itemM = new JMenu("inherited from "+c.getSimpleName());
                        itemM.setFont(new Font(item.getFont().getFontName(),
                                              Font.ITALIC,item.getFont().getSize()));
                        itemM.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_parent.png")));
                        popup.add(itemM);

                        fillPopup(itemM, c, null);
                    }
                }
                while(c!=null);

                sep = new JSeparator();
                popup.add(sep);

                //fillPopupFields(popup, obj.getClass(), myObj);
                
                //If it is a MyInteractiveObject, only the methods of the attached interface should be shown
                if(!myObj.getClass().equals(lu.fisch.unimozer.interactiveproject.MyInteractiveObject.class))
                    fillPopup(popup, obj.getClass(), myObj);
                else
                    fillPopup(popup, obj.getClass().getInterfaces()[0], myObj);
                sep = new JSeparator();
                popup.add(sep);

                item = new JMenuItem("Remove");
                item.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/gen_del.png")));
                item.addActionListener(this);
                popup.add(item);

            }
            catch (Throwable ex)
            {
                MyError.display(ex);
            }
          }
        }
    }

    public static void printLinkedHashMap(String name,LinkedHashMap<String,String> lhm)
    {
        System.out.println("*** "+name+" ***");
        Object[] keySet = lhm.keySet().toArray();
        for(int j=0;j<keySet.length;j++)
        {
            String key = (String) keySet[j];
            String value = lhm.get(key);
            System.out.println(key+" -> "+value);
        }
    
    }
    
    public static void printHashtable(String name,Hashtable<String,String> lhm)
    {
        System.out.println("*** "+name+" ***");
        Object[] keySet = lhm.keySet().toArray();
        for(int j=0;j<keySet.length;j++)
        {
            String key = (String) keySet[j];
            String value = lhm.get(key);
            System.out.println(key+" -> "+value);
        }
    
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        // get clicked JMenuItem
        JMenuItem sourceMenuItem = (JMenuItem) e.getSource();
        // get the JPopupMenu
        JPopupMenu sourcePopupMenu = (JPopupMenu) sourceMenuItem.getParent();
        // try to the the parent JMenu
        JMenu sourceMenu = null;
        if(sourcePopupMenu.getInvoker() instanceof JMenu) sourceMenu=(JMenu) sourcePopupMenu.getInvoker();
        // if there is a JMenu, get it's name
        String sourceName = "";
        if(sourceMenu!=null) sourceName=sourceMenu.getName();
        if(sourceName==null) sourceName="";
        // also get the text of the JMenu (if any)
        String sourceText = "";
        if(!sourceName.equals("")) sourceText=sourceMenu.getText();


        if(((JMenuItem) e.getSource()).getText().equals("Remove") && selected!=null)
        {
            //objects.remove(selected.getName());
            removeObject(selected.getName());
            selected.cleanUp();
            repaint();
        }
        else if(((JMenuItem) e.getSource()).getText().startsWith("Inspect") && selected!=null)
        {
            ObjectInspector oi = new ObjectInspector(selected, this.frame,diagram,this);
        }
        // call a method on the object
        /*else if(selected!=null && sourceMenuItem.getName().equals("primitiveField"))
        {
            try
            {
                String sign = ((JMenuItem) e.getSource()).getText();
                String name = sign.substring(sign.indexOf(" ")).trim();
                java.lang.reflect.Field field = selected.getObject().getClass().getDeclaredField(name);
                field.setAccessible(true);
                JOptionPane.showMessageDialog(frame, field.get(selected.getObject()).toString(), "Field value", JOptionPane.INFORMATION_MESSAGE,Unimozer.IMG_INFO);
            }
            catch (IllegalArgumentException ex)
            {
                ex.printStackTrace();
            }
            catch (IllegalAccessException ex)
            {
                ex.printStackTrace();
            }
            catch (NoSuchFieldException ex)
            {
                ex.printStackTrace();
            }
            catch (SecurityException ex)
            {
                ex.printStackTrace();
            }
        }*/
        // call a method on the object
        else if(selected!=null && !sourceName.equals("field"))
        {            
            //System.out.println("Sel "+selected.getName());
            // get full signature
            String fullSign = ((JMenuItem) e.getSource()).getText();
            // get signature
            /*System.out.println(selected);
            System.out.println(selected.getMyClass());
            System.out.println(selected.getMyClass().getSignatureByFullSignature(fullSign));*/
            String sign = fullSign;
            if(selected!=null && selected.getMyClass()!=null)
            {
                sign = selected.getMyClass().getSignatureByFullSignature(fullSign);
                //String complete = selected.getMyClass().getCompleteSignatureBySignature(sign);
            }

            // find method
            Object obj = selected.getObject();
            Class c = obj.getClass();
            Method m[] = c.getMethods();
            for (int i = 0; i < m.length; i++)
            {
                final Method meto = m[i];
                /*String full = "";
                full+= m[i].getReturnType().getSimpleName();
                full+=" ";
                full+= m[i].getName();
                full+= "(";
                Class<?>[] tvm = m[i].getParameterTypes();
                LinkedHashMap<String,String> genericInputs = new LinkedHashMap<String,String>();
                for(int t=0;t<tvm.length;t++)
                {
                    String sn = tvm[t].toString();
                    genericInputs.put("param"+t,sn);
                    sn=sn.substring(sn.lastIndexOf('.')+1,sn.length());
                    if(sn.startsWith("class")) sn=sn.substring(5).trim();
                    // array is shown as ";"  ???
                    if(sn.endsWith(";"))
                    {
                        sn=sn.substring(0,sn.length()-1)+"[]";
                    }
                    full+= sn+", ";
                }
                if(tvm.length>0) full=full.substring(0,full.length()-2);
                full+= ")";
                 */
                String full = toFullStringReplaced(m[i],selected);
                LinkedHashMap<String,String> genericInputs = getInputsReplaced(m[i],selected);

                //if(full.equals(sign) || full.equals(fullSign))
                if(full.equals(fullSign))
                {
                    String signi = toFullString(m[i]);
                    boolean succes = isConsoleActive();
                    if (succes)
                    {
                        //printHashtable("selected.generics",selected.generics);
                        LinkedHashMap<String,String> inputs = null;
                        if(selected.getMyClass()!=null)
                            inputs = selected.getMyClass().getInputsBySignature(signi,selected.generics);
                        else {
                            inputs=genericInputs;
                        }
                        if(inputs.size()!=genericInputs.size())
                        {
                            inputs=genericInputs;
                        }
                        MethodInputs mi = null;
                        boolean go = true;
                        if(inputs.size()>0)
                        {
                            if(selected.getMyClass()!=null)
                                mi = new MethodInputs(frame,inputs,full,selected.getMyClass().getJavaDocBySignature(sign));
                            else
                                mi = new MethodInputs(frame,inputs,full,"");
                            go = mi.OK;
                        }
                        if(go==true)
                        {
                            try
                            {
                                // generated the command to call
                                // objectname.methodname(
                                String method = selected.getName()+"."+m[i].getName()+"(";
                                if(inputs.size()>0)
                                {
                                    Object[] keys = inputs.keySet().toArray();
                                    // add parameters
                                    for(int in=0;in<keys.length;in++)
                                    {
                                        String name = (String) keys[in];
                                        String val = mi.getValueFor(name);
                                        if (val.equals("")) method+=val+"null,";
                                        else
                                        {
                                            String type = mi.getTypeFor(name);
                                            //System.out.println(type);
                                            if (type.toLowerCase().equals("byte"))
                                                method+="Byte.valueOf(\""+val+"\"),";
                                            else if (type.toLowerCase().equals("short"))
                                                method+="Short.valueOf(\""+val+"\"),";
                                            else if (type.toLowerCase().equals("float"))
                                                method+="Float.valueOf(\""+val+"\"),";
                                            else if (type.toLowerCase().equals("long"))
                                                method+="Long.valueOf(\""+val+"\"),";
                                            else if (type.toLowerCase().equals("double"))
                                                method+="Double.valueOf(\""+val+"\"),";
                                            else if (type.toLowerCase().equals("char"))
                                                method+="'"+val+"',";
                                            else
                                                method+=val+",";
                                        }
                                    }
                                    // delete the last ","
                                    if(!method.endsWith("("))
                                        method=method.substring(0, method.length()-1);
                                }
                                // close it
                                method+=")";

                                //System.out.println(method);

                                // Invoke method in a new thread
                                final String myMeth = method;
                                final Objectizer me = this;
                                Runnable r = new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        try
                                        {
                                            Object retobj = Runtime5.getInstance().executeMethod(myMeth);
                                            if(retobj!=null && diagram.getInteractiveProject()!=null)
                                            {
                                                //To prevent that the Popup is blocked by the Project Window
                                                diagram.getInteractiveProject().setOnTop(false);
                                                JOptionPane.showMessageDialog(frame, retobj.toString(), "Result", JOptionPane.INFORMATION_MESSAGE,Unimozer.IMG_INFO);
                                                diagram.getInteractiveProject().setOnTop(true);
                                            }
                                            else if(retobj!=null) 
                                                JOptionPane.showMessageDialog(frame, retobj.toString(), "Result", JOptionPane.INFORMATION_MESSAGE,Unimozer.IMG_INFO);
                                            else if(!meto.getReturnType().getSimpleName().equals("void")) JOptionPane.showMessageDialog(frame, "NULL", "Result", JOptionPane.INFORMATION_MESSAGE,Unimozer.IMG_INFO);
                                        }
                                        catch (EvalError ex)
                                        {
                                            if(!ex.toString().contains("java.lang.ThreadDeath"))
                                            {
                                                JOptionPane.showMessageDialog(frame, ex.toString(), "Invokation error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                                                MyError.display(ex);
                                            }
                                        }
                                        me.repaint();
                                        setCallingText("");
                                    }
                                };
                                Thread t = new Thread(r);
                                t.setName(myMeth);
                                setCallingText(myMeth);
                                threads.add(t);
                                t.start();

                                // get back the result as generic object
                                //Object retobj = Runtime5.getInstance().executeMethod(method);
                                //if(retobj!=null) JOptionPane.showMessageDialog(frame, retobj.toString(), "Result", JOptionPane.INFORMATION_MESSAGE,Unimozer.IMG_INFO);
                            }
                            catch (Throwable ex)
                            {
                                JOptionPane.showMessageDialog(frame, ex.toString(), "Invokation error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                                MyError.display(ex);
                            }
                        }
                    }
                    else JOptionPane.showMessageDialog(frame, "There is already another method reading from the console.\n"+
                                                              "It is not possible to start a new method while another one\n"+
                                                              "is reading from the console.\n\n"+
                                                              "If the method is stuck in a loop, try to recompile the project!"
                                                              , "Execution error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                }
             }
        }
        // call a method on a public field
        else if(selected!=null && sourceName.equals("field"))
        {
            try
            {
                // get the object name
                // the name does always contain a space
                String objectType = sourceText.substring(0, sourceText.indexOf(" ")).trim();
                String objectName = sourceText.substring(sourceText.indexOf(" ")).trim();
                // get full signature
                String fullSign = ((JMenuItem) e.getSource()).getText();
                // is it a class we have defined?
                MyClass objectMyClass = diagram.getClass(objectType);
                String sign = "";
                String complete = "";
                if(objectMyClass!=null)
                {
                    sign = objectMyClass.getSignatureByFullSignature(fullSign);
                    complete = objectMyClass.getCompleteSignatureBySignature(sign);
                }


                Class c = Runtime5.getInstance().load(objectType);
                Method[] m = c.getMethods();
                for (int i = 0; i < m.length; i++)
                {
                    final Method meto = m[i];
                    String full = "";
                    full+= m[i].getReturnType().getSimpleName();
                    full+=" ";
                    full+= m[i].getName();
                    full+= "(";
                    Class<?>[] tvm = m[i].getParameterTypes();
                    LinkedHashMap<String,String> genericInputs = new LinkedHashMap<String,String>();
                    for(int t=0;t<tvm.length;t++)
                    {
                        String sn = tvm[t].toString();
                        genericInputs.put("param"+t,sn);
                        sn=sn.substring(sn.lastIndexOf('.')+1,sn.length());
                        if(sn.startsWith("class")) sn=sn.substring(5).trim();
                        full+= sn+", ";
                    }
                    if(tvm.length>0) full=full.substring(0,full.length()-2);
                    full+= ")";

                    if(full.equals(sign) || full.equals(fullSign))
                    {
                        boolean succes = isConsoleActive();
                        if (succes)
                        {
                            LinkedHashMap<String,String> inputs = new LinkedHashMap<String,String>();
                            if(objectMyClass!=null) objectMyClass.getInputsBySignature(sign);
                            if(inputs.size()!=genericInputs.size())
                            {
                                inputs=genericInputs;
                            }
                            MethodInputs mi = null;
                            boolean go = true;
                            if(inputs.size()>0)
                            {
                                mi = new MethodInputs(frame,inputs,full,objectMyClass.getJavaDocBySignature(sign));
                                go = mi.OK;
                            }
                            if(go==true)
                            {
                                try
                                {
                                    // generated the command to call
                                    // objectname.methodname(
                                    String method = selected.getName()+"."+objectName+"."+m[i].getName()+"(";
                                    if(inputs.size()>0)
                                    {
                                        Object[] keys = inputs.keySet().toArray();
                                        // add parameters
                                        for(int in=0;in<keys.length;in++)
                                        {
                                            String name = (String) keys[in];
                                            //method+=mi.getValueFor(name)+",";
                                            String val = mi.getValueFor(name);
                                            if (val.equals("")) val="null";
                                            else
                                            {
                                                String type = mi.getTypeFor(name);
                                                if (type.toLowerCase().equals("byte"))
                                                    method+="Byte.valueOf(\""+val+"\"),";
                                                else if (type.toLowerCase().equals("short"))
                                                    method+="Short.valueOf(\""+val+"\"),";
                                                else if (type.toLowerCase().equals("float"))
                                                    method+="Float.valueOf(\""+val+"\"),";
                                                else if (type.toLowerCase().equals("long"))
                                                    method+="Long.valueOf(\""+val+"\"),";
                                                else if (type.toLowerCase().equals("double"))
                                                    method+="Double.valueOf(\""+val+"\"),";
                                                else
                                                    method+=val+",";
                                            }
                                        }
                                        // delete the last ","
                                        method=method.substring(0, method.length()-1);
                                    }
                                    // close it
                                    method+=")";

                                    // Invoke method in a new thread
                                    final String myMeth = method;
                                    final Objectizer me = this;
                                    Runnable r = new Runnable()
                                    {
                                            @Override
                                            public void run()
                                            {
                                                try
                                                {
                                                    Object retobj = Runtime5.getInstance().executeMethod(myMeth);
                                                    if(retobj!=null) JOptionPane.showMessageDialog(frame, retobj.toString(), "Result", JOptionPane.INFORMATION_MESSAGE,Unimozer.IMG_INFO);
                                                    else if(!meto.getReturnType().getSimpleName().equals("void")) JOptionPane.showMessageDialog(frame, "NULL", "Result", JOptionPane.INFORMATION_MESSAGE,Unimozer.IMG_INFO);
                                                }
                                                catch (EvalError ex)
                                                {
                                                    if(!ex.toString().contains("java.lang.ThreadDeath"))
                                                    {
                                                        JOptionPane.showMessageDialog(frame, ex.toString(), "Invokation error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                                                        MyError.display(ex);
                                                    }
                                                }
                                                me.repaint();
                                                setCallingText("");
                                            }
                                    };
                                    Thread t = new Thread(r);
                                    t.setName(myMeth);
                                    setCallingText(myMeth);
                                    threads.add(t);
                                    t.start();

                                    // get back the result as generic object
                                    //Object retobj = Runtime5.getInstance().executeMethod(method);
                                    //if(retobj!=null) JOptionPane.showMessageDialog(frame, retobj.toString(), "Result", JOptionPane.INFORMATION_MESSAGE,Unimozer.IMG_INFO);
                                }
                                catch (Throwable ex)
                                {
                                    JOptionPane.showMessageDialog(frame, ex.toString(), "Invokation error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                                    MyError.display(ex);
                                }
                            }
                        }
                        else JOptionPane.showMessageDialog(frame, "There is already another method reading from the console.\n"+
                                                                  "It is not possible to start a new method while another one\n"+
                                                                  "is reading from the console.\n\n"+
                                                                  "If the method is stuck in a loop, try to recompile the project!"
                                                                  , "Execution error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                    }
                }
            }
            catch (ClassNotFoundException ex)
            {
                JOptionPane.showMessageDialog(frame, ex.toString(), "Invokation error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                MyError.display(ex);
            }

        }
        repaint();
    }

    /**
     * @param diagram the diagram to set
     */
    public void setDiagram(Diagram diagram)
    {
        this.diagram = diagram;
    }

    /**
     * @param frame the frame to set
     */
    public void setFrame(Mainform frame)
    {
        this.frame = frame;
    }

    @Override
    public void windowOpened(WindowEvent e)
    {
    }

    
    
    @Override
    public void windowClosing(WindowEvent e)
    {
        // we are only listening frames that would close,
        // not the other ones
        //if (((JFrame) e.getSource()).getDefaultCloseOperation()==JFrame.DISPOSE_ON_CLOSE)
        //{
            // remove the object from objectizer
            removeObject(((JFrame) e.getSource()).getName());
            // dispose the window
            ((JFrame) e.getSource()).dispose();
            // repaint objectizer
            repaint();
        //}
    }

    @Override
    public void windowClosed(WindowEvent e)
    {
    }

    @Override
    public void windowIconified(WindowEvent e)
    {
    }

    @Override
    public void windowDeiconified(WindowEvent e)
    {
    }

    @Override
    public void windowActivated(WindowEvent e)
    {
    }

    @Override
    public void windowDeactivated(WindowEvent e)
    {
    }

    /**
     * @return the calling
     */
    public JLabel getCalling()
    {
        return calling;
    }

    /**
     * @param calling the calling to set
     */
    public void setCalling(JLabel calling)
    {
        this.calling = calling;
    }

    private void setCallingText(String text)
    {
        if(calling!=null) 
        {
            calling.setBackground(Color.decode("#ffffaa"));
            if(text.equals("")) calling.setText("");
            else calling.setText(" Method call: " + text);
        }
    }

	// PopupListener Methods
	class PopupListener extends MouseAdapter
	{ 
        @Override
		public void mousePressed(MouseEvent e)
		{
			showPopup(e);
		}

        @Override
		public void mouseReleased(MouseEvent e)
		{
			showPopup(e);
		}

		private void showPopup(MouseEvent e)
		{
			if (e.isPopupTrigger())
			{
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}


    @Override
    public void mouseReleased(MouseEvent e)
    {
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
    }

}
