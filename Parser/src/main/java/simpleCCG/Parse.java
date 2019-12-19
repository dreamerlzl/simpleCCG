package simpleCCG;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Vector;

import simpleCCG.CCGRule.BinaryRule;
import simpleCCG.CCGRule.UnaryRule;

public class Parse {
	
	public static void writeTagResult(Vector<AgentEntry> parses, String outPath) throws IOException
	{
		BufferedWriter bWriter = null;
		try
		{
			bWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outPath), "UTF-8"));
			int sentenceNo = 0;
			for(AgentEntry parse : parses)
			{
				sentenceNo += 1;
				bWriter.write("<=========== start of sentence " + String.valueOf(sentenceNo) +" ===========>\n");
				if(parse == null)
				{
					bWriter.write("Sentence " + String.valueOf(sentenceNo) + " has no parse!\n");
				}
				else
				{
					bWriter.write(parse.toParseTree()+"\n");
				}
				bWriter.write("<=========== end of sentence " + String.valueOf(sentenceNo) +" ===========>\n");
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			if(bWriter != null)
				bWriter.close();
		}
	}
	
	public static Vector<AgentEntry> readTagResult(String inputPath) throws IOException
	{
		Vector<AgentEntry> parses = new Vector<>();
		BufferedReader bReader = null;
		String line = "";
		int word_count = 0;
		try
		{
			bReader = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath), "UTF-8"));
			Agent agent = new Agent();
			while((line = bReader.readLine()) != null && line.length() > 0)
			{
				word_count = 0;
				// a new sentence
//				System.out.println("line1:" + line);
				addTagsForWord(line, word_count++, agent);
				while((line = bReader.readLine()) != null && line.length() > 0)
				{
//					System.out.println("line2:" + line);
					addTagsForWord(line, word_count++, agent);
				}
				agent.prepareHcost();
				parses.add(parseSentence(agent, word_count));
//				agent.clearAndPrint();
				agent.reset();
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
		return parses;
	}
	
	public static boolean isCompleteParse(AgentEntry entry, int sentenceLen)
	{
//		&& (entry.getCategory().sameCategory(new CCGCategory("S")) || entry.getCategory().sameCategory(new CCGCategory("NP"))))
		if(entry.start == 0 && entry.end == sentenceLen && entry.getCategory().isAtomic)
		{
			return true;
		}
		else
			return false;
	}
	
	public static AgentEntry parseSentence(Agent agent, int sentenceLen)
	{
		Chart chart = new Chart(sentenceLen);
		AgentEntry top = null;
		Vector<AgentEntry> completeParse = new Vector<>();
		System.out.println("begin parsing...");
		double completeWeight = Double.MAX_VALUE;
		while(!agent.isEmpty())
		{
			top = agent.pop();
//			System.out.println("considering: " + top);
			if(top.weight > completeWeight)
				break;
			if(isCompleteParse(top, sentenceLen))
			{
				completeParse.add(top);
				break;
				// retrieve all the parses with the same tag sequence
//				completeWeight = top.weight;
			}
			
			int start = top.start, end = top.end;
			chart.add(start, end, top);
			for(UnaryRule rule : CCGRule.ccgUnaryRules)
			{
				rule.apply(top, agent);
			}
			
			for(AgentEntry first: chart.getLeft(start, end))
			{
				for(BinaryRule rule : CCGRule.ccgBinaryRules)
				{
					rule.apply(first, top, agent);
				}
			}
			
			for(AgentEntry second: chart.getRight(start, end))
			{
				for(BinaryRule rule : CCGRule.ccgBinaryRules)
				{
					rule.apply(top, second, agent);
				}
			}
		}
//		System.out.println("number of parsers: "+completeParse.size());
//		for(AgentEntry parse: completeParse)
//		{
//			System.out.println(parse.toParseTree());
//		}
		return pickParse(completeParse);
	}
	
	public static AgentEntry pickParse(Vector<AgentEntry> completeParse)
	{
		if(!completeParse.isEmpty())
			return completeParse.elementAt(0);
		else
			return null;
	}
	
	public static void addTagsForWord(String line, int word_count, Agent agent)
	{
		String[] word_tags = line.split(Config.tag_split);
		String word = word_tags[0];

		String[] tag_weight = word_tags[1].split(Config.tag_weight_split);
		String tag = tag_weight[0];
		double weight = Double.parseDouble(tag_weight[1]);
		agent.addOptimTag(weight); // for computing hcost
		agent.addInit(word_count, word_count + 1, CCGCategory.parse(tag), weight, word);
		
		for(int i = 2; i < word_tags.length; ++i)
		{
			tag_weight = word_tags[i].split(Config.tag_weight_split);
			tag = tag_weight[0];
			weight = Double.parseDouble(tag_weight[1]);
			agent.addInit(word_count, word_count + 1, CCGCategory.parse(tag), weight, word);
		}
	}
	
	public static void main(String[] args) throws IOException
	{
		Config.main(null);
		writeTagResult(readTagResult(Config.parseInput), Config.parseOutput);
//		String line = "silences\tN 0.001312255859375\tNP 7.32759952545166";
//		Agent agent = new Agent();
//		addTagsForWord(line, 0, agent);
	}
}
