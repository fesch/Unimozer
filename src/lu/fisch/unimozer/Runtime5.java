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
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import lu.fisch.unimozer.compilation.ByteClassLoader;
import lu.fisch.unimozer.console.Console;
import org.codehaus.janino.CompileException;
import org.codehaus.janino.DebuggingInformation;
import org.codehaus.janino.JavaSourceClassLoader;
import org.codehaus.janino.Parser.ParseException;
import org.codehaus.janino.Scanner.ScanException;
import org.codehaus.janino.util.StringPattern;
import org.codehaus.janino.util.enumerator.EnumeratorSet;
import org.codehaus.janino.util.resource.MapResourceFinder;

/**
 *
 * @author robertfisch
 */
public class Runtime5
{
    protected static final int COMPILER_SUN = 0;
    protected static final int COMPILER_JANINO = 1;

    private ClassLoader janinoClassLoader = new JavaSourceClassLoader(ClassLoader.getSystemClassLoader(),
                                                                       new MapResourceFinder(new Hashtable()),
                                                                       (String) null,
                                                                       DebuggingInformation.NONE);
    private javax.tools.JavaCompiler compiler = null;
    private ByteClassLoader classLoader =  null;
    public Interpreter interpreter;

    protected int compilerUsed = -1;

    protected static Runtime5 runtime = null;

    private String rootDirectory = null;


    protected Runtime5()
    {
        interpreter = new Interpreter();
        //interpreter.DEBUG=true;
        
        //interpreter.setErr(Console.getErr());
        //interpreter.setOut(Console.getOut());

        //interpreter.setStrictJava(true);
        //interpreter.getNameSpace().
        //new Thread(interpreter).start();

        try
        {
            // try to get the system compiler
            //compiler =  javax.tools.ToolProvider.getSystemJavaCompiler();
            compiler = com.sun.tools.javac.api.JavacTool.create();
        }
        catch(NoClassDefFoundError ex)
        {
            compiler = null;
        }
        finally
        {
            // determine now which compiler to use
            if (compiler == null) compilerUsed=COMPILER_JANINO;
            else compilerUsed = COMPILER_SUN;
        }
        
        Console.disconnectAll();
        System.out.println("Using compile: "+compilerUsed);
        Console.connectAll();
    }

    public static Runtime5 getInstance()
    {
        if (runtime==null) runtime = new Runtime5();
        //runtime.setConsole(console);
        return runtime;
    }

    public boolean usesSunJDK()
    {
        return (compilerUsed == COMPILER_SUN);
    }

    void compileToPath(File[] files, String path, String target, String myClassPath) throws ClassNotFoundException, IOException, ScanException, ParseException, CompileException
    {
        try
        {
            if(Main.classpath!=null)
            {
                if(myClassPath.length()>0)
                    if(System.getProperty("os.name").toLowerCase().contains("windows"))
                        myClassPath+=";";
                    else
                        myClassPath+=":";
                myClassPath+=Main.classpath;
            }
            Unimozer.messages.add("CompileToPath using CP = "+myClassPath);
        }
        catch (Error e) {}

        
        if (compilerUsed == COMPILER_JANINO)
        {
            File            destinationDirectory      = new File(path);
            File[]          optionalSourcePath        = null;
            File[]          classPath                 = { new File(".") };
            File[]          optionalExtDirs           = null;
            File[]          optionalBootClassPath     = null;
            String          optionalCharacterEncoding = null;
            boolean         verbose                   = false;
            EnumeratorSet   debuggingInformation      = DebuggingInformation.DEFAULT_DEBUGGING_INFORMATION;
            StringPattern[] warningHandlePatterns     = org.codehaus.janino.Compiler.DEFAULT_WARNING_HANDLE_PATTERNS;
            boolean         rebuild                   = false;

            org.codehaus.janino.Compiler jCompiler = new org.codehaus.janino.Compiler(
                optionalSourcePath,
                classPath,
                optionalExtDirs,
                optionalBootClassPath,
                destinationDirectory,
                optionalCharacterEncoding,
                verbose,
                debuggingInformation,
                warningHandlePatterns,
                rebuild
            );
            
            jCompiler.compile(files);
        }
        else if (compilerUsed == COMPILER_SUN)
        {
            Runtime6.getInstance(interpreter,rootDirectory).compileToPath(files,path,target,myClassPath);
        }
        else System.out.println("Runtime.compile: This should never happen!");
    }
    
