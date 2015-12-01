package me.sunnen.euroconverter;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@RestController
public class RateController
{
    @RequestMapping(value="/rate", method=RequestMethod.GET)
    public Rate getRate(@RequestParam(value="currency",required = true) String currency,@RequestParam(value="date", required = true) String date)
    {
    	try
    	{
    		File file = new File("/Users/Dany/Downloads/eurofxref-hist-90d.xml");
    		DocumentBuilderFactory docFact = DocumentBuilderFactory.newInstance();
        	DocumentBuilder docBuilder = docFact.newDocumentBuilder();
         	Document doc = docBuilder.parse(file);
         	Element element = doc.getDocumentElement();
         	NodeList rootChildren = element.getChildNodes();
         	if (rootChildren.getLength()==0) return new Rate("2014-11-30","CHF","error"); //error
         	Node rootChild;
         	for(int z=0;z<rootChildren.getLength();z++)
         	{
         		rootChild = rootChildren.item(z);
         		if (rootChild.getNodeName().compareTo("Cube")==0)
         		{
                 	NodeList dateNodes = rootChild.getChildNodes();
                 	NodeList currencyNodes;
                 	Node dateNode, dateAttribute;
                 	NamedNodeMap attributes;
                 	Node currencyNode, currencyAttribute, rateAttribute;
                 	for(int i=0;i<dateNodes.getLength();i++)
                 	{
                 		dateNode = dateNodes.item(i);
                 		attributes = dateNode.getAttributes();
                 		dateAttribute = attributes.getNamedItem("time");
                 		if (dateAttribute.getNodeValue().compareTo(date)==0)
                 		{
                 			//Found the correct date
                 			currencyNodes = dateNode.getChildNodes();
                 			for(int j=0;j<currencyNodes.getLength();j++)
                 			{
                 				currencyNode = currencyNodes.item(j);
                 				attributes = currencyNode.getAttributes();
                 				currencyAttribute = attributes.getNamedItem("currency");
                 				if (currencyAttribute.getNodeValue().compareTo(currency)==0)
                 				{
                 					//Found the correct date and currency
                 					rateAttribute = attributes.getNamedItem("rate");
                 					return new Rate(date,currency,rateAttribute.getNodeValue());
                 				}
                 			}
                 		}
                 	}
         		}
         	}
         	
    	}
    	catch(ParserConfigurationException e)
    	{
    		return new Rate("2014-11-30","CHF","exception");
    	}
    	catch(IOException e)
    	{
    		return new Rate("2014-11-30","CHF","exception");
    	} catch (SAXException e)
    	{
    		return new Rate("2014-11-30","CHF","exception");
		}
        return new Rate("2014-11-30","CHF","error");
    }
}