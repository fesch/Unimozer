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

/**
 * Heavily modified version of:
 * program BlueJ
 * package bluej.terminal
 * version $Id: Terminal.java 6215 2009-03-30 13:28:25Z polle $
 * author  Michael Kolling
 */

/*
 This file is part of the BlueJ program.
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

 This file is subject to the Classpath exception as provided in the
 LICENSE.txt file that accompanied this code.
 */

package lu.fisch.unimozer.console;

import bluej.terminal.InputBuffer;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.Scanner;
import javax.swing.JFrame;
import javax.swing.JTextPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import lu.fisch.unimozer.Objectizer;


public class Console extends JTextPane implements KeyListener
{
    // status
    /** remembers if a new call has been placed */
    private boolean newMethodCall = true;
    /** remembers if the console has been initialised */
    private boolean initialised = false;
    /** remembers if the console is active or not */
    private boolean isActive = false;

    // Buffer
    /** this is the input buffer where received keypressed arrive in */
    private InputBuffer buffer;

    // Streams
    /** this is the terminal reader for the standard input */
    private Reader in = new TerminalReader();
    /** this is the terminal writer for the standard output */
    private Writer out = new TerminalWriter(false);
    /** this is the terminal writer for the standard error */
    private Writer err = new TerminalWriter(true);

