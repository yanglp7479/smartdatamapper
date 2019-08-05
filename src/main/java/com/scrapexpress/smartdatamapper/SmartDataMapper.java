package com.scrapexpress.smartdatamapper;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.scrapexpress.smartdatamapper.Operation.Constant;
import com.scrapexpress.smartdatamapper.Operation.Var;

public class SmartDataMapper {
	
	public static enum FieldNameType{FIELDNAME,JSONPROPERTY,XMLPROPERTY,MAPPROPERTY};
	
	private static Logger logger= Logger.getLogger(SmartDataMapper.class);
	
	public static SmartDataMapper build(Object srcObject,Object targetObject){
		return new SmartDataMapper( srcObject, targetObject);
	}
	
	
	protected Object srcObject;
	private Map<String,List<String>> fromToFieldNamesMap;
	private Map<String,List<Object>> toFieldAssignmentsMap = new HashMap<String, List<Object>>();
	private Object targetObject;
	protected FieldNameType fromFieldNameType = FieldNameType.FIELDNAME;
	protected FieldNameType toFieldNameType = FieldNameType.FIELDNAME;
	
	private Map<Class<?>,Map<String, Field>> classFieldAnnotationsMap = new HashMap<>();
	private String latestFromFieldPath = null;
	private String latestToFieldPath = null;
	private String currentToFieldName= "";
	private String currentFromFieldName= "";
	
	private Map<String, List<Operation>> operationsMap = new HashMap<>();
	private Map<String, List<Filter>> filtersMap = new HashMap<>();
	private Map<String, Class<?>> toFieldPathDataTypesMap = new HashMap<>();
	private Set<String> toFieldPathsInAppendElementMode = new HashSet<String>();
	
	protected SmartDataMapper(Object srcObject,Object targetObject){
		
		this.srcObject = srcObject;
		this.targetObject = targetObject;
		
	}
	
	
	public SmartDataMapper(Object srcObject, Map<String,List<String>> srcToTagetFieldNamesMap,Object targetObject){
		this.srcObject = srcObject;
		this.fromToFieldNamesMap = srcToTagetFieldNamesMap;
		
		
		this.targetObject = targetObject;
	}
	
	
	
	public FieldNameType getFromFieldNameType() {
		return fromFieldNameType;
	}


	public FieldNameType getToFieldNameType() {
		return toFieldNameType;
	}


	final public SmartDataMapper setFromFieldNameType(FieldNameType fromFieldNameType){
		this.fromFieldNameType  = fromFieldNameType;
		
		
		return this;
	}
	
	final public SmartDataMapper setToFieldNameType(FieldNameType toFieldNameType){
		this.toFieldNameType  = toFieldNameType;
		
		return this;
	}
	

	
	final public SmartDataMapper assignValueToField(Object valueOrVar, String toFieldName){
		
		List<Object> objs = toFieldAssignmentsMap.get(toFieldName);
		if(objs == null){
			objs = new LinkedList<Object>();
			toFieldAssignmentsMap.put(toFieldName, objs);
		}
		
		objs.add(valueOrVar);
		
		return this;
		
	}
	
	final public SmartDataMapper addFieldsPair(Var value ,String toFieldName){
		
		
		return assignValueToField(value, toFieldName);
		
	}
	
	final public SmartDataMapper addFieldsPair(String fromFieldname ,String toFieldName){
		if(fromToFieldNamesMap == null){
			fromToFieldNamesMap = new HashMap<>();
		}
		List<String> toFields = fromToFieldNamesMap.get(fromFieldname);
		if(toFields == null){
			toFields =  new LinkedList<String>();
			fromToFieldNamesMap.put(fromFieldname, toFields);
			
		}
		
		toFields.add(toFieldName);
		
		currentToFieldName = toFieldName;
		currentFromFieldName= fromFieldname;
		
		return this;
		
	}
	
	final public SmartDataMapper andPairsWith(String toFieldName){
		if(StringUtils.isEmpty(currentFromFieldName )){
			return this;
		}
		
		return addFieldsPair(currentFromFieldName, toFieldName);
		
	}
	
	final public SmartDataMapper setToFieldPathDataType( Class<?> type){
		
		setToFieldPathDataType(currentToFieldName, type);
		
		return this;
	}
	
	final public SmartDataMapper setToFieldPathDataType(String toFieldPath, Class<?> type){
		
		setToFieldPathDataType(toFieldPath, type, false);
		
		return this;
	}
	
