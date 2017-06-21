package org.or.mip.cg;

import com.dashoptimization.XPRB;
import com.dashoptimization.XPRBctr;
import com.dashoptimization.XPRBprob;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by baohuaw on 6/21/17.
 */
public class MultiCommodityFlow {
    XPRB bcl = new XPRB();
    XPRBprob problem = bcl.newProb("MCF");      /* Create a new problem in BCL */

    Map<Flow, XPRBctr> flowConstraints = new HashMap<>();
    Map<Flow, Double> flowDuals = new HashMap<>();
    Map<String, XPRBctr> edgeConstraints = new HashMap<>();
    Map<String, Double> edgeDuals = new HashMap<>();
}
