# FileSystem
An implementation of file system API written from scratch in Java

First compile the class files:
filehandle.java
FileBuffer.java 
FileServer.java
fileSystemAPI.java
FileSystemClientAPI.java
testClient.java
Make sure that all of them are in same directory.

First run the server by entering the following in command prompt:
java FileServer <port number> <server home directory>
Then run testClient.java by entering
java testClient <server ip in Ipv4 format> <port no. of server> <filename including extension>

FileSystemClientAPI.java impements the fileSystemAPI.java interface. FileBuffer.java is used by FileSystemClientAPI.java to cache 1KB blocks of the file from the server. When the offset requested by FileSystemClientAPI falls beyond the cache, the cache is updated and the correct data is returned.
