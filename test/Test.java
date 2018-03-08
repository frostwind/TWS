package test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ib.client.Contract;

import test.DayLong.BUY_STRATEGY;

public class Test {
	public static void main(String[] args) {
		Date[] d=getMinMaxAvailableHist("123","sss");
//		System.out.println(d[0]);
//		System.out.println(d[2]);
		Double[] prices=DayLong.getAllPrices("700",DayLong.HK_EXCHANGE);
//		System.out.println(prices.length);
		System.out.println(DayLong.maxProfit(1000, prices));
	}
	
	public static Date[] getMinMaxAvailableHist(String symbol,String exchange) {
		Connection conn=DayLong.getConnection();
		try {
			PreparedStatement pstmt=conn.prepareStatement("select min(trade_date),max(trade_date) from hist_price where "
					+ "symbol=? and exchange=?");
			pstmt.setString(1, symbol);
			pstmt.setString(2,exchange);
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
}
