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


import japa.parser.ASTHelper;
import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.ModifierSet;
import japa.parser.ast.type.ClassOrInterfaceType;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JScrollPane;
import lu.fisch.structorizer.elements.Root;
import lu.fisch.unimozer.aligner.Space;
import lu.fisch.unimozer.dialogs.ClassEditor;
import lu.fisch.unimozer.visitors.ClassVisitor;
import lu.fisch.unimozer.visitors.ClassChanger;
import lu.fisch.unimozer.visitors.ConstructorChanger;
import lu.fisch.unimozer.visitors.FieldChanger;
import lu.fisch.unimozer.visitors.FieldVisitor;
import lu.fisch.unimozer.visitors.MethodChanger;
import lu.fisch.unimozer.visitors.MethodVisitor;
import lu.fisch.unimozer.visitors.StructorizerVisitor;
import lu.fisch.unimozer.visitors.UsageVisitor;
import lu.fisch.unimozer.utils.StringList;
import lu.fisch.unimozer.visitors.ExtendsVisitor;
import lu.fisch.unimozer.visitors.InterfaceVisitor;
import lu.fisch.unimozer.visitors.PackageVisitor;
import org.mozilla.intl.chardet.* ;


/**
 *
 * @author robertfisch
 */
public class MyClass implements Space
{
    public static final int PAD = 8;
    public static final String NO_SYNTAX_ERRORS = "No syntax errors";
    
    private CompilationUnit cu;
    private boolean validCode = true;
    private boolean changed = false;
    private long lastModified = -1;
    

    private Point position = new Point(0,0);
    private int width = 0;
    private int height = 0;
    private boolean selected = false;
    private boolean compiled = false;
    private String internalName = new String();

    private MyClass extendsMyClass = null;
    private Vector<MyClass> usesMyClass = new Vector<MyClass>();
    private String extendsClass = new String();
    private Vector<String> implementsClasses = new Vector<String>();

    private Vector<Element> classes = new Vector<Element>();
    //private Vector<Element> constructors = new Vector<Element>();
    private Vector<Element> methods = new Vector<Element>();
    private Vector<Element> fields = new Vector<Element>();
    
    private StringList content = new StringList();

    private lu.fisch.structorizer.gui.Diagram nsd = null;

    private boolean enabled = true;

    private boolean isUML = true;
    private boolean isInterface = false;
    private boolean displaySource = true;
    private boolean displayUML = true;

    private String packagename = Package.DEFAULT;
    
    public Hashtable<String,String> generics = new Hashtable<String,String>();

    /*
    public boolean hasMethod(String name)
    {
        boolean found = false;
        for(int i=0;i<methods.size();i++)
        {
            if( ((MethodDeclaration) methods.get(i).getNode()).getName().equals(name)) found=true;
        }
        return found;
    }
     */

    public MyClass(String name)
    {
        cu = new CompilationUnit();
        // create the type declaration
        ClassOrInterfaceDeclaration type = new ClassOrInterfaceDeclaration(ModifierSet.PUBLIC, false, name);
        ASTHelper.addTypeDeclaration(cu, type);
        content = StringList.explode(getJavaCode(),"\n");
        inspect();
    }

    public MyClass(ClassEditor ce)
    {
        cu = new CompilationUnit();
        // create the type declaration
        ClassOrInterfaceDeclaration type = new ClassOrInterfaceDeclaration(ce.getModifier(), false, ce.getClassName());
        type.setInterface(ce.isInterface());
        ASTHelper.addTypeDeclaration(cu, type);
        if(!ce.getExtends().equals(""))
        {
            Vector<ClassOrInterfaceType> list = new Vector<ClassOrInterfaceType>();
            list.add(new ClassOrInterfaceType(ce.getExtends()));
            this.setExtendsClass(ce.getExtends());
            type.setExtends(list);
        }
        content = StringList.explode(getJavaCode(),"\n");
        inspect();
    }/**/

