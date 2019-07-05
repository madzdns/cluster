package com.github.madzdns.cluster.core.backend.node.dynamic;

import java.util.Arrays;
import java.util.List;

public class SystemInfo {
	
	public static final String CPUTYPE = "cpu";
	public static final String MEMTYPE = "mem";
	public static final String BWTYPE = "bw";
	
	public static final byte CPU = 1;
	public static final byte MEM = 2;
	public static final byte BW = 3;
	
	public static final byte ISCRITICAL = 1;
	public static final byte NOTCRITICAL = 0;
	
	public static List<String> TypeValues;
	public static List<Byte> NetTypes;
	
	static{
		TypeValues = Arrays.asList(CPUTYPE,MEMTYPE,BWTYPE);
		NetTypes = Arrays.asList(SystemInfo.BW);
	}
	public static boolean isNetType(Byte type)
	{
		return NetTypes.contains(type);
	}
	public static byte typeFromString(String type)
	{
		if(type.equalsIgnoreCase(CPUTYPE))
			return CPU;
		if(type.equalsIgnoreCase(MEMTYPE))
			return MEM;
		if(type.equalsIgnoreCase(BWTYPE))
			return BW;
		return 0;
	}
	public String getTypeAsString()
	{
		return type==CPU?CPUTYPE:type==MEM?MEMTYPE:type==BW?BWTYPE:"";
	}
	
	public byte type;
	public byte xthreshold;
	public byte nthreshold;
	public byte value;
	public byte critical = NOTCRITICAL;
	public SystemInfo()
	{}
	public SystemInfo(final byte type,final byte xthreshold,final byte nthreshold,final byte value,final boolean critic)
	{
		this.xthreshold=xthreshold;
		this.nthreshold=nthreshold;
		this.type = type;
		this.value = value;
		this.critical = critic?ISCRITICAL:NOTCRITICAL;
	}
	@Override
	public String toString() {
		
		return getTypeAsString()+","+(short)value+",["+(short)nthreshold+"-"+(short)xthreshold+"]"+","+(critical==ISCRITICAL?"critical":"normal");
	}
}
