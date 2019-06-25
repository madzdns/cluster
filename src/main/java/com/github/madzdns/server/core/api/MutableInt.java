package com.github.madzdns.server.core.api;

public class MutableInt
{
    int value;
    
    public MutableInt() {
        this.value = 0;
    }
    
    public MutableInt(final int start) {
        this.value = start;
    }
    
    public void inc() {
        ++this.value;
    }
    
    public int incGet() {
        return ++this.value;
    }
    
    public void dec() {
        --this.value;
    }
    
    public int decGet() {
        return --this.value;
    }
    
    public int get() {
        return this.value;
    }
}
