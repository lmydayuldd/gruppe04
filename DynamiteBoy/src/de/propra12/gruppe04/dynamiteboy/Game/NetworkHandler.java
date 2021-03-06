package de.propra12.gruppe04.dynamiteboy.Game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JOptionPane;

/**
 * Handles networking by setting up a client or server. Player-position is
 * passed through a PlayerData Object. If a bomb is planted by a player a
 * PlayerData object is sent
 * 
 * @author Peter
 * 
 */
public class NetworkHandler {
	protected ObjectOutputStream out;
	protected ServerSocket serverSocket;
	protected Socket clientSocket;
	protected ObjectInputStream in;
	private String ip;
	protected Socket socket;
	protected BufferedReader reader;
	protected boolean isRunning = true;

	// Variables for Playerhandling
	protected int playerXPos;
	protected int playerYPos;
	protected boolean playerBomb;
	protected int playerBombcount;
	protected int type;

	/**
	 * Creates a client/server for a network game
	 * 
	 * @param ip
	 *            only relevant if a client is created
	 * @param type
	 *            SERVER = 1, CLIENT = 0
	 */
	public NetworkHandler(String ip, int type) {
		this.ip = ip;
		this.type = type;
		switch (type) {
		case C.SERVER:
			setUpServer();
			break;
		case C.CLIENT:
			setUpClient(ip);
			break;
		}
	}

	/**
	 * Set up a client and connect to passed ip and port 4242
	 * 
	 * @param ip
	 */
	private void setUpClient(String ip) {
		try {
			while (socket == null) {
				socket = new Socket(ip, 4242);
			}
			in = new ObjectInputStream(socket.getInputStream());
			out = new ObjectOutputStream(socket.getOutputStream());

			Thread readerThread = new Thread(new IncomingReader());
			readerThread.start();
		} catch (IOException ex) {
			// ex.printStackTrace();
			JOptionPane.showMessageDialog(C.frame, "Server nicht gefunden!");
		}

	} // close setUpClient

	/**
	 * Set up a Server. Clients can connect on port 4242
	 */
	private void setUpServer() {
		try {
			serverSocket = new ServerSocket(4242);
			// wait until a client connects
			while (clientSocket == null) {
				clientSocket = serverSocket.accept();
			}
			out = new ObjectOutputStream(clientSocket.getOutputStream());

			// handle client stuff
			socket = clientSocket;
			in = new ObjectInputStream(socket.getInputStream());

			Thread readerThread = new Thread(new IncomingReader());
			readerThread.start();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	} // close setUpServer

	// GETTERS AND SETTERS FOR NETWORKHANDLER

	/**
	 * 
	 * @return X position of the player from the other side of the network
	 * 
	 */
	public int getPlayerXPos() {
		return playerXPos;
	}

	/**
	 * Sets x position of the player from the other side of the network
	 * 
	 * @param playerXPos
	 */
	public void setPlayerXPos(int playerXPos) {
		this.playerXPos = playerXPos;
	}

	/**
	 * 
	 * @return Y position of the player from the other side of the network
	 * 
	 */
	public int getPlayerYPos() {
		return playerYPos;
	}

	/**
	 * Sets x position of the player from the other side of the network
	 * 
	 * @param playerYPos
	 */
	public void setPlayerYPos(int playerYPos) {
		this.playerYPos = playerYPos;
	}

	/**
	 * 
	 * @return true if a bomb has been planted by the other network-player
	 */
	public boolean isPlayerBomb() {
		return playerBomb;
	}

	/**
	 * Set state of playerBomb
	 * 
	 * @param playerBomb
	 *            true if a bomb should be planted. <br>
	 *            <i>(set to)</i> false if a bomb already has been planted
	 */
	public void setPlayerBomb(boolean playerBomb) {
		this.playerBomb = playerBomb;
	}

	/**
	 * 
	 * @return playerBombcount of the other network-player
	 */
	public int getPlayerBombcount() {

		return playerBombcount;
	}

	/**
	 * Sets the playerbombcount
	 * 
	 * @param count
	 *            number of bombs
	 */
	public void setPlayerBombcount(int count) {

		this.playerBombcount = count;
	}

	// end of getters and setters

	/**
	 * 
	 * Handles incoming Objects and sets variables of the other network-player
	 * 
	 * 
	 */
	public class IncomingReader implements Runnable {

		@Override
		public void run() {
			Object obj = null;
			while (isRunning && in != null && socket != null) {
				try {
					if (in != null) {
						obj = in.readObject();
					}
				} catch (IOException e) {
					netStop();
				} catch (ClassNotFoundException e) {
					netStop();
				}
				if (obj instanceof PlayerData) {
					PlayerData data = (PlayerData) obj;
					setPlayerXPos(data.getxPos());
					setPlayerYPos(data.getyPos());
					setPlayerBombcount(data.getBombcount());
				}
				if (obj instanceof BombData) {
					BombData data = (BombData) obj;
					setPlayerBomb(data.isBomb());
				}
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					netStop();
					if (type == C.SERVER) {
						System.out.print("Client disconnected");
					} else {
						System.out.print("Server disconnected");
					}
				}
			}

		}
	} // close IncomingReader

	// OBJECTS THAT CAN BE SENT AND METHODS TO SEND THEM

	/**
	 * Creates a serialized object with playerdata to send through the network
	 * 
	 */
	public static class PlayerData implements Serializable {
		private int xPos;
		private int yPos;
		private int bombcount;

		public PlayerData(int xPos, int yPos, int bombcount) {
			this.xPos = xPos;
			this.yPos = yPos;
			this.bombcount = bombcount;
		}

		public int getxPos() {
			return xPos;
		}

		public int getyPos() {
			return yPos;
		}

		public int getBombcount() {
			return bombcount;
		}

		public void setBombcount(int bombcount) {
			this.bombcount = bombcount;
		}

	} // close PlayerData

	/**
	 * Sends a PlayerData object created from values of passed player
	 * 
	 * @param player
	 *            Player object to send the parameters from
	 */
	public void sendPlayerdata(Player player) {
		PlayerData pd = new PlayerData(player.getxPos(), player.getyPos(),
				player.getBombCount());
		try {
			if (socket == null) {
				System.out.println("No server found");
				netStop();
			} else {
				out.writeObject(pd);
				out.flush();
			}
		} catch (IOException e) {
			System.out.println("Failed to send playerdata");
		}

	}

	/**
	 * Creates a serialized object with bombdata to send through the network
	 * 
	 */
	public static class BombData implements Serializable {
		private boolean bomb;

		public BombData(boolean bomb) {
			this.bomb = bomb;
		}

		public boolean isBomb() {
			return bomb;
		}
	}

	public void sendBombData(boolean bomb) {
		BombData bd = new BombData(bomb);
		try {
			out.writeObject(bd);
			out.flush();
		} catch (IOException e) {
			System.out.println("Failed to send bombdata");
		}
	}

	protected void netStop() {
		isRunning = false;
		try {
			if (socket != null) {
				socket.close();
			}
			if (in != null) {
				in.close();
			}
			if (out != null) {
				out.close();
			}
			System.out.println("Network closed");
		} catch (IOException e) {

		}
	}

}
