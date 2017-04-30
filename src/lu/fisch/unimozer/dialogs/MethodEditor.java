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

package lu.fisch.unimozer.dialogs;

import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.ModifierSet;
import japa.parser.ast.body.Parameter;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import lu.fisch.unimozer.Element;
import lu.fisch.unimozer.Ini;
import lu.fisch.unimozer.Java;
import lu.fisch.unimozer.Unimozer;
import sun.security.krb5.internal.crypto.Des;

/**
 *
 * @author robertfisch
 */
public class MethodEditor extends javax.swing.JDialog
{
    public boolean OK = false;

    private DefaultComboBoxModel model = new DefaultComboBoxModel(new String[] { "boolean", "byte", "short", "int", "long", "float", "double", "String" });


    public static MethodEditor showModal(Frame frame, String title, Collection additionalTypes)
    {
        MethodEditor ce = new MethodEditor(frame,title,true);
        
        Vector<String> types = new Vector<String>();
        types.add("void");
        types.add("boolean");
        types.add("byte");
        types.add("short");
        types.add("int");
        types.add("long");
        types.add("float");
        types.add("double");
        types.add("String");
        types.addAll(additionalTypes);
        
        ce.model = new DefaultComboBoxModel(types);
        ce.cbType.setModel(ce.model);
        
        ce.genDoc.setSelected(Boolean.valueOf(Ini.get("javaDocMethod", "true")));
        ce.setLocationRelativeTo(frame);
        ce.setVisible(true);
        return ce;
    }
 
    public static MethodEditor showModal(Frame frame, String title, Element ele)
    {
        MethodEditor ce = new MethodEditor(frame,title,true);
        ce.setLocationRelativeTo(frame);

        ce.setModifier(((MethodDeclaration) ele.getNode()).getModifiers());
        ce.setMethodName(((MethodDeclaration) ele.getNode()).getName());
        ce.setMethodType(((MethodDeclaration) ele.getNode()).getType().toString());
        
        List<Parameter> pl = ((MethodDeclaration) ele.getNode()).getParameters();
        Vector<Vector<String>> params = new Vector<Vector<String>>();
        if(pl!=null)
        for(Parameter p : pl)
        {
            Vector<String> ret = new Vector<String>();
            ret.add(p.getType().toString());
            ret.add(p.getId().getName());
            params.add(ret);
        }
        ce.setParams(params);

        ce.genDoc.setVisible(false);

        ce.setVisible(true);
        return ce;
    }

    public String getMethodName()
    {
        return edtName.getText();
    }

    public void setMethodName(String name)
    {
        edtName.setText(name);
    }

    public String getMethodType()
    {
        return (String) cbType.getSelectedItem();
    }

    public boolean generateJavaDoc()
    {
        return genDoc.isSelected();
    }


    public void setMethodType(String name)
    {
        cbType.setSelectedItem(name);
    }

    public void setParams(Vector<Vector<String>> params)
    {
        tblParams.setGridColor(Color.LIGHT_GRAY);
        tblParams.setShowGrid(true);
        DefaultTableModel tm = (DefaultTableModel) tblParams.getModel();
        while(tm.getRowCount()>0) tm.removeRow(0);
        for(Vector vec : params) tm.addRow(vec);
    }

    public Vector<Vector<String>> getParams()
    {
        DefaultTableModel tm = (DefaultTableModel) tblParams.getModel();
        Vector<Vector<String>> params = new Vector<Vector<String>>();
        for(int r=0;r<tm.getRowCount();r++)
        {
            Vector<String> ret = new Vector<String>();
            ret.add((String) tm.getValueAt(r, 0));
            ret.add((String) tm.getValueAt(r, 1));
            params.add(ret);
        }
        return params;
    }

    public void setModifier(int mod)
    {
        if(ModifierSet.isPublic(mod)) selPublic.setSelected(true);
        if(ModifierSet.isProtected(mod)) selProtected.setSelected(true);
        if(ModifierSet.isPrivate(mod)) selPrivate.setSelected(true);
        if(ModifierSet.isAbstract(mod)) selAbstract.setSelected(true);
        if(ModifierSet.isStatic(mod)) selStatic.setSelected(true);
        if(ModifierSet.isFinal(mod)) selFinal.setSelected(true);

    }

    public int getModifier()
    {
        int mod = 0;
        if(selPublic.isSelected()) mod+=ModifierSet.PUBLIC;
        if(selProtected.isSelected()) mod+=ModifierSet.PROTECTED;
        if(selPrivate.isSelected()) mod+=ModifierSet.PRIVATE;
        if(selStatic.isSelected()) mod+=ModifierSet.STATIC;
        if(selFinal.isSelected()) mod+=ModifierSet.FINAL;
        if(selAbstract.isSelected()) mod+=ModifierSet.ABSTRACT;
        return mod;
    }

