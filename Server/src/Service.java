/**
 * Simple service code that runs a server
 * @author fc51033; fc51088; fc51101
 */
public class Service {

	public static void main(String[] args) {

		if (args.length != 1) {
			System.out.println("Exemplo de uso:\nServer <server_id>");
			System.exit(-1);
		}

		//Server id passed in args
		int serverid = Integer.parseInt(args[0]);
		ServerRaft server = new ServerRaft(serverid);//Creating server
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("\nA encerrar servidor...");
				server.closeServer();
			}
		});	
		server.startServer();//Starting server
	}
}
