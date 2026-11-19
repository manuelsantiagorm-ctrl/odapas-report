package mx.odapas.reporte.privacidad;
import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
public final class Claves {
 private static final SecureRandom RANDOM=new SecureRandom();
 private Claves() {}
 public static String token() { byte[] b=new byte[32]; RANDOM.nextBytes(b); return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
 public static String huella(String texto) {
  try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
  catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
 }
 public static String guardar(String clave) {
  byte[] sal=new byte[16]; RANDOM.nextBytes(sal);
  return "pbkdf2$210000$"+Base64.getEncoder().encodeToString(sal)+"$"+Base64.getEncoder().encodeToString(derivar(clave,sal,210000));
 }
 private static byte[] derivar(String clave,byte[] sal,int vueltas) {
  PBEKeySpec spec=new PBEKeySpec(clave.toCharArray(),sal,vueltas,256);
  try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
  catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
  finally { spec.clearPassword(); }
 }
 public static boolean coincide(String clave,String hash) {
  if(clave==null || hash==null || !hash.startsWith("pbkdf2$")) return false;
  try {
   String[] p=hash.split("\\$"); int vueltas=Integer.parseInt(p[1]);
   if(vueltas<100000 || vueltas>1000000) return false;
   return MessageDigest.isEqual(derivar(clave,Base64.getDecoder().decode(p[2]),vueltas),Base64.getDecoder().decode(p[3]));
  } catch(RuntimeException e) { return false; }
 }
}

