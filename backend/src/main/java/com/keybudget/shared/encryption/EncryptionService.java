package com.keybudget.shared.encryption;

public interface EncryptionService {
    String encrypt(String plaintext);
    String decrypt(String ciphertext);
}
