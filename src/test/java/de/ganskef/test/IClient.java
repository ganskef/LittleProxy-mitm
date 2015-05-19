package de.ganskef.test;

import java.io.File;

public interface IClient {

    public abstract File get(String url, IProxy proxy) throws Exception;

    public abstract File get(String url) throws Exception;

}