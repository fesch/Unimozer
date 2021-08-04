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

import japa.parser.ast.Node;
import japa.parser.ast.body.*;
import org.codehaus.janino.Java;

import javax.swing.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author robertfisch
 */
public class Element
{
    public static final int CLASS = 0;
    public static final int METHOD = 1;
    public static final int FIELD = 2;
    public static final int CONSTRUCTOR = 3;
    public static final int INTERFACE = 4;
    public static final int ICONSIZE = 24;

    public static final Image PLUS = new javax.swing.ImageIcon(Element.class.getResource("/lu/fisch/icons/uml_plus.png")).getImage();
    public static final Image MINUS = new javax.swing.ImageIcon(Element.class.getResource("/lu/fisch/icons/uml_minus.png")).getImage();
    public static final Image DIEZE = new javax.swing.ImageIcon(Element.class.getResource("/lu/fisch/icons/uml_dieze.png")).getImage();
    public static final Image TILDE = new javax.swing.ImageIcon(Element.class.getResource("/lu/fisch/icons/uml_tilde.png")).getImage();

    private String name;
    private Node parent;
    private String umlName = new String();
    private Point position = new Point();
    private int width = 0;
    private int height = 0;
    private boolean selected = false;
    private BodyDeclaration node;
    private int type = CLASS;
    private String signature = null;
    private LinkedHashMap<String,String> params = new LinkedHashMap<String,String>();

    private boolean isUML = true;

    public Element(int type, Node parent)
    {
        this.type=type;
        this.parent = parent;
    }

    public Element(BodyDeclaration node, Node parent)
    {
        if(node instanceof FieldDeclaration) type=FIELD;
        else if(node instanceof MethodDeclaration) type=METHOD;
        else if(node instanceof ConstructorDeclaration) type=CONSTRUCTOR;
        this.node=node;
        this.parent = parent;
    }

    public boolean isInside(Point pt)
    {
        boolean res =  (getPosition().x<=pt.x && pt.x<getPosition().x+getWidth() &&
                        getPosition().y<=pt.y && pt.y<getPosition().y+getHeight());
        return res;
    }

