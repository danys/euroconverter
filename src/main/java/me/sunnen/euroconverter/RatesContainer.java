package me.sunnen.euroconverter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class contains the FX rates from the last 90 days in a two level hash map
 * @author Dany
 *
 */
public class RatesContainer
{
	private Map<String,Map<String,String>> rateMap;
	private LocalDate earliestDate, latestDate;
	
	private final int maxTries = 10;
	private final String defaultCurrency = "USD";
	
	//Must be statically constructed => no need to care about synchronized accesses of fields in that situation
	public RatesContainer()
	{
		rateMap = new HashMap<String,Map<String,String>>();
		earliestDate = LocalDate.now();
		latestDate = LocalDate.of(2000, 1, 1);
	}
	
	public void addEntriesFromFile(File file, boolean removeEarliestKey)
	{
		if (removeEarliestKey)
		{
			synchronized(this)
     		{
				if (rateMap!=null)
				{
					//Remove the given key
					rateMap.remove(earliestDate.toString());
					LocalDate nextDate = earliestDate.plusDays(1);
					int nTries=0;
					//Attempt to find the earliest key present
					while(!rateMap.containsKey(nextDate.toString()) && nTries<maxTries)
					{
						nTries++;
						nextDate = nextDate.plusDays(1);
					}
					if (nTries!=maxTries)
					{
						//OK found the earliest date
						earliestDate = nextDate;
					}
					else
					{
						//Earliest date was not found
						System.out.println("Could not find new earliest date!");
						return;
					}
				}
     		}
		}
		try
		{
			DocumentBuilderFactory docFact = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder docBuilder = docFact.newDocumentBuilder();
	     	Document doc = docBuilder.parse(file);
	     	Element element = doc.getDocumentElement();
	     	NodeList rootChildren = element.getChildNodes();
	     	if (rootChildren.getLength()==0) return; //error
	     	Node rootChild;
	     	Map<String,String> currencyMap;
	     	LocalDate currentDate;
	     	for(int z=0;z<rootChildren.getLength();z++)
	     	{
	     		rootChild = rootChildren.item(z);
	     		if (rootChild.getNodeName().compareTo("Cube")==0)
	     		{	
	     			//Found the Cube root node
	             	NodeList dateNodes = rootChild.getChildNodes();
	             	NodeList currencyNodes;
	             	Node dateNode, dateAttribute;
	             	NamedNodeMap attributes;
	             	Node currencyNode, currencyAttribute, rateAttribute;
	             	for(int i=0;i<dateNodes.getLength();i++)
	             	{
	             		//Loop through the dates
	             		dateNode = dateNodes.item(i);
	             		if (dateNode.getNodeType()!=Node.ELEMENT_NODE) continue;
	             		attributes = dateNode.getAttributes();
	             		dateAttribute = attributes.getNamedItem("time");
	             		//Check if date is already in RatesContainer: if yes skip
             			if (rateMap.containsKey(dateAttribute.getNodeValue())) continue;
	             		currencyNodes = dateNode.getChildNodes();
	             		synchronized(this)
	             		{
	             			currencyMap = new HashMap<String,String>();
		             		for(int j=0;j<currencyNodes.getLength();j++)
		             		{
		             			//Loop through the currencies
		             			currencyNode = currencyNodes.item(j);
		             			if (currencyNode.getNodeType()!=Node.ELEMENT_NODE) continue;
		             			attributes = currencyNode.getAttributes();
		             			currencyAttribute = attributes.getNamedItem("currency");
		             			rateAttribute = attributes.getNamedItem("rate");
		             			//Add a currency, rate key-value pair to the HashMap
		             			currencyMap.put(currencyAttribute.getNodeValue(), rateAttribute.getNodeValue());
		             		}
		             		currentDate=LocalDate.parse(dateAttribute.getNodeValue());
		             		//Add the currency map to the entire hash map
		             		rateMap.put(dateAttribute.getNodeValue(), currencyMap);
		             		//Adjust earliest and latest dates
		             		if (currentDate.compareTo(earliestDate)<0) earliestDate = currentDate;
			             	if (currentDate.compareTo(latestDate)>0) latestDate = currentDate;
	             		}
	             	}
	     		}
	     	}
		}
		catch(ParserConfigurationException e)
    	{
    		System.out.println("Exception: Parser configuration not correct!");
    		return;
    	}
    	catch(IOException e)
    	{
    		System.out.println("Exception: Error reading XML file!");
    		return;
    	}
    	catch (SAXException e)
    	{
    		System.out.println("Exception: Error parsing XML document!");
    		return;
		}
	}
	
	public Rate getRate(String currency, String date)
	{
		synchronized(this)
		{
			//Check if the given date is in the RatesContainer
 			if (!rateMap.containsKey(date))
 			{
 				//Default to latest date
 				Map<String,String> m = rateMap.get(latestDate.toString());
 				if (m==null) return new Rate(latestDate.toString(),defaultCurrency,"error"); //error latest date not found
 				if (m.containsKey(currency))
 				{
 					String rate = m.get(currency);
 					return new Rate(latestDate.toString(),currency,rate);
 				}
 				else
 				{
 					//Default to default currency
 					String rate = m.get(defaultCurrency);
 					if (rate==null) return new Rate(latestDate.toString(),defaultCurrency,"error"); //error getting value for default currency
 					else return new Rate(latestDate.toString(),currency,rate);
 				}
 			}
 			else //OK given date is in RateContainer
 			{
 				Map<String,String> m = rateMap.get(date);
 				if (m.containsKey(currency))
 				{
 					String rate = m.get(currency);
 					return new Rate(date,currency,rate);
 				}
 				else
 				{
 					//Default to default currency
 					String rate = m.get(defaultCurrency);
 					if (rate==null) return new Rate(date,defaultCurrency,"error"); //error getting value for default currency
 					else return new Rate(date,currency,rate);
 				}
 			}
		}
	}
	
	public String[] getCurrencies()
	{
		String result[];
		Map<String,String> map;
		synchronized(this)
		{
			map = rateMap.get(latestDate.toString());
		}
		int size = map.size();
		result = new String[size];
		Set<String> set = map.keySet();
		Iterator<String> it = set.iterator();
		int i=0;
		while(it.hasNext())
		{
			result[i] = it.next();
			i++;
		}
		return result;
	}
	
	public String[] getDates()
	{
		String result[];
		synchronized(this)
		{
			Set<String> set = rateMap.keySet();
			int size = set.size();
			result = new String[size];
			Iterator<String> it = set.iterator();
			int i=0;
			while(it.hasNext())
			{
				result[i] = it.next();
				i++;
			}
		}
		return result;
	}
}
