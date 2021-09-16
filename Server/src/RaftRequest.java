import java.io.Serializable;

/**
 * Request to the raft server
 * 
 * @author fc51033; fc51088; fc51101
 */
public class RaftRequest implements Serializable {

	private static final long serialVersionUID = 6295354798957752386L;

	private String clientID;
	private int operationID;
	private RequestType type;
	private String key;
	private String newValue;
	private String oldValue;

	///////////////////////////////////
	/////////// CONSTRUCTORS///////////
	//////////////////////////////////
	/**
	 * Constructor for RequestType List
	 * 
	 * @param clientID - id of the client who created the request
	 * @param opid     - operation id
	 * @param type     - type of the request
	 */
	public RaftRequest(String clientID, int opid, RequestType type) {
		this.type = type;
		this.clientID = clientID;
		this.operationID = opid;
	}

	/**
	 * Constructor for RequestType Get & Del
	 * 
	 * @param clientID - id of the client who created the request
	 * @param opid     - operation id
	 * @param type     - type of the request
	 * @param key      - given key
	 */
	public RaftRequest(String clientID, int opid, RequestType type, String key) {
		this(clientID, opid, type);
		this.key = key;
	}

	/**
	 * Constructor for RequestType Put
	 * 
	 * @param clientID - id of the client who created the request
	 * @param opid     - operation id
	 * @param type     - type of the request
	 * @param key      - given key
	 * @param newValue - value to put in the store
	 */
	public RaftRequest(String clientID, int opid, RequestType type, String key, String newValue) {
		this(clientID, opid, type, key);
		this.newValue = newValue;
	}

	/**
	 * Constructor for RequestType Cas
	 * 
	 * @param clientID - id of the client who created the request
	 * @param opid     - operation id
	 * @param type     - type of the request
	 * @param key      - given key
	 * @param newValue - value to put in the store
	 * @param oldValue - value to substitute
	 */
	public RaftRequest(String clientID, int opid, RequestType type, String key, String newValue, String oldValue) {
		this(clientID, opid, type, key, newValue);
		this.oldValue = oldValue;
	}

	///////////////////////////////////
	/////////// FIELDS/////////////////
	//////////////////////////////////

	/**
	 * @return the clientID
	 */
	public String getClientID() {
		return clientID;
	}

	/**
	 * @return the operationID
	 */
	public int getOperationID() {
		return operationID;
	}

	/**
	 * Type of the request
	 * 
	 * @return type of the request
	 */
	public RequestType getType() {
		return type;
	}

	/**
	 * Key of the request
	 * 
	 * @return - key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * New Value
	 * 
	 * @return new value
	 */
	public String getNewValue() {
		return newValue;
	}

	/**
	 * Old Value
	 * 
	 * @return old value
	 */
	public Object getOldValue() {
		return oldValue;
	}

	public boolean isReadOnly() {
		return type == RequestType.GET || type == RequestType.LIST;
	}

	///////////////////////////////////
	/////////// LOG UTILS///////////////
	//////////////////////////////////

	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append(clientID);
		res.append(":" + operationID);
		res.append(":" + type.toString());
		if (key != null) {
			res.append(":" + key);
			if (newValue != null) {
				res.append(":" + newValue);
				if (oldValue != null) {
					res.append(":" + oldValue);
				}
			}
		}
		return res.toString();
	}

	public static RaftRequest fromString(String representation) {
		String[] splitted = representation.split(":");
		if (splitted.length == 6) {
			return new RaftRequest(splitted[0], Integer.parseInt(splitted[1]), RequestType.valueOf(splitted[2]),
					splitted[3], splitted[4], splitted[5]);
		}
		if (splitted.length == 5) {
			return new RaftRequest(splitted[0], Integer.parseInt(splitted[1]), RequestType.valueOf(splitted[2]),
					splitted[3], splitted[4]);

		}
		if (splitted.length == 4) {
			return new RaftRequest(splitted[0], Integer.parseInt(splitted[1]), RequestType.valueOf(splitted[2]),
					splitted[3]);
		}
		if (splitted.length == 3) {
			return new RaftRequest(splitted[0], Integer.parseInt(splitted[1]), RequestType.valueOf(splitted[2]));
		}
		return null;
	}
}
