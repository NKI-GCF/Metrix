// Metrix - A server / client interface for Illumina Sequencing Metrics.
// Copyright (C) 2013 Bernd van der Veen

// This program comes with ABSOLUTELY NO WARRANTY;
// This is free software, and you are welcome to redistribute it
// under certain conditions; for more information please see LICENSE.txt

package nki.objects;

import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.Date;
import java.util.Arrays;
import java.util.Comparator;
import nki.constants.Constants;
import nki.objects.Summary;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PostProcess implements Serializable {
	
	public static 	final long serialVersionUID = 42L;
	private String		type;
	private String		id;
	private String 		title;
	protected int		order;
	protected int		subOrder;

	public PostProcess(Node block){
		this.setType(block.getNodeName());
		NamedNodeMap attrN = block.getAttributes();

		this.setId(attrN.getNamedItem("id").getNodeValue());
		this.setTitle(attrN.getNamedItem("title").getNodeValue());
		this.setOrder(Integer.parseInt(attrN.getNamedItem("execOrder").getNodeValue()));
	}

	public PostProcess(){

	}

	public void setType(String type){
		this.type = type;
	}

	public String getType(){
		return this.type;
	}

	public void setId(String id){
		this.id = id;
	}

	public String getId(){
		return this.id;
	}

	public void setTitle(String title){
		this.title = title;
	}

	public String getTitle(){
		return this.title;
	}

	public void setOrder(int order){
		this.order = order;
	}

	public int getOrder(){
		return this.order;
	}

	public void setSubOrder(int subOrder){
		this.subOrder = subOrder;
	}

	public int getSubOrder(){
		return this.subOrder;
	}
}

