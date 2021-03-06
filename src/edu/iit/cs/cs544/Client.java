package edu.iit.cs.cs544;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.text.IconView;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


public class Client extends JFrame {
	//GUI
	private Container contents;
	private JLabel option,Iconlabel;
	private JButton setup,play, pause,teardown,describe;
	ImageIcon icon;
	//RTP 
	DatagramSocket RTPSocket;
	DatagramPacket rcvdpacket;
	public static final int RTP_PORT=8888;
	Timer timer;
	int bufsize=1024*1024;
	byte[] buf= new byte[bufsize];
	
	//RTSP	
	public static final int INIT=0;
	public static final int READY=1;
	public static final int PLAYING=2;
	public static int state;
	Socket RTSPSocket;
	

	
	//File reader and writer
	public static BufferedReader buffreader;
	public static BufferedWriter buffwriter;
	public static String VideoFileName;
	public static int RTSPSeq=0;
	public static int RTSPId=0;
	public static final String CRLF ="\r\n";
	
	//Video format
	
	public static final int MJPEG_TYPE =26; //RTP payload type for MJPEG file.
	
	
	
	public Client( )  {
	  super( "Welcome" );
	  
	  contents = getContentPane( );
	  //contents.add(frame);
	  contents.setLayout( new FlowLayout( ) );
	  option = new JLabel( "Choose your option" );
	  Iconlabel=new JLabel("");
	  setup = new JButton("SETUP");
	  play = new JButton( "PLAY" );
	  pause= new JButton( "PAUSE" );
	  teardown= new JButton("TEARDOWN");
	  describe=new JButton("DESCRIBE");
	  
	    // add components to the window
	    contents.add( option );
	    contents.add(Iconlabel);
	    contents.add(setup);
	    contents.add( play );
	    contents.add( pause );
	    contents.add(teardown);
	    contents.add(describe);
	    
	    
	    // instantiate our event handler
	    ButtonHandler bh = new ButtonHandler( );
	    // add event handler as listener for both buttons
	    setup.addActionListener(bh);
	    play.addActionListener( bh );
	    pause.addActionListener( bh );
	    teardown.addActionListener( bh );
	    describe.addActionListener(bh);
	    setSize( 500,500 );
	    setVisible( true );
	    
	    //Init timer
	
	    timer = new Timer(10, new timerListener());
	    timer.setInitialDelay(0);
	    timer.setCoalesce(true);
	   }
	

	//private inner class event handler
	public class ButtonHandler implements ActionListener  {
	
	// implement actionPerformed method
	public void actionPerformed( ActionEvent ae )
	{
		
		
	 try    { 
	  // identify which button was pressed
		 if(ae.getSource()== setup) {
			 if (state==INIT) {
				 try {
				 RTPSocket=new DatagramSocket(RTP_PORT);
				 RTPSocket.setSoTimeout(100);
				 }
				 catch(SocketException e) {
					 e.getStackTrace();
				 }
				 //init RTSP seq no
				 RTSPSeq=1;
				 //Send RTSP SETUP request
				 
				 send_RTSP_request("SETUP");
				 
				 //Parse the server response
				 
				 if (parse_server_response()!=200) {
					 System.out.println("Invalis Response for SETUP request");
				 }
				 else {
					 state=READY;
				 }
				 
			 }			 
		 }
	 else if ( ae.getSource( ) == play) {
		 
	    if(state==READY) {
	    	RTSPSeq++;
	    	send_RTSP_request("PLAY");
	    	
	    	//parse server response
	    	if (parse_server_response() !=200) {
	    		System.out.println("Invalid server resposne for PLAY request");
	    	}
	    	else {
	    		state=PLAYING;
	    		System.out.println("PALYING");
	    		timer.start();
	    	}
	    }
	  }
	  else if ( ae.getSource( ) == pause ) {
		if(state==PLAYING) {
			RTSPSeq++;
			send_RTSP_request("PAUSE");
			
			//parse server response
			if(parse_server_response()!=200) {
				System.out.println("Invalid server response for PAUSE request");
			}
			else {
				state=READY;
				timer.stop();
			}
		}
	  }
	  
	  else if ( ae.getSource( ) == teardown ) {
		  RTSPSeq++;
		  send_RTSP_request("TEARDOWN");
		  if(parse_server_response()!=200) {
			  System.out.println("Invalid server respose for TEARDOWN request");
		  }
		  else {
			  state=INIT;
			  timer.stop();
			  System.exit(0);
		  }
	  }
	  else if(ae.getSource()==describe) {
		  if(state==READY) {
		  RTSPSeq++;
		  send_RTSP_request("DESCRIBE");
		  if(parse_server_response()!=200) {
			  System.out.println("Invalid server resposne for DESCRIBE request");
		  }
		  else {
			  String line;
			  while((line=buffreader.readLine()) != null) {
				  if(line.equals("done")) {
					  break;
				  }
			  System.out.println(line);
			  }
			  System.out.println("Done");
			  state=READY;
		  }
	  }
	  }
	 	 
	 }

	 catch( Exception e )    {
		    e.getStackTrace();
		   }
		  }
	}		 

