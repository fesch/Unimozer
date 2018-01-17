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
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.ModifierSet;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import javax.jnlp.ClipboardService;
import javax.jnlp.ServiceManager;
import javax.swing.*;
import javax.swing.Timer;
import lu.fisch.filefilter.PNGFilter;
import lu.fisch.structorizer.gui.PrintPreview;
import lu.fisch.unimozer.aligner.Area;
import lu.fisch.unimozer.aligner.Grille;
import lu.fisch.unimozer.compilation.CompilationError;
import lu.fisch.unimozer.console.Console;
import lu.fisch.unimozer.dialogs.*;
import lu.fisch.unimozer.interactiveproject.InteractiveProject;
import lu.fisch.unimozer.utils.CopyDirectory;
import lu.fisch.unimozer.utils.StringList;
import net.iharder.dnd.FileDrop;
import org.apache.commons.io.FileUtils;
import org.codehaus.janino.CompileException;
import org.codehaus.janino.Parser.ParseException;
import org.codehaus.janino.Scanner.ScanException;

/**
 *
 * @author robertfisch
 */
public class Diagram extends JPanel implements MouseListener, MouseMotionListener, ActionListener, Printable
{
    public final static int MODE_SELECT = 0;
    public final static int MODE_EXTENDS = 1;

    private int mode = MODE_SELECT;
    private MyClass extendsFrom = null;
    private MyClass extendsTo = null;
    private Point extendsDragPoint = null;

    private ConcurrentHashMap<String,MyClass> classes = new ConcurrentHashMap<String,MyClass>();
    private Vector<String> removedClasses = new Vector<String>();
    private ConcurrentHashMap<String,Package> packages = new ConcurrentHashMap<String,Package>();
    private Point mousePoint = new Point(0,0);
    private Point mouseRelativePoint = new Point(0,0);
    private boolean mousePressed = false;
    private MyClass mouseClass = null;

    private Package mousePackage = null;

    private Point commentPoint = null;
    private String commentString = null;

    private CodeEditor editor = null;
    private Mainform frame = null;
    private Objectizer objectizer = null;
    private Diagram diagram = this;
    private JLabel status = null;

    private JPopupMenu popup = new JPopupMenu();

    private String directoryName = null;
    private String containingDirectoryName = null;

    private CodeReader codeReader = null;

    private boolean showHeritage = true;
    private boolean showComposition = true;
    private boolean showAggregation = true;

    private boolean showFields  = true;
    private boolean showMethods = true;

    private boolean isUML = true;

    private final boolean allowEdit = false;
    private boolean hasChanged = true;
    private boolean hasChangedAutoSave = true;

    private static final float dash[] = {5.0f, 2.0f};
    public static final BasicStroke dashed = new BasicStroke(1.0f, BasicStroke.CAP_BUTT,
                                                              BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);

    private lu.fisch.structorizer.gui.Mainform structorizer = null;
    private lu.fisch.structorizer.gui.Diagram nsd = null;

    private Vector<String> pageList = new Vector<String>();
    private PrintOptions printOptions = new PrintOptions();

    private int mostRight;
    private int mostBottom;

    private String lastSaved = null;
    private Vector<File> toBeDeleted = new Vector<File>();

    // auto save timer
    private Timer saveTimer;
    private int AUTOSAVE_TIMEOUT = 1000*60*10; // 10 minutes
    
    private Point topLeft = new Point(0,0);
    private Point bottomRight = new Point(0,0);

    //is null if it isn't an interactive project
    private InteractiveProject interactiveProject = null;
    
    //private Vector<BufferedImage> sheets = new Vector<BufferedImage>();

    public Diagram()
    {
        super();

        setDoubleBuffered(true);

        /*this.setFocusable(true);
        this.addKeyListener(new KeyListener() {
                    public void keyTyped(KeyEvent e) {
                        SwingUtilities.processKeyBindings(e);
                    }
                    public void keyPressed(KeyEvent e) {
                        System.err.println("Pressed: "+e.getKeyText(e.getKeyCode()));
                        SwingUtilities.processKeyBindings(e);
                    }
                    public void keyReleased(KeyEvent e) {
                        SwingUtilities.processKeyBindings(e);
                    }
                });/**/

        this.setLayout(new BorderLayout());
        this.addMouseListener(this);
        this.addMouseMotionListener(this);

        this.addMouseListener(new PopupListener());
        this.add(popup);
        // set the filedropper for the diagram
        FileDrop fileDrop = new FileDrop(this, new FileDrop.Listener()
        {

            @Override
            public void filesDropped(java.io.File[] files)
            {
                boolean found = false;
                for (int i = 0; i < files.length; i++)
                {
                    String filename = files[i].toString();
                    File f = new File(filename);
                    if (filename.substring(filename.length() - 5, filename.length()).toLowerCase().equals(".java"))
                    {
                        try
                        {
                            //MyClass mc = new MyClass(new FileInputStream(filename));
                            MyClass mc = new MyClass(filename, Unimozer.FILE_ENCODING);
                            mc.setPosition(new Point(0, 0));
                            addClass(mc);
                            setChanged(true);
                            diagram.setChanged(true);
                        }
                        catch (Exception ex)
                        {
                            MyError.display(ex);
                        }
                    }
                    else if (f.isDirectory())
                    {
                        if (((PackageFile.exists(f) == true) || 
                            (BlueJPackageFile.exists(f) == true) || 
                            (NetBeansPackageFile.exists(f) == true) 
                           )
                           && (f.isDirectory()))
                        {
                            final String fim = filename;
                            (new Thread(new Runnable() {

                                @Override
                                public void run() {
                                    if (askToSave() == true)
                                    {
                                        //Console.disconnectAll();
                                        //System.out.println("Opening: "+fim);
                                        diagram.open(fim);
                                    }
                                }
                            })).start();
                        }
                        else
                        {
                            addDir(f);
                        }
                    }
                }
                diagram.repaint();
                frame.setTitleNew();
            }

        });

        // start the auto-save timer (after 10 minutes)
        saveTimer = new Timer(AUTOSAVE_TIMEOUT,autoSave);
        //saveTimer.start();
        // => it is started upon the first change!
    }

    private void addDir(File dir)
    {
        addDir(dir,true);
    }

    private void addDir(File dir, boolean showError)
    {
        //System.out.println("Adding directory: "+dir.getAbsolutePath());
        // get all files
        if(!dir.exists()) return;
        File[] files = dir.listFiles();
        for (int f = 0; f < files.length; f++)
        {
            if (files[f].isDirectory())
            {
                addDir(files[f],showError);
            }
            else
            {
                if (files[f].getAbsolutePath().toLowerCase().endsWith(".java"))
                {
                    try
                    {
                        //MyClass mc = new MyClass(new FileInputStream(filename));
                        MyClass mc = new MyClass(files[f].getAbsolutePath(), Unimozer.FILE_ENCODING);
                        if(!removedClasses.contains(mc.getFullName()))
                        {
                            mc.setPosition(new Point(0, 0));
                            addClass(mc,false,showError);
                        }
                        //setChanged(true);
                        //diagram.setChanged(true);
                    }
                    catch (Exception ex)
                    {
                        if(showError)
                            MyError.display(ex);
                    }
                }
            }
        }
    }

    // define an action
    ActionListener autoSave = new ActionListener()
    {
            @Override
            public void actionPerformed(ActionEvent evt)
            {
                    //doAutoSave();
            }
    };

