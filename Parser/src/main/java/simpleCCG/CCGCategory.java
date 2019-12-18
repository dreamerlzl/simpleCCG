package simpleCCG;

import java.util.Stack;

public class CCGCategory {
    private CCGCategory argument, value; // any CCG category is a unary function
    private char direction; // the direction of slash
    
    private String atomCategory, feature; //feature is for S, NP, N
    private int arity; // 
    public boolean isAtomic; // S, NP, N, PP are atomic category
    
    public String carriedFeature;

    public static void main(String[] args)
    {
    		CCGCategory c1 = CCGCategory.parse("((S[dcl]\\NP)/(S[adj]\\NP))"), c2 = CCGCategory.parse("S[adj]\\NP");
    		System.out.println(c1.getArgument().sameCategory(c2));
//    		String testCat = "(((S[dcl]\\NP)/(S[to]\\NP))/PP)/(S[adj]\\NP)";
//    		CCGCategory parseResult = parse(testCat);
//    		System.out.println(parseResult + "\n" + "arity: " + parseResult.getArity() + "\n");
    }
    
    public CCGCategory(String atom)
    {
    		atomCategory = atom;
    		isAtomic = true;
    		feature = "";
    		direction = ' ';
    		argument = value = null;
    		arity = 0;
    		carriedFeature = "";
    }
    
    public CCGCategory(String atom, String f)
    {
    		atomCategory = atom;
    		feature = f;
    		isAtomic = true;
    		direction = ' ';
    		argument = value = null;
    		arity = 0;
    		carriedFeature = "";
    }
    
    public CCGCategory(CCGCategory val, char dir, CCGCategory arg)
    {
    		value = val;
    		direction = dir;
    		argument = arg;
    		isAtomic = false;
    		arity = val.arity + 1;
    		atomCategory = "";
    		feature = "";
    		carriedFeature = "";
    }
    
    public CCGCategory(CCGCategory x, String f)
    {
    		atomCategory = x.atomCategory;
    		direction = x.direction ;
    		isAtomic = x.isAtomic;
    		argument = x.argument;
    		value = x.value;
    		arity = x.arity ;
    		carriedFeature = "";
    		feature = f;
    }
    
    public String toString()
    {
    		if (isAtomic)
    			if (feature.isEmpty())
    				return atomCategory;
    			else
    				return new StringBuilder().append(atomCategory).append('[').append(feature).append(']').toString();
    		else
    			return new StringBuilder().append('(').append(value.toString()).append(direction).append(argument.toString()).append(')').toString();
    }
    
    public boolean sameDirection(CCGCategory o)
    {
    		return direction == o.direction;
    }
    
    public boolean sameCategory(CCGCategory o)
    {
    		return sameCategory(this, o);
    }
    
    public static boolean compatiableFeature(CCGCategory s, CCGCategory o)
    {
    		String f1 = s.getFeature(), f2= o.getFeature();
//    		if (s.atomCategory.contentEquals("S"))
//    		{	
//    			if(f1.contentEquals("") && !f2.contentEquals(""))
//    			{
//    				s.carriedFeature = f2;
//    				return true;
//    			}
//    			else if(!f1.contentEquals("") && f2.contentEquals(""))
//    			{
//    				o.carriedFeature = f1;
//    				return true;
//    			}
//    			else if(f1.contentEquals(f2))
//    			{
//    				return true;
//    			}
//    			return false;
//    		}
//    		else
    			return f1.contentEquals("") || f2.contentEquals("") || f1.contentEquals(f2);
    }
    
    public static boolean sameCategory(CCGCategory s, CCGCategory o)
    {
    		if(s.isAtomic == o.isAtomic)
    		{
    			if(s.isAtomic)
    			{
    				return s.atomCategory.contentEquals(o.atomCategory) && compatiableFeature(s, o);
    			}
    			else
    			{
    				return s.direction == o.direction && sameCategory(s.value, o.value) && sameCategory(s.argument, o.argument);
    			}
    		}
    		else 
    			return false;
    }
    
    public static boolean strictSameCategory(CCGCategory s, CCGCategory o)
    {
		if(s.isAtomic == o.isAtomic)
		{
			if(s.isAtomic)
				return s.atomCategory.contentEquals(o.atomCategory) && s.feature.contentEquals(o.feature);
			else
			{
				return s.direction == o.direction && sameCategory(s.value, o.value) && sameCategory(s.argument, o.argument);
			}
		}
		else 
			return false;
    }
    