    private MyClass (String code, boolean display)
    {
        try {
            content.setText(code);
            cu = JavaParser.parse(new ByteArrayInputStream(getContent().getText().getBytes()));
            inspect();
        } catch (ParseException ex) {
            Logger.getLogger(MyClass.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private MyClass(FileInputStream fis) throws FileNotFoundException, ParseException, IOException
    {

        StringBuffer buffer = new StringBuffer();
        Reader in = null;
        try
        {
            InputStreamReader isr = new InputStreamReader(fis,Unimozer.FILE_ENCODING);
            in = new BufferedReader(isr);
            int ch;
            while ((ch = in.read()) > -1)
            {
                buffer.append((char)ch);
            }

            content = StringList.explode(buffer.toString(),"\n");
            cu = JavaParser.parse(new ByteArrayInputStream(getContent().getText().getBytes()));
        }
        finally
        {
            in.close();
        }

        /*
        try
        {
            content = new StringList();
            BufferedReader br = new BufferedReader(new InputStreamReader(in,Charset.));
            String line = br.readLine();
            while(line!=null)
            {
                System.out.println(line);
                content.add(line);
                line = br.readLine();
            }

            cu = JavaParser.parse(new ByteArrayInputStream(content.getText().getBytes()));
            // parse the file
            //cu = JavaParser.parse(in);
        }
        finally
        {
            in.close();
        }
        */
        inspect();
    }

    public MyClass(String filename, String defaultEncoding) throws FileNotFoundException, ParseException, IOException, URISyntaxException
    {
        /*
         * detected used caracter encoding
         */
	// Initalize the nsDetector() ;
	int lang = nsPSMDetector.UNIMOZER ;
	nsDetector det = new nsDetector(lang) ;

	// Set an observer...
	// The Notify() will be called when a matching charset is found.

	det.Init(new nsICharsetDetectionObserver()
        {
		public void Notify(String charset)
                {
		    HtmlCharsetDetector.found = true ;
		    //System.out.println("CHARSET = " + charset);
		}
    	});

        File f = new File(filename);

	URL url = f.toURI().toURL();
	BufferedInputStream imp = new BufferedInputStream(url.openStream());

	byte[] buf = new byte[1024] ;
	int len;
	boolean done = false ;
	boolean isAscii = true ;

	while( (len=imp.read(buf,0,buf.length)) != -1)
        {

		// Check if the stream is only ascii.
		if (isAscii)
		    isAscii = det.isAscii(buf,len);

		// DoIt if non-ascii and not done yet.
		if (!isAscii && !done)
 		    done = det.DoIt(buf,len, false);
	}
	det.DataEnd();
        imp.close();

        boolean found = false;
        String encoding = new String(Unimozer.FILE_ENCODING);

	if (isAscii)
        {
	   //System.out.println("CHARSET = ASCII");
           encoding="US-ASCII";
	   found = true ;
	}

	if (!found)
        {
	   String prob[] = det.getProbableCharsets() ;
	   //for(int i=0; i<prob.length; i++)
           //{
	   //	System.out.println("Probable Charset = " + prob[i]);
	   //}
           if(prob.length>0)
           {
                encoding=prob[0];
           }
           else
           {
                encoding=defaultEncoding;
           }
	}
        String filenameSmall = new File(filename).getName().replace(".java","");
        this.setInternalName(filenameSmall);
        File codeFile = new File(filename);
        lastModified = codeFile.lastModified();
        loadFromFileInputStream(new FileInputStream(filename),encoding);
    }

    public MyClass(FileInputStream fis, String encoding) throws FileNotFoundException, ParseException, IOException
    {
        loadFromFileInputStream(fis,encoding);
    }

    public boolean isDisplaySource() {
        return displaySource;
    }

    public void setDisplaySource(boolean displaySource) {
        this.displaySource = displaySource;
    }

    public void setDisplayUML(boolean displayUML) {
        this.displayUML = displayUML;
    }
    
    public boolean hasCyclicInheritance()
    {
        if (this.getExtendsClass().trim().equals("")) return false;
        else
        {
            if (this.getExtendsMyClass()==null) return false;
            else
            {
                StringList l = new StringList();
                MyClass other = this.getExtendsMyClass();
                l.add(this.getShortName());
                while ((other!=null) && (!l.contains(other.getShortName())))
                {
                    l.add(other.getShortName());
                    other=other.getExtendsMyClass();
                    if (other==null) break;
                }
                return (other!=null);
            }
        }
    }

    private void loadFromFileInputStream(FileInputStream fis, String encoding) throws FileNotFoundException, ParseException, IOException
    {
        StringBuffer buffer = new StringBuffer();
        Reader in = null;
        setValidCode(true);
        try
        {
            InputStreamReader isr = new InputStreamReader(fis,encoding);
            in = new BufferedReader(isr);
            int ch;
            while ((ch = in.read()) > -1)
            {
                buffer.append((char)ch);
            }
            content = StringList.explode(buffer.toString(),"\n");
            /*Console.disconnectAll();
            System.out.println(content.getText());
            Console.connectAll();*/
            //cu = JavaParser.parse(new ByteArrayInputStream(getContent().getText().getBytes()));
            parse();
        }
        catch(Exception ex)
        {
            setValidCode(false);
        }
        finally
        {
            in.close();
        }

        if(isValidCode())
        {
            inspect();
        }

    }

    public String parse()
    {
        boolean OK = false;
        String ret = NO_SYNTAX_ERRORS;
        setValidCode(true);
        try
        {
            cu = JavaParser.parse(new ByteArrayInputStream(getContent().getText().getBytes()));
            OK = true;
        }
        catch (ParseException ex)
        {
            ret = ex.getMessage();
            setValidCode(false);
        }
        catch (Error ex)
        {
            ret = ex.getMessage();
            setValidCode(false);
        }

        return ret;
    }

    public String loadFromString(String code)
    {
/*
        try
        {
            com.sun.tools.javac.util.Context context = new com.sun.tools.javac.util.Context();
            context.put(JavaFileManager.class, ToolProvider.getSystemJavaCompiler().getStandardFileManager(null,null,null));
            com.sun.tools.javac.parser.Scanner.Factory scannerFactory = com.sun.tools.javac.parser.Scanner.Factory.instance(context);
            com.sun.tools.javac.parser.Lexer lexer = scannerFactory.newScanner(code);
            com.sun.tools.javac.parser.Parser.Factory parserFactory = com.sun.tools.javac.parser.Parser.Factory.instance(context);
            com.sun.tools.javac.parser.Parser parser = parserFactory.newParser(lexer, true, false);
            
            System.err.println(parser.compilationUnit().getTree().toString());

            //System.err.println(parser.compilationUnit().toString());
            System.out.println(parser.compilationUnit().getTree().toString());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        catch (Error ex)
        {
            ex.printStackTrace();
        }
*/
        setContent(StringList.explode(code, "\n"));
        //System.err.println("--- content is ---");
        //System.err.println(content.getText());

        String ret = parse();

        if(isValidCode())
        {
            inspect();
        }
        
        /*Console.disconnectAll();
        System.err.println("--- code is ---");
        System.err.println(code);
        System.err.println("--- java ode is ---");
        System.err.println(getJavaCode());
        System.err.println("--- ret ---");
        System.err.println(ret);
        Console.connectAll();
         /**/
        return ret;

    }

    public void update(Element ele, String name, int modifiers, String extendsClass)
    {
        ClassChanger cnc = new ClassChanger((ClassOrInterfaceDeclaration) ele.getNode(), name, modifiers, extendsClass);
        cnc.visit(cu, null);
        inspect();
    }

    public void update(Element ele, String fieldType, String fieldName, int modifier)
    {
        FieldChanger fnc = new FieldChanger((FieldDeclaration) ele.getNode(), fieldType, fieldName, modifier);
        fnc.visit(cu, null);
        inspect();
    }

    void update(Element ele, String methodType, String methodName, int modifier, Vector<Vector<String>> params)
    {
        MethodChanger mnc = new MethodChanger((MethodDeclaration) ele.getNode(), methodType, methodName, modifier, params);
        mnc.visit(cu, null);
        inspect();
    }

    void update(Element ele, int modifier, Vector<Vector<String>> params)
    {
        ConstructorChanger cnc = new ConstructorChanger((ConstructorDeclaration) ele.getNode(), modifier, params);
        cnc.visit(cu, null);
        inspect();
    }

    public ClassOrInterfaceDeclaration getNode()
    {
        if(classes.size()>0)
        {
            return (ClassOrInterfaceDeclaration) classes.get(0).getNode();
        }
        else return null;
    }

    public int getModifiers()
    {
        if(classes.size()>0)
        {
            return ((ClassOrInterfaceDeclaration) classes.get(0).getNode()).getModifiers();
        }
        else return 0;
    }

    public String getExtendsClass()
    {
        if(isValidCode())
        {
            ExtendsVisitor cv = new ExtendsVisitor();
            cv.visit(cu,null);
            return cv.getExtends();
        }
        else return "";
    }

    private String insert(String what, String s, int start)
    { 
        return s.substring(0,start-1)+what+s.substring(start-1,s.length()); 
    }

    public String getJavaCode()
    {
        if(isValidCode()) return cu.toString();
        //if(isValidCode()) return getContent().getText();
        else return getContent().getText();
    }

    public String getJavaCodeCommentless()
    {
        /*
        MyDumpVisitor visitor = new MyDumpVisitor();
        cu.accept(visitor, null);
        return visitor.getSource();
         */

        // this is not yet optimized!

        String code = getContent().getText();
        String res = "";

        // wipe out comments
        boolean inMComment = false;
        for(int i=0;i<code.length();i++)
        {
            if(code.charAt(i)=='/' && code.indexOf("/*",i)==i)
            {
                inMComment=true;
            }

            if(!inMComment==true)
            {
                res=res+code.charAt(i);
            }

            if(code.charAt(i)=='*' && code.indexOf("*/",i)==i)
            {
                i++;
                inMComment=false;
            }

        }

        return res;

    }

    public String getName()
    {
        if(isValidCode())
        {
            ClassVisitor cv = new ClassVisitor();
            cv.visit(cu,null);
            return cv.getName();
        }
        else return getInternalName();
    }

    public StringList getUsesWho()
    {
        if(isValidCode())
        {
            UsageVisitor cv = new UsageVisitor();
            cv.visit(cu,null);
            return cv.getUesedClasses();
        }
        else return new StringList();
    }

    public boolean isDisplayUML() {
        return displayUML;
    }
    
    public String getShortName()
    {
        if(classes.size()>0) 
        {
            internalName = ((ClassOrInterfaceDeclaration) classes.get(0).getNode()).getName();
            return internalName;
        }
        else return getInternalName();
    }

    public String getFullName()
    {
        String result = getShortName();
        if(!getPackagename().equals(Package.DEFAULT)) result=getPackagename()+"."+result;
        return result;
    }

    public void addField(String fieldType, String fieldName, int modifier, boolean javaDoc, boolean setter, boolean getter, boolean useThis)
    {
        int insertAt = getCodePositions().get(0);

        String mod = "";
        if(ModifierSet.isAbstract(modifier)) mod+=" abstract";
        if(ModifierSet.isPrivate(modifier)) mod+=" private";
        if(ModifierSet.isProtected(modifier)) mod+=" protected";
        if(ModifierSet.isPublic(modifier)) mod+=" public";
        if(ModifierSet.isStatic(modifier)) mod+=" static";
        if(ModifierSet.isFinal(modifier)) mod+=" final";
        if(ModifierSet.isTransient(modifier)) mod+=" transient";
        if(ModifierSet.isSynchronized(modifier)) mod+=" synchronized";
        mod=mod.trim();

        String field = "\t"+mod+" "+fieldType.trim()+" "+fieldName.trim()+";";

        String jd = "";
        if(javaDoc)
        {
            //jd="\t/**\n"+
            //   "\t * Write a description of field \""+fieldName+"\" here.\n"+
            //   "\t */\n";
            
            jd="\t/** Write a description of field \""+fieldName+"\" here. */\n";
        }


        if (getter==true)
        {
            Vector<Vector<String>> params = new Vector<Vector<String>>();
            String body = "\t\treturn "+fieldName+";\n";
            this.addMethod(fieldType, (fieldType.trim().toLowerCase().equals("boolean") ?"is":"get")+fieldName.substring(0,1).toUpperCase()+fieldName.substring(1), ModifierSet.PUBLIC+(ModifierSet.isStatic(modifier)?ModifierSet.STATIC:0), params, javaDoc,body);
        }
        
        if (setter==true)
        {
            if(useThis==false)
            {
                Vector<Vector<String>> params = new Vector<Vector<String>>();
                Vector<String> param = new Vector<String>();
                param.add(fieldType);
                param.add("p"+fieldName.substring(0,1).toUpperCase()+fieldName.substring(1));
                params.add(param);
                String body = "\t\t"+fieldName+" = "+"p"+fieldName.substring(0,1).toUpperCase()+fieldName.substring(1)+";\n";
                this.addMethod("void", "set"+fieldName.substring(0,1).toUpperCase()+fieldName.substring(1), ModifierSet.PUBLIC+(ModifierSet.isStatic(modifier)?ModifierSet.STATIC:0), params, javaDoc,body);
            }
            else
            {
                Vector<Vector<String>> params = new Vector<Vector<String>>();
                Vector<String> param = new Vector<String>();
                param.add(fieldType);
                param.add(fieldName);
                params.add(param);
                String body = "\t\tthis."+fieldName+" = "+fieldName+";\n";
                this.addMethod("void", "set"+fieldName.substring(0,1).toUpperCase()+fieldName.substring(1), ModifierSet.PUBLIC+(ModifierSet.isStatic(modifier)?ModifierSet.STATIC:0), params, javaDoc,body);
            }
        }

        String code = getContent().getText();
        code=code.substring(0, insertAt)+"\n\n"+jd+field+""+code.substring(insertAt);
        loadFromString(code);
    }

    /**
     * Method to add a Field using the AST-Parser
     * 
     * @param fieldType
     * @param fieldName
     * @param modifier
     * @return
     */
    /*
    public FieldDeclaration addField(String fieldType, String fieldName, int modifier)
    {
        FieldDeclaration fd = ASTHelper.createFieldDeclaration(modifier,new ClassOrInterfaceType(fieldType),fieldName);
        TypeDeclaration thisClassType = cu.getTypes().get(0);
        ASTHelper.addMember(thisClassType, fd);
        inspect();
        return fd;
    }
     */

    public String addMethod(String methodType, String methodName, int modifier, Vector<Vector<String>> params, boolean javaDoc)
    {
        return addMethod(methodType,methodName,modifier,params,javaDoc,"");
    }

    public String addMethod(String methodType, String methodName, int modifier, Vector<Vector<String>> params, boolean javaDoc, String body)
    {
        int insertAt = getCodePositions().get(2);

        String mod = "";
        if(ModifierSet.isPrivate(modifier)) mod+=" private";
        if(ModifierSet.isProtected(modifier)) mod+=" protected";
        if(ModifierSet.isPublic(modifier)) mod+=" public";
        if(ModifierSet.isStatic(modifier)) mod+=" static";
        if(ModifierSet.isFinal(modifier)) mod+=" final";
        if(ModifierSet.isAbstract(modifier)) mod+=" abstract";
        if(ModifierSet.isSynchronized(modifier)) mod+=" synchronized";
        mod=mod.trim();

        String param = "";
        for(int i=0;i<params.size();i++)
        {
            if(!param.equals("")) param+=", ";
            param+=params.get(i).get(0)+" "+params.get(i).get(1);
        }


        String field;
        String ret = mod+" "+methodType.trim()+" "+methodName.trim()+"("+param.trim()+")";
        if(!mod.contains("abstract"))
        {
            field =    "\t"+mod+" "+methodType.trim()+" "+methodName.trim()+"("+param.trim()+")\n"+
                       "\t{\n"+
                        body+
                       //"        \n"+
                       "\t}";
        }
        else
        {
            field =    "    "+mod+" "+methodType.trim()+" "+methodName.trim()+"("+param.trim()+");";
        }

        String jd="";
        if(javaDoc)
        {
            jd= "\t/**\n"+
                "\t * Write a description of method \""+methodName.trim()+"\" here."+"\n"; //+
               // "\t * "+"\n";

            int maxLength = 0;
            for(int i=0;i<params.size();i++)
            {
                int thisLength=params.get(i).get(1).length();
                if(thisLength>maxLength) maxLength=thisLength;
            }
            for(int i=0;i<params.size();i++)
            {
                String thisName=params.get(i).get(1);
                while (thisName.length()<maxLength) thisName+=" ";
                jd+="\t * @param "+thisName+"    a description of the parameter \""+thisName+"\"\n";
            }
            if(!methodType.trim().equals("void"))
            {
                jd+="\t * @return                a description of the returned result\n";
            }
            jd+= "\t */\n";

            if(field.trim().startsWith("public static void main(String[] args)"))
            {
                jd =     "\t/**\n"+
                         "\t * The main entry point for executing this program."+"\n"+
                         "\t */\n";
            }
        }

        String code = getContent().getText();
        code=code.substring(0, insertAt)+"\n\n"+jd+field+""+code.substring(insertAt);
        loadFromString(code);
        return ret;
    }

    public void selectBySignature(String sign)
    {
        for(int i=0;i<methods.size();i++)
        //for(Element ele : methods)
        {
            Element ele = methods.get(i); 
            //System.out.println(sign+" => "+ele.getSignature()+" / "+ele.getFullName());
            if(ele.getFullName().equals(sign)) ele.setSelected(true);
        }
    }

/*
    public MethodDeclaration addMethod(String methodType, String methodName, int modifier, Vector<Vector<String>> params)
    {
        // add the method
        MethodDeclaration md = new MethodDeclaration(modifier, new ClassOrInterfaceType(methodType), methodName);
        TypeDeclaration thisClassType = cu.getTypes().get(0);
        ASTHelper.addMember(thisClassType, md);
        // add the parameters
        for(Vector<String> param : params)
        {
            Parameter pd = ASTHelper.createParameter(new ClassOrInterfaceType((String) param.get(0)), (String) param.get(1));
            //pd.setVarArgs(true);
            ASTHelper.addParameter(md,pd);
        }
        // add a body to the method
        BlockStmt block = new BlockStmt();
        md.setBody(block);

        inspect();
        return md;
    }
*/

    public String addConstructor(int modifier, Vector<Vector<String>> params, boolean javaDoc)
    {
        int insertAt = getCodePositions().get(1);

        String mod = "";
        if(ModifierSet.isAbstract(modifier)) mod+=" abstract";
        if(ModifierSet.isPrivate(modifier)) mod+=" private";
        if(ModifierSet.isProtected(modifier)) mod+=" protected";
        if(ModifierSet.isPublic(modifier)) mod+=" public";
        if(ModifierSet.isStatic(modifier)) mod+=" static";
        if(ModifierSet.isFinal(modifier)) mod+=" final";
        if(ModifierSet.isSynchronized(modifier)) mod+=" synchronized";
        mod=mod.trim();

        String param = "";
        for(int i=0;i<params.size();i++)
        {
            if(!param.equals("")) param+=", ";
            param+=params.get(i).get(0)+" "+params.get(i).get(1);
        }

        String ret   = mod+" "+getInternalName().trim()+"("+param.trim()+")";
        String field = "\t"+mod+" "+getInternalName().trim()+"("+param.trim()+")\n"+
                       "\t{\n"+
                       //"\t\t\n"+
                       "\t}";

        String jd="";
        if(javaDoc)
        {
             jd= "\t/**\n"+
                 "\t * Write a description of this constructor here."+"\n"; //+
                 //"\t * "+"\n";

            int maxLength = 0;
            for(int i=0;i<params.size();i++)
            {
                int thisLength=params.get(i).get(1).length();
                if(thisLength>maxLength) maxLength=thisLength;
            }
            for(int i=0;i<params.size();i++)
            {
                String thisName=params.get(i).get(1);
                while (thisName.length()<maxLength) thisName+=" ";
                jd+="\t * @param "+thisName+"    a description of the parameter \""+thisName+"\"\n";
            }
            jd+= "\t */\n";
        }

        String code = getContent().getText();
        code=code.substring(0, insertAt)+"\n\n"+jd+field+""+code.substring(insertAt);
        loadFromString(code);
        return ret;
    }

    public boolean hasMain()
    {
        for(int i=0;i<methods.size();i++)
        //for(Element element : methods)
        {
            Element element = methods.get(i);
            if (element.getFullName().equals("public static void main(String[] args)") ||
                element.getFullName().equals("public static void main(String args[])")) return true;
        }
        return false;
    }

    public boolean hasMain1()
    {
        for(int i=0;i<methods.size();i++)
        //for(Element element : methods)
        {
            Element element = methods.get(i);
            if (element.getFullName().equals("public static void main(String[] args)")) return true;
        }
        return false;
    }

    public boolean hasMain2()
    {
        for(int i=0;i<methods.size();i++)
        //for(Element element : methods)
        {
            Element element = methods.get(i);
            if (element.getFullName().equals("public static void main(String args[])")) return true;
        }
        return false;
    }

    /*
    public ConstructorDeclaration addConstructor(int modifier, Vector<Vector<String>> params)
    {
        // add the method
        ConstructorDeclaration cd = new ConstructorDeclaration(modifier, getShortName());
        TypeDeclaration thisClassType = cu.getTypes().get(0);
        ASTHelper.addMember(thisClassType, cd);
        // add the parameters
        for(Vector<String> param : params)
        {
            Parameter pd = ASTHelper.createParameter(new ClassOrInterfaceType((String) param.get(0)), (String) param.get(1));
            //pd.setVarArgs(true);
            ASTHelper.addParameter(cd, pd);
        } 
        // add a body to the method
        cd.setBlock(new BlockStmt());

        inspect();
        return cd;
    }
     */


    public String getFullSignatureBySignature(String sign)
    {
        String ret = "";
        for(int i=0;i<methods.size();i++)
        //for(Element ele : methods)
        {
            Element ele = methods.get(i);
            if(ele.getSignature().equals(sign)) ret=ele.getShortName();
        }
        if(ret.equals("") && extendsMyClass!=null) ret=extendsMyClass.getFullSignatureBySignature(sign);
        return ret;
    }

    public int getFullSignatureBySignaturePos(String sign)
    {
        int ret = -1;
        for(int i=0;i<methods.size();i++)
        //for(Element ele : methods)
        {
            Element ele = methods.get(i);
            ret++;
            if(ele.getSignature().equals(sign)) return ret;
        }
        if(ret==-1 && extendsMyClass!=null) ret=extendsMyClass.getFullSignatureBySignaturePos(sign);
        return ret;
    }

    public String getCompleteSignatureBySignature(String sign)
    {
        boolean debug = false;
        String ret = "";
        //String mSign = sign.replace("Object", ".*");
        if (debug) System.out.println("Looking for: "+sign);
        for(int i=0;i<methods.size();i++)
        //for(Element ele : methods)
        {
            Element ele = methods.get(i);
            if (debug) System.out.println("Having: "+ele.getSignature());
            if(ele.getSignature().equals(sign)) ret=ele.getName();
            //if(ele.getSignature().equals(sign) || ele.getSignature().matches(mSign)) ret=ele.getName();
        }
        if(ret.equals("") && extendsMyClass!=null) ret=extendsMyClass.getCompleteSignatureBySignature(sign);
        return ret;
    }

    public int getCompleteSignatureBySignaturePos(String sign)
    {
        int ret = -1;
        //System.out.println("Looking for: "+sign);
        for(int i=0;i<methods.size();i++)
        //for(Element ele : methods)
        {
            Element ele = methods.get(i);
            //System.out.println("Having: "+ele.getSignature());
            ret++;
            if(ele.getSignature().equals(sign)) return ret;
        }
        if(ret==-1 && extendsMyClass!=null) ret=extendsMyClass.getCompleteSignatureBySignaturePos(sign);
        return ret;
    }

    public boolean isInterface()
    {
        return isInterface;
    }

    public String getSignatureByFullSignature(String sign)
    {
        String ret = "";
        //System.out.println("SBFS Looking for: "+sign);
        for(int i=0;i<methods.size();i++)
        //for(Element ele : methods)
        {
            Element ele = methods.get(i);
            //System.out.println("SBFS Having: "+ele.getFullName());
            if(ele.getShortName().equals(sign)) ret=ele.getSignature();
        }
        if(ret.equals("") && extendsMyClass!=null) ret=extendsMyClass.getSignatureByFullSignature(sign);
        return ret;
    }

    public LinkedHashMap<String,String> getInputsBySignature(String sign)
    {
        boolean debug = false;
        

        LinkedHashMap<String,String> ret = new LinkedHashMap<String,String>();
        if (debug) System.out.println("IBS Looking for: "+sign);
        for(int i=0;i<methods.size();i++)
        //for(Element ele : methods)
        {
            Element ele = methods.get(i);
            String eleSig = ele.getSignature();
            eleSig = eleSig.replace(", ", ",");
            if (debug) System.out.println("IBS Having: "+eleSig);
            
            if((hasMain2()) && (eleSig.equals("void main(String)")))
            {
                eleSig="void main(String[])";
            }
            
            // the part after the "or" is needed in case the class is inside a package
            if(eleSig.equals(sign) || (getPackagename()+"."+eleSig).equals(sign) ) 
            {
                if (debug) System.out.println("IBS Case 1");
                if((hasMain2()) && (eleSig.equals("void main(String[])")))
                {
                    ret.put((String) ele.getParams().keySet().toArray()[0], "String[]");
                }
                else
                {
                    ret=(LinkedHashMap<String, String>) ele.getParams().clone();
                    Object[] keySet = ret.keySet().toArray();
                    for(int j=0;j<keySet.length;j++)
                    {
                        String key = (String) keySet[j];
                        String value = ret.get(key);
                        if (debug) System.out.println("IBS: Looking for type <"+value+">");
                        if(generics.containsKey(value))
                        {
                            ret.put(key, generics.get(value));
                            if (debug) System.out.println("IBS: Replaceing type <"+value+"> with <"+generics.get(value)+">");
                        }
                    }
                    return ret;
                }
            }
            /*else {
                String mSig = new String(sign);
                mSig=mSig.replace("Object",".*");
                if (debug) System.out.println("IBS Case 2: "+mSig);
                // if we have a match, try to replace with generics
                // the part after the "or" is needed in case the class is inside a package
                if(eleSig.matches(mSig)  || (getPackagename()+"."+eleSig).matches(mSig) ) 
                {
                    ret=ele.getParams();
                    Object[] keySet = ret.keySet().toArray();
                    for(int j=0;j<keySet.length;j++)
                    {
                        String key = (String) keySet[j];
                        String value = ret.get(key);
                        if (debug) System.out.println("IBS: Looking for type <"+value+">");
                        if(generics.containsKey(value))
                        {
                            ret.put(key, generics.get(value));
                            if (debug) System.out.println("IBS: Replaceing type <"+value+"> with <"+generics.get(value)+">");
                        }
                    }
                    return ret;
                }
            }*/
            
        }
        if(ret.size()==0 && extendsMyClass!=null) ret=extendsMyClass.getInputsBySignature(sign);
        return ret;
    }
    
    public LinkedHashMap<String,String> getInputsBySignature(String sign, Hashtable<String,String> theseGenerics)
    {
        boolean debug = false;
        
        LinkedHashMap<String,String> ret = new LinkedHashMap<String,String>();
        if (debug) System.out.println("IBS Looking for: "+sign);
        for(int i=0;i<methods.size();i++)
        //for(Element ele : methods)
        {
            Element ele = methods.get(i);
            String eleSig = ele.getSignature();
            eleSig = eleSig.replace(", ", ",");
            if (debug) System.out.println("IBS Having: "+ele.getSignature());
            

            if((hasMain2()) && (eleSig.equals("void main(String)")))
            {
                eleSig="void main(String[])";
            }
            
            // the part after the "or" is needed in case the class is inside a package
            if(eleSig.equals(sign) || (getPackagename()+"."+eleSig).equals(sign) ) 
            {
                //Objectizer.printHashtable("theseGenerics", theseGenerics);
                
                if (debug) System.out.println("IBS: Found "+ele.getFullName());
                if((hasMain2()) && (eleSig.equals("void main(String[])")))
                {
                    ret.put((String) ele.getParams().keySet().toArray()[0], "String[]");
                }
                else
                {
                    ret=(LinkedHashMap<String, String>) ele.getParams().clone();
                    Object[] keySet = ret.keySet().toArray();
                    for(int j=0;j<keySet.length;j++)
                    {
                        String key = (String) keySet[j];
                        String value = ret.get(key);
                        if (debug) System.out.println("IBS: Looking for type <"+value+">");
                        if(theseGenerics.containsKey(value))
                        {
                            ret.put(key, theseGenerics.get(value));
                            if (debug) System.out.println("IBS: Replaceing type <"+value+"> with <"+theseGenerics.get(value)+">");
                        }
                    }
                    return ret;
                }
            }
            
        }
        if(ret.size()==0 && extendsMyClass!=null) ret=extendsMyClass.getInputsBySignature(sign);
        return ret;
    }

    public String getJavaDocBySignature(String sign)
    {
        String ret = "";
        //System.out.println("Looking for: "+sign);
        for(int i=0;i<methods.size();i++)
        //for(Element ele : methods)
        {
            Element ele = methods.get(i);
            //System.out.println("Having: "+ele.getSignature());
            if(ele.getSignature().equals(sign)) ret=ele.getJavaDoc();
            if(ret==null) ret="";
        }
        if(ret.length()==0 && extendsMyClass!=null) ret=extendsMyClass.getJavaDocBySignature(sign);
        return ret;
    }

    private void deselectAll()
    {
        for(int i=0;i<classes.size();i++) //for(Element ele : classes) 
            { classes.get(i).setSelected(false); }
        for(int i=0;i<fields.size();i++) //for(Element ele : fields) 
            { fields.get(i).setSelected(false); }
        for(int i=0;i<methods.size();i++) //for(Element ele : methods) 
            { methods.get(i).setSelected(false); }
    }

    public Element getSelected()
    {
        Element sel = null;
        for(int i=0;i<classes.size();i++) //for(Element ele : classes) 
            { Element ele = classes.get(i); if(ele.isSelected()) sel=ele; }
        for(int i=0;i<fields.size();i++) //(Element ele : fields) 
            { Element ele = fields.get(i); if(ele.isSelected()) sel=ele; }
        for(int i=0;i<methods.size();i++) //for(Element ele : methods) 
            { Element ele = methods.get(i); if(ele.isSelected()) sel=ele; }
        return sel;
    }

    public Vector<String> getImplements()
    {
        return implementsClasses;
    }

    public void updateContent()
    {
        setContent(StringList.explode(getJavaCode(), "\n"));
    }

    public void inspect()
    {
        /*
        if(getSelected()!=null)
        {
            getSelected().getName();

            StructorizerVisitor sv = new StructorizerVisitor(getSelected().getName());
            sv.visit(cu,null);


            // create editor
            lu.fisch.structorizer.gui.Mainform form;
            form=new lu.fisch.structorizer.gui.Mainform();

            // change the default closing behaviour
            form.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            // affect the new diagram to the editor
            form.diagram.root=sv.root;
            // redraw the diagram
            form.diagram.redraw();
            form.setVisible(true);
        }
        */

        // update the content
        //updateContent(); // <-- don't do this here!

        if(isValidCode())
        {
            // get the old selected element
            String selectedSignature = null;
            if(getSelected()!=null) selectedSignature=getSelected().getName();

            // class
            ClassVisitor cv = new ClassVisitor();
            cv.visit(cu,null);
            classes=cv.getElements();
            isInterface=cv.isInterface();

            // interfaces
            InterfaceVisitor iv = new InterfaceVisitor();
            iv.visit(cu, null);
            implementsClasses=iv.getImplementsClasses();

            // extends
            ExtendsVisitor ev = new ExtendsVisitor();
            ev.visit(cu,null);
            extendsClass=ev.getExtends();

            // package
            PackageVisitor pv = new PackageVisitor();
            pv.visit(cu,null);
            packagename=pv.getPackageName();

            // fields
            FieldVisitor fv = new FieldVisitor(this.getShortName());
            fv.visit(cu,null);
            fields=fv.getElements();

            // methods
            MethodVisitor mv = new MethodVisitor(this.getShortName());
            mv.visit(cu,getContent());
            methods=mv.getElements();
            //constructors=mv.getConstructors();
            setCompiled(false);

            /*
            System.out.println("Inspected <"+getFullName()+">");
            System.out.println("Extends: "+extendsClass);
            System.out.print("Implements: ");
            for(String cla : implementsClasses) System.out.println(cla+", ");
            System.out.println();
             */

            // reselect the element that has been updated
            // works only if the signature of the element has not been changed!
            for(int i=0;i<classes.size();i++) //for(Element ele : classes) 
                { Element ele = classes.get(i); ele.setUML(isUML); if(ele.getName().equals(selectedSignature)) ele.setSelected(true); }
            for(int i=0;i<fields.size();i++) //for(Element ele : fields)  
                { Element ele = fields.get(i); ele.setUML(isUML); if(ele.getName().equals(selectedSignature)) ele.setSelected(true); }
            for(int i=0;i<methods.size();i++) //for(Element ele : methods) 
                { Element ele = methods.get(i); ele.setUML(isUML); if(ele.getName().equals(selectedSignature)) ele.setSelected(true); }
        }
        else
        {
            //this.classes.clear();
            //this.fields.clear();
            //this.methods.clear();
        }
        
        updateNSD();

    }

    public void draw(Graphics graphics, boolean showFields, boolean showMethods, int mode)
    {
        if(displayUML)
        {
            // save state
            boolean compiled = this.isCompiled();
            boolean selected = this.selected;
            Element selEle = this.getSelected();
            Point position = (Point) this.getPosition().clone();

            // reset
            this.deselectAll();
            this.setCompiled(false);
            this.selected = false;
            this.setPosition(new Point(0,0));

            // selected depending on the mode
            if(mode==1)
            {
                this.selected = true;
                for(int i=0;i<classes.size();i++)
                    { Element ele = classes.get(i); ele.setSelected(true); }
            }
            else if (mode==2)
            {
                this.setCompiled(true);
                this.setPosition(new Point(2,2));
            }

            draw(graphics,showFields,showMethods);

            // reset
            this.deselectAll();
            this.setCompiled(false);
            this.selected = false;


            // restore state
            this.setCompiled(compiled);
            this.selected = selected;
            if (selEle!=null) selEle.setSelected(true);
            this.setPosition(position);
        }
    }

    public void draw(Graphics graphics, boolean showFields, boolean showMethods)
    {
        if(displayUML)
        {
            Graphics2D g = (Graphics2D) graphics;
            Color drawColor = new Color(255,245,235);
            if(!isValidCode())
            {
                drawColor = Color.RED;
            }
            else if(selected==true)
            {
                drawColor = Color.YELLOW;
            }
            else if(isCompiled()==true)
            {
                drawColor = new Color(235,255,235);
            }

            boolean cleanIt=false;
            if(!isValidCode() && classes.isEmpty())
            {
                cleanIt=true;
                Element ele = new Element(Element.CLASS);
                ele.setName("public class "+internalName);
                ele.setUmlName(internalName);
                ele.setUML(isUML);
                classes.add(ele);
            }

            // inspect the class

            // dertermine ervery values
            int totalHeight = 0;
            int maxWidth = 0;
            int classesHeight = 0*PAD;
            int fieldsHeight = 0*PAD;
            int methodsHeight = 0*PAD;

            if(isInterface())
            {
                Element ele = new Element(Element.INTERFACE);
                ele.setUmlName("<interface>");
                ele.setName("<interface>");
                classes.add(0, ele);
            }

            for(int i=0;i<classes.size();i++)
            //for(Element ele : classes)
            {
                Element ele = classes.get(i);

                //int fontStyle = g.getFont().getStyle();
                //if (ModifierSet.isAbstract(getModifiers())) g.setFont(new Font(g.getFont().getFontName(),Font.ITALIC,g.getFont().getSize()));
                g.setFont(new Font(g.getFont().getFamily(),ele.getFontStyle(),Unimozer.DRAW_FONT_SIZE));
                int h = (int) g.getFont().getStringBounds(ele.getPrintName(), g.getFontRenderContext()).getHeight()+PAD;
                int w = (int) g.getFont().getStringBounds(ele.getPrintName(), g.getFontRenderContext()).getWidth()+Element.ICONSIZE;
                //g.setFont(new Font(g.getFont().getFontName(),fontStyle,g.getFont().getSize()));
                g.setFont(new Font(g.getFont().getFamily(),Font.PLAIN,Unimozer.DRAW_FONT_SIZE));

                ele.setHeight(h);
                ele.setPosition(new Point(position.x,position.y+totalHeight));
                if (w>maxWidth) maxWidth=w;
                classesHeight+=h;
                totalHeight+=h;
            }
            if(showFields)
            {
                totalHeight+=0*PAD;
                for(int i=0;i<fields.size();i++)                
                //for(Element ele : fields)
                {
                    Element ele = fields.get(i);
                    g.setFont(new Font(g.getFont().getFamily(),ele.getFontStyle(),Unimozer.DRAW_FONT_SIZE));
                    int h = (int) g.getFont().getStringBounds(ele.getPrintName(), g.getFontRenderContext()).getHeight()+PAD;
                    int w = (int) g.getFont().getStringBounds(ele.getPrintName(), g.getFontRenderContext()).getWidth()+Element.ICONSIZE;
                    g.setFont(new Font(g.getFont().getFamily(),Font.PLAIN,Unimozer.DRAW_FONT_SIZE));
                    ele.setHeight(h);
                    ele.setPosition(new Point(position.x,position.y+totalHeight));
                    if (w>maxWidth) maxWidth=w;
                    fieldsHeight+=h;
                    totalHeight+=h;
                }
            }
            if(showMethods)
            {
                totalHeight+=0*PAD;
                if(fieldsHeight==0) totalHeight+= fieldsHeight = PAD;
                for(int i=0;i<methods.size();i++)
                //for(Element ele : methods)
                {
                    Element ele = methods.get(i);
                    g.setFont(new Font(g.getFont().getFamily(),ele.getFontStyle(),Unimozer.DRAW_FONT_SIZE));
                    int h = (int) g.getFont().getStringBounds(ele.getPrintName(), g.getFontRenderContext()).getHeight()+PAD;
                    int w = (int) g.getFont().getStringBounds(ele.getPrintName(), g.getFontRenderContext()).getWidth()+Element.ICONSIZE;
                    g.setFont(new Font(g.getFont().getFamily(),Font.PLAIN,Unimozer.DRAW_FONT_SIZE));
                    ele.setHeight(h);
                    ele.setPosition(new Point(position.x,position.y+totalHeight));
                    if (w>maxWidth) maxWidth=w;
                    methodsHeight+=h;
                    totalHeight+=h;
                }
                totalHeight+=0;
                if(methodsHeight==0) totalHeight+= methodsHeight = PAD;
            }

            this.width=maxWidth+2*PAD;
            this.height=totalHeight;

            // set widths
            for(int i=0;i<classes.size();i++) //for(Element ele : classes) 
                { classes.get(i).setWidth(this.getWidth()); }
            for(int i=0;i<fields.size();i++) //if(showFields) for(Element ele : fields) 
                { fields.get(i).setWidth(this.getWidth()); }
            for(int i=0;i<methods.size();i++) //if(showMethods)for(Element ele : methods) 
                { methods.get(i).setWidth(this.getWidth()); }

            // draw background
            g.setColor(drawColor);
            g.fillRect(position.x,position.y,this.getWidth(), this.getHeight());

            g.setColor(drawColor);

            for(int i=0;i<classes.size();i++) //for(Element ele : classes) 
                { classes.get(i).draw(g); }
            if(showFields) for(int i=0;i<fields.size();i++) //for(Element ele : fields) 
                { fields.get(i).draw(g); }
            if(showMethods)for(int i=0;i<methods.size();i++) //for(Element ele : methods) 
                { methods.get(i).draw(g); }

            // draw boxes
            Stroke oldStroke = g.getStroke();
            if(isInterface()) g.setStroke(Diagram.dashed);

            g.setColor(Color.BLACK);
            g.drawRect(position.x,position.y,this.getWidth(),classesHeight);
            g.drawRect(position.x,position.y+classesHeight,this.getWidth(),fieldsHeight);
            g.drawRect(position.x,position.y+classesHeight+fieldsHeight,this.getWidth(),methodsHeight);
            if(isCompiled()==true)
            {
                g.drawRect(position.x-2,position.y-2,this.getWidth()+4,this.getHeight()+4);
            }

            g.setStroke(oldStroke);

            if(!isValidCode() && cleanIt)
            {
                classes.clear();
            }

            if(!isEnabled())
            {
                g.setColor(new Color(128,128,128,128));
                g.fillRect(this.getPosition().x,this.getPosition().y,getWidth(),getHeight());
            }

            if(isInterface())
            {
                classes.remove(0);
            }
        }
    }

    public boolean isInside(Point pt)
    {
        return (position.x<=pt.x && pt.x<=position.x+getWidth() &&
                position.y<=pt.y && pt.y<=position.y+getHeight());
    }

    public Point getRelative(Point pt)
    {
        if (isInside(pt))
        {
            return new Point(pt.x-position.x,
                             pt.y-position.y);
        }
        else return new Point(0,0);
    }

    /**
     * @return the position
     */
    public Point getPosition()
    {
        return position;
    }

    /**
     * @param position the position to set
     */
    public void setPosition(Point position)
    {
        this.position = position;
    }

    /**
     * @return the selected
     */
    public boolean isSelected()
    {
        return selected;
    }

    /**
     * @param selected the selected to set
     */
    public void deselect()
    {
        deselectAll();
        this.selected = false;
    }

    public void select(Point pt)
    {
        deselectAll();
        this.selected = true;
        if(pt!=null)
        {
            for(int i=0;i<classes.size();i++) //for(Element ele : classes) 
                { Element ele = classes.get(i); ele.setSelected(ele.isInside(pt)); }
            for(int i=0;i<fields.size();i++) //for(Element ele : fields) 
                { Element ele = fields.get(i); ele.setSelected(ele.isInside(pt)); }
            for(int i=0;i<methods.size();i++) //for(Element ele : methods) 
                { Element ele = methods.get(i); ele.setSelected(ele.isInside(pt)); }
        }
    }

    public Element getHover(Point pt)
    {
        Element ret = null;
        if(pt!=null)
        {
            for(int i=0;i<classes.size();i++) //for(Element ele : classes) 
                { Element ele = classes.get(i); if (ele.isInside(pt)) ret=ele; }
            for(int i=0;i<fields.size();i++) //for(Element ele : fields) 
                { Element ele = fields.get(i); if (ele.isInside(pt)) ret=ele; }
            for(int i=0;i<methods.size();i++) //for(Element ele : methods) 
                { Element ele = methods.get(i); if (ele.isInside(pt)) ret=ele; }
        }
        return ret;
    }


    public StringList getMethodTypes()
    {
        StringList sl = new StringList();

        for(int i=0;i<methods.size();i++) //
        //for(Element ele : fields)
        {
            try
            {
                Element ele = methods.get(i);
                if(ele.getNode() instanceof  MethodDeclaration)
                {
                    String type =((MethodDeclaration)ele.getNode()).getType().toString();
                    type = type.replace("[]", ""); 
                    //System.err.println(type);
                    sl.addIfNew(Unimozer.getTypesOf(type));
                }
            }
            catch (Exception e)
            {
                // ignore?
            }
        }

        return sl;
    }

    public StringList getFieldTypes()
    {
        StringList sl = new StringList();

        for(int i=0;i<fields.size();i++) //
        //for(Element ele : fields)
        {
            Element ele = fields.get(i);
            String type =((FieldDeclaration)ele.getNode()).getType().toString();
            type = type.replace("[]", ""); 
            //System.err.println(type);
            sl.addIfNew(Unimozer.getTypesOf(type));
        }

        return sl;
    }
 
    /**
     * @return the width
     */
    public int getWidth()
    {
        return width;
    }

    /**
     * @return the height
     */
    public int getHeight()
    {
        return height;
    }

    /**
     * @return the compiled
     */
    public boolean isCompiled()
    {
        return compiled;
    }

    /**
     * @return the extendsMyClass
     */
    public MyClass getExtendsMyClass()
    {
        if (extendsMyClass!=null)
        {
            if (extendsMyClass.getShortName().equals(extendsClass)) return extendsMyClass;
            else return null;
        }
        else return null;
    }

    /**
     * @param extendsMyClass the extendsMyClass to set
     */
    public void setExtendsMyClass(MyClass extendsMyClass)
    {
        this.extendsMyClass = extendsMyClass;
    }

    /**
     * @param compiled the compiled to set
     */
    public void setCompiled(boolean compiled)
    {
        this.compiled = compiled;
    }

    /**
     * @param extendsClass the extendsClass to set
     */
    public void setExtendsClass(String extendsClass)
    {
        this.extendsClass = extendsClass;
    }

    /**
     * @return the usesMyClass
     */
    public Vector<MyClass> getUsesMyClass()
    {
        return usesMyClass;
    }

    /**
     * @param usesMyClass the usesMyClass to set
     */
    public void setUsesMyClass(Vector<MyClass> usesMyClass)
    {
        this.usesMyClass = usesMyClass;
    }

    /**
     * @return the connector
     */
    public StringList getContent()
    {
        return content;
    }

    /**
     * @param content the content to set
     */
    public void setContent(StringList content)
    {
        this.content = content;
    }

    /*public void updateNSD(lu.fisch.structorizer.gui.Mainform form)
    {
        if(getSelected()!=null && form!=null)
        {
            getSelected().getName();

            StructorizerVisitor sv = new StructorizerVisitor(getSelected().getName());
            sv.visit(cu,null);

            // affect the new diagram to the editor
            form.diagram.root=sv.root;
            // redraw the diagram
            form.diagram.redraw();
            form.setVisible(true);
        }
    }*/

    public static Root setErrorNSD()
    {
        Root root = new Root();
        root.setText("---[ please select a method ]---");
        return root;
    }

    void updateNSD(lu.fisch.structorizer.gui.Diagram nsd)
    {
        this.nsd=nsd;
        if(nsd!=null)
        {
            boolean ERROR = false;
            if(getSelected()!=null)
            {
                getSelected().getName();

                if ((getSelected().getType()==Element.METHOD) || (getSelected().getType()==Element.CONSTRUCTOR))
                {
                    StructorizerVisitor sv = new StructorizerVisitor(getSelected().getName());
                    sv.visit(cu,null);

                    // affect the new diagram to the editor
                    nsd.setRoot(sv.root,false,true);
                    // redraw the diagram
                    //nsd.redraw();
                    // redraw the parent
                    nsd.getParent().getParent().repaint();
                    // make it fir
                    nsd.redraw();
                    nsd.setPreferredSize(new Dimension(nsd.getRoot().width,nsd.getRoot().height));
                    ((JScrollPane)nsd.getParent().getParent()).setViewportView(nsd);
                }
                else ERROR=true;
            }
            else ERROR=true;

            if(ERROR)
            {
                nsd.setRoot(setErrorNSD(),false,true);
                nsd.getParent().getParent().repaint();
            }
        }
    }

    private void updateNSD()
    {
        updateNSD(nsd);
    }

    /**
     * @return the isUML
     */
    public boolean isUML()
    {
        return isUML;
    }

    /**
     * @param isUML the isUML to set
     */
    public void setUML(boolean isUML)
    {
        this.isUML = isUML;
        for(int i=0;i<classes.size();i++) 
        //for(Element ele : classes)
        {
            classes.get(i).setUML(isUML);
        }
        for(int i=0;i<fields.size();i++) 
        //for(Element ele : fields)
        {
            fields.get(i).setUML(isUML);
        }
        for(int i=0;i<methods.size();i++) 
        //for(Element ele : methods)
        {
            methods.get(i).setUML(isUML);
        }
    }

    /**
     * @return the validCode
     */
    public boolean isValidCode()
    {
        return validCode;
    }

    /**
     * @param validCode the validCode to set
     */
    public void setValidCode(boolean validCode)
    {
        this.validCode = validCode;
        /*if(validCode==false)
        {
            fields.clear();
            classes.clear();
            methods.clear();
        }
         */
    }

    /**
     * @return the internalName
     */
    public String getInternalName()
    {
        if(classes.size()>0) return getShortName();
        else return internalName;
    }

    /**
     * @param internalName the internalName to set
     */
    public void setInternalName(String internalName)
    {
        this.internalName = internalName;
    }

    public Vector<Integer> getCodePositions()
    {
        // get the entire content into a single string
        String code = getContent().getText();

        // innit the positions
        int posFields = -1;
        int posConstructors = -1;
        int posMethods = -1;
        int posClass = -1;
        int posOver = -1;
        // init the states
        boolean inMethod = false;
        boolean inConstructor = false;
        // init
        String firstField = "";
        String firstConstructor = "";
        String firstMethod = "";
        String lastField = "";
        String lastConstructor = "";
        String lastMethod = "";
        int lastConstructorPos = -1;
        int lastMethodPos = -1;
        char lastNonBlank = ' ';

        // wipe out comments
        boolean inSComment = false;
        boolean inMComment = false;
        for(int i=0;i<code.length();i++)
        {
            if(code.charAt(i)=='/' && code.indexOf("/*",i)==i)
            {
                inMComment=true;
            }
            else if(code.charAt(i)=='*' && code.indexOf("*/",i)==i)
            {
                code=code.substring(0, i)+"  "+code.substring(i+2);
                inMComment=false;
            }
            else if(code.charAt(i)=='/' && code.indexOf("//",i)==i)
            {
                inSComment = true;
            }

            if(code.charAt(i)=='\n' && inSComment==true)
            {
                inSComment = false;
            }

            if(inSComment==true || inMComment==true)
            {
                code=code.substring(0, i)+" "+code.substring(i+1);
            }
        }


        // find methods and constructors
        int open = 0;
        String tmp = "";
        for(int i=0;i<code.length();i++)
        {
            if(code.charAt(i)=='{')
            {
                open++;                                // if last non blank symbol is a "=" we have an array!
                if((open == 2) && (lastNonBlank!='=')) // the previous thing is a constructor or a method
                {
                    if(tmp.contains(" "+this.getShortName()+"(")) // we have a constructor
                    {
                        //lastConstructor=tmp.substring(tmp.indexOf(this.getShortName()+"(")).trim();
                        lastConstructor=tmp.substring(tmp.lastIndexOf("\n", tmp.indexOf(" "+this.getShortName()+"("))).trim();
                        //lastConstructor=tmp.substring(tmp.trim().lastIndexOf("\n")).trim();
                        if(firstConstructor.equals("")) firstConstructor=lastConstructor;
                        inConstructor=true;
                    }
                    else
                    {
                        if(tmp.trim().contains("\n")) lastMethod = tmp.trim().substring(tmp.trim().lastIndexOf("\n")).trim();
                        else lastMethod = tmp.trim();
                        if(firstMethod.equals("")) firstMethod=lastMethod;
                        inMethod = true;
                    }
                    tmp="";
                }
                else if (open==1)
                {
                    posClass=i+1;
                }
            }
            else if(code.charAt(i)=='}')
            {
               open--;
               tmp="";
               if(open==0) posOver=i;
               else if (open==1)
               {
                    if(inConstructor)
                    {
                        posConstructors=i+1;
                        inConstructor=false;
                    }
                    else if (inMethod)
                    {
                        posMethods=i+1;
                        inMethod=false;
                    }
               }
            }
            else if ((code.charAt(i)==';') && (open==1)) 
            {
                if(!tmp.contains("abstract")) // the end of a field declaration
                {
                    tmp+=code.charAt(i);
                    if(tmp.trim().contains("\n")) lastField = tmp.trim().substring(tmp.trim().lastIndexOf("\n")).trim();
                    else lastField = tmp.trim();
                    if(firstField.equals("")) firstField=lastField;
                    posFields=i+1;
                    tmp="";
                }
                else //an abstract method (no body!)
                {
                    tmp+=code.charAt(i);
                    if(tmp.trim().contains("\n")) lastMethod = tmp.trim().substring(tmp.trim().lastIndexOf("\n")).trim();
                    else lastMethod = tmp.trim();
                    if(firstMethod.equals("")) firstMethod=lastMethod;
                }
            }
            else tmp+=code.charAt(i);

            String last = code.charAt(i)+"";
            if(!last.trim().equals("")) lastNonBlank=code.charAt(i);
        }

        if(firstField.equals("")) // no fields there
        {
            posFields = posClass;
        }
        if(firstConstructor.equals("")) // no constructor there
        {
            if(lastField.equals("")) // no field there either
            {
                posConstructors = posClass;
            }
            else
            {
                posConstructors = code.indexOf(lastField)+lastField.length();
            }
        }
        if(posMethods==-1) posMethods=posOver-1;

        Vector<Integer> res = new Vector<Integer>();

        // if there is an inline comment after the last field declaration
        code = getContent().getText();
        if(posFields<code.length()-1)
            while(!code.substring(posFields, posFields+1).equals("\n") && posFields<code.length())
            {
                posFields++;
            }
        if(posConstructors<code.length()-1)
            while(!code.substring(posConstructors, posConstructors+1).equals("\n") && posConstructors<code.length())
            {
                posConstructors++;
            }
        if(posMethods<code.length()-1)
            while(!code.substring(posMethods, posMethods+1).equals("\n") && posMethods<code.length())
            {
                posMethods++;
            }

        code=code.substring(0, posMethods)+"\n<METHOD>\n"+code.substring(posMethods);
        code=code.substring(0, posConstructors)+"\n<CONSTRUCTOR>"+code.substring(posConstructors);
        code=code.substring(0, posFields)+"\n<FIELD>"+code.substring(posFields);
        /*
        System.err.println("FF: "+firstField);
        System.err.println("LF: "+lastField);
        System.err.println("FC: "+firstConstructor);
        System.err.println("LC: "+lastConstructor);
        System.err.println("FM: "+firstMethod);
        System.err.println("LM: "+lastMethod);
        System.err.println("-------------------");
        System.err.println("FP "+posFields);
        System.err.println("CP "+posConstructors);
        System.err.println("MP "+posMethods);
        System.err.println("-------------------");
        System.err.println(code);
        System.err.println("-------------------");
        /**/

        res.add(posFields);
        res.add(posConstructors);
        res.add(posMethods);

        return res;
    }

    public void addPackage(String myPack)
    {
        String code = getContent().getText();
        code="package "+myPack+";\n\n"+code;
        loadFromString(code);
    }
    
    public void addImport(String myPack)
    {
        String code = getContent().getText();
        code="import "+myPack+";\n\n"+code;
        loadFromString(code);
    }
    
    public void addClassJavaDoc()
    {
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        java.util.Date date = new java.util.Date();
        String today = dateFormat.format(date);
        String jd =
             "/**\n"+
             " * Write a description of "+(isInterface()?"interface":"class")+" \""+getInternalName()+"\" here."+"\n"+
             " * "+"\n"+
             " * @author     "+System.getProperty("user.name")+"\n"+
             " * @version    "+today+"\n"+
             " */\n\n";

        String code = getContent().getText();
        code=jd+code;
        loadFromString(code);
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * @return the packagename
     */
    public String getPackagename()
    {
        return packagename;
    }

    /**
     * @param packagename the packagename to set
     */
    public void setPackagename(String packagename)
    {
        this.packagename = packagename;
    }

    public boolean hasField(String myName)
    {
        boolean found = false;
        for(int i=0;i<fields.size();i++)
        {
            Element e = fields.get(i);
            if (e.getSimpleName().equals(myName)) found=true;
        }
        return found;
    }

    /**
     * @return the constructors
     */
    /*
    public Vector<Element> getConstructors()
    {
        return constructors;
    }
    */

    public void addExtends(String className)
    {
        if (className!=null)
            if (!className.trim().equals(""))
                if (this.getExtendsClass().trim().equals(""))
                {
                    // find the line in which the class is being defined
                    ClassVisitor cv = new ClassVisitor();
                    cv.visit(cu,null);
                    int classLine = cv.getClassLine()-1;

                    // get the code line to change
                    String line = getContent().get(classLine);

                    // modifiy the code

                    /* possible patterns
                     * <modifier> class|interface some
                     * class|interface some
                     * <modifier> class|interface some implements <interfaces>
                     * class|interface some implements <interfaces>
                     *
                     * insert the code after the next word following "class" or "interface"
                    */
                    StringList words = StringList.explode(line, " ");
                    int posi = words.indexOf("class");
                    if (!words.contains("class"))
                    {
                        posi=words.indexOf("interface");
                    }
                    if (posi!=-1)
                    {
                        String toMod = words.get(posi+1).trim();
                        if(toMod.endsWith("{"))
                        {
                            toMod = toMod.subSequence(1, toMod.length()-1)+
                                    " extends "+
                                    className+" {";
                        }
                        else
                        {
                            toMod = toMod+
                                    " extends "+
                                    className;
                        }
                        words.delete(posi+1);
                        words.insert(toMod,posi+1);
                        line = words.getText().replace("\n", " ");
                    }

                    // put it back
                    getContent().delete(classLine);
                    getContent().insert(line, classLine);

                    // reload the class from the new code
                    loadFromString(getContent().getText());
                }
    }

    @Override
    public int getX()
    {
        return getPosition().x;
    }

    @Override
    public int getY()
    {
        return getPosition().y;
    }

    public boolean isChanged()
    {
        return changed;
    }

    public void setChanged(boolean changed)
    {
        this.changed = changed;
    }

    public long getLastModified()
    {
        return lastModified;
    }

    public void setLastModified(long lastModified)
    {
        this.lastModified = lastModified;
    }
    
    public Point getCenter()
    {
        return new Point(getX()+getWidth()/2,getY()+getHeight()/2);
    }
}
