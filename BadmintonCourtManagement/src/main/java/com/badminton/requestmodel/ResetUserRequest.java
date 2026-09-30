package com.badminton.requestmodel;

import lombok.Data;

@Data
public class ResetUserRequest {
    private String userName;
    /** Base64 RSA-OAEP ciphertext — decrypted server-side before comparison. */
    private String newPass;
    /** Base64 RSA-OAEP ciphertext of the same plaintext as newPass. */
    private String repeatNewPass;
    private String resetToken;
}
