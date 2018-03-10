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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ib.client.Contract;

import test.Util.BUY_STRATEGY;
import test.Util.SELL_STRATEGY;

public class InDayWaveAuto {
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
//		System.setOut(out);
//		String[] symbols=new String[] {"AMD","NVDA","TSLA","GOOG","BZUN"};
//		String[] hk_symbols=new String[] {"2318","700","388","1398"};
		List<String[]> symbols=getAllSymbolsFromDB();
		
		SimpleDateFormat form = new SimpleDateFormat("yyyy-MM-dd");
		String barsize=Util.BAR_15_MIN;
		for (String[] symbol:symbols) {
			Calendar cal=Calendar.getInstance();
			for (int i=0;i<1;i++) {
				
				String s=" 到 "+form.format(cal.getTime());
				Date end=cal.getTime();
				cal.add(Calendar.YEAR, -2);
				Date start=cal.getTime();
				s="从 "+form.format(cal.getTime())+s;
				System.out.println("======================"+s+"======================");
				
				
				analyzeStock(symbol[0],symbol[1],false,start,end,barsize);
			}
			
		}
		out.flush();
		out.close();
		
	}
	public static void analyzeStock(String symbol,String exchange,boolean list_detail,Date start,Date end,String barsize) {
		Calendar cal=Calendar.getInstance();
		cal.setTime(start);
		Date old_start=cal.getTime();
		cal.setTime(end);
		Date old_end=cal.getTime();
//		Double[] prices=getPriceFromFile(symbol);
		
		
		
		Price[] prices=getPriceFromDB(symbol,exchange,start,end,barsize);
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
		for (int i=0;i<prices.length;i++) {
//			System.out.println(prices[i].getTrade_date()+" "+macd[i]);
		}
		BUY_STRATEGY[] buy=new BUY_STRATEGY[BUY_STRATEGY.values().length];
		SELL_STRATEGY[] sell=new SELL_STRATEGY[SELL_STRATEGY.values().length];
		
		List<List<BUY_STRATEGY>> res_buy = new ArrayList<List<BUY_STRATEGY>>();
		List<BUY_STRATEGY> list=new ArrayList<BUY_STRATEGY>(); 
		getAllBuyStrategy(res_buy,list,0);
		List<List<SELL_STRATEGY>> res_sell = new ArrayList<List<SELL_STRATEGY>>();
		List<SELL_STRATEGY> list2=new ArrayList<SELL_STRATEGY>();
		getAllSellStrategy(res_sell,list2,0);
		
		
		

		cleanGainResultFromDB(symbol,exchange);
		BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>(); 
		
		int N=Runtime.getRuntime().availableProcessors()/3;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(N, N*3/2, 60, TimeUnit.SECONDS, queue);
        for (int i=1;i<=10;i++) {
			for (int j=1;j<=10;j++) {
				for (int k=1;k<=5;k++) {
					executor.execute(new Thread(new WorkerThread(i,j,k,ema5,ema120,ema60,ema30,ema20,macd,prices,res_buy.get(0),res_sell.get(0),symbol,exchange,false,list_detail,vol_ema20), "WorkerThread"+i));
//					testStrategy(i,j,k,ema5,ema120,ema60,ema30,ema20,macd,prices,res_buy.get(0),res_sell.get(0),symbol,exchange,false,list_detail,vol_ema20);
				}
			}
			
		}
		executor.shutdown();   
		while (!executor.isTerminated()) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		chooseMostProfitStrategy(symbol,exchange,start,end);
		
	
		
		
		
		

		
		
	}
	public static void chooseMostProfitStrategy(String symbol,String exchange,Date start,Date end) {
		Connection conn=Util.getConnection();
		if (conn==null) return;
		PreparedStatement pstmt;
		Calendar cal=Calendar.getInstance();
		cal.setTime(start);
		Date s_date=cal.getTime();
		cal.setTime(end);
		Date e_date=cal.getTime();
		try {
			pstmt = conn.prepareStatement("select min(date(buy_date)),max(date(buy_date)) from detail_gain where symbol=? and exchange=? and buy_date between ? and ?");
			int index=1;
			pstmt.setString(index++, symbol);
			pstmt.setString(index++, exchange);
			pstmt.setTimestamp(index++, new java.sql.Timestamp(start.getTime()));
			pstmt.setTimestamp(index++, new java.sql.Timestamp(end.getTime()));
			ResultSet rs=pstmt.executeQuery();
			System.out.println(symbol+" "+exchange+" "+start+" "+end);
			if (rs.next()) {
				start=rs.getTimestamp(1);
				end=rs.getTimestamp(2);
				System.out.println(start+" "+end);
			}
			List<Date> list=new ArrayList<Date>();
			while (start.before(end)) {
				list.add(start);
				cal=Calendar.getInstance();
				cal.setTime(start);
				cal.add(Calendar.DAY_OF_YEAR, 1);
				start=cal.getTime();
			}
			PreparedStatement clean=conn.prepareStatement("delete from detail_gain_agg_month where symbol=? and exchange=?");
			clean.setString(1, symbol);
			clean.setString(2, exchange);
			clean.executeUpdate();
			clean=conn.prepareStatement("delete from  revised_detail_gain where symbol=? and exchange=?");
			clean.setString(1, symbol);
			clean.setString(2, exchange);
			clean.executeUpdate();
			
			PreparedStatement pre_processing=conn.prepareStatement(" insert into detail_gain_agg_month select ?,?,t.gain_percent,t.buy_date,t.strategy,t.trades from \r\n" + 
					" (select sum(gain_percent) as gain_percent , count(*) as trades , FIRST_DAY_OF_MONTH(buy_date) as buy_date, strategy \r\n" + 
					"  from detail_gain where symbol=? and exchange=?\r\n" + 
					" and sell_date between ? and ? \r\n" + 
					"   group by FIRST_DAY_OF_MONTH(buy_date), strategy) t");
			index=1;
			pre_processing.setString(index++, symbol);
			pre_processing.setString(index++, exchange);
			pre_processing.setString(index++, symbol);
			pre_processing.setString(index++, exchange);
			pre_processing.setTimestamp(index++, new java.sql.Timestamp(s_date.getTime()));
			pre_processing.setTimestamp(index++, new java.sql.Timestamp(e_date.getTime()));
			pre_processing.executeUpdate();
			
//			PreparedStatement pstmt2=conn.prepareStatement("select * from (select sum(gain_percent),count(*),strategy from detail_gain where symbol=? and exchange=? and sell_date between ? and ? group by strategy order by sum(gain_percent) desc) t limit 1");
			
			PreparedStatement pstmt2=conn.prepareStatement("select * from (select sum(gain_percent),sum(trades),strategy from detail_gain_agg_month where symbol=? and exchange=? and trade_date between ? and ? group by strategy order by sum(gain_percent) desc) t limit 1");
			PreparedStatement pstmt3=conn.prepareStatement("select symbol,exchange,hold_days,buy_date,sell_date,gain_percent,sellprice,buyprice,strategy from detail_gain where  symbol=? and exchange=? and buy_date between ? and ? and strategy=? order by buy_date");
			PreparedStatement pstmt4=conn.prepareStatement("insert into revised_detail_gain ( symbol,exchange,hold_days,buy_date,sell_date,gain_percent,sellprice,buyprice,strategy)  values (?,?,?,?,?,?,?,?,?)");
			int batch_count=0;
			Map<Date,Integer> monthly_gain=new HashMap<Date,Integer>();
			for (int i=0;i<list.size();i++) {
				end=list.get(i);
				cal=Calendar.getInstance();
				cal.setTime(end);
				cal.add(Calendar.DAY_OF_YEAR, -365);
				start=cal.getTime();
				index=1;
				pstmt2.setString(index++, symbol);
				pstmt2.setString(index++, exchange);
				pstmt2.setTimestamp(index++, new java.sql.Timestamp(start.getTime()));
				pstmt2.setTimestamp(index++, new java.sql.Timestamp(end.getTime()));
				rs=pstmt2.executeQuery();
				int gain=0;
				int trades=0;
				String strategy=null;
				if (rs.next()) {
					gain=rs.getInt(1);
					trades=rs.getInt(2);
					strategy=rs.getString(3);
				}
				if (trades>0) {
					
					index=1;
					start=list.get(i);
					cal=Calendar.getInstance();
					cal.setTime(start);
					cal.add(Calendar.DAY_OF_YEAR, 1);
					end=cal.getTime();
					
					pstmt3.setString(index++, symbol);
					pstmt3.setString(index++, exchange);
					pstmt3.setTimestamp(index++, new java.sql.Timestamp(start.getTime()));
					pstmt3.setTimestamp(index++, new java.sql.Timestamp(end.getTime()));
					pstmt3.setString(index++, strategy);
					rs=pstmt3.executeQuery();
					List<DetailGain> toInsert=new ArrayList<DetailGain>();
					int day_gain=0;
					
					while (rs.next()) {
						
						index=1;
						DetailGain dg=new DetailGain();
						dg.symbol=rs.getString(index++);
						dg.exchange=rs.getString(index++);
						dg.hold_days=rs.getInt(index++);
						dg.buy_date=rs.getTimestamp(index++);
						dg.sell_date=rs.getTimestamp(index++);
						dg.gain_percent=rs.getInt(index++);
						dg.sellprice=rs.getDouble(index++);
						dg.buyprice=rs.getDouble(index++);
						dg.strategy=rs.getString(index++);
						
						
						Date buydate=dg.buy_date;
						cal.setTime(buydate);
						cal.set(Calendar.DAY_OF_MONTH, 1);
						cal.set(Calendar.HOUR_OF_DAY, 0);
						cal.set(Calendar.MINUTE, 0);
						cal.set(Calendar.SECOND, 0);
						cal.set(Calendar.MILLISECOND, 0);
						buydate=cal.getTime();
						if (!monthly_gain.containsKey(buydate)) {
							monthly_gain.put(buydate, 0);
						}
						Integer p=monthly_gain.get(buydate);
						if (p<-500) {
							//每月5%止损
//							break;
						}
						
						toInsert.add(dg);

						Integer profit=monthly_gain.get(buydate);
						profit+=dg.gain_percent;
						monthly_gain.put(buydate, profit);
						
						day_gain+=dg.gain_percent;
						if (day_gain<-200) {
							//每日3%止损
							break;
						}
					}
					for (int j=0;j<toInsert.size();j++) {
						DetailGain dg=toInsert.get(j);
						index=1;
						pstmt4.setString(index++, dg.symbol);
						pstmt4.setString(index++, dg.exchange);
						pstmt4.setInt(index++, dg.hold_days);
						pstmt4.setTimestamp(index++, new java.sql.Timestamp(dg.buy_date.getTime()));
						pstmt4.setTimestamp(index++, new java.sql.Timestamp(dg.sell_date.getTime()));
						pstmt4.setInt(index++, dg.gain_percent);
						pstmt4.setDouble(index++, dg.sellprice);
						pstmt4.setDouble(index++, dg.buyprice);
						pstmt4.setString(index++, dg.strategy);
						pstmt4.addBatch();
						if (batch_count++%1000==0) {
							pstmt4.executeBatch();
						}
					}
				}
				
			}
			pstmt4.executeBatch();
			
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
	
	public static void testStrategy(int bar_num,int stop_loss_step, int revert_trend_step,Double[] ema5,Double[] ema120,Double[] ema60,Double[] ema30,Double[] ema20,Double[] macd,Price[] prices,
			List<BUY_STRATEGY> list_buy ,List<SELL_STRATEGY> list_sell,String symbol,
			String exchange,boolean delay_to_next_day_trade,boolean list_detail,Double[] vol_ema20) {
		if (list_buy.size()==0 || list_sell.size()==0) return;
		Connection conn=null;
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
		SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		

		conn=getConnection();
		
		PreparedStatement pstmt;
		Date min_date=null;
		Date max_date=null;
		try {
			pstmt = conn.prepareStatement("select min(date(buy_date)),max(date_sub(date(buy_date),interval 2 day)) from detail_gain where symbol=? and exchange=? and strategy=?");
			int index=1;
			pstmt.setString(index++, symbol);
			pstmt.setString(index++, exchange);
			String strategy="BULL-"+bar_num+"-"+stop_loss_step+"-"+revert_trend_step;
//			System.out.println(strategy);
			pstmt.setString(index++, strategy);
			ResultSet rs=pstmt.executeQuery();
			if (rs.next()) {
				min_date=rs.getTimestamp(1);
				max_date=rs.getTimestamp(2);
//				System.out.println("========="+min_date+" "+max_date);
			}
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		int trades=0;
		PreparedStatement batchInsert=null;
		int count=0;
		try {
			batchInsert=conn.prepareStatement("replace into detail_gain values (?,?,?,?,?,?,?,?,?,?,?)");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int skipped=0;
		for (int i=1;i<prices.length;i++) {
			Price p=prices[i];
			if (max_date!=null && p.getTrade_date().before(max_date)) {
				skipped++;
				continue;
			}
			if (status.equals("")) {
				boolean bull=true;
				for (int x=bar_num;x>=1 && i-x>=0;x--) {
					if (prices[i-x].getClose()<ema5[i-x] || (vol_ema20!=null && prices[i-x].getVolumn()>0 && prices[i-x].getVolumn()<vol_ema20[i-x]*1.2)) {
//					if (prices[i-x].getClose()<ema5[i-x] || (prices[i-x].getVolumn()>0 )) {
						bull=false;
					}
				}
				
				if (prices[i].getTrade_date().getHours()==13 || (prices[i].getTrade_date().getHours()==12 && prices[i].getTrade_date().getMinutes()>=45) ) {
					bull=false;
				}
				
				if (bull) {
					status="BULL";
				}
				
				
//				if (b && prices[i].getClose()>ema20[i]) {
				if (!status.equals("")) {
					buyprice=prices[i].getOpen();
					buyday=i;
					buydate=prices[i].getTrade_date();
					buymacd=macd[i];
				}
			}else {
				boolean s=false;
				if (status.equals("BULL")) {
					if (prices[i-1].getClose()/buyprice<1-0.001*stop_loss_step || (prices[i-1].getClose()/prices[i-1].getOpen()<1-0.001*stop_loss_step && getBarSize(i-1,prices)>revert_trend_step*0.1)) {
//					if ((prices[i].getClose()/prices[i].getOpen()<0.995 && getBarSize(i,prices)>0.8)) {
						s=true;
					}
				}
				
				if (prices[i].getTrade_date().getHours()==13 || (prices[i].getTrade_date().getHours()==12 && prices[i].getTrade_date().getMinutes()>=45) ) {
					s=true;
				}
				if (s) {
					
					trades++;
					sellprice=prices[i-1].getClose();
					sellday=i;
					selldate=prices[i-1].getTrade_date();
					sellmacd=macd[i];
					double gain=0.0;
					if (status.equals("BULL")) {
						sum_gain+=(sellprice-buyprice)/buyprice;
						gain=(sellprice-buyprice)/buyprice;
					}else if (status.equals("BEAR")) {
						sum_gain+=(buyprice-sellprice)/buyprice;
						gain=(buyprice-sellprice)/buyprice;
					}
//					System.out.println(gain);
					
					total_hold_days+=sellday-buyday;
					if (list_detail) {
						System.out.println("持股时间:"+(sellday-buyday)+" 分钟  从"+df.format(buydate)+" 到"+df.format(selldate)+(status.equals("BULL")?"多":"空")+" 收益:"+Math.round(gain*10000) +"%% 卖价："+sellprice+" 买价： "+buyprice+ "卖出MACD："+Math.round(sellmacd*100)/100D+" 买入MACD:"+Math.round(buymacd*100)/100D);
					}
					saveDetailGainToDB(conn,batchInsert,count++,symbol,exchange,sellday-buyday,
							buydate,selldate,
							(int)Math.round(gain*10000),
							sellprice,buyprice,Math.round(sellmacd*100)/100D,Math.round(buymacd*100)/100D,status+"-"+bar_num+"-"+stop_loss_step+"-"+revert_trend_step);
					status="";
				
					
				}
			}
		}
//		System.out.println("skipped:"+skipped);
		try {
			batchInsert.executeBatch();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	

		System.out.println(symbol+" "+(delay_to_next_day_trade?"延迟一天交易 ":"")+"看前面连续"+bar_num+"根3分钟线 止盈止损因子"+stop_loss_step+"趋势反转因子"+revert_trend_step+" 总收益："+Math.round((Double)sum_gain*100)+"% "+ "总持股分钟数："+total_hold_days+" 总交易次数:"+trades);
//		create table sum_gain (symbol varchar(100),exchange varchar(100),delay_one_day int, strategy varchar(255), gain_percent int, total_hold_days int);
		TotalGain tg=new TotalGain(symbol,exchange,delay_to_next_day_trade,
				bar_num+"",(int)Math.round((Double)sum_gain*100),total_hold_days);
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
	
	
	public static void cleanGainResultFromDB(String symbol, String exchange) {

		Connection conn=getConnection();
		PreparedStatement pstmt;
		try {
			pstmt = conn.prepareStatement("delete from sum_gain where symbol=? and exchange=?");
			pstmt.setString(1, symbol);
			pstmt.setString(2, exchange);
			pstmt.execute();
			/*
			pstmt = conn.prepareStatement("delete from detail_gain where  symbol=? and exchange=?");
			pstmt.setString(1, symbol);
			pstmt.setString(2, exchange);
			pstmt.execute();
			*/
			
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
	
	public static void saveDetailGainToDB(Connection conn,PreparedStatement pstmt,int count,String symbol,String exchange,int hold_days,
			Date buydate,Date selldate,
			int gain_percent,
			double sellprice,double buyprice,double sellmacd,double buymacd,String strategy){


		try {
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
			pstmt.addBatch();
			if (count%1000==0) {
				pstmt.executeBatch();
			}
			
			
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
			PreparedStatement pstmt=conn.prepareStatement("select distinct symbol, exchange from hist_price where barsize='15 mins' and symbol  not in ('AMD') and symbol in (select symbol from stock where category='SP500') ");
			ResultSet rs= pstmt.executeQuery();
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
		if (buy==null || sell==null) return "";
		StringBuilder sb=new StringBuilder();
		sb.append("");
		if (buy.contains(BUY_STRATEGY.BUY_ON_RED)) {
			sb.append("变红买入 ");
		}
		if (buy.contains(BUY_STRATEGY.BUY_ON_SHORTER_GREEN)) {
			sb.append("绿柱变短买入 ");
		}
		if (buy.contains(BUY_STRATEGY.BUY_ON_LONGER_RED)){
			sb.append("红柱变长买入 ");
		}
		if (sell.contains(SELL_STRATEGY.SELL_ON_GREEN)) {
			sb.append("变绿卖出 ");
		}
		if (sell.contains(SELL_STRATEGY.SELL_ON_SHORTER_RED)) {
			sb.append("红柱变短卖出 ");
		}
		if (sell.contains(SELL_STRATEGY.SELL_ON_LONGER_GREEN)) {
			sb.append("绿柱变长卖出 ");
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
	
	public static Price[] getPriceFromDB(String symbol, String exchange,Date start,Date end,String barsize) {
		Connection conn=getConnection();
		List<Price> list=new ArrayList<Price>();
		PreparedStatement pstmt;
		try {
			pstmt = conn.prepareStatement("select * from hist_price where symbol=? and exchange=? and barsize=? and trade_date between ? and ? order by trade_date");
			pstmt.setString(1, symbol);
			pstmt.setString(2, exchange);
			pstmt.setString(3, barsize);
			pstmt.setTimestamp(4, new java.sql.Timestamp(start.getTime()));
			pstmt.setTimestamp(5, new java.sql.Timestamp(end.getTime()));
			ResultSet rs=pstmt.executeQuery();
			
			while (rs.next()) {
				Price p=new Price();
				int index=1;
				p.setSymbol(rs.getString(index++));
				p.setExchange(rs.getString(index++));
				p.setBarsize(rs.getString(index++));
				p.setTrade_date(rs.getTimestamp(index++));
				p.setOpen(rs.getDouble(index++));
				p.setHigh(rs.getDouble(index++));
				p.setLow(rs.getDouble(index++));
				p.setClose(rs.getDouble(index++));
				p.setVolumn(rs.getInt(index++));
				Date d=p.getTrade_date();
				d.setTime(d.getTime()+60*1000);
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
	
	
	public static double getBarSize(int i,Price[] prices) {
		Price p=prices[i];
		return Math.abs(p.getOpen()-p.getClose())/Math.abs(p.getHigh()-p.getLow()) ;

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
