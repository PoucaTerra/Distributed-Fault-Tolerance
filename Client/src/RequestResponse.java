import java.io.Serializable;
import java.util.Set;


/**
 * Response to a request from a client
 * @author fc51033; fc51088; fc51101
 */
public class RequestResponse implements Serializable{
    private static final long serialVersionUID = 6295354798957752386L;

    private int operationID;
    private RequestType type;
    private String gotResult;
    private Set<String> list;
    
	///////////////////////////////////
	///////////CONSTRUCTORS///////////
	//////////////////////////////////
    
    public RequestResponse(int opid, RequestType type) {
        this.operationID = opid;
        this.type = type;
    }
    
    /**
     * Response for the put/get/del/cas operations
     * @param opid - operation id
     * @param type - type of the request
     * @param result - put ==> new value stored, get ==> value of the given key, del ==> removed value, cas ==> old value
     */
    public RequestResponse(int opid, RequestType type, String result) {
        this(opid, type);
        this.gotResult = result;
    }
    
    /**
     * Response for the list operation
     * @param opid - operation id
     * @param type - type of the operation
     * @param keySet - keys in the store
     */
    public RequestResponse(int opid, RequestType type, Set<String> keySet) {
        this(opid, type);
        this.list = keySet;
    }
    
	///////////////////////////////////
	///////////FIELDS/////////////////
	//////////////////////////////////
    
    /**
	 * @return the operationID
	 */
	public int getOperationID() {
		return operationID;
	}
    
    /**
     * Result of the operation
     * @return null if unsuccessful
     */
    public String getResult() {
        return gotResult;
    }
    
    /**
     * Keys in the key/value store
     * @return keys
     */
    public Set<String> getKeys() {
        return list;
    }

	/**
	 * @return the type
	 */
	public RequestType getType() {
		return type;
	}
}
