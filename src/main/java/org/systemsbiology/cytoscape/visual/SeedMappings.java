package org.systemsbiology.cytoscape.visual;

import java.awt.Color;
import java.awt.Stroke;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import cytoscape.*;
import cytoscape.visual.mappings.BoundaryRangeValues;
import cytoscape.visual.mappings.ContinuousMapping;
import cytoscape.visual.mappings.ObjectMapping;
import cytoscape.visual.*;
import cytoscape.visual.calculators.*;


/**
 * @author Sarah Killcoyne, Wei-ju Wu
 * Class to create a seed of node mappings specifically for movies 
 * (handleHashMap()) so something can be
 * shown and the user can customize later.
 */
public class SeedMappings {
    // colors used in node borders
    private static final Color BLUE   = new Color(0, 0, 204); 
    private static final Color PURPLE = new Color(204, 0, 204);
    private static final Color YELLOW = new Color(255, 255, 0);
    private static final Color ORANGE = new Color(255, 153, 0);
    private static final Color BLACK  = Color.BLACK;

    // colors used in node fill
    private static final Color RED      = new Color(255, 0, 51); 
    private static final Color PINK     = new Color(255, 153, 153);
    private static final Color GREEN    = new Color(0, 255, 0);
    private static final Color LT_GREEN = new Color(153, 255, 153);
    private static final Color WHITE    = Color.WHITE;

    // shapes used
    private static final NodeShape CIRCLE    = NodeShape.ROUND_RECT;
    private static final NodeShape SQUARE    = NodeShape.RECT;
    private static final NodeShape TRIANGLE  = NodeShape.TRIANGLE;
    private static final NodeShape HEXAGON   = NodeShape.HEXAGON;
    private static final NodeShape TRAPEZOID = NodeShape.TRAPEZOID;

    // sizes used
    private static final Double SIZE_HIGH    = new Double(105);
    private static final Double SIZE_MIDHIGH = new Double(70);
    private static final Double SIZE_MID     = new Double(35);
    private static final Double SIZE_MIDLOW  = new Double(50);
    private static final Double SIZE_LOW     = new Double(15);

    // line types used
    private static final Stroke LINE_TYPE_HIGH    = LineStyle.SOLID.getStroke(7);
    private static final Stroke LINE_TYPE_MIDHIGH = LineStyle.EQUAL_DASH.getStroke(5);
    private static final Stroke LINE_TYPE_MID     = LineStyle.SOLID.getStroke(4);
    private static final Stroke LINE_TYPE_MIDLOW  = LineStyle.EQUAL_DASH.getStroke(3);
    private static final Stroke LINE_TYPE_LOW     = LineStyle.SOLID.getStroke(1);

    private static HashSet<String> MappedAttributes;
    private NodeAppearanceCalculator nodeAppearanceCalculator;
    private int mappingCount = 0;
	
    public SeedMappings(NodeAppearanceCalculator nac) {
        if (MappedAttributes == null) MappedAttributes = new HashSet<String>();
        this.nodeAppearanceCalculator = nac;
		}

    private void setCalculator(String name, ObjectMapping mapping,
                               VisualPropertyType type) {
        nodeAppearanceCalculator.setCalculator(new BasicCalculator(name, mapping, type));
    }

