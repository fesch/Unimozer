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

import java.awt.EventQueue;

/**
 *
 * @author robertfisch
 */
public class CodeReader implements Runnable
{
    private Diagram diagram = null;
    private MyClass mc = null;
    private CodeCode code = null;
    private boolean isRunning = false;
    Thread runner = null;

    public CodeReader(Diagram diagram, MyClass mc, CodeCode code)
    {
        synchronized(this)
        {
            isRunning=false;
        }
        this.diagram=diagram;
        this.mc=mc;
        this.code=code;
    }

    private void update()
    {
        if(mc!=null && code!=null && diagram!=null)
        {
            try
            {
                String oldPName = mc.getPackagename();
                diagram.loadClassFromString(mc, code.getCode());
                String newPName = mc.getPackagename();
                
                if(!oldPName.equals(newPName))
                {
                    // we need to check if the stored file has to be moved
                    diagram.moveFile(mc,oldPName);
                }
                
                diagram.cleanAll();
                // compile on the fly
                if(Unimozer.javaCompileOnTheFly)
                {
                    //diagram.makeSilent();
                    //if (!code.getCode().trim().equals(""))
                    diagram.compile();
                }
                //diagram.clean(mc);
                diagram.repaint();
            }
            catch (Exception ex)
            {
                
            }
        }
        //System.err.println("done");
    }

    public void doUpdate()
    {
        //System.err.print("Request to update ... ");
        if(runner!=null)
        {
            if(!runner.isAlive() && isRunning==false)
            {
                newRunner();
            }
            else
            {
                //System.err.println("Update still in progress ...");
            }
        }
        else
        {
            newRunner();
        }
    }

    private void newRunner()
    {
        //System.err.println("yes");
        runner = new Thread(this, "CodeReader");
        synchronized(this)
        {
            isRunning=true;
        }
        runner.start();
        //EventQueue.invokeLater(this);
    }

    @Override
    public void run()
    {
        //System.err.print("Updating ... ");
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException ex) 
        {
        }
        update();
        synchronized(this) 
        { 
            isRunning=false; 
        }
    }
 
}
