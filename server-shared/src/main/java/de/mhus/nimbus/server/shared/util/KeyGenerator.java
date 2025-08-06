package de.mhus.nimbus.server.shared.util;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

/**
 * Utility class to generate RSA key pairs for JWT signing.
 * This is used to generate the initial key pair for the Identity Service.
 */
public class KeyGenerator {

    public static void main(String[] args) throws NoSuchAlgorithmException {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        keyGenerator.initialize(2048);
        KeyPair keyPair = keyGenerator.generateKeyPair();

        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        System.out.println("Private Key (Base64):");
        System.out.println("-----BEGIN PRIVATE KEY-----");
        System.out.println(Base64.getEncoder().encodeToString(privateKey.getEncoded()));
        System.out.println("-----END PRIVATE KEY-----");

        System.out.println("\nPublic Key (Base64):");
        System.out.println("-----BEGIN PUBLIC KEY-----");
        System.out.println(Base64.getEncoder().encodeToString(publicKey.getEncoded()));
        System.out.println("-----END PUBLIC KEY-----");
    }
}