	final public SmartDataMapper setToFieldPathDataType(String toFieldPath, Class<?> type, boolean isAppendElementMode){
		
		if(toFieldPath != null && type != null){
			toFieldPathDataTypesMap.put(currentFromFieldName + ":" + toFieldPath, type);
		}
		
		if(isAppendElementMode){
			setToFieldPathAppendElementMode(toFieldPath);
		}
		
		return this;
	}
	
	final public SmartDataMapper setToFieldPathAppendElementMode(){
		
		
		setToFieldPathAppendElementMode(currentToFieldName);
		
		
		return this;
	}
	
	final public SmartDataMapper setToFieldPathAppendElementMode(String toFieldPath){
		
		if(toFieldPath != null ){
			toFieldPathsInAppendElementMode.add(currentFromFieldName + ":" + toFieldPath);
		}
		
		return this;
	}
	
	
	
	final public SmartDataMapper applyOperation(Operation... ops){
		for(Operation op : ops){
			if(op instanceof Filter){
				if(StringUtils.isEmpty(((Filter)op).getFilterFieldName())){
					
					((Filter)op).setFilterFieldName(currentFromFieldName);
					
				}
				
				if(!StringUtils.isEmpty(currentToFieldName ) ){
					String key = ((Filter)op).getFilterFieldName() + ":"+currentToFieldName;
					List<Filter> existings = filtersMap.get(key);
					if(existings == null){
						existings = new LinkedList<>();
						filtersMap.put(key, existings);
					}
					existings.add((Filter) op);
					
				}
				
			}else if(!StringUtils.isEmpty(currentFromFieldName ) && !StringUtils.isEmpty(currentToFieldName )){
				String key = currentFromFieldName+":"+currentToFieldName;
				List<Operation> existings = operationsMap.get(key);
				if(existings == null){
					existings = new LinkedList<Operation>();
					operationsMap.put(key, existings);
				}
				existings.add(op);
				
			}
			
			op.setObjectDataMapper(this);
		}
		
		return this;
		
	}
	
	/**
	 * 
	 */
	public void map(){
		
		//execute the mapping rules depending on the pair of from and to fields
		if(fromToFieldNamesMap != null && fromToFieldNamesMap.size() > 0){
			
			Set<Entry<String,List<String>>> entrySet = fromToFieldNamesMap.entrySet();
			for(Entry<String,List<String>> entry : entrySet){
				String fromFieldName = entry.getKey();
				List<String> targetFieldNames = entry.getValue();
				
				currentFromFieldName = fromFieldName;
				
				if(targetFieldNames!=null){
					for(String targetfieldName : targetFieldNames){
						try {
							
							latestFromFieldPath = null;
							latestToFieldPath = null;
							
							currentToFieldName = targetfieldName;
							mapFieldValue(srcObject, fromFieldName, targetObject, targetObject.getClass());
						
			
						} catch (NoSuchFieldException | SecurityException
								| IllegalArgumentException | IllegalAccessException | InstantiationException e) {
							logger.error(e);
						}					
					}		
				}		
			}	
		}
		
		//execute the assignment operations to target fields
		for(Entry< String , List<Object> > fieldops : toFieldAssignmentsMap.entrySet()){
			String toFieldName = fieldops.getKey();
			List<Object> ops = fieldops.getValue();
			if(toFieldName != null && ops != null){
				for(Object op : ops){
					try {
						if(op instanceof Constant){
							setFieldValue(targetObject, targetObject.getClass(), toFieldName, ((Constant) op).value());
						}else if(op instanceof Var){
							
							Object v = getFieldValue(srcObject, ((Var)op).name(), fromFieldNameType);
							setFieldValue(targetObject, targetObject.getClass(), toFieldName, v);
							
						}else {
							
							setFieldValue(targetObject, targetObject.getClass(), toFieldName, op);
						}
					} catch (IllegalArgumentException | IllegalAccessException
							| NoSuchFieldException | InstantiationException e) {
						logger.error(e);
					}
				}	
			}	
		}
		
	}
	
	private Map<String,Field> extractXmlPropertyAnnotations(Class<?> objClass ){
		
		//init 
		
		Map<String,Field> pas = new HashMap<String, Field>();
		 while(objClass != null){
			 
			 for(Field f : objClass.getDeclaredFields()){
				String name = null;
				
				XmlElement xe = f.getAnnotation(XmlElement.class);
				if(xe != null){
					name  = xe.name();
				}else{
					XmlAttribute xa = f.getAnnotation(XmlAttribute.class);
					if(xa != null){
						name  = xa.name();
					}
				}
				
				name = name == null ? f.getName() : name;
				
				if(name!=null){
					pas.put(name, f);
				}	
			}
			 
			 objClass = objClass.getSuperclass();
		 }
		 return pas;
 
		
	}
	
