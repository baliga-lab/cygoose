## CyGoose - Cytoscape Gaggle Plugin

This is a Cytoscape plugin that integrates with the Gaggle communication
framework.

The current version was tested with Cytoscape 2.8.0 and 2.7.0 and was
adapted to account for the changes made starting in 2.7.0.
The original CyGoose was written by

* Sarah Killcoyne and Dan Tenenbaum, Institute for Systems Biology
* John Lin, Kevin Drew and Richard Bonneau, NYU Bonneau Lab

### Build Requirements

JDK >= 1.5
Maven >= 2.x
Cytoscape >= 2.7.0

<b>Note:</b> Institute for Systems Biology provides the Maven repository for
Gaggle dependencies, due to licensing/packaging, a separate Cytoscape
download is required to compile the project.

### Build Instructions

For practical reasons, we do not reference the cytoscape.jar file
directly - it is about 45 MB in size ! Instead, the dependency was
added via a systemPath setting, which means it is expected that the
developer has downloaded the Cytoscape application and provides the
setting either through a profile in a settings.xml or via the command
line.

1. Profile in settings.xml

In your ~/.m2/settings.xml, add the following section

  ...
  &lt;profiles&gt;
    ...
    &lt;profile&gt;
      &lt;id&gt;cytoscape&lt;/id&gt;
      &lt;properties&gt;
        &lt;cytoscape.home&gt;/path/to/cytoscape.jar/directory&lt;/cytoscape.home&gt;
      &lt;/properties&gt;
    &lt;/profile&gt;
    ...
  &lt;/profiles&gt;
  ...

Invoke 

	mvn assembly:assembly -P cytoscape

2. Specifying the location directly

invoke

  mvn assembly:assembly -Dcytoscape.home=/path/to/cytoscape.jar/directory

The project will build a jar file including the necessary dependencies.


-- Wei-ju Wu, January 7th, 2011