	class timerListener implements ActionListener {
			public void actionPerformed(ActionEvent e) {
				rcvdpacket=new DatagramPacket(buf,buf.length);
				
				try {
					RTPSocket.receive(rcvdpacket);
					//Create a RTP packet
					RTPpacket rtp_packet = new RTPpacket(rcvdpacket.getData(),rcvdpacket.getLength());
					//Print important fields in the RTP packet
					System.out.println("RTP Packet Details " + "Seq no "+ rtp_packet.getsequencenumber()+ "TimeStamp " + rtp_packet.gettimestamp()+ " ms, of type " + rtp_packet.getpayloadtype());
					rtp_packet.printheader();
					
					int payload_length = rtp_packet.getpayload_length();
					System.out.println("Payload lenght is " +payload_length);
					byte []payload = new byte[payload_length];
					rtp_packet.getpayload(payload);
					
				   
				
				//Get an image object from the payload bitscreen and display it as icon
				
				Toolkit toolkit =Toolkit.getDefaultToolkit();
				Image image =toolkit.createImage(payload, 0, payload_length);
				System.out.println("Image Displayed");
				icon=new ImageIcon(image);
				Iconlabel.setIcon(icon);
				}
				//catch(InterruptedException ie) {
					//ie.printStackTrace();
				//} 
				catch (IOException e1){
					System.out.println("EOF");
					System.exit(0);
				}
			}
	}
			
			public int parse_server_response() {
				int reply_code =0;
				try {
					String status =buffreader.readLine();
					System.out.println(status);
					
					StringTokenizer st = new StringTokenizer(status);
					st.nextToken();	
					reply_code=Integer.parseInt(st.nextToken());
					if(reply_code==200) {
						String SeqNumLine = buffreader.readLine();
						System.out.println(SeqNumLine);
						
						String SessLine=buffreader.readLine();
						System.out.println(SessLine);
						st=new StringTokenizer(SessLine);
						st.nextToken();
						RTSPId=Integer.parseInt(st.nextToken());
					}	
				}
				catch (Exception e)  {
					e.printStackTrace();
					System.exit(0);
				}
				return(reply_code);
			}
			
			public void send_RTSP_request(String request_type) {
				try {
					//buffwriter.write(request_type+""+VideoFileName+" RTSP/1.0" +CRLF);
					buffwriter.write(request_type+" "+VideoFileName+ CRLF);
					//buffwriter.write(VideoFileName+CRLF);
					System.out.println(request_type+""+VideoFileName+" RTSP/1.0" +CRLF);
					buffwriter.write(RTSPSeq+ CRLF);
					System.out.println("CSReq "+ RTSPSeq+ CRLF);
					//Check the RTSP request type
					
					if(request_type.equals("SETUP")) {
						buffwriter.write("client_port= " +RTP_PORT+CRLF);
						System.out.println("Transport: RTP/UDP; client_port= " +RTP_PORT+CRLF);
						
					}
					else  {
						buffwriter.write("Session: "+RTSPId+ CRLF);
						System.out.println("Session: "+RTSPId+ CRLF);
					}
					try {
						buffwriter.flush();
					}
					catch(IOException e) {
						e.printStackTrace();
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		
	

	public static void main (String[] args ) {
		Client client =new Client();
		int RTSP_server_port =Integer.parseInt(args[1]);
		String Server = args[0];
		InetAddress ServerIPAddr = null;
		try {
		System.out.println(Server);	
		ServerIPAddr = InetAddress.getByName(Server);
		System.out.println(ServerIPAddr);
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
		}
		VideoFileName = args[2];
		
		try {
			client.RTSPSocket= new Socket(ServerIPAddr,RTSP_server_port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Set read and write buffers
		
		try {
			buffreader= new BufferedReader(new InputStreamReader(client.RTSPSocket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			buffwriter= new BufferedWriter(new OutputStreamWriter(client.RTSPSocket.getOutputStream()));
		}
		
		catch (IOException e) {
			e.printStackTrace();
		}
		state = INIT;
		client.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
	}


	
	
}



