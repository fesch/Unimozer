/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package lu.fisch.unimozer.dialogs;

import japa.parser.ast.stmt.ForeachStmt;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.JOptionPane;
import lu.fisch.unimozer.Diagram;
import lu.fisch.unimozer.Element;
import lu.fisch.unimozer.MyClass;
import lu.fisch.unimozer.interactiveproject.InteractiveProject;

/**
 *
 * @author Ronny
 */
public class CreateInteractiveProjectDialog extends javax.swing.JDialog {

    private Diagram diagram;

    private boolean controllerBased = true;
    private String packageString;
    private MyClass interfaceClass;
    private MyClass studentClass;
    private String interfaceAttribute;
    /**
     * Creates new form CreateInteractiveProjectDialog
     */
    public CreateInteractiveProjectDialog(java.awt.Frame parent, boolean modal, Diagram diagram) {
        super(parent, modal);
        initComponents();
        this.diagram = diagram;
        this.setLocationRelativeTo(parent);
        this.setTitle("Create Interactive Project");
        loadProject();
    }

    private void loadProject() {
        createButton.setEnabled(true);
        warningLabel.setText("");
        if(getPackage()!=null)
        {
            packageString=getPackage();
            if(packageString.equals("<default>"))
                packageString="";
        }
        
        realClassLabel.setVisible(controllerBased);
        realClassBox.setVisible(controllerBased);
        mainClassBox.setVisible(controllerBased);
        mainClassLabel.setVisible(controllerBased);
        
        if(controllerBased)
        {
            interfaceLabel.setText("Interface class:");
            checkController();
            checkInterfaces();
            checkRealClass();
            checkAttribute();
        }
        else{
            checkStudentClass();
            interfaceLabel.setText("Student class:");
        }
        
        
        checkMain();
        
    }

    private void createInteractiveProject()
    {
        if(!nameTextField.getText().isEmpty())
        {
            String type;
            if(controllerRadioButton.isSelected())
                type="controller-based";
            else
                type="model-based";
            String main = mainClassBox.getSelectedItem().toString();
            String path;
            if(packageString.equals("<default>"))
                path="";
            else
            {
                path = packageString.replace('.', '/');
                path = "/" + path + "/";
            }
            
            if(!controllerBased)
            {
                for (int i = 0; i < diagram.getClassCount(); i++) {
                MyClass myClass = diagram.getClass(i);
                if(myClass.getShortName().equals(interfaceBox.getSelectedItem()))
                    studentClass = myClass;
                }
            }
            else{
                if(interfaceBox.getItemCount() == 0
                        || realClassBox.getItemCount() == 0
                        || mainClassBox.getItemCount() == 0)
                {
                    JOptionPane.showMessageDialog(null, "Some of the required classes are missing", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            
            
            InteractiveProject interactiveProject= new InteractiveProject(
                    type,
                    nameTextField.getText(), 
                    interfaceClass, 
                    interfaceAttribute, //get this in a method
                    packageString, 
                    main, 
                    path, 
                    studentClass, 
                    diagram,
                    false);
            diagram.setInteractiveProject(interactiveProject);
            this.dispose();
        }
        else{
            JOptionPane.showMessageDialog(null, "You need to specify a name!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void checkAttribute()
    {
        MyClass realClass = null;
        MyClass mainClass = null;
        for (int i = 0; i < diagram.getClassCount(); i++) {
            MyClass myClass = diagram.getClass(i);
            if (myClass.getShortName().equals(realClassBox.getSelectedItem())) {
                realClass = myClass;
            }
            if (myClass.getShortName().equals(mainClassBox.getSelectedItem())) {
                mainClass = myClass;
            }
        }
        if(realClass == null || mainClass == null)
            return;
        
        Vector<Element> field = mainClass.getFields();
        for (Iterator<Element> iterator = field.iterator(); iterator.hasNext();) {
            Element next = iterator.next();
            String name = next.getFullName();
            if(name.contains(realClass.getShortName()))
            {
                if(!name.contains("public"))
                {
                    warningLabel.setText(warningLabel.getText() + "Attribute of type "+realClassBox.getSelectedItem().toString()+" has to be unique and public.\n");
                    return;
                }
                interfaceAttribute = name.split("\\s+")[name.split("\\s+").length-1];
                System.out.println(interfaceAttribute);
                return;
           }
        }
        
        warningLabel.setText(warningLabel.getText() + "No attribute of type "+realClassBox.getSelectedItem().toString()+" found in the main class.\n");
    }
    
    
    private void checkStudentClass()
    {
        for (int i = 0; i < diagram.getClassCount(); i++) {
            MyClass myClass = diagram.getClass(i);
            interfaceBox.addItem(myClass.getShortName());
        }
    }
    
    private void checkController() {
        for (int i = 0; i < diagram.getClassCount(); i++) {
            MyClass myClass = diagram.getClass(i);
            if (myClass.getShortName().equals("Controller")) {
                studentClass = myClass;
                return;
            }
        }

        warningLabel.setText(warningLabel.getText() + "There has to be a class Controller, that the student needs to complete.\n");
        createButton.setEnabled(false);

    }

    private void checkRealClass()
    {
        realClassBox.removeAllItems();
        MyClass myClass = null;
        for (int i = 0; i < diagram.getClassCount(); i++) {
            myClass = diagram.getClass(i);
            if (myClass.getShortName().equals(interfaceBox.getSelectedItem())){
                interfaceClass = myClass;
                break;
            }
        }
        if(interfaceClass != null)
        {
            for (int i = 0; i < diagram.getClassCount(); i++) {
                myClass = diagram.getClass(i);
                if(myClass.getImplements().contains(interfaceClass.getShortName()))
                {
                    realClassBox.addItem(myClass.getShortName());
                }
            }
        }
        
    }
    
    private void checkInterfaces() {
        for (int i = 0; i < diagram.getClassCount(); i++) {
            MyClass myClass = diagram.getClass(i);
            if (myClass.isInterface()) {
                interfaceBox.addItem(myClass.getShortName());
            }
        }
    }

    private String getPackage() {
        if (diagram.getClassCount() > 0) {
            String packageName = diagram.getClass(0).getPackagename();
            for (int i = 1; i < diagram.getClassCount(); i++) {
                MyClass myClass = diagram.getClass(i);
                if (!myClass.getPackagename().equals(packageName)) {
                    warningLabel.setText(warningLabel.getText() + "All classes have to be in the same Package.\n");
                    createButton.setEnabled(false);
                    return null;
                }
            }
            return packageName;
        } else {
            warningLabel.setText(warningLabel.getText() + "There are no classes in the project.\n");
            createButton.setEnabled(false);
            return null;
        }
    }

    private void checkMain() {
        //gets the class that inherits from JFrame and contains a default constructor.
        for (int i = 0; i < diagram.getClassCount(); i++) {
            MyClass myClass = diagram.getClass(i);
            if (myClass.getExtendsClass().equals("javax.swing.JFrame")) {
                mainClassBox.addItem(myClass.getShortName());
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        projectTypeButtonGroup = new javax.swing.ButtonGroup();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel1 = new javax.swing.JLabel();
        modelRadioButton = new javax.swing.JRadioButton();
        controllerRadioButton = new javax.swing.JRadioButton();
        jSeparator1 = new javax.swing.JSeparator();
        jLabel2 = new javax.swing.JLabel();
        nameTextField = new javax.swing.JTextField();
        interfaceLabel = new javax.swing.JLabel();
        createButton = new javax.swing.JButton();
        interfaceBox = new javax.swing.JComboBox<>();
        jScrollPane2 = new javax.swing.JScrollPane();
        warningLabel = new javax.swing.JTextArea();
        mainClassLabel = new javax.swing.JLabel();
        mainClassBox = new javax.swing.JComboBox<>();
        realClassLabel = new javax.swing.JLabel();
        realClassBox = new javax.swing.JComboBox<>();

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setText("Select type of interactive Project:");

        projectTypeButtonGroup.add(modelRadioButton);
        modelRadioButton.setText("Model-based Project (Student has to write the model class)");
        modelRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modelRadioButtonActionPerformed(evt);
            }
        });

        projectTypeButtonGroup.add(controllerRadioButton);
        controllerRadioButton.setSelected(true);
        controllerRadioButton.setText("Controller-based Project (Student has to write the controller class)");
        controllerRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                controllerRadioButtonActionPerformed(evt);
            }
        });

        jLabel2.setText("Name:");

        interfaceLabel.setText("Interface-Class:");

        createButton.setText("Create Project");
        createButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createButtonActionPerformed(evt);
            }
        });

