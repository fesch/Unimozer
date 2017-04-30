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

import java.io.File;
import lu.fisch.unimozer.Unimozer;

public class BlueJPackageFile
{
    private File dir;
    private File pkgFile;

    /**
     * @see PackageFileFactory
     */
    BlueJPackageFile(File dir)
    {
        this.dir = dir;
        this.pkgFile = new File(dir, Unimozer.B_PACKAGENAME);
    }

    @Override
    public String toString()
    {
        return "BlueJ package file in: " + dir.toString();
    }

    /**
     * Whether a BlueJ package file exists in this directory.
     */
    public static boolean exists(File dir)
    {
        if (dir == null)
            return false;

        // don't try to test Windows root directories (you'll get in
        // trouble with disks that are not in drives...).

        if (dir.getPath().endsWith(":\\"))
            return false;

        if (!dir.isDirectory())
            return false;

        File packageFile = new File(dir, Unimozer.B_PACKAGENAME);
        //System.out.println(packageFile.exists()+" => "+packageFile.getAbsolutePath());
        return packageFile.exists();
    }

    public static boolean isPackageFileName(String name)
    {
        return name.equals(Unimozer.B_PACKAGENAME);
    }
}
