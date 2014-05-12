// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.Comparator;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Application extends PostProcess {

  public static final long serialVersionUID = 42L;
  private String scriptPath;
  private String arguments;
  private String outputPath;
  private String workingDirectory;

  public Application(Node parentNode, Node childNode) {
    NamedNodeMap parentAttr = parentNode.getAttributes();
    NamedNodeMap childAttr = childNode.getAttributes();

    this.setOrder(Integer.parseInt(parentAttr.getNamedItem("execOrder").getNodeValue()));
    this.setSubOrder(Integer.parseInt(childAttr.getNamedItem("execOrder").getNodeValue()));
    this.setId(childAttr.getNamedItem("id").getNodeValue());
    this.setTitle(childAttr.getNamedItem("title").getNodeValue());

    NodeList foProps = childNode.getChildNodes();

    for (int i = 0; i < foProps.getLength(); i++) {
      // Iterate over node properties
      Node p = foProps.item(i);
      if (p.getNodeName().equalsIgnoreCase("ScriptPath")) {
        this.setScriptPath(p.getTextContent());
      }
      else if (p.getNodeName().equalsIgnoreCase("Arguments")) {
        this.setArguments(p.getTextContent());
      }
      else if (p.getNodeName().equalsIgnoreCase("OutputPath")) {
        this.setOutputPath(p.getTextContent());
      }
      else if (p.getNodeName().equalsIgnoreCase("WorkingDirectory")) {
        this.setWorkingDirectory(p.getTextContent());
      }
    }
  }

  public void setScriptPath(String sp) {
    this.scriptPath = sp;
  }

  public String getScriptPath() {
    return this.scriptPath;
  }

  public void setArguments(String arg) {
    this.arguments = arg;
  }

  public String getArguments() {
    return this.arguments;
  }

  public void setOutputPath(String outputPath) {
    this.outputPath = outputPath;
  }

  public String getOutputPath() {
    return this.outputPath;
  }

  public void setWorkingDirectory(String workingDirectory) {
    this.workingDirectory = workingDirectory;
  }

  public String getWorkingDirectory() {
    return this.workingDirectory;
  }

}

