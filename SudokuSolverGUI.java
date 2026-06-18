import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Stack;

public class SudokuSolverGUI extends JFrame {

    //main component
    private JTextField[][] gridCells = new JTextField[9][9];
    private int[][] board = new int[9][9]; 
    private Stack<int[][]> undoStack = new Stack<>(); 

    public SudokuSolverGUI() {
        setTitle("Sudoku Solver by Jun Xian");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(25, 80, 120)); 

        UIManager.put("ToolTip.background", new Color(70, 130, 180));
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.font", new Font("Arial", Font.PLAIN, 12));

        add(createGridPanel(), BorderLayout.CENTER);
        add(createControlPanel(), BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

//UI Part


    private JPanel createGridPanel() {
        JPanel mainGridPanel = new JPanel(new BorderLayout());
        mainGridPanel.setOpaque(false);
        mainGridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(new JLabel("    "), BorderLayout.WEST);
        JPanel colLabels = new JPanel(new GridLayout(1, 9));
        colLabels.setOpaque(false);
        for (int i = 1; i <= 9; i++) {
            JLabel label = new JLabel(String.valueOf(i), SwingConstants.CENTER);
            label.setForeground(Color.WHITE);
            label.setFont(new Font("Arial", Font.BOLD, 14));
            colLabels.add(label);
        }
        topPanel.add(colLabels, BorderLayout.CENTER);
        mainGridPanel.add(topPanel, BorderLayout.NORTH);

        JPanel rowLabels = new JPanel(new GridLayout(9, 1));
        rowLabels.setOpaque(false);
        for (char c = 'A'; c <= 'I'; c++) {
            JLabel label = new JLabel(" " + c + " ", SwingConstants.CENTER);
            label.setForeground(Color.WHITE);
            label.setFont(new Font("Arial", Font.BOLD, 14));
            rowLabels.add(label);
        }
        mainGridPanel.add(rowLabels, BorderLayout.WEST);

        JPanel grid = new JPanel(new GridLayout(9, 9));
        grid.setBackground(Color.BLACK);
        grid.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                gridCells[row][col] = new JTextField();
                
                AbstractDocument doc = (AbstractDocument) gridCells[row][col].getDocument();
                doc.setDocumentFilter(new OneDigitFilter());
                
                gridCells[row][col].setHorizontalAlignment(JTextField.CENTER);
                gridCells[row][col].setFont(new Font("Arial", Font.BOLD, 24));
                gridCells[row][col].setForeground(Color.BLUE); 
                
                int top = (row % 3 == 0 && row != 0) ? 2 : 1;
                int left = (col % 3 == 0 && col != 0) ? 2 : 1;
                int bottom = (row == 8) ? 1 : 0;
                int right = (col == 8) ? 1 : 0;
                gridCells[row][col].setBorder(BorderFactory.createMatteBorder(top, left, bottom, right, Color.BLACK));
                
                final int r = row;
                final int c = col;
                gridCells[row][col].addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        int code = e.getKeyCode();
                        int nextR = r, nextC = c;
                        if (code == KeyEvent.VK_UP) nextR = (r - 1 + 9) % 9;
                        else if (code == KeyEvent.VK_DOWN) nextR = (r + 1) % 9;
                        else if (code == KeyEvent.VK_LEFT) nextC = (c - 1 + 9) % 9;
                        else if (code == KeyEvent.VK_RIGHT) nextC = (c + 1) % 9;
                        gridCells[nextR][nextC].requestFocus();
                    }
                });
                grid.add(gridCells[row][col]);
            }
        }
        grid.setPreferredSize(new Dimension(450, 450));
        mainGridPanel.add(grid, BorderLayout.CENTER);
        return mainGridPanel;
    }

    private JPanel createControlPanel() {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setOpaque(false);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 20));
        Color panelBg = new Color(30, 95, 140);

        JButton btnSolve = new JButton("Solve");
        JButton btnCheck = new JButton("Check");
        JButton btnSeed = new JButton("Seed");
        JButton btnClear = new JButton("Clear");
        JButton btnUndo = new JButton("Undo");

        btnCheck.setToolTipText("Checks if the puzzle is valid and has a single solution.");
        btnUndo.setToolTipText("Revert changes one by one");
        btnSeed.setToolTipText("Seed the grid from a pasted character string");

        btnUndo.addActionListener(e -> performUndo());
        btnClear.addActionListener(e -> { saveState(); clearGrid(); });
        btnSeed.addActionListener(e -> promptAndSeedGrid());

        btnCheck.addActionListener(e -> {
            readGridToModel();
            if (!isBoardValidForStart()) {
                JOptionPane.showMessageDialog(this, "The puzzle is INVALID.", "Check Result", JOptionPane.ERROR_MESSAGE);
                return;
            }
            int[][] tempBoard = cloneBoard(board);
            int solutions = countSolutions(0, 0);
            board = cloneBoard(tempBoard);
            if (solutions == 0) JOptionPane.showMessageDialog(this, "NO solution.");
            else if (solutions == 1) JOptionPane.showMessageDialog(this, "VALID, single solution.");
            else JOptionPane.showMessageDialog(this, "INVALID, multiple solutions.");
        });

        btnSolve.addActionListener(e -> {
            saveState();
            readGridToModel();
            if (solveSudokuBacktracking()) updateGridFromModel(Color.BLACK);
            else { undoStack.pop(); JOptionPane.showMessageDialog(this, "No valid solution."); }
        });

        controlPanel.add(createSectionPanel("Solving", panelBg, new Component[]{btnSolve}));
        controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        controlPanel.add(createSectionPanel("Puzzle Analysis", panelBg, new Component[]{btnCheck}));
        controlPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        controlPanel.add(createSectionPanel("Puzzle", panelBg, new Component[]{btnSeed, btnClear, btnUndo}));

        return controlPanel;
    }

    private JPanel createSectionPanel(String title, Color bgColor, Component[] components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(bgColor);
        TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), title);
        border.setTitleColor(Color.WHITE);
        panel.setBorder(border);
        for (Component comp : components) {
            if (comp instanceof JButton) ((JButton) comp).setFocusPainted(false);
            panel.add(comp);
        }
        panel.setMaximumSize(new Dimension(350, 150));
        return panel;
    }

