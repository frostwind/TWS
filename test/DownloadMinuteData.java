package test;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import samples.testbed.advisor.FAMethodSamples;
import samples.testbed.contracts.ContractSamples;
import samples.testbed.orders.AvailableAlgoParams;
import samples.testbed.orders.OrderSamples;
import samples.testbed.scanner.ScannerSubscriptionSamples;

import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.EReader;
import com.ib.client.EReaderSignal;
import com.ib.client.ExecutionFilter;
import com.ib.client.Order;
import com.ib.client.Types.FADataType;
import com.ib.controller.AccountSummaryTag;

public class DownloadMinuteData {
	static EWrapperImpl wrapper = null;
	private static PrintStream out=null;
	public static volatile boolean no_data=false;
	public static volatile int error_request_id=-1;
	static Map<Integer,Request> req_map=new ConcurrentHashMap<Integer,Request>();
	static List<Request> err_req_list=Collections.synchronizedList(new LinkedList<Request>()) ;
	
	public static void main(String[] args) throws InterruptedException {
		try {
			out = new PrintStream(new BufferedOutputStream(new FileOutputStream("c://dev//trash.txt")));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.setOut(out);
		
		wrapper = new EWrapperImpl();
		
		final EClientSocket m_client = wrapper.getClient();
		final EReaderSignal m_signal = wrapper.getSignal();
		//! [connect]
//		m_client.eConnect("127.0.0.1", 7496, 0);//host port client_id
		
		m_client.eConnect("127.0.0.1", 4001, 0);//host port client_id
		//! [connect]
		//! [ereader]
		final EReader reader = new EReader(m_client, m_signal);        
        reader.start();        
        new Thread() {
        	public void run() {
        		while (m_client.isConnected()) {
	    			m_signal.waitForSignal();
    				try {
    					reader.processMsgs();
    				} catch (Exception e) {
//    					System.out.println("Exception: "+e.getMessage());
    					e.printStackTrace();
    				}
        		}
        	}
        }.start();
        //! [ereader]
        Thread.sleep(1000);
		
		//orderOperations(wrapper.getClient(), wrapper.getCurrentOrderId());
		//contractOperations(wrapper.getClient());
        //hedgeSample(wrapper.getClient(), wrapper.getCurrentOrderId());
        //testAlgoSamples(wrapper.getClient(), wrapper.getCurrentOrderId());
        //bracketSample(wrapper.getClient(), wrapper.getCurrentOrderId());
		//bulletins(wrapper.getClient());
        //reutersFundamentals(wrapper.getClient());
//        marketDataType(wrapper.getClient());
        historicalDataRequests(wrapper.getClient());
//        placeOrder(wrapper.getClient());
        
        
//        accountOperations(wrapper.getClient());
		
        out.flush();
		out.close();
		Thread.sleep(20000);
		wrapper.closeConnection();
		m_client.eDisconnect();
		
	}
	
	private static void accountInfo(EClientSocket client) {
//		client.reqAccountSummary(arg0, arg1, arg2);
	}
	private static void placeOrder(EClientSocket client) {
		Contract contract = new Contract();
		contract.symbol("MSFT");
		contract.secType("STK");
		contract.currency("USD");
		contract.exchange("SMART");
		//Specify the Primary Exchange attribute to avoid contract ambiguity
		contract.primaryExch("ISLAND");
		
		Order order = new Order();
		order.action("SELL");
		order.orderType("MKT");
		order.totalQuantity(1);
		client.placeOrder(1, contract, OrderSamples.MarketOrder("SELL", 1));

		
	}
	
	
	private static void orderOperations(EClientSocket client, int nextOrderId) throws InterruptedException {
		
		/*** Requesting the next valid id ***/
		//! [reqids]
        //The parameter is always ignored.
        client.reqIds(-1);
        //! [reqids]
        //Thread.sleep(1000);
        /*** Requesting all open orders ***/
        //! [reqallopenorders]
        client.reqAllOpenOrders();
        //! [reqallopenorders]
        //Thread.sleep(1000);
        /*** Taking over orders to be submitted via TWS ***/
        //! [reqautoopenorders]
        client.reqAutoOpenOrders(true);
        //! [reqautoopenorders]
        //Thread.sleep(1000);
        /*** Requesting this API client's orders ***/
        //! [reqopenorders]
        client.reqOpenOrders();
        //! [reqopenorders]
        //Thread.sleep(1000);
		
        /*** Placing/modifying an order - remember to ALWAYS increment the nextValidId after placing an order so it can be used for the next one! ***/
        //! [order_submission]
        client.placeOrder(nextOrderId++, ContractSamples.USStock(), OrderSamples.LimitOrder("SELL", 1, 50));
        //! [order_submission]
        
        //! [faorderoneaccount]
        Order faOrderOneAccount = OrderSamples.MarketOrder("BUY", 100);
        // Specify the Account Number directly
        faOrderOneAccount.account("DU119915");
        client.placeOrder(nextOrderId++, ContractSamples.USStock(), faOrderOneAccount);
        //! [faorderoneaccount]
        
        //! [faordergroupequalquantity]
        Order faOrderGroupEQ = OrderSamples.LimitOrder("SELL", 200, 2000);
        faOrderGroupEQ.faGroup("Group_Equal_Quantity");
        faOrderGroupEQ.faMethod("EqualQuantity");
        client.placeOrder(nextOrderId++, ContractSamples.USStock(), faOrderGroupEQ);
        //! [faordergroupequalquantity]
        
        //! [faordergrouppctchange]
        Order faOrderGroupPC = OrderSamples.MarketOrder("BUY", 0); ;
        // You should not specify any order quantity for PctChange allocation method
        faOrderGroupPC.faGroup("Pct_Change");
        faOrderGroupPC.faMethod("PctChange");
        faOrderGroupPC.faPercentage("100");
        client.placeOrder(nextOrderId++, ContractSamples.EurGbpFx(), faOrderGroupPC);
        //! [faordergrouppctchange]
        
        //! [faorderprofile]
        Order faOrderProfile = OrderSamples.LimitOrder("BUY", 200, 100);
        faOrderProfile.faProfile("Percent_60_40");
        client.placeOrder(nextOrderId++, ContractSamples.EuropeanStock(), faOrderProfile);
        //! [faorderprofile]
        
		//client.placeOrder(nextOrderId++, ContractSamples.USStock(), OrderSamples.PeggedToMarket("BUY", 10, 0.01));
		//client.placeOrder(nextOrderId++, ContractSamples.EurGbpFx(), OrderSamples.MarketOrder("BUY", 10));
        //client.placeOrder(nextOrderId++, ContractSamples.USStock(), OrderSamples.Discretionary("SELL", 1, 45, 0.5));
		
        //! [reqexecutions]
        client.reqExecutions(10001, new ExecutionFilter());
        //! [reqexecutions]
        
        Thread.sleep(10000);
        
    }
	
	private static void OcaSample(EClientSocket client, int nextOrderId) {
		
		//OCA order
		//! [ocasubmit]
		List<Order> OcaOrders = new ArrayList<Order>();
		OcaOrders.add(OrderSamples.LimitOrder("BUY", 1, 10));
		OcaOrders.add(OrderSamples.LimitOrder("BUY", 1, 11));
		OcaOrders.add(OrderSamples.LimitOrder("BUY", 1, 12));
		OcaOrders = OrderSamples.OneCancelsAll("TestOCA_" + nextOrderId, OcaOrders, 2);
		for (Order o : OcaOrders) {
			
			client.placeOrder(nextOrderId++, ContractSamples.USStock(), o);
		}
		//! [ocasubmit]
		
	}
	
	private static void tickDataOperations(EClientSocket client) throws InterruptedException {
		
		/*** Requesting real time market data ***/
		//Thread.sleep(1000);
		//! [reqmktdata]
		client.reqMktData(1001, ContractSamples.StockComboContract(), "", false, null);
		//! [reqmktdata]
		
		//! [reqmktdata_snapshot]
		client.reqMktData(1003, ContractSamples.FutureComboContract(), "", true, null);
		//! [reqmktdata_snapshot]
		
		//! [reqmktdata_genticks]
		//Requesting RTVolume (Time & Sales), shortable and Fundamental Ratios generic ticks
		client.reqMktData(1004, ContractSamples.USStock(), "233,236,258", false, null);
		//! [reqmktdata_genticks]
		//! [reqmktdata_contractnews]
		client.reqMktData(1005, ContractSamples.USStock(), "mdoff,292:BZ", false, null);
		client.reqMktData(1006, ContractSamples.USStock(), "mdoff,292:BT", false, null);
		client.reqMktData(1007, ContractSamples.USStock(), "mdoff,292:FLY", false, null);
		client.reqMktData(1008, ContractSamples.USStock(), "mdoff,292:MT", false, null);
		//! [reqmktdata_contractnews]
		//! [reqmktdata_broadtapenews]
		client.reqMktData(1009, ContractSamples.BTbroadtapeNewsFeed(), "mdoff,292", false, null);
		client.reqMktData(1010, ContractSamples.BZbroadtapeNewsFeed(), "mdoff,292", false, null);
		client.reqMktData(1011, ContractSamples.FLYbroadtapeNewsFeed(), "mdoff,292", false, null);
		client.reqMktData(1012, ContractSamples.MTbroadtapeNewsFeed(), "mdoff,292", false, null);
		//! [reqmktdata_broadtapenews]
		//! [reqoptiondatagenticks]
        //Requesting data for an option contract will return the greek values
        client.reqMktData(1002, ContractSamples.OptionWithLocalSymbol(), "", false, null);
        //! [reqoptiondatagenticks]
		
		Thread.sleep(10000);
		//! [cancelmktdata]
		client.cancelMktData(1001);
		client.cancelMktData(1002);
		client.cancelMktData(1003);
		//! [cancelmktdata]
		
	}
	
	private static void historicalDataRequests(EClientSocket client) throws InterruptedException {
		
		/*** Requesting historical data ***/
//        Contract contract = new Contract();
//		contract.symbol("HFI");
//		contract.secType("FUT");
//		contract.currency("HKD");
//		contract.exchange("SMART");
		//Specify the Primary Exchange attribute to avoid contract ambiguity
//		contract.primaryExch("HKFE");
        
		List<Contract> contracts=new ArrayList<Contract>();
		
		/*
		for (int i=0;i<DayLong.hk_symbols.length;i++) {
			Contract contract = new Contract();
			contract.symbol(DayLong.hk_symbols[i]);
			contract.secType("STK");
			contract.currency("HKD");
			contract.exchange(DayLong.HK_EXCHANGE);
			contracts.add(contract);
		}
		*/
		
		/*
		for (int i=0;i<Util.nasdaq_symbols.length;i++) {
			Contract contract = new Contract();
			contract.symbol(Util.nasdaq_symbols[i]);
			contract.secType("STK");
			contract.currency("USD");
			contract.exchange(Util.NASDAQ);
			contracts.add(contract);
		}
		*/
		
		/*
		for (int i=0;i<Util.us_symbols.length;i++) {
			Contract contract = new Contract();
			contract.symbol(Util.us_symbols[i]);
			contract.secType("STK");
			contract.currency("USD");
			contract.exchange(Util.US_EXCHANGE);
			contracts.add(contract);
		}
		*/
		
		/*
		for (int i=0;i<Util.dow_symbols.length;i++) {
			Contract contract = new Contract();
			contract.symbol(Util.dow_symbols[i]);
			contract.secType("STK");
			contract.currency("USD");
			contract.exchange(Util.US_EXCHANGE);
			contracts.add(contract);
		}
		*/
		String[] sp500=Util.getSP500List();
		for (int i=0;i<sp500.length;i++) {
			Contract contract = new Contract();
			contract.symbol(sp500[i]);
			contract.secType("STK");
			contract.currency("USD");
			contract.exchange(Util.US_EXCHANGE);
			contract.primaryExch("ISLAND");
			contracts.add(contract);
		}
		
		
		
		
//		
//		{
//			Contract contract = new Contract();
//			contract.symbol("HSI");
//			contract.secType("IND");
//			contract.currency("HKD");
//			contract.exchange("HKFE");
//			contracts.add(contract);
//		}
		
		
//		{
//			Contract contract = new Contract();
//			contract.symbol("INDU");
//			contract.secType("IND");
//			contract.currency("USD");
//			contract.exchange("NYSE");
//			contracts.add(contract);
//		}
		
		
		
		System.out.println("===============");
		
		int request_id=1;
		for (Contract contract : contracts) {
			Calendar cal = Calendar.getInstance();
//			Map<Integer,Contract> stock_map=wrapper.getStockMap();
			int unit=4;
			String duration=unit+" W";
			String barSize=Util.BAR_15_MIN;
			int shiftUnit=Calendar.WEEK_OF_YEAR;
			wrapper.barSize=barSize;
//			stock_map.put(request_id, contract);
			Date[] d=DayLong.getMinMaxAvailableHist(contract,barSize);
			if (d[0]==null) {
				for (int i=0;i<12*2;i++) {
					SimpleDateFormat form = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
					String formatted = form.format(cal.getTime());
					Request r=new Request();
					r.setId(request_id);
					r.setContract(contract);
					r.setFormatted(formatted);
					r.setDuration(duration);
					r.setBarSize(barSize);
					req_map.put(request_id, r);
					client.reqHistoricalData(request_id, contract, formatted, duration, barSize, "TRADES", 1, 1, null);
					request_id++;
					
//					out.flush();
				    Thread.sleep(3000);
					cal.add(shiftUnit, -unit);
					
//					if (no_data) {
//						Contract c=req_map.get(error_request_id).getContract();
//						if (c.symbol().equals(contract.symbol()) && c.exchange().equals(contract.exchange())) {
//							no_data=false;
//							break;
//						}
//					}
					
				}
			}else {
				Date max_date=d[1];
				Date min_date=d[0];
//				cal.add(Calendar.DAY_OF_YEAR, -5);
				while (cal.getTime().after(max_date)) {
					SimpleDateFormat form = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
					String formatted = form.format(cal.getTime());
					Request r=new Request();
					r.setId(request_id);
					r.setContract(contract);
					r.setFormatted(formatted);
					r.setDuration(duration);
					r.setBarSize(barSize);
					req_map.put(request_id, r);
					client.reqHistoricalData(request_id, contract, formatted, duration, barSize, "TRADES", 1, 1, null);
					request_id++;
//					out.flush();
				    Thread.sleep(3000);
					cal.add(shiftUnit, -unit);
//					if (no_data) {
//						Contract c=req_map.get(error_request_id).getContract();
//						if (c.symbol().equals(contract.symbol()) && c.exchange().equals(contract.exchange())) {
//							no_data=false;
//							break;
//						}
//					}
				}
				cal=Calendar.getInstance();
				cal.add(Calendar.YEAR, -2);
				Date many_years_ago=cal.getTime();
				cal.setTime(min_date);
				while (many_years_ago.before(cal.getTime())) {
					SimpleDateFormat form = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
					String formatted = form.format(cal.getTime());
					Request r=new Request();
					r.setId(request_id);
					r.setContract(contract);
					r.setFormatted(formatted);
					r.setDuration(duration);
					r.setBarSize(barSize);
					req_map.put(request_id, r);
					client.reqHistoricalData(request_id, contract, formatted, duration, barSize, "TRADES", 1, 1, null);
					request_id++;
//					out.flush();
				    Thread.sleep(2000);
					cal.add(shiftUnit, -unit);
//					if (no_data) {
//						Contract c=req_map.get(error_request_id).getContract();
//						if (c.symbol().equals(contract.symbol()) && c.exchange().equals(contract.exchange())) {
//							no_data=false;
//							break;
//						}
//					}
				}
			}
			while (!err_req_list.isEmpty()) {
				Request r=err_req_list.remove(0);
				if (r!=null && r.getContract()!=null) {
					client.reqHistoricalData(r.getId(), r.getContract(), r.getFormatted(), r.getDuration(), r.getBarSize(), "TRADES", 1, 1, null);
				}
				
				Thread.sleep(5000);
			}
			
//			client.cancelHistoricalData(request_id);
//			request_id++;
		}
		
		
		
		
	}
	
	private static void realTimeBars(EClientSocket client) throws InterruptedException {
		
		/*** Requesting real time bars ***/
        //! [reqrealtimebars]
        client.reqRealTimeBars(3001, ContractSamples.EurGbpFx(), 5, "MIDPOINT", true, null);
        //! [reqrealtimebars]
        Thread.sleep(2000);
        /*** Canceling real time bars ***/
        //! [cancelrealtimebars]
        client.cancelRealTimeBars(3001);
        //! [cancelrealtimebars]
		
	}
	
	private static void marketDepthOperations(EClientSocket client) throws InterruptedException {
		
		/*** Requesting the Deep Book ***/
        //! [reqmarketdepth]
        client.reqMktDepth(2001, ContractSamples.EurGbpFx(), 5, null);
        //! [reqmarketdepth]
        Thread.sleep(2000);
        /*** Canceling the Deep Book request ***/
        //! [cancelmktdepth]
        client.cancelMktDepth(2001);
        //! [cancelmktdepth]
		
	}
	
	private static void accountOperations(EClientSocket client) throws InterruptedException {
		
        //client.reqAccountUpdatesMulti(9002, null, "EUstocks", true);
		//! [reqpositionsmulti]
        client.reqPositionsMulti(9003, "DU74649", "EUstocks");
      //! [reqpositionsmulti]
        Thread.sleep(10000);

        /*** Requesting managed accounts***/
        //! [reqmanagedaccts]
        client.reqManagedAccts();
        //! [reqmanagedaccts]
        /*** Requesting accounts' summary ***/
        Thread.sleep(2000);
        //! [reqaaccountsummary]
        client.reqAccountSummary(9001, "All", "AccountType,NetLiquidation,TotalCashValue,SettledCash,AccruedCash,BuyingPower,EquityWithLoanValue,PreviousEquityWithLoanValue,GrossPositionValue,ReqTEquity,ReqTMargin,SMA,InitMarginReq,MaintMarginReq,AvailableFunds,ExcessLiquidity,Cushion,FullInitMarginReq,FullMaintMarginReq,FullAvailableFunds,FullExcessLiquidity,LookAheadNextChange,LookAheadInitMarginReq ,LookAheadMaintMarginReq,LookAheadAvailableFunds,LookAheadExcessLiquidity,HighestSeverity,DayTradesRemaining,Leverage");
        //! [reqaaccountsummary]
        
      //! [reqaaccountsummaryledger]
        client.reqAccountSummary(9002, "All", "$LEDGER");
        //! [reqaaccountsummaryledger]
        Thread.sleep(2000);
        //! [reqaaccountsummaryledgercurrency]
        client.reqAccountSummary(9003, "All", "$LEDGER:EUR");
        //! [reqaaccountsummaryledgercurrency]
        Thread.sleep(2000);
        //! [reqaaccountsummaryledgerall]
        client.reqAccountSummary(9004, "All", "$LEDGER:ALL");
        //! [reqaaccountsummaryledgerall]
		
		//! [cancelaaccountsummary]
		client.cancelAccountSummary(9001);
		client.cancelAccountSummary(9002);
		client.cancelAccountSummary(9003);
		client.cancelAccountSummary(9004);
		//! [cancelaaccountsummary]
        
        /*** Subscribing to an account's information. Only one at a time! ***/
        Thread.sleep(2000);
        //! [reqaaccountupdates]
        client.reqAccountUpdates(true, "U150462");
        //! [reqaaccountupdates]
		Thread.sleep(2000);
		//! [cancelaaccountupdates]
		client.reqAccountUpdates(false, "U150462");
		//! [cancelaaccountupdates]
		
        //! [reqaaccountupdatesmulti]
        client.reqAccountUpdatesMulti(9002, "U150462", "EUstocks", true);
        //! [reqaaccountupdatesmulti]
        Thread.sleep(2000);
        /*** Requesting all accounts' positions. ***/
        //! [reqpositions]
        client.reqPositions();
        //! [reqpositions]
		Thread.sleep(2000);
		//! [cancelpositions]
		client.cancelPositions();
		//! [cancelpositions]
    }
	
	private static void conditionSamples(EClientSocket client, int nextOrderId) {
		
		//! [order_conditioning_activate]
		Order mkt = OrderSamples.MarketOrder("BUY", 100);
		//Order will become active if conditioning criteria is met
		mkt.conditionsCancelOrder(true);
		mkt.conditions().add(OrderSamples.PriceCondition(208813720, "SMART", 600, false, false));
		mkt.conditions().add(OrderSamples.ExecutionCondition("EUR.USD", "CASH", "IDEALPRO", true));
		mkt.conditions().add(OrderSamples.MarginCondition(30, true, false));
		mkt.conditions().add(OrderSamples.PercentageChangeCondition(15.0, 208813720, "SMART", true, true));
		mkt.conditions().add(OrderSamples.TimeCondition("20160118 23:59:59", true, false));
		mkt.conditions().add(OrderSamples.VolumeCondition(208813720, "SMART", false, 100, true));
		client.placeOrder(nextOrderId++, ContractSamples.EuropeanStock(), mkt);
		//! [order_conditioning_activate]
		
		//Conditions can make the order active or cancel it. Only LMT orders can be conditionally canceled.
		//! [order_conditioning_cancel]
		Order lmt = OrderSamples.LimitOrder("BUY", 100, 20);
		//The active order will be cancelled if conditioning criteria is met
		lmt.conditionsCancelOrder(true);
		lmt.conditions().add(OrderSamples.PriceCondition(208813720, "SMART", 600, false, false));
		client.placeOrder(nextOrderId++, ContractSamples.EuropeanStock(), lmt);
		//! [order_conditioning_cancel]
		
	}
	
	private static void contractOperations(EClientSocket client) {
		
		//! [reqcontractdetails]
		client.reqContractDetails(210, ContractSamples.OptionForQuery());
		//! [reqcontractdetails]
		
	}
	
	private static void contractNewsFeed(EClientSocket client) {
		
		//! [reqcontractdetailsnews]
		client.reqContractDetails(211, ContractSamples.NewsFeedForQuery());
		//! [reqcontractdetailsnews]
		
	}
	
	private static void hedgeSample(EClientSocket client, int nextOrderId) throws InterruptedException {
		
		//F Hedge order
		//! [hedgesubmit]
		//Parent order on a contract which currency differs from your base currency
		Order parent = OrderSamples.LimitOrder("BUY", 100, 10);
		parent.orderId(nextOrderId++);
		//Hedge on the currency conversion
		Order hedge = OrderSamples.MarketFHedge(parent.orderId(), "BUY");
		//Place the parent first...
		client.placeOrder(parent.orderId(), ContractSamples.EuropeanStock(), parent);
		//Then the hedge order
		client.placeOrder(nextOrderId++, ContractSamples.EurGbpFx(), hedge);
		//! [hedgesubmit]
		
	}
	
	private static void testAlgoSamples(EClientSocket client, int nextOrderId) throws InterruptedException {
		
		//! [algo_base_order]
		Order baseOrder = OrderSamples.LimitOrder("BUY", 1000, 1);
		//! [algo_base_order]
		
		//! [arrivalpx]
		AvailableAlgoParams.FillArrivalPriceParams(baseOrder, 0.1, "Aggressive", "09:00:00 CET", "16:00:00 CET", true, true);
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
		//! [arrivalpx]
		
		Thread.sleep(500);
		
		//! [darkice]
		AvailableAlgoParams.FillDarkIceParams(baseOrder, 10, "09:00:00 CET", "16:00:00 CET", true);
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
		//! [darkice]
		
		Thread.sleep(500);
		
		//! [ad]
		AvailableAlgoParams.FillAccumulateDistributeParams(baseOrder, 10, 60, true, true, 1, true, true, "09:00:00 CET", "16:00:00 CET");
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
		//! [ad]
		
		Thread.sleep(500);
		
		//! [twap]
		AvailableAlgoParams.FillTwapParams(baseOrder, "Marketable", "09:00:00 CET", "16:00:00 CET", true);
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
		//! [twap]
		
		Thread.sleep(500);
		
		//! [vwap]
		AvailableAlgoParams.FillVwapParams(baseOrder, 0.2, "09:00:00 CET", "16:00:00 CET", true, true);
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
		//! [vwap]
		
		Thread.sleep(500);
		
		//! [balanceimpactrisk]
		AvailableAlgoParams.FillBalanceImpactRiskParams(baseOrder, 0.1, "Aggressive", true);
		client.placeOrder(nextOrderId++, ContractSamples.USOptionContract(), baseOrder);
		//! [balanceimpactrisk]
		
		Thread.sleep(500);
		
		//! [minimpact]
		AvailableAlgoParams.FillMinImpactParams(baseOrder, 0.3);
		client.placeOrder(nextOrderId++, ContractSamples.USOptionContract(), baseOrder);
		//! [minimpact]
		
		//! [adaptive]
		AvailableAlgoParams.FillAdaptiveParams(baseOrder, "Normal");
		client.placeOrder(nextOrderId++, ContractSamples.USStockAtSmart(), baseOrder);
		//! [adaptive]		
		
	}
	
	private static void bracketSample(EClientSocket client, int nextOrderId) throws InterruptedException {
		
		//BRACKET ORDER
        //! [bracketsubmit]
		List<Order> bracket = OrderSamples.BracketOrder(nextOrderId++, "BUY", 100, 30, 40, 20);
		for(Order o : bracket) {
			client.placeOrder(o.orderId(), ContractSamples.EuropeanStock(), o);
		}
		//! [bracketsubmit]
		
	}
	
	private static void bulletins(EClientSocket client) throws InterruptedException {
		
		//! [reqnewsbulletins]
		client.reqNewsBulletins(true);
		//! [reqnewsbulletins]
		
		Thread.sleep(2000);
		
		//! [cancelnewsbulletins]
		client.cancelNewsBulletins();
		//! [cancelnewsbulletins]
		
	}
	
	private static void reutersFundamentals(EClientSocket client) throws InterruptedException {
		
		//! [reqfundamentaldata]
		client.reqFundamentalData(8001, ContractSamples.USStock(), "ReportsFinSummary");
		//! [reqfundamentaldata]
		
		Thread.sleep(2000);
		
		//! [fundamentalexamples]
		client.reqFundamentalData(8002, ContractSamples.USStock(), "ReportSnapshot"); //for company overview
		client.reqFundamentalData(8003, ContractSamples.USStock(), "ReportRatios"); //for financial ratios
		client.reqFundamentalData(8004, ContractSamples.USStock(), "ReportsFinStatements"); //for financial statements
		client.reqFundamentalData(8005, ContractSamples.USStock(), "RESC"); //for analyst estimates
		client.reqFundamentalData(8006, ContractSamples.USStock(), "CalendarReport"); //for company calendar
		//! [fundamentalexamples]
		
		//! [cancelfundamentaldata]
		client.cancelFundamentalData(8001);
		//! [cancelfundamentaldata]
		
	}
	
	private static void marketScanners(EClientSocket client) throws InterruptedException {
		
		/*** Requesting all available parameters which can be used to build a scanner request ***/
        //! [reqscannerparameters]
        client.reqScannerParameters();
        //! [reqscannerparameters]
        Thread.sleep(2000);

        /*** Triggering a scanner subscription ***/
        //! [reqscannersubscription]
        client.reqScannerSubscription(7001, ScannerSubscriptionSamples.HighOptVolumePCRatioUSIndexes(), null);
        //! [reqscannersubscription]

        Thread.sleep(2000);
        /*** Canceling the scanner subscription ***/
        //! [cancelscannersubscription]
        client.cancelScannerSubscription(7001);
        //! [cancelscannersubscription]
		
	}
	
	private static void financialAdvisorOperations(EClientSocket client) {
		
		/*** Requesting FA information ***/
		//! [requestfaaliases]
		client.requestFA(FADataType.ALIASES.ordinal());
		//! [requestfaaliases]
		
		//! [requestfagroups]
		client.requestFA(FADataType.GROUPS.ordinal());
		//! [requestfagroups]
		
		//! [requestfaprofiles]
		client.requestFA(FADataType.PROFILES.ordinal());
		//! [requestfaprofiles]
		
		/*** Replacing FA information - Fill in with the appropriate XML string. ***/
		//! [replacefaonegroup]
		client.replaceFA(FADataType.GROUPS.ordinal(), FAMethodSamples.FaOneGroup);
		//! [replacefaonegroup]
		
		//! [replacefatwogroups]
		client.replaceFA(FADataType.GROUPS.ordinal(), FAMethodSamples.FaTwoGroups);
		//! [replacefatwogroups]
		
		//! [replacefaoneprofile]
		client.replaceFA(FADataType.PROFILES.ordinal(), FAMethodSamples.FaOneProfile);
		//! [replacefaoneprofile]
		
		//! [replacefatwoprofiles]
		client.replaceFA(FADataType.PROFILES.ordinal(), FAMethodSamples.FaTwoProfiles);
		//! [replacefatwoprofiles]
		
	}
	
	private static void testDisplayGroups(EClientSocket client) throws InterruptedException {
		
		//! [querydisplaygroups]
		client.queryDisplayGroups(9001);
		//! [querydisplaygroups]
		
		Thread.sleep(500);
		
		//! [subscribetogroupevents]
		client.subscribeToGroupEvents(9002, 1);
		//! [subscribetogroupevents]
		
		Thread.sleep(500);
		
		//! [updatedisplaygroup]
		client.updateDisplayGroup(9002, "8314@SMART");
		//! [updatedisplaygroup]
		
		Thread.sleep(500);
		
		//! [subscribefromgroupevents]
		client.unsubscribeFromGroupEvents(9002);
		//! [subscribefromgroupevents]
		
	}
	
	private static void marketDataType(EClientSocket client) {
		
		//! [reqmarketdatatype]
        /*** Switch to live (1) frozen (2) delayed (3) or delayed frozen (4)***/
        client.reqMarketDataType(2);
        //! [reqmarketdatatype]
		
	}
	
	private static void optionsOperations(EClientSocket client) {
		
		//! [reqsecdefoptparams]
		client.reqSecDefOptParams(0, "IBM", "", "STK", 8314);
		//! [reqsecdefoptparams]
		
		//! [calculateimpliedvolatility]
		client.calculateImpliedVolatility(5001, ContractSamples.OptionAtBOX(), 5, 85);
		//! [calculateimpliedvolatility]
		
		//** Canceling implied volatility ***
		client.cancelCalculateImpliedVolatility(5001);
		
		//! [calculateoptionprice]
		client.calculateOptionPrice(5002, ContractSamples.OptionAtBOX(), 0.22, 85);
		//! [calculateoptionprice]
		
		//** Canceling option's price calculation ***
		client.cancelCalculateOptionPrice(5002);
		
		//! [exercise_options]
		//** Exercising options ***
		client.exerciseOptions(5003, ContractSamples.OptionWithTradingClass(), 1, 1, "", 1);
		//! [exercise_options]
	}
	
}
