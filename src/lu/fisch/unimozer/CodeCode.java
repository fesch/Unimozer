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
public class CodeCode
{
    private String code = "";

    /**
     * @return the code
     */
    public String getCode()
    {
        synchronized(this)
        {
            return code;
        }
    }

    /**
     * @param code the code to set
     */
    public void setCode(String code)
    {
        synchronized(this)
        {
            this.code = code;
        }
    }


}
