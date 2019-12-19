package simpleCCG;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
//import java.io.InputStream;
//import org.yaml.snakeyaml.Yaml;

public class Config {
	public static int max_arity = 4;
	public static String tag_weight_split = " ";
	public static String tag_split = "\t";
	public static String parseInput = "";
	public static String parseOutput = "";
	public static String configInput = "/Users/lin/Documents/linguistics/LIN424/project/simpleCCG/config.yaml";
	public static String treeIndent = "  ";
	
	public static void main(String[]args) throws IOException
	{
		BufferedReader bReader = null;
		String line = "";
		try
		{
			bReader = new BufferedReader(new InputStreamReader(new FileInputStream(configInput), "UTF-8"));
			while((line = bReader.readLine()) != null)
			{
				String[] pair = line.split(":");
				if(pair.length < 2)
					continue;
				String key = pair[0].trim(), value = pair[1].trim();
//				System.out.println(key);
				if(key.contentEquals("parse_input"))
				{
					parseInput = value;
				}
				else if(key.contentEquals("parse_output"))
				{
					parseOutput = value;
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(bReader != null)
				bReader.close();
		}
		System.out.println(parseInput + "\n" + parseOutput);
//		Yaml yaml = new Yaml();
//	    try (InputStream in = Config.class.getResourceAsStream(configInput)) {
//	          Object obj = yaml.load(in);
//	          System.out.println("Loaded object type:" + obj.getClass());
//	          System.out.println(obj);
//	      }
	}
}
