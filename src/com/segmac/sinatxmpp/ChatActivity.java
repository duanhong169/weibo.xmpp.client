package com.segmac.sinatxmpp;

import java.util.ArrayList;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

public class ChatActivity extends Activity {
    private ArrayList<String> messages = new ArrayList<String>();//存放当前聊天消息
    private Handler mHandler = new Handler();
    private XMPPConnection connection;
    private EditText mSendText;
    private ListView mList;
    private MyRosterEntry talkTo;
    
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);
        mSendText = (EditText) this.findViewById(R.id.sendText);
        mList = (ListView) this.findViewById(R.id.listMessages);		
		Bundle bundle = getIntent().getExtras();
		talkTo = (MyRosterEntry)bundle.getSerializable("talkTo");
		setTitle("与[" + talkTo.username + "]聊天中");
		connection = ClientMain.getConnection();
        setListAdapter();
		setMessageListener();
        //点击该按钮发送消息
        Button send = (Button) this.findViewById(R.id.send);
        send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	if(connection == null){
            		Toast toast = Toast.makeText(ChatActivity.this, "请先登录", Toast.LENGTH_SHORT);
            		toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
            		toast.show();
            		return;
            	}
            	String to = talkTo.xmppAddress;
                String text = mSendText.getText().toString();

                Log.i("XMPPClient", "Sending text [" + text + "] to [" + to + "]");
                Message msg = new Message(to, Message.Type.chat);
                msg.setBody(text);
                msg.setFrom(connection.getUser());
                connection.sendPacket(msg);
                messages.add("你对" + talkTo.username + "说：" + text);
                setListAdapter();
                //注意：请不要连续两次发送完全一样的内容，否则新浪的服务器不会接收后一条消息
                mSendText.setText("");
            }
        });
	}
    
    //显示消息
    private void setListAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.multi_line_list_item,
                messages);
        mList.setAdapter(adapter);
    }
    
    private void setMessageListener() {
        if (connection != null) {
            //注册一个消息接收监听器，用户接收对方发来的消息
            PacketFilter chatMessageFilter = new MessageTypeFilter(Message.Type.chat);
            connection.addPacketListener(new PacketListener() {
                public void processPacket(Packet packet) {
                    System.out.println(packet.toXML());
                	Message message = (Message) packet;
                    if (message.getBody() != null) {
                        String fromName = StringUtils.parseBareAddress(message.getFrom());
                        String username = getUsernameByUID(fromName);
                        Log.i("XMPPClient", "Got text [" + message.getBody() + "] from [" + fromName + "]talkTo.xmppAddress" + talkTo.xmppAddress);
                        Log.i("XMPPClient", "username = " + username + " talkTo.username" + talkTo.username);
                        if(!(talkTo.xmppAddress.startsWith(StringUtils.parseName(fromName)))) return;
                        messages.add(username + "对你说：" + message.getBody());
                        //将新接收到的消息添加到消息列表中
                        mHandler.post(new Runnable() {
                            public void run() {
                                setListAdapter();
                            }
                        });
                    }
                }
            }, chatMessageFilter);
        }
    }
    
    //在当前的roster中查询标识为UID的用户名（screen name）
    private String getUsernameByUID(String UID){
    	Roster roster = connection.getRoster();
    	RosterEntry rosterEntry = roster.getEntry(UID);
    	return rosterEntry.getName();
    }
}
