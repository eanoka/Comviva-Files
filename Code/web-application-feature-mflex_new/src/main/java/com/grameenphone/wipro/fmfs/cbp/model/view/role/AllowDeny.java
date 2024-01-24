package com.grameenphone.wipro.fmfs.cbp.model.view.role;

public class AllowDeny {
    public String name;
    public boolean allow;
    public boolean deny;

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AllowDeny ? ((AllowDeny) obj).name.equals(name) : super.equals(obj);
    }
}