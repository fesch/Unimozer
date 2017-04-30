/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ObjectInspector.java
 *
 * Created on Oct 16, 2010, 3:48:10 PM
 */

package lu.fisch.unimozer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.JDialog;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;

/**
 *
 * @author robertfisch
 */
public class ObjectInspector extends javax.swing.JDialog
{

    protected MyObject myObject = null;
    protected Diagram diagram = null;
    String display;
    protected Objectizer objectizer = null;
    protected Object selected = null;
    protected String selectedName = null;

    private static Point oiLocation = null;
    private static Dimension oiDimension = null;



    /** Creates new form ObjectInspector */
    public ObjectInspector(MyObject myObject, Frame frame, Diagram diagram, Objectizer objectizer)
    {
        super(frame,"Object Inspector",true);

        this.diagram=diagram;
        this.objectizer=objectizer;

        // init components
        initComponents();

        // clear labels
        inspectName.setText("");
        inspectType.setText("");
        inspectValue.setText("");

        // what to do with the dialog on close
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        // we need a modal dialog
        //setModal(true);
        // title?
        //setTitle("Object Inspector");

        // clear tree
        tree.removeAll();
        // set the renderer (to get the spcial icons)
        tree.setCellRenderer(new MyRenderer());
        // save reference to object
        this.myObject=myObject;
        // add elements
        DefaultMutableTreeNode node = null;

        if (!diagram.isUML())
            display = "<html><font color=#C0C0C0>"+myObject.getObject().getClass().getCanonicalName()+"</font> <font color=#000000>"+myObject.getName()+"</font></html>";
        else
            display = "<html><font color=#000000>"+myObject.getName()+"</font> : <font color=#C0C0C0>"+myObject.getObject().getClass().getCanonicalName()+"</font></html>";
        node = new DefaultMutableTreeNode(display);
        ((DefaultTreeModel) tree.getModel()).setRoot(node);

        fillFields(node,myObject.getObject().getClass(),myObject.getObject());
        //tree.collapsePath(tree.getPathForRow(0));
        tree.expandPath(tree.getPathForRow(0));

        this.setLocationRelativeTo(frame);

        // remember location and size for this session
        if (oiDimension==null) oiDimension=this.getSize();
        else 
        {
            this.setSize(oiDimension);
            this.setPreferredSize(oiDimension);
        }
        if (oiLocation==null) oiLocation=this.getLocation();
        else
        {
            this.setLocation(oiLocation);
        }
        this.validate();

        // show immediately
        setVisible(true);


    }

