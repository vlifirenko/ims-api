package com.ims.vos;

public class LinkVo implements Comparable {

    public String node1;
    public String node2;
    public Float coefficient = 0.1f;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LinkVo))
            return false;
        LinkVo link = (LinkVo) obj;
        return (this.node1.equals(link.node1) && this.node2.equals(link.node2)
                || this.node1.equals(link.node2) && this.node2.equals(link.node1));
    }

    @Override
    public int compareTo(Object o) {
        return this.coefficient.compareTo(((LinkVo) o).coefficient);
    }
}
