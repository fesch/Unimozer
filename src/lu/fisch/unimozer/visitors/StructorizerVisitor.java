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

import japa.parser.ast.visitor.*;
import japa.parser.ast.BlockComment;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.ImportDeclaration;
import japa.parser.ast.LineComment;
import japa.parser.ast.TypeParameter;
import japa.parser.ast.body.AnnotationDeclaration;
import japa.parser.ast.body.AnnotationMemberDeclaration;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.EmptyMemberDeclaration;
import japa.parser.ast.body.EmptyTypeDeclaration;
import japa.parser.ast.body.EnumConstantDeclaration;
import japa.parser.ast.body.EnumDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.InitializerDeclaration;
import japa.parser.ast.body.JavadocComment;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.ModifierSet;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.body.VariableDeclaratorId;
import japa.parser.ast.expr.AnnotationExpr;
import japa.parser.ast.expr.ArrayAccessExpr;
import japa.parser.ast.expr.ArrayCreationExpr;
import japa.parser.ast.expr.ArrayInitializerExpr;
import japa.parser.ast.expr.AssignExpr;
import japa.parser.ast.expr.BinaryExpr;
import japa.parser.ast.expr.BooleanLiteralExpr;
import japa.parser.ast.expr.CastExpr;
import japa.parser.ast.expr.CharLiteralExpr;
import japa.parser.ast.expr.ClassExpr;
import japa.parser.ast.expr.ConditionalExpr;
import japa.parser.ast.expr.DoubleLiteralExpr;
import japa.parser.ast.expr.EnclosedExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.FieldAccessExpr;
import japa.parser.ast.expr.InstanceOfExpr;
import japa.parser.ast.expr.IntegerLiteralExpr;
import japa.parser.ast.expr.IntegerLiteralMinValueExpr;
import japa.parser.ast.expr.LongLiteralExpr;
import japa.parser.ast.expr.LongLiteralMinValueExpr;
import japa.parser.ast.expr.MarkerAnnotationExpr;
import japa.parser.ast.expr.MemberValuePair;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.NormalAnnotationExpr;
import japa.parser.ast.expr.NullLiteralExpr;
import japa.parser.ast.expr.ObjectCreationExpr;
import japa.parser.ast.expr.QualifiedNameExpr;
import japa.parser.ast.expr.SingleMemberAnnotationExpr;
import japa.parser.ast.expr.StringLiteralExpr;
import japa.parser.ast.expr.SuperExpr;
import japa.parser.ast.expr.ThisExpr;
import japa.parser.ast.expr.UnaryExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.stmt.AssertStmt;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.BreakStmt;
import japa.parser.ast.stmt.CatchClause;
import japa.parser.ast.stmt.ContinueStmt;
import japa.parser.ast.stmt.DoStmt;
import japa.parser.ast.stmt.EmptyStmt;
import japa.parser.ast.stmt.ExplicitConstructorInvocationStmt;
import japa.parser.ast.stmt.ExpressionStmt;
import japa.parser.ast.stmt.ForStmt;
import japa.parser.ast.stmt.ForeachStmt;
import japa.parser.ast.stmt.IfStmt;
import japa.parser.ast.stmt.LabeledStmt;
import japa.parser.ast.stmt.ReturnStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.stmt.SwitchEntryStmt;
import japa.parser.ast.stmt.SwitchStmt;
import japa.parser.ast.stmt.SynchronizedStmt;
import japa.parser.ast.stmt.ThrowStmt;
import japa.parser.ast.stmt.TryStmt;
import japa.parser.ast.stmt.TypeDeclarationStmt;
import japa.parser.ast.stmt.WhileStmt;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.PrimitiveType;
import japa.parser.ast.type.ReferenceType;
import japa.parser.ast.type.Type;
import japa.parser.ast.type.VoidType;
import japa.parser.ast.type.WildcardType;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;

import java.util.Vector;
import lu.fisch.structorizer.elements.Alternative;
import lu.fisch.structorizer.elements.Element;
import lu.fisch.structorizer.elements.For;
import lu.fisch.structorizer.elements.Instruction;
import lu.fisch.structorizer.elements.Repeat;
import lu.fisch.structorizer.elements.Root;
import lu.fisch.structorizer.elements.Subqueue;
import lu.fisch.structorizer.elements.While;
import lu.fisch.utils.StringList;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaUIBackgroundDrawer;


public class StructorizerVisitor extends VoidVisitorAdapter
{
    public Root root = null;
    String signature = null;
    Element lastElement = null;
    Vector<Subqueue> queueList = null;
    String code = "";

    public StructorizerVisitor(String signature)
    {
        root = new Root();
        root.setProgram(false);
        lastElement = root;
        queueList = new Vector<Subqueue>();
        queueList.add(root.children);
        this.signature = signature;
    }

    private String doReplacements(String code)
    {

        /*
        code = code.replaceAll("=", "<-");
        code = code.replaceAll("=<-", "==");
        */

        return code;
    }

    private class SourcePrinter {

        private int level = 0;

        private boolean indented = false;

        private final StringBuilder buf = new StringBuilder();

        public void indent() {
            level++;
        }

        public void unindent() {
            level--;
        }

        private void makeIndent() {
            for (int i = 0; i < level; i++) {
                buf.append("    ");
            }
        }

        public void print(String arg) {
            if (!indented) {
                makeIndent();
                indented = true;
            }
            buf.append(arg);
        }