	private Map<String,Field> extractJsonPropertyAnnotations(Class<?> objClass ){
		
		//init 
		
		Map<String,Field> pas = new HashMap<String, Field>();
		 while(objClass != null){
			 
			 for(Field f : objClass.getDeclaredFields()){
				String name = null;
				
				JsonProperty xe = f.getAnnotation(JsonProperty.class);
				if(xe != null){
					name  = xe.value();
				}
				
				name = name == null ? f.getName() : name;
				
				if(name!=null){
					pas.put(name, f);
				}	
			}
			 
			 objClass = objClass.getSuperclass();
		 }
		 return pas;
 
		
	}
	
	
	
	private FieldWrapper getField(Class<?> objClass, String fieldName, FieldNameType fieldNameType) throws NoSuchFieldException{
		FieldWrapper fieldWrapper = null;
		Field f = null;
		//get current class
		Class<?> currentClass = objClass;
		while(true){
			try {
				if(FieldNameType.MAPPROPERTY.equals(fieldNameType)){
//					if(! objClass.isAssignableFrom(new HashMap<String,Object>().getClass())){
//						throw new NoSuchFieldException(objClass.getName() + " is not Map type!");
//					}
					fieldWrapper = new FieldWrapper(fieldName);
					
					
					break;
					
				}else if(FieldNameType.XMLPROPERTY.equals(fieldNameType) || FieldNameType.JSONPROPERTY.equals(fieldNameType)){
					Map<String,Field> annotationFieldMap = classFieldAnnotationsMap.get(currentClass);
					if(annotationFieldMap == null){
						if(FieldNameType.XMLPROPERTY.equals(fieldNameType)){
							annotationFieldMap = extractXmlPropertyAnnotations(currentClass);
						}else if(FieldNameType.JSONPROPERTY.equals(fieldNameType)){
							annotationFieldMap = extractJsonPropertyAnnotations(currentClass);
							
						} 
						
						classFieldAnnotationsMap.put(objClass,annotationFieldMap);
					}
					
					f= annotationFieldMap.get(fieldName);
					
				}
				
				if(f==null){
					f = currentClass.getDeclaredField(fieldName);
				}
				
				fieldWrapper = new FieldWrapper(f);

				break;
			} catch (NoSuchFieldException e) {
				if(! FieldNameType.MAPPROPERTY.equals(fieldNameType)){
					//if current class has no specified field
					//then get field from its super class
					currentClass = currentClass.getSuperclass();
					if(currentClass == null){
						throw e;
					}	
				}else{
					break;
				}
				
			}	
		}
		
		return fieldWrapper;
		
	}
	
	
	/**
	 * 
	 * @param obj 
	 * @param fieldName , which can be nested field name, such as "person.address.suburb"
	 * @return value of field
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 */
	protected Object getFieldValue(Object obj, String fieldName, FieldNameType fieldNameType) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException {
		String[] iterativeFieldNames = fieldName.split("\\.",2);
		// if current filedName is the "leaf" field without nested fields
		// then get value of the filedname directly
		if(iterativeFieldNames.length == 1){
			
			//get field by fieldname
//			Field f = getField(obj.getClass(), fieldName,fieldNameType);
//			if(f != null){
//				//return field value
//				f.setAccessible(true);
//				return f.get(obj);
//			}else{
//				return null;
//			}	
			
			FieldWrapper fw = getField(obj.getClass(), fieldName,fieldNameType);
			fw.validateFieldName(obj);
			
			return fw.get(obj);
			
		}else{
			// if current fieldname present a "path" field with nested fieldname,example for "person.address.suburb"
			// then get the value of first nested field, in this example, the first field is "person".
			Object memberObj = getFieldValue(obj,iterativeFieldNames[0],fieldNameType);
			if(memberObj != null){
				
				//then get the value of nested field, in this example, the nested field is "address.suburb"
				return getFieldValue(memberObj,iterativeFieldNames[1],fieldNameType);
			}else{
				return null;
			}	
		}	
	}
	
	/**
	 * Please reference the method of setFieldValue(Object obj,Class<?> objClass, String fieldName, Object value, int elelmentIndex)
	 * @param obj
	 * @param objClass
	 * @param fieldName
	 * @param value
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 * @throws InstantiationException
	 */
	protected  Object setFieldValue(Object obj,Class<?> objClass, String fieldName, Object value) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, InstantiationException {
		return setFieldValue(obj, objClass, fieldName, value, 0);
	}
	
