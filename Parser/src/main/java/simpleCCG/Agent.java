package simpleCCG;

import java.util.PriorityQueue;
import java.util.Vector;

// following the delegation design

public class Agent {
	private PriorityQueue<AgentEntry> entries; 
	private Vector<AgentEntry> temp;
	private Vector<Double> optimTag;
	private double minhcost = 0;
	
	public Agent()
	{
		entries = new PriorityQueue<AgentEntry>();
		optimTag = new Vector<>();
		temp = new Vector<>();
	}
	
	public void reset()
	{
		entries.clear();
	}
	
	public AgentEntry peek()
	{
		return entries.peek();
	}
	
	// for applying unary rule
	public void add(AgentEntry a, CCGCategory c)
	{
		AgentEntry x = new AgentEntry(a, c);
		entries.add(x);
	}
	
	// for apply binary rule
	public void add(AgentEntry first, AgentEntry second, CCGCategory c)
	{
		int  s = first.start, e = second.end;
		double g = first.gcost + second.gcost;
		double w = g + hcost(s, e);
		AgentEntry x = new AgentEntry(s, e, c, w, g, first, second);
//		System.out.println("new added entry:" + x);
		entries.add(x);
	}
	
	public void addInit(int s, int e, CCGCategory c, double g, String t)
	{
		AgentEntry x = new AgentEntry(s, e, c, 0.0, g, t);
		temp.add(x);
	}
	
	public void clearAndPrint()
	{
		System.out.println("agent:");
		while(!entries.isEmpty())
		{
			AgentEntry x = entries.poll();
			System.out.println(x);
		}
	}
	
	public boolean isEmpty()
	{
		return entries.isEmpty();
	}
	
	public AgentEntry pop()
	{
		return entries.poll();
	}
	
	public void addOptimTag(double e)
	{
		optimTag.add(e);
	}
	
	public void prepareHcost()
	{
		if(minhcost == 0)
			for(int i = 0;i < optimTag.size(); ++i)
				minhcost += optimTag.elementAt(i);
		for(AgentEntry top : temp)
		{
//			System.out.println(top);
			int start = top.start, end = top.end;
			top.weight = top.gcost + hcost(start, end);
//			System.out.println(top);
			entries.add(top);
		}
		temp.clear();
	}
	
	public double hcost(int start, int end)
	{
		double t = minhcost;
		for(int i = start; i < end; ++i)
			t -= optimTag.elementAt(i);
		return t;
	}
	
}
