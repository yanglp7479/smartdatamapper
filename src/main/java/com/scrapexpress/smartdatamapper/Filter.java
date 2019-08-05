package com.scrapexpress.smartdatamapper;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public abstract class Filter extends Operation{
	
	private static Logger logger= Logger.getLogger(Filter.class);

	public static boolean match(List<Filter> filters, Object srcValue){
		if(filters == null){
			return true;
		}
		
		for(Filter f :filters){
			if(f != null && !f.match(srcValue)){
				return false;
			}
		}
		
		return true;
	}
	
	
	protected String filterFieldName;
	
	protected LinkedList<Filter> filtersChain = new LinkedList<Filter>();
	
	public Filter(){
		
	}

	public Filter(String filterFieldName){
		this.filterFieldName = filterFieldName;
	}
	public abstract  boolean match(Object srcValue) ;
	
	protected Object execOperation(Object srcValue){
		
		return null;
		
	}
	
	public Filter addFilter(Filter filter){
		if(filter != null){
			filtersChain.add(filter);
		}
		return this;
	}
	
	public String getFilterFieldName(){
		return filterFieldName;
	}
	public void setFilterFieldName(String filterFieldName) {
		this.filterFieldName = filterFieldName;
		
	}
	
	public static Filter not(Filter filter){
		Filter notFilter =  new Filter(){

			@Override
			public boolean match(Object srcValue) {
				
				for(Filter filter : filtersChain){
					filter.setObjectDataMapper(this.objectDataMapper)
						.setFromFieldName(this.fromFieldName)
						.setTargetFieldName(this.targetFieldName)
						.setTargetObject(this.targetObject);
					
					if(filter.match(srcValue)){
						return false;
					}
						
				}
				
				return true;
			}	 
		 };
		 
		 notFilter.setFilterFieldName(filter.getFilterFieldName());		 
		 notFilter.addFilter(filter);
		 
		 return notFilter;
		
	}
	
	public static Filter assertNotBlank( ){
		
		return new Filter() {
			
			@Override
			public boolean match(Object srcValue) {
				return srcValue != null && StringUtils.isNotBlank( String.valueOf(srcValue)) ;
				
			}
		};
	}
	
	public static Filter assertNotBlank( Var var){
		
		return new Filter() {
			
			@Override
			public boolean match(Object srcValue) {
				Object varValue = null;
				try {
					varValue =  objectDataMapper.getFieldValue(objectDataMapper.srcObject, var.name(), objectDataMapper.fromFieldNameType);
					
				} catch (IllegalArgumentException | IllegalAccessException
						| NoSuchFieldException e) {
					logger.error(e);
				}
				return varValue != null && StringUtils.isNotBlank( String.valueOf(varValue)) ;
				
			}
		};
	}
	
	public static Filter assertEquals(Object expectedValue){
		return new AssertEquals(  expectedValue);
	}
	
	public static Filter assertEquals(String attachedFromFieldPath, Object expectedValue){
		return new AssertEquals(attachedFromFieldPath, expectedValue);
	}
	
	public static Filter assertEquals(Var var, Object expectedValue){
		return new AssertEquals(var, expectedValue);
	}
	
	public static Filter assertEquals(String attachedFromFieldPath, Var var, Object expectedValue){
		return new AssertEquals(attachedFromFieldPath, var, expectedValue);
	}
	
	
	public static abstract class Assert extends Filter{
		protected LinkedList<Var> params = new LinkedList<>();
		protected Object expectedValue;

		protected Collection<Var> getParams() {
			return params;
		}

		protected Object getExpectedValue() {
			return expectedValue;
		}


		protected void setExpectedValue(Object expectedValue) {
			this.expectedValue = expectedValue;
		}


		protected Assert(String attachedFromFieldPath,Object expectedValue){
			super(attachedFromFieldPath);
			this.expectedValue = expectedValue;
			
		}
	}
	
	public static class AssertEquals extends  Assert {
		
		public AssertEquals( Object expectedValue){
			this("",expectedValue);
		}
		
		public AssertEquals(String attachedFromFieldPath, Object expectedValue){
			this(attachedFromFieldPath, null, expectedValue);
		}
		
		public AssertEquals( Var var, Object expectedValue){
			this("", var, expectedValue);
		}

		public AssertEquals(String attachedFromFieldPath, Var var, Object expectedValue){
			super(attachedFromFieldPath,expectedValue);
			if(var != null){
				params.addAll(Arrays.asList(var));
			}
		
		}
		
		@Override
		public boolean match(Object srcValue) {
			
			Object realValue = srcValue;
			
			if(! params.isEmpty()){
			
				try {
					realValue = objectDataMapper.getFieldValue(objectDataMapper.srcObject, params.get(0).name(), objectDataMapper.fromFieldNameType);
				} catch (IllegalArgumentException | IllegalAccessException
						| NoSuchFieldException e) {
					logger.error(e);
				}
				
			}
			
			return (realValue == null && expectedValue == null)
					|| (realValue != null && realValue.equals(expectedValue));
			
		}
	};
	
	
	
	
}