        warningLabel.setColumns(20);
        warningLabel.setForeground(new java.awt.Color(255, 0, 0));
        warningLabel.setRows(5);
        jScrollPane2.setViewportView(warningLabel);

        mainClassLabel.setText("Main class:");
        mainClassLabel.setToolTipText("The class that contains a default constructor and inherits from a JFrame");

        realClassLabel.setText("Real-Class:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(layout.createSequentialGroup()
                .addGap(35, 35, 35)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 408, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(interfaceLabel)
                            .addComponent(mainClassLabel)
                            .addComponent(realClassLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(interfaceBox, 0, 212, Short.MAX_VALUE)
                            .addComponent(nameTextField)
                            .addComponent(mainClassBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(realClassBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(94, 94, 94)
                                .addComponent(jLabel1))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(30, 30, 30)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(controllerRadioButton)
                                    .addComponent(modelRadioButton)))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(171, 171, 171)
                                .addComponent(createButton)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(controllerRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(modelRadioButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(nameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(interfaceLabel)
                    .addComponent(interfaceBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(mainClassLabel)
                    .addComponent(mainClassBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(realClassLabel)
                    .addComponent(realClassBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(14, 14, 14)
                .addComponent(createButton)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void createButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createButtonActionPerformed
        createInteractiveProject();
    }//GEN-LAST:event_createButtonActionPerformed

    private void modelRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modelRadioButtonActionPerformed
        if(modelRadioButton.isSelected())
        {
            controllerBased = false;
        }
        loadProject();
    }//GEN-LAST:event_modelRadioButtonActionPerformed

    private void controllerRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_controllerRadioButtonActionPerformed
        if(controllerRadioButton.isSelected())
        {
            controllerBased = true;
        }
        loadProject();
    }//GEN-LAST:event_controllerRadioButtonActionPerformed

    /**
     * @param args the command line arguments
     */

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton controllerRadioButton;
    private javax.swing.JButton createButton;
    private javax.swing.JComboBox<String> interfaceBox;
    private javax.swing.JLabel interfaceLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JComboBox<String> mainClassBox;
    private javax.swing.JLabel mainClassLabel;
    private javax.swing.JRadioButton modelRadioButton;
    private javax.swing.JTextField nameTextField;
    private javax.swing.ButtonGroup projectTypeButtonGroup;
    private javax.swing.JComboBox<String> realClassBox;
    private javax.swing.JLabel realClassLabel;
    private javax.swing.JTextArea warningLabel;
    // End of variables declaration//GEN-END:variables
}
