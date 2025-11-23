import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;
import org.json.JSONArray;

/**
 * SERVER
 * This is the ServerThread class that has a socket where we accept clients contacting us.
 * We save the clients ports connecting to the server into a List in this class. 
 * When we wand to send a message we send it to all the listening ports
 */

public class ServerThread extends Thread{
	private final ServerSocket serverSocket;
	private final Set<Socket> listeningSockets = new HashSet<Socket>();
    private final Set<InetSocketAddress> peerAddresses = new HashSet<>();
    private final int port;

    public ServerThread(String portNum) throws IOException {
        this.port = Integer.parseInt(portNum);
		serverSocket = new ServerSocket(this.port);
	}
    
	/**
	 * Starting the thread, we are waiting for clients wanting to talk to us, then save the socket in a list
	 */
	public void run() {
		try {
			while (true) {
				Socket sock = serverSocket.accept();
				listeningSockets.add(sock);

                // The Peer immediately sends it's host/port, read and add it to the set
                InetSocketAddress newPeerAddr = this.readConnectingPeerAddress(sock);
                if (!this.peerAddresses.contains(newPeerAddr) && newPeerAddr.getPort() != this.port) {
                    this.peerAddresses.add(newPeerAddr);

                    // Create socket connection and client thread to handle the new peer
                    Socket peerSocket = null;
                    try {
                        System.out.println("ServerThread: Connecting to new peer on port: " + newPeerAddr.getPort());
                        peerSocket = new Socket(newPeerAddr.getHostString(), newPeerAddr.getPort());
                        new ClientThread(peerSocket, this.port).start();
                    } catch (Exception e) {
                        if (peerSocket != null) {
                            peerSocket.close();
                        }
                        System.out.println("Failed to connect to peer at " + newPeerAddr.getHostString() + ":" + newPeerAddr.getPort());
                    }

                    // Automatically send the list of peer addresses to the newly connected peer
                    // so it can join those other peers
                    String peerAddresses = this.getPeerAddressesAsJson();
                    this.sendMessage(peerAddresses);
                }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
    public int getPort() {
        return this.port;
    }

    
	/**
	 * Sending the message to the OutputStream for each socket that we saved
	 */
	void sendMessage(String message) {
        if (!listeningSockets.isEmpty()) {
            System.out.println("ServerThread: Sending message: " + message);

            try {
                for (Socket s : listeningSockets) {
                    PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                    out.println(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("ServerThread: No listening peers to send message");
        }
	}

    /**
     * Converts the collection of listening sockets into a JSON formatted string
     *
     * @return JSON string containing array of socket information
     */
    public String getPeerAddressesAsJson() {
        JSONObject jsonObject = new JSONObject();
        JSONArray peerArray = new JSONArray();

        for (InetSocketAddress addr : this.peerAddresses) {
            JSONObject socketInfo = new JSONObject();
            socketInfo.put("host", addr.getHostName());
            socketInfo.put("port", addr.getPort());
            peerArray.put(socketInfo);
        }

        jsonObject.put("peerAddresses", peerArray);
        return jsonObject.toString();
    }

    public boolean isServerAddress(InetSocketAddress address) {
        return address.getPort() == this.port;
    }

    /**
     * Reads a JSON string from the given socket and parses it into a JSONObject
     * Expected format: {"host": "hostname", "port": portNumber}
     *
     * @param socket The socket to read from
     * @return JSONObject containing the parsed JSON data
     * @throws IOException If there's an error reading from the socket
     */
    public InetSocketAddress readConnectingPeerAddress(Socket socket) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String jsonString = reader.readLine();
        System.out.println("ServerThread: Received new peer address: " + jsonString);

        JSONObject addrJSON = new JSONObject(jsonString);
        return new InetSocketAddress(addrJSON.getString("host"), addrJSON.getInt("port"));
    }
}
