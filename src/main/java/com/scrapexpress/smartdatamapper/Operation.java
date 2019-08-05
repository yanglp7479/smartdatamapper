package com.scrapexpress.smartdatamapper;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.scrapexpress.smartdatamapper.helper.DateUtils;

public abstract class Operation{
	
	private static Logger logger= Logger.getLogger(Operation.class);

	protected SmartDataMapper objectDataMapper;
	protected Object targetObject;
	protected String targetFieldName;
	protected String fromFieldName;
	
	public Operation setTargetObject(Object targetObject){
		this.targetObject = targetObject;
		return this;
	}
	public Operation setObjectDataMapper(SmartDataMapper objectDataMapper){
		this.objectDataMapper = objectDataMapper;
		return this;
	}
	public Operation setTargetFieldName(String fname){
		this.targetFieldName = fname;
		return this;
	}
	public Operation setFromFieldName(String fromFieldName){
		this.fromFieldName = fromFieldName;
		return this;
	}
	
	
	final public Object exec(Object srcValue){
		
		Object value = execOperation(srcValue);
		
		
		return value;
		
	}
	
	protected abstract Object execOperation(Object srcValue);
	
	
	/**
	 * choose one of not null values from source field value and specified variable values
	 * 
	 * @param vars
	 * @return
	 */
	public static Operation orAnyNotNullValue( Var...vars){
		return  new OrAnyNotNullValue(vars);
	}
	
	/**
	 * concat the source field value and  specified variable values with specified separator and assign to target field
	 * 
	 * @param separator
	 * @param vars
	 * @return
	 */
	public static Operation concat( String separator, Var...vars){
		return  new Concat( separator, vars);
	}
	
	/**
	 * join all of element values of  collection or array source field  with specified splitter and assign to target field 
	 * 
	 * @param splitter
	 * @return 
	 */
	public static Operation join( String splitter){
		return new Join( splitter);
	}
	
	/**
	 * append source field value to the existing value of target field with specified splitter
	 * 
	 * @param splitter
	 * @return
	 */
	public static Operation appendWithSplitter( String splitter){
		return new AppendWithSplitter(splitter);
	}
	
	/**
	 * convert the specified source field value to specified value and assign it to target field.
	 * if source field value does not equal specified value, then convert to default value
	 * @param from
	 * @param to
	 * @param defaultValue
	 * @return
	 */
	public static Operation convert(Object from, Object to, Object defaultValue){
		
		return new ValueConverter(null, defaultValue).addRule(from, to);
		
	}
	
	/**
	 * convert source field value according to the specified mapping and assign to target field
	 * if source field value is not mapped, then convert to default value
	 * @param conversions
	 * @param defaultValue
	 * @return
	 */
	public static Operation convert(Map<String, String> conversions, Object defaultValue){
		
		Map<Object, Object> cm = new HashMap<Object, Object>();
		if(conversions != null)
			cm.putAll(conversions);
		
		return new ValueConverter(cm, defaultValue);
		
	}
	
	/**
	 * convert the source field value to lower case
	 * 
	 * @return
	 */
	public static Operation toLowerCase(){
		return new Operation(){

			@Override
			protected Object execOperation(Object srcValue) {
				return srcValue == null ? srcValue : String.valueOf(srcValue).toLowerCase();
			}
		};
			 
	}

	public static Operation formatUTCDatetime(String datetimeFormat,String timezone){
		
		return new Calculator(){

			@Override
			protected Object execOperation(Object srcValue) {
				
				if(srcValue!= null && datetimeFormat != null ){
					if( srcValue instanceof String){
						return DateUtils.convertUTCDateTimeStringToTimeZoneDateTimeString((String) srcValue, timezone,datetimeFormat);
					}else if(srcValue instanceof Date){
						return DateUtils.convertDateToTimeZoneDateTimeString((Date)srcValue, timezone, datetimeFormat);
					}else if(srcValue instanceof Calendar){
						return DateUtils.convertDateToTimeZoneDateTimeString(((Calendar)srcValue).getTime(), timezone, datetimeFormat);
					}
					
				}
				
				return null;
			}
			
		};
		
		
		
	}
	 
	
	
	public static class Var{
		private String name;
		
		public static  Var of(String name ){
			Var v = new Var();
			v.name = name;
			
			return v;
			
		}
		
		public String name(){
			return name;
		}
	}
	
	public static class Constant extends Var{
		private Object value;
		
		public static  Constant valueOf(Object value ){
			Constant c = new Constant();
			c.value = value;
			
			return c;
			
		}
		
		public Object value(){
			return value;
		}
	}

	
	/**
	 * 
	 * @author andy
	 *
	 */
	public static abstract class Calculator extends Operation{
		
		protected LinkedList<Var> params = new LinkedList<Var>();
		
