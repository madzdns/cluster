package server.utils;

import java.util.List;

public class TripleDictinary<V1, V2, V3> extends Dictionary<V1, V2>{

	List<V3> value3;
	public TripleDictinary(V1 value1, V2 value2,List<V3> value3) {
		super(value1, value2);
		this.value3 = value3;
	}
	public void put(V1 value1, V2 value2, List<V3> value3) {
		super.put(value1, value2);
		this.value3 = value3;
	}
	public List<V3> getValue3()
	{
		return value3;
	}
}