    // the input always contains <Classname, Javacode>
    private void compile(Hashtable<String, String> codes, String classpath) throws ClassNotFoundException
    {
        
        try
        {
            if(Main.classpath!=null)
            {
                if(classpath.length()>0)
                    if(System.getProperty("os.name").toLowerCase().contains("windows"))
                        classpath+=";";
                    else
                        classpath+=":";
                classpath+=Main.classpath;
            }
            Unimozer.messages.add("Compile using CP = "+classpath);
            /*Console.disconnectAll();
            System.out.println("CP: "+classpath);
            Console.connectAll();*/
            //System.out.println("CP: "+classpath);
        }
        catch (Error e) 
        {
            e.printStackTrace();
        }
        
                
        if (compilerUsed == COMPILER_JANINO)
        {
            // we need to convert the content
            Hashtable input = new Hashtable();
            Set<String> set = codes.keySet();
            Iterator<String> itr = set.iterator();
            while (itr.hasNext())
            {
                String classname = itr.next();
                String javacode = (String) codes.get(classname);
                // append ".java" to the classname
                // transform into byte-array
                //System.err.println("Adding "+classname+".java");
                input.put(classname+".java", javacode.getBytes());
            }
            // load the classes
            janinoClassLoader = new JavaSourceClassLoader(
                                        this.getClass().getClassLoader(),  // parentClassLoader
                                        new MapResourceFinder(input),      // optionalSourcePath
                                        (String) null,                     // optionalCharacterEncoding
                                        DebuggingInformation.NONE          // debuggingInformation
                                );

        }
        else if (compilerUsed == COMPILER_SUN)
        {
            //System.out.println("TZ: compile");
            // forward calls to the RunTime6
            Runtime6.getInstance(interpreter,rootDirectory).compile(codes,classpath);
            
            
/*
            // create a diagnostic collector to collect eventually errors
            javax.tools.DiagnosticCollector<javax.tools.JavaFileObject> diagnostics = new javax.tools.DiagnosticCollector<javax.tools.JavaFileObject>();
            // get the code for all dependant classes and put it in a list
            javax.tools.JavaFileObject[] sources = new javax.tools.JavaFileObject[codes.size()];
            Set<String> set = codes.keySet();
            Iterator<String> itr = set.iterator();
            int i = 0;
            while (itr.hasNext())
            {
                String filename = itr.next();
                String content = (String) codes.get(filename);
                sources[i]=new JavaSourceFromString(filename, content);
                i++;
            }
            // create the iterable from the list
            Iterable<? extends javax.tools.JavaFileObject> compilationUnits = Arrays.asList(sources);

            // create a store for the compiled code
            Map<String, javax.tools.JavaFileObject> store = new HashMap<String, javax.tools.JavaFileObject>();
            // create a standard manager for the Java file objects
            javax.tools.StandardJavaFileManager fileMan = compiler.getStandardFileManager(diagnostics, null, null);
            // forward most calls to the standard Java file manager but put the
            // compiled classes into the store with ByteJavaFileManager
            ByteJavaFileManager jfm = new ByteJavaFileManager(fileMan, store);

            // compile the file
            javax.tools.JavaCompiler.CompilationTask task = compiler.getTask(null, jfm, diagnostics, null, null, compilationUnits);

            boolean success = task.call();
            String error = new String();
            for (javax.tools.Diagnostic diagnostic : diagnostics.getDiagnostics())
            {
              //System.out.println(diagnostic.getCode());
              //System.out.println(diagnostic.getKind());
              //System.out.println(diagnostic.getPosition());
              //System.out.println(diagnostic.getStartPosition());
              //System.out.println(diagnostic.getEndPosition());
                error+=diagnostic.getMessage(null)+"\n";
              //System.out.println(diagnostic.getSource());
              //System.out.println(diagnostic.getMessage(null));
            }
            //System.out.println("Success: " + success);
            if(success==true)
            {
                // debug
                //System.out.println("Loading: "+this.getShortName());
                // create the class loader
                classLoader = new ByteClassLoader(store);
                // load the specified class
                //Class<?> cl = classLoader.loadClass(this.getShortName());
            }
            else
            {
                error=error.replaceAll("string:///", "");
                error=error.replaceAll("\\.java", "");
                throw new ClassNotFoundException(error.trim());
            }
*/
        }
        else System.out.println("Runtime.compile: This should never happen!");
    }

