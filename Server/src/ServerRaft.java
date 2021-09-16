import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * RAFT SERVER
 * @author fc51033; fc51088; fc51101
 */
public class ServerRaft {

	// Persistent state
	private AtomicInteger currentTerm;
	private List<LogEntry> log;

	// Election
	private AtomicInteger votedFor;
	private AtomicInteger countVotes;

	private Semaphore voteCountSemaphore;

	// semaphore for applyToStateMachine thread
	private Semaphore updateCommitSemaphore;

	// Volatile state
	private int commitIndex;
	private int lastApplied;

	// volatile state leaders
	private Map<Integer, Integer> nextIndex;
	private Map<Integer, Integer> matchIndex;

	// state machine
	private StateMachine sMachine;

	// ports & sockets
	private int clientPort;
	private int serversPort;
	ServerSocket clientsSocket;
	ServerSocket serversSocket;

	// Servers List
	private Map<Integer, String> servers;

	// leader_id && server_id
	private AtomicInteger leaderID;
	private int serverID;

	// server state
	private ServerState state;

	// leader election
	static private final int MIN_ELECTION_TIMEOUT = 500;
	static private final int MAX_ELECTION_TIMEOUT = 1000;
	private Timer electionTimeout;
	private final Random gen = new Random();
	private int randomTimeout;

	// boolean to kill the server
	private boolean suicide;
	List<Thread> comunicationThreads;
	Thread acceptClientsT;
	Thread acceptServersT;
	Thread updateCommitAndApply;
	Thread countVotesThread;

	// For read-only operations
	private AtomicBoolean canReadOnly;
	private Semaphore readMajority;

	//Logs
	private File logfile;
	private final String SERVER_LOG_DIR = "client_log";

	public ServerRaft(int serverid) {

		// Persistent State initialization
		this.currentTerm = new AtomicInteger();
		this.votedFor = new AtomicInteger(-1);
		this.log = Collections.synchronizedList(new ArrayList<>());
		this.leaderID = new AtomicInteger(0);

		// volatile state initialization
		this.commitIndex = -1;
		this.lastApplied = -1;

		// volatile leader state
		this.nextIndex = new HashMap<>();
		this.matchIndex = new HashMap<>();

		// state machine
		this.sMachine = new StateMachine(serverid);

		// implementation specificic attributtes
		this.serverID = serverid;
		this.servers = new HashMap<>();
		this.state = ServerState.FOLLOWER;

		// Initializes ports
		comunicationThreads = Collections.synchronizedList(new ArrayList<>());
		loadConfig();
		String[] addressports = this.servers.get(this.serverID).split(":");
		this.clientPort = Integer.parseInt(addressports[1]);
		this.serversPort = Integer.parseInt(addressports[2]);

		updateCommitSemaphore = new Semaphore(0);
		electionTimeout = new Timer();
		countVotes = new AtomicInteger(0);
		voteCountSemaphore = new Semaphore(0);
		randomTimeout = gen.nextInt(MAX_ELECTION_TIMEOUT - MIN_ELECTION_TIMEOUT) + MIN_ELECTION_TIMEOUT;

		this.canReadOnly = new AtomicBoolean(false);
	}

	/**
	 * Loads the available servers from the configuration file
	 */
	private void loadConfig() {

		// CREATES THE LOG FILES
		Path path = Paths.get(SERVER_LOG_DIR);
		try {
			if (Files.notExists(path)) {
				Files.createDirectory(path);
			}
			this.logfile = new File(SERVER_LOG_DIR + "/" + this.serverID + ".log");
			if(!logfile.exists()) {
				logfile.createNewFile();
			}else {//here we have file
				restore(this.logfile);
			}
		} catch (IOException e) {
			System.err.println("Error creating files");
		}

		// create and load default properties
		Properties props = new Properties();
		try {

			FileInputStream in = new FileInputStream("./config.properties");
			props.load(in);
			String[] splitted = props.getProperty("servers").split(";");

			for (int i = 0; i < splitted.length; i++) {
				String[] id_address = splitted[i].split("-");
				int id = Integer.parseInt(id_address[0]);
				nextIndex.put(id, 0);
				this.servers.put(id, id_address[1]);
				if (id > this.serverID) {// A server only connects to the ones with higher id
					ServersThread serverCommsThread = new ServersThread(id);
					comunicationThreads.add(serverCommsThread);
					serverCommsThread.start();
				}
			}
			in.close();
		} catch (IOException e) {
			System.err.println("Config file not found!");
			System.exit(-1);
		}
	}

