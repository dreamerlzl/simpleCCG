package simpleCCG;

public class CCGRule {
	
	interface UnaryRule {
		void apply(AgentEntry first, Agent agent);
	}
	
	public static UnaryRule[] ccgUnaryRules = new UnaryRule [] {
		new UnaryRule() {public void apply(AgentEntry first, Agent agent) 
		{
			applyTypeRaising(first, agent);
		}},
		new UnaryRule() {public void apply(AgentEntry first, Agent agent) 
		{
			applyUnaryTypeChanging(first, agent);
		}}
	};
	
	interface BinaryRule {
		void apply(AgentEntry first, AgentEntry second, Agent agent);
	}
	
	public static BinaryRule[] ccgBinaryRules = new BinaryRule [] {
			new BinaryRule(){public void apply(AgentEntry first, AgentEntry second, Agent agent) 
			{
				CCGCategory left = first.getCategory(), right = second.getCategory();
				boolean condition = canForwardApplication(left, right);
//				System.out.println(String.format("left: %s\nright: %s\n%s", left, right, condition));
				if(condition)
					applyForwardApplication(first, second, agent);
			} },
			new BinaryRule(){public void apply(AgentEntry first, AgentEntry second, Agent agent) 
			{
				CCGCategory left = first.getCategory(), right = second.getCategory();
				if(canBackwardApplication(left, right))
					applyBackwardApplication(first, second, agent);
			} },
			new BinaryRule(){public void apply(AgentEntry first, AgentEntry second, Agent agent) 
			{
				CCGCategory left = first.getCategory(), right = second.getCategory();
				if(canForwardComposition(left, right))
					applyForwardComposition(first, second, agent);
			} },
			new BinaryRule(){public void apply(AgentEntry first, AgentEntry second, Agent agent) 
			{
				CCGCategory left = first.getCategory(), right = second.getCategory();
				if(canBackwardComposition(left, right))
				applyBackwardComposition(first, second, agent);
			} },
			new BinaryRule(){public void apply(AgentEntry first, AgentEntry second, Agent agent) 
			{
				CCGCategory left = first.getCategory(), right = second.getCategory();
				if(canBCC(left, right))
				applyBCC(first, second, agent);
			} },
			new BinaryRule(){public void apply(AgentEntry first, AgentEntry second, Agent agent) 
			{
				CCGCategory left = first.getCategory(), right = second.getCategory();
				if(canGFC(left, right))
				applyGFC(first, second, agent);
			} },
			new BinaryRule(){public void apply(AgentEntry first, AgentEntry second, Agent agent) 
			{
				CCGCategory left = first.getCategory(), right = second.getCategory();
				if(canGBC(left, right))
				applyGBC(first, second, agent);
			} },
			new BinaryRule(){public void apply(AgentEntry first, AgentEntry second, Agent agent) 
			{
				CCGCategory left = first.getCategory(), right = second.getCategory();
				applyConj(first, second, agent);
			} },
			new BinaryRule(){public void apply(AgentEntry first, AgentEntry second, Agent agent) 
			{
				CCGCategory left = first.getCategory(), right = second.getCategory();
				applyBinaryTypeChanging(first, second, agent);
			} }
	};
	
	// assume the two words are adjacent
	public static boolean canForwardApplication(CCGCategory left, CCGCategory right)
	{
//		System.out.println("comlex?:"+left.isComplex());
//		System.out.println("direction?:"+(left.getDirection() == '/'));
//		if (left.isComplex())
//			System.out.println("same?:" + left.getArgument().sameCategory(right));
		return left.isComplex() && left.getDirection() == '/' && left.getArgument().sameCategory(right);
	}
	
	public static void applyForwardApplication(AgentEntry first, AgentEntry second, Agent agent)
	{
		CCGCategory left = first.getCategory();
//		String f = left.getArgument().carriedFeature;
//		if(!f.isEmpty())
//			left.getValue().carryFeature(f);
		agent.add(first, second, left.getValue());
	}
	
	public static boolean canBackwardApplication(CCGCategory left, CCGCategory right)
	{
		return right.isComplex() && right.getDirection() == '\\' && left.sameCategory(right.getArgument());
	}
	
	public static void applyBackwardApplication(AgentEntry first, AgentEntry second, Agent agent)
	{
		CCGCategory right = second.getCategory();
//		String f = right.getArgument().carriedFeature;
//		if(!f.isEmpty())
//			right.getValue().carryFeature(f);
		agent.add(first, second, right.getValue());
	}
	
