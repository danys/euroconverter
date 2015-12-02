package me.sunnen.euroconverter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
	private static ConcurrentMap<String,ConcurrentMap<String,String>> rateMap = buildRateMap();
	private static LocalDate earliestDate, latestDate;
	
	private static ConcurrentMap<String,ConcurrentMap<String,String>> buildRateMap()
	{
		rateMap = new ConcurrentHashMap<String,ConcurrentMap<String,String>>();
		earliestDate = LocalDate.now();
		latestDate = LocalDate.now();
		//TODO might fetch the file at startup
    	//Open the file with the rates and extract the requested rate
		//File fetched from: http://www.ecb.europa.eu/stats/eurofxref/eurofxref-hist-90d.xml
    	File file = new File("eurofxref-hist-90d.xml");
    	fillMap(file,null);
		return rateMap;
	}
	
	public static LocalDate getLatestDate()
	{
		return latestDate;
	}
	
	public static void fillMap(File file, String removeKey)
	{
		if ((removeKey!=null) && (rateMap!=null)) rateMap.remove(removeKey);
		try
		{
			DocumentBuilderFactory docFact = DocumentBuilderFactory.newInstance();
	    	DocumentBuilder docBuilder = docFact.newDocumentBuilder();
	     	Document doc = docBuilder.parse(file);
	     	Element element = doc.getDocumentElement();
	     	NodeList rootChildren = element.getChildNodes();
	     	if (rootChildren.getLength()==0) return; //error
	     	Node rootChild;
	     	ConcurrentMap<String,String> currencyMap;
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
	             		currencyMap = new ConcurrentHashMap<String,String>();
	             		attributes = dateNode.getAttributes();
	             		dateAttribute = attributes.getNamedItem("time");
	             		currencyNodes = dateNode.getChildNodes();
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
	             		//Adjust earliest and latest dates
	             		if (currentDate.compareTo(earliestDate)<0) earliestDate = currentDate;
	             		if (currentDate.compareTo(latestDate)>0) latestDate = currentDate;
	             		//Add the currency map to the entire hash map
	             		rateMap.put(dateAttribute.getNodeValue(), currencyMap);
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
	
    @RequestMapping(value="/rate", method=RequestMethod.GET)
    public Rate getRate(@RequestParam(value="currency",defaultValue="USD") String currency,@RequestParam(value="date", required=false) String date)
    {
    	//Check whether the given date string is valid, if not default to yesterday
    	LocalDate localDate = null;
    	LocalDate nowDate = LocalDate.now();
    	LocalDate yesterdayDate = nowDate.minusDays(1);
    	LocalDate localLatestDate, localEarliestDate;
    	try
    	{
    		if (date!=null) localDate = LocalDate.parse(date);
    		else date = nowDate.toString();
    	}
    	catch(DateTimeParseException e)
    	{
    		date = yesterdayDate.toString();
    	}
    	synchronized(this)
    	{
    		localLatestDate = LocalDate.parse(latestDate.toString());
    		localEarliestDate = LocalDate.parse(earliestDate.toString());
    	}
    	LocalDate latestLimitDate = localLatestDate.plusDays(1);
    	LocalDate earliestLimitDate = localEarliestDate.minusDays(1);
    	if ((localDate == null) || !((localDate.isAfter(earliestLimitDate)) && (localDate.isBefore(latestLimitDate)))) date = yesterdayDate.toString();
    	//Check whether the given currency is valid
    	String validCurrencies[] = {"USD","JPY","BGN","CZK","DKK","GBP","HUF","PLN","RON","SEK","CHF","NOK",
    			"HRK","RUB","TRY","AUD","BRL","CAD","CNY","HKD","IDR","ILS","INR","KRW","MXN","MYR","NZD","PHP","SGD","THB","ZAR"};
    	currency = currency.toUpperCase();
    	boolean validCurrency = false;
    	for(int i=0;i<validCurrencies.length;i++)
    	{
    		if (currency.compareTo(validCurrencies[i])==0)
    		{
    			validCurrency=true;
    			break;
    		}
    	}
    	if (!validCurrency) currency = validCurrencies[0];
    	//Retrieve the requested information from the hash map
    	ConcurrentMap<String,String> dateMap = rateMap.get(date);
    	if (dateMap==null) return new Rate(date,currency,"data unavailable");
    	String rate = dateMap.get(currency);
    	if (rate==null) return new Rate(date,currency,"error");
        return new Rate(date,currency,rate);
    }
}