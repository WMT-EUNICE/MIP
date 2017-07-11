package org.or.mip.xpress; /********************************************************
  Xpress-BCL Java Example Problems
  ================================

  file xbelsc.java
  ````````````````
  Economic lot sizing, ELS, problem, solved by adding
  (l,S)-inequalities) in a branch-and-cut heuristic 
  (using the cut manager).
  
  ELS considers production planning over a horizon
  of T periods. In period t, t=1,...,T, there is a
  given demand DEMAND[t] that must be satisfied by
  production prod[t] in period t and by inventory
  carried over from previous periods. There is a
  set-up up cost SETUPCOST[t] associated with
  production in period t. The unit production cost
  in period t is PRODCOST[t]. There is no inventory
  or stock-holding cost.

  (c) 2008 Fair Isaac Corporation
      author: S.Heipcke, 2005, rev. Mar. 2011
********************************************************/

import java.util.*;
import com.dashoptimization.*;

public class xbelsc
{
 static final double EPS = 1e-6;
 static final int T = 6;                 /* Number of time periods */

/****DATA****/
 static final int[] DEMAND    = { 1, 3, 5, 3, 4, 2};  /* Demand per period */ 
 static final int[] SETUPCOST = {17,16,11, 6, 9, 6};  /* Setup cost / period */
 static final int[] PRODCOST  = { 5, 3, 2, 1, 3, 1};  /* Prod. cost / period */
 static int[][] D;                       /* Total demand in periods t1 - t2 */

 static XPRBvar[] prod;                  /* Production in period t */
 static XPRBvar[] setup;                 /* Setup in period t */

 static XPRB bcl;
 static XPRBprob p;

 static class myobj {
     XPRBprob prob;
     double tol;
 };

/**************************************************************************/
/*  Cut generation algorithm:                                             */
/*    get the solution values                                             */
/*    identify and set up violated constraints                            */
/*    add cuts to the problem                                             */
/**************************************************************************/
 static class CutMgrCallback implements XPRScutMgrListener
 {
  public int XPRScutMgrEvent(XPRSprob oprob, Object data)
  {
   int t,l;
   boolean res;
   int ncut;                    /* Counter for cuts */
   double[] solprod, solsetup;  /* Solution values for var.s prod & setup */
   double ds;
   XPRBexpr le;
   ArrayList<XPRBcut> cutlist;
   XPRBcut[] cuts;
   myobj mo;

   mo = (myobj)data;
 
   ncut = 0;
   cutlist = new ArrayList<XPRBcut>();
   try {
      /* Get the solution values */
    mo.prob.beginCB(oprob);
    mo.prob.sync(XPRB.XPRS_SOL);  

    solprod = new double[T];
    solsetup = new double[T];
    for(t=0;t<T;t++)
    {
     solprod[t]= prod[t].getSol();     
     solsetup[t]= setup[t].getSol();    
    }
     
      /* Search for violated constraints: */
    for(l=0;l<T;l++)
    {
     for(ds=0.0, t=0; t<=l; t++)
     {
      if(solprod[t] < D[t][l]*solsetup[t] + mo.tol)  ds += solprod[t];
      else  ds += D[t][l]*solsetup[t];
     }

      /* Add the violated inequality: the minimum of the actual production
         prod[t] and the maximum potential production D[t][l]*setup[t] 
         in periods 0 to l must at least equal the total demand in periods 
         0 to l.
         sum(t=1:l) min(prod[t], D[t][l]*setup[t]) >= D[0][l]
       */
     if(ds < D[0][l] - mo.tol) 
     {
      le = new XPRBexpr();
      for(t=0;t<=l;t++) 
      {
       if (solprod[t] < D[t][l]*solsetup[t] + mo.tol)
        le .add(prod[t]);
       else
        le .add(setup[t].mul(D[t][l]));
      }
      cutlist.add( mo.prob.newCut(le.gEql(D[0][l])) );       
      ncut++; 
     }
    }

  /* Add cuts to the problem */
    if(ncut>0)
    { 
     cuts = new XPRBcut[ncut];
     for(t=0;t<ncut;t++) cuts[t] = (XPRBcut)cutlist.get(t);
     mo.prob.addCuts(cuts);
     System.out.println("Cuts added: " + ncut + 
        " (depth " + oprob.getIntAttrib(XPRS.NODEDEPTH) +
        ", node "+ oprob.getIntAttrib(XPRS.NODES) + 
        "), obj. " + oprob.getDblAttrib(XPRS.LPOBJVAL));
    }
    mo.prob.endCB();
   }
   catch(XPRSprobException e) {
    System.out.println("Error  " + e.getCode() + ": " + e.getMessage());
   }
   return 0;
  }
 }
 
/***********************************************************************/

