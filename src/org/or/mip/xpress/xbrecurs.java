package org.or.mip.xpress; /********************************************************
  Xpress-BCL Java Example Problems
  ================================

  file xbrecurs.java
  ``````````````````
  Recursion solving a non-linear financial planning problem
  The problem is to solve
  	net(t) = Payments(t)  - interest(t)
  	balance(t) = balance(t-1) - net(t)
  	interest(t) = (92/365) * balance(t) * interest_rate
  where
        balance(0) = 0
        balance[T] = 0
  for interest_rate

  (c) 2008 Fair Isaac Corporation
      author: S.Heipcke, 2001, rev. Mar. 2011
********************************************************/

import java.lang.*;
import com.dashoptimization.*;

public class xbrecurs
{
 static final int T=6;

/****DATA****/
 static double X = 0.00;         /* An INITIAL GUESS as to interest rate x */
 static final double[] B = 
          /* {796.35, 589.8918, 398.1351, 201.5451, 0.0, 0.0}; */
             {1,1,1,1,1,1};      /* An INITIAL GUESS as to balances b(t) */
 static final double[] P = {-1000, 0, 0, 0, 0, 0};            /* Payments */
 static final double[] R = {206.6, 206.6, 206.6, 206.6, 206.6, 0}; /*  "  */
 static final double[] V = {-2.95, 0, 0, 0, 0, 0};                 /*  "  */

 static XPRBvar[] b;             /* Balance */
 static XPRBvar x;               /* Interest rate */
 static XPRBvar dx;              /* Change to x */
 static XPRBctr[] interest;
 static XPRBctr ctrd;
 static XPRB bcl;
 static XPRBprob p;

/***********************************************************************/

 static void modFinNLP() throws XPRSexception
 {
  XPRBvar[] i;                   /* Interest */
  XPRBvar[] n;                   /* Net */
  XPRBvar[] epl, emn;            /* + and - deviations */
  XPRBexpr cobj, le;
  int t;

  bcl = new XPRB();              /* Initialize BCL */
  p = bcl.newProb("Fin_nlp");    /* Create a new problem in BCL*/
  XPRS.init();                   /* Initialize Xpress-Optimizer */
 
/****VARIABLES****/
  b = new XPRBvar[T];
  i = new XPRBvar[T];
  n = new XPRBvar[T];
  epl = new XPRBvar[T];
  emn = new XPRBvar[T];
  for(t=0;t<T;t++)
  {
   i[t]=p.newVar("i");
   n[t]=p.newVar("n", XPRB.PL, -XPRB.INFINITY, XPRB.INFINITY);
   b[t]=p.newVar("b", XPRB.PL, -XPRB.INFINITY, XPRB.INFINITY);
   epl[t]=p.newVar("epl");
   emn[t]=p.newVar("emn");
  }
  x=p.newVar("x");
  dx=p.newVar("dx", XPRB.PL, -XPRB.INFINITY, XPRB.INFINITY);
  i[0].fix(0);
  b[T-1].fix(0);
  
/****OBJECTIVE****/
  cobj = new XPRBexpr();
  for(t=0;t<T;t++)               /* Objective: get feasible */
   cobj.add(epl[t]).add(emn[t]);
  p.setObj(cobj);                /* Select objective function */ 
  p.setSense(XPRB.MINIM);        /* Choose the sense of the optimization */   

/****CONSTRAINTS****/
  for(t=0;t<T;t++)
  {                              /* net = payments - interest */
   p.newCtr("net", n[t].eql(i[t].mul(-1).add(P[t]).add(R[t]).add(V[t])) );
                                 /* Money balance across periods */
   if(t>0) p.newCtr("bal", b[t].eql(b[t-1].add(n[t].mul(-1))) );
   else  p.newCtr("bal", b[t].eql(n[t].mul(-1)) );
  }

  interest = new XPRBctr[T];
  for(t=1;t<T;t++)    /* i(t) = (92/365)*( b(t-1)*X + B(t-1)*dx ) approx. */
   interest[t] =
    p.newCtr("int", b[t-1].mul(X) .add(dx.mul(B[t-1])) .add(epl[t]).add(emn[t])
     .eql(i[t].mul(365/92.0)) );
 
  ctrd = p.newCtr("def", x.eql(dx.add(X)) );   /* x = dx + X */
 }

/**************************************************************************/
/*  Recursion loop (repeat until variation of x converges to 0):          */
/*    save the current basis and the solutions for variables b[t] and x   */
/*    set the balance estimates B[t] to the value of b[t]                 */
/*    set the interest rate estimate X to the value of x                  */
/*    reload the problem and the saved basis                              */
/*    solve the LP and calculate the variation of x                       */
/**************************************************************************/
 static void solveFinNLP() throws XPRSexception
 {
  XPRBbasis basis;
  double variation=1.0, oldval, XC;
  double[] BC;
  int t, ct=0;

  p.getXPRSprob().setIntControl(XPRS.CUTSTRATEGY, 0);
                                  /* Switch automatic cut generation off */

  p.lpOptimize("");               /* Solve the LP-problem */

  BC = new double[T]; 
  while(variation>0.000001)
  {
   ct++;
   basis=p.saveBasis();           /* Save the current basis */

                                  /* Get the solution values for b and x */
   for(t=1;t<T;t++) BC[t-1]=b[t-1].getSol();
   XC=x.getSol();
   System.out.println("Loop " + ct + ": " + x.getName() + ":" + x.getSol()   
    + " (variation:" + variation + ")");
                   
   for(t=1;t<T;t++)               /* Change coefficients in interest[t] */
   {   
    interest[t].setTerm(dx, BC[t-1]);
    B[t-1]=BC[t-1];
    interest[t].setTerm(b[t-1], XC);
   }
   ctrd.setTerm(XC);              /* Change constant term of ctrd */ 
   X=XC;

   oldval=XC;
   p.loadMat();                   /* Reload the problem */
   p.loadBasis(basis);            /* Load the saved basis */
   basis=null;                    /* No need to keep the basis any longer */

   p.lpOptimize("");              /* Solve the LP-problem */
   variation=Math.abs(x.getSol()-oldval);
  }

  System.out.println("Objective: " + p.getObjVal()); /* Get objective value */
  System.out.println("Interest rate: " + x.getSol()*100 + "%");
  System.out.print("Balances:  ");
  for(t=0;t<T;t++)                /* Print out the solution values */
   System.out.print(b[t].getName() + ":" + b[t].getSol() + " ");
  System.out.println();
 } 
 
/***********************************************************************/

 public static void main(String[] args) throws XPRSprobException, XPRSexception
 {
  modFinNLP();                    /* Model the problem */
  solveFinNLP();                  /* Solve the problem */
 }
} 
