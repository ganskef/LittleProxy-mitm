package de.ganskef.test;

public interface IProxy {

    public abstract int getProxyPort();

    public abstract IProxy start();

    public abstract void stop();

}