    public void draw(Graphics2D g)
    {
        Color backup = g.getColor();
        Font backupFont = g.getFont();

        //int fs = g.getFont().getSize();
        if(selected==true)
        {
            g.setColor(new Color (255,200,0));
            g.fillRect(position.x,position.y,width,height);
        }
        g.setColor(Color.BLACK);
        int adjx = 0;
        int adjy = 0;

        Image img;

        // FS = 12, adj=(2,2) ? adj=(2,0)
        // FS = 10, adj=(2,0) ? adj=(2,-2)

        if(isUML())
        {
            // draw visibility addon
            if ((getType()!=CLASS) && (getType()!=INTERFACE))
            {
                adjx=2;
                adjy=Unimozer.DRAW_FONT_SIZE-10;
                if(getType()==CLASS)
                {
                    adjx=2;
                    adjy=Unimozer.DRAW_FONT_SIZE-12;
                }

                ClassOrInterfaceDeclaration castedParent = null;
                if(parent instanceof ClassOrInterfaceDeclaration){
                    castedParent = (ClassOrInterfaceDeclaration) parent;
                }
                if(getFullName().contains("private")) img = MINUS;
                else if(getFullName().contains("protected")) img = DIEZE;
                else if(getFullName().contains("public") || (castedParent!=null && castedParent.isInterface())) img = PLUS;
                else img = TILDE;
                if (img!=null && getType()!=INTERFACE) g.drawImage(img, position.x+MyClass.PAD/4+adjx, position.y+MyClass.PAD/2+adjy, null);
            }
        }
        else
        {
            // draw type icon
            adjx=2;
            adjy=Unimozer.DRAW_FONT_SIZE-10;
            if(getType()==CLASS)
            {
                adjx=2;
                adjy=Unimozer.DRAW_FONT_SIZE-12;
            }
            if(getType()==CONSTRUCTOR) img= new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_constructor.png")).getImage();
            else if(getType()==METHOD) img = new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_method.png")).getImage();
            else if(getType()==FIELD) img = new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_field.png")).getImage();
            else img = new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_class.png")).getImage();
            g.drawImage(img, position.x+MyClass.PAD/4+adjx, position.y+MyClass.PAD/2+adjy, null);

            // draw static addon
            if(getName().contains("static"))
            {
                adjx=4+4;
                adjy=Unimozer.DRAW_FONT_SIZE-10+1;
                img = new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/visi_static.png")).getImage();
                g.drawImage(img, position.x+MyClass.PAD/4+adjx, position.y+MyClass.PAD/2+adjy, null);
            }


            // draw visibility addon
            adjx=0;
            adjy=Unimozer.DRAW_FONT_SIZE-10-2;
            if(getFullName().contains("private")) img = new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/visi_private.png")).getImage();
            else if(getFullName().contains("protected")) img = new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/visi_protected.png")).getImage();
            else if(getFullName().contains("public")) img = null;
            else img = new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/visi_package.png")).getImage();
            if (img!=null) g.drawImage(img, position.x+MyClass.PAD/4+adjx, position.y+MyClass.PAD/2+adjy, null);

            // draw final and abstract addon
            adjy=Unimozer.DRAW_FONT_SIZE-10-2;
            g.setColor(Color.BLACK);
            g.setFont(new Font(g.getFont().getFamily(),g.getFont().getStyle(),Unimozer.DRAW_FONT_SIZE_ADDON));
            if(getName().contains("final")) g.drawString("F", position.x+MyClass.PAD/2+8+6, adjy+position.y+MyClass.PAD/2+8);
            if(getName().contains("abstract")) g.drawString("A", position.x+MyClass.PAD/2+8+6, adjy+position.y+MyClass.PAD/2+8);
        }

        // draw text
        
        // set the font
        if(isUML() && getName().contains("static"))
        {
            Map<TextAttribute, Integer> fontAttributes = new HashMap<TextAttribute, Integer>();
            fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            fontAttributes.put(TextAttribute.INPUT_METHOD_UNDERLINE, TextAttribute.UNDERLINE_LOW_GRAY);
            Font underline = new Font(g.getFont().getFamily(),g.getFont().getStyle(), Unimozer.DRAW_FONT_SIZE).deriveFont(fontAttributes);
            g.setFont(underline);
        }
        else
        {
            g.setFont(new Font(backupFont.getFontName(),getFontStyle(),Unimozer.DRAW_FONT_SIZE));
        }
        
        /* replaced by getFontStyle()
        if(getType()==CLASS)
        {
            if(getName().contains("abstract"))
                    g.setFont(new Font(backupFont.getFontName(),Font.ITALIC+Font.BOLD,Unimozer.DRAW_FONT_SIZE));
            else
                    g.setFont(new Font(backupFont.getFontName(),Font.BOLD,Unimozer.DRAW_FONT_SIZE));
        }
        else if (getType()==INTERFACE)
        {
            g.setFont(new Font(g.getFont().getFontName(),Font.ITALIC+Font.BOLD,Unimozer.DRAW_FONT_SIZE));
        }
        */
        
         
        if ((getType()==INTERFACE) || (getType()==CLASS))
        {
            int w = (int) g.getFont().getStringBounds(getPrintName(), g.getFontRenderContext()).getWidth();
            g.drawString(getPrintName(), position.x+(width-w)/2, position.y+height-MyClass.PAD/2-2);
        }
        else g.drawString(getPrintName(), position.x+MyClass.PAD/2+ICONSIZE, position.y+height-MyClass.PAD/2-2);
        

/*
        if(getType()==CLASS)
        {
            g.drawImage(img, position.x+MyClass.PAD/4, position.y+MyClass.PAD/2, null);
            adjx=6;
            adjy=4;
        }

        img = new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_default.png")).getImage();
        if(getFullName().contains("private")) img = new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_private.png")).getImage();
        else if(getFullName().contains("protected")) img = new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_protected.png")).getImage();
        else if(getFullName().contains("public")) img = new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_public.png")).getImage();
        g.drawImage(img, position.x+MyClass.PAD/4+adjx, position.y+MyClass.PAD/2+adjy, null);
*/

/*
        g.setColor(Color.BLACK);
        g.setFont(new Font(g.getFont().getFontName(),g.getFont().getStyle(),10));
        if(getName().contains("static")) g.drawString("S", position.x+MyClass.PAD/2+8, position.y+MyClass.PAD/2+8);
        if(getName().contains("final")) g.drawString("F", position.x+MyClass.PAD/2+8+6, position.y+MyClass.PAD/2+8);
        if(getName().contains("abstract")) g.drawString("A", position.x+MyClass.PAD/2+8+6, position.y+MyClass.PAD/2+8);
*/

        // reset the font
        g.setFont(new Font(backupFont.getFamily(),Font.PLAIN,Unimozer.DRAW_FONT_SIZE));
        g.setColor(backup);
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }
    
