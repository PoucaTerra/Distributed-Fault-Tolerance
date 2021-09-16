import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * RAFT CLIENT
 * @author fc51033; fc51088; fc51101
 */
public class ClientRaft {

	private String clientID;
	private int operationID;

	private Socket socket;
	private ObjectInputStream in;
	private ObjectOutputStream out;

	private Map<Integer, String> servers;

	/**
	 * Constructor
	 */
	public ClientRaft() {
		servers = new HashMap<>();
		init();
		connectToServer();
	}

	/**
	 * Sends a string as a request to the raft server
	 * @param req - request
	 * @return server response
	 */
	public RequestResponse request(RaftRequest req) {

		RequestResponse response = null;
		while(true) {
			try {// Envia pedido para o servidor
				out.writeObject(req);
				out.flush();
				response =  (RequestResponse) in.readObject(); // TODO adicionar timeout
				return response;
			} catch (IOException e) {
				throwConnection();
				connectToServer();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Loads the available servers from the configuration file
	 * @throws IOException
	 */
	private void init() {

		// create and load default properties
		Properties props = new Properties();
		try {
			FileInputStream in = new FileInputStream("./config.properties");
			props.load(in);
			String[] splitted = props.getProperty("servers").split(";");
			for (int i = 0; i < splitted.length; i++) {
				String[] id_address = splitted[i].split("-");
				this.servers.put(Integer.parseInt(id_address[0]), id_address[1]);
			}
			in.close();
		} catch (IOException e) {
			System.out.println("Ficheiro de configuracao nao encontrado.");
			System.exit(-1);
		}
	}

	/**
	 * Connects to an available raft server If the server is no the leader then the
	 * connection is refused and the response is the leaders address:port If the
	 * server is the leader then a connection is made
	 */
	private void connectToServer() {

		try {
			Thread.currentThread().sleep(1500);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		this.clientID = "NOT_LEADER";

		while (true) {
			for (String s : servers.values()) {
				// Try to connect to the server
				try {
					connect(s); // if server is not online we catch IOException
					in = new ObjectInputStream(socket.getInputStream());
					out = new ObjectOutputStream(socket.getOutputStream());
					this.clientID = (String) in.readObject();
					while (this.clientID.equals("NOT_LEADER")) { // if connect wasn't the leader
						System.out.println("Este gajo nao eh lider!");
						String leader = (String) in.readObject();
						System.out.println("Recebi ip do lider!");

						throwConnection();
						connect(leader); // if isn't online exception
						in = new ObjectInputStream(socket.getInputStream());
						out = new ObjectOutputStream(socket.getOutputStream());
						this.clientID = (String) in.readObject(); // if not leader responce is "NOT LEADER" and repeat
					}
					System.out.println("Recebi o ID que foi gerado para mim!");
					return;
				} catch (IOException e) { // failed to connect
					continue;
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
			System.out.println("Nao existem servidores ligados.");
		}
	}

	/**
	 * Creates a socket to communicate with the leader raft server
	 * @param s - leader address:port
	 * @throws IOException
	 */
	private void connect(String s) throws IOException {

		try {
			socket = new Socket(s.split(":")[0], Integer.parseInt(s.split(":")[1]));
		} catch (NumberFormatException | UnknownHostException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Closes the artifacts of a previous connection
	 */
	public void throwConnection() {
		try {
			in.close();
		} catch (IOException e) {
		}
		try {
			out.close();
		} catch (IOException e) {
		}
		try {
			socket.close();
		} catch (IOException e) {
		}
	}

	/////////////////////////////////////
	//////////////OPERATIONS/////////////
	/////////////////////////////////////

	/**
	 * Request to put a entry in the storage
	 * @param key - key to put
	 * @param value - value to put
	 * @return true if sucess; false otherwise;
	 */
	public String put(String key, String value) {
		return request(new RaftRequest(this.clientID, this.operationID++, RequestType.PUT, key, value)).getResult();
	}

	/**
	 * Request to retrieve the value of a entry in the storage
	 * @param key - key to retrieve
	 * @return value associated with the given key
	 */
	public String get(String key) {
		return request(new RaftRequest(this.clientID, this.operationID++, RequestType.GET, key)).getResult();
	}

	/**
	 * Request to delete a entry in the storage
	 * @param key - key of the entry to delete
	 * @return true if success; false otherwise;
	 */
	public String del(String key) {
		return request(new RaftRequest(this.clientID, this.operationID++, RequestType.DEL, key)).getResult();
	}

	/**
	 * Request to list the keys in the storage
	 * @return keys in the storage
	 */
	public Set<String> list() {
		return request(new RaftRequest(this.clientID, this.operationID++, RequestType.LIST)).getKeys();
	}

	/**
	 * Updates a entry in the storage
	 * @param key - key of the entry to update
	 * @param vOld - old value
	 * @param vNew - new value
	 * @return true if success;false otherwise;
	 */
	public String cas(String key, String vOld, String vNew) {
		return request(new RaftRequest(this.clientID, this.operationID++, RequestType.CAS, key, vNew, vOld)).getResult();
	}
}