	/**
	 * Set field value of an object by field name, field name can be a nested format, such as "person.address.suburb"
	 * Notes:
	 * this method can not support set value to primitivie type field
	 * @param obj, the object declaring first nested field,such as "person", if null, an instance will be created by objClass parameter
	 * 	if obj is collection or array type, obj can not be null.
	 * @param objClass, the class of obj parameter
	 * @param fieldName, can be a nested format, such as "person.address.suburb"
	 * @param value, which will be assigned to the last nested field, such as "suburb"
	 * @param elelmentIndex, if first nested field is Collection or Array type, the option indicate which element will be assigned 
	 * @return the object with new value
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 * @throws InstantiationException
	 */
	protected  Object setFieldValue(Object obj,Class<?> objClass, String fieldName, Object value, int elelmentIndex) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, InstantiationException {
		
		if(value == null){
			return obj;
		}
		//
		if(obj == null){
			if(Collection.class.isAssignableFrom(objClass) || (objClass!=null && objClass.isArray())){
				throw new InstantiationException("Collection or array field can not be null");
			}
			
			obj = objClass.newInstance();
			
		}else if(objClass == null ){
			objClass = obj.getClass();
		}else if(objClass.isInterface() 
				|| objClass.isPrimitive()
				|| !objClass.equals(obj.getClass())){
			throw new IllegalArgumentException("Object class is invalid!");
		}
			
		//first nested field, such as "person"
		String[] iterativeFieldNames = fieldName.split("\\.",2);
		latestToFieldPath = latestToFieldPath == null ? iterativeFieldNames[0] : latestToFieldPath+"."+iterativeFieldNames[0];
		
		FieldWrapper fw = getField(objClass, iterativeFieldNames[0], toFieldNameType) ; 
		
		//if current field name is not nested, without "."
		//then set field value directly
		if(iterativeFieldNames.length == 1 ){
			
			if(value != null && fw.getType().equals(String.class) && !( value instanceof String)){
				value = String.valueOf(value);
				
			}
			obj = fw.set(obj, value);

			 return obj;
		}else{
			//if field name is nested format, such as "person.address.suburb"
			
			//get the value of current field name
			Object fieldOjbectvalue = fw.get(obj);
			
			//if current field is collection type
			if(Collection.class.isAssignableFrom(fw.getType()) || fw.getType().isArray()){
				
				if(fieldOjbectvalue == null){
					fieldOjbectvalue = instantiateCollectionField(obj, fw.getField());
				}
				
				//get generic type class
				Class eclass = getLatestToFieldPathDataType();
				Collection arrayCollection  = null;
				if(fw.getType().isArray()){
				
					eclass = eclass == null ? fieldOjbectvalue.getClass().getComponentType() : eclass ;
					
					arrayCollection  = Arrays.asList((Object[])fieldOjbectvalue);
					arrayCollection = new LinkedList<Object>(arrayCollection);
				}else{
					
					eclass = eclass == null ? (Class) ((ParameterizedType)fw.getGenericType()).getActualTypeArguments()[0] :eclass;
					
					arrayCollection = (Collection)fieldOjbectvalue;
				}
				
				
				Object eobj = null;
	
				//if current collection has element with specified index
				//then assign value to the element
				if(! isAppendElementMode()){
					
					if(elelmentIndex >=0 && arrayCollection.size() > 0){
						int index = 0;
						for(Object e :arrayCollection){
							if(index == elelmentIndex){
								eobj = e;
								elelmentIndex = elelmentIndex > 0 ? elelmentIndex -1 : 0;
								break;
							}
							index ++;
						}
					}
				}
				
				
				if(eobj == null){
					//if the element does not exist 
					//then new instance of element
					eobj  = setFieldValue(null,eclass, iterativeFieldNames[1],value,elelmentIndex);
					//add new element to collection
					arrayCollection.add(eclass.cast(eobj));
				}else{
					//set value to existing element
					setFieldValue(eobj,eclass, iterativeFieldNames[1],value,elelmentIndex);
				}	
				
				if(fw.getType().isArray()){
					obj = fw.set(obj, arrayCollection.toArray((Object[])Array.newInstance(fw.getType().getComponentType(), arrayCollection.size())));

				}
				
			} 
			else if(Map.class.isAssignableFrom(fw.getType()) && !FieldNameType.MAPPROPERTY.equals(toFieldNameType)){
				throw new IllegalAccessException("Can not access map type!");
			}
			else{
				if( fieldOjbectvalue==null){
					Class<?> type = getLatestToFieldPathDataType();
					type = type == null ? fw.getType() : type;
					fieldOjbectvalue = type.newInstance();
				}
				
				obj = fw.set(obj, fieldOjbectvalue);
				setFieldValue(fieldOjbectvalue,fieldOjbectvalue.getClass(),iterativeFieldNames[1],value,elelmentIndex);
			}

			return obj;
			
		}
		
	}
	/**
	 * instantiate a field of Collection type as the rules:
	 * HashSet for Set, ArrayList for others
	 * 
	 * @param obj
	 * @param f
	 * @return 
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Object instantiateCollectionField(Object obj, Field f) throws IllegalArgumentException, IllegalAccessException, InstantiationException{
		
		Object fieldOjbectvalue = null;
		if(Set.class.isAssignableFrom(f.getType())){
			fieldOjbectvalue = new HashSet(Arrays.asList(Array.newInstance((Class) ((ParameterizedType)f.getGenericType()).getActualTypeArguments()[0], 0)));
			f.set(obj, fieldOjbectvalue);
		}else if(Collection.class.isAssignableFrom(f.getType())){
			fieldOjbectvalue = new ArrayList(Arrays.asList(Array.newInstance((Class) ((ParameterizedType)f.getGenericType()).getActualTypeArguments()[0], 0)));
			((Collection)fieldOjbectvalue).clear();
			f.set(obj, fieldOjbectvalue);
		}else{
			throw new InstantiationException("Array type field ["+ f.getName()+"] can not be null");
		}	
		
		return fieldOjbectvalue;
	}
	
	protected void mapFieldValue(Object srcobj, String srcfieldName, Object targetObject, Class<?> targetobjClass) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, InstantiationException {
		 mapFieldValue(srcobj, srcfieldName, targetObject, targetobjClass,0);
	}
	
	/**
	 * Map value from source object field to target object field
	 * @param srcobj
	 * @param srcfieldName
	 * @param targetObject
	 * @param targetobjClass
	 * @param targetElelmentIndex
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws NoSuchFieldException
	 * @throws InstantiationException
	 */
	protected void mapFieldValue(Object srcobj, String srcfieldName, Object targetObject, Class<?> targetobjClass,int targetElelmentIndex) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, InstantiationException {
		
		if(srcobj==null || StringUtils.isEmpty(srcfieldName)){
			
			
			if(currentFromFieldName != null && fromToFieldNamesMap.get(currentFromFieldName) != null){
								
				Object srcValue = srcobj;

				try{
					String opKey = currentFromFieldName+":"+currentToFieldName;
					if(operationsMap.get(opKey) != null){
						for(Operation currentOperation : operationsMap.get(opKey)){
							srcValue = currentOperation.setTargetObject(targetObject)
									.setTargetFieldName(currentToFieldName)
									.setFromFieldName(currentFromFieldName)
									.exec(srcValue);
						}		
					}
					
					targetObject = setFieldValue(targetObject, targetobjClass, currentToFieldName, srcValue,targetElelmentIndex);
					
				}catch(Exception e){
					logger.warn("Ignore to set "+currentToFieldName+" field value with "+srcValue+" because of " + e.getMessage());
				}

			}
			return;
		}
		
		String[] iterativeFieldNames = srcfieldName.split("\\.",2);
		
		latestFromFieldPath = latestFromFieldPath == null ? iterativeFieldNames[0] : latestFromFieldPath+"."+iterativeFieldNames[0];
		List<Filter> filters = getFilters();
		
		//if current field name does not include child
		//set the value of current field to target object field directly
		if(iterativeFieldNames.length == 1){
			FieldWrapper fw = getField(srcobj.getClass(),srcfieldName,fromFieldNameType);
			
			if(fw != null){
				//f.setAccessible(true);
				Object fieldValue = fw.get(srcobj);
				if( ! Filter.match(filters,fieldValue)){
					return;
				}
				//System.out.println(fieldValue);
				mapFieldValue(fieldValue, null, targetObject,targetobjClass,targetElelmentIndex);
	
			}else{
				return ;
			}
			
		}else{
			
			//if current field includes child field, then get the child field value and map it to target object field
			Object memberObj = getFieldValue(srcobj,iterativeFieldNames[0],fromFieldNameType);
			if(memberObj != null 
					&& memberObj instanceof Collection<?>){
				
				int size =( (Collection<?>)memberObj).size();
				//int totalindex = targetElelmentIndex < 0 ? 0 : targetElelmentIndex *size ;
				int totalindex =  targetElelmentIndex *size ;
				for(Object e : (Collection<?>)memberObj){
					if( ! Filter.match(filters,e)){
						continue;
					}
					mapFieldValue(e, iterativeFieldNames[1],targetObject,targetobjClass, totalindex ++ );
					
				}
				
			}else if(memberObj != null 
					&& memberObj.getClass().isArray()){
				
				int size =( (Collection<?>)memberObj).size();
				//int totalindex = targetElelmentIndex < 0 ? 0 : targetElelmentIndex *size ;
				int totalindex = targetElelmentIndex *size ;
				for(Object e : (Object[])memberObj){
					if(! Filter.match(filters,e)){
						continue;
					}
					mapFieldValue(e, iterativeFieldNames[1],targetObject,targetobjClass, totalindex ++ );
					
				}
				
			}else if(memberObj != null 
					&& memberObj instanceof Map<?,?> && !FieldNameType.MAPPROPERTY.equals(fromFieldNameType)){
				
				throw new IllegalAccessException("Can not access map type!");
	
			}
			else{
				if( ! Filter.match(filters,memberObj)){
					return;
				}
				mapFieldValue(memberObj,iterativeFieldNames[1],targetObject,targetobjClass,targetElelmentIndex );
			}

			return;
			
		}
		
	}
	
