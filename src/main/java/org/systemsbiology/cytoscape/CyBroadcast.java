package org.systemsbiology.cytoscape;

import EDU.oswego.cs.dl.util.concurrent.ThreadedExecutor;
import com.sosnoski.util.array.ObjectArray;
import com.sosnoski.util.array.StringArray;
import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.Semantics;
import cytoscape.logger.CyLogger;
import org.systemsbiology.cytoscape.dialog.CyAttrDialog;
import org.systemsbiology.cytoscape.dialog.GooseDialog;
import org.systemsbiology.gaggle.core.Boss;
import org.systemsbiology.gaggle.core.Boss3;
import org.systemsbiology.gaggle.core.GooseWorkflowManager;
import org.systemsbiology.gaggle.core.datatypes.*;

import javax.swing.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/**
 *  @author Sarah Killcoyne, Wei-ju Wu
 */
public class CyBroadcast {
    private static CyLogger logger = CyLogger.getLogger(CyBroadcast.class);

    private GooseDialog gDialog;
    private Boss gaggleBoss;

    private String broadcastID = "ID";

    // text strings for popup dialog boxes
    private String broadcastStr = "The following checked attributes will be " +
        "captured for broadcast.\nClick \"OK\" to proceed or \"Cancel\" to " +
        "cancel transaction.";

    public CyBroadcast(GooseDialog Dialog, Boss boss) {
        this.gDialog = Dialog;
        this.gaggleBoss = boss;
    }

    // very basic, for the moment we will only broadcast by ID
    public void broadcastNameList(CyGoose goose, String targetGoose) {
        logger.info("broadcastNameList");

        Namelist namelist = generateNamelist(goose);
        try {
            gaggleBoss.broadcastNamelist(goose.getName(), targetGoose, namelist);
        } catch (Exception ex) {
            String msg = "Failed to broadcast list of names to " + targetGoose;
            showError(msg);
            logger.error(msg, ex);
        }
    }

    private Namelist generateNamelist(CyGoose goose)
    {
        if (getSelectedNodes(goose).size() == 0) {
            showWarning("No nodes were selected for broadcast.");
            return null;
        }
        List<String> selectedIds = new ArrayList<String>();
        for (CyNode selectedNode : getSelectedNodes(goose)) {
            selectedIds.add(selectedNode.getIdentifier());
        }

        Namelist namelist = new Namelist();
        namelist.setSpecies(gDialog.getSpecies());
        namelist.setNames(selectedIds.toArray(new String[0]));
        return namelist;
    }

    // TODO broadcast edge attributes as well
    // broadcasts hash of selected attributes
    public void broadcastTuple(final CyGoose goose, final String targetGoose) {
        logger.info("broadcastTuple");

        if (getSelectedNodes(goose).size() == 0) {
            showWarning("No nodes selected for broadcast.");
            return;
        }

        // pass string of attribute names
        String[] allAttrNames = Cytoscape.getNodeAttributes().getAttributeNames();

        AttrSelectAction okAction = new AttrSelectAction() {
                public void takeAction(String[] selectAttr) {
                    broadcastTuple(selectAttr, goose, targetGoose);
                }
            };

        delegateProcessTuple(goose, targetGoose, okAction);
    }

    private void broadcastTuple(String[] attrNames, CyGoose goose,
                                String targetGoose) {
        GaggleTuple gaggleTuple = generateTuple(attrNames, goose);

        try {
            gaggleBoss.broadcastTuple(goose.getName(), targetGoose, gaggleTuple);
        } catch (Exception ex) {
            String msg = "Failed to broadcast map to " + targetGoose;
            showError(msg);
            logger.error(msg, ex);
        }
    }

