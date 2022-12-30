package com.ims.vos;

import java.util.Date;

public class StatisticVo {

    public Date timeStamp;
    public int createdCount;

    public StatisticVo(Date dateStamp) {
        this.timeStamp = dateStamp;
        this.createdCount = 1;
    }
}
