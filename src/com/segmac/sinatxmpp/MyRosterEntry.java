package com.segmac.sinatxmpp;

import java.io.Serializable;
import java.util.Comparator;

public class MyRosterEntry implements Serializable, Comparator<MyRosterEntry>{

	private static final long serialVersionUID = 9197519834536320984L;
	public String username;
	public String xmppAddress;
	public String status; //TODO: 增加状态实时更新功能
	
	public MyRosterEntry(){
		
	}
	
	public MyRosterEntry(String username, String xmppAdress, String status){
		this.username = username;
		this.xmppAddress = xmppAdress;
		this.status = status;
	}
	
	@Override
	public String toString() {
		return username + "[" + status + "]";
	}

	@Override
	public int compare(MyRosterEntry lhs, MyRosterEntry rhs) {
		// 比较方式 4*4 = 16种，除去等于的4种，此处列出12种
		if((lhs.status == "在线" && rhs.status == "忙碌") ||
				(lhs.status == "在线" && rhs.status == "离开") || 
				(lhs.status == "在线" && rhs.status == "离线") || 
				(lhs.status == "忙碌" && rhs.status == "离开") || 
				(lhs.status == "忙碌" && rhs.status == "离线") || 
				(lhs.status == "离开" && rhs.status == "离线")){
			return -1; //小于0则lhs < rhs 排名靠前，反之靠后
		} else if((lhs.status == "离线" && rhs.status == "离开") ||
				(lhs.status == "离线" && rhs.status == "忙碌") || 
				(lhs.status == "离线" && rhs.status == "在线") || 
				(lhs.status == "离开" && rhs.status == "忙碌") || 
				(lhs.status == "离开" && rhs.status == "在线") || 
				(lhs.status == "忙碌" && rhs.status == "在线")){
			return 1;
		}
		return 0;
	}

}