    private void broadcastTupleToWorkflow(String[] attrNames, CyGoose goose,
                                         String requestID, int targetIndex,
                                         Object syncObj,
                                         GooseWorkflowManager workflowManager,
                                         WorkflowAction action) {
        GaggleTuple gaggleTuple = generateTuple(attrNames, goose);
        logger.info("Submitting tuple to workflow manager for request: " + requestID + " Index: " + targetIndex + " Target: " + action.getTargets()[targetIndex].getGooseName());
        workflowManager.addSessionTargetData(requestID, targetIndex, gaggleTuple);

        logger.info("broadcastTupleToWorkflow Notifying main thread...");
        synchronized (syncObj) {
            syncObj.notify();
        }
        /*if (workflowManager.CompleteWorkflowAction(gaggleBoss, requestID))
        {
            // Now we can clean up the UI
            gDialog.setWorkflowUI(null);
            gDialog.removeRequestNetwork(requestID);
        } */
    }

    private void delegateProcessTuple(CyGoose goose, String targetGoose, AttrSelectAction okAction)
    {
        if (getSelectedNodes(goose).size() == 0) {
            showWarning("No nodes selected for broadcast.");
            return;
        }

        // pass string of attribute names
        String[] allAttrNames = Cytoscape.getNodeAttributes().getAttributeNames();

        CyAttrDialog dialog = new CyAttrDialog(allAttrNames, okAction,
                CyAttrDialog.MULTIPLE_SELECT);
        dialog.setDialogText(broadcastStr);
        dialog.preSelectCheckBox(allAttrNames);
        dialog.buildDialogWin();
    }

    private GaggleTuple generateTuple(String[] attrNames, CyGoose goose)
    {
        logger.info("Generate tuple...");
        if (getSelectedNodes(goose).size() <= 0) {
            showWarning("No nodes were selected for tuple broadcast.");
            return null;
        }

        GaggleTuple gaggleTuple = new GaggleTuple();
        gaggleTuple.setSpecies(gDialog.getSpecies());
        gaggleTuple.setName(getNetworkTitle(goose)); //why?

        Set<CyNode> selectedNodes = getSelectedNodes(goose);
        Iterator<CyNode> nodeIter = selectedNodes.iterator();

        // create a string array of node names
        String[] nodeArr = new String[selectedNodes.size()];
        for (int i = 0; (i < selectedNodes.size()) && nodeIter.hasNext(); i++) {
            CyNode currentNode = (CyNode) nodeIter.next();
            nodeArr[i] = currentNode.getIdentifier();
        }

        Tuple dataTuple = new Tuple();
        Tuple metadata = new Tuple();
        metadata.addSingle(new Single("condition", "static"));
        gaggleTuple.setMetadata(metadata);

        CyAttributes nodeAtts = Cytoscape.getNodeAttributes();

        for (String attr : attrNames) {
            switch (nodeAtts.getType(attr))
            {
                case CyAttributes.TYPE_INTEGER:
                    for (String nodeName : nodeArr) {
                        Tuple row = new Tuple();
                        row.addSingle(new Single(nodeName));
                        row.addSingle(new Single(attr));
                        row.addSingle(new Single(nodeAtts.getIntegerAttribute(nodeName, attr)));
                        dataTuple.addSingle(new Single(row));
                    }
                    break;
                case CyAttributes.TYPE_FLOATING:
                    for (String nodeName : nodeArr) {
                        Tuple row = new Tuple();
                        row.addSingle(new Single(nodeName));
                        row.addSingle(new Single(attr));
                        row.addSingle(new Single(nodeAtts.getDoubleAttribute(nodeName, attr)));
                        dataTuple.addSingle(new Single(row));
                    }
                    break;
                case CyAttributes.TYPE_STRING:
                    for (String nodeName : nodeArr) {
                        if (nodeAtts.getStringAttribute(nodeName, attr) != null) {
                            Tuple row = new Tuple();
                            row.addSingle(new Single(nodeName));
                            row.addSingle(new Single(attr));
                            row.addSingle(new Single(nodeAtts.getStringAttribute(nodeName, attr)));
                            dataTuple.addSingle(new Single(row));
                        }
                    }
                    break;
            }
        }

        gaggleTuple.setData(dataTuple);
        gaggleTuple.setSpecies(this.gDialog.getSpecies());
        return gaggleTuple;
    }