    public void doAutoSave()
    {
        //if (saveTimer.isRunning()) saveTimer.stop();
        if(hasChangedAutoSave==true)
        {
            // has the project allready been saved once?
            //if(directoryName==null)
            //{
                // no, so ask to save it right now
                //custom title, warning icon
                Object[] options = {"Yes",
                                    "No"};
                int n = JOptionPane.showOptionDialog(frame,
                    "You didn't save your project yet!\n"
                    + "Do you want to save it right now?",
                    "Auto save",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);
                if (n==0) 
                {
                    this.saveUnimozer();
                    hasChangedAutoSave=false;
                }
            //}
/*            else
            {
                // yes, so go ahead an create a backup
                createBackup();
                hasChangedAutoSave=false;
            }
            saveTimer.start();
*/
        }
        else if (hasChanged)
        {
            Object[] options = {"Yes",
                                "No"};
            int n = JOptionPane.showOptionDialog(frame,
                "You didn't save your project for at least 10 minutes!\n"
                + "Do you want to save it right now?",
                "Auto save",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
            if (n==0) 
            {
                this.saveUnimozer();
            }
        }
            
    }

    private void drawExtends(Graphics2D g, Point pFrom, Point pTo)
    {
        int ARROW_SIZE = 16;
        double ARROW_ANGLE = Math.PI / 6;

        //g.setColor(Color.BLACK);
        double angle = Math.atan2(-(pFrom.y - pTo.y), pFrom.x - pTo.x);

        Point pArrow = new Point(pTo.x + (int) ((ARROW_SIZE - 2) * Math.cos(angle)), pTo.y
                - (int) ((ARROW_SIZE - 2) * Math.sin(angle)));

        // draw the arrow head
        int[] xPoints = {pTo.x, pTo.x + (int) ((ARROW_SIZE) * Math.cos(angle + ARROW_ANGLE)),
                pTo.x + (int) (ARROW_SIZE * Math.cos(angle - ARROW_ANGLE))};
        int[] yPoints = {pTo.y, pTo.y - (int) ((ARROW_SIZE) * Math.sin(angle + ARROW_ANGLE)),
                pTo.y - (int) (ARROW_SIZE * Math.sin(angle - ARROW_ANGLE))};

        g.drawPolygon(xPoints, yPoints, 3);
        g.drawLine(pFrom.x, pFrom.y, pArrow.x, pArrow.y);
    }

    public void setInteractiveProject(InteractiveProject interactiveProject) {
        this.interactiveProject = interactiveProject;
    }

    public InteractiveProject getInteractiveProject() {
        return interactiveProject;
    }
    
    public Point2D getIntersection(Point2D p1, Point2D p2, Point2D p3, Point2D p4)
    {
        double x1 = p1.getX();
        double y1 = p1.getY();
        double x2 = p2.getX();
        double y2 = p2.getY();
        double x3 = p3.getX();
        double y3 = p3.getY();
        double x4 = p4.getX();
        double y4 = p4.getY();

        double xD1,yD1,xD2,yD2,xD3,yD3;
        double dot,deg,len1,len2;
        double segmentLen1,segmentLen2;
        double ua,ub,div;
        double xi,yi;

        // calculate differences
        xD1=p2.getX()-p1.getX();
        xD2=p4.getX()-p3.getX();
        yD1=p2.getY()-p1.getY();
        yD2=p4.getY()-p3.getY();
        xD3=p1.getX()-p3.getX();
        yD3=p1.getY()-p3.getY();

        // calculate the lengths of the two lines
        len1=Math.sqrt(xD1*xD1+yD1*yD1);
        len2=Math.sqrt(xD2*xD2+yD2*yD2);

        // calculate angle between the two lines.
        dot=(xD1*xD2+yD1*yD2); // dot product
        deg=dot/(len1*len2);

        // if abs(angle)==1 then the lines are parallell,
        // so no intersection is possible
        if(Math.abs(deg)==1) return null;

        div=yD2*xD1-xD2*yD1;
        ua=(xD2*yD3-yD2*xD3)/div;
        ub=(xD1*yD3-yD1*xD3)/div;

        xi = (p1.getX()+ua*xD1);
        yi = (p1.getY()+ua*yD1);
        Point2D pt = new Point2D.Double(xi,yi);

        // calculate the combined length of the two segments
        // between Pt-p1 and Pt-p2
        xD1=pt.getX()-p1.getX();
        xD2=pt.getX()-p2.getX();
        yD1=pt.getY()-p1.getY();
        yD2=pt.getY()-p2.getY();
        segmentLen1=Math.sqrt(xD1*xD1+yD1*yD1)+Math.sqrt(xD2*xD2+yD2*yD2);

        // calculate the combined length of the two segments
        // between Pt-p3 and Pt-p4
        xD1=pt.getX()-p3.getX();
        xD2=pt.getX()-p4.getX();
        yD1=pt.getY()-p3.getY();
        yD2=pt.getY()-p4.getY();
        segmentLen2=Math.sqrt(xD1*xD1+yD1*yD1)+Math.sqrt(xD2*xD2+yD2*yD2);


        // if the lengths of both sets of segments are the same as
        // the lenghts of the two lines the point is actually
        // on the line segment.
        // if the point isn't on the line, return null
        if(Math.abs(len1-segmentLen1)>0.01 || Math.abs(len2-segmentLen2)>0.01) return null;
        return pt;
    }

    private void drawLine(Graphics g, Point a, Point b)
    {
        g.drawLine(a.x, a.y, b.x, b.y);
    }

    private Point getCons(MyClass thisClass, MyClass otherClass, Hashtable<MyClass,Vector<MyClass>> classUsings)
    {
        int otherDIR, thisDIR, otherCON, thisCON;

        if(otherClass.getPosition().y+otherClass.getHeight()/2 < thisClass.getPosition().y+thisClass.getHeight()/2)
        { // top
            thisDIR = 1;
        }
        else
        { // bottom
            thisDIR = -1;
        }

        if(otherClass.getPosition().x+otherClass.getWidth()/2 < thisClass.getPosition().x+thisClass.getWidth()/2)
        { // left
            otherDIR = 1;
        }
        else
        { // right
            otherDIR = -1;
        }

        // create an empty list
        Vector<MyClass> otherUsers = new Vector<MyClass>();

/*
        // iterate through all usages
        Set<MyClass> set = classUsings.keySet();
        Iterator<MyClass> itr = set.iterator();
        while (itr.hasNext())
        {
            // get the actual class ...
            MyClass actual = itr.next();
            // ... and the list of classes it uses
            Vector<MyClass> actualUses = classUsings.get(actual);
            // iterate through that list
            for(MyClass used : actualUses)
            {
                // add the actual class if
                // - it usesd the "otherClass"
                // - and the actual class has not yet been captured
                if(used==otherClass && !otherUsers.contains(actual)) otherUsers.add(actual);
            }
        }
*/
        
        /* let's try this one ... */
        for(Entry<MyClass,Vector<MyClass>> entry : classUsings.entrySet()) 
        {
            // get the actual class ...
            MyClass actual = entry.getKey();
            // ... and the list of classes it uses
            Vector<MyClass> actualUses = classUsings.get(actual);
            // iterate through that list
            for(MyClass used : actualUses)
            {
                // add the actual class if
                // - it usesd the "otherClass"
                // - and the actual class has not yet been captured
                if(used==otherClass && !otherUsers.contains(actual)) otherUsers.add(actual);
            }
        }
        

        if (otherDIR==-1 && thisDIR==1) // Q1 (top-right)
        {
            Vector<MyClass> others = classUsings.get(thisClass);
            thisCON = 1;
            for(MyClass other : others)
            {
                if (
                    (other.getPosition().y+other.getHeight()/2 < thisClass.getPosition().y+thisClass.getHeight()/2)
                    &&
                    (other.getPosition().x+other.getWidth()/2 >= thisClass.getPosition().x+thisClass.getWidth()/2)
                    &&
                    (other.getPosition().y+other.getHeight()/2 < otherClass.getPosition().y+otherClass.getHeight()/2)
                   ) thisCON++;
            }
            thisCON*=12;

            // other Q3
            otherCON = 1;
            for(MyClass other : otherUsers)
            {
                if (
                    (other.getPosition().y+other.getHeight()/2 >= otherClass.getPosition().y+otherClass.getHeight()/2)
                    &&
                    (other.getPosition().x+other.getWidth()/2 < otherClass.getPosition().x+otherClass.getWidth()/2)
                    &&
                    (other.getPosition().x+other.getWidth()/2 < thisClass.getPosition().x+thisClass.getWidth()/2)
                   ) otherCON++;
            }
            otherCON*=12;
        }
        else if(otherDIR==1 && thisDIR==1) // Q2 (top-left)
        {
            Vector<MyClass> others = classUsings.get(thisClass);
            thisCON = 1;
            for(MyClass other : others)
            {
                if (
                    (other.getPosition().y+other.getHeight()/2 < thisClass.getPosition().y+thisClass.getHeight()/2)
                    &&
                    (other.getPosition().x+other.getWidth()/2 < thisClass.getPosition().x+thisClass.getWidth()/2)
                    &&
                    (other.getPosition().y+other.getHeight()/2 < otherClass.getPosition().y+otherClass.getHeight()/2)
                   ) thisCON++;
            }
            thisCON*=-12;

            // other Q4
            otherCON = 1;
            for(MyClass other : otherUsers)
            {
                if (
                    (other.getPosition().y+other.getHeight()/2 >= otherClass.getPosition().y+otherClass.getHeight()/2)
                    &&
                    (other.getPosition().x+other.getWidth()/2 >= otherClass.getPosition().x+otherClass.getWidth()/2)
                    &&
                    (other.getPosition().x+other.getWidth()/2 > thisClass.getPosition().x+thisClass.getWidth()/2)
                   ) otherCON++;
            }
            otherCON*=12;
        }
        else if(otherDIR==1 && thisDIR==-1) // Q3 (bottom-left)
        {
            Vector<MyClass> others = classUsings.get(thisClass);
            thisCON = 1;
            for(MyClass other : others)
            {
                if (
                    (other.getPosition().y+other.getHeight()/2 >= thisClass.getPosition().y+thisClass.getHeight()/2)
                    &&
                    (other.getPosition().x+other.getWidth()/2 < thisClass.getPosition().x+thisClass.getWidth()/2)
                    &&
                    (other.getPosition().y+other.getHeight()/2 > otherClass.getPosition().y+otherClass.getHeight()/2)
                   ) thisCON++;
            }
            thisCON*=-12;

            // other Q1
            otherCON = 1;
            for(MyClass other : otherUsers)
            {
                if (
                    (other.getPosition().y+other.getHeight()/2 < otherClass.getPosition().y+otherClass.getHeight()/2)
                    &&
                    (other.getPosition().x+other.getWidth()/2 >= otherClass.getPosition().x+otherClass.getWidth()/2)
                    &&
                    (other.getPosition().x+other.getWidth()/2 > thisClass.getPosition().x+thisClass.getWidth()/2)
                   ) otherCON++;
            }
            otherCON*=-12;
        }
        else // Q4 (bottom-right)
        {
            Vector<MyClass> others = classUsings.get(thisClass);
            thisCON = 1;
            for(MyClass other : others)
            {
                if (
                    (other.getPosition().y+other.getHeight()/2 >= thisClass.getPosition().y+thisClass.getHeight()/2)
                    &&
                    (other.getPosition().x+other.getWidth()/2 >= thisClass.getPosition().x+thisClass.getWidth()/2)
                    &&
                    (other.getPosition().y+other.getHeight()/2 > otherClass.getPosition().y+otherClass.getHeight()/2)
                   ) thisCON++;
            }
            thisCON*=12;

            // other Q2
            otherCON = 1;
            for(MyClass other : otherUsers)
            {
                if (
                    (other.getPosition().y+other.getHeight()/2 < otherClass.getPosition().y+otherClass.getHeight()/2)
                    &&
                    (other.getPosition().x+other.getWidth()/2 < otherClass.getPosition().x+otherClass.getWidth()/2)
                    &&
                    (other.getPosition().x+other.getWidth()/2 < thisClass.getPosition().x+thisClass.getWidth()/2)
                   ) otherCON++;
            }
            otherCON*=-12;
        }

        /*
        int topRight  = 0;
        int topLeft = 0;
        int bottomLeft = 0;
        int bottomRight = 0;
        Vector<MyClass> others = classUsings.get(thisClass);
        for(MyClass other : otherUsers)
        {
                if (
                    (other.getPosition().y+other.getHeight()/2 >= otherClass.getPosition().y+otherClass.getHeight()/2)
                    &&
                    (other.getPosition().x+other.getWidth()/2 < otherClass.getPosition().x+otherClass.getWidth()/2)
                    &&
                    (other.getPosition().x+other.getWidth()/2 < thisClass.getPosition().x+thisClass.getWidth()/2)
                   ) topRight++;
                else if (
                    (other.getPosition().y+other.getHeight()/2 >= otherClass.getPosition().y+otherClass.getHeight()/2)
                    &&
                    (other.getPosition().x+other.getWidth()/2 >= otherClass.getPosition().x+otherClass.getWidth()/2)
                    &&
                    (other.getPosition().x+other.getWidth()/2 > thisClass.getPosition().x+thisClass.getWidth()/2)
                   ) topLeft++;
                else if (
                    (other.getPosition().y+other.getHeight()/2 < otherClass.getPosition().y+otherClass.getHeight()/2)
                    &&
                    (other.getPosition().x+other.getWidth()/2 >= otherClass.getPosition().x+otherClass.getWidth()/2)
                    &&
                    (other.getPosition().x+other.getWidth()/2 > thisClass.getPosition().x+thisClass.getWidth()/2)
                   ) bottomLeft++;
                else bottomRight++;
        }

        int DIST = 12;
        if (otherDIR==-1 && thisDIR==1) // Q1 (top-right)
        {
            //if(bottomRight==0 && topRight!=0) topRight--;
            otherCON=topRight*DIST;
        }
        else if(otherDIR==1 && thisDIR==1) // Q2 (top-left)
        {
            //if(bottomLeft==0 && topLeft!=0) topLeft--;
            otherCON=topLeft*DIST;
        }
        else if(otherDIR==1 && thisDIR==-1) // Q3 (bottom-left)
        {
            //if(topLeft==0 && bottomLeft!=0) bottomLeft--;
            otherCON=-bottomLeft*DIST;
        }
        else // Q4 /Bottom-right)
        {
            //if(topRight==0 && bottomRight!=0) bottomRight--;
            otherCON=-bottomRight*DIST;
        }
        */

        // adjust into the middle
        thisCON-=(thisCON/Math.abs(thisCON))*6;
        otherCON-=(otherCON/Math.abs(otherCON))*6;
        
        return new Point(thisCON, otherCON);
    }

    // new code
    private void drawCompoAggregation2(Graphics2D g, MyClass thisClass, MyClass otherClass, Hashtable<MyClass,Vector<MyClass>> classUsings,boolean isComposition)
    {
        if(thisClass!=otherClass)
        {
            Point thisTop       = new Point(thisClass.getX()+thisClass.getWidth()/2,thisClass.getY());
            Point thisBottom    = new Point(thisClass.getX()+thisClass.getWidth()/2,thisClass.getY()+thisClass.getHeight());
            Point thisLeft      = new Point(thisClass.getX(),thisClass.getY()+thisClass.getHeight()/2);
            Point thisRight     = new Point(thisClass.getX()+thisClass.getWidth(),thisClass.getY()+thisClass.getHeight()/2);
            Point[] thisPoints = {thisTop,thisBottom,thisLeft,thisRight};
            
            Point otherTop      = new Point(otherClass.getX()+otherClass.getWidth()/2,otherClass.getY());
            Point otherBottom   = new Point(otherClass.getX()+otherClass.getWidth()/2,otherClass.getY()+otherClass.getHeight());
            Point otherLeft     = new Point(otherClass.getX(),otherClass.getY()+otherClass.getHeight()/2);
            Point otherRight    = new Point(otherClass.getX()+otherClass.getWidth(),otherClass.getY()+otherClass.getHeight()/2);
            Point[] otherPoints = {otherTop,otherBottom,otherLeft,otherRight};
            
            double min = Double.MAX_VALUE;
            Point thisPoint  = null;
            Point otherPoint = null;
            double thisMin;
            
            // determine closest middelst
            for(int i=0;i<thisPoints.length;i++)
                for(int j=0;j<otherPoints.length;j++)
                {
                    thisMin = thisPoints[i].distance(otherPoints[j]);
                    if(thisMin<min)
                    {
                        min=thisMin;
                        thisPoint  = thisPoints[i];
                        otherPoint = otherPoints[j];
                    }
                }
            
            //Vector<MyClass> others = classUsings.get(thisClass);
            Vector<MyClass> usingsThisClass = classUsings.get(thisClass);
            // iterate through all usages
            /*Set<MyClass> set = classUsings.keySet();
            Iterator<MyClass> itr = set.iterator();
            while (itr.hasNext())
            {
                // get the actual class ...
                MyClass actual = itr.next();
                // ... and the list of classes it uses
                Vector<MyClass> actualUses = classUsings.get(actual);
                // iterate through that list
                for(MyClass used : actualUses)
                {
                    // add the actual class if
                    // - it usesd the "otherClass"
                    // - and the actual class has not yet been captured
                    if(used==thisClass && !usingsThisClass.contains(actual)) usingsThisClass.add(actual);
                }
            }*/
            
            /* let's try this one ... */
            for(Entry<MyClass,Vector<MyClass>> entry : classUsings.entrySet()) 
            {
                // get the actual class ...
                MyClass actual = entry.getKey();
                // ... and the list of classes it uses
                Vector<MyClass> actualUses = classUsings.get(actual);
                // iterate through that list
                for(MyClass used : actualUses)
                {
                    // add the actual class if
                    // - it usesd the "otherClass"
                    // - and the actual class has not yet been captured
                    if(used==thisClass && !usingsThisClass.contains(actual)) usingsThisClass.add(actual);
                }
            }
            

            Stroke oldStroke = g.getStroke();

            if(thisPoint==thisTop)
            {
                // init number of connectionx
                int thisCon = 1;
                // init the direction into which to move
                int thisDir = 1;
                if(thisPoint.x>otherPoint.x) thisDir=-1;
                // loop through others to determine position
                for(MyClass other : usingsThisClass)
                {
                    // check goto right
                     if (
                         (other.getCenter().y < thisClass.getCenter().y)
                         &&
                         (other.getCenter().x >= thisClass.getCenter().x)
                         &&
                         (other.getCenter().y > otherClass.getCenter().y)
                         && (thisDir==1)) thisCon++;
                    // check goto left
                    if (
                         (other.getCenter().y < thisClass.getCenter().y)
                         &&
                         (other.getCenter().x < thisClass.getCenter().x)
                         &&
                         (other.getCenter().y > otherClass.getCenter().y)
                        && (thisDir==-1)) thisCon++;
                }
                int con = thisCon;
                thisCon=(int)((thisCon-0.5)*(12*thisDir));
                
                Polygon p = new Polygon();
                p.addPoint(thisCon+thisClass.getPosition().x+thisClass.getWidth()/2, thisClass.getPosition().y);
                p.addPoint(thisCon+thisClass.getPosition().x+thisClass.getWidth()/2-4, thisClass.getPosition().y-8);
                p.addPoint(thisCon+thisClass.getPosition().x+thisClass.getWidth()/2, thisClass.getPosition().y-16);
                p.addPoint(thisCon+thisClass.getPosition().x+thisClass.getWidth()/2+4, thisClass.getPosition().y-8);
                if(isComposition) g.fillPolygon(p);
                else g.drawPolygon(p);
                
                thisPoint.y-=15;
                thisPoint.x+=thisCon;
                
                Point movePoint = new Point(thisPoint);
                movePoint.y-=(usingsThisClass.size()-con)*8;
                g.setStroke(dashed);
                drawLine(g, thisPoint, movePoint);
                thisPoint=movePoint;
            }
            else if(thisPoint==thisBottom)
            {
               // init number of connectionx
                int thisCon = 1;
                // init the direction into which to move
                int thisDir = 1;
                if(thisPoint.x>otherPoint.x) thisDir=-1;
                // loop through others to determine position
                for(MyClass other : usingsThisClass)
                {
                    // check goto right
                     if (
                         (other.getCenter().y >= thisClass.getCenter().y)
                         &&
                         (other.getCenter().x >= thisClass.getCenter().x)
                         &&
                         (other.getCenter().y > otherClass.getCenter().y)
                         && (thisDir==1)) thisCon++;
                    // check goto left
                    if (
                         (other.getCenter().y >= thisClass.getCenter().y)
                         &&
                         (other.getCenter().x < thisClass.getCenter().x)
                         &&
                         (other.getCenter().y > otherClass.getCenter().y)
                         && (thisDir==-1)) thisCon++;
                }
                int con = thisCon;
                thisCon=(int)((thisCon-0.5)*(12*thisDir));

                // bottom
                Polygon p = new Polygon();
                p.addPoint(thisCon+thisClass.getPosition().x+thisClass.getWidth()/2, thisClass.getPosition().y+thisClass.getHeight());
                p.addPoint(thisCon+thisClass.getPosition().x+thisClass.getWidth()/2-4, thisClass.getPosition().y+thisClass.getHeight()+8);
                p.addPoint(thisCon+thisClass.getPosition().x+thisClass.getWidth()/2, thisClass.getPosition().y+thisClass.getHeight()+16);
                p.addPoint(thisCon+thisClass.getPosition().x+thisClass.getWidth()/2+4, thisClass.getPosition().y+thisClass.getHeight()+8);
                if(isComposition) g.fillPolygon(p);
                else g.drawPolygon(p);
                
                thisPoint.y+=15;
                thisPoint.x+=thisCon;
                
                Point movePoint = new Point(thisPoint);
                movePoint.y+=(usingsThisClass.size()-con)*8;
                g.setStroke(dashed);
                drawLine(g, thisPoint, movePoint);
                thisPoint=movePoint;
            }
            else if(thisPoint==thisRight)
            {
               // init number of connectionx
                int thisCon = 1;
                // init the direction into which to move
                int thisDir = 1;
                if(thisPoint.y>otherPoint.y) thisDir=-1;
                // loop through others to determine position
                for(MyClass other : usingsThisClass)
                {
                    // check goto up
                    if (
                         (other.getCenter().x >= thisClass.getCenter().x)
                         &&
                         (other.getCenter().y >= thisClass.getCenter().y)
                         &&
                         (other.getCenter().x > otherClass.getCenter().x)
                         && (thisDir==1)) thisCon++;
                    // check goto down
                    if (
                         (other.getCenter().x >= thisClass.getCenter().x)
                         &&
                         (other.getCenter().y < thisClass.getCenter().y)
                         &&
                         (other.getCenter().x > otherClass.getCenter().x)
                         && (thisDir==-1)) thisCon++;
                }
                int con = thisCon;
                thisCon=(int)((thisCon-0.5)*(12*thisDir));

                // right
                Polygon p = new Polygon();
                //thisCON = thisClass.getConnector().getNewBottom(otherDIR);
                p.addPoint(thisClass.getPosition().x+thisClass.getWidth(),   thisCon+thisClass.getPosition().y+thisClass.getHeight()/2);
                p.addPoint(thisClass.getPosition().x+thisClass.getWidth()+8, thisCon+thisClass.getPosition().y+thisClass.getHeight()/2-4);
                p.addPoint(thisClass.getPosition().x+thisClass.getWidth()+16,thisCon+thisClass.getPosition().y+thisClass.getHeight()/2);
                p.addPoint(thisClass.getPosition().x+thisClass.getWidth()+8, thisCon+thisClass.getPosition().y+thisClass.getHeight()/2+4);
                if(isComposition) g.fillPolygon(p);
                else g.drawPolygon(p);
                
                thisPoint.x+=15;
                thisPoint.y+=thisCon;
                
                Point movePoint = new Point(thisPoint);
                movePoint.x+=(usingsThisClass.size()-con)*8;
                g.setStroke(dashed);
                drawLine(g, thisPoint, movePoint);
                thisPoint=movePoint;
            }
            else // left
            {
                // init number of connectionx
                int thisCon = 1;
                // init the direction into which to move
                int thisDir = 1;
                if(thisPoint.y>otherPoint.y) thisDir=-1;
                // loop through others to determine position
                for(MyClass other : usingsThisClass)
                {
                    // check goto up
                     if (
                         (other.getCenter().x < thisClass.getCenter().x)
                         &&
                         (other.getCenter().y >= thisClass.getCenter().y)
                         &&
                         (other.getCenter().x > otherClass.getCenter().x)
                         && (thisDir==1)) thisCon++;
                    // check goto down
                    if (
                         (other.getCenter().x < thisClass.getCenter().x)
                         &&
                         (other.getCenter().y < thisClass.getCenter().y)
                         &&
                         (other.getCenter().x > otherClass.getCenter().x)
                        && (thisDir==-1)) thisCon++;
                }
                int con = thisCon;
                thisCon=(int)((thisCon-0.5)*(12*thisDir));
                
                Polygon p = new Polygon();
                p.addPoint(thisClass.getPosition().x,   thisCon+thisClass.getPosition().y+thisClass.getHeight()/2);
                p.addPoint(thisClass.getPosition().x-8, thisCon+thisClass.getPosition().y+thisClass.getHeight()/2-4);
                p.addPoint(thisClass.getPosition().x-16,thisCon+thisClass.getPosition().y+thisClass.getHeight()/2);
                p.addPoint(thisClass.getPosition().x-8, thisCon+thisClass.getPosition().y+thisClass.getHeight()/2+4);
                if(isComposition) g.fillPolygon(p);
                else g.drawPolygon(p);
                
                thisPoint.y+=thisCon;
                thisPoint.x-=15;
                
                Point movePoint = new Point(thisPoint);
                movePoint.x-=(usingsThisClass.size()-con)*8;
                g.setStroke(dashed);
                drawLine(g, thisPoint, movePoint);
                thisPoint=movePoint;
            }

            //Vector<MyClass> others = classUsings.get(otherClass);
            Vector<MyClass> usingsOtherClass = classUsings.get(otherClass);
            /*
            // iterate through all usages
            set = classUsings.keySet();
            itr = set.iterator();
            while (itr.hasNext())
            {
                // get the actual class ...
                MyClass actual = itr.next();
                // ... and the list of classes it uses
                Vector<MyClass> actualUses = classUsings.get(actual);
                // iterate through that list
                for(MyClass used : actualUses)
                {
                    // add the actual class if
                    // - it usesd the "otherClass"
                    // - and the actual class has not yet been captured
                    if(used==otherClass && !usingsOtherClass.contains(actual)) usingsOtherClass.add(actual);
                }
            }
            */
            
            /* let's try this one ... */
            for(Entry<MyClass,Vector<MyClass>> entry : classUsings.entrySet()) 
            {
                // get the actual class ...
                MyClass actual = entry.getKey();
                // ... and the list of classes it uses
                Vector<MyClass> actualUses = classUsings.get(actual);
                // iterate through that list
                for(MyClass used : actualUses)
                {
                    // add the actual class if
                    // - it usesd the "otherClass"
                    // - and the actual class has not yet been captured
                    if(used==otherClass && !usingsOtherClass.contains(actual)) usingsOtherClass.add(actual);
                }
            }


            Point stopUp;
            Point stopDown;
            Point stopOut;
            Point step;
            Point start = thisPoint;
            Point stop;
            
            if(otherPoint==otherTop)
            {
                // init number of connectionx
                int otherCon = 1;
                // init the direction into which to move
                int otherDir = 1;
                if(otherPoint.x>thisPoint.x) otherDir=-1;
                // loop through others to determine position
                for(MyClass other : usingsOtherClass)
                {
                    // check goto right
                    if (
                         (other.getCenter().y < otherClass.getCenter().y)
                         &&
                         (other.getCenter().x >= otherClass.getCenter().x)
                         &&
                         (other.getCenter().y < thisClass.getCenter().y)
                         && (otherDir==1)) otherCon++;
                    // check goto left
                    if (
                         (other.getCenter().y < otherClass.getCenter().y)
                         &&
                         (other.getCenter().x < otherClass.getCenter().x)
                         &&
                         (other.getCenter().y < thisClass.getCenter().y)
                        && (otherDir==-1)) otherCon++;
                }
                otherCon=(int)((otherCon-0.5)*(12*otherDir));
                
                otherPoint.x+=otherCon;

                stopUp   = new Point(otherPoint.x-4,otherPoint.y-8);
                stopDown = new Point(otherPoint.x+4,otherPoint.y-8);
                stopOut  = new Point(otherPoint.x,otherPoint.y-8);
                stop = stopOut;
                step = new Point(stop.x,start.y);
            }
            else if(otherPoint==otherBottom)
            {
               // init number of connectionx
                int otherCon = 1;
                // init the direction into which to move
                int otherDir = 1;
                if(otherPoint.x>thisPoint.x) otherDir=-1;
                // loop through others to determine position
                for(MyClass other : usingsOtherClass)
                {
                    // check goto right
                     if (
                         (other.getCenter().y >= otherClass.getCenter().y)
                         &&
                         (other.getCenter().x >= otherClass.getCenter().x)
                         &&
                         (other.getCenter().y > thisClass.getCenter().y)
                         && (otherDir==1)) otherCon++;
                    // check goto left
                    if (
                         (other.getCenter().y >= otherClass.getCenter().y)
                         &&
                         (other.getCenter().x < otherClass.getCenter().x)
                         &&
                         (other.getCenter().y > thisClass.getCenter().y)
                         && (otherDir==-1)) otherCon++;
                }
                otherCon=(int)((otherCon-0.5)*(12*otherDir));

                otherPoint.x+=otherCon;
                
                stopUp   = new Point(otherPoint.x-4,otherPoint.y+8);
                stopDown = new Point(otherPoint.x+4,otherPoint.y+8);
                stopOut  = new Point(otherPoint.x,otherPoint.y+8);
                stop = stopOut;
                step = new Point(stop.x,start.y);
            }
            else if(otherPoint==otherRight)
            {
               // init number of connectionx
                int otherCon = 1;
                // init the direction into which to move
                int otherDir = 1;
                if(otherPoint.y>thisPoint.y) otherDir=-1;
                // loop through others to determine position
                for(MyClass other : usingsOtherClass)
                {
                    // check goto up
                    if (
                         (other.getCenter().x >= otherClass.getCenter().x)
                         &&
                         (other.getCenter().y >= otherClass.getCenter().y)
                         &&
                         (other.getCenter().x > thisClass.getCenter().x)
                         && (otherDir==1)) otherCon++;
                    // check goto down
                    if (
                         (other.getCenter().x >= otherClass.getCenter().x)
                         &&
                         (other.getCenter().y < otherClass.getCenter().y)
                         &&
                         (other.getCenter().x > thisClass.getCenter().x)
                         && (otherDir==-1)) otherCon++;
                }
                otherCon=(int)((otherCon-0.5)*(12*otherDir));

                otherPoint.y+=otherCon;
                
                stopUp   = new Point(otherPoint.x+8,otherPoint.y-4);
                stopDown = new Point(otherPoint.x+8,otherPoint.y+4);
                stopOut  = new Point(otherPoint.x+8,otherPoint.y);
                stop = stopOut;
                step = new Point(start.x,stop.y);
            }
            else // left
            {
                // init number of connectionx
                int otherCon = 1;
                // init the direction into which to move
                int otherDir = 1;
                if(otherPoint.y>thisPoint.y) otherDir=-1;
                // loop through others to determine position
                for(MyClass other : usingsOtherClass)
                {
                    // check goto up
                     if (
                         (other.getCenter().x < otherClass.getCenter().x)
                         &&
                         (other.getCenter().y >= otherClass.getCenter().y)
                         &&
                         (other.getCenter().x < thisClass.getCenter().x)
                         && (otherDir==1)) otherCon++;
                    // check goto down
                    if (
                         (other.getCenter().x < otherClass.getCenter().x)
                         &&
                         (other.getCenter().y < otherClass.getCenter().y)
                         &&
                         (other.getCenter().x < thisClass.getCenter().x)
                        && (otherDir==-1)) otherCon++;
                }
                otherCon=(int)((otherCon-0.5)*(12*otherDir));
                
                otherPoint.y+=otherCon;
                
                stopUp   = new Point(otherPoint.x-8,otherPoint.y-4);
                stopDown = new Point(otherPoint.x-8,otherPoint.y+4);
                stopOut  = new Point(otherPoint.x+8,otherPoint.y);
                stop = stopOut;
                step = new Point(start.x,stop.y);
            }            
            
//            drawLine(g,thisPoint,otherPoint);

            boolean inter =false;

            /*

            if(otherClass.getPosition().y+otherClass.getHeight()/2 < thisClass.getPosition().y+thisClass.getHeight()/2)
            { // top
                if(stop.y>start.y)
                {
                    step = new Point(start.x,thisClass.getPosition().y);
                    inter = true;
                }
                else
                {
                    step = new Point(start.x,stop.y);
                }
            }
            else
            { // bottom
                if(stop.y<thisClass.getPosition().y+thisClass.getHeight() || thisClass==otherClass)
                {
                    step = new Point(start.x,
                                     thisClass.getPosition().y+thisClass.getHeight());
                    inter = true;
                }
                else
                {
                    step = new Point(start.x,stop.y);
                }

            }


            drawLine(g,start,step);

            if(inter==true)
            {
                int middle;
                if(thisClass==otherClass)
                {
                    middle = otherClass.getPosition().x+otherClass.getWidth()+16;//-otherCON;
                }
                else if(otherClass.getPosition().x+otherClass.getWidth()/2 > thisClass.getPosition().x+thisClass.getWidth()/2)
                { // left
                    middle = (-(thisClass.getPosition().x+thisClass.getWidth())+(otherClass.getPosition().x))/2+thisClass.getPosition().x+thisClass.getWidth();
                }
                else
                { // right
                    middle = (-(otherClass.getPosition().x+otherClass.getWidth())+(thisClass.getPosition().x))/2+otherClass.getPosition().x+otherClass.getWidth();
                }
                Point next = new Point(middle,step.y);
                drawLine(g,step,next);
                step = new Point(middle,stop.y);
                drawLine(g,step,next);
            }
            */
            /*
            g.setColor(Color.red);
            drawLine(g,start,step);
            drawLine(g,step,stop);
            
            g.setColor(Color.blue);
            step = new Point(stop.x,start.y);
            drawLine(g,start,step);
            drawLine(g,step,stop);
            
            g.setColor(Color.orange);
            drawLine(g,start,
                       new Point(start.x,(start.y+stop.y)/2));
            drawLine(g,new Point(start.x,(start.y+stop.y)/2),
                       new Point((start.x+stop.y)/2,(start.y+stop.y)/2));
            drawLine(g,new Point((start.x+stop.y)/2,(start.y+stop.y)/2),
                       new Point((start.x+stop.y)/2,stop.y));
            drawLine(g,new Point((start.x+stop.y)/2,stop.y),
                       stop);

            g.setColor(Color.black);/**/
            
            drawLine(g,start,step);
            drawLine(g,step,stop);
            
            drawLine(g,otherPoint,stop);
            g.setStroke(oldStroke);
            drawLine(g,stopUp,otherPoint);
            drawLine(g,stopDown,otherPoint);            
        }
    }
            
    private void drawCompoAggregation(Graphics2D g, MyClass thisClass, MyClass otherClass, Hashtable<MyClass,Vector<MyClass>> classUsings,boolean isComposition)
    {
        //g.setColor(Color.BLUE);
        drawCompoAggregation2(g, thisClass, otherClass, classUsings, isComposition);
        /*g.setColor(Color.RED);
        if(thisClass!=otherClass)
        {
            Point start;
            Point stop;
            Point stopUp;
            Point stopDown;
            boolean inter =false;

            int destY;
            Point step;

            Point cons = getCons(thisClass, otherClass, classUsings);
            int thisCON = cons.x;
            int otherCON = cons.y;

            if(otherClass.getPosition().y+otherClass.getHeight()/2 < thisClass.getPosition().y+thisClass.getHeight()/2)
            {   
                // top
                Polygon p = new Polygon();
                //thisCON = thisClass.getConnector().getNewTop(otherDIR);
                p.addPoint(thisCON+thisClass.getPosition().x+thisClass.getWidth()/2, thisClass.getPosition().y);
                p.addPoint(thisCON+thisClass.getPosition().x+thisClass.getWidth()/2-4, thisClass.getPosition().y-8);
                p.addPoint(thisCON+thisClass.getPosition().x+thisClass.getWidth()/2, thisClass.getPosition().y-16);
                p.addPoint(thisCON+thisClass.getPosition().x+thisClass.getWidth()/2+4, thisClass.getPosition().y-8);
                if(isComposition) g.fillPolygon(p);
                else g.drawPolygon(p);

                start = new Point(thisCON+thisClass.getPosition().x+thisClass.getWidth()/2,
                                  thisClass.getPosition().y-16);
            }
            else
            { 
                // bottom
                Polygon p = new Polygon();
                //thisCON = thisClass.getConnector().getNewBottom(otherDIR);
                p.addPoint(thisCON+thisClass.getPosition().x+thisClass.getWidth()/2, thisClass.getPosition().y+thisClass.getHeight());
                p.addPoint(thisCON+thisClass.getPosition().x+thisClass.getWidth()/2-4, thisClass.getPosition().y+thisClass.getHeight()+8);
                p.addPoint(thisCON+thisClass.getPosition().x+thisClass.getWidth()/2, thisClass.getPosition().y+thisClass.getHeight()+16);
                p.addPoint(thisCON+thisClass.getPosition().x+thisClass.getWidth()/2+4, thisClass.getPosition().y+thisClass.getHeight()+8);
                if(isComposition) g.fillPolygon(p);
                else g.drawPolygon(p);

                start = new Point(thisCON+thisClass.getPosition().x+thisClass.getWidth()/2,
                                  thisClass.getPosition().y+thisClass.getHeight()+16);
            }

            Stroke oldStroke = g.getStroke();
            g.setStroke(dashed);

            if(otherClass.getPosition().x+otherClass.getWidth()/2 > thisClass.getPosition().x+thisClass.getWidth()/2)
            { // left
                //otherCON = otherClass.getConnector().getNewLeft(thisDIR);
                stop = new Point(otherClass.getPosition().x,
                                  otherCON+otherClass.getPosition().y+otherClass.getHeight()/2);
                stopUp = new Point(otherClass.getPosition().x-8,
                                  otherCON+otherClass.getPosition().y+otherClass.getHeight()/2-4);
                stopDown = new Point(otherClass.getPosition().x-8,
                                  otherCON+otherClass.getPosition().y+otherClass.getHeight()/2+4);
                destY = otherCON+otherClass.getPosition().y+otherClass.getHeight()/2;
            }
            else
            { // right
                //otherCON = otherClass.getConnector().getNewRight(thisDIR);
                stop = new Point(otherClass.getPosition().x+otherClass.getWidth(),
                                  otherCON+otherClass.getPosition().y+otherClass.getHeight()/2);
                stopUp = new Point(otherClass.getPosition().x+otherClass.getWidth()+8,
                                  otherCON+otherClass.getPosition().y+otherClass.getHeight()/2-4);
                stopDown = new Point(otherClass.getPosition().x+otherClass.getWidth()+8,
                                  otherCON+otherClass.getPosition().y+otherClass.getHeight()/2+4);
                destY = otherCON+otherClass.getPosition().y+otherClass.getHeight()/2;
            }

            if(otherClass.getPosition().y+otherClass.getHeight()/2 < thisClass.getPosition().y+thisClass.getHeight()/2)
            { // top
                if(destY>thisClass.getPosition().y-24)
                {
                    step = new Point(thisCON+thisClass.getPosition().x+thisClass.getWidth()/2,
                                     thisClass.getPosition().y-24);
                    inter = true;
                }
                else
                {
                    step = new Point(thisCON+thisClass.getPosition().x+thisClass.getWidth()/2,destY);
                }
            }
            else
            { // bottom
                if(destY<thisClass.getPosition().y+thisClass.getHeight()+24 || thisClass==otherClass)
                {
                    step = new Point(thisCON+thisClass.getPosition().x+thisClass.getWidth()/2,
                                     thisClass.getPosition().y+thisClass.getHeight()+24);
                    inter = true;
                }
                else
                {
                    step = new Point(thisCON+thisClass.getPosition().x+thisClass.getWidth()/2,destY);
                }

            }


            drawLine(g,start,step);

            if(inter==true)
            {
                int middle;
                if(thisClass==otherClass)
                {
                    middle = otherClass.getPosition().x+otherClass.getWidth()+16;//-otherCON;
                }
                else if(otherClass.getPosition().x+otherClass.getWidth()/2 > thisClass.getPosition().x+thisClass.getWidth()/2)
                { // left
                    middle = (-(thisClass.getPosition().x+thisClass.getWidth())+(otherClass.getPosition().x))/2+thisClass.getPosition().x+thisClass.getWidth();
                }
                else
                { // right
                    middle = (-(otherClass.getPosition().x+otherClass.getWidth())+(thisClass.getPosition().x))/2+otherClass.getPosition().x+otherClass.getWidth();
                }
                Point next = new Point(middle,step.y);
                drawLine(g,step,next);
                step = new Point(middle,stop.y);
                drawLine(g,step,next);
            }

            drawLine(g,step,stop);

            g.setStroke(oldStroke);
            drawLine(g,stopUp,stop);
            drawLine(g,stopDown,stop);
        }
        /**/
    }

    private void drawComposition(Graphics2D g, MyClass thisClass, MyClass otherClass, Hashtable<MyClass,Vector<MyClass>> classUsings)
    {
        drawCompoAggregation(g,thisClass,otherClass,classUsings,true);
    }

    private void drawAggregation(Graphics2D g, MyClass thisClass, MyClass otherClass, Hashtable<MyClass,Vector<MyClass>> classUsings)
    {
        drawCompoAggregation(g,thisClass,otherClass,classUsings,false);
    }

    public MyClass findByShortName(String name)
    {
        //System.out.println("Searching: "+name);
        for(MyClass myClass : classes.values())
        {
            //System.out.println("Having: "+myClass.getShortName());
            if(myClass.getShortName().equals(name)) return myClass;
        }
        return null;
    }

    @Override
    public void paint(Graphics graphics)
    {
            super.paint(graphics);
            Graphics2D g = (Graphics2D) graphics;
            // set anti-aliasing rendering
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);

            g.setFont(new Font(g.getFont().getFontName(),Font.PLAIN,Unimozer.DRAW_FONT_SIZE));

            // clear background
            g.setColor(Color.WHITE);
            g.fillRect(0,0,getWidth()+1,getHeight()+1);
            g.setColor(Color.BLACK);

            /*Set<String> set;
            Iterator<String> itr;
            // draw classes a first time
            for(MyClass clas : classes.values())
            {
              clas.setEnabled(this.isEnabled());
              clas.draw(graphics,showFields,showMethods);
            }*/

            /* let's try this one ... */
            for(Entry<String,MyClass> entry : classes.entrySet()) 
            {
                // get the actual class ...
                MyClass clas = entry.getValue();
                clas.setEnabled(this.isEnabled());
                clas.draw(graphics,showFields,showMethods);
            }            

            // draw packages
            packages.clear();
            for(MyClass myClass : classes.values())
            {
                if(myClass.isDisplayUML())
                {
                    Package myPackage = null;
                    if(!packages.containsKey(myClass.getPackagename()))
                    {
                        myPackage = new Package(myClass.getPackagename(),
                                myClass.getPosition().y,
                                myClass.getPosition().x,
                                myClass.getWidth(),
                                myClass.getHeight());
                        packages.put(myPackage.getName(),myPackage);
                    }
                    else myPackage=packages.get(myClass.getPackagename());

                    if(myClass.getPosition().x+myClass.getWidth() >myPackage.getRight())   myPackage.setRight(myClass.getPosition().x+myClass.getWidth());
                    if(myClass.getPosition().y+myClass.getHeight()>myPackage.getBottom())  myPackage.setBottom(myClass.getPosition().y+myClass.getHeight());

                    if(myClass.getPosition().x<myPackage.getLeft())   myPackage.setLeft(myClass.getPosition().x);
                    if(myClass.getPosition().y<myPackage.getTop())    myPackage.setTop(myClass.getPosition().y);
                }
            }

            // draw classes
            /*
            set = classes.keySet();
            itr = set.iterator();
            while (itr.hasNext())
            {
              String str = itr.next();
              classes.get(str).draw(graphics);
            }/**/

            mostRight = 0;
            mostBottom = 0;

            // ??
            /*
            set = classes.keySet();
            itr = set.iterator();
            while (itr.hasNext())
            {
              String str = itr.next();
              MyClass thisClass = classes.get(str);
            }
            */

            // init topLeft & bottomRight
            topLeft = new Point(this.getWidth(),this.getHeight());
            bottomRight = new Point(0,0);
            
            // draw packages
            if(packages.size()>0)
                if((packages.size()==1 && packages.get(Package.DEFAULT)==null) || packages.size()>1)
                    for(Package pack : packages.values())
                    {
                        pack.draw(graphics);
                        // push outer box
                        if(pack.getTopAbs()<topLeft.y) topLeft.y=pack.getTopAbs();
                        if(pack.getLeftAbs()<topLeft.x) topLeft.x=pack.getLeftAbs();
                        if(pack.getBottomAbs()>bottomRight.y) bottomRight.y=pack.getBottomAbs();
                        if(pack.getRightAbs()>bottomRight.x) bottomRight.x=pack.getRightAbs();
                    }

            // draw implmementations
            if(isShowHeritage())
            {
                Stroke oldStroke = g.getStroke();
                g.setStroke(dashed);

                /*itr = set.iterator();
                while (itr.hasNext())
                {
                  String str = itr.next();
                */
                
                /* let's try this one ... */
                for(Entry<String,MyClass> entry : classes.entrySet()) 
                {
                    // get the actual class ...
                    String str = entry.getKey();
                  
                  MyClass thisClass = classes.get(str);

                  if(thisClass.getPosition().x+thisClass.getWidth()>mostRight) mostRight=thisClass.getPosition().x+thisClass.getWidth();
                  if(thisClass.getPosition().y+thisClass.getHeight()>mostBottom) mostBottom=thisClass.getPosition().y+thisClass.getHeight();

                  if(thisClass.getImplements().size()>0)
                  for(String extendsClass : thisClass.getImplements())
                  {
                    MyClass otherClass = classes.get(extendsClass);
                    if(otherClass==null) otherClass=findByShortName(extendsClass);
                    //if(otherClass==null) System.err.println(extendsClass+" not found (1)");
                    //if (otherClass==null) otherClass=findByShortName(extendsClass);
                    //if(otherClass==null) System.err.println(extendsClass+" not found (2)");
                    if(otherClass!=null && thisClass.isDisplayUML() && otherClass.isDisplayUML())
                    {
                        thisClass.setExtendsMyClass(otherClass);
                        // draw arrow from thisClass to otherClass

                        // get the center point of each class
                        Point fromP = new Point(thisClass.getPosition().x+thisClass.getWidth()/2,
                                                thisClass.getPosition().y+thisClass.getHeight()/2);
                        Point toP = new Point(otherClass.getPosition().x+otherClass.getWidth()/2,
                                              otherClass.getPosition().y+otherClass.getHeight()/2);

                        // get the corner 4 points of the desstination class
                        // (outer margin = 4)
                        Point toP1 = new Point(otherClass.getPosition().x-4,
                                              otherClass.getPosition().y-4);
                        Point toP2 = new Point(otherClass.getPosition().x+otherClass.getWidth()+4,
                                              otherClass.getPosition().y-4);
                        Point toP3 = new Point(otherClass.getPosition().x+otherClass.getWidth()+4,
                                              otherClass.getPosition().y+otherClass.getHeight()+4);
                        Point toP4 = new Point(otherClass.getPosition().x-4,
                                              otherClass.getPosition().y+otherClass.getHeight()+4);

                        // get the intersection with the center line an one of the
                        // sedis of the destination class
                        Point2D toDraw = getIntersection(fromP, toP, toP1, toP2);
                        if(toDraw==null) toDraw = getIntersection(fromP, toP, toP2, toP3);
                        if(toDraw==null) toDraw = getIntersection(fromP, toP, toP3, toP4);
                        if(toDraw==null) toDraw = getIntersection(fromP, toP, toP4, toP1);

                        // draw the arrowed line
                        if(toDraw!=null)
                            drawExtends(g,fromP,new Point((int) toDraw.getX(),(int) toDraw.getY()));

                    }
                  }
                  
                }
                g.setStroke(oldStroke);
            }


            // draw inheritance
            if(isShowHeritage())
            {
                /*itr = set.iterator();
                while (itr.hasNext())
                {
                  String str = itr.next();
                */
                
                /* let's try this one ... */
                for(Entry<String,MyClass> entry : classes.entrySet()) 
                {
                    // get the actual class ...
                    String str = entry.getKey();
                  
                  MyClass thisClass = classes.get(str);

                  if(thisClass.getPosition().x+thisClass.getWidth()>mostRight) mostRight=thisClass.getPosition().x+thisClass.getWidth();
                  if(thisClass.getPosition().y+thisClass.getHeight()>mostBottom) mostBottom=thisClass.getPosition().y+thisClass.getHeight();

                  String extendsClass = thisClass.getExtendsClass();
                  //System.out.println(thisClass.getFullName()+" extends "+extendsClass);
                  if (!extendsClass.equals("") && thisClass.isDisplayUML())
                  {
                    MyClass otherClass = classes.get(extendsClass);
                    if(otherClass==null) otherClass=findByShortName(extendsClass);
                    //if(otherClass==null) System.err.println(extendsClass+" not found (1)");
                    //if (otherClass==null) otherClass=findByShortName(extendsClass);
                    //if(otherClass==null) System.err.println(extendsClass+" not found (2)");
                    if(otherClass!=null)
                    {
                        if (otherClass!=thisClass)
                        {
                            thisClass.setExtendsMyClass(otherClass);
                            // draw arrow from thisClass to otherClass

                            // get the center point of each class
                            Point fromP = new Point(thisClass.getPosition().x+thisClass.getWidth()/2,
                                                    thisClass.getPosition().y+thisClass.getHeight()/2);
                            Point toP = new Point(otherClass.getPosition().x+otherClass.getWidth()/2,
                                                  otherClass.getPosition().y+otherClass.getHeight()/2);

                            // get the corner 4 points of the desstination class
                            // (outer margin = 4)
                            Point toP1 = new Point(otherClass.getPosition().x-4,
                                                  otherClass.getPosition().y-4);
                            Point toP2 = new Point(otherClass.getPosition().x+otherClass.getWidth()+4,
                                                  otherClass.getPosition().y-4);
                            Point toP3 = new Point(otherClass.getPosition().x+otherClass.getWidth()+4,
                                                  otherClass.getPosition().y+otherClass.getHeight()+4);
                            Point toP4 = new Point(otherClass.getPosition().x-4,
                                                  otherClass.getPosition().y+otherClass.getHeight()+4);

                            // get the intersection with the center line an one of the
                            // sedis of the destination class
                            Point2D toDraw = getIntersection(fromP, toP, toP1, toP2);
                            if(toDraw==null) toDraw = getIntersection(fromP, toP, toP2, toP3);
                            if(toDraw==null) toDraw = getIntersection(fromP, toP, toP3, toP4);
                            if(toDraw==null) toDraw = getIntersection(fromP, toP, toP4, toP1);

                            // draw in red if there is a cclic inheritance problem
                            if (thisClass.hasCyclicInheritance())
                            {
                                ((Graphics2D) graphics).setStroke(new BasicStroke(2));
                                graphics.setColor(Color.RED);
                            }

                            // draw the arrowed line
                            if(toDraw!=null)
                                drawExtends((Graphics2D) graphics,fromP,new Point((int) toDraw.getX(),(int) toDraw.getY()));

                        }
                        else
                        {
                            ((Graphics2D) graphics).setStroke(new BasicStroke(2));
                            graphics.setColor(Color.RED);

                            // line
                            graphics.drawLine(thisClass.getPosition().x+thisClass.getWidth()/2,thisClass.getPosition().y,
                                              thisClass.getPosition().x+thisClass.getWidth()/2,thisClass.getPosition().y-32);
                            graphics.drawLine(thisClass.getPosition().x+thisClass.getWidth()/2,thisClass.getPosition().y-32,
                                              thisClass.getPosition().x+thisClass.getWidth()+32,thisClass.getPosition().y-32);
                            graphics.drawLine(thisClass.getPosition().x+thisClass.getWidth()+32,thisClass.getPosition().y-32,
                                              thisClass.getPosition().x+thisClass.getWidth()+32,thisClass.getPosition().y+thisClass.getHeight()+32);
                            graphics.drawLine(thisClass.getPosition().x+thisClass.getWidth()+32,thisClass.getPosition().y+thisClass.getHeight()+32,
                                              thisClass.getPosition().x+thisClass.getWidth()/2,thisClass.getPosition().y+thisClass.getHeight()+32);
                            drawExtends((Graphics2D) graphics,new Point(thisClass.getPosition().x+thisClass.getWidth()/2,thisClass.getPosition().y+thisClass.getHeight()+32),
                                        new Point(thisClass.getPosition().x+thisClass.getWidth()/2,thisClass.getPosition().y+thisClass.getHeight()));
                        }

                        // reset the stroke and the color
                        ((Graphics2D) graphics).setStroke(new BasicStroke(1));
                        graphics.setColor(Color.BLACK);
                    }
                  }
                }
            }

            // setup a hastable to store the relations
            //Hashtable<String,StringList> classUsage = new Hashtable<String,StringList>();

            // store compositions
            Hashtable<MyClass,Vector<MyClass>> classCompositions = new Hashtable<MyClass,Vector<MyClass>>();
            // store aggregations
            Hashtable<MyClass,Vector<MyClass>> classAggregations= new Hashtable<MyClass,Vector<MyClass>>();
            // store all relations
            Hashtable<MyClass,Vector<MyClass>> classUsings = new Hashtable<MyClass,Vector<MyClass>>();

            /*
            // iterate through all classes to find compositions
            itr = set.iterator();
            while (itr.hasNext())
            {
              // get the actual classname
              String str = itr.next();
            */
            
            /* let's try this one ... */
            for(Entry<String,MyClass> entry : classes.entrySet()) 
            {
                // get the actual class ...
                String str = entry.getKey();
              
              // get the corresponding "MyClass" object
              MyClass thisClass = classes.get(str);
              // setup a list to store the relations with this class
              Vector<MyClass> theseCompositions = new Vector<MyClass>();

              // get all fields of this class
              StringList uses = thisClass.getFieldTypes();
              for(int u = 0; u<uses.count(); u++)
              {
                // try to find the other (used) class
                MyClass otherClass = classes.get(uses.get(u));
                if(otherClass==null) otherClass=findByShortName(uses.get(u));
                if(otherClass!=null) // means this class uses the other ones
                {
                    // add the other class to the list
                    theseCompositions.add(otherClass);
                }
              }

              // add the list of used classes to the MyClass object
              thisClass.setUsesMyClass(theseCompositions);
              // store the composition in the general list
              classCompositions.put(thisClass,theseCompositions);
              // store the compositions int eh global relation list
              classUsings.put(thisClass,new Vector<MyClass>(theseCompositions));
              //                        ^^^^^^^^^^^^^^^^^^^^
              //    important !! => create a new vector, otherwise the list
              //                    are the same ...
            }

/*
            // iterate through all classes to find aggregations
            itr = set.iterator();
            while (itr.hasNext())
            {
              // get the actual class
              String str = itr.next();
*/
            
            /* let's try this one ... */
            for(Entry<String,MyClass> entry : classes.entrySet()) 
            {
                // get the actual class ...
                String str = entry.getKey();

              // get the corresponding "MyClass" object
              MyClass thisClass = classes.get(str);
              // we need a list to store the aggragations with this class
              Vector<MyClass> theseAggregations = new Vector<MyClass>();
              // try to get the list of compositions for this class
              // init if not present
              Vector<MyClass> theseCompositions = classCompositions.get(thisClass);
              if (theseCompositions==null) theseCompositions=new Vector<MyClass>();
              // try to get the list of all relations for this class
              // init if not present
              Vector<MyClass> theseClasses = classUsings.get(thisClass);
              if (theseClasses==null) theseClasses=new Vector<MyClass>();
              
              // get the names of the classes that thisclass uses
              StringList foundUsage = thisClass.getUsesWho();
              // go through the list an check to find a corresponding MyClass
              for(int f=0;f<foundUsage.count();f++)
              {
                  // get the name of the used class
                  String usedClass = foundUsage.get(f);

                  MyClass otherClass = classes.get(usedClass);
                  if(otherClass==null) otherClass=findByShortName(usedClass);
                  if(otherClass!=null && thisClass!=otherClass)
                  // meanint "otherClass" is a class used by thisClass
                  {
                        if(!theseCompositions.contains(otherClass)) theseAggregations.add(otherClass);
                        if(!theseClasses.contains(otherClass)) theseClasses.add(otherClass);
                  }
              }

              // get all method types of this class
              StringList uses = thisClass.getMethodTypes();
              for(int u = 0; u<uses.count(); u++)
              {
                // try to find the other (used) class
                MyClass otherClass = classes.get(uses.get(u));
                if(otherClass==null) otherClass=findByShortName(uses.get(u));
                if(otherClass!=null) // means this class uses the other ones
                {
                    // add the other class to the list
                    theseAggregations.add(otherClass);
                }
              }

              
              // store the relations to the class
              thisClass.setUsesMyClass(theseClasses);
              // store the aggregation to the global list
              classAggregations.put(thisClass, theseAggregations);
              // store all relations to the global list
              classUsings.put(thisClass, theseClasses);
            }

            if (isShowComposition() )
            {
                /*Set<MyClass> set2 = classCompositions.keySet();
                Iterator<MyClass> itr2 = set2.iterator();
                while (itr2.hasNext())
                {
                  MyClass thisClass = itr2.next();
                */
                
                /* let's try this one ... */
                for(Entry<MyClass,Vector<MyClass>> entry : classCompositions.entrySet()) 
                {
                    // get the actual class ...
                    MyClass thisClass = entry.getKey();
                  if(thisClass.isDisplayUML()) { 
                    Vector<MyClass> otherClasses = classCompositions.get(thisClass);
                    for(MyClass otherClass : otherClasses) drawComposition(g, thisClass, otherClass, classUsings);
                  }
                }
            }

            if (isShowAggregation())
            {
                /*Set<MyClass> set2 = classAggregations.keySet();
                Iterator<MyClass> itr2 = set2.iterator();
                while (itr2.hasNext())
                {
                  MyClass thisClass = itr2.next();
                */  
                  
                /* let's try this one ... */
                for(Entry<MyClass,Vector<MyClass>> entry : classAggregations.entrySet()) 
                {
                    // get the actual class ...
                    MyClass thisClass = entry.getKey();
                  if(thisClass.isDisplayUML())  
                  {
                    Vector<MyClass> otherClasses = classAggregations.get(thisClass);
                    for(MyClass otherClass : otherClasses) drawAggregation(g, thisClass, otherClass, classUsings);
                  }
                }
            }
            
            // draw classes again to put them on top
            // of the arrows
            /*set = classes.keySet();
            itr = set.iterator();
            while (itr.hasNext())
            {
              String str = itr.next();
            */
            
            /* let's try this one ... */
            for(Entry<String,MyClass> entry : classes.entrySet()) 
            {
                // get the actual class ...
                String str = entry.getKey();
              
              classes.get(str).setEnabled(this.isEnabled());
              classes.get(str).draw(graphics,showFields,showMethods);

                // push outer box
                MyClass thisClass = classes.get(str);
                if(thisClass.getPosition().y<topLeft.y) topLeft.y=thisClass.getPosition().y;
                if(thisClass.getPosition().x<topLeft.x) topLeft.x=thisClass.getPosition().x;
                if(thisClass.getPosition().y+thisClass.getHeight()>bottomRight.y) bottomRight.y=thisClass.getPosition().y+thisClass.getHeight();
                if(thisClass.getPosition().x+thisClass.getWidth()>bottomRight.x) bottomRight.x=thisClass.getPosition().x+thisClass.getWidth();
            
            }

            // comments
            if(commentString!=null)
            {
                String fontName = g.getFont().getName();
                g.setFont(new Font("Courier",g.getFont().getStyle(),Unimozer.DRAW_FONT_SIZE));


                if(!commentString.trim().equals(""))
                {
                    String myCommentString = new String(commentString);
                    Point myCommentPoint = new Point(commentPoint);
                    //System.out.println(myCommentString);

                    // adjust comment
                    myCommentString=myCommentString.trim();
                    // adjust position
                    myCommentPoint.y=myCommentPoint.y+16;

                    // explode comment
                    StringList sl = StringList.explode(myCommentString,"\n");
                    // calculate totals
                    int totalHeight = 0;
                    int totalWidth = 0;
                    for(int i=0;i<sl.count();i++)
                    {
                        String line = sl.get(i).trim();
                        int h = (int) g.getFont().getStringBounds(line, g.getFontRenderContext()).getHeight();
                        int w = (int) g.getFont().getStringBounds(line, g.getFontRenderContext()).getWidth();
                        totalHeight+=h;
                        totalWidth=Math.max(totalWidth, w);
                    }


                    // get comment size
                    // draw background
                    g.setColor(new Color(255,255,128,255));
                    g.fillRoundRect(myCommentPoint.x, myCommentPoint.y,totalWidth+8,totalHeight+8,4,4);
                    // draw border
                    g.setColor(Color.BLACK);
                    g.drawRoundRect(myCommentPoint.x, myCommentPoint.y,totalWidth+8,totalHeight+8,4,4);

                    // draw text
                    totalHeight=0;
                    for(int i=0;i<sl.count();i++)
                    {
                        String line = sl.get(i).trim();
                        int h = (int) g.getFont().getStringBounds(myCommentString, g.getFontRenderContext()).getHeight();
                        g.drawString(line, myCommentPoint.x+4, myCommentPoint.y+h+2+totalHeight);
                        totalHeight+=h;
                    }

                }

                g.setFont(new Font(fontName,Font.PLAIN,Unimozer.DRAW_FONT_SIZE));

            }

            /*
            if(!isEnabled())
            {
                g.setColor(new Color(128,128,128,128));
                g.fillRect(0,0,getWidth(),getHeight());

            }
            */

            this.setPreferredSize(new Dimension(mostRight+32, mostBottom+32));
            // THE NEXT LINE MAKES ALL DIALOGUES DISAPEAR!!
            //this.setSize(mostRight+32, mostBottom+32);
            this.validate();
            ((JScrollPane)this.getParent().getParent()).revalidate();

            if(mode==MODE_EXTENDS && extendsFrom!=null && extendsDragPoint!=null)
            {
                graphics.setColor(Color.BLUE);
                ((Graphics2D) graphics).setStroke(new BasicStroke(2));
                drawExtends(g, new Point(extendsFrom.getPosition().x+extendsFrom.getWidth()/2,extendsFrom.getPosition().y+extendsFrom.getHeight()/2), extendsDragPoint);
                graphics.setColor(Color.BLACK);
                ((Graphics2D) graphics).setStroke(new BasicStroke(1));
            }
    }

