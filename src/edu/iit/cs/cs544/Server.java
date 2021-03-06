package edu.iit.cs.cs544;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.StringTokenizer;


import javax.swing.JFrame;
import javax.swing.Timer;
import javax.swing.JLabel;


public class Server extends JFrame implements ActionListener{
	
	//video frame images rate
	Timer rate;
	static int FRAME_RATE_OF_VIDEO_STREAM = 50;
	byte[] buffer;
	JLabel label;
	Socket RTSPSocket;//RTSP message send&receive
	InetAddress ClientIPAddress;
	static int RTSPServerState;
	static int INIT = 0;
	static int READY = 1;
	static int PLAYING = 2;
	static int SETUP = 3;
	static int PLAY = 4;
	static int PAUSE = 5;
	static int TEARDOWN = 6;
	static int DESCRIBE=7;
	static BufferedReader RTSPBufferedReader;
	static BufferedWriter RTSPBufferedWriter;
	static String VideoFile;
	VideoStream video;
	DatagramSocket RTPSocket;
	int imageTransmitted = 0;
	int VIDEO_LENGTH = 1000;
	static int MJPEG_TYPE = 26;
	//static int FRAME_PERIOD = 100;
	DatagramPacket sendUDPpacket;
	int RTPdestinationPort = 0;
	int sequenceNumberOfRTSP = 0;
	static int RTSP_ID = 111111;
	static int RTSPPortNumber;
	public Server(){
		super("Server");
		rate = new Timer(FRAME_RATE_OF_VIDEO_STREAM,this);
		rate.setInitialDelay(0);
        //Set whether Timer coalesces multiple pending ActionEvent firings
		rate.setCoalesce(true);
		//buffersize
		buffer = new byte[10000];
		addWindowListener(new WindowAdapter(){
			public void closeWindow(WindowEvent event){
				rate.stop();
				System.exit(0);
			}
		});
		
		//GUI
		label = new JLabel("Send frame #   ", JLabel.CENTER);
		getContentPane().add(label, BorderLayout.CENTER);
	}
	
	public InetAddress getClientIPAddress() {
		return this.ClientIPAddress;
	}
	
