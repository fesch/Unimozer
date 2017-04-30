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

package lu.fisch.unimozer.visitors;

import japa.parser.ast.body.ModifierSet;

/**
 *
 * @author robertfisch
 */
public class Modifier
{
    public static String toString(int mod)
    {
        String full = "";
        if(ModifierSet.isPublic(mod)) full+="public ";
        if(ModifierSet.isProtected(mod)) full+="protected ";
        if(ModifierSet.isPrivate(mod)) full+="private ";
        if(ModifierSet.isAbstract(mod)) full+="abstract ";
        if(ModifierSet.isStatic(mod)) full+="static ";
        if(ModifierSet.isFinal(mod)) full+="final ";
        //if(full.length()!=0) full=full.substring(0,full.length()-1);
        return full;
    }
}
