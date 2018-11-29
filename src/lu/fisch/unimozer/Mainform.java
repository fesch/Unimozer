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
 
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.InputMap;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import lu.fisch.structorizer.gui.Editor;
import lu.fisch.unimozer.compilation.CompilationError;
import lu.fisch.unimozer.console.Console;
import lu.fisch.unimozer.dialogs.BootLogReport;
import lu.fisch.unimozer.dialogs.CreateInteractiveProjectDialog;
import lu.fisch.unimozer.dialogs.JSliderOnJOptionPane;
import lu.fisch.unimozer.dialogs.NewInteractiveProjectDialog;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaUIBackgroundDrawer;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rsyntaxtextarea.Token;

import com.bulenkov.darcula.DarculaLaf;

/**
 *
 * @author robertfisch
 */
public class Mainform extends JFrame
{
    lu.fisch.structorizer.gui.Mainform structorizer = null;
    JList errorList = null;

    /** Creates new form Unimozer */
    public Mainform()
    {
        initComponents();
        
        // split panes eat F8 and F6. This is corrected here.
        InputMap map = (InputMap) UIManager.get("SplitPane.ancestorInputMap");
        KeyStroke keyStrokeF6 = KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0);
        KeyStroke keyStrokeF8 = KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0);
        map.remove(keyStrokeF6);
        map.remove(keyStrokeF8);

        // hide some items
        miAllowEdit.setVisible(false);
        miSepAllowEditing.setVisible(false);

        // keystrokes
        miNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        miSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        miOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        miPrintDiagram.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
	miAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1,0));
	miRunFast.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6,0));
	miCompile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F9,0));
	miMake.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F10,0));
	miJar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11,0));
	miJavaDoc.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12,0));
	miQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

        miUndo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        miRedo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        miCut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        miCopy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        miPaste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        miClipboardColoredCode.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()+KeyEvent.SHIFT_MASK));



        miFind.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        miFindAgain.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        miReplace.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));


        getContentPane().setLayout(new AKDockLayout());
        
        this.getContentPane().removeAll();

        this.getContentPane().add(tbActions,AKDockLayout.NORTH);
        this.getContentPane().add(tbFile,AKDockLayout.NORTH);
        this.getContentPane().add(tbMake,AKDockLayout.NORTH);
        this.getContentPane().add(tbElements,AKDockLayout.NORTH);
        this.getContentPane().add(tbShow,AKDockLayout.NORTH);
        this.getContentPane().add(tbFontSize,AKDockLayout.NORTH);

        miToolbarFile.setSelected(true);
        miToolbarUML.setSelected(true);
        miToolbarRun.setSelected(false);
        miToolbarFont.setSelected(true);
        updateToolbars();
        
        this.getContentPane().add(pnlBody,AKDockLayout.CENTER);
        this.getContentPane().validate();


        diagram.setEditor(codeEditor);
        diagram.setFrame(this);
        diagram.setObjectizer(objectizer);
        diagram.setStatus(lblStatus);
        codeEditor.setDiagram(diagram);
        codeEditor.setStatus(lblStatus);
        codeEditor.setFrame(this);
        this.setEnabledEditorActions(false);
        objectizer.setDiagram(diagram); 
        objectizer.setFrame(this);
        objectizer.setCalling(callingLabel);
        bottomPanel.setBackground(Color.decode("#ffffaa"));

        this.setTitle(Unimozer.E_NAME);
        this.setDefaultCloseOperation(Mainform.DO_NOTHING_ON_CLOSE);


        //Runtime.getInstance(console).setConsole(console);

        String message = "Unimozer was unable to find a properly installed JDK. Please make\n"+
                         "shure that the environment variable \"JDK_HOME\" points to the\n"+
                         "installation folder of your JDK.\n"+
                         "\n"+
                         "Unimozer can run without an installed JDK but its compiling\n"+
                         "functionality will be limited.\n"+
                         "\n"+
                         "For now, the internal Janino compiler will be used for compilation,\n"+
                         "which allows you to compile Java 1.4 compliant code with some\n"+
                         "limited functionalities of Java 1.5 ...";

        if(Unimozer.javaCompilerDetected==false)
            JOptionPane.showMessageDialog(this, message , "Compiler not found", JOptionPane.INFORMATION_MESSAGE);

          

        try
        {
            Class.forName("com.sun.tools.javadoc.Main");
            //com.sun.tools.javadoc.Main.execute("",new String[1]);
        }
        catch (ClassNotFoundException ex) //
        {
            miJavaDoc.setVisible(false);
            speJavaDoc.setVisible(false);
            Unimozer.javaDocDetected=false;
        }
        catch (NoClassDefFoundError ex)//
        {
            miJavaDoc.setVisible(false);
            speJavaDoc.setVisible(false);
            Unimozer.javaDocDetected=false;
        }


        // create NSD-diagram
        lu.fisch.structorizer.io.Ini.setUseAppData(true);
        lu.fisch.structorizer.elements.Element.E_DIN = true;
        lu.fisch.structorizer.gui.Diagram nsd = new lu.fisch.structorizer.gui.Diagram(null,"---[ please select a method ]---");
        nsd.setFocusable(true);

        final Mainform mf = this;
        nsd.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e)
            {
                if(e.getClickCount()==2)
                {
                    Console.disconnectAll();
                    lu.fisch.structorizer.gui.Mainform mainform = new lu.fisch.structorizer.gui.Mainform(false);
                    mainform.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    mainform.diagram.setRoot(mf.diagram.getNsd().getRoot(),false,true);
                    mainform.diagram.redraw();
                    Console.connectAll();
                }
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
            }
        });

        spNSD.add(nsd);
        spNSD.setViewportView(nsd);
        diagram.setNsd(nsd);

        //
        Ini ini = Ini.getInstance();
        try
        {
            ini.load();
        }
        catch (FileNotFoundException ex)
        {
            MyError.display(ex);
        }
        catch (IOException ex)
        {
            MyError.display(ex);;
        }

        // visual style
        if(ini.getProperty("umlStandard", "true").equals("true"))
        {
            diagram.setUML(true);
            miDiagramStandardJava.setSelected(false);
            miDiagramStandardUML.setSelected(true);
        }
        else
        {
            diagram.setUML(false);
            miDiagramStandardJava.setSelected(true);
            miDiagramStandardUML.setSelected(false);
        }
        
        // hide private fields
        objectizer.setHidePrivateFields(ini.getProperty("hidePrivateFields","false").equals("true"));
        chkHidePrivateFields.setSelected(objectizer.hasHidePrivateFields());

        // toolbars
        miToolbarFile.setSelected(ini.getProperty("toolbarFile","true").equals("true"));
        miToolbarFont.setSelected(ini.getProperty("toolbarFont","true").equals("true"));
        miToolbarRun.setSelected(ini.getProperty("toolbarRun","false").equals("true"));
        miToolbarUML.setSelected(ini.getProperty("toolbarUML","true").equals("true"));
        miToolbarShow.setSelected(ini.getProperty("toolbarShow","true").equals("true"));
        
        //miShowJavadoc.setSelected(ini.getProperty("showComments","true").equals("true"));
        // initial directory name


        diagram.setContainingDirectoryName(getInitialDir());
        //default encoding
        Unimozer.FILE_ENCODING=ini.getProperty("defaultEncoding",Unimozer.FILE_ENCODING);
        // encoding
        miEncodingUTF8.setSelected(Unimozer.FILE_ENCODING.equals("UTF-8"));
        miEncodingWindows1252.setSelected(Unimozer.FILE_ENCODING.equals("windows-1252"));
        // window
        int top = Integer.valueOf(ini.getProperty("top","0")).intValue();
        int left = Integer.valueOf(ini.getProperty("left","0")).intValue();
        int width = Integer.valueOf(ini.getProperty("width","750")).intValue();
        int height = Integer.valueOf(ini.getProperty("height","550")).intValue();
        
        // Get the size of the default screen
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        // make shure the window is inside the wisible area
        if ( (left>dim.getWidth()) || (top>dim.getHeight()) ||
             (left<0) || (top<0)   )
        {
            // revert to defaults
            top=0;
            left=0;
            width=750;
            height=550;
        }
        
        setPreferredSize(new Dimension(width,height));
        setSize(width,height);
        setLocation(new Point(top,left));
        validate();
        // sliders
        splitty_1.setDividerLocation(Integer.valueOf(ini.getProperty("splitty_1", "350")));
        splitty_2.setDividerLocation(Integer.valueOf(ini.getProperty("splitty_2", "500")));
        splitty_3.setDividerLocation(Integer.valueOf(ini.getProperty("splitty_3", "400")));
        bottomSplitter.setDividerLocation(Integer.valueOf(ini.getProperty("splitty_4", "400")));
        codeEditor.jsp.setDividerLocation(Integer.valueOf(ini.getProperty("splitty_5", String.valueOf(Integer.MAX_VALUE))));
        // font size
        Unimozer.DRAW_FONT_SIZE=Integer.valueOf(ini.getProperty("fontSize", "12"));
        //System.out.println(codeEditor.getFont().getSize());
        codeEditor.setFontSize(Unimozer.DRAW_FONT_SIZE);
        // options
        Unimozer.javaCompileOnTheFly=Boolean.valueOf(ini.getProperty("compileOnTheFly","false"));
        miCompileOnTheFly.setSelected(Unimozer.javaCompileOnTheFly);
        
        RSyntaxTextAreaUIBackgroundDrawer.setSaturation((int) Integer.valueOf(ini.getProperty("structureHighlithningSaturation","0")));
        RSyntaxTextAreaUIBackgroundDrawer.setActive(Boolean.valueOf(ini.getProperty("structureHighlithning","false")));
        
        if(!RSyntaxTextAreaUIBackgroundDrawer.isActive())
            shOff.setSelected(true);
        else if (RSyntaxTextAreaUIBackgroundDrawer.getSaturation()==0)
            shLight.setSelected(true);
        else if (RSyntaxTextAreaUIBackgroundDrawer.getSaturation()==10)
            shMedium.setSelected(true);
        else if (RSyntaxTextAreaUIBackgroundDrawer.getSaturation()==20)
            shStrong.setSelected(true);
        else if (RSyntaxTextAreaUIBackgroundDrawer.getSaturation()==30)
            shVeryStrong.setSelected(true);
        else shCostum.setSelected(true);

        updateToolbars();

        setTitleNew();

        //System.err.println(Unimozer.messages.getText());
        /**/
    }


    public Diagram getDiagram()
    {
        return diagram;
    }

    public void setCanSave()
    {
        speSave.setEnabled(true);
        miSave.setEnabled(true);

        setTitleNew();
    }


    public void setTitleNew()
    {
        if(diagram.getDirectoryName()!=null)
        {
            String name = diagram.getDirectoryName();
            name = name.substring(name.lastIndexOf('/')+1).trim(); 
            if(diagram.isChanged()) name+=" [changed]";

            this.setTitle(Unimozer.E_NAME+" - "+name);
        }
        else this.setTitle(Unimozer.E_NAME+" - [new]");
    }

