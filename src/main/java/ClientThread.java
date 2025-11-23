import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import org.json.*;

/**
 * Client 
 * This is the Client thread class, there is a client thread for each peer we are listening to.
 * We are constantly listening and if we get a message we print it. 
 */

public class ClientThread extends Thread {
	private final BufferedReader bufferedReader;
    private final Set<InetSocketAddress> peerAddressSet = new HashSet<>();
    private int peerServerPort;
	
	public ClientThread(Socket socket, int peerServerPort) throws IOException {
        this.peerServerPort = peerServerPort;
        this.sendPeerAddress(socket, peerServerPort);
		bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}

    /**
     * Sends this client's server port to the connected peer by writing it
     * as a JSON object to the specified socket's output stream.
     *
     * @param socket the socket connected to the peer, through which the port information will be sent
     * @throws IOException if an I/O error occurs while writing to the socket's output stream
     */
    private void sendPeerAddress(Socket socket, int peerPort) throws IOException {
        // Send this client's server port to the connected peer
        JSONObject portJson = new JSONObject();
        portJson.put("host", socket.getInetAddress().getHostName());
        portJson.put("port", peerPort);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        System.out.println("ClientThread: Sending address info: " + portJson.toString());
        out.println(portJson.toString());
    }

    /**
     * Retrieves a list of peer addresses from the input stream, parses them as JSON, and adds the parsed
     * addresses to a set of peer addresses.
     * <p>
     * The expected JSON format for the input is:
     * {
     * "peerAddresses": [
     * { "host": <host>, "port": <port> },
     * { "host": <host>, "port": <port> },
     * ...
     * ]
     * }
     * <p>
     * Each peer address object in the JSON array must contain:
     * - "host": A string representing the hostname or IP address of the peer.
     * - "port": An integer representing the port number of the peer.
     * <p>
     * Any peer address parsed from the input is output to the console in the format:
     * "Peer address - host: <host>, port: <port>", and subsequently added to the peer address set.
     *
     * @return Set<InetSocketAddress>
     * @throws IOException If an I/O error occurs while reading from the input stream.
     */
    protected Set<InetSocketAddress> buildPeerAddresses(String readLine) throws IOException {
        JSONObject json = new JSONObject(readLine);
        JSONArray array = json.getJSONArray("peerAddresses");
        this.peerAddressSet.clear();

        for (int idx = 0; idx < array.length(); idx++) {
            JSONObject addrObj = array.getJSONObject(idx);
            String host = addrObj.getString("host");
            int port = addrObj.getInt("port");
            peerAddressSet.add(new InetSocketAddress(host, port));
        }

        return peerAddressSet;
    }

    public Set<InetSocketAddress> getPeerAddresses() {
        return this.peerAddressSet;
    }

	public void run() {
		while (true) {
			try {
                String readLine = bufferedReader.readLine();
			    JSONObject json = new JSONObject(readLine);
                if (!json.has("peerAddresses")) {
                    System.out.println("[" + json.getString("username") + "]: " + json.getString("message"));
                } else {
                    System.out.println("ClientThread: Received peer addresses: " + readLine);
                   this.buildPeerAddresses(readLine);
                }
			} catch (Exception e) {
				interrupt();
				break;
			}
		}
	}

}
