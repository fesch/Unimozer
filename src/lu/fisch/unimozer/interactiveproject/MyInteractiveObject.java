/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lu.fisch.unimozer.interactiveproject;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.lang.reflect.Field;
import lu.fisch.unimozer.Diagram;
import lu.fisch.unimozer.MyObject;
import static lu.fisch.unimozer.MyObject.PADDING;

/**
 *
 * @author Ronny
 */
public class MyInteractiveObject extends MyObject {

    private String interfaceClass;

    public MyInteractiveObject(String className, Object object, Diagram diagram, String interfaceClass) {
        super(className, object, diagram);
        this.interfaceClass = interfaceClass;
    }

    public int paint(Graphics2D g, int x, int y, boolean isUML) {
        String className = interfaceClass;

        Color color = new Color(100, 255, 100);

        // determine max width an height
        // top
        String top = getName() + " : " + className;
        if (!isUML) {
            top = className + " " + getName();
        }
        g.setFont(new Font(g.getFont().getName(), Font.BOLD, g.getFont().getSize()));
        int topWidth = (int) g.getFont().getStringBounds(top, g.getFontRenderContext()).getWidth();
        int topHeight = (int) g.getFont().getStringBounds(top, g.getFontRenderContext()).getHeight() + 2 * PADDING;

        // init max
        int maxWidth = topWidth;
        int totalHeight = topHeight;

        Class c = getObject().getClass();
        totalHeight += PADDING;
        maxWidth += 2 * PADDING;

        // update position
        this.setPosition(new Point(x, y));
        this.setWidth(maxWidth);
        this.setHeight(totalHeight);

        // draw box
        g.setColor(color);
        g.fillRoundRect(x, y, maxWidth, totalHeight, 8, 8);
        g.setColor(Color.BLACK);
        g.drawRoundRect(x, y, maxWidth, totalHeight, 8, 8);

        // draw line
        g.drawLine(x, y + topHeight + PADDING, x + maxWidth, y + topHeight + PADDING);

        // draw title
        g.setColor(Color.BLACK);
        g.setFont(new Font(g.getFont().getName(), Font.BOLD, g.getFont().getSize()));
        g.drawString(top, x + PADDING, y + topHeight - PADDING);
        
        return maxWidth;
    }

}
