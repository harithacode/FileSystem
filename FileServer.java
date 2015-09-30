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
import java.net.ServerSocket;
import java.io.RandomAccessFile;

public class FileServer
{
	//Types of requests received by server:
	//Read request Format: "read:<number of bytes>:<filename>:<offset>" 
	//Write request format: "write:<filename>". The write mode is always append mode.
	//LastModified request Format: "lastmodified:<filename>"

	//The server should be initialized with a default directory and the server can serve files only from that directory. However, it can 
	//search subdirectories for files too if the specified file is not in the given directory.

	//To run the server type "java FileServer <port number> <default server directory>". Enter the path using forwards slashes. For eg: C:/Users/cs554/Desktop

	//This server processes the requests sequentially (with out multi-threading.)
	
	private static int portNumber = 0;
	private static String homeDir = null;

	public static void main(String args[])
	{
	try{
		String portNo = args[0];
		portNumber = Integer.parseInt(portNo);
		homeDir = args[1];
		String requestString = null;
		Socket clientSock = null;
		ServerSocket listeningSocket = new ServerSocket(portNumber);
		System.out.println("Server is listening on port " + portNo);
		int i=0;
		
		//listen for requests
		while(true)
		{
			clientSock = listeningSocket.accept();

			if(clientSock.isConnected() && clientSock!=null)
			{
				//Get the request string
				int parseCount = 0;
				DataInputStream dis = new DataInputStream(clientSock.getInputStream());
				requestString = dis.readUTF();

				//Now parse the request string and extract paramenters
				String requestType = null;
				while(parseCount<requestString.length())
				{
					parseCount++;
					if(requestString.charAt(parseCount-1) == ':')
					break;
				}
				//parseCount now contains the position(not index) of first : in requestString
				requestType = Character.toString(requestString.charAt(0));
				for(i=1;i<parseCount-1;i++)
				{
					requestType = requestType + Character.toString(requestString.charAt(i));
				}
				//-----------------THE TYPE OF REQUEST HAS BEEN EXTRACTED.---------------- 
				
				//Parse the rest of request and delegate the request to a request handler
				if(requestType.equals("read")) 
				{
					int byteCount = 0; 
					while(parseCount<requestString.length())
					{
						parseCount++;
						if(requestString.charAt(parseCount-1) == ':')
						break;
					}
					//parseCount now contains the position of second : in the requestString
					String numberOfBytes = Character.toString(requestString.charAt(requestType.length()+1));
					for(i=requestType.length()+2;i<parseCount-1;i++)
					{
						numberOfBytes = numberOfBytes + Character.toString(requestString.charAt(i));
					}
					byteCount = Integer.parseInt(numberOfBytes);
					//-------Number of bytes requested by client has been extracted-----

					while(parseCount<requestString.length())
					{
						parseCount++;
						if(requestString.charAt(parseCount-1) == ':')
						break;
					}
					//parseCount now contains the position of third : in the requestString
					String fileName = Character.toString(requestString.charAt(requestType.length()+numberOfBytes.length()+2));
					for(i=requestType.length()+numberOfBytes.length()+3;i<parseCount-1;i++)
					{
						fileName = fileName + Character.toString(requestString.charAt(i));
					}
					//------------file name of the file requested by the client has been extracted------------

					String offset = Character.toString(requestString.charAt(parseCount));
					for(i=parseCount+1;i<requestString.length();i++)
					{
						offset = offset + Character.toString(requestString.charAt(i));	
					}
					long fileOffset = Long.parseLong(offset);
					//------------offset specified by the client has been extracted.-----------------
					
					readFile(clientSock,fileName,byteCount,fileOffset);
				}
				if(requestType.equals("write"))
				{
					String fileName = Character.toString(requestString.charAt(parseCount));
					for(i=parseCount+1;i<requestString.length();i++)
					{
						fileName = fileName + Character.toString(requestString.charAt(i));	
					}
					writeFile(clientSock,fileName);
				}
				if(requestType.equals("lastmodified"))
				{
					String fileName = Character.toString(requestString.charAt(parseCount));
					for(i=parseCount+1;i<requestString.length();i++)
					{
						fileName = fileName + Character.toString(requestString.charAt(i));	
					}
					lastModifiedTime(clientSock,fileName);
				}
				clientSock.close();
			}
	 
		}//end of while

		}//end of try
	catch (Exception e) 
		{   
			e.printStackTrace();    
		}
	}//end of main

