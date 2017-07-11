package org.or.mip.xpress; /********************************************************
 Xpress-BCL Java Example Problems
 ================================

 file xbfixbv.java
 `````````````````
 Using the complete Coco Problem, as in xbcoco.java
 this program implements a binary fixing heuristic.

 (c) 2008 Fair Isaac Corporation
 author: S.Heipcke, 2001, rev. Mar. 2011
 ********************************************************/

import com.dashoptimization.*;

public class xbfixbv {
    static final double TOL = 5.0E-4;

    static final int PHASE = 5;
/* Phase = 4: Mines may open/closed freely; when closed save 20000 per month
 * Phase = 5: Once closed always closed; larger saving */

    static final int NP = 2;            /* Number of products (p) */
    static final int NF = 2;            /*           factories (f) */
    static final int NR = 2;            /*           raw materials (r) */
    static final int NT = 4;            /*           time periods (t) */

    /****DATA****/
    static final double[][] REV =
            {{400, 380, 405, 350},
                    {410, 397, 412, 397}};
    /* Unit selling price of prod. p in period t */
    static final double[][] CMAK =
            {{150, 153},
                    {75, 68}}; /* Unit cost to make product p at factory f */
    static final double[][] CBUY =
            {{100, 98, 97, 100},
                    {200, 195, 198, 200}};
    /* Unit cost to buy raw material r in period t */
    static final double[] COPEN = {50000, 63000};
    /* Fixed cost of factory f being open for one period */
    static final double CPSTOCK = 2.0; /* Unit cost to store any product p */
    static final double CRSTOCK = 1.0; /* Unit cost to store any raw material r */
    static final double[][] REQ =
            {{1.0, 0.5},
                    {1.3, 0.4}}; /* Requirement by unit of prod. p for raw material r */
    static final double[][] MXSELL =
            {{650, 600, 500, 400},
                    {600, 500, 300, 250}};
    /* Max. amount of p that can be sold in period t */
    static final double[] MXMAKE = {400, 500};
    /* Max. amount factory f can make over all products */
    static final double MXRSTOCK = 300;
    /* Max. amount of r that can be stored each f and t */
    static final double[][] PSTOCK0 =
            {{50, 100},
                    {50, 50}};  /* Initial product p stock level at factory f */
    static final double[][] RSTOCK0 =
            {{100, 150},
                    {50, 100}}; /* Initial raw material r stock level at factory f*/

    static XPRB bcl;
    static XPRBprob pb;
    static XPRBvar[][] openm;

    /***********************************************************************/

