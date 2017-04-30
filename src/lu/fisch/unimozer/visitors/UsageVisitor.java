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

import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.ObjectCreationExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.visitor.VoidVisitorAdapter;
import java.util.Iterator;
import lu.fisch.unimozer.Unimozer;
import lu.fisch.unimozer.utils.StringList;

/**
 *
 * @author robertfisch
 */
public class UsageVisitor extends VoidVisitorAdapter
{
    private StringList usedClasses = new StringList();

    @Override
    public void visit(ObjectCreationExpr n, Object arg)
    {
        /*
        List<Type> l = n.getType().getTypeArgs();
        if(l!=null)
            for(Type t : l)
            {
                usedClasses.addIfNew(t.toString());
            }
        */
        usedClasses.addIfNew(Unimozer.getTypesOf(n.getType().toString()));
    }

    @Override
    public void visit(Parameter n, Object arg)
    {
        usedClasses.addIfNew(Unimozer.getTypesOf(n.getType().toString()));
    }

    @Override
    public void visit(VariableDeclarationExpr n, Object arg)
    {
        usedClasses.addIfNew(Unimozer.getTypesOf(n.getType().toString()));
        
        for (Iterator<VariableDeclarator> i = n.getVars().iterator(); i.hasNext();) {
            VariableDeclarator v = i.next();
            v.accept(this, arg);
        }
        
    }  
    
    @Override
    public void visit(NameExpr n, Object arg) {
        usedClasses.addIfNew(Unimozer.getTypesOf(n.getName()));
    }
    
    
    public StringList getUesedClasses()
    {
        return usedClasses;
    }

}
