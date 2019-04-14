package com.edu.simple;

/**
 * @author zhangzhe
 * @date 2019/4/13 19:19
 */
public interface DiscardPolicy {

    void discard () throws DiscardException;
}
