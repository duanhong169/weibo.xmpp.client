package com.segmac.sinatxmpp;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

//该对话框用于输入密码，进一步连接新浪xmpp服务器
public class LoginDialog extends Dialog implements android.view.View.OnClickListener {
    private ClientMain xmppClient;
    private ProgressDialog mSpinner;
    private Handler mHandler = new Handler();
    private Handler xmppClientHandler;

    public LoginDialog(ClientMain xmppClient) {
        super(xmppClient);
        this.xmppClient = xmppClient;
        this.xmppClientHandler = xmppClient.getHandler();
    }

    //初始化对话框
    protected void onStart() {
        super.onStart();
        mSpinner = new ProgressDialog(getContext());
        mSpinner.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mSpinner.setMessage("登录中...");
        setContentView(R.layout.login);
        getWindow().setFlags(4, 4);
        getWindow().setTitleColor(Color.BLACK);
        setTitle("登录");
        Button ok = (Button) findViewById(R.id.ok);
        ok.setOnClickListener(this);
    }

    public void onClick(View v) {
    	       
        new Thread(new Runnable() {
        	public void run() {
				mHandler.post(new Runnable() {
					
					@Override
					public void run() {
						mSpinner.setMessage("正在验证...");
						mSpinner.show();								
					}
				});
            	//新浪微博xmpp服务器信息
            	String host = "xmpp.weibo.com";
                String port = "5222";
                String service = "weibo.com";
                //用户名及密码，其中用户名根据当前连接可以得到，密码则由用户输入
                String username = getEditText(R.id.username);
                String password = getEditText(R.id.password);
                //密码不能为空
                if(password.isEmpty()){
		            mHandler.post(new Runnable() {
						
						@Override
						public void run() {
							mSpinner.dismiss();
				            Toast toast = Toast.makeText(getContext(), "请输入密码", Toast.LENGTH_SHORT);
				    		toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
				    		toast.show();
						}
					});
                	return;
                }
        		//与服务器建立连接
                ConnectionConfiguration connConfig =
	                new ConnectionConfiguration(host, Integer.parseInt(port), service);
                XMPPConnection connection = new XMPPConnection(connConfig);

		        try {
		            connection.connect();
		            Log.i("XMPPClient", "[LoginDialog] Connected to " + connection.getHost());
		        } catch (XMPPException ex) {
		            Log.e("XMPPClient", "[LoginDialog] Failed to connect to " + connection.getHost());
		            Log.e("XMPPClient", ex.toString());
		            ClientMain.setConnection(null);
		        }
		        try {
		            connection.login(username, password);
		            Log.i("XMPPClient", "Logged in as " + connection.getUser());
		
		            //设置状态为在线
		            Presence presence = new Presence(Presence.Type.available);
		            connection.sendPacket(presence);
		            ClientMain.setConnection(connection);
					xmppClient.setUsername(username);
		            mHandler.post(new Runnable() {
						
						@Override
						public void run() {
							mSpinner.dismiss();
							dismiss();
						}
					});
		            //连接成功，更新XMPPChatActivity相关信息
            		xmppClientHandler.post(new Runnable() {
						
						@Override
						public void run() {
							xmppClient.setMessageListener();
				            xmppClient.refreshName();
				            xmppClient.friendsListSetup();
				            xmppClient.enablePresence();
						}
					});
		        } catch (XMPPException ex) {
		            //登录失败
		        	Log.e("XMPPClient", "[LoginDialog] Failed to log in as " + username);
		            Log.e("XMPPClient", ex.toString());
					mHandler.post(new Runnable() {
						
						@Override
						public void run() {
							mSpinner.dismiss();
				            Toast toast = Toast.makeText(getContext(), "密码输入不正确", Toast.LENGTH_SHORT);
				    		toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
				    		toast.show();
						}
					});

		            ClientMain.setConnection(null);
		            return;
		        }

        	}
        }).start();
    }

    //根据EditText的id来获取其中填写的字符串
    private String getEditText(int id) {
        EditText widget = (EditText) this.findViewById(id);
        return widget.getText().toString();
    }
}
