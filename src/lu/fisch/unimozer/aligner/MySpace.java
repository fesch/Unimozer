package lu.fisch.unimozer.aligner;

import java.awt.Graphics;
import java.awt.Color;
/**
 * Write a description of class "MySpace" here.
 * 
 * @author     robertfisch
 * @version    08/05/2012 20:41:33
 */
public class MySpace implements Space
{

	private int x;

	private int y;

	private int width;

	private int height;
	
	public MySpace(int x, int y, int width, int height)
	{
		this.x=x;
		this.y=y;
		this.width=width;
		this.height=height;
	}


	public int getX()
	{
		return x;
	}

	public void setX(int x)
	{
		this.x = x;
	}

	public int getY()
	{
		return y;
	}

	public void setY(int y)
	{
		this.y = y;
	}

	public int getWidth()
	{
		return width;
	}

	public void setWidth(int width)
	{
		this.width = width;
	}

	public int getHeight()
	{
		return height;
	}

	public void setHeight(int height)
	{
		this.height = height;
	}

	public void paint(Graphics g)
	{
		g.setColor(Color.cyan);
		g.fillRect(x, y, width, height);
		g.setColor(Color.black);
		g.drawRect(x, y, width, height);
	}
}