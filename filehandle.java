public class filehandle
{
	private String fileName;
	private long fh;
	private long offset; 
	private static long counter = 0;
	private String ip;
	private int portNumber;
	private int bufferIndex; 
	private String localLocation = null; 
	private long initialSize = 0;

	public filehandle(String hostAddress, int portNo, String requestedFile)
	{
		counter++;
		fh = counter;
		offset = 0;
		ip = hostAddress;
		portNumber = portNo;
		fileName = requestedFile;
		bufferIndex = -1;
	}

	public void setOffset(long newOffset)
	{
		offset = newOffset;
	}

	public String getFileName()
	{
		return fileName;
	}

	public long getFh()
	{
		return fh;
	}

	public long getOffset()
	{
		return offset;
	}

	public String getIp()
	{
		return ip;
	}

	public int getPortNumber()
	{
		return portNumber;
	}

	public void setBufferIndex(int index)
	{
		bufferIndex = index;
	}

	public int getBufferIndex()
	{
		return bufferIndex;
	}

	public String getLocalLocation()
	{
		return localLocation;
	}

	public void setLocalLocation(String localPath)
	{
		localLocation = localPath;
	}
	public long getInitialSize()
	{
		return initialSize;
	}
	public void setInitialSize(long fileSize)
	{
		initialSize = fileSize;
	}
}