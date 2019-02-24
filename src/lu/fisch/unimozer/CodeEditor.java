/*
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import lu.fisch.unimozer.compilation.CompilationError;
import lu.fisch.unimozer.console.Console;
import lu.fisch.unimozer.dialogs.FindDialog;
import lu.fisch.unimozer.dialogs.ReplaceDialog;
import lu.fisch.unimozer.utils.StringList;
import org.fife.rsta.ac.LanguageSupport;
import org.fife.rsta.ac.LanguageSupportFactory;
import org.fife.rsta.ac.java.JarManager;
import org.fife.rsta.ac.java.JavaLanguageSupport;
import org.fife.rsta.ac.java.JavaParser;
import org.fife.rsta.ac.java.rjc.ast.ASTNode;
import org.fife.rsta.ac.java.rjc.ast.CompilationUnit;
import org.fife.rsta.ac.java.tree.JavaOutlineTree;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import lu.fisch.unimozer.copies.RtfTransferable;
import org.fife.rsta.ac.java.buildpath.DirLibraryInfo;
import org.fife.rsta.ac.java.buildpath.JarLibraryInfo;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.parser.ParserNotice;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

/**
 *
 * @author robertfisch
 */
public class CodeEditor extends JPanel implements KeyListener, MouseMotionListener, DocumentListener, Printable, ActionListener, HyperlinkListener, FocusListener, MouseListener
{
    private RSyntaxTextArea codeArea = null;
    private RTextScrollPane codeAreaScroll = null;
    /**/
    /*private JEditorPane codeArea = null;
    private JScrollPane codeAreaScroll = null;
    /**/

    private long keyTimeout = 0;

    private Diagram diagram = null;
    private Mainform frame = null;
    private CodeCode code = new CodeCode();
    private JLabel status = null;
    private JPanel topPanel = null;
    private JLabel myClassname = null;
    private JComboBox showWhat = null;
    private JEditorPane javaDoc = null;
    private JScrollPane javaDocScroll = null;
    public JSplitPane jsp = null;

    public static Color DEFAULT_COLOR = Color.LIGHT_GRAY;

    private String lastFindText = null;
    private int lastFindPosition = -1;
    private boolean lastFindMatchCase = false;
    private boolean lastFindWholeWord = false;

    // Code Completion
    private JavaOutlineTree tree;
    private CompilationUnit cu;
    private Listener listener;

    //File info = null;

    private Icon[] icons;
    
    private boolean badIdea = false;
    
    private String selection = "";

    public RSyntaxTextArea getCodeArea() {
        return codeArea;
    }

    public void setProjectPath()
    {
        // get LanguageSupport
        LanguageSupport ls = LanguageSupportFactory.get().getSupportFor("text/java");
        // clear jars
        JavaLanguageSupport jls = (JavaLanguageSupport) ls;
/* does this make Unimozer crash --> new Java version??        
        //jls.getJarManager().clearClassFileSources();
        //jls.getJarManager().clearJars();
        try
        {
            // add system jar
            jls.getJarManager().addCurrentJreClassFileSource();
            //jls.getJarManager().addJar(null);

            // did we find the src.zip file?
            if(Unimozer.JDK_source!=null)
            {
                //JarInfo ji = JarInfo.getMainJREJarInfo();
                //ji.setSourceLocation(new File(Unimozer.JDK_source));
                //jls.getJarManager().addJar(ji);
                if((new File(Unimozer.JDK_source)).exists())
                    jls.getJarManager().addClassFileSource(new JarLibraryInfo(Unimozer.JDK_source));
                if((new File(Unimozer.JDK_source)).exists())
                    jls.getJarManager().addClassFileSource(new File(Unimozer.JDK_source));
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
/**/
        if(diagram!=null)
        if (diagram.getDirectoryName()!=null)
        {

            String path=diagram.getDirectoryName();
            
/* does this make Unimozer crash --> new Java version??                      
            try
            {
                // add current project path
                if(path.lastIndexOf(System.getProperty("file.separator"))!=path.length()-1) path+=System.getProperty("file.separator");

                //System.out.println("Adding path "+path);
                File myPath;
                myPath = new File(path+"bin");
                // first let's try to add the "bin" folder
                if (myPath.exists()) 
                {
                    if(myPath.isDirectory())
                        jls.getJarManager().addClassFileSource(new DirLibraryInfo(myPath));
                    else 
                        jls.getJarManager().addClassFileSource(myPath); 
                }

                // now let's add the "src" folder
                myPath = new File(path+"src");
                //if (!myPath.exists()) myPath.mkdir();
                if (myPath.exists()) 
                {
                    if(myPath.isDirectory())
                        jls.getJarManager().addClassFileSource(new DirLibraryInfo(myPath));
                    else 
                        jls.getJarManager().addClassFileSource(myPath); 
                }
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
/**/
        }
/*
        try
        {
                JarInfo javaSrcInfo = new JarInfo(new File("/Users/robertfisch/Desktop/src"));
                javaSrcInfo.setSourceLocation(new File("/Users/robertfisch/Desktop/src"));
                ((JavaLanguageSupport) ls).getJarManager().addJar(javaSrcInfo);


                File javaSrc = new File(Unimozer.JDK_home+System.getProperty("file.separator")+"src.zip");
            System.out.println("Found Java sources: "+javaSrc.getAbsolutePath());
            if (javaSrc.exists())
            {
                System.out.println("Adding them ...");
                JarInfo javaSrcInfo = new JarInfo(new File(Unimozer.JDK_home+System.getProperty("file.separator")+"lib"+System.getProperty("file.separator")+"tools.jar"));
                javaSrcInfo.setSourceLocation(javaSrc);
                ((JavaLanguageSupport) ls).getJarManager().addJar(javaSrcInfo);
            }

        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
         /*
         */
    }

