Workload Generator:

Compiling:

The build.xml is located at FileGen directory

The ant version is 1.9.3 is compiled on April 8 2014

To compile the project, type 'ant' or 'ant jar' in the WordCountJava directory. This will compile
the project and generate the jars.

'ant compile' will compile without generating the jars.

'ant clean' will delete the generated bin folder.

The jar files will be generated in CS544_RTP_RTSP directory as output/videostream_javaclient.jar and output/videostream_javaserver.jar

Running the code:

Run the server as java -jar output/videostream_javaserver.jar <port no>

Ex: java -jar output/videostream_javaserver.jar 8888

Run the client as java -jar output/videostream_javaclient.jar <server> <RTP Port> <Filename>

Ex: java -jar output/videostream_javaclient.jar localhost 8888 /home/sughosh/movie.Mjpeg 
