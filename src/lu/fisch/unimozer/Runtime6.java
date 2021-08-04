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

import bsh.EvalError;
import bsh.Interpreter;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import lu.fisch.unimozer.compilation.ByteClassLoader;
import lu.fisch.unimozer.compilation.ByteJavaFileManager;
import lu.fisch.unimozer.compilation.JavaSourceFromString;
import lu.fisch.unimozer.console.Console;
import lu.fisch.unimozer.utils.StringList;

/**
 *
 * @author robertfisch
 */
public class Runtime6
{
    private JavaCompiler compiler = null;
    private ByteClassLoader classLoader =  null;
    private Interpreter interpreter = new Interpreter();

    protected static Runtime6 runtime = null;

    private String rootDirectory = null;

    private Runtime6()
    {
        //compiler =  javax.tools.ToolProvider.getSystemJavaCompiler();
        compiler = com.sun.tools.javac.api.JavacTool.create();
    }

    public static Runtime6 getInstance(Interpreter interpreter, String rootDirectory)
    {
        if (runtime==null) runtime = new Runtime6();
        runtime.interpreter=interpreter;
        runtime.rootDirectory=rootDirectory+System.getProperty("file.separator")+"src";
        return runtime;
    }

    // the input always contains <Classname, Javacode>
    void compileToPath(File[] files, String path, String target, String classPath) throws ClassNotFoundException
    {
            // create a diagnostic collector to collect eventually errors
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
            // create a standard manager for the Java file objects
            StandardJavaFileManager fileMan = compiler.getStandardFileManager(diagnostics, null, null);

            // create the iterable from the list
            Iterable<? extends JavaFileObject> compilationUnits = fileMan.getJavaFileObjectsFromFiles(Arrays.asList(files));

            // create a store for the compiled code
            Map<String, JavaFileObject> store = new HashMap<String, JavaFileObject>();

            // set compileroptions for target JVM
            //System.err.println(path+"bin/");
            String[] options = new String[]{"-d",path};
            Iterable<String> myOptions = null;
            if(target!=null) 
            {
                options = new String[]{"-d",path,"-source", target,"-target", target, "-cp", classPath};
            }
            myOptions = Arrays.asList(options);
            //for(int i=0;i<options.length;i++) System.err.print(options[i]+" // ");
            // compile the file
            CompilationTask task = compiler.getTask(null, fileMan, diagnostics, myOptions , null, compilationUnits);

            boolean success = task.call();
            String error = new String();
            for (Diagnostic diagnostic : diagnostics.getDiagnostics())
            {
                error+=diagnostic.getMessage(null)+"\n";
            }
            //System.out.println("Success: " + success);
            if(success==true)
            {
                // debug
                //System.out.println("Loading: "+this.getShortName());
                // create the class loader
                classLoader = new ByteClassLoader(store,rootDirectory);
                // load the specified class
                //Class<?> cl = classLoader.loadClass(this.getShortName());
            }
            else
            {
                error=error.replaceAll("string:///", "");
                //error=error.replaceAll("\\.java", "");
                throw new ClassNotFoundException(error.trim());
            }
    }