    private Icon createIcon(String res) {
            Icon icon = null;
            try {
                    icon = new ImageIcon(ImageIO.read(getClass().getResource(res)));
            } catch (IOException ioe) { // Never happens
                    ioe.printStackTrace();
            }
            return icon;
    }

    public CodeEditor()
    {
        super();
        this.setLayout(new BorderLayout());
        
        icons = new Icon[3];
        icons[0] = createIcon("error_obj.gif");
        icons[1] = createIcon("warning_obj.gif");
        // Informational icons are annoying - spelling errors, etc.
        icons[2] = createIcon("info_obj.gif");

        codeArea = new RSyntaxTextArea();
        codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        codeArea.addKeyListener(this);
        codeArea.addMouseMotionListener(this);
        codeArea.setEnabled(false);
        codeArea.setEditable(false);
        //codeArea.setTabSize(4);
        codeArea.setAnimateBracketMatching(true);
        codeArea.setBracketMatchingEnabled(true);
        codeArea.setAutoIndentEnabled(true);
        codeArea.setAntiAliasingEnabled(true);
        //codeArea.setCloseCurlyBraces(true);
        codeArea.getDocument().addDocumentListener(this);
        
        // code folding doesn't work with structure highlighting
        /*
        if(!System.getProperty("os.name").toLowerCase().startsWith("mac os x"))
            codeArea.setCodeFoldingEnabled(true);
        */
        
        // set tab lines
        codeArea.setPaintTabLines(true);

        listener = new Listener();
        codeArea.addPropertyChangeListener(RSyntaxTextArea.PARSER_NOTICES_PROPERTY,listener);

        codeAreaScroll = new RTextScrollPane(codeArea);
        /**/
        /*
        codeArea = new JEditorPane();
        EditorKit kit = CloneableEditorSupport.getEditorKit("text/x-java");
        //codeArea.setEditorKit(kit);
        codeArea.addKeyListener(this);
        codeArea.addMouseMotionListener(this);
        codeArea.getDocument().addDocumentListener(this);

        codeAreaScroll = new JScrollPane(codeArea);
        /**/


        //codeArea.setParser(org.fife.ui.rsyntaxtextarea.Parser));
        //codeArea.setMatchedBracketBGColor(new Color(196,196,0));

        // Code Completion
        LanguageSupportFactory.get().register(codeArea);

        LanguageSupportFactory lsf = LanguageSupportFactory.get();
        LanguageSupport support = lsf.getSupportFor(SyntaxConstants.SYNTAX_STYLE_JAVA);
        JavaLanguageSupport jls = (JavaLanguageSupport)support;
        jls.setAutoCompleteEnabled(true);
        jls.setShowDescWindow(true);
        jls.setParameterAssistanceEnabled(true);
        jls.setAutoActivationEnabled(true);
        jls.setAutoActivationDelay(300);
        JarManager.setCheckModifiedDatestamps(true);
        // TODO: This API will change!  It will be easier to do per-editor
        // changes to the build path.
        /*try
        {
            jls.getJarManager().addJar(null);
        } 
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }*/

        codeArea.addCaretListener(listener);
        codeArea.addMouseListener(this);
        codeAreaScroll.setIconRowHeaderEnabled(true);
        codeAreaScroll.getGutter().setBookmarkingEnabled(true);
        /* --- this does the "... class blah blah" output
         */
        tree = new JavaOutlineTree(true);
        tree.addTreeSelectionListener(listener);
        tree.listenTo(codeArea);
        JScrollPane treeSP = new JScrollPane(tree);
        


        codeArea.setMarkOccurrences(true);
        codeArea.setAntiAliasingEnabled(true);
        ToolTipManager.sharedInstance().registerComponent(codeArea);

        codeArea.addFocusListener(this);
        
        jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        jsp.setResizeWeight(1);
        jsp.setTopComponent(codeAreaScroll);
        jsp.setBottomComponent(treeSP);
        //jsp.setDividerLocation(Integer.MAX_VALUE);

        this.add(jsp,BorderLayout.CENTER);

        // -- top panel --
        topPanel = new JPanel(new BorderLayout());
        topPanel.setPreferredSize(new Dimension(codeAreaScroll.getWidth(),24));
        topPanel.setBackground(Color.decode("#ffffaa"));

        myClassname = new JLabel();
        
        myClassname.setFont(new Font(myClassname.getFont().getFontName(),Font.BOLD,myClassname.getFont().getSize()));
        topPanel.add(myClassname,BorderLayout.WEST);

        showWhat = new JComboBox();
        showWhat.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Source Code","Documentation" }));
        showWhat.setEditable(false);
        showWhat.addActionListener(this);
        showWhat.setEnabled(false);
        topPanel.add(showWhat,BorderLayout.EAST);

        this.add(topPanel,BorderLayout.NORTH);

        javaDoc = new JTextPane();
        javaDoc.setContentType("text/html");
        javaDoc.setEditable(false);
        javaDoc.setVisible(true);
        javaDoc.addHyperlinkListener(this);

        javaDocScroll = new JScrollPane(javaDoc);
        javaDocScroll.setVisible(false);

    }

