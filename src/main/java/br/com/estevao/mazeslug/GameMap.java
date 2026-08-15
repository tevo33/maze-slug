package br.com.estevao.mazeslug;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class GameMap {
    private final char[][] cells;
    private final Point playerStart;
    private final Point enemyStart;
    private final Point exit;

    private GameMap(char[][] cells, Point playerStart, Point enemyStart, Point exit) {
        this.cells = cells;
        this.playerStart = playerStart;
        this.enemyStart = enemyStart;
        this.exit = exit;
    }

    static GameMap load(String resource) {
        InputStream stream = GameMap.class.getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalStateException("Mapa não encontrado: " + resource);
        }

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível carregar o mapa", exception);
        }

        if (lines.isEmpty()) {
            throw new IllegalStateException("O mapa está vazio");
        }

        int width = lines.get(0).length();
        char[][] cells = new char[lines.size()][width];
        Point playerStart = null;
        Point enemyStart = null;
        Point exit = null;

        for (int row = 0; row < lines.size(); row++) {
            if (lines.get(row).length() != width) {
                throw new IllegalStateException("Todas as linhas do mapa devem ter o mesmo tamanho");
            }
            for (int column = 0; column < width; column++) {
                char cell = lines.get(row).charAt(column);
                cells[row][column] = cell;
                if (cell == 'P') {
                    playerStart = unique(playerStart, new Point(column, row), "P");
                } else if (cell == 'T') {
                    enemyStart = unique(enemyStart, new Point(column, row), "T");
                } else if (cell == 'E') {
                    exit = unique(exit, new Point(column, row), "E");
                }
            }
        }

        if (playerStart == null || enemyStart == null || exit == null) {
            throw new IllegalStateException("O mapa precisa conter P, T e E");
        }

        return new GameMap(cells, playerStart, enemyStart, exit);
    }

    private static Point unique(Point current, Point value, String symbol) {
        if (current != null) {
            throw new IllegalStateException("Símbolo duplicado no mapa: " + symbol);
        }
        return value;
    }

    int rows() {
        return cells.length;
    }

    int columns() {
        return cells[0].length;
    }

    char cellAt(int row, int column) {
        return cells[row][column];
    }

    Point playerStart() {
        return new Point(playerStart);
    }

    Point enemyStart() {
        return new Point(enemyStart);
    }

    Point exit() {
        return new Point(exit);
    }
}
