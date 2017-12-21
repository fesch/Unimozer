/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lu.fisch.unimozer.interactiveproject;

import bsh.EvalError;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lu.fisch.unimozer.Diagram;
import lu.fisch.unimozer.MyClass;
import lu.fisch.unimozer.Objectizer;
import lu.fisch.unimozer.Runtime5;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Ronny
 */
public class InteractiveProject {
    
    
    private String type;
    
    private Objectizer objectizer;

    private String name;
    private ArrayList<String> classes = new ArrayList<>();
    private MyClass interfaceClass;
    private Object interfaceObject;
    private String interfaceAttribute;
    
    private String myPackage;
    private String main;
    private JFrame frame;
    private String path;
    
    private MyClass studentClass;
    private Object studentObject;
    
    
    private Diagram diagram;
    
    org.w3c.dom.Document document; //XML Document


    public InteractiveProject(String projectName, Diagram diagram)
    {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            document = db.parse(getClass().getResourceAsStream("/lu/fisch/unimozer/interactiveproject/projects.xml"));
            name = projectName;
            this.diagram = diagram;
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(InteractiveProject.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(InteractiveProject.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(InteractiveProject.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public JFrame getFrame() {
        return frame;
    }

    public MyClass getStudentClass() {
        return studentClass;
    }

    public void setFrame(JFrame frame) {
        this.frame = frame;
    }

    public void setObjectizer(Objectizer objectizer) {
        this.objectizer = objectizer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getClasses() {
        return classes;
    }

    public void setClasses(ArrayList<String> files) {
        this.classes = files;
    }

    public MyClass getInteractableClass() {
        return interfaceClass;
    }

    public void setInteractableClass(MyClass interactableClass) {
        this.interfaceClass = interactableClass;
    }

    public void loadFromXML(boolean open)
    {
        //boolean specifies if opening a project (true) or creating new project (false)
        
        try {
            //if file is null, use default xml

            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            
            type =  (String) xpath.compile("/projects/project[@id='"+name+"']/type").evaluate(document, XPathConstants.STRING);
            myPackage = (String) xpath.compile("/projects/project[@id='"+name+"']/package").evaluate(document, XPathConstants.STRING);
            main = (String) xpath.compile("/projects/project[@id='"+name+"']/main").evaluate(document, XPathConstants.STRING);
            String interfaceClassName = (String) xpath.compile("/projects/project[@id='"+name+"']/interface-class").evaluate(document, XPathConstants.STRING);
            
            String studentClassName = (String) xpath.compile("/projects/project[@id='"+name+"']/files/file[@type='student-class']").evaluate(document, XPathConstants.STRING);

            interfaceAttribute = (String) xpath.compile("/projects/project[@id='"+name+"']/interface-attribute").evaluate(document, XPathConstants.STRING);
            
            //load all associated files
            NodeList nl = (NodeList) xpath.compile("/projects/project[@id='"+name+"']/files/file").evaluate(document, XPathConstants.NODESET);
            
            if(path == null)
            {
                path = myPackage.replace('.', '/');
                path = "/"+path+"/";
            }
            //create a MyClass for each File and add to Diagram
            for (int i = 0; i < nl.getLength(); i++) {
                
                //when opening a project, don't load the student's class, as it is read from the project that is opened
                if(!open || !nl.item(i).getTextContent().equals(studentClassName))
                {
                    String filePath = path + nl.item(i).getTextContent()+".txt";
                    //System.out.println(filePath);
                    InputStream inStream =  getClass().getResourceAsStream(filePath);
                    String str = "";
                    StringBuffer strBuffer = new StringBuffer();
                    BufferedReader br = new BufferedReader(new InputStreamReader(inStream));

                    while((str = br.readLine())!=null)
                    {
                        strBuffer.append(str).append("\n");
                    }
                    MyClass myClass = new MyClass(strBuffer.toString(), true);

                    if(interfaceClassName.equals(nl.item(i).getTextContent()))
                    {
                        interfaceClass = myClass;
                        myClass.setDisplaySource(false);
                    }
                    else if(studentClassName.equals(nl.item(i).getTextContent()))
                        studentClass = myClass;
                    else{
                        myClass.setDisplayUML(false);
                        myClass.setDisplaySource(false);
                    }

                    diagram.addClass(myClass);

                    //add classes to interactiveProject
                    classes.add(myPackage+"."+nl.item(i).getTextContent());
                }
            }
        } catch (XPathExpressionException ex) {
            Logger.getLogger(InteractiveProject.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(InteractiveProject.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public void runProject() {
        //loads the MainFrame of the called Project
        try {
            if(diagram.compile())
            {
                Runtime5.getInstance().load(myPackage + "." + main);
                //Class.forName(myPackage + "." + main);
                //Object mainObject = Class.forName(myPackage + "." + main).newInstance();
                
                Object mainObject = Runtime5.getInstance().getInstance("MainFrame", "new " + myPackage + "." + main + "()");
                if(type.equals("controller-based"))
                {
                    Method method = mainObject.getClass().getMethod("getInterfaceObject",null);
                    interfaceObject = method.invoke(mainObject,null);
                    
                    objectizer.addInteractiveObject(interfaceAttribute, interfaceObject);
                    studentObject= Runtime5.getInstance().getInstance("Controller", "new " + myPackage + ".Controller()");
                    
                    //System.out.println(interfaceClass.getShortName());
                    method = studentObject.getClass().getMethod("set"+interfaceClass.getShortName(), Runtime5.getInstance().load(myPackage+"."+interfaceClass.getShortName()));
                    interfaceObject = method.invoke(studentObject, interfaceObject);
                    objectizer.addObject("controller", studentObject);
                }
                else if(type.equals("model-based"))
                {
                    Method method = mainObject.getClass().getMethod("getStudentObject",null);
                    studentObject = method.invoke(mainObject,null);
                    objectizer.addObject(studentClass.getShortName(), studentObject);
                }
                
                
                
                frame = (JFrame) mainObject;
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setAlwaysOnTop(true);
                frame.setVisible(true);
            }
            
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(InteractiveProject.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(InteractiveProject.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(InteractiveProject.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            Logger.getLogger(InteractiveProject.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(InteractiveProject.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(InteractiveProject.class.getName()).log(Level.SEVERE, null, ex);
        } catch (EvalError ex) {
            Logger.getLogger(InteractiveProject.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
