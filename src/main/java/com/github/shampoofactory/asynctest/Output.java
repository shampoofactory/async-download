package com.github.shampoofactory.asynctest;

/**
 *
 * @author vin
 * @param <T>
 */
public interface Output<T> extends WriteBytes {
     T into();
}
