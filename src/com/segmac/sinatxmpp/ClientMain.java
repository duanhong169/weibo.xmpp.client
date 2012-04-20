package com.segmac.sinatxmpp;

import java.util.ArrayList;
import java.util.Collection;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

public class ClientMain extends Activity {
    private Handler mHandler = new Handler();
    private LoginDialog mDialog;//用于连接聊天服务器的对话框
    private ListView friendsList;//用于选择聊天对象
    private ArrayAdapter<MyRosterEntry> friendsAdapter;
    private Roster roster;
    private static XMPPConnection connection;
    private Button login;
    private Spinner presence;
    private ArrayAdapter<CharSequence> presenceAdapter;
	private String TAG = "xmppchatactivity";
	private String myUsername = "";
	private ProgressDialog mSpinner;
    private ArrayList<String> messages = new ArrayList<String>();//存放所有聊天消息
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        presence = (Spinner)findViewById(R.id.presence);
        presence.setPrompt("设置状态");
        presenceSpinnerSetup();
        //未登录，无状态
        disablePresence();
        friendsList = (ListView) this.findViewById(R.id.friendsList);
        mSpinner = new ProgressDialog(this);
        mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 登录对话框
        mDialog = new LoginDialog(this);

        // 点击按钮弹出登录对话框
        login = (Button) this.findViewById(R.id.login);
        login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	if(connection == null) {
	            	new Thread(new Runnable() {
	    		        public void run() {
							mHandler.post(new Runnable() {
								
								@Override
								public void run() {
									mSpinner.setMessage("连接到XMPP服务器...");
									mSpinner.show();								
								}
							});
			            	mHandler.post(new Runnable() {
			                    public void run() {
			                    	mDialog.show();
			                        mSpinner.dismiss();	
			                    }
			                });
	    		        }
	            	}).start();
            	}else{
            		connection.disconnect();
            		connection = null;
            		login.setText("登录");
            		friendsList.setAdapter(null);
            	}
            }
        });
    }
	
    public void setMessageListener(){
        
        if (connection != null) {
            //注册一个消息接收监听器，用户接收对方发来的消息
            PacketFilter chatMessageFilter = new MessageTypeFilter(Message.Type.chat);
            connection.addPacketListener(new PacketListener() {
                public void processPacket(Packet packet) {
                    System.out.println(packet.toXML());
                	Message message = (Message) packet;
                    if (message.getBody() != null) {
                        String fromName = StringUtils.parseBareAddress(message.getFrom());
                        Log.i("XMPPClient", "Got text [" + message.getBody() + "] from [" + getUsernameByUID(fromName) + "]");
                        messages.add(getUsernameByUID(fromName) + ":" + message.getBody());
                    }
                }
            }, chatMessageFilter);
            
            //注册一个状态接收监听器，用户接收对方发来的消息
            PacketFilter filter = new PacketTypeFilter(Presence.class);
            connection.addPacketListener(new PacketListener() {
                public void processPacket(Packet packet) {
                    //System.out.println(packet.toXML());
                }
            }, filter);
        }
    }
    
    public static void setConnection(XMPPConnection newConnection){
    	connection = newConnection;
    }
    
    public static XMPPConnection getConnection(){
    	return connection;
    }

    //更新已连接的用户名（更新在按钮上）
    public void refreshName(){
		login.setText(myUsername + "\n" + "点击注销登录");
    }

    public void setUsername(String username){
    	myUsername = username;
    }
    
    public String getUsername(){
    	return myUsername;
    }
    
    //在当前的roster中查询标识为UID的用户名（screen name）
    private String getUsernameByUID(String UID){
    	Roster roster = connection.getRoster();
    	RosterEntry rosterEntry = roster.getEntry(UID);
    	return rosterEntry.getName();
    }
    
    public Handler getHandler(){
    	return mHandler;
    }
    
    public void disablePresence(){
    	presence.setVisibility(View.INVISIBLE);
    }
    
    public void enablePresence(){
    	presence.setVisibility(View.VISIBLE);
    }
    
    //设置状态
    private void setPresence(int code){
		if(connection == null) return;
		Presence presence;
    	switch(code){
		case 0:
			presence = new Presence(Presence.Type.available); 
			connection.sendPacket(presence);
			Log.v(TAG, "设置在线");
			break;
		case 1:
			presence = new Presence(Presence.Type.available);
			presence.setMode(Presence.Mode.dnd);
			connection.sendPacket(presence);
			Log.v(TAG, "设置忙碌");
			System.out.println(presence.toXML());
			break;
		case 2:
			presence = new Presence(Presence.Type.available);
			presence.setMode(Presence.Mode.away);
			connection.sendPacket(presence);
			connection.sendPacket(presence);
			Log.v(TAG, "设置离开");
			System.out.println(presence.toXML());
			break;
		case 3:
			Roster roster = connection.getRoster();
			Collection<RosterEntry> entries = roster.getEntries();
			for (RosterEntry entry : entries) {
				presence = new Presence(Presence.Type.unavailable);//每一次必须new Packet
				presence.setPacketID(Packet.ID_NOT_AVAILABLE);//XXX: PacketID不知道对隐身是否有影响
				presence.setFrom(connection.getUser());
				presence.setTo(entry.getUser()); 
				connection.sendPacket(presence);
				System.out.println(presence.toXML());
	    	}
			//向同一用户的其他客户端发送隐身状态
			presence = new Presence(Presence.Type.unavailable);
			presence.setPacketID(Packet.ID_NOT_AVAILABLE);
			presence.setFrom(connection.getUser());
			presence.setTo(StringUtils.parseBareAddress(connection.getUser())); 
			connection.sendPacket(presence);
			Log.v(TAG, "设置隐身");
			break;
		case 4:
			presence = new Presence(Presence.Type.unavailable); 
			connection.sendPacket(presence);
			Log.v(TAG, "设置离线");
			break;
		default: break;
		}
    }
    
    //用户状态列表
    private void presenceSpinnerSetup(){
    	presenceAdapter = ArrayAdapter.createFromResource(this, R.array.presences, android.R.layout.simple_spinner_item);
    	//适配器获得值
    	presenceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	//下拉列表从适配器中读取值
    	presence.setAdapter(presenceAdapter);
    	//下拉列表选定值后响应
    	presence.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				setPresence(arg2);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
    }
    
    public void friendsListSetup(){
    	roster = connection.getRoster();
    	Collection<RosterEntry> entries = roster.getEntries();
    	friendsAdapter = new ArrayAdapter<MyRosterEntry>(this, android.R.layout.simple_list_item_1);
    	for (RosterEntry entry : entries) {
    		//recipientAdapter.add(entry);
    		Presence presence = roster.getPresence(entry.getUser());
    		String status = getCNStatus(presence);
    		MyRosterEntry myEntry = new MyRosterEntry(entry.getName(), entry.getUser(), status);
    		friendsAdapter.add(myEntry);
    	}
    	friendsAdapter.sort(new MyRosterEntry());
    	//下拉列表从适配器中读取值
    	friendsList.setAdapter(friendsAdapter);
    	friendsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				MyRosterEntry talkTo = friendsAdapter.getItem(arg2);
				Intent startChat = new Intent(ClientMain.this, ChatActivity.class);
				Bundle bundle = new Bundle();
				bundle.putSerializable("talkTo", talkTo);
				startChat.putExtras(bundle);
				startActivity(startChat);
			}

		});
        roster.addRosterListener(new RosterListener() {
            // Ignored events public void entriesAdded(Collection<String> addresses) {}
            public void entriesDeleted(Collection<String> addresses) {}
            public void entriesUpdated(Collection<String> addresses) {}
            public void presenceChanged(Presence presence) {
            	//String fromName = StringUtils.parseBareAddress(presence.getFrom());
            	Collection<RosterEntry> entries = roster.getEntries();
            	friendsAdapter = new ArrayAdapter<MyRosterEntry>(ClientMain.this, android.R.layout.simple_list_item_1);
            	for (RosterEntry entry : entries) {
            		//recipientAdapter.add(entry);
            		Presence mypresence = roster.getPresence(entry.getUser());
            		String status = getCNStatus(mypresence);
            		MyRosterEntry myEntry = new MyRosterEntry(entry.getName(), entry.getUser(), status);
            		friendsAdapter.add(myEntry);
            	}
            	friendsAdapter.sort(new MyRosterEntry());
                //更新好友列表
                mHandler.post(new Runnable() {
                    public void run() {
                    	friendsList.setAdapter(friendsAdapter);
                    }
                });
            	//System.out.println("Presence changed: " + presence.getFrom() + " " + presence);
            }
			@Override
			public void entriesAdded(Collection<String> arg0) {
			}
        });
    }
    
//    public void refreshFriendsList(){
//    	
//    }
    
	public String getCNStatus(Presence status){
		String cNStatus = "";
		if(status.isAvailable()){
			if(status.isAway()){
				if(status.getMode() == Presence.Mode.away){
					cNStatus = "离开";
				}else if(status.getMode() == Presence.Mode.dnd){
					cNStatus = "忙碌";
				}
			}else{
				cNStatus = "在线";
			}
		}else{
			cNStatus = "离线";
		}
		return cNStatus;
	}
    
//	@Override
//	public void onBackPressed() {
//		if(connection != null){
//			connection.disconnect();
//		}
//		super.onBackPressed();
//	}
}