    public void addClass()
    {
        ClassEditor ce = ClassEditor.showModal(this.frame, "Add a new class", true);
        Ini.set("javaDocCClass", Boolean.toString(ce.genDoc()));
        Ini.set("mainClass", Boolean.toString(ce.genMain()));
        if (ce.OK==true)
        {
            MyClass mc = new MyClass(ce);
            mc.setExtendsClass(ce.getExtends());
            mc.setExtendsMyClass(this.getClass(ce.getExtends()));
            mc.setUML(isUML);

            /*
             * automatic code generation
             */

            // add "public static void mains (String[] args)
            MethodDeclaration md = null;
            if(ce.genMain())
            {
                Vector<String> param = new Vector<String>();
                param.add("String[]");
                param.add("args");
                Vector<Vector<String>> args = new Vector<Vector<String>>();
                args.add(param);
                //md=mc.addMethod("void", "main", ModifierSet.PUBLIC+ModifierSet.STATIC, args);
                mc.addMethod("void", "main", ModifierSet.PUBLIC+ModifierSet.STATIC, args,ce.genDoc());
            }

            // package
            if(!ce.getPackage().trim().equals(""))
            {
                mc.addPackage(ce.getPackage());
            }
            
            //automatically import the interactable class of the interactive project
            //Obseolete??
            /*if(interactiveProject!=null)
            {
                mc.addImport("interactiveproject.knightsimulator.player");
            }*/

            // add JavaDOC Comments
            if(ce.genDoc())
            {
                mc.addClassJavaDoc();
                /*
                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                java.util.Date date = new java.util.Date();
                String today = dateFormat.format(date);
                JavadocComment jdc = new JavadocComment(
                     "\n"+
                     " * Write a description of class “"+ce.getClassName()+"“ here."+"\n"+
                     " * "+"\n"+
                     " * @author     "+System.getProperty("user.name")+"\n"+
                     " * @version    "+today+"\n"+
                     " "
                );
                mc.getNode().setJavaDoc(jdc);
                if(ce.genMain() && (md!=null))
                {
                    jdc = new JavadocComment(
                         "\n"+
                         "     * The main entry point for executing this program."+"\n"+
                         "     "
                    );
                    md.setJavaDoc(jdc);
                }
                 */
            }
            mc.updateContent();
            
            String code = mc.getJavaCode();
            code = code.replace(" {", "\n{");
            mc.setContent(StringList.explode(code, "\n"));

            this.addClass(mc);
            this.selectClass(mc);
            updateEditor();
            this.repaint();
            setChanged(true);
        }
    }

    public boolean containsClass(String classname)
    {
        //System.out.println("Looing for class: "+classname);
        /*
        Enumeration<String> enu =  classes.keys();
        while(enu.hasMoreElements())
        {
            String ele = enu.nextElement();
            System.out.println("- "+ele);
        }
        
        */
        return classes.containsKey(classname);
    }

    public void addClass(MyClass myClass)
    {
        addClass(myClass,false,true);
    }

    public void addClass(MyClass myClass, boolean compile)
    {
        addClass(myClass,false,true);
    }
    
    public void addClass(MyClass myClass, boolean compile, boolean showError)
    {
        /*System.out.println("Adding class: "+myClass.getShortName());
        for(String cName : classes.keySet())
        {
            System.out.print(cName+" - ");
        }
        System.out.println();
         */
        //if(classes.get(myClass.getShortName())==null)
        if(!classes.containsKey(myClass.getFullName()))
        {
            //System.out.println("New class: "+myClass.getFullName());
            // setup the grille with the existing classes
            Grille grille = new Grille();
            for(MyClass clas : classes.values())
            {
              grille.addSpace(clas);
            }
            //lu.fisch.unimozer.aligner.MainFrame main = new lu.fisch.unimozer.aligner.MainFrame(grille);
            // now we need to "draw" the new class
            // to get the correct dimensions
            // the size of the image doesn't matter, so we keep ir really small
            BufferedImage img = new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB);
            myClass.draw(img.getGraphics(), showFields, showMethods);
            // only find a position if this class has none yet!
            if(myClass.getPosition().x==0 && myClass.getPosition().y==0)
            {
                
                // now we need to find a space for the new class
                Area area = grille.findFreeAreaFor(myClass);
                // set the class to this position
                if(area!=null)
                    myClass.setPosition(area.getPosition());
                /**/
            }
            //main.setSpace(myClass);
            //main.repaint();
            // now we can add it to the array
            classes.put(myClass.getFullName(), myClass);
            
            setChanged(true);
            if(Unimozer.javaCompileOnTheFly && compile)
            {
                new Thread(new Runnable() {

                    @Override
                    public void run()
                    {
                        diagram.compile();
                    }
                }).start();
            }
        }
        else if(showError)
        {
            JOptionPane.showMessageDialog(frame, "Sorry, but you already have a class named “"+myClass.getShortName()+"“." , "Error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
        }
    }

    public MyClass getSelectedClass()
    {
        return mouseClass;
    }

    private Package getMousePackage(Point pt)
    {
        mousePackage = null;
        for(Package pack : packages.values())
        {
            if (pack.isInside(pt)) mousePackage=pack;
        }
        return mousePackage;
    }


