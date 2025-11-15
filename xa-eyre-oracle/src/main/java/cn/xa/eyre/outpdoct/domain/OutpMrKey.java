package cn.xa.eyre.outpdoct.domain;

import java.util.Date;
import java.util.StringJoiner;

public class OutpMrKey {
    private Date visitDate;

    private Short visitNo;

    private Short ordinal;

    public Date getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(Date visitDate) {
        this.visitDate = visitDate;
    }

    public Short getVisitNo() {
        return visitNo;
    }

    public void setVisitNo(Short visitNo) {
        this.visitNo = visitNo;
    }

    public Short getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(Short ordinal) {
        this.ordinal = ordinal;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", OutpMrKey.class.getSimpleName() + "[", "]")
                .add("visitDate=" + visitDate)
                .add("visitNo=" + visitNo)
                .add("ordinal=" + ordinal)
                .toString();
    }
}