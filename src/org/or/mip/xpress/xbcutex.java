package org.or.mip.xpress;

/**
 * Created by baohuaw on 7/6/17.
 */

import com.dashoptimization.*;

public class xbcutex {
    static XPRBvar[] start;
    static XPRB bcl;

    static class CutMgrCallback implements XPRScutMgrListener {
        public int XPRScutMgrEvent(XPRSprob oprob, Object data) {
            XPRBprob bprob;
            XPRBcut[] ca;
            int num, i;

            bprob = (XPRBprob) data;             /* Get the BCL problem */

            try {
                bprob.beginCB(oprob);              /* Coordinate BCL and Optimizer */
                num = oprob.getIntAttrib(XPRS.NODES);
                if (num == 2)                       /* Only generate cuts at node 2 */ {
                    ca = new XPRBcut[2];
                    ca[0] = bprob.newCut(start[1].add(2).lEql(start[0]), 2);
                    ca[1] = bprob.newCut(start[2].mul(4).add(start[3].mul(-5.3)).lEql(-17), 2);
                    System.out.println("Adding constraints:");
                    for (i = 0; i < 2; i++) ca[i].print();
                    bprob.addCuts(ca);
                }
                bprob.endCB();                     /* Reset BCL to main problem */
            } catch (XPRSprobException e) {
                System.out.println("Error  " + e.getCode() + ": " + e.getMessage());
            }
            return 0;                           /* Call this method once per node */
        }
    }

    public static void main(String[] args) throws XPRSexception {
        XPRBprob p;
        XPRSprob oprob;
        CutMgrCallback cb;

        bcl = new XPRB();                    /* Initialize BCL */
        p = bcl.newProb("Jobs");             /* Create a new problem */
        XPRS.init();                         /* Initialize Xpress-Optimizer */

        start = new XPRBvar[4];              /* Create 'start' variables */
        for (int j = 0; j < 4; j++)
            start[j] = p.newVar("start");
                     /* Define constraints and an objective function */

        oprob = p.getXPRSprob();             /* Get Optimizer problem */
        p.setCutMode(1);                     /* Enable the cut mode */
        cb = new CutMgrCallback();
        oprob.addCutMgrListener(cb, p);      /* Def. the cut manager callback */
        p.mipOptimize();                     /* Solve the problem as MIP */
                                  /* Solution output */
    }
}