    public void broadcastDataMatrix(final CyGoose goose, final String targetGoose) {
        logger.info("broadcastDataMatrix()");

        AttrSelectAction okAction = new AttrSelectAction() {
            public void takeAction(String[] selectAttr) {
                broadcastDataMatrix(selectAttr, goose, targetGoose);
            }
        };
        delegateProcessMatrix(goose, targetGoose, okAction); //false, null, null, null);
    }

    private DataMatrix generateMatrix(String[] condNames, CyGoose goose)
    {
        logger.info("Generating Matrix...");
        if (getSelectedNodes(goose).size() <= 0) {
            showWarning("No nodes were selected for Data Matrix broadcast.");
            return null;
        }

        // initialize DataMatrix
        DataMatrix matrix = new DataMatrix();
        matrix.setColumnTitles(condNames);
        matrix.setSpecies(gDialog.getSpecies());

        // loop through all flagged nodes and construct a DataMatrix with
        // row=columnNames & column=condNames
        for (CyNode currentSelectedNode : getSelectedNodes(goose)) {
            double[] condVals = new double[condNames.length];
            String nodeId = currentSelectedNode.getIdentifier();
            for (int i = 0; i < condNames.length; i++) {
                try {
                    Double val =
                            Cytoscape.getNodeAttributes().getDoubleAttribute(nodeId,
                                    condNames[i]);
                    if (val != null) condVals[i] = val.doubleValue();
                } catch (Exception ex) {
                    logger.warn("generateDataMatrix() error: incompatible data type for " +
                            condNames[i], ex);
                }
            }
            // use other attribute to identify node if selected by user
            // At some point 'broadcastID' is meant to allow you to select the attribute name to broadcast as an ID, has not yet been added
            nodeId = Cytoscape.getNodeAttributes().getStringAttribute(nodeId, broadcastID);

            // add new row to DataMatrix
            matrix.addRow(nodeId, condVals);
        }
        return matrix;
    }

    private void delegateProcessMatrix(final CyGoose goose, final String targetGoose, AttrSelectAction okAction)
                                       //boolean toWorkflow, final String requestID,
                                       //final GooseWorkflowManager workflowManager, final WorkflowAction action)
    {
        if (getSelectedNodes(goose).size() == 0) {
            showWarning("No nodes selected for broadcast.");
            return;
        }

        // create an array of experiment conditions (columnTitles in DataMatrix)
        List<String> condNamesArrList = new ArrayList<String>();
        String[] attributeNames = Cytoscape.getNodeAttributes().getAttributeNames();

        for (String currentAttr : attributeNames) {
            // assume all DOUBLE type attributes are expression data
            if (Cytoscape.getNodeAttributes().getType(currentAttr) == CyAttributes.TYPE_FLOATING)
                condNamesArrList.add(currentAttr);
        }

        /*AttrSelectAction okAction = null;
        if (!toWorkflow)
        {
            // dialog for user to select attributes for broadcast
            okAction = new AttrSelectAction() {
                public void takeAction(String[] selectAttr) {
                    broadcastDataMatrix(selectAttr, goose, targetGoose);
                }
            };
        }
        else
        {
            okAction = new AttrSelectAction() {
                public void takeAction(String[] selectAttr) {
                    broadcastMatrixToWorkflow(selectAttr, goose, requestID, workflowManager, action);
                }
            };
        } */

        // move everything from ArrayList to a String array
        String[] condNames = new String[condNamesArrList.size()];
        condNamesArrList.toArray(condNames);

        if (condNames.length > 0) {
            CyAttrDialog dialog = new CyAttrDialog(condNames, okAction,
                    CyAttrDialog.MULTIPLE_SELECT);
            dialog.setDialogText(broadcastStr);
            dialog.preSelectCheckBox(condNames);
            dialog.buildDialogWin();
        } else {
            showWarning("The selected nodes do not have numerical attributes " +
                    "for a matrix");
        }
    }

