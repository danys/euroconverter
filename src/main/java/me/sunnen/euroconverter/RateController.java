package me.sunnen.euroconverter;

import java.io.IOException;
import java.net.MalformedURLException;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMethod;

@RestController
public class RateController
{
	public static RatesContainer ratesContainer = init();
	
	private static final String fileName = "eurofxref-hist-90d.xml";
	private static final String fileURL = "http://www.ecb.europa.eu/stats/eurofxref/"+fileName;
	
	private static RatesContainer init()
	{
		//Retrieve the 90 days FX rates file from the ECB
		FileDownloader fileDownloader;
		try
		{
			fileDownloader = new FileDownloader(fileURL, fileName);
		} 
		catch (MalformedURLException e)
		{
			System.out.println("Exception: Supplied invalid FX rates URL!");
			return null;
		}
		catch (IOException e)
		{
			System.out.println("Exception: Error getting FX rates file!");
			return null;
		}
		RatesContainer rc = new RatesContainer();
		rc.addEntriesFromFile(fileDownloader.getFile(),false);
		return rc;
	}
	
    @RequestMapping(value="/rate", method=RequestMethod.GET)
    public Rate getRate(@RequestParam(value="currency",defaultValue="USD") String currency,@RequestParam(value="date", required=false) String date)
    {
    	return ratesContainer.getRate(currency, date);
    }
    
    @RequestMapping(value="/currencies", method=RequestMethod.GET)
    public Currencies getCurrencies()
    {
    	return new Currencies(ratesContainer.getCurrencies());
    }
    
    @RequestMapping(value="/dates", method=RequestMethod.GET)
    public Dates getDates()
    {
    	return new Dates(ratesContainer.getDates());
    }
}