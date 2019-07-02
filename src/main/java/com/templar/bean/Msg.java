package com.templar.bean;

import java.util.Date;

public class Msg {

    private String user;
    private String msg;
    private Date date;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Msg(String user, String msg, Date date) {
        this.user = user;
        this.msg = msg;
        this.date = date;
    }
}
