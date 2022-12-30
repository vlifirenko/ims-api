package com.ims.vos;

public class NodeVo implements Comparable {

    public String name;
    public String color;
    public Float coefficient = 0.1f;

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NodeVo && this.name.equals(((NodeVo) obj).name);
    }

    @Override
    public int compareTo(Object o) {
        return this.coefficient.compareTo(((NodeVo) o).coefficient);
    }
}
