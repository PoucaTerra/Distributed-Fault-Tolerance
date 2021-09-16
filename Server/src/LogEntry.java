import java.io.Serializable;

/**
 * Log Entry
 * @author fc51033; fc51088; fc51101
 */
public class LogEntry implements Serializable{

	private static final long serialVersionUID = -6439722046368880012L;

	private int term;
	private int index;
	private RaftRequest request;
	private String clientID;
	private int operationID;

	/**
	 * Constructor of the log entry
	 * @param term - term of the creator
	 * @param index - index of the entry in the log
	 * @param command - request from the client
	 * @param clientID - id of the client that sent the request
	 * @param opID - operation id
	 */
	public LogEntry(int term, int index, RaftRequest command, String clientID, int opID) {
		this.term = term;
		this.index = index;
		this.request = command;
		this.clientID = clientID;
		this.operationID = opID;
	}

	/*
	 * NO-OP
	 */
	public LogEntry(int term, int index) {
		this.term = term;
		this.index = index;
		this.operationID = -1;
	}

	/**
	 * @return the term
	 */
	public int getTerm() {
		return term;
	}

	/**
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * @return the command
	 */
	public RaftRequest getRequest() {
		return request;
	}

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

	///////////////////////////////////
	/////////// LOG UTILS///////////////
	//////////////////////////////////

	/**
	 * @return String representation
	 */
	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append(term);
		res.append(";" + index);
		res.append(";" + (clientID == null ? "&&&" : clientID));
		res.append(";" + operationID);
		res.append(";" + (request == null ? "&&&" : request.toString()));
		res.append("\n");
		return res.toString();
	}
	/**
	 * @param s
	 * @return
	 */
	public static LogEntry fromString(String s) {
		String[] splitted = s.split(";");
		int newTerm = Integer.parseInt(splitted[0]);
		int newIndex = Integer.parseInt(splitted[1]);
		int newOpID = Integer.parseInt(splitted[3]);
		String requestString = splitted[4];
		String cID = splitted[2];
		if(!requestString.equals("&&&") && !cID.equals("&&&")) {
			return new LogEntry(newTerm, newIndex, RaftRequest.fromString(requestString), cID, newOpID);
		}else {
			return new LogEntry(newTerm, newIndex);
		}
	}

	public boolean isNoOp() {
		return this.request == null;
	}

}