	protected List<Filter> getFilters( ){
		return filtersMap.get(latestFromFieldPath + ":" +currentToFieldName);
		
	}
	
	protected Class<?> getLatestToFieldPathDataType( ){
		return toFieldPathDataTypesMap.get(currentFromFieldName + ":" +latestToFieldPath);
		
	}
	
	protected Boolean isAppendElementMode( ){
		
		
		return toFieldPathsInAppendElementMode.contains(currentFromFieldName + ":" + latestToFieldPath);
		
	}
	
	public static class FieldWrapper{
		
		
		private Field field;
		
	
		
		private String fieldName;
		
		private boolean isMapField;
		

		public FieldWrapper(Field field) {
			super();
			this.field = field;
			
		}
		

		public FieldWrapper(String fieldName) {
			super();
			this.fieldName = fieldName;
		
			isMapField = true;
		}
		
		public void validateFieldName(Object obj) throws NoSuchFieldException{
			if(isMapField ){
				if( obj == null ||fieldName==null || !((Map<String,Object>) obj).containsKey(fieldName)){
					throw new NoSuchFieldException("fieldName does not exist!" );
				}
			}
		}
		
		public Class<?> getType(){
			if(isMapField){
				return new HashMap<String,Object>().getClass();
			}else if(field != null){
				return field.getType();
			}
			return  Object.class;
		}
		public Type getGenericType(){
			return field.getGenericType();
		}

