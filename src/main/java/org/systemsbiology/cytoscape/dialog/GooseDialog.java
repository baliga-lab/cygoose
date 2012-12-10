package org.systemsbiology.cytoscape.dialog;

import com.install4j.runtime.beans.actions.SystemAutoUninstallInstallAction;
import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import org.apache.commons.collections.map.HashedMap;
import org.systemsbiology.gaggle.core.GooseWorkflowManager;
import org.systemsbiology.gaggle.core.datatypes.*;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * @author skillcoy
 */
public class GooseDialog extends javax.swing.JPanel {
    GooseWorkflowManager workflowManager = new GooseWorkflowManager();
    Map<String, String> networkWorkflowActionMap = Collections.synchronizedMap(new HashMap<String, String>());
    String[] nextWorkflowTexts = null;
    int[] nextWorkflowDataTypes = null;
    DefaultListModel listModel = new DefaultListModel();

    public enum GooseButton {
        CONNECT("Connect"), SHOW("Show"), HIDE("Hide"), TUPLE(
            "Tuple"), MATRIX("Matrix"), NETWORK("Network"), LIST("List"), Next("Workflow");

        private String buttonName;

        private GooseButton(String name) {
            buttonName = name;
        }

    }

    /**
     * Creates new form GooseDialog
     */
    public GooseDialog() {
        initComponents();
    }

    public GooseWorkflowManager getWorkflowManager() { return this.workflowManager; }

    public void displayDataType(String type) {
        this.dataTypeText.setText(type);
    }


    public void displayMessage(String msg) {
        this.messageText.setText(msg);
    }

    public void addButtonAction(GooseButton gb, ActionListener l) {
        javax.swing.JButton button = null;
        switch (gb) {
            case CONNECT:
                button = this.connectButton;
                break;
            case SHOW:
                button = this.showButton;
                break;
            case HIDE:
                button = this.hideButton;
                break;
            case TUPLE:
                button = this.tupleButton;
                break;
            case MATRIX:
                button = this.matrixButton;
                break;
            case NETWORK:
                button = this.networkButton;
                break;
            case LIST:
                button = this.listButton;
                break;
            case Next:
                button = this.nextWorkflowButton;
                break;
        }
        button.addActionListener(l);
    }

    public void enableButton(GooseButton gb, boolean enabled) {
        javax.swing.JButton button = null;
        switch (gb) {
            case CONNECT:
                button = this.connectButton;
                break;
            case SHOW:
                button = this.showButton;
                break;
            case HIDE:
                button = this.hideButton;
                break;
            case TUPLE:
                button = this.tupleButton;
                break;
            case MATRIX:
                button = this.matrixButton;
                break;
            case NETWORK:
                button = this.networkButton;
                break;
            case LIST:
                button = this.listButton;
                break;
            case Next:
                button = this.nextWorkflowButton;
        }
        button.setEnabled(enabled);
    }

    public javax.swing.JList getWorkflowGeeseList() {
        return this.nextWorkflowText;
    }

    public int getSelectedGooseDataType(int selectedGoose)
    {
        return nextWorkflowDataTypes[selectedGoose];
    }

    public void setWorkflowDataType(int selectedGoose)
    {
        int selecteddatatype = nextWorkflowDataTypes[selectedGoose];
        dataRadioButtons[selecteddatatype].setSelected(true);
    }

    public javax.swing.JComboBox getGooseChooser() {
        return this.gooseComboBox;
    }

    public javax.swing.JComboBox getLayoutChooser() {
        return this.layoutComboBox;
    }
    
    public void setSpeciesText(String text) {
    	this.speciesText.setText(text);
    }
    
    public String getSpecies() {
    	return this.speciesText.getText();
    }

    public JButton getConnectButton() {
        return this.connectButton;
    }

    public void setConnectButtonStatus(boolean connected) {
        if (connected) {
            connectButton.setText("Disconnect");
            connectButton.setToolTipText("Disconnect from Gaggle");
            connectButton.setActionCommand("disconnect");
        } else {
            connectButton.setText("Connect");
            connectButton.setToolTipText("Connect to Gaggle");
            connectButton.setActionCommand("connect");
        }
    }
    
