package server.backend.kafka;

public enum KafkaTopics {

	RESOURCES("resources"),
	CLUSTER("cluster");
	
	private String value;
	
	private KafkaTopics(String value) {
		
		this.value = value;
	}
	
	public String getValue() {
		
		return this.value;
	}
}