    public void setClassName(String classname)
    {
        if(!classname.trim().equals("")) myClassname.setText(" "+classname+".java");
        else myClassname.setText("");
    }

    public void setDiagram(Diagram diagram)
    {
        this.diagram=diagram;
    }

    @Override
    public void setEnabled(final boolean b)
    {
        /*Runnable r = new Runnable() 
        {
            @Override
            public void run() 
            {*/
                CodeEditor.super.setEnabled(b);
                codeArea.setEnabled(b);
                codeArea.setEditable(b);

                showWhat.setEnabled(b);
                if(frame!=null) frame.setEnabledEditorActions(b);
            /*}
        };
        (new Thread(r)).start();*/
    }

    public void setCode(String code)
    {
        // remember changed state
        boolean changed = false;
        if(diagram!=null)
            changed = diagram.isChanged();
        
        // remove the document listener
        codeArea.getDocument().removeDocumentListener(this);

        // HACK: TODO: Provide a better means of doing this.      
        LanguageSupportFactory fact = LanguageSupportFactory.get();
        JavaLanguageSupport jls = (JavaLanguageSupport)fact.getSupportFor(SyntaxConstants.SYNTAX_STYLE_JAVA);
        JavaParser parser = jls.getParser(codeArea);
        if (parser!=null)
        {
                parser.removePropertyChangeListener(JavaParser.PROPERTY_COMPILATION_UNIT, listener);
        }
        cu = null;
         
        codeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);

        // Bob begin
        if(showWhat.getSelectedIndex()!=0) showWhat.setSelectedIndex(0);
        codeArea.setText(code);
        codeArea.setCaretPosition(0);
        //CODE
        codeArea.discardAllEdits();
        if(status!=null)
        {
            status.setBackground(DEFAULT_COLOR);
            status.setText(" ");
        }
        setEnabled(true);
        // Bob end

        
        ClassLoader cl = getClass().getClassLoader();

        // HACK: TODO: Provide a better means of doing this.
        if (SyntaxConstants.SYNTAX_STYLE_JAVA.equals(SyntaxConstants.SYNTAX_STYLE_JAVA))
        {
                fact = LanguageSupportFactory.get();
                jls = (JavaLanguageSupport)fact.getSupportFor(SyntaxConstants.SYNTAX_STYLE_JAVA);
                parser = jls.getParser(codeArea);
                if (parser!=null) {
                        parser.addPropertyChangeListener(
                                        JavaParser.PROPERTY_COMPILATION_UNIT, listener);
                }
                else
                {
                        System.err.println("ERROR: No JavaParser installed!");
                }
        }

        setProjectPath();

        // add the document listener
        codeArea.getDocument().addDocumentListener(this);
        