        public void printLn(String arg) {
            print(arg);
            printLn();
        }

        public void printLn() {
            buf.append("\n");
            indented = false;
        }

        public String getSource() {
            return buf.toString();
        }

        @Override
        public String toString() {
            return getSource();
        }
    }

    private final SourcePrinter printer = new SourcePrinter();

    public String getSource() {
        return printer.getSource();
    }

    private void printModifiers(int modifiers) {
        if (ModifierSet.isPrivate(modifiers)) {
            printer.print("private ");
        }
        if (ModifierSet.isProtected(modifiers)) {
            printer.print("protected ");
        }
        if (ModifierSet.isPublic(modifiers)) {
            printer.print("public ");
        }
        if (ModifierSet.isAbstract(modifiers)) {
            printer.print("abstract ");
        }
        if (ModifierSet.isStatic(modifiers)) {
            printer.print("static ");
        }
        if (ModifierSet.isFinal(modifiers)) {
            printer.print("final ");
        }
        if (ModifierSet.isNative(modifiers)) {
            printer.print("native ");
        }
        if (ModifierSet.isStrictfp(modifiers)) {
            printer.print("strictfp ");
        }
        if (ModifierSet.isSynchronized(modifiers)) {
            printer.print("synchronized ");
        }
        if (ModifierSet.isTransient(modifiers)) {
            printer.print("transient ");
        }
        if (ModifierSet.isVolatile(modifiers)) {
            printer.print("volatile ");
        }
    }

    private void printMembers(List<BodyDeclaration> members, Object arg) {
        for (BodyDeclaration member : members) {
            printer.printLn();
            member.accept(this, arg);
            printer.printLn();
        }
    }

    private void printMemberAnnotations(List<AnnotationExpr> annotations, Object arg) {
        if (annotations != null) {
            for (AnnotationExpr a : annotations) {
                a.accept(this, arg);
                printer.printLn();
            }
        }
    }

    private void printAnnotations(List<AnnotationExpr> annotations, Object arg) {
        if (annotations != null) {
            for (AnnotationExpr a : annotations) {
                a.accept(this, arg);
                printer.print(" ");
            }
        }
    }

