package edu.iit.cs.cs544;


import java.io.FileInputStream;



public class VideoStream {
	FileInputStream fis;
	int currentFrame;
	public VideoStream(String filename) throws Exception{
		fis = new FileInputStream(filename);
		currentFrame = 0;
	}
	
	public int getNextFrame(byte[] frame) throws Exception{
		int length = 0;
		String length_string;
		byte[] header = new byte[5];
		
		fis.read(header, 0, 5);
		length=Integer.parseInt(new String(header));
		System.out.println("Image size is "+ length +" bytes");
		return(fis.read(frame, 0, length));
	}
}
