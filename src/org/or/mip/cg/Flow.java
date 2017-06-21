package org.or.mip.cg;

import org.jgrapht.GraphPath;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by baohuaw on 6/20/17.
 */
public class Flow {
    String from;
    String to;
    double volume;
    List<GraphPath> paths;

    public Flow(String from, String to, double volume) {
        this.from = from;
        this.to = to;
        this.volume = volume;
        paths = new LinkedList<>();
    }
}