    private void fillFields(DefaultMutableTreeNode parent, Class c, Object o)
    {
        while(c!=null)
        {
            // get declared fields
            Field m[] = c.getDeclaredFields();
            for (int i = 0; i < m.length; i++)
            {
                // create a MyField
                MyField myf = new MyField(m[i].getName(), m[i],o,diagram);
                // create a node
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(myf);
                m[i].setAccessible(true);
                // add encapsulated fields recursively
                /*
                if (
                        !m[i].getType().isPrimitive() &&
                        !m[i].getType().getSimpleName().equals("String") &&
                        !m[i].getType().getSimpleName().equals("Byte") &&
                        !m[i].getType().getSimpleName().equals("Short") &&
                        !m[i].getType().getSimpleName().equals("Integer") &&
                        !m[i].getType().getSimpleName().equals("Long") &&
                        !m[i].getType().getSimpleName().equals("Float") &&
                        !m[i].getType().getSimpleName().equals("Double") &&
                        !m[i].getType().getSimpleName().equals("Character") &&
                        !m[i].getType().getSimpleName().equals("Boolean")
                )
                {
                    //System.err.print("We have: "+m[i].getName());
                    //System.err.println(" of class: "+m[i].getType().getSimpleName());
                    try
                    {
                        m[i].setAccessible(true);
                        fillFields(node, m[i].getType(), m[i].get(o));
                    }
                    catch (Exception ex)
                    {
                        //System.err.println("Catch for "+m[i].getName());
                        //fillFields(node, m[i].getType(), null);
                        //ex.printStackTrace();
                    }
                }
                 */
                // add the node
                parent.add(node);
            }

            if (c.isArray() && o!=null)
            {
                //System.err.println("Looping array: "+c.getSimpleName());
                for(int i=0;i<Array.getLength(o);i++)
                {
                    Object sub = Array.get(o, i);
                    //System.err.println("Found: "+sub.getClass().getSimpleName());
                    // create a MyField
                    MyField myf = new MyField("["+String.valueOf(i)+"]", null,sub,diagram);
                    // create a node
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(myf);
                    // add encapsulated fields recursively
                    if (sub!=null)
                        if (
                                !sub.getClass().getSimpleName().equals("String") &&
                                !sub.getClass().getSimpleName().equals("Byte") &&
                                !sub.getClass().getSimpleName().equals("Short") &&
                                !sub.getClass().getSimpleName().equals("Integer") &&
                                !sub.getClass().getSimpleName().equals("Long") &&
                                !sub.getClass().getSimpleName().equals("Float") &&
                                !sub.getClass().getSimpleName().equals("Double") &&
                                !sub.getClass().getSimpleName().equals("Character") &&
                                !sub.getClass().getSimpleName().equals("Boolean")
                        )
                        {
                            fillFields(node, sub.getClass(), sub);
                        }
                    // add the node
                    parent.add(node);
                }
            }

            // look for fields in superclass
            c=c.getSuperclass();
        }
    }

    class MyRenderer extends DefaultTreeCellRenderer
    {
        public MyRenderer() {
        }

