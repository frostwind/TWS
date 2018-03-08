package test;

import java.util.List;

import test.Util.BUY_STRATEGY;
import test.Util.SELL_STRATEGY;

class WorkerThread implements Runnable {
	int bar_num;int stop_loss_step;int revert_trend_step;Double[] ema5;Double[] ema120;Double[] ema60;Double[] ema30;Double[] ema20;Double[] macd;Price[] prices;
			List<BUY_STRATEGY> list_buy ;List<SELL_STRATEGY> list_sell;String symbol;
			String exchange;boolean delay_to_next_day_trade;boolean list_detail;Double[] vol_ema20;
	public WorkerThread(int bar_num,int stop_loss_step, int revert_trend_step,Double[] ema5,Double[] ema120,Double[] ema60,Double[] ema30,Double[] ema20,Double[] macd,Price[] prices,
			List<BUY_STRATEGY> list_buy ,List<SELL_STRATEGY> list_sell,String symbol,
			String exchange,boolean delay_to_next_day_trade,boolean list_detail,Double[] vol_ema20) {
		this.bar_num=bar_num;
		this.stop_loss_step=stop_loss_step;
		this.revert_trend_step=revert_trend_step;
		this.ema5=ema5;
		this.ema120=ema120;
		this.ema60=ema60;
		this.ema30=ema30;
		this.ema20=ema20;
		this.macd=macd;
		this.prices=prices;
		this.list_buy=list_buy;
		this.list_sell=list_sell;
		this.symbol=symbol;
		this.exchange=exchange;
		this.delay_to_next_day_trade=delay_to_next_day_trade;
		this.list_detail=list_detail;
		this.vol_ema20=vol_ema20;
	}
	public void run() {
		synchronized (this) {
			InDayWaveAuto.testStrategy(bar_num,stop_loss_step,revert_trend_step,ema5,ema120,ema60,ema30,ema20,macd,prices,list_buy,list_sell,symbol,exchange,false,list_detail,vol_ema20);
		}
	}
} 
