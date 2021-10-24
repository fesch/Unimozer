/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package lu.fisch.unimozer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 *
 * @author robertfisch
 */
public class Package
{
    public static final int PADDING = 60;
    public static final int PAD = 4;

    public static final String DEFAULT = "<default>";

    private int top;
    private int left;
    private int width;
    private int height;

    private String name;

    private Color border     = Color.BLACK;
    private Color background = new Color(210,205,188);
    private Color background2 = new Color(255,223,173);

    private Point nameZoneTop = new Point();
    private Point nameZoneBottom = new Point();

    public Package(String name, int top, int left, int width, int height)
    {
        this.name=name;
        this.left=left;
        this.top=top;
        this.width=width;
        this.height=height;
    }

    public boolean isInside(Point pt)
    {
        /*return (nameZoneTop.x<=pt.x && pt.x<=nameZoneBottom.x &&
                nameZoneTop.y<=pt.y && pt.y<=nameZoneBottom.y);*/
        return (getLeftAbs()<=pt.x && pt.x<=getRightAbs() &&
                getTopAbs()<=pt.y && pt.y<=getBottomAbs());
    }

    public void draw(Graphics graphics)
    {
        Graphics2D g = (Graphics2D) graphics;

        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

        // background
        g.setColor(background);
        g.fillRoundRect(getLeft()-PADDING,getTop()-PADDING,getWidth()+2*PADDING,getHeight()+2*PADDING,PADDING/2,PADDING/2);

        // line around
        g.setColor(border);
        g.drawRoundRect(getLeft()-PADDING,getTop()-PADDING,getWidth()+2*PADDING,getHeight()+2*PADDING,PADDING/2,PADDING/2);

        // size of name
        String oldFontName = g.getFont().getFamily();
        g.setFont(new Font("Courier",Font.BOLD,Unimozer.DRAW_FONT_SIZE+1));
        int h = (int) g.getFont().getStringBounds(getName(), g.getFontRenderContext()).getHeight()+2*PAD;
        int w = (int) g.getFont().getStringBounds(getName(), g.getFontRenderContext()).getWidth()+2*PAD;

        // background
        g.setColor(background2);
        g.fillRect(getLeft()-PADDING,getTop()-PADDING,w,h);
        // line around
        g.setColor(border);
        g.drawRect(getLeft()-PADDING,getTop()-PADDING,w,h);

        nameZoneTop = new Point(getLeft()-PADDING,getTop()-PADDING);
        nameZoneBottom = new Point(getLeft()-PADDING+w,getTop()-PADDING+h);

        // text
        g.drawString(getName(), getLeft()-PADDING+PAD,getTop()-PADDING-2*PAD+h);

        g.setFont(new Font(oldFontName,Font.BOLD+Font.ITALIC,Unimozer.DRAW_FONT_SIZE));
    }

    @Override
    public String toString()
    {
        return "["+left+","+top+","+width+","+height+"]";
    }


    public void setName(String name)
    {
        this.name=name;
    }

    public String getName()
    {
        return name;
    }

    public void setRight(int right)
    {
        width=right-left;
    }

    public void setBottom(int bottom)
    {
        height=bottom-top;
    }

    public int getRight()
    {
        return left+width;
    }

    public int getRightAbs()
    {
        return left+width+PADDING;
    }

    public int getBottom()
    {
        return top+height;
    }

    public int getBottomAbs()
    {
        return top+height+PADDING;
    }

    /**
     * @return the top
     */
    public int getTop()
    {
        return top;
    }

    public int getTopAbs()
    {
        BufferedImage img = new BufferedImage(1,1,BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = (Graphics2D) img.getGraphics();
        String oldFontName = g.getFont().getFamily();
        g.setFont(new Font("Courier",Font.BOLD,Unimozer.DRAW_FONT_SIZE+1));
        int h = (int) g.getFont().getStringBounds(getName(), g.getFontRenderContext()).getHeight()+2*PAD;
        
        return getTop()-PADDING/2-h;
    }

    /**
     * @param top the top to set
     */
    public void setTop(int top)
    {
        height+=this.top-top;
        this.top = top;
    }

    /**
     * @return the left
     */
    public int getLeft()
    {
        return left;
    }

    public int getLeftAbs()
    {
        return left-PADDING;
    }

    /**
     * @param left the left to set
     */
    public void setLeft(int left)
    {
        width+=this.left-left;
        this.left = left;
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
    
    public String getRelativeName(Package parent)
    {
        String nThis = this.getName();
        String nThat = parent.getName();
        
        /*if(nThat.equals("<default>")) 
        {
            nThat="";
        }*/
        
        if(nThis.equals("<default>")) 
        {
            nThis="";
        }
        if(nThat.equals("<default>")) 
        {
            nThat="";
        }

        /*
        System.out.println("Getting the relative name from: "+nThis+" to it's parent: "+nThat);
        System.out.println("Is this containted in parent? "+parent.contains(this));
        System.out.println("The relative name = "+(nThat.equals("")?nThis.substring(nThat.length()):nThis.substring(nThat.length()+1)));
        */
        
        if (parent.contains(this)) 
        {
               String end =nThis.substring(nThat.length()+1);
               if(nThat.equals("")) end=nThis.substring(nThat.length());
               
               if (end.contains(".")) end = end.substring(0,end.indexOf("."));
               return end;
        }
        else return "";
    }
    
    public boolean contains(Package other)
    {
        boolean result = true;

        if (other==null) return false;
        
        String nThis = this.getName();
        String nThat = other.getName();
        
        if(nThis.equals("<default>")) 
        {
            nThis="";
        }
        if(nThat.equals("<default>")) 
        {
            nThat="";
        }
        
        if (nThis.length()>=nThat.length()) return false;
        if (!nThat.startsWith(nThis)) return false;
        
        String end = nThat.substring(nThis.length()+1);

        if(nThis.equals("")) end= nThat.substring(nThis.length());
        
        
        //if(end.contains(".")) return false;
        
        return result;
    }

}
