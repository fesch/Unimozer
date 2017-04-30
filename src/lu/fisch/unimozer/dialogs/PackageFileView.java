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


package lu.fisch.unimozer.dialogs;

import javax.swing.*;
import javax.swing.filechooser.*;
import java.io.File;

public class PackageFileView extends FileView
{
    final Icon packageIcon = new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/ico_turtle.png"));
    final Icon blueJIcon = new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/ico_bluej.png"));
    final Icon netBeansIcon = new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/ico_netbeans.png"));

    @Override
    public String getName(File f)
    {
        return null;
    }

    @Override
    public String getDescription(File f)
    {
        return null;
    }

    @Override
    public String getTypeDescription(File f)
    {
        return null;
    }

    @Override
    public Icon getIcon(File f)
    {
        if(PackageFile.exists(f))
            return packageIcon;
        else if(BlueJPackageFile.exists(f))
            return blueJIcon;
        else if(NetBeansPackageFile.exists(f))
            return netBeansIcon;
        else
            return null;
    }
}
