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

import japa.parser.ASTParserTokenManager;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.visitor.VoidVisitorAdapter;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import lu.fisch.unimozer.Element;
import lu.fisch.unimozer.utils.StringList;

/**
 *
 * @author robertfisch
 */
public class FieldVisitor extends VoidVisitorAdapter
{
    private StringList fields = new StringList();
    private Vector<Element> vec = new Vector<Element>();
    private String forClass;
    private boolean inClass=false;
    
    public FieldVisitor(String forClass)
    {
        this.forClass=forClass;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Object arg) 
    {
        if(n.getName().equals(forClass))
        {
            inClass=true;
            if (n.getMembers() != null) {
                for (BodyDeclaration member : n.getMembers()) 
                {
                    member.accept(this, arg);
                }
            }
            inClass=false;
        }
    }
    
    @Override
    public void visit(FieldDeclaration n, Object arg)
    {
        List<VariableDeclarator> pl = n.getVariables();
        if(pl!=null)
        for(VariableDeclarator p : pl)
        {
            String uml = Modifier.toString(n.getModifiers());
            String full = Modifier.toString(n.getModifiers())+n.getType().toString();

            full+=" "+p.getId().getName()+"";
            uml+=" "+p.getId().getName()+"";
            
            if(full.charAt(full.length()-1)==',')
            {
                full=full.substring(0,full.length()-1);
                uml=uml.substring(0,uml.length()-1);
            }
            uml += " : "+n.getType().toString();
            fields.add(full);

            Element ele = new Element(n, n.getParentNode());
            ele.setName(full);
            ele.setUmlName(uml);
            vec.add(ele);
        }
    }

    public Vector<Element> getElements()
    {
        return vec;
    }

    /**
     * @return the methods
     */
    public StringList getFields()
    {
        return fields;
    }

}
