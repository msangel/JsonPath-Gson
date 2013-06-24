package net.sinistersky.j2ee.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class GsonPath {

	private JsonElement obj;

	public GsonPath(String lastResult) {
		this.obj = new JsonParser().parse(lastResult);
	}

	public String getString(String path) {
		String asString = getFromObject(path).getAsString();
		return asString;
	}

	private JsonElement getFromObject(String path) {
		System.out.println("path:"+path);
		System.out.println("data:"+this.obj.toString());
		JsonElement jObj = this.obj;
		String[] split = path.split("\\.");
		JsonElement el = null;
		
		for (String e : split) {
			System.out.println("e:"+e);
			char lastChar = e.charAt(e.length()-1);
			System.out.println("last node char:"+lastChar);
			
			if(']'==lastChar){
				while (']'==lastChar) {
					System.out.println("e+:"+e);
					System.out.println("e.length()-1: "+(e.length()-1));
					System.out.println("e.lastIndexOf('[')+1: "+(e.lastIndexOf('[')+1));
					String index = e.substring(e.lastIndexOf('[')+1, e.length()-1);
					Integer iindex = Integer.valueOf(index);
					System.out.println("iindex:"+iindex);
					e = e.substring(0,e.lastIndexOf('['));
					if("".equals(e)){
						if(el==null){
							el = jObj.getAsJsonArray().get(iindex);
							lastChar = (char)0;
						} else {
							el = el.getAsJsonArray().get(iindex);
							lastChar = (char)0;
						}
					} else {
						lastChar = e.charAt(e.length()-1); // new last char
						System.out.println("new lastChar is:"+lastChar);
						
						
						// if next is object
						if(lastChar!=']'){
							if(el==null){
								el = jObj.getAsJsonObject().get(e).getAsJsonArray().get(iindex);
							} else {
								System.out.println("el:"+el+", path:"+e);
								if(el.isJsonObject()){
									el = el.getAsJsonObject().get(e).getAsJsonArray().get(iindex);										
								} else if(el.isJsonArray()){
									el = el.getAsJsonArray().get(iindex);
								}
							}
						} else { // next is array
							if(el==null){
								if(jObj.isJsonObject()){
									String locale = e.substring(0,e.indexOf('['));
									System.out.println("el:"+el+", path:"+locale);
									el = jObj.getAsJsonObject().get(locale).getAsJsonArray().get(iindex);									
								} else if(jObj.isJsonArray()){
									el = jObj.getAsJsonArray().get(iindex);
								}
							} else {
								if(e.indexOf('[')>-1){
									String locale = e.substring(0,e.indexOf('['));
									System.out.println("el:"+el+", path:"+locale);
									el = el.getAsJsonObject().get(locale).getAsJsonArray().get(iindex);
								} else {
									el = el.getAsJsonArray().get(iindex);	
									System.out.println("el:"+el);
								};

							}
						}
						
						
						
						
						

						
						
						System.out.println("index:"+index);
						System.out.println("new e:"+e);
						// array here
						// TODO
						System.out.println("----");
						

					}
				}
			} else{
				// plain obj
				if(el==null){
					el = jObj.getAsJsonObject().get(e);
				} else {
					el = el.getAsJsonObject().get(e);						
				}
				System.out.println("node val:"+el.toString());
				
			}

		}
		return el;
	}

	public static void main(String[] args) {
		String asString ;
		String json;
		
		json = "{\"a\":{\"b\":{\"c\":\"val\"}}}";
		asString = new GsonPath(json).getString("a.b.c");
		System.err.println(asString);
		
		json = "{\"a\":{\"b\":[{\"c\":\"val\"}]}}";
		asString = new GsonPath(json).getString("a.b[0].c");
		System.err.println(asString);
		
		json = "{\"a\":[{\"b\":{\"c\":\"val\"}}]}";
		asString = new GsonPath(json).getString("a[0].b.c");
		System.err.println(asString);
		
		json = "[{\"b\":{\"c\":\"val\"}},{}]";
		asString = new GsonPath(json).getString("[0].b.c");
		System.err.println(asString);
		
		json = "{a:{b:{c:[[val]]}}}";
		asString = new GsonPath(json).getString("a.b.c[0][0]");
		System.err.println(asString);
		
		
		json = "{\"a\":{\"b\":[[{\"c\":\"val\"}]]}}";
		asString = new GsonPath(json).getString("a.b[0][0].c");
		System.err.println(asString);
		
		
		json = "{\"a\":[[{\"b\":{\"c\":\"val\"}}]]}";
		asString = new GsonPath(json).getString("a[0][0].b.c");
		System.err.println(asString);
		
		json = "[[{\"b\":{\"c\":\"val\"}},{}]]";
		asString = new GsonPath(json).getString("[0][0].b.c");
		System.err.println(asString);
	}
	
}
