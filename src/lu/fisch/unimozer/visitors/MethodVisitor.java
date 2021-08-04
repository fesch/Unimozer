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

import japa.parser.ast.body.*;
import japa.parser.ast.expr.ObjectCreationExpr;
import japa.parser.ast.visitor.VoidVisitorAdapter;
import java.util.List;
import java.util.Vector;
import lu.fisch.unimozer.Element;
import lu.fisch.unimozer.utils.StringList;

/**
 *
 * @author robertfisch
 */
public class MethodVisitor extends VoidVisitorAdapter
{
    private StringList methods = new StringList();
    private Vector<Element> vec = new Vector<Element>();
    //private Vector<Element> decs = new Vector<Element>();
    private String forClass;
    private boolean inClass=false;
    
    public MethodVisitor(String forClass)
    {
        this.forClass=forClass;
    }
    
    @Override
    public void visit(ObjectCreationExpr n, Object arg) {
        // ignore anonymous stuff
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
    public void visit(ConstructorDeclaration n, Object arg)
    {
        try
        {
           if(n.getBlock()!=null)
           {
            String content = ((StringList) arg).copyFrom(n.getBlock().getBeginLine()-1,
                                                           n.getBlock().getBeginColumn()-1,
                                                           n.getBlock().getEndLine()-1,
                                                           n.getBlock().getEndColumn()-1);
            //content=content.trim();
            if(content.startsWith("{")) content=content.substring(1);
            if(content.endsWith("}")) content=content.substring(0,content.length()-1);
            content=content.trim();
            n.setData(content);
           }
           else n.setData(null);
        }
        catch(Exception ex)
        {
           ex.printStackTrace();
        }
        catch (Error er)
        {
           er.printStackTrace();
        }

 
        Element ele = new Element(n);

        String uml = Modifier.toString(n.getModifiers())+n.getName()+"(";
        String full = Modifier.toString(n.getModifiers())+n.getName()+"(";
        String sign = n.getName()+"(";
        List<Parameter> pl = n.getParameters();
        if(pl!=null)
        for(Parameter p : pl)
        {
            //System.out.println(p.getId().getName()+" ==> "+p.getId().getArrayCount());
            ele.getParams().put(p.getId().getName()+(p.getId().getArrayCount()==0?"":"[]"), p.getType().toString());
            uml+=p.getId().getName()+(p.getId().getArrayCount()==0?"":"[]")+" : "+p.getType().toString()+", ";
            full+=p.getType().toString()+" "+p.getId().getName()+(p.getId().getArrayCount()==0?"":"[]")+", ";
            sign+=p.getType().toString()+", ";
        }
        if(full.charAt(full.length()-1)==' ')
        {
            uml=uml.substring(0,uml.length()-2);
            full=full.substring(0,full.length()-2);
            sign=sign.substring(0,sign.length()-2);
        }
        uml +=")";
        full +=")";
        sign +=")";
        getMethods().add(full);

        ele.setName(full);
        ele.setSignature(sign);
        ele.setUmlName(uml);
        vec.add(ele);
        //decs.add(ele);
    }

    @Override
    public void visit(MethodDeclaration n, Object arg)
    {
        try
        {
           if(n.getBody()!=null)
           {
            String content = ((StringList) arg).copyFrom(n.getBody().getBeginLine()-1,
                                                           n.getBody().getBeginColumn()-1,
                                                           n.getBody().getEndLine()-1,
                                                           n.getBody().getEndColumn()-1);
            /*String content2 = ((StringList) arg).copyFrom(n.getBeginLine()-1,
                                                           n.getBeginColumn()-1,
                                                           n.getEndLine()-1,
                                                           n.getEndColumn()-1);
            System.out.println(n.getName()+" : "+n.getBeginLine()+" - "+n.getBeginColumn()+" >> "+((StringList) arg).toLinearPos(n.getBeginLine()-1,n.getBeginColumn()-1)+" - "+content2);
           */
            //content=content.trim();
            if(content.startsWith("{")) content=content.substring(1);
            if(content.endsWith("}")) content=content.substring(0,content.length()-1);
            content=content.trim();
            n.setData(content);
           }
           else n.setData(null);
        }
        catch(Exception ex)
        {
           ex.printStackTrace();
        }
        catch (Error er)
        {
           er.printStackTrace();
        }

        Element ele = new Element(n);

        String uml = Modifier.toString(n.getModifiers())+n.getName()+"(";
        String full = Modifier.toString(n.getModifiers())+n.getType().toString()+" "+n.getName()+"(";
        String sign = n.getType().toString()+" "+n.getName()+"(";
        List<Parameter> pl = n.getParameters();
        if(pl!=null)
        for(Parameter p : pl)
        {
            //System.out.println(p.getId().getName()+" ==> "+p.getId().getArrayCount());
            ele.getParams().put(p.getId().getName()+(p.getId().getArrayCount()==0?"":"[]"), p.getType().toString());
            uml+=p.getId().getName()+(p.getId().getArrayCount()==0?"":"[]")+" : "+p.getType().toString()+", ";
            full+=p.getType().toString()+" "+p.getId().getName()+(p.getId().getArrayCount()==0?"":"[]")+", ";
            sign+=p.getType().toString()+", ";
        }
        if(full.charAt(full.length()-1)==' ')
        {
            uml=uml.substring(0,uml.length()-2);
            full=full.substring(0,full.length()-2);
            sign=sign.substring(0,sign.length()-2);
        }
        uml  +=") : "+n.getType().toString();
        full +=")";
        sign +=")";
        getMethods().add(full);

        ele.setUmlName(uml);
        ele.setName(full);
        ele.setSignature(sign);
        vec.add(ele);
    }

    public Vector<Element> getElements()
    {
        return vec;
    }
/*
    public Vector<Element> getConstructors()
    {
        return decs;
    }
*/
    /**
     * @return the methods
     */
    public StringList getMethods()
    {
        return methods;
    }

}
