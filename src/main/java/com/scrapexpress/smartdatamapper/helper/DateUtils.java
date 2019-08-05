package com.scrapexpress.smartdatamapper.helper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;

public class DateUtils {
	
	 public static String convertUTCDateTimeStringToTimeZoneDateTimeString(String srcDateTimeString, String timeZone, String format){
	    	if(srcDateTimeString == null ){
	    		return null;
	    	}
	    	Date date = convertDateStringToDate(srcDateTimeString, "yyyy-MM-dd'T'HH:mm:ss", "UTC");
	    	
	    	DateFormat df = new SimpleDateFormat(format);
	    	if(!StringUtils.isEmpty( timeZone )){
	        	df.setTimeZone(TimeZone.getTimeZone(timeZone));
	    	}
	    	

	    	return df.format(date);
	    	
	}
	
	 public static String convertUTCDateTimeStringToTimeZoneDateTimeString(String srcDateTimeString, String srcformat, String timeZone, String format){
	    	if(srcDateTimeString == null ){
	    		return null;
	    	}
	    	Date date = convertDateStringToDate(srcDateTimeString, srcformat, "UTC");
	    	
	    	DateFormat df = new SimpleDateFormat(format);
	    	if(!StringUtils.isEmpty( timeZone )){
	        	df.setTimeZone(TimeZone.getTimeZone(timeZone));
	    	}
	    	

	    	return df.format(date);
	    	
	}
	 
	public static String convertDateToTimeZoneDateTimeString(Date date, String timeZone, String format){
	    	if(date == null ){
	    		return null;
	    	}
	    	
	    	DateFormat df = new SimpleDateFormat(format);
	    	if(!StringUtils.isEmpty( timeZone )){
	        	df.setTimeZone(TimeZone.getTimeZone(timeZone));
	    	}
	    	

	    	return df.format(date);
	    	
	}
	
	
	public static Date convertDateStringToDate(String srcDate, String srcDateFormat,String srcTimeZone) {
    	if(StringUtils.isEmpty( srcDate )){
    		return null;
    	}
        SimpleDateFormat reaFormat = new SimpleDateFormat(srcDateFormat);
       
        TimeZone tz = TimeZone.getTimeZone(srcTimeZone);
        reaFormat.setTimeZone(tz);
       
        try {
            return reaFormat.parse(srcDate);
           
        } catch (Exception e) {
     	   e.printStackTrace();
            return null;
        }
    }
 	

}
