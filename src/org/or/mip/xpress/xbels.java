package org.or.mip.xpress; /********************************************************
  Xpress-BCL Java Example Problems
  ================================

  file xbels.java
  ```````````````
  Economic lot sizing, ELS, problem, solved by adding
  (l,S)-inequalities) in several rounds looping over 
  the root node.
  
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
      author: S.Heipcke, 2001, rev. Mar. 2011
********************************************************/

import com.dashoptimization.*;

public class xbels
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

/**************************************************************************/
/*  Cut generation loop at the top node:                                  */
/*    solve the LP and save the basis                                     */
/*    get the solution values                                             */
/*    identify and set up violated constraints                            */
/*    load the modified problem and load the saved basis                  */
/**************************************************************************/
 static void solveEls() throws XPRSexception
 {
  double objval;               /* Objective value */
  int t,l;
  int starttime;
  int ncut, npass, npcut;      /* Counters for cuts and passes */
  double[] solprod, solsetup;  /* Solution values for var.s prod & setup */
  double ds;
  XPRSprob op;
  XPRBbasis basis;
  XPRBexpr le;

  starttime=XPRB.getTime();
  op=p.getXPRSprob();
  op.setIntControl(XPRS.CUTSTRATEGY, 0);
                               /* Disable automatic cuts - we use our own */
  op.setIntControl(XPRS.PRESOLVE, 0);
                               /* Switch presolve off */
  ncut = npass = 0;
  solprod = new double[T];
  solsetup = new double[T];

  do
  {
   npass++;
   npcut = 0;
   p.lpOptimize("p");          /* Solve the LP */
   basis = p.saveBasis();      /* Save the current basis */
   objval = p.getObjVal();     /* Get the objective value */

      /* Get the solution values: */
   for(t=0;t<T;t++)
   {
    solprod[t]=prod[t].getSol();
    solsetup[t]=setup[t].getSol();
   }
  
      /* Search for violated constraints: */
   for(l=0;l<T;l++)
   {
    for(ds=0.0, t=0; t<=l; t++)
    {
     if(solprod[t] < D[t][l]*solsetup[t] + EPS)  ds += solprod[t];
     else  ds += D[t][l]*solsetup[t];
    }

      /* Add the violated inequality: the minimum of the actual production
         prod[t] and the maximum potential production D[t][l]*setup[t] 
         in periods 0 to l must at least equal the total demand in periods 
         0 to l.
         sum(t=1:l) min(prod[t], D[t][l]*setup[t]) >= D[0][l]
       */
    if(ds < D[0][l] - EPS) 
    {
     le = new XPRBexpr();
     for(t=0;t<=l;t++) 
     {
      if (solprod[t] < D[t][l]*solsetup[t] + EPS)
       le .add(prod[t]);
      else
       le .add(setup[t].mul(D[t][l]));
     }
     p.newCtr("cut" +(ncut+1), le.gEql(D[0][l]) );
     ncut++; 
     npcut++;
    }
   }
   
   System.out.println("Pass " +npass + " (" +(XPRB.getTime()-starttime)/1000.0 
      + " sec), objective value " + objval + ", cuts added: " + npcut 
      + " (total " + ncut +")");

   if(npcut==0) 
    System.out.println("Optimal integer solution found:");
   else
   { 
    p.loadMat();                 /* Reload the problem */
    p.loadBasis(basis);          /* Load the saved basis */
    basis = null;                /* No need to keep the basis any longer */
   }
  } while(npcut>0);

      /* Print out the solution: */
  for(t=0;t<T;t++)
   System.out.println("Period " + (t+1) + ": prod " + prod[t].getSol()
      + " (demand: " + DEMAND[t] + ", cost: " + PRODCOST[t] + "), setup " 
      + setup[t].getSol() + " (cost: " + SETUPCOST[t] + ")"); 
 }

/***********************************************************************/

 public static void main(String[] args) throws XPRSprobException, XPRSexception
 {
  modEls();                      /* Model the problem */
  solveEls();                    /* Solve the problem */
 } 
}