        @Override
        public Component getTreeCellRendererComponent(
                            JTree tree,
                            Object value,
                            boolean sel,
                            boolean expanded,
                            boolean leaf,
                            int row,
                            boolean hasFocus) {

            super.getTreeCellRendererComponent(
                            tree, value, sel,
                            expanded, leaf, row,
                            hasFocus);
            if (((DefaultMutableTreeNode) value).getUserObject() instanceof String )
            {
                setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_class.png")));
            }
            else if (
                        ((MyField)((DefaultMutableTreeNode) value).getUserObject()).getType()==null
                    )
            {
                setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/gen_play.png")));
            }
            else /*if (
                        ((MyField)((DefaultMutableTreeNode) value).getUserObject()).getType().isPrimitive()
                        ||
                        ((MyField)((DefaultMutableTreeNode) value).getUserObject()).getType().getSimpleName().equals("String")
                    )*/
            {
                MyField myField = ((MyField)((DefaultMutableTreeNode) value).getUserObject());
                if (myField.field==null)
                {
                    setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/gen_play.png")));
                }
                else
                {
                    String name = myField.field.toGenericString();
                    if (name.contains("private "))
                    {
                        if (name.contains("static "))
                            setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/field_private_static.png")));
                        else
                            setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/field_private.png")));
                    }
                    else if (name.contains("public "))
                    {
                        if (name.contains("static "))
                            setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/field_public_static.png")));
                        else
                            setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/field_public.png")));
                    }
                    else if (name.contains("protected "))
                    {
                        if (name.contains("static "))
                            setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/field_protected_static.png")));
                        else
                            setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/field_protected.png")));
                    }
                    else
                    {
                        if (name.contains("static "))
                            setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/field_package_static.png")));
                        else
                            setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/field_package.png")));
                    }
                }
                //setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_field.png")));
            }
            /*else
            {
                setIcon(new javax.swing.ImageIcon(getClass().getResource("/lu/fisch/icons/uml_class.png")));
            }*/

            return this;
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        tree = new javax.swing.JTree();
        jPanel1 = new javax.swing.JPanel();
        addObject = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        jPanel2 = new javax.swing.JPanel();
        inspectName = new javax.swing.JLabel();
        inspectType = new javax.swing.JLabel();
        inspectValue = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                formKeyPressed(evt);
            }
        });

        jSplitPane1.setDividerLocation(280);
        jSplitPane1.setResizeWeight(1.0);

        tree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                treeValueChanged(evt);
            }
        });
        tree.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                treeKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(tree);

        jSplitPane1.setLeftComponent(jScrollPane1);

        jPanel1.setMinimumSize(new java.awt.Dimension(150, 129));
        jPanel1.setLayout(new java.awt.BorderLayout());

        addObject.setText("Monitor");
        addObject.setEnabled(false);
        addObject.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addObjectActionPerformed(evt);
            }
        });
        jPanel1.add(addObject, java.awt.BorderLayout.SOUTH);

        jScrollPane2.setAutoscrolls(true);

        inspectName.setBackground(new java.awt.Color(204, 255, 204));
        inspectName.setText("jLabel1");

        inspectType.setBackground(new java.awt.Color(204, 255, 204));
        inspectType.setText("jLabel1");

        inspectValue.setBackground(new java.awt.Color(204, 255, 204));
        inspectValue.setText("jLabel2");

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(inspectName, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
                    .add(inspectType, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE)
                    .add(inspectValue, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 191, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(inspectName)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(inspectType)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(inspectValue)
                .addContainerGap(316, Short.MAX_VALUE))
        );

        jScrollPane2.setViewportView(jPanel2);

        jPanel1.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jSplitPane1.setRightComponent(jPanel1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 526, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void treeValueChanged(javax.swing.event.TreeSelectionEvent evt)//GEN-FIRST:event_treeValueChanged
    {//GEN-HEADEREND:event_treeValueChanged
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

        inspectName.setText("");
        inspectType.setText("");
        inspectValue.setText("");

        if (node==null) return;
        else
        {
            if (node.getUserObject() instanceof MyField)
            {
                if (node.getUserObject()!=null)
                {
                    MyField myf = (MyField) node.getUserObject();
                    // add sub objects (if present)
                    if (node.isLeaf())
                    {
                        if (myf.getType()!=null && myf.field!=null && !myf.getValue().equals(myf.NULL))
                        {
                            if (
                                    !myf.getType().isPrimitive() &&
                                    !myf.getType().getSimpleName().equals("String") &&
                                    !myf.getType().getSimpleName().equals("Byte") &&
                                    !myf.getType().getSimpleName().equals("Short") &&
                                    !myf.getType().getSimpleName().equals("Integer") &&
                                    !myf.getType().getSimpleName().equals("Long") &&
                                    !myf.getType().getSimpleName().equals("Float") &&
                                    !myf.getType().getSimpleName().equals("Double") &&
                                    !myf.getType().getSimpleName().equals("Character") &&
                                    !myf.getType().getSimpleName().equals("Boolean")
                            )
                            {
                                try
                                {
                                    fillFields(node, myf.getType(), myf.field.get(myf.object));
                                }
                                catch (Exception ex)
                                {
                                }
                            }

                        }
                    }
                    // display, if we can)
                    if (myf.getType()!=null)
                    if (
                            myf.getType().isPrimitive() ||
                            myf.getType().getSimpleName().equals("String") ||
                            myf.getType().getSimpleName().equals("Byte") ||
                            myf.getType().getSimpleName().equals("Short") ||
                            myf.getType().getSimpleName().equals("Integer") ||
                            myf.getType().getSimpleName().equals("Long") ||
                            myf.getType().getSimpleName().equals("Float") ||
                            myf.getType().getSimpleName().equals("Double") ||
                            myf.getType().getSimpleName().equals("Character") ||
                            myf.getType().getSimpleName().equals("Boolean")
                    )
                    {
                        inspectName.setText("<html><b>Name = </b>"+myf.getName()+"</html>");
                        inspectType.setText("<html><b>Type = </b>"+myf.getType().getCanonicalName()+"</html>");
                        if (myf.getName().startsWith("[") && myf.getName().endsWith("]"))
                        {
                            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
                            MyField mypf = (MyField) parent.getUserObject();
                            inspectValue.setText("<html><b>Value = </b>"+mypf.getArray(Integer.valueOf(myf.getName().substring(1,myf.getName().length()-1)))+"</html>");
                        }
                        else
                            inspectValue.setText("<html><b>Value = </b>"+myf.getValue()+"</html>");
                    }
                    else if (myf.getType().getCanonicalName().equals("java.util.ArrayList"))
                    {
                        inspectName.setText("<html><b>Name = </b>"+myf.getName()+"</html>");
                        inspectType.setText("<html><b>Type = </b>"+myf.getType().getCanonicalName()+"</html>");
                        String code = "<table border=1><tr><td>Index</td><td>Value</td></tr>";
                        ArrayList list = (ArrayList) myf.getObject();
                        for(int i=0;i<list.size();i++)
                        {
                            code += "<tr><td align=right>"+i+"</td><td>"+list.get(i).toString()+"</td></tr>";
                        }
                        code += "</table>";
                        inspectValue.setText("<html>"+code+"</html>");
                    }
                    else if (myf.getType().getCanonicalName().equals("java.util.Vector"))
                    {
                        inspectName.setText("<html><b>Name = </b>"+myf.getName()+"</html>");
                        inspectType.setText("<html><b>Type = </b>"+myf.getType().getCanonicalName()+"</html>");
                        String code = "<table border=1><tr><td>Index</td><td>Value</td></tr>";
                        Vector list = (Vector) myf.getObject();
                        for(int i=0;i<list.size();i++)
                        {
                            code += "<tr><td align=right>"+i+"</td><td>"+list.get(i).toString()+"</td></tr>";
                        }
                        code += "</table>";
                        inspectValue.setText("<html>"+code+"</html>");
                    }
                    else
                    {
                        inspectName.setText("<html><b>Name = </b>"+myf.getName()+"</html>");
                        inspectType.setText("<html><b>Type = </b>"+myf.getType().getCanonicalName()+"</html>");
                        inspectValue.setText("<html><b>Value = </b>"+myf.getValue()+"</html>");
                    }

                    // do we have to enable the button?
                    if (myf.getType().isPrimitive())
                    {
                        addObject.setEnabled(false);
                    }
                    else
                    {
                        selected=myf.getObject();
                        selectedName=myf.getName();
                        addObject.setEnabled(selected!=null);
                    }
                }
            }
        }

    }//GEN-LAST:event_treeValueChanged

    private void addObjectActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_addObjectActionPerformed
    {//GEN-HEADEREND:event_addObjectActionPerformed
        MyObject sel = objectizer.addObject(selectedName, selected);
        myObject.addChild(sel);
    }//GEN-LAST:event_addObjectActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt)//GEN-FIRST:event_formWindowClosing
    {//GEN-HEADEREND:event_formWindowClosing
        oiDimension=this.getSize();
        oiLocation=this.getLocation();
    }//GEN-LAST:event_formWindowClosing

    private void formKeyPressed(java.awt.event.KeyEvent evt)//GEN-FIRST:event_formKeyPressed
    {//GEN-HEADEREND:event_formKeyPressed
        if(evt.getKeyCode() == KeyEvent.VK_ESCAPE)
        {
                setVisible(false);
        }
    }//GEN-LAST:event_formKeyPressed

    private void treeKeyPressed(java.awt.event.KeyEvent evt)//GEN-FIRST:event_treeKeyPressed
    {//GEN-HEADEREND:event_treeKeyPressed
        if(evt.getKeyCode() == KeyEvent.VK_ESCAPE)
        {
                setVisible(false);
        }
    }//GEN-LAST:event_treeKeyPressed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addObject;
    private javax.swing.JLabel inspectName;
    private javax.swing.JLabel inspectType;
    private javax.swing.JLabel inspectValue;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTree tree;
    // End of variables declaration//GEN-END:variables

}