    public void carryFeature(String f)
    {
    		// only for S
    		if(isAtomic && atomCategory.contentEquals("S") && feature.isEmpty())
    		{
    			feature = f;
    		}
    		else if(!isAtomic)
    		{
    			getValue().carryFeature(f);
    			getArgument().carryFeature(f);
    		}
    }
    
    public static CCGCategory parse(String expr)
    {
        // construct a category by parsing the string expression
    		// actually this is an infix operation parser
    	
    		char c = ' ';
    		StringBuilder currentAtom = new StringBuilder(), currentFeature = new StringBuilder();
    		int within_num_parens = 0;
    		Stack<CCGCategory> categories = new Stack<>();
        Stack<Character> operators = new Stack<>();
        Stack<Integer> operatorPriority = new Stack<>(); // assume that '/' and '\' are left associative
    		for(int i = 0; i < expr.length(); )
        {
        		c = expr.charAt(i);
        		switch(c)
        		{
        		case '(': within_num_parens += 1; ++i; break;
        		case ')': within_num_parens -= 1; ++i; break;
        		case '/': 
        			++i;
        			if(operatorPriority.empty() || within_num_parens > operatorPriority.peek())
	        		{
	        			operators.add(c);
	        			operatorPriority.add(within_num_parens);
	        		}
        			else {
						while( !operatorPriority.empty() && within_num_parens < operatorPriority.peek())
						{
							char op = operators.pop();
							operatorPriority.pop();
							CCGCategory rightCat = categories.pop();
							CCGCategory leftCat = categories.pop();
							categories.add(new CCGCategory(leftCat, op, rightCat));
						}
						operators.add(c);
						operatorPriority.add(within_num_parens);
					}
        			break;
        		case '\\': 
        			++i;
        			if(operatorPriority.empty() || within_num_parens > operatorPriority.peek())
        			{
        				operators.add(c);
        				operatorPriority.add(within_num_parens);
        			}
        			else {
						while(!operatorPriority.empty() && within_num_parens < operatorPriority.peek())
						{
							char op = operators.pop();
							operatorPriority.pop();
							CCGCategory rightCat = categories.pop();
							CCGCategory leftCat = categories.pop();
							categories.add(new CCGCategory(leftCat, op, rightCat));
						}
						operators.add(c);
						operatorPriority.add(within_num_parens);
					}
        			break;
        		case '[': // for features of S, N, NP
        			c = expr.charAt(++i);
        			for(;Character.isLetter(c)  && c != ']' && i < expr.length(); c = expr.charAt(++i))
        			{
//        				System.out.println(c);
        				currentFeature.append(c);
        			}
        			
        			// System.out.println(currentFeature.toString());
        			// now c is ]
        			++i;
        			CCGCategory catWithFeature = categories.pop();
        			catWithFeature.feature = currentFeature.toString();
        			categories.add(catWithFeature);
        			currentFeature.setLength(0); // clear the currentFeature
        			break;
        		default: // alphabets for categories
        			if (Character.isLetter(c))
        			{
	        			for(;i < expr.length() && Character.isLetter(expr.charAt(i)); ++i)
	        			{
	//        				System.out.println(expr.charAt(i));
	        				currentAtom.append(expr.charAt(i));
	        			}
	        			
	        			// System.out.println(currentAtom.toString());
	        			CCGCategory atom = new CCGCategory(currentAtom.toString());
	        			categories.add(atom);
	        			currentAtom.setLength(0); // clear the currentAtom
        			}
        			else
        			{
        				// c is a punctuation
        				categories.add(new CCGCategory(String.valueOf(c)));
        				++i;
        			}
        			break;
        		}
        }
    		
    		while(!operators.empty())
    		{
			char op = operators.pop();
			operatorPriority.pop();
			CCGCategory rightCat = categories.pop();
			CCGCategory leftCat = categories.pop();
			categories.add(new CCGCategory(leftCat, op, rightCat));
    		}
    			
    		return categories.pop();
    }

    	public CCGCategory getArgument()
    	{
    		return argument;
    	}
    	
    	public CCGCategory getValue()
    	{
    		return value;
    	}
    
    public String getFeature()
    {
        return feature;
    }

    public String getAtomCategory()
    {
        if (isAtomic == true)
            return atomCategory;
        else 
            return ""; // not an atomic CCG Category
    }

    public boolean isComplex()
    {
    		return !isAtomic;
    }
    
    public char getDirection()
    {
        return direction;
    }
    
    public int getArity()
    {
    	return arity;
    }

}