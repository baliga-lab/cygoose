package org.systemsbiology.cytoscape;

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
        if (getSelectedNodes(goose).size() == 0) {
            showWarning("No nodes were selected for broadcast.");
            return;
        }
        List<String> selectedIds = new ArrayList<String>();
        for (CyNode selectedNode : getSelectedNodes(goose)) {
            selectedIds.add(selectedNode.getIdentifier());
        }

        Namelist namelist = new Namelist();
        namelist.setSpecies(gDialog.getSpecies());
        namelist.setNames(selectedIds.toArray(new String[0]));

        try {
            gaggleBoss.broadcastNamelist(goose.getName(), targetGoose, namelist);
        } catch (Exception ex) {
            String msg = "Failed to broadcast list of names to " + targetGoose;
            showError(msg);
            logger.error(msg, ex);
        }
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

        CyAttrDialog dialog = new CyAttrDialog(allAttrNames, okAction,
                                               CyAttrDialog.MULTIPLE_SELECT);
        dialog.setDialogText(broadcastStr);
        dialog.preSelectCheckBox(allAttrNames);
        dialog.buildDialogWin();
    }

    private void broadcastTuple(String[] attrNames, CyGoose goose,
                                String targetGoose) {
        if (getSelectedNodes(goose).size() <= 0) {
            showWarning("No nodes were selected for tuple broadcast.");
            return;
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

        try {
            gaggleBoss.broadcastTuple(goose.getName(), targetGoose, gaggleTuple);
        } catch (Exception ex) {
            String msg = "Failed to broadcast map to " + targetGoose;
            showError(msg);
            logger.error(msg, ex);
        }
    }

    public void broadcastDataMatrix(final CyGoose goose, final String targetGoose) {
        logger.info("broadcastDataMatrix()");

        if (getSelectedNodes(goose).size() == 0) {
            showWarning("No nodes were selected for Data Matrix broadcast.");
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

        // move everything from ArrayList to a String array
        String[] condNames = new String[condNamesArrList.size()];
        condNamesArrList.toArray(condNames);

        // dialog for user to select attributes for broadcast
        AttrSelectAction okAction = new AttrSelectAction() {
                public void takeAction(String[] selectAttr) {
                    broadcastDataMatrix(selectAttr, goose, targetGoose);
                }
            };

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
        if (getSelectedNodes(goose).size() <= 0) {
            showWarning("No nodes were selected for Data Matrix broadcast.");
            return;
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
                    logger.warn("broadcastDataMatrix() error: incompatible data type for " +
                                condNames[i], ex);
                }
            }
            // use other attribute to identify node if selected by user
            // At some point 'broadcastID' is meant to allow you to select the attribute name to broadcast as an ID, has not yet been added
            nodeId = Cytoscape.getNodeAttributes().getStringAttribute(nodeId, broadcastID);

            // add new row to DataMatrix
            matrix.addRow(nodeId, condVals);
        }

        try {
            this.gaggleBoss.broadcastMatrix(goose.getName(), targetGoose, matrix);
        } catch (Exception ex) {
            String msg = "Failed to broadcast matrix to " + targetGoose;
            showError(msg);
            logger.error(msg, ex);
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


    public void NextWorkflowActionHandler(CyGoose goose)
    {
        if (this.gaggleBoss instanceof Boss3)
        {
            logger.info("Boss supports workflow");
            WorkflowAction action = goose.getWorkflowAction();
            if (action != null)
            {
                logger.info("Prepare workflow response data");

                GaggleData[] gaggleData = null;
                if (action.getTargets() != null && action.getTargets().length > 0)
                {
                    // Parallel targets, we need to duplicate the data for each target
                    Network gaggleNetwork = GenerateNetwork(goose);
                    if (gaggleNetwork != null)
                    {
                        logger.info("Gaggle Network selected: " + gaggleNetwork.getName());
                        gaggleData = new GaggleData[action.getTargets().length];
                        for (int i = 0; i < action.getTargets().length; i++)
                            gaggleData[i] = gaggleNetwork;
                    }
                }

                WorkflowAction response = new WorkflowAction(action.getSessionID(),
                        WorkflowAction.ActionType.Response,
                        action.getSource(),
                        action.getTargets(),
                        action.getOption() | WorkflowAction.Options.SuccessMessage.getValue(),
                        gaggleData
                );
                try
                {
                    ((Boss3)gaggleBoss).handleWorkflowAction(response);
                }
                catch (Exception e)
                {
                    logger.info("Failed to submit response to boss: " + e.getMessage());
                }
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
