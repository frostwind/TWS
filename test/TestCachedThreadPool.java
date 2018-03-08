package test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestCachedThreadPool {
	public static void main(String[] args) {
		ExecutorService executor = Executors.newFixedThreadPool(5);
		for (int i=0;i<100;i++) {
			executor.execute(new TestRunnable());
			System.out.println(i);
		}
	}

}

