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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Window;
import java.lang.reflect.Field;
import java.util.Hashtable;
import java.util.Vector;

/**
 *
 * @author robertfisch
 */
public class MyObject
{
    private Object object;
    private Point position;
    private int width;
    private int height;
    private String className;
    private MyClass myClass;
    protected Diagram diagram;

    public final static int PADDING = 5;
    
    public Hashtable<String,String> generics = new Hashtable<String,String>();
    
    public Vector<MyObject> children = new Vector<MyObject>();
    
    public void addChild(MyObject mo)
    {
        children.add(mo);
    }
    
    public void removeChild(MyObject mo)
    {
        children.remove(mo);
    }

    public MyObject(String className, Object object, Diagram diagram)
    {
        //if(object==null) System.err.println("Setting "+className+" as null!");
        this.className=className;
        this.object=object;
        this.diagram=diagram;
    }

    public boolean isInside(Point pt)
    {
        return (position.x<=pt.x && pt.x<=position.x+getWidth() &&
                position.y<=pt.y && pt.y<=position.y+getHeight());
    }

    public void cleanUp()
    {
        // remove if the object is a Window
        if (object instanceof Window) ((Window) object).dispose();
        object = null;
        System.gc();
        System.gc();
    }

    /**
     * @return the object
     */
    public Object getObject()
    {
        return object;
    }

    /**
     * @param object the object to set
     */
    public void setObject(Object object)
    {
        this.object = object;
    }

    /**
     * @return the position
     */
    public Point getPosition()
    {
        return position;
    }

    /**
     * @param position the position to set
     */
    public void setPosition(Point position)
    {
        this.position = position;
    }

    /**
     * @return the width
     */
    public int getWidth()
    {
        return width;
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width)
    {
        this.width = width;
    }

    /**
     * @return the height
     */
    public int getHeight()
    {
        return height;
    }

    /**
     * @param height the height to set
     */
    public void setHeight(int height)
    {
        this.height = height;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return className;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name)
    {
        this.className = name;
    }


    /**
     * @return the myClass
     */
    public MyClass getMyClass()
    {
        return myClass;
    }

    /**
     * @param myClass the myClass to set
     */
    public void setMyClass(MyClass myClass)
    {
        this.myClass = myClass;
        
        // copy the generic types to this instance
        generics.clear();
        Object[] keys = myClass.generics.keySet().toArray();
        for(int i=0;i<keys.length;i++)
        {
            String key = (String) keys[i];
            generics.put(key, myClass.generics.get(key));
        }
    }

