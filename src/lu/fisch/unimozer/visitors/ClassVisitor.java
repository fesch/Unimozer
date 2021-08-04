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

import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.visitor.VoidVisitorAdapter;
import java.util.Vector;
import lu.fisch.unimozer.Element;

/**
 *
 * @author robertfisch
 */
public class ClassVisitor extends VoidVisitorAdapter
{
    private String name;
    private Element ele;
    private boolean inter = false;
    private int classLine = -1;
    //private String extendsClass = "";

    public boolean isInterface()
    {
        return inter;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Object arg)
    {
        inter = n.isInterface();
        name = Modifier.toString(n.getModifiers())+n.getName();

        classLine=n.getBeginLine();

        ele = new Element(n, n.getParentNode());
        ele.setName(name);
        ele.setUmlName(name);
        /*if (n.getExtends()!=null)
        {
            extendsClass = n.getExtends().get(0).getName();
        }*/
    }

    public Vector<Element> getElements()
    {
        Vector<Element> vec = new Vector<Element>();
        vec.add(ele);
        return vec;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /*public String getExtendsClass()
    {
        return extendsClass;
    }*/

    public int getClassLine()
    {
        return classLine;
    }

}