	public static void readFile(Socket sock, String fileName, int byteCount, long fileOffset)
	{
		//call a method which returns the full path of the file.
		try{
		String filePath = homeDir+"/"+fileName;
		File myFile = new File(filePath);
		if(!(myFile.exists())) //if the file is not in the default home directory then search the sub directories.
		{
		filePath = fileSearch(homeDir,fileName);
		}

		byte[] returnNothing = {(byte) 'a'};
		int bytesRead = 0;

		if(filePath.equals("notfound")) //the file is not in the server directory
		{
			//return nothing
			OutputStream os = sock.getOutputStream();
			os.write(returnNothing, 0, 0);
			os.close();
		}
		else
		{
			byte[] dataHolder = new byte[byteCount];
			myFile = new File(filePath);
			RandomAccessFile requiredFile = new RandomAccessFile(myFile,"r");
			requiredFile.seek(fileOffset);
			bytesRead = requiredFile.read(dataHolder); //read the file from the offset
			if(bytesRead>0)
			{
				OutputStream os = sock.getOutputStream();
				os.write(dataHolder, 0, bytesRead);
				os.flush();
				os.close();
			}
			else
			{
				//return nothing
				OutputStream os = sock.getOutputStream();
				os.write(returnNothing, 0, 0);
				os.close();
			}
		}
		}//end of try
		catch (Exception e) 
		{   
			e.printStackTrace();    
		}
	}//end of readFile

	public static String fileSearch(String currentDir, String fileName)
	{
		//searchs for the file in the current directory. If not found, invokes it self on each sub directory.
		File folder = new File(currentDir);
		File[] listOfFiles = folder.listFiles();
		int i=0;
		for (i = 0; i < listOfFiles.length; i++) 
		{
			if (listOfFiles[i].isFile() && listOfFiles[i].getName().equals(fileName))
			{
				return currentDir+"/"+listOfFiles[i].getName();
			}
		}
		for (i = 0; i < listOfFiles.length; i++)
		{
			if (listOfFiles[i].isDirectory())
			{
				String result = fileSearch(currentDir+"/"+listOfFiles[i].getName(),fileName);
				if(!(result.equals("notfound")))
				{
					return result;
				}
			}
		}
		return "notfound"; 
	}

	public static void writeFile(Socket sock, String fileName)
	{
		try{
		String filePath = fileSearch(homeDir,fileName);
		int bytesRead = 0;
		byte[] dataHolder = new byte[1024]; //Length of dataHolder
		if(filePath.equals("notfound"))
		{
			filePath = homeDir+"/"+fileName; 
		}
		File myFile = new File(filePath);
		DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
		dos.writeUTF("yes"); //This line is used deliberately to synchronize client server communication, so that the server does not have to do two consequitive recieves.
		InputStream is = sock.getInputStream();
		FileOutputStream fos = new FileOutputStream(myFile,true);
		while(true)
		{
			bytesRead = is.read(dataHolder); //read from input stream
			if(bytesRead>0)
			{
				fos.write(dataHolder, 0, bytesRead); //write to file
			}
			if(bytesRead<dataHolder.length)
				break;
		}
		fos.close();
		dos.close();
		}//end of try
		catch (Exception e) 
		{   
			e.printStackTrace();    
		}
	}//end of writeFile

	public static void lastModifiedTime(Socket sock, String fileName)
	{
		try{
		String filePath = fileSearch(homeDir,fileName);
		DataOutputStream dos = new DataOutputStream(sock.getOutputStream());
		if(!(filePath.equals("notfound")))
		{
			File myFile = new File(filePath);
			long mostRecent = myFile.lastModified();
			dos.writeLong(mostRecent);
		}
		else
		{
			long mostRecent = 0; //If no such file exists with the name, return 0
			dos.writeLong(mostRecent);
		}
		dos.close();
		}//end of try
		catch (Exception e) 
		{   
			e.printStackTrace();    
		}
	}//end of lastModifiedTime

}//end of FileServer