    public void setWorkflowUI(WorkflowAction action)
    {
        if (action != null)
        {
            WorkflowComponent[] targets = action.getTargets();
            if (targets != null)
            {
                this.nextWorkflowTexts = new String[targets.length];
                this.nextWorkflowDataTypes = new int[targets.length];
                for (int i = 0; i < targets.length; i++)
                {
                    WorkflowComponent target = targets[i];

                    if (target != null && target.getName() != null && target.getName().length() > 0)
                    {
                        String subAction = (String)target.getParams().get(WorkflowComponent.ParamNames.SubTarget.getValue());
                        String nextGeeseString = target.getName();
                        if (subAction != null && subAction.length() > 0)
                            nextGeeseString += (" " + subAction);
                        this.nextWorkflowTexts[i] = nextGeeseString;
                        listModel.addElement(nextGeeseString);
                        String data = (String)target.getParams().get(WorkflowComponent.ParamNames.EdgeType.getValue());
                        if (data != null)
                        {
                            if (data.equals("matrix"))
                            {
                                this.nextWorkflowDataTypes[i] = 2;
                                this.dataRadioButtons[2].setSelected(true);
                            }
                            else if (data.equals("tuple"))
                            {
                                this.nextWorkflowDataTypes[i] = 1;
                                this.dataRadioButtons[1].setSelected(true);
                            }
                            else if (data.equals("namelist"))
                            {
                                this.nextWorkflowDataTypes[i] = 0;
                                this.dataRadioButtons[0].setSelected(true);
                            }
                            else if (data.equals("network"))
                            {
                                this.nextWorkflowDataTypes[i] = 3;
                                this.dataRadioButtons[3].setSelected(true);
                            }
                        }
                    }
                }
            }
        }
        else
        {
            // Reset the workflowText field
            this.listModel.clear();
        }
    }

    public void removeRequestNetwork(String NetworkId)
    {
        if (NetworkId != null && networkWorkflowActionMap.containsKey(NetworkId))
        {
           networkWorkflowActionMap.remove(NetworkId);
        }
    }

    public void addRequestNetwork(String NetworkId, String requestID)
    {
        if (NetworkId != null && requestID != null)
            networkWorkflowActionMap.put(NetworkId, requestID);
    }

    public String getRequestID(String NetworkId)
    {
        if (this.networkWorkflowActionMap.containsKey(NetworkId))
            return this.networkWorkflowActionMap.get(NetworkId);
        return null;
    }