// Logic Part

    private int[][] cloneBoard(int[][] current) {
        int[][] copy = new int[9][9];
        for (int i = 0; i < 9; i++) System.arraycopy(current[i], 0, copy[i], 0, 9);
        return copy;
    }

    private void saveState() {
        readGridToModel();
        undoStack.push(cloneBoard(board));
    }

    private void performUndo() {
        if (!undoStack.isEmpty()) {
            board = undoStack.pop();
            updateGridFromModel(Color.BLACK);
        } else JOptionPane.showMessageDialog(this, "Nothing to undo!");
    }

    private void readGridToModel() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                String text = gridCells[r][c].getText().trim();
                board[r][c] = text.isEmpty() ? 0 : Integer.parseInt(text);
            }
        }
    }

    private void updateGridFromModel(Color solveColor) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] != 0) {
                    if (gridCells[r][c].getText().trim().isEmpty()) gridCells[r][c].setForeground(solveColor);
                    gridCells[r][c].setText(String.valueOf(board[r][c]));
                } else gridCells[r][c].setText("");
            }
        }
    }

    private void clearGrid() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                board[r][c] = 0;
                gridCells[r][c].setText("");
                gridCells[r][c].setForeground(Color.BLUE);
            }
        }
    }

    private void promptAndSeedGrid() {
        String input = JOptionPane.showInputDialog(this, "Paste an 81-character string:");
        if (input == null || input.length() != 81) return;
        saveState();
        clearGrid();
        for (int i = 0; i < 81; i++) {
            char c = input.charAt(i);
            if (Character.isDigit(c) && c != '0') {
                board[i/9][i%9] = c - '0';
                gridCells[i/9][i%9].setText(String.valueOf(c));
            }
        }
    }

    private boolean isBoardValidForStart() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] != 0) {
                    int temp = board[r][c]; board[r][c] = 0;
                    if (!isValidPlacement(r, c, temp)) { board[r][c] = temp; return false; }
                    board[r][c] = temp;
                }
            }
        }
        return true;
    }

    private int countSolutions(int r, int c) {
        if (r == 9) return 1;
        int nextR = (c == 8) ? r + 1 : r, nextC = (c + 1) % 9;
        if (board[r][c] != 0) return countSolutions(nextR, nextC);
        int count = 0;
        for (int num = 1; num <= 9; num++) {
            if (isValidPlacement(r, c, num)) {
                board[r][c] = num;
                count += countSolutions(nextR, nextC);
                board[r][c] = 0;
                if (count > 1) return count;
            }
        }
        return count;
    }

    private boolean solveSudokuBacktracking() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] == 0) {
                    for (int num = 1; num <= 9; num++) {
                        if (isValidPlacement(r, c, num)) {
                            board[r][c] = num;
                            if (solveSudokuBacktracking()) return true;
                            board[r][c] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isValidPlacement(int row, int col, int num) {
        for (int i = 0; i < 9; i++) {
            if (board[row][i] == num || board[i][col] == num) return false;
        }
        int br = row - row % 3, bc = col - col % 3;
        for (int r = br; r < br + 3; r++) {
            for (int c = bc; c < bc + 3; c++) if (board[r][c] == num) return false;
        }
        return true;
    }

    class OneDigitFilter extends DocumentFilter {
        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            if (text == null) return;
            String f = fb.getDocument().getText(0, fb.getDocument().getLength()).substring(0, offset) + text;
            if (f.isEmpty() || (f.length() == 1 && f.matches("[1-9]"))) super.replace(fb, offset, length, text, attrs);
        }
        @Override
        public void insertString(FilterBypass fb, int offset, String s, AttributeSet a) throws BadLocationException { replace(fb, offset, 0, s, a); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SudokuSolverGUI().setVisible(true));
    }
}