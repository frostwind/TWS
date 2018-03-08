package test;

import com.ib.client.Contract;

public class Request {
	int id;
	Contract contract;
	String formatted;
	String duration;
	String barSize;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public Contract getContract() {
		return contract;
	}
	public void setContract(Contract contract) {
		this.contract = contract;
	}
	public String getFormatted() {
		return formatted;
	}
	public void setFormatted(String formatted) {
		this.formatted = formatted;
	}
	public String getDuration() {
		return duration;
	}
	public void setDuration(String duration) {
		this.duration = duration;
	}
	public String getBarSize() {
		return barSize;
	}
	public void setBarSize(String barSize) {
		this.barSize = barSize;
	}
	
}
