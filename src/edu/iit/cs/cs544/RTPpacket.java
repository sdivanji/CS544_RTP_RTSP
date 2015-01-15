package edu.iit.cs.cs544;

public class RTPpacket{

  static int HEADER_SIZE = 12;
  public int Version;
  public int Padding;
  public int Extension;
  public int CC;
  public int Marker;
  public int PayloadType;
  public int SequenceNumber;
  public int TimeStamp;
  public int Ssrc;
  
  //Bitstream of the RTP header
  public byte[] header;
  //size of the RTP payload
  public int RTP_payload_size;
  //Bitstream of the RTP payload
  public byte[] payload;

  //--------------------------
  //Constructor of an RTPpacket object from header fields and payload bitstream
  //--------------------------
  public RTPpacket(int PType, int Framenb, int Time, byte[] data, int data_length){
    //header fields:
    Version = 2;
    Padding = 0;
    Extension = 0;
    CC = 0;
    Marker = 0;
    Ssrc = 15;
    SequenceNumber = Framenb;
    TimeStamp = Time;
    PayloadType = PType;
    
    //header bistream:
    header = new byte[HEADER_SIZE];

    //Set the Version
    header[0] = (byte) (Version);
    System.out.println("Version is " +header[0]);
    //Set the PayloadType
    header[1] = (byte) (PayloadType);
    //Set the SequenceNumber
    header[2] = (byte) (SequenceNumber >> 8);
    header[3] = (byte) (SequenceNumber & 0xFF);
    //Set Timestamp;
    header[4] = (byte) (TimeStamp>>24);
    header[5] = (byte) (TimeStamp>>16);
    header[6] = (byte) (TimeStamp>>8);
    header[7] = (byte) (TimeStamp);
    System.out.println("TImestamp is "+TimeStamp);
    //Set the Ssrc
    header[8] = (byte) (Ssrc >> 24);
    header[9] = (byte) (Ssrc >>16);
    header[10] = (byte) (Ssrc  >> 8);
    header[11] = (byte) (Ssrc );
        
    //fill the payload bitstream:
    RTP_payload_size = data_length;
    payload = new byte[data_length];

    //fill payload array of byte from data (given in parameter of the constructor)
    for (int i=0; i<data_length; i++)
    {
        payload[i] = data[i];
    }
  }

  //Conctructor Client calls this constructor
  public RTPpacket(byte[] packet, int packet_size)
  {
    //fill default fields:
    Version = 2;
    Padding = 0;
    Extension = 0;
    CC = 0;
    Marker = 0;
    Ssrc = 0;

    //check if total packet size is lower than the header size
    if (packet_size >= HEADER_SIZE) 
      {
        //header bitsream:
        header = new byte[HEADER_SIZE];
        for (int i=0; i < HEADER_SIZE; i++)
          header[i] = packet[i];

        //payload bitstream:
        RTP_payload_size = packet_size - HEADER_SIZE;
        payload = new byte[RTP_payload_size];
        for (int i=HEADER_SIZE; i < packet_size; i++)
          payload[i-HEADER_SIZE] = packet[i];

        //interpret the changing fields of the header:
        PayloadType = header[1] & 127;
        SequenceNumber = unsigned_int(header[3]) + (unsigned_int(header[2])<<8);
        TimeStamp = unsigned_int(header[7])+(unsigned_int(header[6])<<8)+(unsigned_int(header[5])<<16)+(unsigned_int(header[4])<<24);
        System.out.println("TimeStamp is "+TimeStamp);
      }
 }

  public int getpayload(byte[] data) {

    for (int i=0; i < RTP_payload_size; i++)
      data[i] = payload[i];

    System.out.println("Payload size is" +RTP_payload_size);
    return(RTP_payload_size);
  }

  public int getpayload_length() {
    return(RTP_payload_size);
  }

  public int getlength() {
    return(RTP_payload_size + HEADER_SIZE);
  }

  public int getpacket(byte[] packet)
  {
    //construct the packet = header + payload
    for (int i=0; i < HEADER_SIZE; i++)
        packet[i] = header[i];
    for (int i=0; i < RTP_payload_size; i++)
        packet[i+HEADER_SIZE] = payload[i];

    //return total size of the packet
    return(RTP_payload_size + HEADER_SIZE);
  }

  public int gettimestamp() {
    return(TimeStamp);
  }

  public int getsequencenumber() {
    return(SequenceNumber);
  }

  public int getpayloadtype() {
    return(PayloadType);
  }

  public void printheader()
  { 
    for (int i=0; i < (HEADER_SIZE-4); i++)
      {
        for (int j = 7; j>=0 ; j--)
          if (((1<<j) & header[i] ) != 0)
            System.out.print("1");
        else
          System.out.print("0");
        System.out.print(" ");
      }

    System.out.println();
    
  }

  //return the value of 8-bit integer nb
  static int unsigned_int(int n) {
    if (n >= 0)
      return(n);
    else
      return(256+n);
  }

}
