/*
 * source: http://www.java2s.com/Tutorial/Java/0240__Swing/UsingJOptionPanewithaJSlider.htm
 */

package lu.fisch.unimozer.dialogs;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class JSliderOnJOptionPane {

  public static ChangeListener changeListener = null;  
      
  public static int showInputDialog(JFrame parent, String title, String text, int min, int max, int value)
  {
    JOptionPane optionPane = new JOptionPane();
    JSlider slider = getSlider(optionPane);
    if(changeListener!=null)
        slider.addChangeListener(changeListener);
    slider.setMinimum(min);
    slider.setMaximum(max);
    slider.setValue(value);
    optionPane.setMessage(new Object[] { text, slider });
    optionPane.setMessageType(JOptionPane.QUESTION_MESSAGE);
    optionPane.setOptionType(JOptionPane.OK_CANCEL_OPTION);
    JDialog dialog = optionPane.createDialog(parent, title);
    dialog.setVisible(true);
    return (Integer) optionPane.getInputValue();
  }

  static JSlider getSlider(final JOptionPane optionPane) {
    JSlider slider = new JSlider();
    slider.setMajorTickSpacing(10);
    slider.setPaintTicks(true);
    slider.setPaintLabels(true);
    ChangeListener changeListener = new ChangeListener() {
      public void stateChanged(ChangeEvent changeEvent) {
        JSlider theSlider = (JSlider) changeEvent.getSource();
        if (!theSlider.getValueIsAdjusting()) {
          optionPane.setInputValue(new Integer(theSlider.getValue()));
        }
      }
    };
    slider.addChangeListener(changeListener);
    return slider;
  }

}