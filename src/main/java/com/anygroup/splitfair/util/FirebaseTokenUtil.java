package com.anygroup.splitfair.util;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.FirebaseAuthException;

public class FirebaseTokenUtil {

    public static FirebaseToken verify(String token) {
        try {
            // Sử dụng verifyIdToken(token, true) để kiểm tra token có bị thu hồi không
            return FirebaseAuth.getInstance().verifyIdToken(token);
        } catch (FirebaseAuthException e) {
            // In ra mã lỗi cụ thể (ví dụ: INVALID_ID_TOKEN, TOKEN_EXPIRED)
            System.err.println("Firebase Auth Error Code: " + e.getAuthErrorCode());
            System.err.println("Firebase Auth Message: " + e.getMessage());
            throw new RuntimeException("Xác thực Firebase thất bại: " + e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi hệ thống khi verify token", e);
        }
    }
}