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

import japa.parser.ast.PackageDeclaration;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.QualifiedNameExpr;
import japa.parser.ast.visitor.VoidVisitorAdapter;
import lu.fisch.unimozer.Package;

/**
 *
 * @author robertfisch
 */
public class PackageVisitor extends VoidVisitorAdapter
{

    private String packagename = Package.DEFAULT;
    private boolean stop = true;

    public String getPackageName()
    {
        return packagename;
    }

    @Override
    public void visit(PackageDeclaration n, Object arg)
    {
        stop=false;
        packagename = "";
        n.getName().accept(this, arg);
        stop=true;
    }

    @Override
    public void visit(NameExpr n, Object arg)
    {
        if(!stop)
            packagename+=n.getName();
    }

    @Override
    public void visit(QualifiedNameExpr n, Object arg)
    {
        if(!stop)
        {
            n.getQualifier().accept(this, arg);
            packagename+="."+n.getName();
        }
    }


}
