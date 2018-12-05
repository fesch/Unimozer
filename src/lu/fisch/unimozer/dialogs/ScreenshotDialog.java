package lu.fisch.unimozer.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

import org.fife.ui.rtextarea.Gutter;

import lu.fisch.structorizer.gui.Editor;
import lu.fisch.unimozer.CodeEditor;
import lu.fisch.unimozer.Diagram;
import lu.fisch.unimozer.Unimozer;
import sun.awt.AWTAccessor.CheckboxMenuItemAccessor;

import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.imageio.ImageIO;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.Toolkit;
import javax.swing.ImageIcon;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.event.ActionEvent;
import javax.swing.JSlider;

public class ScreenshotDialog extends JDialog {

	private final JPanel contentPanel = new JPanel();
	private Diagram diagram;
	private CodeEditor editor;
	private JButton btnSave;

	/**
	 * Launch the application.
	 */
	/*public static void main(String[] args) {
		try {
			ScreenshotDialog dialog = new ScreenshotDialog();
			dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
			dialog.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}*/

	/**
	 * Create the dialog.
	 */
	public ScreenshotDialog(Diagram diagram, CodeEditor editor) {
		this.diagram = diagram;
		this.editor = editor;
		
		setIconImage(Toolkit.getDefaultToolkit().getImage(ScreenshotDialog.class.getResource("/lu/fisch/icons/export_image.png")));
		setTitle("Screenshot Code");
		setResizable(false);
		setBounds(100, 100, 226, 155);
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		JLabel lblShowLineNumbers = new JLabel("Show line numbers:");
		JLabel lblAddFooter = new JLabel("Add Footer:");
		JCheckBox chckbxShowlinenumbers = new JCheckBox("");
		JCheckBox chckbxAddFooter = new JCheckBox("");
		
		
		{
			btnSave = new JButton("Save");
			btnSave.addActionListener(new ActionListener() {
                    public void actionPerformed(java.awt.event.ActionEvent evt)
                    {
                   	 
                     setVisible(false);
                    	
                   	 JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
                   	 jfc.setDialogTitle("Choose a directory to save your screenshot: ");
                   	 jfc.setCurrentDirectory(new File(diagram.getContainingDirectoryName()));

                   	 FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG Image", "png");
                   	 jfc.addChoosableFileFilter(filter);
                   	 jfc.setFileFilter(filter);
                   	 
                   	 int returnValue = jfc.showSaveDialog(null);
                   	 if (returnValue == JFileChooser.APPROVE_OPTION) 
                   	 {
                   		 BufferedImage biScreenShot = null;
                   		 Gutter gutter = new Gutter(editor.getCodeArea());
                   			 
                   		 BufferedImage biCode = new BufferedImage(editor.getCodeArea().getWidth(), editor.getCodeArea().getHeight(), BufferedImage.TYPE_INT_RGB);
                   		 Graphics2D gCode = biCode.createGraphics();
                   		 editor.getCodeArea().setHighlightCurrentLine(false);
                   		 editor.getCodeArea().paint(gCode);
                   		 editor.getCodeArea().setHighlightCurrentLine(true);
                   			 
                   		 BufferedImage biGutter = new BufferedImage(editor.getGutter().getWidth(), editor.getGutter().getHeight(), BufferedImage.TYPE_INT_RGB);
                   		 Graphics2D gGutter = biGutter.createGraphics();
                   		 editor.getGutter().paint(gGutter);
                  			 
                  			 
                   		 biScreenShot = new BufferedImage(biCode.getWidth() + biGutter.getWidth(), biCode.getHeight() + 25, BufferedImage.TYPE_INT_RGB);
                   		 Graphics2D gScreenshot = biScreenShot.createGraphics();
                   		 gScreenshot.setColor(Color.WHITE);
                   		 gScreenshot.fillRect(0, 0, biScreenShot.getWidth(), biScreenShot.getHeight());
                   		 
                   		 gScreenshot.drawImage(biCode, null, biGutter.getWidth(), 0); 
                   		 
                   		 if(chckbxShowlinenumbers.isSelected()) 
                   		 {
                   			 gScreenshot.drawImage(biGutter, null, 0, 0);
                   		 }
                   		 
                   		 if(chckbxAddFooter.isSelected()) 
                   		 {
                   			 gScreenshot.setFont(new Font(editor.getCodeArea().getFont().getFontName(), Font.PLAIN, 16));
                   			 gScreenshot.setColor(Color.BLACK);
                   			 gScreenshot.drawLine(5, biCode.getHeight() + 2, biScreenShot.getWidth() - 5, biCode.getHeight() + 2);
                   			 gScreenshot.drawString(diagram.getDirectoryName(), 5, biCode.getHeight() + 18);
                   		 }

                   		 try 
                   		 {
                   			 ImageIO.write(biScreenShot, "png", new File(jfc.getSelectedFile().getAbsolutePath() + ".png"));
                   			 JOptionPane.showMessageDialog(null, "Screenshot saved to " + jfc.getSelectedFile().getAbsolutePath() + ".png", "", JOptionPane.INFORMATION_MESSAGE);
                   		 }
                   		 catch (IOException e) 
                   		 {
                   			 e.printStackTrace();
                   		 }
                   	 }

                    }
			});
			btnSave.setIcon(new ImageIcon(ScreenshotDialog.class.getResource("/lu/fisch/icons/gen_save.png")));
		}
		GroupLayout gl_contentPanel = new GroupLayout(contentPanel);
		gl_contentPanel.setHorizontalGroup(
			gl_contentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPanel.createSequentialGroup()
					.addGap(7)
					.addGroup(gl_contentPanel.createParallelGroup(Alignment.LEADING)
						.addComponent(lblShowLineNumbers, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE)
						.addComponent(lblAddFooter, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE)))
		);
		gl_contentPanel.setVerticalGroup(
			gl_contentPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_contentPanel.createSequentialGroup()
					.addGap(11)
					.addComponent(lblShowLineNumbers)
					.addGap(12)
					.addComponent(lblAddFooter))
		);
		contentPanel.setLayout(gl_contentPanel);
		GroupLayout groupLayout = new GroupLayout(getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(contentPanel, GroupLayout.PREFERRED_SIZE, 129, GroupLayout.PREFERRED_SIZE)
					.addGap(54)
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(chckbxShowlinenumbers, GroupLayout.PREFERRED_SIZE, 134, GroupLayout.PREFERRED_SIZE)
						.addComponent(chckbxAddFooter, GroupLayout.PREFERRED_SIZE, 192, GroupLayout.PREFERRED_SIZE)))
				.addGroup(groupLayout.createSequentialGroup()
					.addGap(126)
					.addComponent(btnSave))
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addGroup(groupLayout.createParallelGroup(Alignment.LEADING)
						.addComponent(contentPanel, GroupLayout.PREFERRED_SIZE, 73, GroupLayout.PREFERRED_SIZE)
						.addGroup(groupLayout.createSequentialGroup()
							.addGap(18)
							.addComponent(chckbxShowlinenumbers, GroupLayout.PREFERRED_SIZE, 16, GroupLayout.PREFERRED_SIZE)
							.addGap(10)
							.addComponent(chckbxAddFooter, GroupLayout.PREFERRED_SIZE, 16, GroupLayout.PREFERRED_SIZE)))
					.addGap(9)
					.addComponent(btnSave))
		);
		getContentPane().setLayout(groupLayout);
	}
}