 static void modEls() throws XPRSexception
 {
  int s,t,k;
  XPRBexpr cobj,le; 
  
  bcl = new XPRB();                     /* Initialize BCL */
  p = bcl.newProb("Els");               /* Create a new problem in BCL */
  XPRS.init();                          /* Initialize Xpress-Optimizer */

  D = new int[T][T];
  for(s=0;s<T;s++)
   for(t=0;t<T;t++)
    for(k=s;k<=t;k++)
     D[s][t] += DEMAND[k];

/****VARIABLES****/
  prod = new XPRBvar[T];
  setup = new XPRBvar[T];
  for(t=0;t<T;t++)
  {
   prod[t]=p.newVar("prod" + (t+1));
   setup[t]=p.newVar("setup" +(t+1), XPRB.BV);   
  }

/****OBJECTIVE****/
  cobj = new XPRBexpr();
  for(t=0;t<T;t++)                      /* Minimize total cost */
   cobj .add(setup[t].mul(SETUPCOST[t]) .add(prod[t].mul(PRODCOST[t])) );
  p.setObj(cobj);

/****CONSTRAINTS****/
         /* Production in period t must not exceed the total demand for the
            remaining periods; if there is production during t then there
            is a setup in t */
  for(t=0;t<T;t++)
   p.newCtr("Production", prod[t] .lEql(setup[t].mul(D[t][T-1])) ); 

         /* Production in periods 0 to t must satisfy the total demand
            during this period of time */
  for(t=0;t<T;t++)
  {
   le = new XPRBexpr();
   for(s=0;s<=t;s++) le .add(prod[s]);
   p.newCtr("Demand", le.gEql(D[0][t]) );
  }
 }
 
/***********************************************************************/
 static void treeCutGen() throws XPRSexception
 {
  XPRSprob oprob;
  myobj mo;
  CutMgrCallback cb;
  double feastol;
  int starttime,t;

  starttime=XPRB.getTime();
 
  oprob = p.getXPRSprob();                    /* Get Optimizer problem */

  oprob.setIntControl(XPRS.LPLOG, 0);
  oprob.setIntControl(XPRS.MIPLOG, 3);

  oprob.setIntControl(XPRS.CUTSTRATEGY, 0);   /* Disable automatic cuts */
  oprob.setIntControl(XPRS.PRESOLVE, 0);      /* Switch presolve off */
  oprob.setIntControl(XPRS.EXTRAROWS, 5000);  /* Reserve extra rows */

  feastol=oprob.getDblControl(XPRS.FEASTOL);  /* Get zero tolerance */
  feastol*= 10;

  mo = new myobj();
  mo.prob=p;
  mo.tol=feastol;
  p.setCutMode(1);
  cb = new CutMgrCallback();
  oprob.addCutMgrListener(cb, mo);

  p.mipOptimize("");                          /* Solve the MIP */
  System.out.println("(" + (XPRB.getTime()-starttime)/1000.0 +
    " sec) Global status " + p.getMIPStat() + ", best solution: " +
    p.getObjVal());  

      /* Print out the solution: */
  for(t=0;t<T;t++)
   System.out.println("Period " + (t+1) + ": prod " + prod[t].getSol()
      + " (demand: " + DEMAND[t] + ", cost: " + PRODCOST[t] + "), setup " 
      + setup[t].getSol() + " (cost: " + SETUPCOST[t] + ")"); 
 }
 
/***********************************************************************/

 public static void main(String[] args) throws XPRSprobException, XPRSexception
 {
  modEls();                         /* Model the problem */
  treeCutGen();                     /* Solve the problem */
 } 
}
