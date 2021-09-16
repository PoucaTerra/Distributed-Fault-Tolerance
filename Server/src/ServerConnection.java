import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Connection between servers
 * @author fc51033; fc51088; fc51101
 */
public class ServerConnection {

	private int id;
	private int serverID;
	private Socket socket;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	
	/**
	 * Creates a new server connection
	 * @param socket - socket of the connection
	 * @param in - input stream
	 * @param out - output stream
	 * @param id - id of the server in the other end
	 */
	public ServerConnection(Socket socket, ObjectInputStream in, ObjectOutputStream out, int id) {
		this.id = id;
		this.socket = socket;
		this.in = in;
		this.out = out;
	}
	
	/**
	 * Creates a connection to a given address
	 * @param address - address of the other endpoint
	 * @param port - port of the endpoint
	 * @param serverID - id of the creator
	 * @param id - id of the other endpoint
	 */
	public ServerConnection(String address, String port, int serverID, int id) {
		this.id = id;
		this.serverID = serverID;
		while (true) {
			try {
				this.socket = new Socket(address, Integer.parseInt(port));
				this.in = new ObjectInputStream(socket.getInputStream());
				this.out = new ObjectOutputStream(socket.getOutputStream());
				this.writeInt(this.serverID);

			} catch (NumberFormatException | UnknownHostException e) {
				// should not happen
				e.printStackTrace();
			} catch (IOException e) {
				// target server n deve tar ligado ainda
				try {
					Thread.sleep(200);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				continue;
			}
			break;
		}
	}

	/**
	 * Socket of the connection
	 * @return socket
	 */
	public Socket getSocket() {
		return socket;
	}
	
	/**
	 * Sends an object to the other end of this connection
	 * @param o - object to send
	 * @throws IOException
	 */
	public void writeObject(Object o) throws IOException {
		synchronized (out) {
			out.writeObject(o);
			out.flush();
		}
	}
	
	/**
	 * Sends an integer to the other end of this connection
	 * @param i - int to send
	 * @throws IOException
	 */
	public void writeInt(int i) throws IOException {
		synchronized (out) {
			out.writeInt(i);
			out.flush();
		}
	}
	
	/**
	 * Reads an object sent from the other end
	 * @return received object
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public Object readObject() throws IOException, ClassNotFoundException {

		synchronized (in) {
			return in.readObject();
		}

	}
	
	/**
	 * Reads an integer sent from the other end
	 * @return received int
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public int readInt() throws IOException {
		synchronized (in) {
			return in.readInt();
		}
	}

	/**
	 * Reconecta-se a um servidor
	 * @param sc - socket of the connection that we want to restart
	 */
	public void reconnect(Socket sc) {

			System.out.println("A reconectar ao servidor: " + this.id);
			while (true) {
				try {
					this.socket = new Socket(this.socket.getInetAddress(), this.socket.getPort());
					this.in = new ObjectInputStream(socket.getInputStream());
					this.out = new ObjectOutputStream(socket.getOutputStream());
					this.writeInt(this.serverID);
				} catch (NumberFormatException | UnknownHostException e) {
					// should not happen
					e.printStackTrace();
				} catch (IOException e) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						System.out.println("Interrompida!");
						e1.printStackTrace();
					}
					continue;
				}
				break;
			}
			System.out.println("Reconnected to " + id);
	}
	
	/**
	 * Closes the artifacts of this connection
	 */
	public void close() {
		try {
			this.in.close();
			this.out.close();
			this.socket.close();
		} catch (IOException e) {
//			System.err.println("Erro ao tentar terminar comunicacao com um servidor."
//					+ "\n Provavalmente ja nao estava conectado.");
		}
	}
}