    private MyClass getMouseClass(Point pt)
    {
        deselectAll();
        MyClass ret = null;
        
        /*
        Set<String> set = classes.keySet();
        Iterator<String> itr = set.iterator();
        while (itr.hasNext())
        {
          String str = itr.next();
        */
        
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String str = entry.getKey();
        
          if (classes.get(str).isInside(pt))
          {
              ret=classes.get(str);
          }
        }
        if(ret!=null) ret.select(pt);
        repaint();
        return ret;
    }

    private MyClass getMouseClassNoSelect(Point pt)
    {
        MyClass ret = null;
        
        /*Set<String> set = classes.keySet();
        Iterator<String> itr = set.iterator();
        while (itr.hasNext())
        {
          String str = itr.next();
          if (classes.get(str).isInside(pt))
          {
              ret=classes.get(str);
          }
        }*/
        
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            MyClass myClass = entry.getValue();
            if (myClass.isInside(pt))
            {
                ret=myClass;
            }
        }
        
        repaint();
        return ret;
    }

    private void deselectAll()
    {
        MyClass ret = null;
        /*
        Set<String> set = classes.keySet();
        Iterator<String> itr = set.iterator();
        while (itr.hasNext())
        {
          String str = itr.next();
        */
        
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String str = entry.getKey();
        
          classes.get(str).deselect();
        }
    }

    public void selectClass(MyClass myClass)
    {
        deselectAll();
        if(myClass!=null)
        {
            myClass.select(null);
            mouseClass=myClass;
            if (editor!=null)
            {
                this.updateEditor();
                //editor.setCode(myClass.getJavaCode());
                codeReader = new CodeReader(diagram, mouseClass, editor.getCode());
            }
        }
        frame.setButtons(mouseClass!=null);
        repaint();
    }

    public Vector<MyClass> getChildClasses(String of)
    {
        Vector<MyClass> ret = new Vector<MyClass>();
        
        /*Set<String> set = classes.keySet();
        Iterator<String> itr = set.iterator();
        while (itr.hasNext())
        {
          String str = itr.next();
        */
        
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String str = entry.getKey();
          
          MyClass mc = classes.get(str);
          if(mc.getExtendsClass()!=null)
          {
              if(mc.getExtendsClass().equals(of))
              {
                  ret.add(mc);
                  ret.addAll(getChildClasses(str));
              }
          }
          Vector<MyClass> uses = mc.getUsesMyClass();
          for(MyClass mac : uses)
          {
            if(mac.getShortName().equals(of))
            {
                ret.add(mc);
                ret.addAll(getChildClasses(str));
            }
          }
        }
        return ret;
    }

    public void updateEditor()
    {
        if (editor!=null)
        {
            if(mouseClass!=null)
            {
                addNewFilesAndReloadExistingSavedFiles();
                
                // WHAT THE HELL WAS allowEdit for???
                if(allowEdit==true)
                {
                    if(mouseClass.isDisplaySource())
                    {
                        // laod the code into the editor and enable it
                        editor.setClassName(mouseClass.getShortName());
                        editor.setCode(mouseClass.getJavaCode());
                        //editor.setMouseClass(mouseClass);
                        if(mouseClass.getSelected()!=null)
                        {
                            editor.setCursorTo(mouseClass.getSelected().getFullName());
                        }
                        //if(frame.showComments()) editor.setCode(mouseClass.getJavaCode());
                        //else editor.setCode(mouseClass.getJavaCodeCommentless());
                    }
                    else cleanEditor();
                }
                else
                {
                    if(mouseClass.isDisplaySource())
                    {
                        editor.setCode(mouseClass.getContent().getText());
                        editor.setClassName(mouseClass.getShortName());
                        if(mouseClass.getSelected()!=null)
                        {
                            editor.setCursorTo(mouseClass.getSelected().getFullName());
                        }
                    }
                    else cleanEditor();
                }
                editor.setEnabled(true);
                showParseStatus(mouseClass.parse());
            }
            else
            { 
                cleanEditor();
            }
        } 
    }

    private void cleanEditor()
    {
        // clean the editor and dissable it
        if (editor!=null)
        {
            editor.setCode("");
            editor.setClassName("");
            editor.setEnabled(false);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        /*
        if(isEnabled())
        {
            mousePoint = e.getPoint();
            mousePressed = true;
            mouseClass = getMouseClass(mousePoint);
            //System.out.println(e.getButton());
            if(e.getClickCount()<=1 && e.getButton()==MouseEvent.BUTTON1)
            {
                if(mouseClass!=null)
                {
                    frame.setButtons(mouseClass.isValidCode());
                }
                updateEditor();
            }
            else if(e.getClickCount()==2  && e.getButton()==MouseEvent.BUTTON1 )
            {
                if(mouseClass!=null)
                {
                    Element ele = mouseClass.getSelected();
                    if (ele!=null && allowEdit==true)
                    {
                        if(ele.getType()==Element.CLASS)
                        {
                            ClassEditor cd = ClassEditor.showModal(frame, "Edit class", ele, mouseClass.getExtendsClass());
                            // remove the class from the hashtable
                            classes.remove(mouseClass.getShortName());
                            // update the class
                            mouseClass.update(ele, cd.getClassName(), cd.getModifier(), cd.getExtends());
                            mouseClass.setExtendsClass(cd.getExtends());
                            mouseClass.setExtendsMyClass(getClass(cd.getExtends()));
                            // add the class again to the hashtable
                            classes.put(mouseClass.getShortName(), mouseClass);
                            // set changed state
                            setChanged(true);
                        }
                        else if(ele.getType()==Element.METHOD)
                        {
                            MethodEditor md = MethodEditor.showModal(frame, "Edit method", ele);
                            mouseClass.update(ele, md.getMethodType(), md.getMethodName(), md.getModifier(), md.getParams());
                            // set changed state
                            setChanged(true);
                        }
                        else if(ele.getType()==Element.FIELD)
                        {
                            FieldEditor fd = FieldEditor.showModal(frame, "Edit field", ele);
                            mouseClass.update(ele, fd.getFieldType(), fd.getFieldName(), fd.getModifier());
                            // set changed state
                            setChanged(true);
                        }
                        else if(ele.getType()==Element.CONSTRUCTOR)
                        {
                            ConstructorEditor cd = ConstructorEditor.showModal(frame, "Edit constructor", ele);
                            mouseClass.update(ele, cd.getModifier(), cd.getParams());
                            // set changed state
                            setChanged(true);
                        }
                    }
                    repaint();
                    updateEditor();
                }
            }
            frame.setButtons(mouseClass!=null);
            if(mouseClass!=null) frame.setButtons(mouseClass.isValidCode());
         }
         /**/
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        
        if(isEnabled())
        {
            if (e.getSource() instanceof JMenuItem)
            {
                JMenuItem item = (JMenuItem) e.getSource();
                if(item.getText().equals("Compile"))
                {
                    compile();
                }
                else if(item.getText().equals("Remove class") && mouseClass!=null)
                {
                    int answ = JOptionPane.showConfirmDialog(frame, "Are you sure to remove the class “"+mouseClass.getFullName()+"“", "Remove a class", JOptionPane.YES_NO_OPTION);
                    if (answ == JOptionPane.YES_OPTION)
                    {
                        cleanAll();// clean(mouseClass);
                        removedClasses.add(mouseClass.getFullName());
                        classes.remove(mouseClass.getFullName());
                        mouseClass=null;
                        updateEditor();
                        repaint();
                        objectizer.repaint();
                    }
                }
                else if(mouseClass.getName().contains("abstract"))
                {
                    JOptionPane.showMessageDialog(frame, "Can't create an object of an “abstract“ class ...", "Instatiation error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                }
                else if (item.getText().startsWith("new")) // we have constructor
                {
Logger.getInstance().log("Click on <new> registered");
                    // get full signature
                    String fSign = item.getText();
                    if(fSign.startsWith("new")) fSign=fSign.substring(3).trim();
                    // get signature
                    final String fullSign = fSign;
Logger.getInstance().log("fullSign = "+fSign);
                    final String sign = mouseClass.getSignatureByFullSignature(fullSign);
Logger.getInstance().log("sign = "+sign);

Logger.getInstance().log("Creating runnable ...");
                    Runnable r = new Runnable()
                    {
                            @Override
                            public void run()
                            {
                                //System.out.println("Calling method (full): "+fullSign);
                                //System.out.println("Calling method       : "+sign);
                                try
                                {
Logger.getInstance().log("Loading the class <"+mouseClass.getName()+">");
                                    Class<?> cla = Runtime5.getInstance().load(mouseClass.getFullName()); //mouseClass.load();
Logger.getInstance().log("Loaded!");
                                    
                                    // check if we need to specify a generic class
                                    boolean cont = true;
                                    String generics = "";
                                    TypeVariable[] tv = cla.getTypeParameters();
Logger.getInstance().log("Got TypeVariables with length = "+tv.length);
                                    if(tv.length>0)
                                    {
                                        LinkedHashMap<String,String> gms = new LinkedHashMap<String,String>();
                                        for(int i=0;i<tv.length;i++)
                                        {
                                            gms.put(tv[i].getName(),"");
                                        }
                                        MethodInputs gi = new MethodInputs(frame,gms,"Generic type declaration","Please specify the generic types");
                                        cont = gi.OK;
                                        
                                        // build the string
                                        generics = "<";
                                        Object[] keys = gms.keySet().toArray();
                                        mouseClass.generics.clear();
                                        for(int in=0;in<keys.length;in++)
                                        {
                                            String kname = (String) keys[in];
                                            // save generic type to myClass
                                            mouseClass.generics.put(kname, gi.getValueFor(kname));
                                            generics+=gi.getValueFor(kname)+",";
                                        }
                                        generics=generics.substring(0, generics.length()-1);
                                        generics+=">";                                        
                                    }
                                    
                                    if(cont==true)
                                    {
Logger.getInstance().log("Getting the constructors.");

                                        Constructor[] constr = cla.getConstructors();
                                        for(int c = 0;c<constr.length;c++)
                                        {
                                            // get signature
                                            String full = objectizer.constToFullString(constr[c]);
Logger.getInstance().log("full = "+full);
                                            /*
                                            String full = constr[c].getName();
                                            full+="(";
                                            Class<?>[] tvm = constr[c].getParameterTypes();
                                            for(int t=0;t<tvm.length;t++)
                                            {
                                                String sn = tvm[t].toString();
                                                sn=sn.substring(sn.lastIndexOf('.')+1,sn.length());
                                                if(sn.startsWith("class")) sn=sn.substring(5).trim();
                                                // array is shown as ";"  ???
                                                if(sn.endsWith(";"))
                                                {
                                                    sn=sn.substring(0,sn.length()-1)+"[]";
                                                }
                                                full+= sn+", ";
                                            }
                                            if(tvm.length>0) full=full.substring(0,full.length()-2);
                                            full+= ")";
                                            */
                                            /*System.out.println("Loking for S : "+sign);
                                            System.out.println("Loking for FS: "+fullSign);
                                            System.out.println("Found: "+full);*/

                                            if(full.equals(sign) || full.equals(fullSign))
                                            {
Logger.getInstance().log("We are in!");
                                                //editor.setEnabled(false);
//Logger.getInstance().log("Editor disabled");
                                                String name;
Logger.getInstance().log("Ask user for a name.");
                                                do
                                                {
                                                    String propose = mouseClass.getShortName().substring(0, 1).toLowerCase()+mouseClass.getShortName().substring(1);
                                                    int count = 0;
                                                    String prop = propose+count;
                                                    while(objectizer.hasObject(prop)==true)
                                                    {
                                                        count++;
                                                        prop = propose+count;
                                                    }

                                                    name = (String) JOptionPane.showInputDialog(frame, "Please enter the name for you new instance of “"+mouseClass.getShortName()+"“", "Create object", JOptionPane.QUESTION_MESSAGE,null,null,prop);
                                                    if (Java.isIdentifierOrNull(name)==false)
                                                    {
                                                        JOptionPane.showMessageDialog(frame, "“"+name+"“ is not a correct identifier." , "Error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                                                    }
                                                    else if (objectizer.hasObject(name)==true)
                                                    {
                                                        JOptionPane.showMessageDialog(frame, "An object with the name “"+name+"“ already exists.\nPlease choose another name ..." , "Error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                                                    }
                                                } while (Java.isIdentifierOrNull(name)==false || objectizer.hasObject(name)==true);
Logger.getInstance().log("name = "+name);

                                                if(name!=null)
                                                {
Logger.getInstance().log("Need to get inputs ...");
                                                    LinkedHashMap<String,String> inputs = mouseClass.getInputsBySignature(sign);
                                                    if(inputs.size()==0) inputs = mouseClass.getInputsBySignature(fullSign);
                                                    
                                                    //System.out.println("1) "+sign);
                                                    //System.out.println("2) "+fullSign);
                                                    
                                                    MethodInputs mi = null;
                                                    boolean go = true;
                                                    if(inputs.size()>0)
                                                    {
                                                        mi = new MethodInputs(frame,inputs,full,mouseClass.getJavaDocBySignature(sign));
                                                        go = mi.OK;
                                                    }
Logger.getInstance().log("go = "+go);
                                                    if(go==true)
                                                    {
Logger.getInstance().log("Building string ...");
                                                        //Object arglist[] = new Object[inputs.size()];
                                                        String constructor = "new "+mouseClass.getFullName()+generics+"(";
                                                        if(inputs.size()>0)
                                                        {
                                                            Object[] keys = inputs.keySet().toArray();
                                                            for(int in=0;in<keys.length;in++)
                                                            {
                                                                String kname = (String) keys[in];
                                                                //String type = inputs.get(kname);

                                                                //if(type.equals("int"))  { arglist[in] = Integer.valueOf(mi.getValueFor(kname)); }
                                                                //else if(type.equals("short"))  { arglist[in] = Short.valueOf(mi.getValueFor(kname)); }
                                                                //else if(type.equals("byte"))  { arglist[in] = Byte.valueOf(mi.getValueFor(kname)); }
                                                                //else if(type.equals("long"))  { arglist[in] = Long.valueOf(mi.getValueFor(kname)); }
                                                                //else if(type.equals("float"))  { arglist[in] = Float.valueOf(mi.getValueFor(kname)); }
                                                                //else if(type.equals("double"))  { arglist[in] = Double.valueOf(mi.getValueFor(kname)); }
                                                                //else if(type.equals("boolean"))  { arglist[in] = Boolean.valueOf(mi.getValueFor(kname)); }
                                                                //else arglist[in] = mi.getValueFor(kname);
                                                                
                                                                String val = mi.getValueFor(kname);
                                                                if (val.equals("")) val="null";
                                                                else
                                                                {
                                                                    String type = mi.getTypeFor(kname);
                                                                    if (type.toLowerCase().equals("byte"))
                                                                        constructor+="Byte.valueOf(\""+val+"\"),";
                                                                    else if (type.toLowerCase().equals("short"))
                                                                        constructor+="Short.valueOf(\""+val+"\"),";
                                                                    else if (type.toLowerCase().equals("float"))
                                                                        constructor+="Float.valueOf(\""+val+"\"),";
                                                                    else if (type.toLowerCase().equals("long"))
                                                                        constructor+="Long.valueOf(\""+val+"\"),";
                                                                    else if (type.toLowerCase().equals("double"))
                                                                        constructor+="Double.valueOf(\""+val+"\"),";
                                                                    else if (type.toLowerCase().equals("char"))
                                                                        constructor+="'"+val+"',";
                                                                    else
                                                                        constructor+=val+",";
                                                                }
                                                                
                                                                //constructor+=mi.getValueFor(kname)+",";
                                                            }
                                                            constructor=constructor.substring(0, constructor.length()-1);
                                                        }
                                                        //System.out.println(arglist);
                                                        constructor+=")";
                                                        //System.out.println(constructor);
Logger.getInstance().log("constructor = "+constructor);

                                                        //LOAD: 
                                                        //addLibs();
                                                        Object obj = Runtime5.getInstance().getInstance(name, constructor); //mouseClass.getInstance(name, constructor);
Logger.getInstance().log("Objet is now instantiated!");
                                                        //Runtime.getInstance().interpreter.getNameSpace().i
                                                        //System.out.println(obj.getClass().getSimpleName());
                                                        //Object obj = constr[c].newInstance(arglist);
                                                        MyObject myo = objectizer.addObject(name, obj);
                                                        myo.setMyClass(mouseClass);
                                                        obj = null;
                                                        cla = null;
                                                    }

                                                    objectizer.repaint();
Logger.getInstance().log("Objectizer repainted ...");
                                                    repaint();
Logger.getInstance().log("Diagram repainted ...");
                                                }
                                                //editor.setEnabled(true);
//Logger.getInstance().log("Editor enabled again ...");

                                            }
                                        }
                                    }
                                }
                                catch (Exception ex)
                                {
                                    //ex.printStackTrace();
                                    MyError.display(ex);
                                    JOptionPane.showMessageDialog(frame, ex.toString(), "Instantiation error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                                }
                            }
                    };
                    Thread t = new Thread(r);
                    t.start();
                }
                else // we have a static method
                {
                    try
                    {
                        // get full signature
                        String fullSign = ((JMenuItem) e.getSource()).getText();
                        // get signature
                        String sign = mouseClass.getSignatureByFullSignature(fullSign).replace(", ", ",");
                        String complete = mouseClass.getCompleteSignatureBySignature(sign).replace(", ", ",");

                        /*System.out.println("Calling method (full): "+fullSign);
                        System.out.println("Calling method       : "+sign);
                        System.out.println("Calling method (comp): "+complete);/**/

                        // find method
                        Class c = Runtime5.getInstance().load(mouseClass.getFullName());
                        Method m[] = c.getMethods();
                        for (int i = 0; i < m.length; i++)
                        {
                            /*String full = "";
                            full+= m[i].getReturnType().getSimpleName();
                            full+=" ";
                            full+= m[i].getName();
                            full+= "(";
                            Class<?>[] tvm = m[i].getParameterTypes();
                            LinkedHashMap<String,String> genericInputs = new LinkedHashMap<String,String>();
                            for(int t=0;t<tvm.length;t++)
                            {
                                String sn = tvm[t].toString();
                                //System.out.println(sn);
                                if(sn.startsWith("class")) sn=sn.substring(5).trim();
                                sn=sn.substring(sn.lastIndexOf('.')+1,sn.length());
                                // array is shown as ";"  ???
                                if(sn.endsWith(";"))
                                {
                                    sn=sn.substring(0,sn.length()-1)+"[]";
                                }
                                full+= sn+", ";
                                genericInputs.put("param"+t,sn);
                            }
                            if(tvm.length>0) full=full.substring(0,full.length()-2);
                            full+= ")";*/
                            String full = objectizer.toFullString(m[i]);
                            LinkedHashMap<String,String> genericInputs = objectizer.getInputsReplaced(m[i],null);

                            /*System.out.println("Looking for S : "+sign);
                            System.out.println("Looking for FS: "+fullSign);
                            System.out.println("Found         : "+full);*/

                            if(full.equals(sign) || full.equals(fullSign))
                            {
                                LinkedHashMap<String,String> inputs = mouseClass.getInputsBySignature(sign);
                                //Objectizer.printLinkedHashMap("inputs", inputs);
                                if(inputs.size()!=genericInputs.size())
                                {
                                    inputs=genericInputs;
                                }
                                //Objectizer.printLinkedHashMap("inputs", inputs);
                                MethodInputs mi = null;
                                boolean go = true;
                                if(inputs.size()>0)
                                {
                                    mi = new MethodInputs(frame,inputs,full,mouseClass.getJavaDocBySignature(sign));
                                    go = mi.OK;
                                }
                                if(go==true)
                                {
                                    try
                                    {
                                        String method = mouseClass.getFullName()+"."+m[i].getName()+"(";
                                        if(inputs.size()>0)
                                        {
                                            Object[] keys = inputs.keySet().toArray();
                                            //int cc = 0;
                                            for(int in=0;in<keys.length;in++)
                                            {
                                                String name = (String) keys[in];
                                                String val = mi.getValueFor(name);
                                                
                                                if (val.equals("")) method+=val+"null,";
                                                else
                                                {
                                                    String type = mi.getTypeFor(name);
                                                    if (type.toLowerCase().equals("byte"))
                                                        method+="Byte.valueOf(\""+val+"\"),";
                                                    else if (type.toLowerCase().equals("short"))
                                                        method+="Short.valueOf(\""+val+"\"),";
                                                    else if (type.toLowerCase().equals("float"))
                                                        method+="Float.valueOf(\""+val+"\"),";
                                                    else if (type.toLowerCase().equals("long"))
                                                        method+="Long.valueOf(\""+val+"\"),";
                                                    else if (type.toLowerCase().equals("double"))
                                                        method+="Double.valueOf(\""+val+"\"),";
                                                    else if (type.toLowerCase().equals("char"))
                                                        method+="'"+val+"',";
                                                    else
                                                        method+=val+",";
                                                }

                                                //if (val.equals("")) val="null";
                                                //method+=val+",";
                                            }
                                            if(!method.endsWith("("))
                                                method=method.substring(0, method.length()-1);
                                        }
                                        method+=")";
                                        
                                        //System.out.println(method);

                                        // Invoke method in a new thread
                                        final String myMeth = method;
                                        Runnable r = new Runnable()
                                        {
                                                public void run()
                                                {
                                                    try
                                                    {
                                                        Object retobj = Runtime5.getInstance().executeMethod(myMeth);
                                                        if(retobj!=null) JOptionPane.showMessageDialog(frame, retobj.toString(), "Result", JOptionPane.INFORMATION_MESSAGE,Unimozer.IMG_INFO);
                                                    }
                                                    catch (EvalError ex)
                                                    {
                                                        JOptionPane.showMessageDialog(frame, ex.toString(), "Invokation error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                                                        MyError.display(ex);
                                                    }
                                                }
                                        };
                                        Thread t = new Thread(r);
                                        t.start();

                                        //System.out.println(method);
                                        //Object retobj = Runtime5.getInstance().executeMethod(method);
                                        //if(retobj!=null) JOptionPane.showMessageDialog(frame, retobj.toString(), "Result", JOptionPane.INFORMATION_MESSAGE,Unimozer.IMG_INFO);
                                    }
                                    catch (Throwable ex)
                                    {
                                        JOptionPane.showMessageDialog(frame, ex.toString(), "Execution error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                                        MyError.display(ex);
                                    }
                                }
                            }
                        }
                    }
                    catch(Exception ex)
                    {
                        JOptionPane.showMessageDialog(frame, ex.toString(), "Execution error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                        MyError.display(ex);
                    }
                }
            }
        }
    }

    public void updateFromCode()
    {
        if(codeReader!=null) 
        {
            codeReader.doUpdate();
            setChanged(true);
        }
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        if(isEnabled() && e.getButton()==MouseEvent.BUTTON1)
        {
            if (mode==MODE_EXTENDS)
            {
                // get the clicked point
                mousePoint = e.getPoint();
                mousePressed = true;
                // get clicked class
                extendsFrom = getMouseClass(mousePoint);
                extendsDragPoint=e.getPoint();

                if (extendsFrom==null)
                {
                    setMode(MODE_SELECT);
                    frame.updateMode();
                }
            }
            else
            {
                if(e.getButton()==MouseEvent.BUTTON1) mousePressed = true;
                // get the clicked point
                mousePoint = e.getPoint();
                // get clicked package
                getMousePackage(mousePoint);
                // get clicked class
                MyClass clickClass = getMouseClass(mousePoint);
                if (clickClass!=null) mouseRelativePoint = clickClass.getRelative(mousePoint);
                else if (mousePackage!=null)
                {
                    mouseRelativePoint=mousePoint;
                }
                // load the clicked class
                loadClickedClass(e);
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        if(isEnabled() && e.getButton()==MouseEvent.BUTTON1)
        {
            if (mode==MODE_EXTENDS)
            {
                extendsTo=getMouseClass(e.getPoint());
                if ((extendsFrom!=null) && (extendsTo!=null) && (extendsFrom!=extendsTo))
                {
                    // %TODO%
                    extendsFrom.addExtends(extendsTo.getShortName());
                    repaint();
                }
                // change the mode back to SELECT
                setMode(MODE_SELECT);
                // update the bouttons in the mainform
                frame.updateMode();

                // free pointers
                extendsDragPoint=null;
                extendsFrom=null;
                extendsTo=null;
                deselectAll();
            }
            else
            {
                // load the clickled class (if clicked!)
                if(mousePressed==true) loadClickedClass(e);
                mousePressed = false;
            }
        }
    }

    private void loadClickedClass(MouseEvent e)
    {
        mousePoint = e.getPoint();
        MyClass clickClass = getMouseClass(mousePoint);

        if(e.getClickCount()<=1) // && e.getButton()==MouseEvent.BUTTON1) // && clickClass!=mouseClass)
        {
            // assign the selected class
            mouseClass=clickClass;
            // set the codereader
            codeReader = new CodeReader(this, mouseClass, editor.getCode());
            // update the NSD
            if(mouseClass!=null) mouseClass.updateNSD(getNsd());
            // set button states
            frame.setButtons(mouseClass!=null);
            if(mouseClass!=null) frame.setButtons(mouseClass.isValidCode());
            // update the editor
            updateEditor();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        if (mousePressed == true && isEnabled())
        {
            if (mode==MODE_EXTENDS)
            {
                extendsDragPoint=e.getPoint();
                repaint();
            }
            else
            {
                boolean doRepaint = false;
                if(mouseClass!=null)
                {
                    mouseClass.setPosition(new Point(e.getX()-mouseRelativePoint.x,
                                                     e.getY()-mouseRelativePoint.y));
                    doRepaint=true;
                }
                else if(mousePackage!=null)
                {
                    for(MyClass myClass : classes.values())
                    {
                        if(myClass.getPackagename().equals(mousePackage.getName()))
                        {
                            myClass.setPosition(new Point(myClass.getPosition().x+e.getX()-mouseRelativePoint.x,
                                                          myClass.getPosition().y+e.getY()-mouseRelativePoint.y));
                        }
                       doRepaint=true;
                    }
                    mouseRelativePoint=e.getPoint();
                }
                if (doRepaint) repaint();
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        if(isEnabled())
        {
            // reset
            commentString=null;
            commentPoint=null;
            // get position
            mousePoint = e.getPoint();
            MyClass mouseAtCursor = getMouseClassNoSelect(mousePoint);
            if(mouseAtCursor!=null)
            {
                Element ele = mouseAtCursor.getHover(mousePoint);
                if(ele!=null)
                {
                    if(ele.getJavaDoc()!=null)
                    {
                        commentString=ele.getJavaDoc();
                        commentPoint=mousePoint;
                    }
                }
            }
            repaint();
        }
    }

    public MyClass getClass(String className)
    {
        return classes.get(className);
    }
    
    public MyClass getClass(int index)
    {
        return classes.get(classes.keySet().toArray()[index]);
    }

    /**
     * @return the editor
     */
    public CodeEditor getEditor()
    {
        return editor;
    }

    /**
     * @param editor the editor to set
     */
    public void setEditor(CodeEditor editor)
    {
        this.editor = editor;
    }

    /**
     * @param frame the frame to set
     */
    public void setFrame(Mainform frame)
    {
        this.frame = frame;
    }
    /*
    public void clean(MyClass mc)
    {
        if(objectizer!=null) objectizer.removeByClassName(mc);
        mc.setCompiled(false);
        Vector<MyClass> remThem = getChildClasses(mc.getShortName());
        for(MyClass myc : remThem)
        {
            if(objectizer!=null) objectizer.removeByClassName(myc);
            myc.setCompiled(false);
        }
    }
    */

    public void cleanAll()
    {
        // clean objectizer
        if(objectizer!=null) objectizer.removeAllObjects();
        /*
        // set all classes to be not compiled
        Set<String> set = classes.keySet();
        Iterator<String> itr = set.iterator();
        while (itr.hasNext())
        {
          String str = itr.next();
        */
        
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String str = entry.getKey();

            classes.get(str).setCompiled(false);
        }
    }

    public void clear()
    {
        mouseClass = null;
        if(objectizer!=null) objectizer.removeAllObjects();
        if(nsd!=null)
        {
            nsd.setRoot(MyClass.setErrorNSD(),false,true);
            nsd.getParent().getParent().repaint();
        }
        /*
        Set<String> set = classes.keySet();
        Iterator<String> itr = set.iterator();
        while (itr.hasNext())
        {
          String str = itr.next();
          if(objectizer!=null)
          {
            objectizer.removeByClassName(classes.get(str));
          }
        }
        */
        classes.clear();
        removedClasses.clear();
        directoryName=null;
        interactiveProject=null;
        Console.cls();

        if(frame!=null)
            frame.setCompilationErrors(new Vector<CompilationError>());

        repaint();
        if(objectizer!=null) objectizer.repaint();
        updateEditor();
        setEnabled(true);
        setChanged(false);
    }

    public void about()
    {
        About a = new About(frame);
        a.setLocationRelativeTo(frame);
        a.setVisible(true);
        //System.err.println("about");
    }

    /**
     * @param objectizer the objectizer to set
     */
    public void setObjectizer(Objectizer objectizer)
    {
        this.objectizer = objectizer;
    }

    /**
     * @return the directoryName
     */
    public String getDirectoryName()
    {
        return directoryName;
    }

    /**
     * @return the directoryName
     */
    public String getContainingDirectoryName()
    {
        return containingDirectoryName;
    }

    /**
     * @param directoryName the directoryName to set
     */
    public void setDirectoryName(String directoryName)
    {
        this.directoryName = directoryName;

        String dir = new String(directoryName);
        if(dir.endsWith(System.getProperty("file.separator"))) dir=dir.substring(0,dir.length()-2);
        dir = dir.substring(0,dir.lastIndexOf(System.getProperty("file.separator")));
        setContainingDirectoryName(dir);
    }

    /**
     * @return the status
     */
    public JLabel getStatus()
    {
        return status;
    }

    @Override
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);
        if(frame!=null)
            frame.setEnabledActions(b);
        if(mouseClass!=null) frame.setButtons(mouseClass.isValidCode());
        if(b==false) frame.setButtons(false);
        repaint();
    }

    /**
     * @param status the status to set
     */
    public void setStatus(JLabel status)
    {
        this.status = status;
    }

    /**
     * @return the showHeritage
     */
    public boolean isShowHeritage()
    {
        return showHeritage;
    }

    /**
     * @param showHeritage the showHeritage to set
     */
    public void setShowHeritage(boolean showHeritage)
    {
        this.showHeritage = showHeritage;
    }

    /**
     * @return the showComposition
     */
    public boolean isShowComposition()
    {
        return showComposition;
    }

    /**
     * @param showComposition the showComposition to set
     */
    public void setShowComposition(boolean showComposition)
    {
        this.showComposition = showComposition;
    }

    /**
     * @return the showAggregation
     */
    public boolean isShowAggregation()
    {
        return showAggregation;
    }

    /**
     * @param showAggregation the showAggregation to set
     */
    public void setShowAggregation(boolean showAggregation)
    {
        this.showAggregation = showAggregation;
    }

    /**
     * @return the allowEdit
     */
    public boolean isAllowEdit()
    {
        return allowEdit;
    }

    /**
     * @param allowEdit the allowEdit to set
     */
    /*
    public void setAllowEdit(boolean allowEdit)
    {
        this.allowEdit = allowEdit;
    }
     */

    /**
     * @return the structorizer
     */
    public lu.fisch.structorizer.gui.Mainform getStructorizer()
    {
        return structorizer;
    }

    /**
     * @param structorizer the structorizer to set
     */
    public void setStructorizer(lu.fisch.structorizer.gui.Mainform structorizer)
    {
        this.structorizer = structorizer;
    }

    /**
     * @return the nsd
     */
    public lu.fisch.structorizer.gui.Diagram getNsd()
    {
        return nsd;
    }

    /**
     * @param nsd the nsd to set
     */
    public void setNsd(lu.fisch.structorizer.gui.Diagram nsd)
    {
        this.nsd = nsd;
    }

    /**
     * @return the hasChanged
     */
    public boolean isChanged()
    {
        return hasChanged;
    }

    /**
     * @param hasChanged the hasChanged to set
     */
    public void setChanged(boolean hasChanged)
    {
        this.hasChanged = hasChanged;
        //this.hasChangedAutoSave = hasChanged;

        if(frame!=null) frame.setTitleNew();
        // sstop the timer if running
        if (saveTimer.isRunning()) saveTimer.stop();
        // restart if something has changed
        if(hasChanged==true) saveTimer.start();
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
        
        /* this code seams not to be threadsafe!
        // draw classes again to put them on top
        // of the arrows
        Set<String> set;
        Iterator<String> itr;
        set = classes.keySet();
        itr = set.iterator();
        while (itr.hasNext()) 
        {
          String str = itr.next();
          classes.get(str).setUML(isUML);
        }
        */
        
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            //String key = entry.getKey();
            MyClass myClass = entry.getValue();
            myClass.setUML(isUML);
        }

    }

    /**
     * @param containingDirectoryName the containingDirectoryName to set
     */
    public void setContainingDirectoryName(String containingDirectoryName)
    {
        this.containingDirectoryName = containingDirectoryName;
    }

    public void printDiagram()
    {
        // print preview takes a lot of memory (don't know why)
        // so it is a good idea to sugest to the JVM to clean up the heap
        System.gc();
        printOptions = PrintOptions.showModal(frame, "Print options");
        if(printOptions.OK==true)
        {
            this.deselectAll();
            this.cleanAll();
            this.repaint();

            if (printOptions.getJob()==PrintOptions.JOB_PREVIEW)
            {
                PrintPreview pp = new PrintPreview(frame,this);
                pp.setLocation(Math.round((frame.getWidth()-pp.getWidth())/2+frame.getLocation().x),
                                           (frame.getHeight()-pp.getHeight())/2+frame.getLocation().y);
                pp.setVisible(true);
            }
            else
            {
                try
                {
                    // Use default printer, no dialog
                    PrinterJob prnJob = PrinterJob.getPrinterJob();

                    // get the default page format
                    PageFormat pf0 = prnJob.defaultPage();
                    // clone it
                    PageFormat pf1 = (PageFormat) pf0.clone();
                    Paper p = pf0.getPaper();
                    // set to zero margin
                    p.setImageableArea(0, 0, pf0.getWidth(), pf0.getHeight());
                    pf1.setPaper(p);
                    // let the printer validate it
                    PageFormat pf2 = prnJob.validatePage(pf1);
                    //prnJob.pageDialog(prnJob.defaultPage());
                    
                    prnJob.setPrintable(this,pf2);
                    if (prnJob.printDialog())
                    {
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        prnJob.print();
                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                }
                catch (PrinterException ex)
                {
                    ex.printStackTrace();
                    System.err.println("Printing error: "+ex.toString());
                }
            }
        }
        System.gc();
    }

    private void printHeaderFooter(Graphics g, PageFormat pageFormat, int page, String className)
    {
        int origPage = page+1;

        // Add header
        g.setColor(Color.BLACK);
        int xOffset = (int)pageFormat.getImageableX();
        int topOffset = (int)pageFormat.getImageableY()+20;
        int bottom = (int)(pageFormat.getImageableY()+pageFormat.getImageableHeight());
        // header line
        g.drawLine(xOffset, topOffset-8, xOffset+(int)pageFormat.getImageableWidth(), topOffset-8);
        // footer line
        g.drawLine(xOffset,                                    bottom-11,
                  xOffset+(int)pageFormat.getImageableWidth(), bottom-11);
        g.setFont(new Font(Font.SANS_SERIF,Font.ITALIC,10));

        Graphics2D gg = (Graphics2D) g;
        String pageString = "Page "+origPage;
        int tw = (int) gg.getFont().getStringBounds(pageString,gg.getFontRenderContext()).getWidth();
        // footer text
        g.drawString(pageString, xOffset+(int)pageFormat.getImageableWidth()-tw, bottom-2);

        //System.err.println("Printing: "+directoryName);
        if(directoryName!=null)
        {
            g.setFont(new Font(g.getFont().getFontName(),Font.ITALIC,10));
            String filename = directoryName;
            if(!className.equals("")) filename+=System.getProperty("file.separator")+className+".java";
            // header text
            g.drawString(filename, xOffset, bottom-2);
            File f = new File(filename);
            //System.err.println("Printing: "+filename);
            if(f.exists())
            {
                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                java.util.Date date = new java.util.Date();
                date.setTime(f.lastModified());
                String myDate = dateFormat.format(date);
                int w = (int) gg.getFont().getStringBounds(myDate,gg.getFontRenderContext()).getWidth();
                // header text
                g.drawString("File last modified on "+myDate, xOffset, topOffset-10);
            }
        }
    }

    @Override
    public int print(Graphics g, PageFormat pageFormat, int page) throws PrinterException
    {
/*
                // clone paper
                Paper originalPaper = pageFormat.getPaper();
                Paper paper = new Paper();
                // resize it
                paper.setSize(originalPaper.getWidth(), originalPaper.getHeight());
                paper.setImageableArea(
                        originalPaper.getImageableX(),
                        originalPaper.getImageableY()+30,
                        originalPaper.getImageableWidth(),
                        originalPaper.getImageableHeight()-60);
                // apply it
                pageFormat.setPaper(paper);
*/
/*                Paper paper = new Paper();
                paper.setSize(pageFormat.getWidth(),pageFormat.getHeight());
                
                double paddingLeftRight = pageFormat.getImageableX();
                double paddingTopBottom = pageFormat.getImageableY()+30;
                if (pageFormat.getOrientation()==PageFormat.LANDSCAPE)
                {
                    paddingLeftRight = 60;
                    paddingTopBottom = 60;
                }
                paper.setImageableArea(paddingLeftRight,
                                       paddingTopBottom,
                                       pageFormat.getWidth()-2*paddingLeftRight,
                                       pageFormat.getHeight()-2*paddingTopBottom);
                pageFormat.setPaper(paper);*/

                if(page==0)
                {
                    pageList.clear();
                    if(printOptions.printCode()==true)
                    {
                        /*Set<String> set = classes.keySet();
                        Iterator<String> itr = set.iterator();
                        while (itr.hasNext())
                        {
                          String str = itr.next();*/
                        
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String str = entry.getKey();
                        
                          MyClass thisClass = classes.get(str);
                          CodeEditor edit = new CodeEditor();
                          edit.setFont(new Font(edit.getFont().getName(),Font.PLAIN,printOptions.getFontSize()));

                          String code = "";
                          // get code, depending on JavaDoc filter
                          if(printOptions.printJavaDoc()) code=thisClass.getContent().getText();
                          else code=thisClass.getJavaCodeCommentless();

                          // filter double lines
                          if(printOptions.filterDoubleLines())
                          {
                            StringList sl = StringList.explode(code, "\n");
                            sl.removeDoubleEmptyLines();
                            code=sl.getText();
                          }

                          //edit.setDiagram(diagram);
                          edit.setCode(code);

                          // resize the picture
                          PageFormat pf = (PageFormat) pageFormat.clone();
                          Paper pa = pf.getPaper();
                          pa.setImageableArea(
                                  pa.getImageableX(),
                                  pa.getImageableY()+20,
                                  pa.getImageableWidth(),
                                  pa.getImageableHeight()-40
                          );
                          pf.setPaper(pa);

                          //sheets = new Vector<BufferedImage>();
                          int p = 0;
                          int result = 0;
                          do
                          {
                            /*BufferedImage img = new BufferedImage((int) pageFormat.getImageableWidth(),(int) pageFormat.getImageableHeight(),BufferedImage.TYPE_INT_RGB );
                            img.getGraphics().setColor(Color.WHITE);
                            img.getGraphics().fillRect(0,0,(int) pageFormat.getImageableWidth(),(int) pageFormat.getImageableHeight());*/

                            BufferedImage img = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB );
                            result = edit.print(img.createGraphics(), pf, p);
                            if (result==PAGE_EXISTS)
                            {
                                //sheets.add(img);
                                pageList.add(str);
                                p++;
                            }
                          }
                          while (result==PAGE_EXISTS);

                          //edit.print(g, pf, p);
                          edit=null;
                          System.gc();
                        }
                    }
                }

		if (page == 0 && printOptions.printDiagram()==true)
		{
                        Graphics2D g2d = (Graphics2D) g;

                        int yOffset = (int)pageFormat.getImageableY();

			g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

                        double sX = (pageFormat.getImageableWidth()-1)/getDiagramWidth();
                        double sY = (pageFormat.getImageableHeight()-1-40)/getDiagramHeight();
                        double sca = Math.min(sX,sY);
                        if (sca>1) {sca=1;}
                        g2d.translate(0, 20);
                        g2d.scale(sca,sca);

			paint(g2d);
                        
                        g2d.scale(1/sca,1/sca);
                        g2d.translate(0, -(20));
			g2d.translate(-pageFormat.getImageableX(), -pageFormat.getImageableY());
                        
                        printHeaderFooter(g2d,pageFormat,page,new String());
                        
                        PageFormat pf = (PageFormat) pageFormat.clone();
                        Paper pa = pf.getPaper();
                        pa.setImageableArea(
                                pa.getImageableX(),
                                pa.getImageableY()+20,
                                pa.getImageableWidth(),
                                pa.getImageableHeight()-40
                        );
                        pf.setPaper(pa);

                        // reset the paper
                        //pageFormat.setPaper(originalPaper);
                        return (PAGE_EXISTS);
		}
		else
		{
                        int origPage = page;

                        if(printOptions.printDiagram()==true) page--;

                        if(page>=pageList.size() || printOptions.printCode()==false) return (NO_SUCH_PAGE);
                        else
                        {
                            String mc = pageList.get(page);
                            page--;
                            int p = 0;
                            while(page>=0)
                            {
                                if(pageList.get(page).equals(mc)) p++;
                                page--;
                            }
                            MyClass thisClass = classes.get(mc);
                            
                            CodeEditor edit = new CodeEditor();
                            edit.setFont(new Font(edit.getFont().getName(),Font.PLAIN,printOptions.getFontSize()));
                            
                              String code = "";
                              // get code, depending on JavaDoc filter
                              if(printOptions.printJavaDoc()) code=thisClass.getContent().getText();
                              else code=thisClass.getJavaCodeCommentless();

                              // filter double lines
                              if(printOptions.filterDoubleLines())
                              {
                                StringList sl = StringList.explode(code, "\n");
                                sl.removeDoubleEmptyLines();
                                code=sl.getText();
                              }

                              edit.setCode(code);
                              
                              printHeaderFooter(g,pageFormat,origPage,thisClass.getShortName());
                            
                              PageFormat pf = (PageFormat) pageFormat.clone();
                              Paper pa = pf.getPaper();
                              pa.setImageableArea(
                                      pa.getImageableX(),
                                      pa.getImageableY()+20,
                                      pa.getImageableWidth(),
                                      pa.getImageableHeight()-40
                              );
                              pf.setPaper(pa);
                            
                            edit.print(g, pf, p);
                            edit=null;
                            System.gc();
                            
                            // reset the paper
                            //pageFormat.setPaper(originalPaper);
                            return (PAGE_EXISTS);
                        }
		}
    }

    public boolean loadClassFromFile()
    {
        OpenFile op = new OpenFile(new File(getContainingDirectoryName()),false);
        int result = op.showOpenDialog(frame);
        if(result==OpenProject.APPROVE_OPTION)
        {
            try
            {
                String filename = op.getSelectedFile().getAbsolutePath().toString();
                MyClass mc = new MyClass(filename, Unimozer.FILE_ENCODING);
                mc.setPosition(new Point(0, 0));
                addClass(mc);
                setChanged(true);
                cleanAll();
                return true;
            }
            catch (FileNotFoundException ex)
            {
                MyError.display(ex);
            }
            catch (japa.parser.ParseException ex)
            {
                MyError.display(ex);
            }
            catch (IOException ex)
            {
                MyError.display(ex);
            }
            catch (URISyntaxException ex)
            {
                MyError.display(ex);
            }
            return false;
        } else return false;
  }

    private double getDiagramWidth()
    {
        return mostRight+Package.PADDING;
    }

    private double getDiagramHeight()
    {
        return mostBottom+Package.PADDING;
    }

    void setShowFields(boolean selected)
    {
        showFields=selected;
    }

    void setShowMethods(boolean selected)
    {
        showMethods=selected;
    }

    /**
     * @return the mode
     */
    public int getMode()
    {
        return mode;
    }

    /**
     * @param mode the mode to set
     */
    public void setMode(int mode)
    {
        this.mode = mode;
        if (mode==MODE_EXTENDS) 
        {
            deselectAll();
            mouseClass=null;
            updateEditor();
            repaint();
        }
    }




	// PopupListener Methods
	class PopupListener extends MouseAdapter
	{
                @Override
		public void mousePressed(MouseEvent e)
		{
                        //System.out.println("mousePressed");
                        showPopup(e);
		}

                @Override
		public void mouseReleased(MouseEvent e)
		{
                        //System.out.println("mouseReleased");
			showPopup(e);
		}

    private void fillPopup(JComponent popup, Class c)
    {
        Method m[] = c.getDeclaredMethods();
        boolean found = false;
        for (int i = 0; i < m.length; i++)
        {
            String full = "";
            full+= m[i].getReturnType().getSimpleName();
            Type type = m[i].getGenericReturnType();
            if(type instanceof ParameterizedType)
            {
                full+="<";
                ParameterizedType pt = (ParameterizedType) type;  
                for(int t=0;t<pt.getActualTypeArguments().length;t++)
                {
                    if(t!=0) full+=",";
                    full+=pt.getActualTypeArguments()[t].toString()
                            .replace("class java.lang.","")
                            .replace("class ","");
                }  
                full+=">";
                
            }
            full+=" ";
            full+= m[i].getName();
            full+= "(";
            Type[] tvm = m[i].getParameterTypes();
            for(int t=0;t<tvm.length;t++)
            {
                String sn = tvm[t].toString();
                sn=sn.substring(sn.lastIndexOf('.')+1,sn.length());
                if(sn.startsWith("class")) sn=sn.substring(5).trim();
                // array is shown as ";"  ???
                if(sn.endsWith(";"))
                {
                    sn=sn.substring(0,sn.length()-1)+"[]";
                }
                full+= sn+", ";
            }
            if(tvm.length>0) full=full.substring(0,full.length()-2);
            full+= ")";
            String backup = new String(full);
            //System.out.println("Mod: "+m[i].getModifiers()+" << "+full);
            String complete = new String(Modifier.toString(m[i].getModifiers())+" "+full);
            //System.out.println("Comp: "+complete+" << "+full);

            //if (base==true) System.err.println("Complete: "+complete);

            // get the real full name from the "MyObject" representation
            MyClass mc = diagram.getClass(c.getName());
            if(mc!=null)
            {
                String comp = mc.getCompleteSignatureBySignature(full);
                if(!comp.equals("")) complete = comp;
                full = mc.getFullSignatureBySignature(full);
            }
            if(full.trim().equals("")) full=backup;

            //System.err.println(c.getSimpleName()+" >> "+complete);
            //System.out.println("Comp: "+complete+" << "+full);
            //if((!complete.startsWith("private") || base==true)) // && (!complete.contains("static")))
            if(complete.startsWith("public")==true && complete.contains("static"))
            {
                //System.err.println("Adding: "+complete);
                found=true;
                JMenuItem item = new JMenuItem(full);
                item.addActionListener(diagram);
                item.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_method_static.png")));
                popup.add(item);
            }
        }

        // add separator if there is at leat one static method
        if (found==true)
        {
            JSeparator sep = new JSeparator();
            popup.add(sep);
        }
    }

    private void showPopup(MouseEvent e)
    {
        //System.out.println("showPopup");
        if(diagram.isEnabled())
        {
            //System.out.println("showPopup - en");
	    if (e.isPopupTrigger())
	    {
                //System.out.println("showPopup - trigger");
                mousePoint = e.getPoint();
                MyClass clickClass = getMouseClassNoSelect(mousePoint);
                mouseClass=clickClass;

                // load the clicked class
                loadClickedClass(e);

                /*if(clickClass!=mouseClass)
                {
                    mouseClass=clickClass;
                }*/

                // clear the popup menu
                popup.removeAll();
                // fill with what is needed
                if(clickClass!=null)
                {
                    if(clickClass.isCompiled())
                    {
                        try
                        {
                            // just make sure everthing _is_ right,
                            // so we compile the class (and all subclasses)
                            //mouseClass.compile();
                            // now load the class (and all subclasses)
                            //Class<?> cla = mouseClass.load();
                            Class<?> cla = Runtime5.getInstance().load(clickClass.getFullName());
                            Constructor[] constr = cla.getConstructors();
                            for(int c = 0;c<constr.length;c++)
                            {
                                // get signature
                                /*String full = constr[c].getName();
                                full+="(";
                                Class<?>[] tvm = constr[c].getParameterTypes();
                                for(int t=0;t<tvm.length;t++)
                                {
                                    String sn = tvm[t].toString();
                                    sn=sn.substring(sn.lastIndexOf('.')+1,sn.length());
                                    if(sn.startsWith("class")) sn=sn.substring(5).trim();
                                    // array is shown as ";"  ???
                                    if(sn.endsWith(";"))
                                    {
                                        sn=sn.substring(0,sn.length()-1)+"[]";
                                    }
                                    full+= sn+", ";
                                }
                                if(tvm.length>0) full=full.substring(0,full.length()-2);
                                full+= ")";
                                String backup = new String(full);
                                String complete = new String(full);
                                */
                                String full = objectizer.constToFullString(constr[c]);
                                String backup = objectizer.constToFullString(constr[c]);
                                String complete = objectizer.constToCompleteString(constr[c]);

                                // get full signature
                                full = clickClass.getFullSignatureBySignature(full);
                                complete = clickClass.getCompleteSignatureBySignature(full);
                                if(full.trim().equals("")) full=backup;

                                if(!complete.startsWith("private") && !clickClass.getName().contains("abstract"))
                                {
                                    JMenuItem item = new JMenuItem("new "+full);
                                    item.addActionListener(diagram);
                                    item.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_constructor.png")));
                                    popup.add(item);
                                }
                            }

                            // add separator if there is at least one constructor
                            if(constr.length>0)
                            {
                                JSeparator sep = new JSeparator();
                                popup.add(sep);
                            }

                            // add static methods
                            fillPopup(popup,(Class) cla);
                            
                            JMenuItem item = new JMenuItem("Remove class");
                            item.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/gen_del.png")));
                            item.addActionListener(diagram);
                            popup.add(item);


                        }
                        catch (Exception ex)
                        {
                            MyError.display(ex);
                            repaint();
                            JOptionPane.showMessageDialog(frame, ex.toString(), "Class load error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                        }
                    }
                    else
                    {
                        JMenuItem item = new JMenuItem("Compile");
                        item.addActionListener(diagram);
                        item.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/gen_proc.png")));
                        popup.add(item);

                        JSeparator sep = new JSeparator();
                        popup.add(sep);

                        item = new JMenuItem("Remove class");
                        item.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/gen_del.png")));
                        item.addActionListener(diagram);
                        popup.add(item);
                    }

                    // allowEdit &&
                    if(clickClass.isValidCode())
                    {
                        JSeparator sep = new JSeparator();
                        popup.add(sep);

                        JMenuItem item = new JMenuItem("Add constructor ...");
                        item.addActionListener(
                                   new java.awt.event.ActionListener()
                                   {
                                        @Override
                                        public void actionPerformed(java.awt.event.ActionEvent evt)
                                        {
                                            diagram.addConstructor();
                                        }
                                   }
                        );
                        item.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_constructor.png")));
                        popup.add(item);

                        item = new JMenuItem("Add method ...");
                        item.addActionListener(
                                   new java.awt.event.ActionListener()
                                   {
                                        @Override
                                        public void actionPerformed(java.awt.event.ActionEvent evt)
                                        {
                                            diagram.addMethod();
                                        }
                                   }
                        );
                        item.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_method.png")));
                        popup.add(item);

                        item = new JMenuItem("Add field ...");
                        item.addActionListener(
                                   new java.awt.event.ActionListener()
                                   {
                                        @Override
                                        public void actionPerformed(java.awt.event.ActionEvent evt)
                                        {
                                            diagram.addField();
                                        }
                                   }
                        );
                        item.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_field.png")));
                        popup.add(item);
                    }
                    
                    // in either case
                    JSeparator sep = new JSeparator();
                    popup.add(sep);

                    JMenu menu = new JMenu("Copy diagram as PNG");
                    popup.add(menu);

                    JMenuItem item = new JMenuItem("Uncompiled, nothing selected");
                    item.addActionListener(
                               new java.awt.event.ActionListener()
                               {
                                    public void actionPerformed(java.awt.event.ActionEvent evt)
                                    {
                                        diagram.copyToClipboardPNG(mouseClass,0);
                                    }
                               }
                    );
                    item.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/export_image.png")));
                    menu.add(item);
                    
                    item = new JMenuItem("Uncompiled, selected");
                    item.addActionListener(
                               new java.awt.event.ActionListener()
                               {
                                    public void actionPerformed(java.awt.event.ActionEvent evt)
                                    {
                                        diagram.copyToClipboardPNG(mouseClass,1);
                                    }
                               }
                    );
                    item.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/export_image.png")));
                    menu.add(item);
                    
                    item = new JMenuItem("Compiled");
                    item.addActionListener(
                               new java.awt.event.ActionListener()
                               {
                                    public void actionPerformed(java.awt.event.ActionEvent evt)
                                    {
                                        diagram.copyToClipboardPNG(mouseClass,2);
                                    }
                               }
                    );
                    item.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/export_image.png")));
                    menu.add(item);
                    
                }
                else // click on the empty space
                {
                    JMenuItem item = new JMenuItem("Add class ...");
                    item.addActionListener(
                               new java.awt.event.ActionListener()
                               {
                                    public void actionPerformed(java.awt.event.ActionEvent evt)
                                    {
                                        diagram.addClass();
                                    } 
                               }
                    );
                    item.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_class.png")));
                    popup.add(item);
                }
                popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}
    }

    private StringList getSaveContent()
    {
        StringList content = new StringList();
        content.add((new Boolean(allowEdit)).toString());
        content.add((new Boolean(showHeritage)).toString());
        content.add((new Boolean(showComposition)).toString());
        content.add((new Boolean(showAggregation)).toString());
        content.add((new Boolean(showFields)).toString());
        content.add((new Boolean(showMethods)).toString());
/*
        Set<String> set = classes.keySet();
        Iterator<String> itr = set.iterator();
        while (itr.hasNext())
        {
          String str = itr.next();
*/
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String str = entry.getKey();
            if(interactiveProject==null 
                          || interactiveProject.getStudentClass().getFullName().equals(entry.getKey())
                          || !interactiveProject.getClasses().contains(entry.getKey()))
            {
                  StringList line = new StringList();
                  line.add(str);
                  line.add(String.valueOf(classes.get(str).getPosition().x));
                  line.add(String.valueOf(classes.get(str).getPosition().y));
                  content.add(line.getCommaText());
            }
        }

        return content;
    }

    private StringList getBlueJSaveContent()
    {
        StringList content = new StringList();
        content.add("#BlueJ package file");

        // dependency
        int dependencyCounter = 0;
        // composition
        Hashtable<String,StringList> classUsage = new Hashtable<String,StringList>();
/*
        Set<String> set = classes.keySet();
        Iterator<String> itr = set.iterator();
        while (itr.hasNext())
        {
          String str = itr.next();
*/
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String str = entry.getKey();

          
          MyClass thisClass = classes.get(str);
          Vector<MyClass> useWho = new Vector<MyClass>();
          StringList usesClassNames = new StringList();

          StringList uses = thisClass.getFieldTypes();
          for(int u = 0; u<uses.count(); u++)
          {
            MyClass otherClass = classes.get(uses.get(u));
            if(otherClass!=null) // means this class uses the other ones
            {
                useWho.add(otherClass);
                usesClassNames.add(otherClass.getShortName());

                dependencyCounter++;
                content.add("dependency"+dependencyCounter+".from="+thisClass.getShortName());
                content.add("dependency"+dependencyCounter+".to="+otherClass.getShortName());
                content.add("dependency"+dependencyCounter+".type=UsesDependency");
            }
          }
          thisClass.setUsesMyClass(useWho);
          classUsage.put(thisClass.getShortName(), usesClassNames);
        }
        // usage
/*        
        itr = set.iterator();
        while (itr.hasNext())
        {
          String str = itr.next();*/
          
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String str = entry.getKey();

            
          MyClass thisClass = classes.get(str);
          Vector<MyClass> useWho = new Vector<MyClass>();
          useWho.addAll(thisClass.getUsesMyClass());
          StringList usesClassNames = classUsage.get(thisClass.getShortName());

          StringList foundUsage = thisClass.getUsesWho();
          for(int f=0;f<foundUsage.count();f++)
          {
              String usage = foundUsage.get(f);
              if(!usesClassNames.contains(usage))
              {
                MyClass otherClass = getClass(usage);
                if(otherClass!=null) // menange "otherClass" is a class used by thisClass
                {
                    useWho.add(otherClass);
                    
                    dependencyCounter++;
                    content.add("dependency"+dependencyCounter+".from="+thisClass.getShortName());
                    content.add("dependency"+dependencyCounter+".to="+otherClass.getShortName());
                    content.add("dependency"+dependencyCounter+".type=UsesDependency");
                }
              }
          }
          thisClass.setUsesMyClass(useWho);
       }
       /**/

       content.add("package.editor.height=900");
       content.add("package.editor.width=700");
       content.add("package.editor.x=0");
       content.add("package.editor.y=0");
       content.add("package.numDependencies="+dependencyCounter);
       content.add("package.numTargets="+classes.size());
       content.add("package.showExtends=true");
       content.add("package.showUses=true");

       int count = 0;
       /*itr = set.iterator();
       while (itr.hasNext())
       {
           String str = itr.next();
*/
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String str = entry.getKey();
           
           MyClass thisClass = classes.get(str);
           count++;
           String type = "ClassTarget";
           if (thisClass.getName().contains("abstract")) type = "AbstractTarget";
           content.add("target"+count+".editor.height=700");
           content.add("target"+count+".editor.width=400");
           content.add("target"+count+".editor.x=0");
           content.add("target"+count+".editor.y=0");
           content.add("target"+count+".height=50");
           content.add("target"+count+".name="+thisClass.getShortName());
           content.add("target"+count+".showInterface=false");
           content.add("target"+count+".type="+type);
           content.add("target"+count+".width=80");
           content.add("target"+count+".x="+thisClass.getPosition().x);
           content.add("target"+count+".y="+thisClass.getPosition().y);

       }

       return content;
    }

    private StringList getBlueJSaveContent(Package pack)
    {
        StringList content = new StringList();
        content.add("#BlueJ package file");

        // dependency
        int dependencyCounter = 0;
        // composition
        
        Hashtable<String,StringList> classUsage = new Hashtable<String,StringList>();

/*        Set<String> set = classes.keySet();
        Iterator<String> itr = set.iterator();
        while (itr.hasNext())
        {
          String str = itr.next();
*/
          
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String str = entry.getKey();

            MyClass thisClass = classes.get(str);
          Vector<MyClass> useWho = new Vector<MyClass>();
          StringList usesClassNames = new StringList();

          StringList uses = thisClass.getFieldTypes();
          for(int u = 0; u<uses.count(); u++)
          {
            MyClass otherClass = classes.get(uses.get(u));
            if(otherClass!=null) // means this class uses the other ones
            {
                useWho.add(otherClass);
                usesClassNames.add(otherClass.getShortName());

                dependencyCounter++;
                content.add("dependency"+dependencyCounter+".from="+thisClass.getShortName());
                content.add("dependency"+dependencyCounter+".to="+otherClass.getShortName());
                content.add("dependency"+dependencyCounter+".type=UsesDependency");
            }
          }
          thisClass.setUsesMyClass(useWho);
          classUsage.put(thisClass.getShortName(), usesClassNames);
        }
        // usage
        
        /*itr = set.iterator();
        while (itr.hasNext())
        {
          String str = itr.next();*/

        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String str = entry.getKey();
          
          MyClass thisClass = classes.get(str);
          Vector<MyClass> useWho = new Vector<MyClass>();
          useWho.addAll(thisClass.getUsesMyClass());
          StringList usesClassNames = classUsage.get(thisClass.getShortName());

          StringList foundUsage = thisClass.getUsesWho();
          for(int f=0;f<foundUsage.count();f++)
          {
              String usage = foundUsage.get(f);
              if(!usesClassNames.contains(usage))
              {
                MyClass otherClass = getClass(usage);
                if(otherClass!=null) // menange "otherClass" is a class used by thisClass
                {
                    useWho.add(otherClass);
                    
                    dependencyCounter++;
                    content.add("dependency"+dependencyCounter+".from="+thisClass.getShortName());
                    content.add("dependency"+dependencyCounter+".to="+otherClass.getShortName());
                    content.add("dependency"+dependencyCounter+".type=UsesDependency");
                }
              }
          }
          thisClass.setUsesMyClass(useWho);
       }
       /**/

       content.add("package.editor.height=900");
       content.add("package.editor.width=700");
       content.add("package.editor.x=0");
       content.add("package.editor.y=0");
       content.add("package.numDependencies="+dependencyCounter);
       content.add("package.showExtends=true");
       content.add("package.showUses=true");

       int count = 0;

       /* itr = set.iterator();
       while (itr.hasNext())
       {
           String str = itr.next();*/
           
                   /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String str = entry.getKey();

            
           MyClass thisClass = classes.get(str);
           if(thisClass.getPackagename().equals(pack.getName()))
           {
               count++;
               String type = "ClassTarget";
               if (thisClass.getName().contains("abstract")) type = "AbstractTarget";
               content.add("target"+count+".editor.height=700");
               content.add("target"+count+".editor.width=400");
               content.add("target"+count+".editor.x=0");
               content.add("target"+count+".editor.y=0");
               content.add("target"+count+".height=50");
               content.add("target"+count+".name="+thisClass.getShortName());
               content.add("target"+count+".showInterface=false");
               content.add("target"+count+".type="+type);
               content.add("target"+count+".width=80");
               content.add("target"+count+".x="+thisClass.getPosition().x);
               content.add("target"+count+".y="+thisClass.getPosition().y);
           }
       }
       
       StringList added = new StringList();
       int i = 0;
       for(Package myPack : packages.values())
       {
           //System.out.println("Saving package: "+pack.getName());
           //System.out.println("Is: "+myPack.getName()+" contained in: "+pack.getName()+" ? "+pack.contains(myPack));
           
           String addPack = myPack.getRelativeName(pack);
           if (pack.contains(myPack) && !added.contains(addPack))
           {
               /*
                target2.height=62
                target2.name=lu
                target2.type=PackageTarget
                target2.width=80
                target2.x=200
                target2.y=40
               */
               count++;
               i++;
               content.add("target"+count+".height=80");
               content.add("target"+count+".name="+addPack);
               content.add("target"+count+".type=PackageTarget");
               content.add("target"+count+".width=80");
               content.add("target"+count+".x="+(i*100)+"");
               content.add("target"+count+".y=10");
               added.add(addPack);
           }
       }
       
       content.add("package.numTargets="+(count));

       return content;
    }

    private void saveFiles()
    {
        if(directoryName!=null)
        {
            // delete the "src" directory
            //deleteDirectory(new File(directoryName+ System.getProperty("file.separator")+"src"+System.getProperty("file.separator")));
            // setup a new "src" directory" (for the files used by netbeans!)

            /*
             * Don't delete the src-directory because otherwise some
             * files needed by NetBeans & co could be lost.
             *
             * This implies that the directory could contain somm mess
             * in case packages are changed in Unimozer.
             *
             * I should add something like a watchdog for Java files or
             * maybe detect package changes?
             */

            // delete files marked for deletion
            for(int i=0;i<toBeDeleted.size();i++)
                toBeDeleted.get(i).delete();
            toBeDeleted.clear();

            // clean the structures
            deleteEmptyDirectories(new File(directoryName + System.getProperty("file.separator")));


            File fDir = new File(directoryName + System.getProperty("file.separator")+"bin");
            if(!fDir.exists()) fDir.mkdir();

            fDir = new File(directoryName + System.getProperty("file.separator")+"src");
            if(!fDir.exists()) fDir.mkdir();

            /*Set<String> set = classes.keySet();
            Iterator<String> itr = set.iterator();
            while (itr.hasNext())
            {*/
                
            /* let's try this one ... */
            for(Entry<String,MyClass> entry : classes.entrySet()) 
            {
                // get the actual class ...
                String str = entry.getKey();
                
                //when saving an interactive project, don't save the given files
                if(interactiveProject==null 
                        || interactiveProject.getStudentClass().getFullName().equals(entry.getKey())
                        || !interactiveProject.getClasses().contains(entry.getKey())
                        || !interactiveProject.isBuildIn()
                        )
                {
                    try
                    {
                        //String str = itr.next();
                        //System.out.println("Saving source ... "+classes.get(str).getShortName());
                        String code;
                        if(allowEdit==true) code = classes.get(str).getJavaCode();
                        else code = classes.get(str).getContent().getText();

                        String filename;
                        FileOutputStream fos;
                        Writer out;

                        // write standard file
                        /*
                        if(!classes.get(str).getPackagename().equals(Package.DEFAULT))
                        {
                            String dirNames = directoryName + System.getProperty("file.separator");
                            //dirNames += classes.get(str).getPackagename().replaceAll("\\.",System.getProperty("file.separator"))+System.getProperty("file.separator");
                            dirNames += classes.get(str).getPackagename().replace(".", System.getProperty("file.separator"))+System.getProperty("file.separator");
                            File dirs = new File(dirNames);
                            dirs.mkdirs();

                            filename = dirs.getAbsolutePath() + System.getProperty("file.separator") + classes.get(str).getShortName() + ".java";
                        }
                        else
                            filename = directoryName + System.getProperty("file.separator") + classes.get(str).getShortName() + ".java";


                        fos = new FileOutputStream(filename);
                        out = new OutputStreamWriter(fos, Unimozer.FILE_ENCODING);
                        out.write(code);
                        out.close();
                        */

                        // write file to "src" directory
                        if(!classes.get(str).getPackagename().equals(Package.DEFAULT))
                        {
                            String dirNames = directoryName + System.getProperty("file.separator") + "src" + System.getProperty("file.separator");
                            dirNames += classes.get(str).getPackagename().replace(".", System.getProperty("file.separator"))+System.getProperty("file.separator");
                            File dirs = new File(dirNames);
                            dirs.mkdirs();

                            filename = dirs.getAbsolutePath() + System.getProperty("file.separator") + classes.get(str).getShortName() + ".java";
                        }
                        else
                            filename = directoryName + System.getProperty("file.separator") + "src" + System.getProperty("file.separator") + classes.get(str).getShortName() + ".java";


                        fos = new FileOutputStream(filename);
                        out = new OutputStreamWriter(fos, Unimozer.FILE_ENCODING);
                        out.write(code);
                        out.close();

                    }
                    catch (IOException ex)
                    {
                        System.err.println("Error while saving ...");
                        System.err.println(ex.getMessage());
                    }
                }
            }
            createBackup();
        }
    }

    private void createBackup()
    {
        File fDir = new File(directoryName + System.getProperty("file.separator")+"src");
        if (classes.size()>0)
        {
            // check the versions directory
            String versionDir = directoryName+System.getProperty("file.separator")+"versions";
            File versionFile = new File(versionDir);
            if (!versionFile.exists()) versionFile.mkdir();
            // get the new filename
            SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
            String now = dateFormater.format(new Date());
            String zipFilename = now+".zip";
            zipFilename = directoryName+System.getProperty("file.separator")+"versions"+System.getProperty("file.separator")+zipFilename;
            try
            {
                ZipOutputStream out = new ZipOutputStream(new FileOutputStream(new File(zipFilename)));
                addToZip(out, now+System.getProperty("file.separator"), fDir);
                out.close();

            } catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }


    private void emptyDir()
    {
        File dir = new File(directoryName);
        if (!dir.exists())
        {
          //System.err.println(directoryName + " does not exist");
          return;
        }

        String[] info = dir.list();
        for (int i = 0; i < info.length; i++)
        {
          File n = new File(directoryName + File.separator + info[i]);
          if (!n.isFile()) continue;
          //System.err.println("removing " + n.getPath());
          //if (!n.delete()) System.err.println("Couldn't remove " + n.getPath());
        }
    }

    public void addNewFilesAndReloadExistingSavedFiles()
    {
        File dir = new File(directoryName+System.getProperty("file.separator")+"src");
        
        // first of all, add non existing files
        addDir(dir,false);
        
        // now check if the content of existing files have changed
        //System.out.println("Adding directory: "+dir.getAbsolutePath());
        reloadExistingSavedFiles(dir);
        
    }
    
    public void updateLastModified()
    {
/*
        Set<String> set = classes.keySet();
        Iterator<String> itr = set.iterator();
        while (itr.hasNext())
        {
            String str = itr.next();
*/
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String str = entry.getKey();
            
            MyClass mc = classes.get(str);
            File f = new File(getFullPathSrc(mc));
            mc.setLastModified(f.lastModified());
        }
    }
    
    public void reloadExistingSavedFiles(File dir)
    {
        if(!dir.exists()) return;
        // get all files
        File[] files = dir.listFiles();
        for (int f = 0; f < files.length; f++)
        {
            if (files[f].isDirectory())
            {
                reloadExistingSavedFiles(files[f]);
            }
            else
            {
                if (files[f].getAbsolutePath().toLowerCase().endsWith(".java"))
                {
                    try
                    {
                        MyClass nClass = new MyClass(files[f].getAbsolutePath(), Unimozer.FILE_ENCODING);
                        
                        /*Console.disconnectAll();
                        System.out.println(nClass.getContent().getText());
                        Console.connectAll();*/
                        
                        
                        MyClass eClass = classes.get(nClass.getFullName());
                        
                        if(eClass!=null)
                        
                            if(!eClass.isChanged())
                            {
                                if(!eClass.getContent().getText().equals(nClass.getContent().getText()) &&
                                        eClass.getLastModified()<nClass.getLastModified())
                                {
                                    diagram.loadClassFromString(eClass, nClass.getContent().getText());
                                    diagram.cleanAll();
                                    eClass.setChanged(false);
                                    repaint();
                                    if(eClass==mouseClass) updateEditor();
                                }
                            }
                            else
                            {
                                if(!eClass.getContent().getText().equals(nClass.getContent().getText()) &&
                                        eClass.getLastModified()<nClass.getLastModified())
                                {
                                    // the class changed!
                                    // ask the user to reload it ...
                                    int answer = JOptionPane.showConfirmDialog(this.frame, "The class "+eClass.getFullName()+" has been changed.\nReload it from the disk?", "File changed",JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                                    if(answer==JOptionPane.YES_OPTION)
                                    {
                                        diagram.loadClassFromString(eClass, nClass.getContent().getText());
                                        diagram.cleanAll();
                                        //eClass.setContent(nClass.getContent().copy());
                                        eClass.setChanged(false);
                                        repaint();
                                        if(eClass==mouseClass) updateEditor();
                                    }
                                    else
                                    {
                                        eClass.setLastModified(nClass.getLastModified());
                                    }
                                }
                            }
                        
                    }
                    catch (Exception ex)
                    {
                        //MyError.display(ex);
                        ex.printStackTrace();
                    }
                }
            }
        }
        
    }
    
    boolean save() throws FileNotFoundException, UnsupportedEncodingException, IOException
    {
        if(directoryName!=null)
        {
            // in case some other programm was chaning the project in the background, reload classes 
            // that are net yet contained in the project
            addNewFilesAndReloadExistingSavedFiles();
            
            emptyDir();
            cleanUp(directoryName);
            //System.out.println("Saving package ...");
            // save the "package"
            String filename = directoryName+System.getProperty("file.separator")+Unimozer.U_PACKAGENAME;
            StringList content = getSaveContent();
            content.saveToFile(filename);
            // save netbeans save file
            saveNetBeansProject();
            // save the java files
            saveFiles();
            // save BlueJ Package
            saveBlueJPackages();
            if(interactiveProject!=null)
                interactiveProject.save(directoryName);
            markClassesAsNotChanged();
            updateLastModified();
            /*
            filename = directoryName+System.getProperty("file.separator")+"src"+System.getProperty("file.separator")+Unimozer.B_PACKAGENAME;
            content = getBlueJSaveContent();
            content.saveToFile(filename);
            */
            return true;
        } //else System.out.println("Dirname is null???");
        else return saveWithAskingLocation();
    }
    
   
    
    public void markClassesAsNotChanged()
    {
/*        Set<String> set = classes.keySet();
        Iterator<String> itr = set.iterator();
        while (itr.hasNext())
        {
            String str = itr.next();
*/
            
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String str = entry.getKey();
            MyClass mc = classes.get(str);
            mc.setChanged(false);
        }
    }
    
    private void saveBlueJPackages() throws FileNotFoundException, UnsupportedEncodingException, IOException
    {
        if(directoryName!=null)
        {
            String base = directoryName+System.getProperty("file.separator")+"src";
            for(Package pack : packages.values())
            {
                String filename = base+
                                  System.getProperty("file.separator")+
                                  Unimozer.B_PACKAGENAME;
                if(!pack.getName().equals("<default>"))
                {
                    filename = base+
                               System.getProperty("file.separator")+
                               pack.getName().replace('.', System.getProperty("file.separator").charAt(0))+
                               System.getProperty("file.separator")+
                               Unimozer.B_PACKAGENAME;
                }
                StringList content = getBlueJSaveContent(pack);
                content.saveToFile(filename);
            }
        }
    }

    public void showParseStatus(String ret)
    {
        if(ret.equals(MyClass.NO_SYNTAX_ERRORS))
        {
            status.setBackground(new Color(135,255,135));
            this.setEnabled(true);
        }
        else
        {
            status.setBackground(new Color(255,135,135));
            this.setEnabled(true);
        }
        if(ret.length()>100) status.setText(ret.substring(0, 100).trim());
        else status.setText(ret);
        status.setToolTipText("<html><pre>"+ret+"</pre></html>");
    }

    public String getFullPath(MyClass mc)
    {
        if (directoryName!=null)
        {
            String dirNames = null;

            if(!mc.getPackagename().equals(Package.DEFAULT))
            {
                dirNames = directoryName + System.getProperty("file.separator") + 
                           mc.getPackagename().replace(".", System.getProperty("file.separator"))+System.getProperty("file.separator");
            }
            else
                dirNames = directoryName + System.getProperty("file.separator");

            File dir = new File(dirNames);
            return dir.getAbsolutePath() + System.getProperty("file.separator") + mc.getShortName() + ".java";
        }
        else return null;
    }

    public String getFullPathSrc(MyClass mc)
    {
        if (directoryName!=null)
        {
            String dirNames = null;

            if(!mc.getPackagename().equals(Package.DEFAULT))
            {
                dirNames = directoryName + System.getProperty("file.separator") + "src" + System.getProperty("file.separator")+
                           mc.getPackagename().replace(".", System.getProperty("file.separator"))+System.getProperty("file.separator");
            }
            else
                dirNames = directoryName + System.getProperty("file.separator") + "src" + System.getProperty("file.separator");

            File dir = new File(dirNames);
            return dir.getAbsolutePath() + System.getProperty("file.separator") + mc.getShortName() + ".java";
        }
        else return null;
    }

    public void loadClassFromString(MyClass mc, String code)
    {
        // get the old package name
        String oldFullName    = getFullPath(mc);
        String oldFullNameSrc = getFullPathSrc(mc);

        // remove the class from the hashtable
        classes.remove(mc.getFullName());
        // reload it from the new code
        //System.err.println(code);
        String ret = mc.loadFromString(code);
        // add it back to the hashtable
        classes.put(mc.getFullName(), mc);

        // get the new package name
        String newFullNameSrc = getFullPathSrc(mc);

        if (oldFullNameSrc!=null)
        if (!oldFullNameSrc.equals(newFullNameSrc))
        {
            // mark the old file to be delete upon the next saving
            toBeDeleted.add(new File(oldFullName));
            toBeDeleted.add(new File(oldFullNameSrc));
        }

        showParseStatus(ret);
    }

    private void doCompilationSilent() throws ClassNotFoundException
    {
        doCompilationOnlyMark(false);
    }

    private void doCompilationOnly() throws ClassNotFoundException
    {
        //System.out.println("TZ: doCompilationOnly");
        doCompilationOnlyMark(true);
    }

    private void doCompilationOnlyMark(boolean mark) throws ClassNotFoundException
    {
        if(!classes.isEmpty())
        {
            // get the code for all classes
            Hashtable<String,String> codes = new Hashtable<String,String>();
            /*Set<String> set = classes.keySet();
            Iterator<String> itr = set.iterator();
            while (itr.hasNext())
            {
              String str = itr.next();
*/
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
              // get the actual class ...
              String str = entry.getKey();
              MyClass mc = classes.get(str);

              //mc.getCodePositions(); // just to test it

              if(allowEdit)
              {
                  //System.err.println("---[ Using tree :\n"+mc.getJavaCode());
                  codes.put(mc.getFullName(), mc.getJavaCode());
              }
              else 
              {
                  //System.err.println("---[ Using content:\n"+mc.getContent().getText());
                  codes.put(mc.getFullName(), mc.getContent().getText());
              }
              /*Console.disconnectAll();
              System.out.println("Adding class: "+mc.getFullName());
              Console.connectAll();/**/
            }

            Console.cls();

            if(directoryName!=null)
            {
                try
                {
                    /*Console.disconnectAll();
                    System.out.println("Setting dir: "+mc.getFullName());
                    Console.connectAll();*/
                    Runtime5.getInstance().executeCommand("System.setProperty(\"user.dir\",\"" + directoryName + "\")");
                    Runtime5.getInstance().setRootDirectory(directoryName);
                }
                catch (EvalError ex)
                {
                    System.err.println(ex.getMessage());
                }
            }
            
            Runtime5.getInstance().compileAndLoad(codes,getLibPath());

            // set compiled flag
            if(mark)
            {
                /*set = classes.keySet();
                itr = set.iterator();
                while (itr.hasNext())
                {
                  String str = itr.next();*/

        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String str = entry.getKey();
                  
                  MyClass mc = classes.get(str);
                  mc.setCompiled(true);
                }

                repaint();
            }
        }
    }

    public void compileSilent()
    {
        try
        {
            doCompilationSilent();
        }
        catch (Exception ex)
        {
        }
    }

    public boolean compile()
    {
        // stop all running threads
        if(objectizer!=null)
            objectizer.stopAllThreads();
        this.cleanAll();

        try
        {
            //System.out.println("TZ: compile");
            doCompilationOnly();
            if(frame!=null)
                frame.setCompilationErrors(new Vector<CompilationError>());
            return true;
        }
        catch (Exception ex)
        {
            //ex.printStackTrace();
            String error = ex.toString();
            error=error.replaceAll("java\\.lang\\.ClassNotFoundException:", "");

            // split up the error messags
            Vector<CompilationError> errors = new Vector<CompilationError>();
            
            
            Console.disconnectAll();
            System.out.println(error);
             // --> We could display more detailed information!!
            Console.connectAll();
            
            
            StringList sl = StringList.explode(error.trim(),"\n");
            for(int i=0;i<sl.count();i++)
            {
                // we only need each third line <-- why??
                //if(i%3==0)
                //{

                    StringList msl = StringList.explode(sl.get(i).trim(), "@");
                    try
                    {
                        errors.add(new CompilationError(
                                    msl.get(0).trim(),
                                    Integer.valueOf(msl.get(1).trim()),
                                    msl.get(2).trim()
                                ));
                    }
                    catch(Exception exi)
                    {
                        MyError.display(msl.getText());
                    }
                //}
            }
            if(frame!=null)
                frame.setCompilationErrors(errors);
            return false;

            //JOptionPane.showMessageDialog(frame, error.trim(), "Compilation error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
        }
    }

    public void make()
    {
        makeInteractive(true,null,true);
        // now recompile everthing as well!
        compile();
    }

    public void makeSilent()
    {
        compileSilent();
        makeInteractive(false,null,false,false);
    }

    public boolean makeInteractive(boolean showMessages, String target, boolean compile)
    {
        return makeInteractive(showMessages,target,compile,true);
    }

    public boolean makeInteractive(boolean showMessages, String target, boolean compile, boolean clean)
    {

        try
        {
            // first we need to compile all classes
            // to make sure there are no more errors

            if(compile) doCompilationOnly();

            // repaint the diagram
            repaint();

            // for now, we need to save the project
            if (save())
            {

                // next we can do and create the class files

                // create emtpy array
                File[] files = new File[classes.size()];

                // delete the directory
                if(clean)
                    deleteDirectory(new File(directoryName+ System.getProperty("file.separator")+"bin"+System.getProperty("file.separator")));

                // control dirname
                String dirname = getDirectoryName();
                if (!dirname.endsWith(System.getProperty("file.separator")))
                {
                    dirname += System.getProperty("file.separator");
                }
                // make the "bin" directory
                File binDir = new File(dirname+"bin"+System.getProperty("file.separator"));
                binDir.mkdir();
                int i = 0;
                // add the files to the array
/*                Set<String> set = classes.keySet();
                Iterator<String> itr = set.iterator();
                while (itr.hasNext())
                {
                    String classname = itr.next();*/
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String classname = entry.getKey();
                    files[i] = new File(dirname + "src"+System.getProperty("file.separator")+classname.replaceAll("\\.",System.getProperty("file.separator")) + ".java");
                    i++;
                }
                try
                {
                    Runtime5.getInstance().compileToPath(files, dirname+"bin"+System.getProperty("file.separator"), target, getLibs().getText());
                    if(showMessages) JOptionPane.showMessageDialog(frame, "All CLASS-files have been generated ...", "Success", JOptionPane.INFORMATION_MESSAGE,Unimozer.IMG_INFO);
                    return true;
                }
                catch (IOException ex)
                {
                    if(showMessages) JOptionPane.showMessageDialog(frame, "There was an error during the make process ...", "Compilation error :: IOException", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                    return false;
                }
                catch (ScanException ex)
                {
                    if(showMessages) JOptionPane.showMessageDialog(frame, "There was an error during the make process ...", "Compilation error :: ScanException", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                    return false;
                }
                catch (ParseException ex)
                {
                    if(showMessages) JOptionPane.showMessageDialog(frame, "There was an error during the make process ...", "Compilation error :: ParseException", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                    return false;
                }
                catch (CompileException ex)
                {
                    if(showMessages) JOptionPane.showMessageDialog(frame, "There was an error during the make process ...", "Compilation error :: CompileException", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                    return false;
                }
                catch (ClassNotFoundException ex)
                {
                    if(showMessages) JOptionPane.showMessageDialog(frame, "There was an error during the make process ...", "Compilation error :: ClassNotFoundException", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                    return false;
                }
            }
            else return false;
        }
        catch (Exception ex)
        {
            if(showMessages)
            {
                JOptionPane.showMessageDialog(frame, "The creation of the CLASS-files failed because\nyour project contains some errors.\nPlease correct them and try again ...", "Compilation error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);

                String error = ex.toString();
                error=error.replaceAll("java\\.lang\\.ClassNotFoundException:", "");
                JOptionPane.showMessageDialog(frame, error.trim(), "Compilation error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
            }
            return false;
        }
    }

    public void run()
    {
        try
        {
            // compile all
            //doCompilationOnly(null);
            //frame.setCompilationErrors(new Vector<CompilationError>());
            if(compile())
            {

            Vector<String> mains = getMains();
            String runnable = null;
            if(mains.size()==0)
            {
                JOptionPane.showMessageDialog(
                    frame,
                    "Sorry, but your project does not contain any runnable public class ...",
                    "Error",
                    JOptionPane.ERROR_MESSAGE,
                    Unimozer.IMG_ERROR
                );
            }
            else 
            {

                if(mains.size()==1)
                {
                    runnable = mains.get(0);
                }
                else
                {
                    String[] classNames = new String[mains.size()];
                    for(int c=0;c<mains.size();c++) classNames[c]=mains.get(c);
                    runnable = (String) JOptionPane.showInputDialog(
                                       frame,
                                       "Unimozer detected more than one runnable class.\n"
                                       +"Please select which one you want to run.",
                                       "Run",
                                       JOptionPane.QUESTION_MESSAGE,
                                       Unimozer.IMG_QUESTION,
                                       classNames,
                                       "");
                }

                // we know now what to run
                MyClass runnClass = classes.get(runnable);

                // set full signature
                String fullSign = "void main(String[] args)";
                if(runnClass.hasMain2()) fullSign = "void main(String args[])";

                // get signature
                String sign = runnClass.getSignatureByFullSignature(fullSign);
                String complete = runnClass.getCompleteSignatureBySignature(sign);
                
                if((runnClass.hasMain2()) && (sign.equals("void main(String)")))
                {
                    sign="void main(String[])";
                }

                //System.out.println("Calling method (full): "+fullSign);
                //System.out.println("Calling method       : "+sign);

                // find method
                Class c = Runtime5.getInstance().load(runnClass.getFullName());
                Method m[] = c.getMethods();
                for (int i = 0; i < m.length; i++)
                {
                    String full = "";
                    full+= m[i].getReturnType().getSimpleName();
                    full+=" ";
                    full+= m[i].getName();
                    full+= "(";
                    Class<?>[] tvm = m[i].getParameterTypes();
                    LinkedHashMap<String,String> genericInputs = new LinkedHashMap<String,String>();
                    for(int t=0;t<tvm.length;t++)
                    {
                        String sn = tvm[t].toString();
                        genericInputs.put("param"+t,sn);
                        sn=sn.substring(sn.lastIndexOf('.')+1,sn.length());
                        if(sn.startsWith("class")) sn=sn.substring(5).trim();
                        // array is shown as ";"  ???
                        if(sn.endsWith(";"))
                        {
                            sn=sn.substring(0,sn.length()-1)+"[]";
                        }
                        full+= sn+", ";
                    }
                    if(tvm.length>0) full=full.substring(0,full.length()-2);
                    full+= ")";

                    //if((full.equals(sign) || full.equals(fullSign)))
                    //    System.out.println("Found: "+full);

                    if((full.equals(sign) || full.equals(fullSign)))
                    {
                        LinkedHashMap<String,String> inputs = runnClass.getInputsBySignature(sign);
                        //System.out.println(inputs);
                        //System.out.println(genericInputs);
                        if(inputs.size()!=genericInputs.size())
                        {
                            inputs=genericInputs;
                        }
                        MethodInputs mi = null;
                        boolean go = true;
                        if(inputs.size()>0)
                        {
                            mi = new MethodInputs(frame,inputs,full,runnClass.getJavaDocBySignature(sign));
                            go = mi.OK;
                        }
                        if(go==true)
                        {
                            try
                            {
                                String method = runnClass.getFullName()+"."+m[i].getName()+"(";
                                if(inputs.size()>0)
                                {
                                    Object[] keys = inputs.keySet().toArray();
                                    //int cc = 0;
                                    for(int in=0;in<keys.length;in++)
                                    {
                                        String name = (String) keys[in];
                                        String val = mi.getValueFor(name);
                                        if (val.equals("")) val="null";
                                        else if(!val.startsWith("new String[]"))
                                        {
                                            String[] pieces = val.split("\\s+");
                                            String inp = "";
                                            for(int iin=0; iin<pieces.length; iin++)
                                            {
                                                if(inp.equals("")) inp=pieces[iin];
                                                else inp+="\",\""+pieces[iin].replace("\"", "\\\"");
                                            }
                                            val = "new String[] {\""+inp+"\"}";
                                        }
                                        
                                        method+=val+",";
                                    }
                                    method=method.substring(0, method.length()-1);
                                }
                                method+=")";
                                
                                

                                // Invoke method in a new thread
                                final String myMeth = method;
                                Console.disconnectAll();
                                System.out.println("Running now: "+myMeth);
                                Console.connectAll();
                                Runnable r = new Runnable()
                                {
                                        public void run()
                                        {
                                            Console.cls();
                                            try
                                            {
                                                //Console.disconnectAll();
                                                //System.out.println(myMeth);
                                                Object retobj = Runtime5.getInstance().executeMethod(myMeth);
                                                if(retobj!=null) JOptionPane.showMessageDialog(frame, retobj.toString(), "Result", JOptionPane.INFORMATION_MESSAGE,Unimozer.IMG_INFO);
                                            }
                                            catch (EvalError ex)
                                            {
                                                JOptionPane.showMessageDialog(frame, ex.toString(), "Execution error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                                                MyError.display(ex);
                                            }
                                        }
                                };
                                Thread t = new Thread(r);
                                t.start();

                                //System.out.println(method);
                                //Object retobj = Runtime5.getInstance().executeMethod(method);
                                //if(retobj!=null) JOptionPane.showMessageDialog(frame, retobj.toString(), "Result", JOptionPane.INFORMATION_MESSAGE,Unimozer.IMG_INFO);
                            }
                            catch (Throwable ex)
                            {
                                JOptionPane.showMessageDialog(frame, ex.toString(), "Execution error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                                MyError.display(ex);
                            }
                        }
                    }
                }
            }
        }
        }
        catch (ClassNotFoundException ex)
        {
            JOptionPane.showMessageDialog(frame, "There was an error while running your project ...", "Error :: ClassNotFoundException", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
        }
    }

    public void runFast()
    {
        //System.out.println("TZ: runFast");
        try
        {
            // compile all
            //doCompilationOnly(null);
            //frame.setCompilationErrors(new Vector<CompilationError>());
            //Console.disconnectAll();
            //Console.disconnectErr();
            if(compile())
            {
                

            Vector<String> mains = getMains();
            String runnable = null;
            if(mains.size()==0)
            {
                JOptionPane.showMessageDialog(
                    frame,
                    "Sorry, but your project does not contain any runnable public class ...",
                    "Error",
                    JOptionPane.ERROR_MESSAGE,
                    Unimozer.IMG_ERROR
                );
            }
            else 
            {

                if(mains.size()==1)
                {
                    runnable = mains.get(0);
                }
                else
                {
                    String[] classNames = new String[mains.size()];
                    for(int c=0;c<mains.size();c++) classNames[c]=mains.get(c);
                    runnable = (String) JOptionPane.showInputDialog(
                                       frame,
                                       "Unimozer detected more than one runnable class.\n"
                                       +"Please select which one you want to run.",
                                       "Run",
                                       JOptionPane.QUESTION_MESSAGE,
                                       Unimozer.IMG_QUESTION,
                                       classNames,
                                       "");
                }

                // we know now what to run
                MyClass runnClass = classes.get(runnable);

                // set full signature
                String fullSign = "void main(String[] args)";
                if(runnClass.hasMain2()) fullSign = "void main(String args[])";

                // get signature
                String sign = runnClass.getSignatureByFullSignature(fullSign);
                String complete = runnClass.getCompleteSignatureBySignature(sign);
                
                if((runnClass.hasMain2()) && (sign.equals("void main(String)")))
                {
                    sign="void main(String[])";
                }

                //System.out.println("Calling method (full): "+fullSign);
                //System.out.println("Calling method       : "+sign);

                // find method
                System.out.println("TZ: compile");
                Class c = Runtime5.getInstance().load(runnClass.getFullName());
                Method m[] = c.getMethods();
                for (int i = 0; i < m.length; i++)
                {
                    String full = "";
                    full+= m[i].getReturnType().getSimpleName();
                    full+=" ";
                    full+= m[i].getName();
                    full+= "(";
                    Class<?>[] tvm = m[i].getParameterTypes();
                    LinkedHashMap<String,String> genericInputs = new LinkedHashMap<String,String>();
                    for(int t=0;t<tvm.length;t++)
                    {
                        String sn = tvm[t].toString();
                        genericInputs.put("param"+t,sn);
                        sn=sn.substring(sn.lastIndexOf('.')+1,sn.length());
                        if(sn.startsWith("class")) sn=sn.substring(5).trim();
                        // array is shown as ";"  ???
                        if(sn.endsWith(";"))
                        {
                            sn=sn.substring(0,sn.length()-1)+"[]";
                        }
                        full+= sn+", ";
                    }
                    if(tvm.length>0) full=full.substring(0,full.length()-2);
                    full+= ")";

                    //if((full.equals(sign) || full.equals(fullSign)))
                    //    System.out.println("Found: "+full);

                    if((full.equals(sign) || full.equals(fullSign)))
                    {
                        try
                        {
                            String method = runnClass.getFullName()+"."+m[i].getName()+"(null)";



                            // Invoke method in a new thread
                            final String myMeth = method;
                            //System.out.println("Running now: "+myMeth);
                            Runnable r = new Runnable()
                            {
                                    public void run()
                                    {
                                        Console.cls();
                                        try
                                        {
                                            Console.disconnectAll();
                                            System.out.println("Running now: "+myMeth);
                                            System.out.println(Runtime5.getInstance().toString());
                                            Console.connectAll();
                                            Object retobj = Runtime5.getInstance().executeMethod(myMeth);
                                                                                        if(retobj!=null) JOptionPane.showMessageDialog(frame, retobj.toString(), "Result", JOptionPane.INFORMATION_MESSAGE,Unimozer.IMG_INFO);
                                        }
                                        catch (EvalError ex)
                                        {
                                            JOptionPane.showMessageDialog(frame, ex.toString(), "Execution error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                                            MyError.display(ex);
                                        }
                                    }
                            };
                            Thread t = new Thread(r);
                            t.start();

                            //System.out.println(method);
                            //Object retobj = Runtime5.getInstance().executeMethod(method);
                            //if(retobj!=null) JOptionPane.showMessageDialog(frame, retobj.toString(), "Result", JOptionPane.INFORMATION_MESSAGE,Unimozer.IMG_INFO);
                        }
                        catch (Throwable ex)
                        {
                            JOptionPane.showMessageDialog(frame, ex.toString(), "Execution error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                            MyError.display(ex);
                        }
                        
                    }
                }
            }
        }
        }
        catch (ClassNotFoundException ex)
        {
            JOptionPane.showMessageDialog(frame, "There was an error while running your project ...", "Error :: ClassNotFoundException", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
        }
    }
    
    public void saveNetBeansProject() throws FileNotFoundException, UnsupportedEncodingException, IOException
    {
        // adjust the dirname
        String dir = getDirectoryName();
        if (!dir.endsWith(System.getProperty("file.separator")))
        {
            dir += System.getProperty("file.separator");
        }

        // adjust the filename
        String name = getDirectoryName();
        if (name.endsWith(System.getProperty("file.separator")))
        {
            name = name.substring(0, name.length() - 1);
        }
        name = name.substring(name.lastIndexOf(System.getProperty("file.separator"))+1);

        // create the directory
        File fDir = new File(dir+"nbproject"+System.getProperty("file.separator"));
        fDir.mkdir();

        // variables we need
        String filename;
        StringList content;

        // save the the file "project.xml"
        filename = fDir.getAbsolutePath()+System.getProperty("file.separator")+"project.xml";
        content = new StringList();
        content.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        content.add("<project xmlns=\"http://www.netbeans.org/ns/project/1\">");
        content.add("   <type>org.netbeans.modules.java.j2seproject</type>");
        content.add("   <configuration>");
        content.add("       <data xmlns=\"http://www.netbeans.org/ns/j2se-project/3\">");
        content.add("           <name>"+name+"</name>");
        content.add("           <minimum-ant-version>1.6.5</minimum-ant-version>");
        content.add("           <source-roots>");
        content.add("               <root id=\"src.dir\"/>");
        content.add("           </source-roots>");
        content.add("           <test-roots>");
        content.add("           </test-roots>");
        content.add("       </data>");
        content.add("   </configuration>");
        content.add("</project>");
        content.saveToFile(filename); 

        // save the the file "project.properties"
        filename = fDir.getAbsolutePath()+System.getProperty("file.separator")+"project.properties";
        content = new StringList();
        content.add("build.classes.dir=${build.dir}/classes");
        content.add("build.classes.excludes=**/*.java,**/*.form");
        content.add("build.dir=build");
        content.add("build.generated.dir=${build.dir}/generated");
        content.add("build.generated.sources.dir=${build.dir}/generated-sources");
        content.add("build.sysclasspath=ignore");
        content.add("build.test.classes.dir=${build.dir}/test/classes");
        content.add("build.test.results.dir=${build.dir}/test/results");
        content.add("debug.classpath=\\");
        content.add("   ${run.classpath}");
        content.add("debug.test.classpath=\\");
        content.add("   ${run.test.classpath}");
        content.add("dist.dir=dist");
        content.add("dist.jar=${dist.dir}/"+name+".jar");
        content.add("dist.javadoc.dir=${dist.dir}/doc");
        content.add("excludes=bin");
        content.add("includes=**");
        content.add("jar.compress=true");
        content.add("javac.classpath=\\");
        content.add("   ${libs.swing-layout.classpath}");
        content.add("javac.compilerargs=");
        content.add("javac.deprecation=false");
        content.add("javac.source=1.7");
        content.add("javac.target=1.7");
        content.add("javadoc.additionalparam=");
        content.add("javadoc.author=true");
        content.add("javadoc.encoding=${source.encoding}");
        content.add("javadoc.noindex=false");
        content.add("javadoc.nonavbar=false");
        content.add("javadoc.notree=false");
        content.add("javadoc.private=true");
        content.add("javadoc.splitindex=true");
        content.add("javadoc.use=true");
        content.add("javadoc.version=true");
        content.add("javadoc.windowtitle=");
        Vector<String> mains = getMains();
        if(mains.size()>0) content.add("main.class="+mains.get(0));
        else content.add("main.class=");
        content.add("manifest.file=manifest.mf");
        content.add("meta.inf.dir=${src.dir}/META-INF");
        content.add("platform.active=default_platform");
        content.add("run.classpath=\\");
        content.add("    ${javac.classpath}:\\");
        content.add("   ${build.classes.dir}");
        content.add("run.jvmargs=");
        content.add("   run.test.classpath=\\");
        content.add("   ${javac.test.classpath}:\\");
        content.add("   ${build.test.classes.dir}");
        content.add("source.encoding=UTF-8");
        content.add("src.dir=src");
        content.add("test.src.dir=test");

        content.saveToFile(filename);

    }

    public void jar()
    {
        try
        {
            // compile all
            if(compile())
            if(save())
            {

                // adjust the dirname
                String dir = getDirectoryName();
                if (!dir.endsWith(System.getProperty("file.separator")))
                {
                    dir += System.getProperty("file.separator");
                }

                // adjust the filename
                String name = getDirectoryName();
                if (name.endsWith(System.getProperty("file.separator")))
                {
                    name = name.substring(0, name.length() - 1);
                }
                name = name.substring(name.lastIndexOf(System.getProperty("file.separator"))+1);

                /*String[] classNames = new String[classes.size()+1];
                Set<String> set = classes.keySet();
                Iterator<String> itr = set.iterator();
                classNames[0]=null;
                int c = 1;
                while (itr.hasNext())
                {
                    classNames[c]=itr.next();
                    c++;
                }/**/
                Vector<String> mains = getMains();
                String[] classNames = new String[mains.size()];
                for(int c=0;c<mains.size();c++) classNames[c]=mains.get(c);
                // default class to launch
                String mc = "";
                {
                    if(classNames.length==0) 
                    {
                        mc="";
                        JOptionPane.showMessageDialog(printOptions,
                                                      "Unimozer was unable to detect a startable class\n"+
                                                      "inside your project. The JAR-archive will be created\n"+
                                                      "but it won't be executable!",
                                                      "Mainclass",
                                                      JOptionPane.INFORMATION_MESSAGE,
                                                      Unimozer.IMG_INFO);
                    }
                    else if(classNames.length==1) mc=classNames[0];
                    else mc= (String) JOptionPane.showInputDialog(
                                       frame,
                                       "Unimozer detected more than one runnable class.\n"
                                       +"Please select which one you want to be launched\n"
                                       +"automatically with the JAR-archive.",
                                       "Autostart",
                                       JOptionPane.QUESTION_MESSAGE,
                                       Unimozer.IMG_QUESTION,
                                       classNames,
                                       "");
                }
                // target JVM
                String target = null;
                if(Runtime5.getInstance().usesSunJDK() && mc!=null)
                {
                    
                       
                    String[] targets = new String[]{"1.1","1.2","1.3","1.5","1.6"};
                    if(System.getProperty("java.version").startsWith("1.7"))
                        targets = new String[]{"1.1","1.2","1.3","1.5","1.6","1.7"};
                    if(System.getProperty("java.version").startsWith("1.8"))
                        targets = new String[]{"1.1","1.2","1.3","1.5","1.6","1.7","1.8"};
                    
                    target= (String) JOptionPane.showInputDialog(
                                       frame,
                                       "Please enter version of the JVM you want to target.",
                                       "Target JVM",
                                       JOptionPane.QUESTION_MESSAGE,
                                       Unimozer.IMG_QUESTION,
                                       targets,
                                       "1.6");
                }
                // make the class-files and all
                // related stuff
                if (
                        (
                            (Runtime5.getInstance().usesSunJDK() && target!=null)
                            ||
                            (!Runtime5.getInstance().usesSunJDK())
                        )
                        &&
                        (mc!=null)
                   )
                if (makeInteractive(false,target,false)==true)
                {

                    StringList manifest = new StringList();
                    manifest.add("Manifest-Version: 1.0");
                    manifest.add("Created-By: "+Unimozer.E_VERSION+" "+Unimozer.E_VERSION);
                    manifest.add("Name: "+name);
                    if(mc!=null)
                    {
                        manifest.add("Main-Class: "+mc);
                    }

                    // compose the filename
                    File fDir = new File(dir+"dist"+System.getProperty("file.separator"));
                    fDir.mkdir();
                    name = dir + "dist"+System.getProperty("file.separator") + name + ".jar";
                    String baseName = dir;
                    String libFolderName = dir + "lib";
                    String distLibFolderName = dir + "dist" + System.getProperty("file.separator") + "lib";

                    File outFile = new File(name);
                    FileOutputStream bo = new FileOutputStream(name);
                    JarOutputStream jo = new JarOutputStream(bo);

                    String dirname = getDirectoryName();
                    if (!dirname.endsWith(System.getProperty("file.separator")))
                    {
                        dirname += System.getProperty("file.separator");
                    }
                    // add the files to the array
                    addToJar(jo,"",new File(dirname+"bin"+System.getProperty("file.separator")));
                    // add the files to the array
                    addToJar(jo,"",new File(dirname+"src"+System.getProperty("file.separator")),new String[]{"java"});
                    /*
                    // define a filter for files that do not start with a dot
                    FilenameFilter filter = new FilenameFilter() { public boolean accept(File dir, String name) { return !name.startsWith("."); } };                     
                    // get the bin directory
                    File binDir = new File(dirname+"bin"+System.getProperty("file.separator")); 
                    // get all files
                    File[] files = binDir.listFiles(filter);
                    for(int f=0;f<files.length;f++)
                    {
                        FileInputStream bi = new FileInputStream(files[f]);
                        String entry = files[f].getAbsolutePath();
                        entry = entry.substring(binDir.getAbsolutePath().length()+1);
                        JarEntry je = new JarEntry(entry);
                        jo.putNextEntry(je);
                        byte[] buf = new byte[1024];
                        int anz;
                        while ((anz = bi.read(buf)) != -1)
                        {
                            jo.write(buf, 0, anz);
                        }
                        bi.close();
                    }
                     */

                    // ask to include another direectory
                    // directory filter
                    /*
                    FilenameFilter dirFilter = new FilenameFilter() { public boolean accept(File dir, String name) { File isDir = new File(dir+System.getProperty("file.separator")+name); return isDir.isDirectory() && !name.equals("bin") && !name.equals("src") && !name.equals("dist") && !name.equals("nbproject") && !name.equals("doc"); } };
                    // get directories
                    File projectDir = new File(dirname);
                    String[] subDirs = projectDir.list(dirFilter);
                    if(subDirs.length>0)
                    {
                        String subdir = (String) JOptionPane.showInputDialog(
                                       frame,
                                       "Do you want to include any other resources directory?\n"+
                                       "Click ”Cancel” to not include any resources directory!",
                                       "JAR Packager",
                                       JOptionPane.QUESTION_MESSAGE,
                                       Unimozer.IMG_QUESTION,
                                       subDirs,
                                       null);
                        if(subdir!=null)
                        {
                            addToJar(jo,subdir+"/",new File(dirname+subdir+System.getProperty("file.separator")));
                        }
                    }
                     */
                    
                    /*
                    Set<String> set = classes.keySet();
                    Iterator<String> itr = set.iterator();
                    int i = 0;
                    while (itr.hasNext())
                    {
                        String classname = itr.next();
                        String act = classname + ".class";
                        FileInputStream bi = new FileInputStream(dirname+"bin"+System.getProperty("file.separator")+act);
                        JarEntry je = new JarEntry(act);
                        jo.putNextEntry(je);
                        byte[] buf = new byte[1024];
                        int anz;
                        while ((anz = bi.read(buf)) != -1)
                        {
                            jo.write(buf, 0, anz);
                        }
                        bi.close();
                    }
                     */
                    
                    // copy libs
                    File lib = new File(libFolderName);
                    File distLib = new File(distLibFolderName);
                    StringList libs = null;
                    if(lib.exists())
                    {
                        libs = CopyDirectory.copyFolder(lib, distLib);
                    }
                    String cp = new String();
                    if(libs!=null)
                    {
                        for(int i=0;i<libs.count();i++)
                        {
                            String myLib = libs.get(i);
                            myLib = myLib.substring(baseName.length());
                            if(i!=0) cp=cp+" ";
                            cp=cp+myLib;
                        }
                        //manifest.add("Class-Path: "+cp);
                    }
                    
                    
                    // Let's search for the path of the swing-layout JAR file
                    String cpsw = "";
                    if(getCompleteSourceCode().contains("org.jdesktop.layout"))
                    {
                        if(Main.classpath!=null)
                        {
                            // copy the file
                            String src = Main.classpath;
                            File f1 = new File(src);
                            String dest = distLibFolderName+System.getProperty("file.separator")+f1.getName();
                            // create folder if not exists
                            File f2 = new File(distLibFolderName);
                            if (!f2.exists()) f2.mkdir();
                            // copy the file
                            InputStream in = new FileInputStream(src);
                            OutputStream out = new FileOutputStream(dest); 
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = in.read(buffer)) > 0){
                               out.write(buffer, 0, length);
                            }
                            in.close();
                            out.close();
                            // add the manifest entry
                            cpsw="lib"+System.getProperty("file.separator")+f1.getName();
                        }
                    }
                    
                    manifest.add("Class-Path: "+cp+" "+cpsw);

                    // adding the manifest file
                    manifest.add("");
                    JarEntry je = new JarEntry("META-INF/MANIFEST.MF");
                    jo.putNextEntry(je);
                    String mf = manifest.getText();
                    jo.write(mf.getBytes(), 0, mf.getBytes().length);

                    jo.close();
                    bo.close();

                    cleanAll();
                    

                    JOptionPane.showMessageDialog(frame, "The JAR-archive has been generated ...", "Success", JOptionPane.INFORMATION_MESSAGE,Unimozer.IMG_INFO);
                 }
            }
        }
        /*catch (ClassNotFoundException ex)
        {
            JOptionPane.showMessageDialog(frame, "There was an error while creating the JAR-archive ...", "Error :: ClassNotFoundException", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
        }*/
        catch (IOException ex)
        {
            JOptionPane.showMessageDialog(frame, "There was an error while creating the JAR-archive ...", "Error :: IOException", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
        }
    }
    
    private String getCompleteSourceCode()
    {
        String source = new String();
/*
        Set<String> set = classes.keySet();
        Iterator<String> itr = set.iterator();
        while (itr.hasNext())
        {
            String str = itr.next();
*/
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String str = entry.getKey();
            MyClass mc = classes.get(str);
            String classcode = mc.getJavaCode();
            source+=classcode;
        }
        
        /*Console.disconnectAll();
        System.out.println(source);
        Console.connectAll();*/
        
        return source;
    }

    private void addToJar(JarOutputStream jo, String baseDir, File directory) throws FileNotFoundException, IOException
    {
        addToJar( jo, baseDir, directory, new String[]{});
    }

    private String getExtension(File f)
    {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (f.isDirectory())
        	ext = null;
        else if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }


    private void addToJar(JarOutputStream jo, String baseDir, File directory, String[] excludeExtention) throws FileNotFoundException, IOException
    {
        // get all files
        File[] files = directory.listFiles();
        for(int f=0;f<files.length;f++)
        {
            if(files[f].isDirectory())
            {
                String entry = files[f].getAbsolutePath();
                entry = entry.substring(directory.getAbsolutePath().length()+1);
                addToJar(jo,baseDir+entry+"/",files[f],excludeExtention);
            }
            else
            {
                //System.out.println("File = "+files[f].getAbsolutePath());
                //System.out.println("List = "+Arrays.deepToString(excludeExtention));
                //System.out.println("We got = "+getExtension(files[f]));
                if(!Arrays.asList(excludeExtention).contains(getExtension(files[f])))
                {

                    FileInputStream bi = new FileInputStream(files[f]);

                    String entry = files[f].getAbsolutePath();
                    entry = entry.substring(directory.getAbsolutePath().length()+1);
                    entry = baseDir+entry;
                    JarEntry je = new JarEntry(entry);
                    jo.putNextEntry(je);
                    byte[] buf = new byte[1024];
                    int anz;
                    while ((anz = bi.read(buf)) != -1)
                    {
                        jo.write(buf, 0, anz);
                    }
                    bi.close();
                }
            }
        }
    }

    private void addToZip(ZipOutputStream zo, String baseDir, File directory) throws FileNotFoundException, IOException
    {
        // get all files
        File[] files = directory.listFiles();
        for(int f=0;f<files.length;f++)
        {
            if(files[f].isDirectory())
            {
                String entry = files[f].getAbsolutePath();
                entry = entry.substring(directory.getAbsolutePath().length()+1);
                addToZip(zo,baseDir+entry+"/",files[f]);
            }
            else
            {
                //System.out.println("File = "+files[f].getAbsolutePath());
                //System.out.println("List = "+Arraysv.deepToString(excludeExtention));
                //System.out.println("We got = "+getExtension(files[f]));
                FileInputStream bi = new FileInputStream(files[f]);

                String entry = files[f].getAbsolutePath();
                entry = entry.substring(directory.getAbsolutePath().length()+1);
                entry = baseDir+entry;
                ZipEntry ze = new ZipEntry(entry);
                zo.putNextEntry(ze);
                byte[] buf = new byte[1024];
                int anz;
                while ((anz = bi.read(buf)) != -1)
                {
                    zo.write(buf, 0, anz);
                }
                zo.closeEntry();
                bi.close();
            }
        }
    }

    /**
     * http://snippets.dzone.com/posts/show/3468
     * modified to create the zip file if it does not exist
     * 
     * @param zipFile
     * @param files
     * @throws IOException
     */
    public void addFilesToExistingZip(File zipFile, String baseDir, File directory) throws IOException
    {
        ZipOutputStream out;
        File tempFile = null;

        byte[] buf = new byte[1024];
        boolean delete = false;

        if (zipFile.exists())
        {
            delete=true;
            // get a temp file
            tempFile = File.createTempFile(zipFile.getName(), null);
            // delete it, otherwise you cannot rename your existing zip to it.
            tempFile.delete();

            boolean renameOk=zipFile.renameTo(tempFile);
            if (!renameOk)
            {
                    throw new RuntimeException("could not rename the file "+zipFile.getAbsolutePath()+" to "+tempFile.getAbsolutePath());
            }

            ZipInputStream zin = new ZipInputStream(new FileInputStream(tempFile));
            out = new ZipOutputStream(new FileOutputStream(zipFile));

            ZipEntry entry = zin.getNextEntry();
            while (entry != null)
            {
                String name = entry.getName();
                boolean notInFiles = true;
                /*
                for (File f : files)
                {
                    if (f.getName().equals(name))
                    {
                        notInFiles = false;
                        break;
                    }
                }*/
                if (notInFiles)
                {
                    // Add ZIP entry to output stream.
                    out.putNextEntry(new ZipEntry(name));
                    // Transfer bytes from the ZIP file to the output file
                    int len;
                    while ((len = zin.read(buf)) > 0)
                    {
                        out.write(buf, 0, len);
                    }
                }
                entry = zin.getNextEntry();
            }
            // Close the streams
            zin.close();
        }
        else
        {
            out = new ZipOutputStream(new FileOutputStream(zipFile));
        }
        /*
        // Compress the files
        for (int i = 0; i < files.length; i++)
        {
            InputStream in = new FileInputStream(files[i]);
            // Add ZIP entry to output stream.
            out.putNextEntry(new ZipEntry(files[i].getName()));
            // Transfer bytes from the file to the ZIP file
            int len;
            while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
            }
            // Complete the entry
            out.closeEntry();
            in.close();
        }
        */

        addToZip(out, baseDir, directory);

        // Complete the ZIP file
        out.close();
        
        if (delete==true)
            tempFile.delete();
    }


    public void createJavaDoc(boolean showMessage)
    {
        try
        {
            if(save())
            {
                setChanged(false);
                String dirname = getDirectoryName();
                try
                {
                    if (!dirname.endsWith(System.getProperty("file.separator")))
                    {
                        dirname += System.getProperty("file.separator");
                    }
                    String[] args = new String[classes.size()+14];
                    args[0]="-d";
                    args[1]=""+dirname.replace("\\\\", "//")+"doc"+System.getProperty("file.separator")+"";
                    //args[1]=args[1].replaceAll(" ","%20");
                    //if(args[1].contains(" ")) args[1]='"'+args[1]+'"';
                    args[2]="-link";
                    args[3]="http://docs.oracle.com/javase/7/docs/api/";
                    args[4]="-encoding";
                    args[5]=Unimozer.FILE_ENCODING;
                    args[6]="-docencoding";
                    args[7]=Unimozer.FILE_ENCODING;
                    args[8]="-charset";
                    args[9]=Unimozer.FILE_ENCODING;
                    args[10]="-quiet"; //"-quiet";
                    args[11]="-private";
                    args[12]="-version";
                    args[13]="-author";
                    Set<String> set = classes.keySet();
                    int i = 14;

                    /*Iterator<String> itr = set.iterator();
                    while (itr.hasNext())
                    {
                        String classname = itr.next();*/
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String classname = entry.getKey();
                    
                        classname="src"+System.getProperty("file.separator")+classname.replace(".", System.getProperty("file.separator")); // replaceAll("\\.", System.getProperty("file.separator"));
                        args[i]=dirname+classname+".java";
                        i++;
                    }
                    frame.console.disconnect();
                    int retres = com.sun.tools.javadoc.Main.execute("javadoc",args);
                    //com.sun.tools.javadoc.Main
                    frame.console.connect();
                    
                    // lauch in external Browser
                    
                    String docindex = args[1]+"index.html";
                    try
                    {
                        //Runtime.getRuntime().exec(new String[] { "cmd", "/C", "start", docindex });
                        Process process = new ProcessBuilder("cmd","/C","start",docindex).start();
                    }
                    catch (Exception ex)
                    {
                        //ex.printStackTrace();
                        try
                        {
                            Process process = new ProcessBuilder("/usr/bin/open",docindex).start();
                            /*InputStream is = process.getInputStream();
                            InputStreamReader isr = new InputStreamReader(is);
                            BufferedReader br = new BufferedReader(isr);
                            String line;
                            System.out.printf("Output of running %s is:", Arrays.toString(args));
                            while ((line = br.readLine()) != null) {
                                System.out.println(line);
                            }*/

                            //Runtime.getRuntime().exec("open \""+docindex+"\"");
                        }
                        catch (Exception exi)
                        {
                            //ex.printStackTrace();
                            JOptionPane.showMessageDialog(
                                            frame,
                                            "Sorry, but it was not possible to open JavaDoc automatically.\n"+
                                            "Please open de file manually.\n\n"+
                                            docindex,
                                            "Error",
                                            JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    
                    
                    /*for(int h=0;h<args.length;h++)
                    {
                        System.out.print(args[h]+" ");
                    }
                    System.out.println();*/
                }
                catch (Exception ex)
                {
                    save();
                    JOptionPane.showMessageDialog(frame, "Something went wrong!\n\n"+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                }
                catch (Error ex)
                {
                    save();
                    JOptionPane.showMessageDialog(frame, "Something went wrong!\n\n"+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                }

                save();
                if (showMessage) JOptionPane.showMessageDialog(frame, "The JavaDoc files have been generated ...", "Success", JOptionPane.INFORMATION_MESSAGE,Unimozer.IMG_INFO);
            }
        } 
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(frame, "A terrible error occured!\n"+
                        ex.getMessage()+"\n","Error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
            setChanged(true);
        }
    }

    public void createJavaDoc()
    {
        createJavaDoc(false);
    }

    public void javadoc()
    {
        createJavaDoc(true);
    }

    // http://www.rgagnon.com/javadetails/java-0483.html
    public boolean deleteDirectory(File path)
    {
        try
        {
            FileUtils.deleteDirectory(path);
        }
        catch(Exception e)
        {
            return false;
        }
        return true;
        /*
        boolean result = true;
        if( path.exists() )
        {
            File[] files = path.listFiles();
            for(int i=0; i<files.length; i++)
            {
                if(files[i].isDirectory())
                {
                    result = result && deleteDirectory(files[i]);
                    result = result && files[i].delete();
                }
                else
                {
                    System.out.println("Deleting: "+files[i].getAbsolutePath());
                    result = result && files[i].delete();
                }
            }
            System.out.println("Deleting: "+path.getAbsolutePath());
            result = result & path.delete();
        }
        //return( result && path.delete() );
        // do not delete the base directory!
        return result;
         */
    }

    public boolean cleanHiddenFiles(File path)
    {
        boolean result = true;
        if( path.exists() )
        {
            File[] files = path.listFiles();
            for(int i=0; i<files.length; i++)
            {
                if(files[i].isDirectory())
                {
                    result = result && cleanHiddenFiles(files[i]);
                }
                else
                {
                    if (files[i].getName().equals(".DS_Store"))
                        result = (result) && (files[i].delete());
                }
            }
        }
        //return( result && path.delete() );
        // do not delete the base directory!
        return result;
    }

    public boolean deleteEmptyDirectories(File path)
    {
        boolean result = cleanHiddenFiles(path);
        if( path.exists() )
        {
            File[] files = path.listFiles();
            for(int i=0; i<files.length; i++)
            {
                if(files[i].isDirectory())
                {
                    result = result && deleteEmptyDirectories(files[i]);
                    if (files[i].listFiles().length==0) result = result && files[i].delete();
                }
            }
        }
        return result;
    }

    
    private void cleanUpSub(String dirname)
    {
        if(dirname!=null)
        {
            if (!dirname.endsWith(System.getProperty("file.separator")))
            {
                dirname += System.getProperty("file.separator");
            }
            
            // delete all "class" and "ctxt" files (BlueJ)
            // delete all BlueJ package files
            // delete .java files in the root
            File path = new File(dirname);
            if(path.exists())
            {
                File[] files = path.listFiles();
                for(int i=0; i<files.length; i++)
                {
                    if (files[i].isDirectory())
                        cleanUpSub(files[i].getAbsolutePath());
                    if(
                        files[i].getName().endsWith(".class")
                        ||
                        files[i].getName().endsWith(".ctxt")
                        ||
                        files[i].getName().endsWith(".java")
                        ||
                        files[i].getName().equals("package.bluej")
                        ||
                        files[i].getName().equals("bluej.pkg")
                    )
                    {
                        files[i].delete();
                    }
                }
            }
        }
        
    }

    public void cleanUp(String dirname)
    {
        if(dirname!=null)
        {
            if (!dirname.endsWith(System.getProperty("file.separator")))
            {
                dirname += System.getProperty("file.separator");
            }

            // delete all "class" and "ctxt" files (BlueJ)
            // delete all BlueJ package files
            // delete .java files in the root
            /*File path = new File(dirname);
            if(path.exists())
            {
                File[] files = path.listFiles();
                for(int i=0; i<files.length; i++)
                {
                    if(
                        files[i].getName().endsWith(".class")
                        ||
                        files[i].getName().endsWith(".ctxt")
                        ||
                        files[i].getName().endsWith(".java")
                        ||
                        files[i].getName().equals("package.bluej")
                        ||
                        files[i].getName().equals("bluej.pkg")
                    )
                    {
                        files[i].delete();
                    }
                }
            }*/
            cleanUpSub(dirname);
            
            // delete packages from the root
            for(Package pack : packages.values())
            {
                File f = new File(dirname+System.getProperty("file.separator")+pack.getName().replace('.', System.getProperty("file.separator").charAt(0)));
                if(f.exists())
                {
                    try
                    {
                        deleteDirectory(f);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }   
            }         
            
        }
    }
    
    public void clean()
    {
        // get the directory
        String dirname = getDirectoryName();
        if(dirname!=null)
        {
            if (!dirname.endsWith(System.getProperty("file.separator")))
            {
                dirname += System.getProperty("file.separator");
            }


            /*
            // delete all CLASS-files
            Set<String> set = classes.keySet();
            Iterator<String> itr = set.iterator();
            int i = 0;
            while (itr.hasNext())
            {
                String classname = itr.next();
                String act = classname + ".class";
                // also clean classes generated by BlueJ (if present)
                File file = new File(dirname+act);
                if(file.exists()) file.delete();
                file = new File(dirname+"bin/"+act);
                if(file.exists()) file.delete();
            }
            // delete the directory
            File binDir = new File(dirname+"bin/");
            binDir.delete();
            // delete the JAR-file
            // adjust the dirname
            String dir = getDirectoryName();
            if (!dir.endsWith("/"))
            {
                dir += "/";
            }

            // adjust the filename
            // adjust the filename
            String name = getDirectoryName();
            if (name.endsWith("/"))
            {
                name = name.substring(0, name.length() - 1);
            }
            name = name.substring(name.lastIndexOf("/")+1);

            // compose the filename
            File fDir = new File(dir+"dist/");
            name = dir + "dist/" + name + ".jar";
            File file = new File(name);
            //if(file.exists()) file.delete();
            if(fDir.exists()) fDir.delete();*/

            // delete all "class" and "ctxt" files (BlueJ)
            File path = new File(dirname);
            if(path.exists())
            {
                File[] files = path.listFiles();
                for(int i=0; i<files.length; i++)
                {
                    if(
                        files[i].getName().endsWith(".class")
                        ||
                        files[i].getName().endsWith(".ctxt")
                    )
                    {
                        files[i].delete();
                    }
                }
            }


            boolean d1 = deleteDirectory(new File(dirname+"bin"+System.getProperty("file.separator")));
            boolean d2 = deleteDirectory(new File(dirname+"dist"+System.getProperty("file.separator")));
            boolean d3 = deleteDirectory(new File(dirname+"doc"+System.getProperty("file.separator")));
            boolean d4 = deleteDirectory(new File(dirname+"versions"+System.getProperty("file.separator")));
            /*
            String zipFilename = dirname + "versions.zip";
            File zipFile = new File(zipFilename);
            boolean d4 = true;
            if (zipFile.exists()) d4=zipFile.delete();
            */
            if (
                    d1
                    &&
                    d2
                    &&
                    d3
                    &&
                    d4
            )
            {
                JOptionPane.showMessageDialog(
                    frame,
                    "Project cleaned up ...",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE,
                    Unimozer.IMG_INFO
                );
            }
            else
            {
                JOptionPane.showMessageDialog(
                    frame,
                    "Project cleaned up ...",
                    "Something went wrong ...",
                    JOptionPane.ERROR_MESSAGE,
                    Unimozer.IMG_ERROR
                );
            }
        }
    }

    public void command()
    {
        String cmd = JOptionPane.showInputDialog(frame, "Please enter the command you want to run.", "Run command", JOptionPane.QUESTION_MESSAGE);
        if (cmd!=null)
        {
            try
            {
                Runtime5.getInstance().executeCommand(cmd);
            }
            catch (EvalError ex)
            {
                JOptionPane.showMessageDialog(frame, "Your command returned an error:\n\n"+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
            }
        }
    }

    private void save(String dirName) throws FileNotFoundException, UnsupportedEncodingException, IOException
    {
        setDirectoryName(dirName);
        this.save();
        frame.setCanSave();
    }

    public StringList getLibs()
    {
        StringList sl = new StringList();
        String libName = directoryName+System.getProperty("file.separator")+"lib";
        File libs = new File(libName);
        if(libs.exists())
        {
            File[] files = libs.listFiles();
            for (int f = 0; f < files.length; f++)
            {
                if (files[f].getAbsolutePath().toLowerCase().endsWith(".jar"))
                {
                    sl.add(files[f].getAbsolutePath());
                }
            }
        }
        return sl;
    }
    
    public String getLibPath()
    {
        StringList sl = getLibs();
        String path = "";
        for(int i=0;i<sl.count();i++)
        {
            if(i!=0) path+=";";
            path+=sl.get(i);
        }
        return path;
    }
    
    private void addLibs()
    {
        String libName = directoryName+System.getProperty("file.separator")+"lib";
        File libs = new File(libName);
        if(libs.exists())
        {
            File[] files = libs.listFiles();
            for (int f = 0; f < files.length; f++)
            {
                if (files[f].getAbsolutePath().toLowerCase().endsWith(".jar"))
                {
                    try
                    {
                        System.out.println("Adding JAR: "+files[f].getAbsolutePath());
                        Runtime5.getInstance().interpreter.getClassManager().addClassPath(new URL("file://"+files[f].getAbsolutePath()));
                        Runtime5.getInstance().interpreter.getClassManager().reloadAllClasses();
                    } 
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
    
    private void openInteractiveProject(String dirName)
    {
        String filename = dirName+System.getProperty("file.separator")+"interactiveproject.pck";
        File file = new File(filename);
        if(file.exists())
        {
            StringList content = new StringList();
            content.loadFromFile(filename);
            
            if(!content.get(0).contains("xml"))
                interactiveProject = new InteractiveProject(content.get(0), diagram);
            else
                interactiveProject = new InteractiveProject(diagram, false);
            interactiveProject.loadFromXML(true);
        }
            
    }
    
    public void open(String dirname)
    {
        clear();
        //allowEdit=true;
        if(frame!=null)
        {
            frame.setAllowEdit(allowEdit);
            frame.setRelations(true,true,true,true,true);
        }
        
        setDirectoryName(dirname);
        //System.out.println("Opening: "+dirname);
        // load package
        String filename = directoryName+System.getProperty("file.separator")+Unimozer.U_PACKAGENAME;

        // is this a Unimozer Package
        File file = new File(filename);
        if(file.exists())
        {
            StringList content = new StringList();
            content.loadFromFile(filename);
            
            // init min position
            int minx = 0;
            int miny = 0;
            
            // load files
            for(int i = 0;i<content.count();i++)
            {
                if(
                    (i<=5)
                    &&
                        (
                            content.get(i).toLowerCase().equals("true")
                            ||
                            content.get(i).toLowerCase().equals("false")
                        )
                )
                {
                    switch(i)
                    {
                        case 0:
                                //allowEdit=Boolean.parseBoolean(content.get(i))
                                if(frame!=null);
                                    frame.setAllowEdit(allowEdit);
                                break;
                        case 1:
                                showHeritage=Boolean.parseBoolean(content.get(i));
                                break;
                        case 2:
                                showComposition=Boolean.parseBoolean(content.get(i));
                                break;
                        case 3:
                                showAggregation=Boolean.parseBoolean(content.get(i));
                                if(frame!=null)
                                    frame.setRelations(showHeritage, showComposition, showAggregation,showFields,showMethods);
                                break;
                        case 4:
                                showFields=Boolean.parseBoolean(content.get(i));
                                break;
                        case 5:
                                showMethods=Boolean.parseBoolean(content.get(i));
                                if(frame!=null)
                                    frame.setRelations(showHeritage, showComposition, showAggregation,showFields,showMethods);
                                break;
                    }
                }
                else
                {
                    try
                    {
                        StringList line = new StringList();
                        line.setCommaText(content.get(i));
                        //MyClass mc = new MyClass(new FileInputStream(directoryName + "/" + line.get(0) + ".java"));

                        // the source fioles can be located in two different places
                        File fileDefault = new File(directoryName + System.getProperty("file.separator") + line.get(0).replace(".", System.getProperty("file.separator")) + ".java");
                        File fileSrc     = new File(directoryName + System.getProperty("file.separator") + "src" + System.getProperty("file.separator") + line.get(0).replace(".", System.getProperty("file.separator")) + ".java");
                        
                        MyClass mc = null;
                        
                        // only a "default" file is present
                        if(fileDefault.exists() && !fileSrc.exists()) mc = new MyClass(fileDefault.getAbsolutePath(),Unimozer.FILE_ENCODING);
                        // only a "src" file is present
                        else if(!fileDefault.exists() && fileSrc.exists()) mc = new MyClass(fileSrc.getAbsolutePath(),Unimozer.FILE_ENCODING);
                        // both files are present, so take the latest one!
                        else if(fileDefault.exists() && fileSrc.exists())
                        {
                            if(fileDefault.lastModified()>fileSrc.lastModified()) mc = new MyClass(fileDefault.getAbsolutePath(),Unimozer.FILE_ENCODING);
                            else mc = new MyClass(fileSrc.getAbsolutePath(),Unimozer.FILE_ENCODING);
                        }
                        
                        if(mc!=null)
                        {
                            mc.setPosition(new Point(Integer.valueOf(line.get(1)), Integer.valueOf(line.get(2))));
                            addClass(mc);
                            if(Integer.valueOf(line.get(1))<minx) minx=Integer.valueOf(line.get(1));
                            if(Integer.valueOf(line.get(2))<miny) miny=Integer.valueOf(line.get(2));
                        }
                        else System.err.println("Unable to find the file: "+line.get(0) + ".java");
                    }
                    catch (Exception ex)
                    {
                        System.err.println(ex.getMessage());
                    }
                }
            }
            
            // translate min position
            if (minx<0)
                for(Entry<String,MyClass> entry : classes.entrySet()) 
                {
                    // get the actual class ...
                    String acuClassMame = entry.getKey();
                    MyClass acuMyClass = classes.get(acuClassMame);
                    Point pos = acuMyClass.getPosition();
                    pos.x-=minx;
                    acuMyClass.setPosition(pos);
                }
            if (miny<0)
                for(Entry<String,MyClass> entry : classes.entrySet()) 
                {
                    // get the actual class ...
                    String acuClassMame = entry.getKey();
                    MyClass acuMyClass = classes.get(acuClassMame);
                    Point pos = acuMyClass.getPosition();
                    pos.y-=miny;
                    acuMyClass.setPosition(pos);
                }
            
            // in case we load a modified project (by NetBeans?) we need to make shure all classes are loaded, so
            // add the src directory again.
            addDir(new File(directoryName+System.getProperty("file.separator")+"src"),false);
            addLibs();
            //setChanged(false);
        }
        else // check if we can open a BlueJ Package
        {
            
            String bfilename = directoryName+System.getProperty("file.separator")+Unimozer.B_PACKAGENAME;
            String nfilename = directoryName+System.getProperty("file.separator")+Unimozer.N_PACKAGENAME;
            
            File bfile = new File(bfilename);
            File nfile = new File(nfilename);

            if(bfile.exists())
            {
                loadFromBlueJ(directoryName);
            }
            else if (nfile.exists()) // load NetBeans source folder
            {
                addDir(new File(directoryName+System.getProperty("file.separator")+"src"));
            }
            else // simply load all Java files -OR- it is a NetBeans project
            {
                addDir(new File(directoryName));
                /*
                String loadDir = directoryName;
                    
                File dir = new File(loadDir);

                String[] children = dir.list();
                if (children == null) {
                    // Either dir does not exist or is not a directory
                } else {
                    for (int i=0; i<children.length; i++) {
                        // Get filename of file or directory
                        String ffilename = children[i];
                    }
                }

                // It is also possible to filter the list of returned files.
                // This example does not return any files that start with `.'.
                FilenameFilter filter = new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return (!name.startsWith(".") && name.toLowerCase().endsWith(".java"));
                    }
                };
                children = dir.list(filter);

                for(int i=0;i<children.length;i++)
                {
                    try
                    {
                        MyClass mc = new MyClass(loadDir + System.getProperty("file.separator") + children[i],Unimozer.FILE_ENCODING);
                        addClass(mc);
                    }
                    catch (Exception ex)
                    {
                        MyError.display(ex);
                    }
                }*/

            }
            addLibs();
        }
        openInteractiveProject(dirname);
        updateEditor();
        if(frame!=null)
            frame.setCanSave();
        setChanged(false);
        setUML(isUML);

        if(Unimozer.javaCompileOnTheFly)
        {
            new Thread(new Runnable() {

                @Override
                public void run()
                {
                    diagram.compile();
                }
            }).start();
        }
    }
    
    private void loadFromBlueJ(String path)
    {
        // load files from the current directory
        String bfilename = path+System.getProperty("file.separator")+Unimozer.B_PACKAGENAME;

        File bfile = new File(bfilename);
        if(bfile.exists())
        {
            Properties p = new Properties();
            try
            {
                p.load(new FileInputStream(bfilename));
                int i = 1;
                while(p.containsKey("target"+i+".name"))
                {
                    if(!p.getProperty("target"+i+".type").equals("PackageTarget"))
                    {
                        MyClass mc = new MyClass(path + System.getProperty("file.separator") + p.getProperty("target"+i+".name") + ".java",Unimozer.FILE_ENCODING);
                        mc.setPosition(new Point(Integer.valueOf(p.getProperty("target"+i+".x")),
                                                 Integer.valueOf(p.getProperty("target"+i+".y"))));
                        addClass(mc);
                    }
                    i++;
                }
            }
            catch (Exception ex)
            {
                MyError.display(ex);
            }
        }
        
        // search all other directories
        File dir = new File(path);

        String[] children = dir.list();
        if (children == null) {
            // Either dir does not exist or is not a directory
        } else {
            for (int i=0; i<children.length; i++) {
                // Get filename of file or directory
                String ffilename = children[i];
            }
        }

        // It is also possible to filter the list of returned files.
        // This example does not return any files that start with `.'.
        FilenameFilter filter = new FilenameFilter() 
        {
            @Override
            public boolean accept(File dir, String name) 
            {
                return ((new File(dir.getAbsolutePath()+System.getProperty("file.separator")+name)).isDirectory());
            }
        };
        children = dir.list(filter);

        for(int i=0;i<children.length;i++)
        {
            //System.out.println("Loading: "+directoryName + System.getProperty("file.separator") + children[i]);
            loadFromBlueJ(path + System.getProperty("file.separator") + children[i]);
        }
        
    
    }

    private boolean saveWithAskingLocation()
    {
       OpenProject op = new OpenProject(new File(getContainingDirectoryName()),false);
       //op.setSelectedFile(new File(new File(getDirectoryName()).getName()));
       //op.set
       int result = op.showSaveDialog(frame,"Save");
       if(result==OpenProject.APPROVE_OPTION)
       {
            String dirName = op.getSelectedFile().getAbsolutePath().toString();
            File myDir = new File(dirName);
            if(myDir.exists())
            {
                 JOptionPane.showMessageDialog(frame, "The selected project or directory already exists.\nPlease choose another one ..." , "New project", JOptionPane.WARNING_MESSAGE,Unimozer.IMG_WARNING);
                 return false;
            }
            else
            {
                boolean created = myDir.mkdir();
                if (created==false)
                {
                    JOptionPane.showMessageDialog(frame, "Error while creating the projet directory.\n"+
                        "The project name you specified is probably not valid!\n",
                        "Save project as ...", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                    return false;
                }
                else
                {
                    try
                    {
                        //System.out.println(dirName);
                        diagram.save(dirName);
                        setChanged(false);
                        return true;
                    }
                    catch (Exception e)
                    {
                        JOptionPane.showMessageDialog(frame, "An unknown error occured while saving your projet!\n", "Save project as ...", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                        return false;
                    }
                }
            }
       } return false;

    }

    public boolean askToSave()
    {
        try
        {
            if(diagram.getClassCount()>0 && isChanged()==true)
            {
                int answ = JOptionPane.showConfirmDialog(frame, "Do you want to save the current project?", "Save project?", JOptionPane.YES_NO_CANCEL_OPTION);
                if (answ == JOptionPane.YES_OPTION)
                {
                    if(directoryName==null) return saveWithAskingLocation();
                    else return save();
                }
                else if (answ == JOptionPane.NO_OPTION)
                {
                    return true;
                }
                else return false;
            }
            return true;
        }
        catch(Exception e)
        {
            JOptionPane.showMessageDialog(frame, "A terrible error occured!\n"+
                        e.getMessage()+"\n","Error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
            return false;
        }
    }

    public boolean openUnimozer()
    {
        if(askToSave()==true)
        {
            OpenProject op = new OpenProject(new File(getContainingDirectoryName()),false);
            int result = op.showOpenDialog(frame);
            if(result==OpenProject.APPROVE_OPTION)
            {
                String dirName = op.getSelectedFile().getAbsolutePath().toString();
                this.open(dirName);
                setChanged(false);
                setEnabled(true);
                return true;
            } else return false;
        } else return false;
    }

    public boolean openUnimozer(String filename)
    {
        if(askToSave()==true)
        {
            String dirName = (new File(filename)).getParent();
            this.open(dirName);
            setChanged(false);
            setEnabled(true);
            return true;
        } else return false;
    }

    public boolean saveAsUnimozer()
    {
       OpenProject op = new OpenProject(new File(getContainingDirectoryName()),false);
       int result = op.showSaveDialog(frame);
       if(result==OpenProject.APPROVE_OPTION)
       {
            String dirName = op.getSelectedFile().getAbsolutePath().toString();
            File myDir = new File(dirName);
            if(myDir.exists())
            {
                 JOptionPane.showMessageDialog(frame, "The selected project or directory already exists.\nPlease choose another one ..." , "Save project as ...", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                 return false;
            }
            else
            {
                boolean created = myDir.mkdir();
                if (created==false)
                {
                    JOptionPane.showMessageDialog(frame, "Error while creating the projet directory.\n"+
                        "The project name you specified is probably not valid!\n",
                        "Save project as ...", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                    return false;
                }
                else
                {
                    try
                    {
                        this.save(dirName);
                        setChanged(false);
                        return true;
                    }
                    catch (Exception e)
                    {
                        JOptionPane.showMessageDialog(frame, "An unknown error occured while saving your projet!\n"+e.getMessage(), "Save project as ...", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                        setChanged(true);
                        return false;
                    }
                }
            }
       } return false;
    }

    public boolean newUnimozer()
    {
       if(askToSave()==true)
       {
           /*
           OpenProject op = new OpenProject(null,false,false);
           int result = op.showSaveDialog(frame);
           if(result==OpenProject.APPROVE_OPTION)
           {
                String dirName = op.getSelectedFile().getAbsolutePath().toString();
                File myDir = new File(dirName);
                if(myDir.exists())
                {
                     JOptionPane.showMessageDialog(frame, "The selected project or directory already exists.\nPlease choose another one ..." , "New project", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                     return false;
                }
                else
                {
            */
                    clear();
                    setChanged(false);
                    setEnabled(true);
                    repaint();
            /*
                    myDir.mkdir();
                    //System.out.println(dirName);
                    this.save(dirName);
             */
                    return true;
             /*
             }
           }
           return false;
            */
       } return false;
    }

    public void saveUnimozer()
    {
        try
        {
            this.save();
            setChanged(false);
        } 
        catch (Exception ex)
        {
            JOptionPane.showMessageDialog(frame, "A terrible error occured!\n"+
                        ex.getMessage()+"\n","Error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
            setChanged(true);
        }
    }

    public int getClassCount()
    {
        return classes.size();
    } 

    //
    // (!) classes must be compiled (!)
    //
    public Vector<String> getMains() //throws ClassNotFoundException
    {
        Vector<String> mains = new Vector<String>();
        int c = 1;
        /*Set<String> set = classes.keySet();
        Iterator<String> itr = set.iterator();
        while (itr.hasNext())
        {
            String acuClassMame =itr.next();
/*
            
        /* let's try this one ... */
        for(Entry<String,MyClass> entry : classes.entrySet()) 
        {
            // get the actual class ...
            String acuClassMame = entry.getKey();
            
            MyClass acuMyClass = classes.get(acuClassMame);

            if(ModifierSet.isPublic(acuMyClass.getModifiers()))
            {
                /*
                Class<?> cla = Runtime5.getInstance().load(acuMyClass.getShortName());

                Method m[] = ((Class) cla).getDeclaredMethods();

                boolean found = false;
                for (int i = 0; i < m.length; i++)
                {
                    String full = "";
                    full+= m[i].getReturnType().getSimpleName();
                    full+=" ";
                    full+= m[i].getName();
                    full+= "(";
                    Type[] tvm = m[i].getParameterTypes();
                    for(int t=0;t<tvm.length;t++)
                    {
                        String sn = tvm[t].toString();
                        sn=sn.substring(sn.lastIndexOf('.')+1,sn.length());
                        if(sn.startsWith("class")) sn=sn.substring(5).trim();
                        full+= sn+", ";
                    }
                    if(tvm.length>0) full=full.substring(0,full.length()-2);
                    full+= ")";
                    String complete = new String(Modifier.toString(m[i].getModifiers())+" "+full);
                    //System.err.println(acuClassMame+" >> "+complete);
                    if(complete.equals("public static void main(String;)")) mains.add(acuClassMame);
                }
                */

                if(acuMyClass.hasMain()) mains.add(acuClassMame);
            } 
        }
        return mains;
    }


	public void exportPNG()
	{
		selectClass(null);

		JFileChooser dlgSave = new JFileChooser("Export diagram as PNG ...");
		// propose name
		String uniName = directoryName.substring(directoryName.lastIndexOf('/')+1).trim();
		dlgSave.setSelectedFile(new File(uniName));

		dlgSave.addChoosableFileFilter(new PNGFilter());
		int result = dlgSave.showSaveDialog(frame);
		if (result == JFileChooser.APPROVE_OPTION)
		{
			String filename=dlgSave.getSelectedFile().getAbsoluteFile().toString();
			if(!filename.substring(filename.length()-4, filename.length()).toLowerCase().equals(".png"))
			{
				filename+=".png";
			}

			File file = new File(filename);
			BufferedImage bi = new BufferedImage(this.getWidth(), this.getHeight(),BufferedImage.TYPE_4BYTE_ABGR);
			paint(bi.getGraphics());
			try
			{
				ImageIO.write(bi, "png", file);
			}
			catch(Exception e)
			{
				JOptionPane.showOptionDialog(frame,"Error while saving the image!","Error",JOptionPane.OK_OPTION,JOptionPane.ERROR_MESSAGE,null,null,null);
			}
		}
	}

	// Inner class is used to hold an image while on the clipboard.
	public static class ImageSelection implements Transferable
		{
			// the Image object which will be housed by the ImageSelection
			private Image image;

			public ImageSelection(Image image)
			{
                            this.image = image;
                        }

			// Returns the supported flavors of our implementation
			public DataFlavor[] getTransferDataFlavors()
			{
                                return new DataFlavor[] {DataFlavor.imageFlavor};
			}

			// Returns true if flavor is supported
			public boolean isDataFlavorSupported(DataFlavor flavor)
			{
                            return DataFlavor.imageFlavor.equals(flavor);
			}

			// Returns Image object housed by Transferable object
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException,IOException
			{
				if (!DataFlavor.imageFlavor.equals(flavor))
				{
					throw new UnsupportedFlavorException(flavor);
				}
				// else return the payload
				return image;
			}
		}

    public void copyToClipboardPNG()
    {
        Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        //DataFlavor pngFlavor = new DataFlavor("image/png","Portable Network Graphics");

        // reset the comments
        commentString=null;
        commentPoint=null;

        // get diagram
        BufferedImage image = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
        paint(image.getGraphics());

        /*System.out.println(this.topLeft.x);
        System.out.println(this.topLeft.y);
        System.out.println(this.bottomRight.x);
        System.out.println(this.bottomRight.y);*/

        int x = this.topLeft.x-2;
        if(x<0) x=0;
        
        int y = this.topLeft.y-2;
        if(y<0) y=0;
        
        image = new BufferedImage(this.bottomRight.x+2+1+4, 
                                  this.bottomRight.y+2+1+4, BufferedImage.TYPE_INT_ARGB);
        paint(image.getGraphics());
        
        /*System.out.println(this.getWidth());
        System.out.println(this.getParent().getWidth());
        System.out.println(x+this.bottomRight.x-x+2+1+4);
        
        System.out.println(x+" , "+ y+" , "+
                            (this.bottomRight.x-x+2+1+4)+" , "+
                            (this.bottomRight.y-y+2+1+4));
        */
        BufferedImage image2 =
                            image.getSubimage(x, y, 
                            this.bottomRight.x-x+2+1+4, 
                            this.bottomRight.y-y+2+1+4);
        // put image to clipboard
        ImageSelection imageSelection = new ImageSelection(image2);
        
        try 
        {
            systemClipboard.setContents(imageSelection, null);
        }
        catch (Exception ex) 
        {
            // ignore
        }

        // use the JWS clipboard if loaded via JWS
        try 
        {
            Class.forName("javax.jnlp.ClipboardService");
            final ClipboardService clipboardService = (ClipboardService)ServiceManager.lookup("javax.jnlp.ClipboardService");
            clipboardService.setContents(imageSelection);
        } 
        catch (Exception ex) 
        {
            //ex.printStackTrace();
        }

    }

    /**
     * Copies the UML diagram of a *SINGLE CLASS* as PNG to the clipboard.
     * @param mode  the drawing mode:
     *                  0 = uncompiled, nothing selected
     *                  1 = uncompiled, selected
     *                  2 = compiled
     */
    public void copyToClipboardPNG(MyClass myClass, int mode)
    {
        Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        //DataFlavor pngFlavor = new DataFlavor("image/png","Portable Network Graphics");

        // reset the comments
        commentString=null;
        commentPoint=null;

        // get diagram
        BufferedImage image = new BufferedImage(myClass.getWidth()+1, myClass.getHeight()+1, BufferedImage.TYPE_INT_ARGB);
        myClass.draw(image.getGraphics(), showFields, showMethods, mode);
        // do twice to make shure the image dimensions are OK
        if(mode==2) // because of the double border
            image = new BufferedImage(myClass.getWidth()+5, myClass.getHeight()+5, BufferedImage.TYPE_INT_ARGB);
        else
            image = new BufferedImage(myClass.getWidth()+1, myClass.getHeight()+1, BufferedImage.TYPE_INT_ARGB);
        myClass.draw(image.getGraphics(), showFields, showMethods, mode);

        /* debug: save the image somewhere */
        /*
        try {
            ImageIO.write((RenderedImage) image, "png", new File("/Users/robertfisch/Desktop/test.png"));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        */

        /*
        for (final DataFlavor flavor : 
        Toolkit.getDefaultToolkit().getSystemClipboard().getAvailableDataFlavors()) {
                    System.out.println(flavor.getMimeType());
        }         
        */
        
        // put image to clipboard
        ImageSelection imageSelection = new ImageSelection(image);
        try
        {
            systemClipboard.setContents(imageSelection, null);
        }
        catch (Exception ex) 
        {
            // ignore
        }

        // use the JWS clipboard if loaded via JWS
        try 
        {
            Class.forName("javax.jnlp.ClipboardService");
            final ClipboardService clipboardService = (ClipboardService)ServiceManager.lookup("javax.jnlp.ClipboardService");
            clipboardService.setContents(imageSelection);
        } 
        catch (Exception ex) 
        {
             // ignore
        }
    }


    /**
     * méthode qui recherche le plus grand nombre dans une liste
     * 1) supposer que le premier élément est le plus grand
     * 2) parcourir la liste et tester les éléments restants s'il
     *    n'y a pas un autre élément plus grand et mémoriser alors
     *    celui-ci
     */
    public int findMax(int[] liste)
    {
        int max = liste[0];
        for(int i=1 ; i<liste.length ; i++)
        {
            if (liste[i]>max)
            {
                max = liste[i];
            }
        }
        return max;
    }
    
    public void addConstructor()
    {
        MyClass mc = this.getSelectedClass();
        if (mc!=null)
        {
            ConstructorEditor ce = ConstructorEditor.showModal(this.frame, "Add a new constructor");
            Ini.set("javaDocConstructor", Boolean.toString(ce.generateJavaDoc()));
            if (ce.OK==true)
            {
                String sign = mc.addConstructor(ce.getModifier(),ce.getParams(),ce.generateJavaDoc());
                diagram.selectClass(mc);
                mc.selectBySignature(sign);
                updateEditor();
                editor.focus();
                setChanged(true);
                if(Unimozer.javaCompileOnTheFly)
                {
                    new Thread(new Runnable() {

                        @Override
                        public void run()
                        {
                            diagram.compile();
                        }
                    }).start();
                }

                /*
                ConstructorDeclaration cd = mc.addConstructor(ce.getModifier(),ce.getParams());

                Ini.set("javaDocConstructor", Boolean.toString(ce.generateJavaDoc()));
                if(ce.generateJavaDoc())
                {
                    String jd=
                         "\n"+
                         "     * Write a description of this constructor here."+"\n"+
                         "     * "+"\n";
                    Vector<Vector<String>> params = ce.getParams();
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
                        jd+="     * @param "+thisName+"    a description of the parameter “"+thisName+"“\n";
                    }
                    jd+= "     ";
                    JavadocComment jdc = new JavadocComment(jd);
                    cd.setJavaDoc(jdc);
                }


                diagram.selectClass(mc);
                setChanged(true);
                 */
            }
        }
    }

    public void addMethod()
    {
        MyClass mc = this.getSelectedClass();
        if (mc!=null)
        {
            Vector<String> names = new Vector<String>();
            for(String name : classes.keySet())
            {
                names.add(name);
            }
                        
            MethodEditor me = MethodEditor.showModal(this.frame,"Add a new method",names);
            Ini.set("javaDocMethod", Boolean.toString(me.generateJavaDoc()));
            if (me.OK==true)
            {
                /*if (mc.hasMethod(me.getMethodName()))
                {
                    JOptionPane.showMessageDialog(frame, "Sorry, but this class has already have a method named “"+me.getMethodName()+"“." , "Error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                }
                else
                {*/
                    String sign = mc.addMethod(me.getMethodType(),me.getMethodName(),me.getModifier(), me.getParams(),me.generateJavaDoc());
                    diagram.selectClass(mc);
                    mc.selectBySignature(sign);
                    updateEditor();
                    editor.focus();
                    setChanged(true);
                    if(Unimozer.javaCompileOnTheFly)
                    {
                        new Thread(new Runnable() {

                            public void run()
                            {
                                diagram.compile();
                            }
                        }).start();
                    }

                    /*
                    MethodDeclaration md = mc.addMethod(me.getMethodType(),me.getMethodName(),me.getModifier(), me.getParams());

                    Ini.set("javaDocMethod", Boolean.toString(me.generateJavaDoc()));
                    if(me.generateJavaDoc())
                    {
                        String jd=
                             "\n"+
                             "     * Write a description of method “"+me.getMethodName()+"“ here."+"\n"+
                             "     * "+"\n";
                        Vector<Vector<String>> params = me.getParams();
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
                            jd+="     * @param "+thisName+"    a description of the parameter “"+thisName+"“\n";
                        }
                        if(!me.getMethodType().equals("void"))
                        {
                            String thisName = new String();
                            while (thisName.length()<maxLength) thisName+=" ";
                            jd+="     * @return "+thisName+"     a description of the returned result\n";
                        }
                        jd+= "     ";
                        JavadocComment jdc = new JavadocComment(jd);
                        md.setJavaDoc(jdc);
                    }

                    diagram.selectClass(mc);
                    setChanged(true);
                     */
                //}
            }
        }
    }

    public void addField()
    {
        MyClass mc = this.getSelectedClass();
        if (mc!=null)
        {
            FieldEditor fe = FieldEditor.showModal(this.frame, "Add a new field");
            Ini.set("thisField", Boolean.toString(!fe.generateThis()));
            Ini.set("javaDocField", Boolean.toString(fe.generateJavaDoc()));
            Ini.set("setterField", Boolean.toString(fe.generateSetterIni()));
            Ini.set("getterField", Boolean.toString(fe.generateGetterIni()));
            if (fe.OK==true)
            {
                if (mc.hasField(fe.getFieldName()))
                {
                    JOptionPane.showMessageDialog(frame, "Sorry, but this class has already have a field named “"+fe.getFieldName()+"“." , "Error", JOptionPane.ERROR_MESSAGE,Unimozer.IMG_ERROR);
                }
                else
                {
                    mc.addField(fe.getFieldType(),fe.getFieldName(),fe.getModifier(),fe.generateJavaDoc(),fe.generateSetter(),fe.generateGetter(),fe.generateThis());
                    diagram.selectClass(mc);
                    updateEditor();
                    setChanged(true);
                    if(Unimozer.javaCompileOnTheFly)
                    {
                        new Thread(new Runnable() {

                            @Override
                            public void run()
                            {
                                diagram.compile();
                            }
                        }).start();
                    }

                    /*
                    FieldDeclaration fd = mc.addField(fe.getFieldType(),fe.getFieldName(),fe.getModifier());

                    Ini.set("javaDoField", Boolean.toString(fe.generateJavaDoc()));
                    if(fe.generateJavaDoc())
                    {
                        String jd=
                             "\n"+
                             "     * Write a description of field “"+fe.getFieldName()+"“ here."+"\n";
                        jd+= "     ";
                        JavadocComment jdc = new JavadocComment(jd);
                        fd.setJavaDoc(jdc);
                    }

                    diagram.selectClass(mc);
                    setChanged(true);
                     */
                }
            }
        }
    }
    
    public void resetInteractiveProject()
    {
        interactiveProject = null;
    }


}