    static void modCoco() throws XPRSexception {
        XPRBvar[][][] make, sell, pstock, buy, rstock;
        XPRBexpr lobj, lc;
        int p, f, r, t;

        bcl = new XPRB();               /* Initialize BCL */
        pb = bcl.newProb("Coco");       /* Create a new problem in BCL */
        XPRS.init();                    /* Initialize Xpress-Optimizer */

/****VARIABLES****/
        make = new XPRBvar[NP][NF][NT];
        sell = new XPRBvar[NP][NF][NT];
        pstock = new XPRBvar[NP][NF][NT + 1];
        buy = new XPRBvar[NR][NF][NT];
        rstock = new XPRBvar[NR][NF][NT + 1];
        openm = new XPRBvar[NF][NT];
        for (p = 0; p < NP; p++)
            for (f = 0; f < NF; f++) {
                for (t = 0; t < NT; t++) {
                    make[p][f][t] = pb.newVar("make_p" + (p + 1) + "_f" + (f + 1));
        /* Amount of prod. p to make at factory f in period t */
                    sell[p][f][t] = pb.newVar("sell_p" + (p + 1) + "_f" + (f + 1));
        /* Amount of prod. p sold from factory f in period t */
                }
                for (t = 0; t < NT + 1; t++)
                    pstock[p][f][t] = pb.newVar("pstock_p" + (p + 1) + "_f" + (f + 1));
        /* Stock level of prod. p at factory f at start of period t */
            }
        for (r = 0; r < NR; r++)
            for (f = 0; f < NF; f++) {
                for (t = 0; t < NT; t++)
                    buy[r][f][t] = pb.newVar("buy_r" + (r + 1) + "_f" + (f + 1));
        /* Amount of raw material r bought for factory f in period t */
                for (t = 0; t < NT + 1; t++)
                    rstock[r][f][t] = pb.newVar("rstock_r" + (r + 1) + "_f" + (f + 1));
        /* Stock level of raw mat. r at factory f at start of per. t */
            }

        for (f = 0; f < NF; f++)
            for (t = 0; t < NT; t++)
                openm[f][t] = pb.newVar("open_f" + (f + 1), XPRB.BV);
        /* 1 if factory f is open in period t, else 0 */

/****OBJECTIVE****/
        lobj = new XPRBexpr();
        for (f = 0; f < NF; f++)        /* Objective: maximize total profit */ {
            for (p = 0; p < NP; p++) {
                for (t = 0; t < NT; t++)
                    lobj.add(sell[p][f][t].mul(REV[p][t]))
                            .add(make[p][f][t].mul(-CMAK[p][f]));
                for (t = 1; t < NT + 1; t++) lobj.add(pstock[p][f][t].mul(-CPSTOCK));
            }
            if (PHASE == 4)
                for (t = 0; t < NT; t++) lobj.add(openm[f][t].mul(20000 - COPEN[f]));
            else if (PHASE == 5)
                for (t = 0; t < NT; t++) lobj.add(openm[f][t].mul(-COPEN[f]));
            for (r = 0; r < NR; r++) {
                for (t = 0; t < NT; t++) lobj.add(buy[r][f][t].mul(-CBUY[r][t]));
                for (t = 1; t < NT + 1; t++) lobj.add(rstock[r][f][t].mul(-CRSTOCK));
            }
        }
        pb.setObj(lobj);         /* Set objective function */

/****CONSTRAINTS****/
        for (p = 0; p < NP; p++)        /* Product stock balance */
            for (f = 0; f < NF; f++)
                for (t = 0; t < NT; t++)
                    pb.newCtr("PBal", pstock[p][f][t].add(make[p][f][t])
                            .eql(sell[p][f][t].add(pstock[p][f][t + 1])));

        for (r = 0; r < NR; r++)        /* Raw material stock balance */
            for (f = 0; f < NF; f++)
                for (t = 0; t < NT; t++) {
                    lc = new XPRBexpr();
                    for (p = 0; p < NP; p++) lc.add(make[p][f][t].mul(REQ[p][r]));
                    pb.newCtr("RBal", rstock[r][f][t].add(buy[r][f][t])
                            .eql(lc.add(rstock[r][f][t + 1])));
                }

        for (p = 0; p < NP; p++)
            for (t = 0; t < NT; t++) {                       /* Limit on the amount of product p to be sold */
                lc = new XPRBexpr();
                for (f = 0; f < NF; f++) lc.add(sell[p][f][t]);
                pb.newCtr("MxSell", lc.lEql(MXSELL[p][t]));
            }

        for (f = 0; f < NF; f++)
            for (t = 0; t < NT; t++) {                       /* Capacity limit at factory f */
                lc = new XPRBexpr();
                for (p = 0; p < NP; p++) lc.add(make[p][f][t]);
                pb.newCtr("MxMake", lc.lEql(openm[f][t].mul(MXMAKE[f])));
            }

        for (f = 0; f < NF; f++)
            for (t = 1; t < NT + 1; t++) {                       /* Raw material stock limit */
                lc = new XPRBexpr();
                for (r = 0; r < NR; r++) lc.add(rstock[r][f][t]);
                pb.newCtr("MxRStock", lc.lEql(MXRSTOCK));
            }

        if (PHASE == 5)
            for (f = 0; f < NF; f++)
                for (t = 0; t < NT - 1; t++)    /* Once closed, always closed */
                    pb.newCtr("Closed", openm[f][t + 1].lEql(openm[f][t]));

/****BOUNDS****/
        for (p = 0; p < NP; p++)
            for (f = 0; f < NF; f++)
                pstock[p][f][0].fix(PSTOCK0[p][f]);    /* Initial product levels */

        for (r = 0; r < NR; r++)
            for (f = 0; f < NF; f++)
                rstock[r][f][0].fix(RSTOCK0[r][f]);    /* Initial raw mat. levels */

        if (PHASE <= 3)
            for (f = 0; f < NF; f++)
                for (t = 0; t < NT; t++)
                    openm[f][t].fix(1);
    }

/**************************************************************************/
/*  Solution heuristic:                                                   */
/*    solve the LP and save the basis                                     */
/*    fix all open variables which are integer feasible at the relaxation */
/*    solve the MIP and save the best solution value                      */
/*    reset all variables to their original bounds                        */
/*    load the saved basis                                                */
/*    solve the MIP using the solution value found previously as cutoff   */