    public void seedMappings(String attribute,
                             double upperPoint, double lowerPoint) {
        if (MappedAttributes.contains(attribute) || isNodeAttributeMapped(attribute)) {
            // this way if it's already present we will only have
            // gone through the calculators to determine that once
            MappedAttributes.add(attribute);
            return; 
        } else {
            System.out.println("*** " + attribute + " NOT mapped, creating seed");
            MappedAttributes.add(attribute);
            double midPoint = (upperPoint + lowerPoint) / 2;
			
            switch(mappingCount) {
            case 0:
                setCalculator("GaggleNodeColor_" + attribute, 
                              getNodeColor(attribute, upperPoint, midPoint, lowerPoint),
                              VisualPropertyType.NODE_FILL_COLOR);
                break;
            case 1:
                setCalculator("GaggleNodeSize_" + attribute,
                              getNodeSize(attribute, upperPoint, midPoint, lowerPoint),
                              VisualPropertyType.NODE_SIZE);
                break;
            case 2:
                setCalculator("GaggleNodeShape_" + attribute, 
                              getNodeShape(attribute, upperPoint, midPoint, lowerPoint),
                              VisualPropertyType.NODE_SHAPE);
                break;
            case 3:
                setCalculator("GaggleNodeBorder_" + attribute,
                              getNodeBorderLineType(attribute, upperPoint, midPoint,
                                                    lowerPoint),
                              VisualPropertyType.NODE_LINETYPE);
                break;
            case 4:
                setCalculator("GaggleBorderColor_" + attribute,
                              getNodeBorderColor(attribute, upperPoint, midPoint,
                                                 lowerPoint),
                              VisualPropertyType.NODE_BORDER_COLOR);
                break;
            }
            mappingCount++;
        }
		}
	
    private boolean isNodeAttributeMapped(String attributeName) {
        for (Calculator calculator : nodeAppearanceCalculator.getCalculators()) {
            for (ObjectMapping mapping : calculator.getMappings()) {
                String controllingAttName = mapping.getControllingAttributeName();
                if (controllingAttName != null &&
                    controllingAttName.equalsIgnoreCase(attributeName)) return true;
            }
        }
        return false;
		}

    private ContinuousMapping getNodeColor(String attribute, double upperPoint,
                                           double midPoint, double lowerPoint) {
        ContinuousMapping mapping = new ContinuousMapping(WHITE, ObjectMapping.NODE_MAPPING);
        mapping.setControllingAttributeName(attribute, Cytoscape.getCurrentNetwork(), false);

        mapping.addPoint(lowerPoint,
                         boundaryRangeValuesWith()
                         .equalValue(LT_GREEN)
                         .lesserValue(GREEN)
                         .greaterValue(WHITE)
                         .get());

        mapping.addPoint(midPoint,
                         boundaryRangeValuesWith()
                         .equalValue(WHITE)
                         .lesserValue(LT_GREEN)
                         .greaterValue(PINK)
                         .get());

        mapping.addPoint(upperPoint,
                         boundaryRangeValuesWith()
                         .equalValue(PINK)
                         .lesserValue(WHITE)
                         .greaterValue(RED)
                         .get());

        return mapping;
		}
	
    private ContinuousMapping getNodeShape(String attribute, double upperPoint,
                                           double midPoint, double lowerPoint) {
        ContinuousMapping mapping = new ContinuousMapping(CIRCLE, ObjectMapping.NODE_MAPPING);
        mapping.setControllingAttributeName(attribute, Cytoscape.getCurrentNetwork(), false);

        mapping.addPoint(lowerPoint,
                         boundaryRangeValuesWith()
                         .equalValue(TRAPEZOID)
                         .lesserValue(SQUARE)
                         .greaterValue(CIRCLE)
                         .get());

        mapping.addPoint(midPoint,
                         boundaryRangeValuesWith()
                         .equalValue(CIRCLE)
                         .lesserValue(TRAPEZOID)
                         .greaterValue(TRIANGLE)
                         .get());

        mapping.addPoint(upperPoint,
                         boundaryRangeValuesWith()
                         .equalValue(TRIANGLE)
                         .lesserValue(CIRCLE)
                         .greaterValue(HEXAGON)
                         .get());

        return mapping;
		}
	
