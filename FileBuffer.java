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

public class FileBuffer
{
	private byte[] dataHolder;
	private int nextByteIndex;
	private int realSize;
	//realSize is the length of valid data in the buffer. nextByteIndex is the buffer offset.
	
	public FileBuffer()
	{
		dataHolder = new byte[1024];
		nextByteIndex = 0;
		realSize = 0;
	}
	//read from buffer corresponding to this filehandle into the given array.
	public int readBuffer(filehandle fh,byte[] givenArray) 
	{
		int i;
		int numberOfBytesRead = -1;
		String fileOffset = Long.toString(fh.getOffset());
		long filePosition = fh.getOffset();
		String serverRequestString = "read:1024:"+fh.getFileName()+":"+fileOffset;
		if(realSize<dataHolder.length && realSize>0 && nextByteIndex==realSize) //realSize is the length of valid data in the buffer. nextByteIndex is the buffer offset.
		{
			return -1;
		}
		//try to fill in each byte in givenArray. Possible problems: 
		//The end of buffer is reached, 
		//the end of file is reached, 
		for(i=0;i<givenArray.length;i++)
		{
			if(nextByteIndex < realSize)//if the end of buffer is not reached nextbyteindex < realSize
			{
				//read the byte into buffer. Increment buffer offset
				givenArray[i] = dataHolder[nextByteIndex];
				nextByteIndex++;
			}
			else 
			{
				if((nextByteIndex==0 && realSize==0) || (nextByteIndex==dataHolder.length && realSize==dataHolder.length))
				{
					try{
					//retrive more of the given file from server	
					Socket sock = new Socket(fh.getIp(), fh.getPortNumber());
					DataOutputStream dos = new DataOutputStream(sock.getOutputStream());   
					dos.writeUTF(serverRequestString);   
					InputStream is = sock.getInputStream();
					numberOfBytesRead = is.read(dataHolder,0,dataHolder.length);
					}
					catch(Exception e)
					{
						numberOfBytesRead = -1;
						e.printStackTrace();
					}

					if(numberOfBytesRead==-1) //meaning, no more file left to be read.
					{
						break;
					}
					else
					{
						//update file offset for this file handle, read one byte into the array, update realsize and buffer offset
						realSize = numberOfBytesRead;
						filePosition = filePosition + Long.parseLong(String.valueOf(numberOfBytesRead));
						fh.setOffset(filePosition);
						nextByteIndex = 0;
						givenArray[i] = dataHolder[nextByteIndex];
						nextByteIndex++;
					}
				}
				if(nextByteIndex == realSize && realSize>0 && realSize<dataHolder.length) 
				{
					break;
				}
			}
		}//end of for loop to read data into given array.
		if(i==0)
			return -1;
		else
			return i;
	}//end of readBuffer
	
	public boolean isEndOfFile(filehandle fh)
	{
		byte[] testArray = new byte[1];
		int serverReturnValue = 0;
		boolean fileEnd = false;
		try{
			if(nextByteIndex<realSize)
				fileEnd = false;
			if((nextByteIndex==0 && realSize==0) || (nextByteIndex==dataHolder.length && realSize==dataHolder.length))
			{
				//query the server for more data and see what it returns.
				String fileOffset = Long.toString(fh.getOffset());
				String serverRequestString = "read:1:"+fh.getFileName()+":"+fileOffset;
				Socket sock = new Socket(fh.getIp(), fh.getPortNumber());
				DataOutputStream dos = new DataOutputStream(sock.getOutputStream());   
				dos.writeUTF(serverRequestString);   
				InputStream is = sock.getInputStream();
				serverReturnValue = is.read(testArray,0,testArray.length);
				if(serverReturnValue == 1)
					fileEnd = false;
				else
					fileEnd = true;
			}
			if(nextByteIndex == realSize && realSize>0 && realSize<dataHolder.length) 
				fileEnd = true;
		}
		catch (Exception e)
		{
			fileEnd = true;
			e.printStackTrace(); 
		}
		return fileEnd;
	}
}