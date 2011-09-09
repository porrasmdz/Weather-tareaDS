package weather.jinn.com;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.developerworks.android.Message;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class Weather extends Activity {
    /** Called when the activity is first created. */
	String APPNAME = "Weather"; 
	RSSWeatherObject wo = new RSSWeatherObject();
	List<Message> myMessageList;
	public String location_String;
	
	HtmlCleaner cleaner = new HtmlCleaner();
	CleanerProperties props = cleaner.getProperties();
	

    private static final int[] fcIconImageView = {
        R.id.fcIconImageView0,
        R.id.fcIconImageView1,
        R.id.fcIconImageView2,
        R.id.fcIconImageView3,
        R.id.fcIconImageView4,
        R.id.fcIconImageView5
    };
    
    private static final int[] fcDateTextViews = {
        R.id.fcDateTextView0,
        R.id.fcDateTextView1,
        R.id.fcDateTextView2,
        R.id.fcDateTextView3,
        R.id.fcDateTextView4,
        R.id.fcDateTextView5
    };
    
    private static final int[] fcDescTextViews = {
        R.id.fcDescTextView0,
        R.id.fcDescTextView1,
        R.id.fcDescTextView2,
        R.id.fcDescTextView3,
        R.id.fcDescTextView4,
        R.id.fcDescTextView5,
    };

    private static final int[] fcTempTextViews = {
        R.id.fcTempTextView0,
        R.id.fcTempTextView1,
        R.id.fcTempTextView2,
        R.id.fcTempTextView3,
        R.id.fcTempTextView4,
        R.id.fcTempTextView5,
    };
    
    private static final int[] fcWindTextViews = {
        R.id.fcWindTextView0,
        R.id.fcWindTextView1,
        R.id.fcWindTextView2,
        R.id.fcWindTextView3,
        R.id.fcWindTextView4,
        R.id.fcWindTextView5
    };
    
    private static final int[] fcPrecipTextViews = {
        R.id.fcPrecipTextView0,
        R.id.fcPrecipTextView1,
        R.id.fcPrecipTextView2,
        R.id.fcPrecipTextView3,
        R.id.fcPrecipTextView4,
        R.id.fcPrecipTextView5
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
              // Called when a new location is found by the network location provider.
              // makeUseOfNewLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
          };

        // Register the listener with the Location Manager to receive location updates
        // locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        // locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        
        // hard coded for now
        location_String = "02130";
		
		URL weatherURL = null;
		try {
			weatherURL = new URL("http", "www.rssweather.com", "/zipcode/" + location_String + "/rss.php");
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		HttpURLConnection urlConnection = null;
		try {
			urlConnection = (HttpURLConnection) weatherURL.openConnection();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			SAXParser parser = factory.newSAXParser();
			RSSHandler handler = new RSSHandler();
			parser.parse(urlConnection.getInputStream(), handler);
			myMessageList = handler.getMessages();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		for (Message msg: myMessageList){
			// check category info to see how to process the information
			if (msg.getCategory().equals("Current Conditions")){
				// pick apart the content field from wo

				props.setPruneTags("dt, img");
				props.setTranslateSpecialEntities(true);
				
				TagNode node = cleaner.clean(msg.getContent());
				
				wo.setTemperature(node.findElementByAttValue("class", "temp", true, false).getText().toString().replaceAll("&#176;", "�"));
				wo.setDescription(node.findElementByAttValue("class", "sky", true, false).getText().toString());
				wo.setHumidity(node.findElementByAttValue("id", "humidity", true, false).getText().toString());
				wo.setWind_speed(node.findElementByAttValue("id", "windspeed", true, false).getText().toString() + 
						"-" + node.findElementByAttValue("id", "gusts", true, false).getText().toString());
				wo.setWind_direction(node.findElementByAttValue("id", "winddir", true, false).getText().toString().replaceAll("&#176;", "�"));
				wo.setBarometer(node.findElementByAttValue("id", "pressure", true, false).getText().toString());
				wo.setDewpoint(node.findElementByAttValue("id", "dewpoint", true, false).getText().toString().replaceAll("&#176;", "�"));
				wo.setHeat_index(node.findElementByAttValue("id", "heatindex", true, false).getText().toString().replaceAll("&#176;", "�"));
				wo.setWind_chill(node.findElementByAttValue("id", "windchill", true, false).getText().toString().replaceAll("&#176;", "�"));
				wo.setVisibility(node.findElementByAttValue("id", "visibility", true, false).getText().toString());
			}
			
			else if (msg.getCategory().equals("Weather Forecast")){
				WeatherForecast wf = new WeatherForecast(); 
				String[] descriptionArray; 
				String temp_pattern, wind_pattern, 
					becoming_wind_pattern, rain_chance_pattern;

				wf.setTitle(msg.getTitle());
				
				
				temp_pattern = "";
				wind_pattern = " \\in the\\ "; 
				becoming_wind_pattern = "\\becoming\\ d+ \\mph in the\\";
				rain_chance_pattern = "\\Q Chance of rain\\E d+ \\Qpercent.\\E";
				
				descriptionArray = msg.getDescription().split(".");
				
				for (String s : descriptionArray){
					Log.i(APPNAME, s);
					if (s.matches(wind_pattern)){
						wf.setCurrentWind(s);
					}
					else if (s.matches(becoming_wind_pattern)){
						wf.setFutureWind(s);
					}
					else if (s.matches(rain_chance_pattern)){
						wf.setPrecip_chance(s);
					}
					else {
						wf.setDescription(wf.getDescription() + " " + s);
					}
				}
				wo.wf.add(wf);
			}
		}
		
		// the inside of this needs to be replaced by a function in WeatherDisplayController:updateDisplay()
		if (wo != null) {
			// setTxt
			TextView PubDateTextView, LocationTextView;
			TextView TempDescTextView, HumidBaroTextView, DewpointTextView, WindSpdDirTextView, 
			 	WindChillHeatIndxTextView, VisibilityTextView;
			ImageView ConditionIconImageView;

			SimpleDateFormat CurrentDateTimeDisplayFormat = new SimpleDateFormat("EEEE yyyy-MM-dd HH:mm");
			
			PubDateTextView = (TextView) findViewById(R.id.PubDateTextView);
			//PubDateTextView.setText();
			
			LocationTextView = (TextView) findViewById(R.id.LocationTextView);
			//LocationTextView.setText();
			
			TempDescTextView = (TextView) findViewById(R.id.ccTempDescTextView);
			TempDescTextView.setText((CharSequence)wo.getTemperature() + " " + wo.getDescription());
			
			HumidBaroTextView = (TextView) findViewById(R.id.ccHumidBaroTextView);
			HumidBaroTextView.setText("Humidity: " + (CharSequence)wo.getHumidity() + " Pressure: " + wo.getBarometer());
			
			DewpointTextView = (TextView) findViewById(R.id.ccDewpointTextView);
			DewpointTextView.setText("Dewpoint: " + (CharSequence)wo.getDewpoint());
			
			WindSpdDirTextView = (TextView) findViewById(R.id.ccWindSpdDirTextView);
			WindSpdDirTextView.setText("Wind: " + (CharSequence)wo.getWind_speed() + " " + wo.getWind_direction());
			
			WindChillHeatIndxTextView = (TextView) findViewById(R.id.ccWindChillHeatIndxTextView);
			WindChillHeatIndxTextView.setText("Wind Chill: " + (CharSequence)wo.getWind_chill() + " Heat Index: " + wo.getHeat_index());
			
			VisibilityTextView = (TextView) findViewById(R.id.ccVisibilityTextView);
			VisibilityTextView.setText("Visibility: " + (CharSequence)wo.getVisibility());
			
			/* URL ConditionIconImageViewURL = null;
			try {
				ConditionIconImageViewURL = new URL("http://www.google.com" + wo.getIcon());
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			
			ImageView fcIconImageView[] = new ImageView[6];
			TextView fcDateTextView[] = new TextView[6];
			TextView fcDescTextView[] = new TextView[6];
			TextView fcTempTextView[] = new TextView[6];
			TextView fcWindTextView[] = new TextView[6];
			TextView fcPrecipTextView[] = new TextView[6];
			
			for (int x = 0; x < wo.wf.size(); x++){
				/* try {
					ForecastIconImageViewURL = new URL ("http://www.google.com" + wo.wf.get(x).getIconImgPath());
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} */
				
				fcDateTextView[x] = (TextView) findViewById(fcDateTextViews[x]);
				fcDateTextView[x].setText(wo.wf.get(x).getTitle());
				
				fcDescTextView[x] = (TextView) findViewById(fcDescTextViews[x]);
				fcDescTextView[x].setText(wo.wf.get(x).getDescription());
				
				fcTempTextView[x] = (TextView) findViewById(fcTempTextViews[x]);
				fcTempTextView[x].setText(wo.wf.get(x).getTemperature());
				
				fcWindTextView[x] = (TextView) findViewById(fcWindTextViews[x]);
				fcWindTextView[x].setText(wo.wf.get(x).getCurrentWind() + " " + wo.wf.get(x).getFutureWind());
				
				fcPrecipTextView[x] = (TextView) findViewById(fcPrecipTextViews[x]);
				fcPrecipTextView[x].setText(wo.wf.get(x).getPrecip_chance());
			}
		}
		else{
			Log.v("WeatherAPP", "wo empty again...");
		}
    }
    
	public Bitmap getImagefromURL(URL imageURL){
        Bitmap bm = null; 
        try { 
            URLConnection conn = imageURL.openConnection(); 
            conn.connect(); 
            InputStream is = conn.getInputStream(); 
            BufferedInputStream bis = new BufferedInputStream(is); 
            bm = BitmapFactory.decodeStream(bis); 
            bis.close(); 
            is.close(); 
       } catch (IOException e) { 
           Log.e("WeatherAPP", "Error getting bitmap", e); 
       } 
       return bm; 
    }
}