	/**
	 * Reads from the log file and restores the memory log
	 * @param file - logfile of this server
	 */
	private void restore(File file) {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			reader.lines().forEach(s -> {
				LogEntry recovered = LogEntry.fromString(s);
				log.add(recovered);
				if(!recovered.isNoOp()) {
					sMachine.execute(recovered.getRequest());
				}
			});		
			System.out.println("RESTORED LOG!");
		} catch (FileNotFoundException e) {
			System.out.println("There is no previous logs to catch up...");
		} catch (IOException e) {
			System.err.println("Error reading catch up file");
		}
	}

	/*
	 * 
	 */
	private synchronized void sendHeartbeats() {
		this.readMajority = new Semaphore(0);
		try {
			readMajority.acquire(servers.size() / 2);
		} catch (InterruptedException e) {
		}

	}


	/**
	 * Begin raft server protocols
	 */
	public void startServer() {

		try {
			clientsSocket = new ServerSocket(this.clientPort);// Opens the socket in the given port
			serversSocket = new ServerSocket(this.serversPort);

			/**
			 * Thread that accepts incoming client connections
			 */
			acceptClientsT = new Thread(() -> {
				while (!suicide) {
					try {
						Socket inSoc = clientsSocket.accept();
						ReceiveCommandsThread rct = new ReceiveCommandsThread(inSoc);
						if (leaderID.get() == serverID && rct.isConnected()) {
							comunicationThreads.add(rct);
							rct.start(); // starts thread
						}
					} catch (IOException e) {
						System.err.println("Error accepting a client.");
						continue;
					}
				}
			});

			/**
			 * Thread that accepts incoming server connections
			 */
			acceptServersT = new Thread(() -> {
				while (!suicide) {
					try {
						// Accepts the server
						Socket inSoc = serversSocket.accept();
						// Creates a thread to the accepted server
						ServersThread serverThread = new ServersThread(inSoc);
						comunicationThreads.add(serverThread);
						serverThread.start();
					} catch (IOException e) {
						System.err.println("Error accepting a server.");
						continue;
					}
				}
			});

			/**
			 * Thread that updates the commit index if it was replicated in half of the
			 * servers
			 */
			updateCommitAndApply = new Thread(() -> {
				int numServers = servers.size();
				while (!suicide) {
					try {
						updateCommitSemaphore.acquire();
					} catch (InterruptedException e) {
						break;
					}

					List<Integer> indexes = matchIndex.values().stream().filter(x -> x > commitIndex)
							.collect(Collectors.toList());
					Collections.sort(indexes);

					int majority = numServers / 2;

					int n = indexes.size() >= majority ? indexes.get(indexes.size() - majority) : -1;

					if (n != -1 && log.get(n).getTerm() == currentTerm.get()) {
						commitIndex = n;
						while (lastApplied < commitIndex) {
							lastApplied++;
							LogEntry toApply = log.get(lastApplied);
							if(toApply.isNoOp()) {
								if(toApply.getTerm() == currentTerm.get()) {
									canReadOnly.set(true);
									synchronized(canReadOnly) {
										canReadOnly.notifyAll();
									}
								}
								continue;
							}
							execute(toApply.getRequest());
							synchronized (toApply) {
								toApply.notifyAll();// desbloquear o cliente
							}
						}
					}
				}
			});

			/**
			 * Thread responsible for counting votes
			 */
			countVotesThread = new Thread(() -> {
				while (!suicide) {
					try {
						voteCountSemaphore.acquire();
					} catch (InterruptedException e) {
						break;
					}
					if (countVotes.get() > servers.size() / 2) {
						electionTimeout.cancel();
						System.out.println("Im leader in term " + currentTerm.get());
						synchronized (state) {
							// Add No-op to log when the term starts
							state = ServerState.LEADER;
							synchronized(leaderID) {
								leaderID.set(serverID);
								leaderID.notifyAll();
							}
							for (Integer sId : nextIndex.keySet()) {
								nextIndex.put(sId, log.size());
							}
							for (Integer sId : matchIndex.keySet()) {
								matchIndex.put(sId, -1);
							}
							LogEntry noop = new LogEntry(currentTerm.get(),log.size());
							log.add(noop);
							writeToLog(noop);
						}
						countVotes.set(0);
						synchronized (servers) {
							servers.notifyAll();
						}
						voteCountSemaphore.drainPermits();
					}
				}
			});

			// Inicia as threads
			acceptClientsT.start();
			acceptServersT.start();
			updateCommitAndApply.start();
			countVotesThread.start();

			resetTimer();

			try {
				acceptServersT.join();
				acceptClientsT.join();
				updateCommitAndApply.join();
				countVotesThread.join();

			} catch (InterruptedException e) {
				//e.printStackTrace();
			}
		} catch (IOException e) {
			System.err.println("Error opening sockets for incoming connections.");
			System.exit(-1);
		}
	}

	/**
	 * Closes the server. Called by Service on ShutdownHook.
	 */
	public void closeServer() {
		suicide = true;
		updateCommitAndApply.interrupt();
		countVotesThread.interrupt();
		try {
			clientsSocket.close();
			serversSocket.close();
		} catch (IOException e) {
			System.err.println("Error closing sockets for incoming connections.");
		}
		for (Thread t : comunicationThreads) {
			try {
				t.interrupt();
			} catch (ConcurrentModificationException e) {
				System.out.println("Error when interrupting the thread...");
			}
		}
	}

	/**
	 * Sends the request to the state machine
	 * @param r - request from the client
	 */
	public void execute(RaftRequest r) {
		this.sMachine.processRequest(r);
	}

	/**
	 * Writes entries to the logo file
	 * @param logEntry - entry to persist
	 */
	public void writeToLog(LogEntry logEntry) {
		try (FileWriter writer = new FileWriter(logfile, true)) {
			writer.write(logEntry.toString());
			writer.flush();
		} catch (IOException e) {
			System.err.println("Error writing in log");
		}			
	}

	public synchronized void resetTimer() {
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				state = ServerState.CANDIDATE;
				synchronized(leaderID) {
					leaderID.set(0);
				}
				countVotes.set(1);
				int t = currentTerm.incrementAndGet();
				System.out.println("Starting election in term: " + t);
				votedFor.set(serverID);
				randomTimeout = gen.nextInt(MAX_ELECTION_TIMEOUT - MIN_ELECTION_TIMEOUT) + MIN_ELECTION_TIMEOUT;
				synchronized (servers) {
					servers.notifyAll();
					resetTimer();
				}

			}
		};
		electionTimeout.cancel();
		electionTimeout = new Timer();
		electionTimeout.schedule(task, randomTimeout);
	}

	private class ReceiveCommandsThread extends Thread {

		private Socket socket;
		private ObjectOutputStream outStream = null;
		private ObjectInputStream inStream = null;
		private String id;

		private ReceiveCommandsThread(Socket inSoc) {
			socket = inSoc;
			// Opens communication channels with the user
			try {
				outStream = new ObjectOutputStream(socket.getOutputStream());
				inStream = new ObjectInputStream(socket.getInputStream());

				while(true) {

					// Enquanto nao sabemos quem eh o lider
					while(leaderID.get() == 0) {
						try {
							synchronized(leaderID) {
								leaderID.wait();
							}	
						} catch (InterruptedException e1) {
							if (!suicide) {
								close();
							}
							return;
						}
					}

					int leader = leaderID.get();
					if(leader != 0 && leader != serverID) {
						outStream.writeObject("NOT_LEADER");
						outStream.writeObject(servers.get(leader));
						close();
						break;
					}else if(leader == serverID){
						id = UUID.randomUUID().toString();
						outStream.writeObject(id);
						break;
					}
				}
			} catch (IOException e) {
				System.err.println("Error opening client communication sockets.");
				close();
				id = null;
			}
		}

		@Override
		public void run() {
			// To close the thread we generate a interruption
			// the thread only runs while no interruption was generated
			while (!suicide && state == ServerState.LEADER && id != null) {
				try {
					RaftRequest req = (RaftRequest) this.inStream.readObject();
					LogEntry logEntry = new LogEntry(currentTerm.get(), log.size(), req, req.getClientID(),
							req.getOperationID());
					log.add(logEntry);
					writeToLog(logEntry);
					synchronized (log) {
						log.notifyAll();
					}

					if(req.isReadOnly()) {
						if(!canReadOnly.get()) {
							try {
								synchronized(canReadOnly) {
									canReadOnly.wait();
								}
							} catch (InterruptedException e) {
							}
						}
						sendHeartbeats();
						sMachine.execute(req);
					}else {
						while (lastApplied < logEntry.getIndex()) {
							synchronized (logEntry) {
								try {
									logEntry.wait();
								} catch (InterruptedException e) {
									break;
								}
							}
						}
					}

					// Responde ao client
					RequestResponse resposta = sMachine.retrieveResponse(req.getClientID(), req.getOperationID());
					this.outStream.writeObject(resposta);
					this.outStream.flush();
				} catch (ClassNotFoundException e) {
					System.err.println("RaftRequest Class Not Found");
					break;
				} catch (IOException e) {
					id = null;
					break;
				}
			}
			comunicationThreads.remove(this);
			close();
		}

		public void close() {
			try {
				outStream.close();
				inStream.close();
				socket.close();
			} catch (IOException e) {
				System.err.println("Error closing client communication sockets.");
			}
		}

		/*
		 * Serve para ver se o cliente ainda esta ligado, 
		 * antes de dar start ah thread
		 */
		public boolean isConnected() {
			return id != null;
		}
	}

	private class ServersThread extends Thread {

		private Socket socket = null;
		private int id;
		private ServerConnection connection;
		private AppendEntriesResponse aer = null;
		private Thread sendRPC = null;
		private boolean kill;
		private Semaphore readSemaphore;

		private AtomicBoolean triedReconnect = new AtomicBoolean(false);

		/**
		 * Constructor for a thread to received connections
		 * 
		 * @param inSoc - accepted socket to communicate with other server
		 */
		private ServersThread(Socket inSoc) {
			this.socket = inSoc;
			// Opens communication channels with the follower
			try {
				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
				id = in.readInt();
				connection = new ServerConnection(socket, in, out, id);
			} catch (IOException e) {
				System.err.println("Error opening communication channels with the follower!");
				comunicationThreads.remove(this);
				connection.close();
			}
		}

		/**
		 * Constructor for threads to requested communications
		 * 
		 * @param id - id of requester
		 */
		private ServersThread(int id) {
			this.id = id;
		}

		@Override
		public void run() {

			if (connection == null) {
				String[] adr_port = servers.get(id).split(":");
				this.connection = new ServerConnection(adr_port[0], adr_port[2], serverID, this.id);
			}

			Thread receive = receive();
			receive.start();
			while (!suicide) {

				if (state == ServerState.FOLLOWER) {
					try {
						synchronized (servers) {
							servers.wait();
						}
					} catch (InterruptedException e) {
						break;
					}
				} else if (state == ServerState.CANDIDATE) {
					try {
						int lastLogTerm = log.size() == 0 ? 0 : log.get(log.size() - 1).getTerm();
						int lastLogIndex = log.size() == 0 ? 0 : log.size() - 1;
						connection.writeObject(new RequestVote(currentTerm.get(), id, lastLogIndex, lastLogTerm));
					} catch (IOException e) {
						if (serverID < this.id) {
							tryToReconnect();
						} else {
							break;
						}
					}
					try {
						synchronized (servers) {
							servers.wait();
						}
					} catch (InterruptedException e) {
						System.err.println("Thread interrupted while waiting for election result.");
						break;
					}
				} else {
					sendRPC = sendAppendEntriesRPC();
					sendRPC.start();
					try {
						sendRPC.join();
					} catch (InterruptedException e) {
						break;
					}
				}
			}
			comunicationThreads.remove(this);
			connection.close();
		}

		private Thread sendAppendEntriesRPC() {

			Thread result = new Thread(() -> {
				while (state == ServerState.LEADER && !suicide && !kill) {
					try {
						synchronized (log) {
							log.wait(50);
						}
					} catch (InterruptedException e1) {
						break;
					}
					int lastIndex = nextIndex.get(id);
					if (lastIndex < log.size()) { // there are new commands to send
						ArrayList<LogEntry> toSend = new ArrayList<>();
						for (int i = lastIndex; i < log.size(); i++) {
							toSend.add(log.get(i));
						}

						int prevIndex = lastIndex == 0 ? 0 : lastIndex - 1;
						AppendEntries append = new AppendEntries(currentTerm.get(), serverID, prevIndex,
								log.get(prevIndex).getTerm(), toSend, commitIndex);
						try {
							connection.writeObject(append);

							try {
								synchronized (sendRPC) {
									sendRPC.wait();
								}
							} catch (InterruptedException e) {
								System.err.println("Thread interrupted while waiting for AE response.");
								break;
							}
							AppendEntriesResponse aer = this.aer;

							if(outdatedTerm(aer.getTerm())) {
								continue;
							}

							if(readMajority != null && readSemaphore != readMajority) {
								readMajority.release();
								readSemaphore = readMajority;
							}

							if (aer.isSuccess()) {
								matchIndex.put(this.id, toSend.get(toSend.size() - 1).getIndex());
								nextIndex.put(this.id, lastIndex + toSend.size());
								updateCommitSemaphore.release();
							} else {
								nextIndex.put(this.id, lastIndex == 0 ? 0 : --lastIndex);
							}

						} catch (IOException e) {
							if (serverID < this.id) {
								tryToReconnect();
							} else {
								comunicationThreads.remove(this);
								kill = true;
								break;
							}
						}
					} else { // send heart beat
						AppendEntries append = new AppendEntries(currentTerm.get(),serverID,commitIndex);
						try {
							connection.writeObject(append);
							try {
								synchronized (sendRPC) {
									sendRPC.wait();
								}
							} catch (InterruptedException e) {
								System.err.println("Thread interrupted while waiting for AE response.");
								break;
							}

							AppendEntriesResponse aer = this.aer;
							if(!outdatedTerm(aer.getTerm()) && aer.isSuccess()) {
								if(readMajority != null && readSemaphore != readMajority) {
									readMajority.release();
									readSemaphore = readMajority;
								}
							}
						} catch (IOException e) {
							if (serverID < this.id) {
								tryToReconnect();
							} else {
								comunicationThreads.remove(this);
								kill = true;
								break;
							}
						}
					}
				}
			});
			return result;
		}

		/**
		 * Receives the append entry rpc
		 */
		private Thread receive() {

			Thread result = new Thread(() -> {
				while (!suicide && !kill) {
					try {
						Object received = connection.readObject();
						if (received == null) {
							continue;
						}
						if (received instanceof AppendEntries) {
							AppendEntries ae = (AppendEntries) received;
							processAppendEntries(ae);
						} else if (received instanceof AppendEntriesResponse) {
							AppendEntriesResponse aer = (AppendEntriesResponse) received;
							synchronized (sendRPC) {
								this.aer = aer;
								sendRPC.notifyAll();
							}
						} else if (received instanceof RequestVote) {
							RequestVote rv = (RequestVote) received;
							outdatedTerm(rv.getTerm());
							int lastLogTerm = log.isEmpty() ? 0 : log.get(log.size() - 1).getTerm();
							int term = currentTerm.get();

							if (term > rv.getTerm() || rv.getLastLogTerm() < lastLogTerm) {
								connection.writeObject(new RequestVoteResponse(term, false));
							} else {
								synchronized (votedFor) {
									if ((votedFor.get() == -1 || votedFor.get() == this.id)
											&& (rv.getLastLogTerm() > lastLogTerm
													|| rv.getLastLogIndex() >= log.size() - 1)) {
										connection.writeObject(new RequestVoteResponse(term, true));
										votedFor.set(this.id);
									} else {
										connection.writeObject(new RequestVoteResponse(term, false));
									}
								}
							}

						} else if (received instanceof RequestVoteResponse) {
							RequestVoteResponse rvr = (RequestVoteResponse) received;

							if (rvr.isVoteGranted() && rvr.getTerm() == currentTerm.get()
									&& state != ServerState.LEADER) {
								countVotes.incrementAndGet();
								voteCountSemaphore.release();
							}
							outdatedTerm(rvr.getTerm());
						}

					} catch (ClassNotFoundException | IOException e) {
						// Se for maior que o meu id sou eu que sou responsavel por reconectar
						if (serverID < this.id) {
							tryToReconnect();
						} else { // Se for menor basta terminar a thread.
							// O outro servidor eh que vai estabelecer uma nova ligacao quando iniciar
							comunicationThreads.remove(this);
							kill = true;
							break;
						}
					}
				}
			});
			return result;
		}

		private void processAppendEntries(AppendEntries ae) throws IOException {

			int term = currentTerm.get();
			if (ae.getTerm() < term) {
				connection.writeObject(new AppendEntriesResponse(term, false));
				return;
			}
			resetTimer();
			outdatedTerm(ae.getTerm());

			synchronized(leaderID) {
				leaderID.set(ae.getLeaderID());
				leaderID.notifyAll();
			}

			if (!ae.isHeartbeat()) {
				// Rule 1 & 2
				boolean ruleOneTwo;
				try {
					ruleOneTwo = !log.isEmpty() && log.get(ae.getPrevLogIndex()).getTerm() != ae.getPrevLogTerm();
				} catch (IndexOutOfBoundsException e) {
					ruleOneTwo = true;
				}
				if (ruleOneTwo) {
					connection.writeObject(new AppendEntriesResponse(term, false));
					return;
				} else {

					// Rule 3
					for (LogEntry l : ae.getEntry()) {
						if (log.size() > l.getIndex() && log.get(l.getIndex()).getTerm() == l.getTerm()) {
							log.subList(l.getIndex(), log.size()).clear();
							break;
						}
					}

					// Rule 4
					log.addAll(ae.getEntry());
					for(LogEntry l : ae.getEntry()) {
						writeToLog(l);
					}
					connection.writeObject(new AppendEntriesResponse(term, true));
				}
			} else {
				connection.writeObject(new AppendEntriesResponse(term, true));
			}

			// Rule 5
			if (ae.getLeaderCommitIndex() > commitIndex) {
				commitIndex = Math.min(ae.getLeaderCommitIndex(),log.size()-1);
				while (lastApplied < commitIndex) {
					lastApplied++;
					LogEntry toApply = log.get(lastApplied);
					if(!toApply.isNoOp()) {
						execute(toApply.getRequest());
					}
				}
			}
			resetTimer();
		}

		private synchronized void tryToReconnect() {
			if (triedReconnect.compareAndSet(false, true)) {
				connection.reconnect(socket);
				triedReconnect.set(false);
			} else {
				try { // sleep to save resources
					Thread.sleep(200);
				} catch (InterruptedException e1) {
					//e1.printStackTrace();
				}
			}
		}

		private synchronized boolean outdatedTerm(int term) {
			if(term > currentTerm.get()) {
				currentTerm.set(term);
				synchronized (state) {
					votedFor.set(-1);
					leaderID.set(0);
					state = ServerState.FOLLOWER;
					canReadOnly.set(false);
					System.out.println("Im follower, received term " + term);
					resetTimer();
					synchronized (servers) {
						servers.notifyAll();
					}
				}
				return true;
			}
			return false;
		}
	}
}