    /*public int getNextWorkflowDataType()
    {
        if (radioButtonNamelist.isSelected())
            return 0;
        if (radioButtonMatrix.isSelected())
            return 1;
        if (radioButtonTuple.isSelected())
            return 2;
        if (radioButtonNetwork.isSelected())
            return 3;
        return -1;
    } */

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">
    private void initComponents() {
        gooseControlPanel = new javax.swing.JPanel();
        gooseComboBox = new javax.swing.JComboBox();
        connectButton = new javax.swing.JButton();
        showButton = new javax.swing.JButton();
        hideButton = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JSeparator();
        workflowPanel = new javax.swing.JPanel();
        nextWorkflowText = new javax.swing.JList(listModel);
        nextWorkflowText.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // single selection
        nextworkListScrollPanel = new JScrollPane(nextWorkflowText);
        nextWorkflowDataText= new javax.swing.JPanel(new java.awt.GridLayout(0, 2));
        dataRadioButtonGroup = new javax.swing.ButtonGroup();
        for (int i = 0; i < 4; i++)
        {
            if (i == 0)
                dataRadioButtons[i] = new JRadioButton("List", true);
            else if (i == 1)
                dataRadioButtons[i] = new JRadioButton("Tuple", false);
            else if (i == 2)
                dataRadioButtons[i] = new JRadioButton("Matrix", false);
            else if (i == 3)
                dataRadioButtons[i] = new JRadioButton("Network", false);
            dataRadioButtonGroup.add(dataRadioButtons[i]);
            nextWorkflowDataText.add(dataRadioButtons[i]);
        }
        nextWorkflowButton = new javax.swing.JButton();
        jSeparator4 = new javax.swing.JSeparator();
        broadcastPanel = new javax.swing.JPanel();
        broadcastLabel = new javax.swing.JLabel();
        tupleButton = new javax.swing.JButton();
        matrixButton = new javax.swing.JButton();
        networkButton = new javax.swing.JButton();
        listButton = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        layoutPanel = new javax.swing.JPanel();
        layoutLabel = new javax.swing.JLabel();
        layoutComboBox = new javax.swing.JComboBox();
        speciesPanel = new javax.swing.JPanel();
        speciesLabel = new javax.swing.JLabel();
        speciesText = new javax.swing.JTextField();
        textPanel = new javax.swing.JPanel();
        dataTypeText = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        messageText = new javax.swing.JTextPane();
        jSeparator3 = new javax.swing.JSeparator();

        setFont(new java.awt.Font("Lucida Grande", 0, 12));
        setPreferredSize(new java.awt.Dimension(300, 600));
        //gooseComboBox.setModel(new javax.swing.DefaultComboBoxModel());
        gooseComboBox.setToolTipText("List of Geese currently connected to Gaggle");
        gooseComboBox.setMaximumSize(new java.awt.Dimension(120, 30));
        gooseComboBox.setMinimumSize(new java.awt.Dimension(80, 27));

        connectButton.setFont(new java.awt.Font("Lucida Grande", 0, 10));
        connectButton.setText("Disconnect");
        connectButton.setToolTipText("Update list of Geese");
        connectButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, java.awt.Color.lightGray, java.awt.Color.white, java.awt.Color.darkGray, java.awt.Color.gray));
        connectButton.setEnabled(false);

        showButton.setFont(new java.awt.Font("Lucida Grande", 0, 10));
        showButton.setText("Show");
        showButton.setToolTipText("Show selected Goose");
        showButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, java.awt.Color.lightGray, java.awt.Color.white, java.awt.Color.gray, java.awt.Color.gray));

        hideButton.setFont(new java.awt.Font("Lucida Grande", 0, 10));
        hideButton.setText("Hide");
        hideButton.setToolTipText("Hide selected Goose");
        hideButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, java.awt.Color.lightGray, java.awt.Color.white, java.awt.Color.darkGray, java.awt.Color.gray));

        org.jdesktop.layout.GroupLayout gooseControlPanelLayout = new org.jdesktop.layout.GroupLayout(gooseControlPanel);
        gooseControlPanel.setLayout(gooseControlPanelLayout);
        gooseControlPanelLayout.setHorizontalGroup(
            gooseControlPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(gooseControlPanelLayout.createSequentialGroup()
                .add(9, 9, 9)
                .add(connectButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 73, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(showButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 43, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(hideButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 39, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
            .add(gooseControlPanelLayout.createSequentialGroup()
                .add(gooseComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 225, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(22, 22, 22))
        );
        gooseControlPanelLayout.setVerticalGroup(
            gooseControlPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, gooseControlPanelLayout.createSequentialGroup()
                .addContainerGap(14, Short.MAX_VALUE)
                .add(gooseComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 27, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(gooseControlPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(connectButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(showButton)
                    .add(hideButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 18, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
        );

        jSeparator1.setMaximumSize(new java.awt.Dimension(50, 10));

        //nextWorkflowLabel.setFont(new java.awt.Font("Lucida Grande", 0, 12));
        //nextWorkflowLabel.setText("Next Workflow Component:");

        nextWorkflowText.setBackground(new java.awt.Color(230, 230, 230));
        nextWorkflowText.setFont(new java.awt.Font("Lucida Grande", 0, 12));
        nextWorkflowText.setToolTipText("Shows the next component of the workflow");
        nextWorkflowText.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createTitledBorder(null, "Next Workflow Component:", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.DEFAULT_POSITION), "", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Lucida Grande", 0, 12)));
        nextWorkflowText.setFocusable(false);

        nextWorkflowDataText.setBackground(new java.awt.Color(230, 230, 230));
        nextWorkflowDataText.setFont(new java.awt.Font("Lucida Grande", 0, 12));
        nextWorkflowDataText.setToolTipText("Shows the data type to be broadcast to the next component of the workflow");
        nextWorkflowDataText.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createTitledBorder(null, "Data Broadcast to the Next Workflow Component:", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.DEFAULT_POSITION), "", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Lucida Grande", 0, 12)));
        nextWorkflowDataText.setFocusable(false);

        nextWorkflowButton.setFont(new java.awt.Font("Lucida Grande", 0, 10));
        nextWorkflowButton.setText("Next");
        nextWorkflowButton.setToolTipText("Broadcast data to the next workflow component");
        nextWorkflowButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, java.awt.Color.lightGray, java.awt.Color.white, java.awt.Color.darkGray, java.awt.Color.gray));
        nextWorkflowButton.setEnabled(true);

        org.jdesktop.layout.GroupLayout workflowPanelLayout = new org.jdesktop.layout.GroupLayout(workflowPanel);
        workflowPanel.setLayout(workflowPanelLayout);
        workflowPanelLayout.setHorizontalGroup(
                workflowPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(workflowPanelLayout.createSequentialGroup()
                                .add(workflowPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                        .add(nextworkListScrollPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 250, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(nextWorkflowDataText, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 250, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                        .add(nextWorkflowButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 30, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                )
                                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        workflowPanelLayout.setVerticalGroup(
                workflowPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(workflowPanelLayout.createSequentialGroup()
                                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .add(nextworkListScrollPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 40, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(1, 1, 1)
                                .add(nextWorkflowDataText, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 80, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(1, 1, 1)
                                .add(nextWorkflowButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        )
        );
        jSeparator4.setMaximumSize(new java.awt.Dimension(50, 10));

        broadcastLabel.setFont(new java.awt.Font("Lucida Grande", 0, 12));
        broadcastLabel.setText("Broadcast Data");

        tupleButton.setFont(new java.awt.Font("Lucida Grande", 0, 10));
        tupleButton.setText("Tuple");
        tupleButton.setToolTipText("Broadcast HashMap ");
        tupleButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, java.awt.Color.lightGray, java.awt.Color.white, java.awt.Color.darkGray, java.awt.Color.gray));

        matrixButton.setFont(new java.awt.Font("Lucida Grande", 0, 10));
        matrixButton.setText("Matrix");
        matrixButton.setToolTipText("Broadcast Matrix");
        matrixButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, java.awt.Color.lightGray, java.awt.Color.white, java.awt.Color.darkGray, java.awt.Color.gray));

        networkButton.setFont(new java.awt.Font("Lucida Grande", 0, 10));
        networkButton.setText("Network");
        networkButton.setToolTipText("Broadcast Network\n");
        networkButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, java.awt.Color.lightGray, java.awt.Color.white, java.awt.Color.darkGray, java.awt.Color.gray));

        listButton.setFont(new java.awt.Font("Lucida Grande", 0, 10));
        listButton.setText("List");
        listButton.setToolTipText("Broadcast List");
        listButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED, java.awt.Color.lightGray, java.awt.Color.white, java.awt.Color.darkGray, java.awt.Color.gray));

        org.jdesktop.layout.GroupLayout broadcastPanelLayout = new org.jdesktop.layout.GroupLayout(broadcastPanel);
        broadcastPanel.setLayout(broadcastPanelLayout);
        broadcastPanelLayout.setHorizontalGroup(
            broadcastPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(broadcastPanelLayout.createSequentialGroup()
                .add(broadcastPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(broadcastLabel)
                    .add(broadcastPanelLayout.createSequentialGroup()
                        .add(tupleButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 45, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(matrixButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 50, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(networkButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 54, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(listButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)))
                .addContainerGap())
        );
        broadcastPanelLayout.setVerticalGroup(
            broadcastPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(broadcastPanelLayout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(broadcastLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(broadcastPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(tupleButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(matrixButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(networkButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(listButton)))
        );

        jSeparator2.setMaximumSize(new java.awt.Dimension(50, 10));

        layoutLabel.setFont(new java.awt.Font("Lucida Grande", 0, 12));
        layoutLabel.setText("Select Default Layout:");

        layoutComboBox.setFont(new java.awt.Font("Lucida Grande", 0, 12));
        layoutComboBox.setToolTipText("Set default layout for gaggle networks");
        layoutComboBox.setMaximumSize(new java.awt.Dimension(100, 30));
        layoutComboBox.setMinimumSize(new java.awt.Dimension(50, 27));

        org.jdesktop.layout.GroupLayout layoutPanelLayout = new org.jdesktop.layout.GroupLayout(layoutPanel);
        layoutPanel.setLayout(layoutPanelLayout);
        layoutPanelLayout.setHorizontalGroup(
            layoutPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layoutPanelLayout.createSequentialGroup()
                .add(layoutPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(layoutLabel)
                    .add(layoutComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 200, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layoutPanelLayout.setVerticalGroup(
            layoutPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layoutPanelLayout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(layoutLabel)
                .add(1, 1, 1)
                .add(layoutComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 25, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        speciesLabel.setFont(new java.awt.Font("Lucida Grande", 0, 12));
        speciesLabel.setText("Species used in broadcasts:");

        org.jdesktop.layout.GroupLayout speciesPanelLayout = new org.jdesktop.layout.GroupLayout(speciesPanel);
        speciesPanel.setLayout(speciesPanelLayout);
        speciesPanelLayout.setHorizontalGroup(
            speciesPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(speciesPanelLayout.createSequentialGroup()
                .add(speciesLabel)
                .add(48, 48, 48))
            .add(speciesPanelLayout.createSequentialGroup()
                .add(speciesText, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 189, Short.MAX_VALUE)
                .addContainerGap())
        );
        speciesPanelLayout.setVerticalGroup(
            speciesPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(speciesPanelLayout.createSequentialGroup()
                .add(speciesLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 11, Short.MAX_VALUE)
                .add(speciesText, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        dataTypeText.setBackground(new java.awt.Color(230, 230, 230));
        dataTypeText.setFont(new java.awt.Font("Lucida Grande", 0, 12));
        dataTypeText.setToolTipText("Shows the data type currently being broadcast to a Cytoscape Goose");
        dataTypeText.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createTitledBorder(null, "Current Data Type:", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.DEFAULT_POSITION), "", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Lucida Grande", 0, 12)));
        dataTypeText.setFocusable(false);

        messageText.setBackground(new java.awt.Color(230, 230, 230));
        messageText.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Messages:", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Lucida Grande", 0, 12)));
        messageText.setEditable(false);
        messageText.setFont(new java.awt.Font("Lucida Grande", 0, 12));
        messageText.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        messageText.setFocusable(false);
        messageText.setVerifyInputWhenFocusTarget(false);
        jScrollPane1.setViewportView(messageText);

        org.jdesktop.layout.GroupLayout textPanelLayout = new org.jdesktop.layout.GroupLayout(textPanel);
        textPanel.setLayout(textPanelLayout);
        textPanelLayout.setHorizontalGroup(
            textPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(dataTypeText, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 222, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 222, Short.MAX_VALUE)
        );
        textPanelLayout.setVerticalGroup(
            textPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(textPanelLayout.createSequentialGroup()
                .add(dataTypeText, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 49, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(layout.createSequentialGroup()
                        .add(gooseControlPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(4, 4, 4))
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                                .add(org.jdesktop.layout.GroupLayout.LEADING, jSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 200, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(org.jdesktop.layout.GroupLayout.LEADING, workflowPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                )
                                .add(17, 17, 17))
                    .add(layout.createSequentialGroup()
                        .add(jSeparator4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 200, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(51, Short.MAX_VALUE))
                    .add(layout.createSequentialGroup()
                        .add(broadcastPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(51, Short.MAX_VALUE))
                    .add(layout.createSequentialGroup()
                        .add(jSeparator2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 198, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(53, Short.MAX_VALUE))
                    .add(layout.createSequentialGroup()
                        .add(textPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .add(layout.createSequentialGroup()
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, jSeparator3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 200, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, speciesPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                                .add(layoutPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 197, Short.MAX_VALUE)
                                .add(12, 12, 12)))
                        .add(17, 17, 17))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(gooseControlPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(workflowPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(21, 21, 21)
                .add(jSeparator4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(broadcastPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSeparator2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layoutPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(21, 21, 21)
                .add(speciesPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSeparator3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(1, 1, 1)
                .add(textPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(51, Short.MAX_VALUE))
        );
    }// </editor-fold>
    
    
    // Variables declaration - do not modify
    private javax.swing.JLabel workflowTitleLabel;
    private javax.swing.JPanel workflowPanel;
    private javax.swing.JList nextWorkflowText;
    private javax.swing.JScrollPane nextworkListScrollPanel;
    private javax.swing.JPanel nextWorkflowDataText;
    private javax.swing.ButtonGroup dataRadioButtonGroup;
    private javax.swing.JRadioButton dataRadioButtons[] = new JRadioButton[4];
    private javax.swing.JButton nextWorkflowButton;

    private javax.swing.JLabel broadcastLabel;
    private javax.swing.JPanel broadcastPanel;
    private javax.swing.JButton connectButton;
    private javax.swing.JTextField dataTypeText;
    private javax.swing.JComboBox gooseComboBox;
    private javax.swing.JPanel gooseControlPanel;
    private javax.swing.JButton hideButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JComboBox layoutComboBox;
    private javax.swing.JLabel layoutLabel;
    private javax.swing.JPanel layoutPanel;
    private javax.swing.JButton listButton;
    private javax.swing.JButton matrixButton;
    private javax.swing.JTextPane messageText;
    private javax.swing.JButton networkButton;
    private javax.swing.JButton showButton;
    private javax.swing.JLabel speciesLabel;
    private javax.swing.JPanel speciesPanel;
    private javax.swing.JTextField speciesText;
    private javax.swing.JPanel textPanel;
    private javax.swing.JButton tupleButton;
    // End of variables declaration
}
