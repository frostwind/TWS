package test;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ib.client.Contract;

import test.DayShort.BUY_STRATEGY;

public class DayShort {
	/*
	public static String[] hk_symbols=new String[] {"5","11","23","388","939","1299","1398","2318",
			"2388","2628","3328","3988","2","3","6","836","1038",
			"4","12","16","17","83","101","688","823","1109",
			"1113","1997","2007",
			"1","19","27","66","144","151","175","267","288",
			"386","700","762","857","883","941","992","1044","1088",
			"1928","2018","2319","2382"};
			*/
	public static String[] hk_symbols=new String[] {"2318","700"};
	public static String[] nasdaq_symbols=new String[] {"BZUN","NVDA","SQ","MU","VERI"};
	
	public static String[] us_symbols=new String[] {"ON","TWTR"};
	public static enum BUY_STRATEGY{
		BUY_ON_GREEN,
		BUY_ON_SHORTER_RED,
		BUY_ON_LONGER_GREEN
	};
	public static enum SELL_STRATEGY{
		SELL_ON_SHORTER_GREEN,
		SELL_ON_RED,
		SELL_ON_LONGER_RED
	};
	public static final String HK_EXCHANGE="SEHK";
	public static final String US_EXCHANGE="SMART";
//	public static final String NYSE="NYSE";
	public static void main(String[] args) {
		PrintStream out=null;
		try {
			out = new PrintStream(new BufferedOutputStream(new FileOutputStream("c://dev//strategy.txt")));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.setOut(out);
//		String[] symbols=new String[] {"AMD","NVDA","TSLA","GOOG","BZUN"};
//		String[] hk_symbols=new String[] {"2318","700","388","1398"};
		List<String[]> hk_symbols=getAllSymbolsFromDB();
		SimpleDateFormat form = new SimpleDateFormat("yyyy-MM-dd");
		cleanGainResultFromDB();
		for (String[] symbol:hk_symbols) {
			Calendar cal=Calendar.getInstance();
			for (int i=0;i<1;i++) {
				
				String s=" 到 "+form.format(cal.getTime());
				Date end=cal.getTime();
				cal.add(Calendar.YEAR, -10);
				Date start=cal.getTime();
				s="从 "+form.format(cal.getTime())+s;
				System.out.println("======================"+s+"======================");
				analyzeStock(symbol[0],symbol[1],true,start,end);
			}
			
		}
		
	}
	public static void analyzeStock(String symbol,String exchange,boolean list_detail,Date start,Date end) {
//		Double[] prices=getPriceFromFile(symbol);
		Price[] prices=getPriceFromDB(symbol,exchange,start,end);
		boolean vol=false;
		if (prices!=null && prices.length>0) {
			vol= prices[0].getVolumn()>0;
		}
		Double[] ema12=getEMA(prices,12);
		Double[] ema26=getEMA(prices,26);
		Double[] ema5=getEMA(prices,5);
		Double[] ema120=getEMA(prices,120);
		Double[] ema60=getEMA(prices,60);
		Double[] ema20=getEMA(prices,20);
		Double[] ema30=getEMA(prices,30);
		Double[] vol_ema20=null;
		if (vol) vol_ema20=getEMAVol(prices,20);
		Double[] dif=new Double[prices.length];
		Double[] macd=new Double[prices.length];
		for (int i=0;i<dif.length;i++) {
			dif[i]=ema12[i]-ema26[i];
		}
		Double[] dea=getEMA(dif,9);
		for (int i=0;i<macd.length;i++) {
			macd[i]=(dif[i]-dea[i])*2;
		}
//		Util.print(ema5);
//		Util.print(getEMA(prices,10));
//		Util.print(ema12);
//		Util.print(ema26);
//		Util.print(dif);
//		Util.print(dea);
//		Util.print(macd);
		BUY_STRATEGY[] buy=new BUY_STRATEGY[BUY_STRATEGY.values().length];
		SELL_STRATEGY[] sell=new SELL_STRATEGY[SELL_STRATEGY.values().length];
		
		List<List<BUY_STRATEGY>> res_buy = new ArrayList<List<BUY_STRATEGY>>();
		List<BUY_STRATEGY> list=new ArrayList<BUY_STRATEGY>(); 
		getAllBuyStrategy(res_buy,list,0);
		List<List<SELL_STRATEGY>> res_sell = new ArrayList<List<SELL_STRATEGY>>();
		List<SELL_STRATEGY> list2=new ArrayList<SELL_STRATEGY>();
		getAllSellStrategy(res_sell,list2,0);
		
		for (int i=0;i<res_buy.size();i++) {
			
			for (int j=0;j<res_sell.size();j++) {
				List<BUY_STRATEGY> list_buy=res_buy.get(i);
				List<SELL_STRATEGY> list_sell=res_sell.get(j);
				
				testStrategy(ema120,ema60,ema30,ema20,macd,prices,list_buy,list_sell,symbol,exchange,false,list_detail,vol_ema20);
			}
		}
		
		
	}
	
	public static void getAllBuyStrategy(List<List<BUY_STRATEGY>> res,List<BUY_STRATEGY> list, int i ){
		if (i==BUY_STRATEGY.values().length) {
			List<BUY_STRATEGY> copy=new ArrayList<BUY_STRATEGY>();
//			Collections.copy(copy, list);
			for (int j=0;j<list.size();j++) {
				copy.add(list.get(j));
			}
			res.add(copy);
			return;
		}
		list.add(BUY_STRATEGY.values()[i]);
		getAllBuyStrategy(res,list,i+1);
		list.remove(list.size()-1);
		getAllBuyStrategy(res,list,i+1);
	}
	
	public static void getAllSellStrategy(List<List<SELL_STRATEGY>> res,List<SELL_STRATEGY> list, int i ){
		if (i==SELL_STRATEGY.values().length) {
			List<SELL_STRATEGY> copy=new ArrayList<SELL_STRATEGY>();
//			Collections.copy(copy, list);
			for (int j=0;j<list.size();j++) {
				copy.add(list.get(j));
			}
			res.add(copy);
			return;
		}
		list.add(SELL_STRATEGY.values()[i]);
		getAllSellStrategy(res,list,i+1);
		list.remove(list.size()-1);
		getAllSellStrategy(res,list,i+1);
	}
	
	public static void testStrategy(Double[] ema120,Double[] ema60,Double[] ema30,Double[] ema20,Double[] macd,Price[] prices,
			List<BUY_STRATEGY> list_buy ,List<SELL_STRATEGY> list_sell,String symbol,
			String exchange,boolean delay_to_next_day_trade,boolean list_detail,Double[] vol_ema20) {
		if (list_buy.size()==0 || list_sell.size()==0) return;
		Connection conn=getConnection();
//		macd变绿卖掉 变红买入
		String status="";
		double buyprice=0.0;
		int buyday=0;
		Date buydate=null;
		int sellday=0;
		Date selldate=null;
		int total_hold_days=0;
		double sellprice=0.0;
		double sum_gain=0.0;
		double buymacd=0.0;
		double sellmacd=0.0;
		SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd");
		for (int i=1;i<macd.length;i++) {
			if (status.equals("")) {
				boolean b=false;
				if (list_buy.contains(BUY_STRATEGY.BUY_ON_GREEN)) {
					if (macd[i]<0 && macd[i-1]>0 ) {
						b=true;
					}
				}
				if (list_buy.contains(BUY_STRATEGY.BUY_ON_SHORTER_RED)) {
//					if ((macd[i]<0 && macd[i]>macd[i-1] )||(macd[i]<0 && macd[i-1]<0 && macd[i]/macd[i-1]<1.05)) {
					if ((macd[i]>0 && macd[i]<macd[i-1] )) {
						b=true;
					}
				}
					
				if (list_buy.contains(BUY_STRATEGY.BUY_ON_LONGER_GREEN)){
					if (macd[i-1]<0 && macd[i]<macd[i-1]) {
						b=true;
					}
				}
//				if (b && prices[i].getClose()>ema20[i]) {
				if (b) {
					status="HOLD";
					buyprice=prices[i].getClose();
					buyday=i;
					buydate=prices[i].getTrade_date();
					buymacd=macd[i];
				}
			}else {
				boolean s=false;
				if (list_sell.contains(SELL_STRATEGY.SELL_ON_RED)) {
					if (macd[i-1]<0 && macd[i]>=0) {
						s=true;
					}
				}
				if (list_sell.contains(SELL_STRATEGY.SELL_ON_SHORTER_GREEN)) {
					if (macd[i]>macd[i-1] && macd[i]<0) {
						s=true;
					}
				}
				if (list_sell.contains(SELL_STRATEGY.SELL_ON_LONGER_RED)) {
					if (macd[i-1]>0 && macd[i]>macd[i-1] ) {
//					if (macd[i-1]<0 && macd[i]<macd[i-1]) {
						s=true;
					}
				}
				if (i>0 && (prices[i].getClose()-prices[i-1].getClose())/prices[i-1].getClose()<0.1 && vol_ema20!=null && prices[i].getVolumn()>vol_ema20[i]*1.5) {
//					s=true;
				}
				if (s) {
					status="";
					if (delay_to_next_day_trade) {
						sellprice=i<prices.length-1?prices[i+1].getOpen():prices[i].getClose();
						sellday=i<prices.length-1?i+1:i;
						selldate=i<prices.length-1?prices[i+1].getTrade_date():prices[i].getTrade_date();
						sellmacd=macd[i];
						sum_gain+=(sellprice-buyprice)/buyprice;
						total_hold_days+=sellday-buyday;
						if (list_detail) {
							System.out.println("持股时间:"+(sellday-buyday)+" 天  从"+df.format(buydate)+" 到"+df.format(selldate)+" 收益:"+Math.round((sellprice-buyprice)*100/buyprice) +"% 卖价："+sellprice+" 买价： "+buyprice+ "卖出MACD："+Math.round(sellmacd*100)/100D+" 买入MACD:"+Math.round(buymacd*100)/100D);
//							create table detail_gain (symbol varchar(100), exchange varchar(100),hold_days int,buydate datetime, selldate datetime, gain_percent int, sellprice double, buyprice double, sellmacd double, buymacd double);
						}
						saveDetailGainToDB(conn,symbol,exchange,sellday-buyday,
								buydate,selldate,
								(int)Math.round((sellprice-buyprice)*100/buyprice),
								sellprice,buyprice,Math.round(sellmacd*100)/100D,Math.round(buymacd*100)/100D,getStrategyDesc(list_buy,list_sell));
						
					}else {
						sellprice=prices[i].getClose();
						sellday=i;
						selldate=prices[i].getTrade_date();
						sellmacd=macd[i];
						sum_gain+=(sellprice-buyprice)/buyprice;
						total_hold_days+=sellday-buyday;
						if (list_detail) {
							System.out.println("持股时间:"+(sellday-buyday)+" 天  从"+df.format(buydate)+" 到"+df.format(selldate)+" 收益:"+Math.round((sellprice-buyprice)*100/buyprice) +"% 卖价："+sellprice+" 买价： "+buyprice+ "卖出MACD："+Math.round(sellmacd*100)/100D+" 买入MACD:"+Math.round(buymacd*100)/100D);
						}
						saveDetailGainToDB(conn,symbol,exchange,sellday-buyday,
								buydate,selldate,
								(int)Math.round((sellprice-buyprice)*100/buyprice),
								sellprice,buyprice,Math.round(sellmacd*100)/100D,Math.round(buymacd*100)/100D,getStrategyDesc(list_buy,list_sell));
						
					}
					
				}
			}
		}
		System.out.println(symbol+" "+(delay_to_next_day_trade?"延迟一天交易 ":"")+getStrategyDesc(list_buy,list_sell)+" 总收益："+Math.round((Double)sum_gain*100)+"% "+ "总持股天数："+total_hold_days);
//		create table sum_gain (symbol varchar(100),exchange varchar(100),delay_one_day int, strategy varchar(255), gain_percent int, total_hold_days int);
		TotalGain tg=new TotalGain(symbol,exchange,delay_to_next_day_trade,
				getStrategyDesc(list_buy,list_sell),(int)Math.round((Double)sum_gain*100),total_hold_days);
		saveGainResultToDB(conn,tg);
		closeConnection(conn);
	}
	
	public static void closeConnection(Connection conn) {
		try {
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static void cleanGainResultFromDB() {

		Connection conn=getConnection();
		PreparedStatement pstmt;
		try {
			pstmt = conn.prepareStatement("delete from sum_gain");
			pstmt.execute();
			pstmt = conn.prepareStatement("delete from detail_gain");
			pstmt.execute();
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			try {
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void saveDetailGainToDB(Connection conn,String symbol,String exchange,int hold_days,
			Date buydate,Date selldate,
			int gain_percent,
			double sellprice,double buyprice,double sellmacd,double buymacd,String strategy){


		PreparedStatement pstmt;
		try {
			pstmt = conn.prepareStatement("insert into detail_gain values (?,?,?,?,?,?,?,?,?,?,?)");
			int index=1; 
			pstmt.setString(index++, symbol);
			pstmt.setString(index++, exchange);
			pstmt.setInt(index++, hold_days);
			pstmt.setTimestamp(index++, new java.sql.Timestamp(buydate.getTime()));
			pstmt.setTimestamp(index++, new java.sql.Timestamp(selldate.getTime()));
			pstmt.setInt(index++, gain_percent);
			pstmt.setDouble(index++, sellprice);
			pstmt.setDouble(index++, buyprice);
			pstmt.setDouble(index++, sellmacd);
			pstmt.setDouble(index++, buymacd);
			pstmt.setString(index++, strategy);
			pstmt.execute();
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
	}
	
	public static void saveGainResultToDB(Connection conn,TotalGain tg) {

		PreparedStatement pstmt;
		try {
			pstmt = conn.prepareStatement("insert into sum_gain values (?,?,?,?,?,?)");
			pstmt.setString(1, tg.symbol);
			pstmt.setString(2, tg.exchange);
			pstmt.setBoolean(3, tg.delay_one_day);
			pstmt.setString(4, new String(tg.strategyDesc));
			pstmt.setInt(5, tg.gain_percent);
			pstmt.setInt(6, tg.total_hold_days);
			pstmt.execute();
			
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static List<String[]> getAllSymbolsFromDB(){
		Connection conn=getConnection();
		List<String[]> res=new ArrayList<String[]>();
		try {
			PreparedStatement pstmt=conn.prepareStatement("select distinct symbol, exchange from hist_price");
			ResultSet rs=pstmt.executeQuery();
			while (rs.next()) {
				res.add(new String[] {rs.getString(1),rs.getString(2)});
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			try {
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return res;
		
	}
	
	
	public static String getStrategyDesc(List<BUY_STRATEGY> buy,List<SELL_STRATEGY> sell) {
		StringBuilder sb=new StringBuilder();
		sb.append("");
		if (buy.contains(BUY_STRATEGY.BUY_ON_GREEN)) {
			sb.append("变绿买入 ");
		}
		if (buy.contains(BUY_STRATEGY.BUY_ON_SHORTER_RED)) {
			sb.append("红柱变短买入 ");
		}
		if (buy.contains(BUY_STRATEGY.BUY_ON_LONGER_GREEN)){
			sb.append("绿柱变长买入 ");
		}
		if (sell.contains(SELL_STRATEGY.SELL_ON_RED)) {
			sb.append("变红卖出 ");
		}
		if (sell.contains(SELL_STRATEGY.SELL_ON_SHORTER_GREEN)) {
			sb.append("绿柱变短卖出 ");
		}
		if (sell.contains(SELL_STRATEGY.SELL_ON_LONGER_RED)) {
			sb.append("红柱变长卖出 ");
		}
		return sb.toString();
	}
	public static void print(Object[] arr) {
		for (int i=0;i<arr.length;i++) {
			if (arr[i] instanceof Double) {
				System.out.print(Math.round((Double)arr[i]*100)/100D+" ");
			}else {
				System.out.print(arr[i]+" ");
			}
			
		}
		System.out.println();
	}
	public static Double[] getEMA(Price[] prices, int days) {
		Double[] ema=new Double[prices.length];
		for (int i=0;i<prices.length;i++) {
			ema[i]=0.0;
		}
		double k=2.0/(days+1.0);
		for (int i=1;i<prices.length;i++) {
			ema[i]=prices[i].getClose()*k+ema[i-1]*(1-k);
		}
		for (int i=0;i<prices.length;i++) {
//			System.out.println(ema[i]);
		}
		return ema;
	}
	
	public static Double[] getEMAVol(Price[] prices, int days) {
		Double[] ema=new Double[prices.length];
		for (int i=0;i<prices.length;i++) {
			ema[i]=0.0;
		}
		double k=2.0/(days+1.0);
		for (int i=1;i<prices.length;i++) {
			ema[i]=prices[i].getVolumn()*k+ema[i-1]*(1-k);
		}
		for (int i=0;i<prices.length;i++) {
//			System.out.println(ema[i]);
		}
		return ema;
	}
	
	public static Double[] getEMA(Double[] prices, int days) {
		Double[] ema=new Double[prices.length];
		for (int i=0;i<prices.length;i++) {
			ema[i]=0.0;
		}
		double k=2.0/(days+1.0);
		for (int i=1;i<prices.length;i++) {
			ema[i]=prices[i]*k+ema[i-1]*(1-k);
		}
		for (int i=0;i<prices.length;i++) {
//			System.out.println(ema[i]);
		}
		return ema;
	}
	
	public static Date[] getMinMaxAvailableHist(Contract c) {
		Connection conn=getConnection();
		try {
			PreparedStatement pstmt=conn.prepareStatement("select min(trade_date),max(trade_date) from hist_price where "
					+ "symbol=? and exchange=?");
			pstmt.setString(1, c.symbol());
			pstmt.setString(2, c.exchange());
			ResultSet rs=pstmt.executeQuery();
			if (rs.next()) {
				return new Date[] {rs.getTimestamp(1),rs.getTimestamp(2)};
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			try {
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
		
	}
	
	public static Price[] getPriceFromDB(String symbol, String exchange,Date start,Date end) {
		Connection conn=getConnection();
		List<Price> list=new ArrayList<Price>();
		PreparedStatement pstmt;
		try {
			pstmt = conn.prepareStatement("select * from hist_price where symbol=? and exchange=? and trade_date between ? and ? order by trade_date");
			pstmt.setString(1, symbol);
			pstmt.setString(2, exchange);
			pstmt.setTimestamp(3, new java.sql.Timestamp(start.getTime()));
			pstmt.setTimestamp(4, new java.sql.Timestamp(end.getTime()));
			ResultSet rs=pstmt.executeQuery();
			
			while (rs.next()) {
				Price p=new Price();
				int index=1;
				p.setSymbol(rs.getString(index++));
				p.setExchange(rs.getString(index++));
				p.setTrade_date(rs.getTimestamp(index++));
				p.setOpen(rs.getDouble(index++));
				p.setHigh(rs.getDouble(index++));
				p.setLow(rs.getDouble(index++));
				p.setClose(rs.getDouble(index++));
				p.setVolumn(rs.getInt(index++));
				list.add(p);
				
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			try {
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return list.toArray(new Price[0]);
		
	}
	public static Connection getConnection() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			System.out.println("Where is your MySQL JDBC Driver?");
			e.printStackTrace();
			return null;
		}

//		System.out.println("MySQL JDBC Driver Registered!");
		Connection connection = null;

		try {
			connection = DriverManager
			.getConnection("jdbc:mysql://localhost:3306/ib?useSSL=false","root", "xpxp");

		} catch (SQLException e) {
			System.out.println("Connection Failed! Check output console");
			e.printStackTrace();
		}
		PreparedStatement pstmt;
		try {
			pstmt = connection.prepareStatement("set names utf8");
			pstmt.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return connection;
	}
	public static Double[] getPriceFromFile(String symbol) {
		List<Double> list=new ArrayList<Double>();
		try (BufferedReader br = new BufferedReader(new FileReader("c://dev//"+symbol+".csv"))) {
		    String line;
		    int cnt=0;
		    while ((line = br.readLine()) != null) {
		    	
		       // process the line.
		    	if (++cnt>2) {
		    		Double d=new Double(line.split(",")[1].replace("\"", ""));
//		    		System.out.println(d);
		    		list.add(d);
		    	}
		    }
		    Collections.reverse(list);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return list.toArray(new Double[0]);
	}
	
	public static Double[] getAllPrices(String symbol, String exchange) {

		Connection conn=getConnection();
		List<Double> list=new ArrayList<Double>();
		PreparedStatement pstmt;
		try {
			pstmt = conn.prepareStatement("select close from hist_price where symbol=? and exchange=? order by trade_date");
			pstmt.setString(1, symbol);
			pstmt.setString(2, exchange);
			ResultSet rs=pstmt.executeQuery();
			
			while (rs.next()) {
				list.add(rs.getDouble(1));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			try {
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return list.toArray(new Double[0]);
		
	
	}
	public static boolean isCross(int i,Price[] prices) {
		Price p=prices[i];
		if (Math.abs(p.getOpen()-p.getClose())/Math.abs(p.getHigh()-p.getLow()) < 0.2) {
			return true;
		}
		return false;
	}
	
	public static Double maxProfit(int k, Double[] prices) {
		 // dp[i, j] represents the max profit up until prices[j] using at most i transactions. 
//		         dp[i][j]=Max(dp[i][j-1])
//		         dp[i-1][j ]
		        
		        if (k<=0 || prices==null || prices.length==0) return 0D;
		        int n=prices.length;
		        
		        if (k>=n/2) {
		        	Double maxprofit=0D;
		        	for (int i=1;i<prices.length;i++) {
		        		if (prices[i]>prices[i-1]) {
		        			maxprofit+=(prices[i]-prices[i-1]);
		        		}
		        	}
		        	return maxprofit;
		        }
		       
		        double[][] dp=new double[k+1][prices.length];
		        for (int i=1;i<dp.length;i++){
		        	Double rowmax=dp[i-1][0]-prices[0];
		            for (int j=1;j<dp[0].length;j++){
		            	rowmax=Math.max(rowmax,dp[i-1][j-1]-prices[j-1]);
		                dp[i][j]=dp[i][j-1];
		                dp[i][j]=Math.max(dp[i][j],prices[j]+rowmax);
//		                simplify algorithm
//		                for (int jj=0;jj<=j-1;jj++){
//		                    dp[i][j]=Math.max(dp[i][j],dp[i-1][jj]+prices[j]-prices[jj]);
//		                }
		                
		            }
		        }
		        return dp[k][prices.length-1];
		        
		    }
	
}
