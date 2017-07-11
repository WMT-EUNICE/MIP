package org.or.mip.xpress; /********************************************************
 Xpress-BCL Java Example Problems
 ================================

 file d1wagon2.java
 `````````````````
 Load balancing of train wagons
 (second version, using heuristic solution as
 start solution for MIP)

 (c) 2014 Fair Isaac Corporation
 author: L.Bertacco, 2014
 ********************************************************/

import java.util.*;

import com.dashoptimization.*;

public class xbd1wagon2 {
    /* Box weights */
    static final int[] WEIGHT = {34, 6, 8, 17, 16, 5, 13, 21, 25, 31, 14, 13, 33, 9, 25, 25};
    static final int NBOXES = WEIGHT.length;       /* Number of boxes                       */
    static final int NWAGONS = 3;                  /* Number of wagons                      */
    static final int WMAX = 100;                   /* Weight limit of the wagons            */
    static final int[] HeurSol = new int[NBOXES];  /* Heuristic solution: for each box      */

    static XPRB bcl;
    static XPRBprob prob;

    /****VARIABLES****/
    static final XPRBvar[][] load = new XPRBvar[NBOXES][NWAGONS];
    static XPRBvar maxweight;

    /***********************************************************************/

    static void d1w2_model() throws XPRSexception {
        /****VARIABLES****/
 
   /* Create load[box,wagon] (binary) */
        for (int b = 0; b < NBOXES; b++)
            for (int w = 0; w < NWAGONS; w++)
                load[b][w] = prob.newVar("load_" + (b + 1) + "_" + (w + 1), XPRB.BV);
 
   /* Create maxweight (continuous with lb=ceil((sum(b in BOXES) WEIGHT(b))/NBOXES) */
        double sum_weights = 0;
        for (int b = 0; b < NBOXES; b++) sum_weights += WEIGHT[b];
        maxweight = prob.newVar("maxweight", XPRB.PL, Math.ceil(sum_weights / NBOXES), XPRB.INFINITY);

        /****CONSTRAINTS****/
 
   /* Every box into one wagon: forall(b in BOXES) sum(w in WAGONS) load(b,w) = 1 */
        for (int b = 0; b < NBOXES; b++) {
            XPRBexpr eq = new XPRBexpr();
            for (int w = 0; w < NWAGONS; w++) eq.add(load[b][w]);
            prob.newCtr(eq.eql(1));
        }
 
   /* Limit the weight loaded into every wagon: forall(w in WAGONS) sum(b in BOXES) WEIGHT(b)*load(b,w) <= maxweight */
        for (int w = 0; w < NWAGONS; w++) {
            XPRBexpr le = new XPRBexpr();
            for (int b = 0; b < NBOXES; b++) le.add(load[b][w].mul(WEIGHT[b]));
            prob.newCtr(le.lEql(maxweight));
        }

        /****OBJECTIVE****/

        prob.setObj(maxweight);
        prob.setSense(XPRB.MINIM);
    }

    static void d1w2_solve() {
        int b, w;
        XPRSprob oprob = prob.getXPRSprob(); /* Get Optimizer problem */
    
   /* Alternative to lower bound on maxweight: adapt the optimizer cutoff value  */
   /* oprob.setDblControl(XPRS.MIPADDCUTOFF, -0.99999); */
 
   /* Comment out the following line to enable the optimizer log */
        oprob.setIntControl(XPRS.OUTPUTLOG, 0);
 
   /* Create a BCL solution from the heuristic solution we have found */
        XPRBsol sol = prob.newSol();
   /* Set the solution values for all discrete variables that are non-zero */
        for (b = 0; b < NBOXES; b++) sol.setVar(load[b][HeurSol[b]], 1);
 
   /* It is possible, but not necessary, to set values for ALL discrete vars  */
   /* by uncommenting the following line. In this case, the usersolnotify     */
   /* callback would return status equal to 2 (instead of 3), as the solution */
   /* would be feasible without the need of a local search.                   */
   /* for (b=0; b<NBOXES; b++) for (w=0; w<NWAGONS; w++) sol.setVar(load[b][w], w==HeurSol[b] ? 1 : 0); */

        prob.addMIPSol(sol, "heurSol");      /* Send the solution to the optimizer */
   /* Request notification of solution status after processing */
        oprob.addUserSolNotifyListener(new UserSolNotifyCallback());
 
   /* Parameter settings to make use of loaded solution */
        oprob.setDblControl(XPRS.HEURSEARCHEFFORT, 2);
        oprob.setIntControl(XPRS.HEURSEARCHROOTSELECT, 31);
        oprob.setIntControl(XPRS.HEURSEARCHTREESELECT, 19);

        prob.mipOptimize();          /* Solve the LP-problem */
        int statmip = prob.getMIPStat(); /* Get the problem status */
        if (statmip == XPRB.MIP_SOLUTION || statmip == XPRB.MIP_OPTIMAL) { /* An integer solution has been found */
            System.out.printf("Optimal solution:\n Max weight: %.0f\n", prob.getObjVal());
            for (w = 0; w < NWAGONS; w++) {
                int tot_weight = 0;
                System.out.print(" " + (w + 1) + ":");
                for (b = 0; b < NBOXES; b++)
                    if (load[b][w].getSol() > .5) {
                        System.out.print(" " + (b + 1));
                        tot_weight += WEIGHT[b];
                    }
                System.out.printf(" (total weight: %d)\n", tot_weight);
            }
        }
    }

