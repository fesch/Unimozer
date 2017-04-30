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

import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.body.VariableDeclaratorId;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.visitor.VoidVisitorAdapter;
import java.util.Vector;

/**
 *
 * @author robertfisch
 */
public class FieldChanger extends VoidVisitorAdapter
{
    FieldDeclaration node;
    String to;
    String type;
    int mods;

    public FieldChanger(FieldDeclaration node, String type, String to, int mods)
    {
        this.node=node;
        this.type=type;
        this.to=to;
        this.mods=mods;
    }

    @Override
    public void visit(FieldDeclaration n, Object arg)
    {
        if(n.equals(node))
        {
            // update type
            n.setType(new ClassOrInterfaceType(type));
            // update name
            Vector<VariableDeclarator> variables = new Vector<VariableDeclarator>();
            VariableDeclaratorId vdi = new VariableDeclaratorId(to);
            VariableDeclarator vd = new VariableDeclarator(vdi);
            variables.add(vd);
            n.setVariables(variables);
            // update modifier
            n.setModifiers(mods);
        }
    }

}
