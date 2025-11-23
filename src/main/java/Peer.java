import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

/**
 * This is the main class for the peer2peer program.
 * It starts a client with a username and port. Next the peer can decide who to listen to. 
 * So this peer2peer application is basically a subscriber model, we can "blurt" out to anyone who wants to listen and 
 * we can decide who to listen to. We cannot limit in here who can listen to us. So we talk publicly but listen to only the other peers
 * we are interested in. 
 * 
 */

public class Peer {
	private String username;
	private BufferedReader bufferedReader;
	private ServerThread serverThread;
    private String bootStrapPeerHost;
	private int bootStrapPeerPort = -1;
    Set<InetSocketAddress> connectedPeerAddresses = new HashSet<>();

	public Peer(BufferedReader bufReader, String username, ServerThread serverThread){
		this.username = username;
		this.bufferedReader = bufReader;
		this.serverThread = serverThread;
	}

    public Peer(BufferedReader bufReader, String username, ServerThread serverThread, String bootStrapPeerHost, int bootStrapPeerPort){
        this.username = username;
        this.bufferedReader = bufReader;
        this.serverThread = serverThread;
        this.bootStrapPeerHost = bootStrapPeerHost;
        this.bootStrapPeerPort = bootStrapPeerPort;
    }

	/**
	 * Main method saying hi and also starting the Server thread where other peers can subscribe to listen
	 *
	 * @param args username, args[1] port for server, args[2] optional bootstrap peer host:port
	 */
	public static void main (String[] args) throws Exception {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		String username = args[0];
		System.out.println("Hello " + username + " and welcome! Your port will be " + args[1]);

		// starting the Server Thread, which waits for other peers to want to connect
		ServerThread serverThread = new ServerThread(args[1]);
		serverThread.start();

        if (args.length == 2) {
            Peer peer = new Peer(bufferedReader, args[0], serverThread);
            peer.askForInput();
        } else if (args.length == 3) {
            String[] bsAddress = args[2].split(":");
            int bsPort = -1;
            String bsHost = "localhost";

            if (bsAddress.length == 1) {
                bsPort = Integer.parseInt(bsAddress[0]);
            } else {
                bsHost = bsAddress[0];
                bsPort = Integer.parseInt(bsAddress[1]);
            }

            Peer peer = new Peer(bufferedReader, args[0], serverThread, bsHost, bsPort);
            peer.connectToBootstrapPeer();
            peer.askForInput();
        }
	}

    /**
     * Attempts to establish a connection to a bootstrap peer using the configured
     * bootstrap peer host and port. If the connection is successful, retrieves peer
     * addresses from the bootstrap peer and attempts to connect to them.
     *
     * If the bootstrap peer port is set (-1), it terminates the program with an
     * appropriate message. If the connection fails, it closes the socket and prints
     * an error message before exiting the program.
     *
     * @throws IOException if an I/O error occurs while closing a socket during a
     *                     failed connection attempt.
     */
    public void connectToBootstrapPeer() throws IOException {
        System.out.println("Peer: Connecting to bootstrap peer");
        if (bootStrapPeerPort != -1) {
            Socket socket = null;
            try {
                socket = new Socket(bootStrapPeerHost, bootStrapPeerPort);
                ClientThread clientThread = new ClientThread(socket, serverThread.getPort());
                clientThread.start();
                Set<InetSocketAddress> peerAddressSet = clientThread.getPeerAddresses();
                while (peerAddressSet.isEmpty()) {
                    Thread.sleep(1000);
                    peerAddressSet = clientThread.getPeerAddresses();
                }
                this.connectToPeers(peerAddressSet);
            } catch (Exception c) {
                if (socket != null) {
                    socket.close();
                } else {
                    System.out.println("Cannot connect to bootstrap peer, wrong input");
                    System.exit(0);
                }
            }
        } else {
            System.out.println("Boot Strap Peer host:port not set");
            System.exit(0);
        }
    }

    /**
     * Establishes connections to a set of peer addresses and starts a client thread
     * for each successful connection. If a connection attempt fails, it handles
     * the exception and closes the socket.
     *
     * @param peerAddresses a set of InetSocketAddress objects representing the
     *                      addresses of peers to connect to
     * @throws IOException if an I/O error occurs while closing a socket during a
     *                     failed connection attempt
     */
    private void connectToPeers(Set<InetSocketAddress> peerAddresses) throws IOException {
        for (InetSocketAddress address : peerAddresses) {
            if (!this.serverThread.isServerAddress(address)) { // Avoid connecting to it's owm server

                if (!this.connectedPeerAddresses.contains(address)) {
                    Socket socket = null;
                    try {
                        System.out.println("Connecting to peer at " + address.getHostString() + ":" + address.getPort());
                        socket = new Socket(address.getHostString(), address.getPort());
                        new ClientThread(socket, this.serverThread.getPort()).start();
                        this.connectedPeerAddresses.add(address);
                    } catch (Exception e) {
                        if (socket != null) {
                            socket.close();
                        }
                        System.out.println("Failed to connect to peer at " + address.getHostString() + ":" + address.getPort());
                    }
                }
            } else {
                System.out.println("Peer: Skipped self connect");
            }
        }
    }
	
	/**
	 * Client waits for user to input their message or quit
	 *
	 * @param bufReader bufferedReader to listen for user entries
	 * @param username name of this peer
	 * @param serverThread server thread that is waiting for peers to sign up
	 */
	public void askForInput() throws Exception {
		try {
			System.out.println("> You can now start chatting (exit to exit)");
			while(true) {
				String message = bufferedReader.readLine();
				if (message.equals("exit")) {
					System.out.println("bye, see you next time");
					break;
				} else {
					// we are sending the message to our server thread. this one is then responsible for sending it to listening peers
					serverThread.sendMessage("{'username': '"+ username +"','message':'" + message + "'}");
				}	
			}
			System.exit(0);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
