import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;

/**
 * Client application that uses a raft client
 * @author fc51033; fc51088; fc51101
 */
public class Client {

	public static void main(String[] args) {

		if (args.length != 0) {
			System.out.println("Exemplo de uso:\nClient");
			System.exit(-1);
		}
		try {
			final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			final ClientRaft client = new ClientRaft();
			System.out.println("Client Raft created...");
			System.out.println("Commands available:\n"
					+"put <key> <value>\n"
					+"get <key>\n"
					+"del <key>\n"
					+"list\n"
					+"cas <key> <vold> <vnew>\n"
					+"exit\n");
			while (true) {// Loop a enviar pedidos para o Raft server
				String consoleInput = br.readLine();
				if(consoleInput == null) break;//when the client shutsdown
				
				String[] separated = consoleInput.split(" ");
				//////////////////////////
				////////OPERATIONS////////
				/////////////////////////
				switch(separated[0]) {
					case "put":
						if(separated.length != 3) {
							System.out.println("Usage: put <key> <value>");
							break;
						}
						String added = client.put(separated[1],separated[2]);
						if(added != null) {
							System.out.println("Successfully added the value " + added + " to the key-value store!");
						} else {
							System.out.println("Error in Put :(");
						}
						break;
					case "get":
						if(separated.length != 2) {
							System.out.println("Usage: get <key>");
							break;
						}
						String res = client.get(separated[1]);
						if(res != null) {
							System.out.println("Value of '" + separated[1] + "' : " + res);
						} else {
							System.out.println("No such key in storage!");
						}
						break;
					case "del":
						if(separated.length != 2) {
							System.out.println("Usage: del <key>");
							break;
						}
						String ret = client.del(separated[1]);
						if(ret == null){
							System.out.println("The given key does not exists in the key-value store!");
						} else {
							System.out.println("Successfully removed the value: " + ret);
						}
						break;
					case "list":
						if(separated.length != 1) {
							System.out.println("Usage: list");
							break;
						}
						Set<String> keys = client.list();
						if(keys.isEmpty()){
							System.out.println("The key-value store contains no keys!");
						} else {
							System.out.println("Keys in the key value store: ");
							int i = 0;
							for (String k : keys) {
								if(i == keys.size()-1) {
									System.out.println(k);
								}else {
									System.out.print(k + ", ");
								}
								i++;
							}
						}
						break;
					case "cas":
						if(separated.length != 4) {
							System.out.println("Usage: cas <key> <vold> <vnew>");
							break;
						}
						String x = client.cas(separated[1],separated[2],separated[3]);
						if(x.equals(separated[2])){
							System.out.println("The value in the key-value store was successfully updated!");
						} else {
							System.out.println("The key-value store remained unaltered!");
						}
						break;
					case "exit":
						System.exit(0);//Bye bye
					default:
						System.out.println("Invalid command");
				}
				System.out.println();
			}
			System.out.println("Closing the client!");

		} catch (IOException e) {
			System.out.println("Error reading from the console!!");
			System.exit(-1);
		}
	}
}

