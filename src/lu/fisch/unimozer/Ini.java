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
 *      Author:         Bob Fisch
 *
 *      Description:    This class manages entries in the INI-file
 *
 ******************************************************************************************************
 *
 *      Revision List
 *
 *      Author          Date			Description
 *      ------			----			-----------
 *      Bob Fisch       2008.05.02      First Issue
 *
 ******************************************************************************************************
 *
 *      Comment:		
 *
 ******************************************************************************************************///

import java.io.*;
import java.util.*;

public class Ini {

	private static String dirname = "";
	private static String ininame = "unimozer.ini";
	private static String filename = "";
	private static File dir = new File(dirname);
	private static File file = new File(filename);
	private static Properties p = new Properties();
	private static Ini ini = null;


        public static String getDirname()
        {
            // mac
            if(System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0)
            {
                return System.getProperty("user.home")+"/Library/Application Support/Unimozer";
            }
            // windows
            else if (System.getProperty("os.name").toLowerCase().indexOf( "win" ) >= 0)
            {
                String appData = System.getenv("APPDATA");
                if(appData!=null)
                    if (!appData.equals(""))
                    {
                        return appData+"\\Unimozer";
                    }
                return System.getProperty("user.home") + "\\Application Data\\Unimozer";
            }
            else
                return System.getProperty("user.home")+System.getProperty("file.separator")+".unimozer";
        }

        public static void set(String key, String value)
        {
            Ini ini = Ini.getInstance();
            try
            {
                ini.load();
                ini.setProperty(key, value);
                ini.save();
            }
            catch (Exception ex)
            {
                // ignore any exception
            }
        }


        public static String get(String key, String defaultValue)
        {
            Ini ini = Ini.getInstance();
            try
            {
                ini.load();
                return ini.getProperty(key, defaultValue);
            }
            catch (Exception ex)
            {
                // ignore any exception
            }
            return null;
        }

	public static Ini getInstance()
	{
            try
            {
                dirname = getDirname(); //System.getProperty("user.home")+System.getProperty("file.separator")+".unimozer";
                filename = dirname+System.getProperty("file.separator")+ininame;
                dir = new File(dirname);
                file = new File(filename);
            }
            catch(Error e)
            {
                e.printStackTrace();
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

            if (ini==null)
            {
                    ini = new Ini();
            }
            return ini;
	}
	
	public void load() throws FileNotFoundException, IOException
	{
		File f = new File(filename);
		if(f.length()==0)
		{
			//System.out.println("File is empty!");
		}
		else
		{
			//p.loadFromXML(new FileInputStream(filename));
			p.load(new FileInputStream(filename));
		}
	}
	
	public void save() throws FileNotFoundException, IOException
	{
		/*OutputStream os = new FileOutputStream(filename);
		p.storeToXML(os, "last updated " + new java.util.Date());
		os.close();
		*/
		//p.storeToXML(new FileOutputStream(filename), "last updated " + new java.util.Date());
		p.store(new FileOutputStream(filename), "last updated " + new java.util.Date());
	}

	public String getProperty(String _name, String _default)
	{
		if (p.getProperty(_name)==null)
		{
			return _default;
		}
		else
		{
			return p.getProperty(_name);
		}
	}
	
	public void setProperty(String _name, String _value)
	{
		p.setProperty(_name,_value);
	}
	
	public Set keySet()
	{
		return p.keySet();
	}

	private Ini()
	{
            try
            {
		if(!dir.exists()) 
		{
			dir.mkdir();
		}
		
		if(!file.exists())
		{
			try
			{	
				File predefined = new File("unimozer.ini");
                                if(predefined.exists())
                                    p.load(new FileInputStream(predefined.getAbsolutePath()));
                                //setProperty("dummy","dummy");
				save();
			}
			catch (Exception e) 
			{
				e.printStackTrace();
				System.out.println(e.getMessage());
			}
		}
            }
            catch(Error e)
            {
                System.out.println(e.getMessage());
            }
            catch(Exception e)
            {
                System.out.println(e.getMessage());
            }
	}

}
