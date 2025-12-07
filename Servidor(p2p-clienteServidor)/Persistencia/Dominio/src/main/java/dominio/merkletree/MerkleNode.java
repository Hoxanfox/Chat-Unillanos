package dominio.merkletree;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class MerkleNode {
    private final String hash;
    private final MerkleNode left;
    private final MerkleNode right;

    // Solo para nodos hoja
    private final IMerkleEntity data;

    /**
     * Constructor para Nodo Hoja (Leaf)
     */
    public MerkleNode(IMerkleEntity data) {
        this.data = data;
        this.left = null;
        this.right = null;
        this.hash = generarHash(data.getDatosParaHash());
    }

    /**
     * Constructor para Nodo Interno (Rama)
     */
    public MerkleNode(MerkleNode left, MerkleNode right) {
        this.data = null;
        this.left = left;
        this.right = right;
        // El hash de un nodo interno es el hash de sus hijos concatenados
        String leftHash = (left != null) ? left.getHash() : "";
        String rightHash = (right != null) ? right.getHash() : "";
        this.hash = generarHash(leftHash + rightHash);
    }

    public String getHash() { return hash; }
    public MerkleNode getLeft() { return left; }
    public MerkleNode getRight() { return right; }
    public IMerkleEntity getData() { return data; }
    public boolean esHoja() { return left == null && right == null; }

    private String generarHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error calculando hash SHA-256", e);
        }
    }
}