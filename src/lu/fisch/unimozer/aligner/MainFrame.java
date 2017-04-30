package lu.fisch.unimozer.aligner;

import javax.swing.JFrame;
import java.awt.Dimension;
/**
 * Write a description of class "MainFrame" here.
 * 
 * @author     robertfisch
 * @version    08/05/2012 19:41:06
 */
public class MainFrame extends JFrame
{
        private DrawPanel drawPanel;
    
	public MainFrame()
	{
		drawPanel = new DrawPanel();
		getContentPane().add(drawPanel);
		setSize(new Dimension(640,480));
	}
        
        public MainFrame(Grille grille)
        {
		drawPanel = new DrawPanel(grille);
		getContentPane().add(drawPanel);
		setSize(new Dimension(640,480));
                setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                show();
        }
        
        public void setSpace(Space space)
        {
            drawPanel.setSpace(space);
        }
	
	public static void main(String[] args)
	{
		MainFrame mainFrame = new MainFrame();
		mainFrame.show();
		mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
}