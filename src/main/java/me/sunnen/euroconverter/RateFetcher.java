package me.sunnen.euroconverter;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RateFetcher
{
	private final String fileName = "eurofxref-daily.xml";
	private final String fileURL = "http://www.ecb.europa.eu/stats/eurofxref/"+fileName;
	
	@Scheduled(cron = "0 10 15 * * MON-FRI") //Get the rates every work day (MON-FRI) at 15:10
    public void fetchRatesAndUpdate()
	{
		//Try to retrieve the XML file with the daily rates
		FileDownloader fileDownloader;
		try
		{
			fileDownloader = new FileDownloader(fileURL,fileName);
		}
		catch (Exception e)
		{
			System.out.println("Exception: Could not retrieve the rates");
			return;
		}
		//Update the rate hash map
		RateController.ratesContainer.addEntriesFromFile(fileDownloader.getFile(), true);
    }
}