    public Class<?> load(String classname) throws ClassNotFoundException
    {
        try
        {
            if (compilerUsed == COMPILER_SUN)
            {
                // no need to set the classloader because this
                // is done in Runtime6.getInstance(interpreter)
                // somewhere beneath
                //interpreter.setClassLoader(classLoader);
            }
            else if (compilerUsed == COMPILER_JANINO)
            {
                interpreter.setClassLoader(janinoClassLoader);
            }
            else System.err.println("Runtime.load: This should never happen!");

            interpreter.getNameSpace().importClass(classname);

            if (compilerUsed == COMPILER_SUN) return Runtime6.getInstance(interpreter,rootDirectory).load(classname); //classLoader.loadClass(classname);
            else if (compilerUsed == COMPILER_JANINO) return janinoClassLoader.loadClass(classname);
            else return null; // this should never happen!
        }
        catch (ClassNotFoundException ex)
        {
            // if a class is not found, it's because it has not been compiled correctely :-(
            if(ex.getCause()!=null) throw new ClassNotFoundException(ex.getCause().getMessage(), ex.getCause());
            else throw new ClassNotFoundException(ex.getMessage(), ex.getCause());
        }

    }

    public void compileAndLoad(Hashtable<String, String> codes, String classpath) throws ClassNotFoundException
    {
        //System.out.println("TZ: comileAndLoad");
        compile(codes,classpath);
        //if(classname==null) return null;
        //else return load(classname);
    }

    public Object getInstance(String identifier, String constructor) throws EvalError
    {
        interpreter.eval(identifier+" = "+constructor);
        //System.out.println(identifier+" = "+constructor);
        Object instance = interpreter.get(identifier);
        return instance;
    }

    /**
     * @return the rootDirectory
     */
    public String getRootDirectory()
    {
        return rootDirectory;
    }

    /**
     * @param rootDirectory the rootDirectory to set
     */
    public void setRootDirectory(String rootDirectory)
    {
        this.rootDirectory = rootDirectory;
    }

    private class ReturnObject
    {
        public Object object = null;
        public EvalError error = null;
    }

    public Object executeMethod(String method) throws EvalError
    {
        //System.err.println("Executing: "+method);
        //System.out.println(method);
        /*
        final Object returnLock = new Object();

        final ReturnObject retObj = new ReturnObject();

        final String m = method;

        final Runtime5 run = this;
        
        Runnable r = new Runnable()
        {
                public void run()
                {
                        synchronized(returnLock)
                        {
                            try
                            {
                                retObj.object = interpreter.eval(m);
                            }
                            catch (EvalError ex)
                            { 
                                retObj.error = ex;
                            }
                            run.notify();
                        }
                }
        };
        Thread t = new Thread(r);
        t.start();
        try
        {
            t.join();
            // Finished
        }
        catch (InterruptedException e)
        {
            System.out.println("Thread way interrupted ...");
        }

        synchronized(returnLock)
        {
            if(retObj.error!=null) throw retObj.error;
            else return retObj.object;
        }
        */
        //System.out.println("> "+method);
        return interpreter.eval(method);
    }
    
    public void setObject(String name, Object object) throws EvalError
    {
        interpreter.set(name, object);
    }

    public Object executeCommand(String cmd) throws EvalError
    {
        //System.out.println(interpreter);
        //System.out.println(method);
        cmd=cmd.replace("\\", "\\\\");
        return interpreter.eval(cmd);
    }

    /*public void setConsole(ConsoleInterface in)
    {
        interpreter.setConsole(in);
    }

    public void setErr(PrintStream ps)
    {
        interpreter.setErr(ps);
    }

    public void setOut(PrintStream ps)
    {
        interpreter.setOut(ps);
    }*/

}
