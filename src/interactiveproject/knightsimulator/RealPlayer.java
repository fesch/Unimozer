/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package interactiveproject.knightsimulator;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ronny
 */
public class RealPlayer implements Player{
    private int x;
    private int y;
    // 0:UP 1:RIGHT 2:DOWN 3:LEFT
    private int direction;
    //backreference to the board
    private Board board;
    BufferedImage img; 
    
    
    //constants
    private static final int UP = 0;
    private static final int RIGHT = 1;
    private static final int DOWN = 2;
    private static final int LEFT = 3;
    
    public RealPlayer(Board board)
    {
        this.board = board;
        direction = 1;
        try {
            img = ImageIO.read(getClass().getResourceAsStream("/interactiveproject/knightsimulator/images/knight.png"));
        } catch (IOException ex) {
            Logger.getLogger(RealPlayer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
    
    private void move(int dX, int dY)
    {
        if(x+dX <board.getCols() 
                && x+dX >0
                && y+dY<board.getRows() 
                && y+dY>0
                && !((board.getMap())[y+dY][x+dX].equals("#")))
        {
            x+=dX;
            y+=dY;
        }
            
    }
    public void move()
    {
        switch(direction) {
            case UP:
                move(0,-1);
                break;
            case RIGHT:
                move(1,0);
                break;  
            case DOWN:
                move(0,1);
                break;
            case LEFT:
                move(-1,0);
                break;
        }
    }
    public void turnClockwise()
    {
        direction = (direction+1)%4;
    }
    
    public boolean isAtGold() 
    {
        return board.getMap()[y][x].equals("x");
    }
    
    public void collect()
    {
        if(isAtGold())
        {
            board.getMap()[y][x]=".";
        }
    }
    
    public void draw(Graphics g, int offsetLeft, int offsetTop, int cellSize)
    {
        g.drawImage(img, offsetLeft+x*cellSize, offsetTop+y*cellSize, cellSize, cellSize, null);
    }
    
}
