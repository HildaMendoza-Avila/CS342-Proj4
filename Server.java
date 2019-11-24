import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.control.ListView;


public class Server{
	int count = 1;	
	ArrayList<ClientThread> players = new ArrayList<ClientThread>();
	ArrayList<Match> gameMatches = new ArrayList<Match>();
	ServerThread sever;
	private Consumer<Serializable> callback;
	GameInfo serverMessage;
	
	Server(Consumer<Serializable> call){
	
		callback = call;
		server = new ServerThread();
		serverMessage = new GameInfo();
		serverMessage.onlinePlayers.add(0);
		serverMessage.player = -1;
		server.start();
	}
	
	public class Match {
		int player1id, player2id;
		String player1play,player2play;
		String r = "rock", p = "paper", s = "scissors", l = "lizard", sp = "spock"; 
		int winnerID = -1;
		
		Match(int p1, String p1Play, int p2, String p2Play){
			player1id = p1;
			player2id = p2;
			player1play = p1Play;
			player2play = p2Play;
		}
		
		//returns a -1 if there is a tie and -2 if there is no valid play for player 1
		int getWinnerID() { //the logic of the game 
			switch(player1play) {
			case s: 
				switch(player2play) {
				case p: return player1id;
				case l: return player1id;
				case s: return -1;
				default: return player2id;
				}
			case p:
				switch(player2id) {
				case r: return player1id;
				case sp: return player1id;
				case p: return -1;
				default: return player2id;
				}
			case r:
				switch(player2id) {
				case l: return player1id;
				case s: return player1id;
				case r: return -1;
				default: return player2id;
				}	
			case l:
				switch(player2id) {
				case sp: return player1id;
				case p: return player1id;
				case l: return -1;
				default: return player2id;
				}	
			case sp:
				switch(player2id) {
				case s: return player1id;
				case r: return player1id;
				case sp: return -1;
				default: return player2id;
				}	
			default: return -2;
			}
		}
		
	}
	
	
	public class ServerThread extends Thread{
		int serverThreadID = 0;
		ServerThread oppReqHelper, gameMatchHelper, disconnectHelper, playerJoinHelper, playerListHelper;
		ObjectInputStream in;
		ObjectOutputStream out;
		
