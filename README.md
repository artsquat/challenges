# Peer-to-Peer Chat System

## Overview

This is a basic peer-to-peer chat application where all peers can communicate with each other. Each peer is **both a client and a server simultaneously**. This is the fundamental concept of peer-to-peer architecture.

## Architecture

### Key Concept: Dual Role

Each peer has two roles:
- **Server Role**: Accepts connections from other peers who want to listen to me
- **Client Role**: Connects to other peers to listen to their messages

### The Files

**Peer.java** - Main entry point
- Starts the ServerThread (so others can connect to me)
- Asks user which peers to listen to
- Creates ClientThread for each peer we want to listen to
- Handles user input (typing messages or "exit")
- Sends typed messages to ServerThread for broadcasting

**ServerThread.java** - The server component
- Runs in background, waiting for other peers to connect
- Maintains a list of all sockets (peers who are listening to me)
- When I type a message, broadcasts it to all listeners

**ClientThread.java** - The client component
- One thread created for EACH peer we listen to
- Constantly listens for messages from that peer
- Prints received messages to console

**build.gradle** - Gradle build configuration
- Currently has a `runPeer` task to start a peer
- Takes command-line arguments: name and port
- You will need to modify this to accept an optional third argument (bootstrap peer port)

### The Subscribe Model

Think of it like a newsletter subscription:
- **I connect to you** = I subscribe to your newsletter = I can hear your messages
- **You connect to me** = You subscribe to my newsletter = You can hear my messages

For bidirectional chat (we both hear each other), we need **BOTH connections**:
- I create a ClientThread connecting to your ServerThread
- You create a ClientThread connecting to my ServerThread

## How to Run (Current Manual Setup)

### Step 1: Start First Peer (Alice)

```bash
# Terminal 1
gradle runPeer --args "Alice 8000" --console=plain -q
```

Output:
```
Hello Alice and welcome! Your port will be 8000
> Who do you want to listen to? Enter host:port
```

**Alice has no one to listen to yet**, so just press Enter (or type nothing).

```
> You can now start chatting (exit to exit)
```

Alice can type messages, but no one will hear them yet.

### Step 2: Start Second Peer (Bob)

```bash
# Terminal 2
gradle runPeer --args "Bob 8001" --console=plain -q
```

Output:
```
Hello Bob and welcome! Your port will be 8001
> Who do you want to listen to? Enter host:port
```

Bob wants to hear Alice, so type:
```
localhost:8000
```

Now **Bob can hear Alice** (Bob → Alice connection exists).
But **Alice CANNOT hear Bob** yet (no Alice → Bob connection).

### Step 3: Make it Bidirectional

To make Alice hear Bob, you must **RESTART Alice**:

```bash
# Terminal 1 (restart Alice)
gradle runPeer --args "Alice 8000" --console=plain -q
> Who do you want to listen to? Enter host:port
localhost:8001
```

Now both can hear each other!

### Step 4: Adding a Third Peer (Charlie)

```bash
# Terminal 3
gradle runPeer --args "Charlie 8002" --console=plain -q
> Who do you want to listen to? Enter host:port
localhost:8000 localhost:8001
```

Charlie can now hear Alice and Bob.
But **Alice and Bob cannot hear Charlie** - they must both RESTART and add `localhost:8002`.

**This gets very tedious with more peers!**

## Message Flow Example

Once Alice, Bob, and Charlie are all connected to each other:

```
# Terminal 1 (Alice types):
> Hello everyone!

# Terminal 2 (Bob sees):
[Alice]: Hello everyone!

# Terminal 3 (Charlie sees):
[Alice]: Hello everyone!
```

## Threading Model

When Alice (port 8000) and Bob (port 8001) are fully connected:

**Alice's threads:**
- Main thread: handles user input (typing messages)
- ServerThread: listens on port 8000, accepts Bob's connection
- ClientThread: connected to Bob's port 8001, receives Bob's messages