    public int getFontStyle()
    {
        int result = Font.PLAIN;

        if(getType()==CLASS)
        {
            if(getName().contains("abstract"))
                    result = Font.ITALIC+Font.BOLD;
            else
                    result = Font.BOLD;
        }
        else if (getType()==INTERFACE)
        {
            result = Font.ITALIC+Font.BOLD;
        }
                
        return result;
    }

    public String getPrintName()
    {
        String pname = new String(getUmlName());
        if(isUML==false) pname = new String(getName());
        if(pname.startsWith("private ")) pname=pname.substring(7).trim();
        if(pname.startsWith("protected ")) pname=pname.substring(9).trim();
        if(pname.startsWith("public ")) pname=pname.substring(6).trim();
        if(pname.startsWith("abstract ")) pname=pname.substring(8).trim();
        if(pname.startsWith("static ")) pname=pname.substring(6).trim();
        if(pname.startsWith("final ")) pname=pname.substring(5).trim();
        if(pname.startsWith("abstract ")) pname=pname.substring(8).trim();
        return pname;
    }

    public String getShortName()
    {
        String pname = new String(getName());
        if(pname.startsWith("private ")) pname=pname.substring(7).trim();
        if(pname.startsWith("protected ")) pname=pname.substring(9).trim();
        if(pname.startsWith("public ")) pname=pname.substring(6).trim();
        if(pname.startsWith("abstract ")) pname=pname.substring(8).trim();
        if(pname.startsWith("static ")) pname=pname.substring(6).trim();
        if(pname.startsWith("final ")) pname=pname.substring(5).trim();
        if(pname.startsWith("abstract ")) pname=pname.substring(8).trim();
        return pname;
    }

    public String getSimpleName()
    {
        return getShortName().substring(getShortName().indexOf(' ')+1);
    }

    public String getFullName()
    {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name)
    {
        this.name = name;
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
     * @return the selected
     */
    public boolean isSelected()
    {
        return selected;
    }

    /**
     * @param selected the selected to set
     */
    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }

    /**
     * @return the type
     */
    public int getType()
    {
        return type;
    }

    /**
     * @return the node
     */
    public BodyDeclaration getNode()
    {
        return node;
    }

    /**
     * @return the signature
     */
    public String getSignature()
    {
        return (signature==null?getName():signature);
    }

    /**
     * @param signature the signature to set
     */
    public void setSignature(String signature)
    {
        this.signature = signature;
    }

    /**
     * @return the params
     */
    public LinkedHashMap<String, String> getParams()
    {
        return params;
    }

    /**
     * @param params the params to set
     */
    public void setParams(LinkedHashMap<String, String> params)
    {
        this.params = params;
    }

    public String getJavaDoc()
    {
        if(node.getJavaDoc()!=null) return node.getJavaDoc().getContent();
        else return null;
    }

    /**
     * @return the umlName
     */
    public String getUmlName()
    {
        return umlName;
    }

    /**
     * @param umlName the umlName to set
     */
    public void setUmlName(String umlName)
    {
        this.umlName = umlName;
    }

    /**
     * @return the isUML
     */
    public boolean isUML()
    {
        return isUML;
    }

    /**
     * @param isUML the isUML to set
     */
    public void setUML(boolean isUML)
    {
        this.isUML = isUML;
    }


}
