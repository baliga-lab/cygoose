package org.systemsbiology.cytoscape;

import com.install4j.runtime.installer.helper.Logger;
import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.CytoscapeInit;
import cytoscape.CytoscapeVersion;
import cytoscape.init.CyInitParams;
import cytoscape.logger.CyLogger;
import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.layout.CyLayouts;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.view.cytopanels.CytoPanel;
import cytoscape.view.CytoscapeDesktop;
import org.biojava.bio.symbol.IntegerAlphabet;
import org.systemsbiology.cytoscape.dialog.GooseDialog;
import org.systemsbiology.cytoscape.dialog.GooseDialog.GooseButton;
import org.systemsbiology.gaggle.core.Boss;
import org.systemsbiology.gaggle.core.GooseWorkflowManager;
import org.systemsbiology.gaggle.core.datatypes.WorkflowAction;
import org.systemsbiology.gaggle.core.datatypes.WorkflowComponent;
import org.systemsbiology.gaggle.geese.common.GaggleConnectionListener;
import org.systemsbiology.gaggle.geese.common.GooseShutdownHook;
import org.systemsbiology.gaggle.geese.common.RmiGaggleConnector;
import org.systemsbiology.gaggle.util.MiscUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 * Cytoscape-Gaggle Plugin.
 * @author Sarah Killcoyne, Wei-ju Wu
 */