    // the input always contains <Classname, Javacode>
    void compile(Hashtable<String, String> codes, String classPath) throws ClassNotFoundException
    {
        //System.out.println("TZ: compile6");
        
            // create a diagnostic collector to collect eventually errors
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
            // get the code for all dependant classes and put it in a list
            JavaFileObject[] sources = new JavaFileObject[codes.size()];
            Set<String> set = codes.keySet();
            Iterator<String> itr = set.iterator();
            int i = 0;
            while (itr.hasNext())
            {
                String filename = itr.next();
                //Console.disconnectAll();
                //System.out.println("Adding: "+filename);
                String content = (String) codes.get(filename);
                sources[i]=new JavaSourceFromString(filename, content);
                i++;
            }
            // create the iterable from the list
            Iterable<? extends javax.tools.JavaFileObject> compilationUnits = Arrays.asList(sources);

            // create a store for the compiled code
            Map<String, JavaFileObject> store = new HashMap<String, JavaFileObject>();
            // create a standard manager for the Java file objects
            StandardJavaFileManager fileMan = compiler.getStandardFileManager(diagnostics, null, null);
            //fileMan.getL
            // forward most calls to the standard Java file manager but put the
            // compiled classes into the store with ByteJavaFileManager
            ByteJavaFileManager jfm = new ByteJavaFileManager(fileMan, store);


            // set compileroptions for target JVM
            //System.err.println(classPath);
            String[] options = new String[]{"-classpath", classPath};
            Iterable<String> myOptions = null;
            myOptions = Arrays.asList(options);

            // compile the file
            CompilationTask task = compiler.getTask(null, jfm, diagnostics, myOptions, null, compilationUnits);

            boolean success = task.call();
            String error = new String();
            for (Diagnostic diagnostic : diagnostics.getDiagnostics())
            {
                /*Console.disconnectAll();
                System.out.println(diagnostic.getCode());
                System.out.println(diagnostic.getKind());
                System.out.println(diagnostic.getPosition());
                System.out.println(diagnostic.getStartPosition());
                System.out.println(diagnostic.getEndPosition());
                System.out.println(diagnostic.getSource());
                System.out.println(diagnostic.getMessage(null));
                System.out.println(((JavaSourceFromString)diagnostic.getSource()).getName());
                System.out.println(diagnostic.getLineNumber());
                /**/
                
                // get the name of the class that has been compiled
                String classname = "<UNKNOWN>";
                if(diagnostic.getSource()!=null)
                {
                    try
                    {
                        classname = ((JavaSourceFromString)diagnostic.getSource()).getName();
                    }
                    catch(Exception e)
                    {
                        System.err.println(e.getMessage());
                        System.err.println(diagnostic.getMessage(null));
                        System.err.println(diagnostic.getSource().getClass());
                    }
                }
                else
                {
                    classname = "<NONE>";
                }
                //classname=classname.replace(".java","");
                // get the message
                String message = diagnostic.getMessage(null);

                
                /*
                    Console.disconnectAll();
                    System.err.println(message);
                    Console.connectAll();
                */
                
                // remove unused things
                message=message.replaceAll("string:///", "");
                // check if it's an old (JDK6) message, then trunk it
                if(message.startsWith(classname))
                {
                    StringList sl = StringList.explode(message,":");
                    message = "";
                    for(int m=2;m<sl.count();m++)
                    {
                        message+=sl.get(m);
                    }           
                }
                // remove not used lines
                if(message.contains("\n"))
                {
                    String what = "";
                    StringList part = StringList.explode(message, "\n");
                    if(part.count()>1)
                        if(part.get(1).contains("symbol"))
                        {
                            part = StringList.explode(part.get(1),":");
                            what = part.get(1).trim();
                        }
                    message=message.substring(0,message.indexOf("\n"));
                    if(!what.equals(""))
                        message += ": "+what+"";
                }
                // put everything together
                message=classname+"@"+diagnostic.getLineNumber()+"@"+message.trim();
                
                /*
                System.out.println("MSG = "+message);  
                Console.connectAll();
                /**/
                if(diagnostic.getLineNumber()!=-1)
                    error+=message+"\n";
                else
                {
                    Console.disconnectAll();
                    System.err.println(message);
                    Console.connectAll();
                }
            }
            //System.out.println("Success: " + success);
            if(success==true)
            {
                // debug
                //System.out.println("Loading: "+this.getShortName());
                // create the class loader
                /*Console.disconnectAll();
                System.out.println("root: "+rootDirectory);
                Console.connectAll();/**/
                classLoader = new ByteClassLoader(store,rootDirectory);
                // load the specified class
                //Class<?> cl = classLoader.loadClass(this.getShortName());
 
            }
            else
            {
                error=error.replaceAll("string:///", "");
                error=error.replaceAll("\\.java", "");
                throw new ClassNotFoundException(error.trim());
            }
            
            
    }

    public Class<?> load(String classname) throws ClassNotFoundException
    {
        try
        {
            interpreter.setClassLoader(classLoader);
            return classLoader.loadClass(classname);
        }
        catch (ClassNotFoundException ex)
        {
            // if a class is not found, it's because it has not been compiled correctely :-(
            if(ex.getCause()==null) throw ex;
            throw new ClassNotFoundException(ex.getCause().getMessage(), ex.getCause());
        }

    }



}
