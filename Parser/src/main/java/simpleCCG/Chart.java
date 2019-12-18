package simpleCCG;

import java.util.HashSet;
import java.util.HashMap;

public class Chart {

	private HashMap<Integer, HashMap<Integer, HashSet<AgentEntry>>> table;
	private int sentenceLen = 0;
	
	public Chart(int size)
	{
		sentenceLen = size;
		table = new HashMap<>();
	}
	
	public void init(int start, int end)
	{
		if (!table.containsKey(start))
			table.put(start, new HashMap<>());
		if(!table.get(start).containsKey(end))
			table.get(start).put(end, new HashSet<>());
	}
	
	public void add(int start, int end, AgentEntry entry)
	{
		init(start, end);
		table.get(start).get(end).add(entry);
	}
	
	public HashSet<AgentEntry> get(int start, int end)
	{
		init(start, end);
		return table.get(start).get(end);
	}
	
	public HashSet<AgentEntry> getRight(int start, int end)
	{
		HashSet<AgentEntry> result = new HashSet<>();
		for(int i = end+1; i <= sentenceLen; ++i)
			result.addAll(get(end, i));
		return result;
	}
	
	public HashSet<AgentEntry> getLeft(int start, int end)
	{
		HashSet<AgentEntry> result = new HashSet<>();
		for(int i = 0; i < start;++i)
			result.addAll(get(i, start));
		return result;
	}
}
