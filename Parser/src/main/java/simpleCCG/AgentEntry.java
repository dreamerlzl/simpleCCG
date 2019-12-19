package simpleCCG;

import java.lang.Math;
import java.util.Objects;

public class AgentEntry implements Comparable<AgentEntry>{
	public double weight, gcost;
	public int start, end, head;
	private CCGCategory category;
	private String text;
	private AgentEntry left, right;

	public static void main(String[] args)
	{
		AgentEntry temp = new AgentEntry(0, 1, new CCGCategory("NP"), 0, 0, "I");
		AgentEntry temp2 = new AgentEntry(1, 2, new CCGCategory("S\\NP"), 0, 0, "sleep");
		AgentEntry temp3 = new AgentEntry(0, 2, new CCGCategory("S"), 0, 0, temp, temp2);
		System.out.println(temp3.toParseTree(0));
	}
	
	// for complex category
	public AgentEntry(int s, int e, CCGCategory c, double w, double g, AgentEntry l, AgentEntry r)
	{
		start = s;
		end = e;
		category = c;
		weight = w;
		gcost = g;
		text = "";
		left = l;
		right = r;
	}
	
	public AgentEntry(int s, int e, CCGCategory c, double w, double g, String t)
	{
		start = s; end = e; category = c; weight =w; text = t; gcost = g;
		left = right = null;
		head = e;
	}
	
	public AgentEntry(AgentEntry ref, CCGCategory c)
	{
		start = ref.start;
		end = ref.end;
		weight = ref.weight;
		text = ref.text;
		gcost = ref.gcost;
		left = ref.left;
		right = ref.right;
		head = ref.head;
		category = c;
	}
	
//	// for atom category
//	public AgentEntry(int s, int e, CCGCategory c, double w, String t)
//	{
//		start = s; end = e; category = c; weight =w; text = t;
//	}
	
	@Override
	public boolean equals (Object o)
	{
		if (o == this) return true;
		if (!(o instanceof AgentEntry)) {
            return false;
        }
		return (this.hashCode() == o.hashCode());
	}
	
	@Override
	public int hashCode()
	{
		if(left == null && right == null)
			return Objects.hash(start, end, category.hashCode(), 0, 0);
		else if(left!=null && right!=null)
			return Objects.hash(start, end, category.hashCode(), left.hashCode(), right.hashCode());
		else if(left == null && right != null)
			return Objects.hash(start, end, category.hashCode(), 0, right.hashCode());
		else
			return Objects.hash(start, end, category.hashCode(), left.hashCode(), 0);
	}
	
	public int compareTo(AgentEntry o)
	{
		return (int) Math.signum(weight - o.weight);
	}
	
	public CCGCategory getCategory()
	{
		return category;
	}
	
	public String getText()
	{
		return text;
	}
	
	public String toString()
	{
		StringBuilder result = new StringBuilder(String.format("[%d, %d] %s %.3f %.3f", start, end, category.toString(), gcost, weight));
		if (left != null)
			result.append("\t").append(left.getCategory().toString());
		if(right != null)
			result.append("\t").append(right.getCategory().toString());
		return result.toString();
	}
	
	public static String repeat(String s, int times)
	{
		StringBuilder temp = new StringBuilder();
		for(int i = 0;i < times; ++i)
			temp.append(s);
		return temp.toString();
	}
	
	public String toParseTree()
	{
		return toParseTree(0);
	}
	
	public String toParseTree(int depth)
	{
		if(left == null || right == null)
		{
			return String.format("%s{%s %s}", repeat(Config.treeIndent, depth), category, text);
		}
		else
		{
			String indents = repeat(Config.treeIndent, depth);
			return String.format("%s{%s\n%s\n%s}", indents, category, left.toParseTree(depth+1), right.toParseTree(depth+1));
		}
	}
	
	public static boolean sameParseTree(AgentEntry s, AgentEntry o)
	{
		if (s == null && o == null)
			return true;
		else if( s == null && o != null)
			return false;
		else if (s != null && o == null)
			return false;
		else
			return s.getCategory().sameCategory(o.getCategory()) && sameParseTree(s.left, o.left) && sameParseTree(s.right, o.right);
	}
}