	public static boolean canForwardComposition(CCGCategory left, CCGCategory right)
	{
		return left.isComplex() && right.isComplex() && left.getDirection() == '/' && right.getDirection() == '/'
				&& left.getArgument().sameCategory(right.getValue());
	}
	
	public static void applyForwardComposition(AgentEntry first, AgentEntry second, Agent agent)
	{
		CCGCategory left = first.getCategory(), right = second.getCategory();
//		String f1 = left.getArgument().carriedFeature, f2 = right.getValue().carriedFeature;
//		if(!f1.isEmpty())
//			left.getValue().carryFeature(f1);
//		else if(!f2.isEmpty())
//			right.getArgument().carryFeature(f2);
		agent.add(first, second, new CCGCategory(left.getValue(), '/', right.getArgument()));
	}
	
	public static boolean canBackwardComposition(CCGCategory left, CCGCategory right)
	{
		return left.isComplex() && right.isComplex() && left.getDirection() == '\\' && right.getDirection() == '\\' 
				&& right.getArgument().getAtomCategory() != "N" && right.getArgument().getAtomCategory() != "NP"
				&& left.getValue().sameCategory(right.getArgument());
	}
	
	public static void applyBackwardComposition(AgentEntry first, AgentEntry second, Agent agent)
	{
		CCGCategory left = first.getCategory(), right = second.getCategory();
//		String f1 = left.getValue().carriedFeature, f2 = right.getArgument().carriedFeature;
//		if(!f1.isEmpty())
//			left.getArgument().carryFeature(f1);
//		else if(!f2.isEmpty())
//			right.getValue().carryFeature(f2);
		agent.add(first, second, new CCGCategory(right.getValue(), '\\', left.getArgument()));
	}
	
	//Backward-Crossed Composition
	public static boolean canBCC(CCGCategory left, CCGCategory right)
	{
		return left.isComplex() && right.isComplex() && left.getDirection() == '/' && right.getDirection() == '\\'
				&& right.getArgument().getAtomCategory() != "N" && right.getArgument().getAtomCategory() != "NP"
				&& left.getValue().sameCategory(right.getArgument());
	}
	
	public static void applyBCC(AgentEntry first, AgentEntry second, Agent agent)
	{
		CCGCategory left = first.getCategory(), right = second.getCategory();
//		String f1 = left.getValue().carriedFeature, f2 = right.getArgument().carriedFeature;
//		if(!f1.isEmpty())
//			left.getArgument().carryFeature(f1);
//		else if(!f2.isEmpty())
//			right.getValue().carryFeature(f2);
		agent.add(first, second, new CCGCategory(right.getValue(), '/', left.getArgument()));
	}
	
	// Generalized Forward Composition
	public static boolean canGFC(CCGCategory left, CCGCategory right)
	{
		return left.isComplex() && right.isComplex() && right.getValue().isComplex() && left.getDirection() == '/' && right.getDirection() == '/' &&
				(right.getArity() < Config.max_arity) && left.getArgument().sameCategory(right.getValue().getValue());
	}
	
	public static void applyGFC(AgentEntry first, AgentEntry second, Agent agent)
	{
		CCGCategory left = first.getCategory(), right = second.getCategory();
//		String f1 = left.getArgument().carriedFeature, f2 = right.getValue().getValue().carriedFeature;
//		if(!f1.isEmpty())
//			left.getValue().carryFeature(f1);
//		else if(!f2.isEmpty())
//			right.getValue().getArgument().carryFeature(f2);
		agent.add(first, second, new CCGCategory(new CCGCategory(left.getValue(), '/', right.getValue().getArgument()), '/', 
				right.getArgument()));
	}
	
	// Generalized Backward Composition
	public static boolean canGBC(CCGCategory left, CCGCategory right)
	{
		return left.isComplex() && left.getValue().isComplex() && right.isComplex() && left.getDirection() == '\\' && right.getDirection() == '\\' &&
				(left.getArity() < Config.max_arity) && left.getValue().getValue().sameCategory(right.getArgument());
	}
	
