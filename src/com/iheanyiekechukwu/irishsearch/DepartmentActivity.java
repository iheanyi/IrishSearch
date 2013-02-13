package com.iheanyiekechukwu.irishsearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;

public class DepartmentActivity extends Activity implements OnItemClickListener {

	private ListView deptListView;

	// Initialize constants of arrays
	private String[] deptNames;
	private String[] deptKeys;
	
	private String deptSelect; 
	private ArrayList<String> deptList;
	private DeptAdapter deptAdapter;	
	private Context context;

	// Constant page for ClassSearchServlet
	private static final String CLASS_PAGE = "https://wlsx-prod.nd.edu/reg/srch/ClassSearchServlet";
	private static final String DESCRIPTION_PAGE = "https://wlsx-prod.nd.edu/reg/srch/ClassSearchServlet?CRN=";
	private static final String DESCRIPTION_PAGE_TERM = "&TERM=201220";
	
	private ProgressDialog prog;
	private ArrayList<Course> courseObjectList;

		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_department);
		Typeface robothin = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
		Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Thin.ttf");
		Typeface bolded = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf");
		//this.getActionBar().setDisplayShowTitleEnabled(false);	    
	    
		if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			int actionBarTitleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
			TextView actionBarTextView = (TextView) findViewById(actionBarTitleId);
			actionBarTextView.setTextColor(Color.WHITE);
	    	actionBarTextView.setTypeface(robothin);
	    
		}
	    
	    LinearLayout layout = (LinearLayout) findViewById(R.id.courseLayout);
	    
	    //layout.addView(adView);
	    
		context = this.getBaseContext();
        
        Resources res = getResources();
        
        deptNames = res.getStringArray(R.array.deptArray);
        deptKeys = res.getStringArray(R.array.keyArray);
        deptListView = (ListView) this.findViewById(R.id.deptListView);

        deptList = new ArrayList<String>(Arrays.asList(res.getStringArray(R.array.deptArray)));
        
        courseObjectList = new ArrayList<Course>();
        
        //deptListView.setTypef
                
        DeptAdapter deptAdapter = new DeptAdapter(this, R.layout.deptitem, deptList);
        
        //deptAdapter = new DeptAdapter(this, android.R.layout.deptitem, deptList);
        deptListView.setAdapter(deptAdapter);
        deptListView.setFastScrollEnabled(true);
        
        Context mCtx = this; // Get current context.
        GoogleAnalytics myInstance = GoogleAnalytics.getInstance(mCtx.getApplicationContext());
        Tracker newTracker = myInstance.getTracker("UA-36709787-3"); // Placeholder tracking ID.
        myInstance.setDefaultTracker(newTracker); // Set newTracker as the default tracker globally.

        deptListView.setOnItemClickListener(this);
	}
	
    public class CustomListItem{
        TextView deptTextView;  // PUBLIC!!!

    }
	
	private class DeptAdapter extends ArrayAdapter<String>{
	    private LayoutInflater inflater;
	    private ArrayList<String> data;
	    private Typeface typeface;
	    private Typeface bolded;
	    
	    private ArrayList<String> depts;
	    
	    
	    

	    public DeptAdapter(Context context, int resource, ArrayList<String> data){

	    	super(context, resource, data);
		    CustomListItem myItem;

		    typeface = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Thin.ttf");
		    bolded = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Regular.ttf");
	   
		
	    }

	    public View getView(int position, View convertView, ViewGroup parent){
	        String myText = getItem(position);    
	        
	        //View view = super.getView(position, convertView, parent);
		    CustomListItem myListItem;
	        if(convertView == null){
	            LayoutInflater inflater = LayoutInflater.from(DepartmentActivity.this);
	            convertView = inflater.inflate(R.layout.deptitem, parent, false);  // we should be able to get the layout from the class
	            myListItem = new CustomListItem();

	            myListItem.deptTextView = (TextView)convertView.findViewById(R.id.text1);
	 
	            
	            convertView.setTag(myListItem);
	        } else{
	        	myListItem = (CustomListItem) convertView.getTag();
	        }
	        
	        //((TextView) convertView.findViewById(R.id.text1)).setTypeface(this.typeface);
	        // Of course you will need to set the same ID in your item list XML layout.

	        myListItem.deptTextView.setText(myText);
	        myListItem.deptTextView.setTypeface(bolded);
	        myListItem.deptTextView.setTextSize(14);
	        
	        


	        return convertView;
	    }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_department, menu);
		return true;
	}

	
	private HttpClient createHttpClient()
    {
    	//Toast.makeText(context, "HTTP Client Created", Toast.LENGTH_SHORT);
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);
        HttpProtocolParams.setUseExpectContinue(params, true);

        SchemeRegistry schReg = new SchemeRegistry();
        schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schReg.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);

        return new DefaultHttpClient(conMgr, params);
    }
    
    private String getPage( String deptKey ) {
    	String pageHTML = "NO HTML FOUND";
        

        try {
        	HttpClient client = createHttpClient();
            HttpPost post = new HttpPost(CLASS_PAGE);
           //BasicCookieStore cookieStore = new BasicCookieStore();
           //((AbstractHttpClient) client).setCookieStore(cookieStore);
           //Toast.makeText(context, "HTTP PAGE Created", Toast.LENGTH_SHORT);

	        List<NameValuePair> pparams = new ArrayList<NameValuePair>();
	        pparams.add(new BasicNameValuePair("TERM", "201220"));
	        pparams.add(new BasicNameValuePair("DIVS", "A"));
	        pparams.add(new BasicNameValuePair("CAMPUS", "M"));
	        pparams.add(new BasicNameValuePair("SUBJ", deptKey));
	        pparams.add(new BasicNameValuePair("ATTR","0ANY"));
	        pparams.add(new BasicNameValuePair("CREDIT", "A"));
	        //pparams.add(new BasicNameValuePair("SUBJ", "CSE"));
	        UrlEncodedFormEntity ent;
			ent = new UrlEncodedFormEntity(pparams);
			//ent.setContentEncoding(HTTP.ISO_8859_1);
			//ent.setContentType("application/x-www-form-urlencoded");
			
	        post.setEntity(ent);
    		HttpResponse rp = client.execute(post);
	        //HttpEntity resEntity = responsePOST.getEntity();  
	        //if (rp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {   
    		if(rp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
	        HttpEntity result = rp.getEntity();
	        pageHTML = EntityUtils.toString(result);
    		}

        }
        catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    	    	
        return pageHTML;

    }
    
    private class HTTPTask extends AsyncTask<String, Integer, String> {

		@Override
		protected String doInBackground(String... params) {	
	    	//Toast.makeText(context, "HTTP Task Entered", Toast.LENGTH_SHORT);

			String deptKey = params[0];
			String html = getPage( deptKey );   
			
			
			return html;
		}
		
		 @Override
		protected void onProgressUpdate(Integer... values) {
				// TODO Auto-generated method stub
				//super.onProgressUpdate(values);
			
				//myProgressBar.setProgress(values[0]);
			 
			 
		}
		
	    protected void onPostExecute(String result) {
	    	 //textView.setText(result);  // CAN access views in main thread
	    	//Toast.makeText(context, "Post Execute", Toast.LENGTH_SHORT).show();

	    	//textView.setText(result);
	    	try {
		    	prog.dismiss();
		    	//prog = null;
		    	processHTML(result);
	    	} catch (Exception e) {
	    		// nothing
	    	}
	    }
    }
    
	
	  @Override
	  public void onStart() {
	    super.onStart();
	    EasyTracker.getInstance().activityStart(this); // Add this method.
	  }

	  @Override
	  public void onStop() {
	    super.onStop();
	    EasyTracker.getInstance().activityStop(this); // Add this method.
	  }


	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		
		courseObjectList.clear();
		String deptKey = deptKeys[arg2];
		this.deptSelect = deptNames[arg2];
		
		prog = ProgressDialog.show(this, "", "Fetching courses for " + deptNames[arg2]);
        HTTPTask myTask = new HTTPTask();
        myTask.execute(deptKey);
		//Intent myIntent = new Intent(this.context, CourseActivity.class);
		
		//myIntent.putExtra("Dept", deptKey);
		
		//this.startActivityForResult(myIntent, 1);

	}
	
	public void processHTML(String html) {
		String crn;
		String title = null;
		String time;
		String instructor;
		String op_spots;
		String max_spots;
		String course;
		String section;
		String credits;
		String location;
		
		Course c =  null;
		Section s = null;
		
		String courseInfo;
		String sectionInfo;
		
		
		if(html.length() < 30000) {
			
			//prog.dismiss();
			
			AlertDialog.Builder b = new AlertDialog.Builder(this);
            b.setTitle(android.R.string.dialog_alert_title);
            
            b.setMessage("No courses were found for the selected department!");
            
            b.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() { 
            	
            		@Override
            		public void onClick(DialogInterface dlg, int arg1) { 
            			
            			dlg.dismiss();
            		}
            });
			
            b.create().show();			
		}
		
		else {
		Document doc = Jsoup.parse(html);
		
		Element table = doc.select("table[id=resulttable]").first();
		Element tablebody = table.select("tbody").get(0);
		String previous = "";
		String previous_title = "";
		
		for(Element row : tablebody.select("tr")) {
			
			if(title != null) {
				previous = title;
			}
			//previous = title;
			Elements cells = row.select("td");
			
			crn = cells.get(7).text();
			title = cells.get(1).text();
			time = cells.get(10).text();
			instructor = cells.get(9).text();
			op_spots = cells.get(5).text();
			max_spots = cells.get(4).text();
			course = cells.get(0).text();
			credits = cells.get(2).text();
			location = cells.get(13).text();
			
			String[] values = course.split(" ");
			
			course = values[0];
			section = values[2];
			
			//sectionInfo = crn + " " + time + " " + " " + instructor + " " + op_spots + "/" + max_spots;
			if(!courseObjectList.isEmpty()) {
				previous_title = courseObjectList.get(courseObjectList.size() - 1).getCourseName();
			}
			
			// If the object is just created or you have encountered a new class
			if(title.equals(previous)) {
				s = new Section(section, op_spots, max_spots, crn, instructor, time, location);
				c.add(s);
				int index = courseObjectList.indexOf(c);
				courseObjectList.set(index, c);			
			}
			
			// Else, it most likely is the same class
			else {
				//courseInfo = course + " - " + title;
				//courseAdapter.add(courseInfo);
				//sectionAdapter.add(sectionInfo);
				c = new Course(course, title, credits);
				s = new Section(section, op_spots, max_spots, crn, instructor, time, location);
				c.add(s);
				courseObjectList.add(c);		
			}
		}
    	//Toast.makeText(context, "Process HTML", Toast.LENGTH_SHORT).show(); 

		//courseListView.setVisibility(View.VISIBLE);
    	
    	// Loop through entire Course Object List Array and populate the ListView
		ArrayList<String> iString = new ArrayList<String>();
    	Iterator<Course> it = courseObjectList.iterator();
    	while(it.hasNext()) {
    		
    		Course currentCourse = it.next();
    		
    		// Get Course Name and number and add it to ArrayAdapter
    		String course_info = currentCourse.getCourseNumber() + " - " + currentCourse.getCourseName();
    		//courseAdapter.add(course_info);
    	}
    	
	//	courseAdapter.notifyDataSetChanged();
    	//progressBar.setVisibility(View.GONE);
    	//courseListView.setVisibility(View.VISIBLE);
    	
    	Intent intent = new Intent(this, CourseActivity.class);
    	
    	ArrayList<Course> courseExtras = new ArrayList<Course>();
    	
    	for (int i = 0; i < courseObjectList.size(); ++i) {
    		courseExtras.add(courseObjectList.get(i));
    	}
    	
    	intent.putExtra("courseList", courseExtras);
    	intent.putExtra("department", this.deptSelect);
    	this.startActivity(intent);
   
	}

	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	        Log.d("CheckStartActivity","onActivityResult and resultCode = "+resultCode);
	        // TODO Auto-generated method stub
  
	        courseObjectList.clear();
	}
	}
