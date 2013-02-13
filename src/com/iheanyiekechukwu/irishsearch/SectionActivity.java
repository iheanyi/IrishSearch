package com.iheanyiekechukwu.irishsearch;


import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.Tracker;

@SuppressLint("NewApi")
public class SectionActivity extends Activity {
	
	private Intent i;
	private Context context;
	private SectionAdapter sectionAdapter;
	private ArrayList<Section> sectionObjectList;
	private TextView descriptionView;
	private TextView nameView;
	private String description;
	
	private ArrayList<Course> courseObjectList;
	private ProgressDialog prog;

	
	private ListView sectionListView;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_section);
	   
		i = this.getIntent();
	    String course = i.getStringExtra("coursenum");
	    Typeface robothin = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
		Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Thin.ttf");
		Typeface bolded = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf");

		// Show the Up button in the action bar.
		if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {	
			getActionBar().setDisplayHomeAsUpEnabled(true);
		    getActionBar().setTitle(course);
		    int actionBarTitleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
		    TextView actionBarTextView = (TextView) findViewById(actionBarTitleId);
		    actionBarTextView.setTextColor(Color.WHITE);
		    actionBarTextView.setTypeface(robothin);
		   
		}

		
		
		
		
		// Show the Up button in the action bar.
		
	    
	    
	    descriptionView = (TextView) this.findViewById(R.id.courseDescriptionText);
	    nameView = (TextView) this.findViewById(R.id.nameText);
	    

	    
	    
	    String name = i.getStringExtra("coursename");
	    
	    ArrayList<Section> list = (ArrayList<Section>) i.getSerializableExtra("sectionList");
	    ArrayList<Course> cList = (ArrayList<Course>) i.getSerializableExtra("courseList");
	    
	    this.courseObjectList = cList;
	    this.description = i.getStringExtra("description");
	    
	    
	    descriptionView.setText(this.description);
	    descriptionView.setTypeface(robothin);
	    descriptionView.setTextSize(14);
	    
	    nameView.setText(name);
	    nameView.setTypeface(robothin);
	    //nameView.setTextSize(16);
	    
	    sectionObjectList = list;
	    
	    sectionListView = (ListView) this.findViewById(R.id.sectionListView);
	    
        SectionAdapter sectionAdapter = new SectionAdapter(this, R.layout.sectitem, sectionObjectList);

	    sectionListView.setAdapter(sectionAdapter);
        sectionListView.setFastScrollEnabled(true);
        
        Context mCtx = this;  // Get current context.
        EasyTracker.getInstance().setContext(mCtx);  // setContext will use mCtx to retrieve the application context.
        GoogleAnalytics myInstance = GoogleAnalytics.getInstance(mCtx.getApplicationContext());
        Tracker myDefault = myInstance.getDefaultTracker();
        // EasyTracker is now ready for use.
        //sectionListView.setOnItemClickListener(this);
	    
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.activity_section, menu);
		return true;
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
			//i.putExtra("courseList", this.courseObjectList);
			//NavUtils.navigateUpFromSameTask(this);
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ( keyCode == KeyEvent.KEYCODE_BACK ) {
	        //Log.d(TAG, "MENU pressed");
	    	i.putExtra("courseList", courseObjectList);
	    	finish();
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
    public class CustomListItem{
        TextView sectView;  // PUBLIC!!!
        TextView descView;

    }

	private class SectionAdapter extends ArrayAdapter<Section>{
	    private LayoutInflater inflater;
	    private ArrayList<String> data;
	    private Typeface typeface;
	    private Typeface bolded;
	    private Typeface robothin;
	    
	    private ArrayList<String> courses;
	    
	    
	    public SectionAdapter(Context context, int resource, ArrayList<Section> data){
	
	    	super(context, resource, data);
		    CustomListItem myItem;
	
		    typeface = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Thin.ttf");
		    bolded = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Regular.ttf");
			robothin = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");

		
	    }
	
	    public View getView(int position, View convertView, ViewGroup parent){
	        Section mySection = getItem(position);    
	        
	        //View view = super.getView(position, convertView, parent);
		    CustomListItem myListItem;
	        if(convertView == null){
	            LayoutInflater inflater = LayoutInflater.from(SectionActivity.this);
	            convertView = inflater.inflate(R.layout.sectitem, parent, false);  // we should be able to get the layout from the class
	            myListItem = new CustomListItem();
	
	            //myListItem.deptTextView = (TextView)convertView.findViewById(R.id.text1);
	            
	            myListItem.sectView = (TextView) convertView.findViewById(R.id.sectView);
	            myListItem.descView = (TextView) convertView.findViewById(R.id.descView);
	
	            
	            convertView.setTag(myListItem);
	        } else{
	        	myListItem = (CustomListItem) convertView.getTag();
	        }
	        
	        //((TextView) convertView.findViewById(R.id.text1)).setTypeface(this.typeface);
	        // Of course you will need to set the same ID in your item list XML layout.
	
	        String sectText = "Section " + mySection.getSectionnum() + " \u00B7 " + "(" + mySection.getOpen() + "/" + mySection.getMax() + ")";
	        String descText = mySection.getTime() + " \u00B7 " + mySection.getInstructor() + " \u00B7 " +  mySection.getLocation() + " \u00B7 " + mySection.getCrn();
	        myListItem.descView.setText(descText);
	        myListItem.descView.setTypeface(robothin);
	        myListItem.descView.setTextSize(12);
	        
	        myListItem.sectView.setText(sectText);
	        myListItem.sectView.setTypeface(robothin);
	        myListItem.sectView.setTextSize(12);
	        
	        return convertView;
	    }
	}

}
