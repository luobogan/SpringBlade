package org.springblade.order.vo;

import lombok.Data;

@Data
public class OrderCountStats {
    private int waitPay;
    private int waitReceive;
    private int waitComment;
    private int refund;

    public int getWaitPay() {
        return waitPay;
    }

    public void setWaitPay(int waitPay) {
        this.waitPay = waitPay;
    }

    public int getWaitReceive() {
        return waitReceive;
    }

    public void setWaitReceive(int waitReceive) {
        this.waitReceive = waitReceive;
    }

    public int getWaitComment() {
        return waitComment;
    }

    public void setWaitComment(int waitComment) {
        this.waitComment = waitComment;
    }

    public int getRefund() {
        return refund;
    }

    public void setRefund(int refund) {
        this.refund = refund;
    }
}
