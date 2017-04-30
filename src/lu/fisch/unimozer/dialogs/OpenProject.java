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

/**
 * Heavily modified version of:
 * program BlueJ
 * package bluej.utility
 * author  Michael Kolling
 * author  Axel Schmolitzky
 * author  Markus Ostman
 * version $Id: PackageChooser.java 6347 2009-05-20 15:22:43Z polle $ * version $Id: Terminal.java 6215 2009-03-30 13:28:25Z polle $
 */

/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 This file is subject to the Classpath exception as provided in the
 LICENSE.txt file that accompanied this code.
 */


package lu.fisch.unimozer.dialogs;


import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.util.Vector;

import javax.swing.*;
import javax.swing.plaf.FileChooserUI;
import javax.swing.plaf.basic.BasicFileChooserUI;
import lu.fisch.unimozer.Mainform;


public class OpenProject extends JFileChooser
{
    final Icon classIcon = new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_class.png"));
    final Icon packageIcon = new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/ico_turtle.png"));
    final Icon netbeansIcon = new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/ico_netbeans.png"));

  
    /**
     * Create a new PackageChooser.
     *
     * @param startDirectory 	the directory to start the package selection in.
     * @param preview           whether to show the package structure preview pane
     * @param showArchives      whether to allow choosing jar and zip files
     */
    public OpenProject(File startDirectory, boolean showArchives)
    {
        super(startDirectory);

        /* doesn't work as expected ! 
        RestrictedFileSystemView_1 fsv = new RestrictedFileSystemView_1(new File("c:/"));
        this.setFileSystemView(fsv);
         */

       
/*
        if (showArchives) {
            setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        }
        else {
            setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }*/
        //setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);


        setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        setFileView(new PackageFileView());

        /*
        if (System.getProperty("os.name").toLowerCase().indexOf( "win" ) >= 0)
        {
            Vector<String> drives = new Vector();
            drives.add("C:");
            drives.add("D:");
            hideVolumeSelector(this,drives);
        }
         */
        
        cleanField();
    }

    /* dat bréngt och nix ! */
    public void hideVolumeSelector( Container c, Vector<String> drives )
    {
        int len = c.getComponentCount();
        for (int i = 0; i < len; i++)
        {
            Component comp = c.getComponent(i);
            if (comp instanceof JComboBox)
            {
                JComboBox cb = (JComboBox)comp;
                // this is the box we want to modify
                if (cb.getParent().getParent().getClass().getName().equals("lu.fisch.unimozer.dialogs.OpenProject"))
                {
                    for(int j=cb.getItemCount()-1;j>=0;j--)
                    {
                        String name = cb.getItemAt(j).toString();
                        System.out.println("Checking ... "+name);
                        boolean remove = true;
                        for(int d=0;d<drives.size();d++)
                            if (name.toLowerCase().contains(""+drives.get(d).toCharArray()+"\\"))
                                remove=false;
                        if (remove==true)
                        {
                            System.out.println("Removing ... "+name);
                            cb.removeItemAt(j);
                        }
                    }
                }
            }
            else if (comp instanceof Container)
            {
                hideVolumeSelector((Container)comp, drives);
            }
        }
    }

    @Override
    public boolean accept(File f)
    {
        if (f.isDirectory())
        {
            return true;
        }
        else
            return false;
        /*
        String fname = f.getName();
        return fname.endsWith(".jar") || fname.endsWith(".JAR") ||
                fname.endsWith(".zip") || fname.endsWith(".ZIP");
         */
    }

    @Override
    public void setSelectedFile(File f)
    {
        String content = new String();
        String dir = new String();
        FileChooserUI myUi = getUI();
        super.setSelectedFile(f);
        if (myUi instanceof BasicFileChooserUI) {
            BasicFileChooserUI mui = (BasicFileChooserUI) myUi;
            dir = mui.getFileName();
            // assign to a file
            File fi = new File(dir);
            // only get the last part of the path
            content = fi.getName();
        }
        if (myUi instanceof BasicFileChooserUI) {
            BasicFileChooserUI mui = (BasicFileChooserUI) myUi;
            // only set if it's a Unimozer/BlueJ/NetBeans package
            File pDir = new File(dir);
            if ( PackageFile.exists(pDir) || BlueJPackageFile.exists(pDir) || NetBeansPackageFile.exists(pDir) )
                mui.setFileName(content);
            else
                mui.setFileName("");
        }
    }

    /**
     *  A directory was double-clicked. If this is a BlueJ package, consider
     *  this a package selection and accept it as the "Open" action, otherwise
     *  just traverse into the directory.
     */
    @Override
    public void setCurrentDirectory(File dir)   // redefined
    {
        // get the actual content of the fielname field
        String content = new String();
        FileChooserUI myUi = getUI();
        if (myUi instanceof BasicFileChooserUI) {
            BasicFileChooserUI mui = (BasicFileChooserUI) myUi;
            content = mui.getFileName();
             // assign to a file
            File fi = new File(content);
            // only get the last part of the path
            content = fi.getName();
       }

        if ( PackageFile.exists(dir) || BlueJPackageFile.exists(dir) || NetBeansPackageFile.exists(dir) )
        {
            setSelectedFile(dir);
            //System.out.println("Selecting: "+content);
            super.approveSelection();
        }
        else
        {
            super.setCurrentDirectory(dir);
            // reset the content of the filename field
            if (myUi instanceof BasicFileChooserUI) {
                BasicFileChooserUI mui = (BasicFileChooserUI) myUi;
                //System.out.println("Setting 2: "+content);
                mui.setFileName(content);
            }
        }
    }

    // clean entry
    private void cleanField()
    {
        FileChooserUI myUi = getUI();
        if (myUi instanceof BasicFileChooserUI) {
            BasicFileChooserUI mui = (BasicFileChooserUI) myUi;
            mui.setFileName("");
        }
    }

    /**
     * Approve the selection. We have this mainly so that derived classes
     * can call it...
     */
    protected void approved()
    {
        super.approveSelection();
    }

    public int showSaveDialog(Mainform frame, String string)
    {
        return this.showDialog(frame, string);
    }

}