    private void printTypeArgs(List<Type> args, Object arg) {
        if (args != null) {
            printer.print("<");
            for (Iterator<Type> i = args.iterator(); i.hasNext();) {
                Type t = i.next();
                t.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
            printer.print(">");
        }
    }

    private void printTypeParameters(List<TypeParameter> args, Object arg) {
        if (args != null) {
            printer.print("<");
            for (Iterator<TypeParameter> i = args.iterator(); i.hasNext();) {
                TypeParameter t = i.next();
                t.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
            printer.print(">");
        }
    }

    private void printArguments(List<Expression> args, Object arg) {
        printer.print("(");
        if (args != null) {
            for (Iterator<Expression> i = args.iterator(); i.hasNext();) {
                Expression e = i.next();
                e.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print(")");
    }

    private void printJavadoc(JavadocComment javadoc, Object arg) {
        if (javadoc != null) {
            javadoc.accept(this, arg);
        }
    }

    public void visit(CompilationUnit n, Object arg) {
        if (n.getPackage() != null) {
            n.getPackage().accept(this, arg);
        }
        if (n.getImports() != null) {
            for (ImportDeclaration i : n.getImports()) {
                i.accept(this, arg);
            }
            printer.printLn();
        }
        if (n.getTypes() != null) {
            for (Iterator<TypeDeclaration> i = n.getTypes().iterator(); i.hasNext();) {
                i.next().accept(this, arg);
                printer.printLn();
                if (i.hasNext()) {
                    printer.printLn();
                }
            }
        }
    }

    public void visit(NameExpr n, Object arg)
    {
        printer.print(n.getName());
    }

    public void visit(QualifiedNameExpr n, Object arg) {
        n.getQualifier().accept(this, arg);
        printer.print(".");
        printer.print(n.getName());
    }

    public void visit(ClassOrInterfaceDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        if (n.isInterface()) {
            printer.print("interface ");
        } else {
            printer.print("class ");
        }

        printer.print(n.getName());

        printTypeParameters(n.getTypeParameters(), arg);

        if (n.getExtends() != null) {
            printer.print(" extends ");
            for (Iterator<ClassOrInterfaceType> i = n.getExtends().iterator(); i.hasNext();) {
                ClassOrInterfaceType c = i.next();
                c.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }

        if (n.getImplements() != null) {
            printer.print(" implements ");
            for (Iterator<ClassOrInterfaceType> i = n.getImplements().iterator(); i.hasNext();) {
                ClassOrInterfaceType c = i.next();
                c.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }

        printer.printLn(); printer.printLn("{");
        printer.indent();
        if (n.getMembers() != null) {
            printMembers(n.getMembers(), arg);
        }
        printer.unindent();
        printer.print("}");
    }

    public void visit(EmptyTypeDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        printer.print(";");
    }

    public void visit(JavadocComment n, Object arg)
    {
        /*
        if (lastElement!=null)
        {
            lastElement.getText().add("/**");
            lastElement.getText().add(n.getContent());
            //lastElement.getText().add("* /");
        }
        */
    }

    public void visit(ClassOrInterfaceType n, Object arg) {
        if (n.getScope() != null) {
            n.getScope().accept(this, arg);
            printer.print(".");
        }
        printer.print(n.getName());
        printTypeArgs(n.getTypeArgs(), arg);
    }

    public void visit(TypeParameter n, Object arg) {
        printer.print(n.getName());
        if (n.getTypeBound() != null) {
            printer.print(" extends ");
            for (Iterator<ClassOrInterfaceType> i = n.getTypeBound().iterator(); i.hasNext();) {
                ClassOrInterfaceType c = i.next();
                c.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(" & ");
                }
            }
        }
    }

    public void visit(PrimitiveType n, Object arg) {
        switch (n.getType()) {
            case Boolean:
                printer.print("boolean");
                break;
            case Byte:
                printer.print("byte");
                break;
            case Char:
                printer.print("char");
                break;
            case Double:
                printer.print("double");
                break;
            case Float:
                printer.print("float");
                break;
            case Int:
                printer.print("int");
                break;
            case Long:
                printer.print("long");
                break;
            case Short:
                printer.print("short");
                break;
        }
    }

    public void visit(ReferenceType n, Object arg) {
        n.getType().accept(this, arg);
        for (int i = 0; i < n.getArrayCount(); i++) {
            printer.print("[]");
        }
    }

    public void visit(WildcardType n, Object arg) {
        printer.print("?");
        if (n.getExtends() != null) {
            printer.print(" extends ");
            n.getExtends().accept(this, arg);
        }
        if (n.getSuper() != null) {
            printer.print(" super ");
            n.getSuper().accept(this, arg);
        }
    }

    public void visit(FieldDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());
        n.getType().accept(this, arg);

        printer.print(" ");
        for (Iterator<VariableDeclarator> i = n.getVariables().iterator(); i.hasNext();) {
            VariableDeclarator var = i.next();
            var.accept(this, arg);
            if (i.hasNext()) {
                printer.print(", ");
            }
        }

        printer.print(";");
    }

    public void visit(VariableDeclarator n, Object arg) {
        n.getId().accept(this, arg);
        if (n.getInit() != null) {
            printer.print(" = ");
            n.getInit().accept(this, arg);
        }
    }

    public void visit(VariableDeclaratorId n, Object arg) {
        printer.print(n.getName());
        for (int i = 0; i < n.getArrayCount(); i++) {
            printer.print("[]");
        }
    }

    public void visit(ArrayInitializerExpr n, Object arg) {
        printer.print("{");
        if (n.getValues() != null) {
            printer.print(" ");
            for (Iterator<Expression> i = n.getValues().iterator(); i.hasNext();) {
                Expression expr = i.next();
                expr.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
            printer.print(" ");
        }
        printer.print("}");
    }

    public void visit(VoidType n, Object arg) {
        printer.print("void");
    }

    public void visit(ArrayAccessExpr n, Object arg) {
        n.getName().accept(this, arg);
        printer.print("[");
        n.getIndex().accept(this, arg);
        printer.print("]");
    }

    public void visit(ArrayCreationExpr n, Object arg) {
        printer.print("new ");
        n.getType().accept(this, arg);

        if (n.getDimensions() != null) {
            for (Expression dim : n.getDimensions()) {
                printer.print("[");
                dim.accept(this, arg);
                printer.print("]");
            }
            for (int i = 0; i < n.getArrayCount(); i++) {
                printer.print("[]");
            }
        } else {
            for (int i = 0; i < n.getArrayCount(); i++) {
                printer.print("[]");
            }
            printer.print(" ");
            n.getInitializer().accept(this, arg);
        }
    }

    public void visit(AssignExpr n, Object arg) 
    {
        n.getTarget().accept(this, arg);
        printer.print(" ");
        switch (n.getOperator()) {
            case assign:
                printer.print(" <- ");
                break;
            case and:
                printer.print("&=");
                break;
            case or:
                printer.print("|=");
                break;
            case xor:
                printer.print("^=");
                break;
            case plus:
                printer.print("+=");
                break;
            case minus:
                printer.print("-=");
                break;
            case rem:
                printer.print("%=");
                break;
            case slash:
                printer.print("/=");
                break;
            case star:
                printer.print("*=");
                break;
            case lShift:
                printer.print("<<=");
                break;
            case rSignedShift:
                printer.print(">>=");
                break;
            case rUnsignedShift:
                printer.print(">>>=");
                break;
        }
        printer.print(" ");
        n.getValue().accept(this, arg);
    }

    public void visit(BinaryExpr n, Object arg) {
        n.getLeft().accept(this, arg);
        printer.print(" ");
        switch (n.getOperator()) {
            case or:
                printer.print(" ou ");
                break;
            case and:
                printer.print(" et ");
                break;
            case binOr:
                printer.print("|");
                break;
            case binAnd:
                printer.print("&");
                break;
            case xor:
                printer.print("^");
                break;
            case equals:
                printer.print("==");
                break;
            case notEquals:
                printer.print("!=");
                break;
            case less:
                printer.print("<");
                break;
            case greater:
                printer.print(">");
                break;
            case lessEquals:
                printer.print("<=");
                break;
            case greaterEquals:
                printer.print(">=");
                break;
            case lShift:
                printer.print("<<");
                break;
            case rSignedShift:
                printer.print(">>");
                break;
            case rUnsignedShift:
                printer.print(">>>");
                break;
            case plus:
                printer.print("+");
                break;
            case minus:
                printer.print("-");
                break;
            case times:
                printer.print("*");
                break;
            case divide:
                printer.print("/");
                break;
            case remainder:
                printer.print("%");
                break;
        }
        printer.print(" ");
        n.getRight().accept(this, arg);
    }

    public void visit(CastExpr n, Object arg) {
        printer.print("(");
        n.getType().accept(this, arg);
        printer.print(") ");
        n.getExpr().accept(this, arg);
    }

    public void visit(ClassExpr n, Object arg) {
        n.getType().accept(this, arg);
        printer.print(".class");
    }

    public void visit(ConditionalExpr n, Object arg) {
        n.getCondition().accept(this, arg);
        printer.print(" ? ");
        n.getThenExpr().accept(this, arg);
        printer.print(" : ");
        n.getElseExpr().accept(this, arg);
    }

    public void visit(EnclosedExpr n, Object arg) {
        printer.print("(");
        n.getInner().accept(this, arg);
        printer.print(")");
    }

    public void visit(FieldAccessExpr n, Object arg) {
        n.getScope().accept(this, arg);
        printer.print(".");
        printer.print(n.getField());
    }

    public void visit(InstanceOfExpr n, Object arg) {
        n.getExpr().accept(this, arg);
        printer.print(" instanceof ");
        n.getType().accept(this, arg);
    }

    public void visit(CharLiteralExpr n, Object arg) {
        printer.print("'");
        printer.print(n.getValue());
        printer.print("'");
    }

    public void visit(DoubleLiteralExpr n, Object arg) {
        printer.print(n.getValue());
    }

    public void visit(IntegerLiteralExpr n, Object arg) {
        printer.print(n.getValue());
    }

    public void visit(LongLiteralExpr n, Object arg) {
        printer.print(n.getValue());
    }

    public void visit(IntegerLiteralMinValueExpr n, Object arg) {
        printer.print(n.getValue());
    }

    public void visit(LongLiteralMinValueExpr n, Object arg) {
        printer.print(n.getValue());
    }

    public void visit(StringLiteralExpr n, Object arg) {
        printer.print("\"");
        printer.print(n.getValue());
        printer.print("\"");
    }

    public void visit(BooleanLiteralExpr n, Object arg) {
        printer.print(String.valueOf(n.getValue()));
    }

    public void visit(NullLiteralExpr n, Object arg) {
        printer.print("null");
    }

    public void visit(ThisExpr n, Object arg) {
        if (n.getClassExpr() != null) {
            n.getClassExpr().accept(this, arg);
            printer.print(".");
        }
        printer.print("this");
    }

    public void visit(SuperExpr n, Object arg)
    {
        /*
        System.out.println(n.toString());
        String code = n.toString().trim();
        code = doReplacements(code);
        if(code.endsWith(";")) code=code.substring(0,code.length()-1);
        Instruction ele = new Instruction(code);
        lastElement = ele;
        queueList.get(queueList.size()-1).addElement(ele);
         */
    }

    public void visit(MethodCallExpr n, Object arg) {
        if (n.getScope() != null) {
            n.getScope().accept(this, arg);
            printer.print(".");
        }
        printTypeArgs(n.getTypeArgs(), arg);
        printer.print(n.getName());
        printArguments(n.getArgs(), arg);
    }

    public void visit(ObjectCreationExpr n, Object arg) {
        if (n.getScope() != null) {
            n.getScope().accept(this, arg);
            printer.print(".");
        }

        printer.print("new ");

        printTypeArgs(n.getTypeArgs(), arg);
        n.getType().accept(this, arg);

        printArguments(n.getArgs(), arg);

        if (n.getAnonymousClassBody() != null) {
            printer.printLn(); printer.printLn("{");
            printer.indent();
            printMembers(n.getAnonymousClassBody(), arg);
            printer.unindent();
            printer.print("}");
        }
    }

    public void visit(UnaryExpr n, Object arg) {
        switch (n.getOperator()) {
            case positive:
                printer.print("+");
                break;
            case negative:
                printer.print("-");
                break;
            case inverse:
                printer.print("~");
                break;
            case not:
                printer.print("!");
                break;
            case preIncrement:
                printer.print("++");
                break;
            case preDecrement:
                printer.print("--");
                break;
        }

        n.getExpr().accept(this, arg);

        switch (n.getOperator()) {
            case posIncrement:
                printer.print("++");
                break;
            case posDecrement:
                printer.print("--");
                break;
        }
    }

    @Override
    public void visit(ConstructorDeclaration n, Object arg)
    {
        // get the full signature of the current method
        String full = Modifier.toString(n.getModifiers())+n.getName()+"(";
        String sign = n.getName()+"(";
        String pasc = n.getName()+"(";
        List<Parameter> pl = n.getParameters();
        if(pl!=null)
        for(Parameter p : pl)
        {
            full+=p.getType().toString()+" "+p.getId().getName()+", ";
            sign+=p.getType().toString()+", ";
            pasc+=p.getId().getName()+", ";
        }
        if(full.charAt(full.length()-1)==' ')
        {
            full=full.substring(0,full.length()-2);
            sign=sign.substring(0,sign.length()-2);
            pasc=pasc.substring(0,pasc.length()-2);
        }
        full +=")";
        sign +=")";
        pasc +=")";

        // is this the method we are looking for?
        if(signature.equals(full))
        {
            // set the root text
            root.setText(pasc);
            root.setColor(RSyntaxTextAreaUIBackgroundDrawer.COLOR_METHOD);
            // handle over to other methods
            if (n.getBlock() != null)
            {
                n.getBlock().accept(this, arg);
            }
        }
    }

    @Override
    public void visit(MethodDeclaration n, Object arg)
    {
        // get the full signature of the current method
        String full = Modifier.toString(n.getModifiers())+n.getType().toString()+" "+n.getName()+"(";
        String sign = n.getType().toString()+" "+n.getName()+"(";
        String pasc = Modifier.toString(n.getModifiers())+n.getType().toString()+" "+n.getName()+"(";
        List<Parameter> pl = n.getParameters();
        if(pl!=null)
        for(Parameter p : pl)
        {
            full+=p.getType().toString()+" "+p.getId().getName()+", ";
            sign+=p.getType().toString()+", ";
            pasc+=p.getId().getName()+", ";
        }
        if(full.charAt(full.length()-1)==' ')
        {
            full=full.substring(0,full.length()-2);
            sign=sign.substring(0,sign.length()-2);
            pasc=pasc.substring(0,pasc.length()-2);
        }
        full +=")";
        sign +=")";
        pasc +=")";

        // is this the method we are looking for?
        if(signature.equals(full))
        {
            // set the root text
            root.setText(pasc);
            root.setColor(RSyntaxTextAreaUIBackgroundDrawer.COLOR_METHOD);
            // handle over to other methods
            if (n.getBody() != null)
            {
                n.getBody().accept(this, arg);
            }
        }

    }

    public void visit(Parameter n, Object arg) {
        printAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        n.getType().accept(this, arg);
        if (n.isVarArgs()) {
            printer.print("...");
        }
        printer.print(" ");
        n.getId().accept(this, arg);
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt n, Object arg) 
    {
        String code = n.toString().trim();
        code = doReplacements(code);
        if(code.endsWith(";")) code=code.substring(0,code.length()-1);
        Instruction ele = new Instruction(code);
        lastElement = ele;
        queueList.get(queueList.size()-1).addElement(ele);
    }

    public void visit(VariableDeclarationExpr n, Object arg) {
        printAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        n.getType().accept(this, arg);
        printer.print(" ");

        for (Iterator<VariableDeclarator> i = n.getVars().iterator(); i.hasNext();) {
            VariableDeclarator v = i.next();
            v.accept(this, arg);
            if (i.hasNext()) {
                printer.print(", ");
            }
        }
    }

    public void visit(TypeDeclarationStmt n, Object arg) {
        n.getTypeDeclaration().accept(this, arg);
    }

    public void visit(AssertStmt n, Object arg) {
        printer.print("assert ");
        n.getCheck().accept(this, arg);
        if (n.getMessage() != null) {
            printer.print(" : ");
            n.getMessage().accept(this, arg);
        }
        printer.print(";");
    }

    @Override
    public void visit(BlockStmt n, Object arg)
    {
        if (n.getStmts() != null)
        {
            for (Statement s : n.getStmts())
            {
                s.accept(this, arg);
            }
        }
    }

    public void visit(LabeledStmt n, Object arg)
    {
        printer.print(n.getLabel());
        printer.print(": ");
        n.getStmt().accept(this, arg);
    }

    @Override
    public void visit(EmptyStmt n, Object arg)
    {
    }

    @Override
    public void visit(ExpressionStmt n, Object arg)
    {
        /*code = "";
        n.getExpression().accept(this, arg);*/

        String code = n.getExpression().toString().trim();
        code = doReplacements(code);
        if(code.endsWith(";")) code=code.substring(0,code.length()-1);
        int posi = code.indexOf("\"");

        // this does the replacement also in "sysout's" :(
        code = code.replaceAll("([^\\\"]*)([\\*\\/\\-\\+])=(.*)","$1=$1$2$3");

        if(posi>=0)
        {
            String first = code.substring(0, posi);
            String second = code.substring(posi);
            code = first.replaceFirst("[^=]=", " <- ")+second;
        }
        else code = code.replaceFirst("[^=]=", " <- ");
        
        code=code.replace("int ","");
        code=code.replace("float ","");
        code=code.replace("double ","");
        code=code.replace("long ","");
        code=code.replace("boolean ","");
        
        Instruction ele = new Instruction(code);
        lastElement = ele;
        queueList.get(queueList.size()-1).addElement(ele);
    }

    public void visit(SwitchStmt n, Object arg) {
        printer.print("switch(");
        n.getSelector().accept(this, arg);
        printer.printLn(")");
        printer.printLn("{");
        if (n.getEntries() != null) {
            printer.indent();
            for (SwitchEntryStmt e : n.getEntries()) {
                e.accept(this, arg);
            }
            printer.unindent();
        }
        printer.printLn("}");

    }

    public void visit(SwitchEntryStmt n, Object arg) {
        if (n.getLabel() != null) {
            printer.print("case ");
            n.getLabel().accept(this, arg);
            printer.print(":");
        } else {
            printer.print("default:");
        }
        printer.printLn();
        printer.indent();
        if (n.getStmts() != null) {
            for (Statement s : n.getStmts()) {
                s.accept(this, arg);
                printer.printLn();
            }
        }
        printer.unindent();
    }

    public void visit(BreakStmt n, Object arg) {
        printer.print("break");
        if (n.getId() != null) {
            printer.print(" ");
            printer.print(n.getId());
        }
        printer.print(";");
    }

    @Override
    public void visit(ReturnStmt n, Object arg)
    {
        String code = n.toString().trim();
        if(code.endsWith(";")) code=code.substring(0,code.length()-1);
        Element ele = new Instruction(code);
        lastElement = ele;
        queueList.get(queueList.size()-1).addElement(ele);
    }

    public void visit(EnumDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        printer.print("enum ");
        printer.print(n.getName());

        if (n.getImplements() != null) {
            printer.print(" implements ");
            for (Iterator<ClassOrInterfaceType> i = n.getImplements().iterator(); i.hasNext();) {
                ClassOrInterfaceType c = i.next();
                c.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }

        printer.printLn(); printer.printLn("{");
        printer.indent();
        if (n.getEntries() != null) {
            printer.printLn();
            for (Iterator<EnumConstantDeclaration> i = n.getEntries().iterator(); i.hasNext();) {
                EnumConstantDeclaration e = i.next();
                e.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        if (n.getMembers() != null) {
            printer.printLn(";");
            printMembers(n.getMembers(), arg);
        } else {
            if (n.getEntries() != null) {
                printer.printLn();
            }
        }
        printer.unindent();
        printer.print("}");
    }

    public void visit(EnumConstantDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        printer.print(n.getName());

        if (n.getArgs() != null) {
            printArguments(n.getArgs(), arg);
        }

        if (n.getClassBody() != null) {
            printer.printLn(); printer.printLn("{");
            printer.indent();
            printMembers(n.getClassBody(), arg);
            printer.unindent();
            printer.printLn("}");
        }
    }

    public void visit(EmptyMemberDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        printer.print(";");
    }

    public void visit(InitializerDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        if (n.isStatic()) {
            printer.print("static ");
        }
        n.getBlock().accept(this, arg);
    }

    @Override
    public void visit(IfStmt n, Object arg) 
    {
        String code = "("+n.getCondition().toString().trim()+")";

        Alternative ele = new Alternative(code);
        ele.setColor(RSyntaxTextAreaUIBackgroundDrawer.COLOR_ALT);
        if(code.length()>25)
        {
            StringList sl = StringList.explodeWithDelimiter(code, "&&");
            for(int s=sl.count()-1;s>=0;s--)
            {
                if (sl.get(s).equals("&&"))
                {
                    sl.delete(s);
                    sl.set(s-1, sl.get(s-1)+" &&");
                }
            }
            ele = new Alternative(sl);
        }

        lastElement = ele;
        queueList.get(queueList.size()-1).addElement(ele);
        queueList.add(ele.qTrue);
        n.getThenStmt().accept(this, arg);
        queueList.remove(queueList.size()-1);
        if (n.getElseStmt() != null)
        {
            queueList.add(ele.qFalse);
            n.getElseStmt().accept(this, arg);
            queueList.remove(queueList.size()-1);
        }
    }

    @Override
    public void visit(WhileStmt n, Object arg)
    {
        String code = n.getCondition().toString();
        //code = D7Parser.preWhile+"("+code+")"+D7Parser.postWhile;
        code = "while ("+code+")";
        While ele = new While(code);
        ele.setColor(RSyntaxTextAreaUIBackgroundDrawer.COLOR_LOOP);
        lastElement = ele;
        queueList.get(queueList.size()-1).addElement(ele);
        queueList.add(ele.q);
        n.getBody().accept(this, arg);
        queueList.remove(queueList.size()-1);
    }

    public void visit(ContinueStmt n, Object arg) {
        printer.print("continue");
        if (n.getId() != null) {
            printer.print(" ");
            printer.print(n.getId());
        }
        printer.print(";");
    }

    @Override
    public void visit(DoStmt n, Object arg)
    {
        String code = n.getCondition().toString();
        //code = D7Parser.preRepeat+"(not("+code+"))"+D7Parser.postRepeat;
        code = "while ("+code+")";
        Repeat ele = new Repeat(code);
        ele.setColor(RSyntaxTextAreaUIBackgroundDrawer.COLOR_LOOP);
        lastElement = ele;
        queueList.get(queueList.size()-1).addElement(ele);
        queueList.add(ele.q);
        n.getBody().accept(this, arg);
        queueList.remove(queueList.size()-1);
    }

    public void visit(ForeachStmt n, Object arg) {
        printer.print("for (");
        n.getVariable().accept(this, arg);
        printer.print(" : ");
        n.getIterable().accept(this, arg);
        printer.print(") ");
        n.getBody().accept(this, arg);
    }

    @Override
    public void visit(ForStmt n, Object arg)
    {
        String code = new String();

        if (n.getInit() != null)
        {
            for (Iterator<Expression> i = n.getInit().iterator(); i.hasNext();)
            {
                Expression e = i.next();
                code+= e.toString();
                //e.accept(this, arg);
                if (i.hasNext())
                {
                    code += ", ";
                }
            }
        }
        code += "; ";
        if (n.getCompare() != null)
        {
            code += n.getCompare().toString();
            //n.getCompare().accept(this, arg);
        }
        code += "; ";
        if (n.getUpdate() != null)
        {
            for (Iterator<Expression> i = n.getUpdate().iterator(); i.hasNext();)
            {
                Expression e = i.next();
                code += e.toString();
                //e.accept(this, arg);
                if (i.hasNext())
                {
                    code += ", ";
                }
            }
        }

        code = doReplacements(code);

        // Todo: interprete and set to correct form!
        //code = D7Parser.preFor+"("+code+")";
        // input : int i = 1 ; i <= number ; i++
        // output: i <- 1 à number

        StringList sl = StringList.explode(code,";");
        // this only works if we have a FOR-loop that increments
        // the counter by only one unit!


        // get the counter variable
        String counter = sl.get(0).trim();
        if (counter.indexOf("=")!=-1) counter=counter.substring(0,counter.indexOf("=")-1);
        if (counter.indexOf(" ")!=-1) counter=counter.substring(counter.indexOf(" ")+1);
        counter=counter.trim();

        // get the start value
        String startvalue = sl.get(0).trim();
        if (startvalue.indexOf("=")!=-1) startvalue=startvalue.substring(startvalue.indexOf("=")+1);
        startvalue=startvalue.trim();

        // get the increment value
        String increment = sl.get(2).trim();
        if ((increment.indexOf("++")!=-1) || (increment.indexOf("--")!=-1))
        {
            if (increment.indexOf("++")!=-1)
            {
                String vari = increment.substring(0,increment.indexOf("++")).trim();
                increment = vari+"="+vari+"+1";
            }
            else
            {
                String vari = increment.substring(0,increment.indexOf("--")).trim();
                increment = vari+"="+vari+"-1";
            }
        }
        else if (increment.indexOf("+=")!=-1)
        {
            String vari = increment.substring(0,increment.indexOf("+=")).trim();
            String inc = increment.substring(increment.indexOf("+=")+2).trim();
            increment = vari+"="+vari+"+"+inc;
        }
        else if (increment.indexOf("-=")!=-1)
        {
            String vari = increment.substring(0,increment.indexOf("-=")).trim();
            String inc = increment.substring(increment.indexOf("-=")+2).trim();
            increment = vari+"="+vari+"-"+inc;
        }
        else if (increment.indexOf("*=")!=-1)
        {
            String vari = increment.substring(0,increment.indexOf("*=")).trim();
            String inc = increment.substring(increment.indexOf("*=")+2).trim();
            increment = vari+"="+vari+"*"+inc;
        }
        else if (increment.indexOf("/=")!=-1)
        {
            String vari = increment.substring(0,increment.indexOf("/=")).trim();
            String inc = increment.substring(increment.indexOf("/=")+2).trim();
            increment = vari+"="+vari+"/"+inc;
        }
        if(increment.indexOf(counter)!=-1)
        {
            increment=increment.substring(increment.indexOf(counter)+counter.length()).trim();
            if(increment.indexOf(counter)!=-1)
            {
                increment=increment.substring(increment.indexOf(counter)+counter.length()).trim();
                if(increment.indexOf("+")!=-1) increment=increment.substring(increment.indexOf("+")+1).trim();
                if(increment.indexOf("-")!=-1) increment=increment.substring(increment.indexOf("-")+1).trim();
                if(increment.indexOf("*")!=-1) increment=increment.substring(increment.indexOf("*")+1).trim();
                if(increment.indexOf("/")!=-1) increment=increment.substring(increment.indexOf("/")+1).trim();
            }
        }

        // get the stop value
        String stopvalue = sl.get(1).trim();
        if(stopvalue.indexOf(counter)!=-1)
        {
            stopvalue=stopvalue.substring(stopvalue.indexOf(counter)+counter.length()).trim();
            try
            {
                if(stopvalue.indexOf(">=")!=-1)
                {
                    stopvalue = stopvalue.substring(stopvalue.indexOf(">=")+2).trim();
                }
                else if(stopvalue.indexOf(">")!=-1)
                {
                    stopvalue = stopvalue.substring(stopvalue.indexOf(">")+1).trim();
                    stopvalue = String.valueOf(Double.valueOf(stopvalue)+Double.valueOf(increment));
                    if(Double.valueOf(stopvalue)==Double.valueOf(stopvalue).intValue())
                    {
                        stopvalue=String.valueOf(Integer.valueOf(Double.valueOf(stopvalue).intValue()));
                    }
                }
                else if(stopvalue.indexOf("<=")!=-1)
                {
                    stopvalue = stopvalue.substring(stopvalue.indexOf("<=")+2).trim();
                }
                else if(stopvalue.indexOf("<")!=-1)
                {
                    stopvalue = stopvalue.substring(stopvalue.indexOf("<")+1).trim();
                    // this works for a numeric "stopvalue"
                    try
                    {
                        stopvalue = String.valueOf(Double.valueOf(stopvalue)-Double.valueOf(increment));
                        if(Double.valueOf(stopvalue)==Double.valueOf(stopvalue).intValue())
                        {
                            stopvalue=String.valueOf(Integer.valueOf(Double.valueOf(stopvalue).intValue()));
                        }
                    }
                    // try to handle a non-numeric stopvalue
                    catch (Exception e)
                    {
                        if (stopvalue.contains("+ "+increment))
                        {
                            stopvalue = stopvalue.substring(0,stopvalue.indexOf("+ "+increment));
                        }
                        else stopvalue=stopvalue+" - "+increment;
                    }
                }
                else if(stopvalue.indexOf("==")!=-1)
                {

                }
                else if(stopvalue.indexOf("!=")!=-1)
                {
                    stopvalue = stopvalue.substring(stopvalue.indexOf("!=")+2).trim();
                    // this works for a numeric "stopvalue"
                    try
                    {
                        stopvalue = String.valueOf(Double.valueOf(stopvalue)-Double.valueOf(increment));
                        if(Double.valueOf(stopvalue)==Double.valueOf(stopvalue).intValue())
                        {
                            stopvalue=String.valueOf(Integer.valueOf(Double.valueOf(stopvalue).intValue()));
                        }
                    }
                    // try to handle a non-numeric stopvalue
                    catch (Exception e)
                    {
                        if (stopvalue.contains("+ "+increment))
                        {
                            stopvalue = stopvalue.substring(0,stopvalue.indexOf("+ "+increment));
                        }
                        else stopvalue=stopvalue+" - "+increment;
                    }
                }
           }
           catch(Exception e)
           {
           }
        }

        code = "pour "+counter+" <- "+startvalue+" à "+stopvalue+", pas = "+increment;

        /*
        if(sl.count()>=3 && (sl.get(sl.count()-1).contains("++") || sl.get(sl.count()-1).contains("--")))
        {
            // get the right part
            String vari = sl.get(0);
            // cut off after the first space
            vari = vari.substring(vari.indexOf(" ")+1).trim();
            code = vari;
            vari = vari.substring(0,vari.indexOf(" ")).trim();
            // replace the assignment operator
            code = code.replace("=", "<-");
            // add "à"
            code+= " à ";
            // get the middle part
            String sec = sl.get(1).trim();
            // get everything after the last space
            //System.err.println(vari);
            //System.err.println(sec);
            sec =  sec.substring(sec.indexOf(vari.trim())+vari.length()).trim();
            if(sec.startsWith("<="))
            {
                sec=sec.substring(2);
            }
            else if(sec.startsWith("<"))
            {
                sec=sec.substring(1)+" - 1";
            }
            else if(sec.startsWith(">="))
            {
                sec=sec.substring(2);
            }
            else if(sec.startsWith(">"))
            {
                sec=sec.substring(1)+" + 1";
            }
            sec=sec.trim();
            
            code+=sec;
            code = "pour "+code;
        }
        else
        {
            code = "for ("+code+")";
        }
        */


        For ele = new For(code);
        ele.setColor(RSyntaxTextAreaUIBackgroundDrawer.COLOR_LOOP);
        lastElement = ele;
        queueList.get(queueList.size()-1).addElement(ele);
        queueList.add(ele.q);
        n.getBody().accept(this, arg);
        queueList.remove(queueList.size()-1);
    }

    public void visit(ThrowStmt n, Object arg) {
        printer.print("throw ");
        n.getExpr().accept(this, arg);
        printer.print(";");
    }

    public void visit(SynchronizedStmt n, Object arg) {
        printer.print("synchronized (");
        n.getExpr().accept(this, arg);
        printer.print(") ");
        n.getBlock().accept(this, arg);
    }

    public void visit(TryStmt n, Object arg) {
        printer.print("try ");
        n.getTryBlock().accept(this, arg);
        if (n.getCatchs() != null) {
            for (CatchClause c : n.getCatchs()) {
                c.accept(this, arg);
            }
        }
        if (n.getFinallyBlock() != null) {
            printer.print(" finally ");
            n.getFinallyBlock().accept(this, arg);
        }
    }

    public void visit(CatchClause n, Object arg) {
        printer.print(" catch (");
        n.getExcept().accept(this, arg);
        printer.print(") ");
        n.getCatchBlock().accept(this, arg);

    }

    public void visit(AnnotationDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        printer.print("@interface ");
        printer.print(n.getName());
        printer.printLn(); printer.printLn("{");
        printer.indent();
        if (n.getMembers() != null) {
            printMembers(n.getMembers(), arg);
        }
        printer.unindent();
        printer.print("}");
    }

    public void visit(AnnotationMemberDeclaration n, Object arg) {
        printJavadoc(n.getJavaDoc(), arg);
        printMemberAnnotations(n.getAnnotations(), arg);
        printModifiers(n.getModifiers());

        n.getType().accept(this, arg);
        printer.print(" ");
        printer.print(n.getName());
        printer.print("()");
        if (n.getDefaultValue() != null) {
            printer.print(" default ");
            n.getDefaultValue().accept(this, arg);
        }
        printer.print(";");
    }

    public void visit(MarkerAnnotationExpr n, Object arg) {
        printer.print("@");
        n.getName().accept(this, arg);
    }

    public void visit(SingleMemberAnnotationExpr n, Object arg) {
        printer.print("@");
        n.getName().accept(this, arg);
        printer.print("(");
        n.getMemberValue().accept(this, arg);
        printer.print(")");
    }

    public void visit(NormalAnnotationExpr n, Object arg) {
        printer.print("@");
        n.getName().accept(this, arg);
        printer.print("(");
        if (n.getPairs() != null) {
            for (Iterator<MemberValuePair> i = n.getPairs().iterator(); i.hasNext();) {
                MemberValuePair m = i.next();
                m.accept(this, arg);
                if (i.hasNext()) {
                    printer.print(", ");
                }
            }
        }
        printer.print(")");
    }

    public void visit(MemberValuePair n, Object arg) {
        printer.print(n.getName());
        printer.print(" = ");
        n.getValue().accept(this, arg);
    }

    public void visit(LineComment n, Object arg) {
        printer.print("//");
        printer.printLn(n.getContent());
    }

    public void visit(BlockComment n, Object arg) {
        printer.print("/*");
        printer.print(n.getContent());
        printer.printLn("*/");
    }

}
