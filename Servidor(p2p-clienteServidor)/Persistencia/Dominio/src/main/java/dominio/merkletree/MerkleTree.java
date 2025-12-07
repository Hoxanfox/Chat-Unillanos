package dominio.merkletree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MerkleTree {
    private MerkleNode root;

    public MerkleTree(List<? extends IMerkleEntity> datos) {
        if (datos == null || datos.isEmpty()) {
            this.root = null;
            return;
        }
        // 1. Ordenar para consistencia
        List<IMerkleEntity> sorted = new ArrayList<>(datos);
        sorted.sort(Comparator.comparing(IMerkleEntity::getId));

        // 2. Crear hojas
        List<MerkleNode> currentLevel = new ArrayList<>();
        for (IMerkleEntity entity : sorted) currentLevel.add(new MerkleNode(entity));

        // 3. Construir hacia arriba
        this.root = construirRecursivo(currentLevel);
    }

    private MerkleNode construirRecursivo(List<MerkleNode> nodes) {
        if (nodes.size() == 1) return nodes.get(0);

        List<MerkleNode> nextLevel = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i += 2) {
            MerkleNode left = nodes.get(i);
            MerkleNode right = (i + 1 < nodes.size()) ? nodes.get(i + 1) : null;
            nextLevel.add(new MerkleNode(left, right));
        }
        return construirRecursivo(nextLevel);
    }

    public String getRootHash() {
        return (root != null) ? root.getHash() : "EMPTY";
    }

    // --- VISUALIZACIÓN GRÁFICA ---
    public void imprimirArbol() {
        if (root == null) {
            System.out.println("(Árbol Vacío)");
            return;
        }
        System.out.println("\n--- ESTADO DEL ÁRBOL MERKLE ---");
        printRecursive(root, "", true);
        System.out.println("-------------------------------");
    }

    private void printRecursive(MerkleNode node, String prefix, boolean isTail) {
        if (node == null) return;

        // Cortar hash para legibilidad
        String hashCorto = node.getHash().length() > 6 ? node.getHash().substring(0, 6) + "..." : node.getHash();

        // Iconos y colores
        String tipo = node.esHoja() ? "\u001B[32m🍃 HOJA\u001B[0m" : "\u001B[34m🌳 NODO\u001B[0m";
        String info = node.esHoja() ? " (ID: " + node.getData().getId() + ")" : "";

        System.out.println(prefix + (isTail ? "└── " : "├── ") + tipo + " Hash: " + hashCorto + info);

        // Hijos
        List<MerkleNode> children = new ArrayList<>();
        if (node.getLeft() != null) children.add(node.getLeft());
        if (node.getRight() != null) children.add(node.getRight());

        for (int i = 0; i < children.size(); i++) {
            printRecursive(children.get(i), prefix + (isTail ? "    " : "│   "), i == children.size() - 1);
        }
    }
}