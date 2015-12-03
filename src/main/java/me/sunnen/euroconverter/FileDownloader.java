package me.sunnen.euroconverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * Class downloads a file from a given URL
 * @author Dany
 *
 */
public class FileDownloader
{
	private URL url;
	private String fileName;
	private File file;
	
	private final int maxAttempts = 5; //maxmimum number of tries to download file
	private final long retryDelay = 60000L; //waiting time after failed download
	
	/**
	 * Download a file from a supplied URL to a local file with a given name
	 * @param urlString the URL to download from
	 * @param fileName the name of the file that gets the content
	 */
	public FileDownloader(String urlString, String fileName) throws IOException, MalformedURLException
	{
		//Create an URL from the urlString
		FileOutputStream fos = null;
		try
		{
			this.url = new URL(urlString);
		}
		catch (MalformedURLException e)
		{
			System.out.println("Exception: URL is malformed!");
			throw e;
		}
		//Try to download the file on the fiven URL.
		//If it fails retry
		int nAttempts=0;
		boolean success=false;
		while(!success)
		{
			try
			{
				nAttempts++;
				ReadableByteChannel rbc = Channels.newChannel(url.openStream());
				fos = new FileOutputStream(fileName,false);
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
				success=true; //break out of loop
			}
			catch(IOException e)
			{
				System.out.println("Exception: IO problem!");
				//Retry maximum maxAttempts-1 times
				try
				{
					Thread.sleep(retryDelay);
				}
				catch (InterruptedException e1)
				{
					System.out.println("Exception: Sleeping thread interrupted!");
				}
				if (nAttempts==maxAttempts)
				{
					throw new IOException();
				}
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
		}
		//Create a file object
		try
		{
			file = new File(fileName);
		}
		catch(NullPointerException e)
		{
			file = null;
			System.out.println("Exception: Could not create file object as name was null!");
		}
	}
	
	public URL getURL()
	{
		return url;
	}
	
	public String getFileName()
	{
		return fileName;
	}
	
	public File getFile()
	{
		return file;
	}
}