    /***********************************************************************/

 /* LPT (Longest processing time) heuristic:     */
 /* One at a time, place the heaviest unassigned */
 /* box onto the wagon with the least load       */
    static double solve_heur() {
        Integer[] ORDERW = new Integer[NBOXES];   /* Box indices sorted in decreasing weight order                                              */
        int[] CurNum = new int[NWAGONS];          /* For each wagon w, this is the number of boxes currently loaded                             */
        int[] CurWeight = new int[NWAGONS];       /* For each wagon w, this is the current weight, i.e. the sum of weights of loaded boxes      */
        int[][] Load = new int[NWAGONS][NBOXES];  /* Load[w][i] (for i=0..CurNum[w]-1) contains the box index of the i-th box loaded on wagon w */

   /* Copy the box indices into array ORDERW and sort them in decreasing     */
   /* order of box weights (the sorted indices are returned in array ORDERW) */
        for (int b = 0; b < NBOXES; b++) ORDERW[b] = b;
        Arrays.sort(ORDERW, new Comparator<Integer>() {
            public int compare(Integer i1, Integer i2) {
                return ((Integer) WEIGHT[i2]).compareTo(WEIGHT[i1]);
            }
        });

   /* Distribute the loads to the wagons using the LPT heuristic  */
        for (int b = 0; b < NBOXES; b++) {
            int v = 0;                          /* Find wagon v with the smallest load */
            for (int w = 0; w < NWAGONS; w++) if (CurWeight[w] <= CurWeight[v]) v = w;
            Load[v][CurNum[v]] = ORDERW[b];     /* Add current box to wagon v */
            CurNum[v]++;                        /* Increase the counter of boxes on v */
            CurWeight[v] += WEIGHT[ORDERW[b]];  /* Update current weight of the wagon */
        }

   /* Calculate the solution value */
        double heurobj = 0;                   /* heuristic solution objective value (max wagon weight) */
        for (int w = 0; w < NWAGONS; w++) if (CurWeight[w] > heurobj) heurobj = CurWeight[w];

   /* Solution printing */
        System.out.printf("Heuristic solution:\n Max weight: %.0f\n", heurobj);

        for (int w = 0; w < NWAGONS; w++) {
            System.out.printf(" %d:", w + 1);
            for (int i = 0; i < CurNum[w]; i++) System.out.print(" " + (Load[w][i] + 1));
            System.out.printf(" (total weight: %d)\n", CurWeight[w]);
        }

     /* Save the heuristic solution into the HeurSol array */
        for (int w = 0; w < NWAGONS; w++) for (int i = 0; i < CurNum[w]; i++) HeurSol[Load[w][i]] = w;
        return heurobj;
    }

    /* Callback function reporting loaded solution status */
    static class UserSolNotifyCallback implements XPRSuserSolNotifyListener {
        public void XPRSuserSolNotifyEvent(XPRSprob oprob, Object data, String name, int status) {
            System.out.printf("Optimizer loaded solution %s with status=%d\n", name, status);
        }
    }

    /***********************************************************************/

    public static void main(String[] args) throws XPRSexception {
        if (solve_heur() <= WMAX) {
            System.out.println("Heuristic solution fits capacity limits");
        } else {
            XPRS.init();                      /* Initialize Xpress-Optimizer */
            bcl = new XPRB();                 /* Initialize BCL */
            prob = bcl.newProb("d1wagon2");   /* Create a new problem in BCL */
            d1w2_model();                     /* Model the problem */
            d1w2_solve();                     /* Solve the problem */
        }
    }

}