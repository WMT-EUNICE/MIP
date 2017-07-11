package org.or.mip.xpress; /********************************************************
  Xpress-BCL Java Example Problems
  ================================

  file xbgoalobj.java
  ```````````````````
  Archimedian and pre-emptive goal programming
  using objective functions.

  (c) 2008 Fair Isaac Corporation
      author: S.Heipcke, 2005, rev. Mar. 2011
********************************************************/

import java.lang.*;
import com.dashoptimization.*;

public class xbgoalobj
{
 static final int NGOALS = 3;

/**** Data ****/
 static final String[] Type = {"perc", "abs", "perc"};
 static final String[] Sense = {"max", "min", "max"};
 static final double[] Weight = {100, 1, 0.1};
 static final double[] Deviation = {10, 4, 20};

 public static void main(String[] args) throws XPRSprobException, XPRSexception
 {
  XPRB bcl;
  XPRBvar x,y;
  XPRBexpr[] goal;
  XPRBexpr wobj;
  XPRBctr[] goalCtr;
  XPRBctr aCtr;
  double[] Target;
  XPRBprob prob;
  int i,g;

  bcl = new XPRB();              /* Initialize BCL */
  prob = bcl.newProb("Goal");    /* Create a new problem */
  XPRS.init();                   /* Initialize Xpress-Optimizer */

  Target = new double[NGOALS];
  goalCtr = new XPRBctr[NGOALS];
  goal = new XPRBexpr[NGOALS];
  wobj = new XPRBexpr();

 /* Adding the variables */
  x = prob.newVar("x",XPRB.PL);
  y = prob.newVar("y",XPRB.PL);

 /* Adding a constraint */
  aCtr = prob.newCtr("Limit", x.mul(42) .add(y.mul(13)) .lEql(100) );

 /* Goals */
  goal[0] = x.mul(5) .add(y.mul(2)) .add(-20);
  goal[1] = x.mul(-3) .add(y.mul(15)) .add(-48);
  goal[2] = x.mul(1.5) .add(y.mul(21)) .add(-3.8);
  for(g=0;g<NGOALS;g++) 
   goalCtr[g] = prob.newCtr("Goal"+(g+1), goal[g]);

 /**** Archimedian GP ****/
  System.out.println("Archimedian:");
  for(g=0;g<NGOALS;g++) 
  {
   if (Sense[g]=="max")
    wobj.add(((XPRBexpr)goal[g].clone()).mul(-Weight[g]));
   else
    wobj.add(((XPRBexpr)goal[g].clone()).mul(Weight[g]));
  } 
  prob.setObj(wobj);
  prob.getXPRSprob().setIntControl(XPRS.OUTPUTLOG, 0);
  prob.lpOptimize("");

 /* Solution printout */
  System.out.println(" Solution: x: " + x.getSol() + ", y: " + y.getSol());
  System.out.println(" Goal   Target     Value");
  for(g=0;g<NGOALS;g++) 
   System.out.println("  " + (g+1) + "       " + Sense[g] + "      " +
                      (goalCtr[g].getAct() - goalCtr[g].getRHS())); 
  
  
 /**** Prememptive GP ****/
  System.out.println("Prememptive:");
  i=-1;
  while (i<NGOALS-1) 
  {
   i+=1;
   if (Sense[i]=="max") 
   {
    prob.setObj(goal[i]);
    prob.setSense(XPRB.MAXIM);
    prob.lpOptimize("");
    if (prob.getLPStat() != XPRB.LP_OPTIMAL)
    {
     System.out.println("Cannot satisfy goal " + (i+1));
     break;
    }
    else
    {
     Target[i]=prob.getObjVal();
     if (Type[i]=="perc")
      Target[i]-= Math.abs(Target[i])*Deviation[i]/100;
     else
      Target[i]-= Deviation[i];
     if (i<NGOALS-1) goalCtr[i].add(Target[i]);
     goalCtr[i].setType(XPRB.G);
    }
   }
   else
   {
    prob.setObj(goal[i]);
    prob.setSense(XPRB.MINIM);
    prob.lpOptimize("");
    if (prob.getLPStat() != XPRB.LP_OPTIMAL)
    {
     System.out.println("Cannot satisfy goal " + i);
     break;
    }
    else
    {
     Target[i]=prob.getObjVal();
     if (Type[i]=="perc")
      Target[i]+= Math.abs(Target[i])*Deviation[i]/100;
     else
      Target[i]+= Deviation[i];
     if (i<NGOALS-1) goalCtr[i].add(Target[i]);
     goalCtr[i].setType(XPRB.L);
    } 
   }
   System.out.println("Solution(" + (i+1) + "):  x: " + x.getSol() + 
                      ", y: " + y.getSol());
  }

 /* Solution printout */
  System.out.println(" Goal        Target                Value");
  for(g=0;g<=i;g++) 
  {
   System.out.print("  " + (g+1) + "    " + 
                    (goalCtr[g].getType()==XPRB.G?" >=  ":" <=  ") + Target[g]);
   if(g==NGOALS-1) 
    System.out.println("   " + prob.getObjVal());
   else   
    System.out.println("   " + (goalCtr[g].getAct() - goalCtr[g].getRHS() 
                       + Target[g]));
  }

 }

}
