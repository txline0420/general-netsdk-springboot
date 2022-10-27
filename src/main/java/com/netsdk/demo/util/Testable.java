package com.netsdk.demo.util;

public interface Testable {
	void initTest();
	
	void runTest() throws InterruptedException;
	
	void endTest();
}
