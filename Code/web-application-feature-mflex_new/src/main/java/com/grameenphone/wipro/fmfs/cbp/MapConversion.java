package com.grameenphone.wipro.fmfs.cbp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.grameenphone.wipro.utility.marshal.Json;

public class MapConversion 
{
	public static void main(String[] args) throws IOException {
		String mapValue = "{\"1771\":\"1-BILL_MONTH:50 SURCHARGE:60 MSISDN:70 \",\"1772\":\"1-BILL_MONTH:50 SURCHARGE:60 MSISDN:70 \"}";
		String mapValue1 = "{\"1810\":128.01,\"1818\":128.01}";
		System.out.println("Actual Map String:\t"+mapValue);
		System.out.println("Actual Map String:\t"+mapValue1);
		Map<Long, Double> map1 = new HashMap<Long, Double>();
		Map<String, String> map2 = new HashMap<String, String>();
		
		String mapValue12 = "{\"BILL_MONTH:50 SURCHARGE:60 MSISDN:70 \"}";
		String[] split = mapValue12.trim().split(" ");
		System.out.println(split);
		
		//Map<Long, String> map = Json.fromJson(mapValue, new TypeReference<>() {});
		//System.out.println(map);
		
		/*
		if(mapValue.contains("-"))
		{
			Map<Long, String> map = Json.fromJson(mapValue, new TypeReference<>() {});
			System.out.println("Exact Map:\t\t"+map);
			for (Map.Entry<Long,String> mapElement : map.entrySet()) {
				String[] value = mapElement.getValue().split("-");
				String str = mapElement.getValue();
				map1.put(mapElement.getKey(), Double.parseDouble(value[0]));
			}
			System.out.println("First Map:\t\t"+map1);
			/*
			 * String str = "BILL_MONTH:50 SURCHARGE:60 MSISDN:70"; Map fromJson =
			 * Json.fromJson(str, Map.class); System.out.println(fromJson);
			
		}
		else
		{
			Map<Long, Double> map11 = Json.fromJson(mapValue1, new TypeReference<>() {});
		}
		if(mapValue.contains("-"))
		{
			Map<Long, String> map = Json.fromJson(mapValue, new TypeReference<>() {});
			System.out.println(map);
			for (Map.Entry<Long,String> mapElement : map.entrySet()) {
	            String[] value = mapElement.getValue().split("-");
	            System.out.println("Value1:"+value[1]);
	            String[] split = value[1].split(" ");
	            for(String v : split)
	            {
	            	System.out.println("Value:"+v);
	            	map2.put(v.substring(0, v.indexOf(":")), v.substring(v.indexOf(":")+1));
	            }
	            System.out.println(map2);
	            map1.put(mapElement.getKey(), Double.parseDouble(value[0]));            
	        }
			System.out.println(map1);
		}*/
	}
}