public class GagglePlugin extends CytoscapePlugin
implements PropertyChangeListener, GaggleConnectionListener,
           GooseListChangedListener {

    interface GooseAction {
        public void doGoose(String gooseNetworkId, CyGoose goose) throws RemoteException;
    }

    protected static CyLogger logger = CyLogger.getLogger(GagglePlugin.class);

    private boolean registered;
    private CyGoose defaultGoose;

    private static org.systemsbiology.gaggle.core.Boss gaggleBoss;
    private static boolean connectedToGaggle = false;
    private static GooseDialog gDialog;
    private static CyBroadcast broadcast;
    private static boolean pluginInitialized = false;

    public static String ORIGINAL_GOOSE_NAME;
    protected static String myGaggleName;
    // keeps track of all the network ids key = network id  value = goose
    private static Map<String, CyGoose> networkGeese;
    private static Set<String> species;

    private GooseWorkflowManager workflowManager = new GooseWorkflowManager();

    public GagglePlugin() {
        // constructor gets called at load time and every time the toolbar is used
        if (pluginInitialized) return;

        Cytoscape.getPropertyChangeSupport().addPropertyChangeListener(this);

        // Register listeners to menu items to record actions for workflows
        registerMenuListeners();

        ORIGINAL_GOOSE_NAME = "Cytoscape" + " v." + new CytoscapeVersion().getFullVersion();
        myGaggleName = ORIGINAL_GOOSE_NAME;

        networkGeese = new HashMap<String, CyGoose>();
        gDialog = new GooseDialog();

        CytoPanel GooseCyPanel =
            Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
        GooseCyPanel.add("CyGoose", gDialog);
        GooseCyPanel.setSelectedIndex(GooseCyPanel.indexOfComponent(gDialog));
        initUI();

        try {
            // this gives an initial goose that is cytoscape with a null network
            createDefaultGoose();
            gDialog.displayMessage("Connected To Gaggle Boss");
            registered = true;
        } catch (RemoteException ex) {
            registered = false;
            this.gDialog.displayMessage("Not connected to Gaggle Boss");
            ex.printStackTrace();
        }

        broadcast = new CyBroadcast(gDialog, gaggleBoss);
        pluginInitialized = true;

        // Update the context class loader, this is necessary to fix the bug introduced by Java 7u25
        //this.gDialog.setContextClassLoader();
    }

    public GooseWorkflowManager getWorkflowManager() { return workflowManager; }

    private String getTargetGoose() {
        int targetGooseIndex = this.gDialog.getGooseChooser().getSelectedIndex();
        String targetGooseName = (String) this.gDialog.getGooseChooser().getSelectedItem();
        logger.info("Target index: " + targetGooseIndex + "  Target item: " + targetGooseName);
        return targetGooseName;
    }

    private void registerMenuListeners()
    {
        // Select Node menu
        JMenu nodesSubmenu = (JMenu)Cytoscape.getDesktop().getCyMenus().getSelectMenu().getItem(1);
        for (int i = 0; i < nodesSubmenu.getItemCount(); i++)
        {
            final int menuindex = i;
            JMenuItem item = nodesSubmenu.getItem(i);
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    //To change body of implemented methods use File | Settings | File Templates.
                    if (gDialog.getWorkflowStarted())
                    {
                        logger.info("Select Nodes submenu item performed " + e.getActionCommand() + " " + new Integer(menuindex).toString());

                    }
                }
            });
        }


        // Select Edge menu
        JMenu edgesSubmenu = (JMenu)Cytoscape.getDesktop().getCyMenus().getSelectMenu().getItem(2);
        for (int i = 0; i < edgesSubmenu.getItemCount(); i++)
        {
            final int menuindex = i;
            JMenuItem item = nodesSubmenu.getItem(i);
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    //To change body of implemented methods use File | Settings | File Templates.
                    if (gDialog.getWorkflowStarted())
                    {
                        logger.info("Select Nodes submenu item performed " + e.getActionCommand() + " " + new Integer(menuindex).toString());
                    }
                }
            });
        }
    }


    public void gooseListChanged(String[] gooseList) {
        logger.debug("gooseListChanged() " + gooseList.toString());
        if (connectedToGaggle) {
            if (gDialog.getGooseChooser() != null && defaultGoose != null) {
                logger.debug("how many geese are there? " + networkGeese.size());
                logger.debug("is defaultGoose null? " + (defaultGoose == null));
                MiscUtil.updateGooseChooser(gDialog.getGooseChooser(),
                                            defaultGoose.getName(),
                                            defaultGoose.getActiveGooseNames());
                gDialog.getGooseChooser().setSelectedIndex(0);
            }

        }
    }

    public void setConnected(boolean connected, Boss boss) {
        gaggleBoss = boss;
        connectedToGaggle = connected;
        gDialog.setConnectButtonStatus(connected);
        if (connectedToGaggle) {
            gDialog.displayMessage("Connected To Gaggle Boss");
            if (gDialog.getGooseChooser() != null && defaultGoose != null) {
                Cytoscape.getDesktop().setTitle(defaultGoose.getName());
                MiscUtil.updateGooseChooser(gDialog.getGooseChooser(),
                                            defaultGoose.getName(),
                                            defaultGoose.getActiveGooseNames());
                gDialog.getGooseChooser().setSelectedIndex(0);
            }
        } else {
            gDialog.displayMessage("Not connected to Gaggle Boss");
        }
    }

    // creates the "null" network goose
    private void createDefaultGoose() throws RemoteException {
        logger.info("Creating default goose...");
        // this gives an initial goose that is cytoscape with a null network
        CyNetwork CurrentNet = Cytoscape.getNullNetwork();
        CurrentNet.setTitle(myGaggleName);
        defaultGoose = this.createNewGoose(CurrentNet);
        defaultGoose.addGooseListChangedListener(this);

        networkGeese.put(CurrentNet.getIdentifier(), defaultGoose);
        logger.info("size of active names: " + defaultGoose.getActiveGooseNames().length);
        for (String g : defaultGoose.getActiveGooseNames()) logger.info(g);

        if (gDialog.getGooseChooser() == null)
            logger.info("goose dialog NULL before updating");

        MiscUtil.updateGooseChooser(gDialog.getGooseChooser(),
                                    defaultGoose.getName(),
                                    defaultGoose.getActiveGooseNames());
        gDialog.getGooseChooser().setSelectedIndex(0);

        Cytoscape.getDesktop().setTitle(defaultGoose.getName());
    }

    // Network created: create goose  Network destroyed: remove goose Network
    // title changed: change the goose name
    public void propertyChange(final PropertyChangeEvent Event) {
        // nothing has been registered, don't try to handle events
        if (!registered) return;

        Runnable propertyChangeTask = new Runnable() {
            public void run() {

                if (Event.getPropertyName() == Cytoscape.NETWORK_TITLE_MODIFIED) {
                    // change the goose name
                    logger.info("===== EVENT " + Event.getPropertyName() + "======");
                    try { // this allows the goose to work in 2.5 as well
                        Class titleChange = Class.forName("cytoscape.CyNetworkTitleChange");
                        cytoscape.CyNetworkTitleChange OldTitle =
                            (cytoscape.CyNetworkTitleChange) Event.getOldValue();
                        cytoscape.CyNetworkTitleChange NewTitle =
                            (cytoscape.CyNetworkTitleChange) Event.getNewValue();

                        // this should always be true but if somehow it ain't....
                        if (!OldTitle.getNetworkIdentifier().equals(NewTitle.getNetworkIdentifier())) {
                            logger.warn("ERROR: " + Cytoscape.NETWORK_TITLE_MODIFIED +
                                        " event does not refer to the same networks!");
                            return;
                        } else {
                            logger.info("Obtaining Goose for " + NewTitle.getNetworkIdentifier());
                            CyGoose goose = networkGeese.get(NewTitle.getNetworkIdentifier());
                            if (goose != null &&
                                !goose.getName().equals(NewTitle.getNetworkTitle())) {
                                try {
                                    String NewGooseName =
                                        gaggleBoss.renameGoose(goose.getName(),
                                                                    NewTitle.getNetworkTitle());
                                    Cytoscape.getNetwork(goose.getNetworkId()).setTitle(NewGooseName);
                                    logger.info("Goose renamed to " + goose.getName() + " " + goose.getNetworkId());
                                } catch (RemoteException re) {
                                    re.printStackTrace();
                                }
                            }
                        }
                    } catch (java.lang.ClassNotFoundException ex) {
                        logger.error("Caught a ClassNotFoundException for " +
                                     "cytoscape.CyNetworkTitleChange");
                    }
                } else if (Event.getPropertyName() == Cytoscape.NETWORK_CREATED) {
                    // register a goose
                    logger.info("==== Event " + Event.getPropertyName() + "====");
                    String netId = Event.getNewValue().toString();
                    CyNetwork net = Cytoscape.getNetwork(netId);

                    try {
                        CyGoose NewGoose = createNewGoose(net);
                        logger.info("Saved new goose " + NewGoose.getName() + " for " + net.getIdentifier());
                        networkGeese.put(net.getIdentifier(), NewGoose);
                        MiscUtil.updateGooseChooser(gDialog.getGooseChooser(),
                                                "ADummyString",
                                                NewGoose.getActiveGooseNames());
                    } catch (RemoteException ex) {
                        GagglePlugin.showDialogBox("Failed to create a new Goose for network " +
                                                   net.getTitle(), "Error", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                } else if (Event.getPropertyName() == Cytoscape.NETWORK_DESTROYED) {
                    // remove a goose
                    logger.info("==== Event " + Event.getPropertyName() + "====");
                    String netId = Event.getNewValue().toString();
                    CyNetwork net = Cytoscape.getNetwork(netId);

                    String Name = "";
                    try {
                        CyGoose OldGoose = (CyGoose) networkGeese.get(net.getIdentifier());
                        Name = OldGoose.getName();
                        gaggleBoss.unregister(OldGoose.getName());
                        UnicastRemoteObject.unexportObject(OldGoose, true);
                        gDialog.getGooseChooser().removeItem(OldGoose.getName());
                    } catch (RemoteException ex) {
                        GagglePlugin.showDialogBox("Failed to remove goose '" + Name +
                                                   "' from Boss", "Warning",
                                                   JOptionPane.WARNING_MESSAGE);
                        ex.printStackTrace();
                    }
                } else if (Event.getPropertyName() == CytoscapeDesktop.NETWORK_VIEW_FOCUSED) {
                    logger.info("=== Event " + Event.getPropertyName() + "===");
                    CyGoose current = networkGeese.get(Cytoscape.getCurrentNetwork().getIdentifier());
                    gDialog.setSpeciesText(current.getSpeciesName());
                    // Set the next workflow component text
                    String requestID = gDialog.getRequestID(Cytoscape.getCurrentNetwork().getIdentifier());
                    WorkflowAction action = workflowManager.getWorkflowAction(requestID);
                    if (action.getTargets() != null && action.getTargets().length > 0)
                        logger.info("Data type for target 0: " + action.getTargets()[0].getParams().get(WorkflowComponent.ParamNames.EdgeType.getValue()));
                    gDialog.setWorkflowUI(action, requestID);
                }
            }
        };
        gDialog.invokeLater2(propertyChangeTask);
    }

    public void onCytoscapeExit() {
        logger.info("GagglePlugin in onCytoscapeExit()");
        mapToGeese(new GooseAction() {
                public void doGoose(String gooseNetworkId, CyGoose goose)
                    throws RemoteException {
                    if (!"0".equals(goose.getNetworkId())) {
                        try {
                            gaggleBoss.unregister(goose.getName());
                        } catch (RemoteException ex) {
                            logger.info("Error disconnecting from gaggle, goose may " +
                                        "have already been disconnected by user.");
                            ex.printStackTrace();
                        }
                    }
                }
            });
        logger.info("leaving onCytoscapeExit()");
    }

    /*
     * Creates a new goose for the given network
     */
    private CyGoose createNewGoose(CyNetwork Network)
        throws RemoteException, IllegalArgumentException {
        logger.info("createNewGoose(): initial network name: " + Network.getTitle());
        CyGoose Goose = new CyGoose(gDialog);//, gaggleBoss);
        Goose.setNetworkId(Network.getIdentifier());
        // Set the species of the new goose to what is set on the gDialog
        logger.info("Current gaggle dialog species " + gDialog.getSpecies());
        Goose.setSpeciesName(gDialog.getSpecies());
        // Include the original goose name to the goose name for recording workflow purpose
        logger.info("Create new goose get network title: " + Network.getTitle());
        Goose.setName(Network.getTitle());
        //Goose.setName(ORIGINAL_GOOSE_NAME Network.getTitle() + "_" + CyGoose.instanceCount + "_" + );
        RmiGaggleConnector connector = new RmiGaggleConnector(Goose);
        connector.addListener(this);
        if (Network.getIdentifier().equals("0")) {
            new GooseShutdownHook(connector);
        }
        else
        {
            String workflowRequestID =  gDialog.getRequestID("0");
            if (workflowRequestID != null && workflowRequestID.length() > 0)
            {
                // HACK HACK associate the new goose with the workflow request ID
                gDialog.addRequestNetwork(Network.getIdentifier(), workflowRequestID, true);
            }
        }
        try {
            connector.connectToGaggle();
        } catch (Exception ex) {
            throw new RemoteException(ex.getMessage());
        }

        logger.info("goose name after registration: " + Goose.getName());
        logger.info("setting title on network " + Network.getIdentifier());
        Network.setTitle(Goose.getName());
        gaggleBoss = (org.systemsbiology.gaggle.core.Boss3)connector.getBoss();
        Goose.setBoss(gaggleBoss);
        if (gaggleBoss instanceof org.systemsbiology.gaggle.core.Boss3)
        {
            // Get process id and send it to boss
            try
            {
                //String workDir = System.getProperty("user.dir");
                //workDir += ("/" + ORIGINAL_GOOSE_NAME + ".exe");

                //java.lang.management.RuntimeMXBean runtime = java.lang.management.ManagementFactory.getRuntimeMXBean();
                //java.lang.reflect.Field jvm = runtime.getClass().getDeclaredField("jvm");
                //jvm.setAccessible(true);
                //sun.management.VMManagement mgmt = (sun.management.VMManagement) jvm.get(runtime);
                //java.lang.reflect.Method pid_method = mgmt.getClass().getDeclaredMethod("getProcessId");
                //pid_method.setAccessible(true);
                //int pid = (Integer) pid_method.invoke(mgmt);
                //logger.info("Goose process id: " + pid);

                logger.info("Goose query string: Exe.Name.ct=cytoscape");
                String query = "Exe.Name.ct=Cytoscape";
                ((org.systemsbiology.gaggle.core.Boss3)gaggleBoss).recordAction("Cytoscape",
                        null, query, -1, null, null, null);
            }
            catch (Exception e)
            {
                logger.error("Failed to record app name " + e.getMessage());
            }
        }
        return Goose;
    }


    private void initUI() {
        //if (CytoscapeInit.getCyInitParams().getMode() == CyInitParams.GUI)
        logger.info("initUI()");
        // layouts
        java.util.Collection<CyLayoutAlgorithm> Layouts = CyLayouts.getAllLayouts();
        gDialog.getLayoutChooser().addItem("Default");
        for (CyLayoutAlgorithm current : Layouts) {
            gDialog.getLayoutChooser().addItem(current.getName());
        }

        gDialog.setSpeciesText(CytoscapeInit.getProperties().getProperty("defaultSpeciesName"));
        
        addBroadcastNameListAction();
        addBroadcastNetworkAction();
        addBroadcastMatrixAction();
        addBroadcastTupleAction();
        addConnectAction();
        addNextWorkflowAction();
        addWorkflowListListener();
    }

    private void addBroadcastNameListAction() {
        gDialog.addButtonAction(GooseButton.LIST, new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    try {
                        CyNetwork Network = Cytoscape.getCurrentNetwork();
                        CyGoose Goose = networkGeese.get(Network.getIdentifier());
                        String TargetGoose = getTargetGoose();

                        broadcast.broadcastNameList(Goose, TargetGoose);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
    }

    private void addBroadcastNetworkAction() {
        gDialog.addButtonAction(GooseButton.NETWORK, new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    logger.info("Net action");
                    try {
                        CyNetwork Network = Cytoscape.getCurrentNetwork();
                        CyGoose Goose = networkGeese.get(Network.getIdentifier());
                        String TargetGoose = getTargetGoose();

                        broadcast.broadcastNetwork(Goose, TargetGoose);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
    }

    private void addBroadcastMatrixAction() {
        gDialog.addButtonAction(GooseButton.MATRIX, new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    try {
                        CyNetwork Network = Cytoscape.getCurrentNetwork();
                        CyGoose Goose = networkGeese.get(Network.getIdentifier());
                        String TargetGoose = getTargetGoose();

                        broadcast.broadcastDataMatrix(Goose, TargetGoose);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
    }

    private void addBroadcastTupleAction() {
        gDialog.addButtonAction(GooseButton.TUPLE, new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    try {
                        CyNetwork Network = Cytoscape.getCurrentNetwork();
                        CyGoose Goose = networkGeese.get(Network.getIdentifier());
                        String TargetGoose = getTargetGoose();
                        broadcast.broadcastTuple(Goose, TargetGoose);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
    }

    private void addConnectAction() {
        logger.info("in connectAction");
        gDialog.addButtonAction(GooseButton.CONNECT, new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    logger.info("in connectAction's listener");
                    if (event.getActionCommand().equals("connect")) {
                        reconnectAllGeese();
                    } else if (event.getActionCommand().equals("disconnect")) {
                        disconnectAllGeese();
                    }
                }
            });
    }

    private void addNextWorkflowAction() {
        logger.info("in nextWorkflowAction");
        gDialog.addButtonAction(GooseButton.Next, new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                logger.info("in nextWorkflowAction's listener");
                CyNetwork Network = Cytoscape.getCurrentNetwork();
                logger.info("Current Network Id: " + Network.getIdentifier());
                CyGoose Goose = networkGeese.get(Network.getIdentifier());
                if (Goose != null)
                {
                    logger.info("Obtained goose: " + Goose.getName() + " " + Goose.getNetworkId());
                    String requestID = gDialog.getRequestID(Network.getIdentifier());
                    if (requestID != null)
                    {
                        logger.info("In Next workflow handler for " + requestID);
                        broadcast.NextWorkflowActionHandler(Goose, gDialog.getWorkflowManager(), requestID);
                    }
                    else
                        logger.warning("Couldn't find requestID for " + Network.getIdentifier());
                }
                else
                    logger.warning("Couldn't find Goose for Network: " + Network.getIdentifier());
            }
        });
    }

    private void addWorkflowListListener()
    {
        logger.info("Add workflow list listener");
        ListSelectionListener listSelectionListener = new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                logger.info("First index: " + listSelectionEvent.getFirstIndex());
                logger.info(", Last index: " + listSelectionEvent.getLastIndex());
                boolean adjust = listSelectionEvent.getValueIsAdjusting();
                logger.info(", Adjusting? " + adjust);
                if (!adjust) {
                    JList list = (JList) listSelectionEvent.getSource();
                    int selections[] = list.getSelectedIndices();
                    Object selectionValues[] = list.getSelectedValues();
                    for (int i = 0, n = selections.length; i < n; i++) {
                        logger.info("Selected goose index: " + selections[i]);
                        gDialog.setWorkflowDataType(selections[i]);
                    }
                }
            }
        };
        gDialog.getWorkflowGeeseList().addListSelectionListener(listSelectionListener);

    }

    private void reconnectAllGeese() {
        mapToGeese(new GooseAction() {
                public void doGoose(String gooseNetworkId, CyGoose goose)
                    throws RemoteException {
                    String newName = gaggleBoss.register(goose);
                    if (gooseNetworkId.equals("0")) {
                        Cytoscape.getDesktop().setTitle(newName);
                    } else {
                        CyNetwork cyNet = Cytoscape.getNetwork(gooseNetworkId);
                        logger.info("old name: " + cyNet.getTitle());
                        cyNet.setTitle(newName);
                        logger.info("new name is: " + cyNet.getIdentifier());
                    }
                }
            });
        gDialog.setConnectButtonStatus(true);
    }

    private void disconnectAllGeese() {
        mapToGeese(new GooseAction() {
                public void doGoose(String gooseNetworkId, CyGoose goose)
                    throws RemoteException{     
                    gaggleBoss.unregister(goose.getName());
                }
            });

        gDialog.setConnectButtonStatus(false);
        myGaggleName = ORIGINAL_GOOSE_NAME;
        defaultGoose.setName(ORIGINAL_GOOSE_NAME);
        Cytoscape.getDesktop().setTitle(ORIGINAL_GOOSE_NAME);
    }

    private void mapToGeese(GooseAction action) {
        try {
            for (Map.Entry<String, CyGoose> entry : networkGeese.entrySet()) {
                action.doGoose(entry.getKey(), entry.getValue());
            }
        } catch (Exception ex) {
            // TODO quit popping box up, add error message bar to goose panel
            gDialog.setConnectButtonStatus(false);
            gDialog.displayMessage("Failed to communicate with Gaggle");
            ex.printStackTrace();
        }
    }

    public static void showDialogBox(String message, String title, int msgType) {
        JOptionPane.showMessageDialog(Cytoscape.getDesktop(), message, title, msgType);
    }
}
