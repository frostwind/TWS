package test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Util {

	public static String[] hk_symbols=new String[] {"2318","700"};
	public static String[] nasdaq_symbols=new String[] {"BZUN","NVDA","SQ","MU","VERI"};
	
	public static String[] us_symbols=new String[] {"ZSAN"};
	
	public static String[] dow_symbols=new String[] {"AXP","AAPL","BA","CAT","CVX",
			"CSCO","KO","DIS","DWDP","XOM","GE","GS","HD","IBM","INTC","JNJ","JPM",
			"MCD","MRK","MSFT","NKE","PFE","PG","TRV","UTX","UNH","VZ","V","WMT"};
//	public static String[] us_symbols=new String[] {"FB","VERI","MU","SQ","AMZN","AMD","TSLA","APPL","IBM"};
//	public static String[] us_symbols=new String[] {"MU","SQ","AMZN","TSLA","APPL","IBM"};
	public static enum BUY_STRATEGY{
		BUY_ON_RED,
		BUY_ON_SHORTER_GREEN,
		BUY_ON_LONGER_RED
	};
	public static enum SELL_STRATEGY{
		SELL_ON_SHORTER_RED,
		SELL_ON_GREEN,
		SELL_ON_LONGER_GREEN
	};
	
	public static final String BAR_1_SEC="1 sec";
	public static final String BAR_5_SEC="5 secs";
	public static final String BAR_15_SEC="15 secs";
	public static final String BAR_30_SEC="30 secs";
	public static final String BAR_1_MIN="1 min";
	public static final String BAR_2_MIN="2 mins";
	public static final String BAR_3_MIN="3 mins";
	public static final String BAR_5_MIN="5 mins";
	public static final String BAR_15_MIN="15 mins";
	public static final String BAR_30_MIN="30 mins";
	public static final String BAR_1_HOUR="1 hour";
	public static final String BAR_1_DAY="1 day";
	
	/**Duration unit
	 * " S (seconds) - " D (days)
" W (weeks) - " M (months)
" Y (years)
	 */
	
	/**
	 *  1 sec
		5 secs
		15 secs
		30 secs
		1 min
		2 mins
		3 mins
		5 mins
		15 mins
		30 mins
		1 hour
		1 day
	 * @return
	 */
	
	public static final String HK_EXCHANGE="SEHK";
	public static final String US_EXCHANGE="SMART";
	
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
	
	public static String[] getSP500List() {
		Connection conn=getConnection();
		PreparedStatement pstmt;
		List<String> list=new ArrayList<String>();
		try {
			pstmt = conn.prepareStatement("select symbol from stock where category='SP500'");
			ResultSet rs=pstmt.executeQuery();
			while (rs.next()) {
				list.add(rs.getString(1));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] res=list.toArray(new String[0]);
		return res;
		
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
				Date d=p.getTrade_date();
				d.setTime(d.getTime()+15*60*1000);
				p.setTrade_date(d);
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
	
	public static double calculateComplexProfit(String symbol, String exchange) {
		Connection conn=getConnection();
		List<Double> list=new ArrayList<Double>();
		double multi=1.0;
		try {
			PreparedStatement pstmt=conn.prepareStatement("select gain_percent,buy_date from revised_detail_gain where symbol=? and exchange=?  order by sell_date");
			pstmt.setString(1, symbol);
			pstmt.setString(2, exchange);
			ResultSet rs=pstmt.executeQuery();
			while (rs.next()) {
				multi*=(1+(rs.getDouble(1)-0.5)/10000);
//				if (multi<1)
//				System.out.println(multi+" "+rs.getTimestamp(2));
				list.add(multi);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Double[] dd=list.toArray(new Double[0]);
		
		Double min=null;
		
		for (int i=0;i<dd.length;i++) {
			if (min==null) {
				min=dd[i];
			}
			else if (dd[i]<min){
				min=dd[i];
			}
		}
		System.out.println(min);
		
		
		
		
		
		
		return multi;
	}
	
	
	
	public static void main(String[] args) {
		System.out.println(calculateComplexProfit("AMD","SMART"));
	}
}
