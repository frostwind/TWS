package test;

public class TotalGain{
	String symbol;
	String exchange;
	boolean delay_one_day;
	String strategyDesc;
	int gain_percent;
	int total_hold_days;
	
	public TotalGain(String symbol,String exchange, boolean delay_one_day, 
			String strategyDesc, int gain_percent,int total_hold_days) {
		this.symbol=symbol;
		this.exchange=exchange;
		this.delay_one_day=delay_one_day;
		this.strategyDesc=strategyDesc;
		this.gain_percent=gain_percent;
		this.total_hold_days=total_hold_days;
	}
}