	public static void main(String args[]){
		Server server = new Server();
		server.pack();
		server.setVisible(true);
		
		RTSPPortNumber = Integer.parseInt(args[0]);
		
		//TODO:!!!!!COTINUE
		ServerSocket listenSocket;
		listenSocket=null;
		try {
			listenSocket = new ServerSocket(RTSPPortNumber);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    try {
			server.RTSPSocket = listenSocket.accept();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    try {
			listenSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		server.ClientIPAddress = server.RTSPSocket.getInetAddress();
		System.out.println(server.ClientIPAddress);
		RTSPServerState = INIT;//initiate RTSP state
		
		try {
			RTSPBufferedReader = new BufferedReader(new InputStreamReader(server.RTSPSocket.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(server.RTSPSocket.getOutputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int request;
		boolean finish = true;
		while(finish){
			
			//TODO:parse_RTSP_request
			request = server.parseRTSPrequest();
			System.out.println("Request in main is "+request);
			
			if(request == SETUP){
				finish = true;
				RTSPServerState = READY;
				//TODO:send_RTSP_request
				server.responseOfRTSPsent();
				try {
					server.video = new VideoStream(VideoFile);
					System.out.println("Video file is "+VideoFile);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					server.RTPSocket = new DatagramSocket();
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		
			
			if((request == PLAY) && (RTSPServerState == READY)){
				server.responseOfRTSPsent();
				server.rate.start();
				RTSPServerState = PLAYING;
				System.out.println("Playing");
			}else if((request == PAUSE) && (RTSPServerState == PLAYING)){
				server.responseOfRTSPsent();
				server.rate.stop();
				RTSPServerState = READY;
				System.out.println("Ready");
			}else if(request == TEARDOWN){
				server.responseOfRTSPsent();
				server.rate.stop();
				try {
					server.RTSPSocket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				server.RTPSocket.close();
				System.exit(0);
			}
			else if((request==DESCRIBE) && (RTSPServerState==READY)) {
			System.out.println("Describe");
			server.responseOfRTSPsent();
			server.senddescribe();
			RTSPServerState=READY;
			}
		}
	}
	
	public void actionPerformed(ActionEvent event){
		if(imageTransmitted < VIDEO_LENGTH){
			imageTransmitted++;
			try{
				int image_length = video.getNextFrame(buffer);
				RTPpacket packetRTP = new RTPpacket(MJPEG_TYPE, imageTransmitted, imageTransmitted*FRAME_RATE_OF_VIDEO_STREAM, buffer, image_length);
				int packet_length = packetRTP.getlength();
				byte[] packet_bits = new byte[packet_length];
				packetRTP.getpacket(packet_bits);
				sendUDPpacket = new DatagramPacket(packet_bits, packet_length, ClientIPAddress, RTPdestinationPort);
				RTPSocket.send(sendUDPpacket);
				System.out.println("Send frame # " + imageTransmitted);
				packetRTP.printheader();
				label.setText("send frame # " + imageTransmitted);
			}catch(Exception e){
				System.out.println("EOF");
				System.exit(0);
			}
		}
		else{
			rate.stop();
		}
	}
	
	private int parseRTSPrequest(){
		int request = -1;
		try{
			String RequestLine = RTSPBufferedReader.readLine();
			System.out.println(RequestLine);
			StringTokenizer tokens = new StringTokenizer(RequestLine);
			String requestTypeString = tokens.nextToken();
			
			System.out.println("Request type string is "+requestTypeString);
			if((new String(requestTypeString)).compareTo("SETUP") == 0){
				request = SETUP;
			}else if((new String(requestTypeString)).compareTo("PLAY") == 0){
				request = PLAY;
			}else if((new String(requestTypeString)).compareTo("PAUSE") == 0){
				request = PAUSE;
			}else if((new String(requestTypeString)).compareTo("TEARDOWN") == 0){
				request = TEARDOWN;
			}
			else if((new String(requestTypeString)).compareTo("DESCRIBE") == 0){
				request = DESCRIBE;
			}
			
			System.out.println("Request is "+request);
			if(request == SETUP){
				VideoFile = tokens.nextToken();
				System.out.println("Video File is "+VideoFile);
			}
			
			String sequenceNumber = RTSPBufferedReader.readLine();
			System.out.println("Seq no is "+sequenceNumber);
			sequenceNumberOfRTSP = Integer.parseInt(sequenceNumber);
			String LastLine = RTSPBufferedReader.readLine();
			System.out.println("Last line is "+LastLine);
			
			if(request == SETUP){
				tokens = new StringTokenizer(LastLine," ");
					tokens.nextToken();
					RTPdestinationPort = Integer.parseInt(tokens.nextToken());
					System.out.println("RTP port is "+RTPdestinationPort);
				}
			}catch(Exception e){
				e.printStackTrace();
				System.out.println("Exception caught 2: " + e);
				System.exit(0);
		}
		return(request);
	}
	private void responseOfRTSPsent(){
		try{
			RTSPBufferedWriter.write("RTSP/1.0 200 OK \n");
			RTSPBufferedWriter.write("CSeq: " + sequenceNumberOfRTSP +"\n");
			RTSPBufferedWriter.write("Session: " + RTSP_ID + "\n");
			RTSPBufferedWriter.flush();
		}catch(Exception e){
			System.out.println("Exception caught 3: " + e);
			System.exit(0);
		}
	}
	
	private String getDate() {
		DateFormat df= new SimpleDateFormat("dd/MM/yy HH:mm:ss");
		Date date = new Date();
		return df.format(date).toString();
	}
	private void senddescribe() {
		System.out.println("Inside senddescribe");
		String date=getDate();
		Calendar time=Calendar.getInstance();
		long SessId=time.getTimeInMillis();
		Random r=new Random(System.currentTimeMillis());
		InetAddress ClientIPAddress =getClientIPAddress();
		long SessVersion=SessId+r.nextInt(1000);
		try {
		RTSPBufferedWriter.write("Date "+date+ "\n");
		RTSPBufferedWriter.write("Content-Type: application/sdp"+"\n");
		RTSPBufferedWriter.write("v=0 "+"\n");
		RTSPBufferedWriter.write("o=sdivanji "+SessId +" " + SessVersion+" " +"IN IP4 "+"127.0.0.1 "+"\n");
		RTSPBufferedWriter.write("s=CS544_Project_Demo "+"\n");
		RTSPBufferedWriter.write("i=Demo for a RTP/RTSP streaming video application "+"\n");
		RTSPBufferedWriter.write("u=http://localhost/ "+"\n");
		RTSPBufferedWriter.write("e=sdivanji@hawk.iit.eu"+"\n");
		RTSPBufferedWriter.write("IN IP4 "+ClientIPAddress+"\n");
		RTSPBufferedWriter.write("t= "+System.currentTimeMillis()+"\n");
		RTSPBufferedWriter.write("a=RecvOnly"+"\n");
		RTSPBufferedWriter.write("m=video  "+RTSPPortNumber+" RTP/UDP 0"+"\n");
		RTSPBufferedWriter.write("done"+"\n");
		RTSPBufferedWriter.flush();
		System.out.println("SendDescribe done");
	}
	catch (IOException e) {
		e.printStackTrace();
	}
}
}