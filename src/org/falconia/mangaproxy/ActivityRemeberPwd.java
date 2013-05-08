/*author: conowen
 * date: 2012.4.2
 * 
 */
package org.falconia.mangaproxy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.os.StrictMode;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import org.falconia.mangaproxy.ActivityGenreList.IntentHandler;
import org.falconia.mangaproxy.data.Site;
import org.falconia.mangaproxy.plugin.Plugins;
import org.falconia.mangaproxyex.R;

public class ActivityRemeberPwd extends Activity {
	public final static class IntentHandler {

		private static final String BUNDLE_KEY_SITE_ID = "BUNDLE_KEY_SITE_ID";

		private static Intent getIntent(Context context, int siteId) {
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_KEY_SITE_ID, siteId);
			Intent i = new Intent(context, ActivityRemeberPwd.class);
			i.putExtras(bundle);
			return i;
		}

		protected static int getSiteId(ActivityRemeberPwd activity) {
			return activity.getIntent().getExtras().getInt(BUNDLE_KEY_SITE_ID);
		}

		public static void startActivityRemeberPwd(Context context, int siteId) {
			context.startActivity(getIntent(context, siteId));
		}

	}
	AutoCompleteTextView cardNumAuto;
	EditText passwordET;
	Button logBT;
	Button returnBT;

	CheckBox savePasswordCB;
	SharedPreferences sp;
	String cardNumStr;
	String passwordStr;
	private Site mSite;
	private int mSiteId;
	
	public int getSiteId() {
		return mSiteId;
	}
	
	public String getSiteName() {
		return mSite.getName();
	}

	String getSiteDisplayname() {
		return mSite.getDisplayname();
	}
	
	private void startLoginThread() {
		new Thread(downloadRun).start();  
	}
	
	private void showSccuessMessage(){
		Toast.makeText(this, "登陆成功，正在获取用户数据……", Toast.LENGTH_SHORT).show();
	}
	
	private void showFailedMessage(){
		Toast.makeText(this, "密码错误，请重新输入", Toast.LENGTH_SHORT).show();
	}
	
	private void startActivityGenreList(){
		if (mSite.hasGenreList()) {
			ActivityGenreList.IntentHandler.startActivityGenreList(ActivityRemeberPwd.this, mSiteId);
		} else {
			ActivityMangaList.IntentHandler.startActivityAllMangaList(ActivityRemeberPwd.this, mSiteId);
		}
		finish();
	}
	
	/**
	 * 下载线程
	 */
	Runnable downloadRun = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
			Looper.prepare();
			cardNumStr = cardNumAuto.getText().toString();
			passwordStr = passwordET.getText().toString();
			
			
			if (Plugins.getPlugin(getSiteId()).login(cardNumStr,passwordStr))
			{
				if (savePasswordCB.isChecked()) {// 登陆成功才保存密码
					sp.edit().putString(cardNumStr, "").commit();
				}
				AppUtils.logV(this, "login Sucessfully!");
				showSccuessMessage();
				startActivityGenreList();

			
			}
			else
			{
				showFailedMessage();
				AppUtils.logV(this, "login failed!");
			}
		}
	};
	
    

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mSiteId = IntentHandler.getSiteId(this);
		mSite = Site.get(mSiteId);

		setContentView(R.layout.activity_login);
		cardNumAuto = (AutoCompleteTextView) findViewById(R.id.cardNumAuto);
		passwordET = (EditText) findViewById(R.id.passwordET);
		logBT = (Button) findViewById(R.id.logBT);
		returnBT = (Button) findViewById(R.id.returnBT);
		sp = this.getSharedPreferences("usernameFile", MODE_PRIVATE);
		savePasswordCB = (CheckBox) findViewById(R.id.savePasswordCB);
		savePasswordCB.setChecked(true);// 默认为记住密码
		cardNumAuto.setThreshold(1);// 输入1个字母就开始自动提示
		passwordET.setInputType(InputType.TYPE_CLASS_TEXT
				| InputType.TYPE_TEXT_VARIATION_PASSWORD);
		// 隐藏密码为InputType.TYPE_TEXT_VARIATION_PASSWORD，也就是0x81
		// 显示密码为InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD，也就是0x91
		if (Plugins.getPlugin(getSiteId()).needLogin()== false || Plugins.getPlugin(getSiteId()).getCookies().length()>0)
		{
			if (mSite.hasGenreList()) {
				ActivityGenreList.IntentHandler.startActivityGenreList(ActivityRemeberPwd.this, mSiteId);
			} else {
				ActivityMangaList.IntentHandler.startActivityAllMangaList(ActivityRemeberPwd.this, mSiteId);
			}
			this.finish();
		}

		
		cardNumAuto.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				String[] allUserName = new String[sp.getAll().size()];// sp.getAll().size()返回的是有多少个键值对
				allUserName = sp.getAll().keySet().toArray(new String[0]);
				// sp.getAll()返回一张hash map
				// keySet()得到的是a set of the keys.
				// hash map是由key-value组成的

				ArrayAdapter<String> adapter = new ArrayAdapter<String>(
						ActivityRemeberPwd.this,
						android.R.layout.simple_dropdown_item_1line,
						allUserName);

				cardNumAuto.setAdapter(adapter);// 设置数据适配器

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				passwordET.setText(sp.getString(cardNumAuto.getText()
						.toString(), ""));// 自动输入密码

			}
		});

		// 返回
		returnBT.setOnClickListener(new OnClickListener() {
           
			@Override
			public void onClick(View v) {
				
              finish();
			}
		});
		
		
		
		
		
		
		// 登陆
		logBT.setOnClickListener(new OnClickListener() {


			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
                
               startLoginThread();

			}
		});

	}

}