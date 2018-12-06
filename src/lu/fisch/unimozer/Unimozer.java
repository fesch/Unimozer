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

import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import lu.fisch.unimozer.utils.StringList;

public class Unimozer
{
    public final static String E_NAME = "Unimozer";
    public final static String E_VERSION = "0.27-58" +
            "" +
            "";

    public static int DRAW_FONT_SIZE = 10;
    public static int DRAW_FONT_SIZE_ADDON = 10;

    public static String FILE_ENCODING = "UTF-8";

    public static StringList messages = new StringList();

    public static String E_THANKS =
    "Developed and maintained by\n"+
    " - Robert Fisch <robert.fisch@education.lu>\n"+
    "\n"+
    "Turtle icon designed by\n"+
    " - rainie_billybear@yahoo.com <rainiew@cass.net>\n"+
    "\n"+
    "Different ideas provided by (see the changelog tab for more details)\n"+
    " - Fred Faber <frederic.faber@education.lu>\n"+
    " - Jens Getreu <jens.getreu@education.lu>\n"+
    "\n"+
    "Take a look at the “license“ tab for details about third party components and files."
    ;

    public static String U_PACKAGENAME = "unimozer.pck";
    public static String B_PACKAGENAME = "package.bluej";
    public static String N_PACKAGENAME = "nbproject";

    public static final ImageIcon IMG_INFO = new ImageIcon(Unimozer.class.getResource("/lu/fisch/icons/iconfinder_info_lgpl_matrc_martin.png"));
    public static final ImageIcon IMG_ERROR = new ImageIcon(Unimozer.class.getResource("/lu/fisch/icons/iconfinder_error_lgpl_david_vignoni.png"));
    public static final ImageIcon IMG_WARNING = new ImageIcon(Unimozer.class.getResource("/lu/fisch/icons/iconfinder_warning_gpl_pavel_infernodemon.png"));
    public static final ImageIcon IMG_QUESTION = new ImageIcon(Unimozer.class.getResource("/lu/fisch/icons/iconfinder_question_lgpl_david_vignoni.png"));

    public static boolean javaDocDetected = true;
    public static boolean javaCompilerDetected = true;
    public static boolean javaCompileOnTheFly = false;

    public static String JDK_home = null;
    public static String JDK_source = null;

    public static void switchButtons(JButton a, JButton b)
    {
        if(!System.getProperty("os.name").toLowerCase().startsWith("mac os x"))
	{
            JButton btnTmp = new JButton();
            copyButton(a,btnTmp);
            copyButton(b,a);
            copyButton(btnTmp,b);
            b.requestFocusInWindow();
        }
        else a.requestFocusInWindow();
    }
    
    private static void copyButton(JButton from, JButton to)
    {
        to.setText(from.getText());
        for(ActionListener a : to.getActionListeners())
        {
            to.removeActionListener(a);
        }
        for(ActionListener a : from.getActionListeners())
        {
            to.addActionListener(a);
        }
        if(from.hasFocus())
        {
            to.requestFocus();
            to.requestFocusInWindow();
        }
    }


    private static int countBeforeIndex(String str, Character c, int index)
    {
        int count = 0;

        for(int i=0;i<Math.min(index,str.length());i++)
        {
            if(str.charAt(i)==c) count++;
        }

        return count;
    }

    public static StringList getTypesOf(String type)
    {
        /* The following code is not clean. It is a bad work-around.
         * A proper solution would be to split up the expression correctly
         * respectively to parse it ...
         */

        //System.err.println("Analysing: "+type);

        StringList sl = new StringList();

        String full = new String(type);

        // do the first check to see if we need
        // to cutoff the first part
        boolean checkCutOff = true;
        checkCutOff = checkCutOff && type.contains("<");
        if(type.contains(","))
        {
            int commaPos = type.indexOf(",");
            checkCutOff = checkCutOff && type.indexOf("<")<type.indexOf(",");
            checkCutOff = checkCutOff && countBeforeIndex(type,'<',commaPos)!=countBeforeIndex(type,'>',commaPos);
        }

        // do the second check to see if we need
        // to seperate by comma
        boolean checkComma = false;
        if(type.contains(","))
        {
            checkComma = true;
            int commaPos = type.indexOf(",");
            checkComma = checkComma && countBeforeIndex(type,'<',commaPos)==countBeforeIndex(type,'>',commaPos);
        }

        if(checkCutOff)
        {
            type=type.substring(0,type.indexOf("<"));
            sl.add(type);

            if(full.contains(">"))
            {
                String others=full.substring(full.indexOf("<")+1,full.lastIndexOf(">"));
                sl.addIfNew(getTypesOf(others.trim()));
            }
        }
        else if(checkComma)
        {
            int commaPos = type.indexOf(",");
            String first = type.substring(0,commaPos).trim();
            String second = type.substring(commaPos+1).trim();
            sl.add(getTypesOf(first));
            sl.add(getTypesOf(second));
        }
        else sl.add(type);

        return sl;
    }

}