    private ContinuousMapping getNodeSize(String attribute, double upperPoint,
                                          double midPoint, double lowerPoint) {
        ContinuousMapping mapping = new ContinuousMapping(SIZE_MID,
                                                          ObjectMapping.NODE_MAPPING);
        mapping.setControllingAttributeName(attribute, Cytoscape.getCurrentNetwork(), false);
        mapping.addPoint(lowerPoint,
                         boundaryRangeValuesWith()
                         .equalValue(SIZE_MIDLOW)
                         .lesserValue(SIZE_LOW)
                         .greaterValue(SIZE_MID)
                         .get());
        
        mapping.addPoint(midPoint,
                         boundaryRangeValuesWith()
                         .equalValue(SIZE_MID)
                         .lesserValue(SIZE_MIDLOW)
                         .greaterValue(SIZE_MIDHIGH)
                         .get());

        mapping.addPoint(upperPoint,
                         boundaryRangeValuesWith()
                         .equalValue(SIZE_MIDHIGH)
                         .lesserValue(SIZE_MID)
                         .greaterValue(SIZE_HIGH)
                         .get());

        return mapping;
    }

    private ContinuousMapping getNodeBorderLineType(String attribute,
                                                    double upperPoint,
                                                    double midPoint,
                                                    double lowerPoint) {

        ContinuousMapping mapping = new ContinuousMapping(LINE_TYPE_MID,
                                                          ObjectMapping.NODE_MAPPING);
        mapping.setControllingAttributeName(attribute, Cytoscape.getCurrentNetwork(), false);
        mapping.addPoint(lowerPoint,
                         boundaryRangeValuesWith()
                         .equalValue(LINE_TYPE_MIDLOW)
                         .lesserValue(LINE_TYPE_LOW)
                         .greaterValue(LINE_TYPE_MID)
                         .get());

        mapping.addPoint(midPoint,
                         boundaryRangeValuesWith()
                         .equalValue(LINE_TYPE_MID)
                         .lesserValue(LINE_TYPE_MIDLOW)
                         .greaterValue(LINE_TYPE_MIDHIGH)
                         .get());

        mapping.addPoint(lowerPoint,
                         boundaryRangeValuesWith()
                         .equalValue(LINE_TYPE_MIDHIGH)
                         .lesserValue(LINE_TYPE_MID)
                         .greaterValue(LINE_TYPE_HIGH)
                         .get());
        return mapping;
		}

    private ContinuousMapping getNodeBorderColor(String attribute, double upperPoint,
                                                 double midPoint, double lowerPoint) {
        ContinuousMapping mapping = new ContinuousMapping(BLACK, ObjectMapping.NODE_MAPPING);
        mapping.setControllingAttributeName(attribute, Cytoscape.getCurrentNetwork(), false);

        mapping.addPoint(lowerPoint,
                         boundaryRangeValuesWith()
                         .equalValue(PURPLE)
                         .lesserValue(BLUE)
                         .greaterValue(BLACK)
                         .get());

        mapping.addPoint(midPoint,
                         boundaryRangeValuesWith()
                         .equalValue(BLACK)
                         .greaterValue(ORANGE)
                         .lesserValue(PURPLE)
                         .get());

        mapping.addPoint(upperPoint,
                         boundaryRangeValuesWith()
                         .equalValue(ORANGE)
                         .greaterValue(YELLOW)
                         .lesserValue(BLACK)
                         .get());
        return mapping;
		}

    private BoundaryRangeValuesBuilder boundaryRangeValuesWith() {
        return new BoundaryRangeValuesBuilder();
    }
    class BoundaryRangeValuesBuilder {
        private BoundaryRangeValues values = new BoundaryRangeValues();
        public BoundaryRangeValuesBuilder equalValue(Object value) {
            values.equalValue = value;
            return this;
        }
        public BoundaryRangeValuesBuilder greaterValue(Object value) {
            values.greaterValue = value;
            return this;
        }
        public BoundaryRangeValuesBuilder lesserValue(Object value) {
            values.lesserValue = value;
            return this;
        }
        public BoundaryRangeValues get() { return values; }
    }
}
