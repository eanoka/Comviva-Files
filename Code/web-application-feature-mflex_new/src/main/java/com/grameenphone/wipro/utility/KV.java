package com.grameenphone.wipro.utility;

/**
 * To make passing key value pair array easy
 */
public class KV<A, B> {
    public A key;
    public B value;

    public KV(A a, B b) {
        key = a;
        value = b;
    }
}