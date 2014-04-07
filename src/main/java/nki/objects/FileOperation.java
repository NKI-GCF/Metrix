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

public class FileOperation extends PostProcess {

  public static final long serialVersionUID = 42L;
  private String processType;  // Copy or Symlink
  private String globbing;    // Globbing string
  private String source;      // Source path for operation
  private String destination;  // Destination path for operation
  private String overwrite;    //

  public FileOperation(Node parentNode, Node childNode) {
    NamedNodeMap parentAttr = parentNode.getAttributes();
    NamedNodeMap childAttr = childNode.getAttributes();

    this.setOrder(Integer.parseInt(parentAttr.getNamedItem("execOrder").getNodeValue()));
    this.setSubOrder(Integer.parseInt(childAttr.getNamedItem("execOrder").getNodeValue()));
    this.setId(childAttr.getNamedItem("id").getNodeValue());
    this.setTitle(childAttr.getNamedItem("title").getNodeValue());
    this.setProcessType(childNode.getNodeName());

    NodeList foProps = childNode.getChildNodes();

    for (int i = 0; i < foProps.getLength(); i++) {
      // Iterate over node properties
      Node p = foProps.item(i);
      if (p.getNodeName().equalsIgnoreCase("Source")) {
        this.setSource(p.getTextContent());
      }
      else if (p.getNodeName().equalsIgnoreCase("Destination")) {
        this.setDestination(p.getTextContent());
      }
      else if (p.getNodeName().equalsIgnoreCase("Globbing")) {
        this.setGlobbing(p.getTextContent());
      }
      else if (p.getNodeName().equalsIgnoreCase("Overwrite")) {
        this.setOverwrite(p.getTextContent());
      }
    }
  }

  public void setProcessType(String pt) {
    this.processType = pt;
  }

  public String getProcessType() {
    return this.processType;
  }

  public void setGlobbing(String gl) {
    this.globbing = gl;
  }

  public String getGlobbing() {
    return this.globbing;
  }

  public void setSource(String src) {
    this.source = src;
  }

  public String getSource() {
    return this.source;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  public String getDestination() {
    return this.destination;
  }

  public void setOverwrite(String overwrite) {
    if (overwrite.equals("")) {
      this.overwrite = "N";
    }
    else {
      this.overwrite = overwrite;
    }
  }

  public boolean isCopyOperation() {
    return processType.equalsIgnoreCase("copy") ? true : false;
  }

  public boolean isSymlinkOperation() {
    return processType.equalsIgnoreCase("symlink") ? true : false;
  }

  public boolean hasGlobbing() {
    if (!getGlobbing().equals("")) {
      return true;
    }
    else {
      return false;
    }
  }

  public boolean needOverwrite() {
    return this.overwrite.equalsIgnoreCase("Y") ? true : false;
  }

  public boolean isValid() {
    if (source != null && !source.equals("") &&
        destination != null && !destination.equals("")) {
      return true;
    }
    else {
      return false;
    }
  }
}

