package me.sunnen.euroconverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RateFetcher
{
	@Scheduled(cron = "10 15 * * MON-FRI")
    public void fetchRatesAndUpdate()
	{
        //Get file: http://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml
		FileOutputStream fos = null;
		try
		{
			URL url = new URL("http://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml");
			ReadableByteChannel rbc = Channels.newChannel(url.openStream());
			fos = new FileOutputStream("eurofxref-daily.xml");
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		}
		catch(MalformedURLException e)
		{
			System.out.println("Exception: URL is malformed!");
			return;
		}
		catch(IOException e)
		{
			System.out.println("Exception: IO problem!");
			//TODO might retry a couple of times
			return;
		}
		finally
		{
			if (fos!=null)
			try
			{
				fos.close();
			}
			catch (IOException e)
			{
				System.out.println("Exception: Error closing output stream!");
			}
		}
		//Update the rate hash map
		File file = new File("eurofxref-daily.xml");
    	RateController.fillMap(file,RateController.getLatestDate().toString());
    	//Update earliest and latest dates
    	//TODO
    }
}
