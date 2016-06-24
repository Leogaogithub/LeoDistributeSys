package MyChannel;

import java.io.*;
import java.net.*;

import Leo.NodeInfo;
import Leo.SharedData;
import MyMessage.Message;
import MyMessage.MessageFactory;
import MyUtil.ConfigExpert;
import MyUtil.LogWriter;

import com.sun.nio.sctp.*;
public class SctpClientPart 
{
	public int MESSAGE_SIZE = 0;
	//Buffer to hold messages in byte format
	NodeInfo serverInfo;	
	//int localPort = 5002;
	
	String serverAddress = "";
	SctpChannel sctpChannel;
	
	public SctpClientPart(NodeInfo serInfo){
		MESSAGE_SIZE = ConfigExpert.getSingleton().MESSAGE_SIZE;
		serverInfo = serInfo;				
		serverAddress = serverInfo.nodeName+".utdallas.edu";
		//serverAddress = "127.0.0.1";//delete		
	}
	
	private void connect(){
		//Create a socket address for  server at net01 at port 5000
		SocketAddress socketAddress = new InetSocketAddress(serverAddress,serverInfo.port);
		//Open a channel. NOT SERVER CHANNEL
		boolean connected = false;
		while(!connected){
			try {
				sctpChannel = SctpChannel.open();
				//Bind the channel's socket to a local port. Again this is not a server bind
				//sctpChannel.bind(new InetSocketAddress(localPort));
				//Connect the channel's socket to  the remote server
				sctpChannel.connect(socketAddress);
				connected = true;
			} catch (IOException e) {			
				e.printStackTrace();
				System.out.println(serverInfo.nodeName + " is not online now, SctpClientPart will connect in 1000ms");
				LogWriter.getSingle().write(serverInfo.nodeName + " is not online now, SctpClientPart will connect in 1000ms\n");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {					
					e1.printStackTrace();
				}
			}
		}
	}
	
	public void connectChanel(){		
		connect();
		SharedData.getSingleton().updateIsConnected(serverInfo.nodeId);
		LogWriter.getSingle().log("connectChanel() in the SctpClientPart");
		InOutChannel ch = new InOutChannel(serverInfo.nodeId, sctpChannel);
		InOutChannelManager.getSingleton().addOutChannel(ch);
		new SctpRecieverThread(ch).start();
		int id = ConfigExpert.getSingleton().getLocalNodeId();
		Message msg = MessageFactory.getSingleton().getMessageNodeID(id);				
		send(msg.toString());
	}
	
	public void send(String message){
		InOutChannel.send(sctpChannel, message);
	}
	
	
	public void close(){
		try {
			sctpChannel.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}
	}
	
	public static void main(String args[])
	{
		String message = "hello world, this is from sctpclient part";
		String serverName= "127.0.0.1";
		int serverPort = 5006;
		NodeInfo serInfo = new NodeInfo(1,serverName,serverPort);
		SctpClientPart SctpClientObj = new SctpClientPart(serInfo);
		SctpClientObj.connectChanel();	
		int i = 0;
		boolean active = true;
		while(active){			
			message = String.valueOf(i);			
			SctpClientObj.send(message);
			i= i+1;
			System.out.println(i);			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {				
				e.printStackTrace();
				SctpClientObj.close();
				active = false;
			}
		}
		
	}
}