    /** a constant wrapper */
    private static final int SHORTCUT_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    /** defines the input font style */
    private final AttributeSet inputArttibutes = StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.YELLOW);
    /** defines the output font style for normal text*/
    private final AttributeSet normalArttibutes = StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.WHITE);
    /** defines the output font style for error text*/
    private final AttributeSet errorArttibutes = StyleContext.getDefaultStyleContext().addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, Color.RED);

    // backup variables
    /** store the initial input stream */
    private InputStream systemIn = null;
    /** store the initial standard output stream */
    private PrintStream systemOut = null;
    /** store the initial standard error stream */
    private PrintStream systemErr = null;


    // static things
    /** if used as singleton, this is the reference to the unique object */
    private static Console console = null;
    /** a frame the console lives in (for the demo) */
    private static JFrame frame = null;
    /** a container the console is put in (for the demo) */
    private static Container container = null;

    /**
     * Get the printstream for the standard error
     * @return  the printstream for the standard error
     */
    public static PrintStream getErr()
    {
        if(console!=null)
        {
            return console.systemErr;
        }
        return null;
    }

    /**
     * Get the printstream for the standard output
     * @return  the printstream for the standard output
     */
    public static PrintStream getOut()
    {
        if(console!=null)
        {
            return console.systemOut;
        }
        return null;
    }

    /**
     * Clear the console
     */
    public static void cls()
    {
        if(console!=null)
        {
            console.clear();
        }
    }

    /**
     * Main entry point (for the demo)
     * @param args 
     */
    public static void main(String[] args)
    {
        Console.invoke();
    }

    /**
     * invoke the console on a given container
     * @param container     the container to invoke the console to
     */
    public static void invoke(Container container)
    {
        Console.container=container;
        
        if(console==null)
        {
            console = new Console();
            console.setPreferredSize(new Dimension(640, 480));
        }

        if(container!=null)
        {
            container.add(console);
        }
    }

    /**
     * invoke a new console (demo)
     */
    public static void invoke()
    {
        if(frame==null)
        {
            // create a new frame
            frame = new JFrame();
            // set the default close operation
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            // create a new console
            console = new Console();
            // set its size
            console.setPreferredSize(new Dimension(640, 480));
            // add it to the frame
            frame.getContentPane().add(console);
            // pack the frame
            frame.pack();
            // show the frame
            frame.setVisible(true);
            // create a new scanner
            Scanner scanner = new Scanner(System.in);
            // output some text
            System.out.print("Please enter your name: ");
            // read some text
            String name = scanner.nextLine();
            // print again some text
            System.out.println("Hi "+name);
            System.out.println("");
            System.out.println("Press <Enter> to quit ...");
            scanner.nextLine();
            // close the frame
            frame.dispose();
        }
        else
        {
            frame.setVisible(true);
        }
    }

    /**
     * dispose the frame
     */
    public static void dispose()
    {
        if(console!=null)
        {
            // disconnect before deispose
            console.disconnect();
        }
        console=null;
        container=null;
        if(frame!=null)
        {
            frame.dispose();
        }
    }

    /** 
     * pause the console = wait for <Enter> being pressed
     */
    public static void pause()
    {
        Scanner scan = new Scanner(System.in);
        scan.nextLine();
    }

    /**
     * create a new console
     */
    public Console()
    {
        // iniatilise
        initialise();
        // add the local keylisteners
        this.addKeyListener(this);
        this.setEditable(isActive);
        this.setForeground(Color.WHITE);
        this.setBackground(Color.BLACK);
        this.setMargin(new Insets(6, 6, 6, 6));
        this.setFont(new Font("Monospaced",Font.BOLD,12));
        // store the reference
        console=this;
        // connect the console
        connect();
    }

    /**
     * connect the console to the standard error
     */
    public static void connectErr()
    {
        if(console!=null)
        {
            console.systemErr = System.err;
            System.setErr(console.getErrorPrintStream());
        }
    }

    /**
     * connect the console to all streams
     */
    public static void connectAll()
    {
        if(console!=null)
        {
            console.systemErr = System.err;
            console.systemOut = System.out;
            console.systemIn = System.in;
            System.setErr(console.getErrorPrintStream());
            System.setOut(console.getPrintStream());
            System.setIn(console.getInputStream());
        }
    }

    /**
     * get a reference to the running instance
     * @return  the running instance (create if not existing)
     */
    public static Console getInstance()
    {
        if(console==null) console=new Console();
        return console;
    }

    /**
     * disconnect the standard error
     */
    public static void disconnectErr()
    {
        if(console!=null)
        {
            System.setErr(console.systemErr);
        }
    }

    /**
     * disconnect all streams
     */
    public static void disconnectAll()
    {
        if(console!=null)
        {
            System.setErr(console.systemErr);
            System.setOut(console.systemOut);
            System.setIn(console.systemIn);
        }
    }

    /**
     * connect the console to all streams
     */
    public void connect()
    {
        systemIn = System.in;
        systemOut = System.out;
        systemErr = System.err;
        System.setIn(this.getInputStream());
        System.setOut(this.getPrintStream());
        System.setErr(this.getErrorPrintStream());
    }

    /**
     * disconnect from all streams
     */
    public void disconnect()
    {
        System.setIn(systemIn);
        System.setOut(systemOut);
        System.setErr(systemErr);
    }

    /**
     * ???
     * @param n 
     */
    public void setColumns(int n)
    {
    }

    /**
     * ???
     * @param n 
     */
    public void setRows(int n)
    {
    }

    /**
     * the key typed event
     * @param event 
     */
    @Override
    public void keyTyped(KeyEvent event)
    {
        initialise();
        // set default colored text
        if(isActive)
        {
            char ch = event.getKeyChar();

            switch(ch) {

            case 4:   // CTRL-D (unix/Mac EOF)
            case 26:  // CTRL-Z (DOS/Windows EOF)
                buffer.signalEOF();
                writeToInputTerminal("\n");
                break;

            case '\b':	// backspace
                if(buffer.backSpace()) {
                    try {
                        int length = this.getDocument().getLength();
                        this.getDocument().remove(length-1,1);
                        //this.replaceRange("", length-1, length);
                    }
                    catch (Exception exc)
                    {
                        exc.printStackTrace();
                    }
                }
                break;

            case '\r':	// carriage return
            case '\n':	// newline
                if(buffer.putChar('\n')) {
                    //System.err.println("Enter!");
                    writeToInputTerminal(String.valueOf(ch));
                    buffer.notifyReaders();
                }
                break;

            default:
                if(buffer.putChar(ch))
                writeToInputTerminal(String.valueOf(ch));
                break;
            }
        }
        event.consume();	// make sure the text area doesn't handle this
    }

    /**
     * the key pressed event
     * @param event 
     */
    @Override
    public void keyPressed(KeyEvent event)
    {
        if(event.getModifiers() != SHORTCUT_MASK && !(isDeadKey(event) && isActive) )
        {
            event.consume();
        }
    }

    /**
     * the key released event
     * @param event 
     */
    @Override
    public void keyReleased(KeyEvent event)
    {
        if(event.getModifiers() != SHORTCUT_MASK && !(isDeadKey(event) && isActive) )
        {
            event.consume();
        }
    }

    /**
     * Determines whether the given key is a dead key.
     */
    public static boolean isDeadKey(KeyEvent event)
    {
        switch(event.getKeyCode()) {
            case KeyEvent.VK_DEAD_GRAVE:
            case KeyEvent.VK_DEAD_ACUTE:
            case KeyEvent.VK_DEAD_CIRCUMFLEX:
            case KeyEvent.VK_DEAD_TILDE:
            case KeyEvent.VK_DEAD_MACRON:
            case KeyEvent.VK_DEAD_BREVE:
            case KeyEvent.VK_DEAD_ABOVEDOT:
            case KeyEvent.VK_DEAD_DIAERESIS:
            case KeyEvent.VK_DEAD_ABOVERING:
            case KeyEvent.VK_DEAD_DOUBLEACUTE:
            case KeyEvent.VK_DEAD_CARON:
            case KeyEvent.VK_DEAD_CEDILLA:
            case KeyEvent.VK_DEAD_OGONEK:
            case KeyEvent.VK_DEAD_IOTA:
            case KeyEvent.VK_DEAD_VOICED_SOUND:
            case KeyEvent.VK_DEAD_SEMIVOICED_SOUND:
                return true;
        }
        return false;
    }

    /**
     * Initialise the terminal; create the UI.
     */
    private synchronized void initialise()
    {
        if(! initialised)
        {
            buffer = new InputBuffer(256);
            initialised = true;
        }
    }

    /**
     * Show or hide the Terminal window.
     */
    public void showHide(boolean show)
    {
        initialise();
        setVisible(show);
        if(show)
        {
            this.requestFocus();
        }
    }
    
    /**
     * Prepare the terminal for I/O.
     */
    public void prepare()
    {
        if(newMethodCall)
        {   // prepare only once per method call
            showHide(true);
            newMethodCall = false;
        }
    }


    public boolean isActive()
    {
        return isActive;
    }

    /**
     * Make the window active.
     */
    public void activate(boolean active)
    {
        if(active != isActive)
        {
            initialise();
            this.setEditable(active);
            isActive = active;
        }
        if (active==true) this.requestFocus();
    }

    /**
     * Clear the terminal.
     */
    public void clear()
    {
        initialise();
        this.setText("");
    }

    /**
     * Write some text to the terminal.
     */
    private void writeToTerminal(String s)
    {
        prepare();

        // The form-feed character should clear the screen.
        int n = s.lastIndexOf('\f');
        if (n != -1)
        {
            clear();
            s = s.substring(n + 1);
        }

        //this.setText(this.getText()+s);
        try
        {
            this.getDocument().insertString(this.getDocument().getLength(), s, normalArttibutes);
        }
        catch (BadLocationException ex)
        {
            ex.printStackTrace();
        }
        //this.append(s);
        this.setCaretPosition(this.getDocument().getLength());
    }

    private void writeToInputTerminal(String s)
    {
        prepare();

        // The form-feed character should clear the screen.
        int n = s.lastIndexOf('\f');
        if (n != -1)
        {
            clear();
            s = s.substring(n + 1);
        }

        //this.setText(this.getText()+s);
        try
        {
            this.getDocument().insertString(this.getDocument().getLength(), s, inputArttibutes);
        }
        catch (BadLocationException ex)
        {
            ex.printStackTrace();
        }
        //this.append(s);
        this.setCaretPosition(this.getDocument().getLength());
    }

    /**
     * Write some text to error output.
     */
    private void writeToErrorOut(String s)
    {
        prepare();

        // The form-feed character should clear the screen.
        int n = s.lastIndexOf('\f');
        if (n != -1)
        {
            clear();
            s = s.substring(n + 1);
        }

        //this.setText(this.getText()+s);
        try
        {
            this.getDocument().insertString(this.getDocument().getLength(), s, errorArttibutes);
        }
        catch (BadLocationException ex)
        {
            ex.printStackTrace();
        }
        //this.append(s);
        this.setCaretPosition(this.getDocument().getLength());
    }

    /**
     * Return the input stream that can be used to read from this terminal.
     */
    public Reader getReader()
    {
        return in;
    }


    /**
     * get the input stream
     * @return  the input stream
     */
    public InputStream getInputStream()
    {
        return new ReaderInputStream(getReader());
    }


    /**
     * Return the output stream that can be used to write to this terminal
     */
    public Writer getWriter()
    {
        return out;
    }

    /**
     * get the output stream   
     * @return  the output stream
     */
    public OutputStream getOutputStream()
    {
        return new WriterOutputStream(getWriter());
    }

    /**
     * get the print stream
     * @return  the print stream
     */
    public PrintStream getPrintStream()
    {
        return new PrintStream(getOutputStream());
    }

    /**
     * Return the output stream that can be used to write error output to this terminal
     */
    public Writer getErrorWriter()
    {
        return err;
    }

    /**
     * get the output stream for the error output
     * @return  the output stream for the error output
     */
    public OutputStream getErrorOutputStream()
    {
        return new WriterOutputStream(getErrorWriter());
    }

    /**
     * get the print stream for the error output
     * @return  the print stream for the error output
     */
    public PrintStream getErrorPrintStream()
    {
        return new PrintStream(getErrorOutputStream());
    }


    /**[ Private Class TerminalReader ]****************************************/

    private class TerminalReader extends Reader
    {
        @Override
        public int read(char[] cbuf, int off, int len)
        {
            // activate console as soon as someone needs to read from it
            activate(true);

            initialise();

            int charsRead = 0;

            while(charsRead < len)
            {
                cbuf[off + charsRead] = buffer.getChar();
                charsRead++;
                                       // stop reading on <carrage return> or <enter>
                if(buffer.isEmpty() || (cbuf[off + charsRead-1]=='\r') || (cbuf[off + charsRead-1]=='\n'))
                {
                    // disable when reading is being stopped
                    activate(false);
                    break;
                }
            }
            return charsRead;
        }

        @Override
        public boolean ready()
        {
            return ! buffer.isEmpty();
        }

        @Override
        public void close()
        {
            activate(false);
        }
    }

    /**[ Private Class TerminalWriter ]****************************************/

    private class TerminalWriter extends Writer
    {
        private boolean isErrorOut;

        TerminalWriter(boolean isError)
        {
            super();
            isErrorOut = isError;
        }

        @Override
        public void write(final char[] cbuf, final int off, final int len)
        {
            if (isEnabled())
            {
                
                final Runnable r = new Runnable()
                {
                        @Override
                        public void run()
                        {
                            initialise();
                            if(isErrorOut)
                            {
                                writeToErrorOut(new String(cbuf, off, len));
                            }
                            else
                            {
                                writeToTerminal(new String(cbuf, off, len));
                            }
                        }
                };
                /*
                Runnable r2 = new Runnable()
                {
                        public void run()
                        {
                            try
                            {
                                // We use invokeAndWait so that terminal output is limited to
                                // the processing speed of the event queue. This means the UI
                                // will still respond to user input even if the output is really
                                // gushing.
                                EventQueue.invokeAndWait(r);
                            }
                            catch (InvocationTargetException ite)
                            {
                                ite.printStackTrace();
                            }
                            catch (InterruptedException ie)
                            {
                            }
                        }
                };
                */

                //SwingUtilities.invokeLater(r);
                
                try
                {
                    // We use invokeAndWait so that terminal output is limited to
                    // the processing speed of the event queue. This means the UI
                    // will still respond to user input even if the output is really
                    // gushing.

                    // if this thread is the eventDispatcherThread
                    // we need to launch the putput via another (new) thread
                    if(EventQueue.isDispatchThread())
                    {
                        Thread t = new Thread(r);
                        t.start();
                        t.join();
                        Objectizer.getInstance().repaint();
                    }
                    else // in any other case, launch it on the EventQueue
                    {
                        EventQueue.invokeAndWait(r);
                    }
                }
                catch (InvocationTargetException ite)
                {
                    ite.printStackTrace();
                }
                catch (InterruptedException ie)
                {
                }
                /**/
                /*
                try
                {
                    Thread t = new Thread(r);
                    t.start();
                    t.join();
                }
                catch (InterruptedException ie)
                {
                }
                /**/
                /*
                            initialise();
                            if(isErrorOut)
                            {
                                writeToErrorOut(new String(cbuf, off, len));
                            }
                            else
                            {
                                writeToTerminal(new String(cbuf, off, len));
                            }/**/


            }
        }

        @Override
        public void flush()
        {
        }

        @Override
        public void close()
        {
        }
    }

}