/*
    private void addField()
    {
        MyClass mc = diagram.getSelectedClass();
        if (mc!=null)
        {
            FieldEditor fe = FieldEditor.showModal(this, "Add a new class");
            if (fe.OK==true)
            {
                mc.addField(fe.getFieldType(),fe.getFieldName(),fe.getModifier());
                diagram.selectClass(mc);
            }
        }
    }

    private void addConstructor()
    {
        MyClass mc = diagram.getSelectedClass();
        if (mc!=null)
        {
            ConstructorEditor ce = ConstructorEditor.showModal(this, "Add a new constructor");
            if (ce.OK==true)
            {
                mc.addConstructor(ce.getModifier(),ce.getParams());
                diagram.selectClass(mc);
            }
        }
    }

    private void addMethod()
    {
        MyClass mc = diagram.getSelectedClass();
        if (mc!=null)
        {
            MethodEditor me = MethodEditor.showModal(this,"Add a new method");
            if (me.OK==true)
            {
                mc.addMethod(me.getMethodType(),me.getMethodName(),me.getModifier(), me.getParams());
                diagram.selectClass(mc);
            }
        }
    }
*/
    public void updateMode()
    {
        if (diagram.getMode() == Diagram.MODE_SELECT) speModeSelect.setSelected(true);
        else speModeExtends.setSelected(true);
    }

    public void setAllowEdit(boolean b)
    {
        miAllowEdit.setSelected(b);
    }

    public void setRelations(boolean heritage, boolean composition, boolean aggregation, boolean showFields, boolean showMethods)
    {
        miShowHeritage.setSelected(heritage);
        miShowComposition.setSelected(composition);
        miShowAggregation.setSelected(aggregation);
        miShowFields.setSelected(showFields);
        miShowMethods.setSelected(showMethods);

        speShowHeritage.setSelected(heritage);
        speShowComposition.setSelected(composition);
        speShowAggregation.setSelected(aggregation);
        speShowFields.setSelected(showFields);
        speShowMethods.setSelected(showMethods);

        updateDiagramElements();
    }

    private void updateDiagramElements()
    {
        diagram.setShowHeritage(miShowHeritage.isSelected());
        diagram.setShowComposition(miShowComposition.isSelected());
        diagram.setShowAggregation(miShowAggregation.isSelected());
        diagram.setShowFields(miShowFields.isSelected());
        diagram.setShowMethods(miShowMethods.isSelected());
        diagram.repaint();
        diagram.validate();
    }

    private void updateToolbars()
    {
        this.getContentPane().remove(tbFile);
        this.getContentPane().remove(tbMake);
        this.getContentPane().remove(tbElements);
        this.getContentPane().remove(tbShow);
        this.getContentPane().remove(tbFontSize);

        if(miToolbarFile.isSelected())
        {
            this.getContentPane().add(tbFile,AKDockLayout.NORTH);
        }
        if(miToolbarRun.isSelected())
        {
            this.getContentPane().add(tbMake,AKDockLayout.NORTH);
        }
        if(miToolbarUML.isSelected())
        {
            this.getContentPane().add(tbElements,AKDockLayout.NORTH);
        }
        if(miToolbarShow.isSelected())
        {
            this.getContentPane().add(tbShow,AKDockLayout.NORTH);
        }
        if(miToolbarFont.isSelected())
        {
            this.getContentPane().add(tbFontSize,AKDockLayout.NORTH);
        }
        this.getContentPane().repaint();
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */ 
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        outPopup = new javax.swing.JPopupMenu();
        popClear = new javax.swing.JMenuItem();
        actionGroup = new javax.swing.ButtonGroup();
        syntaxHightlighterGroup = new javax.swing.ButtonGroup();
        tbElements = new javax.swing.JToolBar();
        speAddClass = new javax.swing.JButton();
        speAddConstructor = new javax.swing.JButton();
        speAddMethod = new javax.swing.JButton();
        speAddField = new javax.swing.JButton();
        pnlBody = new javax.swing.JPanel();
        splitty_1 = new javax.swing.JSplitPane();
        bottomSplitter = new javax.swing.JSplitPane();
        splitty_2 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        lblStatus = new javax.swing.JLabel();
        codeEditor = new lu.fisch.unimozer.CodeEditor();
        spNSD = new javax.swing.JScrollPane();
        bottomPanel = new javax.swing.JPanel();
        consoleScroller = new javax.swing.JScrollPane();
        console = new lu.fisch.unimozer.console.Console();
        callingLabel = new javax.swing.JLabel();
        splitty_3 = new javax.swing.JSplitPane();
        scrollDiagram = new javax.swing.JScrollPane();
        diagram = new lu.fisch.unimozer.Diagram();
        objectizer = new lu.fisch.unimozer.Objectizer();
        tbFile = new javax.swing.JToolBar();
        speNew = new javax.swing.JButton();
        speOpen = new javax.swing.JButton();
        speSave = new javax.swing.JButton();
        tbMake = new javax.swing.JToolBar();
        speRunFast = new javax.swing.JButton();
        speRun = new javax.swing.JButton();
        speStop = new javax.swing.JButton();
        speCommand = new javax.swing.JButton();
        speCompile = new javax.swing.JButton();
        speMake = new javax.swing.JButton();
        speJar = new javax.swing.JButton();
        speJavaDoc = new javax.swing.JButton();
        speClean = new javax.swing.JButton();
        tbFontSize = new javax.swing.JToolBar();
        speFontDown = new javax.swing.JButton();
        speFontUp = new javax.swing.JButton();
        tbShow = new javax.swing.JToolBar();
        speShowHeritage = new javax.swing.JToggleButton();
        speShowComposition = new javax.swing.JToggleButton();
        speShowAggregation = new javax.swing.JToggleButton();
        speShowFields = new javax.swing.JToggleButton();
        speShowMethods = new javax.swing.JToggleButton();
        tbActions = new javax.swing.JToolBar();
        speModeSelect = new javax.swing.JToggleButton();
        speModeExtends = new javax.swing.JToggleButton();
        jMenuBar = new javax.swing.JMenuBar();
        mFile = new javax.swing.JMenu();
        miNew = new javax.swing.JMenuItem();
        miNewVisualizer = new javax.swing.JMenuItem();
        miOpen = new javax.swing.JMenuItem();
        miAddFile = new javax.swing.JMenuItem();
        miSave = new javax.swing.JMenuItem();
        miSaveAs = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        miPrintDiagram = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        miQuit = new javax.swing.JMenuItem();
        mProject = new javax.swing.JMenu();
        miRunFast = new javax.swing.JMenuItem();
        miRun = new javax.swing.JMenuItem();
        miStop = new javax.swing.JMenuItem();
        miCommand = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        miCompile = new javax.swing.JMenuItem();
        miMake = new javax.swing.JMenuItem();
        miJar = new javax.swing.JMenuItem();
        miJavaDoc = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        miClean = new javax.swing.JMenuItem();
        jSeparator11 = new javax.swing.JPopupMenu.Separator();
        miCreateInteractive = new javax.swing.JMenuItem();
        mEdit = new javax.swing.JMenu();
        miUndo = new javax.swing.JMenuItem();
        miRedo = new javax.swing.JMenuItem();
        jSeparator9 = new javax.swing.JPopupMenu.Separator();
        miCut = new javax.swing.JMenuItem();
        miCopy = new javax.swing.JMenuItem();
        miPaste = new javax.swing.JMenuItem();
        jSeparator10 = new javax.swing.JPopupMenu.Separator();
        miFind = new javax.swing.JMenuItem();
        miFindAgain = new javax.swing.JMenuItem();
        miReplace = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JPopupMenu.Separator();
        miClipboardPNG = new javax.swing.JMenuItem();
        miClipboardColoredCode = new javax.swing.JMenuItem();
        mView = new javax.swing.JMenu();
        miToolbars = new javax.swing.JMenu();
        miToolbarFile = new javax.swing.JCheckBoxMenuItem();
        miToolbarUML = new javax.swing.JCheckBoxMenuItem();
        miToolbarRun = new javax.swing.JCheckBoxMenuItem();
        miToolbarFont = new javax.swing.JCheckBoxMenuItem();
        miToolbarShow = new javax.swing.JCheckBoxMenuItem();
        miDiagramStandard = new javax.swing.JMenu();
        miDiagramStandardUML = new javax.swing.JCheckBoxMenuItem();
        miDiagramStandardJava = new javax.swing.JCheckBoxMenuItem();
        mDiagram = new javax.swing.JMenu();
        miAllowEdit = new javax.swing.JCheckBoxMenuItem();
        miSepAllowEditing = new javax.swing.JSeparator();
        miAddClass = new javax.swing.JMenuItem();
        miAddConstructor = new javax.swing.JMenuItem();
        miAddMethod = new javax.swing.JMenuItem();
        miAddField = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        miShowHide = new javax.swing.JMenu();
        miShowHeritage = new javax.swing.JCheckBoxMenuItem();
        miShowComposition = new javax.swing.JCheckBoxMenuItem();
        miShowAggregation = new javax.swing.JCheckBoxMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        miShowFields = new javax.swing.JCheckBoxMenuItem();
        miShowMethods = new javax.swing.JCheckBoxMenuItem();
        jSeparator5 = new javax.swing.JSeparator();
        miExportPNG = new javax.swing.JMenuItem();
        mOptions = new javax.swing.JMenu();
        miEncoding = new javax.swing.JMenu();
        miEncodingUTF8 = new javax.swing.JRadioButtonMenuItem();
        miEncodingWindows1252 = new javax.swing.JRadioButtonMenuItem();
        miCompileOnTheFly = new javax.swing.JCheckBoxMenuItem();
        miStructureHighlithningLEvel = new javax.swing.JMenu();
        miDarkTheme = new javax.swing.JCheckBoxMenuItem();
        shOff = new javax.swing.JRadioButtonMenuItem();
        shLight = new javax.swing.JRadioButtonMenuItem();
        shMedium = new javax.swing.JRadioButtonMenuItem();
        shStrong = new javax.swing.JRadioButtonMenuItem();
        shVeryStrong = new javax.swing.JRadioButtonMenuItem();
        shCostum = new javax.swing.JRadioButtonMenuItem();
        chkRealtime = new javax.swing.JCheckBoxMenuItem();
        chkHidePrivateFields = new javax.swing.JCheckBoxMenuItem();
        mHelp = new javax.swing.JMenu();
        miAbout = new javax.swing.JMenuItem();
        miBootLogReport = new javax.swing.JMenuItem();

        popClear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/gen_clear.png"))); // NOI18N
        popClear.setText("Clear");
        popClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popClearActionPerformed(evt);
            }
        });
        outPopup.add(popClear);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
            public void windowActivated(java.awt.event.WindowEvent evt) {
                formWindowActivated(evt);
            }
        });

        tbElements.setRollover(true);

        speAddClass.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_class.png"))); // NOI18N
        speAddClass.setToolTipText("Add a class ...");
        speAddClass.setFocusable(false);
        speAddClass.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speAddClass.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speAddClass.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speAddClassActionPerformed(evt);
            }
        });
        tbElements.add(speAddClass);

        speAddConstructor.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_constructor.png"))); // NOI18N
        speAddConstructor.setToolTipText("Add a constructor ...");
        speAddConstructor.setEnabled(false);
        speAddConstructor.setFocusable(false);
        speAddConstructor.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speAddConstructor.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speAddConstructor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speAddConstructorActionPerformed(evt);
            }
        });
        tbElements.add(speAddConstructor);

        speAddMethod.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_method.png"))); // NOI18N
        speAddMethod.setToolTipText("Add a method ...");
        speAddMethod.setEnabled(false);
        speAddMethod.setFocusable(false);
        speAddMethod.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speAddMethod.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speAddMethod.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speAddMethodActionPerformed(evt);
            }
        });
        tbElements.add(speAddMethod);

        speAddField.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_field.png"))); // NOI18N
        speAddField.setToolTipText("Add a field ...");
        speAddField.setEnabled(false);
        speAddField.setFocusable(false);
        speAddField.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speAddField.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speAddField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speAddFieldActionPerformed(evt);
            }
        });
        tbElements.add(speAddField);

        pnlBody.setBackground(new java.awt.Color(204, 255, 204));

        splitty_1.setDividerLocation(350);
        splitty_1.setResizeWeight(0.7);
        splitty_1.setContinuousLayout(true);

        bottomSplitter.setDividerLocation(400);
        bottomSplitter.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        bottomSplitter.setResizeWeight(1.0);

        splitty_2.setDividerLocation(600);
        splitty_2.setResizeWeight(1.0);

        jPanel1.setLayout(new java.awt.BorderLayout());

        lblStatus.setText("...");
        lblStatus.setOpaque(true);
        jPanel1.add(lblStatus, java.awt.BorderLayout.PAGE_END);

        codeEditor.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                codeEditorFocusLost(evt);
            }
        });
        jPanel1.add(codeEditor, java.awt.BorderLayout.CENTER);

        splitty_2.setLeftComponent(jPanel1);

        spNSD.setMinimumSize(new java.awt.Dimension(0, 0));
        splitty_2.setRightComponent(spNSD);

        bottomSplitter.setLeftComponent(splitty_2);

        bottomPanel.setLayout(new java.awt.BorderLayout());

        console.setColumns(20);
        console.setRows(5);
        console.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                consoleMouseClicked(evt);
            }
        });
        consoleScroller.setViewportView(console);

        bottomPanel.add(consoleScroller, java.awt.BorderLayout.CENTER);
        bottomPanel.add(callingLabel, java.awt.BorderLayout.PAGE_START);

        bottomSplitter.setBottomComponent(bottomPanel);

        splitty_1.setRightComponent(bottomSplitter);

        splitty_3.setDividerLocation(400);
        splitty_3.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        splitty_3.setResizeWeight(1.0);
        splitty_3.setContinuousLayout(true);

        diagram.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                diagramComponentResized(evt);
            }
        });

        org.jdesktop.layout.GroupLayout diagramLayout = new org.jdesktop.layout.GroupLayout(diagram);
        diagram.setLayout(diagramLayout);
        diagramLayout.setHorizontalGroup(
            diagramLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 337, Short.MAX_VALUE)
        );
        diagramLayout.setVerticalGroup(
            diagramLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 397, Short.MAX_VALUE)
        );

        scrollDiagram.setViewportView(diagram);

        splitty_3.setLeftComponent(scrollDiagram);

        objectizer.setDiagram(diagram);

        org.jdesktop.layout.GroupLayout objectizerLayout = new org.jdesktop.layout.GroupLayout(objectizer);
        objectizer.setLayout(objectizerLayout);
        objectizerLayout.setHorizontalGroup(
            objectizerLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 339, Short.MAX_VALUE)
        );
        objectizerLayout.setVerticalGroup(
            objectizerLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 23, Short.MAX_VALUE)
        );

        splitty_3.setBottomComponent(objectizer);

        splitty_1.setLeftComponent(splitty_3);

        org.jdesktop.layout.GroupLayout pnlBodyLayout = new org.jdesktop.layout.GroupLayout(pnlBody);
        pnlBody.setLayout(pnlBodyLayout);
        pnlBodyLayout.setHorizontalGroup(
            pnlBodyLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, splitty_1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 866, Short.MAX_VALUE)
        );
        pnlBodyLayout.setVerticalGroup(
            pnlBodyLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(splitty_1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 540, Short.MAX_VALUE)
        );

        tbFile.setRollover(true);

        speNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/gen_new_project.png"))); // NOI18N
        speNew.setToolTipText("Create a new project ...");
        speNew.setFocusable(false);
        speNew.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speNew.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speNewActionPerformed(evt);
            }
        });
        tbFile.add(speNew);

        speOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/gen_open_project.png"))); // NOI18N
        speOpen.setToolTipText("Open an existing project ...");
        speOpen.setFocusable(false);
        speOpen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speOpen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speOpenActionPerformed(evt);
            }
        });
        tbFile.add(speOpen);

        speSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/gen_save.png"))); // NOI18N
        speSave.setToolTipText("Save the project");
        speSave.setEnabled(false);
        speSave.setFocusable(false);
        speSave.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speSave.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speSaveActionPerformed(evt);
            }
        });
        tbFile.add(speSave);

        tbMake.setRollover(true);

        speRunFast.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/netbeans_run.png"))); // NOI18N
        speRunFast.setToolTipText("Run the project's main class.");
        speRunFast.setFocusable(false);
        speRunFast.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speRunFast.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speRunFast.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speRunFastActionPerformed(evt);
            }
        });
        tbMake.add(speRunFast);

        speRun.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/netbeans_run-args.png"))); // NOI18N
        speRun.setToolTipText("Run the project's main class with arguments.");
        speRun.setFocusable(false);
        speRun.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speRun.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speRunActionPerformed(evt);
            }
        });
        tbMake.add(speRun);

        speStop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/netbeans_stop.png"))); // NOI18N
        speStop.setToolTipText("Stop any code execution...");
        speStop.setFocusable(false);
        speStop.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speStop.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speStopActionPerformed(evt);
            }
        });
        tbMake.add(speStop);

        speCommand.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/iconfinder_command_gpl_gnome_project.png"))); // NOI18N
        speCommand.setToolTipText("Execute a command ...");
        speCommand.setFocusable(false);
        speCommand.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speCommand.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speCommand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speCommandActionPerformed(evt);
            }
        });
        tbMake.add(speCommand);

        speCompile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/gen_proc.png"))); // NOI18N
        speCompile.setToolTipText("Compile all classes in memory.");
        speCompile.setFocusable(false);
        speCompile.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speCompile.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speCompile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speCompileActionPerformed(evt);
            }
        });
        tbMake.add(speCompile);

        speMake.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/java_make.png"))); // NOI18N
        speMake.setToolTipText("Write class files to the disk.");
        speMake.setFocusable(false);
        speMake.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speMake.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speMake.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speMakeActionPerformed(evt);
            }
        });
        tbMake.add(speMake);

        speJar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/netbeans_build.png"))); // NOI18N
        speJar.setToolTipText("Create JAR bundle ...");
        speJar.setFocusable(false);
        speJar.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speJar.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speJar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speJarActionPerformed(evt);
            }
        });
        tbMake.add(speJar);

        speJavaDoc.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/iconfinder_doc_lgpl_david_vignoni.png"))); // NOI18N
        speJavaDoc.setToolTipText("Create Java-DOC");
        speJavaDoc.setFocusable(false);
        speJavaDoc.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speJavaDoc.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speJavaDoc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speJavaDocActionPerformed(evt);
            }
        });
        tbMake.add(speJavaDoc);

        speClean.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/netbeans_clean.png"))); // NOI18N
        speClean.setToolTipText("Delete the compiled files.");
        speClean.setFocusable(false);
        speClean.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speClean.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speClean.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speCleanActionPerformed(evt);
            }
        });
        tbMake.add(speClean);

        tbFontSize.setRollover(true);

        speFontDown.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/font_down.png"))); // NOI18N
        speFontDown.setToolTipText("Decrease the font size ...");
        speFontDown.setFocusable(false);
        speFontDown.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speFontDown.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speFontDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speFontDownActionPerformed(evt);
            }
        });
        tbFontSize.add(speFontDown);

        speFontUp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/font_up.png"))); // NOI18N
        speFontUp.setToolTipText("Increase the font size ...");
        speFontUp.setFocusable(false);
        speFontUp.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speFontUp.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speFontUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speFontUpActionPerformed(evt);
            }
        });
        tbFontSize.add(speFontUp);

        tbShow.setRollover(true);

        speShowHeritage.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/showHeritage.png"))); // NOI18N
        speShowHeritage.setSelected(true);
        speShowHeritage.setToolTipText("Show inheritance links.");
        speShowHeritage.setFocusable(false);
        speShowHeritage.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speShowHeritage.setMaximumSize(new java.awt.Dimension(28, 20));
        speShowHeritage.setMinimumSize(new java.awt.Dimension(28, 20));
        speShowHeritage.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speShowHeritage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speShowHeritageActionPerformed(evt);
            }
        });
        tbShow.add(speShowHeritage);

        speShowComposition.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/showComposition.png"))); // NOI18N
        speShowComposition.setSelected(true);
        speShowComposition.setToolTipText("Show composition links.");
        speShowComposition.setFocusable(false);
        speShowComposition.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speShowComposition.setMaximumSize(new java.awt.Dimension(28, 20));
        speShowComposition.setMinimumSize(new java.awt.Dimension(28, 20));
        speShowComposition.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speShowComposition.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speShowCompositionActionPerformed(evt);
            }
        });
        tbShow.add(speShowComposition);

        speShowAggregation.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/showAggregation.png"))); // NOI18N
        speShowAggregation.setSelected(true);
        speShowAggregation.setToolTipText("Show aggregation links.");
        speShowAggregation.setFocusable(false);
        speShowAggregation.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speShowAggregation.setMaximumSize(new java.awt.Dimension(28, 20));
        speShowAggregation.setMinimumSize(new java.awt.Dimension(28, 20));
        speShowAggregation.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speShowAggregation.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speShowAggregationActionPerformed(evt);
            }
        });
        tbShow.add(speShowAggregation);

        speShowFields.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/showFields.png"))); // NOI18N
        speShowFields.setSelected(true);
        speShowFields.setToolTipText("Show fields.");
        speShowFields.setFocusable(false);
        speShowFields.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speShowFields.setMaximumSize(new java.awt.Dimension(28, 20));
        speShowFields.setMinimumSize(new java.awt.Dimension(28, 20));
        speShowFields.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speShowFields.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speShowFieldsActionPerformed(evt);
            }
        });
        tbShow.add(speShowFields);

        speShowMethods.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/showMethods.png"))); // NOI18N
        speShowMethods.setSelected(true);
        speShowMethods.setToolTipText("Show methods.");
        speShowMethods.setFocusable(false);
        speShowMethods.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speShowMethods.setMaximumSize(new java.awt.Dimension(28, 20));
        speShowMethods.setMinimumSize(new java.awt.Dimension(28, 20));
        speShowMethods.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speShowMethods.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speShowMethodsActionPerformed(evt);
            }
        });
        tbShow.add(speShowMethods);

        tbActions.setRollover(true);

        actionGroup.add(speModeSelect);
        speModeSelect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/action_select.png"))); // NOI18N
        speModeSelect.setSelected(true);
        speModeSelect.setToolTipText("Selection tool");
        speModeSelect.setFocusable(false);
        speModeSelect.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speModeSelect.setMaximumSize(new java.awt.Dimension(28, 20));
        speModeSelect.setMinimumSize(new java.awt.Dimension(28, 20));
        speModeSelect.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speModeSelect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speModeSelectActionPerformed(evt);
            }
        });
        tbActions.add(speModeSelect);

        actionGroup.add(speModeExtends);
        speModeExtends.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/action_extends.png"))); // NOI18N
        speModeExtends.setToolTipText("Insert an inheritance relation");
        speModeExtends.setFocusable(false);
        speModeExtends.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        speModeExtends.setMaximumSize(new java.awt.Dimension(28, 20));
        speModeExtends.setMinimumSize(new java.awt.Dimension(28, 20));
        speModeExtends.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        speModeExtends.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speModeExtendsActionPerformed(evt);
            }
        });
        tbActions.add(speModeExtends);

        mFile.setText("File");

        miNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/gen_new_project.png"))); // NOI18N
        miNew.setText("New Project ...");
        miNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miNewActionPerformed(evt);
            }
        });
        mFile.add(miNew);

        miNewVisualizer.setText("New Interactive Project ...");
        miNewVisualizer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miNewVisualizerActionPerformed(evt);
            }
        });
        mFile.add(miNewVisualizer);

        miOpen.setText("Open Project ...");
        miOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miOpenActionPerformed(evt);
            }
        });
        mFile.add(miOpen);

        miAddFile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/newFile.png"))); // NOI18N
        miAddFile.setText("Add File ...");
        miAddFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miAddFileActionPerformed(evt);
            }
        });
        mFile.add(miAddFile);

        miSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/gen_save.png"))); // NOI18N
        miSave.setText("Save Project");
        miSave.setEnabled(false);
        miSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miSaveActionPerformed(evt);
            }
        });
        mFile.add(miSave);

        miSaveAs.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/gen_save.png"))); // NOI18N
        miSaveAs.setText("Save Project As ...");
        miSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miSaveAsActionPerformed(evt);
            }
        });
        mFile.add(miSaveAs);
        mFile.add(jSeparator2);

        miPrintDiagram.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/print.png"))); // NOI18N
        miPrintDiagram.setText("Print ...");
        miPrintDiagram.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miPrintDiagramActionPerformed(evt);
            }
        });
        mFile.add(miPrintDiagram);
        mFile.add(jSeparator7);

        miQuit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        miQuit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/gen_run.png"))); // NOI18N
        miQuit.setText("Quit");
        miQuit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miQuitActionPerformed(evt);
            }
        });
        mFile.add(miQuit);

        jMenuBar.add(mFile);

        mProject.setText("Project");

        miRunFast.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F6, 0));
        miRunFast.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/netbeans_run.png"))); // NOI18N
        miRunFast.setText("Run");
        miRunFast.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miRunFastActionPerformed(evt);
            }
        });
        mProject.add(miRunFast);

        miRun.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/netbeans_run-args.png"))); // NOI18N
        miRun.setText("Run (with arguments)");
        miRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miRunActionPerformed(evt);
            }
        });
        mProject.add(miRun);

        miStop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/netbeans_stop.png"))); // NOI18N
        miStop.setText("Stop");
        miStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miStopActionPerformed(evt);
            }
        });
        mProject.add(miStop);

        miCommand.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/iconfinder_command_gpl_gnome_project.png"))); // NOI18N
        miCommand.setText("Command");
        miCommand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miCommandActionPerformed(evt);
            }
        });
        mProject.add(miCommand);
        mProject.add(jSeparator4);

        miCompile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/gen_proc.png"))); // NOI18N
        miCompile.setText("Compile");
        miCompile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miCompileActionPerformed(evt);
            }
        });
        mProject.add(miCompile);

        miMake.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/java_make.png"))); // NOI18N
        miMake.setText("Make");
        miMake.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miMakeActionPerformed(evt);
            }
        });
        mProject.add(miMake);

        miJar.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/netbeans_build.png"))); // NOI18N
        miJar.setText("Create JAR");
        miJar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miJarActionPerformed(evt);
            }
        });
        mProject.add(miJar);

        miJavaDoc.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/iconfinder_doc_lgpl_david_vignoni.png"))); // NOI18N
        miJavaDoc.setText("Create JavaDoc");
        miJavaDoc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miJavaDocActionPerformed(evt);
            }
        });
        mProject.add(miJavaDoc);
        mProject.add(jSeparator3);

        miClean.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/netbeans_clean.png"))); // NOI18N
        miClean.setText("Clean");
        miClean.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miCleanActionPerformed(evt);
            }
        });
        mProject.add(miClean);
        mProject.add(jSeparator11);

        miCreateInteractive.setText("Create Interactive Project");
        miCreateInteractive.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miCreateInteractiveActionPerformed(evt);
            }
        });
        mProject.add(miCreateInteractive);

        jMenuBar.add(mProject);

        mEdit.setText("Edit");

        miUndo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/iconfinder_undo_gplaleksandra_wolska.png"))); // NOI18N
        miUndo.setText("Undo");
        miUndo.setEnabled(false);
        miUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miUndoActionPerformed(evt);
            }
        });
        mEdit.add(miUndo);

        miRedo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/iconfinder_redo_gplaleksandra_wolska.png"))); // NOI18N
        miRedo.setText("Redo");
        miRedo.setEnabled(false);
        miRedo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miRedoActionPerformed(evt);
            }
        });
        mEdit.add(miRedo);
        mEdit.add(jSeparator9);

        miCut.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/iconfinder_cut_gplaleksandra_wolska.png"))); // NOI18N
        miCut.setText("Cut");
        miCut.setEnabled(false);
        miCut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miCutActionPerformed(evt);
            }
        });
        mEdit.add(miCut);

        miCopy.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/iconfinder_copy_gplaleksandra_wolska.png"))); // NOI18N
        miCopy.setText("Copy");
        miCopy.setEnabled(false);
        miCopy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miCopyActionPerformed(evt);
            }
        });
        mEdit.add(miCopy);

        miPaste.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/iconfinder_paste_gpl_aleksandra_wolska.png"))); // NOI18N
        miPaste.setText("Paste");
        miPaste.setEnabled(false);
        miPaste.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miPasteActionPerformed(evt);
            }
        });
        mEdit.add(miPaste);
        mEdit.add(jSeparator10);

        miFind.setText("Find ...");
        miFind.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miFindActionPerformed(evt);
            }
        });
        mEdit.add(miFind);

        miFindAgain.setText("Find Again");
        miFindAgain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miFindAgainActionPerformed(evt);
            }
        });
        mEdit.add(miFindAgain);

        miReplace.setText("Replace All ...");
        miReplace.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miReplaceActionPerformed(evt);
            }
        });
        mEdit.add(miReplace);
        mEdit.add(jSeparator8);

        miClipboardPNG.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/export_image.png"))); // NOI18N
        miClipboardPNG.setText("Copy diagram");
        miClipboardPNG.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miClipboardPNGActionPerformed(evt);
            }
        });
        mEdit.add(miClipboardPNG);

        miClipboardColoredCode.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/export_code.png"))); // NOI18N
        miClipboardColoredCode.setText("Copy colored code");
        miClipboardColoredCode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miClipboardColoredCodeActionPerformed(evt);
            }
        });
        mEdit.add(miClipboardColoredCode);

        jMenuBar.add(mEdit);

        mView.setText("View");

        miToolbars.setText("Toolbars");

        miToolbarFile.setSelected(true);
        miToolbarFile.setText("File");
        miToolbarFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miToolbarFileActionPerformed(evt);
            }
        });
        miToolbars.add(miToolbarFile);

        miToolbarUML.setSelected(true);
        miToolbarUML.setText("UML");
        miToolbarUML.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miToolbarUMLActionPerformed(evt);
            }
        });
        miToolbars.add(miToolbarUML);

        miToolbarRun.setSelected(true);
        miToolbarRun.setText("Run");
        miToolbarRun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miToolbarRunActionPerformed(evt);
            }
        });
        miToolbars.add(miToolbarRun);

        miToolbarFont.setSelected(true);
        miToolbarFont.setText("Font");
        miToolbarFont.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miToolbarFontActionPerformed(evt);
            }
        });
        miToolbars.add(miToolbarFont);

        miToolbarShow.setSelected(true);
        miToolbarShow.setText("Show / Hide");
        miToolbarShow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miToolbarShowActionPerformed(evt);
            }
        });
        miToolbars.add(miToolbarShow);

        mView.add(miToolbars);

        miDiagramStandard.setText("Diagram standard");

        miDiagramStandardUML.setSelected(true);
        miDiagramStandardUML.setText("UML");
        miDiagramStandardUML.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aDiagramStandardUML(evt);
            }
        });
        miDiagramStandard.add(miDiagramStandardUML);

        miDiagramStandardJava.setText("Java");
        miDiagramStandardJava.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aDiagramStandardJava(evt);
            }
        });
        miDiagramStandard.add(miDiagramStandardJava);

        mView.add(miDiagramStandard);

        jMenuBar.add(mView);

        mDiagram.setText("Diagram");

        miAllowEdit.setSelected(true);
        miAllowEdit.setText("Allow editing");
        miAllowEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miAllowEditActionPerformed(evt);
            }
        });
        mDiagram.add(miAllowEdit);
        mDiagram.add(miSepAllowEditing);

        miAddClass.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_class.png"))); // NOI18N
        miAddClass.setText("Add class ...");
        miAddClass.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miAddClassActionPerformed(evt);
            }
        });
        mDiagram.add(miAddClass);

        miAddConstructor.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_constructor.png"))); // NOI18N
        miAddConstructor.setText("Add constructor ...");
        miAddConstructor.setEnabled(false);
        miAddConstructor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miAddConstructorActionPerformed(evt);
            }
        });
        mDiagram.add(miAddConstructor);

        miAddMethod.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_method.png"))); // NOI18N
        miAddMethod.setText("Add method ...");
        miAddMethod.setEnabled(false);
        miAddMethod.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miAddMethodActionPerformed(evt);
            }
        });
        mDiagram.add(miAddMethod);

        miAddField.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_field.png"))); // NOI18N
        miAddField.setText("Add field ...");
        miAddField.setEnabled(false);
        miAddField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miAddFieldActionPerformed(evt);
            }
        });
        mDiagram.add(miAddField);
        mDiagram.add(jSeparator1);

        miShowHide.setText("Show / hide elements");

        miShowHeritage.setSelected(true);
        miShowHeritage.setText("Heritage");
        miShowHeritage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miShowHeritageActionPerformed(evt);
            }
        });
        miShowHide.add(miShowHeritage);

        miShowComposition.setSelected(true);
        miShowComposition.setText("Composition");
        miShowComposition.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miShowCompositionActionPerformed(evt);
            }
        });
        miShowHide.add(miShowComposition);

        miShowAggregation.setSelected(true);
        miShowAggregation.setText("Aggregation");
        miShowAggregation.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miShowAggregationActionPerformed(evt);
            }
        });
        miShowHide.add(miShowAggregation);
        miShowHide.add(jSeparator6);

        miShowFields.setSelected(true);
        miShowFields.setText("Fields");
        miShowFields.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miShowFieldsActionPerformed(evt);
            }
        });
        miShowHide.add(miShowFields);

        miShowMethods.setSelected(true);
        miShowMethods.setText("Methods");
        miShowMethods.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miShowMethodsActionPerformed(evt);
            }
        });
        miShowHide.add(miShowMethods);

        mDiagram.add(miShowHide);
        mDiagram.add(jSeparator5);

        miExportPNG.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/export_image.png"))); // NOI18N
        miExportPNG.setText("Export as PNG ...");
        miExportPNG.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miExportPNGActionPerformed(evt);
            }
        });
        mDiagram.add(miExportPNG);

        jMenuBar.add(mDiagram);

        mOptions.setText("Options");

        miEncoding.setText("Encoding");

        miEncodingUTF8.setSelected(true);
        miEncodingUTF8.setText("UTF-8");
        miEncodingUTF8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miEncodingUTF8ActionPerformed(evt);
            }
        });
        miEncoding.add(miEncodingUTF8);

        miEncodingWindows1252.setSelected(true);
        miEncodingWindows1252.setText("windows-1252");
        miEncodingWindows1252.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miEncodingWindows1252ActionPerformed(evt);
            }
        });
        miEncoding.add(miEncodingWindows1252);

        mOptions.add(miEncoding);

        miCompileOnTheFly.setSelected(true);
        miCompileOnTheFly.setText("Compile on-the-fly?");
        miCompileOnTheFly.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miCompileOnTheFlyActionPerformed(evt);
            }
        });
        mOptions.add(miCompileOnTheFly);

        miStructureHighlithningLEvel.setText("Structure Highlighting");

        syntaxHightlighterGroup.add(shOff);
        shOff.setSelected(true);
        shOff.setText("Off");
        shOff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shOffActionPerformed(evt);
            }
        });
        miStructureHighlithningLEvel.add(shOff);

        syntaxHightlighterGroup.add(shLight);
        shLight.setText("Light");
        shLight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shLightActionPerformed(evt);
            }
        });
        miStructureHighlithningLEvel.add(shLight);

        syntaxHightlighterGroup.add(shMedium);
        shMedium.setText("Medium");
        shMedium.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shMediumActionPerformed(evt);
            }
        });
        miStructureHighlithningLEvel.add(shMedium);

        syntaxHightlighterGroup.add(shStrong);
        shStrong.setText("Strong");
        shStrong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shStrongActionPerformed(evt);
            }
        });
        miStructureHighlithningLEvel.add(shStrong);

        syntaxHightlighterGroup.add(shVeryStrong);
        shVeryStrong.setText("Very Strong");
        shVeryStrong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shVeryStrongActionPerformed(evt);
            }
        });
        miStructureHighlithningLEvel.add(shVeryStrong);

        syntaxHightlighterGroup.add(shCostum);
        shCostum.setText("Custom");
        shCostum.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                shCostumActionPerformed(evt);
            }
        });
        miStructureHighlithningLEvel.add(shCostum);

        mOptions.add(miStructureHighlithningLEvel);

        chkRealtime.setText("Real-time object monitoring");
        chkRealtime.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkRealtimeActionPerformed(evt);
            }
        });
        mOptions.add(chkRealtime);

        chkHidePrivateFields.setText("Hide private fields in object monitor");
        chkHidePrivateFields.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkHidePrivateFieldsActionPerformed(evt);
            }
        });
        mOptions.add(chkHidePrivateFields);

        miDarkTheme.setText("Use dark theme");
        miDarkTheme.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	Theme theme;
				try {
					theme = Theme.load(getClass().getResourceAsStream("dark-theme.xml"));
					theme.apply(codeEditor.getCodeArea());
					codeEditor.getCodeArea().setHighlightCurrentLine(false);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				try {
					UIManager.setLookAndFeel(new DarculaLaf());
				} catch (UnsupportedLookAndFeelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
				
				updateFrame();
				
            }
        });
        
        
        
        mOptions.add(miDarkTheme);
        
        jMenuBar.add(mOptions);

        mHelp.setText("Help");

        miAbout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/ico_turtle.png"))); // NOI18N
        miAbout.setText("About");
        miAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miAboutActionPerformed(evt);
            }
        });
        mHelp.add(miAbout);

        miBootLogReport.setText("Boot Log Report");
        miBootLogReport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                miBootLogReportActionPerformed(evt);
            }
        });
        mHelp.add(miBootLogReport);

        jMenuBar.add(mHelp);

        setJMenuBar(jMenuBar);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(pnlBody, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(12, 12, 12)
                        .add(tbElements, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(34, 34, 34)
                        .add(tbFile, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(layout.createSequentialGroup()
                        .add(67, 67, 67)
                        .add(tbShow, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .add(19, 19, 19)
                .add(tbMake, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(51, 51, 51)
                .add(tbFontSize, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(182, Short.MAX_VALUE))
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(layout.createSequentialGroup()
                    .add(383, 383, 383)
                    .add(tbActions, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 100, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(383, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(tbElements, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(tbFile, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(5, 5, 5)
                        .add(tbShow, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(layout.createSequentialGroup()
                        .add(16, 16, 16)
                        .add(tbFontSize, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(layout.createSequentialGroup()
                        .add(17, 17, 17)
                        .add(tbMake, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .add(18, 18, 18)
                .add(pnlBody, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                .add(layout.createSequentialGroup()
                    .add(45, 45, 45)
                    .add(tbActions, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 26, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(556, Short.MAX_VALUE)))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void speAddClassActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speAddClassActionPerformed
    {//GEN-HEADEREND:event_speAddClassActionPerformed
        diagram.addClass();
    }//GEN-LAST:event_speAddClassActionPerformed

    private void speAddFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speAddFieldActionPerformed
    {//GEN-HEADEREND:event_speAddFieldActionPerformed
        diagram.addField();
    }//GEN-LAST:event_speAddFieldActionPerformed

    private void speAddMethodActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speAddMethodActionPerformed
    {//GEN-HEADEREND:event_speAddMethodActionPerformed
        diagram.addMethod();
}//GEN-LAST:event_speAddMethodActionPerformed

    private void diagramComponentResized(java.awt.event.ComponentEvent evt)//GEN-FIRST:event_diagramComponentResized
    {//GEN-HEADEREND:event_diagramComponentResized
    }//GEN-LAST:event_diagramComponentResized

    private void miAboutActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miAboutActionPerformed
    {//GEN-HEADEREND:event_miAboutActionPerformed
        diagram.about();
    }//GEN-LAST:event_miAboutActionPerformed

    private void miQuitActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miQuitActionPerformed
    {//GEN-HEADEREND:event_miQuitActionPerformed
        formWindowClosing(null);
    }//GEN-LAST:event_miQuitActionPerformed

    private void speAddConstructorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speAddConstructorActionPerformed
    {//GEN-HEADEREND:event_speAddConstructorActionPerformed
        diagram.addConstructor();
}//GEN-LAST:event_speAddConstructorActionPerformed

    private void speOpenActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speOpenActionPerformed
    {//GEN-HEADEREND:event_speOpenActionPerformed
        diagram.resetInteractiveProject();
        diagram.openUnimozer();
        setTitleNew();
}//GEN-LAST:event_speOpenActionPerformed

    private void speNewActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speNewActionPerformed
    {//GEN-HEADEREND:event_speNewActionPerformed
        diagram.resetInteractiveProject();
        diagram.newUnimozer();
        setTitleNew();
    }//GEN-LAST:event_speNewActionPerformed

    private void speSaveActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speSaveActionPerformed
    {//GEN-HEADEREND:event_speSaveActionPerformed
        diagram.saveUnimozer();
        setTitleNew();
    }//GEN-LAST:event_speSaveActionPerformed

    private void miNewActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miNewActionPerformed
    {//GEN-HEADEREND:event_miNewActionPerformed
        diagram.resetInteractiveProject();
        diagram.newUnimozer();
        setTitleNew();
    }//GEN-LAST:event_miNewActionPerformed

    private void miOpenActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miOpenActionPerformed
    {//GEN-HEADEREND:event_miOpenActionPerformed
        diagram.resetInteractiveProject();
        diagram.openUnimozer();
        setTitleNew();
    }//GEN-LAST:event_miOpenActionPerformed

    private void miSaveActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miSaveActionPerformed
    {//GEN-HEADEREND:event_miSaveActionPerformed
        diagram.saveUnimozer();
        setTitleNew();
    }//GEN-LAST:event_miSaveActionPerformed

    private void miSaveAsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miSaveAsActionPerformed
    {//GEN-HEADEREND:event_miSaveAsActionPerformed
        diagram.saveAsUnimozer();
        setTitleNew();
    }//GEN-LAST:event_miSaveAsActionPerformed

    private void miAddClassActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miAddClassActionPerformed
    {//GEN-HEADEREND:event_miAddClassActionPerformed
        diagram.addClass();
    }//GEN-LAST:event_miAddClassActionPerformed

    private void miAddFieldActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miAddFieldActionPerformed
    {//GEN-HEADEREND:event_miAddFieldActionPerformed
        if(miAllowEdit.isSelected()) diagram.addField();
    }//GEN-LAST:event_miAddFieldActionPerformed

    private void miAddConstructorActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miAddConstructorActionPerformed
    {//GEN-HEADEREND:event_miAddConstructorActionPerformed
        if(miAllowEdit.isSelected()) diagram.addConstructor();
    }//GEN-LAST:event_miAddConstructorActionPerformed

    private void miAddMethodActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miAddMethodActionPerformed
    {//GEN-HEADEREND:event_miAddMethodActionPerformed
        if(miAllowEdit.isSelected()) diagram.addMethod();
    }//GEN-LAST:event_miAddMethodActionPerformed

    private void miExportPNGActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miExportPNGActionPerformed
    {//GEN-HEADEREND:event_miExportPNGActionPerformed
        diagram.exportPNG();
    }//GEN-LAST:event_miExportPNGActionPerformed

    private void miClipboardPNGActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miClipboardPNGActionPerformed
    {//GEN-HEADEREND:event_miClipboardPNGActionPerformed
        //Console.disconnectErr();
        diagram.copyToClipboardPNG();
        //Console.connectErr();
    }//GEN-LAST:event_miClipboardPNGActionPerformed

    private void popClearActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_popClearActionPerformed
    {//GEN-HEADEREND:event_popClearActionPerformed
        console.clear();
}//GEN-LAST:event_popClearActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosed
    {//GEN-HEADEREND:event_formWindowClosed
    }//GEN-LAST:event_formWindowClosed

    private void miCompileActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miCompileActionPerformed
    {//GEN-HEADEREND:event_miCompileActionPerformed
        console.clear();
        diagram.compile();
    }//GEN-LAST:event_miCompileActionPerformed

    private void miMakeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miMakeActionPerformed
    {//GEN-HEADEREND:event_miMakeActionPerformed
        console.clear();
        diagram.make();
    }//GEN-LAST:event_miMakeActionPerformed

    private void speCompileActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speCompileActionPerformed
    {//GEN-HEADEREND:event_speCompileActionPerformed
        console.clear();
        diagram.compile();
}//GEN-LAST:event_speCompileActionPerformed

    private void speMakeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speMakeActionPerformed
    {//GEN-HEADEREND:event_speMakeActionPerformed
        console.clear();
        diagram.make();
}//GEN-LAST:event_speMakeActionPerformed

    private void miJarActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miJarActionPerformed
    {//GEN-HEADEREND:event_miJarActionPerformed
       diagram.jar();
    }//GEN-LAST:event_miJarActionPerformed

    private void speJarActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speJarActionPerformed
    {//GEN-HEADEREND:event_speJarActionPerformed
        diagram.jar();
}//GEN-LAST:event_speJarActionPerformed

    private void speRunActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speRunActionPerformed
    {//GEN-HEADEREND:event_speRunActionPerformed
        diagram.run();
}//GEN-LAST:event_speRunActionPerformed

    private void miRunFastActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miRunFastActionPerformed
    {//GEN-HEADEREND:event_miRunFastActionPerformed
        if(diagram.getInteractiveProject()==null)
            diagram.runFast();
        else
        {
            diagram.getInteractiveProject().setObjectizer(objectizer);
            diagram.getInteractiveProject().runProject();
        }
    }//GEN-LAST:event_miRunFastActionPerformed

    private void speCleanActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speCleanActionPerformed
    {//GEN-HEADEREND:event_speCleanActionPerformed
        diagram.clean();
}//GEN-LAST:event_speCleanActionPerformed

    private void miCleanActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miCleanActionPerformed
    {//GEN-HEADEREND:event_miCleanActionPerformed
        diagram.clean();
    }//GEN-LAST:event_miCleanActionPerformed

    private void miToolbarFileActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miToolbarFileActionPerformed
    {//GEN-HEADEREND:event_miToolbarFileActionPerformed
        updateToolbars();
    }//GEN-LAST:event_miToolbarFileActionPerformed

    private void miToolbarUMLActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miToolbarUMLActionPerformed
    {//GEN-HEADEREND:event_miToolbarUMLActionPerformed
        updateToolbars();
    }//GEN-LAST:event_miToolbarUMLActionPerformed

    private void miToolbarRunActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miToolbarRunActionPerformed
    {//GEN-HEADEREND:event_miToolbarRunActionPerformed
        updateToolbars();
    }//GEN-LAST:event_miToolbarRunActionPerformed

    private void speCommandActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speCommandActionPerformed
    {//GEN-HEADEREND:event_speCommandActionPerformed
        diagram.command();
}//GEN-LAST:event_speCommandActionPerformed

    private void miCommandActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miCommandActionPerformed
    {//GEN-HEADEREND:event_miCommandActionPerformed
        diagram.command();
    }//GEN-LAST:event_miCommandActionPerformed

    private void miShowHeritageActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miShowHeritageActionPerformed
    {//GEN-HEADEREND:event_miShowHeritageActionPerformed
        updateDiagramElements();
}//GEN-LAST:event_miShowHeritageActionPerformed

    private void miShowAggregationActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miShowAggregationActionPerformed
    {//GEN-HEADEREND:event_miShowAggregationActionPerformed
        updateDiagramElements();
}//GEN-LAST:event_miShowAggregationActionPerformed

    private void miShowCompositionActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miShowCompositionActionPerformed
    {//GEN-HEADEREND:event_miShowCompositionActionPerformed
        updateDiagramElements();
}//GEN-LAST:event_miShowCompositionActionPerformed

    private void speJavaDocActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speJavaDocActionPerformed
    {//GEN-HEADEREND:event_speJavaDocActionPerformed
        diagram.javadoc();
}//GEN-LAST:event_speJavaDocActionPerformed

    private void miJavaDocActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miJavaDocActionPerformed
    {//GEN-HEADEREND:event_miJavaDocActionPerformed
        diagram.javadoc();
}//GEN-LAST:event_miJavaDocActionPerformed

    private void miAllowEditActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miAllowEditActionPerformed
    {//GEN-HEADEREND:event_miAllowEditActionPerformed
        /*
        //name = JOptionPane.showInputDialog(frame, "Please enter the name for you new instance of “"+mouseClass.getShortName()+"“", "Create object", JOptionPane.QUESTION_MESSAGE);
        if(miAllowEdit.isSelected()==true)
        { // ugeschallt
            int res = JOptionPane.showConfirmDialog(this,
                                          "You have activated to edit the diagram directely.\n"+
                                          "This means that your source code will be parsed and\n"+
                                          "rebuild and thus that a lot of comments will probably\n"+
                                          "be lost!\n"+
                                          "\n"+
                                          "Do you still want to continue?",
                                          "Allow editing the diagram?",
                                          JOptionPane.YES_NO_OPTION,
                                          JOptionPane.QUESTION_MESSAGE
                                          );
           if(res==JOptionPane.NO_OPTION) miAllowEdit.setSelected(false);
        }
        else
        { // ausgeschallt
            int res = JOptionPane.showConfirmDialog(this,
                                          "You have disabled the direct diagram editor. This allows\n"+
                                          "you to add more comments inside your source code but you\n"+
                                          "can no longer edit the diagram directly.\n"+
                                          "\n"+
                                          "Once this options is activated for a project, it is heavily\n"+
                                          "recommended to not switch it on again!\n"+
                                          "\n"+
                                          "Do you still want to continue?",
                                          "Allow editing the diagram?",
                                          JOptionPane.YES_NO_OPTION,
                                          JOptionPane.QUESTION_MESSAGE
                                          );
           if(res==JOptionPane.NO_OPTION) miAllowEdit.setSelected(true);
        }
        setButtons(speAddClass.isEnabled());
         */
    }//GEN-LAST:event_miAllowEditActionPerformed

    private void speFontDownActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speFontDownActionPerformed
    {//GEN-HEADEREND:event_speFontDownActionPerformed
       if(Unimozer.DRAW_FONT_SIZE>10) Unimozer.DRAW_FONT_SIZE--;
       codeEditor.setFontSize(Unimozer.DRAW_FONT_SIZE);
       diagram.repaint();
}//GEN-LAST:event_speFontDownActionPerformed

    private void speFontUpActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speFontUpActionPerformed
    {//GEN-HEADEREND:event_speFontUpActionPerformed
       if(Unimozer.DRAW_FONT_SIZE<30) Unimozer.DRAW_FONT_SIZE++;
       codeEditor.setFontSize(Unimozer.DRAW_FONT_SIZE);
       diagram.repaint();
}//GEN-LAST:event_speFontUpActionPerformed

    private void miToolbarFontActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miToolbarFontActionPerformed
    {//GEN-HEADEREND:event_miToolbarFontActionPerformed
        updateToolbars();
}//GEN-LAST:event_miToolbarFontActionPerformed

    private void aDiagramStandardUML(java.awt.event.ActionEvent evt)//GEN-FIRST:event_aDiagramStandardUML
    {//GEN-HEADEREND:event_aDiagramStandardUML
        miDiagramStandardUML.setSelected(true);
        miDiagramStandardJava.setSelected(false);
        diagram.setUML(true);
        diagram.repaint();
        objectizer.repaint();
    }//GEN-LAST:event_aDiagramStandardUML

    private void aDiagramStandardJava(java.awt.event.ActionEvent evt)//GEN-FIRST:event_aDiagramStandardJava
    {//GEN-HEADEREND:event_aDiagramStandardJava
        miDiagramStandardUML.setSelected(false);
        miDiagramStandardJava.setSelected(true);
        diagram.setUML(false);
        diagram.repaint();
        objectizer.repaint();
    }//GEN-LAST:event_aDiagramStandardJava

    public void closeWindow()
    {
        /*
         * Save settings to the INI file
         */
        try
        {
            objectizer.removeAllObjects();
            Ini ini = Ini.getInstance();
            ini.load();
            ini.setProperty("umlStandard", Boolean.toString(diagram.isUML()));
            ini.setProperty("toolbarFile", Boolean.toString(miToolbarFile.isSelected()));
            ini.setProperty("toolbarFont", Boolean.toString(miToolbarFont.isSelected()));
            ini.setProperty("toolbarRun", Boolean.toString(miToolbarRun.isSelected()));
            ini.setProperty("toolbarUML", Boolean.toString(miToolbarUML.isSelected()));
            ini.setProperty("toolbarShow", Boolean.toString(miToolbarShow.isSelected()));
            //ini.setProperty("showComments", Boolean.toString(miShowJavadoc.isSelected()));
            ini.setProperty("lastDirename", diagram.getContainingDirectoryName());
            ini.setProperty("defaultEncoding", Unimozer.FILE_ENCODING);
            // window
            // position
            ini.setProperty("top",Integer.toString(getLocationOnScreen().x));
            ini.setProperty("left",Integer.toString(getLocationOnScreen().y));
            ini.setProperty("width",Integer.toString(getWidth()));
            ini.setProperty("height",Integer.toString(getHeight()));
            // sliders
            ini.setProperty("splitty_1",Integer.toString(splitty_1.getDividerLocation()));
            ini.setProperty("splitty_2",Integer.toString(splitty_2.getDividerLocation()));
            ini.setProperty("splitty_3",Integer.toString(splitty_3.getDividerLocation()));
            ini.setProperty("splitty_4",Integer.toString(bottomSplitter.getDividerLocation()));
            ini.setProperty("splitty_5",Integer.toString(codeEditor.jsp.getDividerLocation()));
            // font size
            ini.setProperty("fontSize", Integer.toString(Unimozer.DRAW_FONT_SIZE));

            ini.save();
        }
        catch (FileNotFoundException ex)
        {
            MyError.display(ex);
        }
        catch (IOException ex)
        {
            MyError.display(ex);
        }

        /*
         * Save diagram?
         */
        if(diagram.isChanged())
        {
            if(diagram.askToSave()) System.exit(0);
        }
        else System.exit(0);
    }

    private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
    {//GEN-HEADEREND:event_formWindowClosing
        closeWindow();
    }//GEN-LAST:event_formWindowClosing
 
    public Console getConsole()
    {
        return console;
    }

    private void consoleMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_consoleMouseClicked
    {//GEN-HEADEREND:event_consoleMouseClicked
        if(evt.getButton()==MouseEvent.BUTTON3) outPopup.show(console, evt.getX(), evt.getY());
    }//GEN-LAST:event_consoleMouseClicked

    private void miEncodingUTF8ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miEncodingUTF8ActionPerformed
    {//GEN-HEADEREND:event_miEncodingUTF8ActionPerformed
        miEncodingUTF8.setSelected(true);
        miEncodingWindows1252.setSelected(false);
        Unimozer.FILE_ENCODING="UTF-8";
    }//GEN-LAST:event_miEncodingUTF8ActionPerformed

    private void miEncodingWindows1252ActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miEncodingWindows1252ActionPerformed
    {//GEN-HEADEREND:event_miEncodingWindows1252ActionPerformed
        miEncodingWindows1252.setSelected(true);
        miEncodingUTF8.setSelected(false);
        Unimozer.FILE_ENCODING="windows-1252";
    }//GEN-LAST:event_miEncodingWindows1252ActionPerformed

    private void miPrintDiagramActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miPrintDiagramActionPerformed
    {//GEN-HEADEREND:event_miPrintDiagramActionPerformed
        diagram.printDiagram();
    }//GEN-LAST:event_miPrintDiagramActionPerformed

    private void miCompileOnTheFlyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miCompileOnTheFlyActionPerformed
    {//GEN-HEADEREND:event_miCompileOnTheFlyActionPerformed
        Unimozer.javaCompileOnTheFly=miCompileOnTheFly.isSelected();
        Ini.set("compileOnTheFly",Boolean.toString(miCompileOnTheFly.isSelected()));
    }//GEN-LAST:event_miCompileOnTheFlyActionPerformed

    private void miAddFileActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miAddFileActionPerformed
    {//GEN-HEADEREND:event_miAddFileActionPerformed
        diagram.loadClassFromFile();
    }//GEN-LAST:event_miAddFileActionPerformed

    private void miReplaceActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miReplaceActionPerformed
    {//GEN-HEADEREND:event_miReplaceActionPerformed
        codeEditor.doReplace();
    }//GEN-LAST:event_miReplaceActionPerformed

    private void miFindActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miFindActionPerformed
    {//GEN-HEADEREND:event_miFindActionPerformed
        codeEditor.doFind();
    }//GEN-LAST:event_miFindActionPerformed

    private void miFindAgainActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miFindAgainActionPerformed
    {//GEN-HEADEREND:event_miFindAgainActionPerformed
        codeEditor.doFindAgain();
    }//GEN-LAST:event_miFindAgainActionPerformed

    private void miShowMethodsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miShowMethodsActionPerformed
    {//GEN-HEADEREND:event_miShowMethodsActionPerformed
        updateDiagramElements();
    }//GEN-LAST:event_miShowMethodsActionPerformed

    private void miShowFieldsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miShowFieldsActionPerformed
    {//GEN-HEADEREND:event_miShowFieldsActionPerformed
        updateDiagramElements();
    }//GEN-LAST:event_miShowFieldsActionPerformed

    private void speShowHeritageActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speShowHeritageActionPerformed
    {//GEN-HEADEREND:event_speShowHeritageActionPerformed
        miShowHeritage.setSelected(speShowHeritage.isSelected());
        updateDiagramElements();
    }//GEN-LAST:event_speShowHeritageActionPerformed

    private void speShowCompositionActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speShowCompositionActionPerformed
    {//GEN-HEADEREND:event_speShowCompositionActionPerformed
        miShowComposition.setSelected(speShowComposition.isSelected());
        updateDiagramElements();
    }//GEN-LAST:event_speShowCompositionActionPerformed

    private void speShowAggregationActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speShowAggregationActionPerformed
    {//GEN-HEADEREND:event_speShowAggregationActionPerformed
        miShowAggregation.setSelected(speShowAggregation.isSelected());
        updateDiagramElements();
    }//GEN-LAST:event_speShowAggregationActionPerformed

    private void speShowFieldsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speShowFieldsActionPerformed
    {//GEN-HEADEREND:event_speShowFieldsActionPerformed
        miShowFields.setSelected(speShowFields.isSelected());
        updateDiagramElements();
    }//GEN-LAST:event_speShowFieldsActionPerformed

    private void speShowMethodsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speShowMethodsActionPerformed
    {//GEN-HEADEREND:event_speShowMethodsActionPerformed
        miShowMethods.setSelected(speShowMethods.isSelected());
        updateDiagramElements();
    }//GEN-LAST:event_speShowMethodsActionPerformed

    private void miToolbarShowActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miToolbarShowActionPerformed
    {//GEN-HEADEREND:event_miToolbarShowActionPerformed
        updateToolbars();
    }//GEN-LAST:event_miToolbarShowActionPerformed

    private void miUndoActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miUndoActionPerformed
    {//GEN-HEADEREND:event_miUndoActionPerformed
        codeEditor.editorUndo();
    }//GEN-LAST:event_miUndoActionPerformed

    private void miRedoActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miRedoActionPerformed
    {//GEN-HEADEREND:event_miRedoActionPerformed
        codeEditor.editorRedo();
    }//GEN-LAST:event_miRedoActionPerformed

    private void miCutActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miCutActionPerformed
    {//GEN-HEADEREND:event_miCutActionPerformed
        codeEditor.editorCut();
    }//GEN-LAST:event_miCutActionPerformed

    private void miCopyActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miCopyActionPerformed
    {//GEN-HEADEREND:event_miCopyActionPerformed
        codeEditor.editorCopy();
    }//GEN-LAST:event_miCopyActionPerformed

    private void miPasteActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miPasteActionPerformed
    {//GEN-HEADEREND:event_miPasteActionPerformed
        codeEditor.editorPaste();
    }//GEN-LAST:event_miPasteActionPerformed

    private void codeEditorFocusLost(java.awt.event.FocusEvent evt)//GEN-FIRST:event_codeEditorFocusLost
    {//GEN-HEADEREND:event_codeEditorFocusLost
      
    }//GEN-LAST:event_codeEditorFocusLost

    private void miClipboardColoredCodeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miClipboardColoredCodeActionPerformed
    {//GEN-HEADEREND:event_miClipboardColoredCodeActionPerformed
        codeEditor.copyAdRtf();
    }//GEN-LAST:event_miClipboardColoredCodeActionPerformed

    private void speModeSelectActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speModeSelectActionPerformed
    {//GEN-HEADEREND:event_speModeSelectActionPerformed
        diagram.setMode(Diagram.MODE_SELECT);
    }//GEN-LAST:event_speModeSelectActionPerformed

    private void speModeExtendsActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speModeExtendsActionPerformed
    {//GEN-HEADEREND:event_speModeExtendsActionPerformed
        diagram.setMode(Diagram.MODE_EXTENDS);
    }//GEN-LAST:event_speModeExtendsActionPerformed

    private void speStopActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_speStopActionPerformed
    {//GEN-HEADEREND:event_speStopActionPerformed
        objectizer.stopAllThreads();
        objectizer.removeAllObjects();
        console.clear();
        diagram.cleanAll();
    }//GEN-LAST:event_speStopActionPerformed

    private void miStopActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miStopActionPerformed
    {//GEN-HEADEREND:event_miStopActionPerformed
        objectizer.stopAllThreads();
        objectizer.removeAllObjects();
        console.clear();
        diagram.cleanAll();
    }//GEN-LAST:event_miStopActionPerformed

    private void setSyntaxHighLight(int value, boolean status)
    {
        RSyntaxTextAreaUIBackgroundDrawer.setActive(status);
        RSyntaxTextAreaUIBackgroundDrawer.setSaturation(value);

        Ini.set("structureHighlithningSaturation",String.valueOf(RSyntaxTextAreaUIBackgroundDrawer.getSaturation()));
        Ini.set("structureHighlithning",Boolean.toString(status));
        
        codeEditor.repaint();
    }
    
    private void shCostumActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_shCostumActionPerformed
    {//GEN-HEADEREND:event_shCostumActionPerformed
        //String sat = JOptionPane.showInputDialog(this, "What color saturation do you want to use?\nPlease enter a value between 0 (light) and 80 (dark).", RSyntaxTextAreaUIBackgroundDrawer.getSaturation());
        //setSyntaxHighLight(Integer.valueOf(sat),true);
        JSliderOnJOptionPane.changeListener = new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e)
            {
                setSyntaxHighLight(((JSlider) e.getSource()).getValue(),true);
            }
        };
        int sat = JSliderOnJOptionPane.showInputDialog(this, "Structure Highlighting ", "What color saturation do you want to use?", 0, 80, RSyntaxTextAreaUIBackgroundDrawer.getSaturation());
        setSyntaxHighLight(sat,true);
    }//GEN-LAST:event_shCostumActionPerformed

    private void shOffActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_shOffActionPerformed
    {//GEN-HEADEREND:event_shOffActionPerformed
        setSyntaxHighLight(RSyntaxTextAreaUIBackgroundDrawer.getSaturation(),false);
    }//GEN-LAST:event_shOffActionPerformed

    private void shLightActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_shLightActionPerformed
    {//GEN-HEADEREND:event_shLightActionPerformed
        setSyntaxHighLight(0,true);
    }//GEN-LAST:event_shLightActionPerformed

    private void shMediumActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_shMediumActionPerformed
    {//GEN-HEADEREND:event_shMediumActionPerformed
        setSyntaxHighLight(10,true);
    }//GEN-LAST:event_shMediumActionPerformed

    private void shStrongActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_shStrongActionPerformed
    {//GEN-HEADEREND:event_shStrongActionPerformed
        setSyntaxHighLight(20,true);
    }//GEN-LAST:event_shStrongActionPerformed

    private void shVeryStrongActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_shVeryStrongActionPerformed
    {//GEN-HEADEREND:event_shVeryStrongActionPerformed
        setSyntaxHighLight(30,true);
    }//GEN-LAST:event_shVeryStrongActionPerformed

    private void miBootLogReportActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_miBootLogReportActionPerformed
    {//GEN-HEADEREND:event_miBootLogReportActionPerformed
        BootLogReport blr = new BootLogReport(this);
        blr.setVisible(true);
    }//GEN-LAST:event_miBootLogReportActionPerformed

    private void chkRealtimeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_chkRealtimeActionPerformed
    {//GEN-HEADEREND:event_chkRealtimeActionPerformed
        if(chkRealtime.isSelected())
        {
            objectizer.setAutoRefresh(200);
        }
        else
        {
            objectizer.setAutoRefresh(0);
        }
    }//GEN-LAST:event_chkRealtimeActionPerformed

    private void formWindowActivated(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowActivated
    {//GEN-HEADEREND:event_formWindowActivated
        diagram.addNewFilesAndReloadExistingSavedFiles();
    }//GEN-LAST:event_formWindowActivated

    private void speRunFastActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_speRunFastActionPerformed
        if(diagram.getInteractiveProject()==null)
            diagram.runFast();
        else
        {
            diagram.getInteractiveProject().setObjectizer(objectizer);
            diagram.getInteractiveProject().runProject();
        }
    }//GEN-LAST:event_speRunFastActionPerformed

    private void miRunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miRunActionPerformed
        diagram.run();
    }//GEN-LAST:event_miRunActionPerformed

    private void miNewVisualizerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miNewVisualizerActionPerformed
        diagram.resetInteractiveProject();
        NewInteractiveProjectDialog dg = new NewInteractiveProjectDialog(this, rootPaneCheckingEnabled, diagram);
        dg.setVisible(true);
    }//GEN-LAST:event_miNewVisualizerActionPerformed

    private void miCreateInteractiveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_miCreateInteractiveActionPerformed
        CreateInteractiveProjectDialog dg = new CreateInteractiveProjectDialog(this, rootPaneCheckingEnabled, diagram);
        dg.setVisible(true);
    }//GEN-LAST:event_miCreateInteractiveActionPerformed

    private void chkHidePrivateFieldsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkHidePrivateFieldsActionPerformed
        if(chkHidePrivateFields.isSelected())
        {
            objectizer.setHidePrivateFields(true);
        }
        else
        {
            objectizer.setHidePrivateFields(false);
        }
        objectizer.repaint();
        Ini.set("hidePrivateFields",Boolean.toString(chkHidePrivateFields.isSelected()));
    }//GEN-LAST:event_chkHidePrivateFieldsActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup actionGroup;
    private javax.swing.JPanel bottomPanel;
    private javax.swing.JSplitPane bottomSplitter;
    private javax.swing.JLabel callingLabel;
    private javax.swing.JCheckBoxMenuItem chkHidePrivateFields;
    private javax.swing.JCheckBoxMenuItem chkRealtime;
    private lu.fisch.unimozer.CodeEditor codeEditor;
    lu.fisch.unimozer.console.Console console;
    private javax.swing.JScrollPane consoleScroller;
    public lu.fisch.unimozer.Diagram diagram;
    private javax.swing.JMenuBar jMenuBar;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator10;
    private javax.swing.JPopupMenu.Separator jSeparator11;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JPopupMenu.Separator jSeparator8;
    private javax.swing.JPopupMenu.Separator jSeparator9;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JMenu mDiagram;
    private javax.swing.JMenu mEdit;
    private javax.swing.JMenu mFile;
    private javax.swing.JMenu mHelp;
    private javax.swing.JMenu mOptions;
    private javax.swing.JMenu mProject;
    private javax.swing.JMenu mView;
    private javax.swing.JMenuItem miAbout;
    private javax.swing.JMenuItem miAddClass;
    private javax.swing.JMenuItem miAddConstructor;
    private javax.swing.JMenuItem miAddField;
    private javax.swing.JMenuItem miAddFile;
    private javax.swing.JMenuItem miAddMethod;
    private javax.swing.JCheckBoxMenuItem miAllowEdit;
    private javax.swing.JMenuItem miBootLogReport;
    private javax.swing.JMenuItem miClean;
    private javax.swing.JMenuItem miClipboardColoredCode;
    private javax.swing.JMenuItem miClipboardPNG;
    private javax.swing.JMenuItem miCommand;
    private javax.swing.JMenuItem miCompile;
    private javax.swing.JCheckBoxMenuItem miCompileOnTheFly;
    private javax.swing.JMenuItem miCopy;
    private javax.swing.JMenuItem miCreateInteractive;
    private javax.swing.JMenuItem miCut;
    private javax.swing.JMenu miDiagramStandard;
    private javax.swing.JCheckBoxMenuItem miDiagramStandardJava;
    private javax.swing.JCheckBoxMenuItem miDiagramStandardUML;
    private javax.swing.JMenu miEncoding;
    private javax.swing.JRadioButtonMenuItem miEncodingUTF8;
    private javax.swing.JRadioButtonMenuItem miEncodingWindows1252;
    private javax.swing.JCheckBoxMenuItem miDarkTheme;
    private javax.swing.JMenuItem miExportPNG;
    private javax.swing.JMenuItem miFind;
    private javax.swing.JMenuItem miFindAgain;
    private javax.swing.JMenuItem miJar;
    private javax.swing.JMenuItem miJavaDoc;
    private javax.swing.JMenuItem miMake;
    private javax.swing.JMenuItem miNew;
    private javax.swing.JMenuItem miNewVisualizer;
    private javax.swing.JMenuItem miOpen;
    private javax.swing.JMenuItem miPaste;
    private javax.swing.JMenuItem miPrintDiagram;
    private javax.swing.JMenuItem miQuit;
    private javax.swing.JMenuItem miRedo;
    private javax.swing.JMenuItem miReplace;
    private javax.swing.JMenuItem miRun;
    private javax.swing.JMenuItem miRunFast;
    private javax.swing.JMenuItem miSave;
    private javax.swing.JMenuItem miSaveAs;
    private javax.swing.JSeparator miSepAllowEditing;
    private javax.swing.JCheckBoxMenuItem miShowAggregation;
    private javax.swing.JCheckBoxMenuItem miShowComposition;
    private javax.swing.JCheckBoxMenuItem miShowFields;
    private javax.swing.JCheckBoxMenuItem miShowHeritage;
    private javax.swing.JMenu miShowHide;
    private javax.swing.JCheckBoxMenuItem miShowMethods;
    private javax.swing.JMenuItem miStop;
    private javax.swing.JMenu miStructureHighlithningLEvel;
    private javax.swing.JCheckBoxMenuItem miToolbarFile;
    private javax.swing.JCheckBoxMenuItem miToolbarFont;
    private javax.swing.JCheckBoxMenuItem miToolbarRun;
    private javax.swing.JCheckBoxMenuItem miToolbarShow;
    private javax.swing.JCheckBoxMenuItem miToolbarUML;
    private javax.swing.JMenu miToolbars;
    private javax.swing.JMenuItem miUndo;
    private lu.fisch.unimozer.Objectizer objectizer;
    private javax.swing.JPopupMenu outPopup;
    private javax.swing.JPanel pnlBody;
    private javax.swing.JMenuItem popClear;
    private javax.swing.JScrollPane scrollDiagram;
    private javax.swing.JRadioButtonMenuItem shCostum;
    private javax.swing.JRadioButtonMenuItem shLight;
    private javax.swing.JRadioButtonMenuItem shMedium;
    private javax.swing.JRadioButtonMenuItem shOff;
    private javax.swing.JRadioButtonMenuItem shStrong;
    private javax.swing.JRadioButtonMenuItem shVeryStrong;
    private javax.swing.JScrollPane spNSD;
    private javax.swing.JButton speAddClass;
    private javax.swing.JButton speAddConstructor;
    private javax.swing.JButton speAddField;
    private javax.swing.JButton speAddMethod;
    private javax.swing.JButton speClean;
    private javax.swing.JButton speCommand;
    private javax.swing.JButton speCompile;
    private javax.swing.JButton speFontDown;
    private javax.swing.JButton speFontUp;
    private javax.swing.JButton speJar;
    private javax.swing.JButton speJavaDoc;
    private javax.swing.JButton speMake;
    private javax.swing.JToggleButton speModeExtends;
    private javax.swing.JToggleButton speModeSelect;
    private javax.swing.JButton speNew;
    private javax.swing.JButton speOpen;
    private javax.swing.JButton speRun;
    private javax.swing.JButton speRunFast;
    private javax.swing.JButton speSave;
    private javax.swing.JToggleButton speShowAggregation;
    private javax.swing.JToggleButton speShowComposition;
    private javax.swing.JToggleButton speShowFields;
    private javax.swing.JToggleButton speShowHeritage;
    private javax.swing.JToggleButton speShowMethods;
    private javax.swing.JButton speStop;
    private javax.swing.JSplitPane splitty_1;
    private javax.swing.JSplitPane splitty_2;
    private javax.swing.JSplitPane splitty_3;
    private javax.swing.ButtonGroup syntaxHightlighterGroup;
    private javax.swing.JToolBar tbActions;
    private javax.swing.JToolBar tbElements;
    private javax.swing.JToolBar tbFile;
    private javax.swing.JToolBar tbFontSize;
    private javax.swing.JToolBar tbMake;
    private javax.swing.JToolBar tbShow;
    // End of variables declaration//GEN-END:variables

    void setEnabledActions(boolean b)
    {
        // diss^able menu items
        miCompile.setEnabled(b);
        miMake.setEnabled(b);
        miRun.setEnabled(b);
        miRunFast.setEnabled(b);
        miClean.setEnabled(b);
        miJar.setEnabled(b);
        miJavaDoc.setEnabled(b);
        miCommand.setEnabled(b);
        miAddClass.setEnabled(b);
        miAddConstructor.setEnabled(b);
        miAddMethod.setEnabled(b);
        miAddField.setEnabled(b);
        miSave.setEnabled(b);
        miSaveAs.setEnabled(b);
        miAddFile.setEnabled(b);
        // dissable buttons
        speCompile.setEnabled(b);
        speMake.setEnabled(b);
        speRun.setEnabled(b);
        speRunFast.setEnabled(b);
        speClean.setEnabled(b);
        speJar.setEnabled(b);
        speJavaDoc.setEnabled(b);
        speCommand.setEnabled(b);
        speAddClass.setEnabled(b);
        speAddConstructor.setEnabled(b);
        speAddMethod.setEnabled(b);
        speAddField.setEnabled(b);
        speSave.setEnabled(b);
    }

    public void setEnabledEditorActions(boolean b)
    {
        miFind.setEnabled(b);
        miFindAgain.setEnabled(b);
        miReplace.setEnabled(b);
        miUndo.setEnabled(b);
        miRedo.setEnabled(b);
        miCut.setEnabled(b);
        miCopy.setEnabled(b);
        miPaste.setEnabled(b);
        miClipboardColoredCode.setEnabled(b);
    }

    public void setButtons(boolean b)
    {
        //System.err.println("Setting buttons to "+Boolean.toString(b));
        //b = b && miAllowEdit.isSelected();

        //diagram.setAllowEdit(miAllowEdit.isSelected());

        speAddField.setEnabled(b);
        speAddConstructor.setEnabled(b);
        speAddMethod.setEnabled(b);

        miAddField.setEnabled(b);
        miAddConstructor.setEnabled(b);
        miAddMethod.setEnabled(b);

        setTitleNew();
    }
     

    void setCompilationErrors(Vector<CompilationError> errors)
    {
        if(errors.isEmpty())
        {
            consoleScroller.invalidate();
            consoleScroller.remove(console);
            if(errorList!=null) consoleScroller.remove(errorList);
            console.setVisible(true);
            if(errorList!=null) errorList.setVisible(false);
            consoleScroller.add(console);
            consoleScroller.setViewportView(console);
            consoleScroller.validate();
            consoleScroller.repaint();
        }
        else
        {
            consoleScroller.invalidate();
            consoleScroller.remove(console);
            if(errorList!=null) consoleScroller.remove(errorList);
            errorList = new JList(errors);
            console.setVisible(false);
            errorList.setVisible(true);
            errorList.setBackground(codeEditor.DEFAULT_COLOR);
            errorList.setForeground(Color.red);
            errorList.setFont(new Font("Monospaced",Font.BOLD,12));

            final Vector<CompilationError> finalErrors = errors;
            errorList.addMouseListener(new MouseListener() {

                public void mouseClicked(MouseEvent me)
                {
                    CompilationError error = finalErrors.get(errorList.getSelectedIndex());
                    MyClass errorClass = diagram.getClass(error.getClassName().replaceAll("/", "."));
                    diagram.selectClass(errorClass);
                    diagram.updateEditor();
                    codeEditor.highlightError(error);
                }

                @Override
                public void mousePressed(MouseEvent me)
                {
                }

                @Override
                public void mouseReleased(MouseEvent me)
                {
                }

                @Override
                public void mouseEntered(MouseEvent me)
                {
                    }

                @Override
                public void mouseExited(MouseEvent me)
                {
                }
            });

            consoleScroller.add(errorList);
            consoleScroller.setViewportView(errorList);
            consoleScroller.validate();
            consoleScroller.repaint();
        }
    }

    private String getInitialDir()
    {
        String home = null;
        if(System.getProperty("home")!=null)
        {
            home = System.getProperty("home");
            // get actual environment variables
            Map env = System.getenv();
            Vector keys = new Vector(env.keySet());
            for(int i=0;i<keys.size();i++)
            {
                // do substitution
                home=home.replace("%"+keys.get(i)+"%", (CharSequence) env.get(keys.get(i)));
            }
            return home;
        }
        else if(!Ini.get("home","").equals(""))
        {
            home = Ini.get("home","");
            // get actual envoronment variables
            Map env = System.getenv();
            Vector keys = new Vector(env.keySet());
            for(int i=0;i<keys.size();i++)
            {
                // do substitution
                home=home.replace("%"+keys.get(i)+"%", (CharSequence) env.get(keys.get(i)));
            }
            return home;
        }
        else return Ini.get("lastDirename",System.getProperty("user.home"));
    }
    
    public void updateFrame () {
    	SwingUtilities.updateComponentTreeUI(this);
    }

    
    public static void main(String[] args)
    {
        final Mainform mainform = new Mainform();
        mainform.setVisible(true);
    }
}
