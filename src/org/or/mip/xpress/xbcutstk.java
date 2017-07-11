package org.or.mip.xpress; /********************************************************
  Xpress-BCL Java Example Problems
  ================================

  file xbcutstk.java
  ``````````````````
  Cutting stock problem, solved by column (= cutting
  pattern) generation heuristic looping over the
  root node.

  (c) 2008 Fair Isaac Corporation
      author: S.Heipcke, 2001, rev. Mar. 2014
********************************************************/

import java.lang.*;
import com.dashoptimization.*;

public class xbcutstk
{
 static final int NWIDTHS = 5;
 static final int MAXWIDTH = 94;

 static final double EPS = 1e-6;
 static final int MAXCOL = 10;

/****DATA****/
 static final double[] WIDTH = {17, 21, 22.5,  24, 29.5}; /* Possible widths */
 static final int[] DEMAND = {150, 96,   48, 108,  227};  /* Demand per width */
 static int[][] PATTERNS;    /* (Basic) cutting patterns */

 static XPRBvar[] pat;              /* Rolls per pattern */
 static XPRBctr[] dem;              /* Demand constraints */
 static XPRBctr cobj;               /* Objective function */

 static XPRB bcl;
 static XPRBprob p;

/***********************************************************************/

 static void modCutStock() throws XPRSexception
 {
  int i,j;
  XPRBexpr le;

  bcl = new XPRB();                 /* Initialize BCL */
  p = bcl.newProb("CutStock");      /* Create a new problem in BCL */
  XPRS.init();

  PATTERNS = new int[NWIDTHS][NWIDTHS];
  for(j=0;j<NWIDTHS;j++) 
   PATTERNS[j][j] = (int)Math.floor((double)MAXWIDTH/WIDTH[j]);
 
 /****VARIABLES****/
  pat = new XPRBvar[NWIDTHS+MAXCOL];
  for(j=0;j<NWIDTHS;j++)
   pat[j]=p.newVar("pat_"+(j+1), XPRB.UI, 0,
       (int)Math.ceil((double)DEMAND[j]/PATTERNS[j][j]));
       
/****OBJECTIVE****/
  le = new XPRBexpr();
  for(j=0;j<NWIDTHS;j++) le.add(pat[j]);
  cobj = p.newCtr("OBJ", le);
  p.setObj(cobj);                  /* Minimize total number of rolls */

/****CONSTRAINTS****/
  dem = new XPRBctr[NWIDTHS];
  for(i=0;i<NWIDTHS;i++)           /* Satisfy the demand per width */
  { 
   le = new XPRBexpr();
   for(j=0;j<NWIDTHS;j++)
    le .add(pat[j].mul(PATTERNS[i][j]));
   dem[i] = p.newCtr("Demand", le.gEql(DEMAND[i]) );
  }
 }

/**************************************************************************/
/*  Column generation loop at the top node:                               */
/*    solve the LP and save the basis                                     */
/*    get the solution values                                             */
/*    generate new column(s) (=cutting pattern)                           */
/*    load the modified problem and load the saved basis                  */
/**************************************************************************/
 static void solveCutStock()  throws XPRSexception
 {
  double objval=0;                /* Objective value */
  int i,j;
  int starttime;
  int npatt, npass;               /* Counters for columns and passes */
  double[] solpat;                /* Solution values for variables pat */
  double[] dualdem;               /* Dual values of demand constraints */
  XPRBbasis basis;
  double dw,z;
  int[] x;

  x = new int[NWIDTHS];
  solpat = new double[NWIDTHS+MAXCOL];
  dualdem = new double[NWIDTHS];

  starttime=XPRB.getTime();
  npatt = NWIDTHS;

  for(npass=0;npass<MAXCOL;npass++)
  {
   p.lpOptimize("");               /* Solve the LP */
   basis = p.saveBasis();          /* Save the current basis */
   objval = p.getObjVal();         /* Get the objective value */     

      /* Get the solution values: */
   for(j=0;j<npatt;j++)  solpat[j]=pat[j].getSol();
   for(i=0;i<NWIDTHS;i++)  dualdem[i]=dem[i].getDual();
  
      /* Solve integer knapsack problem  z = min{cx : ax<=r, x in Z^n}
         with r=MAXWIDTH, n=NWIDTHS */
   z = knapsack(NWIDTHS, dualdem, WIDTH, (double)MAXWIDTH, DEMAND, x);
      /* Force garbage collection to clean up subproblem: */
/*   System.gc();
   System.runFinalization();
*/    
   System.out.print("(" + (XPRB.getTime()-starttime)/1000.0 + " sec) Pass " +
     (npass+1) + ": ");
   
   if(z < 1+EPS)
   {
    System.out.println("no profitable column found.");
    System.out.println();
    basis=null;                     /* No need to keep the basis any longer */
    break;
   }
   else 
   {
      /* Print the new pattern: */
    System.out.println("new pattern found with marginal cost " + (z-1) );
    System.out.print("   Widths distribution: ");
    dw=0;
    for(i=0;i<NWIDTHS;i++)  
    {
     System.out.print(WIDTH[i] +":"+ x[i] +"  ");
     dw += WIDTH[i]*x[i];
    } 
    System.out.println("Total width: " + dw);

      /* Create a new variable for this pattern: */
    pat[npatt]=p.newVar("pat_"+(npatt+1),XPRB.UI);

    cobj .add(pat[npatt]);          /* Add new var. to the objective */
    dw=0;
    for(i=0; i<NWIDTHS; i++)        /* Add new var. to demand constraints*/
     if(x[i] > EPS)
     {
      dem[i].add(pat[npatt].mul(x[i]));
      if((int)Math.ceil((double)DEMAND[i]/x[i]) > dw)
       dw = (int)Math.ceil((double)DEMAND[i]/x[i]);
     }
    pat[npatt].setUB(dw);           /* Change the upper bound on the new var.*/   
    npatt++;

    p.loadMat();                    /* Reload the problem */
    p.loadBasis(basis);             /* Load the saved basis */
    basis=null;                     /* No need to keep the basis any longer */
   }
  }

  p.mipOptimize("");                /* Solve the MIP */
  
  System.out.println("(" + (XPRB.getTime()-starttime)/1000.0 + 
    " sec) Optimal solution: " + p.getObjVal() + " rolls, " + npatt + 
    " patterns");
  System.out.print("   Rolls per pattern: ");
  for(i=0;i<npatt;i++) System.out.print(pat[i].getSol() + ", ");
  System.out.println();
 }

/**************************************************************************/
/* Integer Knapsack Algorithm for solving the integer knapsack problem    */
/*    z = max{cx : ax <= R, x <= d, x in Z^N}                             */
/*                                                                        */
/* Input data:                                                            */
/*   N:        Number of item types                                       */
/*   c[i]:     Unit profit of item type i, i=1..n                         */
/*   a[i]:     Unit resource use of item type i, i=1..n                   */
/*   R:        Total resource available                                   */
/*   d[i]:     Demand for item type i, i=1..n                             */
/* Return values:                                                         */
/*   xbest[i]: Number of items of type i used in optimal solution         */
/*   zbest:    Value of optimal solution                                  */
/**************************************************************************/
 static double knapsack(int N, double[] c, double[] a, double R, int[] d, int[] xbest)
 {
  int j;
  double zbest = 0.0;
  XPRBvar[] x;
  XPRBexpr klobj, knap;
  XPRBprob pk;

/*
  System.out.println("Solving z = max{cx : ax <= b; x in Z^n}");
  System.out.print("   c   =");
  for(j = 0; j < N; j++)  System.out.print(" " + c[j] + ",");
  System.out.println();
  System.out.print("   a   =");
  for(j = 0; j < N; j++)  System.out.print(" " + a[j] + ",");
  System.out.println();
  System.out.print("   c/a =");
  for (j = 0; j < N; j++)  System.out.print(" " + c[j]/a[j] + ",");
  System.out.println();
  System.out.print("   b   = " + R);
*/  

  pk = bcl.newProb("Knapsack"); 
  x = new XPRBvar[N];
  for(j=0;j<N;j++) x[j]=pk.newVar("x", XPRB.UI, 0, d[j]);

  klobj = new XPRBexpr();
  for(j=0;j<N;j++) klobj.add(x[j].mul(c[j]));
  pk.setObj(klobj);  
  knap = new XPRBexpr();
  for(j=0;j<N;j++) knap.add(x[j].mul(a[j]));
  pk.newCtr("KnapXPRBctr", knap.lEql(R) );
  pk.setSense(XPRB.MAXIM);
  pk.mipOptimize("");

  zbest = pk.getObjVal();
  for(j=0;j<N;j++) xbest[j]=(int)Math.floor(x[j].getSol() + 0.5);
/*
  System.out.println("   z = " + zbest);
  System.out.print("   x =");
  for(j=0; j<N; j++) System.out.print(" " + xbest[j] + ",");
  System.out.println();
*/

  x = null;
  pk = null;

  return (zbest);
}

/***********************************************************************/

 public static void main(String[] args) throws XPRSexception
 {
  modCutStock();                    /* Model the problem */
  solveCutStock();                  /* Solve the problem */
 }
} 

