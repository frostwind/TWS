package test;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.Date;

import com.sun.org.apache.bcel.internal.generic.NEW;

public class ExportPriceHistoryToCSV {
	public static void main(String[] args) {
		Calendar cal=Calendar.getInstance();
		Date end=cal.getTime();
		cal.add(Calendar.YEAR, -1);
//		cal.add(Calendar.DAY_OF_YEAR, -365);
		Date start=cal.getTime();
		Price[] prices=Util.getPriceFromDB("BZUN","SMART",start,end);
//		PrintStream out=null;
		BufferedWriter out=null;
//		String name="C:dev\\ta4j-origins-master\\ta4j-origins-master\\ta4j-examples\\src\\main\\resources\\amd_hist.txt";
		String name="C://dev//ta4j-origins-master//ta4j-origins-master//ta4j-examples//src//main//resources//amd_hist.txt";
		File file=new File(name);
		file.delete();
		try {
//			out = new PrintStream(new BufferedOutputStream(new FileOutputStream("C:\\dev\\ta4j-origins-master\\ta4j-origins-master\\ta4j-examples\\src\\main\\resources\\amd_hist.txt")));
//			out = new PrintStream(new FileOutputStream("C:\\dev\\ta4j-origins-master\\ta4j-origins-master\\ta4j-examples\\src\\main\\resources\\amd_hist.txt"));
			out=new BufferedWriter(new FileWriter(name));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		out.println("timestamp,price,amount");
		try {
			out.write("timestamp,price,amount");
			out.newLine();
			for (int i=0;i<prices.length;i++) {
//				out.println(prices[i].getTrade_date().getTime()/1000+","+prices[i].getClose()+","+new Double(prices[i].getVolumn()));
				out.write(prices[i].getTrade_date().getTime()/1000+","+prices[i].getClose()+","+new Double(prices[i].getVolumn()));
				out.newLine();
			}
//			out.flush();
			out.close();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
