import java.io.Serializable;


/**
 * RequestVote rpc to gather votes from other servers in election
 * @author fc51033; fc51088; fc51101
 */
public class RequestVote implements Serializable {
	
	private static final long serialVersionUID = 5528573574594761698L;
	
	private int term;
	private int candidateId;
	private int lastLogIndex;
	private int lastLogTerm;
	
	/**
	 * RequestVote
	 * @param term - candidate term
	 * @param candidateId - candidate id
	 * @param lastLogIndex - last log index of the candidate
	 * @param lastLogterm - last log term of the candidate
	 */
	public RequestVote(int term, int candidateId, int lastLogIndex, int lastLogterm) {
		this.term = term;
		this.candidateId = candidateId;
		this.lastLogIndex = lastLogIndex;
		this.lastLogTerm = lastLogterm;
	}

	/**
	 * @return the term
	 */
	public int getTerm() {
		return term;
	}

	/**
	 * @return the candidateId
	 */
	public int getCandidateId() {
		return candidateId;
	}

	/**
	 * @return the lastLogIndex
	 */
	public int getLastLogIndex() {
		return lastLogIndex;
	}

	/**
	 * @return the lastLogTerm
	 */
	public int getLastLogTerm() {
		return lastLogTerm;
	}
}