	public static void applyGBC(AgentEntry first, AgentEntry second, Agent agent)
	{
		CCGCategory left = first.getCategory(), right = second.getCategory();
		agent.add(first, second, new CCGCategory(new CCGCategory(right.getValue(), '\\', left.getValue().getArgument()), '\\', 
				left.getArgument()));
	}
	
	
	public static void applyConj(AgentEntry first, AgentEntry second, Agent agent)
	{
		CCGCategory left = first.getCategory(), right = second.getCategory();
		if (left.getAtomCategory().contentEquals("conj") || left.getAtomCategory().contentEquals(",") || left.getAtomCategory().contentEquals(";"))
		{
			agent.add(first, second, new CCGCategory(right, "conj"));
		}
		else if(left.sameCategory(right) && left.getFeature().contentEquals("") && right.getFeature().contentEquals("conj"))
		{
			agent.add(first, second, left);
		}
	}
	
	// 3 type-raising rules in A* CCG (Mike Lewis et. al.)
	public static void applyTypeRaising(AgentEntry first, Agent agent)
	{
		CCGCategory c = first.getCategory();
		if(c.getAtomCategory().contentEquals("NP"))
		{
			agent.add(first, CCGCategory.parse("S/(S\\NP)"));
			agent.add(first, CCGCategory.parse("(S\\NP)/((S\\NP)/NP)"));
		}
		else if(c.getAtomCategory().contentEquals("PP"))
		{
			agent.add(first, CCGCategory.parse("(S\\NP)/((S\\NP)/PP)"));
		}
	}
	
	// 10 unary type-changing rules in A* CCG (Mike Lewis et.al.)
	public static void applyUnaryTypeChanging(AgentEntry first, Agent agent)
	{
		CCGCategory c = first.getCategory();
		if(c.sameCategory(new CCGCategory("N")))
		{
			agent.add(first, new CCGCategory("NP"));
		}
		else if(c.isComplex() && c.getValue().getAtomCategory().contentEquals("S") &&  c.getArgument().getAtomCategory().contentEquals("NP"))
		{
			CCGCategory val = c.getValue();
			char d = c.getDirection();
			if(d == '\\')
			{
				if (val.getFeature().contentEquals("pss"))
				{
					agent.add(first, CCGCategory.parse("NP\\NP"));
					agent.add(first, CCGCategory.parse("VP"));
				}
				else if(val.getFeature().contentEquals("to"))
				{
					agent.add(first, CCGCategory.parse("NP\\NP"));
					agent.add(first, CCGCategory.parse("N\\N"));
					agent.add(first, CCGCategory.parse("S/S"));
				}
				else if(val.getFeature().contentEquals("ng"))
				{
					agent.add(first, CCGCategory.parse("NP\\NP"));
					agent.add(first, CCGCategory.parse("S/S"));
				}
				else if(val.getFeature().contentEquals("adj"))
				{
					agent.add(first, CCGCategory.parse("NP\\NP"));
				}
			}
			else if(val.getFeature().contentEquals("dcl"))
			{
				agent.add(first, CCGCategory.parse("NP\\NP"));
			}
		}
		
	}
	
	public static void applyBinaryTypeChanging(AgentEntry first, AgentEntry second, Agent agent)
	{
		CCGCategory left = first.getCategory(), right = second.getCategory();
		CCGCategory Sdcl = new CCGCategory("S", "dcl");
		if(left.isAtomic && right.isAtomic)
		{
			if(left.getAtomCategory().contentEquals("NP") && right.getAtomCategory().contentEquals(","))
			{
				agent.add(first, second, CCGCategory.parse("S/S")); 
			}
			else if(left.getAtomCategory().contentEquals(",") && right.getAtomCategory().contentEquals("NP"))
			{
				agent.add(first, second, CCGCategory.parse("S\\S"));
				agent.add(first, second, CCGCategory.parse("(S\\NP)\\(S\\NP)"));
			}
		}
		else if(left.isComplex() && left.getValue().sameCategory(Sdcl) && left.getArgument().sameCategory(Sdcl) && 
				right.getAtomCategory().contentEquals(","))
		{
			if(left.getDirection() == '\\')
			{
				agent.add(first, second, CCGCategory.parse("S/S"));
			}
			else
			{
				// the following rules are from C&C (2007) 
				agent.add(first, second, CCGCategory.parse("S/S"));
				agent.add(first, second, CCGCategory.parse("S\\S"));
				agent.add(first, second, CCGCategory.parse("(S\\NP)\\(S\\NP)"));
				agent.add(first, second, CCGCategory.parse("(S\\NP)/(S\\NP)"));
			}
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
