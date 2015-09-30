import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.util.ArrayList;

public class FileSystemClientAPI implements fileSystemAPI
{
	private ArrayList<filehandle> filesList = new ArrayList<filehandle>();
	private ArrayList<FileBuffer> bufferList = new ArrayList<FileBuffer>();

	public filehandle open(String url) throws java.io.IOException
	{
		String hostAddress = null;
		String hostPort = null;
		int hostPortNumber = 0;
		String requestedFile = null;
		filehandle newHandle = null;
		int parseCount = 0;
		int i = 0;
		filehandle fh = null;
			
		try{
			//from the URL, extract IP address, port and filename
			while(parseCount<url.length())
			{
				parseCount++;
				if(url.charAt(parseCount-1) == ':')
					break;
			}
			//parseCount now contains the position of : in URL
			hostAddress = Character.toString(url.charAt(0));
			for(i=1;i<parseCount-1;i++)
			{
				hostAddress = hostAddress + Character.toString(url.charAt(i));
			}
			//-------IP of the fileserver has been extracted---------------
			
			while(parseCount<url.length())
			{
				parseCount++;
				if(url.charAt(parseCount-1) == '/')
					break;
			}
			//parseCount now contains the position of / in the URL
			hostPort = Character.toString(url.charAt(hostAddress.length()+1));
			for(i=hostAddress.length()+2;i<parseCount-1;i++)
			{
				hostPort = hostPort + Character.toString(url.charAt(i));
			}
			hostPortNumber = Integer.parseInt(hostPort);
			//-----------Port number of server process has been extracted-----------

			requestedFile = Character.toString(url.charAt(parseCount));
			for(i=parseCount+1;i<url.length();i++)
			{
			requestedFile = requestedFile + Character.toString(url.charAt(i));	
			}
			//------------file name of required file has been extracted-------------

			//create a new file handle object
			newHandle = new filehandle(hostAddress, hostPortNumber, requestedFile);
			filesList.add(newHandle);
			fh = newHandle;
			
		}
		catch (Exception e) 
		{      
			e.printStackTrace();    
		}
		return fh;
	}

	public int read(filehandle fh, byte[] data)	throws java.io.IOException 
	{
		FileBuffer newBuffer = null;
		int numberOfBytesRead = -1;
		try
		{
			//first check if the file handle has a filebuffer object associated with it
			//file buffer is allocated the first time a read is called on a file handle.
			if(fh.getBufferIndex()==-1)
			{
				//create a file buffer and associate it with the file handle.
				newBuffer = new FileBuffer();
				bufferList.add(newBuffer);
				fh.setBufferIndex(bufferList.size()-1);
			}
			//retrive the buffer of the filehandle and call buffer method to read data into given array.
			newBuffer = bufferList.get(fh.getBufferIndex());
			numberOfBytesRead = newBuffer.readBuffer(fh,data);
		}
		catch (Exception e) 
		{      
			e.printStackTrace();    
		}
		return numberOfBytesRead;
	}

	public boolean write(filehandle fh, byte[] data) throws java.io.IOException
	{
		boolean operationSuccess = false;

		try{
		//Construct a Server request string and send the data
		String serverRequestString = "write:"+fh.getFileName();
		Socket sock = new Socket(fh.getIp(), fh.getPortNumber());
		DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
		dos.writeUTF(serverRequestString); 
		DataInputStream dis = new DataInputStream(sock.getInputStream());
		String serverReady = dis.readUTF(); //This line is added deliberately to synchronize send and recieve b/n client and server.
		if(serverReady.equals("yes"))
		{  
			OutputStream os = sock.getOutputStream();
			os.write(data, 0, data.length);
			os.flush();
	    	sock.close();
	    	operationSuccess = true;
		}
		}
		catch (Exception e) 
		{   
			operationSuccess = false;
			e.printStackTrace();    
		}
		return operationSuccess;
	}

	public boolean close(filehandle fh) throws java.io.IOException
	{
		boolean operationSuccess = false;
		int i=0;
		int bufferId = -1;
		int fhId = -1;
		int newId = -1;
		FileBuffer removedBuffer;
		filehandle removedHandle;
		try{
			//For non caching client, we simply have to discard the file handle object and its associated buffer. Plus for each file handle object which 
			//has a buffer whose index is greater than that of the presently discarded buffer, we have to decrement their buffer indexes.
			bufferId = fh.getBufferIndex();
			for(i=0;i<filesList.size();i++)
			{
				if(fh==filesList.get(i))
				{
					fhId = i;
				}
				if(filesList.get(i).getBufferIndex()>bufferId)
				{
					newId = filesList.get(i).getBufferIndex() - 1;
					filesList.get(i).setBufferIndex(newId);
				}
			}
			removedBuffer = bufferList.remove(bufferId);
			removedHandle = filesList.remove(fhId);
			operationSuccess = true;
		}
		catch (Exception e) 
		{   
			operationSuccess = false;   
			e.printStackTrace();    
		}
		return operationSuccess;
	}

	public boolean isEOF(filehandle fh)	throws java.io.IOException
	{
		boolean endOfFile = false;
		if(fh.getBufferIndex()>=0)
		{
			FileBuffer fb = bufferList.get(fh.getBufferIndex());
			try
			{
				endOfFile = fb.isEndOfFile(fh);
			}
			catch(Exception e)
			{
				e.printStackTrace(); 
			}
		}
		else //buffer has not yet been allocated to this file handle
		{
			byte[] testArray = new byte[1];
			int serverReturnValue = 0;
			try{
				String serverRequestString = "read:1:"+fh.getFileName()+":0";
				Socket sock = new Socket(fh.getIp(), fh.getPortNumber());
				DataOutputStream dos = new DataOutputStream(sock.getOutputStream());   
				dos.writeUTF(serverRequestString);   
				InputStream is = sock.getInputStream();
				serverReturnValue = is.read(testArray,0,testArray.length);
				if(serverReturnValue == 1)
					endOfFile = false;
				else
					endOfFile = true;
				sock.close();
			}
			catch(Exception e)
			{
				endOfFile = true;
				e.printStackTrace(); 
			}

		}
		return endOfFile;
	}
}