    private void broadcastDataMatrix(String[] condNames, CyGoose goose, String targetGoose) {
        DataMatrix matrix = generateMatrix(condNames, goose);

        try {
            this.gaggleBoss.broadcastMatrix(goose.getName(), targetGoose, matrix);
        } catch (Exception ex) {
            String msg = "Failed to broadcast matrix to " + targetGoose;
            showError(msg);
            logger.error(msg, ex);
        }
    }

    private void broadcastMatrixToWorkflow(String[] condNames, CyGoose goose,
                                         String requestID, int targetIndex,
                                         Object syncObj,
                                         GooseWorkflowManager workflowManager,
                                         WorkflowAction action) {
        DataMatrix matrix = generateMatrix(condNames, goose);
        logger.info("Submitting matrix to Request: " + requestID + " Index: " + targetIndex + " Target: " + action.getTargets()[targetIndex].getGooseName());
        workflowManager.addSessionTargetData(requestID, targetIndex, matrix);

        logger.info("Broadcast matrix notify main thread");
        synchronized (syncObj) {
            syncObj.notify();
        }
    }

    public void broadcastNetwork(CyGoose goose, String targetGoose) {
        logger.info("broadcastNetwork " + getNetworkIdentifier(goose));

        Network gaggleNetwork = GenerateNetwork(goose);
        logger.debug("in broadcastnetwork, species is " + gaggleNetwork.getSpecies());
        try {
            this.gaggleBoss.broadcastNetwork(goose.getName(), targetGoose, gaggleNetwork);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    class TupleProcessingThread extends Thread
    {
        private CyGoose goose;
        private AttrSelectAction myAction;

        public TupleProcessingThread(CyGoose goose, AttrSelectAction okAction)
        {
            this.goose = goose;
            this.myAction = okAction;
        }

        public void run()
        {
            delegateProcessTuple(this.goose, null, this.myAction);
        }
    }

    class MatrixProcessingThread extends Thread
    {
        private CyGoose goose;
        private AttrSelectAction myAction;
        private Object syncObj;

        public MatrixProcessingThread(CyGoose goose, AttrSelectAction okAction, Object syncObj)
        {
            this.goose = goose;
            this.myAction = okAction;
            this.syncObj = syncObj;
        }

        public void run()
        {
            delegateProcessMatrix(this.goose, null, this.myAction);
            logger.info("Broadcast matrix thread notifying main thread...");
            synchronized (syncObj) {
                syncObj.notify();
            }
        }
    }

    class PrepareWorkflowResponseThread extends Thread
    {
        private WorkflowAction action;
        private GooseWorkflowManager workflowManager;
        private String requestID;
        private CyGoose goose;

        public PrepareWorkflowResponseThread(WorkflowAction action, CyGoose goose, GooseWorkflowManager workflowManager, String requestID)
        {
            this.goose = goose;
            this.action = action;
            this.workflowManager = workflowManager;
            this.requestID = requestID;
        }

        public void run()
        {
            if (action.getTargets() != null && action.getTargets().length > 0)
            {
                for (int i = 0; i < action.getTargets().length; i++)
                {
                    WorkflowComponent target = action.getTargets()[i];
                    String edgeType = (String)target.getParams().get(WorkflowComponent.ParamNames.EdgeType.getValue());
                    logger.info("Edge type: " + edgeType);
                    if (edgeType.equals("namelist") || edgeType.equals("data"))
                    {
                        // Namelist
                        Namelist namelist = generateNamelist(goose);
                        if (namelist != null)
                        {
                            logger.info("Gaggle Namelist selected: " + namelist.getName());
                            workflowManager.addSessionTargetData(requestID, i, namelist);
                        }
                    }
                    else if (edgeType.equals("matrix"))
                    {
                        final Object syncObj = new Object();
                        final int index = i;
                        // Matrix
                        AttrSelectAction okAction = new AttrSelectAction() {
                            public void takeAction(String[] selectAttr) {
                                broadcastMatrixToWorkflow(selectAttr, goose, requestID, index, syncObj, workflowManager, action);
                            }
                        };

                        MatrixProcessingThread matrixthread = new MatrixProcessingThread(goose, okAction, syncObj);
                        matrixthread.start();

                        try
                        {
                            synchronized (syncObj) {
                              syncObj.wait();
                              logger.info("Notified by delegateProcessMatrix");
                            }
                        }
                        catch (Exception ex0)
                        {
                            logger.warning("Failed to wait on delegateProcessMatrix: " + ex0.getMessage());
                            workflowManager.addSessionTargetData(requestID, i, null);
                        }
                    }
                    else if (edgeType.equals("tuple"))
                    {
                        // Tuple
                        final int index = i;
                        final Object syncObj = new ObjectArray();
                        AttrSelectAction okAction = new AttrSelectAction() {
                            public void takeAction(String[] selectAttr) {
                                broadcastTupleToWorkflow(selectAttr, goose, requestID, index, syncObj, workflowManager, action);
                            }
                        };

                        TupleProcessingThread tuplethread = new TupleProcessingThread(goose,okAction);
                        tuplethread.start();
                        //delegateProcessTuple(goose, null, okAction);
                        try
                        {
                            synchronized (syncObj) {
                                syncObj.wait();
                                logger.info("Notified by delegateProcessTuple");
                            }
                        }
                        catch (Exception ex1)
                        {
                            logger.warning("Failed to wait on delegateProcessTuple: " + ex1.getMessage());
                            workflowManager.addSessionTargetData(requestID, i, null);
                        }
                    }
                    else if (edgeType.equals("network"))
                    {
                        // Parallel targets, we need to duplicate the data for each target
                        Network gaggleNetwork = GenerateNetwork(goose);

                        if (gaggleNetwork != null)
                        {
                            logger.info("Gaggle Network selected: " + gaggleNetwork.getName());
                            workflowManager.addSessionTargetData(requestID, i, gaggleNetwork);
                        }
                    }
                }

                logger.info("Completing workflow action...");
                if (workflowManager.CompleteWorkflowAction(gaggleBoss, requestID))
                {
                    // Now we can clean up the UI
                    gDialog.setWorkflowUI(null);
                    gDialog.removeRequestNetwork(requestID);
                }
            }
        }
    }


    /**
     * Handler for the next button to send data to the next component of the workflow
     * @param goose
     * @param workflowManager: GooseWorkflowManager that manages the workflowaction
     * @param requestID: the ID of the workflow action
     */
    public void NextWorkflowActionHandler(final CyGoose goose, final GooseWorkflowManager workflowManager, final String requestID)
    {
        if (this.gaggleBoss instanceof Boss3)
        {
            logger.info("Boss supports workflow");
            final WorkflowAction action = workflowManager.getWorkflowAction(requestID);
            if (action != null)
            {
                logger.info("Prepare workflow response data");
                // Put the processing in a thread to avoid UI frozen due to the logic that delegates handle Tuple and Matrix
                // to their respective delegate functions
                PrepareWorkflowResponseThread pwrt = new PrepareWorkflowResponseThread(action, goose, workflowManager, requestID);
                pwrt.start();
            }
        }
    }

    private Network GenerateNetwork(CyGoose goose)
    {
        Network gaggleNetwork = new Network();
        gaggleNetwork.setSpecies(gDialog.getSpecies());

        for (CyNode currentSelectedNode : getSelectedNodes(goose)) {
            logger.info("Network ID: " + currentSelectedNode.getIdentifier());
            gaggleNetwork.add(currentSelectedNode.getIdentifier());
        }

        CyAttributes edgeAtts = Cytoscape.getEdgeAttributes();
        for (CyEdge currentSelectedEdge : getSelectedEdges(goose)) {
            CyNode sourceNode = (CyNode) currentSelectedEdge.getSource();
            CyNode targetNode = (CyNode) currentSelectedEdge.getTarget();
            logger.info("Source node: " + sourceNode.getIdentifier() + " Target node: " + targetNode.getIdentifier());

            // create a new GaggleInteraction for broadcast
            String interactionType =
                    edgeAtts.getStringAttribute(currentSelectedEdge.getIdentifier(),
                            Semantics.INTERACTION);
            Interaction gaggleInteraction =
                    new Interaction(sourceNode.getIdentifier(),
                            targetNode.getIdentifier(),
                            interactionType,
                            currentSelectedEdge.isDirected());
            gaggleNetwork.add(gaggleInteraction);
        }

        gaggleNetwork = addAttributes(gaggleNetwork);
        return gaggleNetwork;
    }


    private Network addAttributes(Network gaggleNet) {
        for (String id : gaggleNet.getNodes())
            gaggleNet = addAttributes(id, Cytoscape.getNodeAttributes(),
                                      gaggleNet, NetworkObject.NODE);

        for (Interaction interaction : gaggleNet.getInteractions())
            gaggleNet = addAttributes(interaction.toString(),
                                      Cytoscape.getEdgeAttributes(), gaggleNet,
                                      NetworkObject.EDGE);
        return gaggleNet;
    }


    // add attributes to the node/edge
    private Network addAttributes(String Identifier, CyAttributes cyAtts,
                                  Network gaggleNet, NetworkObject obj) {

        for (String AttributeName : cyAtts.getAttributeNames()) {
            Object Value = "";

            // don't think we should pass on hidden attributes, they aren't useful to the user
            if (!cyAtts.getUserVisible(AttributeName))
                continue;

            switch (cyAtts.getType(AttributeName)) {
            case CyAttributes.TYPE_BOOLEAN:
                Value = cyAtts.getBooleanAttribute(Identifier, AttributeName);
                break;
            case CyAttributes.TYPE_INTEGER:
                Value = cyAtts.getIntegerAttribute(Identifier, AttributeName);
                break;
            case CyAttributes.TYPE_STRING:
                Value = cyAtts.getStringAttribute(Identifier, AttributeName);
                break;
            case CyAttributes.TYPE_FLOATING:
                Value = cyAtts.getDoubleAttribute(Identifier, AttributeName);
                break;
            }

            if (Value == null) Value = "";
            switch (obj) {
            case NODE:
                gaggleNet.addNodeAttribute(Identifier, AttributeName, Value);
                break;
            case EDGE:
                gaggleNet.addEdgeAttribute(Identifier, AttributeName, Value);
                break;
            }
        }
        return gaggleNet;
    }

    // Helper functions to lower the noise
    private void showWarning(String msg) {
        GagglePlugin.showDialogBox(msg, "Warning", JOptionPane.WARNING_MESSAGE);
    }
    private void showError(String msg) {
        GagglePlugin.showDialogBox(msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
    private Set<CyNode> getSelectedNodes(CyGoose goose) {
        return Cytoscape.getNetwork(goose.getNetworkId()).getSelectedNodes();
    }
    private Set<CyEdge> getSelectedEdges(CyGoose goose) {
        return Cytoscape.getNetwork(goose.getNetworkId()).getSelectedEdges();
    }
    private String getNetworkIdentifier(CyGoose goose) {
        return Cytoscape.getNetwork(goose.getNetworkId()).getIdentifier();
    }
    private String getNetworkTitle(CyGoose goose) {
        return Cytoscape.getNetwork(goose.getNetworkId()).getTitle();
    }

}
