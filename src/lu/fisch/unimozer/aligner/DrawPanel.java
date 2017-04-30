package lu.fisch.unimozer.aligner;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Color;
/**
 * Write a description of class "DrawPanel" here.
 * 
 * @author     robertfisch
 * @version    08/05/2012 19:41:19
 */
public class DrawPanel extends JPanel
{
	private Grille grille = new Grille();
	private MySpace space = null;

	public DrawPanel(Grille grille)
	{
            this.grille=grille;
        }
        
        public void setSpace(Space space)
        {
            this.space = new MySpace(space.getX(),space.getY(),space.getWidth(),space.getHeight());
        }

        public DrawPanel()
	{
		space = new MySpace(10,10,50,80);
		grille.addSpace(space);
		space = new MySpace(100,200,100,200);
		grille.addSpace(space);
		space = new MySpace(80,30,240,80);
		grille.addSpace(space);

		space = new MySpace(0,0,30,30);
		Area a = grille.findFreeAreaFor(space);
		space.setX(a.getX());
		space.setY(a.getY());
	}

	
	public void paintComponent(Graphics g)
	{
		g.setColor(Color.WHITE);
		g.fillRect(0,0,getWidth(),getHeight());	

		grille.paint(g,getWidth(),getHeight());
                if(space!=null)
                    space.paint(g);
	}
}