**Bob's threads:**
- Main thread: handles user input
- ServerThread: listens on port 8001, accepts Alice's connection
- ClientThread: connected to Alice's port 8000, receives Alice's messages

## The Problem with This Design

**Issues:**
1. **New peers must know all existing peers** - Have to list them manually
2. **Existing peers must restart** - To hear new peers who join later
3. **Very tedious** - For 5 peers, each must list 4 other peers manually
4. **No dynamic joining** - Can't join a chat in progress
5. **Error-prone** - One wrong port number and it fails

**Your assignment**: Fix this by implementing automatic peer discovery and joining!

## Code Flow Walkthrough

### Starting a Peer

```java
// Peer.java main()
1. Read command line args (name, port)
2. Start ServerThread (so others can connect to me)
3. Create Peer object
4. Call updateListenToPeers() - asks user who to listen to
5. Call askForInput() - wait for user to type messages
```

### When User Types Message

```java
// Peer.java askForInput()
1. User types: "Hello"
2. Call serverThread.sendMessage("{'username':'Alice','message':'Hello'}")
3. ServerThread.sendMessage() loops through all connected sockets
4. Sends message to each socket
5. Each connected peer's ClientThread receives it and prints it
```

### When Another Peer Connects to Me

```java
// ServerThread.java run()
1. serverSocket.accept() - waits for connection
2. New peer connects, get their socket
3. Add socket to listeningSockets list
4. Loop back to accept() for next connection
```

### When I Connect to Another Peer

```java
// Peer.java updateListenToPeers()
1. User enters: "localhost:8000"
2. Create Socket connecting to localhost:8000
3. Create new ClientThread(socket)
4. ClientThread.run() constantly reads from socket
5. When message arrives, print it
```

## Testing Tips

1. **Start with 2 peers** - Get bidirectional chat working
2. **Print debug statements** - See when connections happen
3. **Test message flow** - Type in each terminal, verify others see it
4. **Try 3 peers** - Understand the complexity of manual setup
5. **Imagine 10 peers** - Realize how tedious this is!

## What NOT to Change (for now)

- The dual client/server architecture
- The threading model (ServerThread, ClientThread)
- The JSON message format
- The basic subscribe model

These are the foundation you'll build on for the assignment.

## Assignment Goal: Automatic Join

Your task is to implement automatic peer discovery and joining. The expected behavior:

### How it Should Work After Your Implementation

**Start first peer (no bootstrap needed):**
```bash
gradle runPeer --args "Alice 8000"
# Alice starts, no one to connect to yet
> You can now start chatting (exit to exit)
```

**Start second peer (joins through Alice):**
```bash
gradle runPeer --args "Bob 8001 8000"
# Bob connects to Alice (port 8000)
# Bob automatically learns about the network
# Alice automatically learns about Bob
# Both can now chat!
> You can now start chatting (exit to exit)
```

**Start third peer (joins through Bob OR Alice):**
```bash
gradle runPeer --args "Charlie 8002 8001"
# Charlie connects to Bob (port 8001)
# Charlie automatically learns about Alice AND Bob
# Bob and Alice automatically learn about Charlie
# All three can now chat!
> You can now start chatting (exit to exit)
```

### The Bootstrap Peer Concept

- **First peer**: Started without any arguments (or with no bootstrap peer)
- **All subsequent peers**: Provide ONE existing peer's port as third argument
- The bootstrap peer can be ANY peer in the network (not just the first one)
- Once connected through the bootstrap peer, the new peer learns about ALL other peers
- All existing peers learn about the new peer

### What You Need to Implement

1. **Modified command-line handling**: Accept optional third argument (bootstrap peer port)
2. **Automatic join protocol**: When given a bootstrap peer, automatically connect
3. **Peer list sharing**: Bootstrap peer sends its full peer list to new peer
4. **Recursive connection**: New peer connects to all peers in the list
5. **Join propagation**: All existing peers learn about and connect to new peer

No more manual "Who do you want to listen to?" prompt - it should all happen automatically!