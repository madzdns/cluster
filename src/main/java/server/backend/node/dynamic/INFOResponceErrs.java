package server.backend.node.dynamic;

public class INFOResponceErrs {

	public static final String DUPLICATE_INSTANCES = "Sounds more than one instances of node is running! Please check processes to kill other services";
	
	public static final String INTERVAL_VIOLATION = "The interval should be no less than ";
	
	public static final String CONCURENT_VIOLATION = "Cuncurrent nodes must be no more than ";
	
	public static final String WRONG_HANDSHAKE = "The key was wrong for entry ";
	
	public static final String CONFILIC_WITH_FIXED = "There was already a fixed node equals to this server's ip address in the database";
	
	public static final String WRONG_ACCESS_FOR_HOST_NODE = "Wrong access";
	
	public static final String WRONG_ACCESS_FOR_FOR_FIXED_ENTRY = "Wrong access";

}