    /** Creates new form ClassEditor */
    public MethodEditor() {
        initComponents();
        Unimozer.switchButtons(btnOK, btnCancel);
        this.pack();
    }

    
    public MethodEditor(Frame frame, String title, boolean modal)
    {
        super(frame,title, modal);
        initComponents();
        Unimozer.switchButtons(btnOK, btnCancel);
        this.pack();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        radioVisibility = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        edtName = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        selDefault = new javax.swing.JRadioButton();
        selPublic = new javax.swing.JRadioButton();
        selProtected = new javax.swing.JRadioButton();
        selPrivate = new javax.swing.JRadioButton();
        jPanel2 = new javax.swing.JPanel();
        selStatic = new javax.swing.JCheckBox();
        selFinal = new javax.swing.JCheckBox();
        selAbstract = new javax.swing.JCheckBox();
        btnOK = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        cbType = new javax.swing.JComboBox();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        cbParamType = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        edtParamName = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblParams = new javax.swing.JTable();
        btnAdd = new javax.swing.JButton();
        btnRemove = new javax.swing.JButton();
        genDoc = new javax.swing.JCheckBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jLabel1.setText("Type:");

        edtName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                edtNameKeyPressed(evt);
            }
        });

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Visibility"));

        radioVisibility.add(selDefault);
        selDefault.setText("default");

        radioVisibility.add(selPublic);
        selPublic.setSelected(true);
        selPublic.setText("public");
        selPublic.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selPublicActionPerformed(evt);
            }
        });

        radioVisibility.add(selProtected);
        selProtected.setText("protected");

        radioVisibility.add(selPrivate);
        selPrivate.setText("private");

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(selDefault)
            .add(selPublic)
            .add(selProtected)
            .add(selPrivate)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                .add(selPublic)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(selProtected)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(selDefault)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(selPrivate)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Modifier"));

        selStatic.setText("static");
        selStatic.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selStaticActionPerformed(evt);
            }
        });

        selFinal.setText("final");
        selFinal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selFinalActionPerformed(evt);
            }
        });

        selAbstract.setText("abstract");
        selAbstract.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selAbstractActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(selStatic)
                    .add(selFinal)
                    .add(selAbstract))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .add(selStatic)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(selFinal)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(selAbstract)
                .addContainerGap(24, Short.MAX_VALUE))
        );

        btnOK.setText("OK");
        btnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOKActionPerformed(evt);
            }
        });

        btnCancel.setText("Cancel");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        jLabel2.setText("Name:");

        cbType.setEditable(true);
        cbType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "void", "boolean", "byte", "short", "int", "long", "float", "double", "String" }));
        cbType.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                cbTypeKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                cbTypeKeyReleased(evt);
            }
        });

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Parameter"));

        jLabel3.setText("Name:");

        cbParamType.setEditable(true);
        cbParamType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "boolean", "byte", "short", "int", "long", "float", "double", "String" }));

        jLabel4.setText("Type:");

        edtParamName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                edtParamNameKeyPressed(evt);
            }
        });

        tblParams.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Type", "Name"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane1.setViewportView(tblParams);

        btnAdd.setText("Add");
        btnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddActionPerformed(evt);
            }
        });

        btnRemove.setText("Remove");
        btnRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRemoveActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(20, 20, 20)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel4)
                    .add(jLabel3))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(cbParamType, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(edtParamName)))
            .add(jPanel3Layout.createSequentialGroup()
                .add(10, 10, 10)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jPanel3Layout.createSequentialGroup()
                        .add(btnAdd)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(btnRemove))
                    .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 223, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(edtParamName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(cbParamType, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(btnRemove)
                    .add(btnAdd))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 122, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        genDoc.setSelected(true);
        genDoc.setText("JavaDoc Comments");

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(layout.createSequentialGroup()
                                .add(jLabel1)
                                .add(25, 25, 25))
                            .add(layout.createSequentialGroup()
                                .add(jLabel2)
                                .add(18, 18, 18)))
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(edtName, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 186, Short.MAX_VALUE)
                            .add(cbType, 0, 186, Short.MAX_VALUE)))
                    .add(layout.createSequentialGroup()
                        .add(btnCancel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(btnOK))
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(layout.createSequentialGroup()
                                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(18, 18, 18)
                                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(genDoc))
                        .add(0, 0, Short.MAX_VALUE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(edtName, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel2))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(cbType, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel1))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(genDoc)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(btnCancel)
                            .add(btnOK)))
                    .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void selPublicActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_selPublicActionPerformed
    {//GEN-HEADEREND:event_selPublicActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_selPublicActionPerformed

    private void btnOKActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnOKActionPerformed
    {//GEN-HEADEREND:event_btnOKActionPerformed
        boolean test1 = Java.isIdentifier(edtName.getText());
        boolean test2 = true;
        String wrong = new String();

        DefaultTableModel tm = (DefaultTableModel) tblParams.getModel();
        for(int r=0;r<tm.getRowCount() && test2==true;r++)
        {
            Vector<String> ret = new Vector<String>();
            wrong = (String) tm.getValueAt(r, 1);
            test2 = Java.isIdentifier(wrong);
        }

        if(test1 && test2)
        {
            OK = true;
            this.setVisible(false);
        }
        else
        {
            if(!test1) JOptionPane.showMessageDialog(this, "“"+edtName.getText()+"“ is not a correct method name." , "Error", JOptionPane.ERROR_MESSAGE);
            else JOptionPane.showMessageDialog(this, "“"+wrong+"“ is not a correct parameter name." , "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnOKActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnCancelActionPerformed
    {//GEN-HEADEREND:event_btnCancelActionPerformed
        OK = false;
        this.setVisible(false);
    }//GEN-LAST:event_btnCancelActionPerformed

    private void edtNameKeyPressed(java.awt.event.KeyEvent evt)//GEN-FIRST:event_edtNameKeyPressed
    {//GEN-HEADEREND:event_edtNameKeyPressed
		if(evt.getKeyCode() == KeyEvent.VK_ESCAPE)
		{
			OK=false;
			setVisible(false);
		}
		else if(evt.getKeyCode() == KeyEvent.VK_ENTER) // && (evt.isShiftDown() || evt.isControlDown()))
		{
			btnOKActionPerformed(null);
		}
    }//GEN-LAST:event_edtNameKeyPressed

    private void edtParamNameKeyPressed(java.awt.event.KeyEvent evt)//GEN-FIRST:event_edtParamNameKeyPressed
    {//GEN-HEADEREND:event_edtParamNameKeyPressed
        // TODO add your handling code here:
}//GEN-LAST:event_edtParamNameKeyPressed

    private void btnAddActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnAddActionPerformed
    {//GEN-HEADEREND:event_btnAddActionPerformed
        if(Java.isIdentifier(edtParamName.getText()))
        {
            tblParams.setGridColor(Color.LIGHT_GRAY);
            tblParams.setShowGrid(true);
            DefaultTableModel tm = (DefaultTableModel) tblParams.getModel();
            Vector<String> param = new Vector<String>();
            param.add((String) cbParamType.getSelectedItem());
            param.add(edtParamName.getText());
            tm.addRow(param);
        }
        else
        {
            JOptionPane.showMessageDialog(this, "“"+edtParamName.getText()+"“ is not a correct parameter name." , "Error", JOptionPane.ERROR_MESSAGE);
        }
}//GEN-LAST:event_btnAddActionPerformed

    private void btnRemoveActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_btnRemoveActionPerformed
    {//GEN-HEADEREND:event_btnRemoveActionPerformed
        DefaultTableModel tm = (DefaultTableModel) tblParams.getModel();
        tm.removeRow(tblParams.getSelectedRow());
    }//GEN-LAST:event_btnRemoveActionPerformed

    private void cbTypeKeyReleased(java.awt.event.KeyEvent evt)//GEN-FIRST:event_cbTypeKeyReleased
    {//GEN-HEADEREND:event_cbTypeKeyReleased
        // TODO add your handling code here:
    }//GEN-LAST:event_cbTypeKeyReleased

    private void cbTypeKeyPressed(java.awt.event.KeyEvent evt)//GEN-FIRST:event_cbTypeKeyPressed
    {//GEN-HEADEREND:event_cbTypeKeyPressed
        edtNameKeyPressed(evt);
    }//GEN-LAST:event_cbTypeKeyPressed

    private void selStaticActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_selStaticActionPerformed
    {//GEN-HEADEREND:event_selStaticActionPerformed
        selAbstract.setSelected(false);
    }//GEN-LAST:event_selStaticActionPerformed

    private void selAbstractActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_selAbstractActionPerformed
    {//GEN-HEADEREND:event_selAbstractActionPerformed
        selStatic.setSelected(false);
        selFinal.setSelected(false);
    }//GEN-LAST:event_selAbstractActionPerformed

    private void selFinalActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selFinalActionPerformed
        selAbstract.setSelected(false);
    }//GEN-LAST:event_selFinalActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdd;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnOK;
    private javax.swing.JButton btnRemove;
    private javax.swing.JComboBox cbParamType;
    private javax.swing.JComboBox cbType;
    private javax.swing.JTextField edtName;
    private javax.swing.JTextField edtParamName;
    private javax.swing.JCheckBox genDoc;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.ButtonGroup radioVisibility;
    private javax.swing.JCheckBox selAbstract;
    private javax.swing.JRadioButton selDefault;
    private javax.swing.JCheckBox selFinal;
    private javax.swing.JRadioButton selPrivate;
    private javax.swing.JRadioButton selProtected;
    private javax.swing.JRadioButton selPublic;
    private javax.swing.JCheckBox selStatic;
    private javax.swing.JTable tblParams;
    // End of variables declaration//GEN-END:variables

}
