/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.simple.ipeer.config;

/**
 *
 * @author iPeer
 */
public class UnsupportedConfigOperationException extends Exception {

    /**
     * Creates a new instance of <code>UnsupportedOperationException</code> without detail message.
     */
    public UnsupportedConfigOperationException() {
    }

    /**
     * Constructs an instance of <code>UnsupportedOperationException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public UnsupportedConfigOperationException(String msg) {
	super(msg);
    }
}
