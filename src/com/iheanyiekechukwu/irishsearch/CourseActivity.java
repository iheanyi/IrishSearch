package com.iheanyiekechukwu.irishsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
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
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.ads.AdView;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class CourseActivity extends Activity implements OnClickListener, OnItemClickListener {

	private ListView courseListView;
	private TextView courseText;
	private ArrayList<String> courseList;
	//private ArrayAdapter<String> courseAdapter;
	private CourseAdapter courseAdapter;
	private ArrayList<String> stringList;
	private AdView adView;
	private TextView descriptionText;
	
	private ProgressDialog prog;
	
	private ArrayList<Section> sectionObjectList;
	private ArrayList<Course> courseObjectList;

	private Context context;
	private Intent i;
	
	private static final String DESCRIPTION_PAGE = "https://wlsx-prod.nd.edu/reg/srch/ClassSearchServlet?CRN=";
	private static final String DESCRIPTION_PAGE_TERM = "&TERM=201220";
	
	private Course selected;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_course);
		
		Typeface robothin = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
		Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Thin.ttf");
		Typeface bolded = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf");
		
		i = this.getIntent();

		
		// Show the Up button in the action bar.
		   if (Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.GINGERBREAD_MR1){
				getActionBar().setDisplayHomeAsUpEnabled(true);
				
			    int actionBarTitleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
			    
			    TextView actionBarTextView = (TextView) findViewById(actionBarTitleId);
			    actionBarTextView.setTextColor(Color.WHITE);
			    actionBarTextView.setTypeface(robothin);
				getActionBar().setTitle(i.getStringExtra("department"));
		   }
	    
	    descriptionText = (TextView) findViewById(R.id.courseDescriptionText);
		ArrayList<Course> list = (ArrayList<Course>) i.getSerializableExtra("courseList");
		
		this.courseObjectList = list;
		
        courseListView = (ListView) this.findViewById(R.id.courseListView);
        
        this.context = this.getBaseContext();
        
        stringList = new ArrayList<String>();
        CourseAdapter courseAdapter = new CourseAdapter(this, R.layout.deptitem, stringList);

        courseListView.setAdapter(courseAdapter);
        courseListView.setFastScrollEnabled(true);
        courseListView.setOnItemClickListener(this);
        
        Context mCtx = this;  // Get current context.
        EasyTracker.getInstance().setContext(mCtx);  // setContext will use mCtx to retrieve the application context.
        GoogleAnalytics myInstance = GoogleAnalytics.getInstance(mCtx.getApplicationContext());
        Tracker myDefault = myInstance.getDefaultTracker();
        // EasyTracker is now ready for use.
        
    	Iterator<Course> it = list.iterator();
    	
    	Resources res = this.getResources();
    	while(it.hasNext()) {
    		
    		Course currentCourse = it.next();
    		
    		// Get Course Name and number and add it to ArrayAdapter
    		String course_info = currentCourse.getCourseNumber() + " - " + currentCourse.getCourseName();
    		//courseAdapter.add(course_info);
    		stringList.add(course_info);
    	}
    	

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_course, menu);
		return true;
	}
	
    public class CustomListItem{
        TextView deptTextView;  // PUBLIC!!!

    }
	
	private class CourseAdapter extends ArrayAdapter<String>{
	    private LayoutInflater inflater;
	    private ArrayList<String> data;
	    private Typeface typeface;
	    private Typeface bolded;
	    
	    private ArrayList<String> courses;
	    
	    
	    public CourseAdapter(Context context, int resource, ArrayList<String> data){

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
	            LayoutInflater inflater = LayoutInflater.from(CourseActivity.this);
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

	public void processDetailHTML(String result) {
		// TODO Auto-generated method stub
		Document doc = Jsoup.parse(result);
		
		Element table = doc.select("table.datadisplaytable").first();
		Element tablebody = table.select("tbody").first();
		
		Element row = tablebody.select("tr").get(1);
		
		Element cell = row.select("td").first();
		
		String description = cell.childNode(6).toString();
	
		
		//String description = cell.text();
		
		//descriptionText.setText(description);
		
		Intent intent = new Intent(this, SectionActivity.class);
		ArrayList<Section> sectionExtras = new ArrayList<Section>();
		
		//for (int i = 0; i < sectionObjectList.size(); ++i) {
			//sectionExtras.add(sectionObjectList.get(i));
		//}
		
		String name = selected.getCourseName();
		String num = selected.getCourseNumber();
		intent.putExtra("sectionList", selected.getSections());
		intent.putExtra("courseList", this.courseObjectList);
		intent.putExtra("description", description);
		//intent.putExtra("course", this.selected.get);
		intent.putExtra("coursenum", num);
		intent.putExtra("coursename", name);
		this.startActivity(intent);
		
		
		
	}

	private class DetailTask extends AsyncTask<String, Integer, String> {
	
		@Override
		protected String doInBackground(String... params) {	
	    	//Toast.makeText(context, "HTTP Task Entered", Toast.LENGTH_SHORT);
	
			String URL = params[0];
			String html = getDetailPage( URL );   
			
			
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
	    	//Toast.makeText(context, "Detail Post Execute", Toast.LENGTH_SHORT).show();
	
	    	//textView.setText(result);	
	    	//descriptionText.setText(result);
	    	try {
		    	prog.dismiss();
		    	prog = null;
	    	} catch (Exception e) {
	    		// nothing
	    	}
			processDetailHTML(result);
	    }
		
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			
			finish();
			//NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub

		Course selectedCourse = courseObjectList.get(arg2);
		
		
		this.selected = selectedCourse;
		
		prog = ProgressDialog.show(this, "", "Fetching sections for " + selected.getCourseName());
		
		// Loop through sections of selected course, creating the list view
		Iterator<Section> it = selectedCourse.getSections().iterator();
		Iterator<Section> is = selectedCourse.getSections().iterator();
		
		if(is.hasNext()) {
			Section firstSection = is.next();
			
			String courseCRN = firstSection.getCrn();
			String fullURL = DESCRIPTION_PAGE + courseCRN + DESCRIPTION_PAGE_TERM;
			
			//Toast.makeText(context, fullURL, Toast.LENGTH_SHORT);
			DetailTask dTask = new DetailTask();
			dTask.execute(fullURL);
		}
		
		while(it.hasNext()) {
			Section currentSection = it.next();
			
			// Get section info and add it to the adapter
			String section_info = "Section " + currentSection.getSectionnum() + ": (" + currentSection.getCrn() + ") " + currentSection.getTime() + " " +  currentSection.getInstructor() + " " + currentSection.getOpen() + "/" + currentSection.getMax();

		}
		
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

	private String getDetailPage( String URLToFetch ) {
		String pageHTML = "";
	
	    try
		{
			HttpClient client = createHttpClient();
			HttpGet request = new HttpGet(URLToFetch);
	
			HttpResponse rp = client.execute(request);
	
			if(rp.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
			{
				HttpEntity result = rp.getEntity();
				pageHTML = EntityUtils.toString(result);    			
			}
		}catch(IOException e){
			e.printStackTrace();
		} 
		    	
	    return pageHTML;
	
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	        Log.d("CheckStartActivity","onActivityResult and resultCode = "+resultCode);
	        // TODO Auto-generated method stub
  
	        sectionObjectList.clear();
	 }

}
