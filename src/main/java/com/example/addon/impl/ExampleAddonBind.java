package com.example.addon.impl;

import dev.boze.api.input.Bind;

public class ExampleAddonBind implements Bind {

    private int bind;
    private boolean button;

    public ExampleAddonBind(int bind, boolean button) {
        this.bind = bind;
        this.button = button;
    }

    public void setBind(int bind, boolean button) {
        this.bind = bind;
        this.button = button;
    }

    @Override
    public int getBind() {
        return bind;
    }

    @Override
    public boolean isButton() {
        return button;
    }
}
