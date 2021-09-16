import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * State machine
 * 
 * @author fc51033; fc51088; fc51101
 */
public class StateMachine {

	private Map<String, String> store;

	private Map<String, Pair<Integer, RequestResponse>> latestProcessed;

	public StateMachine(int serverid) {
		this.latestProcessed = new HashMap<>();
		store = new HashMap<>();
	}

	public synchronized void processRequest(RaftRequest request) {
		// State machine keeps the last operation for every client
		// so it doesnt need to process on lost responses
		String clientID = request.getClientID();
		int opid = request.getOperationID(); 

		if (this.latestProcessed.get(clientID) != null
				&& this.latestProcessed.get(clientID).getKey() == opid) {
			return;
		}
		
		RequestResponse response = execute(request);

		if (response != null) {
			latestProcessed.put(clientID, new Pair<>(opid, response));
		}
	}

	protected RequestResponse execute(RaftRequest request) {

		RequestType type = request.getType();
		RequestResponse response;

		int opid = request.getOperationID();

		switch (type) {
			case CAS:
				String x = store.get(request.getKey());
				if (x.equals(request.getOldValue())) {
					store.put(request.getKey(), request.getNewValue());
				}
				response = new RequestResponse(opid, type, x);
				break;
			case DEL:
				String removed = store.remove(request.getKey());
				response = new RequestResponse(opid, type, removed);
				break;
			case GET:
				response = new RequestResponse(opid, type, store.get(request.getKey()));
				break;
			case LIST:
				Set<String> keys = store.keySet();
				HashSet<String> toSend = new HashSet<>(keys.size());
				for (String k : keys) {
					toSend.add(k);
				}
				response = new RequestResponse(opid, type, toSend);
				break;
			case PUT:
				store.put(request.getKey(), request.getNewValue());
				response = new RequestResponse(opid, type, request.getNewValue());
				break;
			default:
				return null;
		}
		
//		if(writeToLog) {
//			// write to the log file
//			try (FileWriter writer = new FileWriter(logfile, true)) {
//				writer.write(request.toString());
//				writer.flush();
//			} catch (IOException e) {
//				System.out.println("Erro a escrever no logfile");
//				return null;
//			}	
//		}

		return response;
	}

	/**
	 * Retrieves a already processed answer
	 * 
	 * @param clientID
	 * @param opID
	 * @return answer
	 */
	public RequestResponse retrieveResponse(String clientID, int opID) {

		if (this.latestProcessed.get(clientID) != null && this.latestProcessed.get(clientID).getKey() == opID) {
			return this.latestProcessed.get(clientID).getValue();
		}

		return null;
	}

	/**
	 * Key-Value store auxiliary class
	 * 
	 * @author fc51033; fc51088; fc51101
	 *
	 */
	private class Pair<K, V> {

		private K key;
		private V value;

		public Pair(K key, V value) {
			this.key = key;
			this.value = value;
		}

		/**
		 * @return the key
		 */
		public K getKey() {
			return key;
		}

		/**
		 * @return the value
		 */
		public V getValue() {
			return value;
		}

	}
}
