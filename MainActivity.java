package com.example.sodukusolver;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Stack;

public class MainActivity extends AppCompatActivity {


    private EditText[][] gridCells = new EditText[9][9];
    private int[][] board = new int[9][9];
    private Stack<int[][]> undoStack = new Stack<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initGrid();
        initButtons();
    }

//main ui
    private void initGrid() {
        GridLayout gridLayout = findViewById(R.id.sudokuGrid);

      //limit user input 1-9 and only one number
        InputFilter oneDigitFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                if (source.length() == 0) return null;
                if (!source.toString().matches("[1-9]")) return "";
                if (dest.length() >= 1) return "";
                return null;
            }
        };

        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                EditText cell = new EditText(this);


                cell.setBackgroundResource(R.drawable.cell_border);
                cell.setGravity(Gravity.CENTER);
                cell.setTextColor(Color.BLUE);
                cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                cell.setIncludeFontPadding(false);
                cell.setInputType(InputType.TYPE_CLASS_NUMBER);
                cell.setFilters(new InputFilter[]{oneDigitFilter});
                cell.setBackgroundColor(Color.WHITE); // 默认背景为白色


                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.rowSpec = GridLayout.spec(row, 1f);
                params.columnSpec = GridLayout.spec(col, 1f);


                int topMargin = (row % 3 == 0 && row != 0) ? 4 : 1;
                int leftMargin = (col % 3 == 0 && col != 0) ? 4 : 1;
                params.setMargins(leftMargin, topMargin, 1, 1);

                cell.setLayoutParams(params);

                gridCells[row][col] = cell;
                gridLayout.addView(cell);
            }
        }
    }

    private void initButtons() {
        Button btnSolve = findViewById(R.id.btnSolve);
        Button btnCheck = findViewById(R.id.btnCheck);
        Button btnSeed = findViewById(R.id.btnSeed);
        Button btnClear = findViewById(R.id.btnClear);
        Button btnUndo = findViewById(R.id.btnUndo);

        btnUndo.setOnClickListener(e -> performUndo());
        btnClear.setOnClickListener(e -> { saveState(); clearGrid(); });
        btnSeed.setOnClickListener(e -> promptAndSeedGrid());

        btnCheck.setOnClickListener(e -> {
            readGridToModel();
            if (!isBoardValidForStart()) {
                showAlert("Check Result", "The puzzle is INVALID.");
                return;
            }
            int[][] tempBoard = cloneBoard(board);
            int solutions = countSolutions(0, 0);
            board = cloneBoard(tempBoard); // 恢复原状

            if (solutions == 0) showAlert("Result", "NO solution.");
            else if (solutions == 1) showAlert("Result", "VALID, single solution.");
            else showAlert("Result", "INVALID, multiple solutions.");
        });

        btnSolve.setOnClickListener(e -> {
            saveState();
            readGridToModel();
            if (solveSudokuBacktracking()) {
                updateGridFromModel(Color.BLACK);
            } else {
                undoStack.pop();
                showAlert("Result", "No valid solution.");
            }
        });
    }

    //main logic

    private int[][] cloneBoard(int[][] current) {
        int[][] copy = new int[9][9];
        for (int i = 0; i < 9; i++) {
            System.arraycopy(current[i], 0, copy[i], 0, 9);
        }
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
        } else {
            Toast.makeText(this, "Nothing to undo!", Toast.LENGTH_SHORT).show();
        }
    }

    private void readGridToModel() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                String text = gridCells[r][c].getText().toString().trim();
                board[r][c] = text.isEmpty() ? 0 : Integer.parseInt(text);
            }
        }
    }

    private void updateGridFromModel(int solveColor) {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] != 0) {
                    if (gridCells[r][c].getText().toString().trim().isEmpty()) {
                        gridCells[r][c].setTextColor(solveColor);
                    }
                    gridCells[r][c].setText(String.valueOf(board[r][c]));
                } else {
                    gridCells[r][c].setText("");
                }
            }
        }
    }

    private void clearGrid() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                board[r][c] = 0;
                gridCells[r][c].setText("");
                gridCells[r][c].setTextColor(Color.BLUE);
            }
        }
    }

    private void promptAndSeedGrid() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Paste an 81-character string:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String seedStr = input.getText().toString();
            if (seedStr.length() != 81) {
                Toast.makeText(this, "Must be exactly 81 characters", Toast.LENGTH_SHORT).show();
                return;
            }
            saveState();
            clearGrid();
            for (int i = 0; i < 81; i++) {
                char c = seedStr.charAt(i);
                if (Character.isDigit(c) && c != '0') {
                    board[i / 9][i % 9] = c - '0';
                    gridCells[i / 9][i % 9].setText(String.valueOf(c));
                }
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showAlert(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private boolean isBoardValidForStart() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (board[r][c] != 0) {
                    int temp = board[r][c];
                    board[r][c] = 0;
                    if (!isValidPlacement(r, c, temp)) {
                        board[r][c] = temp;
                        return false;
                    }
                    board[r][c] = temp;
                }
            }
        }
        return true;
    }

    private int countSolutions(int r, int c) {
        if (r == 9) return 1;
        int nextR = (c == 8) ? r + 1 : r;
        int nextC = (c + 1) % 9;

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
        int br = row - row % 3;
        int bc = col - col % 3;
        for (int r = br; r < br + 3; r++) {
            for (int c = bc; c < bc + 3; c++) {
                if (board[r][c] == num) return false;
            }
        }
        return true;
    }
}