    public int paint(Graphics2D g, int x, int y, boolean isUML)
    {
          String className = "<?>";
          if (this.getMyClass()!=null) className=this.getMyClass().getShortName();
          else if (getObject().getClass()!=null) className=getObject().getClass().getCanonicalName();

          Color color = new Color(255,100,100);

          // determine max width an height
          // top
          String top = getName()+" : "+className;
          if (!isUML) top = className + " "+ getName();
          g.setFont(new Font(g.getFont().getName(),Font.BOLD,g.getFont().getSize()));
          int topWidth = (int) g.getFont().getStringBounds(top, g.getFontRenderContext()).getWidth();
          int topHeight = (int) g.getFont().getStringBounds(top, g.getFontRenderContext()).getHeight()+2*PADDING;

          // init max
          int maxWidth = topWidth;
          int totalHeight = topHeight;

          Class c = getObject().getClass();

          while(c!=null)
          {
              try
              {
                  //System.out.println("Checking class: "+c.getSimpleName());


                  // loop through fields
                  for(int f=0;f<c.getDeclaredFields().length;f++)
                  //for (Field field : c.getDeclaredFields())
                  {
                    Field field = c.getDeclaredFields()[f];
                    //System.out.println("Found field: "+field.getName());
                    field.setAccessible(true);
                    String display = field.getName() + " = ?";
                    try
                    {
                        if(getObject()!=null)
                        {
                            Object o = field.get(getObject());
                            if(o!=null)
                                if (field.getType().isArray())
                                {
                                    /*
                                    Encapsulate our Object in an Object[] array and use java.util.Arrays.deepToString() to
                                    create a string representation. Manually remove the leading [ and trailing ].
                                    */
                                    Object[] tmp = {o};
                                    String s = java.util.Arrays.deepToString(tmp);
                                    display = field.getName() + " = " + s.substring(1, s.length()-1);
                                }
                                else    
                                    display = field.getName() + " = " + o.toString();
                            else
                                display = field.getName() + " = <NULL>";
                        }
                        else
                            display = field.getName() + " = <NULL>";
                    }
                    catch (IllegalArgumentException ex)
                    {
                        //ex.printStackTrace();
                    }
                    catch (IllegalAccessException ex)
                    {
                        //ex.printStackTrace();
                    }
                    g.setFont(new Font(g.getFont().getName(),Font.PLAIN,g.getFont().getSize()));

                    if(display.length()>50) display=display.substring(0,47)+"...";

                    int fieldWidth = (int) g.getFont().getStringBounds(display, g.getFontRenderContext()).getWidth();
                    totalHeight += (int) g.getFont().getStringBounds(display, g.getFontRenderContext()).getHeight()+PADDING;
                    if(fieldWidth>maxWidth) maxWidth=fieldWidth;
                  }

                  c=c.getSuperclass();
                  // stop if the superclass has not been defined by ourselves
                  if (diagram!=null && c!=null)
                  {
                    if (!diagram.containsClass(c.getName()) ) c=null;
                  }
              }
              catch (Exception e)
              {
                  c=null;
              }
          }
          totalHeight+=PADDING;
          maxWidth+=2*PADDING;

          // update position
          this.setPosition(new Point(x,y));
          this.setWidth(maxWidth);
          this.setHeight(totalHeight);

          // draw box
          g.setColor(color);
          g.fillRoundRect(x, y, maxWidth, totalHeight, 8,8);
          g.setColor(Color.BLACK);
          g.drawRoundRect(x, y, maxWidth, totalHeight, 8,8);

          // draw line
          g.drawLine(x, y+topHeight+PADDING, x+maxWidth, y+topHeight+PADDING);

          // draw title
          g.setColor(Color.BLACK);
          g.setFont(new Font(g.getFont().getName(),Font.BOLD,g.getFont().getSize()));
          g.drawString(top, x+PADDING, y+topHeight-PADDING);
          int drawTop = y+topHeight+PADDING;

          c = getObject().getClass();

          while(c!=null)
          {
            try
            {
              // draw fields
              for(int f=0;f<c.getDeclaredFields().length;f++)
              //for (Field field : c.getDeclaredFields())
              {
                Field field = c.getDeclaredFields()[f];  
                field.setAccessible(true);
                String display = field.getName() + " = ?";
                try
                {
                    if(getObject()!=null)
                    {
                        Object o = field.get(getObject());
                        if(o!=null)
                            if (field.getType().isArray())
                            {
                                /*
                                Encapsulate our Object in an Object[] array and use java.util.Arrays.deepToString() to
                                create a string representation. Manually remove the leading [ and trailing ].
                                */
                                Object[] tmp = {o};
                                String s = java.util.Arrays.deepToString(tmp);
                                display = field.getName() + " = " + s.substring(1, s.length()-1);
                            }
                            else    
                                display = field.getName() + " = " + o.toString();
                        else
                            display = field.getName() + " = <NULL>";
                    }
                    else
                        display = field.getName() + " = <NULL>";
                    //display = field.getName() + " = " + field.get(getObject()).toString();
                }
                catch (IllegalArgumentException ex)
                {
                    //ex.printStackTrace();
                }
                catch (IllegalAccessException ex)
                {
                    //ex.printStackTrace();
                }

                if(display.length()>50) display=display.substring(0,47)+"...";

                g.setFont(new Font(g.getFont().getName(),Font.PLAIN,g.getFont().getSize()));
                int fieldHeight = (int) g.getFont().getStringBounds(display, g.getFontRenderContext()).getHeight()+PADDING;
                g.drawString(display, x+PADDING, drawTop+fieldHeight-PADDING);
                drawTop += fieldHeight;
              }
            }
            catch (Exception e)
            {
                // --do nothing (Windows 7 Problem?)
                // try it again!
                try
                {
                    Thread.sleep(500);
                }
                catch (InterruptedException ex)
                {
                    try
                    {
                      // draw fields
                      for(int f=0;f<c.getDeclaredFields().length;f++)
                      //for (Field field : c.getDeclaredFields())
                      {
                        Field field = c.getDeclaredFields()[f];  
                        field.setAccessible(true);
                        String display = field.getName() + " = ?";
                        try
                        {
                            if(getObject()!=null)
                            {
                                Object o = field.get(getObject());
                                if(o!=null)
                                    if (field.getType().isArray())
                                    {
                                        /*
                                        Encapsulate our Object in an Object[] array and use java.util.Arrays.deepToString() to
                                        create a string representation. Manually remove the leading [ and trailing ].
                                        */
                                        Object[] tmp = {o};
                                        String s = java.util.Arrays.deepToString(tmp);
                                        display = field.getName() + " = " + s.substring(1, s.length()-1);
                                    }
                                    else    
                                        display = field.getName() + " = " + o.toString();
                                else
                                    display = field.getName() + " = <NULL>";
                            }
                            else
                                display = field.getName() + " = <NULL>";
                            //display = field.getName() + " = " + field.get(getObject()).toString();
                        }
                        catch (IllegalArgumentException exi)
                        {
                            //exi.printStackTrace();
                        }
                        catch (IllegalAccessException exi)
                        {
                            //exi.printStackTrace();
                        }

                        if(display.length()>50) display=display.substring(0,47)+"...";

                        g.setFont(new Font(g.getFont().getName(),Font.PLAIN,g.getFont().getSize()));
                        int fieldHeight = (int) g.getFont().getStringBounds(display, g.getFontRenderContext()).getHeight()+PADDING;
                        g.drawString(display, x+PADDING, drawTop+fieldHeight-PADDING);
                        drawTop += fieldHeight;
                      }
                    }
                    catch (Exception exi)
                    {
                        // give up
                    }
                }
            }



            c=c.getSuperclass();
            // stop if the superclass has not been defined by ourselves
            if (diagram!=null && c!=null)
                if (!diagram.containsClass(c.getName())) c=null;
          }

          /*
          int wo = (int) g.getFont().getStringBounds(getName(), g.getFontRenderContext()).getWidth();
          g.setFont(new Font(g.getFont().getName(),Font.BOLD,g.getFont().getSize()));
          int wc = (int) g.getFont().getStringBounds(className, g.getFontRenderContext()).getWidth();
          g.setFont(new Font(g.getFont().getName(),Font.PLAIN,g.getFont().getSize()));
          int width = Math.max(wo,wc)+2*8;

          this.setPosition(new Point(x,8));
          this.setWidth(width);
          this.setHeight(50);

          g.setColor(color);
          g.fillRoundRect(x, 8, width, 50, 8,8);
          g.setColor(Color.BLACK);
          g.drawRoundRect(x, 8, width, 50, 8,8);

          g.setColor(Color.BLACK);
          g.setFont(new Font(g.getFont().getName(),Font.BOLD,g.getFont().getSize()));
          g.drawString(className, x+(width-wc)/2, 4*8);
          g.setFont(new Font(g.getFont().getName(),Font.PLAIN,g.getFont().getSize()));
          g.drawString(getName(), x+(width-wo)/2, 6*8);
          */

          return maxWidth;
    }

}