		public Object set(Object obj, Object fieldValue) throws IllegalArgumentException, IllegalAccessException{
			if(!isMapField){
				if(field != null && obj != null){
					field.setAccessible(true);
					field.set(obj, fieldValue);
				}
				
				return obj;
				
			}else if(!StringUtils.isEmpty(fieldName) ){
				if(obj== null)
					obj = new HashMap<String, Object>();

				
				((Map<String, Object>)obj).put(fieldName, fieldValue);	
				 
				 return obj;
			}else{
				throw new IllegalArgumentException("Invalid fieldname " + fieldName);
			}
		}
		
		public Object get(Object obj) throws IllegalArgumentException, IllegalAccessException{
			if(!isMapField){
				if(field != null && obj != null){
					field.setAccessible(true);
					return field.get(obj);
				}else{
					throw new IllegalArgumentException("Field or dataObject can not be null!");
				}
			} else if(!StringUtils.isEmpty(fieldName) && obj!= null){
				if( !((Map<String, Object>)obj).containsKey(fieldName)){
					((Map<String, Object>)obj).put(fieldName, null);
				}
				
				 return ((Map<String, Object>)obj).get(fieldName);
			} else if(!StringUtils.isEmpty(fieldName) && obj== null){
				return null;
			} else{
				throw new IllegalArgumentException("Invalid fieldname " + fieldName);
			}
		}

		public Field getField() {
			return field;
		}


		public String getFieldName() {
			return fieldName;
		}

		
		public void setField(Field field) {
			this.field = field;
		}


		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}


		
		
	}
	

	
	 
	
	
	


}
