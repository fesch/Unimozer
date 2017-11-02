/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lu.fisch.unimozer.interactiveproject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import lu.fisch.unimozer.Diagram;
import lu.fisch.unimozer.MyClass;
import lu.fisch.unimozer.Objectizer;

/**
 *
 * @author Ronny
 */
public class InteractiveProject {

    private Objectizer objectizer;

    private String name;
    private ArrayList<String> classes = new ArrayList<>();
    private MyClass interactableClass;
    private String myPackage;
    private String main;
    private JFrame frame;

    public InteractiveProject(String name, String myPackage, String main) {
        this.name = name;
        this.myPackage = myPackage;
        this.main = main;
    }

    public JFrame getFrame() {
        return frame;
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
        return interactableClass;
    }

    public void setInteractableClass(MyClass interactableClass) {
        this.interactableClass = interactableClass;
    }

    public void runProject() {
        //loads the MainFrame of the called Project
        try {
            Object obj = Class.forName(myPackage + "." + main).newInstance();
            //InteractiveMainFrame iFrame = (InteractiveMainFrame) obj;
            Method method = obj.getClass().getMethod("getPlayer", null);
            //System.out.println(method.invoke(obj, null).getClass());
            //Class.forName(myPackage + ".Player").cast(obj);
            objectizer.addInteractiveObject("player",method.invoke(obj, null));
            frame = (JFrame) obj;
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setVisible(true);
            
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(InteractiveProject.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
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
        }
    }
}