		public void run() {
			oppReqHelper = new ServerThread();
			oppReqHelper.start();
			
			gameMatchHelper = new ServerThread();
			gameMatchHelper.start();
			
			disconnectHelper = new ServerThread();
			disconnectHelper.start();
			
			playerJoinHelper = new ServerThread();
			playerJoinHelper.start();
			
			playerListHelper = new ServerThread();
			playerListHelper.start();
			
			int socketNum;
			if (Thread.currentThread().getName() == server.getName()) {
				socketNum = 5555;
			}
			else if (Thread.currentThread().getName() == oppReqHelper.getName()) {
				socketNum = 5355;
			}
			else if (Thread.currentThread().getName() == gameMatchHelper.getName()) {
				socketNum = 5535;
			}
			else if (Thread.currentThread().getName() == disconnectHelper.getName()) {
				socketNum = 5335;
			}
			else if (Thread.currentThread().getName() == playerJoinHelper.getName()) {
				socketNum = 3339;
			}
			else if (Thread.currentThread().getName() == playerListHelper.getName()) {
				socketNum = 3335;
			}
			else {
				socketNum = 0;
			}
			try(ServerSocket mysocket = new ServerSocket(socketNum);){
			    
			    try {
					in = new ObjectInputStream(mysocket.getInputStream());
					out = new ObjectOutputStream(mysocket.getOutputStream());
					connection.setTcpNoDelay(true);	
				}
				catch(Exception e) {
					System.out.println("Streams not open");
				}
			    if (Thread.currentThread().getName() == server.getName()) {
			    	System.out.println("Server is waiting for players to join!");
				    while(true) {
					    try {
					    		GameInfo playerMsg = (GameInfo)in.readObject();
						    	int messageType = playerMsg.typeOfMessage;
						    	if(messageType < 0) { //-# == the index of the client that wants to disconnect multiplied by -1 		
						    		disconnectHelper.out.writeObject(playerMsg);
						    	}
						    	else {
						    		switch(messageType) {
						    		case 1: oppReqHelper.out.writeObject(playerMsg); //1 -  Opponent has been chosen
						    			break;
						    		case 3: gameMatchHelper.out.writeObject(playerMsg); //3 - Player has played
						    			break;
						    		case 5: playerListHelper.out.writeObject(playerMsg); //5 - Update the ArrayList
					    				break;
						    		}//end of switch
						    	}//end of else
					    	}//end of try
					    }//end of while
		    	}//end of if stmt for main thread
		    	else if (Thread.currentThread().getName() == oppReqHelper.getName())) {
		    		ManageOpponentRequests(mysocket);
		    	}
		    	else if (Thread.currentThread().getName() == gameMatchHelper.getName())) {
		    		ManageMatches(mysocket);
		    	}
		    	else if (Thread.currentThread().getName() == disconnectHelper.getName())) {
		    		ManagePlayerDisconnections(mysocket);
		    	}
		    	else if (Thread.currentThread().getName() == playerListHelper.getName())) {
		    		UpdatePlayerList(mysocket);
		    	}
		    	else if (Thread.currentThread().getName() == playerJoinHelper.getName())) {
		    		AddPlayersToServer(mysocket);
		    	}
			}//end of try (connection try)
			catch(Exception e) {
				 callback.accept("OOOOPPs...Server closing down!");
				  break;
			}
		}//end of run
		
		public void ManagePlayerDisconnections(ServerSocket mysocket) {
			try {
				in = new ObjectInputStream(mysocket.getInputStream());
				out = new ObjectOutputStream(mysocket.getOutputStream());
				connection.setTcpNoDelay(true);	
			}
			catch(Exception e) {
				System.out.println("Streams not open");
			}
			
			while(true) {
				try {
			    	GameInfo newGameInfo = (GameInfo) in.readObject();
			    	//lock the serverMessage
			    	
			    	//mark the playerStatus as offline 
			    	serverMessage.onlinePlayers.set(newGameInfo.typeOfMessage * (-1), 0);
			    	
			    	//let the other player know if in curr match (typeOfMessage = 6 Opponent Disconnected)
			    	ClientThread otherPlayer;
			    	Match thisMatch;
			    	for(int i = gameMatches.size() -1; i >= 0; i--) {
			    		thisMatch = gameMatches.get(i);
			    		if (thisMatch.player1id == newGameInfo.player && thisMatch.winnerID == -1) {
			    			otherPlayer = players.get(thisMatch.player2id);
			    			serverMessage.typeOfMessage = 6; //(typeOfMessage = 6 Opponent Disconnected)
			    			otherPlayer.out.writeObject(serverMessage);
			    			serverMessage.typeOfMessage = 7; //no message in particular
			    			break;
			    		}
			    		else if (thisMatch.player2id == newGameInfo.player && thisMatch.winnerID == -1) {
			    			otherPlayer = players.get(thisMatch.player1id);
			    			serverMessage.typeOfMessage = 6; //(typeOfMessage = 6 Opponent Disconnected)
			    			otherPlayer.out.writeObject(serverMessage);
			    			serverMessage.typeOfMessage = 7; //no message in particular
			    			break;
			    		}
					}
			    }
			    catch(Exception e) {}
			}//end of while
		}//end of ManagePlayerDisconnections
		
		public void ManageOpponentRequests(ServerSocket mysocket) {
			try {
				in = new ObjectInputStream(mysocket.getInputStream());
				out = new ObjectOutputStream(mysocket.getOutputStream());
				connection.setTcpNoDelay(true);	
			}
			catch(Exception e) {
				System.out.println("Streams not open");
			}
			
			while(true) {
			    try {
			    	GameInfo newGameInfo = (GameInfo) in.readObject();
			    	//lock the serverMessage for this whole if else statement
			    	if (serverMessage.onlinePlayers.get(newGameInfo.opp_id) == 1) {
			    		//we mark both the player and the opponent status in serverMessage.onlinePlayers = 2
			    		serverMessage.onlinePlayers.get(newGameInfo.player) = 2;
			    		serverMessage.onlinePlayers.get(newGameInfo.opp_id) = 2;
			    		
			    		//we ask the opponent and the player for their play with a message with typeOfMessage = 1 because Opponent has been chosen
			    		serverMessage.typeOfMessage = 0; //0 Being challenged
			    		players.get(newGameInfo.opp_id).out.writeObject(serverMessage);
			    		players.get(newGameInfo.player).out.writeObject(serverMessage);
			    		serverMessage.typeOfMessage = 7; //no message in particular
			    		
			    		//we create a Match instance and update its fields and add it to gameMatches arraylist
			    		Match match = new Match(newGameInfo.player, "",  newGameInfo.opp_id, "");
			    		gameMatches.add(match);
			    	}
			    	else { //the chosen opponent is not available
			    		serverMessage.typeOfMessage = 6; //6 Opponent Unavailable
			    		players.get(newGameInfo.player).out.writeObject(serverMessage);
			    		serverMessage.typeOfMessage = 7; //no message in particular
			    	}
			    }
			    catch(Exception e) {}
			}//end of while
		}//end of ManageOpponentRequests
		
		public void ManageMatches(ServerSocket mysocket) {  //here is where the player and the opponent will send their plays
			try {
				in = new ObjectInputStream(mysocket.getInputStream());
				out = new ObjectOutputStream(mysocket.getOutputStream());
				mysocket.setTcpNoDelay(true);	
			}
			catch(Exception e) {
				System.out.println("Streams not open");
			}
			
			while(true) {
			    try {
			    	GameInfo newGameInfo = (GameInfo) in.readObject();
			    	Match thisMatch;
			    	//look for the match the newGame.player is part of (start from the end of the arraylist) and we update their play
			    	for(int i = gameMatches.size() -1; i >= 0; i--) {
			    		if (gameMatches.get(i).player1id == newGameInfo.player) {
			    			thisMatch = gameMatches.get(i);
			    			thisMatch.player1play = newGameInfo.player_plays;
			    			break;
			    		}
			    		else if (gameMatches.get(i).player2id == newGameInfo.player) {
			    			thisMatch = gameMatches.get(i);
			    			thisMatch.player2play = newGameInfo.player_plays;
			    			break;
			    		}
					}
			    	//lock the serverMessage
			    	
			    	//if the plays of both players are in the match then we call getWinnerID and update the players on who won
			    	if (thisMatch.player1play != "" and thisMatch.player2play != "") {
			    		//we update both players on who won with the result of calling match.getWinnerID()
			    		serverMessage.typeOfMessage = 4; //Sever has the result of the match
			    		if (thisMatch.winnerID == thisMatch.player1id) {
			    			serverMessage.winner = "Self";
			    			players.get(thisMatch.player1id).out.writeObject(serverMessage);
			    			serverMessage.winner = "Opp";
			    			players.get(thisMatch.player2id).out.writeObject(serverMessage);
			    		}
			    		else {
			    			serverMessage.winner = "Opp";
			    			players.get(thisMatch.player1id).out.writeObject(serverMessage);
			    			serverMessage.winner = "Self";
			    			players.get(thisMatch.player2id).out.writeObject(serverMessage);
			    		}
			    		serverMessage.winner = "";
			    		serverMessage.typeOfMessage = 7; //no message in particular
			    		//we mark both the player and the opponent status in serverMessage.onlinePlayers = 1
			    		serverMessage.onlinePlayers.set(thisMatch.player1id, 1);
			    		serverMessage.onlinePlayers.set(thisMatch.player2id, 1);
			    	}
			    }
			    catch(Exception e) {}
			}
		}
		
		public void UpdatePlayerList(ServerSocket mysocket) {
			try {
				in = new ObjectInputStream(mysocket.getInputStream());
				out = new ObjectOutputStream(mysocket.getOutputStream());
				mysocket.setTcpNoDelay(true);	
			}
			catch(Exception e) {
				System.out.println("Streams not open");
			}
			
			while(true) {
			    try {
			    	GameInfo newGameInfo = (GameInfo) in.readObject();
			    	
			    	for(int i = 0; i < players.size(); i++) {
						ClientThread t = players.get(i);
						try {
						 t.out.writeObject(serverMessage);        //figure out a way to lock serverMessage
						}
						catch(Exception e) {}
					}
			    }
			    catch(Exception e) {}
			}
		}
		
		public void AddPlayersToServer(ServerSocket mysocket) {
		    while(true) { 
				ClientThread c = new ClientThread(mysocket.accept());
				players.add(c);													         	
				serverMessage.typeOfMessage = 5;
				serverMessage.onlinePlayers.add(1); 
				server.out.WriteObject(serverMessage); //update the connected players that a new player has joined
				c.gi.playerID = count;
				c.start();
				count++;
		    }
		}//end of AddPlayersToServer
	
	} //end of ServerThread class
	
		class ClientThread extends Thread{
			
			
		}//end of client thread
}


	
	

	
