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

/**
 *
 * @author robertfisch
 */
public class Java
{
    public static boolean isIdentifier(String name)
    {
          //System.err.println("Testing: "+test);
          return name.matches("[a-zA-Z_][a-zA-Z0-9_\\-]*") && !name.contains(" ");
          /*if (test == null || test.length() == 0)
          {
              return false;
          }

          if (!Character.isJavaIdentifierStart(test.charAt(0)) && test.charAt(0) != '_')
          {
              return false;
          }

          for (int i = 1; i < test.length(); i++)
          {
              if (
                      (!Character.isJavaIdentifierPart(test.charAt(i)) && test.charAt(i) != '_')
                      ||
                      test.charAt(i) == ' '
                 )
              {
                  return false;
              }
          }

          return true;*/
    }

    public static boolean isExtends(String name)
    {
          //System.err.println("Testing: "+test);
          return name.matches("[a-zA-Z_][a-zA-Z0-9_\\-\\.]*") && !name.contains(" ");
          /*if (test == null || test.length() == 0)
          {
              return false;
          }

          if (!Character.isJavaIdentifierStart(test.charAt(0)) && test.charAt(0) != '_')
          {
              return false;
          }

          for (int i = 1; i < test.length(); i++)
          {
              if (
                      (!Character.isJavaIdentifierPart(test.charAt(i)) && test.charAt(i) != '_')
                      ||
                      test.charAt(i) == ' '
                 )
              {
                  return false;
              }
          }

          return true;*/
    }

    public static boolean isIdentifierOrNull(String name)
    {
        //System.err.println("Testing: "+name);
        boolean ret = (name==null);
        if(name!=null) ret = isIdentifier(name);
        return ret;
    }

}