    /**************************************************************************/
    static void solveCoco() throws XPRSexception {
        XPRSprob po;
        XPRBbasis basis;
        int f, t, ifgsol;
        double solval = 0;
        double[][] osol;

/****SOLVING + OUTPUT****/
        po = pb.getXPRSprob();
        po.setIntControl(XPRS.CUTSTRATEGY, 0);
                             /* Disable automatic cuts - we use a heuristic */
        po.setIntControl(XPRS.PRESOLVE, 0);
                             /* Switch presolve off */
        pb.setSense(XPRB.MAXIM);   /* Choose the sense of the optimization */
        pb.mipOptimize("l");       /* Solve the LP-relaxation */
        basis = pb.saveBasis();      /* Save the current basis */
 
      /* For t=0,1 get the solution values of the open variables: */
        osol = new double[NF][2];
        for (f = 0; f < NF; f++)
            for (t = 0; t < 2; t++) {
                osol[f][t] = openm[f][t].getSol();
      /* Fix all variables which are integer feasible: */
                if (osol[f][t] < TOL) openm[f][t].setUB(0.0);
                else if (1 - osol[f][t] < TOL) openm[f][t].setLB(1.0);
            }

        pb.mipOptimize("c");       /* Continue solving the MIP-problem */
        ifgsol = 0;
        if (pb.getMIPStat() == XPRS.MIP_SOLUTION || pb.getMIPStat() == XPRS.MIP_OPTIMAL) {                          /* If a global solution was found */
            ifgsol = 1;
            solval = pb.getObjVal();    /* Get the value of the best solution */
        }

    /* Reset variables to their original bounds: */
        for (f = 0; f < NF; f++)
            for (t = 0; t < 2; t++)
                if ((osol[f][t] < TOL) || (1 - osol[f][t] < TOL)) {
                    openm[f][t].setLB(0.0);
                    openm[f][t].setUB(1.0);
                }

        pb.loadBasis(basis);      /* Load the saved basis: bound changes are
                               immediately passed on from BCL to the
                               Optimizer if the problem has not been modified
                               in any other way, so that there is no need to 
                               reload the matrix */
        basis = null;             /* No need to store the saved basis any longer */
        if (ifgsol == 1)
            po.setDblControl(XPRS.MIPABSCUTOFF, solval);
                            /* Set the cutoff to the best known solution */
        pb.mipOptimize("");       /* Solve the MIP-problem */

        if (pb.getMIPStat() == XPRS.MIP_SOLUTION || pb.getMIPStat() == XPRS.MIP_OPTIMAL)
            System.out.println("Best integer solution: " + pb.getObjVal());
        else
            System.out.println("Best integer solution: " + solval);
    }

    /***********************************************************************/

    public static void main(String[] args) throws XPRSexception {
        modCoco();                 /* Model the problem */
        solveCoco();               /* Solve the problem */
    }

} 