        // reapply save changed
        if(diagram!=null)
            diagram.setChanged(changed);
    }

    @Override
    public void focusGained(FocusEvent e)
    {
        Console.disconnectErr();
        // pass focus
        for(int i=0;i<this.getFocusListeners().length;i++)
        {
            this.getFocusListeners()[i].focusGained(e);
        }
    }

    @Override
    public void focusLost(FocusEvent e)
    {
        Console.connectErr();
        // pass focus
        for(int i=0;i<this.getFocusListeners().length;i++)
        {
            this.getFocusListeners()[i].focusLost(e);
        }
    }

    @Override
    public void keyTyped(KeyEvent e)
    {
        if(diagram!=null)
        {
            MyClass mc = diagram.getSelectedClass();
            if(mc!=null)
            {
                mc.setChanged(true);
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        if(codeArea.getText().isEmpty() || 
                !(
                    codeArea.getText().contains("class") || 
                    codeArea.getText().contains("public class") || 
                    codeArea.getText().contains("abstract class") ||
                    codeArea.getText().contains("public abstract class") ||
                    codeArea.getText().contains("public interface") ||
                    codeArea.getText().contains("interface")
                ))
        {
            JOptionPane.showMessageDialog(this.frame, "Sorry, deleting an entire class this way is a bad idea!", "Error", JOptionPane.ERROR_MESSAGE);
            codeArea.undoLastAction();
        }
        else
        {
            // catch copy, paste & cut event (not handeled by the documentChange!)
            if((e.getKeyChar()=='x' || e.getKeyChar()=='c' || e.getKeyChar()=='v') && (e.getModifiersEx()==KeyEvent.CTRL_DOWN_MASK || e.getModifiersEx()==KeyEvent.META_DOWN_MASK))
            {
                changedUpdate(null);
            }
            else if(diagram!=null)
            {
                MyClass mc = diagram.getSelectedClass();
                if(mc!=null)
                {
                    /*
                    // I tried to intercept very badly handeled class code,
                    // but I did not succeed yet ...
                    badIdea = true;
                    //mc.setChanged(true);
                    try
                    {
                        MyClass mc2 = new MyClass(mc.getName());
                        mc2.loadFromString(codeArea.getText());
                        System.out.println("Class name is: "+mc2.getName());
                        mc.setContent(StringList.explode(codeArea.getText(),"\n"));
                        badIdea = false;
                    }
                    catch(Exception ex)
                    {
                        System.out.println(codeArea.getText());
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(this.frame, "Sorry, but your last operation was a very bad idea!", "Error", JOptionPane.ERROR_MESSAGE);
                        codeArea.undoLastAction();
                    }*/
                    mc.setContent(StringList.explode(codeArea.getText(),"\n"));
                    mc.setChanged(true);
                }
            }
        }
        /*
        // this block is a bad work-around for the
        // problem that the autoindentation set the
        // caret to a non existing position
        if(e.getKeyCode() == KeyEvent.VK_ENTER)
        {
            //codeArea.convertTabsToSpaces();
        }

        if(diagram!=null)
        {
            MyClass mc = diagram.getSelectedClass();
            if(mc!=null)
            {
                //long isNow = Calendar.getInstance().getTimeInMillis();
                //if(isNow>keyTimeout+1000)
                //{
                    //mc.loadFromString(codeArea.getText());

                    if (
                               (e.getKeyCode()!=KeyEvent.VK_ALT)
                               &&
                               (e.getKeyCode()!=KeyEvent.VK_ALT_GRAPH)
                               &&
                               (e.getKeyCode()!=KeyEvent.VK_CAPS_LOCK)
                               &&
                               (e.getKeyCode()!=KeyEvent.VK_DOWN)
                               &&
                               (e.getKeyCode()!=KeyEvent.VK_UP)
                               &&
                               (e.getKeyCode()!=KeyEvent.VK_LEFT)
                               &&
                               (e.getKeyCode()!=KeyEvent.VK_RIGHT)
                               &&

                       )
                    {

                    }
                    JOptionPane.showMessageDialog(null,e.getKeyCode());
                    // if we hit the "save" button, no change is done!
                
                    if(e.getModifiers()==Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() && e.getKeyCode()==KeyEvent.VK_S)
                    {
                        //JOptionPane.showMessageDialog(null,"Ctrl-S");
                        //diagram.setChanged(false);
                    }
                    else // update from the code in any other situation
                    {
                        getCode().setCode(codeArea.getText());
                        diagram.setEnabled(false);
                        status.setBackground(Color.ORANGE);
                        status.setText("Checking syntax ...");
                        diagram.updateFromCode();
                    }
                    //diagram.loadClassFromString(mc, codeArea.getText());
                    //diagram.clean(mc);
                    //diagram.repaint();
                    //keyTimeout = isNow;
                //}
            }
        }
         /**/
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {

    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        //keyReleased(null);
    }

    /**
     * @return the code
     */
    public CodeCode getCode()
    {
        return code;
    }

    /**
     * @return the status
     */
    public JLabel getStatus()
    {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(JLabel status)
    {
        this.status = status;
        if(status!=null)
        {
            DEFAULT_COLOR=status.getBackground();
        }
    }


    @Override
    public void insertUpdate(DocumentEvent e)
    {
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
    }

    public void setCursorTo(String text)
    {
        if(codeArea.getText()!=null)
            if(codeArea.getText().indexOf(text)>=0)
            {
                String code=codeArea.getText();
                int posi = code.indexOf(text);
                
                /*int posiClose = text.length()-1;
                
                int open = 0;
                for(int i=posi;i<code.length();i++)
                {
                    if(code.charAt(i)=='{') open++;
                    else if(code.charAt(i)=='}')
                    {
                        open--;
                        if(open==0)
                        {
                            posiClose=i;
                            i=code.length();
                        }
                    }
                    else if(code.charAt(i)==';')
                    {
                        if(open==0)
                        {
                            posiClose=i;
                            i=code.length();
                        }
                    }
                }
                codeArea.setCaretPosition(posiClose);
                /**/
                codeArea.setCaretPosition(code.length()-1);
                codeArea.validate();
                codeArea.repaint();
                codeArea.setCaretPosition(posi);


                //RXTextUtilities.gotoStartOfLine(codeArea, codeArea.getCaretLineNumber());
                //RXTextUtilities.centerLineInScrollPane(codeArea);

                //System.err.println(codeArea.getCaret().);
                //Rectangle rect = codeAreaScroll.getViewport().getV
                //System.err.println(rect);
                /**/
            }
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
        if(diagram!=null)
        {
            MyClass mc = diagram.getSelectedClass();
            if(mc!=null)
            {
                if(!codeArea.getText().isEmpty() 
                    &&
                        
                   (
                    codeArea.getText().contains("class") || 
                    codeArea.getText().contains("public class") || 
                    codeArea.getText().contains("abstract class") ||
                    codeArea.getText().contains("public abstract class") ||
                    codeArea.getText().contains("public interface") ||
                    codeArea.getText().contains("interface")
                  ))
                {
                    if(badIdea==false)
                    {
                        getCode().setCode(codeArea.getText());
                        diagram.setEnabled(false);
                        status.setBackground(Color.ORANGE);
                        status.setText("Checking syntax ...");
                        diagram.updateFromCode();
                    }
                }
            }
        }
    }

    public void setFontSize(int size)
    {
        if(codeArea!=null)
        {
            Font f = codeArea.getFont();
            Font f2 = new Font(f.getFontName(),f.getStyle(),size);
            this.setFont(f2);
        }
    }

    @Override
    public void setFont(Font font)
    {
        super.setFont(font);
        if(codeArea!=null) codeArea.setFont(font);
    }

    @Override
    public Font getFont()
    {
        if(codeArea!=null) return codeArea.getFont();
        else return super.getFont();
    }

    @Override
    public int print(Graphics grphcs, PageFormat pf, int i) throws PrinterException
    {
        //return Printable.NO_SUCH_PAGE;
        // CODE
        return codeArea.print(grphcs, pf, i);
    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        if (showWhat.getSelectedIndex()==1 && !myClassname.getText().trim().equals("") && diagram.getSelectedClass()!=null)
        {
            if (!javaDocScroll.isVisible())
            if (Unimozer.javaDocDetected && diagram.getSelectedClass().isValidCode())
            {
                /*
                status.setBackground(DEFAULT_COLOR);

                this.remove(jsp);
                this.remove(javaDocScroll);
                jsp.setVisible(false);
                this.add(javaDocScroll,BorderLayout.CENTER);
                javaDoc.setText(" Loading ...");
                javaDocScroll.setVisible(true);
                */

                Runnable timeConsumingRunnable = new Runnable()
                {
                    @Override
                   public void run()
                   {
                      SwingUtilities.invokeLater(
                         new Runnable()
                         {
                            @Override
                            public void run()
                            {
                               status.setText("Generating documentation ...");
                            }
                         });

                      diagram.createJavaDoc();

                      SwingUtilities.invokeLater(
                         new Runnable()
                         {
                            @Override
                            public void run()
                            {
                                status.setText("Loading documentation ...");
                            }
                         });

                      /*SwingUtilities.invokeLater(
                         new Runnable()
                         {
                            @Override
                            public void run()
                            {
                                String filename = diagram.getDirectoryName()+System.getProperty("file.separator")+"doc"+System.getProperty("file.separator");
                                if (!diagram.getSelectedClass().getPackagename().equals("<default>") &&
                                    !diagram.getSelectedClass().getPackagename().equals(""))
                                    filename += diagram.getSelectedClass().getPackagename().replace(".", System.getProperty("file.separator"))+System.getProperty("file.separator");
                                filename += myClassname.getText().trim().replaceAll(".java", ".html");
                                try
                                {
                                    //filename=filename.replaceAll("\\"+System.getProperty("file.separator"), "\\/");
                                    String fullName = "file:///" + filename;
                                    //+ "?salt=" + Math.random();
                                    //fullName = fullName.replaceAll("\\ ","\\%20");
                                    //System.out.println("URL = "+fullName);

                                    Document doc = javaDoc.getDocument();
                                    doc.putProperty(Document.StreamDescriptionProperty, null);
                                    URL url = new URL(fullName);
                                    //System.out.println("Loading: "+url);
                                    javaDoc.setPage(url);
                                    if(javaDoc.getPage()!=null)
                                        if(javaDoc.getDocument()!=null)
                                            if (!javaDoc.getPage().equals(url)) javaDoc.getDocument().putProperty(Document.StreamDescriptionProperty, url);
                                }
                                catch (IOException ex)
                                {
                                    MyError.display(ex);
                                }
                                status.setText(" ");
                                status.repaint();
                            }
                         });*/
                    }
                };
                new Thread(timeConsumingRunnable).start();
            }
            else if(!diagram.getSelectedClass().isValidCode())
            {
                javaDoc.setText("<html><blockquote><h1>Error</h1><br>Your code contains error.<br>JavaDoc documentation cannot be generated!</blockquote></html>");
                javaDoc.repaint();

                this.remove(jsp);
                this.remove(javaDocScroll);
                jsp.setVisible(false);
                this.add(javaDocScroll,BorderLayout.CENTER);
                javaDocScroll.setVisible(true);
            }
            else if(myClassname.getText().trim().equals(""))
            {
                javaDoc.setText("<html><blockquote><h1>Error</h1><br>You need to select a class first.</blockquote></html>");
                javaDoc.repaint();

                this.remove(jsp);
                this.remove(javaDocScroll);
                jsp.setVisible(false);
                this.add(javaDocScroll,BorderLayout.CENTER);
                javaDocScroll.setVisible(true);
            }
            else
            {
                javaDoc.setText("<html><blockquote><h1>Sorry</h1><br>Unimozer was not able to detect a valid JDK installation,<br>so it can't create JavaDoc documentation.</blockquote></html>");
                javaDoc.repaint();

                this.remove(jsp);
                this.remove(javaDocScroll);
                jsp.setVisible(false);
                this.add(javaDocScroll,BorderLayout.CENTER);
                javaDocScroll.setVisible(true);
            }

            // this is a very bad and ugly tweak to make things drawing correctely!
            Container con = javaDocScroll.getParent();
            if(con!=null)
                while((con!=null) && !(con instanceof JSplitPane)) con=con.getParent();
            if(con!=null)
                ((JSplitPane) con).setDividerLocation(((JSplitPane) con).getDividerLocation());

            javaDocScroll.validate();
            javaDocScroll.repaint();
            javaDoc.repaint();
        }
        else if(!jsp.isVisible())
        {
            this.remove(jsp);
            this.remove(javaDocScroll);
            jsp.setVisible(true);
            this.add(jsp,BorderLayout.CENTER);
            javaDocScroll.setVisible(false);
            jsp.setBounds(javaDocScroll.getBounds());
            jsp.setPreferredSize(javaDocScroll.getPreferredSize());

            // this is a very bad and ugly tweak to make things drawing correctely!
            Container con = jsp.getParent();
            while(! (con instanceof JSplitPane)) con=con.getParent();
            ((JSplitPane) con).setDividerLocation(((JSplitPane) con).getDividerLocation());

            jsp.validate();
            jsp.repaint();
            codeArea.repaint();
        }
    }

    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        HyperlinkEvent.EventType type = e.getEventType();
        if (type == HyperlinkEvent.EventType.ENTERED)
        {
            status.setText(" "+e.getURL().toString());
        }
        else if (type == HyperlinkEvent.EventType.EXITED)
        {
            status.setText(" ");
        }
        else
        {
            ((JTextPane) javaDoc).setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try
            {
                javaDoc.setPage(e.getURL());
            }
            catch (Exception ex)
            {

            }

        }
    }

    private Action getAction(String name)
    {
            Action action = null;
            Action[] actions = codeArea.getActions();

            for (int i = 0; i < actions.length; i++)
            {
                    if (name.equals( actions[i].getValue(Action.NAME).toString() ) )
                    {
                            action = actions[i];
                            break;
                    }
            }

            return action;
    }


    public void highlightError(CompilationError error)
    {
        String text = codeArea.getText();
        int countEOL = 0;
        int pos = 0;
        while(countEOL!=error.getLine())
        {
            if(text.charAt(pos)=='\n') countEOL++;
            pos++;
        }
        codeArea.setCaretPosition(pos-1);
    }

    private String escape(String expression)
    {
        String result = new String();

        for(int i=0;i<expression.length();i++)
        {
            switch(expression.charAt(i))
            {
                case '^':
                case '[':
                case '.':
                case '$':
                case '{':
                case '*':
                case '(':
                case '\\':
                case '+':
                case ')':
                case '|':
                case '?':
                case '<':
                case '>':
                            result=result+"\\";
                default:    result=result+expression.charAt(i);
            }
        }

        return result;
    }

    public void doReplace()
    {
        ReplaceDialog rd = ReplaceDialog.showModal(frame, "Replace");
        if(rd.OK==true)
        {
            SearchContext sc = new SearchContext(rd.getWhat());
            sc.setReplaceWith(rd.getWith());
            sc.setMatchCase(rd.getMatchCase());
            sc.setWholeWord(rd.getWholeWord());
            sc.setRegularExpression(false);
            SearchEngine.replaceAll(codeArea,sc);
            //SearchEngine.replaceAll(codeArea, rd.getWhat(), rd.getWith(), rd.getMatchCase(), rd.getWholeWord(), false);
            
            /*

            String text = codeArea.getText();

            int cpos = codeArea.getCaretPosition();


            if(!rd.getMatchCase()) text = text.replaceAll("(?i)"+escape(what), escape(with));
            else text = text.replaceAll(escape(what), escape(with));

            

            //codeArea.selectAll();
            //codeArea.replaceSelection(text);
            codeArea.setText(text);
            codeArea.setCaretPosition(cpos);
            */
            
            changedUpdate(null);
        }
    }

    public void doFind()
    {
        FindDialog fd = FindDialog.showModal(frame, "Find");
        if(fd.OK==true)
        {
            SearchContext sc = new SearchContext(fd.getTextToFind());
            sc.setMatchCase(fd.getMatchCase());
            sc.setWholeWord(fd.getWholeWord());
            sc.setRegularExpression(false);
            SearchEngine.find(codeArea, sc);
            //SearchEngine.find(codeArea, fd.getTextToFind(), true , fd.getMatchCase(), fd.getWholeWord(), false);
            lastFindText = fd.getTextToFind();
            lastFindMatchCase = fd.getMatchCase();
            lastFindWholeWord = fd.getWholeWord();
            /*
            String text = codeArea.getText();
            String what = fd.getTextToFind();
            if(!fd.getMatchCase())
            {
                text=text.toLowerCase();
                what=what.toLowerCase();
            }
            int pos = text.indexOf(what);

            if(pos>=0)
            {
                lastFindText = what;
                lastFindPosition = pos;
                lastFindMatchCase = fd.getMatchCase();

                codeArea.setCaretPosition(pos);
            }*/
        }

    }

    public void doFindAgain()
    {
        if(lastFindText!=null)
        {
            SearchContext sc = new SearchContext(lastFindText);
            sc.setMatchCase(lastFindMatchCase);
            sc.setWholeWord(lastFindWholeWord);
            sc.setRegularExpression(false);
            SearchEngine.find(codeArea, sc);
            //SearchEngine.find(codeArea, lastFindText, true , lastFindMatchCase, lastFindWholeWord, false);
/*            String text = codeArea.getText();
            String what = lastFindText;
            if(!lastFindMatchCase)
            {
                text=text.toLowerCase();
                what=what.toLowerCase();
            }
            int pos = text.indexOf(what,lastFindPosition+1);

            if(pos<0) pos = text.indexOf(what);

            if(pos>=0)
            {
                lastFindPosition = pos;

                codeArea.setCaretPosition(pos);
            }*/
        }
    }

    /**
     * @param frame the frame to set
     */
    public void setFrame(Mainform frame)
    {
        this.frame = frame;
    }

    void focus()
    {
        codeArea.requestFocusInWindow();
    }

    public void editorCopy()
    {
        codeArea.copy();
    }
    
    public void editorPaste()
    {
        codeArea.paste();
    }
    
    public void editorCut()
    {
        codeArea.cut();
    }

    public void editorUndo()
    {
        codeArea.undoLastAction();
    }

    public void editorRedo()
    {
        codeArea.redoLastAction();
    }

    @Override
    public void mouseClicked(MouseEvent me) {
        
    }

    @Override
    public void mousePressed(MouseEvent me) {
        
    }

    @Override
    public void mouseReleased(MouseEvent me) {
        
    }

    @Override
    public void mouseEntered(MouseEvent me) {
        
    }

    @Override
    public void mouseExited(MouseEvent me) {
        
    }



	/**
	 * Listens for events in the text editor.
	 */
    private class Listener implements CaretListener, ActionListener,
                    PropertyChangeListener, TreeSelectionListener
    {

        private Timer t;

        public Listener()
        {
                t = new Timer(500, this);
                t.setRepeats(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {

                // Highlight the line range of the Java method being edited in the gutter.
                // Compilation unit will be null if not editing Java.
                if (cu != null)
                {
                        int dot = codeArea.getCaretPosition();
                        Point p = cu.getEnclosingMethodRange(dot);
                        if (p != null)
                        {
                                try
                                {
                                        int startLine = codeArea.getLineOfOffset(p.x);
                                        int endLine = codeArea.getLineOfOffset(p.y);
                                        //codeAreaScroll.getGutter().setActiveLineRange(startLine, endLine);
                                        codeArea.setActiveLineRange(startLine, endLine);
                                }
                                catch (BadLocationException ble)
                                {
                                        //ble.printStackTrace();
                                }
                        }
                        else
                        {
                                //codeAreaScroll.getGutter().clearActiveLineRange();
                        }
                }

        }

        @Override
        public void caretUpdate(CaretEvent e)
        {
            if(e.getDot()!=-1 && e.getMark()!=-1)
            {
                selection=codeArea.getText().substring(Math.min(e.getDot(), e.getMark()), Math.max(e.getDot(), e.getMark()));
                //System.out.println(selection+" : "+Math.min(e.getDot(), e.getMark()));
            }
            else
                selection="";
            t.restart();
        }

        @Override
        public void propertyChange(PropertyChangeEvent e)
        {

                String name = e.getPropertyName();
                //System.out.println("Propertyname = "+name);

                // A text area has been re-parsed (picked up from from RText)
                if (RSyntaxTextArea.PARSER_NOTICES_PROPERTY.equals(name)) 
                {
                    RSyntaxTextArea textArea = (RSyntaxTextArea)e.getSource();
                    RTextScrollPane sp = (RTextScrollPane)textArea.getParent().getParent();
                    Gutter g = sp.getGutter();
                    // TODO: Note this isn't entirely correct; if some other
                    // component has added tracking icons to the gutter, this will
                    // remove those as well!
                    g.removeAllTrackingIcons();
                    
                    List notices = textArea.getParserNotices();
                    for (Iterator i=notices.iterator(); i.hasNext(); ) {
                            ParserNotice notice = (ParserNotice)i.next();
                            int line = notice.getLine();
                            Icon icon = icons[notice.getLevel()];
                            try {
                                    g.addLineTrackingIcon(line, icon);
                            } catch (BadLocationException ble) { // Never happens
                                    System.err.println("*** Error adding notice:\n" +
                                                    notice + ":");
                                    ble.printStackTrace();
                            }
                    }

                }

                if (JavaParser.PROPERTY_COMPILATION_UNIT.equals(name))
                {
                        CompilationUnit cu = (CompilationUnit)e.getNewValue();
                        CodeEditor.this.cu = cu;
                }
        }

        @Override
        public void valueChanged(TreeSelectionEvent e)
        {
                // Select the item clicked in the tree in the editor.
                TreePath path = e.getNewLeadSelectionPath();
                if (path != null)
                {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                        Object obj = node.getUserObject();
                        if (obj instanceof ASTNode)
                        {
                                ASTNode astNode = (ASTNode) obj;
                                int start = astNode.getNameStartOffset();
                                int end = astNode.getNameEndOffset();
                                codeArea.select(start, end);
                        }
                }
        }

    }


    public void copyAdRtf()
    {
        int start = codeArea.getSelectionStart();
        int stop = codeArea.getSelectionEnd();
        int caret = codeArea.getCaretPosition();
        if(codeArea.getSelectionStart()==codeArea.getSelectionEnd())
            codeArea.selectAll();
        codeArea.copyAsRtf();

        // convert the content of the clipboard from Unicode to ISO-8859-1
        Clipboard clipboad = getToolkit().getSystemClipboard();
        try
        {
            ByteArrayInputStream bais = (ByteArrayInputStream) clipboad.getContents(null).getTransferData(new DataFlavor("text/rtf", "RTF"));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            final int BUF_SIZE = 1 << 8; //1KiB buffer
            byte[] buffer = new byte[BUF_SIZE];
            int bytesRead = -1;
            while((bytesRead = bais.read(buffer)) > -1) {
                  out.write(buffer, 0, bytesRead);
            }
            bais.close();

            String clip = new String(out.toByteArray());

            byte[] imageBytes = out.toByteArray();
            clipboad.setContents(new RtfTransferable(clip.getBytes("ISO-8859-1")), null);
        }
        catch (UnsupportedFlavorException ex)
        {
            ex.printStackTrace();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }

        codeArea.setCaretPosition(caret);
        codeArea.setSelectionStart(start);
        codeArea.setSelectionEnd(stop);
    }


    public Gutter getGutter() {
            return codeAreaScroll.getGutter();
    }

    public String getSelection() {
        return selection;
    }

 }
