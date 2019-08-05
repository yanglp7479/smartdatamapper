package com.scrapexpress.smartdatamapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SmartDataMapperTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}


	@Test
	public void testMapFieldValue() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, InstantiationException{
		
		DataClass source = new DataClass();
		SubDataClass sub = new SubDataClass();
		source.getDataCollection().add(sub);
		
		sub.setName("x");
		
		sub = new SubDataClass();
		source.getDataCollection().add(sub);
		sub.setName("y");
		
		
		
		DataClass target = new DataClass();
		
		//new SmartDataMapper(null, null, null).mapFieldValue(source, "dataCollection.name", target, DataClass.class, "dataCollection.name");
		
		System.out.println(target);
	}
	
	@Test
	public void testSetFieldValue() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, InstantiationException{
		DataClass target = (DataClass)new SmartDataMapper(null, null, null).setFieldValue(new DataClass(), DataClass.class, "dataCollection.name", "x");
		target = (DataClass)new SmartDataMapper(null, null, null).setFieldValue(target, DataClass.class, "dataCollection.name", "y");
		System.out.println(((ArrayList<SubDataClass>)target.getDataCollection()).get(0).getName());
		System.out.println(((ArrayList<SubDataClass>)target.getDataCollection()).get(1).getName());
	}
	
	
	
	@Test
	public void testMap(){
		

		DataClass source = new DataClass();
		source.getTitles().add("t1");
		source.getTitles().add("t2");
		
		SubDataClass sub = new SubDataClass();
		source.getDataCollection().add(sub);
		sub.setName("n1");
		
		sub = new SubDataClass();
		source.getDataCollection().add(sub);
		sub.setName("n2");
		
		DataClass mappedTarget = new DataClass();
		
		Map<String,List<String>> fieldMap = new HashMap<>();
		fieldMap.put("dataCollection.name", Arrays.asList(new String[]{"dataCollection.name"}) );
		fieldMap.put("titles",  Arrays.asList(new String[]{"dataCollection.titles"}));
		
		new SmartDataMapper(source, fieldMap, mappedTarget).map( );
		
		System.out.println(mappedTarget);
		
		
	}
	
	@Test
	public void testMapObjectsWithParentClass(){
		

		DataClass source = new DataClass();
		source.getTitles().add("t1");
		source.setLength(100);
		
		SubDataClass sub = new SubDataClass();
		source.getParentDataCollection().add(sub);
		sub.setName("x");
		sub.setAge(1);
		
		sub = new SubDataClass();
		source.getParentDataCollection().add(sub);
		sub.setName("y");
		sub.setAge(2);
		
		SubDataClass[] arrayData = new SubDataClass[2];
		source.setDataArray(arrayData);
		arrayData[0] = new SubDataClass();
		arrayData[0].setName("array1");
		arrayData[0].setAge(10);
		arrayData[1] = new SubDataClass();
		arrayData[1].setName("array2");
		arrayData[1].setAge(20);
		
		
		DataClass mappedTarget = new DataClass();
		
		Map<String,List<String>> fieldMap = new HashMap<>();
		fieldMap.put("parentDataCollection.name",  Arrays.asList(new String[]{"dataCollection.name"}));
		fieldMap.put("parentDataCollection.age",  Arrays.asList(new String[]{"dataCollection.age"}));
		fieldMap.put("dataArray.name",  Arrays.asList(new String[]{"dataArray.name"}));
		fieldMap.put("dataArray.age",  Arrays.asList(new String[]{"dataArray.age"}));
		fieldMap.put("length",  Arrays.asList(new String[]{"length"}));
		
		fieldMap.put("titles",  Arrays.asList(new String[]{"titles"}));
		fieldMap.put("properties",  Arrays.asList(new String[]{"properties"}));
		
		new SmartDataMapper(source, fieldMap, mappedTarget).map();
		
		System.out.println(mappedTarget);
		
		
	}
	
	public static class ParentDataClass{
		private Collection<SubDataClass> parentDataCollection  = new ArrayList<>();

		public Collection<SubDataClass> getParentDataCollection() {
			return parentDataCollection;
		}

		public void setParentDataCollection(
				Collection<SubDataClass> parentDataCollection) {
			this.parentDataCollection = parentDataCollection;
		}
		
	}
	
	public static class DataClass extends ParentDataClass{
		private Collection<SubDataClass> dataCollection  = new ArrayList<>();
		private SubDataClass[] dataArray = {};
		
		public SubDataClass[] getDataArray() {
			return dataArray;
		}

		public void setDataArray(SubDataClass[] dataArray) {
			this.dataArray = dataArray;
		}

		public Collection<SubDataClass> getDataCollection(){
			return dataCollection;
		}
		
		private Map<String,String> properties;
		
		private int length;
		
		
		public int getLength() {
			return length;
		}

		public void setLength(int length) {
			this.length = length;
		}

		private List<String> titles = new ArrayList<>();
		public List<String> getTitles() {
			return titles;
		}

		public void setTitles(List<String> titles) {
			this.titles = titles;
		}
		
		
		
		
		
	}
	
	public static class SubDataClass{
		private String name;
		private Integer age;
		
		public Integer getAge() {
			return age;
		}

		public void setAge(Integer age) {
			this.age = age;
		}

		public String getName(){
			return name;
		}
		
		public void setName(String name){
			this.name = name;
		}
		
		private List<String> titles = new ArrayList<>();
		public List<String> getTitles() {
			return titles;
		}

		public void setTitles(List<String> titles) {
			this.titles = titles;
		}
		
		
	}

}
