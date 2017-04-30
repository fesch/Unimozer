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

/******************************************************************************************************
 *
 *      Author:         Christopher Bach 
 *
 *      Description:    A layout manager to control toolbar docking.
 *
 ******************************************************************************************************
 *
 *      Revision List
 *
 *      Author          Date			Description
 *      ------			----			-----------
 *      Bob Fisch       2007.12.12      First Issue
 *      Bob Fisch       2008.04.18      Modification for SOUTH element
 *
 ******************************************************************************************************
 *
 *      Comment:		I've found the code of this class over here:
 *						http://forum.java.sun.com/thread.jspa?threadID=439235&messageID=1986757
 *						(Bob Fisch)
 *
 ******************************************************************************************************///

import java.awt.*;
import java.util.*;
import javax.swing.*;


public class AKDockLayout extends BorderLayout
	{
		private ArrayList north = new ArrayList(1);
		private ArrayList south = new ArrayList(1);
		private ArrayList east = new ArrayList(1);
		private ArrayList west = new ArrayList(1);
		private Component center = null;
		private int northHeight, southHeight, eastWidth, westWidth;
		
		
		public static final int TOP = SwingConstants.TOP;
		public static final int BOTTOM = SwingConstants.BOTTOM;
		public static final int LEFT = SwingConstants.LEFT;
		public static final int RIGHT = SwingConstants.RIGHT;
		
		
		public void addLayoutComponent(Component c, Object con)
		{
			synchronized (c.getTreeLock())
			{
				if (con != null)
				{
					String s = con.toString();
					if (s.equals(NORTH))
					{
						north.add(c);
					}
					else if (s.equals(SOUTH))
					{
						south.add(c);
					}
					else if (s.equals(EAST))
					{
						east.add(c);
					}
					else if (s.equals(WEST))
					{
						west.add(c);
					}
					else if (s.equals(CENTER))
					{
						center = c;
					}
					
					c.getParent().validate();
				}
			}
		}
		
		
		public void removeLayoutComponent(Component c)
		{
			north.remove(c);
			south.remove(c);
			east.remove(c);
			west.remove(c);
			if (c == center)
				center = null;
			
			flipSeparators(c,SwingConstants.VERTICAL);
		}
		
		
		public void layoutContainer(Container target)
		{
			synchronized (target.getTreeLock())
			{
				Insets insets = target.getInsets();
				int top = insets.top;
				int bottom = target.getHeight() - insets.bottom;
				int left = insets.left;
				int right = target.getWidth() - insets.right;
				
				northHeight = getPreferredDimension(north).height;
				southHeight = getPreferredDimension(south).height;
				eastWidth = getPreferredDimension(east).width;
				westWidth = getPreferredDimension(west).width;
				
				
				placeComponents(target, north, left, top, right - left, northHeight,
								TOP);
				
				top += (northHeight + getVgap());
				
				placeComponents(target, south, left, bottom - southHeight,
								right - left, southHeight, BOTTOM);
				
				bottom -= (southHeight + getVgap());
				
				placeComponents(target, east, right - eastWidth, top, eastWidth,
								bottom - top, RIGHT);
				
				right -= (eastWidth + getHgap());
				
				placeComponents(target, west, left, top, westWidth, bottom - top, LEFT);
				
				left += (westWidth + getHgap());
				
				
				if (center != null)
				{
					center.setBounds(left, top, right - left, bottom - top);
				}
			}
		}
		
		
		
		// Returns the ideal width for a vertically oriented toolbar
		// and the ideal height for a horizontally oriented toolbar:
		private Dimension getPreferredDimension(ArrayList comps)
		{
			int w = 0, h = 0;
			
			for (int i = 0; i < comps.size(); i++)
			{
				Component c = (Component)(comps.get(i));
				Dimension d = c.getPreferredSize();
				w = Math.max(w, d.width);
				h = Math.max(h, d.height);
			}
			
			return new Dimension(w, h);
		}
		
		
		
		private void placeComponents(Container target, ArrayList comps,
									 int x, int y, int w, int h, int orientation)
		{
			int offset = 0;
			Component c = null;
			
			
			if (orientation == TOP || orientation == BOTTOM)
			{
				offset = x;
				int totalWidth = 0;
				
				for (int i = 0; i < comps.size(); i++)
				{
					c = (Component)(comps.get(i));
					flipSeparators(c, SwingConstants.VERTICAL);
					int cwidth = c.getPreferredSize().width;
					// modif BoB to make the component full width
					if (cwidth==0) {cwidth=w;};
					totalWidth += cwidth;
					
					if (w < totalWidth && i != 0)
					{
						offset = x;
						
						if (orientation == TOP)
						{
							y += h;
							northHeight += h;
						}
						
						else if (orientation == BOTTOM)
						{
							southHeight += h;
							y -= h;
						}
						totalWidth = cwidth;
					}
					
					c.setBounds(x + offset, y, cwidth, h);
					offset += cwidth;
				}
				
				flipSeparators(c, SwingConstants.VERTICAL);
			}
			
			else
			{
				int totalHeight = 0;
				
				for (int i = 0; i < comps.size(); i++)
				{
					c = (Component)(comps.get(i));
					int cheight = c.getPreferredSize().height;
					totalHeight += cheight;
					
					if (h < totalHeight && i != 0)
					{
						if (orientation == LEFT)
						{
							x += w;
							westWidth += w;
						}
						
						else if (orientation == RIGHT)
						{
							eastWidth += w;
							x -= w;
						}
						
						totalHeight = cheight;
						offset = 0;
					}
					
					if (totalHeight > h)
						cheight = h - 1;
					c.setBounds(x, y + offset, w, cheight);
					offset += cheight;
				}
				
				flipSeparators(c, SwingConstants.HORIZONTAL);
			}
		}
		
		
		
		
		
		
		
		
		private void flipSeparators(Component c, int orientn)
		{
			
			if (c != null && c instanceof JToolBar &&
		        UIManager.getLookAndFeel().getName().toLowerCase().indexOf("windows")
		        != -1)
			{
				JToolBar jtb = (JToolBar) c;
				Component comps[] = jtb.getComponents();
				
				if (comps != null && comps.length > 0)
				{
					for (int i = 0; i < comps.length; i++)
					{
						try
						{
							Component component = comps[i];
							
							if (component != null)
							{
								if (component instanceof JSeparator)
								{
									jtb.remove(component);
									JSeparator separ = new JSeparator();
									
									if (orientn == SwingConstants.VERTICAL)
									{
										separ.setOrientation(SwingConstants.VERTICAL);
										separ.setMinimumSize(new Dimension(2, 6));
										separ.setPreferredSize(new Dimension(2, 6));
										separ.setMaximumSize(new Dimension(2, 100));
									}
									
									else
									{
										separ.setOrientation(SwingConstants.HORIZONTAL);
										separ.setMinimumSize(new Dimension(6, 2));
										separ.setPreferredSize(new Dimension(6, 2));
										separ.setMaximumSize(new Dimension(100, 2));
									}
									
									jtb.add(separ, i);
								}
							}
						}
						
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				}
			}
		}
		
	}