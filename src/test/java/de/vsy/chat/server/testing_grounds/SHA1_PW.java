package de.vsy.chat.server.testing_grounds;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 * @author fredward
 */
public class SHA1_PW {

  public static void main(String[] args) throws NoSuchAlgorithmException {
    final var pwList = new HashMap<String, String>();
    BigInteger test;
    byte[] pwDigest;
    MessageDigest digester = MessageDigest.getInstance("SHA-1");

    pwDigest = digester.digest("Xlite/802/R".getBytes(StandardCharsets.UTF_8));
    pwList.put("Xl//", new BigInteger(1, pwDigest).toString(16).toUpperCase());

    pwDigest = digester.digest("XLite/802/R".getBytes(StandardCharsets.UTF_8));
    pwList.put("XL//", new BigInteger(1, pwDigest).toString(16).toUpperCase());

    pwDigest = digester.digest("Xlite/802R".getBytes(StandardCharsets.UTF_8));
    pwList.put("Xl/", new BigInteger(1, pwDigest).toString(16).toUpperCase());

    pwDigest = digester.digest("Xlite802R".getBytes(StandardCharsets.UTF_8));
    pwList.put("Xl", new BigInteger(1, pwDigest).toString(16).toUpperCase());

    pwDigest = digester.digest("xlite802R".getBytes(StandardCharsets.UTF_8));
    pwList.put("xl", new BigInteger(1, pwDigest).toString(16).toUpperCase());

    pwDigest = digester.digest("BrummBagger1".getBytes(StandardCharsets.UTF_8));
    pwList.put("Brumm", new BigInteger(1, pwDigest).toString(16).toUpperCase());

    pwDigest = digester.digest("XL/802/R".getBytes(StandardCharsets.UTF_8));
    pwList.put("XL//", new BigInteger(1, pwDigest).toString(16).toUpperCase());

    for (final var pwSet : pwList.entrySet()) {
    }
  }
}
