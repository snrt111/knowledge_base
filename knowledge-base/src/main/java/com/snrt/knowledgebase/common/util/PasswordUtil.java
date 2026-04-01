package com.snrt.knowledgebase.common.util;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

/**
 * 密码加密工具类
 * 
 * 使用 BCrypt 进行密码加密和验证
 * 
 * @author SNRT
 * @since 1.0
 */
@Component
public class PasswordUtil {

    /**
     * 加密密码
     * 
     * @param password 原始密码
     * @return 加密后的密码
     */
    public String encode(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * 验证密码
     * 
     * @param password 原始密码
     * @param hashedPassword 加密后的密码
     * @return 是否匹配
     */
    public boolean matches(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}