		protected Calculator(Var...vars){
			params.addAll(Arrays.asList(vars));
		}
		
	}

	
	/**
	 * Choose one of not null values from current source field value and specified variables
	 * 
	 * @author andy
	 *
	 */
	public static class OrAnyNotNullValue extends Calculator{
		
		public OrAnyNotNullValue(Var...vars) {
			super(vars);
		}
		
		@Override
		protected Object execOperation(Object srcValue) {
			if(srcValue != null){
				return srcValue;
			}
			for(Var p : params){
				if(p instanceof Constant && ((Constant)p).value() != null){
					return  ((Constant)p).value();
				}else{
					try {
						
						Object v = objectDataMapper.getFieldValue(objectDataMapper.srcObject, p.name(), objectDataMapper.fromFieldNameType);
						if(v != null){
							return v;
						}
					} catch (IllegalArgumentException | IllegalAccessException
							| NoSuchFieldException e) {
						
						e.printStackTrace();
					}
				}			
			}
			
			return null;
		}	
	}
	
	
	/**
	 * Concat current source field value and specified variable values to string with specified separator
	 * 
	 * a variable can be a Constant type or a field of source object
	 * 
	 * @author andy
	 *
	 */
	public static class Concat extends Calculator{
		
		
		
		private String separator ;
		
		private Concat(String separator, Var...vars){
			super(vars);
			this.separator = separator == null ? "" : separator;
			
		}
	
		@Override
		protected Object execOperation(Object srcValue) {
			
			String result = srcValue == null ? "" : String.valueOf(srcValue);
			
			for(Var p : params){
				if(p instanceof Constant && ((Constant)p).value() != null){
					result += separator + ((Constant)p).value();
				}else{
					try {
						
						Object v = objectDataMapper.getFieldValue(objectDataMapper.srcObject, p.name(), objectDataMapper.fromFieldNameType);
						if(v != null){
							result += separator+String.valueOf(v);
						}
					} catch (IllegalArgumentException | IllegalAccessException
							| NoSuchFieldException e) {
						
						e.printStackTrace();
					}
				}				
			}
			
			return result;
		}	
	}
	
	
	
	/**
	 * Join the elements of current source field to string with specified separator if it is collection or array 
	 * 
	 * @author andy
	 *
	 */
	public static class Join extends Operation{
		
		
		private String splitter ;
		
		private Join(String splitter){
			
			this.splitter = splitter == null ? "" : splitter;
			
		}
	
		@Override
		protected Object execOperation(Object srcValue) {
			
			if(srcValue == null){
				return null;
			}
			
			if(srcValue instanceof Collection ){
				return StringUtils.join((Collection<?>)srcValue,splitter);
			}else if(srcValue.getClass().isArray()){
				return StringUtils.join((Object[])srcValue,splitter);
			}else{
				logger.error("Invalid collection or array type of object " + srcValue.getClass());
				return null;
			}			
		}	
	}
	
	
	
	
	public static class AppendWithSplitter extends Operation{
		private String splitter ;
		public AppendWithSplitter(String splitter){
			this.splitter = splitter;
		}
		
		@Override
		public Object execOperation(Object srcValue) {
			return execAppendOperation(srcValue);
		}
	
		protected Object execAppendOperation(Object srcValue) {
			Object appended = null;
			if(!StringUtils.isEmpty( this.targetFieldName ) && this.objectDataMapper != null){
				try {
					
					appended = objectDataMapper.getFieldValue(targetObject, targetFieldName, objectDataMapper.toFieldNameType);
					
					
					if(srcValue != null && !StringUtils.isEmpty( String.valueOf(srcValue))){
						if(appended == null){
							appended =  String.valueOf(srcValue);
						}else{
							appended = String.valueOf(appended) +  splitter + String.valueOf(srcValue);
						}
						
					}
					
					
				} catch (IllegalArgumentException | IllegalAccessException
						| NoSuchFieldException e) {
					logger.warn(e);
				}
			}
			return appended;	
		}
		
	};
	
	/**
	 * convert current source field value according to conversion rules based on map structure
	 * 
	 * @author andy
	 *
	 */
	public static class ValueConverter extends Operation {
		
		private Map<Object, Object> conversions = new HashMap<Object, Object>();
		private Object defaultValue ;
		
		
		
		final public ValueConverter addRule(Object from , Object to){
			conversions.put(from, to);
			return this;
		}
		
		final public ValueConverter setDefaultValue(Object defaultValue){
			this.defaultValue = defaultValue;
			return this;
		}
		
		private ValueConverter(Map<Object, Object> conversions, Object defaultValue){
			if(conversions != null){
				this.conversions = conversions;
			}
			this.defaultValue = defaultValue;
			
		}
	
		@Override
		protected Object execOperation(Object srcValue) {
			Object v = null;
			if(srcValue != null )
				v = conversions.get(srcValue);
			
			v = v == null ? defaultValue : v;
			
			return v == null ? srcValue : v;
				
		}	
	};
	

	

}
